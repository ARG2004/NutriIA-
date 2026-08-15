package com.example.nutriia.analisisIA

import com.google.firebase.Timestamp

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS — Módulo de Análisis de Alimentos con IA
// ═══════════════════════════════════════════════════════════════════════════

// ─── 1. Resultado de detección de alimento (OpenRouter Vision) ───────────────
data class FoodDetectionResult(
    val foodName    : String  = "",
    val ingredients : List<String> = emptyList(),
    val foodType    : String  = "",
    val confidence  : Double  = 0.0
)

// ─── 2. Información nutricional (Spoonacular) ────────────────────────────────
data class NutritionInfo(
    val calories      : Double = 0.0,
    val protein       : Double = 0.0,
    val carbohydrates : Double = 0.0,
    val fat           : Double = 0.0,
    val sugar         : Double = 0.0,
    val fiber         : Double = 0.0,
    val sodium        : Double = 0.0
)

// ─── 3. Análisis pediátrico (OpenRouter LLM) ─────────────────────────────────
data class PediatricAnalysis(
    val recommended       : Boolean       = false,
    val recommendedPortion: String        = "",
    val benefits          : List<String>  = emptyList(),
    val warnings          : List<String>  = emptyList(),
    val frequency         : String        = ""
)

// ─── 4. Resultado completo guardado en Firebase ──────────────────────────────
data class AnalisisCompleto(
    val id            : String           = "",
    val childId       : String           = "",
    val fecha         : String           = "",
    val imagePath     : String           = "",
    val foodDetection : FoodDetectionResult = FoodDetectionResult(),
    val nutrition     : NutritionInfo       = NutritionInfo(),
    val analysis      : PediatricAnalysis   = PediatricAnalysis(),
    val creadoEn      : Timestamp?          = null
)

// ─── 5. UI State — flujo de estados de la pantalla ───────────────────────────
sealed class AnalisisUiState {
    /** Pantalla inicial: lista vacía, esperando acción del usuario */
    object Idle : AnalisisUiState()

    /** Cámara abierta, esperando toma de foto */
    object Capturando : AnalisisUiState()

    /** Procesando — el mensaje muestra el paso actual al usuario */
    data class Analizando(val mensaje: String) : AnalisisUiState()

    /** Análisis completado con éxito */
    data class Exito(val resultado: AnalisisCompleto) : AnalisisUiState()

    /** Análisis guardado en Firebase */
    object Guardado : AnalisisUiState()

    /** Error recuperable — se muestra al usuario */
    data class Error(val mensaje: String) : AnalisisUiState()
}
