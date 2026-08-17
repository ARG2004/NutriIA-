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
                        val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
                        val fecha = runCatching { doc.get<String?>("fecha") }.getOrNull() ?: ""
                        val nombreMedico = runCatching { doc.get<String?>("nombreMedico") }.getOrNull() ?: ""
                        val motivo = runCatching { doc.get<String?>("motivo") }.getOrNull() ?: ""
                        val notasTexto = runCatching { doc.get<String?>("notas") }.getOrNull() 
                            ?: runCatching { doc.get<String?>("texto") }.getOrNull() 
                            ?: ""
                        val proxCita = runCatching { doc.get<String?>("proximaCita") }.getOrNull() ?: ""
                        val peso = runCatching { doc.get<Double?>("pesoEnConsulta") }.getOrNull()
                            ?: runCatching { doc.get<Long?>("pesoEnConsulta")?.toDouble() }.getOrNull()
                            ?: 0.0
                        val talla = runCatching { doc.get<Double?>("tallaEnConsulta") }.getOrNull()
                            ?: runCatching { doc.get<Long?>("tallaEnConsulta")?.toDouble() }.getOrNull()
                            ?: 0.0
                        val tipo = runCatching { doc.get<String?>("tipo") }.getOrNull() ?: "consulta"
                        val autorUid = runCatching { doc.get<String?>("autorUid") }.getOrNull() ?: ""
                        val autorNombre = runCatching { doc.get<String?>("autorNombre") }.getOrNull() ?: "Nutriólogo"
                        val cId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: childId

                        Consulta(
                            id = id,
                            fecha = fecha,
                            nombreMedico = nombreMedico,
                            motivo = motivo,
                            notas = notasTexto,
                            texto = notasTexto,
                            proximaCita = proxCita,
                            pesoEnConsulta = peso,
                            tallaEnConsulta = talla,
                            tipo = tipo,
                            autorUid = autorUid,
                            autorNombre = autorNombre,
                            childId = cId
                        )
                    }.getOrNull()
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }
}
