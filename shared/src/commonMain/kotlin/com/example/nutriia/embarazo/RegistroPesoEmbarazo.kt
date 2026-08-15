package com.example.nutriia.embarazo

import com.google.firebase.Timestamp

data class RegistroPesoEmbarazo(
    val id:               String     = "",
    val fecha:            String     = "",              // dd/MM/yyyy
    val semanaGestacion:  Int        = 0,
    val pesoActualKg:     Double     = 0.0,
    val alturaUterinaCm:  Double?    = null,            // opcional, solo desde semana 20
    val fuente:           String     = "auto_registro", // "auto_registro" | "consulta_medica"
    val notas:            String     = "",
    val creadoEn:         Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fecha" to fecha,
        "semanaGestacion" to semanaGestacion,
        "pesoActualKg" to pesoActualKg,
        "alturaUterinaCm" to alturaUterinaCm,
        "fuente" to fuente,
        "notas" to notas,
        "creadoEn" to (creadoEn ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): RegistroPesoEmbarazo = RegistroPesoEmbarazo(
            id = id,
            fecha = map["fecha"] as? String ?: "",
            semanaGestacion = (map["semanaGestacion"] as? Long)?.toInt() ?: (map["semanaGestacion"] as? Int) ?: 0,
            pesoActualKg = (map["pesoActualKg"] as? Double) ?: (map["pesoActualKg"] as? Long)?.toDouble() ?: (map["pesoActualKg"] as? Float)?.toDouble() ?: 0.0,
            alturaUterinaCm = (map["alturaUterinaCm"] as? Double) ?: (map["alturaUterinaCm"] as? Long)?.toDouble() ?: (map["alturaUterinaCm"] as? Float)?.toDouble(),
            fuente = map["fuente"] as? String ?: "auto_registro",
            notas = map["notas"] as? String ?: "",
            creadoEn = map["creadoEn"] as? Timestamp
        )
    }
}
