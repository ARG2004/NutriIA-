package com.example.nutriia.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.example.nutriia.firebase.firestore.await

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    data class Exito(val rol: String, val hijos: List<ChildProfile> = emptyList()) : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}

private data class UsuarioSesion(
    val uid:      String = "",
    val nombre:   String = "",
    val email:    String = "",
    val telefono: String = "",
    val rol:      String = "padre",
    val intentosIaDisponibles: Int = 3,
    val suscripcionIaVigenteHasta: Long = 0L,
    val ultimoResetIa: Long = 0L
)

class LoginViewModel : ViewModel() {

    private val repositorio = RepositorioLogin()

    private val _estado = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val estado: StateFlow<LoginUiState> = _estado

    private val _sesion = MutableStateFlow(UsuarioSesion())

    val uidUsuario:      String get() = _sesion.value.uid
    val nombreUsuario:   String get() = _sesion.value.nombre
    val emailUsuario:    String get() = _sesion.value.email
    val telefonoUsuario: String get() = _sesion.value.telefono
    val rolUsuario:      String get() = _sesion.value.rol
    val intentosIaDisponibles: Int get() = _sesion.value.intentosIaDisponibles
    val suscripcionIaVigenteHasta: Long get() = _sesion.value.suscripcionIaVigenteHasta
    val ultimoResetIa: Long get() = _sesion.value.ultimoResetIa

    fun decrementarIntentoIaLocal() {
        val uid = _sesion.value.uid
        val restantes = _sesion.value.intentosIaDisponibles
        if (restantes > 0 && uid.isNotBlank()) {
            _sesion.value = _sesion.value.copy(intentosIaDisponibles = restantes - 1)
            viewModelScope.launch {
                repositorio.decrementarIntentoIa(uid, restantes)
            }
        }
    }

    // ── Login ─────────────────────────────────────────────────────────────────
    fun login(email: String, contrasena: String) {
        viewModelScope.launch {
            _estado.value = LoginUiState.Loading
            val resultado = repositorio.login(email, contrasena)
            _estado.value = when (resultado) {
                is ResultadoAuth.Exito -> {
                    val hijos = if (resultado.rol == "padre")
                        repositorio.cargarHijos(resultado.uid)
                    else emptyList()
                    cargarDatosSesion(resultado.uid, resultado.rol, email)
                    LoginUiState.Exito(rol = resultado.rol, hijos = hijos)
                }
                is ResultadoAuth.Error -> LoginUiState.Error(resultado.mensaje)
            }
        }
    }

