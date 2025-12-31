<p align="center">
  <h1 align="center">🔋 Battery Logger App</h1>
</p>

## Description

**Battery Logger App** is a utility designed for researchers and developers to collect high-fidelity battery telemetry data. It runs as a foreground service to ensure Android doesn't kill the process, making it ideal for long-term discharge cycle monitoring. The resulting data is exported in a structured CSV format, ready for integration into ML pipelines or data analysis tools.

### Key Features

- ⏱️ **Frequent Logging** - Records battery data every 10 seconds accurately
- ⚙️ **Foreground Service** - Ensures continuous background operation even if the app is closed
- 🌙 **WakeLock Integration** - Prevents CPU sleep to ensure no data points are missed
- 📊 **CSV Export** - Structured data output (timestamp, voltage, current, temperature)
- 📁 **Scoped Storage** - Saves to user-accessible `Documents` folder (Android 10+ compatible)
- 🧠 **ML Ready** - Data structure optimized for battery health prediction models

## 📥 Download APK

You can download and install the latest APK from GitHub Releases:

👉 **[Download Latest APK](https://github.com/nidulaj/Battery-logger/releases/download/v1.0.0/battery-logger.apk)**

> [!IMPORTANT]  
> Enable **“Install unknown apps”** on your Android device before installing.

## Logged Parameters

| Field | Description |
| :--- | :--- |
| `timestamp` | Date & time of the reading |
| `voltage_v` | Battery voltage (Volts) |
| `current_ma` | Battery current (mA, negative = discharging) |
| `temperature_c` | Battery temperature (°C) |

### Sample CSV Output

```csv
timestamp,voltage_v,current_ma,temperature_c
2025-01-02 10:15:00,3.87,-420.0,32.5
2025-01-02 10:15:10,3.86,-418.0,32.6
2025-01-02 10:15:20,3.85,-415.0,32.7
```
## 📂 CSV File Location

After stopping logging, the CSV file is exported to:

```bash
/Internal Storage/Documents/BatteryLogger/
```

## 🛠 Tech Stack
Language: Kotlin

IDE: Android Studio

Service Type: Foreground Service

Storage: MediaStore (Documents directory)

Target Use: Battery data collection for ML pipelines

## 🔐 Permissions Used
```bash
android.permission.FOREGROUND_SERVICE
android.permission.POST_NOTIFICATIONS
android.permission.WAKE_LOCK
```
## 🧠 Battery Health Prediction (ML)
The exported CSV files can be directly used with this machine learning repository:  Battery Health Predictor (ML Project)

👉 https://github.com/nidulaj/Battery-health-predictor

This ML project supports:

Battery degradation analysis
