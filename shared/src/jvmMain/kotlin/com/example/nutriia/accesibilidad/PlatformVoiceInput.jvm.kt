package com.example.nutriia.accesibilidad

actual class PlatformVoiceInput actual constructor() {

    actual fun isAvailable(): Boolean = false

    actual fun startListening(
        lang: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        onError("Reconocimiento de voz no soportado en JVM")
    }

    actual fun stopListening() {}

    actual fun cancel() {}
}
