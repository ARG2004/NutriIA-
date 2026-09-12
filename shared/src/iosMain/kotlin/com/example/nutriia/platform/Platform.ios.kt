package com.example.nutriia.platform

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun getPlatformName(): String = "iOS"

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    platformLog("Platform", "Abriendo URL segura")
    UIApplication.sharedApplication.openURL(nsUrl, emptyMap<Any?, Any?>()) { _ -> }
}

actual fun platformLog(tag: String, msg: String) {
    val safeMsg = sanitizePII(msg)
    platform.Foundation.NSLog("[NutriIA-%s] %s", tag, safeMsg)
    println("[NutriIA-$tag] $safeMsg")
}

actual fun isVoiceOverActive(): Boolean = platform.UIKit.UIAccessibilityIsVoiceOverRunning()

actual fun setKeepScreenOn(enabled: Boolean) {
    UIApplication.sharedApplication.idleTimerDisabled = enabled
}

@androidx.compose.runtime.Composable
actual fun KeepScreenOn() {
    androidx.compose.runtime.DisposableEffect(Unit) {
        UIApplication.sharedApplication.idleTimerDisabled = true
        onDispose {
            UIApplication.sharedApplication.idleTimerDisabled = false
        }
    }
}
