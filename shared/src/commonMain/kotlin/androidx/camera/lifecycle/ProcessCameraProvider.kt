package androidx.camera.lifecycle

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import java.lang.Runnable

class CameraFuture(private val provider: ProcessCameraProvider = ProcessCameraProvider()) {
    fun addListener(listener: Runnable, executor: Any?) {
        listener.run()
    }
    fun addListener(listener: () -> Unit, executor: Any?) {
        listener()
    }
    fun get(): ProcessCameraProvider = provider
}

class ProcessCameraProvider {
    fun unbindAll() {}
    fun bindToLifecycle(lifecycleOwner: LifecycleOwner, cameraSelector: CameraSelector, vararg useCases: Any): Any? = null

    companion object {
        fun getInstance(context: Any?): CameraFuture = CameraFuture()
    }
}
