package com.example.nutriia.accesibilidad

expect class PlatformVoiceInput() {
    fun isAvailable(): Boolean
    fun startListening(
        lang: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    )
    fun stopListening()
    fun cancel()
}
