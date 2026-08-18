@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.accesibilidad

import com.example.nutriia.platform.CrashStorage
import platform.AVFAudio.*
import platform.Foundation.NSLog

class IosNutriTTS {

    companion object {
        private val sharedSynthesizer = AVSpeechSynthesizer()
    }

    private fun setupAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(
                category = AVAudioSessionCategoryPlayback,
                mode = AVAudioSessionModeDefault,
                options = AVAudioSessionCategoryOptionDuckOthers,
                error = null
            )
            session.setActive(true, error = null)
        } catch (t: Throwable) {
            val msg = "⚠️ [IosNutriTTS] Error in setupAudioSession: ${t.message}\n${t.stackTraceToString()}"
            NSLog("%s", msg)
            CrashStorage.saveCrash(msg)
        }
    }

    fun speak(text: String, lang: String = "es-MX") {
        try {
            if (text.isBlank()) return
            setupAudioSession()

            if (sharedSynthesizer.isSpeaking()) {
                sharedSynthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }

            val utterance = AVSpeechUtterance(string = text)
            utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(lang)
                ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-MX")
                ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-US")
                ?: AVSpeechSynthesisVoice.voiceWithLanguage("es-ES")
                ?: AVSpeechSynthesisVoice.voiceWithLanguage("es")
            utterance.rate = AVSpeechUtteranceDefaultSpeechRate
            utterance.pitchMultiplier = 1.0f
            utterance.volume = 1.0f

            sharedSynthesizer.speakUtterance(utterance)
        } catch (t: Throwable) {
            val msg = "❌ [IosNutriTTS] Error in speak(): ${t.message}\n${t.stackTraceToString()}"
            NSLog("%s", msg)
            CrashStorage.saveCrash(msg)
        }
    }

    fun stop() {
        try {
            if (sharedSynthesizer.isSpeaking()) {
                sharedSynthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
            }
        } catch (t: Throwable) {
            val msg = "⚠️ [IosNutriTTS] Error in stop(): ${t.message}\n${t.stackTraceToString()}"
            NSLog("%s", msg)
            CrashStorage.saveCrash(msg)
        }
    }
}
