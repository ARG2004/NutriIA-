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
}
