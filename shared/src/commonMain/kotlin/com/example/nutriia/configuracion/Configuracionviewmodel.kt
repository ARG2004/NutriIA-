package com.example.nutriia.configuracion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.auth.RepositorioLogin
import com.example.nutriia.ui.theme.ChildProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EstadoNotificaciones(
    val recordatoriosComidas:    Boolean = true,
    val alertasCrecimiento:      Boolean = true,
    val recomendacionesIA:       Boolean = false
)

data class EstadoConfiguracion(
    val cargando:       Boolean              = false,
    val errorMensaje:   String?              = null,
    val exito:          String?              = null,
    val notificaciones: EstadoNotificaciones = EstadoNotificaciones()
)

class ConfiguracionViewModel(
    private val repositorio: RepositorioLogin = RepositorioLogin()
) : ViewModel() {

    private val _estado = MutableStateFlow(EstadoConfiguracion())
    val estado: StateFlow<EstadoConfiguracion> = _estado.asStateFlow()

    fun setNotifComidas(value: Boolean) {
        _estado.value = _estado.value.copy(
            notificaciones = _estado.value.notificaciones.copy(recordatoriosComidas = value)
        )
    }

    fun setNotifCrecimiento(value: Boolean) {
        _estado.value = _estado.value.copy(
            notificaciones = _estado.value.notificaciones.copy(alertasCrecimiento = value)
        )
    }

    fun setNotifRecomendaciones(value: Boolean) {
        _estado.value = _estado.value.copy(
            notificaciones = _estado.value.notificaciones.copy(recomendacionesIA = value)
        )
    }

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

    fun cerrarSesion(onDone: () -> Unit) {
        viewModelScope.launch {
            _estado.value = _estado.value.copy(cargando = true)
            repositorio.cerrarSesion()
            _estado.value = _estado.value.copy(cargando = false)
            onDone()
        }
    }

    fun limpiarMensajes() {
        _estado.value = _estado.value.copy(errorMensaje = null, exito = null)
    }
}