package com.example.nutriia.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FechaUtils {
    private val formatFechaHoraUS = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
    private val formatFechaUS     = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val formatHoraUS      = SimpleDateFormat("HH:mm:ss", Locale.US)

    @Synchronized
    fun fechaHoraActual(): String = formatFechaHoraUS.format(Date())

    @Synchronized
    fun fechaActual(): String = formatFechaUS.format(Date())

    @Synchronized
    fun horaActual(): String = formatHoraUS.format(Date())

    @Synchronized
    fun formatearFecha(date: Date): String = formatFechaUS.format(date)

    @Synchronized
    fun formatearFecha(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return formatFechaUS.format(Date(epochMillis))
    }

    @Synchronized
    fun formatearHora(date: Date): String = formatHoraUS.format(date)

    @Synchronized
    fun formatearHora(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return formatHoraUS.format(Date(epochMillis))
    }

    @Synchronized
    fun formatearFechaHora(date: Date): String = formatFechaHoraUS.format(date)

    @Synchronized
    fun formatearFechaHora(epochMillis: Long): String {
        if (epochMillis <= 0L) return ""
        return formatFechaHoraUS.format(Date(epochMillis))
    }

    @Synchronized
    fun parsearFechaHora(fechaStr: String): Date? {
        val ms = parsearTextoAEpochMillis(fechaStr) ?: return null
        return Date(ms)
    }

    fun parsearTextoAEpochMillis(texto: String?): Long? {
        if (texto.isNullOrBlank()) return null
        val clean = texto.trim()

        // 1. Número en string (epoch ms o seg)
        clean.toLongOrNull()?.let { num ->
            return if (num > 100_000_000_000L) num else num * 1000L
        }

        // 2. Formato latinoamericano
        try {
            val d = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US).parse(clean)
            if (d != null) return d.time
        } catch (_: Exception) {}

        try {
            val d = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).parse(clean)
            if (d != null) return d.time
        } catch (_: Exception) {}

        try {
            val d = SimpleDateFormat("dd/MM/yyyy", Locale.US).parse(clean)
            if (d != null) return d.time
        } catch (_: Exception) {}

        // 3. Formato ISO
        try {
            val cleanIso = clean.replace("T", " ").replace("Z", "")
            val d = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).parse(cleanIso)
            if (d != null) return d.time
        } catch (_: Exception) {}

        try {
            val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(clean)
            if (d != null) return d.time
        } catch (_: Exception) {}

        return null
    }

    fun parsearEpochMillis(value: Any?): Long = when (value) {
        is Timestamp -> value.toDate().time
        is Date -> value.time
        is Number -> {
            val n = value.toLong()
            if (n > 100_000_000_000L) n else n * 1000L
        }
        is String -> parsearTextoAEpochMillis(value) ?: System.currentTimeMillis()
        else -> 0L
    }
}

