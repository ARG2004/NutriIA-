package com.example.nutriia.teleconsulta

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSError
import cocoapods.GoogleWebRTC.*

// ══════════════════════════════════════════════════════════════════════════
// WEBRTC ENGINE — iOS (Kotlin/Native + cinterop sobre GoogleWebRTC pod)
// Espejo funcional de teleconsulta/Webrtcengine.kt de Android (org.webrtc).
// Requiere: plugin kotlin("native.cocoapods") + pod("GoogleWebRTC") en
// build.gradle.kts (ver build.gradle.kts entregado junto con este archivo),
// y correr `pod install` en el Xcode project antes de compilar.
//
// IMPORTANTE: los nombres exactos de métodos generados por cinterop pueden
// variar un poco según la versión del pod (p.ej. offerForConstraints vs
// createOfferWithConstraints). Verifica con autocompletado de Xcode/IDE
// después de correr `pod install` y ajusta si hace falta — esto es un
// scaffold funcional, no algo que pude compilar sin tu toolchain de Xcode.
// ══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalForeignApi::class)
enum class EngineState {
    IDLE, INITIALIZING, INITIALIZED, CONNECTING, CONNECTED, DISCONNECTED, FAILED
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

@OptIn(ExperimentalForeignApi::class)
class WebRtcEngine(
    private val callback: WebRtcEngineCallback
) {
    companion object {
        // Mismos servidores STUN que usa Android — mantener paridad para que
        // ambos lados negocien candidatos compatibles.
        private val ICE_SERVER_URLS = listOf(
            "stun:stun.l.google.com:19302",
            "stun:stun1.l.google.com:19302",
            "stun:stun2.l.google.com:19302",
            "stun:stun3.l.google.com:19302",
            "stun:stun4.l.google.com:19302"
        )
    }

    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    private var factory: RTCPeerConnectionFactory? = null
    private var peerConnection: RTCPeerConnection? = null
    private var localAudioTrack: RTCAudioTrack? = null
    private var localVideoTrack: RTCVideoTrack? = null
    private var videoCapturer: RTCCameraVideoCapturer? = null

    // ─── Inicialización ───────────────────────────────────────────────────

    fun initialize() {
        if (_engineState.value != EngineState.IDLE) return

        RTCInitializeSSL()
        val encoderFactory = RTCDefaultVideoEncoderFactory()
        val decoderFactory = RTCDefaultVideoDecoderFactory()
        factory = RTCPeerConnectionFactory(
            encoderFactory = encoderFactory,
            decoderFactory = decoderFactory
        )

        _engineState.value = EngineState.INITIALIZED
    }

    // ─── Crear PeerConnection ─────────────────────────────────────────────

    fun createPeerConnection(isOffer: Boolean, tipo: TipoLlamada) {
        val f = factory ?: run {
            callback.onError("Factory no inicializada")
            return
        }

        val iceServers = ICE_SERVER_URLS.map { url ->
            RTCIceServer(uRLStrings = listOf(url))
        }

        val config = RTCConfiguration().apply {
            this.iceServers = iceServers
            bundlePolicy = RTCBundlePolicy.RTCBundlePolicyMaxBundle
            rtcpMuxPolicy = RTCRtcpMuxPolicy.RTCRtcpMuxPolicyRequire
            sdpSemantics = RTCSdpSemantics.RTCSdpSemanticsUnifiedPlan
            continualGatheringPolicy = RTCContinualGatheringPolicy.RTCContinualGatheringPolicyGatherContinually
        }

        val constraints = RTCMediaConstraints(
            mandatoryConstraints = null,
            optionalConstraints = null
        )

        peerConnection = f.peerConnectionWithConfiguration(
            configuration = config,
            constraints = constraints,
            delegate = createPeerConnectionDelegate()
        ) ?: run {
            callback.onError("No se pudo crear PeerConnection")
            return
        }

        addLocalTracks(f, tipo)

        _engineState.value = if (isOffer) EngineState.CONNECTING else EngineState.INITIALIZED
    }

    // ─── Tracks de audio y video local ────────────────────────────────────

    private fun addLocalTracks(f: RTCPeerConnectionFactory, tipo: TipoLlamada) {
        // Audio siempre
        val audioConstraints = RTCMediaConstraints(
            mandatoryConstraints = mapOf(
                "echoCancellation" to "true",
                "noiseSuppression" to "true",
                "autoGainControl" to "true"
            ),
            optionalConstraints = null
        )
        val audioSource = f.audioSourceWithConstraints(audioConstraints)
        localAudioTrack = f.audioTrackWithSource(audioSource, trackId = "audio0")
        localAudioTrack?.let { peerConnection?.addTrack(it, streamIds = listOf("stream0")) }

        // Video solo si es videollamada
        if (tipo == TipoLlamada.VIDEO) {
            val videoSource = f.videoSource()
            videoCapturer = RTCCameraVideoCapturer(delegate = videoSource)
            iniciarCapturaCamara(videoCapturer!!)

            localVideoTrack = f.videoTrackWithSource(videoSource, trackId = "video0")
            localVideoTrack?.let { peerConnection?.addTrack(it, streamIds = listOf("stream0")) }
        }
    }

    private fun iniciarCapturaCamara(capturer: RTCCameraVideoCapturer) {
        // Busca la cámara frontal y su mejor formato disponible ~640x480@30fps.
        val devices = RTCCameraVideoCapturer.captureDevices()
        val frontCamera = devices.firstOrNull { device ->
            (device as platform.AVFoundation.AVCaptureDevice).position == platform.AVFoundation.AVCaptureDevicePositionFront
        } as? platform.AVFoundation.AVCaptureDevice ?: devices.firstOrNull() as? platform.AVFoundation.AVCaptureDevice
        val device = frontCamera ?: run {
            callback.onError("No se encontró cámara disponible")
            return
        }

        val formats = RTCCameraVideoCapturer.supportedFormatsForDevice(device)
        val format = formats.firstOrNull() as? platform.AVFoundation.AVCaptureDeviceFormat ?: run {
            callback.onError("No se encontró formato de cámara soportado")
            return
        }

        capturer.startCaptureWithDevice(device, format = format, fps = 30)
    }

    // ─── Offer / Answer ────────────────────────────────────────────────────

    fun createOffer() {
        val pc = peerConnection ?: return
        val constraints = RTCMediaConstraints(
            mandatoryConstraints = mapOf(
                "OfferToReceiveAudio" to "true",
                "OfferToReceiveVideo" to "true"
            ),
            optionalConstraints = null
        )
        pc.offerForConstraints(constraints) { sdp, error ->
            if (error != null || sdp == null) {
                callback.onError("CreateOffer falló: ${error?.localizedDescription}")
                return@offerForConstraints
            }
            pc.setLocalDescription(sdp) { setError ->
                if (setError != null) {
                    callback.onError("SetLocalDescription (offer) falló: ${setError.localizedDescription}")
                } else {
                    callback.onLocalSdpReady(SessionDescription(SessionDescription.Type.OFFER, sdp.sdp))
                }
            }
        }
    }

    private fun createAnswer() {
        val pc = peerConnection ?: return
        val constraints = RTCMediaConstraints(
            mandatoryConstraints = mapOf(
                "OfferToReceiveAudio" to "true",
                "OfferToReceiveVideo" to "true"
            ),
            optionalConstraints = null
        )
        pc.answerForConstraints(constraints) { sdp, error ->
            if (error != null || sdp == null) {
                callback.onError("CreateAnswer falló: ${error?.localizedDescription}")
                return@answerForConstraints
            }
            pc.setLocalDescription(sdp) { setError ->
                if (setError != null) {
                    callback.onError("SetLocalDescription (answer) falló: ${setError.localizedDescription}")
                } else {
                    callback.onLocalSdpReady(SessionDescription(SessionDescription.Type.ANSWER, sdp.sdp))
                }
            }
        }
    }

    /** Padre llama esto al recibir el OFFER del nutriólogo */
    fun setRemoteOffer(sdp: String) {
        val remoteSdp = RTCSessionDescription(type = RTCSdpType.RTCSdpTypeOffer, sdp = sdp)
        peerConnection?.setRemoteDescription(remoteSdp) { error ->
            if (error != null) {
                callback.onError("SetRemoteDescription (offer) falló: ${error.localizedDescription}")
            } else {
                createAnswer()
            }
        }
    }

    /** Nutriólogo llama esto cuando recibe el ANSWER del padre */
    fun setRemoteAnswer(sdp: String) {
        val remoteSdp = RTCSessionDescription(type = RTCSdpType.RTCSdpTypeAnswer, sdp = sdp)
        peerConnection?.setRemoteDescription(remoteSdp) { error ->
            if (error != null) {
                callback.onError("SetRemoteDescription (answer) falló: ${error.localizedDescription}")
            }
        }
    }

    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = RTCIceCandidate(sdp = sdp, sdpMLineIndex = sdpMLineIndex, sdpMid = sdpMid)
        peerConnection?.addIceCandidate(candidate)
    }

