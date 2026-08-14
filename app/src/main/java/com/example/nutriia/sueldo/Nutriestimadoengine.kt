package com.example.nutriia.sueldo


/**
 * Motor de estimación nutricional ligero para el menú semanal.
 *
 * Se activa SOLO cuando ComidasDiarias.caloriasEstimadas == 0.0,
 * es decir, cuando DietaEngine aún no llenó esos campos.
 * En cuanto DietaEngine los llene, este engine queda como fallback silencioso.
 *
 * Valores base OMS + ajuste por NivelIngreso + variación ±5 % por día
 * para que los 7 días no sean idénticos entre sí.
 */
object NutriEstimadoEngine {

    private data class BaseNutri(
        val kcal: Double, val prot: Double, val grasas: Double, val carbos: Double,
        val hierro: Double, val calcio: Double, val vitA: Double, val vitC: Double, val zinc: Double
    )

    private fun baseParaEdad(meses: Int): BaseNutri = when {
        meses <  6 -> BaseNutri(550.0,  10.0, 31.0,  60.0,  0.27, 200.0, 400.0, 40.0, 2.0)
        meses <  9 -> BaseNutri(700.0,  13.5, 30.0,  95.0, 11.0,  260.0, 500.0, 50.0, 3.0)
        meses < 24 -> BaseNutri(1000.0, 14.0, 35.0, 135.0, 11.0,  700.0, 300.0, 50.0, 3.0)
        else       -> BaseNutri(1200.0, 16.0, 40.0, 160.0,  7.0,  700.0, 300.0, 15.0, 3.0)
    }

    private fun factorNivel(nivel: NivelIngreso): Double = when (nivel) {
        NivelIngreso.BASICO     -> 0.88
        NivelIngreso.MEDIO_BAJO -> 0.94
        NivelIngreso.MEDIO      -> 1.00
        NivelIngreso.ALTO       -> 1.05
    }

    /**
     * @param seed  Usar el índice del día (0–6) para variación estable entre recomposiciones.
     */
    fun estimarDia(meses: Int, nivel: NivelIngreso, seed: Int = 0): NutriEstimadoDia {
        val base      = baseParaEdad(meses)
        val factor    = factorNivel(nivel)
        val variacion = 1.0 + ((seed % 7) - 3) * 0.016   // ±4.8 % determinista

        fun v(x: Double) = x * factor * variacion

        return NutriEstimadoDia(
            caloriasEstimadas  = v(base.kcal),
            proteinasEstimadas = v(base.prot),
            grasasEstimadas    = v(base.grasas),
            carbosEstimados    = v(base.carbos),
            hierroEstimado     = v(base.hierro),
            calcioEstimado     = v(base.calcio),
            vitaminaAEstimada  = v(base.vitA),
            vitaminaCEstimada  = v(base.vitC),
            zincEstimado       = v(base.zinc)
        )
    }
}

/** DTO con los 9 valores nutricionales estimados para un día. */
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