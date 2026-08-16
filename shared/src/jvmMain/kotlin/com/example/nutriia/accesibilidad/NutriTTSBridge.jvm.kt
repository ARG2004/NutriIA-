package com.example.nutriia.accesibilidad

actual class NutriTTSBridge actual constructor() {
    actual fun speak(text: String, lang: String) {
        println("TTS speak ($lang): $text")
    }
    actual fun stop() {
        println("TTS stop")
    }
}
