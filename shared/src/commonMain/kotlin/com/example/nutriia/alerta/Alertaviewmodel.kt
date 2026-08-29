package com.example.nutriia.alerta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AlertaUiState {
    object Idle    : AlertaUiState()
    object Saved   : AlertaUiState()
    object Deleted : AlertaUiState()
    data class Error(val msg: String) : AlertaUiState()
}

class AlertaViewModel : ViewModel() {

    private val repo = AlertaRepository()
    private var observerJob: kotlinx.coroutines.Job? = null

    private val _alertas   = MutableStateFlow<List<Alerta>>(emptyList())
    val alertas: StateFlow<List<Alerta>> = _alertas.asStateFlow()

    private val _uiState   = MutableStateFlow<AlertaUiState>(AlertaUiState.Idle)
    val uiState: StateFlow<AlertaUiState> = _uiState.asStateFlow()

    private val _cargando  = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    fun init(childId: String?, uid: String? = null) {
        observerJob?.cancel()
        observerJob = viewModelScope.launch {
            repo.observarPorHijo(childId, uid)
                .catch { e -> _uiState.value = AlertaUiState.Error(e.message ?: "Error") }
                .collect { lista -> _alertas.value = lista }
        }
    }

    fun guardar(alerta: Alerta) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                val res = repo.guardar(alerta)
                if (res.isSuccess) {
                    AlertaScheduler.programar(alerta = alerta)
                    _uiState.value = AlertaUiState.Saved
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Error al guardar"
                    _uiState.value = AlertaUiState.Error(err)
                }
            } catch (e: Exception) {
                _uiState.value = AlertaUiState.Error(e.message ?: "Error al guardar")
            } finally {
                _cargando.value = false
            }
        }
    }

    fun eliminar(alerta: Alerta) {
        viewModelScope.launch {
            try {
                val res = repo.eliminar(alerta.childId, alerta.id)
                if (res.isSuccess) {
                    AlertaScheduler.cancelar(alertaId = alerta.id)
                    _uiState.value = AlertaUiState.Deleted
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Error al eliminar"
                    _uiState.value = AlertaUiState.Error(err)
                }
            } catch (e: Exception) {
                _uiState.value = AlertaUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    fun toggleActiva(alerta: Alerta) {
        viewModelScope.launch {
            try {
                val nueva = alerta.copy(activa = !alerta.activa)
                val res = repo.toggleActiva(alerta.childId, alerta.id, nueva.activa)
                if (res.isSuccess) {
                    AlertaScheduler.programar(alerta = nueva)
                } else {
                    val err = res.exceptionOrNull()?.message ?: "Error"
                    _uiState.value = AlertaUiState.Error(err)
                }
            } catch (e: Exception) {
                _uiState.value = AlertaUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun resetState() { _uiState.value = AlertaUiState.Idle }
}