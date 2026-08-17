package com.example.nutriia.solidos

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.RecetaMexicana
import com.example.nutriia.utils.FechaUtils
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

typealias SolidosRepositorio = SolidosRepository

class SolidosRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun col(childId: String, ownerUid: String? = null) =
        if (childId.isEmpty()) {
            db.collection("usuarios")
                .document(ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("solidos")
        } else {
            db.collection("usuarios")
                .document(ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("hijos")
                .document(childId)
                .collection("solidos")
        }

    suspend fun guardarAlimento(childId: String, a: AlimentoIntroducido, ownerUid: String? = null): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val id = if (a.id.isEmpty()) generateUUID() else a.id
            val ref = col(childId, ownerUid).document(id)

            val data = mapOf<String, Any?>(
                "id" to id,
                "childId" to childId,
                "userId" to currentUid,
                "nombre" to a.nombre,
                "grupo" to a.grupo.name,
                "fechaIntroduccion" to a.fechaIntroduccion,
                "reaccion" to a.reaccion.name,
                "notas" to a.notas,
                "notes" to a.notas,
                "creadoEnMillis" to currentTimeMillis(),
                "fechaCreacion" to FechaUtils.fechaActual(),
                "horaCreacion" to FechaUtils.horaActual()
            )
            ref.set(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrarAlimento(childId: String, a: AlimentoIntroducido, ownerUid: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val id = if (a.id.isEmpty()) generateUUID() else a.id
            val ref = col(childId, ownerUid).document(id)

            val data = mapOf<String, Any?>(
                "id" to id,
                "childId" to childId,
                "userId" to currentUid,
                "nombre" to a.nombre,
                "grupo" to a.grupo.name,
                "fechaIntroduccion" to a.fechaIntroduccion,
                "reaccion" to a.reaccion.name,
                "notas" to a.notas,
                "notes" to a.notas,
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

    suspend fun eliminarAlimento(childId: String, id: String, ownerUid: String? = null): Result<Unit> {
        return try {
            col(childId, ownerUid).document(id).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun actualizarReaccion(
        childId: String,
        id: String,
        reaccion: ReaccionAlimento,
        ownerUid: String? = null
    ): Result<Unit> {
        return try {
            col(childId, ownerUid).document(id).update(mapOf("reaccion" to reaccion.name))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarAlimentos(childId: String, ownerUid: String? = null): Flow<List<AlimentoIntroducido>> {
        return try {
            col(childId, ownerUid).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
                        val nombre = runCatching { doc.get<String?>("nombre") }.getOrNull() ?: ""
                        if (nombre.isBlank()) return@mapNotNull null

                        val grupoStr = runCatching { doc.get<String?>("grupo") }.getOrNull() ?: GrupoAlimento.FRUTAS.name
                        val fechaStr = runCatching { doc.get<String?>("fechaIntroduccion") }.getOrNull() ?: ""
                        val reaccionStr = runCatching { doc.get<String?>("reaccion") }.getOrNull() ?: ReaccionAlimento.NINGUNA.name
                        val notasStr = runCatching { doc.get<String?>("notas") }.getOrNull() 
                            ?: runCatching { doc.get<String?>("notes") }.getOrNull() 
                            ?: ""

                        AlimentoIntroducido(
                            id = id,
                            nombre = nombre,
                            grupo = runCatching { GrupoAlimento.valueOf(grupoStr) }.getOrDefault(GrupoAlimento.FRUTAS),
                            fechaIntroduccion = fechaStr,
                            reaccion = runCatching { ReaccionAlimento.valueOf(reaccionStr) }.getOrDefault(ReaccionAlimento.NINGUNA),
                            notas = notasStr
                        )
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observarAlergenos(childId: String, ownerUid: String? = null): Flow<List<Alergeno>> {
        return observarAlimentos(childId, ownerUid).map { alimentos ->
            alimentos.filter { it.reaccion == ReaccionAlimento.ALERGIA || it.reaccion == ReaccionAlimento.LEVE || it.reaccion == ReaccionAlimento.RECHAZO }
                .mapNotNull { al ->
                    Alergeno.entries.firstOrNull { it.label.contains(al.nombre, ignoreCase = true) || al.nombre.contains(it.label, ignoreCase = true) }
                }.distinct()
        }
    }

    fun observarRecetas(childId: String, ownerUid: String? = null): Flow<List<RecetaMexicana>> {
        return flowOf(emptyList())
    }
}
