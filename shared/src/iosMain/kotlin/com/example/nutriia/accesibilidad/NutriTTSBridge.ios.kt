package com.example.nutriia.accesibilidad

actual class NutriTTSBridge actual constructor() {
    private val iosTts by lazy { IosNutriTTS() }

    actual fun speak(text: String, lang: String) {
        try {
            iosTts.speak(text, lang)
        } catch (_: Throwable) {}
    }

    actual fun stop() {
        try {
            iosTts.stop()
        } catch (_: Throwable) {}
    }
}
