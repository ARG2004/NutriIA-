package com.example.nutriia.lactancia

import com.example.nutriia.shared.Timestamp
import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS — MODULO LACTANCIA
// Fuente: WHO — Infant and Young Child Feeding (2023)
// https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding
// Fuente: UNICEF — Early Childhood Nutrition (2023)
// https://www.unicef.org/nutrition/early-childhood-nutrition
// ═══════════════════════════════════════════════════════════════════════════

@Serializable
enum class BreastSide(val label: String) {
    LEFT("Pecho izquierdo"),
    RIGHT("Pecho derecho"),
    BOTH("Ambos pechos"),
    FORMULA("Fórmula")
}

/** Registro de una toma */
@Serializable
data class FeedingLog(
    val id: String = "",
    val childId: String = "",
    val userId: String = "",
    val date: String = "",              // "yyyy-MM-dd"
    val startTime: String = "",         // "HH:mm"
    val durationMinutes: Int = 0,
    val side: String = BreastSide.LEFT.name,
    val formulaMl: Int = 0,
    val notes: String = "",
    val createdAt: Timestamp? = null
)

/** Resumen diario calculado desde los registros */
data class DailyFeedingSummary(
    val date: String = "",
    val totalSessions: Int = 0,
    val totalMinutes: Int = 0,
    val totalFormulaMl: Int = 0,
    val leftSessions: Int = 0,
    val rightSessions: Int = 0,
    val formulaSessions: Int = 0,
    val avgIntervalMinutes: Int = 0
)

// ═══════════════════════════════════════════════════════════════════════════
// RECOMENDACIONES OMS — LACTANCIA
//
// Fuentes primarias verificadas:
//   • WHO Fact Sheet "Infant and young child feeding" (20 dic 2023)
//     https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding
//   • UNICEF "Early Childhood Nutrition" (2023)
//     https://www.unicef.org/nutrition/early-childhood-nutrition
//   • The Lancet Breastfeeding Series (Victora et al., 2016)
//
// Notas de alineación con las fuentes:
//   – La OMS recomienda inicio de lactancia en la 1.ª hora de vida.
//   – Lactancia EXCLUSIVA los primeros 6 meses (sin agua, jugos ni fórmula).
//   – Continuar lactancia + alimentos complementarios hasta 2 años o más.
//   – Leche materna aporta ~50 % de energía entre 6-12 m y ~33 % entre 12-24 m.
//   – Frecuencias: 8-12 tomas/día en recién nacidos, disminuyendo con la edad.
// ═══════════════════════════════════════════════════════════════════════════

data class OmsLactanciaRecommendation(
    val ageLabel: String,
    val minAgeMonths: Int,
    val maxAgeMonths: Int,
    val feedingsPerDay: String,
    val minIntervalHours: Float,
    val maxIntervalHours: Float,
    // Estimación clínica orientativa — la OMS no especifica duración promedio de toma.
    // Fuente: Academia Americana de Pediatría / práctica clínica estándar.
    val avgDurationMinutes: Int,
    val keyFacts: List<String>,
    val alertIfLessThan: Int,
    val sourceUrl: String = "https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding",
    val sourceLabel: String = "WHO — Infant and Young Child Feeding, 2023"
)

