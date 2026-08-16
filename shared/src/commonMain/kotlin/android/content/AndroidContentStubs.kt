package android.content

import java.io.File

interface SharedPreferences {
    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putInt(key: String, value: Int): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
        fun apply()
        fun commit(): Boolean
    }
    fun getString(key: String, defValue: String? = null): String?
    fun getInt(key: String, defValue: Int = 0): Int
    fun getBoolean(key: String, defValue: Boolean = false): Boolean
    fun edit(): Editor
}

abstract class BroadcastReceiver {
    open fun onReceive(context: Context?, intent: Intent?) {}
}

open class Context {
    val filesDir: File get() = File("")
    val applicationContext: Context get() = this

    open fun deleteSharedPreferences(name: String): Boolean = true
    open fun getSharedPreferences(name: String, mode: Int): SharedPreferences = object : SharedPreferences {
        private val map = mutableMapOf<String, Any?>()
        override fun getString(key: String, defValue: String?): String? = map[key] as? String ?: defValue
        override fun getInt(key: String, defValue: Int): Int = map[key] as? Int ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = map[key] as? Boolean ?: defValue
        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?): SharedPreferences.Editor { map[key] = value; return this }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { map[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { map[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { map.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { map.clear(); return this }
            override fun apply() {}
            override fun commit(): Boolean = true
        }
    }
    open fun getSystemService(name: String): Any? = when (name) {
        CLIPBOARD_SERVICE -> ClipboardManager()
        ACCESSIBILITY_SERVICE -> android.view.accessibility.AccessibilityManager()
        else -> null
    }
    open fun getPackageName(): String = "com.example.nutriia"
    open fun getPackageManager(): android.content.pm.PackageManager = android.content.pm.PackageManager()
    open fun startActivity(intent: Intent) {}

    companion object {
        const val MODE_PRIVATE = 0
        const val NOTIFICATION_SERVICE = "notification"
        const val ACCESSIBILITY_SERVICE = "accessibility"
        const val CLIPBOARD_SERVICE = "clipboard"
    }
}

class ClipboardManager {
    fun setPrimaryClip(clip: ClipData) {}
}

class ClipData(val label: CharSequence?, val mimeTypes: Array<String>, val item: Any?) {
    companion object {
        fun newPlainText(label: CharSequence?, text: CharSequence?): ClipData = ClipData(label, arrayOf("text/plain"), text)
    }
}

open class Intent(action: String? = null, uri: android.net.Uri? = null) {
    constructor(packageContext: Context, cls: Any?) : this()
    var data: android.net.Uri? = uri
    var flags: Int = 0
    var type: String? = null

    fun setPackage(pkg: String): Intent = this
    fun putExtra(name: String, value: String): Intent = this
    fun putExtra(name: String, value: Int): Intent = this
    fun putExtra(name: String, value: Long): Intent = this
    fun putExtra(name: String, value: Boolean): Intent = this
    fun putExtra(name: String, value: Array<String>): Intent = this
    fun getStringExtra(name: String): String? = null
    fun getIntExtra(name: String, defValue: Int): Int = defValue
    fun getBooleanExtra(name: String, defValue: Boolean): Boolean = defValue

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_INSERT = "android.intent.action.INSERT"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val FLAG_ACTIVITY_NEW_TASK = 268435456
        const val ACTION_ACCESSIBILITY_SETTINGS = "android.settings.ACCESSIBILITY_SETTINGS"

        fun createChooser(target: Intent, title: CharSequence?): Intent = target
    }
}

object CalendarContract {
    object Events {
        val CONTENT_URI: android.net.Uri = android.net.Uri()
        const val TITLE = "title"
        const val DESCRIPTION = "description"
        const val EVENT_LOCATION = "eventLocation"
        const val ALL_DAY = "allDay"
    }
    const val EXTRA_EVENT_BEGIN_TIME = "beginTime"
    const val EXTRA_EVENT_END_TIME = "endTime"
}
