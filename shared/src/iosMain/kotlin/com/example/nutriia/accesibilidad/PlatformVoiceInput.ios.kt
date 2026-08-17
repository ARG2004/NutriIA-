@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.accesibilidad

import platform.AVFAudio.*
import platform.Foundation.NSLocale
import platform.Speech.*

actual class PlatformVoiceInput actual constructor() {

    private var audioEngine: AVAudioEngine? = null
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null

    actual fun isAvailable(): Boolean {
        return try {
            val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-MX"))
            recognizer.isAvailable()
        } catch (_: Throwable) {
            false
        }
    }

    actual fun startListening(
        lang: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            stopListening()

            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryRecord, error = null)
            audioSession.setMode(AVAudioSessionModeMeasurement, error = null)
            audioSession.setActive(true, error = null)

            val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = lang))
            if (!recognizer.isAvailable()) {
                onError("El reconocedor de voz no está disponible en este momento.")
                return
            }
            speechRecognizer = recognizer

            val request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            recognitionRequest = request

            val engine = AVAudioEngine()
            audioEngine = engine

            val inputNode = engine.inputNode
            val recordingFormat = inputNode.outputFormatForBus(0u)

            inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
                if (buffer != null) {
                    request.appendAudioPCMBuffer(buffer)
                }
            }

            engine.prepare()
            engine.startAndReturnError(null)

            recognitionTask = recognizer.recognitionTaskWithRequest(request) { result, error ->
                if (result != null) {
                    val text = result.bestTranscription.formattedString
                    if (result.isFinal()) {
                        onFinalResult(text)
                    } else {
                        onPartialResult(text)
                    }
                }
                if (error != null) {
                    onError(error.localizedDescription ?: "Error de reconocimiento de voz")
                    stopListening()
                }
            }
        } catch (t: Throwable) {
            onError(t.message ?: "Error al inicializar audio")
            stopListening()
        }
    }

    actual fun stopListening() {
        try {
            audioEngine?.stop()
            audioEngine?.inputNode?.removeTapOnBus(0u)
            audioEngine = null

            recognitionRequest?.endAudio()
            recognitionRequest = null

            recognitionTask?.cancel()
            recognitionTask = null
        } catch (_: Throwable) {}
    }

    actual fun cancel() {
        stopListening()
    }
}
