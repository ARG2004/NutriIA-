package com.example.nutriia.embarazo

import com.example.nutriia.platform.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PlanEmbarazoIARepository {

    suspend fun generarPlanIA(
        perfil: PerfilEmbarazo,
        recetasBase: List<RecetaEmbarazo>
    ): Result<List<PlanDietaEmbarazoSemanal>> = withContext(Dispatchers.Default) {
        try {
            val plan = DietaEmbarazoEngine.generarPlanSemanal(
                semanas = perfil.semanas,
                nivel = perfil.nivelIngreso,
                region = perfil.region,
                alergenos = perfil.alergenosParsados,
                condiciones = perfil.condiciones,
                alimentosRegistrados = emptyList()
            )
            Result.success(plan)
        } catch (e: Exception) {
            Log.e("PlanEmbarazoIA", "Exception generating plan", e)
            Result.failure(e)
        }
    }
}

data class PlanIAResponse(
    val dias: List<PlanDietaEmbarazoSemanal> = emptyList()
)
