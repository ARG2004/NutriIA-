package com.example.nutriia.ginecologo

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GinecologoRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val colGinecologosPublicos = db.collection("ginecologos_publicos")
    private val colVinculaciones       = db.collection("vinculaciones_embarazo")

    // ═════════════════════════════════════════════════════════════════════════
    // GINECÓLOGO — Perfil y Código
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun publicarPerfilGinecologo(
        nombre:       String,
        especialidad: String,
        cedula:       String,
        email:        String
    ): Result<GinecologoPublico> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            val existente = colGinecologosPublicos.document(uid).get().await()
            val codigo = existente.getString("codigo")?.takeIf { it.isNotBlank() }
                ?: generarCodigo(nombre)

            val perfil = GinecologoPublico(
                uid          = uid,
                nombre       = nombre,
                especialidad = especialidad,
                cedula       = cedula,
                codigo       = codigo,
                email        = email.trim().lowercase()
            )
            colGinecologosPublicos.document(uid).set(perfil.toMap()).await()
            Result.success(perfil)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarGinecologoPorCodigo(codigo: String): Result<GinecologoPublico?> {
        val q = codigo.trim().uppercase()
        if (q.isEmpty()) return Result.success(null)

        return try {
            val snap = colGinecologosPublicos
                .whereEqualTo("codigo", q)
                .limit(1)
                .get().await()

            val perfil = snap.documents.firstOrNull()?.data?.let { GinecologoPublico.fromMap(it) }
            Result.success(perfil)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarGinecologoPorEmail(email: String): Result<GinecologoPublico?> {
        val e = email.trim().lowercase()
        if (e.isEmpty()) return Result.success(null)

        return try {
            val snap = colGinecologosPublicos
                .whereEqualTo("email", e)
                .limit(1)
                .get().await()

            val perfil = snap.documents.firstOrNull()?.data?.let { GinecologoPublico.fromMap(it) }
            Result.success(perfil)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DIRECTORIO
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun listarGinecologos(limite: Long = 50): Result<List<GinecologoPublico>> {
        return try {
            val snap = colGinecologosPublicos.limit(limite).get().await()
            val lista = snap.documents.mapNotNull { doc ->
                doc.data?.let { GinecologoPublico.fromMap(it) }
            }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarGinecologosEnDirectorio(query: String): Result<List<GinecologoPublico>> {
        val q = query.trim()
        if (q.isBlank()) return listarGinecologos()

        return try {
            // Intento búsqueda por nombre
            val porNombre = colGinecologosPublicos
                .orderBy("nombre")
                .startAt(q)
                .endAt(q + "\uF8FF")
                .limit(20)
                .get().await()
                .documents.mapNotNull { it.data?.let { d -> GinecologoPublico.fromMap(d) } }

            Result.success(porNombre)
        } catch (e: Exception) {
            // Fallback manual si falla el índice compuesto
            try {
                val todos = listarGinecologos().getOrDefault(emptyList())
                val filtrado = todos.filter {
                    it.nombre.contains(q, ignoreCase = true) || it.especialidad.contains(q, ignoreCase = true)
                }
                Result.success(filtrado)
            } catch (e2: Exception) {
                Result.failure(e2)
            }
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // GESTIÓN DE VINCULACIÓN
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun solicitarVinculacion(
        ginecologo: GinecologoPublico,
        mamaNombre: String
    ): Result<VinculacionEmbarazo> {
        val mamaUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No auth"))

        return try {
            val docId = VinculacionEmbarazo.docId(ginecologo.uid, mamaUid)

            val vinculacion = VinculacionEmbarazo(
                id               = docId,
                ginecologoUid    = ginecologo.uid,
                ginecologoNombre = ginecologo.nombre,
                mamaUid          = mamaUid,
                mamaNombre       = mamaNombre,
                estado           = EstadoVinculacionEmbarazo.PENDIENTE,
                creadoEn         = Timestamp.now()
            )

            colVinculaciones.document(docId).set(vinculacion.toMap()).await()
            Result.success(vinculacion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun responderSolicitud(vinculacionId: String, aceptar: Boolean): Result<Unit> {
        return try {
            val nuevoEstado = if (aceptar) EstadoVinculacionEmbarazo.ACTIVO.name else EstadoVinculacionEmbarazo.RECHAZADO.name
            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "estado"        to nuevoEstado,
                    "actualizadoEn" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revocarVinculacion(vinculacionId: String): Result<Unit> {
        return try {
            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "estado"        to EstadoVinculacionEmbarazo.REVOCADO.name,
                    "actualizadoEn" to Timestamp.now()
                )
            ).await()
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
            val snap = colVinculaciones.document(vinculacionId).get().await()
            val vinc = snap.data?.let { VinculacionEmbarazo.fromMap(snap.id, it) }
                ?: return Result.failure(Exception("Vinculación no encontrada"))

            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "proximaCitaFecha"  to fecha,
                    "proximaCitaHora"   to hora,
                    "proximaCitaMotivo" to motivo,
                    "proximaCitaTipo"   to tipo,
                    "actualizadoEn"     to Timestamp.now()
                )
            ).await()

            val refCitas = db.collection("usuarios")
                .document(vinc.mamaUid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("citas")
            val newDoc = refCitas.document()
            val cita = CitaEmbarazo(
                id = newDoc.id,
                fecha = fecha,
                hora = hora,
                motivo = motivo,
                tipo = tipo,
                ginecologoUid = vinc.ginecologoUid,
                ginecologoNombre = vinc.ginecologoNombre
            )
            newDoc.set(cita.toMap()).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelarCita(vinculacionId: String): Result<Unit> {
        return try {
            val snap = colVinculaciones.document(vinculacionId).get().await()
            val vinc = snap.data?.let { VinculacionEmbarazo.fromMap(snap.id, it) }
                ?: return Result.failure(Exception("Vinculación no encontrada"))

            val oldFecha = vinc.proximaCitaFecha
            val oldHora = vinc.proximaCitaHora

            colVinculaciones.document(vinculacionId).update(
                mapOf(
                    "proximaCitaFecha"  to "",
                    "proximaCitaHora"   to "",
                    "proximaCitaMotivo" to "",
                    "proximaCitaTipo"   to "",
                    "actualizadoEn"     to Timestamp.now()
                )
            ).await()

            if (oldFecha.isNotBlank()) {
                val query = db.collection("usuarios")
                    .document(vinc.mamaUid)
                    .collection("perfilEmbarazo")
                    .document("unico")
                    .collection("citas")
                    .whereEqualTo("fecha", oldFecha)
                    .whereEqualTo("hora", oldHora)
                    .get()
                    .await()
                for (doc in query.documents) {
                    doc.reference.delete().await()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // OBSERVADORES REAL-TIME
    // ═════════════════════════════════════════════════════════════════════════

    fun observarVinculacionDeLaMama(): Flow<VinculacionEmbarazo?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val listener = colVinculaciones
            .whereEqualTo("mamaUid", uid)
            .orderBy("creadoEn", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("GineRepo", "Error observando vinculación mama", err)
                    return@addSnapshotListener
                }
                val vinculacion = snap?.documents?.firstOrNull()?.let { doc ->
                    VinculacionEmbarazo.fromMap(doc.id, doc.data ?: emptyMap())
                }
                trySend(vinculacion)
            }
        awaitClose { listener.remove() }
    }.catch { emit(null) }

    fun observarCitasDeLaMama(): Flow<List<CitaEmbarazo>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("usuarios")
            .document(uid)
            .collection("perfilEmbarazo")
            .document("unico")
            .collection("citas")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.data?.let { CitaEmbarazo.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }

    fun observarVinculacionesDelGinecologo(): Flow<List<VinculacionEmbarazo>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val listener = colVinculaciones
            .whereEqualTo("ginecologoUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val lista = snap?.documents?.mapNotNull { doc ->
                    VinculacionEmbarazo.fromMap(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }

    // ═════════════════════════════════════════════════════════════════════════
    // PERFIL PROPIO
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun obtenerMiPerfilPublico(): Result<GinecologoPublico?> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No auth"))
        return try {
            val doc = colGinecologosPublicos.document(uid).get().await()
            val perfil = if (doc.exists()) doc.data?.let { GinecologoPublico.fromMap(it) } else null
            Result.success(perfil)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generarCodigo(nombre: String): String {
        val prefijo = nombre.take(4).uppercase().filter { it.isLetter() }.padEnd(4, 'X')
        val sufijo  = UUID.randomUUID().toString().take(5).uppercase()
        return "GINE-$prefijo-$sufijo"
    }
}
