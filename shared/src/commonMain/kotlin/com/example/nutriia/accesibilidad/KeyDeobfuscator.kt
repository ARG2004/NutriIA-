package com.example.nutriia.accesibilidad

import android.util.Base64

object KeyDeobfuscator {
    private val MASK = byteArrayOf(0x57, 0x39, 0x41, 0x6E, 0x75, 0x74, 0x72, 0x49, 0x41, 0x21, 0x39, 0x38)

    fun deobfuscate(key: String): String {
        if (key.isBlank() || key == "TU_CLAVE_GROQ_AQUI") return ""
        return try {
            val decoded = Base64.decode(key, Base64.DEFAULT)
            val result = ByteArray(decoded.size)
            for (i in decoded.indices) {
                result[i] = (decoded[i].toInt() xor MASK[i % MASK.size].toInt()).toByte()
            }
            String(result, Charsets.UTF_8)
        } catch (e: Exception) {
            key
        }
    }
}
