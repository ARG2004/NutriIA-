package com.example.nutriia.nutriente

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class NutrientesRepositorio {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ── Ruta: usuarios/{uid}/hijos/{childId}/nutrientes ───────────────────────
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
                .collection("nutrientes")
        } else {
            db.collection("usuarios")
                .document(currentUid)
                .collection("nutrientes")
        }
    }

    suspend fun guardar(registro: RegistroNutrientes): Result<Unit> {
        return try {
            auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(registro.childId).document(registro.id).set(registro.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(childId: String?, registroId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(registroId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarPorHijoYFecha(childId: String, fecha: String): Flow<List<RegistroNutrientes>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener = try {
                coleccion(childId)
                    .whereEqualTo("fecha", fecha)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                        val lista = snap?.documents
                            ?.mapNotNull { it.data?.let { m -> RegistroNutrientes.fromMap(m) } }
                            ?: emptyList()
                        trySend(lista)
                    }
            } catch (e: Exception) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    fun observarTodosPorHijo(childId: String): Flow<List<RegistroNutrientes>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener = try {
                coleccion(childId)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                        val lista = snap?.documents
                            ?.mapNotNull { it.data?.let { m -> RegistroNutrientes.fromMap(m) } }
                            ?: emptyList()
                        trySend(lista)
                    }
            } catch (e: Exception) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }
}