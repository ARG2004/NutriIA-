package com.example.nutriia.accesibilidad

import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechUtterance
import platform.AVFAudio.AVSpeechUtteranceDefaultSpeechRate

class IosNutriTTS {
    private var synthesizer: AVSpeechSynthesizer? = null

    private fun getSynthesizer(): AVSpeechSynthesizer? {
        return try {
            if (synthesizer == null) {
                synthesizer = AVSpeechSynthesizer()
            }
            synthesizer
        } catch (_: Throwable) {
            null
        }
    }

    fun speak(text: String, lang: String = "es-MX") {
        try {
            if (text.isBlank()) return
            val synth = getSynthesizer() ?: return
            val utterance = AVSpeechUtterance(string = text)
            utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(lang)
                ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
                ?: AVSpeechSynthesisVoice.voiceWithLanguage("es")
            utterance.rate = AVSpeechUtteranceDefaultSpeechRate
            synth.speakUtterance(utterance)
        } catch (_: Throwable) {}
    }

    fun stop() {
        try {
            synthesizer?.let { synth ->
                if (synth.isSpeaking()) {
                    synth.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
                }
            }
        } catch (_: Throwable) {}
    }
}
