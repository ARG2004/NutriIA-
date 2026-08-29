package com.example.nutriia.teleconsulta

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.payment.PaymentRepository
import com.example.nutriia.platform.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

data class TeleconsultaUiState(
    val llamadaActual:    SolicitudLlamada? = null,
    val llamadaEntrante:  SolicitudLlamada? = null,
    val historial:        List<SolicitudLlamada> = emptyList(),
    val enLlamada:        Boolean = false,
    val silenciado:       Boolean = false,
    val camaraApagada:    Boolean = false,
    val altavozActivo:    Boolean = true,
    val duracionSegundos: Int     = 0,
    val error:            String? = null,
    val cargando:         Boolean = false,
    val webRtcConectado:  Boolean = false,
    val soyElNutriologo:  Boolean = false
)

class TeleconsultaViewModel : ViewModel(), WebRtcEngineCallback {

    private val repo = TeleconsultaRepository()

    private val _state = MutableStateFlow(TeleconsultaUiState())
    val state: StateFlow<TeleconsultaUiState> = _state.asStateFlow()

    private var timerJob:         Job? = null
    private var observerJob:      Job? = null
    private var entrantesJob:     Job? = null
    private var iceCandidatesJob: Job? = null

    private var esOfferer: Boolean = false
    private val iceCandidatesPendientes = mutableListOf<IceCandidateData>()
    private var remoteSdpEstablecido    = false
    private var pagoIdPendiente: String? = null
    private var pagoIdConsumido: String? = null

    // ═════════════════════════════════════════════════════
    // WebRtcEngineCallback
    // ═════════════════════════════════════════════════════

    override fun onLocalSdpReady(sdp: SessionDescription) {
        val llamadaId = _state.value.llamadaActual?.id ?: return
        viewModelScope.launch {
            when (sdp.type) {
                SdpType.OFFER -> repo.subirOfferSdp(llamadaId, sdp.sdpDescription)
                SdpType.ANSWER -> {
                    repo.subirAnswerSdp(llamadaId, sdp.sdpDescription)
                    remoteSdpEstablecido = true
                    procesarIceCandidatesPendientes()
                }
                else -> {}
            }
        }
    }

    override fun onLocalIceCandidateReady(candidate: IceCandidate) {
        val llamadaId = _state.value.llamadaActual?.id ?: return
        val data = IceCandidateData(
            sdpMid        = candidate.sdpMid,
            sdpMLineIndex = candidate.sdpMLineIndex,
            sdp           = candidate.sdp
        )
        viewModelScope.launch {
            if (esOfferer) repo.subirIceCandidateOffer(llamadaId, data)
            else              repo.subirIceCandidateAnswer(llamadaId, data)
        }
    }

    override fun onConnected() {
        if (_state.value.webRtcConectado) return
        val currentCall = _state.value.llamadaActual
        val updatedCall = if (currentCall != null && (currentCall.estado == EstadoLlamada.SONANDO || currentCall.estado == EstadoLlamada.INICIANDO)) {
            currentCall.copy(estado = EstadoLlamada.ACTIVA)
        } else {
            currentCall
        }
        _state.value = _state.value.copy(
            webRtcConectado  = true,
            enLlamada        = true,
            duracionSegundos = 0,
            llamadaActual    = updatedCall
        )
        iniciarTimer()
        confirmarPagoTrasConexionEstable()
    }

    override fun onDisconnected() {
        _state.value = _state.value.copy(webRtcConectado = false)
    }

    override fun onError(message: String) {
        _state.value = _state.value.copy(error = "WebRTC: $message")
    }

    override fun onRemoteVideoTrackReady(track: VideoTrack) {
    }

    // ═════════════════════════════════════════════════════
    // INICIAR LLAMADA (OFFER SIDE)
    // ═════════════════════════════════════════════════════

