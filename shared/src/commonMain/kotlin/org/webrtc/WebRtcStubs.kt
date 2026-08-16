package org.webrtc

import org.webrtc.audio.JavaAudioDeviceModule

interface VideoSink {
    fun onFrame(frame: Any?)
}

interface SdpObserver {
    fun onCreateSuccess(sdp: SessionDescription)
    fun onSetSuccess()
    fun onCreateFailure(error: String)
    fun onSetFailure(error: String)
}

class SessionDescription(
    val type: Type = Type.OFFER,
    val description: String = ""
) {
    enum class Type {
        OFFER, PRANSWER, ANSWER, ROLLBACK;
        fun canonicalForm(): String = name.lowercase()
    }
}

class IceCandidate(
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val sdp: String = ""
)

open class MediaStreamTrack(val id: String = "") {
    open fun setEnabled(enable: Boolean): Boolean = true
    open fun dispose() {}
}

class AudioTrack(id: String = "") : MediaStreamTrack(id)

class VideoTrack(id: String = "") : MediaStreamTrack(id) {
    fun addSink(sink: VideoSink) {}
    fun removeSink(sink: VideoSink) {}
}

class AudioSource {
    fun dispose() {}
}

class VideoSource {
    val capturerObserver: Any? = null
    fun dispose() {}
}

class MediaConstraints {
    val mandatory: MutableList<KeyValuePair> = mutableListOf()
    val optional: MutableList<KeyValuePair> = mutableListOf()

    class KeyValuePair(val key: String, val value: String)
}

interface EglBase {
    interface Context
    val eglBaseContext: Context

    fun release() {}

    companion object {
        fun create(): EglBase = object : EglBase {
            override val eglBaseContext: Context = object : Context {}
        }
    }
}

class DefaultVideoDecoderFactory(context: EglBase.Context)
class DefaultVideoEncoderFactory(
    context: EglBase.Context,
    enableIntelVp8Encoder: Boolean = true,
    enableH264HighProfile: Boolean = true
)

interface VideoCapturer {
    fun initialize(surfaceTextureHelper: SurfaceTextureHelper?, context: Any?, capturerObserver: Any?) {}
    fun startCapture(width: Int, height: Int, fps: Int) {}
    fun stopCapture() {}
    fun dispose() {}
}

class SurfaceTextureHelper private constructor() {
    fun dispose() {}
    companion object {
        fun create(threadName: String, eglContext: EglBase.Context): SurfaceTextureHelper = SurfaceTextureHelper()
    }
}

interface CameraEnumerator {
    val deviceNames: Array<String>
    fun isFrontFacing(deviceName: String): Boolean
    fun isBackFacing(deviceName: String): Boolean
    fun createCapturer(deviceName: String, eventsHandler: Any?): VideoCapturer?
}

class Camera2Enumerator(context: Any?) : CameraEnumerator {
    override val deviceNames: Array<String> = emptyArray()
    override fun isFrontFacing(deviceName: String): Boolean = false
    override fun isBackFacing(deviceName: String): Boolean = false
    override fun createCapturer(deviceName: String, eventsHandler: Any?): VideoCapturer? = null
}

class Camera1Enumerator(captureToTexture: Boolean = true) : CameraEnumerator {
    override val deviceNames: Array<String> = emptyArray()
    override fun isFrontFacing(deviceName: String): Boolean = false
    override fun isBackFacing(deviceName: String): Boolean = false
    override fun createCapturer(deviceName: String, eventsHandler: Any?): VideoCapturer? = null
}

class MediaStream(val id: String = "")
class DataChannel(val label: String = "")

