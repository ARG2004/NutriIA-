@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.example.nutriia.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.*
import platform.posix.memcpy
import kotlin.coroutines.resume

actual object PlatformHttp {

    actual suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        jsonBody: String,
        timeoutMs: Long
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                continuation.resume(Result.failure(Exception("URL inválida: $url")))
                return@suspendCancellableCoroutine
            }

            val request = NSMutableURLRequest.requestWithURL(
                URL = nsUrl,
                cachePolicy = NSURLRequestReloadIgnoringCacheData,
                timeoutInterval = timeoutMs / 1000.0
            )
            request.setHTTPMethod("POST")
            request.setValue("application/json; charset=utf-8", forHTTPHeaderField = "Content-Type")
            request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 NutriIA/2.2.7", forHTTPHeaderField = "User-Agent")
            headers.forEach { (k, v) -> request.setValue(v, forHTTPHeaderField = k) }

            val bodyBytes = jsonBody.encodeToByteArray()
            val nsData = bodyBytes.toNSData()
            request.setHTTPBody(nsData)

            val session = NSURLSession.sharedSession
            val task = session.dataTaskWithRequest(request) { data, response, error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    return@dataTaskWithRequest
                }
                val httpResponse = response as? NSHTTPURLResponse
                val statusCode = httpResponse?.statusCode?.toInt() ?: 200
                val responseString = data?.toByteArray()?.decodeToString() ?: ""

                if (statusCode in 200..299) {
                    continuation.resume(Result.success(responseString))
                } else {
                    continuation.resume(Result.failure(Exception("HTTP $statusCode: $responseString")))
                }
            }

            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        } catch (e: Throwable) {
            continuation.resume(Result.failure(Exception(e.message)))
        }
    }

    actual suspend fun get(
        url: String,
        headers: Map<String, String>,
        timeoutMs: Long
    ): Result<String> = suspendCancellableCoroutine { continuation ->
        try {
            val nsUrl = NSURL.URLWithString(url)
            if (nsUrl == null) {
                continuation.resume(Result.failure(Exception("URL inválida: $url")))
                return@suspendCancellableCoroutine
            }

            val request = NSMutableURLRequest.requestWithURL(
                URL = nsUrl,
                cachePolicy = NSURLRequestReloadIgnoringCacheData,
                timeoutInterval = timeoutMs / 1000.0
            )
            request.setHTTPMethod("GET")
            request.setValue("Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 NutriIA/2.2.7", forHTTPHeaderField = "User-Agent")
            headers.forEach { (k, v) -> request.setValue(v, forHTTPHeaderField = k) }

            val session = NSURLSession.sharedSession
            val task = session.dataTaskWithRequest(request) { data, response, error ->
                if (error != null) {
                    continuation.resume(Result.failure(Exception(error.localizedDescription)))
                    return@dataTaskWithRequest
                }
                val httpResponse = response as? NSHTTPURLResponse
                val statusCode = httpResponse?.statusCode?.toInt() ?: 200
                val responseString = data?.toByteArray()?.decodeToString() ?: ""

                if (statusCode in 200..299) {
                    continuation.resume(Result.success(responseString))
                } else {
                    continuation.resume(Result.failure(Exception("HTTP $statusCode: $responseString")))
                }
            }

            continuation.invokeOnCancellation { task.cancel() }
            task.resume()
        } catch (e: Throwable) {
            continuation.resume(Result.failure(Exception(e.message)))
        }
    }
}

private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) return NSData.data()
    return usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), size.toULong())
    }
}

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    val result = ByteArray(length)
    result.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return result
}
