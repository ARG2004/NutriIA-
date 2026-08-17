package com.example.nutriia.pediatra

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.utils.FechaUtils
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

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
    val autorNombre:     String = "Nutriólogo",
    val childId:         String = "",
    val texto:           String = ""
)

typealias PediatraNutriologoRepository = PediatraRepository

class PediatraRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun consultasCol(ownerUid: String, childId: String) =
        db.collection("usuarios")
            .document(ownerUid.ifBlank { auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado") })
            .collection("hijos")
            .document(childId)
            .collection("consultas")

    suspend fun guardarNotaNutriologo(
        padreUid:         String,
        childId:          String,
        nota:             Consulta,
        nutriologoNombre: String
    ): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid ?: ""
            val id = if (nota.id.isBlank()) generateUUID() else nota.id
            val fecha = if (nota.fecha.isBlank()) FechaUtils.fechaActual() else nota.fecha
            val notasTexto = nota.notas.ifBlank { nota.texto }

            val data = mapOf<String, Any?>(
                "id" to id,
                "childId" to childId,
                "fecha" to fecha,
                "nombreMedico" to nutriologoNombre,
                "motivo" to nota.motivo.ifBlank { "Nota de especialista" },
                "notas" to notasTexto,
                "texto" to notasTexto,
                "proximaCita" to nota.proximaCita,
                "pesoEnConsulta" to nota.pesoEnConsulta,
                "tallaEnConsulta" to nota.tallaEnConsulta,
                "tipo" to "nota_nutriologo",
                "autorUid" to currentUid,
                "autorNombre" to nutriologoNombre,
                "creadoEnMillis" to currentTimeMillis()
            )

            consultasCol(padreUid, childId).document(id).set(data)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun guardarConsulta(
        ownerUid: String,
        childId:  String,
        consulta: Consulta
    ): Result<String> {
        return try {
            val id = if (consulta.id.isBlank()) generateUUID() else consulta.id
            val data = mapOf<String, Any?>(
                "id" to id,
                "childId" to childId,
                "fecha" to consulta.fecha.ifBlank { FechaUtils.fechaActual() },
                "nombreMedico" to consulta.nombreMedico,
                "motivo" to consulta.motivo,
                "notas" to consulta.notas.ifBlank { consulta.texto },
                "proximaCita" to consulta.proximaCita,
                "pesoEnConsulta" to consulta.pesoEnConsulta,
                "tallaEnConsulta" to consulta.tallaEnConsulta,
                "tipo" to consulta.tipo,
                "autorUid" to (auth.currentUser?.uid ?: ""),
                "autorNombre" to consulta.autorNombre,
                "creadoEnMillis" to currentTimeMillis()
            )
            consultasCol(ownerUid, childId).document(id).set(data)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarConsultas(padreUid: String, childId: String): Flow<List<Consulta>> {
        return try {
            consultasCol(padreUid, childId).snapshots.map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    runCatching {
                        val data = doc.data<Map<String, Any?>>()
                        val notasTexto = data["notas"] as? String ?: (data["texto"] as? String ?: "")
                        Consulta(
                            id = data["id"] as? String ?: doc.id,
                            fecha = data["fecha"] as? String ?: "",
                            nombreMedico = data["nombreMedico"] as? String ?: "",
                            motivo = data["motivo"] as? String ?: "",
                            notas = notasTexto,
                            texto = notasTexto,
                            proximaCita = data["proximaCita"] as? String ?: "",
                            pesoEnConsulta = (data["pesoEnConsulta"] as? Double) ?: ((data["pesoEnConsulta"] as? Long)?.toDouble() ?: 0.0),
                            tallaEnConsulta = (data["tallaEnConsulta"] as? Double) ?: ((data["tallaEnConsulta"] as? Long)?.toDouble() ?: 0.0),
                            tipo = data["tipo"] as? String ?: "consulta",
                            autorUid = data["autorUid"] as? String ?: "",
                            autorNombre = data["autorNombre"] as? String ?: "Nutriólogo",
                            childId = data["childId"] as? String ?: childId
                        )
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }
}
