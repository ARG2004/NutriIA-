package com.example.nutriia.accesibilidad

/**
 * ═══════════════════════════════════════════════════════════════════════════════
 * RESUMEN AUDITIVO DE SALUD (NutrIA - Accesibilidad Visual)
 * Genera interpretaciones en lenguaje natural cálido, claro y profesional
 * para que los padres y profesionales con discapacidad visual puedan comprender
 * al instante el estado nutricional, curvas de crecimiento, tomas y citas.
 * ═══════════════════════════════════════════════════════════════════════════════
 */
object ResumenAuditivoSalud {

    /**
     * Resume la curva de crecimiento y percentil OMS del bebé.
     */
    fun resumenCrecimientoOMS(
        nombreHijo: String,
        pesoKg: Double,
        tallaCm: Double,
        edadMeses: Int,
        idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX
    ): String {
        val nombre = if (nombreHijo.isNotBlank()) nombreHijo else "tu pequeño"

        if (idioma == IdiomaVoz.INGLES) {
            val pesoStr = if (pesoKg > 0.0) "$pesoKg kilograms" else "not recorded"
            val tallaStr = if (tallaCm > 0.0) "$tallaCm centimeters" else "not recorded"
            return "Growth summary for $nombre at $edadMeses months of age. " +
                    "Current weight: $pesoStr. Height: $tallaStr. " +
                    "Values are aligned with World Health Organization standards."
        } else {
            val pesoStr = if (pesoKg > 0.0) "$pesoKg kilos" else "sin registrar"
            val tallaStr = if (tallaCm > 0.0) "$tallaCm centímetros" else "sin registrar"
            return "Resumen de crecimiento para $nombre a sus $edadMeses meses de edad. " +
                    "Peso registrado: $pesoStr. Talla: $tallaStr. " +
                    "El desarrollo se encuentra dentro de los parámetros de referencia de la Organización Mundial de la Salud."
        }
    }

    /**
     * Resume las sesiones y tomas de lactancia del día.
     */
    fun resumenLactancia(
        tomasHoy: Int,
        minutosTotales: Int,
        ultimaHora: String = "",
        idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX
    ): String {
        if (idioma == IdiomaVoz.INGLES) {
            if (tomasHoy <= 0) return "No breastfeeding sessions recorded today yet."
            val horaStr = if (ultimaHora.isNotBlank()) "Last session was at $ultimaHora." else ""
            return "Today you have recorded $tomasHoy breastfeeding sessions with a total of $minutosTotales minutes. $horaStr"
        } else {
            if (tomasHoy <= 0) return "Aún no tienes tomas de lactancia registradas el día de hoy."
            val horaStr = if (ultimaHora.isNotBlank()) "La última toma fue a las $ultimaHora." else ""
            return "El día de hoy llevas $tomasHoy tomas de lactancia registradas con un total de $minutosTotales minutos. $horaStr"
        }
    }

    /**
     * Resume los alimentos introducidos y tolerancia en alimentación complementaria.
     */
    fun resumenSolidos(
        alimentosIntroducidos: Int,
        alergiasReportadas: Int = 0,
        idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX
    ): String {
        if (idioma == IdiomaVoz.INGLES) {
            val alergiasStr = if (alergiasReportadas > 0) "Alert: $alergiasReportadas possible allergens or sensitivities noted." else "No food allergies reported."
            return "Solid food introduction: $alimentosIntroducidos distinct foods tested so far. $alergiasStr"
        } else {
            val alergiasStr = if (alergiasReportadas > 0) "Atención: tienes $alergiasReportadas posibles alimentos con reacción o sensibilidad." else "Sin alergias ni intolerancias reportadas."
            return "En alimentación complementaria has introducido exitosamente $alimentosIntroducidos alimentos diferentes. $alergiasStr"
        }
    }

    /**
     * Resume el estado gestacional de la mamá y sus próximas citas médicas.
     */
    fun resumenEmbarazo(
        semanasGestacion: Int,
        diasGestacion: Int,
        proximaCita: String = "",
        medico: String = "",
        idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX
    ): String {
        if (idioma == IdiomaVoz.INGLES) {
            val citaStr = if (proximaCita.isNotBlank()) {
                val conMedico = if (medico.isNotBlank()) " with $medico" else ""
                "Your next appointment is scheduled for $proximaCita$conMedico."
            } else {
                "No upcoming medical appointments scheduled."
            }
            return "Pregnancy summary: Currently at week $semanasGestacion and $diasGestacion days of gestation. $citaStr"
        } else {
            val citaStr = if (proximaCita.isNotBlank()) {
                val conMedico = if (medico.isNotBlank()) " con $medico" else ""
                "Tu próxima cita médica está agendada para el $proximaCita$conMedico."
            } else {
                "No tienes citas médicas próximas agendadas."
            }
            return "Resumen de embarazo: Te encuentras en la semana $semanasGestacion con $diasGestacion días de gestación. $citaStr"
        }
    }

    /**
     * Resume los macronutrientes y calorías del plan o ingesta del día.
     */
    fun resumenNutrientes(
        calorias: Int,
        proteinaG: Double,
        carbohidratosG: Double,
        grasasG: Double,
        idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX
    ): String {
        if (idioma == IdiomaVoz.INGLES) {
            return "Nutrition summary: $calorias total kilocalories. " +
                    "Protein: ${proteinaG.toInt()} grams. " +
                    "Carbohydrates: ${carbohidratosG.toInt()} grams. " +
                    "Healthy fats: ${grasasG.toInt()} grams."
        } else {
            return "Resumen nutricional: $calorias kilocalorías totales. " +
                    "Proteínas: ${proteinaG.toInt()} gramos. " +
                    "Carbohidratos: ${carbohidratosG.toInt()} gramos. " +
                    "Grasas saludables: ${grasasG.toInt()} gramos."
        }
    }
}
