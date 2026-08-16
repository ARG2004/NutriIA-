package com.example.nutriia.shared

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.DietaEngine
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.PerfilSaludNino
import com.example.nutriia.sueldo.PlanDietaSemanal
import com.example.nutriia.sueldo.RecetaMexicana
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.sueldo.TipoComida
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.*

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS DE DATOS: PERFIL Y ETAPAS
// ═══════════════════════════════════════════════════════════════════════════

data class ChildProfile(
    val id:               String  = generateUUID(),
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
    val region:           RegionMexico = RegionMexico.CENTRO,
    val creadoEn:         Long    = 0L
) {
    // Propiedades calculadas para la UI
    val ageMonths:    Int       get() = calcularEdadMeses(birthDate)
    val ageYears:     Int       get() = (ageMonths / 12).coerceAtLeast(0)
    val tieneFecha:   Boolean   get() = birthDate.length == 10
    val primerNombre: String    get() = name.trim().split(" ").firstOrNull() ?: name
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

    fun edadEnMeses(): Int = ageMonths

    // Conversión a modelo de lógica de negocio (Sueldo/Engine)
    fun toPerfilSalud(): PerfilSaludNino = PerfilSaludNino(
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
        "sexo"             to sexo?.name,
        "nivelIngreso"     to nivelIngreso.name,
        "region"           to region.name,
        "creadoEn"         to creadoEn
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ChildProfile = ChildProfile(
            id               = map["id"]               as? String  ?: generateUUID(),
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
            },
            nivelIngreso     = (map["nivelIngreso"] as? String)?.let {
                runCatching { NivelIngreso.valueOf(it) }.getOrDefault(NivelIngreso.BASICO)
            } ?: NivelIngreso.BASICO,
            region           = (map["region"] as? String)?.let {
                runCatching { RegionMexico.valueOf(it) }.getOrDefault(RegionMexico.CENTRO)
            } ?: RegionMexico.CENTRO,
            creadoEn         = (map["creadoEn"] as? Number)?.toLong() ?: 0L
        )
    }
}

// ─── Información de Etapas de Crecimiento ──────────────────────────────────

data class EtapaInfo(
    val nombre: String,
    val emoji:  String = "",
    val rango:  String = "",
    val color:  Color  = Color(0xFF4CAF50)
)

fun etapaParaMeses(meses: Int): EtapaInfo = when {
    meses < 0   -> EtapaInfo("Sin fecha",            "❓", "",           Color(0xFF9E9E9E))
    meses < 6   -> EtapaInfo("Lactancia exclusiva",  "🍼", "0-6 meses",  Color(0xFF1E88E5))
    meses < 12  -> EtapaInfo("Inicio de sólidos",    "🍎", "6-12 meses", Color(0xFF43A047))
    meses < 24  -> EtapaInfo("Diversificación",      "🍱", "1-2 años",   Color(0xFF00897B))
    meses < 36  -> EtapaInfo("Alimentación variada", "🥗", "2-3 años",   Color(0xFF558B2F))
    meses < 60  -> EtapaInfo("Preescolar",           "🏫", "3-5 años",   Color(0xFFFF8F00))
    meses < 96  -> EtapaInfo("Escolar temprano",     "🎒", "5-8 años",   Color(0xFFE65100))
    meses < 144 -> EtapaInfo("Escolar",              "📚", "8-12 años",  Color(0xFF6A1B9A))
    else        -> EtapaInfo("Adolescencia",         "🏀", "12+ años",   Color(0xFF880E4F))
}

// ═══════════════════════════════════════════════════════════════════════════
// UTILIDADES Y PARSERS (FECHAS Y ALÉRGENOS)
// ═══════════════════════════════════════════════════════════════════════════

fun calcularEdadMeses(birthDate: String): Int {
    if (birthDate.length != 10) return 0
    return try {
        val (dia, mes, anio) = if (birthDate.contains("/")) {
            val p = birthDate.split("/").map { it.toInt() }
            Triple(p[0], p[1], p[2])
        } else {
            val p = birthDate.split("-").map { it.toInt() }
            Triple(p[2], p[1], p[0])
        }
        val hoy = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val anios = hoy.year - anio
        val meses = hoy.monthNumber - mes
        (anios * 12 + meses).coerceAtLeast(0)
    } catch (_: Exception) { 0 }
}

// ─── Mapeo Extenso de Palabras Clave para Alérgenos ─────────────────────────

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

fun parsearAlergenos(texto: String): List<Alergeno> {
    if (texto.isBlank()) return emptyList()
    // Normalización de caracteres especiales
    val normalizado = texto.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ü", "u")
        .replace("ñ", "n")

    // Separación por delimitadores comunes
    val fragmentos = normalizado
        .split(",", ".", ";", " y ", " e ", "/", " o ", " and ")
        .map { it.trim() }.filter { it.isNotBlank() }

    val resultado = mutableSetOf<Alergeno>()
    for (fragmento in fragmentos) {
        val exacto = PALABRAS_ALERGENO[fragmento]
        if (exacto != null) {
            resultado.add(exacto)
        } else {
            // Búsqueda por contención si no hay coincidencia exacta
            for ((clave, alergeno) in PALABRAS_ALERGENO) {
                if (fragmento.contains(clave)) {
                    resultado.add(alergeno)
                }
            }
        }
    }
    return resultado.toList()
}

