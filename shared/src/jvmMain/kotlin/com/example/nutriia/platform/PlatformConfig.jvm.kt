package com.example.nutriia.platform

import com.example.nutriia.accesibilidad.KeyDeobfuscator

actual object PlatformConfig {

    private const val OBFUSCATED_FALLBACK = "MEoqMUUBJDwpTkhBYW0IODMyNhB2b0l0AH4lFxdHNBB4c3QID28zLz48PjwME1FyAQ8RCiZGRx0="

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
