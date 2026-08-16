package com.example.nutriia.solidos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.shared.NutriSharedViewModel
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.DietaEngine
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.PlanDietaSemanal
import com.example.nutriia.sueldo.RecetaMexicana
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.sueldo.TipoComida
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AlimentacionUiState {
    object Idle    : AlimentacionUiState()
    object Loading : AlimentacionUiState()
    object Saved   : AlimentacionUiState()
    object Deleted : AlimentacionUiState()
    data class Error(val msg: String) : AlimentacionUiState()
}

enum class FiltroTipoReceta(val label: String, val tipo: TipoComida?) {
    TODAS    ("Todas",    null),
    DESAYUNO ("Desayuno", TipoComida.DESAYUNO),
    COMIDA   ("Comida",   TipoComida.COMIDA),
    CENA     ("Cena",     TipoComida.CENA),
    COLACION ("Colación", TipoComida.COLACION)
}

class AlimentacionViewModel : ViewModel() {

    // Usa directamente SolidosRepository — el offline lo maneja Firestore
    private val repo = SolidosRepository()
    private var shared: NutriSharedViewModel? = null

    private val _alimentosIntroducidos = MutableStateFlow<List<AlimentoIntroducido>>(emptyList())
    val alimentosIntroducidos: StateFlow<List<AlimentoIntroducido>> = _alimentosIntroducidos.asStateFlow()

    private val _uiState = MutableStateFlow<AlimentacionUiState>(AlimentacionUiState.Idle)
    val uiState: StateFlow<AlimentacionUiState> = _uiState.asStateFlow()

    private val _edadMeses = MutableStateFlow(6)
    val edadMeses: StateFlow<Int> = _edadMeses.asStateFlow()

    private val _busquedaReceta   = MutableStateFlow("")
    val busquedaReceta: StateFlow<String> = _busquedaReceta.asStateFlow()

    private val _filtroTipoReceta = MutableStateFlow(FiltroTipoReceta.TODAS)
    val filtroTipoReceta: StateFlow<FiltroTipoReceta> = _filtroTipoReceta.asStateFlow()

    private val _busqueda    = MutableStateFlow("")
    private val _grupoFiltro = MutableStateFlow<GrupoAlimento?>(null)

    private var currentChildId: String? = null
    private var observeJob: Job? = null
    private val _alergenosNino = MutableStateFlow<List<Alergeno>>(emptyList())
    private val _nivelIngreso = MutableStateFlow(NivelIngreso.BASICO)
    private val _region = MutableStateFlow(RegionMexico.CENTRO)
    private val _recetasPersonalizadas = MutableStateFlow<List<RecetaMexicana>>(emptyList())

    val alergenosNino: StateFlow<List<Alergeno>> = _alergenosNino.asStateFlow()

    // ── Init ──────────────────────────────────────────────────────────────────

    fun init(uid: String, childId: String, meses: Int, sharedVm: NutriSharedViewModel) {
        _edadMeses.value = meses.coerceAtLeast(6)
        shared           = sharedVm
        sharedVm.cargarPerfil(uid, childId)

        if (childId == currentChildId) return
        currentChildId = childId

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            // Collect from sharedVm reactively
            launch { sharedVm.alergenosNino.collect { _alergenosNino.value = it } }
            launch { sharedVm.nivelIngreso.collect { _nivelIngreso.value = it } }
            launch { sharedVm.region.collect { _region.value = it } }
            launch { sharedVm.recetasPersonalizadas.collect { _recetasPersonalizadas.value = it } }

            repo.observarAlimentos(childId)
                .catch { e ->
                    _uiState.value = AlimentacionUiState.Error(e.message ?: "Error al cargar alimentos")
                    emit(emptyList())
                }
                .collect { lista ->
                    val ordenada = lista.sortedByDescending { it.fechaIntroduccion }
                    _alimentosIntroducidos.value = ordenada

                    val tolerados = ordenada
                        .filter {
                            it.reaccion == ReaccionAlimento.NINGUNA ||
                                    it.reaccion == ReaccionAlimento.LEVE
                        }
                        .map { it.nombre }
                    sharedVm.setAlimentosTolerados(tolerados)
                }
        }
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setBusquedaReceta(q: String)             { _busquedaReceta.value = q }
    fun setFiltroTipoReceta(f: FiltroTipoReceta) { _filtroTipoReceta.value = f }
    fun setBusqueda(q: String)                   { _busqueda.value = q }
    fun setGrupoFiltro(g: GrupoAlimento?)        { _grupoFiltro.value = g }

    // ── Recetas filtradas ─────────────────────────────────────────────────────

