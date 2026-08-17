package com.example.nutriia.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

expect object PlatformNetworkMonitor {
    fun startMonitoring()
}

object OfflineManager {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    init {
        PlatformNetworkMonitor.startMonitoring()
    }

    fun setOnline(online: Boolean) {
        _isOnline.value = online
    }

    fun hayConexion(): Boolean = _isOnline.value
}