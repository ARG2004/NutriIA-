package com.example.nutriia.analisisIA

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ui.theme.ChildProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AnalisisViewModel : ViewModel() {

    private val repo = AnalisisRepository()

    private val _uiState = MutableStateFlow<AnalisisUiState>(AnalisisUiState.Idle)
    val uiState: StateFlow<AnalisisUiState> = _uiState

    private var analisisJob: Job? = null
    private var resultadoActual: AnalisisCompleto? = null

    fun resetear() {
        analisisJob?.cancel()
        analisisJob = null
        _uiState.value = AnalisisUiState.Idle
        resultadoActual = null
    }

    fun abrirCamara()   { _uiState.value = AnalisisUiState.Capturando }
    fun cancelarCamara(){ _uiState.value = AnalisisUiState.Idle }
    fun cancelarAnalisis() {
        analisisJob?.cancel()
        analisisJob = null
        _uiState.value = AnalisisUiState.Idle
    }

    fun analizarFoto(
        imagePath: String = "",
        child: ChildProfile? = null,
        perfilEmbarazo: PerfilEmbarazo? = null,
        isEmbarazo: Boolean = false
    ) {
        analisisJob = viewModelScope.launch {
            _uiState.value = AnalisisUiState.Analizando("Analizando alimento con Inteligencia Artificial...")
            val resultado = repo.analizarAlimentoDemo(imagePath, child?.name ?: "Bebé")
            resultadoActual = resultado
            _uiState.value = AnalisisUiState.Exito(resultado)
        }
    }

    fun guardarEnHistorial(childId: String) {
        val resultado = resultadoActual ?: return
        viewModelScope.launch {
            repo.guardarAnalisis(childId, resultado)
            _uiState.value = AnalisisUiState.Guardado
        }
    }
}