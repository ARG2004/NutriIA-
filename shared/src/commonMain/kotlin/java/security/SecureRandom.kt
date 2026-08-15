package java.security

class SecureRandom {
    fun nextInt(bound: Int): Int = kotlin.random.Random.nextInt(bound)
    fun nextBytes(bytes: ByteArray) {
        for (i in bytes.indices) {
            bytes[i] = kotlin.random.Random.nextInt(256).toByte()
        }
    }
}
