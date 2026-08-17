@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.offline

import platform.Network.*
import platform.darwin.dispatch_get_main_queue

actual object PlatformNetworkMonitor {
    private var monitor: nw_path_monitor_t = null

    actual fun startMonitoring() {
        if (monitor != null) return
        val m = nw_path_monitor_create()
        monitor = m
        nw_path_monitor_set_update_handler(m) { path ->
            val status = nw_path_get_status(path)
            val isOnline = status == nw_path_status_satisfied
            OfflineManager.setOnline(isOnline)
        }
        nw_path_monitor_set_queue(m, dispatch_get_main_queue())
        nw_path_monitor_start(m)
    }
}
