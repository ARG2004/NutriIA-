package com.example.nutriia.accesibilidad

import com.example.nutriia.platform.CrashStorage
import platform.Foundation.NSLog

actual class NutriTTSBridge actual constructor() {
    private val iosTts by lazy { IosNutriTTS() }

    actual fun speak(text: String, lang: String) {
        try {
            iosTts.speak(text, lang)
        } catch (t: Throwable) {
            val msg = "❌ [NutriTTSBridge] Error in speak: ${t.message}\n${t.stackTraceToString()}"
            NSLog("%s", msg)
            CrashStorage.saveCrash(msg)
        }
    }

    actual fun stop() {
        try {
            iosTts.stop()
        } catch (t: Throwable) {
            val msg = "⚠️ [NutriTTSBridge] Error in stop: ${t.message}\n${t.stackTraceToString()}"
            NSLog("%s", msg)
            CrashStorage.saveCrash(msg)
        }
    }

    actual fun isSpeaking(): Boolean {
        return try {
            iosTts.isSpeaking()
        } catch (_: Throwable) {
            false
        }
    }
}
