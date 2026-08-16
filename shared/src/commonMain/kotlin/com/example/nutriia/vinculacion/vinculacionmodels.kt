package com.example.nutriia.vinculacion

import com.example.nutriia.platform.currentTimeMillis

// ─── Estado de la vinculación ─────────────────────────────────────────────────
enum class EstadoVinculacion { PENDIENTE, ACTIVO, RECHAZADO, REVOCADO }

// ─── Vinculación entre nutriólogo y padre ─────────────────────────────────────
data class Vinculacion(
    val id:              String              = "",
    val nutriologoUid:   String              = "",
    val nutriologoNombre:String              = "",
    val padreUid:        String              = "",
    val padreNombre:     String              = "",
    val childId:         String              = "",   // hijo al que aplica la vinculación
    val childNombre:     String              = "",
    val estado:          EstadoVinculacion   = EstadoVinculacion.PENDIENTE,
    val creadoEn:        Long?               = null,
    val actualizadoEn:   Long?               = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id"               to id,
        "nutriologoUid"    to nutriologoUid,
        "nutriologoNombre" to nutriologoNombre,
        "padreUid"         to padreUid,
        "padreNombre"      to padreNombre,
        "childId"          to childId,
        "childNombre"      to childNombre,
        "estado"           to estado.name,
        "creadoEn"         to (creadoEn ?: currentTimeMillis()),
        "actualizadoEn"    to currentTimeMillis()
    )

    companion object {
        fun docId(nutriologoUid: String, padreUid: String) =
            "${nutriologoUid}_${padreUid}"

        fun fromMap(id: String, map: Map<String, Any?>): Vinculacion = Vinculacion(
            id               = id,
            nutriologoUid    = map["nutriologoUid"]    as? String ?: "",
            nutriologoNombre = map["nutriologoNombre"] as? String ?: "",
            padreUid         = map["padreUid"]         as? String ?: "",
            padreNombre      = map["padreNombre"]       as? String ?: "",
            childId          = map["childId"]           as? String ?: "",
            childNombre      = map["childNombre"]       as? String ?: "",
            estado           = runCatching {
                EstadoVinculacion.valueOf(map["estado"] as? String ?: "")
            }.getOrDefault(EstadoVinculacion.PENDIENTE),
            creadoEn         = map["creadoEn"]      as? Long,
            actualizadoEn    = map["actualizadoEn"] as? Long
        )
    }
}

// ─── Perfil público del nutriólogo (colección nutriologos_publicos) ────────────
data class NutriologoPublico(
    val uid:         String = "",
    val nombre:      String = "",
    val especialidad:String = "",
    val cedula:      String = "",
    val codigo:      String = "",   // código único de vinculación (ej: NUTRI-A3X7F2)
    val email:       String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "uid"         to uid,
        "nombre"      to nombre,
        "especialidad" to especialidad,
        "cedula"      to cedula,
        "codigo"      to codigo,
        "email"       to email
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, fallbackId: String = ""): NutriologoPublico = NutriologoPublico(
            uid          = (map["uid"] as? String ?: "").takeIf { it.isNotBlank() } ?: fallbackId,
            nombre       = map["nombre"]        as? String ?: "",
            especialidad = map["especialidad"]  as? String ?: "",
            cedula       = map["cedula"]        as? String ?: "",
            codigo       = map["codigo"]        as? String ?: "",
            email        = map["email"]         as? String ?: ""
        )
    }
}

// ─── Plan alimentario creado por el nutriólogo ────────────────────────────────
data class PlanAlimentario(
    val id:              String    = "",
    val childId:         String    = "",
    val padreUid:        String    = "",
    val nutriologoUid:   String    = "",
    val nutriologoNombre:String    = "",
    val titulo:          String    = "",
    val descripcion:     String    = "",
    val comidas:         List<ComidaPlan> = emptyList(),
    val fechaInicio:     String    = "",
    val fechaFin:        String    = "",
    val activo:          Boolean   = true,
    val creadoEn:        Long?     = null
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id"               to id,
        "childId"          to childId,
        "padreUid"         to padreUid,
        "nutriologoUid"    to nutriologoUid,
        "nutriologoNombre" to nutriologoNombre,
        "titulo"           to titulo,
        "descripcion"      to descripcion,
        "comidas"          to comidas.map { it.toMap() },
        "fechaInicio"      to fechaInicio,
        "fechaFin"         to fechaFin,
        "activo"           to activo,
        "creadoEn"         to (creadoEn ?: currentTimeMillis())
    )

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun fromMap(id: String, map: Map<String, Any?>): PlanAlimentario = PlanAlimentario(
            id               = id,
            childId          = map["childId"]          as? String ?: "",
            padreUid         = map["padreUid"]         as? String ?: "",
            nutriologoUid    = map["nutriologoUid"]    as? String ?: "",
            nutriologoNombre = map["nutriologoNombre"] as? String ?: "",
            titulo           = map["titulo"]           as? String ?: "",
            descripcion      = map["descripcion"]      as? String ?: "",
            comidas          = (map["comidas"] as? List<Map<String, Any?>>)
                ?.map { ComidaPlan.fromMap(it) } ?: emptyList(),
            fechaInicio      = map["fechaInicio"]      as? String ?: "",
            fechaFin         = map["fechaFin"]         as? String ?: "",
            activo           = map["activo"]           as? Boolean ?: true,
            creadoEn         = map["creadoEn"]         as? Long
        )
    }
}

data class ComidaPlan(
    val momento:    String = "",
    val alimentos:  String = "",
    val porcion:    String = "",
    val notas:      String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "momento"   to momento,
        "alimentos" to alimentos,
        "porcion"   to porcion,
        "notas"     to notas
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): ComidaPlan = ComidaPlan(
            momento   = map["momento"]   as? String ?: "",
            alimentos = map["alimentos"] as? String ?: "",
            porcion   = map["porcion"]   as? String ?: "",
            notas     = map["notas"]     as? String ?: ""
        )
    }
}