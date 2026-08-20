package com.example.nutriia.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

object PermissionHelper {

    fun hasPermissions(context: Context, permissions: List<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun getRequiredPermissions(type: PermissionType): List<String> {
        return when (type) {
            PermissionType.CAMERA -> listOf(Manifest.permission.CAMERA)
            PermissionType.MICROPHONE -> listOf(Manifest.permission.RECORD_AUDIO)
            PermissionType.PHONE -> listOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CALL_PHONE
            )
            PermissionType.NEAR_DEVICES -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                } else {
                    listOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                }
            }
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    emptyList()
                }
            }
        }
    }

    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
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
    ),
    NOTIFICATIONS(
        "Notificaciones",
        "Esta función requiere permiso para enviarte recordatorios de comidas, vacunas y citas médicas importantes."
    )
}

@Composable
fun rememberPermissionState(
    type: PermissionType,
    onDismissed: () -> Unit = {},
    onGranted: () -> Unit
): PermissionRequesterState {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    val permissions = remember(type) { PermissionHelper.getRequiredPermissions(type) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val allGranted = result.values.all { it }
        if (allGranted) {
            onGranted()
        } else {
            showDialog = true
        }
    }

    val state = remember(type, launcher, showDialog) {
        PermissionRequesterState(
            type = type,
            hasPermission = { PermissionHelper.hasPermissions(context, permissions) },
            requestPermission = {
                if (PermissionHelper.hasPermissions(context, permissions)) {
                    onGranted()
                } else {
                    launcher.launch(permissions.toTypedArray())
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
                    PermissionHelper.openAppSettings(context)
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
