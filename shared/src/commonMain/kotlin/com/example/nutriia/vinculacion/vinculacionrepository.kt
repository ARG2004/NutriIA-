package com.example.nutriia.vinculacion

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
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
            withTimeoutOrNull(8000L) {
                var currentUser = auth.currentUser
                while (currentUser == null) {
                    val user = auth.authStateChanged.filterNotNull().first()
                    if (user.uid.isNotBlank()) {
                        currentUser = user
                        break
                    }
                    kotlinx.coroutines.delay(200L)
                }
                currentUser
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
            if (uid.isBlank()) {
                flowOf(null)
            } else {
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
        nombre: String,
        especialidad: String,
        cedula: String,
        email: String
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
                uid = uid,
                nombre = nombre,
                especialidad = especialidad,
                cedula = cedula,
                codigo = codigo,
                email = email.trim().lowercase()
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
                // Reintento: el snapshot de nutriologos_publicos puede llegar vacío en el
                // primer intento en iOS (cold start). NO caer a un fallback contra
                // "usuarios" filtrado por campo: esa query siempre es rechazada por
                // Firestore con permission-denied, porque la regla de /usuarios/{uid}
                // depende de la variable de ruta uid (esDueno(uid)) y no puede probarse
                // válida para una query filtrada por otro campo.
                kotlinx.coroutines.delay(1200L)
                val docByIdRetry = colNutriologosPublicos.document(q).get()
                if (docByIdRetry.exists) {
                    val perfil = NutriologoPublico.fromMap(docByIdRetry.data(), docByIdRetry.id)
                    if (esEspecialidadGinecologica(perfil.especialidad)) Result.success(null)
                    else Result.success(perfil)
                } else {
                    val snapRetry = colNutriologosPublicos.where { "codigo".equalTo(q) }.get()
                    val docRetry = snapRetry.documents.firstOrNull()
                    if (docRetry != null && docRetry.exists) {
                        val perfil = NutriologoPublico.fromMap(docRetry.data(), docRetry.id)
                        if (esEspecialidadGinecologica(perfil.especialidad)) Result.success(null)
                        else Result.success(perfil)
                    } else {
                        Result.success(null)
                    }
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
                    // Reintento sobre nutriologos_publicos (mismo motivo que arriba: NO usar
                    // fallback contra "usuarios" filtrado por campo, siempre da permission-denied).
                    kotlinx.coroutines.delay(1200L)
                    val allSnapRetry = colNutriologosPublicos.get()
                    val foundRetry = allSnapRetry.documents.mapNotNull { docItem ->
                        runCatching { NutriologoPublico.fromMap(docItem.data(), docItem.id) }.getOrNull()
                    }.firstOrNull { it.email.trim().equals(e, ignoreCase = true) }
                    if (foundRetry != null && !esEspecialidadGinecologica(foundRetry.especialidad)) {
                        Result.success(foundRetry)
                    } else {
                        Result.success(null)
                    }
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIX iOS: doc.data() usa el decoder genérico de gitlive a Map<String, Any?>, que
    // en Kotlin/Native (iOS) no decodifica igual que en Android (donde hay un camino
    // directo vía el SDK nativo de Firebase Android). En vez de confiar en ese mapa
    // genérico, leemos cada campo por separado con doc.get<String>(field) — reified
    // con tipo concreto, que sí es confiable en ambas plataformas.
    private fun nutriologoDesdeDoc(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): NutriologoPublico? {
        return try {
            val uidCampo = runCatching { doc.get<String?>("uid") }.getOrNull()
            NutriologoPublico(
                uid = uidCampo?.takeIf { it.isNotBlank() } ?: doc.id,
                nombre = runCatching { doc.get<String?>("nombre") }.getOrNull() ?: "",
                especialidad = runCatching { doc.get<String?>("especialidad") }.getOrNull()
                    ?: "Nutrición Pediátrica",
                cedula = runCatching { doc.get<String?>("cedula") }.getOrNull() ?: "",
                codigo = runCatching { doc.get<String?>("codigo") }.getOrNull() ?: "",
                email = runCatching { doc.get<String?>("email") }.getOrNull() ?: ""
            )
        } catch (_: Throwable) {
            null
        }
    }

    suspend fun listarNutriologos(limite: Long = 50): Result<List<NutriologoPublico>> {
        getAuthUser() ?: return Result.failure(IllegalStateException("Usuario no autenticado en Firebase"))

        // NOTA IMPORTANTE (iOS/KMP): .get() puntual en gitlive-firebase puede resolver
        // contra el caché local de Firestore, que en el primer arranque en iOS está vacío
        // — y no lanza error, solo regresa 0 documentos. Por eso usamos el listener en
        // tiempo real (snapshots) y esperamos el primer resultado real del servidor,
        // con timeout, en vez de confiar en un solo .get().
        val maxIntentos = 4
        var intentos = 0
        var ultimoError: Exception? = null
        while (intentos < maxIntentos) {
            try {
                val lista = kotlinx.coroutines.withTimeoutOrNull(4000L) {
                    colNutriologosPublicos.snapshots
                        .map { snapshot ->
                            snapshot.documents.take(limite.toInt()).mapNotNull { doc ->
                                nutriologoDesdeDoc(doc)
                            }.filter { !esEspecialidadGinecologica(it.especialidad) }
                        }
                        .filter { it.isNotEmpty() }
                        .first()
                }

                if (lista != null) return Result.success(lista)

                if (intentos == maxIntentos - 1) {
                    // Último intento agotado: confirmamos con un .get() simple si de
                    // verdad no hay nutriólogos publicados (directorio legítimamente vacío)
                    val snapFinal = colNutriologosPublicos.get()
                    val listaFinal = snapFinal.documents.take(limite.toInt()).mapNotNull { doc ->
                        nutriologoDesdeDoc(doc)
                    }.filter { !esEspecialidadGinecologica(it.especialidad) }
                    return Result.success(listaFinal)
                }

                kotlinx.coroutines.delay(1000L)
                intentos++
            } catch (e: Exception) {
                ultimoError = e
                if (intentos >= maxIntentos - 1) return Result.failure(e)
                kotlinx.coroutines.delay(1000L)
                intentos++
            }
        }
        return ultimoError?.let { Result.failure(it) } ?: Result.success(emptyList())
    }

    suspend fun buscarNutriologosEnDirectorio(query: String): Result<List<NutriologoPublico>> {
        val q = query.trim()
        // Reusar listarNutriologos con retry para obtener la lista base
        val baseResult = listarNutriologos(100)
        if (baseResult.isFailure) return Result.failure(baseResult.exceptionOrNull()!!)
        val todos = baseResult.getOrDefault(emptyList())
        if (q.isBlank()) return Result.success(todos)
        val filtrados = todos.filter {
            it.nombre.contains(q, ignoreCase = true) ||
                    it.especialidad.contains(q, ignoreCase = true) ||
                    it.codigo.contains(q, ignoreCase = true) ||
                    it.email.contains(q, ignoreCase = true)
        }
        return Result.success(filtrados)
    }

    suspend fun solicitarVinculacion(
        nutriologo: NutriologoPublico,
        padreNombre: String,
        childId: String,
        childNombre: String
    ): Result<Vinculacion> {
        val user = getAuthUser() ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
        val padreUid = user.uid
        val docId = Vinculacion.docId(nutriologo.uid, padreUid, childId)

        return try {
            val vinc = Vinculacion(
                id = docId,
                nutriologoUid = nutriologo.uid,
                nutriologoNombre = nutriologo.nombre,
                padreUid = padreUid,
                padreNombre = padreNombre,
                childId = childId,
                childNombre = childNombre,
                estado = EstadoVinculacion.PENDIENTE,
                creadoEn = currentTimeMillis()
            )
            // FIX iOS: .set(map) usaba el codificador genérico de gitlive para
            // Map<String, Any?>, poco confiable en iOS/Kotlin-Native — podía escribir
            // el documento con campos que no coincidían exactamente con lo que tus
            // reglas de Firestore comparan (ej. padreUid), dando permission-denied
            // aunque las reglas fueran correctas. Ahora mandamos el objeto @Serializable
            // directo, que usa el serializador real generado por el compilador.
            colVinculaciones.document(docId).set(vinc)
            Result.success(vinc)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun responderSolicitud(vinculacionId: String, aceptar: Boolean): Result<Unit> {
        return try {
            val nuevoEstado = if (aceptar) EstadoVinculacion.ACTIVO else EstadoVinculacion.RECHAZADO
            // FIX iOS: .update(vararg Pair) es más confiable que .update(mapOf(...))
            // para el mismo problema de codificación genérica en Kotlin/Native.
            colVinculaciones.document(vinculacionId).update(
                "estado" to nuevoEstado.name,
                "respondidoEn" to currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun revocarVinculacion(vinculacionId: String): Result<Unit> {
        return try {
            colVinculaciones.document(vinculacionId).update(
                "estado" to EstadoVinculacion.REVOCADO.name,
                "revocadoEn" to currentTimeMillis()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIX iOS: mismo problema que con NutriologoPublico — doc.data() (decoder genérico
    // de gitlive a Map<String, Any?>) no decodifica confiablemente en iOS. Leemos cada
    // campo por separado con tipo concreto.
    private fun vinculacionDesdeDoc(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): Vinculacion? {
        return try {
            val estadoTexto = runCatching { doc.get<String?>("estado") }.getOrNull() ?: ""
            val estado = runCatching { EstadoVinculacion.valueOf(estadoTexto) }
                .getOrDefault(EstadoVinculacion.PENDIENTE)

            fun leerMillis(campo: String): Long? {
                runCatching { doc.get<Long?>(campo) }.getOrNull()?.let { return it }
                runCatching {
                    doc.get<dev.gitlive.firebase.firestore.Timestamp?>(campo)?.let {
                        it.seconds * 1000L + it.nanoseconds / 1_000_000L
                    }
                }.getOrNull()?.let { return it }
                return null
            }

            Vinculacion(
                id = doc.id,
                nutriologoUid = runCatching { doc.get<String?>("nutriologoUid") }.getOrNull() ?: "",
                nutriologoNombre = runCatching { doc.get<String?>("nutriologoNombre") }.getOrNull() ?: "",
                padreUid = runCatching { doc.get<String?>("padreUid") }.getOrNull() ?: "",
                padreNombre = runCatching { doc.get<String?>("padreNombre") }.getOrNull() ?: "",
                childId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: "",
                childNombre = runCatching { doc.get<String?>("childNombre") }.getOrNull() ?: "",
                estado = estado,
                creadoEn = leerMillis("creadoEn"),
                actualizadoEn = leerMillis("actualizadoEn") ?: leerMillis("respondidoEn") ?: leerMillis("revocadoEn")
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun observarVinculacionesDelPadre(padreUid: String = ""): Flow<List<Vinculacion>> {
        if (padreUid.isNotBlank()) {
            return try {
                colVinculaciones.where { "padreUid".equalTo(padreUid) }.snapshots.map { snapshot ->
                    snapshot.documents.mapNotNull { doc -> vinculacionDesdeDoc(doc) }
                }
            } catch (e: Exception) {
                flowOf(emptyList())
            }
        }
        return auth.authStateChanged.flatMapLatest { user ->
            val uid = user?.uid ?: ""
            if (uid.isBlank()) {
                flowOf(emptyList())
            } else {
                try {
                    colVinculaciones.where { "padreUid".equalTo(uid) }.snapshots.map { snapshot ->
                        snapshot.documents.mapNotNull { doc -> vinculacionDesdeDoc(doc) }
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
                    snapshot.documents.mapNotNull { doc -> vinculacionDesdeDoc(doc) }
                }
            } catch (e: Exception) {
                flowOf(emptyList())
            }
        }
        return auth.authStateChanged.flatMapLatest { user ->
            val uid = user?.uid ?: ""
            if (uid.isBlank()) {
                flowOf(emptyList())
            } else {
                try {
                    colVinculaciones.where { "nutriologoUid".equalTo(uid) }.snapshots.map { snapshot ->
                        snapshot.documents.mapNotNull { doc -> vinculacionDesdeDoc(doc) }
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