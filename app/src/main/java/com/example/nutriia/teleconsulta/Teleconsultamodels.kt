package com.example.nutriia.teleconsulta

import androidx.annotation.Keep

// ══════════════════════════════════════════════════════
// MODELOS DE TELECONSULTA — con campos de señalización WebRTC
// ══════════════════════════════════════════════════════

@Keep
enum class EstadoLlamada {
    INICIANDO,   // Creando documento en Firestore
    SONANDO,     // Esperando respuesta del receptor
    ACTIVA,      // En llamada
    FINALIZADA,  // Llamada terminada normalmente
    RECHAZADA,   // Receptor rechazó
    PERDIDA,     // Sin respuesta (timeout)
    OCUPADO      // Ya en otra llamada
}

@Keep
enum class TipoLlamada { AUDIO, VIDEO }

// ─── Candidate ICE serializable para Firestore ────────────────────────────────
@Keep
data class IceCandidateData(
    val sdpMid:        String = "",
    val sdpMLineIndex: Int    = 0,
    val sdp:           String = ""
) {
    fun toMap(): Map<String, Any> = mapOf(
        "sdpMid"        to sdpMid,
        "sdpMLineIndex" to sdpMLineIndex,
        "sdp"           to sdp
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): IceCandidateData = IceCandidateData(
            sdpMid        = map["sdpMid"]        as? String ?: "",
            sdpMLineIndex = (map["sdpMLineIndex"] as? Long)?.toInt() ?: 0,
            sdp           = map["sdp"]           as? String ?: ""
        )
    }
}

// ─── Documento principal de llamada ──────────────────────────────────────────
@Keep
data class SolicitudLlamada(
    val id:               String        = "",
    val emisorUid:        String        = "", // Quién inicia (UID)
    val receptorUid:      String        = "", // Quién debe recibir (UID)
    val nutriologoUid:    String        = "",
    val nutriologoNombre: String        = "",
    val padreUid:         String        = "",
    val padreNombre:      String        = "",
    val childId:          String        = "",
    val childNombre:      String        = "",
    val tipo:             TipoLlamada   = TipoLlamada.VIDEO,
    val estado:           EstadoLlamada = EstadoLlamada.INICIANDO,

    // ── Señalización WebRTC ──────────────────────────────────────────────────
    val offerSdp:     String? = null,
    val answerSdp:    String? = null,

    // ── Metadatos ────────────────────────────────────────────────────────────
    val creadoEn:         Long  = 0L,
    val aceptadoEn:       Long? = null,
    val finalizadoEn:     Long? = null,
    val duracionSegundos: Int   = 0
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "emisorUid"        to emisorUid,
        "receptorUid"      to receptorUid,
        "nutriologoUid"    to nutriologoUid,
        "nutriologoNombre" to nutriologoNombre,
        "padreUid"         to padreUid,
        "padreNombre"      to padreNombre,
        "childId"          to childId,
        "childNombre"      to childNombre,
        "tipo"             to tipo.name,
        "estado"           to estado.name,
        "offerSdp"         to offerSdp,
        "answerSdp"        to answerSdp,
        "creadoEn"         to creadoEn,
        "aceptadoEn"       to aceptadoEn,
        "finalizadoEn"     to finalizadoEn,
        "duracionSegundos" to duracionSegundos
    )

    companion object {
        fun fromMap(id: String, map: Map<String, Any?>): SolicitudLlamada = SolicitudLlamada(
            id               = id,
            emisorUid        = map["emisorUid"]        as? String ?: "",
            receptorUid      = map["receptorUid"]      as? String ?: "",
            nutriologoUid    = map["nutriologoUid"]    as? String ?: "",
            nutriologoNombre = map["nutriologoNombre"] as? String ?: "",
            padreUid         = map["padreUid"]         as? String ?: "",
            padreNombre      = map["padreNombre"]      as? String ?: "",
            childId          = map["childId"]          as? String ?: "",
            childNombre      = map["childNombre"]      as? String ?: "",
            tipo             = runCatching {
                TipoLlamada.valueOf(map["tipo"] as? String ?: "")
            }.getOrDefault(TipoLlamada.VIDEO),
            estado           = runCatching {
                EstadoLlamada.valueOf(map["estado"] as? String ?: "")
            }.getOrDefault(EstadoLlamada.INICIANDO),
            offerSdp         = map["offerSdp"]         as? String,
            answerSdp        = map["answerSdp"]        as? String,
            creadoEn         = map["creadoEn"]         as? Long   ?: 0L,
            aceptadoEn       = map["aceptadoEn"]       as? Long,
            finalizadoEn     = map["finalizadoEn"]     as? Long,
            duracionSegundos = (map["duracionSegundos"] as? Long)?.toInt() ?: 0
        )
    }
}
