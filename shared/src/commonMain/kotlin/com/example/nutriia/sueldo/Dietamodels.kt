package com.example.nutriia.sueldo

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS DE DIETA — NutriIA
//
// NivelIngreso, RegionMexico, PerfilIngreso y ConstantesEconomicas
// siguen viviendo en Nivelingreso.kt — no se duplican aquí.
// ═══════════════════════════════════════════════════════════════════════════

// ── Alérgenos ──────────────────────────────────────────────────────────────
enum class Alergeno(
    val label:       String,
    val descripcion: String,
    val iconoResId:  String
) {
    HUEVO(     "Huevo",            "Clara y yema de huevo",                          "egg"),
    LACTEOS(   "Lacteos",          "Leche, queso, crema, yogur",                     "local_drink"),
    CACAHUATE( "Cacahuate",        "Cacahuate y derivados",                          "eco"),
    NUECES(    "Nueces/Almendras", "Almendras, nuez, pistache, avellana",            "forest"),
    TRIGO(     "Trigo / Gluten",   "Harina de trigo, pan, pasta",                   "grain"),
    SOYA(      "Soya",             "Soya y derivados",                               "spa"),
    PESCADO(   "Pescado",          "Atun, sardina, salmon",                          "set_meal"),
    MARISCOS(  "Mariscos",         "Camaron, cangrejo, almeja",                      "water"),
    MAIZ(      "Maiz",             "Tortilla, elote, masa, pozol",                   "grass"),
    FRUCTOSA(  "Fructosa",         "Frutas con alta fructosa: manzana, pera, mango", "eco")
}

// ── Macronutrientes ────────────────────────────────────────────────────────
data class MacroObjetivo(
    val caloriasMin: Int,
    val caloriasMax: Int,
    val proteinasG:  Double,
    val grasasPorc:  Double,
    val carbosPorc:  Double,
    val hierroMg:    Double,
    val calcioMg:    Double,
    val vitaminaAug: Int,
    val zincMg:      Double
) {
    val proteinasPorc: Double get() = 100.0 - grasasPorc - carbosPorc
}

// ── Receta ─────────────────────────────────────────────────────────────────
enum class TipoComida { DESAYUNO, COLACION, COMIDA, CENA }

data class RecetaMexicana(
    val nombre:       String,
    val ingredientes: List<String>,
    val preparacion:  String,
    val kcal:         Int,
    val tipoComida:   TipoComida,
    val nivelMinimo:  NivelIngreso,
    val edadMinMeses: Int                = 12,
    val fuente:       String,
    val regiones:     List<RegionMexico> = listOf(RegionMexico.GENERAL),
    val alergenos:    List<Alergeno>     = emptyList()
) {
    fun esSegurasParaPerfil(alergenosNino: List<Alergeno>): Boolean =
        alergenos.none { it in alergenosNino }
}

// ── Plan semanal ───────────────────────────────────────────────────────────

data class ComidasDiarias(
    val desayuno:         String,
    val colacion1:        String,
    val almuerzo:         String,
    val colacion2:        String,
    val cena:             String,
    val costoEstimadoDia: Double,
    // ── Nutrientes estimados del día ──────────────────────────────────────
    // Default 0.0 → compatibilidad total con código existente de DietaEngine.
    // NutriEstimadoEngine.estimarDia() los llena al generar el plan semanal.
    val caloriasEstimadas:  Double = 0.0,
    val proteinasEstimadas: Double = 0.0,
    val grasasEstimadas:    Double = 0.0,
    val carbosEstimados:    Double = 0.0,
    val hierroEstimado:     Double = 0.0,
    val calcioEstimado:     Double = 0.0,
    val vitaminaAEstimada:  Double = 0.0,
    val vitaminaCEstimada:  Double = 0.0,
    val zincEstimado:       Double = 0.0
)

data class PlanDietaSemanal(
    val diaSemana:    String,
    val comidas:      ComidasDiarias,
    val macros:       MacroObjetivo,
    val nivelIngreso: NivelIngreso,
    val edadMeses:    Int
)

// ── Resumen nutricional ────────────────────────────────────────────────────
data class ResumenNutricional(
    val etapaLabel:           String,
    val macroObjetivo:        MacroObjetivo,
    val nivelIngreso:         NivelIngreso,
    val costoMensualEstimado: Double,
    val alimentosClave:       List<String>,
    val alertas:              List<String>,
    val alertasAlergia:       List<String> = emptyList()
)

// ── Perfil de salud del niño ───────────────────────────────────────────────
data class PerfilSaludNino(
    val alergenos:     List<Alergeno> = emptyList(),
    val condiciones:   List<String>   = emptyList(),
    val esVegetariano: Boolean        = false,
    val esVegano:      Boolean        = false
) {
    val tieneAlergias: Boolean get() = alergenos.isNotEmpty()

    fun generarAdvertencias(): List<String> {
        val lista = mutableListOf<String>()
        if (Alergeno.LACTEOS   in alergenos) lista.add("Sin lacteos: reforzar calcio con tortilla de maiz, nopal y brocoli.")
        if (Alergeno.HUEVO     in alergenos) lista.add("Sin huevo: compensar proteina con legumbres, pollo y pescado.")
        if (Alergeno.CACAHUATE in alergenos ||
            Alergeno.NUECES    in alergenos) lista.add("Alergia a nueces/cacahuate: revisar etiquetas de cereales y galletas.")
        if (Alergeno.TRIGO     in alergenos) lista.add("Sin trigo/gluten: usar tortilla de maiz y arroz como base de carbohidratos.")
        if (Alergeno.MAIZ      in alergenos) lista.add("Sin maiz: evitar tortilla, elote y masa. Usar arroz o papa como base.")
        if (Alergeno.PESCADO   in alergenos) lista.add("Sin pescado: compensar Omega-3 con chia y linaza.")
        if (esVegano)                        lista.add("Dieta vegana: revisar aporte de B12, hierro, calcio y zinc con pediatra.")
        if (esVegetariano)                   lista.add("Dieta vegetariana: combinar legumbres + cereal para proteina completa.")
        return lista
    }
}