@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.accesibilidad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.useContents
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVCaptureConnection
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDeviceInput
import platform.AVFoundation.AVCaptureDevicePositionFront
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureOutput
import platform.AVFoundation.AVCaptureSession
import platform.AVFoundation.AVCaptureSessionPresetMedium
import platform.AVFoundation.AVCaptureVideoDataOutput
import platform.AVFoundation.AVCaptureVideoDataOutputSampleBufferDelegateProtocol
import platform.AVFoundation.AVCaptureVideoOrientationPortrait
import platform.AVFoundation.AVCaptureVideoPreviewLayer
import platform.AVFoundation.AVLayerVideoGravityResizeAspectFill
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.UIKit.UIApplication
import platform.UIKit.UIView
import platform.Vision.VNDetectHumanHandPoseRequest
import platform.Vision.VNHumanHandPoseObservation
import platform.Vision.VNHumanHandPoseObservationJointNameIndexDIP
import platform.Vision.VNHumanHandPoseObservationJointNameIndexMCP
import platform.Vision.VNHumanHandPoseObservationJointNameIndexPIP
import platform.Vision.VNHumanHandPoseObservationJointNameIndexTip
import platform.Vision.VNHumanHandPoseObservationJointNameLittleDIP
import platform.Vision.VNHumanHandPoseObservationJointNameLittleMCP
import platform.Vision.VNHumanHandPoseObservationJointNameLittlePIP
import platform.Vision.VNHumanHandPoseObservationJointNameLittleTip
import platform.Vision.VNHumanHandPoseObservationJointNameMiddleDIP
import platform.Vision.VNHumanHandPoseObservationJointNameMiddleMCP
import platform.Vision.VNHumanHandPoseObservationJointNameMiddlePIP
import platform.Vision.VNHumanHandPoseObservationJointNameMiddleTip
import platform.Vision.VNHumanHandPoseObservationJointNameRingDIP
import platform.Vision.VNHumanHandPoseObservationJointNameRingMCP
import platform.Vision.VNHumanHandPoseObservationJointNameRingPIP
import platform.Vision.VNHumanHandPoseObservationJointNameRingTip
import platform.Vision.VNHumanHandPoseObservationJointNameThumbCMC
import platform.Vision.VNHumanHandPoseObservationJointNameThumbIP
import platform.Vision.VNHumanHandPoseObservationJointNameThumbMP
import platform.Vision.VNHumanHandPoseObservationJointNameThumbTip
import platform.Vision.VNHumanHandPoseObservationJointNameWrist
import platform.Vision.VNHumanHandPoseObservationJointsGroupNameAll
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizedPoint
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

@Composable
actual fun PlatformSignLanguageCameraPreview(
    modifier: Modifier,
    onLandmarksDetected: (List<NormalizedPoint3D>) -> Unit
) {
    var permisoConcedido by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
        )
    }

    LaunchedEffect(Unit) {
        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
        if (status == AVAuthorizationStatusAuthorized) {
            permisoConcedido = true
        } else if (status == AVAuthorizationStatusNotDetermined) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) {
                    permisoConcedido = granted
                }
            }
        }
    }

    if (!permisoConcedido) {
        Box(
            modifier = modifier.background(Color(0xFF10101C)),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    Icons.Rounded.Videocam,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Permiso de cámara requerido",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    "NutrIA utiliza la cámara frontal para reconocer tus señas LSM en tiempo real.",
                    color = Color.Gray,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        val status = AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)
                        if (status == AVAuthorizationStatusNotDetermined) {
                            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                                dispatch_async(dispatch_get_main_queue()) {
                                    permisoConcedido = granted
                                }
                            }
                        } else {
                            val url = platform.Foundation.NSURL.URLWithString(platform.UIKit.UIApplicationOpenSettingsURLString)
                            if (url != null) {
                                UIApplication.sharedApplication.openURL(url)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Otorgar permiso", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    } else {
        val cameraSessionHolder = remember { CameraVisionSessionHolder(onLandmarksDetected) }

        DisposableEffect(cameraSessionHolder) {
            cameraSessionHolder.start()
            onDispose {
                cameraSessionHolder.stop()
            }
        }

        UIKitView(
            factory = {
                cameraSessionHolder.getView()
            },
            modifier = modifier,
            update = { view ->
                cameraSessionHolder.updateBounds(view)
            }
        )
    }
}

