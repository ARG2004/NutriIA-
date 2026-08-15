package android.content

interface SharedPreferences {
    interface Editor {
        fun putString(key: String, value: String?): Editor
        fun putInt(key: String, value: Int): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun apply()
        fun commit(): Boolean
    }
    fun getString(key: String, defValue: String?): String?
    fun getInt(key: String, defValue: Int): Int
    fun getBoolean(key: String, defValue: Boolean): Boolean
    fun edit(): Editor
}

abstract class BroadcastReceiver {
    abstract fun onReceive(context: Context?, intent: Intent?)
}

class PackageManager {
    fun hasSystemFeature(featureName: String): Boolean = true
}

open class Context {
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
            override fun apply() {}
            override fun commit(): Boolean = true
        }
    }
    open fun getSystemService(name: String): Any? = null
    open fun getPackageName(): String = "com.example.nutriia"
    open fun getPackageManager(): PackageManager = PackageManager()
    open fun startActivity(intent: Intent) {}
}

open class Intent(action: String? = null, uri: android.net.Uri? = null) {
    var data: android.net.Uri? = uri
    fun setPackage(pkg: String): Intent = this
    fun putExtra(name: String, value: String): Intent = this
    fun putExtra(name: String, value: Int): Intent = this
    fun putExtra(name: String, value: Boolean): Intent = this
    fun putExtra(name: String, value: Array<String>): Intent = this
    fun getStringExtra(name: String): String? = null
    fun getIntExtra(name: String, defValue: Int): Int = defValue
    fun getBooleanExtra(name: String, defValue: Boolean): Boolean = defValue

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val EXTRA_SUBJECT = "android.intent.extra.SUBJECT"
        const val EXTRA_TEXT = "android.intent.extra.TEXT"
        const val FLAG_ACTIVITY_NEW_TASK = 268435456
    }
}
