package com.example.nutriia.platform

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

actual object PlatformHttp {

    actual suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
        timeoutMs: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val u = URL(url)
            val conn = u.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = timeoutMs.toInt()
            conn.readTimeout = timeoutMs.toInt()
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

            conn.outputStream.use { os ->
                os.write(jsonBody.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

            if (code in 200..299) {
                Result.success(responseText)
            } else {
                Result.failure(Exception("HTTP $code: $responseText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual suspend fun get(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val u = URL(url)
            val conn = u.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = timeoutMs.toInt()
            conn.readTimeout = timeoutMs.toInt()
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val responseText = stream?.bufferedReader()?.use(BufferedReader::readText) ?: ""

            if (code in 200..299) {
                Result.success(responseText)
            } else {
                Result.failure(Exception("HTTP $code: $responseText"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
