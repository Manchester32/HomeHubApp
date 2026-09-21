package com.example.homehubapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.homehubapp.model.ApiResponse
import com.example.homehubapp.model.RegisterRequest
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        val etName = findViewById<EditText>(R.id.etName)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)
        val tvBackToLogin = findViewById<TextView>(R.id.tvBackToLogin)

        tvBackToLogin.setOnClickListener { finish() }

        btnRegister.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            val error = when {
                name.isBlank() || email.isBlank() || password.isBlank() || confirmPassword.isBlank() -> "Please complete all fields"
                !ValidationUtils.isValidEmail(email) -> "Please enter a valid email address"
                !ValidationUtils.isValidPassword(password) -> "Password needs 6+ characters, uppercase, lowercase, number and symbol"
                password != confirmPassword -> "Passwords do not match"
                else -> null
            }

            if (error != null) {
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            Log.d("HomeHub", "Registration request started")
            ApiClient.service.register(RegisterRequest(name, email, password, confirmPassword))
                .enqueue(object : Callback<ApiResponse> {
                    override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                        btnRegister.isEnabled = true
                        if (response.isSuccessful) {
                            Log.i("HomeHub", "Registration successful")
                            Toast.makeText(this@RegisterActivity, "Registration successful. Please log in.", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
                                .putExtra("registered_email", email)
                            startActivity(intent)
                            finish()
                        } else {
                            Toast.makeText(
                                this@RegisterActivity,
                                ApiErrorParser.message(response, "Registration failed"),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                        btnRegister.isEnabled = true
                        Log.e("HomeHub", "Registration API failed", t)
                        Toast.makeText(
                            this@RegisterActivity,
                            "Could not connect to the HomeHub server. Make sure the API is running.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                })
        }
    }
}
