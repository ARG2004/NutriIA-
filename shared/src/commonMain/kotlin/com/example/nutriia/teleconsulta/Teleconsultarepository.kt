package com.example.nutriia.teleconsulta

import com.example.nutriia.platform.Log
import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

private const val TAG = "TeleconsultaRepo"

@OptIn(ExperimentalCoroutinesApi::class)
class TeleconsultaRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth
    private val col get() = db.collection("teleconsultas")

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    suspend fun iniciarLlamada(
        padreUid:         String,
        padreNombre:      String,
        childId:          String,
        childNombre:      String,
        nutriologoNombre: String,
        tipo:             TipoLlamada = TipoLlamada.VIDEO
    ): Result<SolicitudLlamada> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Usuario no autenticado"))

        return try {
            val docRef = col.document(generateUUID())
            val llamada = SolicitudLlamada(
                id               = docRef.id,
                emisorUid        = uid,
                receptorUid      = padreUid,
                nutriologoUid    = uid,
                nutriologoNombre = nutriologoNombre,
                padreUid         = padreUid,
                padreNombre      = padreNombre,
                childId          = childId,
                childNombre      = childNombre,
                tipo             = tipo,
                estado           = EstadoLlamada.SONANDO,
                creadoEn         = currentTimeMillis()
            )
            // FIX iOS: usar objeto @Serializable directo
            docRef.set(llamada)
            Result.success(llamada)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar llamada: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun iniciarLlamadaComoPadre(
        padreUid:         String,
        padreNombre:      String,
        nutriologoUid:    String,
        nutriologoNombre: String,
        childId:          String,
        childNombre:      String,
        tipo:             TipoLlamada = TipoLlamada.VIDEO
    ): Result<SolicitudLlamada> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Usuario no autenticado"))

        return try {
            val docRef = col.document(generateUUID())
            val llamada = SolicitudLlamada(
                id               = docRef.id,
                emisorUid        = padreUid,
                receptorUid      = nutriologoUid,
                nutriologoUid    = nutriologoUid,
                nutriologoNombre = nutriologoNombre,
                padreUid         = padreUid,
                padreNombre      = padreNombre,
                childId          = childId,
                childNombre      = childNombre,
                tipo             = tipo,
                estado           = EstadoLlamada.SONANDO,
                creadoEn         = currentTimeMillis()
            )
            // FIX iOS: usar objeto @Serializable directo
            docRef.set(llamada)
            Result.success(llamada)
        } catch (e: Exception) {
            Log.e(TAG, "Error al iniciar llamada como padre: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun responderLlamada(llamadaId: String, aceptar: Boolean): Result<Unit> {
        return try {
            val nuevoEstado = if (aceptar) EstadoLlamada.ACTIVA else EstadoLlamada.RECHAZADA
            val updateData = mutableMapOf<String, Any?>("estado" to nuevoEstado.name)
            if (aceptar) {
                updateData["aceptadoEn"] = currentTimeMillis()
            }
            col.document(llamadaId).update(updateData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun llamadaDesdeDoc(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): SolicitudLlamada? {
        return try {
            if (!doc.exists) return null
            val id = doc.id
            val emisorUid = runCatching { doc.get<String?>("emisorUid") }.getOrNull() ?: ""
            val receptorUid = runCatching { doc.get<String?>("receptorUid") }.getOrNull() ?: ""
            val nutriologoUid = runCatching { doc.get<String?>("nutriologoUid") }.getOrNull() ?: ""
            val nutriologoNombre = runCatching { doc.get<String?>("nutriologoNombre") }.getOrNull() ?: ""
            val padreUid = runCatching { doc.get<String?>("padreUid") }.getOrNull() ?: ""
            val padreNombre = runCatching { doc.get<String?>("padreNombre") }.getOrNull() ?: ""
            val childId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: ""
            val childNombre = runCatching { doc.get<String?>("childNombre") }.getOrNull() ?: ""
            val tipoStr = runCatching { doc.get<String?>("tipo") }.getOrNull() ?: TipoLlamada.VIDEO.name
            val tipo = runCatching { TipoLlamada.valueOf(tipoStr) }.getOrDefault(TipoLlamada.VIDEO)
            val estadoStr = runCatching { doc.get<String?>("estado") }.getOrNull() ?: EstadoLlamada.INICIANDO.name
            val estado = runCatching { EstadoLlamada.valueOf(estadoStr) }.getOrDefault(EstadoLlamada.INICIANDO)
            val offerSdp = runCatching { doc.get<String?>("offerSdp") }.getOrNull()
            val answerSdp = runCatching { doc.get<String?>("answerSdp") }.getOrNull()

            val creadoEnTs = runCatching { doc.get<dev.gitlive.firebase.firestore.Timestamp?>("creadoEn") }.getOrNull()
            val creadoEnLong = runCatching { doc.get<Long?>("creadoEn") }.getOrNull()
            val creadoEnStr = runCatching { doc.get<String?>("creadoEn") }.getOrNull()
            val creadoEn = when {
                creadoEnTs != null -> creadoEnTs.seconds * 1000L + (creadoEnTs.nanoseconds / 1_000_000L)
                creadoEnLong != null -> if (creadoEnLong > 100_000_000_000L) creadoEnLong else creadoEnLong * 1000L
                !creadoEnStr.isNullOrBlank() -> com.example.nutriia.utils.FechaUtils.parsearFechaHora(creadoEnStr)
                else -> 0L
            }

            val aceptadoEnTs = runCatching { doc.get<dev.gitlive.firebase.firestore.Timestamp?>("aceptadoEn") }.getOrNull()
            val aceptadoEnLong = runCatching { doc.get<Long?>("aceptadoEn") }.getOrNull()
            val aceptadoEn = when {
                aceptadoEnTs != null -> aceptadoEnTs.seconds * 1000L + (aceptadoEnTs.nanoseconds / 1_000_000L)
                aceptadoEnLong != null -> if (aceptadoEnLong > 100_000_000_000L) aceptadoEnLong else aceptadoEnLong * 1000L
                else -> null
            }

            val finalizadoEnTs = runCatching { doc.get<dev.gitlive.firebase.firestore.Timestamp?>("finalizadoEn") }.getOrNull()
            val finalizadoEnLong = runCatching { doc.get<Long?>("finalizadoEn") }.getOrNull()
            val finalizadoEn = when {
                finalizadoEnTs != null -> finalizadoEnTs.seconds * 1000L + (finalizadoEnTs.nanoseconds / 1_000_000L)
                finalizadoEnLong != null -> if (finalizadoEnLong > 100_000_000_000L) finalizadoEnLong else finalizadoEnLong * 1000L
                else -> null
            }

            val duracion = runCatching { doc.get<Long?>("duracionSegundos")?.toInt() }.getOrNull() ?: 0

            SolicitudLlamada(
                id = id,
                emisorUid = emisorUid,
                receptorUid = receptorUid,
                nutriologoUid = nutriologoUid,
                nutriologoNombre = nutriologoNombre,
                padreUid = padreUid,
                padreNombre = padreNombre,
                childId = childId,
                childNombre = childNombre,
                tipo = tipo,
                estado = estado,
                offerSdp = offerSdp,
                answerSdp = answerSdp,
                creadoEn = creadoEn,
                aceptadoEn = aceptadoEn,
                finalizadoEn = finalizadoEn,
                duracionSegundos = duracion
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun observarLlamada(llamadaId: String): Flow<SolicitudLlamada?> {
        return try {
            col.document(llamadaId).snapshots.conflate().map { snapshot ->
                llamadaDesdeDoc(snapshot)
            }
        } catch (e: Exception) {
            flowOf(null)
        }
    }

    suspend fun subirOfferSdp(llamadaId: String, sdp: String): Result<Unit> {
        return try {
            col.document(llamadaId).update(mapOf("offerSdp" to sdp))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun subirAnswerSdp(llamadaId: String, sdp: String): Result<Unit> {
        return try {
            col.document(llamadaId).update(mapOf("answerSdp" to sdp))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun subirIceCandidateOffer(llamadaId: String, candidate: IceCandidateData): Result<Unit> {
        return try {
            col.document(llamadaId).collection("iceCandidatesOffer")
                .document(generateUUID()).set(candidate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun subirIceCandidateAnswer(llamadaId: String, candidate: IceCandidateData): Result<Unit> {
        return try {
            col.document(llamadaId).collection("iceCandidatesAnswer")
                .document(generateUUID()).set(candidate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarIceCandidatesOffer(llamadaId: String): Flow<List<IceCandidateData>> {
        return try {
            col.document(llamadaId).collection("iceCandidatesOffer").snapshots.conflate().map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    try { doc.data<IceCandidateData>() } catch(e: Exception) { null }
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observarIceCandidatesAnswer(llamadaId: String): Flow<List<IceCandidateData>> {
        return try {
            col.document(llamadaId).collection("iceCandidatesAnswer").snapshots.conflate().map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    try { doc.data<IceCandidateData>() } catch(e: Exception) { null }
                }
            }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun finalizarLlamada(llamadaId: String, duracionSegundos: Int): Result<Unit> {
        return try {
            col.document(llamadaId).update(
                mapOf(
                    "estado" to EstadoLlamada.FINALIZADA.name,
                    "finalizadoEn" to currentTimeMillis(),
                    "duracionSegundos" to duracionSegundos
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun limpiarDatosSenalizacion(llamadaId: String): Result<Unit> {
        return try {
            col.document(llamadaId).update(
                mapOf(
                    "offerSdp" to null,
                    "answerSdp" to null
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarLlamadasEntrantes(padreUid: String): Flow<SolicitudLlamada?> {
        if (padreUid.isBlank()) return flowOf(null)
        return try {
            col.where { "padreUid".equalTo(padreUid) }
                .where { "estado".equalTo(EstadoLlamada.SONANDO.name) }
                .snapshots.conflate().map { querySnapshot ->
                    val lista = querySnapshot.documents.mapNotNull { llamadaDesdeDoc(it) }
                        .sortedByDescending { it.creadoEn }
                    val llamada = lista.firstOrNull()
                    if (llamada != null && llamada.emisorUid == padreUid) {
                        null
                    } else {
                        llamada
                    }
                }
        } catch (e: Exception) {
            flowOf(null)
        }
    }

    fun observarLlamadasEntrantesNutriologo(nutriologoUid: String): Flow<SolicitudLlamada?> {
        if (nutriologoUid.isBlank()) return flowOf(null)
        return try {
            col.where { "nutriologoUid".equalTo(nutriologoUid) }
                .where { "estado".equalTo(EstadoLlamada.SONANDO.name) }
                .snapshots.conflate().map { querySnapshot ->
                    val lista = querySnapshot.documents.mapNotNull { llamadaDesdeDoc(it) }
                        .sortedByDescending { it.creadoEn }
                    val llamada = lista.firstOrNull()
                    if (llamada != null && llamada.emisorUid == nutriologoUid) {
                        null
                    } else {
                        llamada
                    }
                }
        } catch (e: Exception) {
            flowOf(null)
        }
    }

    fun observarHistorial(nutriologoUid: String): Flow<List<SolicitudLlamada>> {
        if (nutriologoUid.isBlank()) return flowOf(emptyList())
        return try {
            col.where { "nutriologoUid".equalTo(nutriologoUid) }
                .snapshots.conflate().map { querySnapshot ->
                    querySnapshot.documents.mapNotNull { llamadaDesdeDoc(it) }
                        .sortedByDescending { it.creadoEn }
                }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }
}
