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
