package com.example.nutriia.nutriente

import com.example.nutriia.sueldo.NivelIngreso

// ═══════════════════════════════════════════════════════════════════════════════
// NUTRIENTES ESTIMADOS DEL MENÚ SEMANAL
//
// Este archivo extiende el modelo existente de DietaEngine con campos de
// macros/micros ESTIMADOS por día de plan. No modifica la lógica de generación
// de recetas ni los estilos visuales — solo añade datos numéricos que
// NutrientesViewModel puede consumir.
//
// PASO 1 — Añade estos campos a tu clase ComidasDia (en Dietamodels.kt):
//
//   data class ComidasDia(
//       val desayuno:  String,
//       val colacion1: String,
//       val almuerzo:  String,
//       val colacion2: String,
//       val cena:      String,
//       val costoEstimadoDia: Double,
//       // ↓ campos nuevos — ponles default 0.0 para no romper constructores existentes
//       val caloriasEstimadas:  Double = 0.0,
//       val proteinasEstimadas: Double = 0.0,
//       val grasasEstimadas:    Double = 0.0,
//       val carbosEstimados:    Double = 0.0,
//       val hierroEstimado:     Double = 0.0,
//       val calcioEstimado:     Double = 0.0,
//       val vitaminaAEstimada:  Double = 0.0,
//       val vitaminaCEstimada:  Double = 0.0,
//       val zincEstimado:       Double = 0.0
//   )
//
// PASO 2 — En DietaEngine.generarPlanSemanal(), cuando construyas cada ComidasDia,
//          llama a NutriEstimadoEngine.estimarDia(...) y asigna los valores.
//
// PASO 3 — No cambia nada en DietaScreen ni en los estilos.
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Motor de estimación nutricional ligero.
 *
 * Traduce la etapa de edad + nivel de ingreso a valores promedio razonables
 * para cada día del plan semanal. No pretende exactitud clínica — es una
 * referencia orientativa coherente con la recomendación OMS que ya muestra
 * NutrientesScreen.
 *
 * Los valores se calculan como:
 *   • Base OMS para el rango de edad  (tabla estática de NutrienteModels.kt)
 *   • Ajuste ±10 % según nivel de ingreso (mejor calidad de ingredientes)
 *   • Variación aleatoria ±5 % por día para que los 7 días no sean idénticos
 */
object NutriEstimadoEngine {

    // ─── Tabla base OMS (espejo simplificado de recomendacionesOMS) ───────────

    private data class BaseNutri(
        val kcal: Double, val prot: Double, val grasas: Double, val carbos: Double,
        val hierro: Double, val calcio: Double, val vitA: Double, val vitC: Double, val zinc: Double
    )

    private fun baseParaEdad(meses: Int): BaseNutri = when {
        meses <  6 -> BaseNutri(550.0, 10.0, 31.0, 60.0,  0.27, 200.0, 400.0, 40.0, 2.0)
        meses <  9 -> BaseNutri(700.0, 13.5, 30.0, 95.0, 11.0,  260.0, 500.0, 50.0, 3.0)
        meses < 24 -> BaseNutri(1000.0,14.0, 35.0,135.0, 11.0,  700.0, 300.0, 50.0, 3.0)
        else       -> BaseNutri(1200.0,16.0, 40.0,160.0,  7.0,  700.0, 300.0, 15.0, 3.0)
    }

    // ─── Factores por nivel de ingreso ────────────────────────────────────────
    // Ingreso más alto → mayor variedad de proteína, frutas y verduras

    private fun factorNivel(nivel: NivelIngreso): Double = when (nivel) {
        NivelIngreso.BASICO     -> 0.88   // restricción calórica leve real en México
        NivelIngreso.MEDIO_BAJO -> 0.94
        NivelIngreso.MEDIO      -> 1.00
        NivelIngreso.ALTO       -> 1.05
    }

    // ─── API pública ──────────────────────────────────────────────────────────

    /**
     * Devuelve los nutrientes estimados para UN día del plan.
     * @param seed  Semilla de variación (usar diaIdx para estabilidad entre recomposiciones)
     */
    fun estimarDia(
        meses:  Int,
        nivel:  NivelIngreso,
        seed:   Int = 0
    ): NutriEstimadoDia {
        val base    = baseParaEdad(meses)
        val factor  = factorNivel(nivel)
        // Variación ±5 % determinista según día para que no cambie en recomposición
        val variacion = 1.0 + ((seed % 7) - 3) * 0.016

        fun ajustar(v: Double) = v * factor * variacion

        return NutriEstimadoDia(
            caloriasEstimadas  = ajustar(base.kcal),
            proteinasEstimadas = ajustar(base.prot),
            grasasEstimadas    = ajustar(base.grasas),
            carbosEstimados    = ajustar(base.carbos),
            hierroEstimado     = ajustar(base.hierro),
            calcioEstimado     = ajustar(base.calcio),
            vitaminaAEstimada  = ajustar(base.vitA),
            vitaminaCEstimada  = ajustar(base.vitC),
            zincEstimado       = ajustar(base.zinc)
        )
    }
}

/** DTO plano con los 9 valores — se mapea 1:1 a los campos nuevos de ComidasDia. */
data class NutriEstimadoDia(
    val caloriasEstimadas:  Double,
    val proteinasEstimadas: Double,
    val grasasEstimadas:    Double,
    val carbosEstimados:    Double,
    val hierroEstimado:     Double,
    val calcioEstimado:     Double,
    val vitaminaAEstimada:  Double,
    val vitaminaCEstimada:  Double,
    val zincEstimado:       Double
)