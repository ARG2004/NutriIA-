@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.example.nutriia.accesibilidad

import platform.AVFoundation.*
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectMake
import platform.CoreMedia.CMSampleBufferGetImageBuffer
import platform.CoreMedia.CMSampleBufferRef
import platform.CoreVideo.CVPixelBufferRef
import platform.Foundation.NSError
import platform.Foundation.NSLog
import platform.Vision.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_queue_create

/**
 * Detector Nativo de Señas LSM para iOS impulsado por Apple Vision Framework y Neural Engine.
 * Extrae los 21 puntos óseos de la mano y los alimenta directamente al SignLanguageClassifier.
 */
class IosSignLanguageDetector(
    private val onLetraDetectada: (ResultadoClasificacion?) -> Unit,
    private val onSenaDetectada: ((ResultadoSenaComunicativa) -> Unit)? = null
) {
    private var handPoseRequest: VNDetectHumanHandPoseRequest? = null
    private val historialFrames = mutableListOf<List<NormalizedPoint3D>>()
    private val processingQueue = dispatch_queue_create("com.nutriia.handpose", null)
    private var isProcessing = false

    init {
        try {
            val req = VNDetectHumanHandPoseRequest()
            req.maximumHandCount = 1
            handPoseRequest = req
        } catch (t: Throwable) {
            NSLog("⚠️ [IosSignLanguageDetector] Vision Hand Pose no disponible: %s", t.message ?: "")
        }
    }

    /**
     * Procesa un fotograma de la cámara de iOS (CMSampleBuffer) sin costo de copia de memoria.
     */
    fun processSampleBuffer(sampleBuffer: CMSampleBufferRef, isFrontCamera: Boolean = true) {
        val request = handPoseRequest ?: return
        if (isProcessing) return
        isProcessing = true

        val pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) ?: run {
            isProcessing = false
            return
        }

        val handler = VNImageRequestHandler(
            cvPixelBuffer = pixelBuffer,
            orientation = if (isFrontCamera) kCGImagePropertyOrientationLeftMirrored else kCGImagePropertyOrientationRight,
            options = emptyMap<Any?, Any>()
        )

        try {
            handler.performRequests(listOf(request), null)
            val observations = request.results as? List<VNRecognizedPointsObservation>
            val firstHand = observations?.firstOrNull()

            if (firstHand != null) {
                val points = extract21Joints(firstHand)
                if (points.size == 21) {
                    synchronized(historialFrames) {
                        historialFrames.add(points)
                        if (historialFrames.size > 30) historialFrames.removeAt(0)
                    }

                    val resultadoLetra = SignLanguageClassifier.clasificarConConfianza(
                        landmarks = points,
                        historialPuntos = historialFrames
                    )

                    val resultadoSena = SignLanguageClassifier.clasificarSenaComunicativa(
                        landmarks = points,
                        historialPuntos = historialFrames
                    )

                    dispatch_async(dispatch_get_main_queue()) {
                        onLetraDetectada(resultadoLetra)
                        if (resultadoSena != null) {
                            onSenaDetectada?.invoke(resultadoSena)
                        }
                    }
                }
            } else {
                synchronized(historialFrames) {
                    if (historialFrames.isNotEmpty()) historialFrames.clear()
                }
                dispatch_async(dispatch_get_main_queue()) {
                    onLetraDetectada(null)
                }
            }
        } catch (t: Throwable) {
            NSLog("⚠️ Error procesando fotograma de mano: %s", t.message ?: "")
        } finally {
            isProcessing = false
        }
    }

    private fun extract21Joints(observation: VNRecognizedPointsObservation): List<NormalizedPoint3D> {
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

        val result = mutableListOf<NormalizedPoint3D>()
        for (key in jointKeys) {
            val point = observation.recognizedPointForJointName(key, null)
            if (point != null && point.confidence > 0.3) {
                // En Apple Vision, las coordenadas Y van de 0 abajo a 1 arriba;
                // las invertimos (1.0 - y) para estar en paridad exacta con MediaPipe/Compose
                val nx = point.location.x.toFloat()
                val ny = (1.0 - point.location.y).toFloat()
                result.add(NormalizedPoint3D(nx, ny, 0f))
            } else {
                // Punto interpolado por defecto si no es visible
                result.add(NormalizedPoint3D(0f, 0f, 0f))
            }
        }
        return result
    }
}
