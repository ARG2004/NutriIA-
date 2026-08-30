@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.accesibilidad

import com.example.nutriia.platform.CrashStorage
import platform.AVFAudio.*
import platform.Foundation.NSLog
import platform.Foundation.NSLocale
import platform.Speech.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_after
import platform.darwin.dispatch_time
import platform.darwin.DISPATCH_TIME_NOW

actual class PlatformVoiceInput actual constructor() {

    companion object {
        private var sharedAudioEngine: AVAudioEngine? = null
        private var sharedSpeechRecognizer: SFSpeechRecognizer? = null
        private var sharedRecognitionRequest: SFSpeechAudioBufferRecognitionRequest? = null
        private var sharedRecognitionTask: SFSpeechRecognitionTask? = null
        private var currentSilenceToken: Long = 0L
        private var isListeningActive: Boolean = false

        fun stopListeningGlobal() {
            try {
                isListeningActive = false
                currentSilenceToken++

                val engine = sharedAudioEngine
                if (engine != null) {
                    if (engine.isRunning()) {
                        engine.stop()
                    }
                    try {
                        engine.inputNode.removeTapOnBus(0u)
                    } catch (_: Throwable) {}
                    sharedAudioEngine = null
                }

                try {
                    sharedRecognitionRequest?.endAudio()
                } catch (_: Throwable) {}
                sharedRecognitionRequest = null

                try {
                    sharedRecognitionTask?.cancel()
                } catch (_: Throwable) {}
                sharedRecognitionTask = null
            } catch (t: Throwable) {
                val msg = "⚠️ [PlatformVoiceInput] stopListeningGlobal error: ${t.message}"
                NSLog("%s", msg)
            }
        }
    }

    actual fun isAvailable(): Boolean {
        return try {
            val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-MX"))
            recognizer?.isAvailable() ?: false
        } catch (t: Throwable) {
            val msg = "⚠️ [PlatformVoiceInput] isAvailable() check failed: ${t.message}\n${t.stackTraceToString()}"
            NSLog("%s", msg)
            CrashStorage.saveCrash(msg)
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
            stopListeningGlobal()

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
            stopListeningGlobal()
        }
    }

    private fun iniciarReconocimiento(
        lang: String,
        onPartialResult: (String) -> Unit,
        onFinalResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            stopListeningGlobal()

            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(
                category = AVAudioSessionCategoryPlayAndRecord,
                mode = AVAudioSessionModeDefault,
                options = AVAudioSessionCategoryOptionDefaultToSpeaker or AVAudioSessionCategoryOptionAllowBluetooth or AVAudioSessionCategoryOptionDuckOthers,
                error = null
            )
            audioSession.setActive(true, error = null)

            val recognizer = SFSpeechRecognizer(locale = NSLocale(localeIdentifier = lang))
                ?: SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-MX"))
                ?: SFSpeechRecognizer(locale = NSLocale(localeIdentifier = "es-ES"))

            sharedSpeechRecognizer = recognizer

            val request = SFSpeechAudioBufferRecognitionRequest()
            request.shouldReportPartialResults = true
            sharedRecognitionRequest = request

            val engine = AVAudioEngine()
            sharedAudioEngine = engine

            val inputNode = engine.inputNode
            val recordingFormat = inputNode.outputFormatForBus(0u)

            try {
                inputNode.removeTapOnBus(0u)
            } catch (_: Throwable) {}

            inputNode.installTapOnBus(0u, bufferSize = 1024u, format = recordingFormat) { buffer, _ ->
                if (buffer != null) {
                    sharedRecognitionRequest?.appendAudioPCMBuffer(buffer)
                }
            }

            engine.prepare()
            engine.startAndReturnError(null)
            isListeningActive = true

            var lastTranscription = ""

            sharedRecognitionTask = recognizer?.recognitionTaskWithRequest(request) { result, error ->
                if (result != null) {
                    val text = result.bestTranscription.formattedString
                    lastTranscription = text

                    dispatch_async(dispatch_get_main_queue()) {
                        if (!isListeningActive && !result.isFinal()) return@dispatch_async

                        onPartialResult(text)

                        if (result.isFinal()) {
                            currentSilenceToken++
                            onFinalResult(text)
                            stopListeningGlobal()
                        } else if (text.isNotBlank()) {
                            // Detector de silencio para iOS: si no hay más voz por 1.2 segundos, finalizar
                            val thisToken = ++currentSilenceToken
                            val delayNanoseconds = (1.2 * 1_000_000_000).toLong()
                            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, delayNanoseconds), dispatch_get_main_queue()) {
                                if (thisToken == currentSilenceToken && isListeningActive && lastTranscription.isNotBlank()) {
                                    val finalPhrase = lastTranscription
                                    stopListeningGlobal()
                                    onFinalResult(finalPhrase)
                                }
                            }
                        }
                    }
                }
                if (error != null) {
                    val desc = error.localizedDescription ?: ""
                    val code = error.code.toLong()
                    val domain = error.domain ?: ""

                    val isCancellation = desc.contains("cancel", ignoreCase = true) ||
                                         desc.contains("cancelad", ignoreCase = true) ||
                                         desc.contains("canceló", ignoreCase = true) ||
                                         code == 216L || code == 201L || code == 1110L ||
                                         (domain.contains("kAFAssistantErrorDomain") && (code == 216L || code == 201L))

                    if (!isCancellation && isListeningActive) {
                        dispatch_async(dispatch_get_main_queue()) {
                            if (isListeningActive) {
                                onError(desc)
                                stopListeningGlobal()
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            val msg = "⚠️ [PlatformVoiceInput] iniciarReconocimiento error: ${t.message}"
            NSLog("%s", msg)
            onError(t.message ?: "Error al inicializar micrófono")
            stopListeningGlobal()
        }
    }

    actual fun stopListening() {
        stopListeningGlobal()
    }

    actual fun cancel() {
        stopListeningGlobal()
    }
}
