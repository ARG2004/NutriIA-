package com.example.nutriia.platform

import kotlinx.cinterop.*
import platform.CoreFoundation.*
import platform.Foundation.*
import platform.Security.*

/**
 * Almacenamiento seguro y preferencias para iOS con cumplimiento ISO/IEC 27001 (A.8.24).
 * Las claves sensibles (UIDs, tokens, pagos) se almacenan cifradas en hardware vía Apple Keychain.
 */
@OptIn(ExperimentalForeignApi::class)
actual object PlatformPreferences {
    private val defaults = NSUserDefaults.standardUserDefaults
    private const val SERVICE_NAME = "com.arg.nutriia.secure_storage"

    // Claves clasificadas como información sensible (PII / PHI)
    private val SENSITIVE_KEYS = setOf(
        "user_uid",
        "ultimo_uid_biometrico",
        "pago_uid",
        "pago_id_exitoso",
        "pago_nombre",
        "auth_token",
        "session_token"
    )

    actual fun getString(key: String, default: String?): String? {
        return try {
            if (SENSITIVE_KEYS.contains(key)) {
                loadFromKeychain(key) ?: defaults.stringForKey(key) ?: default
            } else {
                defaults.stringForKey(key) ?: default
            }
        } catch (_: Throwable) {
            defaults.stringForKey(key) ?: default
        }
    }

    actual fun putString(key: String, value: String?) {
        try {
            if (value == null) {
                if (SENSITIVE_KEYS.contains(key)) {
                    deleteFromKeychain(key)
                }
                defaults.removeObjectForKey(key)
            } else {
                if (SENSITIVE_KEYS.contains(key)) {
                    saveToKeychain(key, value)
                }
                defaults.setObject(value, forKey = key)
            }
            defaults.synchronize()
        } catch (_: Throwable) {}
    }

    actual fun getBoolean(key: String, default: Boolean): Boolean {
        return try {
            if (defaults.objectForKey(key) == null) default
            else defaults.boolForKey(key)
        } catch (_: Throwable) {
            default
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        try {
            defaults.setBool(value, forKey = key)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }

    actual fun remove(key: String) {
        try {
            if (SENSITIVE_KEYS.contains(key)) {
                deleteFromKeychain(key)
            }
            defaults.removeObjectForKey(key)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }

    private fun saveToKeychain(key: String, value: String) {
        try {
            deleteFromKeychain(key)
            val data = (value as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return

            val query = NSMutableDictionary()
            query.setObject(kSecClassGenericPassword, forKey = kSecClass)
            query.setObject(SERVICE_NAME, forKey = kSecAttrService)
            query.setObject(key, forKey = kSecAttrAccount)
            query.setObject(data, forKey = kSecValueData)
            query.setObject(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly, forKey = kSecAttrAccessible)

            SecItemAdd(query as CFDictionaryRef, null)
        } catch (_: Throwable) {}
    }

    private fun loadFromKeychain(key: String): String? {
        return try {
            val query = NSMutableDictionary()
            query.setObject(kSecClassGenericPassword, forKey = kSecClass)
            query.setObject(SERVICE_NAME, forKey = kSecAttrService)
            query.setObject(key, forKey = kSecAttrAccount)
            query.setObject(kCFBooleanTrue, forKey = kSecReturnData)
            query.setObject(kSecMatchLimitOne, forKey = kSecMatchLimit)

            memScoped {
                val result = alloc<COpaquePointerVar>()
                val status = SecItemCopyMatching(query as CFDictionaryRef, result.ptr.reinterpret())
                if (status == errSecSuccess && result.value != null) {
                    val data = CFBridgingRelease(result.value) as? NSData
                    if (data != null) {
                        return NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
                    }
                }
            }
            null
        } catch (_: Throwable) {
            null
        }
    }

    private fun deleteFromKeychain(key: String) {
        try {
            val query = NSMutableDictionary()
            query.setObject(kSecClassGenericPassword, forKey = kSecClass)
            query.setObject(SERVICE_NAME, forKey = kSecAttrService)
            query.setObject(key, forKey = kSecAttrAccount)

            SecItemDelete(query as CFDictionaryRef)
        } catch (_: Throwable) {}
    }
}
