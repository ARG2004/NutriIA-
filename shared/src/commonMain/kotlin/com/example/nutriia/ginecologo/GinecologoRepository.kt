package com.example.nutriia.ginecologo

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class GinecologoRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private val colGinecologosPublicos get() = db.collection("ginecologos_publicos")
    private val colVinculaciones get() = db.collection("vinculaciones_embarazo")

    private fun generarCodigo(nombre: String): String {
        val prefijo = nombre.take(4).uppercase().filter { it.isLetter() }.padEnd(4, 'X')
        val sufijo  = generateUUID().take(5).uppercase()
        return "GINE-$prefijo-$sufijo"
    }

    suspend fun publicarPerfilGinecologo(
        nombre:       String,
        especialidad: String,
        cedula:       String,
        email:        String
    ): Result<GinecologoPublico> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            val docRef = colGinecologosPublicos.document(uid)
            val snap = docRef.get()
            val existingCode = if (snap.exists) {
                snap.get<String?>("codigo")
            } else null

            val codigo = if (!existingCode.isNullOrBlank()) existingCode else generarCodigo(nombre)

            val perfil = GinecologoPublico(
                uid          = uid,
                nombre       = nombre,
                especialidad = especialidad,
                cedula       = cedula,
                codigo       = codigo,
                email        = email.trim().lowercase()
            )
            docRef.set(perfil.toMap())
            Result.success(perfil)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarGinecologoPorCodigo(codigo: String): Result<GinecologoPublico?> {
        val q = codigo.trim().uppercase()
        if (q.isEmpty()) return Result.success(null)

        return try {
            val snap = colGinecologosPublicos.where { "codigo".equalTo(q) }.get()
            val doc = snap.documents.firstOrNull()
            if (doc != null && doc.exists) {
                val perfil = doc.data<GinecologoPublico>()
                Result.success(if (perfil.uid.isBlank()) perfil.copy(uid = doc.id) else perfil)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarGinecologoPorEmail(email: String): Result<GinecologoPublico?> {
        val e = email.trim().lowercase()
        if (e.isEmpty()) return Result.success(null)

        return try {
            val snap = colGinecologosPublicos.where { "email".equalTo(e) }.get()
            val doc = snap.documents.firstOrNull()
            if (doc != null && doc.exists) {
                val perfil = doc.data<GinecologoPublico>()
                Result.success(if (perfil.uid.isBlank()) perfil.copy(uid = doc.id) else perfil)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarGinecologos(limite: Long = 50): Result<List<GinecologoPublico>> {
        return try {
            val snap = colGinecologosPublicos.get()
            val lista = snap.documents.take(limite.toInt()).mapNotNull { doc ->
                runCatching { 
                    val p = doc.data<GinecologoPublico>()
                    if (p.uid.isBlank()) p.copy(uid = doc.id) else p 
                }.getOrNull()
            }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarGinecologosEnDirectorio(query: String): Result<List<GinecologoPublico>> {
        val q = query.trim()
        return try {
            val snap = colGinecologosPublicos.get()
            val todos = snap.documents.mapNotNull { doc ->
                runCatching { 
                    val p = doc.data<GinecologoPublico>()
                    if (p.uid.isBlank()) p.copy(uid = doc.id) else p
                }.getOrNull()
            }
            if (q.isBlank()) return Result.success(todos)
            val filtrados = todos.filter {
                it.nombre.contains(q, ignoreCase = true) || it.especialidad.contains(q, ignoreCase = true)
            }
            Result.success(filtrados)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun solicitarVinculacion(
        ginecologo: GinecologoPublico,
        mamaNombre: String
    ): Result<VinculacionEmbarazo> {
        val mamaUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        val docId = VinculacionEmbarazo.docId(ginecologo.uid, mamaUid)

        return try {
            val vinculacion = VinculacionEmbarazo(
                id               = docId,
                ginecologoUid    = ginecologo.uid,
                ginecologoNombre = ginecologo.nombre,
                mamaUid          = mamaUid,
                mamaNombre       = mamaNombre,
                estado           = EstadoVinculacionEmbarazo.PENDIENTE,
                creadoEn         = currentTimeMillis()
            )
            colVinculaciones.document(docId).set(vinculacion.toMap())
            Result.success(vinculacion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun responderSolicitud(vinculacionId: String, aceptar: Boolean): Result<Unit> {
        return try {
            val nuevoEstado = if (aceptar) EstadoVinculacionEmbarazo.ACTIVO else EstadoVinculacionEmbarazo.RECHAZADO
            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "estado" to nuevoEstado.name,
                    "respondidoEn" to currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revocarVinculacion(vinculacionId: String): Result<Unit> {
        return try {
            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "estado" to EstadoVinculacionEmbarazo.REVOCADO.name,
                    "revocadoEn" to currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun agendarCita(
        vinculacionId: String,
        fecha: String,
        hora: String,
        motivo: String,
        tipo: String
    ): Result<Unit> {
        return try {
            val docSnap = colVinculaciones.document(vinculacionId).get()
            if (!docSnap.exists) return Result.failure(Exception("Vinculación no encontrada"))
            val vinc = docSnap.data<VinculacionEmbarazo>().copy(id = docSnap.id)

            val citaId = generateUUID()
            val cita = CitaEmbarazo(
                id = citaId,
                fecha = fecha,
                hora = hora,
                motivo = motivo,
                tipo = tipo,
                ginecologoUid = vinc.ginecologoUid,
                ginecologoNombre = vinc.ginecologoNombre
            )

            colVinculaciones.document(vinculacionId).collection("citas").document(citaId).set(cita.toMap())

            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "proximaCitaFecha" to fecha,
                    "proximaCitaHora" to hora,
                    "proximaCitaMotivo" to motivo,
                    "proximaCitaTipo" to tipo,
                    "estado" to EstadoVinculacionEmbarazo.ACTIVO.name
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelarCita(vinculacionId: String): Result<Unit> {
        return try {
            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "proximaCitaFecha" to "",
                    "proximaCitaHora" to "",
                    "proximaCitaMotivo" to "",
                    "proximaCitaTipo" to ""
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarVinculacionDeLaMama(): Flow<VinculacionEmbarazo?> {
        val mamaUid = auth.currentUser?.uid ?: return flowOf(null)
        return try {
            colVinculaciones.where { "mamaUid".equalTo(mamaUid) }.snapshots.map { snap ->
                snap.documents.mapNotNull { doc ->
                    runCatching { doc.data<VinculacionEmbarazo>().copy(id = doc.id) }.getOrNull()
                }.firstOrNull { it.estado != EstadoVinculacionEmbarazo.REVOCADO }
            }
        } catch (e: Exception) {
            flowOf(null)
        }
    }

    fun observarCitasDeLaMama(): Flow<List<CitaEmbarazo>> {
        val mamaUid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return try {
            colVinculaciones.where { "mamaUid".equalTo(mamaUid) }.snapshots.map { snap ->
                snap.documents.mapNotNull { doc ->
                    val vinc = runCatching { doc.data<VinculacionEmbarazo>().copy(id = doc.id) }.getOrNull()
                    if (vinc != null && vinc.proximaCitaFecha.isNotBlank()) {
                        CitaEmbarazo(
                            id = vinc.id,
                            fecha = vinc.proximaCitaFecha,
                            hora = vinc.proximaCitaHora,
                            motivo = vinc.proximaCitaMotivo,
                            tipo = vinc.proximaCitaTipo,
                            ginecologoUid = vinc.ginecologoUid,
                            ginecologoNombre = vinc.ginecologoNombre
                        )
                    } else null
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observarVinculacionesDelGinecologo(): Flow<List<VinculacionEmbarazo>> {
        val gineUid = auth.currentUser?.uid ?: return flowOf(emptyList())
        return try {
            colVinculaciones.where { "ginecologoUid".equalTo(gineUid) }.snapshots.map { snap ->
                snap.documents.mapNotNull { doc ->
                    runCatching { doc.data<VinculacionEmbarazo>().copy(id = doc.id) }.getOrNull()
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun obtenerMiPerfilPublico(): Result<GinecologoPublico?> {
        val uid = auth.currentUser?.uid ?: return Result.success(null)
        return try {
            val doc = colGinecologosPublicos.document(uid).get()
            if (doc.exists) {
                val perfil = doc.data<GinecologoPublico>()
                Result.success(if (perfil.uid.isBlank()) perfil.copy(uid = doc.id) else perfil)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
