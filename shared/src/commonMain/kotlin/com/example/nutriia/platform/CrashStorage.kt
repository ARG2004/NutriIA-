package com.example.nutriia.platform

expect object CrashStorage {
    fun saveCrash(crashText: String)
    fun loadCrash(): String?
    fun clearCrash()
}
