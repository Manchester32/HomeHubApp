package com.example.homehubapp

import android.content.Context

object SettingsStore {
    private const val PREFS = "homehub_settings"
    private const val NOTIFICATIONS = "notifications"
    private const val DEFAULT_PAYMENT = "default_payment"

    fun notificationsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(NOTIFICATIONS, true)

    fun setNotificationsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(NOTIFICATIONS, enabled).apply()
    }

    fun defaultPayment(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(DEFAULT_PAYMENT, "Cash") ?: "Cash"

    fun setDefaultPayment(context: Context, method: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(DEFAULT_PAYMENT, method).apply()
    }
}
