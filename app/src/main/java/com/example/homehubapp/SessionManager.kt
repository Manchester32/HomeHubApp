package com.example.homehubapp

import android.content.Context

object SessionManager {
    private const val PREFS = "homehub_session"
    private const val USER_ID = "user_id"
    private const val NAME = "name"
    private const val EMAIL = "email"

    fun saveLogin(context: Context, userId: Int, name: String, email: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(USER_ID, userId)
            .putString(NAME, name)
            .putString(EMAIL, email)
            .apply()
    }

    fun userId(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(USER_ID, -1)

    fun name(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(NAME, "User") ?: "User"

    fun email(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(EMAIL, "") ?: ""

    fun updateName(context: Context, name: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(NAME, name).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
