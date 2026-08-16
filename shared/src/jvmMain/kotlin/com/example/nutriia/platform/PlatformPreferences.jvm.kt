package com.example.nutriia.platform

actual object PlatformPreferences {
    private val storage = mutableMapOf<String, Any>()

    actual fun getString(key: String, default: String?): String? {
        return (storage[key] as? String) ?: default
    }

    actual fun putString(key: String, value: String?) {
        if (value == null) storage.remove(key) else storage[key] = value
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return (storage[key] as? Boolean) ?: default
    }

    actual fun putBoolean(key: String, value: Boolean) {
        storage[key] = value
    }

    actual fun remove(key: String) {
        storage.remove(key)
    }
}
