package com.example.nutriia.accesibilidad

actual class NutriTTSBridge actual constructor() {
    private val iosTts = IosNutriTTS()

    actual fun speak(text: String, lang: String) {
        iosTts.speak(text, lang)
    }

    actual fun stop() {
        iosTts.stop()
    }
}
