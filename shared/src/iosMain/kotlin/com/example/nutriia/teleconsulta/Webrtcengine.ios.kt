package com.example.nutriia.teleconsulta

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import platform.Foundation.NSLog
import platform.UIKit.UIView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi

// En iOS, el track de video real lo maneja Swift. 
// Aquí solo necesitamos un objeto para pasarlo entre la lógica de Kotlin y la UI de Compose.
actual class VideoTrack actual constructor(val nativeTrack: Any?)

/**
 * Bridge que la app nativa en Swift debe implementar para proveer
 * la funcionalidad real de WebRTC.
 */
interface IOSWebRtcProvider {
    fun initializeEngine()
    fun createPeerConnection(isOffer: Boolean, isVideo: Boolean)
    fun createOffer()
    fun setRemoteOffer(sdp: String)
    fun setRemoteAnswer(sdp: String)
    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String)
    fun setMuted(muted: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun switchCamera()
    fun getLocalVideoView(): UIView?
    fun getRemoteVideoView(): UIView?
    fun dispose()
}

interface IOSCallEventsListener {
    fun onConnected()
    fun onDisconnected()
    fun onError(message: String)
    fun onLocalSdpReady(sdp: SessionDescription)
    fun onLocalIceCandidateReady(candidate: IceCandidate)
    fun onRemoteVideoTrackReady(track: VideoTrack)
}

/**
 * Singleton que contiene la referencia al proveedor nativo de iOS.
 * Swift debe inyectarse aquí al arrancar la app.
 */
object IOSCallBridge {
    var provider: IOSWebRtcProvider? = null
    var listener: IOSCallEventsListener? = null
}

actual class WebRtcEngine actual constructor(
    private val callback: WebRtcEngineCallback
) : IOSCallEventsListener {
    private val _engineState = MutableStateFlow(EngineState.IDLE)
    actual val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    init {
        IOSCallBridge.listener = this
    }

    actual fun initialize() {
        if (IOSCallBridge.provider == null) {
            NSLog("⚠️ [WebRtcEngine] IOSCallBridge.provider es nulo. Las llamadas no funcionarán hasta que se inyecte desde Swift.")
            _engineState.value = EngineState.FAILED
            callback.onError("Proveedor nativo no configurado")
            return
        }
        IOSCallBridge.provider?.initializeEngine()
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
        if (IOSCallBridge.listener === this) {
            IOSCallBridge.listener = null
        }
        IOSCallBridge.provider?.dispose()
        _engineState.value = EngineState.IDLE
    }

    // Bridging methods para que Swift llame de vuelta
    actual override fun onConnected() {
        _engineState.value = EngineState.CONNECTED
        callback.onConnected()
    }

    actual override fun onDisconnected() {
        _engineState.value = EngineState.DISCONNECTED
        callback.onDisconnected()
    }

    actual override fun onError(message: String) {
        _engineState.value = EngineState.FAILED
        callback.onError(message)
    }

    actual override fun onLocalSdpReady(sdp: SessionDescription) {
        callback.onLocalSdpReady(sdp)
    }

    actual override fun onLocalIceCandidateReady(candidate: IceCandidate) {
        callback.onLocalIceCandidateReady(candidate)
    }

    actual override fun onRemoteVideoTrackReady(track: VideoTrack) {
        callback.onRemoteVideoTrackReady(track)
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun WebRtcVideoView(
    videoTrack: VideoTrack?,
    modifier:   Modifier,
    isMirror:   Boolean
) {
    val remoteView = IOSCallBridge.provider?.getRemoteVideoView()
    if (remoteView != null) {
        UIKitView(
            factory = { remoteView },
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun LocalVideoSinkView(modifier: Modifier) {
    val localView = IOSCallBridge.provider?.getLocalVideoView()
    if (localView != null) {
        UIKitView(
            factory = { localView },
            modifier = modifier
        )
    }
}
