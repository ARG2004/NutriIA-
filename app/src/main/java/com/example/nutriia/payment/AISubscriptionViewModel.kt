package com.example.nutriia.payment

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.auth.RepositorioLogin
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

class AISubscriptionViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepo = RepositorioLogin(application)

    private val _state = MutableStateFlow(AISubUiState())
    val state: StateFlow<AISubUiState> = _state.asStateFlow()

    companion object {
        const val PRECIO_CENTAVOS = 9900
        const val MONEDA          = "MXN"
        const val DEEP_LINK_SUCCESS = "nutriia://pago-ia-ok"
        const val DEEP_LINK_CANCEL  = "nutriia://pago-ia-cancel"
    }

    fun iniciarPago(uid: String) {
        val transId = "ai_sub_${uid}_${System.currentTimeMillis()}"
        _state.value = _state.value.copy(
            cargando = false,
            transactionId = transId,
            error = null,
            pagoCompletado = false
        )
    }

    fun iniciarPagoIA(context: Context, uid: String) {
        iniciarPago(uid)
        abrirPayPalEnNavegador(context)
    }

    fun abrirPayPalEnNavegador(context: Context) {
        val transId = _state.value.transactionId ?: return
        val monto  = "${PRECIO_CENTAVOS / 100}.00"
        val url = buildPayPalUrl(transId, monto)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun procesarDeepLink(uriStr: String, uid: String) {
        when {
            uriStr.startsWith(DEEP_LINK_SUCCESS) -> {
                onPagoExitoso(uid)
            }
            uriStr.startsWith(DEEP_LINK_CANCEL) -> {
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
                "&return=${DEEP_LINK_SUCCESS}" +
                "&cancel_return=${DEEP_LINK_CANCEL}" +
                "&rm=1"
    }
}
