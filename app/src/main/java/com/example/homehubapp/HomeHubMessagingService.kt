package com.example.homehubapp

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class HomeHubMessagingService : FirebaseMessagingService() {

    // Called when Firebase gives this device a new push token
    override fun onNewToken(token: String) {
        Log.d("HomeHub", "New FCM token: $token")
        val userId = SessionManager.userId(this)
        if (userId != -1) {
            // Store the token in Firestore so the user's device can be targeted later
            FirebaseFirestore.getInstance().collection("users").document(userId.toString())
                .set(mapOf("fcmToken" to token), SetOptions.merge())
        }
    }

    // Called when a push arrives while the app is open
    override fun onMessageReceived(message: RemoteMessage) {
        Log.d("HomeHub", "Push received: ${message.notification?.title}")
        if (!SettingsStore.notificationsEnabled(this)) return

        val title = message.notification?.title ?: message.data["title"] ?: getString(R.string.app_name)
        val body = message.notification?.body ?: message.data["body"] ?: ""
        NotificationHelper.show(this, title, body)
    }
}