    val recetasFiltradas: StateFlow<List<RecetaMexicana>> = combine(
        _edadMeses,
        _busquedaReceta,
        _filtroTipoReceta,
        _alergenosNino,
        _nivelIngreso,
        _region,
        _recetasPersonalizadas
    ) { args ->
        val meses         = args[0] as Int
        val q             = args[1] as String
        val filtroTipo    = args[2] as FiltroTipoReceta
        val alergenos     = args[3] as List<Alergeno>
        val nivel         = args[4] as NivelIngreso
        val region        = args[5] as RegionMexico
        val recetasCustom = args[6] as List<RecetaMexicana>

        val tipos: List<TipoComida> = if (filtroTipo.tipo != null)
            listOf(filtroTipo.tipo)
        else
            TipoComida.entries.toList()

        // Recetas del motor de dietas
        val candidatasEngine = tipos.flatMap { tipo ->
            DietaEngine.recetasPorPerfil(
                meses         = meses,
                nivel         = nivel,
                tipo          = tipo,
                region        = region,
                alergenosNiño = emptyList()
            )
        }.distinctBy { it.nombre }

        // Recetas personalizadas del nutriólogo (filtradas por tipo si aplica)
        val candidatasCustom = recetasCustom.filter { r ->
            filtroTipo.tipo == null || r.tipoComida == filtroTipo.tipo
        }

        // Merge: custom primero, luego engine, sin duplicar por nombre
        val nombresCargados = mutableSetOf<String>()
        val merged = mutableListOf<RecetaMexicana>()
        for (r in candidatasCustom + candidatasEngine) {
            if (nombresCargados.add(r.nombre)) merged.add(r)
        }

        val conBusqueda = if (q.isBlank()) merged else {
            val qLower = q.lowercase()
            merged.filter { r ->
                r.nombre.lowercase().contains(qLower) ||
                        r.ingredientes.any { it.lowercase().contains(qLower) }
            }
        }

        conBusqueda.sortedWith(compareBy { r ->
            if (r.alergenos.any { it in alergenos }) 1 else 0
        })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Flows de alertas ──────────────────────────────────────────────────────

    val alimentosConAlergiaNino: StateFlow<List<AlimentoPermitido>> = combine(
        _edadMeses,
        _alergenosNino
    ) { meses, alergenos ->
        if (alergenos.isEmpty()) emptyList()
        else alimentosParaEdad(meses).filter { a -> a.esContraIndicadoPara(alergenos) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val alertasAlergenos: StateFlow<List<AlimentoPermitido>> = combine(
        _edadMeses,
        _alergenosNino
    ) { meses, alergenos ->
        alimentosParaEdad(meses).filter { a ->
            a.esAlergeno && !a.esContraIndicadoPara(alergenos)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val conReaccion: StateFlow<List<AlimentoIntroducido>> = _alimentosIntroducidos.map { lista ->
        lista.filter { it.reaccion != ReaccionAlimento.NINGUNA }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── Plan semanal desde registrados ───────────────────────────────────────

    val planSemanalDesdeRegistrados: StateFlow<List<PlanSemanalSolidos>> = combine(
        _alimentosIntroducidos,
        _edadMeses,
        _alergenosNino,
        _nivelIngreso,
        _region,
        _recetasPersonalizadas
    ) { args ->
        val lista            = args[0] as List<AlimentoIntroducido>
        val meses            = args[1] as Int
        val alergenos        = args[2] as List<Alergeno>
        val nivel            = args[3] as NivelIngreso
        val region           = args[4] as RegionMexico
        val recetasCustom    = args[5] as List<RecetaMexicana>

        if (lista.isEmpty()) {
            shared?.resetPlanAlimentacion()
            return@combine emptyList<PlanSemanalSolidos>()
        }

        val guia = guiaParaEdad(meses)

        val nombresTolerados = lista
            .filter { it.reaccion != ReaccionAlimento.ALERGIA }
            .map { it.nombre }

        val planDieta = DietaEngine.generarPlanSemanal(
            meses                = meses,
            nivel                = nivel,
            region               = region,
            alergenosNiño        = alergenos,
            alimentosRegistrados = nombresTolerados,
            recetasCustom        = recetasCustom
        )

        shared?.setPlanSemanalAlimentacion(planDieta)

        planDieta.map { diaDieta ->
            PlanSemanalSolidos(
                diaSemana       = diaDieta.diaSemana,
                desayuno        = diaDieta.comidas.desayuno,
                almuerzo        = diaDieta.comidas.almuerzo,
                merienda        = diaDieta.comidas.colacion1,
                colacion2       = diaDieta.comidas.colacion2,
                cena            = diaDieta.comidas.cena,
                porcionLabel    = guia.porcionLabel,
                texturaLabel    = guia.texturaLabel,
                frecuenciaLabel = guia.frecuenciaLabel
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun guardarAlimento(childId: String, a: AlimentoIntroducido) {
        viewModelScope.launch {
            _uiState.value = AlimentacionUiState.Loading
            repo.guardarAlimento(childId, a)
                .onSuccess { _uiState.value = AlimentacionUiState.Saved }
                .onFailure { _uiState.value = AlimentacionUiState.Error(it.message ?: "Error al guardar") }
        }
    }

    fun eliminarAlimento(childId: String, id: String) {
        viewModelScope.launch {
            repo.eliminarAlimento(childId, id)
                .onSuccess { _uiState.value = AlimentacionUiState.Deleted }
                .onFailure { _uiState.value = AlimentacionUiState.Error(it.message ?: "Error al eliminar") }
        }
    }

    fun actualizarReaccion(childId: String, id: String, reaccion: ReaccionAlimento) {
        viewModelScope.launch {
            repo.actualizarReaccion(childId, id, reaccion)
                .onFailure { _uiState.value = AlimentacionUiState.Error(it.message ?: "Error al actualizar") }
        }
    }

    fun resetState() { _uiState.value = AlimentacionUiState.Idle }
}

private data class RecetasParams(
    val meses:      Int,
    val q:          String,
    val filtroTipo: FiltroTipoReceta,
    val alergenos:  List<Alergeno>,
    val nivel:      NivelIngreso
)