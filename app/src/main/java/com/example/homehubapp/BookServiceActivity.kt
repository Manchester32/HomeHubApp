package com.example.homehubapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homehubapp.model.CreateServiceRequest
import com.example.homehubapp.model.RequestCreatedResponse
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class BookServiceActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_service)

        val serviceId = intent.getIntExtra("serviceId", -1)
        val selectedService = intent.getStringExtra("service") ?: "Home Service"
        val tvSelectedService = findViewById<TextView>(R.id.tvSelectedService)
        val etDescription = findViewById<EditText>(R.id.etDescription)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val etDate = findViewById<EditText>(R.id.etDate)
        val btnSubmitRequest = findViewById<Button>(R.id.btnSubmitRequest)

        tvSelectedService.text = selectedService

        btnSubmitRequest.setOnClickListener {
            val description = etDescription.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val dateText = etDate.text.toString().trim()
            val userId = SessionManager.userId(this)

            if (serviceId == -1 || userId == -1) {
                showMessage(getString(R.string.msg_session_missing))
                return@setOnClickListener
            }
            if (description.isBlank() || address.isBlank() || dateText.isBlank()) {
                showMessage(getString(R.string.msg_complete_fields))
                return@setOnClickListener
            }

            val apiDate = parseDate(dateText)
            if (apiDate == null) {
                showMessage(getString(R.string.msg_invalid_date))
                return@setOnClickListener
            }
            if (apiDate == "PAST") {
                showMessage(getString(R.string.msg_past_date))
                return@setOnClickListener
            }

            btnSubmitRequest.isEnabled = false
            val request = CreateServiceRequest(
                userId = userId,
                serviceId = serviceId,
                description = description,
                serviceAddress = address,
                preferredDate = apiDate
            )

            // Offline mode: no connection, so save the request on the phone and sync it later
            if (!SyncManager.isOnline(this)) {
                saveOffline(request, selectedService)
                return@setOnClickListener
            }

            ApiClient.service.createServiceRequest(request)
                .enqueue(object : Callback<RequestCreatedResponse> {
                    override fun onResponse(call: Call<RequestCreatedResponse>, response: Response<RequestCreatedResponse>) {
                        btnSubmitRequest.isEnabled = true
                        if (response.isSuccessful) {
                            Log.i("HomeHub", "Service request created")
                            showMessage(getString(R.string.msg_request_ok))
                            startActivity(Intent(this@BookServiceActivity, MyRequestsActivity::class.java))
                            finish()
                        } else {
                            showMessage(ApiErrorParser.message(response, getString(R.string.msg_request_failed)))
                        }
                    }

                    override fun onFailure(call: Call<RequestCreatedResponse>, t: Throwable) {
                        btnSubmitRequest.isEnabled = true
                        Log.e("HomeHub", "Create request API failed", t)
                        // Server unreachable: keep the request locally so the user does not lose it
                        saveOffline(request, selectedService)
                    }
                })
        }
    }

    // Stores the request in the local SQLite database; SyncManager sends it later
    private fun saveOffline(request: CreateServiceRequest, serviceName: String) {
        LocalDb.get(this).addPending(request, serviceName)
        Log.i("HomeHub", "No connection - request saved locally for later sync")
        showMessage(getString(R.string.msg_saved_offline))
        startActivity(Intent(this, MyRequestsActivity::class.java))
        finish()
    }

    private fun parseDate(value: String): String? {
        val inputFormats = listOf("dd/MM/yyyy", "yyyy-MM-dd")
        for (pattern in inputFormats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }
                val parsed = parser.parse(value) ?: continue

                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                if (parsed.before(today)) return "PAST"
                return SimpleDateFormat("yyyy-MM-dd'T'00:00:00", Locale.US).format(parsed)
            } catch (_: Exception) {
                // Try the next supported date format.
            }
        }
        return null
    }

    private fun showMessage(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}