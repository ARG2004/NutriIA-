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

    private let localView = RTCMTLVideoView(frame: .zero)
    private let remoteView = RTCMTLVideoView(frame: .zero)

    private var isVideoCall = true

    override init() {
        super.init()
        let videoEncoderFactory = RTCDefaultVideoEncoderFactory()
        let videoDecoderFactory = RTCDefaultVideoDecoderFactory()
        self.factory = RTCPeerConnectionFactory(encoderFactory: videoEncoderFactory, decoderFactory: videoDecoderFactory)

        localView.videoContentMode = .scaleAspectFill
        remoteView.videoContentMode = .scaleAspectFill
    }

    private func setupAudioSession() {
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setCategory(.playAndRecord, mode: .videoChat, options: [.defaultToSpeaker, .allowBluetoothHFP])
            try audioSession.setActive(true)
            NSLog("✅ [WebRtcProvider] Audio Session ACTIVADA")
        } catch {
            NSLog("❌ [WebRtcProvider] Error activando Audio Session: \(error.localizedDescription)")
        }
    }

    private func deactivateAudioSession() {
        let audioSession = AVAudioSession.sharedInstance()
        do {
            try audioSession.setActive(false)
            NSLog("ℹ️ [WebRtcProvider] Audio Session desactivada")
        } catch {
            NSLog("❌ [WebRtcProvider] Error desactivando Audio Session: \(error.localizedDescription)")
        }
    }

    func initializeEngine() {
        NSLog("✅ [WebRtcProvider] Inicializado")
    }

    func createPeerConnection(isOffer: Bool, isVideo: Bool) {
        self.isVideoCall = isVideo
        let config = RTCConfiguration()
        config.iceServers = [
            RTCIceServer(urlStrings: ["stun:stun.l.google.com:19302"]),
            RTCIceServer(
                urlStrings: [
                    "turn:openrelay.metered.ca:80",
                    "turn:openrelay.metered.ca:443",
                    "turn:openrelay.metered.ca:443?transport=tcp"
                ],
                username: "openrelayproject",
                credential: "openrelayproject"
            )
        ]
        config.sdpSemantics = .unifiedPlan

        let constraints = RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: nil)
        self.peerConnection = factory.peerConnection(with: config, constraints: constraints, delegate: self)

        setupMediaTracks()
        setupAudioSession()
    }

    private func setupMediaTracks() {
        // Audio
        let audioSource = factory.audioSource(with: nil)
        let audioTrack = factory.audioTrack(with: audioSource, trackId: "audio0")
        peerConnection?.add(audioTrack, streamIds: ["stream0"])

        // Video
        if isVideoCall {
            let videoSource = factory.videoSource()
            let capturer = RTCCameraVideoCapturer(delegate: videoSource)
            self.videoCapturer = capturer

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
            self?.peerConnection?.setLocalDescription(sdp) { error in
                if error == nil {
                    // Mapeo KMP: SdpType.OFFER
                    let sdpDesc = SessionDescription.companion.ofOffer(sdp: sdp.sdp)
                    IOSCallBridge.shared.listener?.onLocalSdpReady(sdp: sdpDesc)
                }
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
            self?.peerConnection?.setLocalDescription(sdp) { error in
                if error == nil {
                    // Mapeo KMP: SdpType.ANSWER
                    let sdpDesc = SessionDescription.companion.ofAnswer(sdp: sdp.sdp)
                    IOSCallBridge.shared.listener?.onLocalSdpReady(sdp: sdpDesc)
                }
            }
        }
    }

    func setRemoteAnswer(sdp: String) {
        let remoteSdp = RTCSessionDescription(type: .answer, sdp: sdp)
        peerConnection?.setRemoteDescription(remoteSdp) { error in
            if let error = error {
                NSLog("❌ [WebRtcProvider] Error setting remote answer: \(error.localizedDescription)")
            } else {
                NSLog("✅ [WebRtcProvider] Remote answer set successfully")
            }
        }
    }

    func addRemoteIceCandidate(sdpMid: String, sdpMLineIndex: Int32, sdp: String) {
        let candidate = RTCIceCandidate(sdp: sdp, sdpMLineIndex: sdpMLineIndex, sdpMid: sdpMid)
        peerConnection?.add(candidate) { error in
            if let error = error {
                NSLog("❌ [WebRtcProvider] Error añadiendo ICE: \(error.localizedDescription)")
            }
        }
    }

    func setMuted(muted: Bool) {
        peerConnection?.senders.forEach { sender in
            if sender.track?.kind == "audio" {
                sender.track?.isEnabled = !muted
            }
        }
    }

    func setCameraEnabled(enabled: Bool) {
        localVideoTrack?.isEnabled = enabled
    }

    func switchCamera() {
        guard let capturer = videoCapturer as? RTCCameraVideoCapturer else { return }
        let currentPos = (capturer.captureSession.inputs.first as? AVCaptureDeviceInput)?.device.position
        let nextPos: AVCaptureDevice.Position = (currentPos == .front) ? .back : .front

        guard let device = RTCCameraVideoCapturer.captureDevices().first(where: { $0.position == nextPos }) else { return }
        guard let format = RTCCameraVideoCapturer.supportedFormats(for: device).last else { return }
        capturer.startCapture(with: device, format: format, fps: 30)
    }

    func getLocalVideoView() -> UIView? { return localView }
    func getRemoteVideoView() -> UIView? { return remoteView }

    func dispose() {
        localVideoTrack?.remove(localView)
        remoteVideoTrack?.remove(remoteView)
        peerConnection?.close()
        peerConnection = nil
        localVideoTrack = nil
        remoteVideoTrack = nil
        (videoCapturer as? RTCCameraVideoCapturer)?.stopCapture()
        videoCapturer = nil
        deactivateAudioSession()
    }
}

extension WebRtcProvider: RTCPeerConnectionDelegate {
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {
        NSLog("📶 [WebRtcProvider] Signaling state: %ld", Int(stateChanged.rawValue))
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {
        NSLog("📺 [WebRtcProvider] Stream añadido")
        if let track = stream.videoTracks.first {
            self.remoteVideoTrack = track
            track.add(remoteView)
            let videoTrack = VideoTrack(nativeTrack: track)
            IOSCallBridge.shared.listener?.onRemoteVideoTrackReady(track: videoTrack)
        }
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {}
    func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {}
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {
        NSLog("❄️ [WebRtcProvider] ICE Connection state: %ld", Int(newState.rawValue))
        DispatchQueue.main.async {
            if newState == .connected || newState == .completed {
                IOSCallBridge.shared.listener?.onConnected()
            } else if newState == .disconnected || newState == .failed {
                IOSCallBridge.shared.listener?.onDisconnected()
            }
        }
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceGatheringState) {
        NSLog("🧊 [WebRtcProvider] ICE Gathering state: %ld", Int(newState.rawValue))
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
        let mid = candidate.sdpMid ?? ""
        let index = Int32(candidate.sdpMLineIndex)
        let sdp = candidate.sdp
        let ice = IceCandidate.companion.create(sdpMid: mid, sdpMLineIndex: index, sdp: sdp)
        IOSCallBridge.shared.listener?.onLocalIceCandidateReady(candidate: ice)
    }
    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove candidates: [RTCIceCandidate]) {}
    func peerConnection(_ peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {}
}
