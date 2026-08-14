package com.example.nutriia.accesibilidad

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.mediapipe.tasks.components.containers.Landmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

class SignLanguageClassifierTest {

    @Before
    fun setUp() {
        val gson = Gson()
        var file = File("src/main/assets/lsm_dataset.json")
        if (!file.exists()) {
            file = File("app/src/main/assets/lsm_dataset.json")
        }
        if (file.exists()) {
            file.reader().use { reader ->
                val type = object : TypeToken<LsmDataset>() {}.type
                val dataset = gson.fromJson<LsmDataset>(reader, type)
                SignLanguageClassifier.datasetStatic = dataset.samples
            }
        }
    }

    @Test
    fun generateDatasetJson() {
        val rawHands = mutableMapOf<String, List<NormalizedLandmark>>()

        // Generate hand for each letter
        val handA = createBaseHand()
        handA[4] = NormalizedLandmark.create(0.30f, 0.75f, 0.0f)
        rawHands["a"] = handA

        val handB = createBaseHand(
            indexExtended = true,
            middleExtended = true,
            ringExtended = true,
            pinkyExtended = true
        )
        handB[8] = NormalizedLandmark.create(0.42f, 0.32f, -0.06f)
        handB[12] = NormalizedLandmark.create(0.50f, 0.30f, -0.06f)
        handB[16] = NormalizedLandmark.create(0.58f, 0.32f, -0.06f)
        handB[20] = NormalizedLandmark.create(0.65f, 0.35f, -0.06f)
        handB[4] = NormalizedLandmark.create(0.48f, 0.75f, 0.02f)
        rawHands["b"] = handB

        val handC = createBaseHand(isCurvedC = true)
        handC[4] = NormalizedLandmark.create(0.28f, 0.75f, -0.04f)
        handC[8] = NormalizedLandmark.create(0.35f, 0.52f, -0.06f)
        rawHands["c"] = handC

        val handD = createBaseHand(indexExtended = true)
        rawHands["d"] = handD

        val handE = createBaseHand()
        handE[8] = NormalizedLandmark.create(0.42f, 0.72f, 0.02f)
        handE[12] = NormalizedLandmark.create(0.50f, 0.70f, 0.02f)
        handE[16] = NormalizedLandmark.create(0.58f, 0.72f, 0.02f)
        handE[20] = NormalizedLandmark.create(0.65f, 0.74f, 0.02f)
        handE[4] = NormalizedLandmark.create(0.42f, 0.78f, 0.02f)
        rawHands["e"] = handE

        val handF = createBaseHand(
            middleExtended = true,
            ringExtended = true,
            pinkyExtended = true
        )
        handF[4] = NormalizedLandmark.create(0.42f, 0.65f, 0.0f)
        handF[8] = NormalizedLandmark.create(0.42f, 0.65f, 0.0f)
        rawHands["f"] = handF

        val handG_base = createBaseHand(indexExtended = true, thumbExtended = true)
        rawHands["g_90"] = rotateHand(handG_base, -Math.PI.toFloat() / 2f)
        rawHands["g_70"] = rotateHand(handG_base, -70f * Math.PI.toFloat() / 180f)
        rawHands["g_50"] = rotateHand(handG_base, -50f * Math.PI.toFloat() / 180f)

        val handH_base = createBaseHand(indexExtended = true, middleExtended = true, thumbExtended = true)
        rawHands["h_90"] = rotateHand(handH_base, -Math.PI.toFloat() / 2f)
        rawHands["h_70"] = rotateHand(handH_base, -70f * Math.PI.toFloat() / 180f)
        rawHands["h_50"] = rotateHand(handH_base, -50f * Math.PI.toFloat() / 180f)

        val handI = createBaseHand(pinkyExtended = true)
        rawHands["i"] = handI

        val handI_relaxed = createBaseHand(pinkyExtended = true)
        handI_relaxed[4] = NormalizedLandmark.create(0.38f, 0.78f, 0.02f)
        rawHands["i_relaxed"] = handI_relaxed

        val handK = createBaseHand(indexExtended = true, middleExtended = true)
        handK[4] = NormalizedLandmark.create(0.46f, 0.55f, -0.02f)
        rawHands["k"] = handK

        val handL = createBaseHand(indexExtended = true, thumbExtended = true)
        rawHands["l"] = handL

        val handM = createBaseHand(isDrapedDown = true)
        handM[16] = NormalizedLandmark.create(0.58f, 0.88f, 0.06f)
        handM[4] = NormalizedLandmark.create(0.46f, 0.82f, 0.02f)
        rawHands["m"] = handM

        val handN = createBaseHand(isDrapedDown = true)
        handN[14] = NormalizedLandmark.create(0.58f, 0.70f, 0.03f)
        handN[15] = NormalizedLandmark.create(0.58f, 0.68f, 0.05f)
        handN[16] = NormalizedLandmark.create(0.58f, 0.66f, 0.06f)
        handN[4] = NormalizedLandmark.create(0.46f, 0.82f, 0.02f)
        rawHands["n"] = handN

        val handO = createBaseHand(indexExtended = true, middleExtended = true, ringExtended = true, pinkyExtended = true)
        // Index
        handO[6] = NormalizedLandmark.create(0.42f, 0.52f, -0.02f)
        handO[7] = NormalizedLandmark.create(0.43f, 0.55f, -0.04f)
        handO[8] = NormalizedLandmark.create(0.44f, 0.60f, -0.05f)
        // Middle
        handO[10] = NormalizedLandmark.create(0.48f, 0.51f, -0.02f)
        handO[11] = NormalizedLandmark.create(0.48f, 0.54f, -0.04f)
        handO[12] = NormalizedLandmark.create(0.48f, 0.59f, -0.05f)
        // Ring
        handO[14] = NormalizedLandmark.create(0.52f, 0.52f, -0.02f)
        handO[15] = NormalizedLandmark.create(0.52f, 0.55f, -0.04f)
        handO[16] = NormalizedLandmark.create(0.52f, 0.60f, -0.05f)
        // Pinky
        handO[18] = NormalizedLandmark.create(0.56f, 0.54f, -0.02f)
        handO[19] = NormalizedLandmark.create(0.56f, 0.57f, -0.04f)
        handO[20] = NormalizedLandmark.create(0.56f, 0.62f, -0.05f)
        // Thumb
        handO[2] = NormalizedLandmark.create(0.42f, 0.76f, -0.02f)
        handO[3] = NormalizedLandmark.create(0.43f, 0.70f, -0.04f)
        handO[4] = NormalizedLandmark.create(0.44f, 0.62f, -0.05f)
        rawHands["o"] = handO

        val handP_base = createBaseHand(indexExtended = true, middleExtended = true)
        handP_base[4] = NormalizedLandmark.create(0.46f, 0.55f, -0.02f)
        rawHands["p_180"] = rotateHand(handP_base, Math.PI.toFloat())
        rawHands["p_150"] = rotateHand(handP_base, 150f * Math.PI.toFloat() / 180f)
        rawHands["p_120"] = rotateHand(handP_base, 120f * Math.PI.toFloat() / 180f)
        rawHands["p_100"] = rotateHand(handP_base, 100f * Math.PI.toFloat() / 180f)

        val handR = createBaseHand(
            indexExtended = true,
            middleExtended = true
        )
        val tempX = handR[8].x()
        handR[8] = NormalizedLandmark.create(handR[12].x(), handR[8].y(), handR[8].z())
        handR[12] = NormalizedLandmark.create(tempX, handR[12].y(), handR[12].z())
        rawHands["r"] = handR

        val handS = createBaseHand()
        handS[4] = NormalizedLandmark.create(0.38f, 0.65f, -0.02f)
        rawHands["s"] = handS

        val handT = createBaseHand()
        handT[4] = NormalizedLandmark.create(0.46f, 0.64f, 0.02f)
        handT[8] = NormalizedLandmark.create(0.42f, 0.72f, -0.02f)
        rawHands["t"] = handT

        val handU = createBaseHand(
            indexExtended = true,
            middleExtended = true
        )
        handU[8] = NormalizedLandmark.create(0.45f, 0.35f, -0.06f)
        handU[12] = NormalizedLandmark.create(0.47f, 0.33f, -0.06f)
        rawHands["u"] = handU

        val handV = createBaseHand(
            indexExtended = true,
            middleExtended = true
        )
        handV[8] = NormalizedLandmark.create(0.38f, 0.35f, -0.06f)
        handV[12] = NormalizedLandmark.create(0.54f, 0.33f, -0.06f)
        rawHands["v"] = handV

        val handW = createBaseHand(
            indexExtended = true,
            middleExtended = true,
            ringExtended = true
        )
        rawHands["w"] = handW

        val handX = createBaseHand()
        handX[6] = NormalizedLandmark.create(0.38f, 0.52f, 0.05f)
        handX[7] = NormalizedLandmark.create(0.40f, 0.56f, 0.05f)
        handX[8] = NormalizedLandmark.create(0.42f, 0.58f, 0.05f)
        handX[4] = NormalizedLandmark.create(0.35f, 0.75f, 0.02f)
        rawHands["x"] = handX

        val handY = createBaseHand(
            thumbExtended = true,
            pinkyExtended = true
        )
        rawHands["y"] = handY

        // Calcular vectores tanto para manos normales como para manos espejo
        val dataset = mutableMapOf<String, List<Float>>()
        for ((label, hand) in rawHands) {
            dataset[label] = obtenerVectorHibridoStatic(hand)
            dataset["${label}_mirror"] = obtenerVectorHibridoStatic(mirrorHand(hand))
        }

        val jsonBuilder = StringBuilder()
        jsonBuilder.append("{\n  \"samples\": [\n")
        val keys = dataset.keys.toList()
        for (i in keys.indices) {
            val key = keys[i]
            val cleanLabel = key.substringBefore("_")
            val vector = dataset[key]!!
            jsonBuilder.append("    {\n")
            jsonBuilder.append("      \"label\": \"$cleanLabel\",\n")
            jsonBuilder.append("      \"vector\": [")
            jsonBuilder.append(vector.joinToString(", "))
            jsonBuilder.append("]\n")
            jsonBuilder.append("    }")
            if (i < keys.size - 1) {
                jsonBuilder.append(",")
            }
            jsonBuilder.append("\n")
        }
        jsonBuilder.append("  ]\n}")

        val outputDir = java.io.File("src/main/assets")
        outputDir.mkdirs()
        java.io.File(outputDir, "lsm_dataset.json").writeText(jsonBuilder.toString())
        println("Dataset generated successfully at " + java.io.File(outputDir, "lsm_dataset.json").absolutePath)
    }

