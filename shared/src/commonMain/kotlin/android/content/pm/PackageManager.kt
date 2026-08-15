package android.content.pm

class PackageManager {
    fun hasSystemFeature(featureName: String): Boolean = true

    companion object {
        const val PERMISSION_GRANTED = 0
        const val PERMISSION_DENIED = -1
        const val FEATURE_CAMERA_ANY = "android.hardware.camera.any"
    }
}
