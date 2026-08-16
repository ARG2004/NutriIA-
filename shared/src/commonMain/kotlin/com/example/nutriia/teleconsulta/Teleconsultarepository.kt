package com.example.nutriia.teleconsulta

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class TeleconsultaRepository {

    private val llamadasState = MutableStateFlow<Map<String, SolicitudLlamada>>(emptyMap())
    private val iceOfferState = MutableStateFlow<Map<String, List<IceCandidateData>>>(emptyMap())
    private val iceAnswerState = MutableStateFlow<Map<String, List<IceCandidateData>>>(emptyMap())

    suspend fun iniciarLlamada(
        padreUid:         String,
        padreNombre:      String,
        childId:          String,
        childNombre:      String,
        nutriologoNombre: String,
        tipo:             TipoLlamada = TipoLlamada.VIDEO
    ): Result<SolicitudLlamada> {
        val id = com.example.nutriia.platform.generateUUID()
        val llamada = SolicitudLlamada(
            id               = id,
            padreUid         = padreUid,
            padreNombre      = padreNombre,
            childId          = childId,
            childNombre      = childNombre,
            nutriologoNombre = nutriologoNombre,
            tipo             = tipo,
            estado           = EstadoLlamada.INICIANDO
        )
        llamadasState.value = llamadasState.value + (id to llamada)
        return Result.success(llamada)
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
        val id = com.example.nutriia.platform.generateUUID()
        val llamada = SolicitudLlamada(
            id               = id,
            padreUid         = padreUid,
            padreNombre      = padreNombre,
            nutriologoUid    = nutriologoUid,
            nutriologoNombre = nutriologoNombre,
            childId          = childId,
            childNombre      = childNombre,
            tipo             = tipo,
            estado           = EstadoLlamada.INICIANDO
        )
        llamadasState.value = llamadasState.value + (id to llamada)
        return Result.success(llamada)
    }

    suspend fun responderLlamada(llamadaId: String, aceptar: Boolean): Result<Unit> {
        val current = llamadasState.value[llamadaId] ?: return Result.failure(Exception("No encontrada"))
        val updated = current.copy(
            estado = if (aceptar) EstadoLlamada.ACTIVA else EstadoLlamada.RECHAZADA
        )
        llamadasState.value = llamadasState.value + (llamadaId to updated)
        return Result.success(Unit)
    }

    fun observarLlamada(llamadaId: String): Flow<SolicitudLlamada?> {
        return llamadasState.map { it[llamadaId] }
    }

    suspend fun subirOfferSdp(llamadaId: String, sdp: String): Result<Unit> {
        val current = llamadasState.value[llamadaId]
        if (current != null) {
            llamadasState.value = llamadasState.value + (llamadaId to current.copy(offerSdp = sdp))
        }
        return Result.success(Unit)
    }

    suspend fun subirAnswerSdp(llamadaId: String, sdp: String): Result<Unit> {
        val current = llamadasState.value[llamadaId]
        if (current != null) {
            llamadasState.value = llamadasState.value + (llamadaId to current.copy(answerSdp = sdp))
        }
        return Result.success(Unit)
    }

    suspend fun subirIceCandidateOffer(llamadaId: String, candidate: IceCandidateData): Result<Unit> {
        val current = iceOfferState.value[llamadaId] ?: emptyList()
        iceOfferState.value = iceOfferState.value + (llamadaId to (current + candidate))
        return Result.success(Unit)
    }

    suspend fun subirIceCandidateAnswer(llamadaId: String, candidate: IceCandidateData): Result<Unit> {
        val current = iceAnswerState.value[llamadaId] ?: emptyList()
        iceAnswerState.value = iceAnswerState.value + (llamadaId to (current + candidate))
        return Result.success(Unit)
    }

    fun observarIceCandidatesOffer(llamadaId: String): Flow<List<IceCandidateData>> {
        return iceOfferState.map { it[llamadaId] ?: emptyList() }
    }

    fun observarIceCandidatesAnswer(llamadaId: String): Flow<List<IceCandidateData>> {
        return iceAnswerState.map { it[llamadaId] ?: emptyList() }
    }

    suspend fun finalizarLlamada(llamadaId: String, duracionSegundos: Int): Result<Unit> {
        val current = llamadasState.value[llamadaId]
        if (current != null) {
            llamadasState.value = llamadasState.value + (llamadaId to current.copy(
                estado = EstadoLlamada.FINALIZADA,
                duracionSegundos = duracionSegundos
            ))
        }
        return Result.success(Unit)
    }

    suspend fun limpiarDatosSenalizacion(llamadaId: String): Result<Unit> {
        val current = llamadasState.value[llamadaId]
        if (current != null) {
            llamadasState.value = llamadasState.value + (llamadaId to current.copy(offerSdp = null, answerSdp = null))
        }
        return Result.success(Unit)
    }

    fun observarLlamadasEntrantes(padreUid: String): Flow<SolicitudLlamada?> {
        return llamadasState.map { map ->
            map.values.firstOrNull { it.padreUid == padreUid && it.estado == EstadoLlamada.SONANDO }
        }
    }

    fun observarLlamadasEntrantesNutriologo(nutriologoUid: String): Flow<SolicitudLlamada?> {
        return llamadasState.map { map ->
            map.values.firstOrNull { it.nutriologoUid == nutriologoUid && it.estado == EstadoLlamada.SONANDO }
        }
    }

    fun observarHistorial(nutriologoUid: String): Flow<List<SolicitudLlamada>> {
        return llamadasState.map { map ->
            map.values.filter { it.nutriologoUid == nutriologoUid }
        }
    }
}