    fun iniciarLlamada(
        padreUid:         String,
        padreNombre:      String,
        childId:          String,
        childNombre:      String,
        nutriologoNombre: String,
        tipo:             TipoLlamada = TipoLlamada.VIDEO
    ) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true, error = null, soyElNutriologo = true)
            esOfferer = true

            remoteSdpEstablecido = false
            iceCandidatesPendientes.clear()

            repo.iniciarLlamada(
                padreUid, padreNombre, childId, childNombre, nutriologoNombre, tipo
            ).fold(
                onSuccess = { llamada -> prepararOffer(llamada, tipo) },
                onFailure = {
                    _state.value = _state.value.copy(
                        cargando = false,
                        error    = "No se pudo iniciar la llamada: ${it.message}"
                    )
                }
            )
        }
    }

    fun iniciarLlamadaComoPadre(
        padreUid:         String,
        padreNombre:      String,
        nutriologoUid:    String,
        nutriologoNombre: String,
        childId:          String,
        childNombre:      String,
        pagoId:           String,
        tipo:             TipoLlamada = TipoLlamada.VIDEO
    ) {
        viewModelScope.launch {
            require(pagoId.isNotBlank()) { "pagoId no puede estar vacio" }
            pagoIdPendiente = pagoId
            _state.value = _state.value.copy(cargando = true, error = null, soyElNutriologo = false)
            esOfferer = true

            remoteSdpEstablecido = false
            iceCandidatesPendientes.clear()

            repo.iniciarLlamadaComoPadre(
                padreUid, padreNombre, nutriologoUid, nutriologoNombre, childId, childNombre, tipo
            ).fold(
                onSuccess = { llamada -> prepararOffer(llamada, tipo) },
                onFailure = {
                    _state.value = _state.value.copy(
                        cargando = false,
                        error    = "Error al solicitar teleconsulta: ${it.message}"
                    )
                }
            )
        }
    }

    private fun prepararOffer(llamada: SolicitudLlamada, tipo: TipoLlamada) {
        val esVideo = (tipo == TipoLlamada.VIDEO)
        _state.value = _state.value.copy(
            llamadaActual = llamada,
            enLlamada     = false,
            cargando      = false,
            altavozActivo = esVideo
        )
        val engine = CallEngineProvider.init(this@TeleconsultaViewModel)
        engine.createPeerConnection(isOffer = true, tipo = tipo)
        engine.createOffer()
        suscribirCambiosLlamada(llamada.id)
        observarIceCandidatesAnswer(llamada.id)
    }

    // ═════════════════════════════════════════════════════
    // RESPONDER LLAMADA (ANSWER SIDE)
    // ═════════════════════════════════════════════════════

    fun responderLlamada(llamadaId: String, aceptar: Boolean) {
        viewModelScope.launch {
            if (!aceptar) {
                repo.responderLlamada(llamadaId, false)
                _state.value = _state.value.copy(llamadaEntrante = null)
                return@launch
            }

            esOfferer = false
            remoteSdpEstablecido = false
            iceCandidatesPendientes.clear()

            val llamada = _state.value.llamadaEntrante ?: return@launch

            val engine = CallEngineProvider.init(this@TeleconsultaViewModel)
            engine.createPeerConnection(isOffer = false, tipo = llamada.tipo)

            repo.responderLlamada(llamadaId, true)
            val uid = repo.getCurrentUserId()
            val soyNutri = uid != null && uid == llamada.nutriologoUid

            val esVideo = (llamada.tipo == TipoLlamada.VIDEO)
            _state.value = _state.value.copy(
                llamadaActual   = llamada.copy(estado = EstadoLlamada.ACTIVA),
                llamadaEntrante = null,
                soyElNutriologo = soyNutri,
                altavozActivo   = esVideo
            )


            suscribirCambiosLlamada(llamadaId)
            observarIceCandidatesOffer(llamadaId)

            llamada.offerSdp?.let { sdp ->
                engine.setRemoteOffer(sdp)
            }
        }
    }

    // ═════════════════════════════════════════════════════
    // SUSCRIPCIÓN TIEMPO REAL
    // ═════════════════════════════════════════════════════

    private fun suscribirCambiosLlamada(llamadaId: String) {
        observerJob?.cancel()
        observerJob = repo.observarLlamada(llamadaId)
            .onEach { llamada ->
                val prev = _state.value.llamadaActual
                _state.value = _state.value.copy(llamadaActual = llamada)

                when (llamada?.estado) {
                    EstadoLlamada.ACTIVA -> {
                        if (!esOfferer) {
                            val offerNuevo    = llamada.offerSdp
                            val offerAnterior = prev?.offerSdp
                            if (offerNuevo != null && offerAnterior == null && CallEngineProvider.isInitialized) {
                                CallEngineProvider.getEngine()?.setRemoteOffer(offerNuevo)
                            }
                        }
                    }
                    EstadoLlamada.FINALIZADA,
                    EstadoLlamada.RECHAZADA -> {
                        detenerTimer()
                        evaluarReactivacionPago()
                        _state.value = _state.value.copy(
                            llamadaActual    = null,
                            enLlamada        = false,
                            duracionSegundos = 0,
                            webRtcConectado  = false
                        )
                        liberarWebRtc()
                        viewModelScope.launch { repo.limpiarDatosSenalizacion(llamadaId) }
                    }
                    else -> {}
                }

                if (esOfferer && llamada != null) {
                    val answerNuevo    = llamada.answerSdp
                    val answerAnterior = prev?.answerSdp
                    if (answerNuevo != null && answerAnterior == null && CallEngineProvider.isInitialized) {
                        CallEngineProvider.getEngine()?.setRemoteAnswer(answerNuevo)
                        remoteSdpEstablecido = true
                        procesarIceCandidatesPendientes()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observarIceCandidatesOffer(llamadaId: String) {
        iceCandidatesJob?.cancel()
        iceCandidatesJob = repo.observarIceCandidatesOffer(llamadaId)
            .onEach { candidates ->
                candidates.forEach { c ->
                    if (remoteSdpEstablecido && CallEngineProvider.isInitialized) {
                        CallEngineProvider.getEngine()?.addRemoteIceCandidate(c.sdpMid, c.sdpMLineIndex, c.sdp)
                    } else if (iceCandidatesPendientes.none { it.sdp == c.sdp }) {
                        iceCandidatesPendientes.add(c)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun observarIceCandidatesAnswer(llamadaId: String) {
        iceCandidatesJob?.cancel()
        iceCandidatesJob = repo.observarIceCandidatesAnswer(llamadaId)
            .onEach { candidates ->
                candidates.forEach { c ->
                    if (remoteSdpEstablecido && CallEngineProvider.isInitialized) {
                        CallEngineProvider.getEngine()?.addRemoteIceCandidate(c.sdpMid, c.sdpMLineIndex, c.sdp)
                    } else if (iceCandidatesPendientes.none { it.sdp == c.sdp }) {
                        iceCandidatesPendientes.add(c)
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    private fun procesarIceCandidatesPendientes() {
        if (!CallEngineProvider.isInitialized) return
        iceCandidatesPendientes.forEach { c ->
            CallEngineProvider.getEngine()?.addRemoteIceCandidate(c.sdpMid, c.sdpMLineIndex, c.sdp)
        }
        iceCandidatesPendientes.clear()
    }

    // ═════════════════════════════════════════════════════
    // FINALIZAR
    // ═════════════════════════════════════════════════════

    fun finalizarLlamada() {
        val id       = _state.value.llamadaActual?.id ?: return
        val duracion = _state.value.duracionSegundos
        detenerTimer()
        viewModelScope.launch {
            repo.finalizarLlamada(id, duracion)
            _state.value = _state.value.copy(
                enLlamada        = false,
                webRtcConectado  = false,
                llamadaActual    = _state.value.llamadaActual?.copy(estado = EstadoLlamada.FINALIZADA)
            )
            liberarWebRtc()
        }
    }

    // ═════════════════════════════════════════════════════
    // CONTROLES
    // ═════════════════════════════════════════════════════

    fun toggleSilencio() {
        val nuevo = !_state.value.silenciado
        _state.value = _state.value.copy(silenciado = nuevo)
        if (CallEngineProvider.isInitialized) CallEngineProvider.getEngine()?.silenciar(nuevo)
    }

    fun toggleCamara() {
        val nuevo = !_state.value.camaraApagada
        _state.value = _state.value.copy(camaraApagada = nuevo)
        if (CallEngineProvider.isInitialized) CallEngineProvider.getEngine()?.apagarCamara(nuevo)
    }

    fun toggleAltavoz() {
        val nuevo = !_state.value.altavozActivo
        _state.value = _state.value.copy(altavozActivo = nuevo)
        if (CallEngineProvider.isInitialized) CallEngineProvider.getEngine()?.setSpeaker(nuevo)
    }

    fun cambiarCamara() {
        if (CallEngineProvider.isInitialized) CallEngineProvider.getEngine()?.cambiarCamara()
    }

    // ═════════════════════════════════════════════════════
    // OBSERVACIONES LLAMADAS ENTRANTES
    // ═════════════════════════════════════════════════════

    fun iniciarObservacionEntrantes(padreUid: String) {
        entrantesJob?.cancel()
        entrantesJob = repo.observarLlamadasEntrantes(padreUid)
            .onEach { llamada ->
                val hayLlamadaActiva = _state.value.llamadaActual != null
                when {
                    llamada == null  -> _state.value = _state.value.copy(llamadaEntrante = null)
                    hayLlamadaActiva -> { }
                    else             -> _state.value = _state.value.copy(llamadaEntrante = llamada)
                }
            }
            .launchIn(viewModelScope)
    }

    fun iniciarObservacionEntrantesNutriologo(nutriologoUid: String) {
        Log.d("TeleconsultaVM", "iniciarObservacionEntrantesNutriologo: nutriologoUid=$nutriologoUid")
        entrantesJob?.cancel()
        entrantesJob = repo.observarLlamadasEntrantesNutriologo(nutriologoUid)
            .onEach { llamada ->
                val hayLlamadaActiva = _state.value.llamadaActual != null
                when {
                    llamada == null  -> _state.value = _state.value.copy(llamadaEntrante = null)
                    hayLlamadaActiva -> { }
                    else             -> _state.value = _state.value.copy(llamadaEntrante = llamada)
                }
            }
            .launchIn(viewModelScope)
    }

    fun detenerObservacionEntrantes() {
        entrantesJob?.cancel()
        entrantesJob = null
        _state.value = _state.value.copy(
            llamadaEntrante = null,
            llamadaActual = null
        )
    }

    fun cargarHistorial(nutriologoUid: String) {
        repo.observarHistorial(nutriologoUid)
            .onEach { historial -> _state.value = _state.value.copy(historial = historial) }
            .launchIn(viewModelScope)
    }

    // ═════════════════════════════════════════════════════
    // HELPERS
    // ═════════════════════════════════════════════════════

    fun cerrarPantallaLlamada() {
        detenerTimer()
        evaluarReactivacionPago()
        liberarWebRtc()
        pagoIdPendiente = null
        _state.value = _state.value.copy(
            llamadaActual    = null,
            enLlamada        = false,
            duracionSegundos = 0,
            webRtcConectado  = false
        )
    }

    fun limpiarError() { _state.value = _state.value.copy(error = null) }

    private fun iniciarTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                _state.value = _state.value.copy(
                    duracionSegundos = _state.value.duracionSegundos + 1
                )
            }
        }
    }

    private fun detenerTimer() { timerJob?.cancel(); timerJob = null }

    private fun liberarWebRtc() {
        evaluarReactivacionPago()
        remoteSdpEstablecido = false
        iceCandidatesPendientes.clear()
        observerJob?.cancel();      observerJob      = null
        iceCandidatesJob?.cancel(); iceCandidatesJob = null
        CallEngineProvider.release()
    }

    private val DURACION_MINIMA_GARANTIZADA_SEGUNDOS = 300L

    private fun confirmarPagoTrasConexionEstable() {
        val pagoId = pagoIdPendiente ?: return
        viewModelScope.launch {
            val llamadaId = _state.value.llamadaActual?.id ?: return@launch
            PaymentRepository().marcarPagoUsado(pagoId, llamadaId).fold(
                onSuccess = {
                    pagoIdConsumido = pagoId
                    pagoIdPendiente = null
                },
                onFailure = {
                    Log.e("TeleconsultaVM", "Error al marcar pago usado: ${it.message}")
                }
            )
        }
    }

    private fun evaluarReactivacionPago() {
        val pagoId = pagoIdConsumido ?: return
        pagoIdConsumido = null
        val duracion = _state.value.duracionSegundos
        if (duracion < DURACION_MINIMA_GARANTIZADA_SEGUNDOS) {
            viewModelScope.launch {
                PaymentRepository().reactivarPago(pagoId).fold(
                    onSuccess = {
                        Log.d("TeleconsultaVM", "Pago $pagoId reactivado exitosamente (duracion: ${duracion}s)")
                    },
                    onFailure = {
                        Log.e("TeleconsultaVM", "Error al reactivar pago $pagoId: ${it.message}")
                    }
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        entrantesJob?.cancel()
        liberarWebRtc()
    }
}