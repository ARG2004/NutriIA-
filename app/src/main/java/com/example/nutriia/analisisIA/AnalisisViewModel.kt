package com.example.nutriia.analisisIA

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.utils.FechaUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.UUID

private const val TIMEOUT_MS = 30_000L   // 30 s máximo por llamada a API

class AnalisisViewModel : ViewModel() {

    private val repo = AnalisisRepository()

    private val _uiState = MutableStateFlow<AnalisisUiState>(AnalisisUiState.Idle)
    val uiState: StateFlow<AnalisisUiState> = _uiState

    private var imageCapture: ImageCapture? = null
    private var analisisJob: Job? = null
    private var resultadoActual: AnalisisCompleto? = null

    // ─── Resetear / cancelar ────────────────────────────────────────────────
    fun resetear() {
        analisisJob?.cancel()
        analisisJob = null
        _uiState.value = AnalisisUiState.Idle
        resultadoActual = null
    }

    fun abrirCamara()   { _uiState.value = AnalisisUiState.Capturando }
    fun cancelarCamara(){ _uiState.value = AnalisisUiState.Idle }

    /** Cancela el análisis en curso y regresa a Idle sin mostrar error */
    fun cancelarAnalisis() {
        analisisJob?.cancel()
        analisisJob = null
        _uiState.value = AnalisisUiState.Idle
    }

    // ─── Flujo principal de análisis ────────────────────────────────────────
    fun analizarFoto(
        imageFile: File,
        child: ChildProfile? = null,
        perfilEmbarazo: PerfilEmbarazo? = null,
        isEmbarazo: Boolean = false
    ) {
        analisisJob = viewModelScope.launch {
            try {
                // Paso 1: Detección
                _uiState.value = AnalisisUiState.Analizando("🔍 Detectando el alimento...")
                val detectionResult = withContext(Dispatchers.IO) {
                    withTimeout(TIMEOUT_MS) { repo.detectarAlimento(imageFile) }
                }
                if (detectionResult.isFailure) {
                    _uiState.value = AnalisisUiState.Error(
                        detectionResult.exceptionOrNull()?.message ?: "No se pudo detectar el alimento"
                    )
                    return@launch
                }
                val foodDetection = detectionResult.getOrThrow()

                // Paso 2: Nutrición (caché o Spoonacular/OpenFoodFacts/LLM)
                _uiState.value = AnalisisUiState.Analizando("🥗 Obteniendo información nutricional...")
                val foodHash     = repo.hashAlimento(foodDetection.foodName)
                val cachedResult = withContext(Dispatchers.IO) {
                    withTimeout(TIMEOUT_MS) { repo.buscarEnCache(foodHash) }
                }

                val nutrition: NutritionInfo
                val analysis: PediatricAnalysis

                if (cachedResult != null) {
                    _uiState.value = AnalisisUiState.Analizando("⚡ Usando análisis guardado previamente...")
                    nutrition = cachedResult.first
                    analysis  = cachedResult.second
                } else {
                    val nutritionResult = withContext(Dispatchers.IO) {
                        withTimeout(TIMEOUT_MS) { repo.obtenerNutricion(foodDetection.foodName) }
                    }
                    nutrition = nutritionResult.getOrDefault(NutritionInfo())

                    val targetNombre = if (isEmbarazo || perfilEmbarazo != null) "tu embarazo" else (child?.name ?: "tu bebé")
                    _uiState.value = AnalisisUiState.Analizando("🤖 Analizando para $targetNombre...")
                    
                    val analysisResult = withContext(Dispatchers.IO) {
                        withTimeout(TIMEOUT_MS) {
                            if (isEmbarazo || perfilEmbarazo != null) {
                                repo.analizarParaEmbarazo(perfilEmbarazo, foodDetection, nutrition)
                            } else if (child != null) {
                                repo.analizarParaNino(child, foodDetection, nutrition)
                            } else {
                                repo.analizarParaEmbarazo(perfilEmbarazo, foodDetection, nutrition)
                            }
                        }
                    }
                    if (analysisResult.isFailure) {
                        _uiState.value = AnalisisUiState.Error(
                            analysisResult.exceptionOrNull()?.message ?: "Error en análisis nutricional"
                        )
                        return@launch
                    }
                    analysis = analysisResult.getOrThrow()

                    withContext(Dispatchers.IO) {
                        withTimeout(TIMEOUT_MS) { repo.guardarEnCache(foodHash, nutrition, analysis) }
                    }
                }

                // Paso 3: Resultado
                val targetId = if (isEmbarazo || perfilEmbarazo != null) "embarazo" else (child?.id ?: "embarazo")
                val analisisCompleto = AnalisisCompleto(
                    id            = UUID.randomUUID().toString(),
                    childId       = targetId,
                    fecha         = FechaUtils.fechaActual(),
                    imagePath     = imageFile.absolutePath,
                    foodDetection = foodDetection,
                    nutrition     = nutrition,
                    analysis      = analysis
                )
                resultadoActual = analisisCompleto
                _uiState.value  = AnalisisUiState.Exito(analisisCompleto)

            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AnalisisUiState.Error(
                    "Tiempo de espera agotado. Revisa tu conexión a internet e intenta de nuevo."
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // Cancelado por el usuario
            } catch (e: Exception) {
                _uiState.value = AnalisisUiState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    // ─── Guardar resultado en Firebase ──────────────────────────────────────
    fun guardarEnHistorial(childId: String) {
        val resultado = resultadoActual ?: return
        viewModelScope.launch {
            try {
                val r = withContext(Dispatchers.IO) {
                    withTimeout(TIMEOUT_MS) { repo.guardarAnalisis(childId, resultado) }
                }
                if (r.isSuccess) _uiState.value = AnalisisUiState.Guardado
                else _uiState.value = AnalisisUiState.Error(
                    r.exceptionOrNull()?.message ?: "Error al guardar"
                )
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                _uiState.value = AnalisisUiState.Error(
                    "No se pudo guardar. Revisa tu conexión e intenta de nuevo."
                )
            } catch (e: Exception) {
                _uiState.value = AnalisisUiState.Error("Error guardando: ${e.message}")
            }
        }
    }

    // ─── Configurar CameraX ─────────────────────────────────────────────────
    fun configurarCamara(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        previewSurfaceProvider: Preview.SurfaceProvider
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewSurfaceProvider)
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                _uiState.value = AnalisisUiState.Error("Error iniciando cámara: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // ─── Tomar foto y lanzar análisis ───────────────────────────────────────
    fun tomarFotoYAnalizar(
        context: Context,
        child: ChildProfile? = null,
        perfilEmbarazo: PerfilEmbarazo? = null,
        isEmbarazo: Boolean = false
    ) {
        val capture = imageCapture
        if (capture == null) {
            _uiState.value = AnalisisUiState.Error("Cámara no lista. Intenta de nuevo.")
            return
        }
        _uiState.value = AnalisisUiState.Analizando("📷 Capturando imagen...")
        val outputFile    = File(context.cacheDir, "nutria_foto_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    analizarFoto(outputFile, child, perfilEmbarazo, isEmbarazo)
                }
                override fun onError(exception: ImageCaptureException) {
                    _uiState.value =
                        AnalisisUiState.Error("Error al tomar foto: ${exception.message}")
                }
            }
        )
    }
}