    // ─── Controles en llamada ──────────────────────────────────────────────

    fun silenciar(silenciado: Boolean) {
        localAudioTrack?.isEnabled = !silenciado
    }

    fun apagarCamara(apagada: Boolean) {
        localVideoTrack?.isEnabled = !apagada
    }

    fun cambiarCamara() {
        val capturer = videoCapturer ?: return
        val devices = RTCCameraVideoCapturer.captureDevices()
        // Alterna simple entre las primeras dos cámaras encontradas (frontal/trasera)
        if (devices.size < 2) return
        val nuevaCamara = devices[1] as platform.AVFoundation.AVCaptureDevice
        val formats = RTCCameraVideoCapturer.supportedFormatsForDevice(nuevaCamara)
        val format = formats.firstOrNull() as? platform.AVFoundation.AVCaptureDeviceFormat ?: return
        capturer.startCaptureWithDevice(nuevaCamara, format = format, fps = 30)
    }

    fun switchCamera() = cambiarCamara()
    fun toggleMute(muted: Boolean) = silenciar(muted)
    fun toggleVideo(enabled: Boolean) = apagarCamara(!enabled)
    fun toggleSpeakerphone(speaker: Boolean) {
        // El AudioSession de iOS se maneja aparte (RTCAudioSession / AVAudioSession)
        // desde la capa nativa/Swift si necesitas forzar altavoz vs auricular.
    }

