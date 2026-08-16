package com.example.nutriia.accesibilidad

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

enum class VoiceInputState { IDLE, LISTENING, PROCESSING, ERROR }

class VoiceInputManager(context: Any? = null) {
    val estado: MutableState<VoiceInputState> = mutableStateOf(VoiceInputState.IDLE)
    val errorMsg: MutableState<String> = mutableStateOf("")
    val errorCodigo: MutableState<Int> = mutableIntStateOf(-1)

    fun isDisponible(): Boolean = true

    fun esErrorRecuperable(): Boolean = false

    fun escuchar(
        idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX,
        modoAccesible: Boolean = false,
        onResult: (String, Boolean) -> Unit
    ) {
        estado.value = VoiceInputState.IDLE
    }

    fun detener() {
        estado.value = VoiceInputState.IDLE
    }

    fun cancelar() {
        estado.value = VoiceInputState.IDLE
    }

    fun liberar() {
        estado.value = VoiceInputState.IDLE
    }
}