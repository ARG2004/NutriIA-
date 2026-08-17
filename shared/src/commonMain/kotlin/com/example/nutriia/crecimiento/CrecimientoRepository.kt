package com.example.nutriia.crecimiento

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.utils.FechaUtils
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class CrecimientoRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun col(childId: String, ownerUid: String? = null) =
        if (childId.isEmpty()) {
            db.collection("usuarios")
                .document(ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("crecimiento")
        } else {
            db.collection("usuarios")
                .document(ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("hijos")
                .document(childId)
                .collection("crecimiento")
        }

    suspend fun guardarMedicion(childId: String, m: MedicionCrecimiento, ownerUid: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val id = if (m.id.isEmpty()) generateUUID() else m.id
            val ref = col(childId, ownerUid).document(id)

            val data = mapOf<String, Any?>(
                "id" to id,
                "childId" to childId,
                "userId" to currentUid,
                "fecha" to m.fecha,
                "pesoKg" to m.pesoKg,
                "tallaCm" to m.tallaCm,
                "circCefCm" to m.circCefCm,
                "notas" to m.notas,
                "notes" to m.notas,
                "creadoEnMillis" to currentTimeMillis(),
                "fechaCreacion" to FechaUtils.fechaActual(),
                "horaCreacion" to FechaUtils.horaActual()
            )
            ref.set(data)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun guardarMedicion(medicion: MedicionCrecimiento): Result<Unit> {
        val key = medicion.childId.ifEmpty { "general" }
        return guardarMedicion(key, medicion).map { }
    }

    suspend fun eliminarMedicion(childId: String, id: String, ownerUid: String? = null): Result<Unit> {
        return try {
            col(childId, ownerUid).document(id).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarHistorial(childId: String, ownerUid: String? = null): Flow<List<MedicionCrecimiento>> {
        return try {
            col(childId, ownerUid).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
                        val childIdDoc = runCatching { doc.get<String?>("childId") }.getOrNull() ?: childId
                        val userId = runCatching { doc.get<String?>("userId") }.getOrNull() ?: ""
                        val fecha = runCatching { doc.get<String?>("fecha") }.getOrNull() ?: ""
                        val peso = runCatching { doc.get<Double?>("pesoKg") }.getOrNull()
                            ?: runCatching { doc.get<Long?>("pesoKg")?.toDouble() }.getOrNull()
                            ?: runCatching { doc.get<String?>("pesoKg")?.toDoubleOrNull() }.getOrNull()
                            ?: 0.0
                        val talla = runCatching { doc.get<Double?>("tallaCm") }.getOrNull()
                            ?: runCatching { doc.get<Long?>("tallaCm")?.toDouble() }.getOrNull()
                            ?: runCatching { doc.get<String?>("tallaCm")?.toDoubleOrNull() }.getOrNull()
                            ?: 0.0
                        val circCef = runCatching { doc.get<Double?>("circCefCm") }.getOrNull()
                            ?: runCatching { doc.get<Long?>("circCefCm")?.toDouble() }.getOrNull()
                            ?: runCatching { doc.get<String?>("circCefCm")?.toDoubleOrNull() }.getOrNull()
                            ?: 0.0
                        val notas = runCatching { doc.get<String?>("notas") }.getOrNull()
                            ?: runCatching { doc.get<String?>("notes") }.getOrNull()
                            ?: ""

                        MedicionCrecimiento(
                            id = id,
                            childId = childIdDoc,
                            userId = userId,
                            fecha = fecha,
                            pesoKg = peso,
                            tallaCm = talla,
                            circCefCm = circCef,
                            notas = notas
                        )
                    }.getOrNull()
                }.sortedByDescending { it.fecha }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observarMediciones(childId: String, ownerUid: String? = null): Flow<List<MedicionCrecimiento>> {
        return observarHistorial(childId, ownerUid)
    }
}
