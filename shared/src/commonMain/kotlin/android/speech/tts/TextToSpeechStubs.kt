package android.speech.tts

import android.content.Context
import java.util.Locale

class Voice(
    val name: String = "default",
    val locale: Locale = Locale.getDefault(),
    val quality: Int = 300,
    val latency: Int = 300,
    val isNetworkConnectionRequired: Boolean = false,
    val features: Set<String> = emptySet()
)

abstract class UtteranceProgressListener {
    abstract fun onStart(utteranceId: String?)
    abstract fun onDone(utteranceId: String?)
    abstract fun onError(utteranceId: String?)
    open fun onError(utteranceId: String?, errorCode: Int) {}
}

class TextToSpeech(context: Context, listener: OnInitListener) {
    fun interface OnInitListener {
        fun onInit(status: Int)
    }

    val voices: Set<Voice>? get() = emptySet()
    var voice: Voice? = null
    var language: Locale? = null
    val isSpeaking: Boolean get() = false

    fun isSpeaking(): Boolean = false
    fun setLanguage(loc: Locale): Int = SUCCESS
    fun setSpeechRate(rate: Float): Int = SUCCESS
    fun setPitch(pitch: Float): Int = SUCCESS
    fun speak(text: CharSequence, queueMode: Int, params: android.os.Bundle?, utteranceId: String?): Int = SUCCESS
    fun stop(): Int = SUCCESS
    fun shutdown() {}
    fun setOnUtteranceProgressListener(listener: UtteranceProgressListener) {}

    companion object {
        const val SUCCESS = 0
        const val ERROR = -1
        const val QUEUE_FLUSH = 0
        const val QUEUE_ADD = 1
    }
}
