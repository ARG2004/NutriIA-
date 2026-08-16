package com.example.nutriia.auth

expect object BiometricHelper {
    fun isAvailable(context: Any? = null): Boolean
    fun prompt(
        activity: Any? = null,
        onSuccess: () -> Unit = {},
        onFail: () -> Unit = {}
    )
}
