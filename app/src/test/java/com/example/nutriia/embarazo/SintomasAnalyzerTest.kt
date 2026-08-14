package com.example.nutriia.embarazo

import org.junit.Assert.assertEquals
import org.junit.Test

class SintomasAnalyzerTest {

    @Test
    fun testSintomasClasificacion() {
        // 1. Validar síntomas comunes de todas las semanas (debe ser NORMAL)
        for (semana in 1..40) {
            val info = obtenerInfoSintomas(semana)
            val trimestre = info.trimestre
            for (sintoma in info.sintomasEs) {
                val resultado = SintomasAnalyzer.analizarSintoma(sintoma, trimestre)
                if (resultado.nivel != NivelSintoma.NORMAL) {
                    println("FALLA ES: sintoma='$sintoma' (semana=$semana) resultado.nivel=${resultado.nivel} nombreEs='${resultado.nombreEs}' detEs='${resultado.detalleEs}'")
                }
                assertEquals(
                    "El síntoma '$sintoma' de la semana $semana debe clasificarse como NORMAL (encontró '${resultado.nombreEs}')",
                    NivelSintoma.NORMAL,
                    resultado.nivel
                )
            }
            for (sintoma in info.sintomasEn) {
                val resultado = SintomasAnalyzer.analizarSintoma(sintoma, trimestre)
                if (resultado.nivel != NivelSintoma.NORMAL) {
                    println("FALLA EN: sintoma='$sintoma' (semana=$semana) resultado.nivel=${resultado.nivel} nombreEn='${resultado.nombreEn}' detEn='${resultado.detalleEn}'")
                }
                assertEquals(
                    "El síntoma inglés '$sintoma' de la semana $semana debe clasificarse como NORMAL (encontró '${resultado.nombreEn}')",
                    NivelSintoma.NORMAL,
                    resultado.nivel
                )
            }
        }

        // 2. Validar molestias de consulta regular (debe ser CONSULTA)
        val consultaListEs = listOf(
            "Vómitos frecuentes (que dificultan la alimentación).",
            "Infección vaginal (flujo con mal olor, ardor o comezón).",
            "Ardor, dolor o molestias al orinar.",
            "Gripe, tos, fiebre menor a 38°C o diarrea.",
            "Aparición de ronchas o picazón persistente en la piel."
        )
        val consultaListEn = listOf(
            "Frequent vomiting (making eating difficult).",
            "Vaginal infection (smelly discharge, burning, or itching).",
            "Burning, pain, or discomfort when urinating.",
            "Flu, cough, fever under 38°C (100.4°F), or diarrhea.",
            "Hives or persistent itching on the skin."
        )

        for (sintoma in consultaListEs) {
            val resultado = SintomasAnalyzer.analizarSintoma(sintoma, 2)
            assertEquals(
                "La molestia '$sintoma' debe clasificarse como CONSULTA",
                NivelSintoma.CONSULTA,
                resultado.nivel
            )
        }
        for (sintoma in consultaListEn) {
            val resultado = SintomasAnalyzer.analizarSintoma(sintoma, 2)
            assertEquals(
                "La molestia inglesa '$sintoma' debe clasificarse como CONSULTA",
                NivelSintoma.CONSULTA,
                resultado.nivel
            )
        }

        // 3. Validar alertas de urgencia (debe ser URGENCIA)
        val alarmasEs = listOf(
            "Hinchazón (edema) repentina de manos, cara o pies.",
            "Ver lucecitas de colores (fosfenos) o visión borrosa.",
            "Zumbido constante de oídos (tinnitus).",
            "Dolor de cabeza muy intenso y persistente.",
            "Dolor agudo en la boca del estómago.",
            "Sangrado vaginal o salida de líquido por la vagina.",
            "Disminución notable o ausencia de movimientos del bebé.",
            "Contracciones dolorosas frecuentes antes de tiempo (semana 37)."
        )
        val alarmasEn = listOf(
            "Sudden swelling (edema) of hands, face, or feet.",
            "Seeing flashing lights or blurred vision.",
            "Constant ringing in the ears (tinnitus).",
            "Very intense and persistent headache.",
            "Sharp pain in the upper stomach.",
            "Vaginal bleeding or fluid leaking.",
            "Noticeable decrease or absence of baby movements.",
            "Frequent painful contractions before term (week 37)."
        )

        for (sintoma in alarmasEs) {
            val resultado = SintomasAnalyzer.analizarSintoma(sintoma, 3)
            assertEquals(
                "La señal de alarma '$sintoma' debe clasificarse como URGENCIA",
                NivelSintoma.URGENCIA,
                resultado.nivel
            )
        }
        for (sintoma in alarmasEn) {
            val resultado = SintomasAnalyzer.analizarSintoma(sintoma, 3)
            assertEquals(
                "La señal de alarma inglesa '$sintoma' debe clasificarse como URGENCIA",
                NivelSintoma.URGENCIA,
                resultado.nivel
            )
        }
    }
}
