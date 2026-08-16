package com.example.nutriia.auth

import com.example.nutriia.platform.currentTimeMillis
import kotlinx.coroutines.delay

data class ResultadoCedula(
    val valida: Boolean,
    val cedula: String = "",
    val nombreTitular: String = "",
    val genero: String = "",
    val institucion: String = "",
    val profesion: String = "",
    val entidad: String = "",
    val anoRegistro: String = "",
    val mensaje: String = ""
)

expect suspend fun verificarEnPortalSEP(cedula: String): ResultadoCedula

object CedulaVerifier {
    private var ultimoIntentoMs: Long = 0L
    private const val RATE_LIMIT_MS: Long = 3000L

    suspend fun verificarCedulaConRateLimit(cedula: String): ResultadoCedula {
        val ahora = currentTimeMillis()
        val diferencia = ahora - ultimoIntentoMs
        if (diferencia in 1..<RATE_LIMIT_MS) {
            delay(RATE_LIMIT_MS - diferencia)
        }
        ultimoIntentoMs = currentTimeMillis()
        return verificarCedula(cedula)
    }

    suspend fun verificarCedula(cedula: String): ResultadoCedula {
        val cedulaLimpia = cedula.filter(Char::isDigit).trim()
        if (cedulaLimpia.length < 6) {
            return ResultadoCedula(
                valida = false,
                mensaje = "La cédula debe contener al menos 6 dígitos numéricos"
            )
        }
        return verificarEnPortalSEP(cedulaLimpia)
    }
}
