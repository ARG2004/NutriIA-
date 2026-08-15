package com.example.nutriia.alerta

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object AlertaScheduler {

    fun programar(context: Context, alerta: Alerta) {
        if (!alerta.activa) {
            cancelar(context, alerta.id)
            return
        }

        // Cancelar alarmas previas para evitar duplicados
        cancelar(context, alerta.id)

        if (alerta.fechaUnica != null) {
            programarUnicaAlarmManager(context, alerta)
        } else {
            alerta.diasSemana.forEach { dia ->
                programarDiaAlarmManager(context, alerta, dia)
            }
        }
    }

    fun cancelar(context: Context, alertaId: String) {
        cancelarAlarmManager(context, alertaId)
    }

    private fun cancelarAlarmManager(context: Context, alertaId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (am == null) return

        // Cancelar única
        val intentUnica = Intent(context, AlertaReceiver::class.java)
        val piUnica = PendingIntent.getBroadcast(
            context,
            alertaId.hashCode(),
            intentUnica,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (piUnica != null) {
            am.cancel(piUnica)
            piUnica.cancel()
        }

        // Cancelar semanales
        DiasSemana.entries.forEach { dia ->
            val intentSemana = Intent(context, AlertaReceiver::class.java)
            val piSemana = PendingIntent.getBroadcast(
                context,
                "${alertaId}_${dia.name}".hashCode(),
                intentSemana,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (piSemana != null) {
                am.cancel(piSemana)
                piSemana.cancel()
            }
        }
    }

    fun reprogramarTodas(context: Context, alertas: List<Alerta>) {
        alertas.forEach { programar(context, it) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ALARM MANAGER SCHEDULING
    // ─────────────────────────────────────────────────────────────────────────

    private fun programarAlarmaSegura(am: AlarmManager, triggerTime: Long, pi: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pi)
            }
        } catch (e: SecurityException) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                } else {
                    am.set(AlarmManager.RTC_WAKEUP, triggerTime, pi)
                }
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
    }

    private fun programarDiaAlarmManager(context: Context, alerta: Alerta, dia: DiasSemana) {
        val delay = calcularDelayHastaDia(alerta.hora, dia)
        val triggerTime = System.currentTimeMillis() + delay

        val intent = Intent(context, AlertaReceiver::class.java).apply {
            putExtra("titulo", alerta.titulo)
            putExtra("descripcion", alerta.descripcion)
            putExtra("tipo", alerta.tipo.name)
            putExtra("childName", alerta.childName)
            putExtra("alertaId", alerta.id)
            putExtra("dia", dia.name)
            putExtra("hora", alerta.hora)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            "${alerta.id}_${dia.name}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (am != null) {
            programarAlarmaSegura(am, triggerTime, pi)
        }
    }

    private fun programarUnicaAlarmManager(context: Context, alerta: Alerta) {
        val fecha = alerta.fechaUnica ?: return
        val delay = calcularDelayHastaFecha(alerta.hora, fecha)
        if (delay <= 0) return
        val triggerTime = System.currentTimeMillis() + delay

        val intent = Intent(context, AlertaReceiver::class.java).apply {
            putExtra("titulo", alerta.titulo)
            putExtra("descripcion", alerta.descripcion)
            putExtra("tipo", alerta.tipo.name)
            putExtra("childName", alerta.childName)
            putExtra("alertaId", alerta.id)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            alerta.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (am != null) {
            programarAlarmaSegura(am, triggerTime, pi)
        }
    }

    fun programarSiguienteSemana(
        context: Context,
        alertaId: String,
        titulo: String,
        descripcion: String,
        tipoNombre: String,
        childName: String,
        dia: DiasSemana,
        hora: String
    ) {
        val delay = calcularDelayHastaDia(hora, dia)
        val triggerTime = System.currentTimeMillis() + delay

        val intent = Intent(context, AlertaReceiver::class.java).apply {
            putExtra("titulo", titulo)
            putExtra("descripcion", descripcion)
            putExtra("tipo", tipoNombre)
            putExtra("childName", childName)
            putExtra("alertaId", alertaId)
            putExtra("dia", dia.name)
            putExtra("hora", hora)
        }

        val pi = PendingIntent.getBroadcast(
            context,
            "${alertaId}_${dia.name}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        if (am != null) {
            programarAlarmaSegura(am, triggerTime, pi)
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELAY CALCULATIONS
    // ─────────────────────────────────────────────────────────────────────────

    private fun calcularDelayHastaDia(hora: String, dia: DiasSemana): Long {
        val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
        val hh = partes.getOrElse(0) { 8 }
        val mm = partes.getOrElse(1) { 0 }

        val ahora = Calendar.getInstance()
        val diaActualCalendar = ahora.get(Calendar.DAY_OF_WEEK)
        val diaObjetivoCalendar = when (dia) {
            DiasSemana.DOMINGO   -> Calendar.SUNDAY
            DiasSemana.LUNES     -> Calendar.MONDAY
            DiasSemana.MARTES    -> Calendar.TUESDAY
            DiasSemana.MIERCOLES -> Calendar.WEDNESDAY
            DiasSemana.JUEVES    -> Calendar.THURSDAY
            DiasSemana.VIERNES   -> Calendar.FRIDAY
            DiasSemana.SABADO    -> Calendar.SATURDAY
        }

        val objetivo = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hh)
            set(Calendar.MINUTE, mm)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var diasDiferencia = diaObjetivoCalendar - diaActualCalendar
        if (diasDiferencia < 0) {
            diasDiferencia += 7
        } else if (diasDiferencia == 0 && objetivo.timeInMillis <= ahora.timeInMillis) {
            diasDiferencia = 7
        }

        objetivo.add(Calendar.DAY_OF_YEAR, diasDiferencia)
        return objetivo.timeInMillis - ahora.timeInMillis
    }

    private fun calcularDelayHastaFecha(hora: String, fecha: String): Long {
        return try {
            val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
            val hh = partes.getOrElse(0) { 8 }
            val mm = partes.getOrElse(1) { 0 }
            val fp = fecha.split("/").mapNotNull { it.toIntOrNull() }
            val dia  = fp.getOrElse(0) { 1 }
            val mes  = fp.getOrElse(1) { 1 }
            val anio = fp.getOrElse(2) { 2025 }

            val objetivo = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, dia)
                set(Calendar.MONTH, mes - 1)
                set(Calendar.YEAR, anio)
                set(Calendar.HOUR_OF_DAY, hh)
                set(Calendar.MINUTE, mm)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            objetivo.timeInMillis - System.currentTimeMillis()
        } catch (e: Exception) { -1L }
    }
}