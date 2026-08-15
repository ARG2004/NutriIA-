package com.example.nutriia.teleconsulta

import android.content.Context

// ══════════════════════════════════════════════════════
// CALL ENGINE PROVIDER
// Singleton que expone el WebRtcEngine inicializado.
// Reemplaza la versión anterior que dependía de Agora.
// ══════════════════════════════════════════════════════

object CallEngineProvider {

    @Volatile
    private var _engine: WebRtcEngine? = null

    /** Motor activo. Lanza excepción si no fue inicializado. */
    val engine: WebRtcEngine
        get() = _engine ?: error("CallEngineProvider no inicializado. Llama init() primero.")

    /** ¿Existe ya un engine inicializado? */
    val isInitialized: Boolean get() = _engine != null

    /**
     * Inicializa el engine WebRTC. Llamar una sola vez (por ejemplo desde
     * el ViewModel antes de crear/responder la llamada).
     *
     * @param context  Contexto de aplicación (no de Activity).
     * @param callback Callbacks del engine (generalmente el ViewModel).
     */
    fun init(context: Context, callback: WebRtcEngineCallback): WebRtcEngine {
        // Limpiar engine previo si existía
        _engine?.dispose()

        return WebRtcEngine(context.applicationContext, callback).also {
            it.initialize()
            _engine = it
        }
    }

    /**
     * Libera el engine y lo marca como nulo.
     * Llamar al finalizar una llamada.
     */
    fun release() {
        _engine?.dispose()
        _engine = null
    }
}