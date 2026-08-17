package com.example.nutriia.accesibilidad

object KeyDeobfuscator {

    private val MASK = byteArrayOf(0x57, 0x39, 0x41, 0x6E, 0x75, 0x74, 0x72, 0x49, 0x41, 0x21, 0x39, 0x38)

    fun deobfuscate(key: String): String {
        if (key.isBlank() || key == "TU_CLAVE_GROQ_AQUI") return ""
        val trimmed = key.trim()
        if (trimmed.startsWith("gsk_")) return trimmed

        return try {
            val decodedBytes = decodeBase64Bytes(trimmed)
            if (decodedBytes.isEmpty()) return trimmed

            val xorResult = ByteArray(decodedBytes.size)
            for (i in decodedBytes.indices) {
                xorResult[i] = (decodedBytes[i].toInt() xor MASK[i % MASK.size].toInt()).toByte()
            }
            val str = xorResult.decodeToString()
            if (str.isNotBlank() && str.startsWith("gsk_")) {
                str
            } else {
                val plain = decodedBytes.decodeToString()
                if (plain.isNotBlank() && plain.startsWith("gsk_")) plain else trimmed
            }
        } catch (_: Exception) {
            trimmed
        }
    }

    private fun decodeBase64Bytes(input: String): ByteArray {
        val base64Chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val clean = input.filter { it in base64Chars || it == '=' }
        if (clean.isEmpty()) return ByteArray(0)

        val bytes = mutableListOf<Byte>()
        var buffer = 0
        var bitsCollected = 0

        for (c in clean) {
            if (c == '=') break
            val value = base64Chars.indexOf(c)
            if (value < 0) continue
            buffer = (buffer shl 6) or value
            bitsCollected += 6
            if (bitsCollected >= 8) {
                bitsCollected -= 8
                bytes.add(((buffer shr bitsCollected) and 0xFF).toByte())
            }
        }

        return bytes.toByteArray()
    }
}
