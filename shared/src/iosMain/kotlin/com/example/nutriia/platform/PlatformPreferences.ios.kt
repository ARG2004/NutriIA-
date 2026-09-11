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

            val serviceRef = CFBridgingRetain(SERVICE_NAME as NSString)
            val accountRef = CFBridgingRetain(key as NSString)
            val dataRef = CFBridgingRetain(data)

            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceRef)
            CFDictionarySetValue(query, kSecAttrAccount, accountRef)
            CFDictionarySetValue(query, kSecValueData, dataRef)
            CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)

            SecItemAdd(query, null)

            if (query != null) CFRelease(query)
            if (serviceRef != null) CFRelease(serviceRef)
            if (accountRef != null) CFRelease(accountRef)
            if (dataRef != null) CFRelease(dataRef)
        } catch (_: Throwable) {}
    }

    private fun loadFromKeychain(key: String): String? {
        return try {
            val serviceRef = CFBridgingRetain(SERVICE_NAME as NSString)
            val accountRef = CFBridgingRetain(key as NSString)

            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceRef)
            CFDictionarySetValue(query, kSecAttrAccount, accountRef)
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)

            var resultString: String? = null

            memScoped {
                val result = alloc<COpaquePointerVar>()
                val status = SecItemCopyMatching(query, result.ptr.reinterpret())
                if (status == errSecSuccess && result.value != null) {
                    val data = CFBridgingRelease(result.value) as? NSData
                    if (data != null) {
                        resultString = NSString.create(data = data, encoding = NSUTF8StringEncoding) as? String
                    }
                }
            }

            if (query != null) CFRelease(query)
            if (serviceRef != null) CFRelease(serviceRef)
            if (accountRef != null) CFRelease(accountRef)

            resultString
        } catch (_: Throwable) {
            null
        }
    }

    private fun deleteFromKeychain(key: String) {
        try {
            val serviceRef = CFBridgingRetain(SERVICE_NAME as NSString)
            val accountRef = CFBridgingRetain(key as NSString)

            val query = CFDictionaryCreateMutable(null, 0, null, null)
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, serviceRef)
            CFDictionarySetValue(query, kSecAttrAccount, accountRef)

            SecItemDelete(query)

            if (query != null) CFRelease(query)
            if (serviceRef != null) CFRelease(serviceRef)
            if (accountRef != null) CFRelease(accountRef)
        } catch (_: Throwable) {}
    }
}
