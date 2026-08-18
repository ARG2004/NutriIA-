package com.example.nutriia.vinculacion

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

@OptIn(ExperimentalCoroutinesApi::class)
class VinculacionRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private val colVinculaciones get() = db.collection("vinculaciones")
    private val colNutriologosPublicos get() = db.collection("nutriologos_publicos")

    private suspend fun getAuthUser(): dev.gitlive.firebase.auth.FirebaseUser? {
        auth.currentUser?.let { return it }
        return try {
            withTimeoutOrNull(3000L) {
                auth.authStateChanged.filterNotNull().first()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun generarCodigo(nombre: String): String {
        val limpio = nombre.trim().filter { it.isLetter() }.take(4).uppercase()
        val random = (1000..9999).random()
        return "NUT-$limpio-$random"
    }

    fun observarHijo(padreUid: String, childId: String): Flow<Map<String, Any?>?> {
        if (childId.isBlank()) return flowOf(null)
        if (padreUid.isNotBlank()) {
            return try {
                db.collection("usuarios")
                    .document(padreUid)
                    .collection("hijos")
                    .document(childId)
                    .snapshots
                    .map { if (it.exists) it.data() else null }
            } catch (e: Exception) {
                flowOf(null)
            }
        }
        return auth.authStateChanged.flatMapLatest { user ->
            val uid = user?.uid ?: ""
            if (uid.isBlank()) flowOf(null)
            else {
                try {
                    db.collection("usuarios")
                        .document(uid)
                        .collection("hijos")
                        .document(childId)
                        .snapshots
                        .map { if (it.exists) it.data() else null }
                } catch (e: Exception) {
                    flowOf(null)
                }
            }
        }
    }

    suspend fun publicarPerfilNutriologo(
        nombre:       String,
        especialidad: String,
        cedula:       String,
        email:        String
    ): Result<NutriologoPublico> {
        val user = getAuthUser() ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        val uid = user.uid

        return try {
            val docRef = colNutriologosPublicos.document(uid)
            val snap = docRef.get()
            val existingCode = if (snap.exists) {
                snap.data<Map<String, Any?>>()["codigo"] as? String
            } else null

            val codigo = if (!existingCode.isNullOrBlank()) existingCode else generarCodigo(nombre)

            val perfil = NutriologoPublico(
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

    suspend fun obtenerMiPerfilPublico(): Result<NutriologoPublico?> {
        val user = getAuthUser() ?: return Result.success(null)
        val uid = user.uid
        return try {
            val doc = colNutriologosPublicos.document(uid).get()
            if (doc.exists) {
                Result.success(NutriologoPublico.fromMap(doc.data(), doc.id))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun esEspecialidadGinecologica(especialidad: String): Boolean {
        val esp = especialidad.lowercase()
        return esp.contains("ginec") || esp.contains("obstet")
    }

    suspend fun buscarNutriologoPorCodigo(codigo: String): Result<NutriologoPublico?> {
        val raw = codigo.trim()
        val q = Regex("NUT-[A-Za-z0-9-]+", RegexOption.IGNORE_CASE).find(raw)?.value?.uppercase() ?: raw.uppercase()
        if (q.isEmpty()) return Result.success(null)

        return try {
            val snap = colNutriologosPublicos.where { "codigo".equalTo(q) }.get()
            val doc = snap.documents.firstOrNull()
            if (doc != null && doc.exists) {
                val perfil = NutriologoPublico.fromMap(doc.data(), doc.id)
                if (esEspecialidadGinecologica(perfil.especialidad)) {
                    Result.success(null)
                } else {
                    Result.success(perfil)
                }
            } else {
                val docById = colNutriologosPublicos.document(q).get()
                if (docById.exists) {
                    val perfil = NutriologoPublico.fromMap(docById.data(), docById.id)
                    if (esEspecialidadGinecologica(perfil.especialidad)) Result.success(null)
                    else Result.success(perfil)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarNutriologoPorEmail(email: String): Result<NutriologoPublico?> {
        val e = email.trim().lowercase()
        if (e.isEmpty()) return Result.success(null)

        return try {
            val snap = colNutriologosPublicos.where { "email".equalTo(e) }.get()
            val doc = snap.documents.firstOrNull()
            if (doc != null && doc.exists) {
                val perfil = NutriologoPublico.fromMap(doc.data(), doc.id)
                if (esEspecialidadGinecologica(perfil.especialidad)) {
                    Result.success(null)
                } else {
                    Result.success(perfil)
                }
            } else {
                val allSnap = colNutriologosPublicos.get()
                val found = allSnap.documents.mapNotNull { docItem ->
                    runCatching { NutriologoPublico.fromMap(docItem.data(), docItem.id) }.getOrNull()
                }.firstOrNull { it.email.trim().equals(e, ignoreCase = true) }
                if (found != null && !esEspecialidadGinecologica(found.especialidad)) {
                    Result.success(found)
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun listarNutriologos(limite: Long = 50): Result<List<NutriologoPublico>> {
        return try {
            val snap = colNutriologosPublicos.get()
            val lista = snap.documents.take(limite.toInt()).mapNotNull { doc ->
                runCatching { NutriologoPublico.fromMap(doc.data(), doc.id) }.getOrNull()
            }.filter { !esEspecialidadGinecologica(it.especialidad) }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun buscarNutriologosEnDirectorio(query: String): Result<List<NutriologoPublico>> {
        val q = query.trim()
        return try {
            val snap = colNutriologosPublicos.get()
            val todos = snap.documents.mapNotNull { doc ->
                runCatching { NutriologoPublico.fromMap(doc.data(), doc.id) }.getOrNull()
            }.filter { !esEspecialidadGinecologica(it.especialidad) }
            if (q.isBlank()) return Result.success(todos)
            val filtrados = todos.filter {
                it.nombre.contains(q, ignoreCase = true) || it.especialidad.contains(q, ignoreCase = true) || it.codigo.contains(q, ignoreCase = true) || it.email.contains(q, ignoreCase = true)
            }
            Result.success(filtrados)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun solicitarVinculacion(
        nutriologo:  NutriologoPublico,
        padreNombre: String,
        childId:     String,
        childNombre: String
    ): Result<Vinculacion> {
        val user = getAuthUser() ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        val padreUid = user.uid
        val docId = Vinculacion.docId(nutriologo.uid, padreUid, childId)

        return try {
            val vinc = Vinculacion(
                id               = docId,
                nutriologoUid    = nutriologo.uid,
                nutriologoNombre = nutriologo.nombre,
                padreUid         = padreUid,
                padreNombre      = padreNombre,
                childId          = childId,
                childNombre      = childNombre,
                estado           = EstadoVinculacion.PENDIENTE,
                creadoEn         = currentTimeMillis()
            )
            colVinculaciones.document(docId).set(vinc.toMap())
            Result.success(vinc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun responderSolicitud(vinculacionId: String, aceptar: Boolean): Result<Unit> {
        return try {
            val nuevoEstado = if (aceptar) EstadoVinculacion.ACTIVO else EstadoVinculacion.RECHAZADO
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
                    "estado" to EstadoVinculacion.REVOCADO.name,
                    "revocadoEn" to currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarVinculacionesDelPadre(padreUid: String = ""): Flow<List<Vinculacion>> {
        if (padreUid.isNotBlank()) {
            return try {
                colVinculaciones.where { "padreUid".equalTo(padreUid) }.snapshots.map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        runCatching { Vinculacion.fromMap(doc.id, doc.data()) }.getOrNull()
                    }
                }
            } catch (e: Exception) {
                flowOf(emptyList())
            }
        }
        return auth.authStateChanged.flatMapLatest { user ->
            val uid = user?.uid ?: ""
            if (uid.isBlank()) flowOf(emptyList())
            else {
                try {
                    colVinculaciones.where { "padreUid".equalTo(uid) }.snapshots.map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            runCatching { Vinculacion.fromMap(doc.id, doc.data()) }.getOrNull()
                        }
                    }
                } catch (e: Exception) {
                    flowOf(emptyList())
                }
            }
        }
    }

    fun observarVinculacionesDelNutriologo(nutriologoUid: String = ""): Flow<List<Vinculacion>> {
        if (nutriologoUid.isNotBlank()) {
            return try {
                colVinculaciones.where { "nutriologoUid".equalTo(nutriologoUid) }.snapshots.map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        runCatching { Vinculacion.fromMap(doc.id, doc.data()) }.getOrNull()
                    }
                }
            } catch (e: Exception) {
                flowOf(emptyList())
            }
        }
        return auth.authStateChanged.flatMapLatest { user ->
            val uid = user?.uid ?: ""
            if (uid.isBlank()) flowOf(emptyList())
            else {
                try {
                    colVinculaciones.where { "nutriologoUid".equalTo(uid) }.snapshots.map { snapshot ->
                        snapshot.documents.mapNotNull { doc ->
                            runCatching { Vinculacion.fromMap(doc.id, doc.data()) }.getOrNull()
                        }
                    }
                } catch (e: Exception) {
                    flowOf(emptyList())
                }
            }
        }
    }

    suspend fun guardarPlan(plan: PlanAlimentario): Result<Unit> {
        val user = getAuthUser()
        val defaultPadreUid = user?.uid ?: ""
        return try {
            val id = if (plan.id.isEmpty()) generateUUID() else plan.id
            val docRef = db.collection("usuarios")
                .document(plan.padreUid.ifBlank { defaultPadreUid })
                .collection("hijos")
                .document(plan.childId)
                .collection("planes_alimentarios")
                .document(id)

            docRef.set(plan.toMap())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarPlanActivo(childId: String): Flow<PlanAlimentario?> {
        return auth.authStateChanged.flatMapLatest { user ->
            val uid = user?.uid ?: ""
            if (uid.isBlank() || childId.isBlank()) flowOf(null)
            else {
                try {
                    db.collection("usuarios")
                        .document(uid)
                        .collection("hijos")
                        .document(childId)
                        .collection("planes_alimentarios")
                        .snapshots
                        .map { snapshot ->
                            snapshot.documents.firstOrNull()?.let { doc ->
                                runCatching { PlanAlimentario.fromMap(doc.id, doc.data()) }.getOrNull()
                            }
                        }
                } catch (e: Exception) {
                    flowOf(null)
                }
            }
        }
    }
}
