package com.example.nutriia.embarazo

import com.google.firebase.Timestamp

data class RegistroSintomasEmbarazo(
    val id: String = "",
    val fecha: String = "",
    val semanaGestacion: Int = 0,
    val sintomas: List<String> = emptyList(),
    val otrosSintomasTexto: String = "",
    val nivelSeveridadMaximo: String = "NORMAL", // "NORMAL", "CONSULTA", "URGENCIA"
    val creadoEn: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fecha" to fecha,
        "semanaGestacion" to semanaGestacion,
        "sintomas" to sintomas,
        "otrosSintomasTexto" to otrosSintomasTexto,
        "nivelSeveridadMaximo" to nivelSeveridadMaximo,
        "creadoEn" to (creadoEn ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): RegistroSintomasEmbarazo = RegistroSintomasEmbarazo(
            id = id,
            fecha = map["fecha"] as? String ?: "",
            semanaGestacion = (map["semanaGestacion"] as? Long)?.toInt() ?: (map["semanaGestacion"] as? Int) ?: 0,
            sintomas = (map["sintomas"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            otrosSintomasTexto = map["otrosSintomasTexto"] as? String ?: "",
            nivelSeveridadMaximo = map["nivelSeveridadMaximo"] as? String ?: "NORMAL",
            creadoEn = map["creadoEn"] as? Timestamp
        )
    }
}
