package com.example.nutriia.offline

actual object PlatformNetworkMonitor {
    actual fun startMonitoring() {
        OfflineManager.setOnline(true)
    }
}
