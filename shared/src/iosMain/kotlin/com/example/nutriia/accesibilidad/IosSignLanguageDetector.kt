package com.example.nutriia.accesibilidad

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Detector Nativo de Señas LSM para iOS impulsado por Apple Vision Framework y Neural Engine.
 * Recibe los puntos de la mano detectados por Vision en Swift o Kotlin/Native y los clasifica.
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

        evaluarPuntos(points)
    }

    /**
     * Procesa una lista directa de NormalizedPoint3D.
     */
    fun procesarPuntos(puntos: List<NormalizedPoint3D>) {
        if (puntos.size != 21) {
            if (historialFrames.isNotEmpty()) historialFrames.clear()
            dispatch_async(dispatch_get_main_queue()) {
                onLetraDetectada(null)
            }
            return
        }
        evaluarPuntos(puntos)
    }

    /**
     * Procesa las coordenadas X e Y (listas de tamaño 21) normalizadas (0.0 a 1.0).
     */
    fun procesarPuntosXY(xList: List<Float>, yList: List<Float>) {
        if (xList.size != 21 || yList.size != 21) {
            if (historialFrames.isNotEmpty()) historialFrames.clear()
            dispatch_async(dispatch_get_main_queue()) {
                onLetraDetectada(null)
            }
            return
        }

        val points = ArrayList<NormalizedPoint3D>(21)
        for (i in 0 until 21) {
            points.add(NormalizedPoint3D(xList[i], yList[i], 0f))
        }
        evaluarPuntos(points)
    }

    private fun evaluarPuntos(points: List<NormalizedPoint3D>) {
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

    fun limpiarHistorial() {
        historialFrames.clear()
    }
}
