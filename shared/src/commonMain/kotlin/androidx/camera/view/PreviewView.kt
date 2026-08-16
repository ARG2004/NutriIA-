package androidx.camera.view

import android.content.Context

class PreviewView(context: Context) {
    var implementationMode: ImplementationMode = ImplementationMode.COMPATIBLE
    var scaleType: ScaleType = ScaleType.FIT_CENTER
    val surfaceProvider: Any? = null

    fun post(action: () -> Unit) {
        action()
    }

    enum class ImplementationMode {
        PERFORMANCE, COMPATIBLE
    }

    enum class ScaleType {
        FIT_CENTER, FILL_CENTER, FIT_START, FIT_END, FILL_START, FILL_END
    }
}
