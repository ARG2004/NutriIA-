package com.example.nutriia.accesibilidad

import android.content.Context
import android.util.Log
import androidx.annotation.Keep
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import java.io.File
import java.io.InputStreamReader
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.atan2

@Keep
data class ResultadoClasificacion(
    val letra: String,
    val confianza: Float
)

@Keep
data class LsmSample(
    val label: String,
    val vector: List<Float>
)

@Keep
data class LsmDataset(
    val samples: List<LsmSample>
)

/**
 * Clasificador Anatómico Relacional 3D Riguroso LSM (Abecedario Completo A-Z + Gestos Dinámicos UTT/SEP).
 * Incorpora orientación vectorial (arriba vs abajo/caído) para distinguir perfectamente M, N, Ñ de L o H,
 * y clasificación 3D para la seña C y todo el abecedario.
 */
object SignLanguageClassifier {

    var datasetStatic: List<LsmSample>? = null

    fun d3D(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        val dz = p1.z() - p2.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun obtenerVectorHibrido(landmarks: List<NormalizedLandmark>): List<Float> {
        if (landmarks.size < 21) return emptyList()

        val wrist = landmarks[0]
        val wX = wrist.x()
        val wY = wrist.y()
        val wZ = wrist.z()

        val dx9 = landmarks[9].x() - wX
        val dy9 = landmarks[9].y() - wY
        val dz9 = landmarks[9].z() - wZ
        val palmSize = sqrt(dx9 * dx9 + dy9 * dy9 + dz9 * dz9).coerceAtLeast(0.001f)

        val vector = ArrayList<Float>(71)

        for (lm in landmarks) {
            vector.add((lm.x() - wX) / palmSize)
            vector.add((lm.y() - wY) / palmSize)
            vector.add((lm.z() - wZ) / palmSize)
        }

        fun nDist(idx1: Int, idx2: Int): Float = d3D(landmarks[idx1], landmarks[idx2]) / palmSize

        vector.add(nDist(4, 8))   // Pulgar-Índice
        vector.add(nDist(8, 12))  // Índice-Medio
        vector.add(nDist(12, 16)) // Medio-Anular
        vector.add(nDist(16, 20)) // Anular-Meñique
        vector.add(nDist(4, 12))  // Pulgar-Medio

        vector.add(nDist(4, 5))   // Pulgar-Nudillo Índice (MCP)
        vector.add(nDist(4, 9))   // Pulgar-Nudillo Medio (MCP)

        val angle = atan2(landmarks[9].y() - wY, landmarks[9].x() - wX)
        vector.add(angle)

        return vector
    }

    private fun lazyLoadDataset(context: Context?): List<LsmSample> {
        if (datasetStatic != null) return datasetStatic!!
        if (context == null) return emptyList()

        val loadedSamples = mutableListOf<LsmSample>()
        val gson = Gson()

        try {
            context.assets.open("lsm_dataset.json").use { inputStream ->
                val reader = InputStreamReader(inputStream)
                val type = object : TypeToken<LsmDataset>() {}.type
                val defaultDb = gson.fromJson<LsmDataset>(reader, type)
                if (defaultDb?.samples != null) {
                    loadedSamples.addAll(defaultDb.samples)
                }
            }
        } catch (e: Exception) {
            Log.e("SignLanguageClassifier", "Error al cargar dataset por defecto: ${e.message}")
        }

        try {
            val customFile = File(context.filesDir, "custom_lsm_dataset.json")
            if (customFile.exists()) {
                customFile.reader().use { reader ->
                    val type = object : TypeToken<LsmDataset>() {}.type
                    val customDb = gson.fromJson<LsmDataset>(reader, type)
                    if (customDb?.samples != null) {
                        val customLabels = customDb.samples.map { it.label }.toSet()
                        loadedSamples.removeAll { it.label in customLabels }
                        loadedSamples.addAll(customDb.samples)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SignLanguageClassifier", "Error al cargar dataset de calibraciones: ${e.message}")
        }

        datasetStatic = loadedSamples
        return loadedSamples
    }

    fun guardarCalibracion(context: Context, letra: String, landmarks: List<NormalizedLandmark>) {
        val vector = obtenerVectorHibrido(landmarks)
        if (vector.isEmpty()) return

        val gson = Gson()
        val customFile = File(context.filesDir, "custom_lsm_dataset.json")
        val existingSamples = mutableListOf<LsmSample>()

        if (customFile.exists()) {
            try {
                customFile.reader().use { reader ->
                    val type = object : TypeToken<LsmDataset>() {}.type
                    val customDb = gson.fromJson<LsmDataset>(reader, type)
                    if (customDb?.samples != null) {
                        existingSamples.addAll(customDb.samples)
                    }
                }
            } catch (e: Exception) {
                Log.e("SignLanguageClassifier", "Error al leer calibraciones existentes: ${e.message}")
            }
        }

        existingSamples.removeAll { it.label == letra }
        existingSamples.add(LsmSample(letra, vector))

        try {
            customFile.writer().use { writer ->
                val db = LsmDataset(existingSamples)
                gson.toJson(db, writer)
            }
        } catch (e: Exception) {
            Log.e("SignLanguageClassifier", "Error al guardar calibración: ${e.message}")
        }

        datasetStatic = null
    }

    fun clasificarConConfianza(
        landmarks2D: List<NormalizedLandmark>,
        landmarks3D: List<Landmark>? = null,
        soloNumeros: Boolean = false,
        esCampoFecha: Boolean = false,
        historialPuntos: List<List<NormalizedLandmark>> = emptyList(),
        debug: Boolean = false,
        context: Context? = null
    ): ResultadoClasificacion? {
        if (landmarks2D.size < 21) return null

        // ── CONVENCIÓN DE CÁMARA SELFIE (FRONTAL) ──
        // 1. Eje X (Horizontal): En MediaPipe LIVE_STREAM con cámara frontal, las coordenadas 2D (0.0 a 1.0)
        //    están espejadas. Se usan magnitudes absolutas `abs(...)` o distancias vectoriales 3D relativas
        //    (`d3D`, `nDist`) para garantizar neutralidad total respecto a la mano (derecha o izquierda).
        // 2. Eje Y (Vertical): 0.0 arriba, 1.0 abajo. Extensión vertical exige `tip.y < pip.y`.
        // 3. Exclusión Mutua Estricta: Cada letra exige condiciones disjuntas; en rangos ambiguos se usa
        //    una zona muerta (deadband) devolviendo `null` para garantizar Cero Falsos Positivos.

        // Puntos 2D normalizados
        val w2d = landmarks2D[0]
        val tMcp2d = landmarks2D[2]
        val tTip2d = landmarks2D[4]
        val iMcp2d = landmarks2D[5]
        val iPip2d = landmarks2D[6]
        val iTip2d = landmarks2D[8]
        val mMcp2d = landmarks2D[9]
        val mPip2d = landmarks2D[10]
        val mTip2d = landmarks2D[12]
        val rMcp2d = landmarks2D[13]
        val rPip2d = landmarks2D[14]
        val rTip2d = landmarks2D[16]
        val pMcp2d = landmarks2D[17]
        val pPip2d = landmarks2D[18]
        val pTip2d = landmarks2D[20]

        // Usa landmarks 3D en metros si están disponibles, de lo contrario cae en 2D
        val tiene3D = landmarks3D != null && landmarks3D.size >= 21

        fun getX(idx: Int) = if (tiene3D) landmarks3D!![idx].x() else landmarks2D[idx].x()
        fun getY(idx: Int) = if (tiene3D) landmarks3D!![idx].y() else landmarks2D[idx].y()
        fun getZ(idx: Int) = if (tiene3D) landmarks3D!![idx].z() else landmarks2D[idx].z()

        fun d3D(idx1: Int, idx2: Int): Float {
            val dx = getX(idx1) - getX(idx2)
            val dy = getY(idx1) - getY(idx2)
            val dz = getZ(idx1) - getZ(idx2)
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        // Tamaño de palma (Muñeca a Nudillo Medio)
        val palmSize = d3D(0, 9).coerceAtLeast(0.001f)

        fun nDist(idx1: Int, idx2: Int): Float = d3D(idx1, idx2) / palmSize
        fun ratioExt(tipIdx: Int, mcpIdx: Int): Float = d3D(tipIdx, 0) / d3D(mcpIdx, 0).coerceAtLeast(0.001f)

        // Ratios de Extensión 3D por Dedo
        val thumbExt = nDist(4, 5) > 0.38f || ratioExt(4, 2) > 1.25f

        // Detección direccional: Extendido hacia ARRIBA (Y_tip < Y_pip)
        val indexExtUp  = ratioExt(8, 5) > 1.22f && iTip2d.y() < iPip2d.y() + 0.05f
        val middleExtUp = ratioExt(12, 9) > 1.22f && mTip2d.y() < mPip2d.y() + 0.05f
        val ringExtUp   = ratioExt(16, 13) > 1.22f && rTip2d.y() < rPip2d.y() + 0.05f
        val pinkyExtUp  = ratioExt(20, 17) > 1.22f && pTip2d.y() < pPip2d.y() + 0.05f

        // Detección direccional: Dedos caídos hacia ABAJO (drapeados sobre el pulgar: M, N, Ñ)
        val indexDrapedDown = iTip2d.y() > iPip2d.y() + 0.06f || iTip2d.y() > iMcp2d.y() + 0.14f
        val middleDrapedDown = mTip2d.y() > mPip2d.y() + 0.06f || mTip2d.y() > mMcp2d.y() + 0.14f
        val ringDrapedDown = rTip2d.y() > rPip2d.y() + 0.06f || rTip2d.y() > rMcp2d.y() + 0.14f
        val pinkyDrapedDown  = ratioExt(20, 17) > 1.10f && pTip2d.y() > pMcp2d.y() + 0.06f

        // Orientación de la mano en espacio de cámara 2D
        val dirY = mMcp2d.y() - w2d.y()
        val dirX = mMcp2d.x() - w2d.x()
        val manoApuntaAbajo = dirY > 0.20f
        val manoHorizontal  = abs(dirX) > abs(dirY) + 0.08f
        val indexHorizontal = abs(iTip2d.x() - iMcp2d.x()) > abs(iTip2d.y() - iMcp2d.y())

        // Distancias relativas clave
        val thumbIndexDist  = nDist(4, 8)
        val thumbMiddleDist = nDist(4, 12)
        val indexMiddleDist = nDist(8, 12)

        val cCurvatureIndex = d3D(8, 5) / (d3D(8, 6) + d3D(6, 5)).coerceAtLeast(0.001f)
        val mCurvatureIndex = d3D(12, 9) / (d3D(12, 10) + d3D(10, 9)).coerceAtLeast(0.001f)

        // Verificación de pulgar posicionado entre índice y medio (K / P)
        val pulgarEntreDedosK = nDist(4, 10) < 0.35f || thumbMiddleDist < 0.35f || (thumbIndexDist < 0.35f && nDist(4, 9) < 0.35f)

        // Verificación de dedos cruzados (R / RR) - Múltiples métodos para cámara frontal (selfie)
        // Método 1: Inversión vectorial de yemas respecto a bases MCP
        val cruceInvertido = (iTip2d.x() - mTip2d.x()) * (iMcp2d.x() - mMcp2d.x()) < 0
        // Método 2: Contacto cercano entre yemas/PIPs cruzados (selfie: el cruce se ve como proximidad)
        val crucePorContacto = (nDist(8, 10) < 0.45f || nDist(12, 6) < 0.45f) && abs(iTip2d.x() - mTip2d.x()) < 0.09f
        // Método 3: Profundidad Z (3D) — en selfie, al cruzar, un dedo queda delante del otro
        val crucePorProfundidad = tiene3D && abs(getZ(8) - getZ(12)) > 0.012f && nDist(8, 12) < 0.28f
        val indexMiddleCrossed = cruceInvertido || crucePorContacto || crucePorProfundidad

        // Configuración de Pose R Base (compartida por R y RR, libre de restricciones de Y / rotación)
        val indexExtendedR = ratioExt(8, 5) > 1.12f
        val middleExtendedR = ratioExt(12, 9) > 1.12f
        val ringClosedR = ratioExt(16, 13) < 1.18f
        val pinkyClosedR = ratioExt(20, 17) < 1.18f
        val esPoseRBase = indexExtendedR && middleExtendedR && ringClosedR && pinkyClosedR && indexMiddleCrossed && thumbIndexDist > 0.30f

        // ── 1. MODALIDAD DE NÚMEROS (0 - 9) ──
        if (soloNumeros) {
            val totalExt = (if (indexExtUp) 1 else 0) + (if (middleExtUp) 1 else 0) +
                           (if (ringExtUp) 1 else 0) + (if (pinkyExtUp) 1 else 0) +
                           (if (thumbExt) 1 else 0)

            if (thumbIndexDist < 0.30f && middleExtUp && ringExtUp && pinkyExtUp) return ResultadoClasificacion("9", 0.95f)
            if (thumbMiddleDist < 0.30f && indexExtUp && ringExtUp && pinkyExtUp) return ResultadoClasificacion("8", 0.95f)

            return when (totalExt) {
                5 -> ResultadoClasificacion("5", 0.96f)
                4 -> ResultadoClasificacion("4", 0.95f)
                3 -> if (indexExtUp && middleExtUp && ringExtUp) ResultadoClasificacion("3", 0.94f) else null
                2 -> if (indexExtUp && middleExtUp) ResultadoClasificacion("2", 0.94f) else null
                1 -> if (indexExtUp) ResultadoClasificacion("1", 0.95f) else null
                0 -> ResultadoClasificacion("0", 0.93f)
                else -> null
            }
        }

        // ── 2. GESTOS DINÁMICOS CON MOVIMIENTO EXPLICITO (J, Ñ, LL, RR, Q) ──
        if (historialPuntos.size >= 3) {
            val movLateral = calcularMovimientoLateral(historialPuntos, 8)
            val movY = calcularMovimientoVertical(historialPuntos, 8)
            val movTrayectoria = calcularLongitudTrayectoria(historialPuntos, 8)
            val ratioOscilacion = calcularRatioOscilacion(historialPuntos, 8)
            val velocidadProm = calcularVelocidadPromedio(historialPuntos, 8)
            val movLateralReciente = calcularMovimientoLateral(historialPuntos.takeLast(15), 8)
            val movTrayectoriaReciente = calcularLongitudTrayectoria(historialPuntos.takeLast(15), 8)
            val ratioOscilacionReciente = calcularRatioOscilacion(historialPuntos.takeLast(15), 8)

            // Guarda de seguridad: Solo evaluar si el rastreo de landmarks es confiable (evita el valor centinela -1f)
            if (movTrayectoria >= 0f && velocidadProm >= 0f) {
                // J: Trazo en gancho (movimiento activo hacia abajo y adentro) con el meñique (landmark 20)
                val movTrayectoriaPinky = calcularLongitudTrayectoria(historialPuntos, 20)
                val movYPinky = calcularMovimientoVertical(historialPuntos, 20)
                val movLatPinky = calcularMovimientoLateral(historialPuntos, 20)
                val meñiqueApuntandoArriba = pinkyExtUp && pTip2d.y() < pPip2d.y() + 0.02f
                val formaI_J = meñiqueApuntandoArriba && !indexExtUp && !middleExtUp && !ringExtUp && abs(tTip2d.x() - iMcp2d.x()) < 0.16f
                // Exigir movimiento activo en gancho (bajada + curva) para evitar que temblores en I activen J
                val trazoCurvoJ = (movYPinky > 0.15f && movLatPinky > 0.08f) ||
                                  (movTrayectoriaPinky > 0.28f && movLatPinky > 0.12f)

                if (formaI_J && trazoCurvoJ && esMovimientoEstabilizado(historialPuntos, 20)) {
                    return ResultadoClasificacion("j", 0.95f)
                }
                // CORRECCIÓN: LL es una traslación lateral ÚNICA y fluida en forma de L.
                val pulgarAbiertoLL = (thumbExt || ratioExt(4, 2) > 1.20f) && abs(tTip2d.x() - iMcp2d.x()) > 0.14f
                val formaL_LL = indexExtUp && !indexHorizontal && pulgarAbiertoLL && !ringExtUp && !pinkyExtUp &&
                                (nDist(8, 5) > nDist(12, 9) + 0.12f || !middleExtUp)
                if (formaL_LL && movLateralReciente > 0.08f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("ll", 0.95f)
                }
                // RR: Configuración de R (dedos índice y medio cruzados) con desplazamiento o movimiento lateral (LSM)
                if (esPoseRBase && movLateralReciente > 0.08f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("rr", 0.95f)
                }
                // Ñ: Forma de N (dedos índice y medio caídos sobre pulgar) con movimiento oscilante ida-y-vuelta (LSM)
                val esFormaN_Dinámica = indexDrapedDown && middleDrapedDown && !ringExtUp && !pinkyExtUp && !indexHorizontal
                if (esFormaN_Dinámica && movLateralReciente > 0.08f && ratioOscilacionReciente > 1.25f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("ñ", 0.95f)
                }
                // X: Gancho del índice con movimiento lateral / vaivén (LSM)
                val indexHookX_Din = nDist(8, 5) < 0.55f && (ratioExt(8, 5) > ratioExt(12, 9) + 0.03f)
                val tresDedosPlegadosX_Din = ratioExt(12, 9) < 1.18f && ratioExt(16, 13) < 1.18f && ratioExt(20, 17) < 1.18f
                val formaX_Din = indexHookX_Din && tresDedosPlegadosX_Din && thumbIndexDist > 0.18f
                if (formaX_Din && (movLateralReciente > 0.08f || movTrayectoriaReciente > 0.10f) && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("x", 0.96f)
                }
                // Q: Gesto dinámico de pinza apuntando hacia ABAJO con movimiento (guía UTT/SEP)
                val movTrayectoriaIndexQ = calcularLongitudTrayectoria(historialPuntos.takeLast(15), 8)
                val movYIndexQ = calcularMovimientoVertical(historialPuntos.takeLast(15), 8)
                val movLatIndexQ = calcularMovimientoLateral(historialPuntos.takeLast(15), 8)
                val indiceExtendidoPinzaQ = nDist(8, 5) > 0.36f || ratioExt(8, 5) > 1.05f
                // Exigir que el índice apunte claramente hacia ABAJO
                val indiceApuntaAbajoQ = iTip2d.y() > iPip2d.y() + 0.04f && indiceExtendidoPinzaQ
                // Medio, anular y meñique deben estar cerrados (no curvados como en C)
                val tresDedosRecogidosQ = !middleExtUp && !ringExtUp && !pinkyExtUp &&
                                          ratioExt(12, 9) < 1.15f && ratioExt(16, 13) < 1.15f
                val formaQ = indiceApuntaAbajoQ && tresDedosRecogidosQ
                // Exigir movimiento DELIBERADO, no micro-temblores al acomodar la mano
                val trazoQ = movTrayectoriaIndexQ > 0.12f || (movYIndexQ > 0.08f && movLatIndexQ > 0.05f)
                val esOclosedLoop = thumbIndexDist < 0.16f

                if (formaQ && trazoQ && !esOclosedLoop && thumbIndexDist > 0.18f && indexMiddleDist > 0.24f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("q", 0.95f)
                }
                // Z: Trazo de la letra 'Z' en el aire con el dedo índice (landmark 8)
                val formaD_Z = ratioExt(8, 5) > 1.16f && ratioExt(12, 9) < 1.12f && ratioExt(16, 13) < 1.12f && ratioExt(20, 17) < 1.12f
                if (formaD_Z && esTrayectoriaZ(historialPuntos, 8) && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("z", 0.96f)
                }
                // K: Movimiento ascendente (de abajo hacia arriba) con forma de V y pulgar entre los dedos
                val pulgarEnK = thumbExt || (thumbMiddleDist < 0.32f && thumbIndexDist < 0.34f)
                val esFormaK = indexExtUp && middleExtUp && !ringExtUp && !pinkyExtUp && !indexHorizontal &&
                               pulgarEnK && indexMiddleDist > 0.15f
                if (esFormaK && (movY > 0.08f || movTrayectoria > 0.12f) && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("k", 0.96f)
                }
            }
        }

        // ── 2.5 CLASIFICACIÓN KNN PARA SEÑAS ESTÁTICAS ──
        val samples = lazyLoadDataset(context)
        if (samples.isNotEmpty()) {
            val currentVector = obtenerVectorHibrido(landmarks2D)
            if (currentVector.size == 71) {
                var bestSample: LsmSample? = null
                var minDistance = Float.MAX_VALUE

                for (sample in samples) {
                    if (sample.vector.size != 71) continue
                    var sum = 0f
                    for (i in 0 until 71) {
                        val diff = currentVector[i] - sample.vector[i]
                        sum += diff * diff
                    }
                    val distance = sqrt(sum)

                    if (distance < minDistance) {
                        minDistance = distance
                        bestSample = sample
                    }
                }

                val threshold = 1.15f
                if (bestSample != null && minDistance < threshold) {
                    var finalLabel = bestSample.label
                    
                    // Salvaguarda I vs Y (Pulgar abierto vs cerrado)
                    val thumbDistance = nDist(4, 5)
                    if (finalLabel == "y" && thumbDistance <= 0.75f) {
                        finalLabel = "i"
                    } else if (finalLabel == "i" && thumbDistance > 0.75f) {
                        finalLabel = "y"
                    }

                    // Salvaguarda U vs V (Dedos juntos vs separados)
                    val indexMiddleDist = nDist(8, 12)
                    if (finalLabel == "u" && indexMiddleDist >= 0.22f) {
                        finalLabel = "v"
                    } else if (finalLabel == "v" && indexMiddleDist < 0.18f) {
                        finalLabel = "u"
                    }

                    // Salvaguarda O (Círculo cerrado vs abierto/puño)
                    val esManoO = thumbIndexDist < 0.38f && thumbMiddleDist < 0.38f && ratioExt(8, 5) > 1.10f && ratioExt(12, 9) > 1.10f && !indexHorizontal
                    if (esManoO) {
                        finalLabel = "o"
                    } else if (finalLabel == "o") {
                        finalLabel = "c"
                    }

                    // Salvaguarda M vs N (Tres dedos drapeados vs dos dedos drapeados)
                    val ringTipKnuckleDist = nDist(16, 13)
                    if (finalLabel == "m" && ringTipKnuckleDist <= 0.30f) {
                        finalLabel = "n"
                    } else if (finalLabel == "n" && ringTipKnuckleDist > 0.30f) {
                        finalLabel = "m"
                    }

                    // Salvaguarda P vs K (Orientación hacia abajo vs hacia arriba)
                    // P exige que el dedo medio apunte claramente hacia abajo en la pantalla (tip.y > pip.y).
                    // K exige que el dedo medio apunte hacia el costado o arriba (tip.y <= pip.y).
                    val isPointingDown = mTip2d.y() > mPip2d.y() - 0.02f

                    if (finalLabel == "k" && isPointingDown) {
                        finalLabel = "p"
                    } else if (finalLabel == "p" && !isPointingDown) {
                        finalLabel = "k"
                    }

                    // Salvaguarda R (Dedos cruzados y base R completa = R, de lo contrario No R)
                    if (esPoseRBase) {
                        finalLabel = "r"
                    } else if (finalLabel == "r") {
                        finalLabel = "u"
                    }

                    val confianza = (1.0f - (minDistance / threshold)).coerceIn(0.0f, 1.0f)
                    val confianzaEscalada = 0.65f + (confianza * 0.33f)
                    if (debug) {
                        Log.d("LSM_KNN", "Seña detectada por KNN: '$finalLabel' (original: '${bestSample.label}') con distancia $minDistance (Confianza: $confianzaEscalada)")
                    }
                    return ResultadoClasificacion(finalLabel, confianzaEscalada)
                } else if (debug && bestSample != null) {
                    Log.d("LSM_KNN", "Seña más cercana '${bestSample.label}' pero distancia $minDistance supera umbral $threshold")
                }
            }
        }

        // ── 3. ABECEDARIO ESTÁTICO LSM RIGUROSO (INFOGRAFÍA UTT/SEP) ──

        // Condición de Pulgar en L (exige que el pulgar sobresalga ampliamente a un lado)
        val pulgarAbiertoL = (thumbExt || ratioExt(4, 2) > 1.20f) && abs(tTip2d.x() - iMcp2d.x()) > 0.14f

        // ── I: Solo meñique extendido (LSM - Adaptado para variaciones y desviaciones anatómicas del meñique)
        // Adaptación de accesibilidad: La extensión del meñique suele jalar ligeramente el dedo anular debido a conexiones tendinosas.
        // Se relaja la restricción del anular (nDist < 0.48f) y se exige que el meñique sea más extendido que el anular.
        val tresDedosCerradosI = !indexExtUp && !middleExtUp && !ringExtUp &&
                                 nDist(8, 5) < 0.48f && nDist(12, 9) < 0.48f &&
                                 (nDist(16, 13) < 0.48f || nDist(20, 17) > nDist(16, 13) + 0.10f)
        val meñiqueRealmenteExtendido = pinkyExtUp ||
                                        (nDist(20, 17) > 0.35f && ratioExt(20, 17) > 1.25f) ||
                                        (nDist(20, 17) > nDist(16, 13) + 0.10f)
        val meñiqueExtendidoI = meñiqueRealmenteExtendido && pTip2d.y() < pPip2d.y() + 0.06f
        val esFormaY = ratioExt(4, 2) > 1.25f && nDist(4, 20) > 0.52f && ratioExt(20, 17) > 1.25f && abs(tTip2d.x() - iMcp2d.x()) > 0.16f
        val esI = meñiqueExtendidoI && tresDedosCerradosI && !esFormaY

        if (esI) {
            return ResultadoClasificacion("i", 0.96f)
        }

        // ── T: Pulgar metido ENTRE índice y medio (guía UTT/SEP)
        // Clave: el pulgar está equidistante a ambos dedos (índice Y medio), metido en el hueco
        val dedosCerradosT = ratioExt(8, 5) < 1.20f && ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val pulgarCercaIndex_T = nDist(4, 5) < 0.30f || nDist(4, 6) < 0.30f
        val pulgarCercaMiddle_T = nDist(4, 9) < 0.24f || nDist(4, 10) < 0.24f
        val pulgarEntreAmbos_T = pulgarCercaIndex_T && pulgarCercaMiddle_T
        val pulgarLejosDAnularT = nDist(4, 14) > 0.24f
        val noEsDrapedM_N = !indexDrapedDown && iTip2d.y() <= iPip2d.y() + 0.06f
        val esT = dedosCerradosT && pulgarEntreAmbos_T && pulgarLejosDAnularT && noEsDrapedM_N && !manoApuntaAbajo
        if (esT) {
            return ResultadoClasificacion("t", 0.95f)
        }

        // ── E: Los 4 dedos flexionados en garra, pulgar DEBAJO de las yemas (guía UTT/SEP)
        val dedosCerradosE = ratioExt(8, 5) < 1.25f && ratioExt(12, 9) < 1.25f && ratioExt(16, 13) < 1.25f && ratioExt(20, 17) < 1.25f
        val dedosGarraE = nDist(8, 5) < 0.42f && nDist(12, 9) < 0.42f && nDist(16, 13) < 0.42f
        val pulgarDebajoYemasE = tTip2d.y() >= iTip2d.y() - 0.01f &&
                                 (nDist(4, 8) < 0.48f || nDist(4, 6) < 0.48f || thumbIndexDist < 0.48f) &&
                                 abs(tTip2d.x() - iMcp2d.x()) < 0.14f
        val esE = dedosCerradosE && dedosGarraE && pulgarDebajoYemasE && !manoApuntaAbajo
        if (esE) {
            return ResultadoClasificacion("e", 0.96f)
        }

        // ── S: Pulgar ENCIMA del dedo índice (guía UTT/SEP)
        val dedosCerradosS = ratioExt(8, 5) < 1.20f && ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val pulgarSobreIndex_S = nDist(4, 6) < 0.42f || nDist(4, 8) < 0.42f || nDist(4, 5) < 0.38f
        val pulgarMasCercaDeIndex = nDist(4, 6) < nDist(4, 10) + 0.08f || nDist(4, 5) < nDist(4, 9) + 0.08f
        val esS = dedosCerradosS && pulgarSobreIndex_S && pulgarMasCercaDeIndex && !indexDrapedDown && !middleDrapedDown && !manoApuntaAbajo
        if (esS) {
            return ResultadoClasificacion("s", 0.95f)
        }

        // ── A: Puño cerrado con el pulgar AL LADO del dedo índice (costado del puño)
        val dedosCerradosA = ratioExt(8, 5) < 1.20f && ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val puñoCerradoA = nDist(8, 5) < 0.52f
        val pulgarAlLadoA = abs(tTip2d.x() - iMcp2d.x()) >= 0.08f && (thumbExt || nDist(4, 5) > 0.14f)
        val esA = dedosCerradosA && puñoCerradoA && pulgarAlLadoA && !manoApuntaAbajo
        if (esA) {
            return ResultadoClasificacion("a", 0.96f)
        }

        // ── O: Yemas del índice y medio convergen con el pulgar formando un óvalo/bulbo cerrado (guía UTT/SEP)
        val yemasUnidasO = thumbIndexDist < 0.38f && thumbMiddleDist < 0.38f && nDist(4, 16) < 0.45f
        val anularMeñiqueNoEstiradosO = (!ringExtUp || ratioExt(16, 13) < 1.30f) && (!pinkyExtUp || ratioExt(20, 17) < 1.30f)
        val noEsPuñoCerradoO = nDist(8, 5) > 0.15f || nDist(12, 9) > 0.15f
        val esFormaO = yemasUnidasO && anularMeñiqueNoEstiradosO && noEsPuñoCerradoO && !manoApuntaAbajo
        if (esFormaO) {
            return ResultadoClasificacion("o", 0.96f)
        }

        // ── G: Dedo índice y pulgar extendidos en escuadra horizontal (Pistola / Pinza Horizontal LSM)
        val indiceExtendidoG = nDist(8, 5) > 0.35f
        val esG = indiceExtendidoG && thumbExt && indexHorizontal && !pinkyExtUp && !ringExtUp &&
                  (nDist(8, 5) > nDist(12, 9) + 0.12f || nDist(12, 9) < 0.45f)

        if (esG) {
            return ResultadoClasificacion("g", 0.96f)
        }

        // ── H: Dedos índice y medio extendidos juntos horizontalmente (LSM)
        // Adaptación de accesibilidad: Se compara la extensión del medio contra el anular de forma relativa.
        // Permite que el anular no esté perfectamente cerrado, siempre que el medio esté claramente más largo.
        val dosDedosExtendidosH = nDist(8, 5) > 0.35f && nDist(12, 9) > 0.28f
        val dosDedosJuntosH = indexMiddleDist < 0.32f
        val esH = dosDedosExtendidosH && dosDedosJuntosH && indexHorizontal && !pinkyExtUp &&
                  (nDist(12, 9) > nDist(16, 13) + 0.12f || nDist(16, 13) < 0.45f) &&
                  abs(nDist(8, 5) - nDist(12, 9)) < 0.20f

        if (esH) {
            return ResultadoClasificacion("h", 0.96f)
        }

        // ── M: 3 dedos (índice, medio, anular) caídos hacia abajo cubriendo el pulgar (guía UTT/SEP)
        val anularCaidoM = ringDrapedDown || (rTip2d.y() > rPip2d.y() + 0.04f && nDist(16, 13) > 0.30f)
        val tresDedosAbajoM = (indexDrapedDown || iTip2d.y() > iPip2d.y() - 0.04f) &&
                              (middleDrapedDown || mTip2d.y() > mPip2d.y() - 0.04f) &&
                              anularCaidoM && nDist(16, 13) > 0.30f
        val meñiquePlegadoM = !pinkyExtUp && (pTip2d.y() < rTip2d.y() + 0.10f || nDist(20, 17) < 0.48f)
        val esM = tresDedosAbajoM && meñiquePlegadoM && !indexHorizontal
        if (esM) {
            return ResultadoClasificacion("m", 0.96f)
        }

        // ── L: Dedo índice extendido hacia ARRIBA + Pulgar sobresaliendo ampliamente a un lado a 90° (Forma de L)
        val esL = pulgarAbiertoL && indexExtUp && cCurvatureIndex > 0.93f && !indexHorizontal && !ringExtUp && !pinkyExtUp
        if (esL) {
            return ResultadoClasificacion("l", 0.97f)
        }

        // ── N: 2 dedos (Índice y Medio) caídos hacia ABAJO sobre el pulgar (guía UTT/SEP)
        // Exclusión mutua estricta vs M: N exige que el dedo anular NO esté caído o esté plegado
        val dosDedosAbajoN = indexDrapedDown && middleDrapedDown
        val anularMeñiquePlegadosN = !ringExtUp && !pinkyExtUp && (!anularCaidoM || nDist(16, 13) <= 0.30f) && (rTip2d.y() < mTip2d.y() + 0.08f || nDist(16, 13) < 0.48f)
        val esN = dosDedosAbajoN && anularMeñiquePlegadosN && indexMiddleDist < 0.38f && !indexHorizontal
        if (esN) {
            return ResultadoClasificacion("n", 0.96f)
        }

        // ── C: Arco semicircular 3D/2D de 4 dedos curvados + pulgar oponente (Vista Frontal y Lateral)
        val aperturaC = thumbIndexDist in 0.18f..0.95f && thumbMiddleDist in 0.18f..0.95f
        val dedosCurvadosC = cCurvatureIndex < 0.98f || mCurvatureIndex < 0.98f || (ratioExt(8, 5) < 1.45f && ratioExt(12, 9) < 1.45f)
        val noEsPuñoCerrado = nDist(8, 0) > 0.38f && nDist(12, 0) > 0.38f && nDist(8, 5) > 0.22f && nDist(12, 9) > 0.42f
        val noEsO = thumbIndexDist > 0.20f || thumbMiddleDist > 0.20f
        val noEsL = !indexExtUp || ratioExt(8, 5) < 1.45f || cCurvatureIndex < 0.96f

        val esCurvaC = aperturaC && dedosCurvadosC && noEsPuñoCerrado && noEsO && noEsL &&
                       !indexDrapedDown && !manoApuntaAbajo && !pinkyExtUp

        if (esCurvaC) {
            return ResultadoClasificacion("c", 0.96f)
        }

        // ── D: Dedo índice apuntando hacia arriba con los otros 3 dedos doblados tocando el pulgar
        val esD = indexExtUp && cCurvatureIndex > 0.93f && !middleExtUp && !ringExtUp && !pinkyExtUp &&
                  !manoHorizontal && !manoApuntaAbajo && !pulgarAbiertoL
        if (esD) {
            return ResultadoClasificacion("d", 0.97f)
        }

        // ── F: Anillo entre pulgar e índice + Medio, Anular y Meñique extendidos hacia arriba
        if (thumbIndexDist < 0.26f && middleExtUp && ringExtUp && pinkyExtUp) {
            return ResultadoClasificacion("f", 0.95f)
        }

        // ── B: 4 dedos extendidos perfectamente RECTOS hacia ARRIBA, PLANOS y JUNTOS
        val cuatroDedosRectos3D = ratioExt(8, 5) > 1.35f &&
                                  ratioExt(12, 9) > 1.35f &&
                                  ratioExt(16, 13) > 1.35f &&
                                  ratioExt(20, 17) > 1.35f &&
                                  cCurvatureIndex > 0.90f &&
                                  nDist(8, 12) < 0.42f &&
                                  nDist(12, 16) < 0.42f
        if (indexExtUp && middleExtUp && ringExtUp && pinkyExtUp && cuatroDedosRectos3D) {
            return ResultadoClasificacion("b", 0.94f)
        }

        // ── W: Índice, Medio y Anular extendidos (Forma de W / 3) + Meñique doblado (guía UTT/SEP)
        val indexExtendedW = ratioExt(8, 5) > 1.15f
        val middleExtendedW = ratioExt(12, 9) > 1.15f
        val ringExtendedW = ratioExt(16, 13) > 1.15f
        val pinkyClosedW = ratioExt(20, 17) < 1.15f
        val esW = indexExtendedW && middleExtendedW && ringExtendedW && pinkyClosedW
        if (esW) {
            return ResultadoClasificacion("w", 0.95f)
        }

        // ── Y: Pulgar y Meñique totalmente extendidos a los lados opuestos (Hang loose / Llamada o chido)
        val indexClosedY = ratioExt(8, 5) < 1.15f
        val middleClosedY = ratioExt(12, 9) < 1.15f
        val ringClosedY = ratioExt(16, 13) < 1.15f
        val thumbExtendedY = ratioExt(4, 2) > 1.18f || thumbExt
        val pinkyExtendedY = ratioExt(20, 17) > 1.18f || pinkyExtUp
        val spanY = nDist(4, 20) > 0.48f
        val esY = indexClosedY && middleClosedY && ringClosedY && thumbExtendedY && pinkyExtendedY && spanY
        if (esY) {
            return ResultadoClasificacion("y", 0.95f)
        }

        // ── P: Dedo índice extendido apuntando hacia ABAJO, pulgar al costado, medio/anular/meñique plegados al puño (guía UTT/SEP)
        val indiceAbajoP = (iTip2d.y() > iMcp2d.y() + 0.04f || dirY > 0.12f) && ratioExt(8, 5) > 1.05f
        val pulgarAlLadoP = thumbExt || abs(tTip2d.x() - iMcp2d.x()) > 0.06f || thumbIndexDist < 0.50f || nDist(4, 5) < 0.48f
        val tresDedosRecogidosP = ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val esP = (indiceAbajoP || indexDrapedDown) && pulgarAlLadoP && tresDedosRecogidosP && !ringExtUp && !pinkyExtUp && !anularCaidoM
        if (esP) {
            return ResultadoClasificacion("p", 0.95f)
        }

        // ── R: Índice y Medio cruzados uno sobre otro (guía UTT/SEP)
        if (esPoseRBase) {
            return ResultadoClasificacion("r", 0.96f)
        }

        // ── K: Índice arriba/diagonal, medio al frente/arriba, pulgar colocado entre ambos dedos (guía UTT/SEP)
        val indiceExtenddoK = indexExtUp || (ratioExt(8, 5) > 1.20f && iTip2d.y() < iMcp2d.y() + 0.05f)
        val esK = indiceExtenddoK && (middleExtUp || ratioExt(12, 9) > 1.05f) && !ringExtUp && !pinkyExtUp &&
                  pulgarEntreDedosK && indexMiddleDist > 0.12f
        if (esK) {
            return ResultadoClasificacion("k", 0.96f)
        }

        // ── U: Índice y Medio extendidos PEGADOS / PARALELOS (guía UTT/SEP)
        // Exclusión mutua con V: Zona muerta entre 0.18f y 0.22f devuelve null en lugar de clasificar mal
        val indexExtendedU = ratioExt(8, 5) > 1.15f
        val middleExtendedU = ratioExt(12, 9) > 1.15f
        val ringClosedU = ratioExt(16, 13) < 1.15f
        val pinkyClosedU = ratioExt(20, 17) < 1.15f
        val dedosParalelosSinCruzar = !indexMiddleCrossed && (nDist(8, 10) > 0.14f || nDist(12, 6) > 0.14f)
        val dedosJuntosU = indexMiddleDist < 0.18f
        val esU = indexExtendedU && middleExtendedU && ringClosedU && pinkyClosedU &&
                  dedosJuntosU && dedosParalelosSinCruzar && !pulgarEntreDedosK
        if (esU) {
            return ResultadoClasificacion("u", 0.95f)
        }

        // ── V: Índice y Medio extendidos SEPARADOS en "V" (guía UTT/SEP)
        val indexExtendedV = ratioExt(8, 5) > 1.15f
        val middleExtendedV = ratioExt(12, 9) > 1.15f
        val ringClosedV = ratioExt(16, 13) < 1.15f
        val pinkyClosedV = ratioExt(20, 17) < 1.15f
        val dedosSeparadosV = indexMiddleDist >= 0.22f
        val esV = indexExtendedV && middleExtendedV && ringClosedV && pinkyClosedV &&
                  dedosSeparadosV && !indexMiddleCrossed && !pulgarEntreDedosK
        if (esV) {
            return ResultadoClasificacion("v", 0.95f)
        }



        // ── X: Dedo índice encorvado en forma de gancho o gatillo (guía UTT/SEP)
        val indexHookX = (nDist(8, 5) < 0.52f || ratioExt(8, 5) > 1.05f) && (ratioExt(8, 5) > ratioExt(12, 9) + 0.03f || nDist(8, 5) > nDist(12, 9) + 0.03f)
        val tresDedosPlegadosX = ratioExt(12, 9) < 1.18f && ratioExt(16, 13) < 1.18f && ratioExt(20, 17) < 1.18f
        val esX = indexHookX && tresDedosPlegadosX && thumbIndexDist > 0.18f && !manoApuntaAbajo
        if (esX) {
            return ResultadoClasificacion("x", 0.95f)
        }



        // ── INSTRUMENTACIÓN DE DIAGNÓSTICO TEMPORAL ──
        // Imprime valores numéricos clave para las letras que no se detectan correctamente,
        // permitiendo calibrar umbrales con datos reales en lugar de a ciegas.
        // NO se instrumentan A, B, D, E, F, J, K — esas ya funcionan correctamente.
        if (debug) {
            val tag = "LSM_DEBUG"

            // ── I: Solo meñique extendido
            Log.d(tag, "--- I ---")
            Log.d(tag, "  tresDedosCerradosI=$tresDedosCerradosI (nDist(8,5)=${nDist(8,5)}, nDist(12,9)=${nDist(12,9)}, nDist(16,13)=${nDist(16,13)})")
            Log.d(tag, "  meñiqueRealmenteExtendido=$meñiqueRealmenteExtendido (nDist(20,17)=${nDist(20,17)}, ratioExt(20,17)=${ratioExt(20,17)})")
            Log.d(tag, "  meñiqueExtendidoI=$meñiqueExtendidoI, esFormaY=$esFormaY, esI=$esI")

            // ── LL: (dinámico) índice+pulgar extendidos + traslación lateral única (sin requisito de oscilación)
            Log.d(tag, "--- LL ---")
            Log.d(tag, "  indexExtUp=$indexExtUp, thumbExt=$thumbExt, middleExtUp=$middleExtUp, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            if (historialPuntos.size >= 3) {
                val movLat = calcularMovimientoLateral(historialPuntos, 8)
                Log.d(tag, "  movLateral=$movLat (umbral>0.18)")
            } else {
                Log.d(tag, "  historialPuntos.size=${historialPuntos.size} (<3, no evaluado)")
            }

            // ── M: 3 dedos caídos sobre pulgar
            Log.d(tag, "--- M ---")
            Log.d(tag, "  tresDedosAbajoM=$tresDedosAbajoM, meñiquePlegadoM=$meñiquePlegadoM, esM=$esM")

            // ── N: 2 dedos caídos sobre pulgar
            Log.d(tag, "--- N ---")
            Log.d(tag, "  dosDedosAbajoN=$dosDedosAbajoN, anularMeñiquePlegadosN=$anularMeñiquePlegadosN, esN=$esN")

            // ── Ñ: (dinámico) 2 dedos caídos + movimiento lateral oscilante
            Log.d(tag, "--- Ñ ---")
            Log.d(tag, "  indexDrapedDown=$indexDrapedDown, middleDrapedDown=$middleDrapedDown, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            if (historialPuntos.size >= 3) {
                val movLat = calcularMovimientoLateral(historialPuntos, 8)
                val ratOsc = calcularRatioOscilacion(historialPuntos, 8)
                Log.d(tag, "  movLateral=$movLat (umbral>0.18), ratioOscilacion=$ratOsc (umbral>1.3)")
            }

            // ── P: Estructura tipo K pero horizontal (índice al frente)
            Log.d(tag, "--- P ---")
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            Log.d(tag, "  manoHorizontal=$manoHorizontal, indexMiddleCrossed=$indexMiddleCrossed, indexMiddleDist=$indexMiddleDist (umbral>0.15)")
            Log.d(tag, "  esP=${indexExtUp && middleExtUp && !ringExtUp && !pinkyExtUp && manoHorizontal && !indexMiddleCrossed && indexMiddleDist > 0.15f}")

            // ── Q: (dinámico) mano apuntando abajo + movimiento
            Log.d(tag, "--- Q ---")
            Log.d(tag, "  manoApuntaAbajo=$manoApuntaAbajo (dirY=$dirY)")
            if (historialPuntos.size >= 3) {
                val movTray = calcularLongitudTrayectoria(historialPuntos, 8)
                val movLat = calcularMovimientoLateral(historialPuntos, 8)
                Log.d(tag, "  movTrayectoria=$movTray (umbral>0.20), movLateral=$movLat (umbral>0.15)")
            }

            // ── R: Índice y medio cruzados
            Log.d(tag, "--- R ---")
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            Log.d(tag, "  indexMiddleCrossed=$indexMiddleCrossed (nDist(8,12)=${nDist(8,12)})")

            // ── RR: (dinámico) dedos cruzados + movimiento lateral oscilante
            Log.d(tag, "--- RR ---")
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp")
            Log.d(tag, "  indexMiddleCrossed=$indexMiddleCrossed, indexMiddleDist=$indexMiddleDist")
            if (historialPuntos.size >= 3) {
                val movLat = calcularMovimientoLateral(historialPuntos, 8)
                val ratOsc = calcularRatioOscilacion(historialPuntos, 8)
                Log.d(tag, "  movLateral=$movLat (umbral>0.18), ratioOscilacion=$ratOsc (umbral>1.3)")
            }

            // ── S: Pulgar cruzado por enfrente del puño
            Log.d(tag, "--- S ---")
            val dbgEsPuño = !indexExtUp && !middleExtUp && !ringExtUp && !pinkyExtUp && nDist(8, 5) < 0.48f
            Log.d(tag, "  dbgEsPuño=$dbgEsPuño")
            val dbgPulgarEnFrenteS = nDist(4, 6) < 0.42f || nDist(4, 10) < 0.42f || nDist(4, 14) < 0.42f || nDist(4, 8) < 0.40f || nDist(4, 5) < 0.38f || nDist(4, 9) < 0.40f
            val dbgPulgarEncimaDedosS = tTip2d.y() < iPip2d.y() + 0.12f && tTip2d.y() > iMcp2d.y() - 0.15f
            val dbgPulgarCruzado = abs(tTip2d.x() - iMcp2d.x()) < 0.25f || abs(tTip2d.x() - mMcp2d.x()) < 0.22f
            Log.d(tag, "  dbgPulgarEnFrenteS=$dbgPulgarEnFrenteS (dIndexPip=${nDist(4,6)}, dMiddlePip=${nDist(4,10)}, dRingPip=${nDist(4,14)})")
            Log.d(tag, "  dbgPulgarEncimaDedosS=$dbgPulgarEncimaDedosS (tTip.y=${tTip2d.y()}, iPip.y=${iPip2d.y()}, iMcp.y=${iMcp2d.y()})")
            Log.d(tag, "  dbgPulgarCruzado=$dbgPulgarCruzado")

            // ── T: Pulgar incrustado entre índice y medio
            Log.d(tag, "--- T ---")
            Log.d(tag, "  dbgEsPuño=$dbgEsPuño")
            val dbgIncrustadoT = (nDist(4, 5) < 0.22f || nDist(4, 6) < 0.22f) && (nDist(4, 9) < 0.24f || nDist(4, 10) < 0.24f)
            val dbgNoAnularT = nDist(4, 14) > 0.30f && nDist(4, 13) > 0.28f
            Log.d(tag, "  dbgIncrustadoT=$dbgIncrustadoT (dIndexMcp=${nDist(4,5)}, dMiddleMcp=${nDist(4,9)})")
            Log.d(tag, "  dbgNoAnularT=$dbgNoAnularT (dRingPip=${nDist(4,14)}), esT=${dbgIncrustadoT && dbgNoAnularT}")

            // ── U: Índice y medio pegados/paralelos
            Log.d(tag, "--- U ---")
            val dbgU_Paralelos = !cruceInvertido && (nDist(8, 10) > 0.14f || nDist(12, 6) > 0.14f)
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp, indexMiddleDist=$indexMiddleDist (umbral<0.25)")
            Log.d(tag, "  cruceInvertido=$cruceInvertido, dbgU_Paralelos=$dbgU_Paralelos")

            // ── V: Índice y medio separados en V
            Log.d(tag, "--- V ---")
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            Log.d(tag, "  indexMiddleDist=$indexMiddleDist (umbral>=0.25), manoHorizontal=$manoHorizontal, manoApuntaAbajo=$manoApuntaAbajo")

            // ── W: Índice, medio y anular extendidos
            Log.d(tag, "--- W ---")
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            Log.d(tag, "  manoHorizontal=$manoHorizontal")

            // ── X: Índice encorvado en gancho
            Log.d(tag, "--- X ---")
            val indexHookedDbg = nDist(8, 6) < 0.35f || nDist(8, 5) < 0.40f
            Log.d(tag, "  indexHooked=$indexHookedDbg (nDist(8,6)=${nDist(8,6)}, nDist(8,5)=${nDist(8,5)})")
            Log.d(tag, "  middleExtUp=$middleExtUp, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            Log.d(tag, "  ratioExt(6,5)=${ratioExt(6,5)} (umbral>0.95)")

            // ── Y: Pulgar y meñique extendidos
            Log.d(tag, "--- Y ---")
            val dbgMeñiqueExt = pinkyExtUp || ratioExt(20, 17) > 1.25f || nDist(20, 17) > 0.32f
            Log.d(tag, "  thumbExt=$thumbExt, meñiqueExtensoTel=$dbgMeñiqueExt (ratioExt(20,17)=${ratioExt(20,17)}, nDist(20,17)=${nDist(20,17)})")
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp, ringExtUp=$ringExtUp")

            // ── Z: (dinámico) Trazo en Z
            Log.d(tag, "--- Z ---")
            Log.d(tag, "  indexExtUp=$indexExtUp, middleExtUp=$middleExtUp, ringExtUp=$ringExtUp, pinkyExtUp=$pinkyExtUp")
            if (historialPuntos.size >= 3) {
                val movTray = calcularLongitudTrayectoria(historialPuntos, 8)
                val movLat = calcularMovimientoLateral(historialPuntos, 8)
                val movY = calcularMovimientoVertical(historialPuntos, 8)
                Log.d(tag, "  movTrayectoria=$movTray (umbral>0.42), movLateral=$movLat (umbral>0.20), movY=$movY (umbral>0.15)")
            }

            Log.d(tag, "=== FIN DEBUG: Ninguna letra coincidió ===")
        }

        // Si la seña no coincide estrictamente con ningún patrón, NO adivinar ni devolver nada
        return null
    }

    // ─── UTILIDADES MATEMÁTICAS Y MEDIDA DE MOVIMIENTO ───
    private fun dist3D(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
        val dx = p1.x() - p2.x()
        val dy = p1.y() - p2.y()
        val dz = p1.z() - p2.z()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calcula la escala de referencia espacial de la mano en un fotograma dado.
     * Mide la distancia 3D entre la muñeca (landmark 0) y el MCP del dedo medio (landmark 9).
     * Retorna un valor mínimo de seguridad (0.01f) para evitar división por cero.
     */
    private fun calcularEscalaReferencia(landmarks: List<NormalizedLandmark>): Float {
        if (landmarks.size <= 9) return 0.01f
        val d = dist3D(landmarks[0], landmarks[9])
        return if (d <= 0f) 0.01f else d.coerceAtLeast(0.01f)
    }

    /**
     * Calcula la escala promedio de la mano a lo largo del historial de fotogramas.
     * Amortigua temblores o pequeñas variaciones de profundidad durante el movimiento del gesto.
     */
    private fun calcularEscalaPromedio(historial: List<List<NormalizedLandmark>>): Float {
        val escalas = historial.mapNotNull { if (it.size > 9) calcularEscalaReferencia(it) else null }
        if (escalas.isEmpty()) return 1.0f
        return (escalas.sum() / escalas.size).coerceAtLeast(0.01f)
    }

    private fun calcularMovimientoLateral(
        historial: List<List<NormalizedLandmark>>,
        indexPunto: Int,
        normalizada: Boolean = false
    ): Float {
        if (historial.size < 3) return 0f
        val xs = historial.mapNotNull { if (it.size > indexPunto) it[indexPunto].x() else null }
        if (xs.isEmpty()) return 0f
        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 0f
        val rangoRaw = maxX - minX
        if (!normalizada) return rangoRaw
        val escala = calcularEscalaPromedio(historial)
        return rangoRaw / escala
    }

    private fun calcularMovimientoVertical(
        historial: List<List<NormalizedLandmark>>,
        indexPunto: Int,
        normalizada: Boolean = false
    ): Float {
        if (historial.size < 3) return 0f
        val ys = historial.mapNotNull { if (it.size > indexPunto) it[indexPunto].y() else null }
        if (ys.isEmpty()) return 0f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 0f
        val rangoRaw = maxY - minY
        if (!normalizada) return rangoRaw
        val escala = calcularEscalaPromedio(historial)
        return rangoRaw / escala
    }

    /**
     * Unifica el cálculo de longitud de trayectoria en espacio 3D reutilizando dist3D(p1, p2).
     * Maneja la robustez ante landmarks faltantes: si más del 30% de los pares consecutivos pierden el landmark,
     * retorna un valor centinela (-1f) para notificar al llamador que el rastreo no fue confiable.
     */
    private fun calcularLongitudTrayectoria(
        historial: List<List<NormalizedLandmark>>,
        indexPunto: Int,
        normalizada: Boolean = false
    ): Float {
        if (historial.size < 3) return 0f
        var distAcum = 0f
        var paresValidos = 0
        val totalPares = historial.size - 1

        for (i in 0 until totalPares) {
            val list1 = historial[i]
            val list2 = historial[i + 1]
            if (list1.size > indexPunto && list2.size > indexPunto) {
                val p1 = list1[indexPunto]
                val p2 = list2[indexPunto]
                distAcum += dist3D(p1, p2)
                paresValidos++
            }
        }

        // Evaluación de robustez: Si se pierde el landmark en > 30% de los pares de frames, retorna centinela -1f
        val proporcionFaltantes = (totalPares - paresValidos).toFloat() / totalPares.toFloat()
        if (proporcionFaltantes > 0.30f) {
            return -1f
        }

        if (!normalizada) return distAcum
        val escala = calcularEscalaPromedio(historial)
        return distAcum / escala
    }

    /**
     * Feature de Ratio de Oscilación: (Longitud de Trayectoria) / (Rango Combinado Euclidiano).
     * Permite diferenciar gestos oscilantes (movimiento alternado/ida y vuelta) de gestos direccionales continuos.
     * Retorna 0f si el rango acumulado es <= 0.001f para evitar división por cero, NaN o Infinity.
     */
    private fun calcularRatioOscilacion(historial: List<List<NormalizedLandmark>>, indexPunto: Int): Float {
        val rangoX = calcularMovimientoLateral(historial, indexPunto, normalizada = true)
        val rangoY = calcularMovimientoVertical(historial, indexPunto, normalizada = true)
        val rangoCombinado = sqrt(rangoX * rangoX + rangoY * rangoY)
        if (rangoCombinado <= 0.001f) return 0f
        val tray = calcularLongitudTrayectoria(historial, indexPunto, normalizada = true)
        if (tray <= 0f) return 0f
        return tray / rangoCombinado
    }

    /**
     * Velocidad Promedio Normalizada en el tiempo: Distancia Acumulada / Número de Fotogramas del Historial.
     * Independiza la característica de movimiento del framerate (FPS) de la cámara del dispositivo.
     */
    private fun calcularVelocidadPromedio(historial: List<List<NormalizedLandmark>>, indexPunto: Int): Float {
        if (historial.size < 3) return 0f
        val distAcum = calcularLongitudTrayectoria(historial, indexPunto, normalizada = true)
        if (distAcum < 0f) return -1f
        return distAcum / historial.size.toFloat()
    }

    /**
     * Función de Debounce / Estabilización para Gestos Dinámicos.
     * Evalúa la variación en los últimos 2-3 fotogramas para asegurar que el punto de referencia
     * se haya asentado o que la trayectoria intencional haya concluido su aceleración de traslado libre.
     */
    private fun esMovimientoEstabilizado(
        historial: List<List<NormalizedLandmark>>,
        landmarkIndex: Int
    ): Boolean {
        if (historial.size < 3) return true
        val ultimosPuntos = historial.takeLast(3).mapNotNull { it.getOrNull(landmarkIndex) }
        if (ultimosPuntos.size < 3) return true

        val p1 = ultimosPuntos[0]
        val p2 = ultimosPuntos[1]
        val p3 = ultimosPuntos[2]

        val delta1 = kotlin.math.hypot(p2.x() - p1.x(), p2.y() - p1.y())
        val delta2 = kotlin.math.hypot(p3.x() - p2.x(), p3.y() - p2.y())

        return delta1 < 0.08f && delta2 < 0.08f
    }

    private fun esTrayectoriaZ(historial: List<List<NormalizedLandmark>>, indexPunto: Int): Boolean {
        if (historial.size < 12) return false
        val puntos = historial.mapNotNull { it.getOrNull(indexPunto) }
        if (puntos.size < 12) return false

        // Evitar falsos positivos por micro-movimientos o ruido estático
        val dTot = calcularLongitudTrayectoria(historial, indexPunto, normalizada = true)
        if (dTot < 0.18f) return false

        // Dividimos en 3 segmentos temporales
        val n = puntos.size
        val seg1 = puntos.subList(0, n / 3)
        val seg2 = puntos.subList(n / 3, (2 * n) / 3)
        val seg3 = puntos.subList((2 * n) / 3, n)

        // Deltas de cada segmento
        val dx1 = seg1.last().x() - seg1.first().x()
        val dx2 = seg2.last().x() - seg2.first().x()
        val dy2 = seg2.last().y() - seg2.first().y()
        val dx3 = seg3.last().x() - seg3.first().x()

        // 1. Z dibujada de izquierda a derecha (cámara normal):
        //   - seg1: derecha (dx1 > 0)
        //   - seg2: diagonal abajo-izquierda (dx2 < 0, dy2 > 0)
        //   - seg3: derecha (dx3 > 0)
        val cumpleNoEspejado = dx1 > 0.02f && dx2 < -0.02f && dy2 > 0.03f && dx3 > 0.02f

        // 2. Z dibujada en pantalla espejada (vista selfie típica):
        //   - seg1: izquierda (dx1 < 0)
        //   - seg2: diagonal abajo-derecha (dx2 > 0, dy2 > 0)
        //   - seg3: izquierda (dx3 < 0)
        val cumpleEspejado = dx1 < -0.02f && dx2 > 0.02f && dy2 > 0.03f && dx3 < -0.02f

        return cumpleNoEspejado || cumpleEspejado
    }
}
