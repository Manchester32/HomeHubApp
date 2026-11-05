package com.example.homehubapp

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.homehubapp.model.UpdateProfileRequest
import com.example.homehubapp.model.UserProfile
import com.example.homehubapp.network.ApiClient
import com.example.homehubapp.network.ApiErrorParser
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class MyHomeActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etAddress: EditText
    private lateinit var ivPhoto: ImageView

    // Opens the phone's gallery and returns the chosen picture
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) uploadPhoto(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_home)

        etName = findViewById(R.id.etProfileName)
        etEmail = findViewById(R.id.etProfileEmail)
        etAddress = findViewById(R.id.etHomeAddress)
        ivPhoto = findViewById(R.id.ivProfilePhoto)
        etEmail.isEnabled = false

        findViewById<TextView>(R.id.tvBackHome).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnSaveProfile).setOnClickListener { saveProfile() }
        findViewById<Button>(R.id.btnChangePhoto).setOnClickListener { pickImage.launch("image/*") }
        findViewById<Button>(R.id.btnLogout).setOnClickListener {
            SessionManager.clear(this)
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }

        loadProfile()
        loadPhoto()
    }

    // Reads the saved picture address from Firestore (NoSQL read) and shows it
    private fun loadPhoto() {
        val userId = SessionManager.userId(this)
        FirebaseHelper.ensureSignedIn(
            onReady = {
                FirebaseFirestore.getInstance().collection("users").document(userId.toString())
                    .get()
                    .addOnSuccessListener { doc ->
                        val url = doc.getString("photoUrl")
                        if (!url.isNullOrBlank()) Glide.with(this).load(url).circleCrop().into(ivPhoto)
                    }
                    .addOnFailureListener { Log.e("HomeHub", "Could not read photo url", it) }
            },
            onError = { Log.e("HomeHub", "No Firebase session for photo", it) }
        )
    }

    // Shrinks the picture so it is small and always under the 5 MB Storage rule
    private fun compressImage(uri: Uri): ByteArray? {
        return try {
            // First pass: read only the size of the picture
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }

            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) sample *= 2

            // Second pass: decode a smaller version
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            out.toByteArray()
        } catch (e: Exception) {
            Log.e("HomeHub", "Could not compress image", e)
            null
        }
    }

    // Uploads the picture to Firebase Storage (blob storage), then saves its link in Firestore
    private fun uploadPhoto(uri: Uri) {
        val userId = SessionManager.userId(this)
        val bytes = compressImage(uri)
        if (bytes == null) {
            Toast.makeText(this, getString(R.string.msg_photo_failed), Toast.LENGTH_LONG).show()
            return
        }

        FirebaseHelper.ensureSignedIn(
            onReady = {
                val ref = FirebaseStorage.getInstance().reference.child("profile_photos/$userId.jpg")
                val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()

                ref.putBytes(bytes, metadata)
                    .continueWithTask { task ->
                        task.exception?.let { throw it }
                        ref.downloadUrl
                    }
                    .addOnSuccessListener { url ->
                        FirebaseFirestore.getInstance().collection("users").document(userId.toString())
                            .set(mapOf("photoUrl" to url.toString(), "name" to etName.text.toString()), SetOptions.merge())
                        Glide.with(this).load(url).circleCrop().into(ivPhoto)
                        Log.i("HomeHub", "Profile photo uploaded")
                        Toast.makeText(this, getString(R.string.msg_photo_ok), Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Log.e("HomeHub", "Photo upload failed", it)
                        // The real error text is shown so we can see what is wrong
                        Toast.makeText(this, getString(R.string.msg_photo_failed) + "\n" + (it.message ?: ""), Toast.LENGTH_LONG).show()
                    }
            },
            onError = {
                Toast.makeText(this, getString(R.string.msg_photo_failed) + "\n" + (it.message ?: ""), Toast.LENGTH_LONG).show()
            }
        )
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
                    Toast.makeText(this@MyHomeActivity, getString(R.string.msg_profile_load_failed), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                Log.e("HomeHub", "Profile API failed", t)
                Toast.makeText(this@MyHomeActivity, getString(R.string.msg_server_down), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun saveProfile() {
        val name = etName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        if (name.isBlank()) {
            Toast.makeText(this, getString(R.string.msg_name_empty), Toast.LENGTH_SHORT).show()
            return
        }

        val userId = SessionManager.userId(this)
        ApiClient.service.updateUser(userId, UpdateProfileRequest(name, address))
            .enqueue(object : Callback<UserProfile> {
                override fun onResponse(call: Call<UserProfile>, response: Response<UserProfile>) {
                    val profile = response.body()
                    if (response.isSuccessful && profile != null) {
                        SessionManager.updateName(this@MyHomeActivity, profile.name)
                        Toast.makeText(this@MyHomeActivity, getString(R.string.msg_profile_saved), Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(
                            this@MyHomeActivity,
                            ApiErrorParser.message(response, getString(R.string.msg_profile_save_failed)),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onFailure(call: Call<UserProfile>, t: Throwable) {
                    Log.e("HomeHub", "Update profile API failed", t)
                    Toast.makeText(this@MyHomeActivity, getString(R.string.msg_server_down), Toast.LENGTH_LONG).show()
                }
            })
    }
}