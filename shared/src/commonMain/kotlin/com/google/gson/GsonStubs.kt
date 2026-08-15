package com.google.gson

open class JsonElement {
    open fun isJsonObject(): Boolean = false
    open fun isJsonArray(): Boolean = false
    open fun isJsonPrimitive(): Boolean = false
    open fun isJsonNull(): Boolean = false

    val isJsonObject: Boolean get() = isJsonObject()
    val isJsonArray: Boolean get() = isJsonArray()
    val isJsonPrimitive: Boolean get() = isJsonPrimitive()
    val isJsonNull: Boolean get() = isJsonNull()

    open fun getAsJsonObject(): JsonObject = this as JsonObject
    open fun getAsJsonArray(): JsonArray = this as JsonArray
    open fun getAsString(): String = toString()
    open fun getAsInt(): Int = toString().toIntOrNull() ?: 0
    open fun getAsLong(): Long = toString().toLongOrNull() ?: 0L
    open fun getAsDouble(): Double = toString().toDoubleOrNull() ?: 0.0
    open fun getAsBoolean(): Boolean = toString().toBoolean()

    val asJsonObject: JsonObject get() = getAsJsonObject()
    val asJsonArray: JsonArray get() = getAsJsonArray()
    val asString: String get() = getAsString()
    val asInt: Int get() = getAsInt()
    val asLong: Long get() = getAsLong()
    val asDouble: Double get() = getAsDouble()
    val asBoolean: Boolean get() = getAsBoolean()
}

class JsonObject : JsonElement() {
    private val map = mutableMapOf<String, JsonElement>()

    override fun isJsonObject(): Boolean = true

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
    operator fun get(member: String): JsonElement? = map[member]
    fun getAsJsonObject(member: String): JsonObject = (map[member] as? JsonObject) ?: JsonObject()
    fun getAsJsonArray(member: String): JsonArray = (map[member] as? JsonArray) ?: JsonArray()
    fun has(member: String): Boolean = map.containsKey(member)
    fun entrySet(): Set<Map.Entry<String, JsonElement>> = map.entries
    override fun toString(): String = "{}"
}

class JsonArray : JsonElement(), Iterable<JsonElement> {
    private val list = mutableListOf<JsonElement>()

    override fun isJsonArray(): Boolean = true

    fun add(element: JsonElement) { list.add(element) }
    fun add(value: String) { list.add(JsonPrimitive(value)) }
    operator fun get(i: Int): JsonElement = list.getOrElse(i) { JsonPrimitive("") }
    fun size(): Int = list.size
    val size: Int get() = list.size
    override fun iterator(): Iterator<JsonElement> = list.iterator()
    override fun toString(): String = "[]"
}

class JsonPrimitive(private val value: Any) : JsonElement() {
    override fun isJsonPrimitive(): Boolean = true
    override fun getAsString(): String = value.toString()
    override fun getAsInt(): Int = value.toString().toIntOrNull() ?: 0
    override fun getAsLong(): Long = value.toString().toLongOrNull() ?: 0L
    override fun getAsDouble(): Double = value.toString().toDoubleOrNull() ?: 0.0
    override fun getAsBoolean(): Boolean = value.toString().toBoolean()
    override fun toString(): String = value.toString()
}

class JsonNull : JsonElement() {
    override fun isJsonNull(): Boolean = true
    override fun toString(): String = "null"
    companion object {
        val INSTANCE = JsonNull()
    }
}

class JsonParser {
    companion object {
        fun parseString(json: String): JsonElement = JsonObject()
    }
}

open class TypeToken<T> {
    val type: Any? = null
}

class Gson {
    fun toJson(src: Any?): String = "{}"
    fun toJson(src: Any?, typeOfSrc: Any?): String = "{}"
    fun <T> fromJson(json: String, classOfT: Any? = null): T? = null
    fun <T> fromJson(json: String, typeToken: TypeToken<T>? = null): T? = null
    fun <T> fromJson(reader: Any?, classOfT: Any? = null): T? = null
}
