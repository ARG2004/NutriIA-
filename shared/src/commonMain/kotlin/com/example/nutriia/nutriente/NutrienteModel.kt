package com.example.nutriia.nutriente

import kotlinx.serialization.Serializable


// ─── Rangos de edad según OMS ─────────────────────────────────────────────────
// 0–6 meses: lactancia exclusiva
// 6–8 meses: inicio alimentación complementaria (2–3 comidas/día)
// 9–23 meses: 3–4 comidas/día + 1–2 snacks
// 24+ meses: dieta variada

@Serializable
enum class RangoEdad(val label: String, val mesesMin: Int, val mesesMax: Int) {
    CERO_SEIS(     "0 – 6 meses",   0,   6),
    SEIS_OCHO(     "6 – 8 meses",   6,   8),
    NUEVE_VEINTITRES("9 – 23 meses", 9,  23),
    VEINTICUATRO_MAS("24+ meses",   24, Int.MAX_VALUE)
}

fun rangoEdadDesde(meses: Int): RangoEdad =
    RangoEdad.entries.lastOrNull { meses >= it.mesesMin } ?: RangoEdad.CERO_SEIS

// ─── Macronutrientes ──────────────────────────────────────────────────────────

@Serializable
data class Macronutrientes(
    val calorias:      Double = 0.0,   // kcal
    val proteinas:     Double = 0.0,   // g
    val grasas:        Double = 0.0,   // g
    val carbohidratos: Double = 0.0    // g
)

// ─── Micronutrientes ──────────────────────────────────────────────────────────

@Serializable
data class Micronutrientes(
    val hierro:     Double = 0.0,   // mg
    val calcio:     Double = 0.0,   // mg
    val vitaminaA:  Double = 0.0,   // µg RAE
    val vitaminaC:  Double = 0.0,   // mg
    val zinc:       Double = 0.0    // mg
)

// ─── Recomendación OMS por rango ─────────────────────────────────────────────

data class RecomendacionOMS(
    val rangoEdad:      RangoEdad,
    val comidasPorDia:  IntRange,          // ej. 2..3
    val snacksPorDia:   IntRange,          // ej. 0..1
    val macros:         Macronutrientes,
    val micros:         Micronutrientes,
    val notas:          List<String>       // tips textuales de la OMS
)

// Tabla estática de referencia OMS
val recomendacionesOMS: Map<RangoEdad, RecomendacionOMS> = mapOf(
    RangoEdad.CERO_SEIS to RecomendacionOMS(
        rangoEdad     = RangoEdad.CERO_SEIS,
        comidasPorDia = 0..0,
        snacksPorDia  = 0..0,
        macros = Macronutrientes(calorias = 550.0, proteinas = 10.0, grasas = 31.0, carbohidratos = 60.0),
        micros = Micronutrientes(hierro = 0.27, calcio = 200.0, vitaminaA = 400.0, vitaminaC = 40.0, zinc = 2.0),
        notas = listOf(
            "Lactancia materna exclusiva durante los primeros 6 meses.",
            "No introducir agua ni otros líquidos.",
            "La leche materna cubre todas las necesidades nutricionales."
        )
    ),
    RangoEdad.SEIS_OCHO to RecomendacionOMS(
        rangoEdad     = RangoEdad.SEIS_OCHO,
        comidasPorDia = 2..3,
        snacksPorDia  = 0..1,
        macros = Macronutrientes(calorias = 700.0, proteinas = 13.5, grasas = 30.0, carbohidratos = 95.0),
        micros = Micronutrientes(hierro = 11.0, calcio = 260.0, vitaminaA = 500.0, vitaminaC = 50.0, zinc = 3.0),
        notas = listOf(
            "Iniciar con pequeñas cantidades e ir aumentando gradualmente.",
            "Continuar con lactancia materna.",
            "Introducir alimentos de consistencia suave (papillas, purés).",
            "Practicar alimentación responsiva: alimentar despacio y con paciencia."
        )
    ),
    RangoEdad.NUEVE_VEINTITRES to RecomendacionOMS(
        rangoEdad     = RangoEdad.NUEVE_VEINTITRES,
        comidasPorDia = 3..4,
        snacksPorDia  = 1..2,
        macros = Macronutrientes(calorias = 1000.0, proteinas = 14.0, grasas = 35.0, carbohidratos = 135.0),
        micros = Micronutrientes(hierro = 11.0, calcio = 700.0, vitaminaA = 300.0, vitaminaC = 50.0, zinc = 3.0),
        notas = listOf(
            "Aumentar variedad y consistencia de los alimentos.",
            "3–4 comidas principales + 1–2 snacks nutritivos.",
            "Usar alimentos enriquecidos o suplementos de vitaminas/minerales si es necesario.",
            "Durante la enfermedad, aumentar líquidos y ofrecer alimentos favoritos blandos."
        )
    ),
    RangoEdad.VEINTICUATRO_MAS to RecomendacionOMS(
        rangoEdad     = RangoEdad.VEINTICUATRO_MAS,
        comidasPorDia = 3..5,
        snacksPorDia  = 1..2,
        macros = Macronutrientes(calorias = 1200.0, proteinas = 16.0, grasas = 40.0, carbohidratos = 160.0),
        micros = Micronutrientes(hierro = 7.0, calcio = 700.0, vitaminaA = 300.0, vitaminaC = 15.0, zinc = 3.0),
        notas = listOf(
            "Dieta variada con todos los grupos alimenticios.",
            "Mantener lactancia materna si es posible hasta los 2 años o más.",
            "Fomentar autonomía en la alimentación."
        )
    )
)