    private fun mirrorHand(landmarks: List<NormalizedLandmark>): List<NormalizedLandmark> {
        return landmarks.map { NormalizedLandmark.create(1.0f - it.x(), it.y(), it.z()) }
    }

    private fun rotateHand(landmarks: List<NormalizedLandmark>, angleRad: Float): List<NormalizedLandmark> {
        val wrist = landmarks[0]
        val wx = wrist.x()
        val wy = wrist.y()
        val cos = kotlin.math.cos(angleRad)
        val sin = kotlin.math.sin(angleRad)
        return landmarks.map { lm ->
            val dx = lm.x() - wx
            val dy = lm.y() - wy
            NormalizedLandmark.create(
                wx + dx * cos - dy * sin,
                wy + dx * sin + dy * cos,
                lm.z()
            )
        }
    }

    private fun obtenerVectorHibridoStatic(landmarks: List<NormalizedLandmark>): List<Float> {
        if (landmarks.size < 21) return emptyList()

        val wrist = landmarks[0]
        val wX = wrist.x()
        val wY = wrist.y()
        val wZ = wrist.z()

        fun d3D(p1: NormalizedLandmark, p2: NormalizedLandmark): Float {
            val dx = p1.x() - p2.x()
            val dy = p1.y() - p2.y()
            val dz = p1.z() - p2.z()
            return kotlin.math.sqrt(dx * dx + dy * dy + dz * dz)
        }

        val dx9 = landmarks[9].x() - wX
        val dy9 = landmarks[9].y() - wY
        val dz9 = landmarks[9].z() - wZ
        val palmSize = kotlin.math.sqrt(dx9 * dx9 + dy9 * dy9 + dz9 * dz9).coerceAtLeast(0.001f)

        val vector = ArrayList<Float>()

        for (lm in landmarks) {
            vector.add((lm.x() - wX) / palmSize)
            vector.add((lm.y() - wY) / palmSize)
            vector.add((lm.z() - wZ) / palmSize)
        }

        fun nDist(idx1: Int, idx2: Int): Float = d3D(landmarks[idx1], landmarks[idx2]) / palmSize

        vector.add(nDist(4, 8))
        vector.add(nDist(8, 12))
        vector.add(nDist(12, 16))
        vector.add(nDist(16, 20))
        vector.add(nDist(4, 12))

        vector.add(nDist(4, 5))
        vector.add(nDist(4, 9))

        val angle = kotlin.math.atan2(landmarks[9].y() - wY, landmarks[9].x() - wX)
        vector.add(angle)

        return vector
    }

