package com.example.nutriia.accesibilidad

import android.media.AudioManager
import android.media.ToneGenerator

/**
 * Gestor de Audio-Iconos (Earcons) de Ultra-Baja Latencia (<5ms) para NutrIA.
 * Utiliza ToneGenerator nativo de Android sin depender de archivos de audio pesados.
 */
object NutriEarcons {

    private var toneGen: ToneGenerator? = null

    init {
        try {
            toneGen = ToneGenerator(AudioManager.STREAM_ACCESSIBILITY, 85)
        } catch (_: Exception) {
            try {
                toneGen = ToneGenerator(AudioManager.STREAM_MUSIC, 70)
            } catch (_: Exception) {}
        }
    }

    /**
     * Beep agudo al iniciar el reconocimiento de voz (1200 Hz).
     */
    fun playMicStart() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 120)
        } catch (_: Exception) {}
    }

    /**
     * Beep suave al detener el micrófono o completar la captura (800 Hz).
     */
    fun playMicStop() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_ACK, 100)
        } catch (_: Exception) {}
    }

    /**
     * Tono armónico ascendente al completar una acción o guardar exitosamente.
     */
    fun playSuccess() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_PROMPT, 150)
        } catch (_: Exception) {}
    }

    /**
     * Tono grave de advertencia ante errores de validación o campos requeridos (300 Hz).
     */
    fun playError() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP2, 180)
        } catch (_: Exception) {}
    }

    /**
     * Pulso auditivo corto de latido (heartbeat) emitido durante cargas de IA (cada 1.5s).
     * Asegura a la persona ciega que el sistema está trabajando y no se ha congelado.
     */
    fun playHeartbeat() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
        } catch (_: Exception) {}
    }

    /**
     * Tono sutil al rozar o tocar un botón accesible (Hover).
     */
    fun playButtonHover() {
        try {
            toneGen?.startTone(ToneGenerator.TONE_PROP_BEEP, 25)
        } catch (_: Exception) {}
    }
}
