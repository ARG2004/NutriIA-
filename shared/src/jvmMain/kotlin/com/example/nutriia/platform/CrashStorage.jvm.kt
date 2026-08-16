package com.example.nutriia.platform

actual object CrashStorage {
    private var inMemoryCrash: String? = null

    actual fun saveCrash(crashText: String) {
        inMemoryCrash = crashText
    }

    actual fun loadCrash(): String? = inMemoryCrash

    actual fun clearCrash() {
        inMemoryCrash = null
    }
}
