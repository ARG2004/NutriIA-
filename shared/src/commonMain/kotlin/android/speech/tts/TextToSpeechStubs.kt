package android.speech.tts

import android.content.Context
import java.util.Locale

class TextToSpeech(context: Context, listener: OnInitListener) {
    interface OnInitListener {
        fun onInit(status: Int)
    }

    fun setLanguage(loc: Locale): Int = SUCCESS
    fun setSpeechRate(rate: Float): Int = SUCCESS
    fun setPitch(pitch: Float): Int = SUCCESS
    fun speak(text: CharSequence, queueMode: Int, params: android.os.Bundle?, utteranceId: String?): Int = SUCCESS
    fun stop(): Int = SUCCESS
    fun shutdown() {}

    companion object {
        const val SUCCESS = 0
        const val ERROR = -1
        const val QUEUE_FLUSH = 0
        const val QUEUE_ADD = 1
    }
}
