package com.example.nutriia.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.vinculacion.EstadoVinculacion
import com.example.nutriia.vinculacion.NutriologoPublico
import com.example.nutriia.vinculacion.PlanAlimentario
import com.example.nutriia.vinculacion.Vinculacion
import com.example.nutriia.vinculacion.VinculacionRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.nutriia.utils.FechaUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class PacienteResumen(
    val ownerUid:            String  = "",
    val vinculacionId:       String  = "",
    val padreUid:            String  = "",
    val padreNombre:         String  = "",
    val childId:             String  = "",
    val childNombre:         String  = "",
    val birthDate:           String  = "",
    val weightKg:            String  = "",
    val heightCm:            String  = "",
    val hasAllergies:        Boolean = false,
    val ultimaActualizacion: String  = ""
)

data class NutritionistDashboardUiState(
    val miPerfil:              NutriologoPublico?    = null,
    val pacientes:             List<PacienteResumen> = emptyList(),
    val planesActivos:         List<PlanAlimentario> = emptyList(),
    val solicitudesPendientes: List<Vinculacion>     = emptyList(),
    val cargando:              Boolean               = true,
    val error:                 String?               = null
)

class NutritionistDashboardViewModel : ViewModel() {

    private val auth            = FirebaseAuth.getInstance()
    private val db              = FirebaseFirestore.getInstance()
    private val vinculacionRepo = VinculacionRepository()

    private val _uiState = MutableStateFlow(NutritionistDashboardUiState())
    val uiState: StateFlow<NutritionistDashboardUiState> = _uiState

    // Guarda un Job por cada childId para no duplicar listeners
    private val hijoJobs = mutableMapOf<String, Job>()

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────

