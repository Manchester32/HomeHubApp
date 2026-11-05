package com.example.homehubapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : AppCompatActivity() {

    // Android 13+ asks the user for permission before notifications can be shown
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            Log.d("HomeHub", "Notification permission granted: $granted")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (SessionManager.userId(this) == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.tvWelcome).text = getString(R.string.welcome_back, SessionManager.name(this))

        findViewById<Button>(R.id.btnServices).setOnClickListener {
            startActivity(Intent(this, ServicesActivity::class.java))
        }
        findViewById<Button>(R.id.btnBookService).setOnClickListener {
            startActivity(Intent(this, ServicesActivity::class.java))
        }
        findViewById<Button>(R.id.btnMyRequests).setOnClickListener {
            startActivity(Intent(this, MyRequestsActivity::class.java))
        }
        findViewById<Button>(R.id.btnMyHome).setOnClickListener {
            startActivity(Intent(this, MyHomeActivity::class.java))
        }
        findViewById<Button>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<Button>(R.id.btnReviews).setOnClickListener {
            startActivity(Intent(this, ReviewsActivity::class.java))
        }

        setupFirebase()
    }

    override fun onResume() {
        super.onResume()
        // Try to send any requests that were saved while offline
        SyncManager.syncPending(this)
    }

    private fun setupFirebase() {
        // Email/password users also get an (anonymous) Firebase session so Firestore/Storage rules accept them
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener { Log.i("HomeHub", "Firebase anonymous session started") }
                .addOnFailureListener { Log.e("HomeHub", "Firebase anonymous sign-in failed", it) }
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Push notifications: everyone who enabled notifications listens to this topic
        if (SettingsStore.notificationsEnabled(this)) {
            FirebaseMessaging.getInstance().subscribeToTopic("homehub_updates")
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) Log.d("HomeHub", "FCM token: ${task.result}")
        }
    }
}