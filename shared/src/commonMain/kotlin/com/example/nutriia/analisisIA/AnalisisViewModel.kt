package com.example.nutriia.analisisIA

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.utils.FechaUtils
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
        isEmbarazo: Boolean = false,
        base64Image: String = ""
    ) {
        analisisJob = viewModelScope.launch {
            try {
                // Paso 1: Detección visual
                _uiState.value = AnalisisUiState.Analizando("🔍 Detectando el alimento con Inteligencia Artificial...")
                val detectionResult = repo.detectarAlimento(base64Image)
                if (detectionResult.isFailure) {
                    _uiState.value = AnalisisUiState.Error(
                        detectionResult.exceptionOrNull()?.message ?: "No se pudo identificar el alimento"
                    )
                    return@launch
                }
                val foodDetection = detectionResult.getOrThrow()

                // Paso 2: Información nutricional
                _uiState.value = AnalisisUiState.Analizando("🥗 Obteniendo balance nutricional...")
                val foodHash = repo.hashAlimento(foodDetection.foodName)
                val cached = repo.buscarEnCache(foodHash)

                val nutrition: NutritionInfo
                val analysis: PediatricAnalysis

                if (cached != null) {
                    _uiState.value = AnalisisUiState.Analizando("⚡ Recuperando análisis optimizado...")
                    nutrition = cached.first
                    analysis = cached.second
                } else {
                    val nutritionRes = repo.obtenerNutricion(foodDetection.foodName)
                    nutrition = nutritionRes.getOrDefault(NutritionInfo())

                    val targetNombre = if (isEmbarazo || perfilEmbarazo != null) "tu embarazo" else (child?.name ?: "tu bebé")
                    _uiState.value = AnalisisUiState.Analizando("🤖 Generando recomendaciones médicas para $targetNombre...")

                    val analysisRes = if (isEmbarazo || perfilEmbarazo != null) {
                        repo.analizarParaEmbarazo(perfilEmbarazo, foodDetection, nutrition)
                    } else if (child != null) {
                        repo.analizarParaNino(child, foodDetection, nutrition)
                    } else {
                        repo.analizarParaEmbarazo(perfilEmbarazo, foodDetection, nutrition)
                    }

                    if (analysisRes.isFailure) {
                        _uiState.value = AnalisisUiState.Error(
                            analysisRes.exceptionOrNull()?.message ?: "Error en análisis nutricional"
                        )
                        return@launch
                    }
                    analysis = analysisRes.getOrThrow()
                    repo.guardarEnCache(foodHash, nutrition, analysis)
                }

                // Paso 3: Consolidación de resultado
                val targetId = if (isEmbarazo || perfilEmbarazo != null) "embarazo" else (child?.id ?: "embarazo")
                val completo = AnalisisCompleto(
                    id = generateUUID(),
                    childId = targetId,
                    fecha = FechaUtils.hoyIso(),
                    imagePath = imagePath,
                    foodDetection = foodDetection,
                    nutrition = nutrition,
                    analysis = analysis
                )
                resultadoActual = completo
                _uiState.value = AnalisisUiState.Exito(completo)

            } catch (e: Exception) {
                _uiState.value = AnalisisUiState.Error("Error procesando imagen: ${e.message}")
            }
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