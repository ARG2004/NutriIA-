package android.provider

object Settings {
    object Secure {
        const val ANDROID_ID = "android_id"
        fun getString(resolver: Any?, name: String): String = "ios_device_id"
    }
}

package android

object Manifest {
    object permission {
        const val CAMERA = "android.permission.CAMERA"
        const val RECORD_AUDIO = "android.permission.RECORD_AUDIO"
        const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"
        const val ACCESS_FINE_LOCATION = "android.permission.ACCESS_FINE_LOCATION"
    }
}

package android.annotation

annotation class成员SuppressLint(vararg val value: String)

package androidx.annotation

annotation class Keep
annotation class RequiresApi(val value: Int = 0, val api: Int = 0)
