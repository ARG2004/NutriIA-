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
