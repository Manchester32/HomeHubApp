package com.example.homehubapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homehubapp.model.LoginRequest
import com.example.homehubapp.model.LoginResponse
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        intent.getStringExtra("registered_email")?.let { etEmail.setText(it) }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                email.isBlank() || password.isBlank() -> showMessage("Please enter email and password")
                !ValidationUtils.isValidEmail(email) -> showMessage("Please enter a valid email address")
                else -> {
                    btnLogin.isEnabled = false
                    Log.d("HomeHub", "Login request started")
                    ApiClient.service.login(LoginRequest(email, password))
                        .enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                btnLogin.isEnabled = true
                                val body = response.body()
                                if (response.isSuccessful && body != null) {
                                    SessionManager.saveLogin(this@LoginActivity, body.userId, body.name, body.email)
                                    Log.i("HomeHub", "Login successful for user ${body.userId}")
                                    showMessage("Welcome ${body.name}")
                                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                                    finish()
                                } else {
                                    showMessage(ApiErrorParser.message(response, "Invalid email or password"))
                                }
                            }

                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                btnLogin.isEnabled = true
                                Log.e("HomeHub", "Login API failed", t)
                                showMessage("Could not connect to the HomeHub server. Make sure the API is running.")
                            }
                        })
                }
            }
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showMessage(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}
