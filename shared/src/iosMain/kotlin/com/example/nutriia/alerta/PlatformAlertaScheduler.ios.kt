@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.alerta

import platform.Foundation.NSDateComponents
import platform.UserNotifications.*

actual object PlatformAlertaScheduler {

    actual fun programarAlerta(alerta: Alerta) {
        val center = UNUserNotificationCenter.currentNotificationCenter()

        val content = UNMutableNotificationContent()
        content.setTitle(alerta.titulo)
        content.setBody(alerta.mensaje)
        content.setSound(UNNotificationSound.defaultSound())

        val (horaInt, minutoInt) = parseHora(alerta.hora)

        if (alerta.fechaUnica != null) {
            val (anio, mes, dia) = parseFecha(alerta.fechaUnica)
            val dateComponents = NSDateComponents().apply {
                this.year = anio.toLong()
                this.month = mes.toLong()
                this.day = dia.toLong()
                this.hour = horaInt.toLong()
                this.minute = minutoInt.toLong()
                this.second = 0L
            }
            val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                dateComponents = dateComponents,
                repeats = false
            )
            val request = UNNotificationRequest.requestWithIdentifier(
                identifier = alerta.id,
                content = content,
                trigger = trigger
            )
            center.addNotificationRequest(request, null)
        } else {
            alerta.diasSemana.forEach { diaSemana ->
                val weekdayNum = when (diaSemana) {
                    DiasSemana.DOMINGO -> 1
                    DiasSemana.LUNES -> 2
                    DiasSemana.MARTES -> 3
                    DiasSemana.MIERCOLES -> 4
                    DiasSemana.JUEVES -> 5
                    DiasSemana.VIERNES -> 6
                    DiasSemana.SABADO -> 7
                }
                val dateComponents = NSDateComponents().apply {
                    this.weekday = weekdayNum.toLong()
                    this.hour = horaInt.toLong()
                    this.minute = minutoInt.toLong()
                    this.second = 0L
                }
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    dateComponents = dateComponents,
                    repeats = true
                )
                val reqId = "${alerta.id}_${diaSemana.name}"
                val request = UNNotificationRequest.requestWithIdentifier(
                    identifier = reqId,
                    content = content,
                    trigger = trigger
                )
                center.addNotificationRequest(request, null)
            }
        }
    }

    actual fun cancelarAlerta(alertaId: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val ids = mutableListOf(alertaId)
        DiasSemana.entries.forEach { dia ->
            ids.add("${alertaId}_${dia.name}")
        }
        center.removePendingNotificationRequestsWithIdentifiers(ids)
    }

    private fun parseHora(horaStr: String): Pair<Int, Int> {
        return try {
            val parts = horaStr.split(":").map { it.trim().toInt() }
            Pair(parts[0], parts[1])
        } catch (_: Exception) {
            Pair(8, 0)
        }
    }

    private fun parseFecha(fechaStr: String): Triple<Int, Int, Int> {
        return try {
            if (fechaStr.contains("-")) {
                val p = fechaStr.split("-").map { it.trim().toInt() }
                Triple(p[0], p[1], p[2])
            } else if (fechaStr.contains("/")) {
                val p = fechaStr.split("/").map { it.trim().toInt() }
                Triple(p[2], p[1], p[0])
            } else {
                Triple(2026, 1, 1)
            }
        } catch (_: Exception) {
            Triple(2026, 1, 1)
        }
    }
}
