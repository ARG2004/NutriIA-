package com.example.nutriia.platform

import platform.Foundation.NSUserDefaults

actual object CrashStorage {
    private const val KEY_CRASH = "LAST_CRASH_LOG_NUTRIIA"

    actual fun saveCrash(crashText: String) {
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.setObject(crashText, forKey = KEY_CRASH)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }

    actual fun loadCrash(): String? {
        return try {
            NSUserDefaults.standardUserDefaults.stringForKey(KEY_CRASH)
        } catch (_: Throwable) {
            null
        }
    }

    actual fun clearCrash() {
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.removeObjectForKey(KEY_CRASH)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }
}
