package com.example.nutriia.platform

expect fun getPlatformName(): String

expect fun currentTimeMillis(): Long

expect fun openUrl(url: String)

fun generateUUID(): String {
    val chars = "0123456789abcdef"
    val time = currentTimeMillis().toString(16)
    val randomPart = (1..16).map { chars.random() }.joinToString("")
    return "$time-$randomPart"
}

object Log {
    fun d(tag: String, msg: String) = println("DEBUG [$tag] $msg")
    fun i(tag: String, msg: String) = println("INFO [$tag] $msg")
    fun w(tag: String, msg: String, tr: Throwable? = null) = println("WARN [$tag] $msg")
    fun e(tag: String, msg: String, tr: Throwable? = null) = println("ERROR [$tag] $msg")
}

object Logger {
    fun d(tag: String, message: String) = Log.d(tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = Log.e(tag, message, throwable)
    fun i(tag: String, message: String) = Log.i(tag, message)
    fun w(tag: String, message: String) = Log.w(tag, message)
}
