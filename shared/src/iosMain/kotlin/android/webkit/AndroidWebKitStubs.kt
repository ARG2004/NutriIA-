package android.webkit

import android.content.Context

annotation class JavascriptInterface

open class WebSettings {
    var javaScriptEnabled: Boolean = true
    var domStorageEnabled: Boolean = true
}

open class WebResourceRequest {
    open fun getUrl(): android.net.Uri? = null
}

open class WebViewClient {
    open fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean = false
    open fun onPageFinished(view: WebView?, url: String?) {}
}

open class WebView(context: Context) {
    val settings: WebSettings = WebSettings()
    var webViewClient: WebViewClient = WebViewClient()

    fun addJavascriptInterface(`object`: Any, name: String) {}
    fun loadUrl(url: String) {}
    fun evaluateJavascript(script: String, resultCallback: ((String?) -> Unit)? = null) {}
    fun stopLoading() {}
    fun destroy() {}
}
