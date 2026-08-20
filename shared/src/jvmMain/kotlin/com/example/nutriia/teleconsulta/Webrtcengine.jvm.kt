package com.example.nutriia.teleconsulta

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

actual class VideoTrack

actual class WebRtcEngine actual constructor(
    private val callback: WebRtcEngineCallback
) {
    private val _engineState = MutableStateFlow(EngineState.IDLE)
    actual val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    actual fun initialize() {
        _engineState.value = EngineState.INITIALIZED
    }

    actual fun createPeerConnection(isOffer: Boolean, tipo: TipoLlamada) {}
    actual fun createOffer() {}
    actual fun setRemoteOffer(sdp: String) {}
    actual fun setRemoteAnswer(sdp: String) {}
    actual fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {}
    actual fun silenciar(silenciado: Boolean) {}
    actual fun apagarCamara(apagada: Boolean) {}
    actual fun cambiarCamara() {}
    actual fun dispose() {
        _engineState.value = EngineState.IDLE
    }
}
