package com.example.nutriia.pediatra

import com.example.nutriia.vinculacion.PlanAlimentario
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

// ─── Modelo ───────────────────────────────────────────────────────────────────

data class Consulta(
    val id:              String = "",
    val fecha:           String = "",
    val nombreMedico:    String = "",
    val motivo:          String = "",
    val notas:           String = "",
    val proximaCita:     String = "",
    val pesoEnConsulta:  Double = 0.0,
    val tallaEnConsulta: Double = 0.0,
    val tipo:            String = "consulta",
    val autorUid:        String = "",
    val autorNombre:     String = ""
)

// ─── Repositorio ──────────────────────────────────────────────────────────────

class PediatraRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ─── Rutas Firestore ──────────────────────────────────────────────────────

    private fun consultasCol(ownerUid: String, childId: String) =
        db.collection("usuarios")
            .document(ownerUid)
            .collection("hijos")
            .document(childId)
            .collection("consultas")

    private fun planesCol(padreUid: String, childId: String) =
        db.collection("usuarios")
            .document(padreUid)
            .collection("hijos")
            .document(childId)
            .collection("planes_alimentarios")

    // ═════════════════════════════════════════════════════════════════════════
    // OBSERVAR CONSULTAS — Flow en tiempo real
    // ═════════════════════════════════════════════════════════════════════════

    fun observarConsultas(ownerUid: String, childId: String): Flow<List<Consulta>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList()); close(); return@callbackFlow
            }

            val listener = try {
                consultasCol(ownerUid, childId)
                    .orderBy("fecha", Query.Direction.DESCENDING)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                        val lista = snap?.documents?.mapNotNull { doc ->
                            runCatching {
                                Consulta(
                                    id              = doc.id,
                                    fecha           = doc.getString("fecha")           ?: "",
                                    nombreMedico    = doc.getString("nombreMedico")    ?: "",
                                    motivo          = doc.getString("motivo")          ?: "",
                                    notas           = doc.getString("notas")           ?: "",
                                    proximaCita     = doc.getString("proximaCita")     ?: "",
                                    pesoEnConsulta  = doc.getDouble("pesoEnConsulta")  ?: 0.0,
                                    tallaEnConsulta = doc.getDouble("tallaEnConsulta") ?: 0.0,
                                    tipo            = doc.getString("tipo")            ?: "consulta",
                                    autorUid        = doc.getString("autorUid")        ?: "",
                                    autorNombre     = doc.getString("autorNombre")     ?: ""
                                )
                            }.getOrNull()
                        } ?: emptyList()
                        trySend(lista)
                    }
            } catch (e: Exception) {
                trySend(emptyList()); close(); return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    // ═════════════════════════════════════════════════════════════════════════
    // GUARDAR CONSULTA — padre crea consulta normal
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun guardarConsulta(
        ownerUid: String,
        childId:  String,
        consulta: Consulta
    ): Result<Unit> {
        val currentUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            val ref = if (consulta.id.isBlank())
                consultasCol(ownerUid, childId).document()
            else
                consultasCol(ownerUid, childId).document(consulta.id)

            val data = mutableMapOf<String, Any?>(
                "fecha"           to consulta.fecha,
                "nombreMedico"    to consulta.nombreMedico,
                "motivo"          to consulta.motivo,
                "notas"           to consulta.notas,
                "proximaCita"     to consulta.proximaCita,
                "pesoEnConsulta"  to consulta.pesoEnConsulta,
                "tallaEnConsulta" to consulta.tallaEnConsulta,
                "tipo"            to "consulta",
                "autorUid"        to currentUid,
                "creadoEn"        to Timestamp.now()
            )
            ref.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GUARDAR NOTA DEL NUTRIÓLOGO — tipo especial, solo él puede editar
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun guardarNotaNutriologo(
        padreUid:         String,
        childId:          String,
        nota:             Consulta,
        nutriologoNombre: String
    ): Result<Unit> {
        val currentUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            val ref = if (nota.id.isBlank())
                consultasCol(padreUid, childId).document()
            else
                consultasCol(padreUid, childId).document(nota.id)

            val data = mapOf(
                "fecha"           to nota.fecha,
                "nombreMedico"    to nutriologoNombre,
                "motivo"          to nota.motivo,
                "notas"           to nota.notas,
                "proximaCita"     to nota.proximaCita,
                "pesoEnConsulta"  to nota.pesoEnConsulta,
                "tallaEnConsulta" to nota.tallaEnConsulta,
                "tipo"            to "nota_nutriologo",
                "autorUid"        to currentUid,
                "autorNombre"     to nutriologoNombre,
                "creadoEn"        to Timestamp.now()
            )
            ref.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // ELIMINAR CONSULTA
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun eliminarConsulta(
        ownerUid:   String,
        childId:    String,
        consultaId: String
    ): Result<Unit> {
        if (auth.currentUser == null)
            return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            consultasCol(ownerUid, childId).document(consultaId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // PLANES ALIMENTARIOS
    // ═════════════════════════════════════════════════════════════════════════

    fun observarPlanes(padreUid: String, childId: String): Flow<List<PlanAlimentario>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList()); close(); return@callbackFlow
            }

            val listener = try {
                planesCol(padreUid, childId)
                    .whereEqualTo("activo", true)
                    .orderBy("creadoEn", Query.Direction.DESCENDING)
                    .addSnapshotListener { snap, err ->
                        if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                        val lista = snap?.documents?.mapNotNull { doc ->
                            doc.data?.let { PlanAlimentario.fromMap(doc.id, it) }
                        } ?: emptyList()
                        trySend(lista)
                    }
            } catch (e: Exception) {
                trySend(emptyList()); close(); return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    suspend fun guardarPlan(
        padreUid: String,
        childId:  String,
        plan:     PlanAlimentario
    ): Result<Unit> {
        if (auth.currentUser == null)
            return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            val ref = if (plan.id.isBlank())
                planesCol(padreUid, childId).document()
            else
                planesCol(padreUid, childId).document(plan.id)

            ref.set(plan.copy(id = ref.id, padreUid = padreUid, childId = childId).toMap()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // HELPER
    // ═════════════════════════════════════════════════════════════════════════

    fun obtenerUidActual(): String? = auth.currentUser?.uid
}