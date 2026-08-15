package android.content

open class Context
open class Intent(action: String? = null, uri: android.net.Uri? = null) {
    fun setPackage(pkg: String): Intent = this
    fun putExtra(name: String, value: String): Intent = this
    fun putExtra(name: String, value: Int): Intent = this
    fun putExtra(name: String, value: Boolean): Intent = this
    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
    }
}
