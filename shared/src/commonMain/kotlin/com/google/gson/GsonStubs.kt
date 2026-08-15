package com.google.gson

import kotlin.reflect.KClass

open class JsonElement {
    open fun isJsonObject(): Boolean = false
    open fun isJsonArray(): Boolean = false
    open fun isJsonPrimitive(): Boolean = false
    open fun getAsJsonObject(): JsonObject = this as JsonObject
    open fun getAsJsonArray(): JsonArray = this as JsonArray
    open fun getAsString(): String = toString()
    open fun getAsInt(): Int = toString().toIntOrNull() ?: 0
    open fun getAsLong(): Long = toString().toLongOrNull() ?: 0L
    open fun getAsDouble(): Double = toString().toDoubleOrNull() ?: 0.0
    open fun getAsBoolean(): Boolean = toString().toBoolean()
}

class JsonObject : JsonElement() {
    private val map = mutableMapOf<String, JsonElement>()

    fun add(property: String, value: JsonElement?) {
        if (value != null) map[property] = value
    }
    fun addProperty(property: String, value: String?) {
        if (value != null) map[property] = JsonPrimitive(value)
    }
    fun addProperty(property: String, value: Number?) {
        if (value != null) map[property] = JsonPrimitive(value)
    }
    fun addProperty(property: String, value: Boolean?) {
        if (value != null) map[property] = JsonPrimitive(value)
    }
    fun get(member: String): JsonElement? = map[member]
    fun getAsJsonObject(member: String): JsonObject = (map[member] as? JsonObject) ?: JsonObject()
    fun getAsJsonArray(member: String): JsonArray = (map[member] as? JsonArray) ?: JsonArray()
    fun has(member: String): Boolean = map.containsKey(member)
    override fun toString(): String = "{}"
}

class JsonArray : JsonElement(), Iterable<JsonElement> {
    private val list = mutableListOf<JsonElement>()

    fun add(element: JsonElement) { list.add(element) }
    fun add(value: String) { list.add(JsonPrimitive(value)) }
    fun get(i: Int): JsonElement = list.getOrElse(i) { JsonPrimitive("") }
    fun size(): Int = list.size
    override fun iterator(): Iterator<JsonElement> = list.iterator()
    override fun toString(): String = "[]"
}

class JsonPrimitive(private val value: Any) : JsonElement() {
    override fun getAsString(): String = value.toString()
    override fun getAsInt(): Int = value.toString().toIntOrNull() ?: 0
    override fun getAsLong(): Long = value.toString().toLongOrNull() ?: 0L
    override fun getAsDouble(): Double = value.toString().toDoubleOrNull() ?: 0.0
    override fun getAsBoolean(): Boolean = value.toString().toBoolean()
    override fun toString(): String = value.toString()
}

class JsonParser {
    companion object {
        fun parseString(json: String): JsonElement = JsonObject()
    }
}

open class TypeToken<T>

class Gson {
    fun toJson(src: Any?): String = "{}"
    fun <T> fromJson(json: String, classOfT: Class<T>? = null): T? = null
    fun <T> fromJson(json: String, typeToken: TypeToken<T>? = null): T? = null
}