// ═══════════════════════════════════════════════════════════════════════════
// NutriSharedViewModel: CONTROLADOR DE ESTADO COMPARTIDO
// ═══════════════════════════════════════════════════════════════════════════

class NutriSharedViewModel : ViewModel() {

    // ── Perfil del niño activo ────────────────────────────────────────────
    private val _childProfile = MutableStateFlow<ChildProfile?>(null)
    val childProfile: StateFlow<ChildProfile?> = _childProfile.asStateFlow()

    // Flujos derivados para simplificar la observación en UI
    val edadMeses: StateFlow<Int> = _childProfile
        .map { it?.edadEnMeses() ?: 6 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 6)

    val perfilSalud: StateFlow<PerfilSaludNino> = _childProfile
        .map { it?.toPerfilSalud() ?: PerfilSaludNino() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilSaludNino())

    val alergenosNino: StateFlow<List<Alergeno>> = perfilSalud
        .map { it.alergenos }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Configuración de parámetros de dieta ──────────────────────────────
    private val _nivelIngreso = MutableStateFlow(NivelIngreso.BASICO)
    val nivelIngreso: StateFlow<NivelIngreso> = _nivelIngreso.asStateFlow()

    private val _region = MutableStateFlow(RegionMexico.CENTRO)
    val region: StateFlow<RegionMexico> = _region.asStateFlow()

    fun setNivelIngreso(nivel: NivelIngreso) { _nivelIngreso.value = nivel }
    fun setRegion(region: RegionMexico)      { _region.value = region }

    // ── Alimentos tolerados y Sincronización ──────────────────────────────
    private val _alimentosTolerados = MutableStateFlow<List<String>>(emptyList())
    val alimentosTolerados: StateFlow<List<String>> = _alimentosTolerados.asStateFlow()

    // ── Recetas personalizadas del nutriólogo ─────────────────────────────
    private val _recetasPersonalizadas = MutableStateFlow<List<RecetaMexicana>>(emptyList())
    val recetasPersonalizadas: StateFlow<List<RecetaMexicana>> = _recetasPersonalizadas.asStateFlow()

    fun setPerfil(perfil: ChildProfile?) {
        _childProfile.value = perfil
        perfil?.let {
            _nivelIngreso.value = it.nivelIngreso
            _region.value = it.region
        }
    }

    /**
     * Sincronización desde el expediente del Nutriólogo.
     * Recibe la lista de nombres de alimentos marcados como "Aceptados".
     */
    fun actualizarDesdeExpediente(nombres: List<String>) {
        val nuevo = nombres.map { it.lowercase() }
        if (_alimentosTolerados.value != nuevo) {
            _alimentosTolerados.value = nuevo
            if (!_planAlimentacionActivo.value) {
                _planSemanal.value = emptyList()
            }
        }
    }

    /**
     * Actualización manual de alimentos (ej. desde el flujo de sólidos).
     */
    fun setAlimentosTolerados(nombres: List<String>) {
        val nuevo = nombres.map { it.lowercase() }
        if (_alimentosTolerados.value != nuevo) {
            _alimentosTolerados.value = nuevo
            if (!_planAlimentacionActivo.value) {
                _planSemanal.value = emptyList()
            }
        }
    }

    // ── Gestión del Plan Semanal ─────────────────────────────────────────
    private val _planAlimentacionActivo = MutableStateFlow(false)
    val planAlimentacionActivo: StateFlow<Boolean> = _planAlimentacionActivo.asStateFlow()

    private val _planSemanal = MutableStateFlow<List<PlanDietaSemanal>>(emptyList())
    val planSemanal: StateFlow<List<PlanDietaSemanal>> = _planSemanal.asStateFlow()

    fun setPlanSemanal(plan: List<PlanDietaSemanal>) {
        if (!_planAlimentacionActivo.value) {
            _planSemanal.value = plan
        }
    }

    fun setPlanSemanalAlimentacion(plan: List<PlanDietaSemanal>) {
        if (plan.isNotEmpty()) {
            _planSemanal.value = plan
            _planAlimentacionActivo.value = true
        }
    }

    fun resetPlanAlimentacion() {
        _planAlimentacionActivo.value = false
        _planSemanal.value = emptyList()
    }

    fun generarPlan(meses: Int, nivel: NivelIngreso, region: RegionMexico = RegionMexico.CENTRO) {
        if (_planAlimentacionActivo.value) return

        val perfil = _childProfile.value?.toPerfilSalud() ?: PerfilSaludNino()
        val tolerados = _alimentosTolerados.value

        _planSemanal.value = DietaEngine.generarPlanSemanal(
            meses = meses,
            nivel = nivel,
            region = region,
            alergenosNiño = perfil.alergenos,
            alimentosRegistrados = tolerados,
            recetasCustom = _recetasPersonalizadas.value
        )
    }

    fun cargarPerfil(uid: String, childId: String) {
        if (uid.isBlank() || childId.isBlank()) return
        // Carga el perfil si se requiere
    }

    fun limpiarPerfil() {
        _childProfile.value = null
        _alimentosTolerados.value = emptyList()
        _planSemanal.value = emptyList()
        _planAlimentacionActivo.value = false
        _recetasPersonalizadas.value = emptyList()
    }
}
