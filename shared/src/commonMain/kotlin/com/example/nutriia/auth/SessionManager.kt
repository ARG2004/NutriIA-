package com.example.nutriia.auth

import com.example.nutriia.platform.PlatformPreferences

object SessionManager {

    fun guardarSesion(uid: String) {
        PlatformPreferences.putString("user_uid", uid)
        PlatformPreferences.putString("ultimo_uid_biometrico", uid)
    }

    fun guardarSesion(context: Any?, uid: String) {
        PlatformPreferences.putString("user_uid", uid)
        PlatformPreferences.putString("ultimo_uid_biometrico", uid)
    }

    fun guardarUltimoUid(uid: String) {
        PlatformPreferences.putString("ultimo_uid_biometrico", uid)
    }

    fun obtenerUltimoUid(): String? {
        return PlatformPreferences.getString("ultimo_uid_biometrico")
            ?: PlatformPreferences.getString("user_uid")
    }

    fun obtenerUid(context: Any? = null): String? {
        return PlatformPreferences.getString("user_uid")
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
    }

    fun obtenerUltimaPantalla(context: Any? = null): String? {
        return PlatformPreferences.getString("ultima_pantalla")
    }

    fun limpiarSesion(context: Any? = null) {
        PlatformPreferences.remove("user_uid")
        PlatformPreferences.remove("ultima_pantalla")
    }

    fun olvidarBiometriaCompleta(context: Any? = null) {
        PlatformPreferences.remove("user_uid")
        PlatformPreferences.remove("ultimo_uid_biometrico")
        PlatformPreferences.remove("biometric_activo")
        PlatformPreferences.remove("huella_confirmada")
        PlatformPreferences.remove("activacion_huella_mostrada")
        PlatformPreferences.remove("ultima_pantalla")
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
    }

    fun obtenerPagoUid(): String? = PlatformPreferences.getString("pago_uid")
    fun obtenerPagoNombre(): String? = PlatformPreferences.getString("pago_nombre")
    fun obtenerPagoIdExitoso(): String? = PlatformPreferences.getString("pago_id_exitoso")
    fun obtenerPagoTipo(): String? = PlatformPreferences.getString("pago_tipo")
}
