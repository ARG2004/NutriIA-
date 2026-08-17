@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.accesibilidad

import platform.AVFAudio.*
import platform.Foundation.NSLocale
import platform.Speech.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_after
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

actual class PlatformVoiceInput actual constructor() {

    private var audioEngine: AVAudioEngine? = null
    private var speechRecognizer: SFSpeechRecognizer? = null
    private var recognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
    private var recognitionTask: SFSpeechRecognitionTask? = null
    private var currentSilenceToken: Long = 0L

    actual fun isAvailable(): Boolean {
        return try {
            val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-MX"))
            recognizer.isAvailable()
        } catch (_: Throwable) {
            true
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

            // 1. Solicitar permisos de reconocimiento de voz y micrófono
            SFSpeechRecognizer.requestAuthorization { authStatus ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (authStatus != SFSpeechRecognizerAuthorizationStatus.SFSpeechRecognizerAuthorizationStatusAuthorized) {
                        onError("Permiso de reconocimiento de voz no concedido.")
                        return@dispatch_async
                    }

                    val session = AVAudioSession.sharedInstance()
                    session.requestRecordPermission { granted ->
                        dispatch_async(dispatch_get_main_queue()) {
                            if (!granted) {
                                onError("Permiso de micrófono no concedido.")
                                return@dispatch_async
                            }

                            iniciarReconocimiento(lang, onPartialResult, onFinalResult, onError)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            onError(t.message ?: "Error al solicitar permisos de voz")
            stopListening()
        }
    }

    private fun iniciarReconocimiento(
        lang: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(
                category = AVAudioSessionCategoryPlayAndRecord,
                mode = AVAudioSessionModeMeasurement,
                options = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionAllowBluetooth,
                error = null
            )
            audioSession.setActive(true, error = null)

            val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = lang))
                ?: SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-MX"))
                ?: SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-ES"))

            speechRecognizer = recognizer

            val request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            recognitionRequest = request

            val engine = AVAudioEngine()
            audioEngine = engine

            val inputNode = engine.inputNode
            val recordingFormat = inputNode.outputFormatForBus(0u)

            inputNode.removeTapOnBus(0u)
            inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
                if (buffer != null) {
                    request.appendAudioPCMBuffer(buffer)
                }
            }

            engine.prepare()
            engine.startAndReturnError(null)

            var lastTranscription = ""

            recognitionTask = recognizer.recognitionTaskWithRequest(request) { result, error ->
                if (result != null) {
                    val text = result.bestTranscription.formattedString
                    lastTranscription = text

                    dispatch_async(dispatch_get_main_queue()) {
                        onPartialResult(text)

                        if (result.isFinal()) {
                            currentSilenceToken++
                            onFinalResult(text)
                            stopListening()
                        } else if (text.isNotBlank()) {
                            // Detector de silencio para iOS: si no hay más voz por 1.3 segundos, finalizar
                            val thisToken = ++currentSilenceToken
                            val delayNanoseconds = (1.3 * 1_000_000_000).toLong()
                            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delayNanoseconds), dispatch_get_main_queue()) {
                                if (thisToken == currentSilenceToken && lastTranscription.isNotBlank()) {
                                    val finalPhrase = lastTranscription
                                    stopListening()
                                    onFinalResult(finalPhrase)
                                }
                            }
                        }
                    }
                }
                if (error != null) {
                    dispatch_async(dispatch_get_main_queue()) {
                        onError(error.localizedDescription ?: "Error en el reconocimiento")
                        stopListening()
                    }
                }
            }
        } catch (t: Throwable) {
            onError(t.message ?: "Error al inicializar micrófono")
            stopListening()
        }
    }

    actual fun stopListening() {
        try {
            currentSilenceToken++
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
