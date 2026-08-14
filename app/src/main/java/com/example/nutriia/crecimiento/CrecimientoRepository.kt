package com.example.nutriia.crecimiento

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
import com.example.nutriia.utils.FechaUtils

class CrecimientoRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun col(childId: String, ownerUid: String? = null) =
        db.collection("usuarios")
            .document(ownerUid ?: auth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado"))
            .collection("hijos")
            .document(childId)
            .collection("crecimiento")

    suspend fun guardarMedicion(childId: String, m: MedicionCrecimiento, ownerUid: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val ref = if (m.id.isEmpty()) col(childId, ownerUid).document()
            else col(childId, ownerUid).document(m.id)

            val data = mapOf(
                "id"        to ref.id,      // siempre el id real del documento
                "childId"   to childId,
                "userId"    to currentUid,
                "fecha"     to DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(m.fecha),
                "pesoKg"    to m.pesoKg,
                "tallaCm"   to m.tallaCm,
                "circCefCm" to m.circCefCm,
                "notes"     to m.notas,     // Manteniendo compatibilidad
                "notas"     to m.notas,
                "creadoEn"  to Timestamp.now(),
                "fechaCreacion" to FechaUtils.fechaActual(),
                "horaCreacion"  to FechaUtils.horaActual()
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

    suspend fun eliminarMedicion(childId: String, id: String, ownerUid: String? = null): Result<Unit> {
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

    fun observarHistorial(childId: String, ownerUid: String? = null): Flow<List<MedicionCrecimiento>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener = try {
                col(childId, ownerUid)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                        val list = snap?.documents?.mapNotNull { doc ->
                            runCatching {
                                MedicionCrecimiento(
                                    // FIX: doc.id es el identificador real del documento en
                                    // Firestore — siempre único. getString("id") puede ser ""
                                    // si el campo no se escribió correctamente en una versión
                                    // anterior, lo que causaba que remember(m.id) compartiera
                                    // estado entre mediciones distintas y la UI no se actualizara.
                                    id        = doc.id,
                                    childId   = doc.getString("childId") ?: "",
                                    userId    = doc.getString("userId") ?: "",
                                    fecha     = DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(doc.getString("fecha") ?: ""),
                                    pesoKg    = doc.getDouble("pesoKg") ?: 0.0,
                                    tallaCm   = doc.getDouble("tallaCm") ?: 0.0,
                                    circCefCm = doc.getDouble("circCefCm") ?: 0.0,
                                    notas     = doc.getString("notas") ?: "",
                                    creadoEn  = doc.getTimestamp("creadoEn")
                                )
                            }.getOrNull()
                        } ?: emptyList()
                        trySend(list)
                    }
            } catch (e: Exception) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }
}