package com.example.nutriia.accesibilidad

object KeyDeobfuscator {
    fun deobfuscate(key: String): String {
        if (key.isBlank() || key == "TU_CLAVE_GROQ_AQUI") return ""
        return key
    }
}
