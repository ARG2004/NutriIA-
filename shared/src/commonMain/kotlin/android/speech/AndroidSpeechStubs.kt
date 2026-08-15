package android.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle

interface RecognitionListener {
    fun onReadyForSpeech(params: Bundle?) {}
    fun onBeginningOfSpeech() {}
    fun onRmsChanged(rmsdB: Float) {}
    fun onBufferReceived(buffer: ByteArray?) {}
    fun onEndOfSpeech() {}
    fun onError(error: Int) {}
    fun onResults(results: Bundle?) {}
    fun onPartialResults(partialResults: Bundle?) {}
    fun onEvent(eventType: Int, params: Bundle?) {}
}

class SpeechRecognizer {
    fun setRecognitionListener(listener: RecognitionListener?) {}
    fun startListening(recognizerIntent: Intent?) {}
    fun stopListening() {}
    fun destroy() {}

    companion object {
        const val RESULTS_RECOGNITION = "results_recognition"
        const val ERROR_AUDIO = 3
        const val ERROR_CLIENT = 5
        const val ERROR_INSUFFICIENT_PERMISSIONS = 9
        const val ERROR_NETWORK = 2
        const val ERROR_NETWORK_TIMEOUT = 1
        const val ERROR_NO_MATCH = 7
        const val ERROR_RECOGNIZER_BUSY = 8
        const val ERROR_SERVER = 4
        const val ERROR_SPEECH_TIMEOUT = 6

        fun isRecognitionAvailable(context: Context): Boolean = true
        fun createSpeechRecognizer(context: Context): SpeechRecognizer = SpeechRecognizer()
    }
}

object RecognizerIntent {
    const val ACTION_RECOGNIZE_SPEECH = "android.speech.action.RECOGNIZE_SPEECH"
    const val EXTRA_LANGUAGE_MODEL = "android.speech.extra.LANGUAGE_MODEL"
    const val LANGUAGE_MODEL_FREE_FORM = "free_form"
    const val EXTRA_LANGUAGE = "android.speech.extra.LANGUAGE"
    const val EXTRA_PROMPT = "android.speech.extra.PROMPT"
    const val EXTRA_LANGUAGE_PREFERENCE = "android.speech.extra.LANGUAGE_PREFERENCE"
    const val EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE = "android.speech.extra.ONLY_RETURN_LANGUAGE_PREFERENCE"
    const val EXTRA_MAX_RESULTS = "android.speech.extra.MAX_RESULTS"
    const val EXTRA_PARTIAL_RESULTS = "android.speech.extra.PARTIAL_RESULTS"
    const val EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS = "android.speech.extras.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS"
    const val EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS = "android.speech.extras.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS"
    const val EXTRA_PREFER_OFFLINE = "android.speech.extra.PREFER_OFFLINE"
}