    // Helper to generate a baseline hand landmark template (all nodes initialized)
    private fun createBaseHand(
        thumbExtended: Boolean = false,
        indexExtended: Boolean = false,
        middleExtended: Boolean = false,
        ringExtended: Boolean = false,
        pinkyExtended: Boolean = false,
        isDrapedDown: Boolean = false,
        isHorizontal: Boolean = false,
        isCurvedC: Boolean = false
    ): MutableList<NormalizedLandmark> {
        val landmarks = MutableList(21) { NormalizedLandmark.create(0f, 0f, 0f) }

        // Wrist (0)
        landmarks[0] = NormalizedLandmark.create(0.5f, 0.9f, 0.0f)

        // Index MCP/PIP/DIP/Tip
        landmarks[5] = NormalizedLandmark.create(0.42f, 0.65f, 0.0f)
        if (indexExtended) {
            if (isHorizontal) {
                landmarks[6] = NormalizedLandmark.create(0.27f, 0.65f, -0.03f)
                landmarks[7] = NormalizedLandmark.create(0.19f, 0.65f, -0.05f)
                landmarks[8] = NormalizedLandmark.create(0.12f, 0.65f, -0.06f)
            } else {
                landmarks[6] = NormalizedLandmark.create(0.42f, 0.50f, -0.03f)
                landmarks[7] = NormalizedLandmark.create(0.42f, 0.42f, -0.05f)
                landmarks[8] = NormalizedLandmark.create(0.42f, 0.35f, -0.06f)
            }
        } else if (isDrapedDown) {
            landmarks[6] = NormalizedLandmark.create(0.42f, 0.77f, 0.03f)
            landmarks[7] = NormalizedLandmark.create(0.42f, 0.85f, 0.05f)
            landmarks[8] = NormalizedLandmark.create(0.42f, 0.90f, 0.06f)
        } else if (isCurvedC) {
            landmarks[6] = NormalizedLandmark.create(0.35f, 0.58f, -0.03f)
            landmarks[7] = NormalizedLandmark.create(0.30f, 0.65f, -0.05f)
            landmarks[8] = NormalizedLandmark.create(0.35f, 0.72f, -0.06f)
        } else {
            // Folded
            landmarks[6] = NormalizedLandmark.create(0.42f, 0.70f, 0.03f)
            landmarks[7] = NormalizedLandmark.create(0.42f, 0.75f, 0.05f)
            landmarks[8] = NormalizedLandmark.create(0.42f, 0.73f, 0.06f)
        }

        // Middle MCP/PIP/DIP/Tip
        landmarks[9] = NormalizedLandmark.create(0.50f, 0.63f, 0.0f)
        if (middleExtended) {
            if (isHorizontal) {
                landmarks[10] = NormalizedLandmark.create(0.35f, 0.63f, -0.03f)
                landmarks[11] = NormalizedLandmark.create(0.27f, 0.63f, -0.05f)
                landmarks[12] = NormalizedLandmark.create(0.20f, 0.63f, -0.06f)
            } else {
                landmarks[10] = NormalizedLandmark.create(0.50f, 0.48f, -0.03f)
                landmarks[11] = NormalizedLandmark.create(0.50f, 0.40f, -0.05f)
                landmarks[12] = NormalizedLandmark.create(0.50f, 0.33f, -0.06f)
            }
        } else if (isDrapedDown) {
            landmarks[10] = NormalizedLandmark.create(0.50f, 0.75f, 0.03f)
            landmarks[11] = NormalizedLandmark.create(0.50f, 0.83f, 0.05f)
            landmarks[12] = NormalizedLandmark.create(0.50f, 0.88f, 0.06f)
        } else if (isCurvedC) {
            landmarks[10] = NormalizedLandmark.create(0.43f, 0.56f, -0.03f)
            landmarks[11] = NormalizedLandmark.create(0.38f, 0.63f, -0.05f)
            landmarks[12] = NormalizedLandmark.create(0.43f, 0.70f, -0.06f)
        } else {
            // Folded
            landmarks[10] = NormalizedLandmark.create(0.50f, 0.68f, 0.03f)
            landmarks[11] = NormalizedLandmark.create(0.50f, 0.73f, 0.05f)
            landmarks[12] = NormalizedLandmark.create(0.50f, 0.71f, 0.06f)
        }

        // Ring MCP/PIP/DIP/Tip
        landmarks[13] = NormalizedLandmark.create(0.58f, 0.65f, 0.0f)
        if (ringExtended) {
            landmarks[14] = NormalizedLandmark.create(0.58f, 0.50f, -0.03f)
            landmarks[15] = NormalizedLandmark.create(0.58f, 0.42f, -0.05f)
            landmarks[16] = NormalizedLandmark.create(0.58f, 0.35f, -0.06f)
        } else if (isDrapedDown) {
            landmarks[14] = NormalizedLandmark.create(0.58f, 0.77f, 0.03f)
            landmarks[15] = NormalizedLandmark.create(0.58f, 0.85f, 0.05f)
            landmarks[16] = NormalizedLandmark.create(0.58f, 0.90f, 0.06f)
        } else if (isCurvedC) {
            landmarks[14] = NormalizedLandmark.create(0.51f, 0.58f, -0.03f)
            landmarks[15] = NormalizedLandmark.create(0.46f, 0.65f, -0.05f)
            landmarks[16] = NormalizedLandmark.create(0.51f, 0.72f, -0.06f)
        } else {
            // Folded
            landmarks[14] = NormalizedLandmark.create(0.58f, 0.70f, 0.03f)
            landmarks[15] = NormalizedLandmark.create(0.58f, 0.75f, 0.05f)
            landmarks[16] = NormalizedLandmark.create(0.58f, 0.73f, 0.06f)
        }

        // Pinky MCP/PIP/DIP/Tip
        landmarks[17] = NormalizedLandmark.create(0.65f, 0.68f, 0.0f)
        if (pinkyExtended) {
            landmarks[18] = NormalizedLandmark.create(0.65f, 0.53f, -0.03f)
            landmarks[19] = NormalizedLandmark.create(0.65f, 0.45f, -0.05f)
            landmarks[20] = NormalizedLandmark.create(0.65f, 0.38f, -0.06f)
        } else if (isDrapedDown) {
            landmarks[18] = NormalizedLandmark.create(0.65f, 0.80f, 0.03f)
            landmarks[19] = NormalizedLandmark.create(0.65f, 0.88f, 0.05f)
            landmarks[20] = NormalizedLandmark.create(0.65f, 0.93f, 0.06f)
        } else if (isCurvedC) {
            landmarks[18] = NormalizedLandmark.create(0.58f, 0.61f, -0.03f)
            landmarks[19] = NormalizedLandmark.create(0.53f, 0.68f, -0.05f)
            landmarks[20] = NormalizedLandmark.create(0.58f, 0.75f, -0.06f)
        } else {
            // Folded
            landmarks[18] = NormalizedLandmark.create(0.65f, 0.73f, 0.03f)
            landmarks[19] = NormalizedLandmark.create(0.65f, 0.78f, 0.05f)
            landmarks[20] = NormalizedLandmark.create(0.65f, 0.76f, 0.06f)
        }

        // Thumb MCP/PIP/DIP/Tip
        landmarks[1] = NormalizedLandmark.create(0.42f, 0.85f, 0.0f)
        landmarks[2] = NormalizedLandmark.create(0.38f, 0.85f, 0.0f)
        if (thumbExtended) {
            landmarks[3] = NormalizedLandmark.create(0.28f, 0.82f, -0.02f)
            landmarks[4] = NormalizedLandmark.create(0.20f, 0.80f, -0.04f)
        } else if (isCurvedC) {
            landmarks[3] = NormalizedLandmark.create(0.35f, 0.78f, -0.02f)
            landmarks[4] = NormalizedLandmark.create(0.30f, 0.75f, -0.04f)
        } else {
            // Folded/Tucked
            landmarks[3] = NormalizedLandmark.create(0.44f, 0.82f, 0.02f)
            landmarks[4] = NormalizedLandmark.create(0.46f, 0.80f, 0.04f)
        }

        return landmarks
    }

