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

        val enabled = isAccessibilityServiceEnabled()
        statusText.text = if (enabled) {
            "Accessibility access: GRANTED\nListening for codes in the background."
        } else {
            "Accessibility access: NOT granted.\nTap the button below, find 'Code Grabber' under Installed Apps (or Downloaded Apps), and turn it on."
        }

        grantButton.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = "$packageName/${CodeAccessibilityService::class.java.name}"
        val flat = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        return flat != null && flat.contains(expectedComponent)
    }
}
