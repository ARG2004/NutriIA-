package com.example.nutriia.util

import com.example.nutriia.platform.openUrl

actual object PlatformPermissionHelper {
    actual fun hasPermission(type: PermissionType): Boolean = true
    actual fun requestPermission(type: PermissionType, onResult: (Boolean) -> Unit) {
        onResult(true)
    }
    actual fun openAppSettings() {
        openUrl("https://apple.com")
    }
}
