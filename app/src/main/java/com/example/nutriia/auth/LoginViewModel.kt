package com.example.nutriia.auth

import android.app.Application
import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ui.theme.ChildProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginUiState {
    object Idle    : LoginUiState()
    object Loading : LoginUiState()
    data class Exito(val rol: String, val hijos: List<ChildProfile> = emptyList()) : LoginUiState()
    data class Error(val mensaje: String) : LoginUiState()
}

data class UsuarioSesion(
    val uid:      String = "",
    val nombre:   String = "",
    val email:    String = "",
    val telefono: String = "",
    val rol:      String = "padre",
    val intentosIaDisponibles: Int = 3,
    val suscripcionIaVigenteHasta: Long? = null,
    val ultimoResetIa: Long = 0L
)

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = RepositorioLogin(application)

    private val _estado = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val estado: StateFlow<LoginUiState> = _estado

    private val _sesion = MutableStateFlow(UsuarioSesion())
    val sesionState: StateFlow<UsuarioSesion> = _sesion

    val uidUsuario:      String get() = _sesion.value.uid
    val nombreUsuario:   String get() = _sesion.value.nombre
    val emailUsuario:    String get() = _sesion.value.email
    val telefonoUsuario: String get() = _sesion.value.telefono
    val rolUsuario:      String get() = _sesion.value.rol
    val intentosIaDisponibles: Int get() = _sesion.value.intentosIaDisponibles
    val suscripcionIaVigenteHasta: Long? get() = _sesion.value.suscripcionIaVigenteHasta

    fun decrementarIntentoIaLocal() {
        val uid = uidUsuario
        if (uid.isBlank()) return
        
        var actual = _sesion.value.intentosIaDisponibles
        val subHasta = _sesion.value.suscripcionIaVigenteHasta ?: 0L
        val isExpired = subHasta > 0 && System.currentTimeMillis() > subHasta

        if (isExpired && actual >= 9999) {
            actual = 3 // Expiró pero no se ha reseteado localmente
            viewModelScope.launch {
                repositorio.resetearIntentosDiarios(uid, System.currentTimeMillis())
            }
        }
        
        if (actual > 0) {
            _sesion.value = _sesion.value.copy(intentosIaDisponibles = actual - 1)
            viewModelScope.launch {
                repositorio.decrementarIntentoIa(uid, actual - 1)
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
            if (SessionManager.obtenerUid(getApplication()) == null) {
                onResultado(null, emptyList())
                return@launch
            }
            val sesion = repositorio.verificarSesionActiva()
            if (sesion == null) {
                onResultado(null, emptyList())
                return@launch
            }
            val exito = sesion as? ResultadoAuth.Exito ?: run {
                onResultado(null, emptyList())
                return@launch
            }
            val hijos = if (exito.rol == "padre")
                repositorio.cargarHijos(exito.uid)
            else emptyList()
            val emailFirebase = repositorio.obtenerUsuarioActual()?.email ?: ""
            cargarDatosSesion(exito.uid, exito.rol, emailFirebase)
            onResultado(exito.rol, hijos)
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

    fun recargarSesion() {
        viewModelScope.launch {
            val uid = _sesion.value.uid
            if (uid.isNotEmpty()) {
                cargarDatosSesion(uid, _sesion.value.rol, _sesion.value.email)
            }
        }
    }

    // ── Helper privado ────────────────────────────────────────────────────────
    private suspend fun cargarDatosSesion(uid: String, rol: String, fallbackEmail: String) {
        try {
            val doc = FirebaseFirestore.getInstance()
                .collection("usuarios")
                .document(uid)
                .get()
                .await()
            
            var intentosIa = doc.getLong("intentosIaDisponibles")?.toInt() ?: 3
            val ultimoReset = doc.getTimestamp("ultimoResetIa")?.toDate()?.time ?: doc.getLong("ultimoResetIa") ?: 0L
            val suscripcionIaVigenteHasta = doc.getTimestamp("suscripcionIaVigenteHasta")?.toDate()?.time ?: doc.getLong("suscripcionIaVigenteHasta")
            
            val hoyEnDias = (System.currentTimeMillis() + java.util.TimeZone.getDefault().rawOffset) / (1000 * 60 * 60 * 24)
            val ultimoResetEnDias = (ultimoReset + java.util.TimeZone.getDefault().rawOffset) / (1000 * 60 * 60 * 24)
            
            var resetNeeded = false
            
            val currentTime = System.currentTimeMillis()
            if (intentosIa >= 9999 && suscripcionIaVigenteHasta != null && suscripcionIaVigenteHasta > 0 && currentTime > suscripcionIaVigenteHasta) {
                intentosIa = 3
                resetNeeded = true
            }

            if (hoyEnDias > ultimoResetEnDias) {
                if (intentosIa < 9999) intentosIa = 3
                resetNeeded = true
            }

            _sesion.value = UsuarioSesion(
                uid      = uid,
                nombre   = doc.getString("nombre")   ?: "",
                email    = doc.getString("email")    ?: fallbackEmail,
                telefono = doc.getString("telefono") ?: "",
                rol      = doc.getString("rol")      ?: rol,
                intentosIaDisponibles = intentosIa,
                suscripcionIaVigenteHasta = suscripcionIaVigenteHasta,
                ultimoResetIa = if (resetNeeded) System.currentTimeMillis() else ultimoReset
            )
            
            if (resetNeeded) {
                repositorio.resetearIntentosDiarios(uid, System.currentTimeMillis())
            }
        } catch (e: Exception) {
            _sesion.value = _sesion.value.copy(uid = uid, rol = rol, email = fallbackEmail)
        }
    }

    fun hayHuellaDisponible(context: Context): Boolean {
        return BiometricHelper.isAvailable(context)
    }

    fun loginConHuella(
        activity: FragmentActivity,
        onExito: (uid: String) -> Unit,
        onFail: () -> Unit
    ) {
        BiometricHelper.prompt(
            activity = activity,
            onSuccess = {
                val uidLocal = SessionManager.obtenerUid(activity)
                if (uidLocal != null) {
                    viewModelScope.launch {
                        val rol = repositorio.obtenerRol(uidLocal)
                        val hijos = if (rol == "padre") repositorio.cargarHijos(uidLocal) else emptyList()
                        cargarDatosSesion(uidLocal, rol, "")
                        _estado.value = LoginUiState.Exito(rol = rol, hijos = hijos)
                        onExito(uidLocal)
                    }
                } else {
                    val usuarioFirebase = repositorio.obtenerUsuarioActual()
                    if (usuarioFirebase != null) {
                        viewModelScope.launch {
                            val rol = repositorio.obtenerRol(usuarioFirebase.uid)
                            val hijos = if (rol == "padre") repositorio.cargarHijos(usuarioFirebase.uid) else emptyList()
                            SessionManager.guardarSesion(activity, usuarioFirebase.uid)
                            cargarDatosSesion(usuarioFirebase.uid, rol, usuarioFirebase.email ?: "")
                            _estado.value = LoginUiState.Exito(rol = rol, hijos = hijos)
                            onExito(usuarioFirebase.uid)
                        }
                    } else {
                        _estado.value = LoginUiState.Error(
                            "Primero inicia sesión con tu correo y contraseña para activar el acceso con huella."
                        )
                        onFail()
                    }
                }
            },
            onFail = { }
        )
    }

    fun olvidarDispositivo(context: Context) {
        repositorio.cerrarSesionBiometrica(context)
    }
}
