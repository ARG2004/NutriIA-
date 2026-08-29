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

    fun observarLlamada(llamadaId: String): Flow<SolicitudLlamada?> {
        return try {
            col.document(llamadaId).snapshots.conflate().map { snapshot ->
                if (snapshot.exists) {
                    try { SolicitudLlamada.fromMap(snapshot.id, snapshot.data()) } catch (e: Exception) { null }
                } else null
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
                    try { IceCandidateData.fromMap(doc.data()) } catch(e: Exception) { null }
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
                    try { IceCandidateData.fromMap(doc.data()) } catch(e: Exception) { null }
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
        return auth.authStateChanged.flatMapLatest { user ->
            if (user == null) flowOf(null)
            else try {
                col.where { "padreUid".equalTo(padreUid) }
                    .where { "estado".equalTo(EstadoLlamada.SONANDO.name) }
                    .orderBy("creadoEn", Direction.DESCENDING)
                    .snapshots.conflate().map { querySnapshot ->
                        val doc = querySnapshot.documents.firstOrNull()
                        val llamada = try { doc?.let { SolicitudLlamada.fromMap(it.id, it.data()) } } catch (e: Exception) { null }
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
    }

    fun observarLlamadasEntrantesNutriologo(nutriologoUid: String): Flow<SolicitudLlamada?> {
        return auth.authStateChanged.flatMapLatest { user ->
            if (user == null) flowOf(null)
            else try {
                col.where { "nutriologoUid".equalTo(nutriologoUid) }
                    .where { "estado".equalTo(EstadoLlamada.SONANDO.name) }
                    .orderBy("creadoEn", Direction.DESCENDING)
                    .snapshots.conflate().map { querySnapshot ->
                        val doc = querySnapshot.documents.firstOrNull()
                        val llamada = try { doc?.let { SolicitudLlamada.fromMap(it.id, it.data()) } } catch (e: Exception) { null }
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
    }

    fun observarHistorial(nutriologoUid: String): Flow<List<SolicitudLlamada>> {
        return auth.authStateChanged.flatMapLatest { user ->
            if (user == null) flowOf(emptyList())
            else try {
                col.where { "nutriologoUid".equalTo(nutriologoUid) }
                    .snapshots.conflate().map { querySnapshot ->
                        querySnapshot.documents.mapNotNull { doc ->
                            try { SolicitudLlamada.fromMap(doc.id, doc.data()) } catch (e: Exception) { null }
                        }
                    }
            } catch (e: Exception) {
                flowOf(emptyList())
            }
        }
    }
}


