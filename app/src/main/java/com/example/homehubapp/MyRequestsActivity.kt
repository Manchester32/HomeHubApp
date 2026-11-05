package com.example.homehubapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homehubapp.model.ApiResponse
import com.example.homehubapp.model.ServiceRequestDto
import com.example.homehubapp.model.UpdateStatusRequest
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyRequestsActivity : AppCompatActivity() {

    private lateinit var requestsContainer: LinearLayout
    private lateinit var pendingContainer: LinearLayout
    private lateinit var tvNoRequests: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_requests)

        requestsContainer = findViewById(R.id.requestsContainer)
        pendingContainer = findViewById(R.id.pendingContainer)
        tvNoRequests = findViewById(R.id.tvNoRequests)
        findViewById<TextView>(R.id.tvBackRequests).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
        // Send any offline requests, then refresh the list when they have synced
        SyncManager.syncPending(this) { loadRequests() }
    }

    private fun loadRequests() {
        val userId = SessionManager.userId(this)
        if (userId == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Offline requests come from the phone's own database, so show them straight away
        val pendingCount = addPendingCards(userId)
        Log.d("HomeHub", "Waiting-to-sync requests on this phone: $pendingCount")

        requestsContainer.removeAllViews()
        tvNoRequests.text = getString(R.string.msg_loading_requests)
        tvNoRequests.visibility = if (pendingCount == 0) View.VISIBLE else View.GONE

        ApiClient.service.getUserRequests(userId).enqueue(object : Callback<List<ServiceRequestDto>> {
            override fun onResponse(call: Call<List<ServiceRequestDto>>, response: Response<List<ServiceRequestDto>>) {
                requestsContainer.removeAllViews()

                if (!response.isSuccessful) {
                    tvNoRequests.text = getString(R.string.msg_requests_failed)
                    tvNoRequests.visibility = if (pendingCount == 0) View.VISIBLE else View.GONE
                    return
                }

                val requests = response.body().orEmpty()
                tvNoRequests.visibility = if (requests.isEmpty() && pendingCount == 0) View.VISIBLE else View.GONE
                tvNoRequests.text = getString(R.string.no_requests)
                requests.forEach { addRequestCard(it) }
                Log.d("HomeHub", "Loaded ${requests.size} requests")
            }

            override fun onFailure(call: Call<List<ServiceRequestDto>>, t: Throwable) {
                Log.e("HomeHub", "Requests API failed", t)
                requestsContainer.removeAllViews()
                // Offline: the waiting requests above stay visible
                tvNoRequests.visibility = if (pendingCount == 0) View.VISIBLE else View.GONE
                tvNoRequests.text = getString(R.string.msg_server_down)
            }
        })
    }

    // Shows requests that are saved offline and waiting to sync
    private fun addPendingCards(userId: Int): Int {
        pendingContainer.removeAllViews()
        val pending = LocalDb.get(this).getPending(userId)
        pending.forEach { p ->
            val card = newCard()
            card.addView(label(p.serviceName, 20f, "#0B3273", true))
            card.addView(label(getString(R.string.lbl_waiting_sync), 15f, "#B26A00", true))
            card.addView(label(getString(R.string.lbl_problem, p.request.description)))
            card.addView(label(getString(R.string.lbl_address, p.request.serviceAddress)))
            card.addView(label(getString(R.string.lbl_preferred_date, p.request.preferredDate.take(10))))
            pendingContainer.addView(card)
        }
        return pending.size
    }

    private fun newCard() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        setBackgroundColor(Color.WHITE)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, 0, 0, dp(16)) }
    }

    private fun addRequestCard(request: ServiceRequestDto) {
        val card = newCard()

        card.addView(label(request.serviceName, 20f, "#0B3273", true))
        card.addView(label(getString(R.string.lbl_status, statusLabel(request.status)), 15f, statusColor(request.status), true))
        card.addView(label(getString(R.string.lbl_problem, request.description)))
        card.addView(label(getString(R.string.lbl_address, request.serviceAddress)))
        card.addView(label(getString(R.string.lbl_preferred_date, request.preferredDate.take(10))))

        if (request.status == "Completed") {
            card.addView(label(getString(R.string.lbl_amount, String.format("%.2f", request.amount)), 16f, "#0B3273", true))
        }

        if (request.isPaid) {
            card.addView(label(getString(R.string.lbl_payment_confirmed), 15f, "#2E7D32", true))
        } else if (request.status == "Completed") {
            card.addView(actionButton(getString(R.string.btn_choose_payment), "#F7941D") {
                startActivity(
                    Intent(this, PaymentActivity::class.java)
                        .putExtra("requestId", request.id)
                        .putExtra("service", request.serviceName)
                        .putExtra("amount", request.amount)
                )
            })
        } else {
            // Prototype-only helper to demonstrate the full Part 2 request flow.
            val next = RequestStatusHelper.nextStatus(request.status)
            if (next != null) {
                card.addView(actionButton(getString(R.string.btn_demo_move, statusLabel(next)), "#0B3273") {
                    val amount = if (next == "Completed") RequestStatusHelper.amountWhenCompleted() else null
                    updateStatus(request.id, next, amount)
                })
            }
        }

        requestsContainer.addView(card)
    }

    private fun updateStatus(id: Int, status: String, amount: Double?) {
        ApiClient.service.updateRequestStatus(id, UpdateStatusRequest(status, amount))
            .enqueue(object : Callback<ApiResponse> {
                override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@MyRequestsActivity, getString(R.string.msg_status_updated, statusLabel(status)), Toast.LENGTH_SHORT).show()
                        loadRequests()
                    } else {
                        Toast.makeText(
                            this@MyRequestsActivity,
                            ApiErrorParser.message(response, getString(R.string.msg_status_failed)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e("HomeHub", "Status API failed", t)
                    Toast.makeText(this@MyRequestsActivity, getString(R.string.msg_server_down), Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun label(
        text: String,
        size: Float = 14f,
        color: String = "#333333",
        bold: Boolean = false
    ) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(Color.parseColor(color))
        setPadding(0, dp(7), 0, 0)
        if (bold) setTypeface(null, android.graphics.Typeface.BOLD)
    }

    private fun actionButton(text: String, color: String, click: () -> Unit) = Button(this).apply {
        this.text = text
        setTextColor(Color.WHITE)
        setBackgroundColor(Color.parseColor(color))
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            dp(52)
        ).apply { setMargins(0, dp(14), 0, 0) }
        setOnClickListener { click() }
    }

    // The database always stores English status names; this translates them for display
    private fun statusLabel(status: String) = when (status) {
        "Pending" -> getString(R.string.status_pending)
        "In Progress" -> getString(R.string.status_in_progress)
        "Completed" -> getString(R.string.status_completed)
        else -> status
    }

    private fun statusColor(status: String) = when (status) {
        "Completed" -> "#2E7D32"
        "In Progress" -> "#F7941D"
        else -> "#B26A00"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}