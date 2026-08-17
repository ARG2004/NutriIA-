package com.example.nutriia.accesibilidad

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

enum class VoiceInputState { IDLE, LISTENING, PROCESSING, ERROR }

class VoiceInputManager(context: Any? = null) {

    val estado: MutableState<VoiceInputState> = mutableStateOf(VoiceInputState.IDLE)
    val errorMsg: MutableState<String> = mutableStateOf("")
    val errorCodigo: MutableState<Int> = mutableIntStateOf(-1)

    private val platformVoice = PlatformVoiceInput()

    fun isDisponible(): Boolean = platformVoice.isAvailable()

    fun esErrorRecuperable(): Boolean = false

    fun escuchar(
        idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX,
        modoAccesible: Boolean = false,
        onResult: (String, Boolean) -> Unit
    ) {
        if (!isDisponible()) {
            estado.value = VoiceInputState.ERROR
            errorMsg.value = "Reconocimiento de voz no disponible"
            errorCodigo.value = -1
            return
        }

        estado.value = VoiceInputState.LISTENING
        errorMsg.value = ""

        platformVoice.startListening(
            lang = idioma.localeVoz,
            onPartialResult = { text ->
                if (text.isNotBlank()) {
                    onResult(text, false)
                }
            },
            onFinalResult = { text ->
                estado.value = VoiceInputState.IDLE
                onResult(text, true)
            },
            onError = { err ->
                estado.value = VoiceInputState.ERROR
                errorMsg.value = err
                errorCodigo.value = 1
            }
        )
    }

    fun detener() {
        platformVoice.stopListening()
        estado.value = VoiceInputState.IDLE
    }

    fun cancelar() {
        platformVoice.cancel()
        estado.value = VoiceInputState.IDLE
    }

    fun liberar() {
        detener()
    }
}