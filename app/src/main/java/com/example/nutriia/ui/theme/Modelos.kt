package com.example.nutriia.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.PerfilSaludNino
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import java.text.SimpleDateFormat
import java.util.*

// ═══════════════════════════════════════════════════════════════════════════
// PERFIL DEL NIÑO
// ═══════════════════════════════════════════════════════════════════════════

data class ChildProfile(
    val id:               String  = UUID.randomUUID().toString(),
    val name:             String  = "",
    val birthDate:        String  = "",
    val weightKg:         String  = "",
    val heightCm:         String  = "",
    val hasAllergies:     Boolean = false,
    val allergiesDetail:  String  = "",
    val hasConditions:    Boolean = false,
    val conditionsDetail: String  = "",
    val sexo:             Sexo?   = null,
    val nivelIngreso:     NivelIngreso = NivelIngreso.BASICO,
    val region:           RegionMexico = RegionMexico.CENTRO
) {
    val ageMonths:    Int      get() = calcularEdadMeses(birthDate)
    val ageYears:     Int      get() = (ageMonths / 12).coerceAtLeast(0)
    val tieneFecha:   Boolean  get() = birthDate.length == 10
    val primerNombre: String   get() = name.trim().split(" ").firstOrNull() ?: name
    val etapa:        EtapaInfo get() = etapaParaMeses(ageMonths)

    val pesoKgDouble:  Double? get() = weightKg.toDoubleOrNull()
    val tallaCmDouble: Double? get() = heightCm.toDoubleOrNull()
    val imc: Double? get() {
        val p = pesoKgDouble ?: return null
        val t = tallaCmDouble ?: return null
        if (t <= 0) return null
        val tM = t / 100.0
        return p / (tM * tM)
    }

    val alergenosParsados: List<Alergeno> get() =
        if (hasAllergies && allergiesDetail.isNotBlank())
            parsearAlergenos(allergiesDetail)
        else emptyList()

    fun toPerfilSaludNino(): PerfilSaludNino = PerfilSaludNino(
        alergenos   = alergenosParsados,
        condiciones = if (hasConditions && conditionsDetail.isNotBlank())
            conditionsDetail.split(",", ";", "\n").map { it.trim() }.filter { it.isNotBlank() }
        else emptyList()
    )

    fun toMap(): Map<String, Any?> = mapOf(
        "id"               to id,
        "name"             to name,
        "birthDate"        to birthDate,
        "weightKg"         to weightKg,
        "heightCm"         to heightCm,
        "hasAllergies"     to hasAllergies,
        "allergiesDetail"  to allergiesDetail,
        "hasConditions"    to hasConditions,
        "conditionsDetail" to conditionsDetail,
        "sexo"             to sexo?.name
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ChildProfile = ChildProfile(
            id               = map["id"]               as? String  ?: UUID.randomUUID().toString(),
            name             = map["name"]             as? String  ?: "",
            birthDate        = map["birthDate"]        as? String  ?: "",
            weightKg         = map["weightKg"]         as? String  ?: "",
            heightCm         = map["heightCm"]         as? String  ?: "",
            hasAllergies     = map["hasAllergies"]     as? Boolean ?: false,
            allergiesDetail  = map["allergiesDetail"]  as? String  ?: "",
            hasConditions    = map["hasConditions"]    as? Boolean ?: false,
            conditionsDetail = map["conditionsDetail"] as? String  ?: "",
            sexo             = (map["sexo"] as? String)?.let {
                runCatching { Sexo.valueOf(it) }.getOrNull()
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// ETAPA DE DESARROLLO
// ═══════════════════════════════════════════════════════════════════════════

data class EtapaInfo(
    val nombre: String,
    val emoji:  String = "",
    val rango:  String = "",
    val color:  Color  = Color(0xFF4CAF50)
)

fun etapaParaMeses(meses: Int): EtapaInfo = when {
    meses < 0   -> EtapaInfo("Sin fecha",           "", "",           Color(0xFF9E9E9E))
    meses < 6   -> EtapaInfo("Lactancia exclusiva", "", "0-6 meses",  Color(0xFF1E88E5))
    meses < 12  -> EtapaInfo("Inicio de solidos",   "", "6-12 meses", Color(0xFF43A047))
    meses < 24  -> EtapaInfo("Diversificacion",     "", "1-2 anos",   Color(0xFF00897B))
    meses < 36  -> EtapaInfo("Alimentacion variada","", "2-3 anos",   Color(0xFF558B2F))
    meses < 60  -> EtapaInfo("Preescolar",          "", "3-5 anos",   Color(0xFFFF8F00))
    meses < 96  -> EtapaInfo("Escolar temprano",    "", "5-8 anos",   Color(0xFFE65100))
    meses < 144 -> EtapaInfo("Escolar",             "", "8-12 anos",  Color(0xFF6A1B9A))
    else        -> EtapaInfo("Adolescencia",        "", "12+ anos",   Color(0xFF880E4F))
}

// ═══════════════════════════════════════════════════════════════════════════
// CÁLCULO DE EDAD
// ═══════════════════════════════════════════════════════════════════════════

fun calcularEdadMeses(birthDate: String): Int {
    if (birthDate.length != 10) return 0
    return try {
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val fecha = fmt.parse(birthDate) ?: return 0
        val hoy = Calendar.getInstance()
        val nac = Calendar.getInstance().apply { time = fecha }
        val anios = hoy.get(Calendar.YEAR)  - nac.get(Calendar.YEAR)
        val meses = hoy.get(Calendar.MONTH) - nac.get(Calendar.MONTH)
        (anios * 12 + meses).coerceAtLeast(0)
    } catch (e: Exception) { 0 }
}

fun calcularEdadAnios(birthDate: String): Int = calcularEdadMeses(birthDate) / 12

// ═══════════════════════════════════════════════════════════════════════════
// PARSER DE ALÉRGENOS
// FIX: reescrito sin break/continue en lambdas (incompatible con Kotlin < 2.2)
// ═══════════════════════════════════════════════════════════════════════════

private val PALABRAS_ALERGENO: Map<String, Alergeno> = mapOf(
    "leche"        to Alergeno.LACTEOS,
    "lacteos"      to Alergeno.LACTEOS,
    "lactosa"      to Alergeno.LACTEOS,
    "lacteo"       to Alergeno.LACTEOS,
    "queso"        to Alergeno.LACTEOS,
    "yogur"        to Alergeno.LACTEOS,
    "yogurt"       to Alergeno.LACTEOS,
    "crema"        to Alergeno.LACTEOS,
    "mantequilla"  to Alergeno.LACTEOS,
    "dairy"        to Alergeno.LACTEOS,
    "milk"         to Alergeno.LACTEOS,
    "huevo"        to Alergeno.HUEVO,
    "huevos"       to Alergeno.HUEVO,
    "clara"        to Alergeno.HUEVO,
    "egg"          to Alergeno.HUEVO,
    "cacahuate"    to Alergeno.CACAHUATE,
    "cacahuates"   to Alergeno.CACAHUATE,
    "cacahuete"    to Alergeno.CACAHUATE,
    "mani"         to Alergeno.CACAHUATE,
    "peanut"       to Alergeno.CACAHUATE,
    "nuez"         to Alergeno.NUECES,
    "nueces"       to Alergeno.NUECES,
    "almendra"     to Alergeno.NUECES,
    "almendras"    to Alergeno.NUECES,
    "pistache"     to Alergeno.NUECES,
    "avellana"     to Alergeno.NUECES,
    "avellanas"    to Alergeno.NUECES,
    "tree nut"     to Alergeno.NUECES,
    "trigo"        to Alergeno.TRIGO,
    "gluten"       to Alergeno.TRIGO,
    "celiaca"      to Alergeno.TRIGO,
    "celiaco"      to Alergeno.TRIGO,
    "celiaquia"    to Alergeno.TRIGO,
    "harina"       to Alergeno.TRIGO,
    "wheat"        to Alergeno.TRIGO,
    "soya"         to Alergeno.SOYA,
    "soja"         to Alergeno.SOYA,
    "soy"          to Alergeno.SOYA,
    "pescado"      to Alergeno.PESCADO,
    "atun"         to Alergeno.PESCADO,
    "salmon"       to Alergeno.PESCADO,
    "sardina"      to Alergeno.PESCADO,
    "tilapia"      to Alergeno.PESCADO,
    "merluza"      to Alergeno.PESCADO,
    "fish"         to Alergeno.PESCADO,
    "camaron"      to Alergeno.MARISCOS,
    "camarones"    to Alergeno.MARISCOS,
    "marisco"      to Alergeno.MARISCOS,
    "mariscos"     to Alergeno.MARISCOS,
    "cangrejo"     to Alergeno.MARISCOS,
    "langosta"     to Alergeno.MARISCOS,
    "almeja"       to Alergeno.MARISCOS,
    "shrimp"       to Alergeno.MARISCOS,
    "maiz"         to Alergeno.MAIZ,
    "elote"        to Alergeno.MAIZ,
    "tortilla"     to Alergeno.MAIZ,
    "masa"         to Alergeno.MAIZ,
    "corn"         to Alergeno.MAIZ,
    "fructosa"     to Alergeno.FRUCTOSA,
    "fructose"     to Alergeno.FRUCTOSA
)

/**
 * Convierte texto libre de alergias a lista tipada de [Alergeno].
 * Compatible con Kotlin < 2.2 — sin break/continue en lambdas.
 *
 * Ejemplos:
 *   "leche, huevo, mani"        → [LACTEOS, HUEVO, CACAHUATE]
 *   "gluten y soya"             → [TRIGO, SOYA]
 *   "intolerancia a la lactosa" → [LACTEOS]
 */
fun parsearAlergenos(texto: String): List<Alergeno> {
    if (texto.isBlank()) return emptyList()

    val normalizado = texto.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ü", "u")
        .replace("ñ", "n")

    val fragmentos = normalizado
        .split(",", ".", ";", " y ", " e ", "/", " o ", " and ")
        .map { it.trim() }
        .filter { it.isNotBlank() }

    val resultado = mutableSetOf<Alergeno>()

    // FIX: usar for clásico con variable booleana en lugar de continue/break
    // dentro de lambdas para compatibilidad con Kotlin 1.x
    for (fragmento in fragmentos) {
        // 1. Buscar coincidencia exacta
        val exacto = PALABRAS_ALERGENO[fragmento]
        if (exacto != null) {
            resultado.add(exacto)
        } else {
            // 2. Buscar si el fragmento CONTIENE alguna clave
            var encontrado = false
            for ((clave, alergeno) in PALABRAS_ALERGENO) {
                if (!encontrado && fragmento.contains(clave)) {
                    resultado.add(alergeno)
                    encontrado = true
                }
            }
        }
    }

    return resultado.toList()
}