package com.example.nutriia.ginecologo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.firebase.auth.FirebaseAuth
import com.example.nutriia.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import com.example.nutriia.firebase.firestore.await

data class SolicitudEmbarazoUiState(
    val miPerfil:              GinecologoPublico?        = null,
    val vinculacionesActivas:  List<VinculacionEmbarazo> = emptyList(),
    val solicitudesPendientes: List<VinculacionEmbarazo>  = emptyList(),
    val cargando:              Boolean                   = true,
    val error:                 String?                   = null
)

class GinecologoDashboardViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()
    private val ginecologoRepo = GinecologoRepository()

    private val _uiState = MutableStateFlow(SolicitudEmbarazoUiState())
    val uiState: StateFlow<SolicitudEmbarazoUiState> = _uiState

    fun init() {
        cargarPerfilPublico()
        escucharVinculaciones()
    }

    private fun cargarPerfilPublico() {
        viewModelScope.launch {
            val resultado = ginecologoRepo.obtenerMiPerfilPublico()
            resultado.fold(
                onSuccess = { perfil ->
                    _uiState.value = _uiState.value.copy(miPerfil = perfil)
                    if (perfil == null) crearPerfilPublicoDesdeUsuario()
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(
                        error    = "No se pudo cargar el perfil",
                        cargando = false
                    )
                }
            )
        }
    }

    private fun crearPerfilPublicoDesdeUsuario() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snap         = db.collection("usuarios").document(uid).get().await()
                val doc          = snap as? com.example.nutriia.firebase.firestore.DocumentSnapshot
                val nombre       = doc?.getString("nombre")       ?: return@launch
                val especialidad = doc?.getString("especialidad") ?: "Ginecología y Obstetricia"
                val cedula       = doc?.getString("cedula")       ?: ""
                val email        = doc?.getString("email")        ?: ""

                ginecologoRepo.publicarPerfilGinecologo(
                    nombre, especialidad, cedula, email
                ).fold(
                    onSuccess = { _uiState.value = _uiState.value.copy(miPerfil = it) },
                    onFailure = { }
                )
            } catch (_: Exception) { }
        }
    }

    private fun escucharVinculaciones() {
        ginecologoRepo.observarVinculacionesDelGinecologo()
            .onEach { vinculaciones ->
                val activas    = vinculaciones.filter { it.estado == EstadoVinculacionEmbarazo.ACTIVO }
                val pendientes = vinculaciones.filter { it.estado == EstadoVinculacionEmbarazo.PENDIENTE }
                _uiState.value = _uiState.value.copy(
                    vinculacionesActivas   = activas,
                    solicitudesPendientes  = pendientes,
                    cargando               = false
                )
            }.launchIn(viewModelScope)
    }

    fun aceptarSolicitud(vinculacionId: String) {
        viewModelScope.launch { ginecologoRepo.responderSolicitud(vinculacionId, aceptar = true) }
    }

    fun rechazarSolicitud(vinculacionId: String) {
        viewModelScope.launch { ginecologoRepo.responderSolicitud(vinculacionId, aceptar = false) }
    }

    fun agendarCita(vinculacionId: String, fecha: String, hora: String, motivo: String, tipo: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            ginecologoRepo.agendarCita(vinculacionId, fecha, hora, motivo, tipo).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(cargando = false)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "Error al agendar cita: ${it.message}")
                }
            )
        }
    }

    fun cancelarCita(vinculacionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(cargando = true)
            ginecologoRepo.cancelarCita(vinculacionId).fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(cargando = false)
                },
                onFailure = {
                    _uiState.value = _uiState.value.copy(cargando = false, error = "Error al cancelar cita: ${it.message}")
                }
            )
        }
    }

    fun limpiarError() { _uiState.value = _uiState.value.copy(error = null) }
}
