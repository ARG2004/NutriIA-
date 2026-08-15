package com.example.nutriia.embarazo

object GananciaPesoCalculator {

    data class RangoGanancia(val minKg: Double, val maxKg: Double)

    fun rangoPorImc(imc: Double): RangoGanancia = when {
        imc < 18.5 -> RangoGanancia(12.5, 18.0)
        imc < 25.0 -> RangoGanancia(11.5, 16.0)
        imc < 30.0 -> RangoGanancia(7.0, 11.5)
        else       -> RangoGanancia(5.0, 9.0)
    }

    // Ajustes normativos: adolescente → límite superior; talla <1.50 m → límite inferior
    fun rangoAjustado(imc: Double, edad: Int, tallaM: Double): RangoGanancia {
        val base = rangoPorImc(imc)
        return when {
            edad in 10..19      -> RangoGanancia(base.maxKg, base.maxKg)
            tallaM in 0.01..1.49 -> RangoGanancia(base.minKg, base.minKg)
            else -> base
        }
    }

    fun gananciaEsperadaAcumulada(semanas: Int, rango: RangoGanancia): Double {
        if (semanas <= 13) return 1.0
        val semanasRestantes = 40 - 13
        val fraccion = ((semanas - 13).coerceIn(0, semanasRestantes)).toDouble() / semanasRestantes
        val promedioRango = (rango.minKg + rango.maxKg) / 2
        return 1.0 + (promedioRango - 1.0) * fraccion
    }

    enum class EstadoGanancia { POR_DEBAJO, EN_RANGO, POR_ARRIBA, SIN_DATOS }

    fun evaluarEstado(gananciaActual: Double, esperadaAcumulada: Double): EstadoGanancia {
        val tolerancia = 1.5
        return when {
            gananciaActual < esperadaAcumulada - tolerancia -> EstadoGanancia.POR_DEBAJO
            gananciaActual > esperadaAcumulada + tolerancia -> EstadoGanancia.POR_ARRIBA
            else -> EstadoGanancia.EN_RANGO
        }
    }
}
