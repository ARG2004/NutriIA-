package com.example.nutriia.payment

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
        // $150.00 MXN — ajusta según tu modelo de negocio
        const val PRECIO_CENTAVOS = 25000
        const val MONEDA          = "MXN"

        // Deep links — deben coincidir exactamente con el AndroidManifest
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
    // Usa el navegador nativo para que PayPal pueda redirigir correctamente
    // al deep link nutriia://pago-ok cuando el pago se complete.
    fun abrirPayPalEnNavegador(context: Context) {
        val pagoId = _state.value.pagoActual?.id ?: return
        val monto  = "%.2f".format(PRECIO_CENTAVOS / 100.0)

        // Construye la URL de PayPal.me — la más simple, sin backend.
        // En Sandbox: usa una cuenta Business de PayPal con el link de pago.
        // En Producción: reemplaza con tu usuario real de PayPal.me
        //
        // Alternativa más robusta: genera una Order desde una Cloud Function
        // y usa el approval_url que devuelve la PayPal Orders API v2.
        val url = buildPayPalUrl(pagoId, monto)

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    // ── Paso 3: se llama desde MainActivity.onNewIntent cuando PayPal redirige ─
    fun procesarDeepLink(uri: Uri) {
        val uriStr = uri.toString()
        val pagoId = uri.getQueryParameter("pagoId")
            ?: _state.value.pagoActual?.id

        when {
            uriStr.startsWith(DEEP_LINK_SUCCESS) -> {
                // El token es el PayPal Order ID (en Sandbox viene como ?token=XXXX)
                val orderId = uri.getQueryParameter("token")
                    ?: uri.getQueryParameter("orderId")
                    ?: "manual_${System.currentTimeMillis()}"
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

    // ─────────────────────────────────────────────────────────────────────────
    // URL de PayPal
    // ─────────────────────────────────────────────────────────────────────────
    private fun buildPayPalUrl(pagoId: String, monto: String): String {
        // OPCIÓN A — PayPal.me (producción, sin backend, más simple):
        // Solo requiere una cuenta de PayPal Business con el link activado.
        // El usuario paga y tú confirmas manualmente desde tu dashboard de PayPal.
        // No hay redirect automático → sirve para demos/MVP.
        //
        //   return "https://www.paypal.me/TUUSUARIO/$monto$MONEDA"

        // OPCIÓN B — PayPal Checkout Sandbox con redirect (RECOMENDADA para pruebas):
        // Requiere crear una app en developer.paypal.com y obtener un Client ID.
        // Esta URL abre el sandbox de PayPal y redirige al deep link al completar.
        //
        // Pasos:
        // 1. Ve a https://developer.paypal.com/dashboard/
        // 2. Crea una app Sandbox → obtén el Client ID
        // 3. En "Return URLs" de tu app, registra: nutriia://pago-ok
        // 4. Reemplaza CLIENT_ID_SANDBOX abajo con tu Client ID real
        //
        // Por ahora esta URL es funcional en Sandbox si sustituyes el Client ID:

        val successEncoded = Uri.encode("$DEEP_LINK_SUCCESS?pagoId=$pagoId")
        val cancelEncoded  = Uri.encode("$DEEP_LINK_CANCEL?pagoId=$pagoId")

        // Esta URL abre el flujo de pago estándar de PayPal Sandbox
        // token=EC-XXXX es generado por tu backend en producción real;
        // para sandbox sin backend usa el link de PayPal.me directamente.
        return "https://www.sandbox.paypal.com/cgi-bin/webscr" +
                "?cmd=_xclick" +
                "&business=sb-smkko50999850@business.example.com" +// ← tu email de cuenta Business Sandbox
                "&item_name=${Uri.encode("Teleconsulta NutriIA")}" +
                "&amount=$monto" +
                "&currency_code=$MONEDA" +
                "&return=$successEncoded" +
                "&cancel_return=$cancelEncoded" +
                "&custom=${Uri.encode(pagoId)}"

        // ─── OPCIÓN MÁS FÁCIL PARA DEMOSTRAR SIN CONFIGURAR NADA ────────────
        // Comenta el return de arriba y descomenta esta línea.
        // Abre el sandbox de PayPal en el navegador, el usuario "paga" con
        // una cuenta de prueba y tú validas viendo los logs de Firestore:
        //
        // return "https://www.sandbox.paypal.com/checkoutnow?token=DEMO_TOKEN"
    }
}