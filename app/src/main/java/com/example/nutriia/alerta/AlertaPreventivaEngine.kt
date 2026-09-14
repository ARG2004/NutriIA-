package com.example.nutriia.alerta

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.nutriia.crecimiento.MedicionCrecimiento
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.crecimiento.interpolarPuntoOMS
import com.example.nutriia.crecimiento.omsPesoPorSexo
import com.example.nutriia.expediente.AlimentoIntroducido
import com.example.nutriia.nutriente.RegistroNutrientes

enum class NivelSeveridadAlerta(val label: String, val color: Color) {
    ESTABLE("Estable", Color(0xFF43A047)),
    OBSERVACION("En Observación", Color(0xFFFFB300)),
    ATENCION_REQUERIDA("Atención Requerida", Color(0xFFE53935))
}

enum class CategoriaAlertaPreventiva(val label: String) {
    CRECIMIENTO_OMS("Trayectoria de Crecimiento OMS"),
    MICRONUTRIENTES("Balance de Micronutrientes"),
    INMUNOTOLERANCIA_ALERGENOS("Ventana Inmunológica y Alérgenos"),
    ADHERENCIA_PLAN("Adherencia y Registro")
}

data class AlertaPreventivaNutriologo(
    val id: String,
    val categoria: CategoriaAlertaPreventiva,
    val titulo: String,
    val hallazgoClinico: String,
    val recomendacionPadres: String,
    val glosaLSM: String,
    val severidad: NivelSeveridadAlerta,
    val icon: ImageVector,
    val metricaClave: String = ""
)

data class DiagnosticoPreventivoPaciente(
    val estadoGeneral: NivelSeveridadAlerta,
    val alertas: List<AlertaPreventivaNutriologo>,
    val resumenClinico: String
)

object AlertaPreventivaEngine {

