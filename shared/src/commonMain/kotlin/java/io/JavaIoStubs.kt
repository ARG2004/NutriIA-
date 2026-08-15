package java.io

open class IOException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)

class ByteArrayOutputStream(size: Int = 32) {
    private val buffer = mutableListOf<Byte>()
    fun write(b: Int) { buffer.add(b.toByte()) }
    fun write(b: ByteArray, off: Int, len: Int) {
        for (i in off until (off + len)) {
            if (i < b.size) buffer.add(b[i])
        }
    }
    fun toByteArray(): ByteArray = buffer.toByteArray()
    fun size(): Int = buffer.size
    fun reset() { buffer.clear() }
    fun close() {}
}

open class InputStream {
    open fun read(): Int = -1
    open fun read(b: ByteArray): Int = -1
    open fun close() {}
}

class InputStreamReader(private val inputStream: InputStream, charsetName: String = "UTF-8") {
    fun readText(): String = ""
    fun close() {}
}

class File(val path: String) {
    constructor(parent: File, child: String) : this("${parent.path}/$child")
    constructor(parent: String, child: String) : this("$parent/$child")

    val name: String get() = path.substringAfterLast('/')
    val absolutePath: String get() = path
    fun exists(): Boolean = true
    fun length(): Long = 0L
    fun delete(): Boolean = true
    fun mkdirs(): Boolean = true
}
