package com.example.nutriia.alerta

actual object PlatformAlertaScheduler {
    actual fun programarAlerta(alerta: Alerta) {}
    actual fun cancelarAlerta(alertaId: String) {}
}
