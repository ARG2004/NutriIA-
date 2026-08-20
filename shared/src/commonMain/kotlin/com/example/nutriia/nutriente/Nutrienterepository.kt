package com.example.nutriia.nutriente

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

typealias NutrientesRepositorio = NutrienteRepository

class NutrienteRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun coleccion(childId: String?) =
        if (!childId.isNullOrEmpty()) {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("hijos")
                .document(childId)
                .collection("nutrientes")
        } else {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("nutrientes")
        }

    suspend fun guardar(registro: RegistroNutrientes): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            // FIX iOS: usar objeto @Serializable directo
            coleccion(registro.childId).document(registro.id).set(registro)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(childId: String?, registroId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(registroId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseRegistro(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): RegistroNutrientes? {
        return runCatching {
            val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
            val childId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: ""
            val fecha = runCatching { doc.get<String?>("fecha") }.getOrNull() ?: ""
            val comida = runCatching { doc.get<String?>("comida") }.getOrNull() ?: ""
            val alimento = runCatching { doc.get<String?>("alimento") }.getOrNull() ?: ""
            val notas = runCatching { doc.get<String?>("notas") }.getOrNull() ?: ""

            val cal = runCatching { doc.get<Double?>("macros.calorias") }.getOrNull()
                ?: runCatching { doc.get<Double?>("calorias") }.getOrNull()
                ?: runCatching { doc.get<Long?>("macros.calorias")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("calorias")?.toDouble() }.getOrNull()
                ?: 0.0

            val prot = runCatching { doc.get<Double?>("macros.proteinas") }.getOrNull()
                ?: runCatching { doc.get<Double?>("proteinas") }.getOrNull()
                ?: runCatching { doc.get<Long?>("macros.proteinas")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("proteinas")?.toDouble() }.getOrNull()
                ?: 0.0

            val gras = runCatching { doc.get<Double?>("macros.grasas") }.getOrNull()
                ?: runCatching { doc.get<Double?>("grasas") }.getOrNull()
                ?: runCatching { doc.get<Long?>("macros.grasas")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("grasas")?.toDouble() }.getOrNull()
                ?: 0.0

            val carb = runCatching { doc.get<Double?>("macros.carbohidratos") }.getOrNull()
                ?: runCatching { doc.get<Double?>("carbohidratos") }.getOrNull()
                ?: runCatching { doc.get<Long?>("macros.carbohidratos")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("carbohidratos")?.toDouble() }.getOrNull()
                ?: 0.0

            val hierro = runCatching { doc.get<Double?>("micros.hierro") }.getOrNull()
                ?: runCatching { doc.get<Double?>("hierro") }.getOrNull()
                ?: runCatching { doc.get<Long?>("micros.hierro")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("hierro")?.toDouble() }.getOrNull()
                ?: 0.0

            val calcio = runCatching { doc.get<Double?>("micros.calcio") }.getOrNull()
                ?: runCatching { doc.get<Double?>("calcio") }.getOrNull()
                ?: runCatching { doc.get<Long?>("micros.calcio")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("calcio")?.toDouble() }.getOrNull()
                ?: 0.0

            val vitA = runCatching { doc.get<Double?>("micros.vitaminaA") }.getOrNull()
                ?: runCatching { doc.get<Double?>("vitaminaA") }.getOrNull()
                ?: runCatching { doc.get<Long?>("micros.vitaminaA")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("vitaminaA")?.toDouble() }.getOrNull()
                ?: 0.0

            val vitC = runCatching { doc.get<Double?>("micros.vitaminaC") }.getOrNull()
                ?: runCatching { doc.get<Double?>("vitaminaC") }.getOrNull()
                ?: runCatching { doc.get<Long?>("micros.vitaminaC")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("vitaminaC")?.toDouble() }.getOrNull()
                ?: 0.0

            val zinc = runCatching { doc.get<Double?>("micros.zinc") }.getOrNull()
                ?: runCatching { doc.get<Double?>("zinc") }.getOrNull()
                ?: runCatching { doc.get<Long?>("micros.zinc")?.toDouble() }.getOrNull()
                ?: runCatching { doc.get<Long?>("zinc")?.toDouble() }.getOrNull()
                ?: 0.0

            RegistroNutrientes(
                id = id,
                childId = childId,
                fecha = fecha,
                comida = comida,
                alimento = alimento,
                macros = Macronutrientes(cal, prot, gras, carb),
                micros = Micronutrientes(hierro, calcio, vitA, vitC, zinc),
                notas = notas
            )
        }.getOrNull()
    }

    fun observarPorHijo(childId: String?): Flow<List<RegistroNutrientes>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return try {
            coleccion(childId).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    parseRegistro(doc)
                }.sortedByDescending { it.fecha }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observarPorHijoYFecha(childId: String?, fecha: String): Flow<List<RegistroNutrientes>> {
        val uid = auth.currentUser?.uid ?: return flowOf(emptyList())
        val dateSlash = com.example.nutriia.util.DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(fecha)
        val dateIso = com.example.nutriia.util.DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(fecha)

        return try {
            coleccion(childId).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    val reg = parseRegistro(doc)
                    if (reg != null && (reg.fecha == fecha || reg.fecha == dateSlash || reg.fecha == dateIso)) reg else null
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun obtenerPorHijoYFecha(childId: String?, fecha: String): List<RegistroNutrientes> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        val dateSlash = com.example.nutriia.util.DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(fecha)
        val dateIso = com.example.nutriia.util.DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(fecha)

        return try {
            val snapshot = coleccion(childId).get()
            snapshot.documents.mapNotNull { doc ->
                val reg = parseRegistro(doc)
                if (reg != null && (reg.fecha == fecha || reg.fecha == dateSlash || reg.fecha == dateIso)) reg else null
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
