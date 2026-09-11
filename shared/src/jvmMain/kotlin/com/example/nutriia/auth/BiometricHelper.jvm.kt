package com.example.nutriia.auth

actual object BiometricHelper {
    actual fun isAvailable(context: Any?): Boolean = false
    actual fun obtenerTipoBiometria(context: Any?): TipoBiometria = TipoBiometria.NINGUNA
    actual fun prompt(
        activity: Any?,
        titulo: String,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        onSuccess()
    }
}
