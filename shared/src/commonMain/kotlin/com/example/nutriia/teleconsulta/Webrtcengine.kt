package com.example.nutriia.teleconsulta

import kotlinx.coroutines.flow.StateFlow

enum class EngineState {
    IDLE, INITIALIZING, INITIALIZED, CONNECTING, CONNECTED, DISCONNECTED, FAILED
}

class SessionDescription(
    val type: SdpType,
    val sdpDescription: String
) {
    companion object {
        fun ofOffer(sdp: String) = SessionDescription(SdpType.OFFER, sdp)
        fun ofAnswer(sdp: String) = SessionDescription(SdpType.ANSWER, sdp)
    }
}

enum class SdpType { OFFER, ANSWER, PRANSWER }

class IceCandidate(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String
) {
    companion object {
        fun create(sdpMid: String, sdpMLineIndex: Int, sdp: String) = IceCandidate(sdpMid, sdpMLineIndex, sdp)
    }
}

// Representa un track de video que la UI puede renderizar
expect class VideoTrack(nativeTrack: Any? = null)

interface WebRtcEngineCallback {
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
    fun onLocalSdpReady(sdp: SessionDescription) {}
    fun onLocalIceCandidateReady(candidate: IceCandidate) {}
    fun onRemoteVideoTrackReady(track: VideoTrack) {}
}

expect class WebRtcEngine(callback: WebRtcEngineCallback) {
    val engineState: StateFlow<EngineState>
    fun initialize()
    fun createPeerConnection(isOffer: Boolean, tipo: TipoLlamada)
    fun createOffer()
    fun setRemoteOffer(sdp: String)
    fun setRemoteAnswer(sdp: String)
    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String)
    fun silenciar(silenciado: Boolean)
    fun apagarCamara(apagada: Boolean)
    fun cambiarCamara()
    fun dispose()

    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
    fun onLocalSdpReady(sdp: SessionDescription)
    fun onLocalIceCandidateReady(candidate: IceCandidate)
    fun onRemoteVideoTrackReady(track: VideoTrack)
}

object CallEngineProvider {
    private var _engine: WebRtcEngine? = null
    val isInitialized: Boolean get() = _engine != null

    fun getEngine(): WebRtcEngine? = _engine

    fun init(callback: WebRtcEngineCallback): WebRtcEngine {
        _engine?.dispose()
        return WebRtcEngine(callback).also {
            it.initialize()
            _engine = it
        }
    }

    fun release() {
        _engine?.dispose()
        _engine = null
    }
}
