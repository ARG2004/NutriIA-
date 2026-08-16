package com.example.nutriia.analisisIA

import kotlinx.serialization.Serializable

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS — Módulo de Análisis de Alimentos con IA
// ═══════════════════════════════════════════════════════════════════════════

// ─── 1. Resultado de detección de alimento ──────────────────────────────────
@Serializable
data class FoodDetectionResult(
    val foodName    : String  = "",
    val ingredients : List<String> = emptyList(),
    val foodType    : String  = "",
    val confidence  : Double  = 0.0
)

// ─── 2. Información nutricional ─────────────────────────────────────────────
@Serializable
data class NutritionInfo(
    val calories      : Double = 0.0,
    val protein       : Double = 0.0,
    val carbohydrates : Double = 0.0,
    val fat           : Double = 0.0,
    val sugar         : Double = 0.0,
    val fiber         : Double = 0.0,
    val sodium        : Double = 0.0
)

// ─── 3. Análisis pediátrico / gestacional ───────────────────────────────────
@Serializable
data class PediatricAnalysis(
    val recommended       : Boolean       = false,
    val recommendedPortion: String        = "",
    val benefits          : List<String>  = emptyList(),
    val warnings          : List<String>  = emptyList(),
    val frequency         : String        = ""
)

// ─── 4. Resultado completo guardado en Firebase ──────────────────────────────
@Serializable
data class AnalisisCompleto(
    val id            : String           = "",
    val childId       : String           = "",
    val fecha         : String           = "",
    val imagePath     : String           = "",
    val foodDetection : FoodDetectionResult = FoodDetectionResult(),
    val nutrition     : NutritionInfo       = NutritionInfo(),
    val analysis      : PediatricAnalysis   = PediatricAnalysis(),
    val creadoEn      : Long                = 0L
)

// ─── 5. UI State — flujo de estados de la pantalla ───────────────────────────
sealed class AnalisisUiState {
    object Idle : AnalisisUiState()
    object Capturando : AnalisisUiState()
    data class Analizando(val mensaje: String) : AnalisisUiState()
    data class Exito(val resultado: AnalisisCompleto) : AnalisisUiState()
    object Guardado : AnalisisUiState()
    data class Error(val mensaje: String) : AnalisisUiState()
}
