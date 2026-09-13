package com.example.nutriia.accesibilidad

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sqrt

data class NormalizedPoint3D(
    val x: Float,
    val y: Float,
    val z: Float = 0f
)

data class ResultadoClasificacion(
    val letra: String,
    val confianza: Float
)

data class LsmSample(
    val label: String,
    val vector: List<Float>
)

data class LsmDataset(
    val samples: List<LsmSample>
)

/**
 * Clasificador Anatómico Relacional 3D Riguroso LSM Multiplataforma (Abecedario A-Z + Señas Comunicativas).
 * Funciona de forma idéntica en Android (MediaPipe) e iOS (Apple Vision Framework / Neural Engine).
 */
object SignLanguageClassifier {

    var datasetStatic: List<LsmSample>? = null

    /**
     * Calcula el ángulo 3D articular entre 3 puntos óseos (A -> B -> C) usando el producto punto de vectores.
     */
    fun anguloArticular3D(a: NormalizedPoint3D, b: NormalizedPoint3D, c: NormalizedPoint3D): Float {
        val v1x = a.x - b.x
        val v1y = a.y - b.y
        val v1z = a.z - b.z

        val v2x = c.x - b.x
        val v2y = c.y - b.y
        val v2z = c.z - b.z

        val dot = v1x * v2x + v1y * v2y + v1z * v2z
        val mag1 = sqrt(v1x * v1x + v1y * v1y + v1z * v1z).coerceAtLeast(0.0001f)
        val mag2 = sqrt(v2x * v2x + v2y * v2y + v2z * v2z).coerceAtLeast(0.0001f)

        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1.0f, 1.0f)
        return (acos(cosTheta.toDouble()) * 180.0 / kotlin.math.PI).toFloat()
    }

    fun d3D(p1: NormalizedPoint3D, p2: NormalizedPoint3D): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        val dz = p1.z - p2.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun obtenerVectorHibrido(landmarks: List<NormalizedPoint3D>): List<Float> {
        if (landmarks.size < 21) return emptyList()

        val wrist = landmarks[0]
        val wX = wrist.x
        val wY = wrist.y
        val wZ = wrist.z

        val dx9 = landmarks[9].x - wX
        val dy9 = landmarks[9].y - wY
        val dz9 = landmarks[9].z - wZ
        val palmSize = sqrt(dx9 * dx9 + dy9 * dy9 + dz9 * dz9).coerceAtLeast(0.001f)

        val vector = ArrayList<Float>(71)

        for (lm in landmarks) {
            vector.add((lm.x - wX) / palmSize)
            vector.add((lm.y - wY) / palmSize)
            vector.add((lm.z - wZ) / palmSize)
        }

        fun nDist(idx1: Int, idx2: Int): Float = d3D(landmarks[idx1], landmarks[idx2]) / palmSize

        vector.add(nDist(4, 8))   // Pulgar-Índice
        vector.add(nDist(8, 12))  // Índice-Medio
        vector.add(nDist(12, 16)) // Medio-Anular
        vector.add(nDist(16, 20)) // Anular-Meñique
        vector.add(nDist(4, 12))  // Pulgar-Medio

        vector.add(nDist(4, 5))   // Pulgar-Nudillo Índice (MCP)
        vector.add(nDist(4, 9))   // Pulgar-Nudillo Medio (MCP)

        val angle = atan2(landmarks[9].y - wY, landmarks[9].x - wX)
        vector.add(angle)

        return vector
    }

    fun clasificarConConfianza(
        landmarks: List<NormalizedPoint3D>,
        soloNumeros: Boolean = false,
        esCampoFecha: Boolean = false,
        historialPuntos: List<List<NormalizedPoint3D>> = emptyList()
    ): ResultadoClasificacion? {
        if (landmarks.size < 21) return null

        val w2d = landmarks[0]
        val tMcp2d = landmarks[2]
        val tTip2d = landmarks[4]
        val iMcp2d = landmarks[5]
        val iPip2d = landmarks[6]
        val iTip2d = landmarks[8]
        val mMcp2d = landmarks[9]
        val mPip2d = landmarks[10]
        val mTip2d = landmarks[12]
        val rMcp2d = landmarks[13]
        val rPip2d = landmarks[14]
        val rTip2d = landmarks[16]
        val pMcp2d = landmarks[17]
        val pPip2d = landmarks[18]
        val pTip2d = landmarks[20]

        fun getX(idx: Int) = landmarks[idx].x
        fun getY(idx: Int) = landmarks[idx].y
        fun getZ(idx: Int) = landmarks[idx].z

        fun d3D(idx1: Int, idx2: Int): Float {
            val dx = getX(idx1) - getX(idx2)
            val dy = getY(idx1) - getY(idx2)
            val dz = getZ(idx1) - getZ(idx2)
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        val palmSize = d3D(0, 9).coerceAtLeast(0.001f)

        fun nDist(idx1: Int, idx2: Int): Float = d3D(idx1, idx2) / palmSize
        fun ratioExt(tipIdx: Int, mcpIdx: Int): Float = d3D(tipIdx, 0) / d3D(mcpIdx, 0).coerceAtLeast(0.001f)

        val anguloIndex  = anguloArticular3D(landmarks[5], landmarks[6], landmarks[8])
        val anguloMiddle = anguloArticular3D(landmarks[9], landmarks[10], landmarks[12])
        val anguloRing   = anguloArticular3D(landmarks[13], landmarks[14], landmarks[16])
        val anguloPinky  = anguloArticular3D(landmarks[17], landmarks[18], landmarks[20])

        val thumbExt = nDist(4, 5) > 0.32f || ratioExt(4, 2) > 1.15f

        val indexExtUp  = (anguloIndex > 135f || ratioExt(8, 5) > 1.10f) && iTip2d.y < iPip2d.y + 0.08f
        val middleExtUp = (anguloMiddle > 135f || ratioExt(12, 9) > 1.10f) && mTip2d.y < mPip2d.y + 0.08f
        val ringExtUp   = (anguloRing > 135f || ratioExt(16, 13) > 1.10f) && rTip2d.y < rPip2d.y + 0.08f
        val pinkyExtUp  = (anguloPinky > 135f || ratioExt(20, 17) > 1.10f) && pTip2d.y < pPip2d.y + 0.08f

        val indexDrapedDown = iTip2d.y > iPip2d.y + 0.03f || iTip2d.y > iMcp2d.y + 0.10f
        val middleDrapedDown = mTip2d.y > mPip2d.y + 0.03f || mTip2d.y > mMcp2d.y + 0.10f
        val ringDrapedDown = rTip2d.y > rPip2d.y + 0.03f || rTip2d.y > rMcp2d.y + 0.10f
        val pinkyDrapedDown  = ratioExt(20, 17) > 1.05f && pTip2d.y > pMcp2d.y + 0.04f

        val dirY = mMcp2d.y - w2d.y
        val dirX = mMcp2d.x - w2d.x
        val manoApuntaAbajo = dirY > 0.20f
        val manoHorizontal  = abs(dirX) > abs(dirY) + 0.08f
        val indexHorizontal = abs(iTip2d.x - iMcp2d.x) > abs(iTip2d.y - iMcp2d.y)

        val thumbIndexDist  = nDist(4, 8)
        val thumbMiddleDist = nDist(4, 12)
        val indexMiddleDist = nDist(8, 12)

        val cCurvatureIndex = d3D(8, 5) / (d3D(8, 6) + d3D(6, 5)).coerceAtLeast(0.001f)
        val mCurvatureIndex = d3D(12, 9) / (d3D(12, 10) + d3D(10, 9)).coerceAtLeast(0.001f)

        val pulgarEntreDedosK = nDist(4, 10) < 0.35f || thumbMiddleDist < 0.35f || (thumbIndexDist < 0.35f && nDist(4, 9) < 0.35f)

        val cruceInvertido = (iTip2d.x - mTip2d.x) * (iMcp2d.x - mMcp2d.x) < 0
        val crucePorContacto = (nDist(8, 10) < 0.45f || nDist(12, 6) < 0.45f) && abs(iTip2d.x - mTip2d.x) < 0.09f
        val crucePorProfundidad = abs(getZ(8) - getZ(12)) > 0.012f && nDist(8, 12) < 0.28f
        val indexMiddleCrossed = cruceInvertido || crucePorContacto || crucePorProfundidad

        val indexExtendedR = ratioExt(8, 5) > 1.12f
        val middleExtendedR = ratioExt(12, 9) > 1.12f
        val ringClosedR = ratioExt(16, 13) < 1.18f
        val pinkyClosedR = ratioExt(20, 17) < 1.18f
        val esPoseRBase = indexExtendedR && middleExtendedR && ringClosedR && pinkyClosedR && indexMiddleCrossed && thumbIndexDist > 0.30f

        // 1. NÚMEROS (0-9)
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

        // 2. DINÁMICAS (J, Ñ, LL, RR, Q, Z, K)
        if (historialPuntos.size >= 3) {
            val movY = calcularMovimientoVertical(historialPuntos, 8)
            val movTrayectoria = calcularLongitudTrayectoria(historialPuntos, 8)
            val velocidadProm = calcularVelocidadPromedio(historialPuntos, 8)
            val movLateralReciente = calcularMovimientoLateral(historialPuntos.takeLast(15), 8)
            val movTrayectoriaReciente = calcularLongitudTrayectoria(historialPuntos.takeLast(15), 8)
            val ratioOscilacionReciente = calcularRatioOscilacion(historialPuntos.takeLast(15), 8)

            if (movTrayectoria >= 0f && velocidadProm >= 0f) {
                val movTrayectoriaPinky = calcularLongitudTrayectoria(historialPuntos, 20)
                val movYPinky = calcularMovimientoVertical(historialPuntos, 20)
                val movLatPinky = calcularMovimientoLateral(historialPuntos, 20)
                val meñiqueApuntandoArriba = pinkyExtUp && pTip2d.y < pPip2d.y + 0.02f
                val formaI_J = meñiqueApuntandoArriba && !indexExtUp && !middleExtUp && !ringExtUp && abs(tTip2d.x - iMcp2d.x) < 0.16f
                val trazoCurvoJ = (movYPinky > 0.15f && movLatPinky > 0.08f) ||
                                  (movTrayectoriaPinky > 0.28f && movLatPinky > 0.12f)

                if (formaI_J && trazoCurvoJ && esMovimientoEstabilizado(historialPuntos, 20)) {
                    return ResultadoClasificacion("j", 0.95f)
                }

                val pulgarAbiertoLL = (thumbExt || ratioExt(4, 2) > 1.20f) && abs(tTip2d.x - iMcp2d.x) > 0.14f
                val formaL_LL = indexExtUp && !indexHorizontal && pulgarAbiertoLL && !ringExtUp && !pinkyExtUp &&
                                (nDist(8, 5) > nDist(12, 9) + 0.12f || !middleExtUp)
                if (formaL_LL && movLateralReciente > 0.08f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("ll", 0.95f)
                }

                if (esPoseRBase && movLateralReciente > 0.08f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("rr", 0.95f)
                }

                val esFormaN_Dinámica = indexDrapedDown && middleDrapedDown && !ringExtUp && !pinkyExtUp && !indexHorizontal
                if (esFormaN_Dinámica && movLateralReciente > 0.08f && ratioOscilacionReciente > 1.25f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("ñ", 0.95f)
                }

                val indexHookX_Din = nDist(8, 5) < 0.55f && (ratioExt(8, 5) > ratioExt(12, 9) + 0.03f)
                val tresDedosPlegadosX_Din = ratioExt(12, 9) < 1.18f && ratioExt(16, 13) < 1.18f && ratioExt(20, 17) < 1.18f
                val formaX_Din = indexHookX_Din && tresDedosPlegadosX_Din && thumbIndexDist > 0.18f
                if (formaX_Din && (movLateralReciente > 0.08f || movTrayectoriaReciente > 0.10f) && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("x", 0.96f)
                }

                val movTrayectoriaIndexQ = calcularLongitudTrayectoria(historialPuntos.takeLast(15), 8)
                val movYIndexQ = calcularMovimientoVertical(historialPuntos.takeLast(15), 8)
                val movLatIndexQ = calcularMovimientoLateral(historialPuntos.takeLast(15), 8)
                val indiceExtendidoPinzaQ = nDist(8, 5) > 0.36f || ratioExt(8, 5) > 1.05f
                val indiceApuntaAbajoQ = iTip2d.y > iPip2d.y + 0.04f && indiceExtendidoPinzaQ
                val tresDedosRecogidosQ = !middleExtUp && !ringExtUp && !pinkyExtUp &&
                                          ratioExt(12, 9) < 1.15f && ratioExt(16, 13) < 1.15f
                val formaQ = indiceApuntaAbajoQ && tresDedosRecogidosQ
                val trazoQ = movTrayectoriaIndexQ > 0.12f || (movYIndexQ > 0.08f && movLatIndexQ > 0.05f)
                val esOclosedLoop = thumbIndexDist < 0.16f

                if (formaQ && trazoQ && !esOclosedLoop && thumbIndexDist > 0.18f && indexMiddleDist > 0.24f && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("q", 0.95f)
                }

                val formaD_Z = ratioExt(8, 5) > 1.16f && ratioExt(12, 9) < 1.12f && ratioExt(16, 13) < 1.12f && ratioExt(20, 17) < 1.12f
                if (formaD_Z && esTrayectoriaZ(historialPuntos, 8) && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("z", 0.96f)
                }

                val pulgarEnK = thumbExt || (thumbMiddleDist < 0.32f && thumbIndexDist < 0.34f)
                val esFormaK = indexExtUp && middleExtUp && !ringExtUp && !pinkyExtUp && !indexHorizontal &&
                               pulgarEnK && indexMiddleDist > 0.15f
                if (esFormaK && (movY > 0.08f || movTrayectoria > 0.12f) && esMovimientoEstabilizado(historialPuntos, 8)) {
                    return ResultadoClasificacion("k", 0.96f)
                }
            }
        }

        // 3. ESTÁTICAS A-Z
        val pulgarAbiertoL = (thumbExt || ratioExt(4, 2) > 1.20f) && abs(tTip2d.x - iMcp2d.x) > 0.14f

        val tresDedosCerradosI = !indexExtUp && !middleExtUp && !ringExtUp &&
                                 nDist(8, 5) < 0.48f && nDist(12, 9) < 0.48f &&
                                 (nDist(16, 13) < 0.48f || nDist(20, 17) > nDist(16, 13) + 0.10f)
        val meñiqueRealmenteExtendido = pinkyExtUp ||
                                        (nDist(20, 17) > 0.35f && ratioExt(20, 17) > 1.25f) ||
                                        (nDist(20, 17) > nDist(16, 13) + 0.10f)
        val meñiqueExtendidoI = meñiqueRealmenteExtendido && pTip2d.y < pPip2d.y + 0.06f
        val esFormaY = ratioExt(4, 2) > 1.25f && nDist(4, 20) > 0.52f && ratioExt(20, 17) > 1.25f && abs(tTip2d.x - iMcp2d.x) > 0.16f
        val esI = meñiqueExtendidoI && tresDedosCerradosI && !esFormaY

        if (esI) return ResultadoClasificacion("i", 0.96f)

        val dedosCerradosT = ratioExt(8, 5) < 1.20f && ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val pulgarCercaIndex_T = nDist(4, 5) < 0.30f || nDist(4, 6) < 0.30f
        val pulgarCercaMiddle_T = nDist(4, 9) < 0.24f || nDist(4, 10) < 0.24f
        val pulgarEntreAmbos_T = pulgarCercaIndex_T && pulgarCercaMiddle_T
        val pulgarLejosDAnularT = nDist(4, 14) > 0.24f
        val noEsDrapedM_N = !indexDrapedDown && iTip2d.y <= iPip2d.y + 0.06f
        val esT = dedosCerradosT && pulgarEntreAmbos_T && pulgarLejosDAnularT && noEsDrapedM_N && !manoApuntaAbajo
        if (esT) return ResultadoClasificacion("t", 0.95f)

        val dedosCerradosE = ratioExt(8, 5) < 1.25f && ratioExt(12, 9) < 1.25f && ratioExt(16, 13) < 1.25f && ratioExt(20, 17) < 1.25f
        val dedosGarraE = nDist(8, 5) < 0.42f && nDist(12, 9) < 0.42f && nDist(16, 13) < 0.42f
        val pulgarDebajoYemasE = tTip2d.y >= iTip2d.y - 0.01f &&
                                 (nDist(4, 8) < 0.48f || nDist(4, 6) < 0.48f || thumbIndexDist < 0.48f) &&
                                 abs(tTip2d.x - iMcp2d.x) < 0.14f
        val esE = dedosCerradosE && dedosGarraE && pulgarDebajoYemasE && !manoApuntaAbajo
        if (esE) return ResultadoClasificacion("e", 0.96f)

        val dedosCerradosS = ratioExt(8, 5) < 1.20f && ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val pulgarSobreIndex_S = nDist(4, 6) < 0.42f || nDist(4, 8) < 0.42f || nDist(4, 5) < 0.38f
        val pulgarMasCercaDeIndex = nDist(4, 6) < nDist(4, 10) + 0.08f || nDist(4, 5) < nDist(4, 9) + 0.08f
        val esS = dedosCerradosS && pulgarSobreIndex_S && pulgarMasCercaDeIndex && !indexDrapedDown && !middleDrapedDown && !manoApuntaAbajo
        if (esS) return ResultadoClasificacion("s", 0.95f)

        val dedosCerradosA = ratioExt(8, 5) < 1.20f && ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val puñoCerradoA = nDist(8, 5) < 0.52f
        val pulgarAlLadoA = abs(tTip2d.x - iMcp2d.x) >= 0.08f && (thumbExt || nDist(4, 5) > 0.14f)
        val esA = dedosCerradosA && puñoCerradoA && pulgarAlLadoA && !manoApuntaAbajo
        if (esA) return ResultadoClasificacion("a", 0.96f)

        val yemasUnidasO = thumbIndexDist < 0.38f && thumbMiddleDist < 0.38f && nDist(4, 16) < 0.45f
        val anularMeñiqueNoEstiradosO = (!ringExtUp || ratioExt(16, 13) < 1.30f) && (!pinkyExtUp || ratioExt(20, 17) < 1.30f)
        val noEsPuñoCerradoO = nDist(8, 5) > 0.15f || nDist(12, 9) > 0.15f
        val esFormaO = yemasUnidasO && anularMeñiqueNoEstiradosO && noEsPuñoCerradoO && !manoApuntaAbajo
        if (esFormaO) return ResultadoClasificacion("o", 0.96f)

        val indiceExtendidoG = nDist(8, 5) > 0.35f
        val esG = indiceExtendidoG && thumbExt && indexHorizontal && !pinkyExtUp && !ringExtUp &&
                  (nDist(8, 5) > nDist(12, 9) + 0.12f || nDist(12, 9) < 0.45f)
        if (esG) return ResultadoClasificacion("g", 0.96f)

        val dosDedosExtendidosH = nDist(8, 5) > 0.35f && nDist(12, 9) > 0.28f
        val dosDedosJuntosH = indexMiddleDist < 0.32f
        val esH = dosDedosExtendidosH && dosDedosJuntosH && indexHorizontal && !pinkyExtUp &&
                  (nDist(12, 9) > nDist(16, 13) + 0.12f || nDist(16, 13) < 0.45f) &&
                  abs(nDist(8, 5) - nDist(12, 9)) < 0.20f
        if (esH) return ResultadoClasificacion("h", 0.96f)

        val anularCaidoM = ringDrapedDown || (rTip2d.y > rPip2d.y + 0.04f && nDist(16, 13) > 0.30f)
        val tresDedosAbajoM = (indexDrapedDown || iTip2d.y > iPip2d.y - 0.04f) &&
                              (middleDrapedDown || mTip2d.y > mPip2d.y - 0.04f) &&
                              anularCaidoM && nDist(16, 13) > 0.30f
        val meñiquePlegadoM = !pinkyExtUp && (pTip2d.y < rTip2d.y + 0.10f || nDist(20, 17) < 0.48f)
        val esM = tresDedosAbajoM && meñiquePlegadoM && !indexHorizontal
        if (esM) return ResultadoClasificacion("m", 0.96f)

        val esL = pulgarAbiertoL && indexExtUp && cCurvatureIndex > 0.93f && !indexHorizontal && !ringExtUp && !pinkyExtUp
        if (esL) return ResultadoClasificacion("l", 0.97f)

        val dosDedosAbajoN = indexDrapedDown && middleDrapedDown
        val anularMeñiquePlegadosN = !ringExtUp && !pinkyExtUp && (!anularCaidoM || nDist(16, 13) <= 0.30f) && (rTip2d.y < mTip2d.y + 0.08f || nDist(16, 13) < 0.48f)
        val esN = dosDedosAbajoN && anularMeñiquePlegadosN && indexMiddleDist < 0.38f && !indexHorizontal
        if (esN) return ResultadoClasificacion("n", 0.96f)

        val aperturaC = thumbIndexDist in 0.18f..0.95f && thumbMiddleDist in 0.18f..0.95f
        val dedosCurvadosC = cCurvatureIndex < 0.98f || mCurvatureIndex < 0.98f || (ratioExt(8, 5) < 1.45f && ratioExt(12, 9) < 1.45f)
        val noEsPuñoCerrado = nDist(8, 0) > 0.38f && nDist(12, 0) > 0.38f && nDist(8, 5) > 0.22f && nDist(12, 9) > 0.42f
        val noEsO = thumbIndexDist > 0.20f || thumbMiddleDist > 0.20f
        val noEsL = !indexExtUp || ratioExt(8, 5) < 1.45f || cCurvatureIndex < 0.96f
        val esCurvaC = aperturaC && dedosCurvadosC && noEsPuñoCerrado && noEsO && noEsL &&
                       !indexDrapedDown && !manoApuntaAbajo && !pinkyExtUp
        if (esCurvaC) return ResultadoClasificacion("c", 0.96f)

        val esD = indexExtUp && cCurvatureIndex > 0.93f && !middleExtUp && !ringExtUp && !pinkyExtUp &&
                  !manoHorizontal && !manoApuntaAbajo && !pulgarAbiertoL
        if (esD) return ResultadoClasificacion("d", 0.97f)

        if (thumbIndexDist < 0.26f && middleExtUp && ringExtUp && pinkyExtUp) {
            return ResultadoClasificacion("f", 0.95f)
        }

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

        val indexExtendedW = ratioExt(8, 5) > 1.15f
        val middleExtendedW = ratioExt(12, 9) > 1.15f
        val ringExtendedW = ratioExt(16, 13) > 1.15f
        val pinkyClosedW = ratioExt(20, 17) < 1.15f
        if (indexExtendedW && middleExtendedW && ringExtendedW && pinkyClosedW) {
            return ResultadoClasificacion("w", 0.95f)
        }

        val indexClosedY = ratioExt(8, 5) < 1.15f
        val middleClosedY = ratioExt(12, 9) < 1.15f
        val ringClosedY = ratioExt(16, 13) < 1.15f
        val thumbExtendedY = ratioExt(4, 2) > 1.18f || thumbExt
        val pinkyExtendedY = ratioExt(20, 17) > 1.18f || pinkyExtUp
        val spanY = nDist(4, 20) > 0.48f
        if (indexClosedY && middleClosedY && ringClosedY && thumbExtendedY && pinkyExtendedY && spanY) {
            return ResultadoClasificacion("y", 0.95f)
        }

        val indiceAbajoP = (iTip2d.y > iMcp2d.y + 0.04f || dirY > 0.12f) && ratioExt(8, 5) > 1.05f
        val pulgarAlLadoP = thumbExt || abs(tTip2d.x - iMcp2d.x) > 0.06f || thumbIndexDist < 0.50f || nDist(4, 5) < 0.48f
        val tresDedosRecogidosP = ratioExt(12, 9) < 1.20f && ratioExt(16, 13) < 1.20f && ratioExt(20, 17) < 1.20f
        val esP = (indiceAbajoP || indexDrapedDown) && pulgarAlLadoP && tresDedosRecogidosP && !ringExtUp && !pinkyExtUp && !anularCaidoM
        if (esP) return ResultadoClasificacion("p", 0.95f)

        if (esPoseRBase) return ResultadoClasificacion("r", 0.96f)

        val indiceExtenddoK = indexExtUp || (ratioExt(8, 5) > 1.20f && iTip2d.y < iMcp2d.y + 0.05f)
        val esK = indiceExtenddoK && (middleExtUp || ratioExt(12, 9) > 1.05f) && !ringExtUp && !pinkyExtUp &&
                  pulgarEntreDedosK && indexMiddleDist > 0.12f
        if (esK) return ResultadoClasificacion("k", 0.96f)

        val indexExtendedU = ratioExt(8, 5) > 1.15f
        val middleExtendedU = ratioExt(12, 9) > 1.15f
        val ringClosedU = ratioExt(16, 13) < 1.15f
        val pinkyClosedU = ratioExt(20, 17) < 1.15f
        val dedosParalelosSinCruzar = !indexMiddleCrossed && (nDist(8, 10) > 0.14f || nDist(12, 6) > 0.14f)
        val dedosJuntosU = indexMiddleDist < 0.18f
        val esU = indexExtendedU && middleExtendedU && ringClosedU && pinkyClosedU &&
                  dedosJuntosU && dedosParalelosSinCruzar && !pulgarEntreDedosK
        if (esU) return ResultadoClasificacion("u", 0.95f)

        val indexExtendedV = ratioExt(8, 5) > 1.15f
        val middleExtendedV = ratioExt(12, 9) > 1.15f
        val ringClosedV = ratioExt(16, 13) < 1.15f
        val pinkyClosedV = ratioExt(20, 17) < 1.15f
        val dedosSeparadosV = indexMiddleDist >= 0.22f
        val esV = indexExtendedV && middleExtendedV && ringClosedV && pinkyClosedV &&
                  dedosSeparadosV && !indexMiddleCrossed && !pulgarEntreDedosK
        if (esV) return ResultadoClasificacion("v", 0.95f)

        val indexHookX = (nDist(8, 5) < 0.52f || ratioExt(8, 5) > 1.05f) && (ratioExt(8, 5) > ratioExt(12, 9) + 0.03f || nDist(8, 5) > nDist(12, 9) + 0.03f)
        val tresDedosPlegadosX = ratioExt(12, 9) < 1.18f && ratioExt(16, 13) < 1.18f && ratioExt(20, 17) < 1.18f
        val esX = indexHookX && tresDedosPlegadosX && thumbIndexDist > 0.18f && !manoApuntaAbajo
        if (esX) return ResultadoClasificacion("x", 0.95f)

        return null
    }

    /**
     * Clasificador de Señas Comunicativas Oficiales LSM (DIELSEME / CONADIS / SEP).
     */
    fun clasificarSenaComunicativa(
        landmarks: List<NormalizedPoint3D>,
        historialPuntos: List<List<NormalizedPoint3D>> = emptyList()
    ): ResultadoSenaComunicativa? {
        if (landmarks.size < 21) return null

        val w2d = landmarks[0]
        val tTip2d = landmarks[4]
        val iMcp2d = landmarks[5]
        val iPip2d = landmarks[6]
        val iTip2d = landmarks[8]
        val mMcp2d = landmarks[9]
        val mPip2d = landmarks[10]
        val mTip2d = landmarks[12]
        val rMcp2d = landmarks[13]
        val rPip2d = landmarks[14]
        val rTip2d = landmarks[16]
        val pMcp2d = landmarks[17]
        val pPip2d = landmarks[18]
        val pTip2d = landmarks[20]

        fun getX(idx: Int) = landmarks[idx].x
        fun getY(idx: Int) = landmarks[idx].y
        fun getZ(idx: Int) = landmarks[idx].z

        fun d3D(idx1: Int, idx2: Int): Float {
            val dx = getX(idx1) - getX(idx2)
            val dy = getY(idx1) - getY(idx2)
            val dz = getZ(idx1) - getZ(idx2)
            return sqrt(dx * dx + dy * dy + dz * dz)
        }

        val palmSize = d3D(0, 9).coerceAtLeast(0.001f)
        fun nDist(idx1: Int, idx2: Int): Float = d3D(idx1, idx2) / palmSize
        fun ratioExt(tipIdx: Int, mcpIdx: Int): Float = d3D(tipIdx, 0) / d3D(mcpIdx, 0).coerceAtLeast(0.001f)

        val anguloIndex  = anguloArticular3D(landmarks[5], landmarks[6], landmarks[8])
        val anguloMiddle = anguloArticular3D(landmarks[9], landmarks[10], landmarks[12])
        val anguloRing   = anguloArticular3D(landmarks[13], landmarks[14], landmarks[16])
        val anguloPinky  = anguloArticular3D(landmarks[17], landmarks[18], landmarks[20])

        val indexExtUp  = (anguloIndex > 135f || ratioExt(8, 5) > 1.10f) && iTip2d.y < iPip2d.y + 0.08f
        val middleExtUp = (anguloMiddle > 135f || ratioExt(12, 9) > 1.10f) && mTip2d.y < mPip2d.y + 0.08f
        val ringExtUp   = (anguloRing > 135f || ratioExt(16, 13) > 1.10f) && rTip2d.y < rPip2d.y + 0.08f
        val pinkyExtUp  = (anguloPinky > 135f || ratioExt(20, 17) > 1.10f) && pTip2d.y < pPip2d.y + 0.08f

        val thumbIndexDist = nDist(4, 8)
        val thumbMiddleDist = nDist(4, 12)

        val movY = if (historialPuntos.size >= 3) calcularMovimientoVertical(historialPuntos, 0) else 0f
        val movLat = if (historialPuntos.size >= 3) calcularMovimientoLateral(historialPuntos, 0) else 0f
        val movTray = if (historialPuntos.size >= 3) calcularLongitudTrayectoria(historialPuntos, 0) else 0f
        val ratioOsc = if (historialPuntos.size >= 3) calcularRatioOscilacion(historialPuntos, 0) else 0f
        val movLatIndex = if (historialPuntos.size >= 3) calcularMovimientoLateral(historialPuntos, 8) else 0f
        val ratioOscIndex = if (historialPuntos.size >= 3) calcularRatioOscilacion(historialPuntos, 8) else 0f

        fun getSena(id: String): SenaLSMInfo = CATALOGO_SENAS_COMUNICATIVAS.first { it.id == id }

        // 1. LECHE
        val esPuño = !indexExtUp && !middleExtUp && !ringExtUp && !pinkyExtUp
        if (esPuño && (movY > 0.06f || movTray > 0.10f) && ratioOsc > 1.15f) {
            return ResultadoSenaComunicativa(getSena("LECHE"), 0.94f)
        }

        // 2. COMIDA
        val yemasUnidasComida = thumbIndexDist < 0.28f && thumbMiddleDist < 0.28f && nDist(4, 16) < 0.35f
        val yemasApuntandoArriba = iTip2d.y < w2d.y
        if (yemasUnidasComida && yemasApuntandoArriba && iTip2d.y < 0.55f && (movY > 0.04f || iTip2d.y < 0.40f)) {
            return ResultadoSenaComunicativa(getSena("COMIDA"), 0.95f)
        }

        // 3. AGUA
        val esFormaW = indexExtUp && middleExtUp && ringExtUp && !pinkyExtUp
        if (esFormaW && iTip2d.y < 0.48f) {
            return ResultadoSenaComunicativa(getSena("AGUA"), 0.96f)
        }

        // 4. BEBE
        if (movLat > 0.10f && ratioOsc > 1.25f && w2d.y > 0.40f) {
            return ResultadoSenaComunicativa(getSena("BEBE"), 0.93f)
        }

        // 5. FIEBRE
        val esFormaF = thumbIndexDist < 0.30f && middleExtUp && ringExtUp && pinkyExtUp
        val manoEnFrente = (iTip2d.y < 0.28f || w2d.y < 0.35f) && (indexExtUp || middleExtUp || esPuño || esFormaF)
        if (manoEnFrente) {
            return ResultadoSenaComunicativa(getSena("FIEBRE"), 0.96f)
        }

        // 6. DOCTOR
        val esD_o_M = (indexExtUp && !middleExtUp && !ringExtUp && !pinkyExtUp) || (!indexExtUp && !pinkyExtUp && nDist(8, 0) < 0.50f)
        val manoBajaOrientada = w2d.y > 0.55f && (movY > 0.04f || movTray > 0.06f)
        if (esD_o_M && manoBajaOrientada) {
            return ResultadoSenaComunicativa(getSena("DOCTOR"), 0.92f)
        }

        // 7. MEDICINA
        val medioHaciaAbajo = mTip2d.y > mPip2d.y && !ringExtUp && !pinkyExtUp
        if ((medioHaciaAbajo || (middleExtUp && ratioOsc > 1.35f)) && movTray > 0.08f) {
            return ResultadoSenaComunicativa(getSena("MEDICINA"), 0.92f)
        }

        // 8. PESO
        val cuatroExtendidos = indexExtUp && middleExtUp && ringExtUp && pinkyExtUp
        if (cuatroExtendidos && movY > 0.08f && ratioOsc > 1.20f) {
            return ResultadoSenaComunicativa(getSena("PESO"), 0.93f)
        }

        // 9. SI_CONFIRMAR
        val pulgarArriba = tTip2d.y < landmarks[2].y - 0.04f && tTip2d.y < iMcp2d.y
        val cuatroCerrados = !indexExtUp && !middleExtUp && !ringExtUp && !pinkyExtUp && nDist(8, 5) < 0.50f
        if (pulgarArriba && cuatroCerrados) {
            return ResultadoSenaComunicativa(getSena("SI_CONFIRMAR"), 0.97f)
        }

        // 10. NO_CANCELAR
        val soloIndiceArriba = indexExtUp && !middleExtUp && !ringExtUp && !pinkyExtUp
        if (soloIndiceArriba && movLatIndex > 0.06f && ratioOscIndex > 1.20f) {
            return ResultadoSenaComunicativa(getSena("NO_CANCELAR"), 0.96f)
        }

        // 11. AYUDA
        if (pulgarArriba && movY > 0.05f && ratioOsc < 1.15f) {
            return ResultadoSenaComunicativa(getSena("AYUDA"), 0.93f)
        }

        // 12. GRACIAS
        val saleDeBarbilla = iTip2d.y > 0.35f && (indexExtUp || middleExtUp) && movY > 0.06f && ratioOsc < 1.15f
        if (saleDeBarbilla && cuatroExtendidos) {
            return ResultadoSenaComunicativa(getSena("GRACIAS"), 0.94f)
        }

        return null
    }

    private fun calcularEscalaReferencia(landmarks: List<NormalizedPoint3D>): Float {
        if (landmarks.size <= 9) return 0.01f
        val d = d3D(landmarks[0], landmarks[9])
        return if (d <= 0f) 0.01f else d.coerceAtLeast(0.01f)
    }

    private fun calcularEscalaPromedio(historial: List<List<NormalizedPoint3D>>): Float {
        val escalas = historial.mapNotNull { if (it.size > 9) calcularEscalaReferencia(it) else null }
        if (escalas.isEmpty()) return 1.0f
        return (escalas.sum() / escalas.size).coerceAtLeast(0.01f)
    }

    private fun calcularMovimientoLateral(
        historial: List<List<NormalizedPoint3D>>,
        indexPunto: Int,
        normalizada: Boolean = false
    ): Float {
        if (historial.size < 3) return 0f
        val xs = historial.mapNotNull { if (it.size > indexPunto) it[indexPunto].x else null }
        if (xs.isEmpty()) return 0f
        val minX = xs.minOrNull() ?: 0f
        val maxX = xs.maxOrNull() ?: 0f
        val rangoRaw = maxX - minX
        if (!normalizada) return rangoRaw
        val escala = calcularEscalaPromedio(historial)
        return rangoRaw / escala
    }

    private fun calcularMovimientoVertical(
        historial: List<List<NormalizedPoint3D>>,
        indexPunto: Int,
        normalizada: Boolean = false
    ): Float {
        if (historial.size < 3) return 0f
        val ys = historial.mapNotNull { if (it.size > indexPunto) it[indexPunto].y else null }
        if (ys.isEmpty()) return 0f
        val minY = ys.minOrNull() ?: 0f
        val maxY = ys.maxOrNull() ?: 0f
        val rangoRaw = maxY - minY
        if (!normalizada) return rangoRaw
        val escala = calcularEscalaPromedio(historial)
        return rangoRaw / escala
    }

    private fun calcularLongitudTrayectoria(
        historial: List<List<NormalizedPoint3D>>,
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
                distAcum += d3D(p1, p2)
                paresValidos++
            }
        }

        val proporcionFaltantes = (totalPares - paresValidos).toFloat() / totalPares.toFloat()
        if (proporcionFaltantes > 0.30f) {
            return -1f
        }

        if (!normalizada) return distAcum
        val escala = calcularEscalaPromedio(historial)
        return distAcum / escala
    }

    private fun calcularRatioOscilacion(historial: List<List<NormalizedPoint3D>>, indexPunto: Int): Float {
        val rangoX = calcularMovimientoLateral(historial, indexPunto, normalizada = true)
        val rangoY = calcularMovimientoVertical(historial, indexPunto, normalizada = true)
        val rangoCombinado = sqrt(rangoX * rangoX + rangoY * rangoY)
        if (rangoCombinado <= 0.001f) return 0f
        val tray = calcularLongitudTrayectoria(historial, indexPunto, normalizada = true)
        if (tray <= 0f) return 0f
        return tray / rangoCombinado
    }

    private fun calcularVelocidadPromedio(historial: List<List<NormalizedPoint3D>>, indexPunto: Int): Float {
        if (historial.size < 3) return 0f
        val distAcum = calcularLongitudTrayectoria(historial, indexPunto, normalizada = true)
        if (distAcum < 0f) return -1f
        return distAcum / historial.size.toFloat()
    }

    private fun esMovimientoEstabilizado(
        historial: List<List<NormalizedPoint3D>>,
        landmarkIndex: Int
    ): Boolean {
        if (historial.size < 3) return true
        val ultimosPuntos = historial.takeLast(3).mapNotNull { it.getOrNull(landmarkIndex) }
        if (ultimosPuntos.size < 3) return true

        val p1 = ultimosPuntos[0]
        val p2 = ultimosPuntos[1]
        val p3 = ultimosPuntos[2]

        val delta1 = kotlin.math.hypot(p2.x - p1.x, p2.y - p1.y)
        val delta2 = kotlin.math.hypot(p3.x - p2.x, p3.y - p2.y)

        return delta1 < 0.08f && delta2 < 0.08f
    }

    private fun esTrayectoriaZ(historial: List<List<NormalizedPoint3D>>, indexPunto: Int): Boolean {
        if (historial.size < 12) return false
        val puntos = historial.mapNotNull { it.getOrNull(indexPunto) }
        if (puntos.size < 12) return false

        val dTot = calcularLongitudTrayectoria(historial, indexPunto, normalizada = true)
        if (dTot < 0.18f) return false

        val n = puntos.size
        val seg1 = puntos.subList(0, n / 3)
        val seg2 = puntos.subList(n / 3, (2 * n) / 3)
        val seg3 = puntos.subList((2 * n) / 3, n)

        val dx1 = seg1.last().x - seg1.first().x
        val dx2 = seg2.last().x - seg2.first().x
        val dy2 = seg2.last().y - seg2.first().y
        val dx3 = seg3.last().x - seg3.first().x

        val cumpleNoEspejado = dx1 > 0.02f && dx2 < -0.02f && dy2 > 0.03f && dx3 > 0.02f
        val cumpleEspejado = dx1 < -0.02f && dx2 > 0.02f && dy2 > 0.03f && dx3 < -0.02f

        return cumpleNoEspejado || cumpleEspejado
    }
}
