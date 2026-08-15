package androidx.security.crypto

import android.content.Context
import android.content.SharedPreferences

class MasterKey {
    enum class KeyScheme { AES256_GCM }

    class Builder(context: Context, keyAlias: String = "") {
        fun setKeyScheme(scheme: KeyScheme): Builder = this
        fun build(): MasterKey = MasterKey()
    }
}

object EncryptedSharedPreferences {
    enum class PrefKeyEncryptionScheme { AES256_SIV }
    enum class PrefValueEncryptionScheme { AES256_GCM }

    fun create(
        context: Context,
        fileName: String,
        masterKey: MasterKey,
        prefKeyEncryptionScheme: PrefKeyEncryptionScheme,
        prefValueEncryptionScheme: PrefValueEncryptionScheme
    ): SharedPreferences = object : SharedPreferences {
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
}
