package com.example.nutriia.teleconsulta

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSLog

// En iOS, el track de video real lo maneja Swift. 
// Aquí solo necesitamos un objeto para pasarlo entre la lógica de Kotlin y la UI de Compose.
actual class VideoTrack(val nativeTrack: Any? = null)

/**
 * Bridge que la app nativa en Swift debe implementar para proveer
 * la funcionalidad real de WebRTC.
 */
interface IOSWebRtcProvider {
    fun initialize()
    fun createPeerConnection(isOffer: Boolean, isVideo: Boolean)
    fun createOffer()
    fun setRemoteOffer(sdp: String)
    fun setRemoteAnswer(sdp: String)
    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String)
    fun setMuted(muted: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun switchCamera()
    fun dispose()
}

/**
 * Singleton que contiene la referencia al proveedor nativo de iOS.
 * Swift debe inyectarse aquí al arrancar la app.
 */
object IOSCallBridge {
    var provider: IOSWebRtcProvider? = null
}

actual class WebRtcEngine actual constructor(
    private val callback: WebRtcEngineCallback
) {
    private val _engineState = MutableStateFlow(EngineState.IDLE)
    actual val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    actual fun initialize() {
        if (IOSCallBridge.provider == null) {
            NSLog("⚠️ [WebRtcEngine] IOSCallBridge.provider es nulo. Las llamadas no funcionarán hasta que se inyecte desde Swift.")
            _engineState.value = EngineState.FAILED
            callback.onError("Proveedor nativo no configurado")
            return
        }
        IOSCallBridge.provider?.initialize()
        _engineState.value = EngineState.INITIALIZED
    }

    actual fun createPeerConnection(isOffer: Boolean, tipo: TipoLlamada) {
        IOSCallBridge.provider?.createPeerConnection(isOffer, tipo == TipoLlamada.VIDEO)
        _engineState.value = if (isOffer) EngineState.CONNECTING else EngineState.INITIALIZED
    }

    actual fun createOffer() {
        IOSCallBridge.provider?.createOffer()
    }

    actual fun setRemoteOffer(sdp: String) {
        IOSCallBridge.provider?.setRemoteOffer(sdp)
    }

    actual fun setRemoteAnswer(sdp: String) {
        IOSCallBridge.provider?.setRemoteAnswer(sdp)
    }

    actual fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        IOSCallBridge.provider?.addRemoteIceCandidate(sdpMid, sdpMLineIndex, sdp)
    }

    actual fun silenciar(silenciado: Boolean) {
        IOSCallBridge.provider?.setMuted(silenciado)
    }

    actual fun apagarCamara(apagada: Boolean) {
        IOSCallBridge.provider?.setCameraEnabled(!apagada)
    }

    actual fun cambiarCamara() {
        IOSCallBridge.provider?.switchCamera()
    }

    actual fun dispose() {
        IOSCallBridge.provider?.dispose()
        _engineState.value = EngineState.IDLE
    }
}
