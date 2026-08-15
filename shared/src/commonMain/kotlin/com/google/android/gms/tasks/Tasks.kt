package com.google.android.gms.tasks

open class Task<T> {
    private var result: T? = null
    fun isSuccessful(): Boolean = true
    fun getResult(): T = result as T
    fun addOnSuccessListener(listener: (T) -> Unit): Task<T> {
        result?.let { listener(it) }
        return this
    }
    fun addOnFailureListener(listener: (Exception) -> Unit): Task<T> = this
}

suspend fun <T> Task<T>.await(): T = getResult()

object Tasks {
    fun <T> forResult(result: T): Task<T> = Task<T>()
}