    // ── Recuperar contraseña ──────────────────────────────────────────────────
    fun recuperarContrasena(email: String, onResultado: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResultado(repositorio.recuperarContrasena(email))
        }
    }

    // ── Verificar sesión activa ───────────────────────────────────────────────
    fun verificarSesion(onResultado: (rol: String?, hijos: List<ChildProfile>) -> Unit) {
        viewModelScope.launch {
            val uid = SessionManager.obtenerUid()
            if (uid == null) {
                onResultado(null, emptyList())
                return@launch
            }
            val rol = repositorio.obtenerRol(uid)
            val hijos = if (rol == "padre")
                repositorio.cargarHijos(uid)
            else emptyList()
            val usuarioFirebase = repositorio.obtenerUsuarioActual()
            val emailFirebase = usuarioFirebase?.email ?: ""
            cargarDatosSesion(uid, rol, emailFirebase)
            _estado.value = LoginUiState.Exito(rol = rol, hijos = hijos)
            onResultado(rol, hijos)
        }
    }

    // ── Guardar hijo ──────────────────────────────────────────────────────────
    fun guardarHijo(child: ChildProfile, onResult: (Boolean) -> Unit = {}) {
        val uid = repositorio.obtenerUsuarioActual()?.uid ?: run { onResult(false); return }
        viewModelScope.launch { onResult(repositorio.guardarHijo(uid, child)) }
    }

    // ── Perfil Embarazo ───────────────────────────────────────────────────────
    fun guardarPerfilEmbarazo(perfil: PerfilEmbarazo, onResult: (Boolean) -> Unit = {}) {
        val uid = repositorio.obtenerUsuarioActual()?.uid ?: run { onResult(false); return }
        viewModelScope.launch { onResult(repositorio.guardarPerfilEmbarazo(uid, perfil)) }
    }

    suspend fun cargarPerfilEmbarazo(): PerfilEmbarazo? {
        val uid = repositorio.obtenerUsuarioActual()?.uid ?: return null
        return repositorio.cargarPerfilEmbarazo(uid)
    }

    // ── Recargar lista de hijos desde Firestore ───────────────────────────────
    fun recargarHijos() {
        val uid = repositorio.obtenerUsuarioActual()?.uid ?: return
        viewModelScope.launch {
            val hijosActualizados = repositorio.cargarHijos(uid)
            val estadoActual = _estado.value
            if (estadoActual is LoginUiState.Exito) {
                _estado.value = estadoActual.copy(hijos = hijosActualizados)
            }
        }
    }

    // ── Actualizar perfil del usuario ─────────────────────────────────────────
    fun actualizarPerfil(nombre: String, email: String, telefono: String) {
        viewModelScope.launch {
            val uid = repositorio.obtenerUsuarioActual()?.uid ?: return@launch
            try {
                FirebaseFirestore.getInstance()
                    .collection("usuarios")
                    .document(uid)
                    .update(
                        mapOf(
                            "nombre"   to nombre,
                            "telefono" to telefono
                        )
                    ).await()
                _sesion.value = _sesion.value.copy(nombre = nombre, telefono = telefono)
            } catch (e: Exception) {
                // silencioso
            }
        }
    }

    // ── Cerrar sesión ─────────────────────────────────────────────────────────
    fun cerrarSesion() {
        viewModelScope.launch {
            repositorio.cerrarSesion()
            _estado.value = LoginUiState.Idle
            _sesion.value = UsuarioSesion()
        }
    }

    fun resetEstado() { _estado.value = LoginUiState.Idle }

    // ── Helper privado ────────────────────────────────────────────────────────
    private suspend fun cargarDatosSesion(uid: String, rol: String, fallbackEmail: String) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .await()
            val intentosEnDb = doc.getLong("intentosIaDisponibles")?.toInt() ?: 3
            val ultimoReset = doc.getLong("ultimoResetIa") ?: 0L
            val currentTime = com.example.nutriia.platform.currentTimeMillis()
            
            var intentosFinales = intentosEnDb
            var resetFinal = ultimoReset
            val msEnUnDia = 24L * 60L * 60L * 1000L

            val tz = kotlinx.datetime.TimeZone.currentSystemDefault()
            val now = kotlinx.datetime.Clock.System.now()
            val today = now.toLocalDateTime(tz).date
            
            val lastResetDate = if (ultimoReset > 0L) {
                kotlinx.datetime.Instant.fromEpochMilliseconds(ultimoReset).toLocalDateTime(tz).date
            } else null

            if (lastResetDate == null || lastResetDate != today) {
                intentosFinales = 3
                resetFinal = now.toEpochMilliseconds()
                repositorio.resetearIntentosDiarios(uid, resetFinal)
            }

            _sesion.value = UsuarioSesion(
                uid      = uid,
                nombre   = doc.getString("nombre")   ?: "",
                email    = doc.getString("email")    ?: fallbackEmail,
                telefono = doc.getString("telefono") ?: "",
                rol      = doc.getString("rol")      ?: rol,
                intentosIaDisponibles = intentosFinales,
                suscripcionIaVigenteHasta = doc.getLong("suscripcionIaVigenteHasta") ?: 0L,
                ultimoResetIa = resetFinal
            )
        } catch (e: Exception) {
            _sesion.value = _sesion.value.copy(uid = uid, rol = rol, email = fallbackEmail)
        }
    }

    fun hayHuellaDisponible(context: Any? = null): Boolean {
        return BiometricHelper.isAvailable(context)
    }

    fun loginConHuella(
        activity: Any? = null,
        onExito: (uid: String) -> Unit,
        onFail: () -> Unit
    ) {
        BiometricHelper.prompt(
            activity = activity,
            onSuccess = {
                val uidLocal = SessionManager.obtenerUltimoUid()
                    ?: SessionManager.obtenerUid()
                    ?: repositorio.obtenerUsuarioActual()?.uid

                if (uidLocal != null) {
                    viewModelScope.launch {
                        _estado.value = LoginUiState.Loading
                        val rol = repositorio.obtenerRol(uidLocal)
                        val hijos = if (rol == "padre") repositorio.cargarHijos(uidLocal) else emptyList()
                        SessionManager.guardarSesion(uidLocal)
                        cargarDatosSesion(uidLocal, rol, "")
                        _estado.value = LoginUiState.Exito(rol = rol, hijos = hijos)
                        onExito(uidLocal)
                    }
                } else {
                    _estado.value = LoginUiState.Error(
                        "Primero inicia sesión con tu correo y contraseña para activar el acceso con huella."
                    )
                    onFail()
                }
            },
            onFail = { onFail() }
        )
    }

    fun olvidarDispositivo(context: Any? = null) {
        SessionManager.olvidarBiometriaCompleta()
        repositorio.cerrarSesionBiometrica()
    }
}
