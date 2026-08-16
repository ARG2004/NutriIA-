package com.example.nutriia.util

import androidx.compose.runtime.*
import com.example.nutriia.platform.openUrl

object PermissionHelper {
    fun hasPermissions(context: Any? = null, permissions: List<String> = emptyList()): Boolean = true
    fun getRequiredPermissions(type: PermissionType): List<String> = listOf(type.name)
    fun openAppSettings(context: Any? = null) {
        openUrl("app-settings:")
    }
}

enum class PermissionType(val displayName: String, val description: String) {
    CAMERA(
        "Cámara",
        "Esta función requiere acceso a la cámara para capturar fotos e imágenes de los alimentos."
    ),
    MICROPHONE(
        "Micrófono",
        "Esta función requiere acceso al micrófono para realizar llamadas y teleconsultas con especialistas."
    ),
    PHONE(
        "Teléfono",
        "Esta función requiere permisos telefónicos para realizar videollamadas o teleconsultas con especialistas."
    ),
    NEAR_DEVICES(
        "Dispositivos cercanos",
        "Esta función requiere permisos para buscar y conectarse a dispositivos Bluetooth cercanos."
    )
}

@Composable
fun rememberPermissionState(
    type: PermissionType,
    onDismissed: () -> Unit = {},
    onGranted: () -> Unit
): PermissionRequesterState {
    var showDialog by remember { mutableStateOf(false) }

    val state = remember(type, showDialog) {
        PermissionRequesterState(
            type = type,
            hasPermission = { true },
            requestPermission = { onGranted() },
            showDialog = showDialog,
            dismissDialog = { showDialog = false }
        )
    }

    return state
}

class PermissionRequesterState(
    val type: PermissionType,
    val hasPermission: () -> Boolean,
    val requestPermission: () -> Unit,
    val showDialog: Boolean,
    val dismissDialog: () -> Unit
)
