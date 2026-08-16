package com.example.nutriia.payment

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class PaymentRepository {

    private val pagosState = MutableStateFlow<Map<String, PagoTeleconsulta>>(emptyMap())

    suspend fun crearPagoPendiente(
        nutriologoUid: String,
        childId:       String,
        montoCentavos: Int,
        moneda:        String = "MXN",
        padreUid:      String = "padre_default",
        paypalOrderId: String = ""
    ): Result<PagoTeleconsulta> {
        val id = com.example.nutriia.platform.generateUUID()
        val pago = PagoTeleconsulta(
            id            = id,
            padreUid      = padreUid,
            nutriologoUid = nutriologoUid,
            childId       = childId,
            montoCentavos = montoCentavos,
            moneda        = moneda,
            paypalOrderId = paypalOrderId,
            estado        = EstadoPago.PENDIENTE
        )
        pagosState.value = pagosState.value + (id to pago)
        return Result.success(pago)
    }

    suspend fun confirmarPago(pagoId: String, paypalOrderId: String = ""): Result<Unit> {
        val current = pagosState.value[pagoId] ?: return Result.failure(Exception("No encontrado"))
        pagosState.value = pagosState.value + (pagoId to current.copy(
            estado = EstadoPago.COMPLETADO,
            paypalOrderId = paypalOrderId
        ))
        return Result.success(Unit)
    }

    suspend fun cancelarPago(pagoId: String): Result<Unit> {
        val current = pagosState.value[pagoId] ?: return Result.failure(Exception("No encontrado"))
        pagosState.value = pagosState.value + (pagoId to current.copy(estado = EstadoPago.FALLIDO))
        return Result.success(Unit)
    }

    suspend fun obtenerPagoVigente(padreUid: String, nutriologoUid: String): Result<PagoTeleconsulta?> {
        val pago = pagosState.value.values.firstOrNull {
            it.padreUid == padreUid && it.nutriologoUid == nutriologoUid && it.estado == EstadoPago.COMPLETADO && it.llamadaId.isEmpty()
        }
        return Result.success(pago)
    }

    suspend fun marcarPagoUsado(pagoId: String, llamadaId: String): Result<Unit> {
        val current = pagosState.value[pagoId]
        if (current != null) {
            pagosState.value = pagosState.value + (pagoId to current.copy(llamadaId = llamadaId))
        }
        return Result.success(Unit)
    }

    suspend fun reactivarPago(pagoId: String): Result<Unit> {
        val current = pagosState.value[pagoId]
        if (current != null) {
            pagosState.value = pagosState.value + (pagoId to current.copy(llamadaId = ""))
        }
        return Result.success(Unit)
    }
}
