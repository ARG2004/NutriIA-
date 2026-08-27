package com.example.nutriia.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.openUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PaymentUiState(
    val cargando:           Boolean           = false,
    val pagoActual:         PagoTeleconsulta? = null,
    val pagoCompletado:     Boolean           = false,
    val error:              String?           = null
)

class PaymentViewModel : ViewModel() {

    private val repo = PaymentRepository()

    private val _state = MutableStateFlow(PaymentUiState())
    val state: StateFlow<PaymentUiState> = _state.asStateFlow()

    companion object {
        const val PRECIO_CENTAVOS = 25000
        const val MONEDA          = "MXN"

        const val DEEP_LINK_SUCCESS = "nutriia://pago-ok"
        const val DEEP_LINK_CANCEL  = "nutriia://pago-cancelado"
    }

    // ── Paso 1: crear el pago pendiente en Firestore ──────────────────────────
    fun iniciarPago(nutriologoUid: String, childId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                cargando       = true,
                error          = null,
                pagoCompletado = false
            )
            repo.crearPagoPendiente(
                nutriologoUid = nutriologoUid,
                childId       = childId,
                montoCentavos = PRECIO_CENTAVOS,
                moneda        = MONEDA
            ).fold(
                onSuccess = { pago ->
                    _state.value = _state.value.copy(
                        cargando   = false,
                        pagoActual = pago
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        cargando = false,
                        error    = "No se pudo iniciar el pago: ${it.message}"
                    )
                }
            )
        }
    }

    // ── Paso 2: abrir PayPal en el navegador del dispositivo ──────────────────
    fun abrirPayPalEnNavegador() {
        val pagoId = _state.value.pagoActual?.id ?: return
        val monto  = "${PRECIO_CENTAVOS / 100}.00"
        val url = buildPayPalUrl(pagoId, monto)
        openUrl(url)
    }

    // ── Paso 3: procesar deep link cuando regrese ─────────────────────────────
    fun procesarDeepLink(uriStr: String) {
        val pagoId = _state.value.pagoActual?.id

        when {
            uriStr.startsWith(DEEP_LINK_SUCCESS) -> {
                val orderId = "paypal_${currentTimeMillis()}"
                if (pagoId != null) {
                    onPagoExitoso(pagoId, orderId)
                }
            }
            uriStr.startsWith(DEEP_LINK_CANCEL) -> {
                if (pagoId != null) {
                    onPagoCancelado(pagoId)
                }
            }
        }
    }

    // ── Confirma el pago en Firestore ─────────────────────────────────────────
    private fun onPagoExitoso(pagoId: String, paypalOrderId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true)
            repo.confirmarPago(pagoId, paypalOrderId).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        cargando       = false,
                        pagoCompletado = true,
                        pagoActual     = PagoTeleconsulta(id = pagoId)
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        cargando = false,
                        error    = "Error al confirmar pago: ${it.message}"
                    )
                }
            )
        }
    }

    fun onPagoCancelado(pagoId: String? = null) {
        val id = pagoId ?: _state.value.pagoActual?.id ?: return
        viewModelScope.launch {
            repo.cancelarPago(id)
            _state.value = _state.value.copy(
                pagoActual = null
            )
        }
    }

    fun limpiarError() { _state.value = _state.value.copy(error = null) }

    fun resetPago() { _state.value = PaymentUiState() }

    private fun buildPayPalUrl(pagoId: String, monto: String): String {
        return "https://www.sandbox.paypal.com/cgi-bin/webscr" +
                "?cmd=_xclick" +
                "&business=sb-smkko50999850@business.example.com" +
                "&item_name=Teleconsulta+NutriIA" +
                "&amount=$monto" +
                "&currency_code=$MONEDA" +
                "&custom=$pagoId" +
                "&return=${DEEP_LINK_SUCCESS}" +
                "&cancel_return=${DEEP_LINK_CANCEL}" +
                "&rm=1" // Redirección vía GET para asegurar compatibilidad con custom schemes en iOS
    }
}