    private fun assertLetter(expected: String, landmarks: List<NormalizedLandmark>, history: List<List<NormalizedLandmark>> = emptyList()) {
        val result = SignLanguageClassifier.clasificarConConfianza(
            landmarks2D = landmarks,
            landmarks3D = landmarks.map { Landmark.create(it.x(), it.y(), it.z()) },
            soloNumeros = false,
            esCampoFecha = false,
            historialPuntos = history,
            debug = false
        )
        assertNotNull("La letra '$expected' no fue clasificada (retornó null)", result)
        assertEquals("La clasificación no coincide para '$expected'", expected, result!!.letra)
    }

    @Test
    fun testLetterA() {
        // A: Fist, thumb lateral to index MCP
        val hand = createBaseHand()
        hand[4] = NormalizedLandmark.create(0.30f, 0.75f, 0.0f)
        assertLetter("a", hand)
    }

    @Test
    fun testLetterB() {
        // B: 4 fingers straight up, together. Thumb folded across the palm
        val hand = createBaseHand(
            indexExtended = true,
            middleExtended = true,
            ringExtended = true,
            pinkyExtended = true
        )
        // Set straight ratio > 1.40f for all 4 fingers
        hand[8] = NormalizedLandmark.create(0.42f, 0.32f, -0.06f)
        hand[12] = NormalizedLandmark.create(0.50f, 0.30f, -0.06f)
        hand[16] = NormalizedLandmark.create(0.58f, 0.32f, -0.06f)
        hand[20] = NormalizedLandmark.create(0.65f, 0.35f, -0.06f)
        hand[4] = NormalizedLandmark.create(0.48f, 0.75f, 0.02f)
        assertLetter("b", hand)
    }

