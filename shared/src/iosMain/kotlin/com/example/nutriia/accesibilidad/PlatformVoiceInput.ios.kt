@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.accesibilidad

import platform.AVFAudio.*
import platform.Foundation.NSLocale
import platform.Speech.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

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

            // 1. Solicitar permisos de reconocimiento de voz y micrófono si aún no se han otorgado
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
                    onError(error.localizedDescription ?: "Error en el reconocimiento")
                    stopListening()
                }
            }
        } catch (t: Throwable) {
            onError(t.message ?: "Error al inicializar micrófono")
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
