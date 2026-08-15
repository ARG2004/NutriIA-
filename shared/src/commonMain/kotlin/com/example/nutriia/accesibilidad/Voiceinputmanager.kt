package com.example.nutriia.accesibilidad

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

// ─── Estados ──────────────────────────────────────────────────────────────────
enum class VoiceInputState { IDLE, LISTENING, PROCESSING, ERROR }

// Códigos de error que son recuperables (se puede reintentar sin intervención del usuario)
private val ERRORES_RECUPERABLES = setOf(
    SpeechRecognizer.ERROR_NO_MATCH,
    SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
    SpeechRecognizer.ERROR_RECOGNIZER_BUSY
)

// ─── Manager de voz ───────────────────────────────────────────────────────────
class VoiceInputManager(private val context: Context) {

    val estado:       MutableState<VoiceInputState> = mutableStateOf(VoiceInputState.IDLE)
    val errorMsg:     MutableState<String>          = mutableStateOf("")
    // Expone el código de error numérico para que el llamador decida si reintentar
    val errorCodigo:  MutableState<Int>             = mutableIntStateOf(-1)

    private var recognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isDisponible(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    // ── Escucha con idioma configurable ───────────────────────────────────────
    fun escuchar(
        idioma:          IdiomaVoz = IdiomaVoz.ESPANOL_MX,
        modoAccesible:   Boolean   = false,   // true = tiempos extendidos para discapacidad visual
        onResult:        (String, Boolean) -> Unit // true si es el resultado final
    ) {
        if (!isDisponible()) {
            estado.value      = VoiceInputState.ERROR
            errorMsg.value    = "Tu dispositivo no soporta reconocimiento de voz"
            errorCodigo.value = -1
            return
        }

        // FIX: destruir en el hilo principal y esperar 150 ms antes de crear uno nuevo
        // Esto evita ERROR_RECOGNIZER_BUSY cuando la instancia anterior no liberó el hardware
        destroyAndRecreate(idioma, modoAccesible, onResult)
    }

    private fun destroyAndRecreate(
        idioma:        IdiomaVoz,
        modoAccesible: Boolean,
        onResult:      (String, Boolean) -> Unit
    ) {
        mainHandler.post {
            try { recognizer?.destroy() } catch (_: Exception) {}
            recognizer = null
            errorMsg.value    = ""
            errorCodigo.value = -1

            // Delay de 250 ms para garantizar que el audio hardware y el binder quedan libres
            mainHandler.postDelayed({
                crearYEscuchar(idioma, modoAccesible, onResult)
            }, 250L)
        }
    }

    private fun crearYEscuchar(
        idioma:        IdiomaVoz,
        modoAccesible: Boolean,
        onResult:      (String, Boolean) -> Unit,
        preferOffline: Boolean = true
    ) {
        recognizer = SpeechRecognizer.createSpeechRecognizer(context)
 
        // Tiempos de silencio: normales vs accesibles (el doble para personas ciegas)
        val silencioCompleto    = if (modoAccesible) 4000L else 2000L
        val silencioParcial     = if (modoAccesible) 3000L else 1500L
 
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE,             idioma.localeVoz)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,  idioma.localeVoz)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS,          3)   // TOP-3: más chances de capturar
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS,      true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,          silencioCompleto)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, silencioParcial)
            if (preferOffline) {
                putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            }
        }
 
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(p: Bundle?)    { estado.value = VoiceInputState.LISTENING }
            override fun onBeginningOfSpeech()           { estado.value = VoiceInputState.LISTENING }
            override fun onRmsChanged(rms: Float)        {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech()                 { estado.value = VoiceInputState.PROCESSING }
            override fun onEvent(t: Int, p: Bundle?)     {}
 
            override fun onPartialResults(bundle: Bundle?) {
                val parcial = bundle
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { it.isNotBlank() } ?: return
                onResult(parcial, false)
            }
 
            override fun onResults(bundle: Bundle?) {
                estado.value = VoiceInputState.IDLE
                // Toma el primer resultado no vacío del top-3
                val texto = bundle
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull { it.isNotBlank() } ?: return
                onResult(texto, true)
            }
 
            override fun onError(code: Int) {
                // Si falla el reconocimiento offline por falta de paquetes (error 12) o problemas de cliente (5), reintentamos online
                if (preferOffline && (code == 12 || code == SpeechRecognizer.ERROR_SERVER || code == SpeechRecognizer.ERROR_CLIENT)) {
                    android.util.Log.i("VoiceInput", "Fallo offline (error $code). Reintentando online...")
                    mainHandler.post {
                        try { recognizer?.destroy() } catch (_: Exception) {}
                        recognizer = null
                        mainHandler.postDelayed({
                            crearYEscuchar(idioma, modoAccesible, onResult, preferOffline = false)
                        }, 300L)
                    }
                    return
                }

                estado.value      = VoiceInputState.IDLE
                errorCodigo.value = code
                errorMsg.value    = traducirError(code)
                android.util.Log.w("VoiceInput", "onError code=$code: ${traducirError(code)}")
            }
        })
 
        recognizer?.startListening(intent)
    }

    fun detener() {
        mainHandler.post {
            try { recognizer?.stopListening() } catch (_: Exception) {}
        }
        estado.value = VoiceInputState.IDLE
    }

    fun liberar() {
        mainHandler.post {
            try { recognizer?.destroy() } catch (_: Exception) {}
            recognizer = null
        }
    }

    fun esErrorRecuperable(): Boolean = errorCodigo.value in ERRORES_RECUPERABLES

    private fun traducirError(code: Int) = when (code) {
        SpeechRecognizer.ERROR_AUDIO                   -> "Error de micrófono"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permiso de micrófono requerido"
        SpeechRecognizer.ERROR_NETWORK,
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT         -> "Sin conexión a internet"
        SpeechRecognizer.ERROR_NO_MATCH                -> "No entendí. Habla de nuevo"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT          -> "No detecté voz. Habla más cerca"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY         -> "Micrófono ocupado, reintentando"
        else                                           -> "Error desconocido ($code)"
    }
}

