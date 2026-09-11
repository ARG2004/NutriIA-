package com.example.nutriia.platform

import platform.Foundation.NSLog
import platform.Foundation.NSUserDefaults

/**
 * Almacenamiento y sanitización de registros de errores conforme a ISO/IEC 27001 (A.8.15 y A.8.16).
 * Enmascara PII/PHI (correos, UIDs, tokens) para evitar fugas de información.
 */
actual object CrashStorage {
    private const val KEY_CRASH = "LAST_CRASH_LOG_NUTRIIA"

    actual fun saveCrash(crashText: String) {
        val sanitized = sanitizePII(crashText)
        NSLog("⚠️ [CrashStorage] %s", sanitized)
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.setObject(sanitized, forKey = KEY_CRASH)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }

    actual fun loadCrash(): String? {
        return try {
            NSUserDefaults.standardUserDefaults.stringForKey(KEY_CRASH)
        } catch (_: Throwable) {
            null
        }
    }

    actual fun clearCrash() {
        try {
            val defaults = NSUserDefaults.standardUserDefaults
            defaults.removeObjectForKey(KEY_CRASH)
            defaults.synchronize()
        } catch (_: Throwable) {}
    }
}

/**
 * Enmascara identificadores personales y tokens de seguridad (ISO 27001 / ISO 27701).
 */
fun sanitizePII(text: String): String {
    var result = text
    // Enmascarar correos electrónicos
    result = result.replace(Regex("([a-zA-Z0-9_.+-])[a-zA-Z0-9_.+-]*@([a-zA-Z0-9-]+\\.[a-zA-Z0-9-.]+)")) { m ->
        "${m.groupValues[1]}***@${m.groupValues[2]}"
    }
    // Enmascarar tokens de autorización y API keys
    result = result.replace(Regex("(?i)(bearer\\s+|key=|apikey=|token=)[a-zA-Z0-9_.-]{12,}")) { m ->
        "${m.groupValues[1]}[REDACTED]"
    }
    // Enmascarar UIDs de Firebase
    result = result.replace(Regex("(?i)(uid[:=]\\s*)[a-zA-Z0-9]{20,}")) { m ->
        "${m.groupValues[1]}[REDACTED_UID]"
    }
    return result
}
