package com.example.nutriia.platform

import com.example.nutriia.accesibilidad.KeyDeobfuscator
import platform.Foundation.NSBundle

actual object PlatformConfig {

    private const val OBFUSCATED_FALLBACK = "MEoqMUUBJDwpTkhBYW0IODMyNhB2b0l0AH4lFxdHNBB4c3QID28zLz48PjwME1FyAQ8RCiZGRx0="

    actual val groqApiKey: String
        get() {
            val plistKey = NSBundle.mainBundle.objectForInfoDictionaryKey("GROQ_API_KEY") as? String
            if (!plistKey.isNullOrBlank()) {
                val deob = KeyDeobfuscator.deobfuscate(plistKey)
                if (deob.isNotBlank()) return deob
            }
            return KeyDeobfuscator.deobfuscate(OBFUSCATED_FALLBACK)
        }
}
