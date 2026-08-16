package com.example.nutriia.platform

import platform.Foundation.NSUserDefaults

actual object PlatformPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, default: String?): String? {
        return try {
            defaults.stringForKey(key) ?: default
        } catch (_: Throwable) {
            default
        }
    }

    actual fun putString(key: String, value: String?) {
        try {
            if (value == null) {
                defaults.removeObjectForKey(key)
            } else {
                defaults.setObject(value, forKey = key)
            }
            defaults.synchronize()
        } catch (_: Throwable) {}
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return try {
            if (defaults.objectForKey(key) == null) default
            else defaults.boolForKey(key)
        } catch (_: Throwable) {
            default
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        try {
            defaults.setBool(value, forKey = key)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }

    actual fun remove(key: String) {
        try {
            defaults.removeObjectForKey(key)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }
}
