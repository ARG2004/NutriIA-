package com.example.nutriia.util

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*

expect object PlatformPermissionHelper {
    fun hasPermission(type: PermissionType): Boolean
    fun requestPermission(type: PermissionType, onResult: (Boolean) -> Unit)
    fun openAppSettings()
}

object PermissionHelper {
    fun hasPermissions(context: Any? = null, permissions: List<String> = emptyList()): Boolean {
        return true
    }

    fun hasPermission(type: PermissionType): Boolean {
        return PlatformPermissionHelper.hasPermission(type)
    }

    fun requestPermission(type: PermissionType, onResult: (Boolean) -> Unit) {
        PlatformPermissionHelper.requestPermission(type, onResult)
    }

    fun getRequiredPermissions(type: PermissionType): List<String> = listOf(type.name)

    fun openAppSettings(context: Any? = null) {
        PlatformPermissionHelper.openAppSettings()
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
            hasPermission = { PlatformPermissionHelper.hasPermission(type) },
            requestPermission = {
                if (PlatformPermissionHelper.hasPermission(type)) {
                    onGranted()
                } else {
                    PlatformPermissionHelper.requestPermission(type) { granted ->
                        if (granted) {
                            onGranted()
                        } else {
                            showDialog = true
                        }
                    }
                }
            },
            showDialog = showDialog,
            dismissDialog = { showDialog = false }
        )
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onDismissed()
            },
            title = { Text("Permiso requerido: ${type.displayName}") },
            text = { Text(type.description + "\n\nPor favor, habilita los permisos correspondientes en los ajustes de tu dispositivo.") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    PlatformPermissionHelper.openAppSettings()
                    onDismissed()
                }) {
                    Text("Configuración")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDialog = false
                    onDismissed()
                }) {
                    Text("Cancelar")
                }
            }
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
