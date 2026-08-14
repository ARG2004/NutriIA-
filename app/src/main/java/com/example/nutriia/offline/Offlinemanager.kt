package com.example.nutriia.offline

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Singleton que mantiene el estado de conectividad observable desde cualquier
 * ViewModel o Composable.
 *
 * Inicializa en Application.onCreate:
 *   OfflineManager.init(this)
 *
 * Consume en un ViewModel:
 *   val isOnline = OfflineManager.isOnline          // StateFlow<Boolean>
 *   val online   = OfflineManager.hayConexion()     // check síncrono
 */
object OfflineManager {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private lateinit var monitor: NetworkMonitor

    fun init(context: Context) {
        monitor = NetworkMonitor(context.applicationContext)
        scope.launch {
            monitor.isOnline.collect { online ->
                _isOnline.value = online
            }
        }
    }

    /** Check síncrono — útil en repositorios y casos de uso. */
    fun hayConexion(): Boolean =
        if (::monitor.isInitialized) monitor.hayConexionActiva() else _isOnline.value
}