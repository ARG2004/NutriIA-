package android.provider

object Settings {
    const val ACTION_APPLICATION_DETAILS_SETTINGS = "android.settings.APPLICATION_DETAILS_SETTINGS"
    const val ACTION_ACCESSIBILITY_SETTINGS = "android.settings.ACCESSIBILITY_SETTINGS"

    object Secure {
        const val ANDROID_ID = "android_id"
        fun getString(resolver: Any?, name: String): String = "ios_device_id"
    }
}
