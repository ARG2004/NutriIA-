package com.example.nutriia.payment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.auth.RepositorioLogin
import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.openUrl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AISubUiState(
    val cargando: Boolean = false,
    val transactionId: String? = null,
    val pagoCompletado: Boolean = false,
    val error: String? = null
)

class AISubscriptionViewModel : ViewModel() {

    private val authRepo = RepositorioLogin()

    private val _state = MutableStateFlow(AISubUiState())
    val state: StateFlow<AISubUiState> = _state.asStateFlow()

    companion object {
        const val PRECIO_CENTAVOS = 9900
        const val MONEDA          = "MXN"
        const val DEEP_LINK_SUCCESS = "nutriia://pago-ia-ok"
        const val DEEP_LINK_CANCEL  = "nutriia://pago-ia-cancel"
    }

    fun iniciarPago(uid: String) {
        val transId = "ai_sub_${uid}_${currentTimeMillis()}"
        _state.value = _state.value.copy(
            cargando = false,
            transactionId = transId,
            error = null,
            pagoCompletado = false
        )
    }

    fun abrirPayPalEnNavegador() {
        val transId = _state.value.transactionId ?: return
        val monto  = "${PRECIO_CENTAVOS / 100}.00"
        val url = buildPayPalUrl(transId, monto)
        openUrl(url)
    }

    fun procesarDeepLink(uriStr: String, uid: String) {
        when {
            uriStr.startsWith(DEEP_LINK_SUCCESS) || uriStr.contains("pago-ia-ok") -> {
                onPagoExitoso(uid)
            }
            uriStr.startsWith(DEEP_LINK_CANCEL) || uriStr.contains("pago-ia-cancel") -> {
                onPagoCancelado()
            }
        }
    }

    private fun onPagoExitoso(uid: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(cargando = true)
            val success = authRepo.activarSuscripcionIa(uid)
            if (success) {
                _state.value = _state.value.copy(
                    cargando = false,
                    pagoCompletado = true
                )
            } else {
                _state.value = _state.value.copy(
                    cargando = false,
                    error = "Error al activar suscripción"
                )
            }
        }
    }

    fun onPagoCancelado() {
        _state.value = _state.value.copy(
            transactionId = null,
            error = "Pago cancelado por el usuario"
        )
    }

    fun limpiarError() { _state.value = _state.value.copy(error = null) }
    fun reset() { _state.value = AISubUiState() }

    private fun buildPayPalUrl(transId: String, monto: String): String {
        return "https://www.sandbox.paypal.com/cgi-bin/webscr" +
                "?cmd=_xclick" +
                "&business=sb-smkko50999850@business.example.com" +
                "&item_name=Desbloqueo+IA+NutriIA+Mensual" +
                "&amount=$monto" +
                "&currency_code=$MONEDA" +
                "&custom=$transId" +
                "&return=${DEEP_LINK_SUCCESS}?tipo=ia" +
                "&cancel_return=${DEEP_LINK_CANCEL}?tipo=ia" +
                "&rm=1"
    }
}
