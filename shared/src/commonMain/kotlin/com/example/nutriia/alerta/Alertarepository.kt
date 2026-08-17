package com.example.nutriia.alerta

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class AlertaRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun coleccion(childId: String?) =
        if (!childId.isNullOrEmpty()) {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("hijos")
                .document(childId)
                .collection("alertas")
        } else {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("alertas")
        }

    suspend fun guardar(alerta: Alerta): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(alerta.childId).document(alerta.id).set(alerta.toMap())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(childId: String?, alertaId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(alertaId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleActiva(childId: String?, alertaId: String, activa: Boolean): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(alertaId).update(mapOf("activa" to activa))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarPorHijo(childId: String?): Flow<List<Alerta>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return try {
            coleccion(childId).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching { Alerta.fromMap(doc.data()) }.getOrNull()
                }.sortedBy { it.hora }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun obtenerTodasActivas(): List<Alerta> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val alertsList = mutableListOf<Alerta>()

            val rootSnap = db.collection("usuarios")
                .document(currentUid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("alertas")
                .where { "activa".equalTo(true) }
                .get()

            for (doc in rootSnap.documents) {
                runCatching { Alerta.fromMap(doc.data()) }.getOrNull()?.let { alertsList.add(it) }
            }

            val hijosSnap = db.collection("usuarios")
                .document(currentUid)
                .collection("hijos")
                .get()

            for (childDoc in hijosSnap.documents) {
                val childAlertsSnap = db.collection("usuarios")
                    .document(currentUid)
                    .collection("hijos")
                    .document(childDoc.id)
                    .collection("alertas")
                    .where { "activa".equalTo(true) }
                    .get()

                for (doc in childAlertsSnap.documents) {
                    runCatching { Alerta.fromMap(doc.data()) }.getOrNull()?.let { alertsList.add(it) }
                }
            }

            alertsList
        } catch (e: Exception) {
            emptyList()
        }
    }
}