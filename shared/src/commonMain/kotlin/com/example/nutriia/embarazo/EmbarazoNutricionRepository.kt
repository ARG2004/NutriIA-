package com.example.nutriia.embarazo

import com.example.nutriia.nutriente.RegistroNutrientes
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

typealias PlanEmbarazoIA = PlanDietaEmbarazoSemanal
typealias RegistroNutrientesEmbarazo = RegistroNutrientes

class EmbarazoNutricionRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun col() =
        db.collection("usuarios")
            .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
            .collection("perfilEmbarazo")
            .document("unico")
            .collection("embarazoNutrientes")

    suspend fun guardar(registro: RegistroNutrientes): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            col().document(registro.id).set(registro.toMap())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registrarNutrientes(registro: RegistroNutrientes): Result<Unit> = guardar(registro)

    suspend fun eliminar(registroId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            col().document(registroId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarPorFecha(fecha: String): Flow<List<RegistroNutrientes>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return try {
            col().where { "fecha".equalTo(fecha) }.snapshots.map { snap ->
                snap.documents.mapNotNull { doc ->
                    runCatching { 
                        val r = doc.data<RegistroNutrientes>()
                        if (r.id.isBlank()) r.copy(id = doc.id) else r
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observarNutrientes(): Flow<List<RegistroNutrientes>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return try {
            col().snapshots.map { snap ->
                snap.documents.mapNotNull { doc ->
                    runCatching { 
                        val r = doc.data<RegistroNutrientes>()
                        if (r.id.isBlank()) r.copy(id = doc.id) else r
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun guardarPlan(plan: PlanDietaEmbarazoSemanal): Result<Unit> {
        return Result.success(Unit)
    }

    fun observarPlanSemana(semana: Int): Flow<PlanDietaEmbarazoSemanal?> {
        return flowOf(null)
    }
}
