package com.example.nutriia.alerta

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.catch
import com.example.nutriia.platform.Log

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
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("AlertaRepo", "No se puede observar: Usuario no autenticado")
            return flowOf(emptyList())
        }
        
        Log.i("AlertaRepo", "Iniciando observación para hijo: $childId (uid: $uid)")
        
        return try {
            coleccion(childId).snapshots
                .map { snapshot ->
                    Log.i("AlertaRepo", "Snapshot recibido con ${snapshot.documents.size} documentos")
                    snapshot.documents.mapNotNull { doc ->
                        try {
                            Alerta.fromMap(doc.data())
                        } catch (e: Exception) {
                            Log.e("AlertaRepo", "Error parseando alerta ${doc.id}: ${e.message}")
                            null
                        }
                    }.sortedBy { it.hora }
                }
                .onStart { Log.i("AlertaRepo", "Flow de alertas iniciado") }
                .catch { e -> 
                    Log.e("AlertaRepo", "Error en snapshots de alertas: ${e.message}")
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e("AlertaRepo", "Error fatal iniciando observación: ${e.message}")
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