package com.example.nutriia.alerta

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class AlertaRepository {

    private val alertsState = MutableStateFlow<Map<String, List<Alerta>>>(emptyMap())

    suspend fun guardar(alerta: Alerta): Result<Unit> {
        val key = alerta.childId.ifEmpty { "general" }
        val current = alertsState.value[key] ?: emptyList()
        val updated = current.filter { it.id != alerta.id } + alerta
        alertsState.value = alertsState.value + (key to updated)
        return Result.success(Unit)
    }

    suspend fun eliminar(childId: String?, alertaId: String): Result<Unit> {
        val key = childId?.ifEmpty { "general" } ?: "general"
        val current = alertsState.value[key] ?: emptyList()
        val updated = current.filter { it.id != alertaId }
        alertsState.value = alertsState.value + (key to updated)
        return Result.success(Unit)
    }

    suspend fun toggleActiva(childId: String?, alertaId: String, activa: Boolean): Result<Unit> {
        val key = childId?.ifEmpty { "general" } ?: "general"
        val current = alertsState.value[key] ?: emptyList()
        val updated = current.map { if (it.id == alertaId) it.copy(activa = activa) else it }
        alertsState.value = alertsState.value + (key to updated)
        return Result.success(Unit)
    }

    fun observarPorHijo(childId: String?): Flow<List<Alerta>> {
        val key = childId?.ifEmpty { "general" } ?: "general"
        return alertsState.map { map ->
            (map[key] ?: emptyList()).sortedBy { it.hora }
        }
    }

    suspend fun obtenerTodasActivas(): List<Alerta> {
        return alertsState.value.values.flatten().filter { it.activa }
    }
}