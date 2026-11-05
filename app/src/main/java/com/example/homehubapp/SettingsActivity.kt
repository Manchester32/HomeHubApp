package com.example.homehubapp

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.SwitchCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.firebase.messaging.FirebaseMessaging

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchNotifications = findViewById<SwitchCompat>(R.id.switchNotifications)
        val paymentGroup = findViewById<RadioGroup>(R.id.radioGroupDefaultPayment)
        val languageGroup = findViewById<RadioGroup>(R.id.radioGroupLanguage)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        switchNotifications.isChecked = SettingsStore.notificationsEnabled(this)
        if (SettingsStore.defaultPayment(this) == "Card") {
            findViewById<RadioButton>(R.id.radioDefaultCard).isChecked = true
        } else {
            findViewById<RadioButton>(R.id.radioDefaultCash).isChecked = true
        }

        when (SettingsStore.language(this)) {
            "zu" -> findViewById<RadioButton>(R.id.radioLangZu).isChecked = true
            "xh" -> findViewById<RadioButton>(R.id.radioLangXh).isChecked = true
            else -> findViewById<RadioButton>(R.id.radioLangEn).isChecked = true
        }

        btnSave.setOnClickListener {
            val payment = when (paymentGroup.checkedRadioButtonId) {
                R.id.radioDefaultCard -> "Card"
                else -> "Cash"
            }
            val language = when (languageGroup.checkedRadioButtonId) {
                R.id.radioLangZu -> "zu"
                R.id.radioLangXh -> "xh"
                else -> "en"
            }

            SettingsStore.setNotificationsEnabled(this, switchNotifications.isChecked)
            SettingsStore.setDefaultPayment(this, payment)
            SettingsStore.setLanguage(this, language)

            // Turn push notifications on/off by (un)subscribing from the Firebase topic
            if (switchNotifications.isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("homehub_updates")
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("homehub_updates")
            }

            Log.i("HomeHub", "Settings saved: language=$language, payment=$payment")
            Toast.makeText(this, getString(R.string.msg_settings_saved), Toast.LENGTH_SHORT).show()

            // Switches the whole app language immediately
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
            finish()
        }
    }
}