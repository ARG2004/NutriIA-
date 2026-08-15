package com.example.nutriia.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class RegisterUiState {
    object Idle    : RegisterUiState()
    object Loading : RegisterUiState()
    data class Exito(val rol: String)     : RegisterUiState()
    data class Error(val mensaje: String) : RegisterUiState()
}

class RegisterViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = RepositorioLogin(application)

    private val _estado = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val estado: StateFlow<RegisterUiState> = _estado

    fun registrarPadre(data: ParentRegisterData, confirmarPassword: String) {
        if (data.password != confirmarPassword) {
            _estado.value = RegisterUiState.Error("Las contraseñas no coinciden")
            return
        }
        viewModelScope.launch {
            _estado.value = RegisterUiState.Loading
            val resultado = repositorio.registrarPadre(
                email            = data.email,
                contrasena       = data.password,
                nombre           = data.name,
                telefono         = data.phone,
                codigoNutriologo = data.nutritionistCode,
                nombreHijo       = data.childName
            )
            _estado.value = when (resultado) {
                is ResultadoAuth.Exito -> RegisterUiState.Exito(resultado.rol)
                is ResultadoAuth.Error -> RegisterUiState.Error(resultado.mensaje)
            }
        }
    }

    fun registrarMamaPrimeriza(data: MamaPrimerizaRegisterData, confirmarPassword: String) {
        if (data.password != confirmarPassword) {
            _estado.value = RegisterUiState.Error("Las contraseñas no coinciden")
            return
        }
        viewModelScope.launch {
            _estado.value = RegisterUiState.Loading
            val resultado = repositorio.registrarMamaPrimeriza(
                email      = data.email,
                contrasena = data.password,
                nombre     = data.name,
                telefono   = data.phone,
                semanas    = data.semanas
            )
            _estado.value = when (resultado) {
                is ResultadoAuth.Exito -> RegisterUiState.Exito(resultado.rol)
                is ResultadoAuth.Error -> RegisterUiState.Error(resultado.mensaje)
            }
        }
    }

    fun registrarNutriologo(
        data: NutritionistRegisterData,
        confirmarPassword: String,
        consentimientoCedula: Boolean = true,
        nombreTitularCedula: String = "",
        profesionCedula: String = ""
    ) {
        if (data.password != confirmarPassword) {
            _estado.value = RegisterUiState.Error("Las contraseñas no coinciden")
            return
        }
        viewModelScope.launch {
            _estado.value = RegisterUiState.Loading
            val resultado = repositorio.registrarNutriologo(
                email                = data.email,
                contrasena           = data.password,
                nombre               = data.name,
                telefono             = data.phone,
                especialidad         = data.specialty,
                cedula               = data.licenseId,
                consentimientoCedula = consentimientoCedula,
                nombreTitularCedula  = nombreTitularCedula,
                profesionCedula      = profesionCedula
            )
            _estado.value = when (resultado) {
                is ResultadoAuth.Exito -> RegisterUiState.Exito(resultado.rol)
                is ResultadoAuth.Error -> RegisterUiState.Error(resultado.mensaje)
            }
        }
    }

    fun registrarGinecologo(
        data: GynecologistRegisterData,
        confirmarPassword: String,
        consentimientoCedula: Boolean = true,
        nombreTitularCedula: String = "",
        profesionCedula: String = ""
    ) {
        if (data.password != confirmarPassword) {
            _estado.value = RegisterUiState.Error("Las contraseñas no coinciden")
            return
        }
        viewModelScope.launch {
            _estado.value = RegisterUiState.Loading
            val resultado = repositorio.registrarGinecologo(
                email                = data.email,
                contrasena           = data.password,
                nombre               = data.name,
                telefono             = data.phone,
                especialidad         = data.specialty,
                cedula               = data.licenseId,
                consentimientoCedula = consentimientoCedula,
                nombreTitularCedula  = nombreTitularCedula,
                profesionCedula      = profesionCedula
            )
            _estado.value = when (resultado) {
                is ResultadoAuth.Exito -> RegisterUiState.Exito(resultado.rol)
                is ResultadoAuth.Error -> RegisterUiState.Error(resultado.mensaje)
            }
        }
    }

    fun resetEstado() { _estado.value = RegisterUiState.Idle }
}
