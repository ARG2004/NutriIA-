package com.example.nutriia.utils

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

object FechaUtils {
    fun fechaHoraActual(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.dayOfMonth.toString().padStart(2, '0')}/${now.monthNumber.toString().padStart(2, '0')}/${now.year} ${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
    }

    fun fechaActual(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.dayOfMonth.toString().padStart(2, '0')}/${now.monthNumber.toString().padStart(2, '0')}/${now.year}"
    }

    fun horaActual(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}:${now.second.toString().padStart(2, '0')}"
    }

    fun hoyIso(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.year}-${now.monthNumber.toString().padStart(2, '0')}-${now.dayOfMonth.toString().padStart(2, '0')}"
    }

    fun horaActualIso(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return "${now.hour.toString().padStart(2, '0')}:${now.minute.toString().padStart(2, '0')}"
    }

    fun formatearFecha(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return try {
            val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
            "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year}"
        } catch (_: Exception) { "" }
    }

    fun formatearHora(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return try {
            val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
            "${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}:${date.second.toString().padStart(2, '0')}"
        } catch (_: Exception) { "" }
    }

    fun formatearFechaHora(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return try {
            val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
            "${date.dayOfMonth.toString().padStart(2, '0')}/${date.monthNumber.toString().padStart(2, '0')}/${date.year} ${date.hour.toString().padStart(2, '0')}:${date.minute.toString().padStart(2, '0')}:${date.second.toString().padStart(2, '0')}"
        } catch (_: Exception) { "" }
    }

    /**
     * Parsea cualquier formato de fecha o timestamp en String a milisegundos de forma segura.
     */
    fun parsearFechaHora(texto: String?): Long {
        return parsearTextoAEpochMillis(texto) ?: Clock.System.now().toEpochMilliseconds()
    }

    fun parsearTextoAEpochMillis(texto: String?): Long? {
        if (texto.isNullOrBlank()) return null
        val clean = texto.trim()

        // 1. Número en string (epoch ms o seg)
        clean.toLongOrNull()?.let { num ->
            return if (num > 100_000_000_000L) num else num * 1000L
        }

        // 2. ISO Instant estándar (ej. 2026-09-11T21:40:00Z)
        try {
            return Instant.parse(clean).toEpochMilliseconds()
        } catch (_: Exception) {}

        // 3. Formato latinoamericano: dd/MM/yyyy HH:mm:ss o dd/MM/yyyy HH:mm o dd/MM/yyyy
        if (clean.contains("/")) {
            try {
                val parts = clean.split(" ")
                val datePart = parts[0].split("/")
                if (datePart.size == 3) {
                    val day = datePart[0].toIntOrNull() ?: 1
                    val month = datePart[1].toIntOrNull() ?: 1
                    val year = datePart[2].toIntOrNull() ?: 2026

                    var hour = 0
                    var minute = 0
                    var second = 0
                    if (parts.size > 1) {
                        val timeParts = parts[1].split(":")
                        if (timeParts.isNotEmpty()) hour = timeParts[0].toIntOrNull() ?: 0
                        if (timeParts.size > 1) minute = timeParts[1].toIntOrNull() ?: 0
                        if (timeParts.size > 2) second = timeParts[2].toIntOrNull() ?: 0
                    }
                    val ldt = LocalDateTime(year, month, day, hour, minute, second)
                    return ldt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                }
            } catch (_: Exception) {}
        }

        // 4. Formato ISO fecha: yyyy-MM-dd HH:mm:ss o yyyy-MM-dd
        if (clean.contains("-")) {
            try {
                val parts = clean.replace("T", " ").split(" ")
                val datePart = parts[0].split("-")
                if (datePart.size == 3) {
                    val year = datePart[0].toIntOrNull() ?: 2026
                    val month = datePart[1].toIntOrNull() ?: 1
                    val day = datePart[2].toIntOrNull() ?: 1

                    var hour = 0
                    var minute = 0
                    var second = 0
                    if (parts.size > 1) {
                        val timeParts = parts[1].split(":")
                        if (timeParts.isNotEmpty()) hour = timeParts[0].toIntOrNull() ?: 0
                        if (timeParts.size > 1) minute = timeParts[1].toIntOrNull() ?: 0
                        if (timeParts.size > 2) second = timeParts[2].split(".")[0].toIntOrNull() ?: 0
                    }
                    val ldt = LocalDateTime(year, month, day, hour, minute, second)
                    return ldt.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                }
            } catch (_: Exception) {}
        }

        return null
    }

    fun parsearTextoATimestamp(texto: String?): com.example.nutriia.shared.Timestamp? {
        val ms = parsearTextoAEpochMillis(texto) ?: return null
        return com.example.nutriia.shared.Timestamp(ms / 1000L, ((ms % 1000L) * 1_000_000).toInt())
    }
}
