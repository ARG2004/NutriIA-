@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.auth

import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.Foundation.NSError
import platform.LocalAuthentication.LAContext
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthentication
import platform.LocalAuthentication.LAPolicyDeviceOwnerAuthenticationWithBiometrics
import platform.LocalAuthentication.LABiometryTypeFaceID
import platform.LocalAuthentication.LABiometryTypeTouchID
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

    actual fun obtenerTipoBiometria(context: Any?): TipoBiometria {
        return try {
            val laContext = LAContext()
            memScoped {
                val errorPtr = alloc<kotlinx.cinterop.ObjCObjectVar<NSError?>>()
                if (laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, errorPtr.ptr)) {
                    when (laContext.biometryType) {
                        LABiometryTypeFaceID -> TipoBiometria.FACE_ID
                        LABiometryTypeTouchID -> TipoBiometria.TOUCH_ID
                        else -> TipoBiometria.FACE_ID
                    }
                } else if (laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthentication, errorPtr.ptr)) {
                    TipoBiometria.TOUCH_ID
                } else {
                    TipoBiometria.NINGUNA
                }
            }
        } catch (_: Throwable) {
            TipoBiometria.NINGUNA
        }
    }

    actual fun prompt(
        activity: Any?,
        titulo: String,
        onSuccess: () -> Unit,
        onFail: () -> Unit
    ) {
        try {
            val laContext = LAContext()
            laContext.localizedCancelTitle = "Cancelar"

            memScoped {
                val errorPtr = alloc<kotlinx.cinterop.ObjCObjectVar<NSError?>>()
                val biometricsAvailable = laContext.canEvaluatePolicy(LAPolicyDeviceOwnerAuthenticationWithBiometrics, errorPtr.ptr)
                val policy = if (biometricsAvailable) {
                    LAPolicyDeviceOwnerAuthenticationWithBiometrics
                } else {
                    LAPolicyDeviceOwnerAuthentication
                }

                val motivo = titulo.ifBlank { "Accede a tu cuenta de NutrIA de forma segura" }
                laContext.evaluatePolicy(
                    policy = policy,
                    localizedReason = motivo
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
        } catch (_: Throwable) {
            onFail()
        }
    }
}
