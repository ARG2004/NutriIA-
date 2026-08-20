@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.util

import com.example.nutriia.platform.openUrl
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionRecordPermissionGranted
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeAudio
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.CoreBluetooth.CBCentralManager
import platform.CoreBluetooth.CBManagerAuthorizationAllowedAlways
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object PlatformPermissionHelper {

    actual fun hasPermission(type: PermissionType): Boolean {
        return try {
            when (type) {
                PermissionType.CAMERA -> {
                    AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) == AVAuthorizationStatusAuthorized
                }
                PermissionType.MICROPHONE -> {
                    AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeAudio) == AVAuthorizationStatusAuthorized
                }
                PermissionType.PHONE -> {
                    true
                }
                PermissionType.NEAR_DEVICES -> {
                    CBCentralManager.authorization == CBManagerAuthorizationAllowedAlways
                }
                PermissionType.NOTIFICATIONS -> {
                    // En iOS el chequeo de notificaciones es asíncrono.
                    // Para mantener la firma síncrona, devolvemos true y dejamos que requestPermission maneje el flujo.
                    true
                }
            }
        } catch (_: Throwable) {
            true
        }
    }

    actual fun requestPermission(type: PermissionType, onResult: (Boolean) -> Unit) {
        try {
            when (type) {
                PermissionType.CAMERA -> {
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                        dispatch_async(dispatch_get_main_queue()) {
                            onResult(granted)
                        }
                    }
                }
                PermissionType.MICROPHONE -> {
                    AVCaptureDevice.requestAccessForMediaType(AVMediaTypeAudio) { granted ->
                        dispatch_async(dispatch_get_main_queue()) {
                            onResult(granted)
                        }
                    }
                }
                PermissionType.PHONE -> {
                    dispatch_async(dispatch_get_main_queue()) {
                        onResult(true)
                    }
                }
                PermissionType.NEAR_DEVICES -> {
                    dispatch_async(dispatch_get_main_queue()) {
                        onResult(true)
                    }
                }
                PermissionType.NOTIFICATIONS -> {
                    val center = UNUserNotificationCenter.currentNotificationCenter()
                    val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                    center.requestAuthorizationWithOptions(options) { granted, _ ->
                        dispatch_async(dispatch_get_main_queue()) {
                            onResult(granted)
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            dispatch_async(dispatch_get_main_queue()) {
                onResult(true)
            }
        }
    }

    fun requestPhotosPermission(onResult: (Boolean) -> Unit) {
        try {
            PHPhotoLibrary.requestAuthorization { status ->
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(status == PHAuthorizationStatusAuthorized)
                }
            }
        } catch (_: Throwable) {
            dispatch_async(dispatch_get_main_queue()) {
                onResult(true)
            }
        }
    }

    fun requestNotificationsPermission(onResult: (Boolean) -> Unit) {
        try {
            val center = UNUserNotificationCenter.currentNotificationCenter()
            val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            center.requestAuthorizationWithOptions(options) { granted, _ ->
                dispatch_async(dispatch_get_main_queue()) {
                    onResult(granted)
                }
            }
        } catch (_: Throwable) {
            dispatch_async(dispatch_get_main_queue()) {
                onResult(true)
            }
        }
    }

    actual fun openAppSettings() {
        openUrl(UIApplicationOpenSettingsURLString)
    }
}
