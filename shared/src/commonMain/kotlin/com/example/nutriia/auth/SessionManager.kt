package com.example.nutriia.auth

import com.example.nutriia.platform.PlatformPreferences

object SessionManager {

    fun guardarSesion(uid: String) {
        PlatformPreferences.putString("user_uid", uid)
    }

    fun guardarSesion(context: Any?, uid: String) {
        PlatformPreferences.putString("user_uid", uid)
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
}
