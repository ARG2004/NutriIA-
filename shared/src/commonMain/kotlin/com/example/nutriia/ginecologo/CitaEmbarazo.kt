package com.example.nutriia.ginecologo

import com.google.firebase.Timestamp

data class CitaEmbarazo(
    val id: String = "",
    val fecha: String = "",
    val hora: String = "",
    val motivo: String = "",
    val tipo: String = "", // "TELECONSULTA" | "PRESENCIAL"
    val ginecologoUid: String = "",
    val ginecologoNombre: String = "",
    val creadoEn: Timestamp? = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "fecha" to fecha,
        "hora" to hora,
        "motivo" to motivo,
        "tipo" to tipo,
        "ginecologoUid" to ginecologoUid,
        "ginecologoNombre" to ginecologoNombre,
        "creadoEn" to (creadoEn ?: Timestamp.now())
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): CitaEmbarazo = CitaEmbarazo(
            id = id,
            fecha = map["fecha"] as? String ?: "",
            hora = map["hora"] as? String ?: "",
            motivo = map["motivo"] as? String ?: "",
            tipo = map["tipo"] as? String ?: "",
            ginecologoUid = map["ginecologoUid"] as? String ?: "",
            ginecologoNombre = map["ginecologoNombre"] as? String ?: "",
            creadoEn = map["creadoEn"] as? Timestamp
        )
    }
}
