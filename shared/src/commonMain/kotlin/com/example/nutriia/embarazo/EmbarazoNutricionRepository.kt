package com.example.nutriia.embarazo

import com.example.nutriia.nutriente.RegistroNutrientes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

typealias PlanEmbarazoIA = PlanDietaEmbarazoSemanal
typealias RegistroNutrientesEmbarazo = RegistroNutrientes

class EmbarazoNutricionRepository {

    private val planesState = MutableStateFlow<List<PlanDietaEmbarazoSemanal>>(emptyList())
    private val registrosState = MutableStateFlow<List<RegistroNutrientes>>(emptyList())

    suspend fun guardarPlan(plan: PlanDietaEmbarazoSemanal): Result<Unit> {
        planesState.value = planesState.value.filter { it.diaSemana != plan.diaSemana } + plan
        return Result.success(Unit)
    }

    suspend fun guardar(registro: RegistroNutrientes): Result<Unit> {
        registrosState.value = registrosState.value.filter { it.id != registro.id } + registro
        return Result.success(Unit)
    }

    suspend fun eliminar(id: String): Result<Unit> {
        registrosState.value = registrosState.value.filter { it.id != id }
        return Result.success(Unit)
    }

    fun observarPlanSemana(semana: Int): Flow<PlanDietaEmbarazoSemanal?> {
        return planesState.map { list -> list.firstOrNull() }
    }

    fun observarPorFecha(fecha: String): Flow<List<RegistroNutrientes>> {
        return registrosState.map { list -> list.filter { it.fecha == fecha } }
    }

    suspend fun registrarNutrientes(registro: RegistroNutrientes): Result<Unit> = guardar(registro)

    fun observarNutrientes(): Flow<List<RegistroNutrientes>> {
        return registrosState
    }
}
