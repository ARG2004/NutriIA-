package com.example.nutriia.alerta

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.nutriia.MainActivity
import com.example.nutriia.R

class AlertaReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val titulo      = intent.getStringExtra("titulo")      ?: "NutriIA"
        val descripcion = intent.getStringExtra("descripcion") ?: ""
        val tipoNombre  = intent.getStringExtra("tipo")        ?: ""
        val childName   = intent.getStringExtra("childName")  ?: ""
        val alertaId    = intent.getStringExtra("alertaId")   ?: "0"
        val hora        = intent.getStringExtra("hora")
        val diaNombre   = intent.getStringExtra("dia")

        val tipo = TipoAlerta.entries.find { it.name == tipoNombre }
            ?: TipoAlerta.TOMA_COMIDA

        mostrarNotificacion(context, titulo, descripcion, tipo, childName, alertaId)

        // Reprogramar para la siguiente semana si es una alerta periódica
        if (diaNombre != null && hora != null) {
            val dia = DiasSemana.entries.find { it.name == diaNombre }
            if (dia != null) {
                AlertaScheduler.programarSiguienteSemana(
                    context = context,
                    alertaId = alertaId,
                    titulo = titulo,
                    descripcion = descripcion,
                    tipoNombre = tipoNombre,
                    childName = childName,
                    dia = dia,
                    hora = hora
                )
            }
        }
    }

    private fun mostrarNotificacion(
        context:     Context,
        titulo:      String,
        descripcion: String,
        tipo:        TipoAlerta,
        childName:   String,
        alertaId:    String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permiso = ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            )
            if (permiso != PackageManager.PERMISSION_GRANTED) return
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "nutriia_alertas"
        val channelName = "Alertas NutriIA"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description      = "Recordatorios de NutriIA"
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 250, 100, 250)
            }
            nm.createNotificationChannel(canal)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("alertaId", alertaId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            alertaId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val subtitulo = if (childName.isNotBlank()) "Para $childName · ${tipo.label}" else tipo.label

        val notificacion = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(if (descripcion.isNotBlank()) descripcion else subtitulo)
            .setSubText(subtitulo)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(descripcion.ifBlank { subtitulo })
                    .setBigContentTitle(titulo)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 250, 100, 250))
            .build()

        nm.notify(alertaId.hashCode(), notificacion)
    }
}