    /**
     * Evalúa la telemetría clínica del paciente pediátrico:
     * 1. Curvas de crecimiento OMS y velocidad ponderal.
     * 2. Brecha de micronutrientes (Hierro, Zinc, Sodio).
     * 3. Ventana de introducción de alérgenos.
     * 4. Consistencia de registros.
     */
    fun evaluarPaciente(
        meses: Int,
        sexo: Sexo? = null,
        pesoActualKg: Double,
        tallaActualCm: Double,
        historialCrecimiento: List<MedicionCrecimiento>,
        alimentos: List<AlimentoIntroducido>,
        nutrientes: List<RegistroNutrientes> = emptyList()
    ): DiagnosticoPreventivoPaciente {
        val alertas = mutableListOf<AlertaPreventivaNutriologo>()

        // ── 1. EVALUACIÓN DE TRAYECTORIA PONDERAL OMS ────────────────────────
        val tablaOMS = omsPesoPorSexo(sexo)
        val pOMS = interpolarPuntoOMS(tablaOMS, meses.coerceIn(0, 144))
        val p3Fmt  = ((pOMS.p3  * 10.0).toInt()) / 10.0
        val p15Fmt = ((pOMS.p15 * 10.0).toInt()) / 10.0
        val p97Fmt = ((pOMS.p97 * 10.0).toInt()) / 10.0
        val pesoFmt = ((pesoActualKg * 10.0).toInt()) / 10.0

        if (pesoActualKg > 0.0) {
            when {
                pesoActualKg < pOMS.p3 -> {
                    alertas.add(
                        AlertaPreventivaNutriologo(
                            id = "peso_p3_critico",
                            categoria = CategoriaAlertaPreventiva.CRECIMIENTO_OMS,
                            titulo = "Desaceleración Ponderal por debajo de Percentil 3",
                            hallazgoClinico = "Peso actual (${pesoFmt} kg) ubicado bajo P3 OMS (${p3Fmt} kg) a los $meses meses.",
                            recomendacionPadres = "Aumentar densidad energética en tomas/comidas y programar valoración pediátrica de absorción.",
                            glosaLSM = "AVISO MEDICO: PESO BEBE BAJO PERCENTIL 3. COMER MAS CALORIAS Y REVISAR DOCTOR.",
                            severidad = NivelSeveridadAlerta.ATENCION_REQUERIDA,
                            icon = Icons.AutoMirrored.Rounded.TrendingDown,
                            metricaClave = "${pesoFmt} kg (P < 3)"
                        )
                    )
                }
                pesoActualKg < pOMS.p15 -> {
                    alertas.add(
                        AlertaPreventivaNutriologo(
                            id = "peso_p15_observacion",
                            categoria = CategoriaAlertaPreventiva.CRECIMIENTO_OMS,
                            titulo = "Monitoreo de Curva Ponderal (Percentil 3 a 15)",
                            hallazgoClinico = "Peso actual (${pesoFmt} kg) en rango P3-P15 OMS a los $meses meses.",
                            recomendacionPadres = "Mantener frecuencia de tomas y asegurar aporte de proteínas de alto valor biológico.",
                            glosaLSM = "AVISO MEDICO: PESO BEBE RANGO BAJO. CUIDAR COMIDA PROTEINA DIARIA.",
                            severidad = NivelSeveridadAlerta.OBSERVACION,
                            icon = Icons.Rounded.Scale,
                            metricaClave = "${pesoFmt} kg (P 3-15)"
                        )
                    )
                }
                pesoActualKg > pOMS.p97 -> {
                    alertas.add(
                        AlertaPreventivaNutriologo(
                            id = "peso_p97_exceso",
                            categoria = CategoriaAlertaPreventiva.CRECIMIENTO_OMS,
                            titulo = "Velocidad de Ganancia Acelerada (> P97)",
                            hallazgoClinico = "Peso actual (${pesoFmt} kg) superior a P97 OMS (${p97Fmt} kg) a los $meses meses.",
                            recomendacionPadres = "Priorizar lactancia a demanda o porciones reguladas según apetito; evitar azúcares o harinas refinadas.",
                            glosaLSM = "AVISO MEDICO: PESO BEBE ALTO PERCENTIL 97. CUIDAR PORCION COMIDA Y EVITAR AZUCAR.",
                            severidad = NivelSeveridadAlerta.OBSERVACION,
                            icon = Icons.AutoMirrored.Rounded.TrendingUp,
                            metricaClave = "${pesoFmt} kg (P > 97)"
                        )
                    )
                }
            }
        }

        // Si hay historial de mediciones múltiples, evaluar velocidad de cambio (cruce de percentiles)
        if (historialCrecimiento.size >= 2) {
            val ordenadas = historialCrecimiento.sortedBy { it.fechaEpoch() }
            val primera = ordenadas.first()
            val ultima = ordenadas.last()
            if (primera.pesoKg > 0.0 && ultima.pesoKg > 0.0 && primera.pesoKg > ultima.pesoKg && meses >= 6) {
                alertas.add(
                    AlertaPreventivaNutriologo(
                        id = "perdida_peso_reciente",
                        categoria = CategoriaAlertaPreventiva.CRECIMIENTO_OMS,
                        titulo = "Pérdida de Peso Involuntaria Detectada",
                        hallazgoClinico = "Descenso de ${primera.pesoKg} kg a ${ultima.pesoKg} kg entre registros clínicos.",
                        recomendacionPadres = "Verificar hidratación, episodios de diarrea o rechazo alimentario reciente.",
                        glosaLSM = "ALERTA MEDICA: BEBE BAJO DE PESO RECIENTE. REVISAR SI ENFERMO O DIARREA.",
                        severidad = NivelSeveridadAlerta.ATENCION_REQUERIDA,
                        icon = Icons.Rounded.WarningAmber,
                        metricaClave = "-${((primera.pesoKg - ultima.pesoKg) * 10.0).toInt() / 10.0} kg"
                    )
                )
            }
        }

        // ── 2. EVALUACIÓN DE INMUNOTOLERANCIA Y ALÉRGENOS ────────────────────
        if (meses in 7..12) {
            val nombresAlergenos = listOf("huevo", "cacahuate", "cacahuete", "pescado", "soya", "trigo")
            val probados = alimentos.map { it.nombre.lowercase() }
            val alergenosProbados = nombresAlergenos.filter { al -> probados.any { it.contains(al) } }

            if (alergenosProbados.isEmpty()) {
                alertas.add(
                    AlertaPreventivaNutriologo(
                        id = "ventana_alergenos_iniciar",
                        categoria = CategoriaAlertaPreventiva.INMUNOTOLERANCIA_ALERGENOS,
                        titulo = "Ventana de Inmunotolerancia Activa (7-12 meses)",
                        hallazgoClinico = "Paciente de $meses meses sin registro de introducción de alérgenos principales.",
                        recomendacionPadres = "Iniciar protocolo de 3 días con huevo cocido o cacahuate en polvo para inducir tolerancia inmunológica según guías OMS.",
                        glosaLSM = "AVISO NUTRIOLOGO: BEBE EDAD BUENA PARA PROBAR HUEVO Y CACAHUATE. REGLA 3 DIAS.",
                        severidad = NivelSeveridadAlerta.OBSERVACION,
                        icon = Icons.Rounded.Shield,
                        metricaClave = "0/6 Alérgenos"
                    )
                )
            }
        }

        // ── 3. EVALUACIÓN DE MICRONUTRIENTES (HIERRO / SODIO) ────────────────
        if (nutrientes.isNotEmpty() && meses >= 6) {
            val ultimosRegistros = nutrientes.takeLast(7)
            val promHierro = ultimosRegistros.map { it.micros.hierro }.average()

            // Requerimiento de hierro en AC (6-12m: ~11mg/día)
            if (promHierro in 0.01..5.0) {
                alertas.add(
                    AlertaPreventivaNutriologo(
                        id = "deficit_hierro_prevencion",
                        categoria = CategoriaAlertaPreventiva.MICRONUTRIENTES,
                        titulo = "Riesgo de Brecha de Hierro Pediátrico",
                        hallazgoClinico = "Ingesta promedio de hierro estimada en ${((promHierro * 10.0).toInt() / 10.0)} mg/día (< 60% IDR).",
                        recomendacionPadres = "Incluir diariamente fuentes de hierro hemo (hígado de pollo, carne de res molida) o leguminosas con vitamina C.",
                        glosaLSM = "AVISO NUTRIOLOGO: FALTA HIERRO COMIDA. COMER MAS FRIJOL LENTEJA CARNE.",
                        severidad = NivelSeveridadAlerta.ATENCION_REQUERIDA,
                        icon = Icons.Rounded.Bloodtype,
                        metricaClave = "${((promHierro * 10.0).toInt() / 10.0)} mg/día"
                    )
                )
            }
        }

        // ── 4. EVALUACIÓN DE REGISTROS ALIMENTARIOS ──────────────────────────
        if (meses >= 6 && alimentos.isEmpty() && nutrientes.isEmpty()) {
            alertas.add(
                AlertaPreventivaNutriologo(
                    id = "sin_registros_alimentarios",
                    categoria = CategoriaAlertaPreventiva.ADHERENCIA_PLAN,
                    titulo = "Sin Registro de Alimentación Complementaria",
                    hallazgoClinico = "Paciente de $meses meses sin registro de alimentos introducidos.",
                    recomendacionPadres = "Comenzar el diario de alimentos para dar seguimiento al plan y asegurar la variedad de grupos nutricionales.",
                    glosaLSM = "AVISO: REGISTRAR COMIDA DIARIA BEBE EN APLICACION.",
                    severidad = NivelSeveridadAlerta.OBSERVACION,
                    icon = Icons.AutoMirrored.Rounded.ListAlt,
                    metricaClave = "0 Registros"
                )
            )
        }

        // Estado General
        val estadoGeneral = when {
            alertas.any { it.severidad == NivelSeveridadAlerta.ATENCION_REQUERIDA } -> NivelSeveridadAlerta.ATENCION_REQUERIDA
            alertas.any { it.severidad == NivelSeveridadAlerta.OBSERVACION } -> NivelSeveridadAlerta.OBSERVACION
            else -> NivelSeveridadAlerta.ESTABLE
        }

        val resumenClinico = when (estadoGeneral) {
            NivelSeveridadAlerta.ESTABLE -> "Biometría y telemetría nutricional en rango óptimo de acuerdo a los estándares de la OMS."
            NivelSeveridadAlerta.OBSERVACION -> "Se detectaron ${alertas.size} indicadores preventivos para monitoreo en consulta."
            NivelSeveridadAlerta.ATENCION_REQUERIDA -> "Se identificaron ${alertas.count { it.severidad == NivelSeveridadAlerta.ATENCION_REQUERIDA }} factores críticos que requieren ajuste en el plan de alimentación."
        }

        return DiagnosticoPreventivoPaciente(
            estadoGeneral = estadoGeneral,
            alertas = alertas,
            resumenClinico = resumenClinico
        )
    }
}
