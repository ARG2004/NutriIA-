package com.example.nutriia.solidos

import com.example.nutriia.shared.Timestamp
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.RecetaMexicana
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

typealias SolidosRepositorio = SolidosRepository

class SolidosRepository {

    private val alimentosState = MutableStateFlow<Map<String, List<AlimentoIntroducido>>>(emptyMap())
    private val alergenosState = MutableStateFlow<Map<String, List<Alergeno>>>(emptyMap())
    private val recetasState   = MutableStateFlow<Map<String, List<RecetaMexicana>>>(emptyMap())

    suspend fun guardarAlimento(childId: String, alimento: AlimentoIntroducido, ownerUid: String? = null): Result<Unit> {
        val id = if (alimento.id.isEmpty()) com.example.nutriia.platform.generateUUID() else alimento.id
        val item = alimento.copy(id = id)
        val current = alimentosState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != id } + item
        alimentosState.value = alimentosState.value + (childId to updated)
        return Result.success(Unit)
    }

    suspend fun registrarAlimento(childId: String, alimento: AlimentoIntroducido, ownerUid: String? = null): Result<String> {
        val id = if (alimento.id.isEmpty()) com.example.nutriia.platform.generateUUID() else alimento.id
        val item = alimento.copy(id = id)
        val current = alimentosState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != id } + item
        alimentosState.value = alimentosState.value + (childId to updated)
        return Result.success(id)
    }

    suspend fun actualizarReaccion(childId: String, id: String, reaccion: ReaccionAlimento, ownerUid: String? = null): Result<Unit> {
        val current = alimentosState.value[childId] ?: emptyList()
        val updated = current.map { if (it.id == id) it.copy(reaccion = reaccion) else it }
        alimentosState.value = alimentosState.value + (childId to updated)
        return Result.success(Unit)
    }

    fun observarAlimentos(childId: String, ownerUid: String? = null): Flow<List<AlimentoIntroducido>> {
        return alimentosState.map { it[childId] ?: emptyList() }
    }

    fun observarAlergenos(childId: String, ownerUid: String? = null): Flow<List<Alergeno>> {
        return alergenosState.map { it[childId] ?: emptyList() }
    }

    fun observarRecetas(childId: String, ownerUid: String? = null): Flow<List<RecetaMexicana>> {
        return recetasState.map { it[childId] ?: emptyList() }
    }

    suspend fun eliminarAlimento(childId: String, id: String, ownerUid: String? = null): Result<Unit> {
        val current = alimentosState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != id }
        alimentosState.value = alimentosState.value + (childId to updated)
        return Result.success(Unit)
    }
}
