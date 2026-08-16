package com.example.nutriia.platform

actual fun getPlatformName(): String = "JVM/Android"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun openUrl(url: String) {
    println("Opening URL: $url")
}

actual fun platformLog(tag: String, msg: String) {
    println("[NutriIA-$tag] $msg")
}

actual fun isVoiceOverActive(): Boolean = false