val omsLactanciaData: List<OmsLactanciaRecommendation> = listOf(

    // ── 0-1 mes: recién nacido ────────────────────────────────────────────
    // OMS: 8-12 tomas/día, lactancia a demanda, inicio en la 1.ª hora de vida.
    OmsLactanciaRecommendation(
        ageLabel           = "Recién nacido (0–4 semanas)",
        minAgeMonths       = 0,
        maxAgeMonths       = 1,
        feedingsPerDay     = "8–12 tomas",
        minIntervalHours   = 1.5f,
        maxIntervalHours   = 3f,
        avgDurationMinutes = 15,
        keyFacts = listOf(
            "La OMS recomienda iniciar la lactancia en la primera hora de vida para reducir la mortalidad neonatal.",
            "La lactancia a demanda (día y noche) estimula la producción de leche materna.",
            "El calostro, producido los primeros 3–5 días, es rico en anticuerpos esenciales para el recién nacido.",
            "No dar agua, jugos ni fórmula durante la lactancia exclusiva, salvo indicación médica (OMS/UNICEF).",
            "La posición correcta al amamantar previene grietas y mastitis."
        ),
        alertIfLessThan = 8
    ),

    // ── 1-3 meses ─────────────────────────────────────────────────────────
    // OMS: lactancia exclusiva continúa; leche madura con alto contenido de grasa.
    OmsLactanciaRecommendation(
        ageLabel           = "1–3 meses",
        minAgeMonths       = 1,
        maxAgeMonths       = 3,
        feedingsPerDay     = "7–9 tomas",
        minIntervalHours   = 2f,
        maxIntervalHours   = 3.5f,
        avgDurationMinutes = 15,
        keyFacts = listOf(
            "La leche materna madura contiene los nutrientes exactos que el bebé necesita, incluyendo grasas esenciales para el desarrollo cerebral.",
            "El bebé regula su propia ingesta; confía en sus señales de hambre y saciedad — alimentación responsiva (OMS).",
            "Mojar 6 o más pañales al día es indicador de hidratación y alimentación adecuada.",
            "La lactancia exclusiva reduce el riesgo de infecciones gastrointestinales y respiratorias.",
            "Amamantar a demanda, día y noche, mantiene la producción de leche materna (OMS, Ten Steps to Successful Breastfeeding)."
        ),
        alertIfLessThan = 6
    ),

    // ── 3-6 meses ─────────────────────────────────────────────────────────
    // OMS: lactancia EXCLUSIVA hasta los 6 meses — sin excepción salvo criterio médico.
    OmsLactanciaRecommendation(
        ageLabel           = "3–6 meses",
        minAgeMonths       = 3,
        maxAgeMonths       = 6,
        feedingsPerDay     = "6–8 tomas",
        minIntervalHours   = 2.5f,
        maxIntervalHours   = 4f,
        avgDurationMinutes = 12,
        keyFacts = listOf(
            "La OMS y UNICEF recomiendan lactancia EXCLUSIVA durante los primeros 6 meses de vida.",
            "No introducir alimentos sólidos antes de los 6 meses: aumenta el riesgo de infecciones y no aporta beneficios nutricionales adicionales.",
            "La leche materna cubre el 100 % de los requerimientos de energía y nutrientes durante este período.",
            "La lactancia exclusiva protege contra infecciones gastrointestinales tanto en países en desarrollo como industrializados (OMS, 2023).",
            "Continuar la lactancia reduce el riesgo de obesidad, diabetes tipo 2 y está asociada a mejor coeficiente intelectual y mayor asistencia escolar (Victora et al., The Lancet, 2016)."
        ),
        alertIfLessThan = 5
    ),

    // ── 6-12 meses ────────────────────────────────────────────────────────
    // OMS: lactancia + alimentos complementarios. La leche materna aporta ~50 % de energía.
    OmsLactanciaRecommendation(
        ageLabel           = "6–12 meses",
        minAgeMonths       = 6,
        maxAgeMonths       = 12,
        feedingsPerDay     = "4–6 tomas",
        minIntervalHours   = 3f,
        maxIntervalHours   = 5f,
        avgDurationMinutes = 10,
        keyFacts = listOf(
            "Continuar lactancia junto con alimentos complementarios. La leche materna sigue siendo fuente clave de energía y nutrientes (OMS, 2023).",
            "La leche materna puede aportar la mitad o más de la energía del niño entre los 6 y 12 meses (OMS, 2023).",
            "Introducir alimentos ricos en hierro desde los 6 meses: carnes, legumbres y cereales fortificados (OMS).",
            "2–3 comidas complementarias al día entre 6–8 meses; 3–4 comidas entre 9–12 meses, con 1–2 snacks según apetito (OMS).",
            "Continuar amamantando a demanda; la lactancia durante enfermedad reduce la mortalidad en niños desnutridos (OMS, 2023)."
        ),
        alertIfLessThan = 3
    ),

    // ── 12-24 meses ───────────────────────────────────────────────────────
    // OMS: recomienda continuar lactancia hasta los 2 años o más. Aporta ~33 % de energía.
    OmsLactanciaRecommendation(
        ageLabel           = "12–24 meses",
        minAgeMonths       = 12,
        maxAgeMonths       = 24,
        feedingsPerDay     = "2–3 tomas",
        minIntervalHours   = 4f,
        maxIntervalHours   = 8f,
        avgDurationMinutes = 8,
        keyFacts = listOf(
            "La OMS recomienda continuar la lactancia materna hasta los 2 años de edad o más.",
            "La leche materna aporta aproximadamente el 33 % de la energía del niño entre los 12 y 24 meses (OMS, 2023).",
            "Mantener la lactancia está asociado a menor riesgo de obesidad y diabetes tipo 2 en la vida adulta.",
            "La leche materna continúa aportando protección inmunológica y factores de crecimiento.",
            "El destete natural, respetando el ritmo del niño, es el enfoque recomendado por UNICEF."
        ),
        alertIfLessThan = 1,
        sourceUrl   = "https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding",
        sourceLabel = "WHO — Infant and Young Child Feeding, 2023"
    )
)

// ═══════════════════════════════════════════════════════════════════════════
// SEÑALES DE HAMBRE Y SACIEDAD
// Fuente: UNICEF / WHO — Feeding your baby (2023)
// https://www.unicef.org/nutrition/early-childhood-nutrition
// ═══════════════════════════════════════════════════════════════════════════

/** Señales de hambre tempranas vs tardías (UNICEF / OMS) */
val hungerCues = mapOf(
    "Tempranas — actuar ahora" to listOf(
        "Mueve la cabeza de lado a lado buscando el pecho (reflejo de búsqueda)",
        "Lleva las manos a la boca",
        "Abre y cierra la boca",
        "Se chupa los labios o la lengua",
        "Se agita o mueve inquieto"
    ),
    "Tardías — bebé muy hambriento" to listOf(
        "Llanto intenso (señal tardía; es más difícil amamantar cuando el bebé llora)",
        "Cara enrojecida",
        "Movimientos bruscos de brazos y piernas",
        "Dificultad para engancharse correctamente al pecho"
    )
)

/** Señales de llenado / saciedad (UNICEF / OMS) */
val fullnessCues = listOf(
    "Se suelta del pecho por sí solo",
    "Cierra la boca o vuelve la cara",
    "Manos abiertas y relajadas",
    "Se queda dormido después de comer satisfecho",
    "Moja 6 o más pañales al día (indicador de ingesta adecuada)"
)