@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.auth

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object BiometricHelper {

    actual fun isAvailable(context: Any?): Boolean {
        return try {
            val laContext = LAContext()
            memScoped {
                val errorPtr = alloc<kotlinx.cinterop.ObjCObjectVar<NSError?>>()
                val hasBiometrics = laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, errorPtr.ptr)
                if (hasBiometrics) true
                else laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, errorPtr.ptr)
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
            laContext.localizedCancelTitle = "Cancelar"
            laContext.evaluatePolicy(
                policy = LAPolicyDeviceOwnerAuthentication,
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
