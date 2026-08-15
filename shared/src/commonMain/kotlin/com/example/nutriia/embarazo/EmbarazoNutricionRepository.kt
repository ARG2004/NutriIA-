package com.example.nutriia.embarazo

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.channels.awaitClose
import com.example.nutriia.nutriente.RegistroNutrientes
import com.example.nutriia.util.DateMigrationHelper

class EmbarazoNutricionRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun col(): com.google.firebase.firestore.CollectionReference {
        val uid = auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        return db.collection("usuarios").document(uid).collection("perfilEmbarazo").document("unico").collection("embarazoNutrientes")
    }

    suspend fun guardar(registro: RegistroNutrientes): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            
            // Unify date format when saving: ensure it's saved as dd/MM/yyyy
            val fechaFormateada = DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(registro.fecha)
            val registroConFechaUnificada = registro.copy(fecha = fechaFormateada)
            
            col().document(registroConFechaUnificada.id).set(registroConFechaUnificada.toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(registroId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            col().document(registroId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarPorFecha(fecha: String): Flow<List<RegistroNutrientes>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            // Unify search key date to dd/MM/yyyy format
            val fechaBúsqueda = DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(fecha)
            android.util.Log.d("EmbarazoRepo", "observarPorFecha: fecha=$fecha, fechaBusqueda=$fechaBúsqueda")

            val listener = try {
                col()
                    .whereEqualTo("fecha", fechaBúsqueda)
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            android.util.Log.e("EmbarazoRepo", "Error en listener de alimentos", err)
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        val lista = snap?.documents
                            ?.mapNotNull { doc ->
                                try {
                                    doc.data?.let { m -> RegistroNutrientes.fromMap(m) }
                                } catch (e: Exception) {
                                    android.util.Log.e("EmbarazoRepo", "Error mapeando documento ${doc.id}", e)
                                    null
                                }
                            }
                            ?: emptyList()
                        android.util.Log.d("EmbarazoRepo", "observarPorFecha: Documentos recuperados = ${lista.size}")
                        trySend(lista)
                    }
            } catch (e: Exception) {
                android.util.Log.e("EmbarazoRepo", "Excepcion iniciando listener", e)
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { e -> 
            android.util.Log.e("EmbarazoRepo", "Excepcion en flujo", e)
            emit(emptyList()) 
        }
}
