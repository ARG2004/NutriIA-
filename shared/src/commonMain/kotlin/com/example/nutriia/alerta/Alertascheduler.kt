package com.example.nutriia.alerta

expect object PlatformAlertaScheduler {
    fun programarAlerta(alerta: Alerta)
    fun cancelarAlerta(alertaId: String)
}

object AlertaScheduler {
    fun programar(context: Any? = null, alerta: Alerta) {
        if (!alerta.activa) {
            cancelar(context, alerta.id)
            return
        }
        cancelar(context, alerta.id)
        PlatformAlertaScheduler.programarAlerta(alerta)
    }

    fun cancelar(context: Any? = null, alertaId: String) {
        PlatformAlertaScheduler.cancelarAlerta(alertaId)
    }

    fun reprogramarTodas(context: Any? = null) {
        // En iOS, las notificaciones programadas en UNUserNotificationCenter persisten en el SO
    }
}