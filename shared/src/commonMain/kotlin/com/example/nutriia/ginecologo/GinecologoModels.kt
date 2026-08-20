package com.example.nutriia.ginecologo

import com.example.nutriia.platform.currentTimeMillis
import kotlinx.serialization.Serializable

// ─── Estado de la vinculación de embarazo ──────────────────────────────────────
@Serializable
enum class EstadoVinculacionEmbarazo { PENDIENTE, ACTIVO, RECHAZADO, REVOCADO }

// ─── Vinculación entre ginecólogo y mamá primeriza ────────────────────────────
@Serializable
data class VinculacionEmbarazo(
    val id:               String                    = "",
    val ginecologoUid:    String                    = "",
    val ginecologoNombre: String                    = "",
    val mamaUid:          String                    = "",
    val mamaNombre:       String                    = "",
    val estado:           EstadoVinculacionEmbarazo = EstadoVinculacionEmbarazo.PENDIENTE,
    val creadoEn:         Long?                     = null,
    val actualizadoEn:    Long?                     = null,
    val proximaCitaFecha:  String                   = "",
    val proximaCitaHora:   String                   = "",
    val proximaCitaMotivo: String                   = "",
    val proximaCitaTipo:   String                   = "" // "TELECONSULTA" o "PRESENCIAL"
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id"               to id,
        "ginecologoUid"    to ginecologoUid,
        "ginecologoNombre" to ginecologoNombre,
        "mamaUid"          to mamaUid,
        "mamaNombre"       to mamaNombre,
        "estado"           to estado.name,
        "creadoEn"         to (creadoEn ?: currentTimeMillis()),
        "actualizadoEn"    to currentTimeMillis(),
        "proximaCitaFecha"  to proximaCitaFecha,
        "proximaCitaHora"   to proximaCitaHora,
        "proximaCitaMotivo" to proximaCitaMotivo,
        "proximaCitaTipo"   to proximaCitaTipo
    )

    companion object {
        fun docId(ginecologoUid: String, mamaUid: String) =
            "${ginecologoUid}_${mamaUid}"

        fun fromMap(id: String, map: Map<String, Any?>): VinculacionEmbarazo = VinculacionEmbarazo(
            id               = id,
            ginecologoUid    = map["ginecologoUid"]    as? String ?: "",
            ginecologoNombre = map["ginecologoNombre"] as? String ?: "",
            mamaUid          = map["mamaUid"]          as? String ?: "",
            mamaNombre       = map["mamaNombre"]       as? String ?: "",
            estado           = runCatching {
                EstadoVinculacionEmbarazo.valueOf(map["estado"] as? String ?: "")
            }.getOrDefault(EstadoVinculacionEmbarazo.PENDIENTE),
            creadoEn         = map["creadoEn"]         as? Long,
            actualizadoEn    = map["actualizadoEn"]    as? Long,
            proximaCitaFecha  = map["proximaCitaFecha"]  as? String ?: "",
            proximaCitaHora   = map["proximaCitaHora"]   as? String ?: "",
            proximaCitaMotivo = map["proximaCitaMotivo"] as? String ?: "",
            proximaCitaTipo   = map["proximaCitaTipo"]   as? String ?: ""
        )
    }
}

// ─── Perfil público del ginecólogo ──────────────────────────────────────────
@Serializable
data class GinecologoPublico(
    val uid:          String = "",
    val nombre:       String = "",
    val especialidad: String = "",
    val cedula:       String = "",
    val codigo:       String = "",
    val email:        String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid"          to uid,
        "nombre"       to nombre,
        "especialidad" to especialidad,
        "cedula"       to cedula,
        "codigo"       to codigo,
        "email"        to email
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, fallbackId: String = ""): GinecologoPublico = GinecologoPublico(
            uid          = (map["uid"] as? String ?: "").takeIf { it.isNotBlank() } ?: fallbackId,
            nombre       = map["nombre"]        as? String ?: "",
            especialidad = map["especialidad"]  as? String ?: "",
            cedula       = map["cedula"]        as? String ?: "",
            codigo       = map["codigo"]        as? String ?: "",
            email        = map["email"]         as? String ?: ""
        )
    }
}