    @Test
    fun testLetterC() {
        // C: Semicircular open arc
        val hand = createBaseHand(isCurvedC = true)
        hand[4] = NormalizedLandmark.create(0.28f, 0.75f, -0.04f)
        hand[8] = NormalizedLandmark.create(0.35f, 0.52f, -0.06f)
        assertLetter("c", hand)
    }

    @Test
    fun testLetterD() {
        // D: Index extended up, others closed
        val hand = createBaseHand(indexExtended = true)
        assertLetter("d", hand)
    }

    @Test
    fun testLetterE() {
        // E: 4 claw fingers, thumb directly under index yema
        val hand = createBaseHand()
        hand[8] = NormalizedLandmark.create(0.42f, 0.72f, 0.02f)
        hand[12] = NormalizedLandmark.create(0.50f, 0.70f, 0.02f)
        hand[16] = NormalizedLandmark.create(0.58f, 0.72f, 0.02f)
        hand[20] = NormalizedLandmark.create(0.65f, 0.74f, 0.02f)
        hand[4] = NormalizedLandmark.create(0.42f, 0.78f, 0.02f)
        assertLetter("e", hand)
    }

    @Test
    fun testLetterF() {
        // F: Thumb and index touch, middle, ring, pinky extended
        val hand = createBaseHand(
            middleExtended = true,
            ringExtended = true,
            pinkyExtended = true
        )
        hand[4] = NormalizedLandmark.create(0.42f, 0.65f, 0.0f)
        hand[8] = NormalizedLandmark.create(0.42f, 0.65f, 0.0f)
        assertLetter("f", hand)
    }

