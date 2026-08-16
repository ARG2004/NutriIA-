package com.example.nutriia.vinculacion

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class VinculacionRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val colVinculaciones       = db.collection("vinculaciones")
    private val colNutriologosPublicos = db.collection("nutriologos_publicos")

    // ═════════════════════════════════════════════════════════════════════════
    // NUTRIÓLOGO — Perfil y Código
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun publicarPerfilNutriologo(
        nombre:       String,
        especialidad: String,
        cedula:       String,
        email:        String
    ): Result<NutriologoPublico> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

        return try {
            val existente = colNutriologosPublicos.document(uid).get().await()
            val codigo = existente.getString("codigo")?.takeIf { it.isNotBlank() }
                ?: generarCodigo(nombre)

            val perfil = NutriologoPublico(
                uid          = uid,
                nombre       = nombre,
                especialidad = especialidad,
                cedula       = cedula,
                codigo       = codigo,
                email        = email.trim().lowercase()
            )
            colNutriologosPublicos.document(uid).set(perfil.toMap()).await()
            Result.success(perfil)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun esEspecialidadGinecologica(especialidad: String): Boolean {
        val esp = especialidad.lowercase()
        return esp.contains("ginec") || esp.contains("obstet")
    }

    suspend fun buscarNutriologoPorCodigo(codigo: String): Result<NutriologoPublico?> {
        val q = codigo.trim().uppercase()
        if (q.isEmpty()) return Result.success(null)

        return try {
            val snap = colNutriologosPublicos
                .whereEqualTo("codigo", q)
                .limit(1)
                .get().await()

            val doc = snap.documents.firstOrNull()
            val perfil = doc?.let { NutriologoPublico.fromMap(it.data ?: emptyMap(), it.id) }
            if (perfil != null && esEspecialidadGinecologica(perfil.especialidad)) {
                Result.success(null)
            } else {
                Result.success(perfil)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarNutriologoPorEmail(email: String): Result<NutriologoPublico?> {
        val e = email.trim().lowercase()
        if (e.isEmpty()) return Result.success(null)

        return try {
            val snap = colNutriologosPublicos
                .whereEqualTo("email", e)
                .limit(1)
                .get().await()

            val doc = snap.documents.firstOrNull()
            val perfil = doc?.let { NutriologoPublico.fromMap(it.data ?: emptyMap(), it.id) }
            if (perfil != null && esEspecialidadGinecologica(perfil.especialidad)) {
                Result.success(null)
            } else {
                Result.success(perfil)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // DIRECTORIO
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun listarNutriologos(limite: Long = 50): Result<List<NutriologoPublico>> {
        if (auth.currentUser == null) return Result.failure(IllegalStateException("No auth"))

        return try {
            val snap = colNutriologosPublicos.limit(limite).get().await()
            val lista = snap.documents.mapNotNull { doc ->
                doc.data?.let { NutriologoPublico.fromMap(it, doc.id) }
            }.filter { !esEspecialidadGinecologica(it.especialidad) }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarNutriologosEnDirectorio(query: String): Result<List<NutriologoPublico>> {
        val q = query.trim()
        if (q.isBlank()) return listarNutriologos()

        return try {
            val snap = colNutriologosPublicos
                .orderBy("nombre")
                .startAt(q)
                .endAt(q + "\uF8FF")
                .limit(20)
                .get().await()

            val porNombre = snap.documents.mapNotNull { doc ->
                doc.data?.let { NutriologoPublico.fromMap(it, doc.id) }
            }.filter { !esEspecialidadGinecologica(it.especialidad) }

            Result.success(porNombre)
        } catch (e: Exception) {
            try {
                val todos = listarNutriologos().getOrDefault(emptyList())
                val filtrado = todos.filter {
                    (it.nombre.contains(q, ignoreCase = true) || it.especialidad.contains(q, ignoreCase = true)) &&
                            !esEspecialidadGinecologica(it.especialidad)
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
        nutriologo:  NutriologoPublico,
        padreNombre: String,
        childId:     String,
        childNombre: String
    ): Result<Vinculacion> {
        val padreUid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No auth"))

        return try {
            val docId = "${nutriologo.uid}_${padreUid}_$childId"

            val vinculacion = Vinculacion(
                id               = docId,
                nutriologoUid    = nutriologo.uid,
                nutriologoNombre = nutriologo.nombre,
                padreUid         = padreUid,
                padreNombre      = padreNombre,
                childId          = childId,
                childNombre      = childNombre,
                estado           = EstadoVinculacion.PENDIENTE,
                creadoEn         = Timestamp.now()
            )

            Log.d("VINCULACION", "docId=$docId")
            Log.d("VINCULACION", "authUid=${auth.currentUser?.uid}")
            Log.d("VINCULACION", "padreUid en data=${vinculacion.padreUid}")
            Log.d("VINCULACION", "estado=${vinculacion.estado.name}")
            Log.d("VINCULACION", "map completo=${vinculacion.toMap()}")

            colVinculaciones.document(docId).set(vinculacion.toMap()).await()
            Result.success(vinculacion)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun responderSolicitud(vinculacionId: String, aceptar: Boolean): Result<Unit> {
        return try {
            val nuevoEstado = if (aceptar) "ACTIVO" else "RECHAZADO"
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
                    "estado"        to "REVOCADO",
                    "actualizadoEn" to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    // OBSERVADORES REAL-TIME
    // ═════════════════════════════════════════════════════════════════════════

    fun observarVinculacionesDelNutriologo(): Flow<List<Vinculacion>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val listener = colVinculaciones
            .whereEqualTo("nutriologoUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val lista = snap?.documents?.mapNotNull { doc ->
                    Vinculacion.fromMap(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }

    fun observarVinculacionesDelPadre(): Flow<List<Vinculacion>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            close()
            return@callbackFlow
        }

        val listener = colVinculaciones
            .whereEqualTo("padreUid", uid)
            .addSnapshotListener { snap, err ->
                if (err != null) return@addSnapshotListener
                val lista = snap?.documents?.mapNotNull { doc ->
                    Vinculacion.fromMap(doc.id, doc.data ?: emptyMap())
                } ?: emptyList()
                trySend(lista)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }

    // ── NUEVO: listener en tiempo real para un hijo específico ────────────────
    fun observarHijo(padreUid: String, childId: String): Flow<Map<String, Any?>?> = callbackFlow {
        val ref = db.collection("usuarios")
            .document(padreUid)
            .collection("hijos")
            .document(childId)

        val listener = ref.addSnapshotListener { snap, err ->
            if (err != null) {
                trySend(null)
                return@addSnapshotListener
            }
            trySend(if (snap?.exists() == true) snap.data else null)
        }
        awaitClose { listener.remove() }
    }.catch { emit(null) }

    // ═════════════════════════════════════════════════════════════════════════
    // PERFIL PROPIO
    // ═════════════════════════════════════════════════════════════════════════

    suspend fun obtenerMiPerfilPublico(): Result<NutriologoPublico?> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("No auth"))
        return try {
            val source = if (com.example.nutriia.offline.OfflineManager.hayConexion()) {
                com.google.firebase.firestore.Source.DEFAULT
            } else {
                com.google.firebase.firestore.Source.CACHE
            }
            val doc = colNutriologosPublicos.document(uid).get(source).await()
            val perfil = if (doc.exists()) doc.data?.let { NutriologoPublico.fromMap(it, doc.id) } else null
            Result.success(perfil)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun generarCodigo(nombre: String): String {
        val prefijo = nombre.take(4).uppercase().filter { it.isLetter() }.padEnd(4, 'X')
        val sufijo  = UUID.randomUUID().toString().take(5).uppercase()
        return "NUT-$prefijo-$sufijo"
    }
}