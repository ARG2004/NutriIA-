package com.example.nutriia.teleconsulta

import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════════════
// MODELOS DE TELECONSULTA — con campos de señalización WebRTC
// ══════════════════════════════════════════════════════

@Serializable
enum class EstadoLlamada {
    INICIANDO,   // Creando documento en Firestore
    SONANDO,     // Esperando respuesta del receptor
    ACTIVA,      // En llamada
    FINALIZADA,  // Llamada terminada normalmente
    RECHAZADA,   // Receptor rechazó
    PERDIDA,     // Sin respuesta (timeout)
    OCUPADO      // Ya en otra llamada
}

@Serializable
enum class TipoLlamada { AUDIO, VIDEO }

// ─── Candidate ICE serializable para Firestore ────────────────────────────────
@Serializable
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
        fun fromMap(map: Map<String, Any?>): IceCandidateData {
            val indexRaw = map["sdpMLineIndex"]
            val index = when (indexRaw) {
                is Number -> indexRaw.toInt()
                is String -> indexRaw.toIntOrNull() ?: 0
                else -> 0
            }
            return IceCandidateData(
                sdpMid        = map["sdpMid"]?.toString() ?: "",
                sdpMLineIndex = index,
                sdp           = map["sdp"]?.toString() ?: ""
            )
        }
    }
}

// ─── Documento principal de llamada ──────────────────────────────────────────
@Serializable
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
        private fun parseLong(value: Any?): Long = when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }

        private fun parseInt(value: Any?): Int = when (value) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }

        fun fromMap(id: String, map: Map<String, Any?>): SolicitudLlamada = SolicitudLlamada(
            id               = id,
            emisorUid        = map["emisorUid"]?.toString() ?: "",
            receptorUid      = map["receptorUid"]?.toString() ?: "",
            nutriologoUid    = map["nutriologoUid"]?.toString() ?: "",
            nutriologoNombre = map["nutriologoNombre"]?.toString() ?: "",
            padreUid         = map["padreUid"]?.toString() ?: "",
            padreNombre      = map["padreNombre"]?.toString() ?: "",
            childId          = map["childId"]?.toString() ?: "",
            childNombre      = map["childNombre"]?.toString() ?: "",
            tipo             = runCatching {
                TipoLlamada.valueOf(map["tipo"]?.toString() ?: "")
            }.getOrDefault(TipoLlamada.VIDEO),
            estado           = runCatching {
                EstadoLlamada.valueOf(map["estado"]?.toString() ?: "")
            }.getOrDefault(EstadoLlamada.INICIANDO),
            offerSdp         = map["offerSdp"]?.toString(),
            answerSdp        = map["answerSdp"]?.toString(),
            creadoEn         = parseLong(map["creadoEn"]),
            aceptadoEn       = map["aceptadoEn"]?.let { parseLong(it) },
            finalizadoEn     = map["finalizadoEn"]?.let { parseLong(it) },
            duracionSegundos = parseInt(map["duracionSegundos"])
        )
    }
}