// ─── Parser de fecha desde voz ────────────────────────────────────────────────
// Convierte texto hablado a formato DD/MM/AAAA
object FechaVozParser {

    private val MESES_ES = mapOf(
        "enero" to "01", "febrero" to "02", "marzo" to "03",
        "abril" to "04", "mayo" to "05", "junio" to "06",
        "julio" to "07", "agosto" to "08", "septiembre" to "09",
        "octubre" to "10", "noviembre" to "11", "diciembre" to "12",
        // Abreviaciones
        "ene" to "01", "feb" to "02", "mar" to "03",
        "abr" to "04", "jun" to "06", "jul" to "07",
        "ago" to "08", "sep" to "09", "oct" to "10",
        "nov" to "11", "dic" to "12"
    )

    private val NUMS_ES = mapOf(
        "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
        "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15,
        "dieciséis" to 16, "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19,
        "veinte" to 20, "veintiuno" to 21, "veintidós" to 22, "veintitrés" to 23,
        "veinticuatro" to 24, "veinticinco" to 25, "veintiséis" to 26,
        "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
        "treinta" to 30, "treinta y uno" to 31,
        // Años comunes
        "dos mil" to 2000, "dos mil uno" to 2001, "dos mil dos" to 2002,
        "dos mil tres" to 2003, "dos mil cuatro" to 2004, "dos mil cinco" to 2005,
        "dos mil seis" to 2006, "dos mil siete" to 2007, "dos mil ocho" to 2008,
        "dos mil nueve" to 2009, "dos mil diez" to 2010, "dos mil once" to 2011,
        "dos mil doce" to 2012, "dos mil trece" to 2013, "dos mil catorce" to 2014,
        "dos mil quince" to 2015, "dos mil dieciséis" to 2016, "dos mil diecisiete" to 2017,
        "dos mil dieciocho" to 2018, "dos mil diecinueve" to 2019,
        "dos mil veinte" to 2020, "dos mil veintiuno" to 2021, "dos mil veintidós" to 2022,
        "dos mil veintitrés" to 2023, "dos mil veinticuatro" to 2024,
        "dos mil veinticinco" to 2025, "dos mil veintiséis" to 2026
    )

    // ── Intenta parsear texto hablado a DD/MM/AAAA ────────────────────────────
    fun parsear(texto: String): String? {
        val t = texto.lowercase().trim()

        // 1. Solo dígitos: "15 3 2023" → "15/03/2023"
        val soloDigitos = Regex("""(\d{1,2})\s+(\d{1,2})\s+(\d{4})""")
        soloDigitos.find(t)?.let { m ->
            val (d, mo, a) = m.destructured
            return formatear(d.toIntOrNull(), mo.toIntOrNull(), a.toIntOrNull())
        }

        // 2. Formato con "de": "15 de marzo de 2023"
        val conDe = Regex("""(\w+)\s+de\s+(\w+)\s+de\s+(.+)""")
        conDe.find(t)?.let { m ->
            val (diaStr, mesStr, anioStr) = m.destructured
            val dia  = parsearNumero(diaStr)
            val mes  = parsearMes(mesStr)
            val anio = parsearAnio(anioStr)
            if (dia != null && mes != null && anio != null)
                return formatear(dia, mes, anio)
        }

        // 3. Solo palabras: "quince marzo dos mil veintitrés"
        val palabras = t.split(" ")
        if (palabras.size >= 3) {
            val dia  = parsearNumero(palabras[0])
            val mes  = parsearMes(palabras[1])
            val anio = parsearAnio(palabras.drop(2).joinToString(" "))
            if (dia != null && mes != null && anio != null)
                return formatear(dia, mes, anio)
        }

        // 4. Dígitos continuos: "15032023" → "15/03/2023"
        val continuo = Regex("""(\d{1,2})(\d{2})(\d{4})""")
        continuo.find(t.filter { it.isDigit() })?.let { m ->
            val (d, mo, a) = m.destructured
            return formatear(d.toIntOrNull(), mo.toIntOrNull(), a.toIntOrNull())
        }

        return null // No se pudo parsear
    }

    private fun formatear(dia: Int?, mes: Int?, anio: Int?): String? {
        if (dia == null || mes == null || anio == null) return null
        if (dia !in 1..31 || mes !in 1..12 || anio !in 1900..2100) return null
        return "${dia.toString().padStart(2,'0')}/${mes.toString().padStart(2,'0')}/$anio"
    }

    private fun parsearNumero(s: String): Int? =
        s.toIntOrNull() ?: NUMS_ES[s.lowercase().trim()]

    private fun parsearMes(s: String): Int? {
        val lower = s.lowercase().trim()
        return lower.toIntOrNull() ?: MESES_ES[lower]?.toIntOrNull()
    }

    private fun parsearAnio(s: String): Int? {
        val lower = s.lowercase().trim()
        return lower.toIntOrNull() ?: NUMS_ES[lower]
    }
}