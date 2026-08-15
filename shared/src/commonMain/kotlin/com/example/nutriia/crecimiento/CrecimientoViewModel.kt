package com.example.nutriia.crecimiento

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class CrecimientoUiState {
    object Idle    : CrecimientoUiState()
    object Loading : CrecimientoUiState()
    object Saved   : CrecimientoUiState()
    object Deleted : CrecimientoUiState()
    data class Error(val msg: String) : CrecimientoUiState()
}

class CrecimientoViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = CrecimientoRepository()

    private val _historial      = MutableStateFlow<List<MedicionCrecimiento>>(emptyList())
    val historial: StateFlow<List<MedicionCrecimiento>> = _historial.asStateFlow()

    private val _ultimaMedicion = MutableStateFlow<MedicionCrecimiento?>(null)
    val ultimaMedicion: StateFlow<MedicionCrecimiento?> = _ultimaMedicion.asStateFlow()

    private val _uiState        = MutableStateFlow<CrecimientoUiState>(CrecimientoUiState.Idle)
    val uiState: StateFlow<CrecimientoUiState> = _uiState.asStateFlow()

    private val _edadMeses      = MutableStateFlow(0)
    private val _sexo           = MutableStateFlow<Sexo?>(null)

    private var currentChildId: String? = null
    private var observeJob: Job? = null

    // ── Init ──────────────────────────────────────────────────────────────────
    fun init(childId: String, meses: Int, sexo: Sexo? = null) {
        // Siempre actualizamos edad y sexo (pueden cambiar entre recomposiciones)
        _edadMeses.value = meses
        _sexo.value      = sexo

        // Solo recreamos el listener si cambia el hijo
        if (childId == currentChildId) return
        currentChildId = childId

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observarHistorial(childId)
                .catch { e ->
                    _uiState.value = CrecimientoUiState.Error(e.message ?: "Error al cargar historial")
                    emit(emptyList())
                }
                .collect { lista ->
                    val ordenada          = lista.sortedWith(
                        compareByDescending<MedicionCrecimiento> { it.fecha }
                            .thenByDescending { it.creadoEn?.seconds ?: 0L }
                            .thenByDescending { it.creadoEn?.nanoseconds ?: 0 }
                    )
                    _historial.value      = ordenada
                    _ultimaMedicion.value = ordenada.firstOrNull()
                }
        }
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────
    fun guardarMedicion(childId: String, m: MedicionCrecimiento) {
        viewModelScope.launch {
            _uiState.value = CrecimientoUiState.Loading
            repo.guardarMedicion(childId, m)
                .onSuccess { _uiState.value = CrecimientoUiState.Saved }
                .onFailure { _uiState.value = CrecimientoUiState.Error(it.message ?: "Error al guardar") }
        }
    }

    fun eliminarMedicion(childId: String, id: String) {
        viewModelScope.launch {
            repo.eliminarMedicion(childId, id)
                .onSuccess { _uiState.value = CrecimientoUiState.Deleted }
                .onFailure { _uiState.value = CrecimientoUiState.Error(it.message ?: "Error al eliminar") }
        }
    }

    fun resetState() { _uiState.value = CrecimientoUiState.Idle }

    // ── Curvas OMS ────────────────────────────────────────────────────────────
    val puntosOmsPeso:  List<PuntoOMS> get() = omsPesoPorSexo(_sexo.value)
    val puntosOmsTalla: List<PuntoOMS> get() = omsTallaPorSexo(_sexo.value)

    // ── Interpretación IMC ────────────────────────────────────────────────────
    // Usamos combine sobre los 3 flows para que se recalcule ante cualquier cambio
    val interpretacionActual: StateFlow<InterpretacionIMC?> =
        combine(_ultimaMedicion, _edadMeses, _sexo) { ultima, meses, sexo ->
            val m = ultima ?: return@combine null
            if (m.imc == 0.0) return@combine null
            interpretarIMC(m.imc, meses, sexo)
        }.stateIn(
            scope        = viewModelScope,
            started      = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val sexoRegistrado: StateFlow<Sexo?> = _sexo.asStateFlow()

    // ── Estadísticas derivadas ────────────────────────────────────────────────
    val variacionPeso: StateFlow<Double?> = _historial.map { lista ->
        if (lista.size < 2) null else lista[0].pesoKg - lista[1].pesoKg
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val variacionTalla: StateFlow<Double?> = _historial.map { lista ->
        if (lista.size < 2) null else lista[0].tallaCm - lista[1].tallaCm
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}