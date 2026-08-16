package com.example.nutriia.pediatra

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.vinculacion.EstadoVinculacion
import com.example.nutriia.vinculacion.NutriologoPublico
import com.example.nutriia.vinculacion.PlanAlimentario
import com.example.nutriia.vinculacion.Vinculacion
import com.example.nutriia.vinculacion.VinculacionRepository
import com.example.nutriia.firebase.auth.FirebaseAuth
import com.example.nutriia.firebase.firestore.FieldValue
import com.example.nutriia.firebase.firestore.FirebaseFirestore
import com.example.nutriia.firebase.firestore.ListenerRegistration
import com.example.nutriia.firebase.firestore.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import com.example.nutriia.utils.FechaUtils
import kotlinx.coroutines.launch

// ─── Modelos de comunicación ──────────────────────────────────────────────────

enum class TipoMensaje { CONSULTA, COMENTARIO, SOLICITUD_LLAMADA }
enum class EstadoMensaje { NUEVO, LEIDO, RESPONDIDO }

data class MensajePaciente(
    val id:           String         = "",
    val padreUid:     String         = "",
    val padreNombre:  String         = "",
    val childId:      String         = "",
    val childNombre:  String         = "",
    val tipo:         TipoMensaje    = TipoMensaje.CONSULTA,
    val asunto:       String         = "",
    val contenido:    String         = "",
    val estado:       EstadoMensaje  = EstadoMensaje.NUEVO,
    val creadoEn:     Long           = 0L,
    val respuesta:    String?        = null,
    val respondidoEn: Long?          = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id"           to id,
        "padreUid"     to padreUid,
        "padreNombre"  to padreNombre,
        "childId"      to childId,
        "childNombre"  to childNombre,
        "tipo"         to tipo.name,
        "asunto"       to asunto,
        "contenido"    to contenido,
        "estado"       to estado.name,
        "creadoEn"     to creadoEn,
        "respuesta"    to respuesta,
        "respondidoEn" to respondidoEn
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): MensajePaciente = MensajePaciente(
            id           = id,
            padreUid     = map["padreUid"]    as? String ?: "",
            padreNombre  = map["padreNombre"] as? String ?: "",
            childId      = map["childId"]     as? String ?: "",
            childNombre  = map["childNombre"] as? String ?: "",
            tipo         = runCatching {
                TipoMensaje.valueOf(map["tipo"] as? String ?: "")
            }.getOrDefault(TipoMensaje.CONSULTA),
            asunto       = map["asunto"]      as? String ?: "",
            contenido    = map["contenido"]   as? String ?: "",
            estado       = runCatching {
                EstadoMensaje.valueOf(map["estado"] as? String ?: "")
            }.getOrDefault(EstadoMensaje.NUEVO),
            creadoEn     = map["creadoEn"]    as? Long   ?: 0L,
            respuesta    = map["respuesta"]   as? String,
            respondidoEn = map["respondidoEn"] as? Long
        )
    }
}

data class PacienteDetalle(
    val vinculacionId:       String           = "",
    val padreUid:            String           = "",
    val padreNombre:         String           = "",
    val childId:             String           = "",
    val childNombre:         String           = "",
    val birthDate:           String           = "",
    val weightKg:            String           = "",
    val heightCm:            String           = "",
    val hasAllergies:        Boolean          = false,
    val alergias:            String           = "",
    val ultimaActualizacion: String           = "",
    val planesActivos:       List<PlanAlimentario> = emptyList(),
    val mensajesNoLeidos:    Int              = 0,
    // ── NUEVO: historial de consultas del hijo ────────────────────────────
    val consultas:           List<Consulta>   = emptyList()
)

// ─── Estado UI ────────────────────────────────────────────────────────────────

data class PediatraDashboardUiState(
    val miPerfil:              NutriologoPublico?    = null,
    val pacientes:             List<PacienteDetalle> = emptyList(),
    val solicitudesPendientes: List<Vinculacion>     = emptyList(),
    val mensajes:              List<MensajePaciente> = emptyList(),
    val mensajesNoLeidos:      Int                   = 0,
    val cargando:              Boolean               = true,
    val error:                 String?               = null,
    val tabSeleccionado:       Int                   = 0,
    // ── NUEVO: paciente seleccionado para agregar nota ────────────────────
    val pacienteParaNota:      PacienteDetalle?      = null
)

// ─── ViewModel ────────────────────────────────────────────────────────────────

class PediatraDashboardViewModel : ViewModel() {

