package com.example.batteryloggerapp

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.BatteryManager
import android.content.IntentFilter
import android.util.Log
import androidx.core.app.NotificationCompat
import android.os.PowerManager
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.provider.MediaStore
import android.content.ContentValues
import java.io.InputStream
import java.io.OutputStream



class BatteryLoggingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val interval = 10_000L  // 10 seconds
    private var wakeLock: PowerManager.WakeLock? = null
    private var loggingRunnable: Runnable? = null
    private var csvFile: File? = null
    private var csvWriter: FileWriter? = null
    private var isLogging = false


    override fun onBind(intent: Intent?): IBinder? {
        return null
    }


    private fun createNotification(): Notification {
        val channelId = "battery_logger_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Battery Logger",
                NotificationManager.IMPORTANCE_DEFAULT  // Changed from LOW to DEFAULT so notification is visible
            ).apply {
                description = "Shows when battery logging is active"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Battery Logger Running")
            .setContentText("Logging battery data every 10 seconds")
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)  // Makes notification non-dismissible while service is running
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        when (intent?.action) {

            "START_LOGGING" -> {
                if (!isLogging) {
                    acquireWakeLock()
                    startForeground(1, createNotification())
                    createCsvFile()
                    startLogging()
                    isLogging = true
                    Log.d("BatteryLogger", "Logging started")
                }
            }

            "STOP_LOGGING" -> {
                stopLogging()
                releaseWakeLock()
                exportCsvToDocuments()
                stopForeground(true)
                stopSelf()
                isLogging = false
                Log.d("BatteryLogger", "Logging stopped & exported")
            }
        }

        return START_STICKY
    }


    private fun acquireWakeLock() {
        if (wakeLock == null) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "BatteryLoggerApp::WakeLock"
            ).apply {
                acquire(10 * 60 * 60 * 1000L /*10 hours*/)  // Keep awake for up to 10 hours
            }
            Log.d("BatteryLogger", "Wake lock acquired")
        }
    }
    
    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d("BatteryLogger", "Wake lock released")
            }
        }
        wakeLock = null
    }


    private fun startLogging() {
        // Stop any existing logging first
        stopLogging()
        
        loggingRunnable = object : Runnable {
            override fun run() {
                logBatteryData()
                loggingRunnable?.let {
                    handler.postDelayed(it, interval)
                }
            }
        }
        handler.post(loggingRunnable!!)
        Log.d("BatteryLogger", "Logging started - will log every 10 seconds")
    }
    
    private fun stopLogging() {
        loggingRunnable?.let {
            handler.removeCallbacks(it)
            loggingRunnable = null
            Log.d("BatteryLogger", "Logging stopped")
        }
    }

    private fun logBatteryData() {
        try {
            val bm = getSystemService(BATTERY_SERVICE) as BatteryManager

            val currentRaw = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            val current = if (currentRaw != Int.MIN_VALUE) currentRaw / 1000.0 else 0.0

            val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

            val voltage = batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_VOLTAGE, 0
            )?.div(1000.0) ?: 0.0

            val temp = batteryIntent?.getIntExtra(
                BatteryManager.EXTRA_TEMPERATURE, 0
            )?.div(10.0) ?: 0.0

            val timestamp = SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date())

            // Write to CSV
            csvWriter?.append(
                "$timestamp,$voltage,$current,$temp\n"
            )
            csvWriter?.flush()

            Log.d(
                "BatteryLogger",
                "Logged → $timestamp V=$voltage I=$current T=$temp"
            )

        } catch (e: Exception) {
            Log.e("BatteryLogger", "Error logging battery data", e)
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        stopLogging()
        releaseWakeLock()

        try {
            csvWriter?.close()
            Log.d("BatteryLogger", "CSV file closed")
        } catch (e: Exception) {
            Log.e("BatteryLogger", "Error closing CSV", e)
        }

        Log.d("BatteryLogger", "Service destroyed")
    }


    private fun createCsvFile() {
        if (csvFile != null) return   // 🔴 prevents overwrite

        val timeStamp = SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            Locale.getDefault()
        ).format(Date())

        val fileName = "battery_log_$timeStamp.csv"
        csvFile = File(getExternalFilesDir(null), fileName)
        csvWriter = FileWriter(csvFile!!, true)

        csvWriter?.append("timestamp,voltage_v,current_ma,temperature_c\n")
        csvWriter?.flush()

        Log.d("BatteryLogger", "CSV created at: ${csvFile!!.absolutePath}")
    }


    fun exportCsvToDocuments() {
        val sourceFile = csvFile ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Documents/BatteryLogger")
        }

        val resolver = applicationContext.contentResolver
        val uri = resolver.insert(
            MediaStore.Files.getContentUri("external"),
            contentValues
        )

        uri?.let {
            resolver.openOutputStream(it)?.use { output ->
                sourceFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
    }



}