private class CameraVisionSessionHolder(
    private val onLandmarksDetected: (List<NormalizedPoint3D>) -> Unit
) {
    private val session = AVCaptureSession()
    private val previewContainerView = CameraPreviewUIView()
    private var previewLayer: AVCaptureVideoPreviewLayer? = null
    private var isSetup = false
    private val processingQueue = dispatch_queue_create("com.nutriia.lsm.vision", null)

    private val sampleBufferDelegate = object : NSObject(), AVCaptureVideoDataOutputSampleBufferDelegateProtocol {
        private var isBusy = false

        override fun captureOutput(
            output: AVCaptureOutput,
            didOutputSampleBuffer: CMSampleBufferRef?,
            fromConnection: AVCaptureConnection
        ) {
            if (didOutputSampleBuffer == null || isBusy) return
            isBusy = true

            try {
                val pixelBuffer = CMSampleBufferGetImageBuffer(didOutputSampleBuffer) ?: run {
                    isBusy = false
                    return
                }

                val request = VNDetectHumanHandPoseRequest()
                request.maximumHandCount = 1u

                val handler = VNImageRequestHandler(
                    cvPixelBuffer = pixelBuffer,
                    options = emptyMap<Any?, Any?>()
                )

                handler.performRequests(listOf(request), null)
                val results = request.results as? List<VNHumanHandPoseObservation>

                if (!results.isNullOrEmpty()) {
                    val hand = results[0]
                    val recognizedPoints = hand.recognizedPointsForJointsGroupName(
                        VNHumanHandPoseObservationJointsGroupNameAll,
                        null
                    )

                    if (recognizedPoints != null) {
                        val jointKeys = listOf(
                            VNHumanHandPoseObservationJointNameWrist,
                            VNHumanHandPoseObservationJointNameThumbCMC,
                            VNHumanHandPoseObservationJointNameThumbMP,
                            VNHumanHandPoseObservationJointNameThumbIP,
                            VNHumanHandPoseObservationJointNameThumbTip,
                            VNHumanHandPoseObservationJointNameIndexMCP,
                            VNHumanHandPoseObservationJointNameIndexPIP,
                            VNHumanHandPoseObservationJointNameIndexDIP,
                            VNHumanHandPoseObservationJointNameIndexTip,
                            VNHumanHandPoseObservationJointNameMiddleMCP,
                            VNHumanHandPoseObservationJointNameMiddlePIP,
                            VNHumanHandPoseObservationJointNameMiddleDIP,
                            VNHumanHandPoseObservationJointNameMiddleTip,
                            VNHumanHandPoseObservationJointNameRingMCP,
                            VNHumanHandPoseObservationJointNameRingPIP,
                            VNHumanHandPoseObservationJointNameRingDIP,
                            VNHumanHandPoseObservationJointNameRingTip,
                            VNHumanHandPoseObservationJointNameLittleMCP,
                            VNHumanHandPoseObservationJointNameLittlePIP,
                            VNHumanHandPoseObservationJointNameLittleDIP,
                            VNHumanHandPoseObservationJointNameLittleTip
                        )

                        val points = ArrayList<NormalizedPoint3D>(21)
                        var validCount = 0

                        for (key in jointKeys) {
                            val pt = recognizedPoints[key] as? VNRecognizedPoint
                            if (pt != null && pt.confidence > 0.15f) {
                                val normX = pt.location.useContents { x.toFloat() }
                                val normY = (1.0 - pt.location.useContents { y }).toFloat()
                                points.add(NormalizedPoint3D(normX, normY, 0f))
                                validCount++
                            } else {
                                points.add(NormalizedPoint3D(0f, 0f, 0f))
                            }
                        }

                        if (validCount >= 18) {
                            dispatch_async(dispatch_get_main_queue()) {
                                onLandmarksDetected(points)
                            }
                        } else {
                            dispatch_async(dispatch_get_main_queue()) {
                                onLandmarksDetected(emptyList())
                            }
                        }
                    } else {
                        dispatch_async(dispatch_get_main_queue()) {
                            onLandmarksDetected(emptyList())
                        }
                    }
                } else {
                    dispatch_async(dispatch_get_main_queue()) {
                        onLandmarksDetected(emptyList())
                    }
                }
            } catch (_: Throwable) {
                dispatch_async(dispatch_get_main_queue()) {
                    onLandmarksDetected(emptyList())
                }
            } finally {
                isBusy = false
            }
        }
    }

    private fun setupCameraIfNeeded() {
        if (isSetup) return
        isSetup = true

        try {
            session.sessionPreset = AVCaptureSessionPresetMedium

            val discovery = AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
                mediaType = AVMediaTypeVideo,
                position = AVCaptureDevicePositionFront
            )
            val frontCamera = discovery.devices.firstOrNull() as? AVCaptureDevice
                ?: AVCaptureDevice.defaultDeviceWithDeviceType(
                    AVCaptureDeviceTypeBuiltInWideAngleCamera,
                    AVMediaTypeVideo,
                    AVCaptureDevicePositionFront
                )

            if (frontCamera != null) {
                val input = AVCaptureDeviceInput.deviceInputWithDevice(frontCamera, null) as? AVCaptureDeviceInput
                if (input != null && session.canAddInput(input)) {
                    session.addInput(input)
                }
            }

            val dataOutput = AVCaptureVideoDataOutput()
            dataOutput.alwaysDiscardsLateVideoFrames = true
            dataOutput.setSampleBufferDelegate(sampleBufferDelegate, processingQueue)

            if (session.canAddOutput(dataOutput)) {
                session.addOutput(dataOutput)
                val connection = dataOutput.connectionWithMediaType(AVMediaTypeVideo)
                if (connection != null) {
                    if (connection.supportsVideoOrientation) {
                        connection.setVideoOrientation(AVCaptureVideoOrientationPortrait)
                    }
                    if (connection.supportsVideoMirroring) {
                        connection.setVideoMirrored(true)
                    }
                }
            }

            val layer = AVCaptureVideoPreviewLayer.layerWithSession(session)
            layer.videoGravity = AVLayerVideoGravityResizeAspectFill
            previewContainerView.setPreviewLayer(layer)
            previewLayer = layer
        } catch (_: Throwable) {}
    }

    fun getView(): UIView {
        setupCameraIfNeeded()
        return previewContainerView
    }

    fun updateBounds(view: UIView) {
        previewContainerView.updateLayerFrame()
    }

    fun start() {
        setupCameraIfNeeded()
        dispatch_async(processingQueue) {
            if (!session.running) {
                session.startRunning()
            }
        }
    }

    fun stop() {
        dispatch_async(processingQueue) {
            if (session.running) {
                session.stopRunning()
            }
        }
    }
}

private class CameraPreviewUIView : UIView(platform.CoreGraphics.CGRectZero.readValue()) {
    private var previewLayer: AVCaptureVideoPreviewLayer? = null

    fun setPreviewLayer(layer: AVCaptureVideoPreviewLayer) {
        this.previewLayer?.removeFromSuperlayer()
        this.previewLayer = layer
        this.layer.addSublayer(layer)
        layer.frame = bounds
    }

    fun updateLayerFrame() {
        previewLayer?.frame = bounds
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        previewLayer?.frame = bounds
    }
}
