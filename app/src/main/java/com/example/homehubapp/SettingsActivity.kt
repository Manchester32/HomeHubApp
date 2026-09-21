package com.example.homehubapp

import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.appcompat.widget.SwitchCompat
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val switchNotifications = findViewById<SwitchCompat>(R.id.switchNotifications)
        val paymentGroup = findViewById<RadioGroup>(R.id.radioGroupDefaultPayment)
        val btnSave = findViewById<Button>(R.id.btnSaveSettings)

        switchNotifications.isChecked = SettingsStore.notificationsEnabled(this)
        if (SettingsStore.defaultPayment(this) == "Card") {
            findViewById<RadioButton>(R.id.radioDefaultCard).isChecked = true
        } else {
            findViewById<RadioButton>(R.id.radioDefaultCash).isChecked = true
        }

        btnSave.setOnClickListener {
            val payment = when (paymentGroup.checkedRadioButtonId) {
                R.id.radioDefaultCard -> "Card"
                else -> "Cash"
            }
            SettingsStore.setNotificationsEnabled(this, switchNotifications.isChecked)
            SettingsStore.setDefaultPayment(this, payment)
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
