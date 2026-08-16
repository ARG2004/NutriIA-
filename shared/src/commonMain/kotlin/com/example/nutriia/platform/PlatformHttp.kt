package com.example.nutriia.platform

expect object PlatformHttp {
    suspend fun postJson(
        url: String,
        headers: Map<String, String> = emptyMap(),
        jsonBody: String,
        timeoutMs: Long = 30000L
    ): Result<String>

    suspend fun get(
        url: String,
        headers: Map<String, String> = emptyMap(),
        timeoutMs: Long = 30000L
    ): Result<String>
}
