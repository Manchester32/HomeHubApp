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
    private lateinit var tvNoRequests: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_requests)

        requestsContainer = findViewById(R.id.requestsContainer)
        tvNoRequests = findViewById(R.id.tvNoRequests)
        findViewById<TextView>(R.id.tvBackRequests).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadRequests()
    }

    private fun loadRequests() {
        val userId = SessionManager.userId(this)
        if (userId == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        requestsContainer.removeAllViews()
        tvNoRequests.text = "Loading requests..."
        tvNoRequests.visibility = View.VISIBLE

        ApiClient.service.getUserRequests(userId).enqueue(object : Callback<List<ServiceRequestDto>> {
            override fun onResponse(call: Call<List<ServiceRequestDto>>, response: Response<List<ServiceRequestDto>>) {
                if (!response.isSuccessful) {
                    tvNoRequests.text = "Could not load requests."
                    return
                }

                val requests = response.body().orEmpty()
                tvNoRequests.visibility = if (requests.isEmpty()) View.VISIBLE else View.GONE
                tvNoRequests.text = "You have no service requests yet."
                requests.forEach { addRequestCard(it) }
                Log.d("HomeHub", "Loaded ${requests.size} requests")
            }

            override fun onFailure(call: Call<List<ServiceRequestDto>>, t: Throwable) {
                Log.e("HomeHub", "Requests API failed", t)
                tvNoRequests.text = "Could not connect to the HomeHub server."
            }
        })
    }

    private fun addRequestCard(request: ServiceRequestDto) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            setBackgroundColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, dp(16)) }
        }

        card.addView(label(request.serviceName, 20f, "#0B3273", true))
        card.addView(label("Status: ${request.status}", 15f, statusColor(request.status), true))
        card.addView(label("Problem: ${request.description}"))
        card.addView(label("Address: ${request.serviceAddress}"))
        card.addView(label("Preferred date: ${request.preferredDate.take(10)}"))

        if (request.status == "Completed") {
            card.addView(label("Amount: R${String.format("%.2f", request.amount)}", 16f, "#0B3273", true))
        }

        if (request.isPaid) {
            card.addView(label("Payment: Confirmed", 15f, "#2E7D32", true))
        } else if (request.status == "Completed") {
            card.addView(actionButton("Choose Cash / Card", "#F7941D") {
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
                card.addView(actionButton("Demo: Move to $next", "#0B3273") {
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
                        Toast.makeText(this@MyRequestsActivity, "Status updated to $status", Toast.LENGTH_SHORT).show()
                        loadRequests()
                    } else {
                        Toast.makeText(
                            this@MyRequestsActivity,
                            ApiErrorParser.message(response, "Could not update status"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                    Log.e("HomeHub", "Status API failed", t)
                    Toast.makeText(this@MyRequestsActivity, "Could not connect to the HomeHub server", Toast.LENGTH_LONG).show()
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

    private fun statusColor(status: String) = when (status) {
        "Completed" -> "#2E7D32"
        "In Progress" -> "#F7941D"
        else -> "#B26A00"
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
