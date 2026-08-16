package com.example.nutriia.platform

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun getPlatformName(): String = "iOS"

actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()

actual fun openUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

actual fun platformLog(tag: String, msg: String) {
    platform.Foundation.NSLog("[NutriIA-%s] %s", tag, msg)
    println("[NutriIA-$tag] $msg")
}

actual fun isVoiceOverActive(): Boolean = platform.UIKit.UIAccessibilityIsVoiceOverRunning()
