package com.google.mediapipe.tasks.vision.handlandmarker

import android.content.Context

class HandLandmarker {
    companion object {
        fun createFromOptions(context: Context, options: Any?): HandLandmarker = HandLandmarker()
    }
    fun detectAsync(image: Any?, timestampMs: Long) {}
    fun close() {}
}
