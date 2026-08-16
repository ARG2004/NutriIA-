package com.example.nutriia.accesibilidad

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate

class IosNutriTTS {
    private val synthesizer by lazy { AVSpeechSynthesizer() }

    fun speak(text: String, lang: String = "es-MX") {
        try {
            if (text.isBlank()) return
            val utterance = AVSpeechUtterance(string = text)
            utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(lang) ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
            utterance.rate = AVSpeechUtteranceDefaultSpeechRate
            synthesizer.speakUtterance(utterance)
        } catch (_: Throwable) {}
    }

    fun stop() {
        try {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        } catch (_: Throwable) {}
    }
}
