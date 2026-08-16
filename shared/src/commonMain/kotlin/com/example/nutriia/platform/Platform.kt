package com.example.nutriia.platform

expect fun getPlatformName(): String

expect fun currentTimeMillis(): Long

expect fun openUrl(url: String)

expect fun platformLog(tag: String, msg: String)

expect fun isVoiceOverActive(): Boolean

fun generateUUID(): String {
    val chars = "0123456789abcdef"
    val time = currentTimeMillis().toString(16)
    val randomPart = (1..16).map { chars.random() }.joinToString("")
    return "$time-$randomPart"
}

object Log {
    fun d(tag: String, msg: String) = platformLog("DEBUG-$tag", msg)
    fun i(tag: String, msg: String) = platformLog("INFO-$tag", msg)
    fun w(tag: String, msg: String, tr: Throwable? = null) = platformLog("WARN-$tag", if (tr != null) "$msg -> ${tr.message}\n${tr.stackTraceToString()}" else msg)
    fun e(tag: String, msg: String, tr: Throwable? = null) = platformLog("ERROR-$tag", if (tr != null) "$msg -> ${tr.message}\n${tr.stackTraceToString()}" else msg)
}

object Logger {
    fun d(tag: String, message: String) = Log.d(tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = Log.e(tag, message, throwable)
    fun i(tag: String, message: String) = Log.i(tag, message)
    fun w(tag: String, message: String) = Log.w(tag, message)
}
