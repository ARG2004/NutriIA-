package com.example.nutriia.accesibilidad

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate

class IosNutriTTS {
    private val synthesizer = AVSpeechSynthesizer()

    fun speak(text: String, lang: String = "es-MX") {
        if (text.isBlank()) return
        val utterance = AVSpeechUtterance(string = text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(lang) ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate
        synthesizer.speakUtterance(utterance)
    }

    fun stop() {
        synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
    }
}
