package com.example.nutriia.data

import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.PerfilSaludNino

// ═══════════════════════════════════════════════════════════════════════════
// MODELO DEL NIÑO — refleja exactamente tu estructura en Firestore
// ═══════════════════════════════════════════════════════════════════════════

data class ChildProfile(
    val id:               String  = "",
    val name:             String  = "",
    val birthDate:        String  = "",   // "31/03/2025"
    val sexo:             String  = "",   // "NINA" | "NINO"
    val heightCm:         String  = "",
    val weightKg:         String  = "",
    val hasAllergies:     Boolean = false,
    val allergiesDetail:  String  = "",   // "Huevo, Lácteos" — string libre
    val hasConditions:    Boolean = false,
    val conditionsDetail: String  = "",
    val creadoEn:         Long    = 0L
) {
    /**
     * Calcula la edad en meses desde birthDate ("dd/MM/yyyy").
     * Si el formato falla devuelve 6 (mínimo seguro).
     */
    fun edadEnMeses(): Int {
        return try {
            val (dia, mes, anio) = if (birthDate.contains("/")) {
                val p = birthDate.split("/").map { it.toInt() }
                Triple(p[0], p[1], p[2])
            } else {
                val p = birthDate.split("-").map { it.toInt() }
                Triple(p[2], p[1], p[0])
            }
            val anioActual = 2026
            val mesActual = 8
            var meses = (anioActual - anio) * 12 + (mesActual - mes)
            if (meses < 0) 0 else meses
        } catch (_: Exception) { 6 }
    }

    /**
     * Convierte allergiesDetail (string libre) → List<Alergeno>.
     * No importa mayúsculas, tildes ni redacción exacta:
     *   "Huevo, Lácteos" / "alérgico al gluten" / "intolerancia a la leche"
     * todos parsean correctamente.
     */
    fun parsearAlergenos(): List<Alergeno> {
        if (!hasAllergies || allergiesDetail.isBlank()) return emptyList()

        val t = allergiesDetail
            .lowercase()
            .replace("á","a").replace("é","e")
            .replace("í","i").replace("ó","o").replace("ú","u")

        val out = mutableListOf<Alergeno>()
        if ("huevo" in t || "egg" in t) out.add(Alergeno.HUEVO)
        if ("lacteo" in t || "leche" in t || "lactosa" in t ||
            "queso"  in t || "yogur" in t || "dairy"   in t) out.add(Alergeno.LACTEOS)
        if ("cacahuate" in t || "cacahuete" in t || "mani" in t || "peanut" in t) out.add(Alergeno.CACAHUATE)
        if ("nuez" in t || "nueces" in t || "almendra" in t ||
            "pistache" in t || "avellana" in t || "nut" in t) out.add(Alergeno.NUECES)
        if ("trigo" in t || "gluten" in t || "harina" in t || "wheat" in t) out.add(Alergeno.TRIGO)
        if ("soya" in t || "soja" in t || "soy" in t) out.add(Alergeno.SOYA)
        if ("pescado" in t || "atun" in t || "salmon" in t ||
            "sardina" in t || "tilapia" in t || "mojarra" in t || "fish" in t) out.add(Alergeno.PESCADO)
        if ("marisco" in t || "camaron" in t || "cangrejo" in t ||
            "almeja"  in t || "ostion"  in t || "seafood"  in t) out.add(Alergeno.MARISCOS)
        if ("maiz" in t || "elote" in t || "tortilla" in t || "corn" in t) out.add(Alergeno.MAIZ)
        if ("fructosa" in t || "fructose" in t) out.add(Alergeno.FRUCTOSA)

        return out.distinct()
    }

    /** Construye un PerfilSaludNino listo para DietaEngine y AlimentacionViewModel */
    fun toPerfilSalud(): PerfilSaludNino = PerfilSaludNino(
        alergenos   = parsearAlergenos(),
        condiciones = if (hasConditions && conditionsDetail.isNotBlank())
            listOf(conditionsDetail) else emptyList()
    )
}
