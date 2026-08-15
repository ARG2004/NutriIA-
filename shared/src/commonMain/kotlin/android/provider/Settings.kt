package android.provider

object Settings {
    object Secure {
        const val ANDROID_ID = "android_id"
        fun getString(resolver: Any?, name: String): String = "ios_device_id"
    }
}
