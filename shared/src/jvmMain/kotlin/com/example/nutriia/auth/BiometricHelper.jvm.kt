package com.example.nutriia.auth

actual object BiometricHelper {
    actual fun isAvailable(context: Any?): Boolean = false
    actual fun prompt(
        activity: Any?,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        onSuccess()
    }
}
