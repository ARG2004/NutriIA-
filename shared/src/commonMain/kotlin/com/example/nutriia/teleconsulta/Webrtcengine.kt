package com.example.nutriia.teleconsulta

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EngineState {
    IDLE,
    INITIALIZING,
    INITIALIZED,
    CONNECTING,
    CONNECTED,
    DISCONNECTED,
    FAILED
}

class SessionDescription(
    val type: Type,
    val description: String
) {
    enum class Type { OFFER, ANSWER, PRANSWER }
}

class IceCandidate(
    val sdpMid: String,
    val sdpMLineIndex: Int,
    val sdp: String
)

class VideoTrack

interface WebRtcEngineCallback {
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
    fun onLocalSdpReady(sdp: SessionDescription) {}
    fun onLocalIceCandidateReady(candidate: IceCandidate) {}
    fun onRemoteVideoTrackReady(track: VideoTrack) {}
}

class WebRtcEngine(
    private val callback: WebRtcEngineCallback
) {
    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    fun initialize() {
        _engineState.value = EngineState.INITIALIZED
    }

    fun createPeerConnection(isOffer: Boolean, tipo: TipoLlamada) {}
    fun createOffer() {}
    fun setRemoteOffer(sdp: String) {}
    fun setRemoteAnswer(sdp: String) {}
    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {}
    fun silenciar(silenciado: Boolean) {}
    fun apagarCamara(apagada: Boolean) {}
    fun cambiarCamara() {}
    fun switchCamera() {}
    fun toggleMute(muted: Boolean) {}
    fun toggleVideo(enabled: Boolean) {}
    fun toggleSpeakerphone(speaker: Boolean) {}

    fun dispose() {
        _engineState.value = EngineState.IDLE
    }
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