package com.example.homehubapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.example.homehubapp.model.LoginRequest
import com.example.homehubapp.model.LoginResponse
import com.example.homehubapp.model.SsoRequest
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch
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
        val btnGoogle = findViewById<Button>(R.id.btnGoogleSignIn)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        intent.getStringExtra("registered_email")?.let { etEmail.setText(it) }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            when {
                email.isBlank() || password.isBlank() -> showMessage(getString(R.string.msg_enter_credentials))
                !ValidationUtils.isValidEmail(email) -> showMessage(getString(R.string.msg_invalid_email))
                else -> {
                    btnLogin.isEnabled = false
                    Log.d("HomeHub", "Login request started")
                    ApiClient.service.login(LoginRequest(email, password))
                        .enqueue(object : Callback<LoginResponse> {
                            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                                btnLogin.isEnabled = true
                                val body = response.body()
                                if (response.isSuccessful && body != null) {
                                    finishLogin(body)
                                } else {
                                    showMessage(ApiErrorParser.message(response, getString(R.string.msg_login_failed)))
                                }
                            }

                            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                                btnLogin.isEnabled = true
                                Log.e("HomeHub", "Login API failed", t)
                                showMessage(getString(R.string.msg_server_down))
                            }
                        })
                }
            }
        }

        // Single sign-on with Google
        btnGoogle.setOnClickListener { signInWithGoogle() }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Step 1: ask Android's Credential Manager for a Google ID token
    private fun signInWithGoogle() {
        val credentialManager = CredentialManager.create(this)
        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(getString(R.string.default_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(googleOption).build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(this@LoginActivity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    firebaseSignIn(googleCredential.idToken)
                } else {
                    showMessage(getString(R.string.msg_sso_failed))
                }
            } catch (e: GetCredentialException) {
                Log.e("HomeHub", "Google sign-in failed", e)
                showMessage(getString(R.string.msg_sso_failed))
            }
        }
    }

    // Step 2: sign into Firebase with the Google token, Step 3: tell our own API
    private fun firebaseSignIn(idToken: String) {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        FirebaseAuth.getInstance().signInWithCredential(firebaseCredential)
            .addOnSuccessListener {
                Log.i("HomeHub", "Firebase SSO successful")
                ApiClient.service.ssoLogin(SsoRequest(idToken))
                    .enqueue(object : Callback<LoginResponse> {
                        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                            val body = response.body()
                            if (response.isSuccessful && body != null) {
                                finishLogin(body)
                            } else {
                                showMessage(ApiErrorParser.message(response, getString(R.string.msg_sso_failed)))
                            }
                        }

                        override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                            Log.e("HomeHub", "SSO API failed", t)
                            showMessage(getString(R.string.msg_server_down))
                        }
                    })
            }
            .addOnFailureListener {
                Log.e("HomeHub", "Firebase sign-in failed", it)
                showMessage(getString(R.string.msg_sso_failed))
            }
    }

    // Shared by normal login and SSO login
    private fun finishLogin(body: LoginResponse) {
        SessionManager.saveLogin(this, body.userId, body.name, body.email)
        Log.i("HomeHub", "Login successful for user ${body.userId}")
        showMessage(getString(R.string.msg_welcome, body.name))
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showMessage(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}