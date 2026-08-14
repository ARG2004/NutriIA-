package com.example.nutriia.alerta

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.example.nutriia.offline.OfflineManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class AlertaRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Ruta: usuarios/{uid}/hijos/{childId}/alertas/{alertaId} ───────────────
    private fun coleccion(childId: String?): com.google.firebase.firestore.CollectionReference {
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrEmpty()) {
            throw IllegalStateException("Usuario no autenticado")
        }
        return if (!childId.isNullOrEmpty()) {
            db.collection("usuarios")
                .document(currentUid)
                .collection("hijos")
                .document(childId)
                .collection("alertas")
        } else {
            db.collection("usuarios")
                .document(currentUid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("alertas")
        }
    }

    // ── Guardar / actualizar ──────────────────────────────────────────────────
    suspend fun guardar(alerta: Alerta): Result<Unit> {
        return try {
            auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(alerta.childId).document(alerta.id).set(alerta.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Eliminar ──────────────────────────────────────────────────────────────
    suspend fun eliminar(childId: String?, alertaId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(alertaId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Toggle activa ─────────────────────────────────────────────────────────
    suspend fun toggleActiva(childId: String?, alertaId: String, activa: Boolean): Result<Unit> {
        return try {
            auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(alertaId).update("activa", activa).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Observar alertas de un hijo (Flow en tiempo real) ─────────────────────
    fun observarPorHijo(childId: String?): Flow<List<Alerta>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener = try {
                coleccion(childId)
                    .orderBy("hora")
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                        val lista = snapshot?.documents?.mapNotNull { doc ->
                            doc.data?.let { Alerta.fromMap(it) }
                        } ?: emptyList()
                        trySend(lista)
                    }
            } catch (e: Exception) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    // ── Obtener todas las alertas activas (para reprogramar al inicio) ────────
    suspend fun obtenerTodasActivas(): List<Alerta> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val alertsList = mutableListOf<Alerta>()
            val source = if (OfflineManager.hayConexion()) Source.DEFAULT else Source.CACHE
            // 1. Alertas de la raíz (embarazo / general)
            val rootSnap = db.collection("usuarios")
                .document(currentUid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("alertas")
                .whereEqualTo("activa", true)
                .get(source).await()
            for (doc in rootSnap.documents) {
                doc.data?.let { map -> alertsList.add(Alerta.fromMap(map)) }
            }

            // 2. Alertas de cada hijo
            val hijosSnap = db.collection("usuarios")
                .document(currentUid)
                .collection("hijos")
                .get(source).await()
            for (childDoc in hijosSnap.documents) {
                val childId = childDoc.id
                val childAlertsSnap = db.collection("usuarios")
                    .document(currentUid)
                    .collection("hijos")
                    .document(childId)
                    .collection("alertas")
                    .whereEqualTo("activa", true)
                    .get(source).await()
                for (doc in childAlertsSnap.documents) {
                    doc.data?.let { map -> alertsList.add(Alerta.fromMap(map)) }
                }
            }
            alertsList
        } catch (e: Exception) { emptyList() }
    }
}