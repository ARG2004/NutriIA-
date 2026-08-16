package com.example.nutriia.platform

actual fun getPlatformName(): String = "JVM/Android"

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

actual fun openUrl(url: String) {
    println("Opening URL: $url")
}
