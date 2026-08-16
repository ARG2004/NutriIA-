@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.auth

import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object BiometricHelper {

    actual fun isAvailable(context: Any?): Boolean {
        val laContext = LAContext()
        return laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, error = null)
    }

    actual fun prompt(
        activity: Any?,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        val laContext = LAContext()
        laContext.evaluatePolicy(
            policy = LAPolicyDeviceOwnerAuthenticationWithBiometrics,
            localizedReason = "Accede a tu cuenta de NutrIA de forma segura"
        ) { success, _ ->
            dispatch_async(dispatch_get_main_queue()) {
                if (success) {
                    onSuccess()
                } else {
                    onFail()
                }
            }
        }
    }
}
