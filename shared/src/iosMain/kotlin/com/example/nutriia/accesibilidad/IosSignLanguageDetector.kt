@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.example.nutriia.accesibilidad

import kotlinx.cinterop.useContents
import platform.Foundation.NSLog
import platform.Vision.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Detector Nativo de Señas LSM para iOS impulsado por Apple Vision Framework y Neural Engine.
 * Extrae los 21 puntos óseos de la mano y los alimenta directamente al SignLanguageClassifier.
 */
class IosSignLanguageDetector(
    private val onLetraDetectada: (ResultadoClasificacion?) -> Unit,
    private val onSenaDetectada: ((ResultadoSenaComunicativa) -> Unit)? = null
) {
    private val historialFrames = mutableListOf<List<NormalizedPoint3D>>()

    /**
     * Procesa los 21 puntos (x, y, z) extraídos de Apple Vision (desde Swift o Kotlin/Native).
     * @param coordenadas FloatArray de tamaño 63 (21 puntos x 3 componentes: x, y, z).
     */
    fun procesarCoordenadas(coordenadas: FloatArray) {
        if (coordenadas.size < 63) {
            if (historialFrames.isNotEmpty()) historialFrames.clear()
            dispatch_async(dispatch_get_main_queue()) {
                onLetraDetectada(null)
            }
            return
        }

        val points = ArrayList<NormalizedPoint3D>(21)
        for (i in 0 until 21) {
            val x = coordenadas[i * 3]
            val y = coordenadas[i * 3 + 1]
            val z = coordenadas[i * 3 + 2]
            points.add(NormalizedPoint3D(x, y, z))
        }

        historialFrames.add(points)
        if (historialFrames.size > 30) {
            historialFrames.removeAt(0)
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

    /**
     * Procesa una observación directa de Apple Vision (VNRecognizedPointsObservation).
     */
    fun procesarObservacionVision(observation: VNRecognizedPointsObservation?) {
        if (observation == null) {
            if (historialFrames.isNotEmpty()) historialFrames.clear()
            dispatch_async(dispatch_get_main_queue()) {
                onLetraDetectada(null)
            }
            return
        }

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

        val points = mutableListOf<NormalizedPoint3D>()
        for (key in jointKeys) {
            val point = observation.recognizedPointForJointName(key, error = null)
            if (point != null && point.confidence > 0.3) {
                val (nx, ny) = point.location.useContents { x.toFloat() to (1.0 - y).toFloat() }
                points.add(NormalizedPoint3D(nx, ny, 0f))
            } else {
                points.add(NormalizedPoint3D(0f, 0f, 0f))
            }
        }

        if (points.size == 21) {
            historialFrames.add(points)
            if (historialFrames.size > 30) historialFrames.removeAt(0)

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
    }

    fun limpiarHistorial() {
        historialFrames.clear()
    }
}
