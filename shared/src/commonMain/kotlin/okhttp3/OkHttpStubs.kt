package okhttp3

import java.io.IOException

class MediaType private constructor(val type: String) {
    companion object {
        fun String.toMediaType(): MediaType = MediaType(this)
    }
}

open class RequestBody {
    companion object {
        fun String.toRequestBody(mediaType: MediaType? = null): RequestBody = RequestBody()
    }
}

class ResponseBody {
    fun string(): String = "{}"
}

class Response(
    val code: Int = 200,
    val isSuccessful: Boolean = true,
    val body: ResponseBody? = ResponseBody()
)

interface Call {
    fun execute(): Response = Response()
    fun enqueue(responseCallback: Callback) {}
    fun cancel() {}
}

interface Callback {
    fun onFailure(call: Call, e: IOException)
    fun onResponse(call: Call, response: Response)
}

class Request private constructor(
    val url: String = "",
    val method: String = "GET"
) {
    class Builder {
        private var url: String = ""
        private var method: String = "GET"
        private val headers = mutableMapOf<String, String>()
        private var body: RequestBody? = null

        fun url(url: String): Builder { this.url = url; return this }
        fun addHeader(name: String, value: String): Builder { headers[name] = value; return this }
        fun header(name: String, value: String): Builder { headers[name] = value; return this }
        fun post(body: RequestBody): Builder { this.method = "POST"; this.body = body; return this }
        fun get(): Builder { this.method = "GET"; return this }
        fun build(): Request = Request(url, method)
    }
}

class OkHttpClient {
    fun newCall(request: Request): Call = object : Call {
        override fun execute(): Response = Response()
        override fun enqueue(responseCallback: Callback) {
            responseCallback.onResponse(this, Response())
        }
        override fun cancel() {}
    }

    class Builder {
        fun connectTimeout(timeout: Long, unit: Any): Builder = this
        fun readTimeout(timeout: Long, unit: Any): Builder = this
        fun writeTimeout(timeout: Long, unit: Any): Builder = this
        fun build(): OkHttpClient = OkHttpClient()
    }
}
