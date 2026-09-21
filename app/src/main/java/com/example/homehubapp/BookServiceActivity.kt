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
                showMessage("Session or service information is missing. Please log in again.")
                return@setOnClickListener
            }
            if (description.isBlank() || address.isBlank() || dateText.isBlank()) {
                showMessage("Please complete all fields")
                return@setOnClickListener
            }

            val apiDate = parseDate(dateText)
            if (apiDate == null) {
                showMessage("Use a valid date such as 20/09/2026")
                return@setOnClickListener
            }
            if (apiDate == "PAST") {
                showMessage("Preferred date cannot be in the past")
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

            ApiClient.service.createServiceRequest(request)
                .enqueue(object : Callback<RequestCreatedResponse> {
                    override fun onResponse(call: Call<RequestCreatedResponse>, response: Response<RequestCreatedResponse>) {
                        btnSubmitRequest.isEnabled = true
                        if (response.isSuccessful) {
                            Log.i("HomeHub", "Service request created")
                            showMessage("Service request submitted successfully")
                            startActivity(Intent(this@BookServiceActivity, MyRequestsActivity::class.java))
                            finish()
                        } else {
                            showMessage(ApiErrorParser.message(response, "Could not submit request"))
                        }
                    }

                    override fun onFailure(call: Call<RequestCreatedResponse>, t: Throwable) {
                        btnSubmitRequest.isEnabled = true
                        Log.e("HomeHub", "Create request API failed", t)
                        showMessage("Could not connect to the HomeHub server")
                    }
                })
        }
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
