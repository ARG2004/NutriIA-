package com.example.nutriia.accesibilidad

expect class NutriTTSBridge() {
    fun speak(text: String, lang: String = "es-MX")
    fun stop()
}
