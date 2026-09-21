package com.example.homehubapp

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.homehubapp.model.UpdateProfileRequest
import com.example.homehubapp.model.UserProfile
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MyHomeActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_home)

        etName = findViewById(R.id.etProfileName)
        etEmail = findViewById(R.id.etProfileEmail)
        etAddress = findViewById(R.id.etHomeAddress)
        etEmail.isEnabled = false

        findViewById<TextView>(R.id.tvBackHome).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            SessionManager.clear(this)
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        loadProfile()
    }

    private fun loadProfile() {
        val userId = SessionManager.userId(this)
        ApiClient.service.getUser(userId).enqueue(object : Callback<UserProfile> {
            override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                val user = response.body()
                if (response.isSuccessful && user != null) {
                    etName.setText(user.name)
                    etEmail.setText(user.email)
                    etAddress.setText(user.homeAddress)
                } else {
                    Toast.makeText(this@MyHomeActivity, "Could not load profile", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Log.e("HomeHub", "Profile API failed", t)
                Toast.makeText(this@MyHomeActivity, "Could not connect to the HomeHub server", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(this, "Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = SessionManager.userId(this)
        ApiClient.service.updateUser(userId, UpdateProfileRequest(name, address))
            .enqueue(object : Callback<UserProfile> {
                override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                    val profile = response.body()
                    if (response.isSuccessful && profile != null) {
                        SessionManager.updateName(this@MyHomeActivity, profile.name)
                        Toast.makeText(this@MyHomeActivity, "Profile saved successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this@MyHomeActivity,
                            ApiErrorParser.message(response, "Could not save profile"),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                    Log.e("HomeHub", "Update profile API failed", t)
                    Toast.makeText(this@MyHomeActivity, "Could not connect to the HomeHub server", Toast.LENGTH_LONG).show()
                }
            })
    }
}
