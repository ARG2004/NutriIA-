package com.example.nutriia.analisisIA

import com.example.nutriia.platform.generateUUID
import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.utils.FechaUtils

class AnalisisRepository {

    suspend fun analizarAlimentoDemo(imagePath: String, targetName: String): AnalisisCompleto {
        return AnalisisCompleto(
            id = generateUUID(),
            childId = targetName,
            fecha = FechaUtils.hoyIso(),
            imagePath = imagePath,
            foodDetection = FoodDetectionResult(
                foodName = "Puré de Manzana con Avena",
                ingredients = listOf("Manzana", "Avena", "Agua"),
                foodType = "desayuno",
                confidence = 0.96
            ),
            nutrition = NutritionInfo(
                calories = 120.0,
                protein = 2.5,
                carbohydrates = 25.0,
                fat = 1.0,
                fiber = 3.5,
                sugar = 12.0,
                sodium = 5.0
            ),
            analysis = PediatricAnalysis(
                recommended = true,
                recommendedPortion = "3 a 4 cucharaditas",
                benefits = listOf("Rico en fibra natural", "Fácil digestión", "Aporte de energía sostenida"),
                warnings = listOf("Introducir gradualmente", "No agregar azúcares añadidos"),
                frequency = "2 a 3 veces por semana"
            ),
            creadoEn = currentTimeMillis()
        )
    }

    suspend fun guardarAnalisis(childId: String, analisis: AnalisisCompleto): Result<Unit> {
        return Result.success(Unit)
    }
}