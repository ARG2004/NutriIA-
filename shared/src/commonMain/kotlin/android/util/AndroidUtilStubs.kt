package android.util

object Log {
    fun d(tag: String?, msg: String?): Int { println("[$tag] $msg"); return 0 }
    fun e(tag: String?, msg: String?): Int { println("[$tag] ERROR: $msg"); return 0 }
    fun e(tag: String?, msg: String?, tr: Throwable?): Int { println("[$tag] ERROR: $msg ${tr?.message}"); return 0 }
    fun i(tag: String?, msg: String?): Int { println("[$tag] $msg"); return 0 }
    fun w(tag: String?, msg: String?): Int { println("[$tag] WARN: $msg"); return 0 }
    fun v(tag: String?, msg: String?): Int { println("[$tag] $msg"); return 0 }
}

object Base64 {
    const val DEFAULT = 0
    const val NO_WRAP = 2
    fun encodeToString(input: ByteArray?, flags: Int): String = ""
    fun decode(str: String?, flags: Int): ByteArray = ByteArray(0)
}

object Patterns {
    val EMAIL_ADDRESS = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}\$")
}
