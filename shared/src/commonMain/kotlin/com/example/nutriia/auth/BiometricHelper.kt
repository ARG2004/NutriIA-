package com.example.nutriia.auth

object BiometricHelper {

    fun isAvailable(context: Any? = null): Boolean = false

    fun prompt(
        activity: Any? = null,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    ) {
        onSuccess()
    }
}
