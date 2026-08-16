package com.example.nutriia.nutriente

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

typealias NutrientesRepositorio = NutrienteRepository

class NutrienteRepository {

    private val nutrientesState = MutableStateFlow<Map<String, List<RegistroNutrientes>>>(emptyMap())

    suspend fun guardar(registro: RegistroNutrientes): Result<Unit> {
        val key = registro.childId.ifEmpty { "general" }
        val current = nutrientesState.value[key] ?: emptyList()
        val updated = current.filter { it.id != registro.id } + registro
        nutrientesState.value = nutrientesState.value + (key to updated)
        return Result.success(Unit)
    }

    suspend fun eliminar(childId: String?, registroId: String): Result<Unit> {
        val key = childId?.ifEmpty { "general" } ?: "general"
        val current = nutrientesState.value[key] ?: emptyList()
        val updated = current.filter { it.id != registroId }
        nutrientesState.value = nutrientesState.value + (key to updated)
        return Result.success(Unit)
    }

    fun observarPorHijo(childId: String?): Flow<List<RegistroNutrientes>> {
        val key = childId?.ifEmpty { "general" } ?: "general"
        return nutrientesState.map { it[key] ?: emptyList() }
    }

    fun observarPorHijoYFecha(childId: String?, fecha: String): Flow<List<RegistroNutrientes>> {
        val key = childId?.ifEmpty { "general" } ?: "general"
        return nutrientesState.map { map -> (map[key] ?: emptyList()).filter { it.fecha == fecha } }
    }

    suspend fun obtenerPorHijoYFecha(childId: String?, fecha: String): List<RegistroNutrientes> {
        val key = childId?.ifEmpty { "general" } ?: "general"
        return (nutrientesState.value[key] ?: emptyList()).filter { it.fecha == fecha }
    }
}
