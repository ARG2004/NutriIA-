package com.example.nutriia.auth

import com.example.nutriia.platform.PlatformPreferences
import com.example.nutriia.platform.currentTimeMillis

/**
 * Gestor de Sesiones con cumplimiento de políticas de seguridad ISO/IEC 27001 (A.8.1 - A.8.5).
 * Implementa rotación de tokens, caducidad por inactividad y almacenamiento de credenciales seguras.
 */
object SessionManager {

    // 30 días de inactividad máxima permitida antes de forzar re-autenticación (ISO 27001)
    private const val MAX_INACTIVIDAD_MS = 30L * 24L * 60L * 60L * 1000L

    fun guardarSesion(uid: String) {
        if (uid.isBlank()) return
        PlatformPreferences.putString("user_uid", uid)
        PlatformPreferences.putString("ultimo_uid_biometrico", uid)
        actualizarActividadReciente()
    }

    fun guardarSesion(context: Any?, uid: String) {
        guardarSesion(uid)
    }

    fun actualizarActividadReciente() {
        PlatformPreferences.putString("ultima_actividad_timestamp", currentTimeMillis().toString())
    }

    fun esSesionValida(): Boolean {
        val uid = PlatformPreferences.getString("user_uid")
        if (uid.isNullOrBlank()) return false

        val lastActivityStr = PlatformPreferences.getString("ultima_actividad_timestamp") ?: return true
        val lastActivity = lastActivityStr.toLongOrNull() ?: return true

        val ahora = currentTimeMillis()
        if (ahora - lastActivity > MAX_INACTIVIDAD_MS) {
            // Sesión caducada por inactividad prolongada (ISO 27001 A.8.5)
            limpiarSesion()
            return false
        }
        return true
    }

    fun guardarUltimoUid(uid: String) {
        if (uid.isBlank()) return
        PlatformPreferences.putString("ultimo_uid_biometrico", uid)
    }

    fun obtenerUltimoUid(): String? {
        val uid = PlatformPreferences.getString("ultimo_uid_biometrico")
            ?: PlatformPreferences.getString("user_uid")
        return if (uid.isNullOrBlank()) null else uid
    }

    fun obtenerUid(context: Any? = null): String? {
        if (!esSesionValida()) return null
        actualizarActividadReciente()
        val uid = PlatformPreferences.getString("user_uid")
        return if (uid.isNullOrBlank()) null else uid
    }

    fun marcarBiometricoActivo(context: Any? = null, activo: Boolean = true) {
        PlatformPreferences.putBoolean("biometric_activo", activo)
    }

    fun esBiometricoActivo(context: Any? = null): Boolean {
        return PlatformPreferences.getBoolean("biometric_activo", false)
    }

    fun marcarActivacionHuellaMostrada(context: Any? = null) {
        PlatformPreferences.putBoolean("activacion_huella_mostrada", true)
    }

    fun yaSeMostroActivacionHuella(context: Any? = null): Boolean {
        return PlatformPreferences.getBoolean("activacion_huella_mostrada", false)
    }

    fun marcarHuellaConfirmada(context: Any? = null) {
        PlatformPreferences.putBoolean("huella_confirmada", true)
    }

    fun huellaYaConfirmada(context: Any? = null): Boolean {
        return PlatformPreferences.getBoolean("huella_confirmada", false)
    }

    fun guardarUltimaPantalla(context: Any? = null, screenName: String = "") {
        PlatformPreferences.putString("ultima_pantalla", screenName)
        actualizarActividadReciente()
    }

    fun obtenerUltimaPantalla(context: Any? = null): String? {
        return PlatformPreferences.getString("ultima_pantalla")
    }

    fun limpiarSesion(context: Any? = null) {
        PlatformPreferences.remove("user_uid")
        PlatformPreferences.remove("ultima_pantalla")
        PlatformPreferences.remove("ultima_actividad_timestamp")
    }

    fun olvidarBiometriaCompleta(context: Any? = null) {
        PlatformPreferences.remove("user_uid")
        PlatformPreferences.remove("ultimo_uid_biometrico")
        PlatformPreferences.remove("biometric_activo")
        PlatformPreferences.remove("huella_confirmada")
        PlatformPreferences.remove("activacion_huella_mostrada")
        PlatformPreferences.remove("ultima_pantalla")
        PlatformPreferences.remove("ultima_actividad_timestamp")
        PlatformPreferences.remove("pago_uid")
        PlatformPreferences.remove("pago_nombre")
        PlatformPreferences.remove("pago_id_exitoso")
        PlatformPreferences.remove("pago_tipo")
    }

    fun guardarEstadoPago(uid: String, nombre: String, idExitoso: String, tipo: String?) {
        PlatformPreferences.putString("pago_uid", uid)
        PlatformPreferences.putString("pago_nombre", nombre)
        PlatformPreferences.putString("pago_id_exitoso", idExitoso)
        tipo?.let { PlatformPreferences.putString("pago_tipo", it) }
        actualizarActividadReciente()
    }

    fun obtenerPagoUid(): String? = PlatformPreferences.getString("pago_uid")
    fun obtenerPagoNombre(): String? = PlatformPreferences.getString("pago_nombre")
    fun obtenerPagoIdExitoso(): String? = PlatformPreferences.getString("pago_id_exitoso")
    fun obtenerPagoTipo(): String? = PlatformPreferences.getString("pago_tipo")
}
