package com.example.nutriia.auth

object SessionManager {
    private val storage = mutableMapOf<String, Any>()

    fun guardarSesion(uid: String) {
        storage["user_uid"] = uid
    }

    fun guardarSesion(context: Any?, uid: String) {
        storage["user_uid"] = uid
    }

    fun obtenerUid(context: Any? = null): String? {
        return storage["user_uid"] as? String
    }

    fun marcarBiometricoActivo(context: Any? = null, activo: Boolean = true) {
        storage["biometric_activo"] = activo
    }

    fun esBiometricoActivo(context: Any? = null): Boolean {
        return (storage["biometric_activo"] as? Boolean) ?: false
    }

    fun marcarActivacionHuellaMostrada(context: Any? = null) {
        storage["activacion_huella_mostrada"] = true
    }

    fun yaSeMostroActivacionHuella(context: Any? = null): Boolean {
        return (storage["activacion_huella_mostrada"] as? Boolean) ?: false
    }

    fun marcarHuellaConfirmada(context: Any? = null) {
        storage["huella_confirmada"] = true
    }

    fun huellaYaConfirmada(context: Any? = null): Boolean {
        return (storage["huella_confirmada"] as? Boolean) ?: false
    }

    fun guardarUltimaPantalla(context: Any? = null, screenName: String = "") {
        storage["ultima_pantalla"] = screenName
    }

    fun obtenerUltimaPantalla(context: Any? = null): String? {
        return storage["ultima_pantalla"] as? String
    }

    fun limpiarSesion(context: Any? = null) {
        storage.clear()
    }
}
