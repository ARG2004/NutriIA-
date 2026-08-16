@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.auth

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object BiometricHelper {

    actual fun isAvailable(context: Any?): Boolean {
        return try {
            val laContext = LAContext()
            memScoped {
                val errorPtr = alloc<kotlinx.cinterop.ObjCObjectVar<NSError?>>()
                laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, errorPtr.ptr)
            }
        } catch (_: Throwable) {
            false
        }
    }

    actual fun prompt(
        activity: Any?,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        try {
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
        } catch (_: Throwable) {
            onFail()
        }
    }
}
