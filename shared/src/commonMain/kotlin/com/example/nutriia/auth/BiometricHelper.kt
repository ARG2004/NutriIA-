package com.example.nutriia.auth

enum class TipoBiometria {
    FACE_ID,
    TOUCH_ID,
    HUELLA_DIGITAL,
    NINGUNA
}

expect object BiometricHelper {
    fun isAvailable(context: Any? = null): Boolean
    fun obtenerTipoBiometria(context: Any? = null): TipoBiometria
    fun prompt(
        activity: Any? = null,
        titulo: String = "Acceso seguro a NutrIA",
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    )
}
