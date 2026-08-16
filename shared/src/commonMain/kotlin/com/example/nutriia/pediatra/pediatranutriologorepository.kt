package com.example.nutriia.pediatra

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

data class Consulta(
    val id:          String = "",
    val texto:       String = "",
    val fecha:       String = "",
    val autorNombre: String = "Nutriólogo",
    val childId:     String = "",
    val tipo:        String = "CONSULTA"
)

typealias PediatraNutriologoRepository = PediatraRepository

class PediatraRepository {

    private val notasState = MutableStateFlow<Map<String, List<Consulta>>>(emptyMap())

    suspend fun guardarNotaNutriologo(
        padreUid:         String,
        childId:          String,
        nota:             Consulta,
        nutriologoNombre: String
    ): Result<Unit> {
        val id = if (nota.id.isEmpty()) com.example.nutriia.platform.generateUUID() else nota.id
        val item = nota.copy(id = id, autorNombre = nutriologoNombre, childId = childId)
        val current = notasState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != id } + item
        notasState.value = notasState.value + (childId to updated)
        return Result.success(Unit)
    }

    fun observarConsultas(padreUid: String, childId: String): Flow<List<Consulta>> {
        return notasState.map { it[childId] ?: emptyList() }
    }
}
