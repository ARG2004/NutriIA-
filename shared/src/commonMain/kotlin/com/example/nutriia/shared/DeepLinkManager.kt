package com.example.nutriia.shared

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Gestor global de Deep Links para NutrIA Multiplataforma.
 * Permite que MainActivity (Android) y iOSApp (Swift) notifiquen la llegada
 * de una URL externa para que la UI de Compose reaccione.
 */
object DeepLinkManager {
    private val _links = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val links = _links.asSharedFlow()

    fun onLinkReceived(url: String) {
        _links.tryEmit(url)
    }
}