    @Test
    fun testLetterG() {
        // G: Index extended horizontally, thumb extended up
        val handG = createBaseHand(indexExtended = true, thumbExtended = true)
        val hand = rotateHand(handG, -Math.PI.toFloat() / 2f)
        assertLetter("g", hand)
    }

    @Test
    fun testLetterH() {
        // H: Index and middle extended horizontally together, thumb extended up
        val handH = createBaseHand(indexExtended = true, middleExtended = true, thumbExtended = true)
        val hand = rotateHand(handH, -Math.PI.toFloat() / 2f)
        assertLetter("h", hand)
    }

    @Test
    fun testLetterI() {
        // I: Pinky extended up, others closed
        val hand = createBaseHand(pinkyExtended = true)
        assertLetter("i", hand)
    }

    @Test
    fun testLetterK() {
        // K: Index up, middle forward, thumb between
        val hand = createBaseHand(indexExtended = true, middleExtended = true)
        hand[4] = NormalizedLandmark.create(0.46f, 0.55f, -0.02f)
        assertLetter("k", hand)
    }

    @Test
    fun testLetterL() {
        // L: Index extended up, thumb extended to side
        val hand = createBaseHand(indexExtended = true, thumbExtended = true)
        assertLetter("l", hand)
    }

    @Test
    fun testLetterM() {
        // M: 3 fingers draped down over thumb
        val hand = createBaseHand(isDrapedDown = true)
        hand[16] = NormalizedLandmark.create(0.58f, 0.88f, 0.06f)
        hand[4] = NormalizedLandmark.create(0.46f, 0.82f, 0.02f)
        assertLetter("m", hand)
    }

