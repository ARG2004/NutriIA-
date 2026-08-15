package android.os

interface Parcelable

class Bundle {
    private val map = mutableMapOf<String, Any?>()
    fun putString(key: String, value: String?) { map[key] = value }
    fun getString(key: String): String? = map[key] as? String
    fun getString(key: String, defaultValue: String): String = (map[key] as? String) ?: defaultValue
    fun putInt(key: String, value: Int) { map[key] = value }
    fun getInt(key: String, defaultValue: Int = 0): Int = (map[key] as? Int) ?: defaultValue
    fun putBoolean(key: String, value: Boolean) { map[key] = value }
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean = (map[key] as? Boolean) ?: defaultValue
    fun getStringArrayList(key: String): ArrayList<String>? = map[key] as? ArrayList<String>
    fun putStringArrayList(key: String, value: ArrayList<String>?) { map[key] = value }
}

class Looper private constructor() {
    companion object {
        fun getMainLooper(): Looper = Looper()
    }
}

class Handler(val looper: Looper = Looper.getMainLooper()) {
    fun post(r: Runnable): Boolean {
        r.run()
        return true
    }
    fun postDelayed(r: Runnable, delayMillis: Long): Boolean {
        r.run()
        return true
    }
    fun removeCallbacks(r: Runnable) {}
}

object SystemClock {
    fun elapsedRealtime(): Long = kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
}

object Build {
    object VERSION {
        const val SDK_INT = 34
    }
    object VERSION_CODES {
        const val M = 23
        const val N = 24
        const val O = 26
        const val P = 28
        const val Q = 29
        const val R = 30
        const val S = 31
        const val TIRAMISU = 33
        const val UPSIDE_DOWN_CAKE = 34
    }
}