    fun init() {
        cargarPerfilPublico()
        escucharVinculaciones()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Perfil público del nutriólogo
    // ─────────────────────────────────────────────────────────────────────────

    private fun cargarPerfilPublico() {
        viewModelScope.launch {
            val resultado = vinculacionRepo.obtenerMiPerfilPublico()
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
                val source = if (com.example.nutriia.offline.OfflineManager.hayConexion()) {
                    com.google.firebase.firestore.Source.DEFAULT
                } else {
                    com.google.firebase.firestore.Source.CACHE
                }
                val doc          = db.collection("usuarios").document(uid).get(source).await()
                val nombre       = doc.getString("nombre")       ?: return@launch
                val especialidad = doc.getString("especialidad") ?: ""
                val cedula       = doc.getString("cedula")       ?: ""
                val email        = doc.getString("email")        ?: ""

                vinculacionRepo.publicarPerfilNutriologo(
                    nombre, especialidad, cedula, email
                ).fold(
                    onSuccess = { _uiState.value = _uiState.value.copy(miPerfil = it) },
                    onFailure = { }
                )
            } catch (_: Exception) { }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. Vinculaciones en tiempo real
    // ─────────────────────────────────────────────────────────────────────────

    private fun escucharVinculaciones() {
        vinculacionRepo.observarVinculacionesDelNutriologo()
            .onEach { vinculaciones ->
                val activas    = vinculaciones.filter { it.estado == EstadoVinculacion.ACTIVO }
                val pendientes = vinculaciones.filter { it.estado == EstadoVinculacion.PENDIENTE }

                _uiState.value = _uiState.value.copy(
                    solicitudesPendientes = pendientes,
                    cargando             = false
                )

                // Cancela listeners de hijos que ya no están en vinculaciones activas
                val idsActivos = activas.map { it.childId }.toSet()
                hijoJobs.keys
                    .filter { it !in idsActivos }
                    .forEach { key -> hijoJobs.remove(key)?.cancel() }

                // Elimina del estado pacientes que ya no tienen vinculación activa
                if (idsActivos.isEmpty()) {
                    _uiState.value = _uiState.value.copy(
                        pacientes     = emptyList(),
                        planesActivos = emptyList()
                    )
                }

                cargarDatosPacientes(activas)
            }
            .launchIn(viewModelScope)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Listener en tiempo real por cada hijo vinculado
    // ─────────────────────────────────────────────────────────────────────────

    private fun cargarDatosPacientes(vinculaciones: List<Vinculacion>) {
        vinculaciones.forEach { vinc ->
            // Evita duplicar el listener si ya existe para este hijo
            if (hijoJobs.containsKey(vinc.childId)) return@forEach

            val job = viewModelScope.launch {
                vinculacionRepo.observarHijo(vinc.padreUid, vinc.childId)
                    .collect { data ->
                        if (data == null) return@collect

                        val paciente = PacienteResumen(
                            ownerUid            = vinc.padreUid,
                            vinculacionId       = vinc.id,
                            padreUid            = vinc.padreUid,
                            padreNombre         = vinc.padreNombre,
                            childId             = vinc.childId,
                            childNombre         = data["name"] as? String ?: vinc.childNombre,
                            birthDate           = data["birthDate"] as? String ?: "",
                            weightKg            = data["weightKg"] as? String ?: "",
                            heightCm            = data["heightCm"] as? String ?: "",
                            hasAllergies        = data["hasAllergies"] as? Boolean ?: false,
                            ultimaActualizacion = formatearFecha(data["creadoEn"])
                        )

                        // Actualiza solo este paciente en la lista existente
                        val listaActual = _uiState.value.pacientes.toMutableList()
                        val idx = listaActual.indexOfFirst { it.childId == vinc.childId }
                        if (idx >= 0) listaActual[idx] = paciente
                        else listaActual.add(paciente)

                        _uiState.value = _uiState.value.copy(pacientes = listaActual)

                        // Carga planes activos del hijo (lectura única, no necesita ser RT)
                        cargarPlanesDelHijo(vinc.padreUid, vinc.childId)
                    }
            }
            hijoJobs[vinc.childId] = job
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Planes activos del hijo (lectura única)
    // ─────────────────────────────────────────────────────────────────────────

    private fun cargarPlanesDelHijo(padreUid: String, childId: String) {
        viewModelScope.launch {
            try {
                val source = if (com.example.nutriia.offline.OfflineManager.hayConexion()) {
                    com.google.firebase.firestore.Source.DEFAULT
                } else {
                    com.google.firebase.firestore.Source.CACHE
                }

                val query = db.collection("usuarios")
                    .document(padreUid)
                    .collection("hijos")
                    .document(childId)
                    .collection("planes_alimentarios")

                val planesSnap = if (com.example.nutriia.offline.OfflineManager.hayConexion()) {
                    query.whereEqualTo("activo", true).get(source).await()
                } else {
                    query.get(source).await()
                }

                val planesDelHijo = planesSnap.documents.mapNotNull { planDoc ->
                    if (!com.example.nutriia.offline.OfflineManager.hayConexion() && planDoc.getBoolean("activo") != true) {
                        return@mapNotNull null
                    }
                    planDoc.data?.let { PlanAlimentario.fromMap(planDoc.id, it) }
                }

                // Reemplaza solo los planes de este hijo, conserva los demás
                val planesActuales = _uiState.value.planesActivos
                    .filter { it.childId != childId }
                    .toMutableList()
                planesActuales.addAll(planesDelHijo)

                _uiState.value = _uiState.value.copy(planesActivos = planesActuales)
            } catch (_: Exception) { }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Acciones
    // ─────────────────────────────────────────────────────────────────────────

    fun aceptarSolicitud(vinculacionId: String) {
        viewModelScope.launch {
            vinculacionRepo.responderSolicitud(vinculacionId, aceptar = true)
        }
    }

    fun rechazarSolicitud(vinculacionId: String) {
        viewModelScope.launch {
            vinculacionRepo.responderSolicitud(vinculacionId, aceptar = false)
        }
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Limpieza al destruir el ViewModel
    // ─────────────────────────────────────────────────────────────────────────

    override fun onCleared() {
        super.onCleared()
        hijoJobs.values.forEach { it.cancel() }
        hijoJobs.clear()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private fun formatearFecha(creadoEnRaw: Any?): String {
        val timestamp = when (creadoEnRaw) {
            is Number -> creadoEnRaw.toLong()
            is String -> {
                FechaUtils.parsearFechaHora(creadoEnRaw)?.time ?: 0L
            }
            is com.google.firebase.Timestamp -> creadoEnRaw.toDate().time
            else -> 0L
        }
        if (timestamp == 0L) return "Sin datos"
        val diffMs   = System.currentTimeMillis() - timestamp
        val diffDias = diffMs / (1000 * 60 * 60 * 24)
        return when {
            diffDias == 0L -> "Hoy"
            diffDias == 1L -> "Ayer"
            diffDias < 7L  -> "Hace $diffDias días"
            else           -> "Hace ${diffDias / 7} semana(s)"
        }
    }
}