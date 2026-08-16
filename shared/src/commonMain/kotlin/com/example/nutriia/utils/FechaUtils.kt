package com.example.nutriia.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FechaUtils {
    private val formatFechaHoraUS = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
    private val formatFechaUS     = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    private val formatHoraUS      = SimpleDateFormat("HH:mm:ss", Locale.US)

    fun fechaHoraActual(): String = formatFechaHoraUS.format(Date())
    fun fechaActual(): String = formatFechaUS.format(Date())
    fun horaActual(): String = formatHoraUS.format(Date())
    fun formatearFecha(date: Date): String = formatFechaUS.format(date)
    fun formatearHora(date: Date): String = formatHoraUS.format(date)
    fun formatearFechaHora(date: Date): String = formatFechaHoraUS.format(date)
    fun parsearFechaHora(fechaStr: String): Date? = runCatching { formatFechaHoraUS.parse(fechaStr) }.getOrNull()
}
