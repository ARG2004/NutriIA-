package com.example.nutriia.shared

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.DietaEngine
import com.example.nutriia.sueldo.ModoPlanAlimentario
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.PerfilSaludNino
import com.example.nutriia.sueldo.PlanDietaSemanal
import com.example.nutriia.sueldo.RecetaMexicana
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.sueldo.TipoComida
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
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

    fun parsearAlergenos(): List<Alergeno> = alergenosParsados

    fun obtenerAlimentosExcluidos(): List<String> {
        val lista = mutableListOf<String>()
        if (hasAllergies && allergiesDetail.isNotBlank()) {
            lista.addAll(com.example.nutriia.ui.theme.extraerAlimentosExcluidosTexto(allergiesDetail))
        }
        if (hasConditions && conditionsDetail.isNotBlank()) {
            lista.addAll(com.example.nutriia.ui.theme.extraerAlimentosExcluidosTexto(conditionsDetail))
        }
        return lista.distinct()
    }

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
    val b = birthDate.trim()
    if (b.isBlank()) return 0
    return try {
        val (dia, mes, anio) = if (b.contains("/")) {
            val p = b.split("/").mapNotNull { it.trim().toIntOrNull() }
            if (p.size == 3) Triple(p[0], p[1], p[2]) else return 0
        } else if (b.contains("-")) {
            val p = b.split("-").mapNotNull { it.trim().toIntOrNull() }
            if (p.size == 3) {
                if (p[0] > 1000) Triple(p[2], p[1], p[0])
                else Triple(p[0], p[1], p[2])
            } else return 0
        } else return 0

        val hoy = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        var m = (hoy.year - anio) * 12 + (hoy.monthNumber - mes)
        if (hoy.dayOfMonth < dia) {
            m -= 1
        }
        m.coerceAtLeast(0)
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

    // ── Modo de Plan Alimentario (Doctor / Motor / Mixto) ─────────────────
    private val _modoPlanAlimentario = MutableStateFlow(ModoPlanAlimentario.MIXTO)
    val modoPlanAlimentario: StateFlow<ModoPlanAlimentario> = _modoPlanAlimentario.asStateFlow()

    fun setModoPlanAlimentario(modo: ModoPlanAlimentario) {
        if (_modoPlanAlimentario.value != modo) {
            _modoPlanAlimentario.value = modo
            _childProfile.value?.let { p ->
                val m = calcularEdadMeses(p.birthDate)
                generarPlan(m, _nivelIngreso.value, _region.value)
            }
        }
    }

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

        val perfilSalud = _childProfile.value?.toPerfilSalud() ?: PerfilSaludNino()
        val tolerados = _alimentosTolerados.value
        val excluidosQuiz = _childProfile.value?.obtenerAlimentosExcluidos() ?: emptyList()

        // Un alimento del quiz permanece excluido a menos que haya sido probado y tolerado en SolidosScreen
        val excluidosFinales = excluidosQuiz.filter { excluido ->
            tolerados.none { tol -> tol.contains(excluido, ignoreCase = true) || excluido.contains(tol, ignoreCase = true) }
        }

        _planSemanal.value = DietaEngine.generarPlanSemanal(
            meses                = meses,
            nivel                = nivel,
            region               = region,
            alergenosNiño        = perfilSalud.alergenos,
            alimentosRegistrados = tolerados,
            recetasCustom        = _recetasPersonalizadas.value,
            alimentosExcluidos   = excluidosFinales,
            modoPlan             = _modoPlanAlimentario.value
        )
    }

    private var recetasJob: kotlinx.coroutines.Job? = null

    fun cargarPerfil(uid: String, childId: String) {
        if (uid.isBlank() || childId.isBlank()) return
        recetasJob?.cancel()
        recetasJob = viewModelScope.launch {
            try {
                Firebase.firestore.collection("usuarios").document(uid)
                    .collection("hijos").document(childId)
                    .collection("recetas_nutriologo")
                    .snapshots
                    .collect { snapshot ->
                        val recetas = snapshot.documents.mapNotNull { d ->
                            val nombre = runCatching { d.get<String?>("nombre") }.getOrNull()
                                ?: runCatching { d.get<String?>("titulo") }.getOrNull() ?: return@mapNotNull null
                            val ingredientes = runCatching { d.get<List<String>?>("ingredientes") }.getOrNull()
                                ?: runCatching { d.get<String?>("texto") ?: d.get<String?>("contenido") }.getOrNull()
                                    ?.lines()
                                    ?.firstOrNull { it.startsWith("Ingredientes:", ignoreCase = true) }
                                    ?.removePrefix("Ingredientes:")?.removePrefix("ingredientes:")
                                    ?.split(",", ";")?.map { it.trim() }?.filter { it.isNotBlank() }
                                ?: emptyList()
                            val preparacion = runCatching { d.get<String?>("preparacion") }.getOrNull()
                                ?: runCatching { d.get<String?>("texto") ?: d.get<String?>("contenido") }.getOrNull()
                                    ?.lines()
                                    ?.filterNot { it.startsWith("Ingredientes:", ignoreCase = true) }
                                    ?.joinToString("\n")?.trim()
                                ?: ""
                            val kcal = runCatching { d.get<Int?>("kcal") }.getOrNull()
                                ?: runCatching { d.get<Long?>("kcal")?.toInt() }.getOrNull() ?: 0
                            val tipoStr = runCatching { d.get<String?>("tipoComida") }.getOrNull() ?: TipoComida.COMIDA.name
                            val tipo = runCatching { TipoComida.valueOf(tipoStr) }.getOrDefault(TipoComida.COMIDA)
                            val edadMin = runCatching { d.get<Int?>("edadMeses") }.getOrNull()
                                ?: runCatching { d.get<Long?>("edadMeses")?.toInt() }.getOrNull() ?: 0
                            val autor = runCatching { d.get<String?>("autorNombre") }.getOrNull() ?: "Nutriólogo"

                            RecetaMexicana(
                                nombre       = nombre,
                                ingredientes = ingredientes,
                                preparacion  = preparacion,
                                kcal         = kcal,
                                tipoComida   = tipo,
                                nivelMinimo  = NivelIngreso.BASICO,
                                edadMinMeses = edadMin.coerceAtLeast(0),
                                fuente       = "Nutriólogo: $autor",
                                regiones     = listOf(RegionMexico.GENERAL),
                                alergenos    = emptyList()
                            )
                        }
                        _recetasPersonalizadas.value = recetas
                        _childProfile.value?.let { p ->
                            val m = calcularEdadMeses(p.birthDate)
                            generarPlan(m, _nivelIngreso.value, _region.value)
                        }
                    }
            } catch (_: Exception) {}
        }
    }

    fun limpiarPerfil() {
        recetasJob?.cancel()
        _childProfile.value = null
        _alimentosTolerados.value = emptyList()
        _planSemanal.value = emptyList()
        _planAlimentacionActivo.value = false
        _recetasPersonalizadas.value = emptyList()
    }
}

