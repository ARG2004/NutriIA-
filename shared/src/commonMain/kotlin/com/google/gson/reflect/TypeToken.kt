package com.google.gson.reflect

open class TypeToken<T> {
    val type: Any? = null
    companion object {
        fun <T> get(type: Any?): TypeToken<T> = object : TypeToken<T>() {}
    }
}
