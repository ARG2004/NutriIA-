package com.example.nutriia.platform

import com.example.nutriia.accesibilidad.KeyDeobfuscator

actual object PlatformConfig {

    private const val OBFUSCATED_FALLBACK = "MEoqMTpNCh55cFx0YVIHViIWQBA3bwFSAH4lFxdHNBAMEgFIYld2CjFDAR4wcFJgPH9xHEMiRiI="

    actual val groqApiKey: String
        get() {
            val envKey = System.getenv("GROQ_API_KEY")
            if (!envKey.isNullOrBlank()) {
                val deob = KeyDeobfuscator.deobfuscate(envKey)
                if (deob.isNotBlank()) return deob
            }
            return KeyDeobfuscator.deobfuscate(OBFUSCATED_FALLBACK)
        }
}