    @Test
    fun testLetterN() {
        // N: 2 fingers draped down over thumb, ring folded against palm
        val hand = createBaseHand(isDrapedDown = true)
        hand[14] = NormalizedLandmark.create(0.58f, 0.70f, 0.03f)
        hand[15] = NormalizedLandmark.create(0.58f, 0.68f, 0.05f)
        hand[16] = NormalizedLandmark.create(0.58f, 0.66f, 0.06f)
        hand[4] = NormalizedLandmark.create(0.46f, 0.82f, 0.02f)
        assertLetter("n", hand)
    }

    @Test
    fun testLetterO() {
        // O: Oval shape with curved fingers and tips touching thumb
        val hand = createBaseHand(indexExtended = true, middleExtended = true, ringExtended = true, pinkyExtended = true)
        // Index
        hand[6] = NormalizedLandmark.create(0.42f, 0.52f, -0.02f)
        hand[7] = NormalizedLandmark.create(0.43f, 0.55f, -0.04f)
        hand[8] = NormalizedLandmark.create(0.44f, 0.60f, -0.05f)
        // Middle
        hand[10] = NormalizedLandmark.create(0.48f, 0.51f, -0.02f)
        hand[11] = NormalizedLandmark.create(0.48f, 0.54f, -0.04f)
        hand[12] = NormalizedLandmark.create(0.48f, 0.59f, -0.05f)
        // Ring
        hand[14] = NormalizedLandmark.create(0.52f, 0.52f, -0.02f)
        hand[15] = NormalizedLandmark.create(0.52f, 0.55f, -0.04f)
        hand[16] = NormalizedLandmark.create(0.52f, 0.60f, -0.05f)
        // Pinky
        hand[18] = NormalizedLandmark.create(0.56f, 0.54f, -0.02f)
        hand[19] = NormalizedLandmark.create(0.56f, 0.57f, -0.04f)
        hand[20] = NormalizedLandmark.create(0.56f, 0.62f, -0.05f)
        // Thumb
        hand[2] = NormalizedLandmark.create(0.42f, 0.76f, -0.02f)
        hand[3] = NormalizedLandmark.create(0.43f, 0.70f, -0.04f)
        hand[4] = NormalizedLandmark.create(0.44f, 0.62f, -0.05f)
        assertLetter("o", hand)
    }