    // ─── Limpieza ──────────────────────────────────────────────────────────

    fun dispose() {
        videoCapturer?.stopCapture()
        videoCapturer = null
        localAudioTrack = null
        localVideoTrack = null
        peerConnection?.close()
        peerConnection = null
        factory = null
        _engineState.value = EngineState.IDLE
    }

    // ─── Delegate de PeerConnection ──────────────────────────────────────

    private fun createPeerConnectionDelegate(): RTCPeerConnectionDelegateProtocol =
        object : platform.Foundation.NSObject(), RTCPeerConnectionDelegateProtocol {

            override fun peerConnection(
                peerConnection: RTCPeerConnection,
                didGenerateIceCandidate: RTCIceCandidate
            ) {
                callback.onLocalIceCandidateReady(
                    IceCandidate(
                        sdpMid = didGenerateIceCandidate.sdpMid ?: "",
                        sdpMLineIndex = didGenerateIceCandidate.sdpMLineIndex,
                        sdp = didGenerateIceCandidate.sdp
                    )
                )
            }

            override fun peerConnection(
                peerConnection: RTCPeerConnection,
                didChangeIceConnectionState: RTCIceConnectionState
            ) {
                when (didChangeIceConnectionState) {
                    RTCIceConnectionState.RTCIceConnectionStateConnected,
                    RTCIceConnectionState.RTCIceConnectionStateCompleted -> {
                        _engineState.value = EngineState.CONNECTED
                        callback.onConnected()
                    }

                    RTCIceConnectionState.RTCIceConnectionStateDisconnected,
                    RTCIceConnectionState.RTCIceConnectionStateFailed,
                    RTCIceConnectionState.RTCIceConnectionStateClosed -> {
                        _engineState.value = EngineState.DISCONNECTED
                        callback.onDisconnected()
                    }

                    else -> {}
                }
            }

            override fun peerConnection(
                peerConnection: RTCPeerConnection,
                didAddReceiver: RTCRtpReceiver,
                streams: List<*>
            ) {
                val track = didAddReceiver.track()
                if (track is RTCVideoTrack) {
                    callback.onRemoteVideoTrackReady(VideoTrack())
                }
            }

            // Callbacks obligatorios del protocolo sin lógica adicional:
            override fun peerConnection(
                peerConnection: RTCPeerConnection,
                didChangeSignalingState: RTCSignalingState
            ) {
            }

            override fun peerConnection(peerConnection: RTCPeerConnection, didAddStream: RTCMediaStream) {}
            override fun peerConnection(peerConnection: RTCPeerConnection, didRemoveStream: RTCMediaStream) {}
            override fun peerConnectionShouldNegotiate(peerConnection: RTCPeerConnection) {}
            override fun peerConnection(
                peerConnection: RTCPeerConnection,
                didChangeIceGatheringState: RTCIceGatheringState
            ) {
            }

            override fun peerConnection(peerConnection: RTCPeerConnection, didRemoveIceCandidates: List<*>) {}
            override fun peerConnection(peerConnection: RTCPeerConnection, didOpenDataChannel: RTCDataChannel) {}
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