package com.example.nutriia.teleconsulta

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.util.UUID

// ══════════════════════════════════════════════════════
// REPOSITORIO DE TELECONSULTA
//
// Estructura Firestore:
// /teleconsultas/{llamadaId}
//   ├── (campos de SolicitudLlamada)
//   ├── offerSdp:  String  ← SDP del nutriólogo
//   ├── answerSdp: String  ← SDP del padre
//   ├── /iceCandidatesOffer/{id}  ← ICE del nutriólogo
//   └── /iceCandidatesAnswer/{id} ← ICE del padre
// ══════════════════════════════════════════════════════

class TeleconsultaRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val col  get() = db.collection("teleconsultas")

    // ── Nutriólogo → inicia llamada ────────────────────────────────────────────
    suspend fun iniciarLlamada(
        padreUid:         String,
        padreNombre:      String,
        childId:          String,
        childNombre:      String,
        nutriologoNombre: String,
        tipo:             TipoLlamada
    ): Result<SolicitudLlamada> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Usuario no autenticado"))

        return try {
            val ref     = col.document()
            val llamada = SolicitudLlamada(
                id               = ref.id,
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
                creadoEn         = System.currentTimeMillis()
            )
            ref.set(llamada.toMap()).await()
            Result.success(llamada)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── PADRE → inicia llamada (flujo con pago previo) ────────────────────────
    suspend fun iniciarLlamadaComoPadre(
        padreUid:         String,
        padreNombre:      String,
        nutriologoUid:    String,
        nutriologoNombre: String,
        childId:          String,
        childNombre:      String,
        tipo:             TipoLlamada
    ): Result<SolicitudLlamada> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(Exception("Usuario no autenticado"))

        if (uid != padreUid) return Result.failure(Exception("UID de padre no coincide"))

        Log.d("TeleconsultaRepo", "iniciarLlamadaComoPadre: nutriologoUid=$nutriologoUid")

        return try {
            val ref     = col.document()
            val llamada = SolicitudLlamada(
                id               = ref.id,
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
                creadoEn         = System.currentTimeMillis()
            )
            ref.set(llamada.toMap()).await()
            Result.success(llamada)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Nutriólogo → sube su SDP Offer ────────────────────────────────────────
    suspend fun subirOfferSdp(llamadaId: String, sdp: String): Result<Unit> = try {
        col.document(llamadaId).update("offerSdp", sdp).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Padre → sube su SDP Answer + cambia estado a ACTIVA ───────────────────
    suspend fun responderLlamada(llamadaId: String, aceptar: Boolean): Result<Unit> = try {
        val updates = if (aceptar) {
            mapOf(
                "estado"     to EstadoLlamada.ACTIVA.name,
                "aceptadoEn" to System.currentTimeMillis()
            )
        } else {
            mapOf(
                "estado"       to EstadoLlamada.RECHAZADA.name,
                "finalizadoEn" to System.currentTimeMillis()
            )
        }
        col.document(llamadaId).update(updates).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Padre → sube su SDP Answer ────────────────────────────────────────────
    suspend fun subirAnswerSdp(llamadaId: String, sdp: String): Result<Unit> = try {
        col.document(llamadaId).update("answerSdp", sdp).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── ICE Candidates del NUTRIÓLOGO (offer side) ─────────────────────────────
    suspend fun subirIceCandidateOffer(
        llamadaId: String,
        candidate: IceCandidateData
    ): Result<Unit> = try {
        col.document(llamadaId)
            .collection("iceCandidatesOffer")
            .add(candidate.toMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── ICE Candidates del PADRE (answer side) ────────────────────────────────
    suspend fun subirIceCandidateAnswer(
        llamadaId: String,
        candidate: IceCandidateData
    ): Result<Unit> = try {
        col.document(llamadaId)
            .collection("iceCandidatesAnswer")
            .add(candidate.toMap())
            .await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Observar ICE candidates del OFFER (padre los escucha) ─────────────────
    fun observarIceCandidatesOffer(llamadaId: String): Flow<List<IceCandidateData>> =
        callbackFlow {
            val listener = col.document(llamadaId)
                .collection("iceCandidatesOffer")
                .addSnapshotListener { snap, err ->
                    if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                    val lista = snap?.documents?.mapNotNull { doc ->
                        doc.data?.let { IceCandidateData.fromMap(it) }
                    } ?: emptyList()
                    trySend(lista)
                }
            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    // ── Observar ICE candidates del ANSWER (nutriólogo los escucha) ───────────
    fun observarIceCandidatesAnswer(llamadaId: String): Flow<List<IceCandidateData>> =
        callbackFlow {
            val listener = col.document(llamadaId)
                .collection("iceCandidatesAnswer")
                .addSnapshotListener { snap, err ->
                    if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                    val lista = snap?.documents?.mapNotNull { doc ->
                        doc.data?.let { IceCandidateData.fromMap(it) }
                    } ?: emptyList()
                    trySend(lista)
                }
            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    // ── Cualquier extremo → finaliza la llamada ────────────────────────────────
    suspend fun finalizarLlamada(llamadaId: String, duracionSegundos: Int): Result<Unit> = try {
        col.document(llamadaId).update(
            mapOf(
                "estado"           to EstadoLlamada.FINALIZADA.name,
                "finalizadoEn"     to System.currentTimeMillis(),
                "duracionSegundos" to duracionSegundos
            )
        ).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    // ── Flow tiempo real: llamadas ENTRANTES para el PADRE ────────────────────
    fun observarLlamadasEntrantes(padreUid: String): Flow<SolicitudLlamada?> =
        callbackFlow {
            val listener = col
                .whereEqualTo("padreUid", padreUid)
                .whereEqualTo("estado", EstadoLlamada.SONANDO.name)
                .orderBy("creadoEn", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.e("TeleconsultaRepo", "Error escuchando llamadas entrantes", err)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val doc     = snap?.documents?.firstOrNull()
                    val llamada = doc?.data?.let { SolicitudLlamada.fromMap(doc.id, it) }
                    if (llamada != null && llamada.emisorUid == padreUid) {
                        trySend(null)
                    } else {
                        trySend(llamada)
                    }
                }
            awaitClose { listener.remove() }
        }.catch { emit(null) }

    // ── Flow tiempo real: llamadas ENTRANTES para el NUTRIÓLOGO ───────────────
    fun observarLlamadasEntrantesNutriologo(nutriologoUid: String): Flow<SolicitudLlamada?> =
        callbackFlow {
            val listener = col
                .whereEqualTo("nutriologoUid", nutriologoUid)
                .whereEqualTo("estado",        EstadoLlamada.SONANDO.name)
                .orderBy("creadoEn", Query.Direction.DESCENDING)
                .limit(1)
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.e("TeleconsultaRepo", "Error escuchando llamadas entrantes", err)
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val doc     = snap?.documents?.firstOrNull()
                    val llamada = doc?.data?.let { SolicitudLlamada.fromMap(doc.id, it) }
                    if (llamada != null && llamada.emisorUid == nutriologoUid) {
                        trySend(null)
                    } else {
                        trySend(llamada)
                    }
                }
            awaitClose { listener.remove() }
        }.catch { emit(null) }

    // ── Flow tiempo real: estado de una llamada específica ────────────────────
    fun observarLlamada(llamadaId: String): Flow<SolicitudLlamada?> =
        callbackFlow {
            val listener = col.document(llamadaId)
                .addSnapshotListener { snap, err ->
                    if (err != null) { trySend(null); return@addSnapshotListener }
                    val llamada = snap?.data?.let { SolicitudLlamada.fromMap(snap.id, it) }
                    trySend(llamada)
                }
            awaitClose { listener.remove() }
        }.catch { emit(null) }

    // ── Flow tiempo real: historial del nutriólogo ─────────────────────────────
    fun observarHistorial(nutriologoUid: String): Flow<List<SolicitudLlamada>> =
        callbackFlow {
            val listener = col
                .whereEqualTo("nutriologoUid", nutriologoUid)
                .orderBy("creadoEn", Query.Direction.DESCENDING)
                .limit(30)
                .addSnapshotListener { snap, err ->
                    if (err != null) { trySend(emptyList()); return@addSnapshotListener }
                    val lista = snap?.documents?.mapNotNull { doc ->
                        doc.data?.let { SolicitudLlamada.fromMap(doc.id, it) }
                    } ?: emptyList()
                    trySend(lista)
                }
            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    fun obtenerUidActual(): String? = auth.currentUser?.uid

    // ── Verifica si el padre tiene pago COMPLETADO para este nutriólogo/hijo ──────
    // Se usa desde PediatraScreen antes de abrir PaymentGateScreen.
    // Si ya pagó y el nutriólogo no respondió, puede reintentar sin pagar de nuevo.
    suspend fun verificarPagoCompletado(
        padreUid:      String,
        nutriologoUid: String,
        childId:       String
    ): Boolean = try {
        val snap = db.collection("pagos_teleconsulta")
            .whereEqualTo("padreUid",      padreUid)
            .whereEqualTo("nutriologoUid", nutriologoUid)
            .whereEqualTo("childId",       childId)
            .whereEqualTo("estado",        "COMPLETADO")
            .orderBy("completadoEn", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .await()
        snap.documents.isNotEmpty()
    } catch (e: Exception) {
        false
    }

    // ── Limpia datos de señalización tras finalizar la llamada ─────────────────
    // Borra offerSdp, answerSdp y las subcolecciones iceCandidatesOffer/Answer.
    // Solo conserva metadatos útiles (participantes, duración, timestamps).
    // Se llama fire-and-forget DESPUÉS de que la llamada ya terminó.
    suspend fun limpiarDatosSeñalizacion(llamadaId: String) {
        try {
            // 1. Limpiar campos SDP del documento principal
            col.document(llamadaId).update(
                mapOf(
                    "offerSdp"  to FieldValue.delete(),
                    "answerSdp" to FieldValue.delete()
                )
            ).await()

            // 2. Borrar subcolección iceCandidatesOffer
            borrarSubcoleccion(llamadaId, "iceCandidatesOffer")

            // 3. Borrar subcolección iceCandidatesAnswer
            borrarSubcoleccion(llamadaId, "iceCandidatesAnswer")

            Log.d("TeleconsultaRepo", "Datos de señalización limpiados para $llamadaId")
        } catch (e: Exception) {
            Log.e("TeleconsultaRepo", "Error limpiando señalización de $llamadaId", e)
        }
    }

    private suspend fun borrarSubcoleccion(llamadaId: String, nombre: String) {
        val docs = col.document(llamadaId)
            .collection(nombre)
            .get()
            .await()
        for (doc in docs.documents) {
            doc.reference.delete().await()
        }
    }
}