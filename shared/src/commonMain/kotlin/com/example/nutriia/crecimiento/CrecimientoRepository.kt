package com.example.nutriia.crecimiento

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class CrecimientoRepository {

    private val medicionesState = MutableStateFlow<Map<String, List<MedicionCrecimiento>>>(emptyMap())

    suspend fun guardarMedicion(childId: String, medicion: MedicionCrecimiento, ownerUid: String? = null): Result<String> {
        val id = if (medicion.id.isEmpty()) com.example.nutriia.platform.generateUUID() else medicion.id
        val item = medicion.copy(id = id)
        val current = medicionesState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != id } + item
        medicionesState.value = medicionesState.value + (childId to updated)
        return Result.success(id)
    }

    suspend fun guardarMedicion(medicion: MedicionCrecimiento): Result<Unit> {
        val key = medicion.childId.ifEmpty { "general" }
        guardarMedicion(key, medicion)
        return Result.success(Unit)
    }

    fun observarHistorial(childId: String, ownerUid: String? = null): Flow<List<MedicionCrecimiento>> {
        return medicionesState.map { it[childId] ?: emptyList() }
    }

    fun observarMediciones(childId: String, ownerUid: String? = null): Flow<List<MedicionCrecimiento>> {
        return observarHistorial(childId, ownerUid)
    }

    suspend fun eliminarMedicion(childId: String, id: String, ownerUid: String? = null): Result<Unit> {
        val current = medicionesState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != id }
        medicionesState.value = medicionesState.value + (childId to updated)
        return Result.success(Unit)
    }
}
