package com.example.batteryloggerapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var startBtn: Button
    private lateinit var stopBtn: Button
    private lateinit var exportLocationText: TextView

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI
        statusText = findViewById(R.id.statusText)
        startBtn = findViewById(R.id.startBtn)
        stopBtn = findViewById(R.id.stopBtn)
        exportLocationText = findViewById(R.id.exportLocationText)

        // Start logging
        startBtn.setOnClickListener {
            requestNotificationPermissionIfNeeded()
        }

        // Stop logging
        stopBtn.setOnClickListener {
            val intent = Intent(this, BatteryLoggingService::class.java)
            intent.action = "STOP_LOGGING"
            startService(intent)

            statusText.text = "Status: Stopped & Exported"
            // Keep the export location visible after stopping
        }

    }

    // Request notification permission for Android 13+
    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST
                )
            } else {
                startBatteryService()
            }
        } else {
            startBatteryService()
        }
    }

    // Start foreground service
    private fun startBatteryService() {
        val intent = Intent(this, BatteryLoggingService::class.java)
        intent.action = "START_LOGGING"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }

        statusText.text = "Status: Logging..."

        // Show export location message when logging starts
        exportLocationText.visibility = View.VISIBLE
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start service
                startBatteryService()
            } else {
                // Permission denied
                statusText.text = "Status: Notification permission denied!"
                // Hide export location if permission denied
                exportLocationText.visibility = View.GONE
            }
        }
    }
}