package com.example.nutriia.configuracion
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.auth.RepositorioLogin
import com.example.nutriia.ui.theme.ChildProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO DE NOTIFICACIONES
// ═══════════════════════════════════════════════════════════════════════════════

data class EstadoNotificaciones(
    val recordatoriosComidas:    Boolean = true,
    val alertasCrecimiento:      Boolean = true,
    val recomendacionesIA:       Boolean = false
)

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO DE CONFIGURACIÓN
// ═══════════════════════════════════════════════════════════════════════════════

data class EstadoConfiguracion(
    val cargando:       Boolean              = false,
    val errorMensaje:   String?              = null,
    val exito:          String?              = null,
    val notificaciones: EstadoNotificaciones = EstadoNotificaciones()
)

// ═══════════════════════════════════════════════════════════════════════════════
// VIEW MODEL
// ═══════════════════════════════════════════════════════════════════════════════

class ConfiguracionViewModel(
    private val repositorio: RepositorioLogin,
    private val prefs:       SharedPreferences
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoConfiguracion())
    val estado: StateFlow<EstadoConfiguracion> = _estado.asStateFlow()

    // ── Preferencias clave ────────────────────────────────────────────────────
    companion object {
        private const val KEY_NOTIF_COMIDAS       = "notif_comidas"
        private const val KEY_NOTIF_CRECIMIENTO   = "notif_crecimiento"
        private const val KEY_NOTIF_RECOMENDACIONES = "notif_recomendaciones"
    }

    // ── Cargar preferencias locales ───────────────────────────────────────────

    init {
        cargarPreferenciasLocales()
    }

    private fun cargarPreferenciasLocales() {
        val notif = EstadoNotificaciones(
            recordatoriosComidas    = prefs.getBoolean(KEY_NOTIF_COMIDAS,         true),
            alertasCrecimiento      = prefs.getBoolean(KEY_NOTIF_CRECIMIENTO,     true),
            recomendacionesIA       = prefs.getBoolean(KEY_NOTIF_RECOMENDACIONES, false)
        )
        _estado.value = _estado.value.copy(notificaciones = notif)
    }

    // ── Actualizar notificación individual ────────────────────────────────────

    fun setNotifComidas(value: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_COMIDAS, value).apply()
        _estado.value = _estado.value.copy(
            notificaciones = _estado.value.notificaciones.copy(recordatoriosComidas = value)
        )
    }

    fun setNotifCrecimiento(value: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_CRECIMIENTO, value).apply()
        _estado.value = _estado.value.copy(
            notificaciones = _estado.value.notificaciones.copy(alertasCrecimiento = value)
        )
    }

    fun setNotifRecomendaciones(value: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIF_RECOMENDACIONES, value).apply()
        _estado.value = _estado.value.copy(
            notificaciones = _estado.value.notificaciones.copy(recomendacionesIA = value)
        )
    }

    // ── Cambiar contraseña (envío de reset por email) ─────────────────────────

    fun enviarRecuperacionPassword(email: String) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, errorMensaje = null, exito = null)
            val ok = repositorio.recuperarContrasena(email)
            _estado.value = _estado.value.copy(
                cargando     = false,
                exito        = if (ok) "Correo de recuperación enviado a $email" else null,
                errorMensaje = if (!ok) "No se pudo enviar el correo. Verifica tu dirección." else null
            )
        }
    }

    // ── Cambiar contraseña directamente ─────────────────────────────────────
    fun cambiarContrasenaDirecta(
        contrasenaActual: String,
        nuevaContrasena: String,
        onResultado: (exito: Boolean, mensaje: String) -> Unit
    ) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, errorMensaje = null, exito = null)
            val res = repositorio.actualizarContrasena(contrasenaActual, nuevaContrasena)
            when (res) {
                is com.example.nutriia.auth.ResultadoAuth.Exito -> {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        exito = "Contraseña actualizada exitosamente"
                    )
                    onResultado(true, "Contraseña actualizada exitosamente")
                }
                is com.example.nutriia.auth.ResultadoAuth.Error -> {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        errorMensaje = res.mensaje
                    )
                    onResultado(false, res.mensaje)
                }
            }
        }
    }

    // ── Eliminar cuenta definitivamente ──────────────────────────────────────
    fun eliminarCuentaDefinitiva(
        contrasenaActual: String,
        onDone: (exito: Boolean, mensaje: String) -> Unit
    ) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true, errorMensaje = null, exito = null)
            val res = repositorio.eliminarCuenta(contrasenaActual)
            when (res) {
                is com.example.nutriia.auth.ResultadoAuth.Exito -> {
                    _estado.value = _estado.value.copy(cargando = false)
                    onDone(true, "Cuenta eliminada correctamente")
                }
                is com.example.nutriia.auth.ResultadoAuth.Error -> {
                    _estado.value = _estado.value.copy(
                        cargando = false,
                        errorMensaje = res.mensaje
                    )
                    onDone(false, res.mensaje)
                }
            }
        }
    }

    // ── Cerrar sesión ─────────────────────────────────────────────────────────

    fun cerrarSesion(onDone: () -> Unit) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)
            repositorio.cerrarSesion()
            _estado.value = _estado.value.copy(cargando = false)
            onDone()
        }
    }

    // ── Limpiar mensajes ──────────────────────────────────────────────────────

    fun limpiarMensajes() {
        _estado.value = _estado.value.copy(errorMensaje = null, exito = null)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FACTORY — instancia el ViewModel con las dependencias correctas
// ═══════════════════════════════════════════════════════════════════════════════

class ConfiguracionViewModelFactory(private val context: Context) :
    androidx.lifecycle.ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repo  = RepositorioLogin(context)
        val prefs = context.getSharedPreferences("nutriia_config", Context.MODE_PRIVATE)
        return ConfiguracionViewModel(repo, prefs) as T
    }
}