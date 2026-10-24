package com.example.mailsound

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var emailInput: EditText
    private lateinit var passInput: EditText
    private lateinit var statusView: TextView

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "status") runOnUiThread { refreshStatus() }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences(MailMonitorService.PREFS, Context.MODE_PRIVATE)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(56), dp(20), dp(32))
        }

        root.addView(TextView(this).apply {
            text = "Mail Sound Alert"
            textSize = 24f
        })
        root.addView(TextView(this).apply {
            text = "Plays your alarm sound when new Gmail arrives. Use a Google App Password, not your normal password."
            setPadding(0, dp(8), 0, dp(16))
        })

        emailInput = EditText(this).apply {
            hint = "Gmail address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(prefs.getString("email", ""))
        }
        passInput = EditText(this).apply {
            hint = "Google App Password"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("password", ""))
        }
        root.addView(emailInput)
        root.addView(passInput)

        root.addView(button("Start email sound monitoring") { start() })
        root.addView(button("Stop monitoring") {
            stopService(Intent(this, MailMonitorService::class.java))
        })
        root.addView(button("Test alarm sound") { AlarmPlayer.play(this, 4) })
        root.addView(button("Battery optimization settings") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })

        statusView = TextView(this).apply { setPadding(0, dp(16), 0, 0) }
        root.addView(statusView)

        setContentView(ScrollView(this).apply { addView(root) })
        refreshStatus()
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun start() {
        val email = emailInput.text.toString().trim()
        val pass = passInput.text.toString().replace(" ", "")
        if (email.isEmpty() || pass.isEmpty()) {
            statusView.text = "Status: enter email and app password first"
            return
        }
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
        getSharedPreferences(MailMonitorService.PREFS, Context.MODE_PRIVATE).edit()
            .putString("email", email)
            .putString("password", pass)
            .putString("status", "Starting...")
            .apply()
        startForegroundService(Intent(this, MailMonitorService::class.java))
    }

    private fun refreshStatus() {
        val s = getSharedPreferences(MailMonitorService.PREFS, Context.MODE_PRIVATE)
            .getString("status", "Not running")
        statusView.text = "Status: $s"
    }

    override fun onResume() {
        super.onResume()
        getSharedPreferences(MailMonitorService.PREFS, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        refreshStatus()
    }

    override fun onPause() {
        getSharedPreferences(MailMonitorService.PREFS, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
        super.onPause()
    }
}