    @Test
    fun testLetterP() {
        // P: Index pointing diagonal down, thumb extended to side, middle/ring/pinky folded in palm
        val handP_base = createBaseHand(indexExtended = true, middleExtended = true)
        handP_base[4] = NormalizedLandmark.create(0.46f, 0.55f, -0.02f)
        val hand = rotateHand(handP_base, Math.PI.toFloat())
        assertLetter("p", hand)
    }

    @Test
    fun testLetterR() {
        // R: Index and middle crossed
        val hand = createBaseHand(
            indexExtended = true,
            middleExtended = true
        )
        val tempX = hand[8].x()
        hand[8] = NormalizedLandmark.create(hand[12].x(), hand[8].y(), hand[8].z())
        hand[12] = NormalizedLandmark.create(tempX, hand[12].y(), hand[12].z())
        assertLetter("r", hand)
    }

    @Test
    fun testLetterS() {
        // S: Fist, thumb over front of index
        val hand = createBaseHand()
        hand[4] = NormalizedLandmark.create(0.38f, 0.65f, -0.02f)
        assertLetter("s", hand)
    }

    @Test
    fun testLetterT() {
        // T: Fist, thumb between index and middle
        val hand = createBaseHand()
        hand[4] = NormalizedLandmark.create(0.46f, 0.64f, 0.02f)
        hand[8] = NormalizedLandmark.create(0.42f, 0.72f, -0.02f)
        assertLetter("t", hand)
    }

    @Test
    fun testLetterU() {
        // U: Index and middle extended, together
        val hand = createBaseHand(
            indexExtended = true,
            middleExtended = true
        )
        hand[8] = NormalizedLandmark.create(0.45f, 0.35f, -0.06f)
        hand[12] = NormalizedLandmark.create(0.47f, 0.33f, -0.06f)
        assertLetter("u", hand)
    }

    @Test
    fun testLetterV() {
        // V: Index and middle extended, separated
        val hand = createBaseHand(
            indexExtended = true,
            middleExtended = true
        )
        hand[8] = NormalizedLandmark.create(0.38f, 0.35f, -0.06f)
        hand[12] = NormalizedLandmark.create(0.54f, 0.33f, -0.06f)
        assertLetter("v", hand)
    }

    @Test
    fun testLetterW() {
        // W: Index, middle, ring extended
        val hand = createBaseHand(
            indexExtended = true,
            middleExtended = true,
            ringExtended = true
        )
        assertLetter("w", hand)
    }

    @Test
    fun testLetterX() {
        // X: Index hook, others folded
        val hand = createBaseHand()
        hand[6] = NormalizedLandmark.create(0.38f, 0.52f, 0.05f)
        hand[7] = NormalizedLandmark.create(0.40f, 0.56f, 0.05f)
        hand[8] = NormalizedLandmark.create(0.42f, 0.58f, 0.05f)
        hand[4] = NormalizedLandmark.create(0.35f, 0.75f, 0.02f)
        assertLetter("x", hand)
    }

    @Test
    fun testLetterY() {
        // Y: Thumb and pinky extended, others closed
        val hand = createBaseHand(
            thumbExtended = true,
            pinkyExtended = true
        )
        assertLetter("y", hand)
    }
}
