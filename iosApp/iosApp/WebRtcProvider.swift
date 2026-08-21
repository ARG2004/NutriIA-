import Foundation
import Shared
import WebRTC
import UIKit
import AVFoundation

/**
 * Implementación real de WebRTC en Swift para satisfacer el bridge de Kotlin.
 */
class WebRtcProvider: NSObject, IOSWebRtcProvider {

    private var factory: RTCPeerConnectionFactory!
    private var peerConnection: RTCPeerConnection?
    private var videoCapturer: RTCVideoCapturer?
    private var localVideoTrack: RTCVideoTrack?
    private var remoteVideoTrack: RTCVideoTrack?

    private let localView = RTCEAGLVideoView()
    private let remoteView = RTCEAGLVideoView()

    private var isVideoCall = true

    override init() {
        super.init()
        RTCInitializeSSL()
        let videoEncoderFactory = RTCDefaultVideoEncoderFactory()
        let videoDecoderFactory = RTCDefaultVideoDecoderFactory()
        self.factory = RTCPeerConnectionFactory(encoderFactory: videoEncoderFactory, decoderFactory: videoDecoderFactory)

        localView.contentMode = .scaleAspectFill
        remoteView.contentMode = .scaleAspectFill

        setupAudioSession()
    }

    private func setupAudioSession() {
        let audioSession = RTCAudioSession.sharedInstance()
        audioSession.lockForConfiguration()
        do {
            try audioSession.setCategory(AVAudioSession.Category.playAndRecord.rawValue, with: [.defaultToSpeaker, .allowBluetooth])
            try audioSession.setMode(AVAudioSession.Mode.videoChat.rawValue)
            try audioSession.setActive(true)
            NSLog("✅ [WebRtcProvider] Audio Session configurada")
        } catch {
            NSLog("❌ [WebRtcProvider] Error configurando Audio Session: \(error)")
        }
        audioSession.unlockForConfiguration()
    }

    func initialize() {
        NSLog("✅ [WebRtcProvider] Inicializado")
    }

    func createPeerConnection(isOffer: Bool, isVideo: Bool) {
        self.isVideoCall = isVideo
        let config = RTCConfiguration()
        config.iceServers = [RTCIceServer(urlStrings: ["stun:stun.l.google.com:19302"])]

        let constraints = RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: nil)
        self.peerConnection = factory.peerConnection(with: config, constraints: constraints, delegate: self)

