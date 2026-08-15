package com.example.nutriia.payment

import com.google.firebase.Timestamp

enum class EstadoPago { PENDIENTE, COMPLETADO, FALLIDO, REEMBOLSADO }

data class PagoTeleconsulta(
    val id:              String        = "",
    val padreUid:        String        = "",
    val nutriologoUid:   String        = "",
    val childId:         String        = "",
    val montoCentavos:   Int           = 0,       // en centavos: 15000 = $150.00 MXN
    val moneda:          String        = "MXN",
    val paypalOrderId:   String        = "",
    val estado:          EstadoPago    = EstadoPago.PENDIENTE,
    val creadaEn:        Timestamp?    = null,
    val completadoEn:    Timestamp?    = null,
    val llamadaId:       String        = ""       // Vincula el pago a una llamada específica para marcarlo como usado
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id"            to id,
        "padreUid"      to padreUid,
        "nutriologoUid" to nutriologoUid,
        "childId"       to childId,
        "montoCentavos" to montoCentavos,
        "moneda"        to moneda,
        "paypalOrderId" to paypalOrderId,
        "estado"        to estado.name,
        "creadaEn"      to (creadaEn ?: Timestamp.now()),
        "completadoEn"  to completadoEn,
        "llamadaId"     to llamadaId
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): PagoTeleconsulta = PagoTeleconsulta(
            id            = id,
            padreUid      = map["padreUid"]      as? String ?: "",
            nutriologoUid = map["nutriologoUid"] as? String ?: "",
            childId       = map["childId"]       as? String ?: "",
            montoCentavos = (map["montoCentavos"] as? Long)?.toInt() ?: 0,
            moneda        = map["moneda"]        as? String ?: "MXN",
            paypalOrderId = map["paypalOrderId"] as? String ?: "",
            estado        = runCatching {
                EstadoPago.valueOf(map["estado"] as? String ?: "")
            }.getOrDefault(EstadoPago.PENDIENTE),
            creadaEn      = map["creadaEn"]     as? Timestamp,
            completadoEn  = map["completadoEn"] as? Timestamp,
            llamadaId     = map["llamadaId"]     as? String ?: ""
        )
    }
}