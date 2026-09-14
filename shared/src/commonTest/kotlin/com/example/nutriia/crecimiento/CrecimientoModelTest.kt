package com.example.nutriia.crecimiento

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CrecimientoModelTest {

    // ═══════════════════════════════════════════════════════════════════════════
    // PRUEBAS MENORES DE 2 AÑOS (0–24 MESES) — PESO-PARA-LONGITUD (WFL) OMS 2006
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun testCaso1_Nina2Meses_Normal() {
        // Niña, 2 meses, 5.1 kg, 57 cm
        val imc = 5.1 / (0.57 * 0.57)
        val res = interpretarIMC(imc = imc, meses = 2, sexo = Sexo.NINA, pesoKg = 5.1, tallaCm = 57.0)
        assertEquals("Normal", res.categoria)
        assertTrue(res.descripcion.contains("peso-para-longitud OMS"))
        assertTrue(res.indicador.contains("peso-para-longitud OMS"))
    }

    @Test
    fun testCaso2_Nina2Meses_Sobrepeso() {
        // Niña, 2 meses, 6.3 kg, 57 cm (peso elevado para 57 cm)
        val imc = 6.3 / (0.57 * 0.57)
        val res = interpretarIMC(imc = imc, meses = 2, sexo = Sexo.NINA, pesoKg = 6.3, tallaCm = 57.0)
        assertEquals("Sobrepeso", res.categoria)
        assertTrue(res.descripcion.contains("peso-para-longitud OMS"))
    }

    @Test
    fun testCaso3_Nina2Meses_63cm_Normal() {
        // Niña, 2 meses, 6.3 kg, 63 cm (peso normal para 63 cm)
        val imc = 6.3 / (0.63 * 0.63)
        val res = interpretarIMC(imc = imc, meses = 2, sexo = Sexo.NINA, pesoKg = 6.3, tallaCm = 63.0)
        assertEquals("Normal", res.categoria)
        assertTrue(res.descripcion.contains("peso-para-longitud OMS"))
    }

    @Test
    fun testDiferenciaCaso2y3_CambiarLongitudModificaResultado() {
        // Misma edad (2m), mismo peso (6.3 kg), diferente longitud (57cm vs 63cm)
        val res57 = interpretarIMC(imc = 6.3 / (0.57 * 0.57), meses = 2, sexo = Sexo.NINA, pesoKg = 6.3, tallaCm = 57.0)
        val res63 = interpretarIMC(imc = 6.3 / (0.63 * 0.63), meses = 2, sexo = Sexo.NINA, pesoKg = 6.3, tallaCm = 63.0)
        
        assertNotEquals(res57.categoria, res63.categoria, "Al cambiar longitud de 57cm a 63cm la categoría nutricional debe cambiar")
        assertEquals("Sobrepeso", res57.categoria)
        assertEquals("Normal", res63.categoria)
    }

    @Test
    fun testCaso4_Nina2Meses_BajoPesoSevero() {
        // Niña, 2 meses, 3.2 kg, 57 cm (< -3 DE OMS)
        val imc = 3.2 / (0.57 * 0.57)
        val res = interpretarIMC(imc = imc, meses = 2, sexo = Sexo.NINA, pesoKg = 3.2, tallaCm = 57.0)
        assertEquals("Bajo peso severo", res.categoria)
        assertTrue(res.descripcion.contains("peso-para-longitud OMS"))
    }

    @Test
    fun testCaso5_Nina2Meses_Obesidad() {
        // Niña, 2 meses, 7.0 kg, 51.7 cm (> +3 DE OMS)
        val imc = 7.0 / (0.517 * 0.517)
        val res = interpretarIMC(imc = imc, meses = 2, sexo = Sexo.NINA, pesoKg = 7.0, tallaCm = 51.7)
        assertEquals("Obesidad", res.categoria)
        assertTrue(res.descripcion.contains("peso-para-longitud OMS"))
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRUEBAS 5–12 AÑOS — WHO GROWTH REFERENCE 2007 (Z-SCORES OFICIALES)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun testNinas5a12Anos_CategoriasZScores() {
        // Probar niñas de 6, 7, 8, 9, 10, 11 y 12 años
        val edades = listOf(72, 84, 96, 108, 120, 132, 144)
        for (meses in edades) {
            val refLms = interpolarPuntoOMSLms(OMS_IMC_2007_NINAS, meses)
            
            // 1. Delgadez severa (Z < -3.0): evaluamos justo debajo de sd3neg
            val resDelgadezSev = interpretarIMC(imc = refLms.sd3neg - 0.2, meses = meses, sexo = Sexo.NINA, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Delgadez severa", resDelgadezSev.categoria, "Niña $meses m con IMC < -3DE debe ser Delgadez severa")
            
            // 2. Delgadez (-3 <= Z < -2): evaluamos en punto medio entre sd3neg y sd2neg
            val imcDelg = (refLms.sd3neg + refLms.sd2neg) / 2.0
            val resDelg = interpretarIMC(imc = imcDelg, meses = meses, sexo = Sexo.NINA, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Delgadez", resDelg.categoria, "Niña $meses m con IMC entre -3 y -2DE debe ser Delgadez")

            // 3. Normal (-2 <= Z <= +1): evaluamos en la Mediana (0 DE)
            val resNorm = interpretarIMC(imc = refLms.sd0, meses = meses, sexo = Sexo.NINA, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Normal", resNorm.categoria, "Niña $meses m en Mediana (0DE) debe ser Normal")

            // 4. Sobrepeso (+1 < Z <= +2): evaluamos en punto medio entre sd1pos y sd2pos
            val imcSobre = (refLms.sd1pos + refLms.sd2pos) / 2.0
            val resSobre = interpretarIMC(imc = imcSobre, meses = meses, sexo = Sexo.NINA, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Sobrepeso", resSobre.categoria, "Niña $meses m con IMC entre +1 y +2DE debe ser Sobrepeso")

            // 5. Obesidad (Z > +2.0): evaluamos por encima de sd2pos
            val resObesa = interpretarIMC(imc = refLms.sd2pos + 0.5, meses = meses, sexo = Sexo.NINA, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Obesidad", resObesa.categoria, "Niña $meses m con IMC > +2DE debe ser Obesidad")
        }
    }

    @Test
    fun testNinos5a12Anos_CategoriasZScores() {
        val edades = listOf(72, 84, 96, 108, 120, 132, 144)
        for (meses in edades) {
            val refLms = interpolarPuntoOMSLms(OMS_IMC_2007_NINOS, meses)
            
            // 1. Delgadez severa (Z < -3.0)
            val resDelgadezSev = interpretarIMC(imc = refLms.sd3neg - 0.2, meses = meses, sexo = Sexo.NINO, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Delgadez severa", resDelgadezSev.categoria)
            
            // 2. Delgadez (-3 <= Z < -2)
            val imcDelg = (refLms.sd3neg + refLms.sd2neg) / 2.0
            val resDelg = interpretarIMC(imc = imcDelg, meses = meses, sexo = Sexo.NINO, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Delgadez", resDelg.categoria)

            // 3. Normal (-2 <= Z <= +1)
            val resNorm = interpretarIMC(imc = refLms.sd0, meses = meses, sexo = Sexo.NINO, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Normal", resNorm.categoria)

            // 4. Sobrepeso (+1 < Z <= +2)
            val imcSobre = (refLms.sd1pos + refLms.sd2pos) / 2.0
            val resSobre = interpretarIMC(imc = imcSobre, meses = meses, sexo = Sexo.NINO, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Sobrepeso", resSobre.categoria)

            // 5. Obesidad (Z > +2.0)
            val resObeso = interpretarIMC(imc = refLms.sd2pos + 0.5, meses = meses, sexo = Sexo.NINO, pesoKg = 20.0, tallaCm = 120.0)
            assertEquals("Obesidad", resObeso.categoria)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // PRUEBAS DE FRONTERA EXACTA (OPERADORES <, <=, >, >=)
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    fun testFronterasExactas_Nina10Anos() {
        // Niña 10 años (120 meses):
        // sd3neg = 12.38 (Z = -3)
        // sd2neg = 13.47 (Z = -2)
        // sd1pos = 19.03 (Z = +1)
        // sd2pos = 22.57 (Z = +2)
        val meses = 120
        val sexo = Sexo.NINA
        val refLms = interpolarPuntoOMSLms(OMS_IMC_2007_NINAS, meses)

        // Frontera Z = -3 (sd3neg)
        val bajoZ3_debajo = interpretarIMC(imc = refLms.sd3neg - 0.05, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        val bajoZ3_exacto = interpretarIMC(imc = refLms.sd3neg, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        assertEquals("Delgadez severa", bajoZ3_debajo.categoria)
        assertEquals("Delgadez", bajoZ3_exacto.categoria)

        // Frontera Z = -2 (sd2neg)
        val bajoZ2_debajo = interpretarIMC(imc = refLms.sd2neg - 0.05, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        val bajoZ2_exacto = interpretarIMC(imc = refLms.sd2neg, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        assertEquals("Delgadez", bajoZ2_debajo.categoria)
        assertEquals("Normal", bajoZ2_exacto.categoria)

        // Frontera Z = +1 (sd1pos)
        val sobreZ1_exacto = interpretarIMC(imc = refLms.sd1pos, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        val sobreZ1_encima = interpretarIMC(imc = refLms.sd1pos + 0.05, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        assertEquals("Normal", sobreZ1_exacto.categoria)
        assertEquals("Sobrepeso", sobreZ1_encima.categoria)

        // Frontera Z = +2 (sd2pos)
        val obesoZ2_exacto = interpretarIMC(imc = refLms.sd2pos, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        val obesoZ2_encima = interpretarIMC(imc = refLms.sd2pos + 0.05, meses = meses, sexo = sexo, pesoKg = 30.0, tallaCm = 135.0)
        assertEquals("Sobrepeso", obesoZ2_exacto.categoria)
        assertEquals("Obesidad", obesoZ2_encima.categoria)
    }
}