        setupMediaTracks()
    }

    private func setupMediaTracks() {
        // Audio
        let audioTrack = factory.audioTrack(with: factory.audioSource(with: nil), trackId: "audio0")
        peerConnection?.add(audioTrack, streamIds: ["stream0"])

        // Video
        if isVideoCall {
            let videoSource = factory.videoSource()
            #if targetEnvironment(simulator)
            self.videoCapturer = RTCFileVideoCapturer(delegate: videoSource)
            #else
            self.videoCapturer = RTCCameraVideoCapturer(delegate: videoSource)
            #endif

            let videoTrack = factory.videoTrack(with: videoSource, trackId: "video0")
            self.localVideoTrack = videoTrack
            peerConnection?.add(videoTrack, streamIds: ["stream0"])

            videoTrack.add(localView)
            startCapture()
        }
    }

    private func startCapture() {
        guard let capturer = videoCapturer as? RTCCameraVideoCapturer else { return }
        guard let device = RTCCameraVideoCapturer.captureDevices().first(where: { $0.position == .front }) else { return }
        guard let format = RTCCameraVideoCapturer.supportedFormats(for: device).last else { return }
        let fps = Int(format.videoSupportedFrameRateRanges.first?.maxFrameRate ?? 30)

        capturer.startCapture(with: device, format: format, fps: fps)
    }

    func createOffer() {
        let constraints = RTCMediaConstraints(mandatoryConstraints: ["OfferToReceiveAudio": "true", "OfferToReceiveVideo": isVideoCall ? "true" : "false"], optionalConstraints: nil)
        peerConnection?.offer(for: constraints) { [weak self] (sdp, error) in
            guard let sdp = sdp else { return }
            self?.peerConnection?.setLocalDescription(sdp) { _ in
                // Notificar a Kotlin
                CallEngineProvider.shared.getEngine()?.onLocalSdpReady(sdp: SessionDescription(type: .offer, description: sdp.sdp))
            }
        }
    }

    func setRemoteOffer(sdp: String) {
        let remoteSdp = RTCSessionDescription(type: .offer, sdp: sdp)
        peerConnection?.setRemoteDescription(remoteSdp) { [weak self] error in
            if error == nil {
                self?.createAnswer()
            }
        }
    }

    private func createAnswer() {
        let constraints = RTCMediaConstraints(mandatoryConstraints: ["OfferToReceiveAudio": "true", "OfferToReceiveVideo": isVideoCall ? "true" : "false"], optionalConstraints: nil)
        peerConnection?.answer(for: constraints) { [weak self] (sdp, error) in
            guard let sdp = sdp else { return }
            self?.peerConnection?.setLocalDescription(sdp) { _ in
                CallEngineProvider.shared.getEngine()?.onLocalSdpReady(sdp: SessionDescription(type: .answer, description: sdp.sdp))
            }
        }
    }

    func setRemoteAnswer(sdp: String) {
        let remoteSdp = RTCSessionDescription(type: .answer, sdp: sdp)
        peerConnection?.setRemoteDescription(remoteSdp) { _ in }
    }

    func addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int32, sdp: String) {
        let candidate = RTCIceCandidate(sdp: sdp, sdpMLineIndex: sdpMLineIndex, sdpMid: sdpMid)
        NSLog("🧊 [WebRtcProvider] Añadiendo ICE candidate remoto")
        peerConnection?.add(candidate) { error in
            if let error = error {
                NSLog("❌ [WebRtcProvider] Error añadiendo ICE: \(error.localizedDescription)")
            }
        }
    }

    func setMuted(muted: KotlinBoolean) {
        peerConnection?.senders.forEach { sender in
            if sender.track?.kind == "audio" {
                sender.track?.isEnabled = !muted.boolValue
            }
        }
    }

    func setCameraEnabled(enabled: KotlinBoolean) {
        localVideoTrack?.isEnabled = enabled.boolValue
    }

    func switchCamera() {
        guard let capturer = videoCapturer as? RTCCameraVideoCapturer else { return }
        // Lógica simple de rotación
        let currentPos = (capturer.captureSession.inputs.first as? AVCaptureDeviceInput)?.device.position
        let nextPos: AVCaptureDevice.Position = (currentPos == .front) ? .back : .front

        guard let device = RTCCameraVideoCapturer.captureDevices().first(where: { $0.position == nextPos }) else { return }
        guard let format = RTCCameraVideoCapturer.supportedFormats(for: device).last else { return }
        capturer.startCapture(with: device, format: format, fps: 30)
    }

    func getLocalVideoView() -> UIView? { return localView }
    func getRemoteVideoView() -> UIView? { return remoteView }

    func dispose() {
        peerConnection?.close()
        peerConnection = nil
        localVideoTrack = nil
        remoteVideoTrack = nil
        (videoCapturer as? RTCCameraVideoCapturer)?.stopCapture()
        videoCapturer = nil
    }
}

extension WebRtcProvider: RTCPeerConnectionDelegate {
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {
        NSLog("📶 [WebRtcProvider] Signaling state: \(stateChanged.rawValue)")
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {
        NSLog("📺 [WebRtcProvider] Stream añadido")
        if let track = stream.videoTracks.first {
            self.remoteVideoTrack = track
            track.add(remoteView)
            CallEngineProvider.shared.getEngine()?.onRemoteVideoTrackReady(track: VideoTrack(nativeTrack: track))
        }
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {}
    func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {}
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {
        NSLog("❄️ [WebRtcProvider] ICE Connection state: \(newState.rawValue)")
        if newState == .connected {
            CallEngineProvider.shared.getEngine()?.onConnected()
        } else if newState == .disconnected {
            CallEngineProvider.shared.getEngine()?.onDisconnected()
        }
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceGatheringState) {}
    func peerConnection(_ peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
        CallEngineProvider.shared.getEngine()?.onLocalIceCandidateReady(candidate: IceCandidate(sdpMid: candidate.sdpMid ?? "", sdpMLineIndex: candidate.sdpMLineIndex, sdp: candidate.sdp))
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove candidates: [RTCIceCandidate]) {}
    func peerConnection(_ peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {}
}
