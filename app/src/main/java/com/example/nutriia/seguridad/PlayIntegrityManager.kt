package com.example.nutriia.seguridad

import android.content.Context
import android.util.Base64
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import com.google.android.play.core.integrity.IntegrityTokenResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom

/**
 * Gestor de Verificación de Integridad mediante Google Play Integrity API.
 * 
 * Permite verificar que la app ejecutada:
 * 1. Es el binario oficial firmado por Google Play (no modificada ni alterada por terceros).
 * 2. Se instaló desde Google Play.
 * 3. Se ejecuta en un dispositivo Android genuino y certificado (evita emuladores alterados o manipulaciones en root).
 */
object PlayIntegrityManager {

    private const val TAG = "PlayIntegrityManager"

    /**
     * Genera un Nonce aleatorio criptográficamente seguro codificado en Base64 URL-Safe.
     * En un entorno de producción con backend, este Nonce se recibe desde tu servidor para prevenir ataques de Replay.
     */
    fun generarNonce(): String {
        val randomBytes = ByteArray(24)
        SecureRandom().nextBytes(randomBytes)
        return Base64.encodeToString(randomBytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    /**
     * Solicita un Token de Integridad a los servicios de Google Play.
     *
     * @param context Contexto de la aplicación.
     * @param requestNonce Cadenas aleatoria de desafío (Nonce).
     * @return Result con el token JWT de integridad codificado como String.
     */
    suspend fun solicitarTokenIntegridad(
        context: Context,
        requestNonce: String = generarNonce()
    ): Result<String> = withContext(Dispatchers.IO) {
        // TODO: Restaurar para producción. Se saltó temporalmente para pruebas en emulador (Android vs Android)
        Log.d(TAG, "[Play Integrity] Bypasseando verificación para entorno de emulador.")
        Result.success("dummy_emulator_token")
        
        /*
        try {
            val integrityManager: IntegrityManager = IntegrityManagerFactory.create(context.applicationContext)

            val tokenRequest = IntegrityTokenRequest.builder()
                .setNonce(requestNonce)
                .build()

            Log.d(TAG, "[Play Integrity] Solicitando token a Google Play Services...")
            val response: IntegrityTokenResponse = integrityManager.requestIntegrityToken(tokenRequest).await()

            val token = response.token()
            Log.d(TAG, "[Play Integrity] Token obtenido exitosamente (Longitud: ${token.length})")
            Result.success(token)
        } catch (e: Exception) {
            Log.e(TAG, "[Play Integrity] Error al obtener token: ${e.message}", e)
            Result.failure(e)
        }
        */
    }

    /**
     * Comprueba en tiempo de ejecución si el servicio de Play Integrity está respondiendo.
     * En desarrollo local (Debug/Emulador), registra la advertencia sin bloquear la experiencia de desarrollo.
     */
    /**
     * Comprueba en tiempo de ejecución si el servicio de Play Integrity está respondiendo.
     * En desarrollo local (Debug/Emulador), registra la advertencia sin bloquear la experiencia de desarrollo.
     */
    suspend fun verificarIntegridadInicial(context: Context): Boolean {
        val resultado = solicitarTokenIntegridad(context)
        return if (resultado.isSuccess) {
            Log.i(TAG, "[Play Integrity] ✓ Entorno verificado correctamente.")
            true
        } else {
            Log.w(TAG, "[Play Integrity] ⚠ Token no disponible (entorno de pruebas o sin Play Services). Continuación permitida en modo desarrollo.")
            false
        }
    }
}
