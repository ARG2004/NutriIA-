package com.example.nutriia.vinculacion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class VinculacionViewModel : ViewModel() {

    private val repo = VinculacionRepository()

    // ── Job del observer del padre (cancelable para forzar refresh) ────────────
    private var padreObserverJob: Job? = null

    // ── Estado compartido ──────────────────────────────────────────────────────
    private val _vinculaciones = MutableStateFlow<List<Vinculacion>>(emptyList())
    val vinculaciones: StateFlow<List<Vinculacion>> = _vinculaciones

    private val _miPerfil = MutableStateFlow<NutriologoPublico?>(null)
    val miPerfil: StateFlow<NutriologoPublico?> = _miPerfil

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _exito = MutableStateFlow<String?>(null)
    val exito: StateFlow<String?> = _exito

    // Resultado de búsqueda puntual (por código o email)
    private val _nutriologoEncontrado = MutableStateFlow<NutriologoPublico?>(null)
    val nutriologoEncontrado: StateFlow<NutriologoPublico?> = _nutriologoEncontrado

    // ── Directorio ─────────────────────────────────────────────────────────────
    private val _directorio = MutableStateFlow<List<NutriologoPublico>>(emptyList())
    val directorio: StateFlow<List<NutriologoPublico>> = _directorio

    private val _cargandoDirectorio = MutableStateFlow(false)
    val cargandoDirectorio: StateFlow<Boolean> = _cargandoDirectorio

    // Nutriólogo seleccionado desde el directorio
    private val _nutriologoSeleccionado = MutableStateFlow<NutriologoPublico?>(null)
    val nutriologoSeleccionado: StateFlow<NutriologoPublico?> = _nutriologoSeleccionado

    // ═════════════════════════════════════════════════════════════════════════
    // INIT
    // ═════════════════════════════════════════════════════════════════════════

    fun initComoNutriologo() {
        repo.observarVinculacionesDelNutriologo()
            .onEach { _vinculaciones.value = it }
            .launchIn(viewModelScope)
        cargarMiPerfilPublico()
    }

    fun initComoPadre() {
        padreObserverJob?.cancel()
        padreObserverJob = repo.observarVinculacionesDelPadre()
            .onEach { _vinculaciones.value = it }
            .launchIn(viewModelScope)
    }

    /** Fuerza re-suscripción al Flow del padre — útil al regresar del directorio en iOS */
    fun recargarVinculaciones() {
        initComoPadre()
        cargarDirectorio()
    }

    // ═════════════════════════════════════════════════════════════════════════
    // NUTRIÓLOGO — Acciones
    // ═════════════════════════════════════════════════════════════════════════

    fun publicarPerfil(nombre: String, especialidad: String, cedula: String, email: String) {
        viewModelScope.launch {
            _cargando.value = true
            limpiarError()
            repo.publicarPerfilNutriologo(nombre, especialidad, cedula, email).fold(
                onSuccess = {
                    _miPerfil.value = it
                    _exito.value = "Perfil actualizado correctamente"
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
                onFailure = { /* Carga silenciosa */ }
            )
        }
    }

    fun aceptarSolicitud(vinculacionId: String) {
        viewModelScope.launch {
            limpiarError()
            repo.responderSolicitud(vinculacionId, aceptar = true).fold(
                onSuccess = { _exito.value = "Vinculación aceptada" },
                onFailure = { _error.value = "No se pudo aceptar la solicitud" }
            )
        }
    }

    fun rechazarSolicitud(vinculacionId: String) {
        viewModelScope.launch {
            limpiarError()
            repo.responderSolicitud(vinculacionId, aceptar = false).fold(
                onSuccess = { _exito.value = "Solicitud rechazada" },
                onFailure = { _error.value = "Error al rechazar" }
            )
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PADRE — Búsqueda y Directorio
    // ═════════════════════════════════════════════════════════════════════════

    fun buscarPorCodigo(codigo: String) {
        if (codigo.isBlank()) return
        viewModelScope.launch {
            _cargando.value = true
            limpiarBusqueda()
            repo.buscarNutriologoPorCodigo(codigo).fold(
                onSuccess = {
                    if (it == null) _error.value = "No se encontró el código"
                    else _nutriologoEncontrado.value = it
                },
                onFailure = { _error.value = "Error en la búsqueda" }
            )
            _cargando.value = false
        }
    }

    fun buscarPorEmail(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            _cargando.value = true
            limpiarBusqueda()
            repo.buscarNutriologoPorEmail(email).fold(
                onSuccess = {
                    if (it == null) _error.value = "No se encontró el correo"
                    else _nutriologoEncontrado.value = it
                },
                onFailure = { _error.value = "Error en la búsqueda" }
            )
            _cargando.value = false
        }
    }

    fun cargarDirectorio() {
        viewModelScope.launch {
            _cargandoDirectorio.value = true
            repo.listarNutriologos().fold(
                onSuccess = { _directorio.value = it },
                onFailure = { _error.value = it.message ?: "Error al cargar directorio" }
            )
            _cargandoDirectorio.value = false
        }
    }

    fun buscarEnDirectorio(query: String) {
        viewModelScope.launch {
            _cargandoDirectorio.value = true
            repo.buscarNutriologosEnDirectorio(query).fold(
                onSuccess = { _directorio.value = it },
                onFailure = { _error.value = "Error al filtrar" }
            )
            _cargandoDirectorio.value = false
        }
    }

    fun seleccionarNutriologoDelDirectorio(nutriologo: NutriologoPublico) {
        _nutriologoSeleccionado.value = nutriologo
        _nutriologoEncontrado.value = nutriologo // Vinculamos para que solicitarVinculacion lo use
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PADRE — Solicitar Vinculación
    // ═════════════════════════════════════════════════════════════════════════

    fun solicitarVinculacion(padreNombre: String, childId: String, childNombre: String) {
        val nutriologo = _nutriologoEncontrado.value ?: run {
            _error.value = "Selecciona un nutriólogo primero"
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            limpiarError()
            repo.solicitarVinculacion(
                nutriologo = nutriologo,
                padreNombre = padreNombre,
                childId = childId,
                childNombre = childNombre
            ).fold(
                onSuccess = {
                    _exito.value = "Solicitud enviada a ${nutriologo.nombre}"
                    limpiarBusqueda() // Limpiar selección tras éxito
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
                onFailure = { _error.value = "No se pudo revocar la vinculación" }
            )
        }
    }

    fun limpiarError() {
        _error.value = null
    }

    fun limpiarExito() {
        _exito.value = null
    }

    fun limpiarBusqueda() {
        _nutriologoEncontrado.value = null
        _nutriologoSeleccionado.value = null
        limpiarError()
    }

    fun limpiarSeleccion() {
        _nutriologoSeleccionado.value = null
        _nutriologoEncontrado.value = null
        limpiarError()
        limpiarExito()
    }
}