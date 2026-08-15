package android.media

class AudioManager {
    fun playSoundEffect(effectType: Int) {}
    companion object {
        const val FX_KEY_CLICK = 0
    }
}

class ExifInterface(filePath: String) {
    fun getAttributeInt(tag: String, defaultValue: Int): Int = defaultValue
    companion object {
        const val TAG_ORIENTATION = "Orientation"
        const val ORIENTATION_ROTATE_90 = 6
        const val ORIENTATION_ROTATE_180 = 3
        const val ORIENTATION_ROTATE_270 = 8
    }
}
