package com.example.nutriia.teleconsulta

import android.content.Context
import android.util.Log
import org.webrtc.*
import org.webrtc.audio.JavaAudioDeviceModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ══════════════════════════════════════════════════════
// WEBRTC ENGINE — Motor de llamadas nativo sin Agora
// Usa la librería oficial de Google WebRTC para Android:
//   implementation("io.github.webrtc-sdk:android:104.5112.09")
// ══════════════════════════════════════════════════════

// ─── Callbacks del motor ──────────────────────────────────────────────────────

interface WebRtcEngineCallback {
    /** SDP Offer/Answer local listo para enviar al remoto vía Firestore */
    fun onLocalSdpReady(sdp: SessionDescription)
    /** ICE Candidate local listo para enviar al remoto vía Firestore */
    fun onLocalIceCandidateReady(candidate: IceCandidate)
    /** La conexión peer-to-peer está establecida */
    fun onConnected()
    /** La conexión se cayó o fue cerrada */
    fun onDisconnected()
    /** Error fatal */
    fun onError(message: String)
    /** Se recibió un stream de video remoto */
    fun onRemoteVideoTrackReady(track: VideoTrack)
}

// ─── Motor principal ──────────────────────────────────────────────────────────

class WebRtcEngine(
    private val context:  Context,
    private val callback: WebRtcEngineCallback
) {
    companion object {
        private const val TAG = "WebRtcEngine"

        // Singleton para el contexto OpenGL compartido
        private val sharedEglBase by lazy { EglBase.create() }
        val eglContext: EglBase.Context get() = sharedEglBase.eglBaseContext

        private var isFactoryInitialized = false

        // Servidores STUN/TURN públicos — para producción añade TURN propios
        private val ICE_SERVERS = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun4.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer()
        )
    }

    // Estado del motor
    private val _engineState = MutableStateFlow(EngineState.IDLE)
    val engineState: StateFlow<EngineState> = _engineState.asStateFlow()

    // WebRTC internals
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection:        PeerConnection?         = null
    private var localAudioSource:      AudioSource?            = null
    private var localVideoSource:      VideoSource?            = null
    private var localAudioTrack:       AudioTrack?             = null
    private var localVideoTrack:       VideoTrack?             = null
    private var videoCapturer:         VideoCapturer?          = null
    private var surfaceTextureHelper:  SurfaceTextureHelper?   = null

    // Renderers de video (se asignan desde la UI)
    var localVideoSink:  VideoSink? = null
    var remoteVideoSink: VideoSink? = null

    // Estado de controles
    var isMuted:       Boolean = false
        private set
    var isCameraOff:   Boolean = false
        private set
    var isFrontCamera: Boolean = true
        private set
    var isSpeakerOn:   Boolean = true
        private set

    // ─── Inicialización ───────────────────────────────────────────────────────

    fun initialize() {
        if (_engineState.value != EngineState.IDLE) return

        // Inicializar WebRTC global (una sola vez por proceso)
        if (!isFactoryInitialized) {
            val initOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(false)
                .createInitializationOptions()
            PeerConnectionFactory.initialize(initOptions)
            isFactoryInitialized = true
            Log.d(TAG, "WebRTC Factory inicializada globalmente")
        }

        // Audio device module con gestión de modo (altavoz/auricular)
        val esEmulador = android.os.Build.FINGERPRINT.startsWith("generic")
                || android.os.Build.FINGERPRINT.startsWith("unknown")
                || android.os.Build.MODEL.contains("google_sdk")
                || android.os.Build.MODEL.contains("Emulator")
                || android.os.Build.MODEL.contains("Android SDK built for x86")
                || android.os.Build.MANUFACTURER.contains("Genymotion")
                || (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic"))
                || "google_sdk" == android.os.Build.PRODUCT

        val audioDeviceModule = JavaAudioDeviceModule.builder(context)
            .setUseStereoInput(false)
            .setUseStereoOutput(false)
            .setUseHardwareAcousticEchoCanceler(!esEmulador)
            .setUseHardwareNoiseSuppressor(!esEmulador)
            .createAudioDeviceModule()

        // Factory de peer connections
        val options = PeerConnectionFactory.Options()
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setOptions(options)
            .setAudioDeviceModule(audioDeviceModule)
            .setVideoDecoderFactory(DefaultVideoDecoderFactory(eglContext))
            .setVideoEncoderFactory(
                DefaultVideoEncoderFactory(eglContext, true, true)
            )
            .createPeerConnectionFactory()

        _engineState.value = EngineState.READY
        Log.d(TAG, "WebRTC Engine inicializado")
    }

    // ─── Crear PeerConnection ─────────────────────────────────────────────────

    fun createPeerConnection(isOffer: Boolean, tipo: TipoLlamada) {
        val factory = peerConnectionFactory ?: run {
            callback.onError("Factory no inicializada")
            return
        }

        val rtcConfig = PeerConnection.RTCConfiguration(ICE_SERVERS).apply {
            bundlePolicy     = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy    = PeerConnection.RtcpMuxPolicy.REQUIRE
            tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.ENABLED
            candidateNetworkPolicy = PeerConnection.CandidateNetworkPolicy.ALL
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            keyType          = PeerConnection.KeyType.ECDSA
            sdpSemantics     = PeerConnection.SdpSemantics.UNIFIED_PLAN
        }

        peerConnection = factory.createPeerConnection(
            rtcConfig,
            createPeerConnectionObserver()
        ) ?: run {
            callback.onError("No se pudo crear PeerConnection")
            return
        }

        // Añadir tracks locales
        addLocalTracks(factory, tipo)

        _engineState.value = if (isOffer) EngineState.CREATING_OFFER else EngineState.WAITING_OFFER
        Log.d(TAG, "PeerConnection creada, isOffer=$isOffer")
    }

    // ─── Tracks de audio y video local ───────────────────────────────────────

    private fun addLocalTracks(factory: PeerConnectionFactory, tipo: TipoLlamada) {
        // Audio siempre
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("echoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("noiseSuppression", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("autoGainControl",  "true"))
        }
        localAudioSource = factory.createAudioSource(audioConstraints)
        localAudioTrack  = factory.createAudioTrack("audio0", localAudioSource).also { track ->
            peerConnection?.addTrack(track, listOf("stream0"))
        }

        // Video solo si es videollamada
        if (tipo == TipoLlamada.VIDEO) {
            surfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", eglContext)
            videoCapturer = createVideoCapturer()?.also { capturer ->
                localVideoSource = factory.createVideoSource(capturer.isScreencast)
                capturer.initialize(surfaceTextureHelper, context, localVideoSource!!.capturerObserver)
                capturer.startCapture(640, 480, 30)
            }
            localVideoTrack = localVideoSource?.let {
                factory.createVideoTrack("video0", it).also { track ->
                    localVideoSink?.let { sink -> track.addSink(sink) }
                    peerConnection?.addTrack(track, listOf("stream0"))
                }
            }
        }
    }

    // ─── Capturador de cámara ─────────────────────────────────────────────────

    private fun createVideoCapturer(): VideoCapturer? {
        val enumerator = Camera2Enumerator(context)
        // Primero buscar cámara frontal
        enumerator.deviceNames.forEach { name ->
            if (enumerator.isFrontFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        // Fallback cámara trasera
        enumerator.deviceNames.forEach { name ->
            if (enumerator.isBackFacing(name)) {
                return enumerator.createCapturer(name, null)
            }
        }
        return null
    }

    // ─── Señalización SDP ─────────────────────────────────────────────────────

    /** Nutriólogo llama esto para crear el OFFER */
    fun createOffer() {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        pc.createOffer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        Log.d(TAG, "Offer SDP local set OK")
                        callback.onLocalSdpReady(sdp)
                    }
                    override fun onSetFailure(error: String) {
                        callback.onError("SetLocalDescription falló: $error")
                    }
                }, sdp)
            }
            override fun onCreateFailure(error: String) {
                callback.onError("CreateOffer falló: $error")
            }
        }, constraints)
    }

    /** Padre llama esto al recibir el OFFER del nutriólogo */
    fun setRemoteOffer(sdpDescription: String) {
        val sdp = SessionDescription(SessionDescription.Type.OFFER, sdpDescription)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote offer set OK")
                createAnswer()
            }
            override fun onSetFailure(error: String) {
                callback.onError("SetRemoteDescription (offer) falló: $error")
            }
        }, sdp)
    }

    /** Crea ANSWER después de recibir el OFFER */
    private fun createAnswer() {
        val pc = peerConnection ?: return
        val constraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
        }
        pc.createAnswer(object : SdpObserverAdapter() {
            override fun onCreateSuccess(sdp: SessionDescription) {
                pc.setLocalDescription(object : SdpObserverAdapter() {
                    override fun onSetSuccess() {
                        Log.d(TAG, "Answer SDP local set OK")
                        callback.onLocalSdpReady(sdp)
                    }
                    override fun onSetFailure(error: String) {
                        callback.onError("SetLocalDescription (answer) falló: $error")
                    }
                }, sdp)
            }
            override fun onCreateFailure(error: String) {
                callback.onError("CreateAnswer falló: $error")
            }
        }, constraints)
    }

    /** Nutriólogo llama esto cuando recibe el ANSWER del padre */
    fun setRemoteAnswer(sdpDescription: String) {
        val sdp = SessionDescription(SessionDescription.Type.ANSWER, sdpDescription)
        peerConnection?.setRemoteDescription(object : SdpObserverAdapter() {
            override fun onSetSuccess() {
                Log.d(TAG, "Remote answer set OK")
            }
            override fun onSetFailure(error: String) {
                callback.onError("SetRemoteDescription (answer) falló: $error")
            }
        }, sdp)
    }

    /** Añade un ICE candidate del lado remoto */
    fun addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int, sdp: String) {
        val candidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
        peerConnection?.addIceCandidate(candidate) ?: Log.w(TAG, "PeerConnection nula al añadir ICE")
    }

    // ─── Controles en llamada ─────────────────────────────────────────────────

    fun silenciar(muted: Boolean) {
        isMuted = muted
        localAudioTrack?.setEnabled(!muted)
        Log.d(TAG, "Micrófono silenciado: $muted")
    }

    fun apagarCamara(off: Boolean) {
        isCameraOff = off
        localVideoTrack?.setEnabled(!off)
        Log.d(TAG, "Cámara apagada: $off")
    }

    fun cambiarCamara() {
        val capturer = videoCapturer as? CameraVideoCapturer ?: return
        isFrontCamera = !isFrontCamera
        capturer.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFront: Boolean) {
                Log.d(TAG, "Cámara cambiada. Frontal: $isFront")
            }
            override fun onCameraSwitchError(error: String) {
                Log.e(TAG, "Error al cambiar cámara: $error")
            }
        })
    }

    fun setSpeaker(on: Boolean) {
        isSpeakerOn = on
        // El modo de altavoz se controla a nivel de AudioManager desde el ViewModel/Activity
    }

    // ─── Limpieza ─────────────────────────────────────────────────────────────

    fun dispose() {
        Log.d(TAG, "Disposing WebRTC Engine")
        videoCapturer?.stopCapture()
        videoCapturer?.dispose()
        videoCapturer = null

        localVideoTrack?.dispose()
        localVideoTrack = null

        localAudioTrack?.dispose()
        localAudioTrack = null

        localVideoSource?.dispose()
        localVideoSource = null

        localAudioSource?.dispose()
        localAudioSource = null

        surfaceTextureHelper?.dispose()
        surfaceTextureHelper = null

        peerConnection?.close()
        peerConnection?.dispose()
        peerConnection = null

        peerConnectionFactory?.dispose()
        peerConnectionFactory = null

        _engineState.value = EngineState.IDLE
    }

    // ─── PeerConnectionObserver ───────────────────────────────────────────────

    private fun createPeerConnectionObserver() = object : PeerConnection.Observer {

        override fun onIceCandidate(candidate: IceCandidate) {
            Log.d(TAG, "ICE candidate local listo")
            callback.onLocalIceCandidateReady(candidate)
        }

        override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) {}

        override fun onIceConnectionChange(state: PeerConnection.IceConnectionState) {
            Log.d(TAG, "ICE connection state: $state")
            when (state) {
                PeerConnection.IceConnectionState.CONNECTED,
                PeerConnection.IceConnectionState.COMPLETED -> {
                    _engineState.value = EngineState.CONNECTED
                    callback.onConnected()
                }
                PeerConnection.IceConnectionState.DISCONNECTED,
                PeerConnection.IceConnectionState.FAILED,
                PeerConnection.IceConnectionState.CLOSED -> {
                    _engineState.value = EngineState.DISCONNECTED
                    callback.onDisconnected()
                }
                else -> {}
            }
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            val track = transceiver.receiver.track() ?: return
            if (track is VideoTrack) {
                Log.d(TAG, "Video track remoto recibido")
                remoteVideoSink?.let { sink -> track.addSink(sink) }
                callback.onRemoteVideoTrackReady(track)
            }
        }

        override fun onAddStream(stream: MediaStream) {}
        override fun onRemoveStream(stream: MediaStream) {}
        override fun onDataChannel(dc: DataChannel) {}
        override fun onRenegotiationNeeded() {}
        override fun onSignalingChange(state: PeerConnection.SignalingState) {}
        override fun onIceConnectionReceivingChange(receiving: Boolean) {}
        override fun onIceGatheringChange(state: PeerConnection.IceGatheringState) {}
        override fun onConnectionChange(state: PeerConnection.PeerConnectionState) {}
    }

    // ─── Estado del motor ─────────────────────────────────────────────────────

    enum class EngineState {
        IDLE, READY, CREATING_OFFER, WAITING_OFFER, CONNECTED, DISCONNECTED
    }
}

// ─── SdpObserver base para no repetir métodos vacíos ─────────────────────────

open class SdpObserverAdapter : SdpObserver {
    override fun onCreateSuccess(sdp: SessionDescription) {}
    override fun onSetSuccess() {}
    override fun onCreateFailure(error: String) {}
    override fun onSetFailure(error: String) {}
}