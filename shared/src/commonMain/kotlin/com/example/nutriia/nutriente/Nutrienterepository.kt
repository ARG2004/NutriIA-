package com.example.nutriia.nutriente

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

typealias NutrientesRepositorio = NutrienteRepository

class NutrienteRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun coleccion(childId: String?) =
        if (!childId.isNullOrEmpty()) {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("hijos")
                .document(childId)
                .collection("nutrientes")
        } else {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("nutrientes")
        }

    suspend fun guardar(registro: RegistroNutrientes): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(registro.childId).document(registro.id).set(registro.toMap())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(childId: String?, registroId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(registroId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarPorHijo(childId: String?): Flow<List<RegistroNutrientes>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return try {
            coleccion(childId).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching { RegistroNutrientes.fromMap(doc.data()) }.getOrNull()
                }.sortedByDescending { it.fecha }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observarPorHijoYFecha(childId: String?, fecha: String): Flow<List<RegistroNutrientes>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        val dateSlash = com.example.nutriia.util.DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(fecha)
        val dateIso = com.example.nutriia.util.DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(fecha)

        return try {
            coleccion(childId).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val reg = RegistroNutrientes.fromMap(doc.data())
                        if (reg.fecha == fecha || reg.fecha == dateSlash || reg.fecha == dateIso) reg else null
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun obtenerPorHijoYFecha(childId: String?, fecha: String): List<RegistroNutrientes> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val dateSlash = com.example.nutriia.util.DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(fecha)
        val dateIso = com.example.nutriia.util.DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(fecha)

        return try {
            val snapshot = coleccion(childId).get()
            snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val reg = RegistroNutrientes.fromMap(doc.data())
                    if (reg.fecha == fecha || reg.fecha == dateSlash || reg.fecha == dateIso) reg else null
                }.getOrNull()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