    private val auth            = FirebaseAuth.getInstance()
    private val db              = FirebaseFirestore.getInstance()
    private val vinculacionRepo = VinculacionRepository()
    private val pediatraRepo    = PediatraRepository()

    private val _uiState = MutableStateFlow(PediatraDashboardUiState())
    val uiState: StateFlow<PediatraDashboardUiState> = _uiState

    private var mensajesListener: ListenerRegistration? = null
    // Mapa de listeners de consultas activos por childId → para limpiarlos al salir
    private val consultasJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // ─────────────────────────────────────────────────────────────────────────
    // INIT
    // ─────────────────────────────────────────────────────────────────────────

    fun init() {
        cargarPerfilPublico()
        escucharVinculaciones()
        escucharMensajes()
    }

    override fun onCleared() {
        super.onCleared()
        mensajesListener?.remove()
        consultasJobs.values.forEach { it.cancel() }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. Perfil público del pediatra/nutriólogo
    // ─────────────────────────────────────────────────────────────────────────

    private fun cargarPerfilPublico() {
        viewModelScope.launch {
            vinculacionRepo.obtenerMiPerfilPublico().fold(
                onSuccess = { perfil ->
                    _uiState.value = _uiState.value.copy(miPerfil = perfil)
                    if (perfil == null) crearPerfilDesdeUsuario()
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

    private fun crearPerfilDesdeUsuario() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val snap         = db.collection("usuarios").document(uid).get().await()
                val doc          = snap as? com.example.nutriia.firebase.firestore.DocumentSnapshot
                val nombre       = doc?.getString("nombre")       ?: return@launch
                val especialidad = doc?.getString("especialidad") ?: ""
                val cedula       = doc?.getString("cedula")       ?: ""
                val email        = doc?.getString("email")        ?: ""
                vinculacionRepo.publicarPerfilNutriologo(nombre, especialidad, cedula, email).fold(
                    onSuccess = { _uiState.value = _uiState.value.copy(miPerfil = it) },
                    onFailure = {}
                )
            } catch (_: Exception) {}
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
                    cargando              = false
                )

                cargarDetallesPacientes(activas)
            }
            .launchIn(viewModelScope)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. Detalles de cada paciente (hijo + planes + consultas en tiempo real)
    // ─────────────────────────────────────────────────────────────────────────

    private fun cargarDetallesPacientes(vinculaciones: List<Vinculacion>) {
        viewModelScope.launch {
            try {
                val lista = mutableListOf<PacienteDetalle>()

                vinculaciones.forEach { vinc ->
                    try {
                        val planesSnap = db.collection("usuarios")
                            .document(vinc.padreUid)
                            .collection("hijos")
                            .document(vinc.childId)
                            .collection("planes_alimentarios")
                            .whereEqualTo("activo", true)
                            .get().await()

                        val planes = (planesSnap as? com.example.nutriia.firebase.firestore.QuerySnapshot)?.documents?.mapNotNull { planDoc ->
                            PlanAlimentario.fromMap(planDoc.id, planDoc.data)
                        } ?: emptyList()

                        val uid = auth.currentUser?.uid ?: ""
                        val noLeidosSnap = db.collection("nutriologos")
                            .document(uid)
                            .collection("mensajes")
                            .whereEqualTo("childId", vinc.childId)
                            .whereEqualTo("estado", EstadoMensaje.NUEVO.name)
                            .get().await()

                        lista.add(
                            PacienteDetalle(
                                vinculacionId        = vinc.id,
                                padreUid             = vinc.padreUid,
                                padreNombre          = vinc.padreNombre,
                                childId              = vinc.childId,
                                childNombre          = vinc.childNombre,
                                ultimaActualizacion  = FechaUtils.formatearFecha(vinc.actualizadoEn ?: 0L),
                                planesActivos        = planes,
                                mensajesNoLeidos     = (noLeidosSnap as? com.example.nutriia.firebase.firestore.QuerySnapshot)?.size() ?: 0
                            )
                        )
                    } catch (_: Exception) {}
                }

                _uiState.value = _uiState.value.copy(
                    cargando  = false,
                    pacientes = lista
                )

                // Iniciar Flow de consultas en tiempo real para cada paciente
                vinculaciones.forEach { vinc ->
                    escucharConsultasDePaciente(vinc.padreUid, vinc.childId)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    cargando = false,
                    error    = "Error al cargar pacientes: ${e.message}"
                )
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. Flow de consultas por paciente (tiempo real)
    // ─────────────────────────────────────────────────────────────────────────

    private fun escucharConsultasDePaciente(padreUid: String, childId: String) {
        // Cancelar listener previo del mismo hijo si existe
        consultasJobs[childId]?.cancel()

        val job = pediatraRepo.observarConsultas(padreUid, childId)
            .onEach { consultas ->
                // Actualizar solo el paciente correspondiente en la lista
                val pacientesActualizados = _uiState.value.pacientes.map { paciente ->
                    if (paciente.childId == childId) paciente.copy(consultas = consultas)
                    else paciente
                }
                _uiState.value = _uiState.value.copy(pacientes = pacientesActualizados)
            }
            .launchIn(viewModelScope)

        consultasJobs[childId] = job
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. Mensajes en tiempo real
    // ─────────────────────────────────────────────────────────────────────────

    private fun escucharMensajes() {
        val uid = auth.currentUser?.uid ?: return

        mensajesListener = db.collection("nutriologos")
            .document(uid)
            .collection("mensajes")
            .orderBy("creadoEn", com.example.nutriia.firebase.firestore.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val lista = snap?.documents?.mapNotNull { doc ->
                    MensajePaciente.fromMap(doc.id, doc.data)
                } ?: emptyList()

                _uiState.value = _uiState.value.copy(
                    mensajes         = lista,
                    mensajesNoLeidos = lista.count { it.estado == EstadoMensaje.NUEVO }
                )
            }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 6. Guardar nota del nutriólogo en el expediente del hijo
    // ─────────────────────────────────────────────────────────────────────────

    fun guardarNotaNutriologo(
        padreUid: String,
        childId:  String,
        nota:     Consulta
    ) {
        val nutriologoNombre = _uiState.value.miPerfil?.nombre ?: "Nutriólogo"
        viewModelScope.launch {
            pediatraRepo.guardarNotaNutriologo(
                padreUid         = padreUid,
                childId          = childId,
                nota             = nota,
                nutriologoNombre = nutriologoNombre
            ).fold(
                onSuccess = { _uiState.value = _uiState.value.copy(pacienteParaNota = null) },
                onFailure = { _uiState.value = _uiState.value.copy(error = "Error al guardar la nota: ${it.message}") }
            )
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 7. Acciones públicas
    // ─────────────────────────────────────────────────────────────────────────

    fun aceptarSolicitud(vinculacionId: String) {
        viewModelScope.launch { vinculacionRepo.responderSolicitud(vinculacionId, aceptar = true) }
    }

    fun rechazarSolicitud(vinculacionId: String) {
        viewModelScope.launch { vinculacionRepo.responderSolicitud(vinculacionId, aceptar = false) }
    }

    fun marcarMensajeLeido(mensajeId: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("nutriologos")
                    .document(uid).collection("mensajes").document(mensajeId)
                    .update("estado", EstadoMensaje.LEIDO.name).await()
            } catch (_: Exception) {}
        }
    }

    fun responderMensaje(mensajeId: String, respuesta: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("nutriologos")
                    .document(uid).collection("mensajes").document(mensajeId)
                    .update(mapOf(
                        "respuesta"    to respuesta,
                        "estado"       to EstadoMensaje.RESPONDIDO.name,
                        "respondidoEn" to com.example.nutriia.platform.currentTimeMillis()
                    )).await()
            } catch (_: Exception) {}
        }
    }

    fun abrirDialogNota(paciente: PacienteDetalle) {
        _uiState.value = _uiState.value.copy(pacienteParaNota = paciente)
    }

    fun cerrarDialogNota() {
        _uiState.value = _uiState.value.copy(pacienteParaNota = null)
    }

    fun cambiarTab(tab: Int) {
        _uiState.value = _uiState.value.copy(tabSeleccionado = tab)
    }

    fun limpiarError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────────────────────────────────────

    private fun formatearFecha(creadoEnRaw: Any?): String {
        val timestamp = when (creadoEnRaw) {
            is Number -> creadoEnRaw.toLong()
            is String -> FechaUtils.parsearFechaHora(creadoEnRaw)
            else -> 0L
        }
        if (timestamp == 0L) return "Sin datos"
        val diffDias = (com.example.nutriia.platform.currentTimeMillis() - timestamp) / (1000 * 60 * 60 * 24)
        return when {
            diffDias == 0L -> "Hoy"
            diffDias == 1L -> "Ayer"
            diffDias < 7L  -> "Hace $diffDias días"
            else           -> "Hace ${diffDias / 7} semana(s)"
        }
    }
}