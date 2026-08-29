package com.example.nutriia.payment

import kotlinx.serialization.Serializable

@Serializable
enum class EstadoPago { PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO }

@Serializable
data class PagoTeleconsulta(
    val id:            String     = "",
    val padreUid:      String     = "",
    val nutriologoUid: String     = "",
    val childId:       String     = "",
    val montoCentavos: Int        = 0,       // en centavos: 15000 = $150.00 MXN
    val moneda:        String     = "MXN",
    val paypalOrderId: String     = "",
    val estado:        EstadoPago = EstadoPago.PENDIENTE,
    val creadaEn:      Long       = 0L,
    val completadoEn:  Long       = 0L,
    val llamadaId:     String     = ""       // Vincula el pago a una llamada específica para marcarlo como usado
)