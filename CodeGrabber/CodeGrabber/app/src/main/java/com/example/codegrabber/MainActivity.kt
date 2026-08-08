package com.example.codegrabber

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val statusText = findViewById<TextView>(R.id.statusText)
        val grantButton = findViewById<Button>(R.id.grantButton)

        val enabled = isNotificationServiceEnabled()
        statusText.text = if (enabled) {
            "Notification access: GRANTED\nListening for codes in the background."
        } else {
            "Notification access: NOT granted.\nTap the button below, find 'Code Grabber' in the list, and turn it on."
        }

        grantButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }
}
