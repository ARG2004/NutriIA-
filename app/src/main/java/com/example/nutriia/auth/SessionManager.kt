package com.example.nutriia.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SessionManager {

    private const val PREF_NAME = "secure_user_prefs"
    private const val KEY_UID = "user_uid"
    private const val KEY_BIOMETRIC_ACTIVO = "biometric_activo"
    private const val KEY_ACTIVACION_HUELLA_MOSTRADA = "activacion_huella_mostrada"
    private const val KEY_HUELLA_CONFIRMADA = "huella_confirmada"
    private const val KEY_ULTIMA_PANTALLA = "ultima_pantalla"

    private fun getSharedPrefs(context: Context): SharedPreferences {
        return try {
            EncryptedSharedPreferences.create(
                context,
                PREF_NAME,
                MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            try {
                // Eliminar archivo de preferencias corrompido
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences(PREF_NAME)
                } else {
                    val sharedPrefsFile = java.io.File(context.filesDir.parent, "shared_prefs/$PREF_NAME.xml")
                    if (sharedPrefsFile.exists()) {
                        sharedPrefsFile.delete()
                    }
                }
                // Borrar llave maestra del Keystore
                val keyStore = java.security.KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
                keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (clearEx: Exception) {
                // Ignorar fallas al limpiar
            }
            
            try {
                EncryptedSharedPreferences.create(
                    context,
                    PREF_NAME,
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (lastEx: Exception) {
                context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            }
        }
    }

    private const val KEY_ULTIMO_UID_BIOMETRICO = "ultimo_uid_biometrico"

    fun guardarSesion(context: Context, uid: String) {
        if (uid.isBlank()) return
        getSharedPrefs(context).edit()
            .putString(KEY_UID, uid)
            .putString(KEY_ULTIMO_UID_BIOMETRICO, uid)
            .apply()
    }

    fun guardarUltimoUid(context: Context, uid: String) {
        if (uid.isBlank()) return
        getSharedPrefs(context).edit().putString(KEY_ULTIMO_UID_BIOMETRICO, uid).apply()
    }

    fun obtenerUltimoUid(context: Context): String? {
        val uid = getSharedPrefs(context).getString(KEY_ULTIMO_UID_BIOMETRICO, null)
            ?: getSharedPrefs(context).getString(KEY_UID, null)
        return if (uid.isNullOrBlank()) null else uid
    }

    fun obtenerUid(context: Context): String? {
        return getSharedPrefs(context).getString(KEY_UID, null)
    }

    fun marcarBiometricoActivo(context: Context, activo: Boolean) {
        getSharedPrefs(context).edit().putBoolean(KEY_BIOMETRIC_ACTIVO, activo).apply()
    }

    fun esBiometricoActivo(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_BIOMETRIC_ACTIVO, false)
    }

    fun marcarActivacionHuellaMostrada(context: Context) {
        getSharedPrefs(context).edit().putBoolean(KEY_ACTIVACION_HUELLA_MOSTRADA, true).apply()
    }

    fun yaSeMostroActivacionHuella(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_ACTIVACION_HUELLA_MOSTRADA, false)
    }

    fun marcarHuellaConfirmada(context: Context) {
        getSharedPrefs(context).edit().putBoolean(KEY_HUELLA_CONFIRMADA, true).apply()
    }

    fun huellaYaConfirmada(context: Context): Boolean {
        return getSharedPrefs(context).getBoolean(KEY_HUELLA_CONFIRMADA, false)
    }

    fun guardarUltimaPantalla(context: Context, screenName: String) {
        getSharedPrefs(context).edit().putString(KEY_ULTIMA_PANTALLA, screenName).apply()
    }

    fun obtenerUltimaPantalla(context: Context): String? {
        return getSharedPrefs(context).getString(KEY_ULTIMA_PANTALLA, null)
    }

    fun limpiarSesion(context: Context) {
        getSharedPrefs(context).edit()
            .remove(KEY_UID)
            .remove(KEY_ULTIMA_PANTALLA)
            .apply()
    }

    fun olvidarBiometriaCompleta(context: Context) {
        getSharedPrefs(context).edit()
            .remove(KEY_UID)
            .remove(KEY_ULTIMO_UID_BIOMETRICO)
            .remove(KEY_BIOMETRIC_ACTIVO)
            .remove(KEY_ACTIVACION_HUELLA_MOSTRADA)
            .remove(KEY_HUELLA_CONFIRMADA)
            .remove(KEY_ULTIMA_PANTALLA)
            .apply()
    }
}
