package com.example.nutriia.solidos

import com.example.nutriia.offline.OfflineManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import com.example.nutriia.util.DateMigrationHelper

class SolidosRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun col(childId: String, ownerUid: String? = null): com.google.firebase.firestore.CollectionReference {
        val uid = ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")
        return if (childId.isEmpty()) {
            db.collection("usuarios").document(uid).collection("perfilEmbarazo").document("unico").collection("solidos")
        } else {
            db.collection("usuarios").document(uid).collection("hijos").document(childId).collection("solidos")
        }
    }

    suspend fun guardarAlimento(childId: String, a: AlimentoIntroducido, ownerUid: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val ref = if (a.id.isEmpty()) col(childId, ownerUid).document()
            else col(childId, ownerUid).document(a.id)

            val data = mapOf(
                "id"                to ref.id,
                "childId"           to childId,
                "userId"            to currentUid,
                "nombre"            to a.nombre,
                "grupo"             to a.grupo.name,
                "fechaIntroduccion" to DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(a.fechaIntroduccion),
                "reaccion"          to a.reaccion.name,
                "notes"             to a.notas,     // Manteniendo compatibilidad
                "notas"             to a.notas,
                "creadoEn"          to Timestamp.now(),
                "fechaCreacion"     to com.example.nutriia.utils.FechaUtils.fechaActual(),
                "horaCreacion"      to com.example.nutriia.utils.FechaUtils.horaActual()
            )
            
            val task = ref.set(data)
            if (OfflineManager.hayConexion()) {
                task.await()
            }
            Result.success(ref.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminarAlimento(childId: String, id: String, ownerUid: String? = null): Result<Unit> {
        return try {
            val task = col(childId, ownerUid).document(id).delete()
            if (OfflineManager.hayConexion()) {
                task.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarReaccion(
        childId:  String,
        id:       String,
        reaccion: ReaccionAlimento,
        ownerUid: String? = null
    ): Result<Unit> {
        return try {
            val task = col(childId, ownerUid).document(id).update("reaccion", reaccion.name)
            if (OfflineManager.hayConexion()) {
                task.await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarAlimentos(childId: String, ownerUid: String? = null): Flow<List<AlimentoIntroducido>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener = try {
                col(childId, ownerUid)
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        val list = snap?.documents?.mapNotNull { doc ->
                            runCatching {
                                AlimentoIntroducido(
                                    id                = doc.getString("id") ?: doc.id,
                                    childId           = doc.getString("childId") ?: "",
                                    userId            = doc.getString("userId") ?: "",
                                    nombre            = doc.getString("nombre") ?: "",
                                    grupo             = GrupoAlimento.valueOf(
                                        doc.getString("grupo") ?: GrupoAlimento.OTROS.name
                                    ),
                                    fechaIntroduccion = DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(doc.getString("fechaIntroduccion") ?: ""),
                                    reaccion          = ReaccionAlimento.valueOf(
                                        doc.getString("reaccion") ?: ReaccionAlimento.NINGUNA.name
                                    ),
                                    notas             = doc.getString("notas") ?: "",
                                    creadoEn          = doc.getTimestamp("creadoEn")
                                )
                            }.getOrNull()
                        }?.sortedByDescending { it.fechaIntroduccion } ?: emptyList()
                        trySend(list)
                    }
            } catch (e: Exception) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    suspend fun obtenerAlergias(childId: String, ownerUid: String? = null): Result<List<AlimentoIntroducido>> {
        return try {
            if (auth.currentUser == null) return Result.success(emptyList())

            val source = if (OfflineManager.hayConexion()) {
                com.google.firebase.firestore.Source.DEFAULT
            } else {
                com.google.firebase.firestore.Source.CACHE
            }

            val query = col(childId, ownerUid)
            val snap = if (OfflineManager.hayConexion()) {
                query.whereEqualTo("reaccion", ReaccionAlimento.ALERGIA.name).get(source).await()
            } else {
                query.get(source).await()
            }

            val list = snap.documents.mapNotNull { doc ->
                if (!OfflineManager.hayConexion() && doc.getString("reaccion") != ReaccionAlimento.ALERGIA.name) {
                    return@mapNotNull null
                }
                runCatching {
                    AlimentoIntroducido(
                        id       = doc.getString("id") ?: doc.id,
                        nombre   = doc.getString("nombre") ?: "",
                        grupo    = GrupoAlimento.valueOf(
                            doc.getString("grupo") ?: GrupoAlimento.OTROS.name
                        ),
                        reaccion = ReaccionAlimento.ALERGIA
                    )
                }.getOrNull()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}