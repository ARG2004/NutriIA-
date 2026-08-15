package com.example.nutriia.alerta

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AlertaUiState {
    object Idle    : AlertaUiState()
    object Saved   : AlertaUiState()
    object Deleted : AlertaUiState()
    data class Error(val msg: String) : AlertaUiState()
}

class AlertaViewModel(app: Application) : AndroidViewModel(app) {

    private val repo    = AlertaRepository()
    private val context: Context get() = getApplication()

    private val _alertas   = MutableStateFlow<List<Alerta>>(emptyList())
    val alertas: StateFlow<List<Alerta>> = _alertas.asStateFlow()

    private val _uiState   = MutableStateFlow<AlertaUiState>(AlertaUiState.Idle)
    val uiState: StateFlow<AlertaUiState> = _uiState.asStateFlow()

    private val _cargando  = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    // ── Inicializar con el hijo activo ────────────────────────────────────────
    fun init(childId: String?) {
        viewModelScope.launch {
            repo.observarPorHijo(childId)
                .catch { e -> _uiState.value = AlertaUiState.Error(e.message ?: "Error") }
                .collect { lista -> _alertas.value = lista }
        }
    }

    // ── Guardar nueva alerta ──────────────────────────────────────────────────
    fun guardar(alerta: Alerta) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                repo.guardar(alerta)
                AlertaScheduler.programar(context, alerta)
                _uiState.value = AlertaUiState.Saved
            } catch (e: Exception) {
                _uiState.value = AlertaUiState.Error(e.message ?: "Error al guardar")
            } finally {
                _cargando.value = false
            }
        }
    }

    // ── Eliminar alerta ───────────────────────────────────────────────────────
    fun eliminar(alerta: Alerta) {
        viewModelScope.launch {
            try {
                repo.eliminar(alerta.childId, alerta.id)
                AlertaScheduler.cancelar(context, alerta.id)
                _uiState.value = AlertaUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = AlertaUiState.Error(e.message ?: "Error al eliminar")
            }
        }
    }

    // ── Toggle activa/inactiva ────────────────────────────────────────────────
    fun toggleActiva(alerta: Alerta) {
        viewModelScope.launch {
            try {
                val nueva = alerta.copy(activa = !alerta.activa)
                repo.toggleActiva(alerta.childId, alerta.id, nueva.activa)
                AlertaScheduler.programar(context, nueva)
            } catch (e: Exception) {
                _uiState.value = AlertaUiState.Error(e.message ?: "Error")
            }
        }
    }

    fun resetState() { _uiState.value = AlertaUiState.Idle }
}