package com.example.homehubapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homehubapp.model.CreatePaymentRequest
import com.example.homehubapp.model.PaymentResponse
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        val requestId = intent.getIntExtra("requestId", -1)
        val service = intent.getStringExtra("service") ?: "Home Service"
        val amount = intent.getDoubleExtra("amount", 0.0)
        val tvPaymentDetails = findViewById<TextView>(R.id.tvPaymentDetails)
        val radioGroup = findViewById<RadioGroup>(R.id.radioGroupPayment)
        val btnConfirm = findViewById<Button>(R.id.btnConfirmPayment)

        tvPaymentDetails.text = "Service: $service\nAmount: R${String.format("%.2f", amount)}"

        when (SettingsStore.defaultPayment(this)) {
            "Card" -> findViewById<RadioButton>(R.id.radioCard).isChecked = true
            else -> findViewById<RadioButton>(R.id.radioCash).isChecked = true
        }

        btnConfirm.setOnClickListener {
            val selectedId = radioGroup.checkedRadioButtonId
            if (requestId == -1 || selectedId == -1) {
                Toast.makeText(this, "Please choose Cash or Card", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val method = findViewById<RadioButton>(selectedId).text.toString()
            btnConfirm.isEnabled = false

            ApiClient.service.createPayment(CreatePaymentRequest(requestId, method))
                .enqueue(object : Callback<PaymentResponse> {
                    override fun onResponse(call: Call<PaymentResponse>, response: Response<PaymentResponse>) {
                        btnConfirm.isEnabled = true
                        if (response.isSuccessful) {
                            Log.i("HomeHub", "Payment method confirmed for request $requestId")
                            Toast.makeText(
                                this@PaymentActivity,
                                "$method payment method saved successfully",
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        } else {
                            Toast.makeText(
                                this@PaymentActivity,
                                ApiErrorParser.message(response, "Could not save payment"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<PaymentResponse>, t: Throwable) {
                        btnConfirm.isEnabled = true
                        Log.e("HomeHub", "Payment API failed", t)
                        Toast.makeText(this@PaymentActivity, "Could not connect to the HomeHub server", Toast.LENGTH_LONG).show()
                    }
                })
        }
    }
}
