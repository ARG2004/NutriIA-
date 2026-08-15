package androidx.camera.core

class CameraSelector {
    companion object {
        val DEFAULT_BACK_CAMERA: CameraSelector = CameraSelector()
        val DEFAULT_FRONT_CAMERA: CameraSelector = CameraSelector()
    }
}

class Preview {
    class Builder {
        fun build(): Preview = Preview()
    }
    fun setSurfaceProvider(surfaceProvider: Any?) {}
}

class ImageCapture {
    class Builder {
        fun build(): ImageCapture = ImageCapture()
    }
    class OutputFileOptions
    abstract class OnImageSavedCallback {
        open fun onImageSaved(outputFileResults: Any) {}
        open fun onError(exception: ImageCaptureException) {}
    }
}

class ImageCaptureException(val imageCaptureError: Int, message: String?, cause: Throwable?) : Exception(message, cause)

class ImageAnalysis {
    interface Analyzer {
        fun analyze(imageProxy: Any)
    }
    class Builder {
        fun build(): ImageAnalysis = ImageAnalysis()
    }
    fun setAnalyzer(executor: Any?, analyzer: Analyzer) {}
}

package androidx.camera.lifecycle

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner

class ProcessCameraProvider {
    fun unbindAll() {}
    fun bindToLifecycle(lifecycleOwner: LifecycleOwner, cameraSelector: CameraSelector, vararg useCases: Any): Any? = null
    companion object {
        fun getInstance(context: Any?): Any = object {
            fun addListener(listener: Runnable, executor: Any?) { listener.run() }
            fun get(): ProcessCameraProvider = ProcessCameraProvider()
        }
    }
}

package androidx.camera.view

import android.content.Context

class PreviewView(context: Context) {
    val surfaceProvider: Any? = null
}

package com.google.android.play.core.integrity

class IntegrityManager
class IntegrityTokenRequest
class IntegrityTokenResponse

package io.github.alexzhirkevich.qrose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun QrCodeView(
    data: String,
    modifier: Modifier = Modifier
) {}
