package com.example.nutriia.ginecologo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class GinecologoViewModel : ViewModel() {

    private val repo = GinecologoRepository()

    private var mamaVinculacionJob: Job? = null
    private var mamaCitasJob: Job? = null
    private var gineVinculacionesJob: Job? = null

    // ── Estado compartido ──────────────────────────────────────────────────────
    private val _vinculacionActual = MutableStateFlow<VinculacionEmbarazo?>(null)
    val vinculacionActual: StateFlow<VinculacionEmbarazo?> = _vinculacionActual

    private val _citasDeLaMama = MutableStateFlow<List<CitaEmbarazo>>(emptyList())
    val citasDeLaMama: StateFlow<List<CitaEmbarazo>> = _citasDeLaMama

    private val _vinculacionesGinecologo = MutableStateFlow<List<VinculacionEmbarazo>>(emptyList())
    val vinculacionesGinecologo: StateFlow<List<VinculacionEmbarazo>> = _vinculacionesGinecologo

    private val _miPerfil = MutableStateFlow<GinecologoPublico?>(null)
    val miPerfil: StateFlow<GinecologoPublico?> = _miPerfil

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _exito = MutableStateFlow<String?>(null)
    val exito: StateFlow<String?> = _exito

    // Resultado de búsqueda puntual
    private val _ginecologoEncontrado = MutableStateFlow<GinecologoPublico?>(null)
    val ginecologoEncontrado: StateFlow<GinecologoPublico?> = _ginecologoEncontrado

    // Directorio
    private val _directorio = MutableStateFlow<List<GinecologoPublico>>(emptyList())
    val directorio: StateFlow<List<GinecologoPublico>> = _directorio

    private val _cargandoDirectorio = MutableStateFlow(false)
    val cargandoDirectorio: StateFlow<Boolean> = _cargandoDirectorio

    // ═════════════════════════════════════════════════════════════════════════
    // INIT
    // ═════════════════════════════════════════════════════════════════════════

    fun initComoMama() {
        mamaVinculacionJob?.cancel()
        mamaVinculacionJob = repo.observarVinculacionDeLaMama()
            .onEach { _vinculacionActual.value = it }
            .launchIn(viewModelScope)

        mamaCitasJob?.cancel()
        mamaCitasJob = repo.observarCitasDeLaMama()
            .onEach { _citasDeLaMama.value = it }
            .launchIn(viewModelScope)
    }

    fun initComoGinecologo() {
        gineVinculacionesJob?.cancel()
        gineVinculacionesJob = repo.observarVinculacionesDelGinecologo()
            .onEach { _vinculacionesGinecologo.value = it }
            .launchIn(viewModelScope)
        cargarMiPerfilPublico()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GINECÓLOGO — Acciones
    // ═════════════════════════════════════════════════════════════════════════

    fun publicarPerfil(nombre: String, especialidad: String, cedula: String, email: String) {
        viewModelScope.launch {
            _cargando.value = true
            limpiarError()
            repo.publicarPerfilGinecologo(nombre, especialidad, cedula, email).fold(
                onSuccess = {
                    _miPerfil.value = it
                    _exito.value = "Perfil de ginecólogo actualizado"
                },
                onFailure = { _error.value = it.message ?: "Error al publicar perfil" }
            )
            _cargando.value = false
        }
    }

    private fun cargarMiPerfilPublico() {
        viewModelScope.launch {
            repo.obtenerMiPerfilPublico().fold(
                onSuccess = { _miPerfil.value = it },
                onFailure = { /* Silencioso */ }
            )
        }
    }

    fun responderSolicitud(vinculacionId: String, aceptar: Boolean) {
        viewModelScope.launch {
            limpiarError()
            repo.responderSolicitud(vinculacionId, aceptar).fold(
                onSuccess = { _exito.value = if (aceptar) "Vinculación aceptada" else "Solicitud rechazada" },
                onFailure = { _error.value = "Error al responder solicitud" }
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAMÁ — Búsqueda y Directorio
    // ═════════════════════════════════════════════════════════════════════════

    fun buscarPorCodigo(codigo: String) {
        if (codigo.isBlank()) return
        viewModelScope.launch {
            _cargando.value = true
            limpiarBusqueda()
            repo.buscarGinecologoPorCodigo(codigo).fold(
                onSuccess = {
                    if (it == null) _error.value = "Ginecólogo no encontrado"
                    else _ginecologoEncontrado.value = it
                },
                onFailure = { _error.value = "Error en la búsqueda" }
            )
            _cargando.value = false
        }
    }

    fun cargarDirectorio() {
        viewModelScope.launch {
            _cargandoDirectorio.value = true
            repo.listarGinecologos().fold(
                onSuccess = { _directorio.value = it },
                onFailure = { _error.value = "Error al cargar directorio" }
            )
            _cargandoDirectorio.value = false
        }
    }

    fun buscarEnDirectorio(query: String) {
        viewModelScope.launch {
            _cargandoDirectorio.value = true
            repo.buscarGinecologosEnDirectorio(query).fold(
                onSuccess = { _directorio.value = it },
                onFailure = { _error.value = "Error al filtrar" }
            )
            _cargandoDirectorio.value = false
        }
    }

    fun seleccionarGinecologo(ginecologo: GinecologoPublico) {
        _ginecologoEncontrado.value = ginecologo
    }

    // ═════════════════════════════════════════════════════════════════════════
    // MAMÁ — Solicitar Vinculación
    // ═════════════════════════════════════════════════════════════════════════

    fun solicitarVinculacion(mamaNombre: String) {
        val ginecologo = _ginecologoEncontrado.value ?: run {
            _error.value = "Selecciona un ginecólogo primero"
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            limpiarError()
            repo.solicitarVinculacion(ginecologo, mamaNombre).fold(
                onSuccess = {
                    _exito.value = "Solicitud enviada a ${ginecologo.nombre}"
                    limpiarBusqueda()
                },
                onFailure = { _error.value = it.message ?: "Error al enviar solicitud" }
            )
            _cargando.value = false
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // AMBOS — Gestión y Limpieza
    // ═════════════════════════════════════════════════════════════════════════

    fun revocarVinculacion(vinculacionId: String) {
        viewModelScope.launch {
            repo.revocarVinculacion(vinculacionId).fold(
                onSuccess = { _exito.value = "Vinculación revocada" },
                onFailure = { _error.value = "Error al revocar" }
            )
        }
    }

    fun agendarCita(vinculacionId: String, fecha: String, hora: String, motivo: String, tipo: String) {
        viewModelScope.launch {
            _cargando.value = true
            limpiarError()
            repo.agendarCita(vinculacionId, fecha, hora, motivo, tipo).fold(
                onSuccess = { _exito.value = "Cita agendada con éxito" },
                onFailure = { _error.value = it.message ?: "Error al agendar cita" }
            )
            _cargando.value = false
        }
    }

    fun limpiarError()    { _error.value = null }
    fun limpiarExito()    { _exito.value = null }
    fun limpiarBusqueda() { _ginecologoEncontrado.value = null }
}