// ─── Registro diario de nutrientes ───────────────────────────────────────────

@Serializable
data class RegistroNutrientes(
    val id:          String          = com.example.nutriia.platform.generateUUID(),
    val childId:     String          = "",
    val fecha:       String          = "",    // "DD/MM/YYYY"
    val comida:      String          = "",    // "Desayuno", "Almuerzo", etc.
    val alimento:    String          = "",
    val macros:      Macronutrientes = Macronutrientes(),
    val micros:      Micronutrientes = Micronutrientes(),
    val notas:       String          = "",
    val creadoEn:    Long            = com.example.nutriia.platform.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id"       to id,
        "childId"  to childId,
        "fecha"    to fecha,
        "comida"   to comida,
        "alimento" to alimento,
        "macros"   to mapOf(
            "calorias"      to macros.calorias,
            "proteinas"     to macros.proteinas,
            "grasas"        to macros.grasas,
            "carbohidratos" to macros.carbohidratos
        ),
        "micros"   to mapOf(
            "hierro"    to micros.hierro,
            "calcio"    to micros.calcio,
            "vitaminaA" to micros.vitaminaA,
            "vitaminaC" to micros.vitaminaC,
            "zinc"      to micros.zinc
        ),
        "notas"    to notas,
        "creadoEn" to creadoEn,
        "fechaCreacion" to com.example.nutriia.utils.FechaUtils.formatearFecha(creadoEn),
        "horaCreacion"  to com.example.nutriia.utils.FechaUtils.formatearHora(creadoEn)
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(map: Map<String, Any?>): RegistroNutrientes {
            val m = map["macros"] as? Map<String, Any?> ?: emptyMap()
            val n = map["micros"] as? Map<String, Any?> ?: emptyMap()
            return RegistroNutrientes(
                id       = map["id"]       as? String ?: com.example.nutriia.platform.generateUUID(),
                childId  = map["childId"]  as? String ?: "",
                fecha    = map["fecha"]    as? String ?: "",
                comida   = map["comida"]   as? String ?: "",
                alimento = map["alimento"] as? String ?: "",
                macros   = Macronutrientes(
                    calorias      = (m["calorias"]      as? Number)?.toDouble() ?: 0.0,
                    proteinas     = (m["proteinas"]     as? Number)?.toDouble() ?: 0.0,
                    grasas        = (m["grasas"]        as? Number)?.toDouble() ?: 0.0,
                    carbohidratos = (m["carbohidratos"] as? Number)?.toDouble() ?: 0.0
                ),
                micros   = Micronutrientes(
                    hierro    = (n["hierro"]    as? Number)?.toDouble() ?: 0.0,
                    calcio    = (n["calcio"]    as? Number)?.toDouble() ?: 0.0,
                    vitaminaA = (n["vitaminaA"] as? Number)?.toDouble() ?: 0.0,
                    vitaminaC = (n["vitaminaC"] as? Number)?.toDouble() ?: 0.0,
                    zinc      = (n["zinc"]      as? Number)?.toDouble() ?: 0.0
                ),
                notas    = map["notas"]    as? String ?: "",
                creadoEn = (map["creadoEn"] as? Number)?.toLong() ?: com.example.nutriia.platform.currentTimeMillis()
            )
        }
    }
}