class PeerConnection(
    private val observer: Observer? = null
) {
    enum class IceConnectionState {
        NEW, CHECKING, CONNECTED, COMPLETED, FAILED, DISCONNECTED, CLOSED
    }

    enum class IceGatheringState {
        NEW, GATHERING, COMPLETE
    }

    enum class PeerConnectionState {
        NEW, CONNECTING, CONNECTED, DISCONNECTED, FAILED, CLOSED
    }

    enum class SignalingState {
        STABLE, HAVE_LOCAL_OFFER, HAVE_REMOTE_OFFER, HAVE_LOCAL_PRANSWER, HAVE_REMOTE_PRANSWER, CLOSED
    }

    interface Observer {
        fun onSignalingChange(state: SignalingState) {}
        fun onIceConnectionChange(state: IceConnectionState) {}
        fun onIceConnectionReceivingChange(receiving: Boolean) {}
        fun onIceGatheringChange(state: IceGatheringState) {}
        fun onConnectionChange(state: PeerConnectionState) {}
        fun onIceCandidate(candidate: IceCandidate) {}
        fun onIceCandidatesRemoved(candidates: Array<IceCandidate>) {}
        fun onAddStream(stream: MediaStream) {}
        fun onRemoveStream(stream: MediaStream) {}
        fun onDataChannel(dataChannel: DataChannel) {}
        fun onRenegotiationNeeded() {}
        fun onAddTrack(receiver: Any?, mediaStreams: Array<Any?>) {}
        fun onTrack(transceiver: RtpTransceiver) {}
    }

    class RtpTransceiver(val receiver: RtpReceiver)
    class RtpReceiver(val track: MediaStreamTrack?)

    class IceServer(val uri: String) {
        companion object {
            fun builder(uri: String): Builder = Builder(uri)
        }
        class Builder(private val uri: String) {
            fun setUsername(username: String): Builder = this
            fun setPassword(password: String): Builder = this
            fun createIceServer(): IceServer = IceServer(uri)
        }
    }

    class RTCConfiguration(val iceServers: List<IceServer>) {
        var sdpSemantics: SdpSemantics = SdpSemantics.UNIFIED_PLAN
    }

    enum class SdpSemantics { PLAN_B, UNIFIED_PLAN }

    fun createOffer(observer: SdpObserver, constraints: MediaConstraints) {}
    fun createAnswer(observer: SdpObserver, constraints: MediaConstraints) {}
    fun setLocalDescription(observer: SdpObserver, sdp: SessionDescription) {}
    fun setRemoteDescription(observer: SdpObserver, sdp: SessionDescription) {}
    fun addIceCandidate(candidate: IceCandidate): Boolean = true
    fun addTrack(track: MediaStreamTrack?, streamIds: List<String>) {}
    fun close() {}
    fun dispose() {}
}

class PeerConnectionFactory private constructor() {
    class Options

    class InitializationOptions private constructor() {
        companion object {
            fun builder(context: Any?): Builder = Builder()
        }
        class Builder {
            fun setEnableInternalTracer(enable: Boolean): Builder = this
            fun createInitializationOptions(): InitializationOptions = InitializationOptions()
        }
    }

    class Builder {
        fun setOptions(options: Options): Builder = this
        fun setAudioDeviceModule(module: JavaAudioDeviceModule): Builder = this
        fun setVideoDecoderFactory(factory: Any?): Builder = this
        fun setVideoEncoderFactory(factory: Any?): Builder = this
        fun createPeerConnectionFactory(): PeerConnectionFactory = PeerConnectionFactory()
    }

    fun createAudioSource(constraints: MediaConstraints): AudioSource = AudioSource()
    fun createAudioTrack(id: String, source: AudioSource): AudioTrack = AudioTrack(id)
    fun createVideoSource(isScreencast: Boolean): VideoSource = VideoSource()
    fun createVideoTrack(id: String, source: VideoSource): VideoTrack = VideoTrack(id)

    fun createPeerConnection(
        iceServers: List<PeerConnection.IceServer>,
        observer: PeerConnection.Observer
    ): PeerConnection? = PeerConnection(observer)

    fun createPeerConnection(
        rtcConfig: PeerConnection.RTCConfiguration,
        observer: PeerConnection.Observer
    ): PeerConnection? = PeerConnection(observer)

    fun dispose() {}

    companion object {
        fun initialize(options: InitializationOptions) {}
        fun builder(): Builder = Builder()
    }
}
