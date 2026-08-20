package com.example.nutriia.alerta

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.utils.FechaUtils

// ─── Tipos de alerta ──────────────────────────────────────────────────────────

enum class TipoAlerta(
    val label:  String,
    val icon:   ImageVector,
    val color:  Color
) {
    TOMA_COMIDA(
        label = "Toma / Comida",
        icon  = Icons.Rounded.ChildCare,
        color = Color(0xFFEC9BBF)
    ),
    VACUNA(
        label = "Vacuna",
        icon  = Icons.Rounded.Vaccines,
        color = Color(0xFF42A5F5)
    ),
    CITA_MEDICA(
        label = "Cita médica",
        icon  = Icons.Rounded.LocalHospital,
        color = Color(0xFF66BB6A)
    ),
    MEDICION(
        label = "Medición",
        icon  = Icons.Rounded.MonitorWeight,
        color = Color(0xFFFF8F00)
    )
}

enum class DiasSemana(val label: String, val short: String) {
    LUNES(    "Lunes",     "L"),
    MARTES(   "Martes",    "M"),
    MIERCOLES("Miércoles", "X"),
    JUEVES(   "Jueves",    "J"),
    VIERNES(  "Viernes",   "V"),
    SABADO(   "Sábado",    "S"),
    DOMINGO(  "Domingo",   "D")
}

// ─── Modelo de Alerta ─────────────────────────────────────────────────────────

data class Alerta(
    val id:          String           = generateUUID(),
    val childId:     String           = "",
    val childName:   String           = "",
    val tipo:        TipoAlerta       = TipoAlerta.TOMA_COMIDA,
    val titulo:      String           = "",
    val descripcion: String           = "",
    val hora:        String           = "08:00",              // "HH:mm"
    val diasSemana:  List<DiasSemana> = DiasSemana.entries.toList(),
    val fechaUnica:  String?          = null,                 // "DD/MM/YYYY"
    val activa:      Boolean          = true,
    val creadoEn:    Long             = currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> = mapOf(
        "id"          to id,
        "childId"     to childId,
        "childName"   to childName,
        "tipo"        to tipo.name,
        "titulo"      to titulo,
        "descripcion" to descripcion,
        "hora"        to hora,
        "diasSemana"  to diasSemana.map { it.name },
        "fechaUnica"  to fechaUnica,
        "activa"      to activa,
        "creadoEn"    to creadoEn,
        "fechaCreacion" to FechaUtils.formatearFecha(creadoEn),
        "horaCreacion"  to FechaUtils.formatearHora(creadoEn)
    )

    companion object {
        fun fromMap(map: Map<String, Any?>): Alerta {
            @Suppress("UNCHECKED_CAST")
            val diasRaw = map["diasSemana"] as? List<String> ?: emptyList()
            return Alerta(
                id          = map["id"]          as? String ?: generateUUID(),
                childId     = map["childId"]     as? String ?: "",
                childName   = map["childName"]   as? String ?: "",
                tipo        = TipoAlerta.entries.find { it.name == map["tipo"] } ?: TipoAlerta.TOMA_COMIDA,
                titulo      = map["titulo"]      as? String ?: "",
                descripcion = map["descripcion"] as? String ?: "",
                hora        = map["hora"]        as? String ?: "08:00",
                diasSemana  = diasRaw.mapNotNull { n -> DiasSemana.entries.find { it.name == n } },
                fechaUnica  = map["fechaUnica"]  as? String,
                activa      = map["activa"]      as? Boolean ?: true,
                creadoEn    = (map["creadoEn"] as? Number)?.toLong() ?: currentTimeMillis()
            )
        }
    }
}