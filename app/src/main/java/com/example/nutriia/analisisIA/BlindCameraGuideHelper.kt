package com.example.nutriia.analisisIA

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.nutriia.accesibilidad.MotorHapticoNutriIA
import com.example.nutriia.accesibilidad.NutriEarcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Estados de orientación de la cámara para personas con discapacidad visual.
 */
sealed class BlindGuideState(val mensajeEs: String, val factorAlineacion: Float) {
    object Inicial : BlindGuideState("Apunta tu teléfono hacia la mesa", 0.0f)
    object InclinarAbajo : BlindGuideState("Inclina el teléfono hacia abajo, hacia tu plato", 0.2f)
    object InclinarArriba : BlindGuideState("Levanta un poco el teléfono", 0.2f)
    object PocaLuz : BlindGuideState("Hay muy poca luz. Enciende una lámpara o muévete hacia la luz", 0.1f)
    object MoverIzquierda : BlindGuideState("Mueve el teléfono un poco a la izquierda", 0.5f)
    object MoverDerecha : BlindGuideState("Mueve el teléfono un poco a la derecha", 0.5f)
    object MoverAdelante : BlindGuideState("Mueve el teléfono un poco hacia adelante", 0.6f)
    object MoverAtras : BlindGuideState("Mueve el teléfono un poco hacia ti", 0.6f)
    object Acercar : BlindGuideState("Acércate un poco más al plato", 0.6f)
    object Alejar : BlindGuideState("Aléjate un poco del plato", 0.6f)
    object EncuadreListo : BlindGuideState("¡Plato centrado! Di 'Foto' o toca cualquier parte de la pantalla", 1.0f)
}

/**
 * Asistente de Encuadre Inteligente para el Modo Blind de NutrIA.
 * Diseñado específicamente para máxima ligereza en hardware desde 2018 (Android 8.0+, 2GB RAM).
 */
class BlindCameraGuideHelper(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val onHablar: (String) -> Unit,
    private val onComandoCaptura: () -> Unit
) : SensorEventListener, ImageAnalysis.Analyzer {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val acelerometro = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _estadoGuia = MutableStateFlow<BlindGuideState>(BlindGuideState.Inicial)
    val estadoGuia: StateFlow<BlindGuideState> = _estadoGuia

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningVoice = false
    private var sonarJob: Job? = null
    private var ultimoTimestampAnalisis = 0L
    private var ultimoTimestampMensaje = 0L
    private var ultimoMensajeHablado = ""
    private var anguloInclinacion = 0f // Grados respecto al horizonte
    private var telefonoEstable = false

    private var activo = false

    fun iniciar() {
        if (activo) return
        activo = true

        // 1. Iniciar sensor de acelerómetro a frecuencia baja (SENSOR_DELAY_UI / 15Hz, 0% CPU)
        acelerometro?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // 2. Iniciar reconocimiento de comandos de voz pasivo
        iniciarReconocedorVoz()

        // 3. Iniciar bucle de sonar háptico progresivo
        iniciarSonarHaptico()
    }

    fun detener() {
        activo = false
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {}

        sonarJob?.cancel()
        sonarJob = null

        detenerReconocedorVoz()
    }

    // ─── 1. Sensor de Inclinación (Acelerómetro) ──────────────────────────────
    override fun onSensorChanged(event: SensorEvent?) {
        if (!activo || event == null) return
        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            val ax = event.values[0]
            val ay = event.values[1]
            val az = event.values[2]

            // Calcular aceleración total para verificar estabilidad de la mano
            val norma = sqrt(ax * ax + ay * ay + az * az)
            telefonoEstable = abs(norma - 9.81f) < 2.2f

            // Ángulo de inclinación vertical (pitch en grados)
            // 0° = plano sobre la mesa mirando al techo
            // 90° = vertical frente a los ojos
            // 45°-75° = ángulo natural para enfocar un plato sobre la mesa
            val pitchRad = atan2(ay.toDouble(), sqrt((ax * ax + az * az).toDouble()))
            anguloInclinacion = Math.toDegrees(pitchRad).toFloat()

            // Si el teléfono apunta al frente o al techo, guiar inmediatamente por inclinación (0% CPU de visión)
            if (anguloInclinacion > 78f) {
                actualizarEstado(BlindGuideState.InclinarAbajo)
            } else if (anguloInclinacion < 20f) {
                actualizarEstado(BlindGuideState.InclinarArriba)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ─── 2. Analizador de Visión Ligero (CameraX Buffer YUV - Escala de Grises) ──
    override fun analyze(image: ImageProxy) {
        if (!activo) {
            image.close()
            return
        }

        val ahora = System.currentTimeMillis()
        // Throttling: solo procesar 1 cuadro cada 650 ms (1.5 FPS para gama baja)
        if (ahora - ultimoTimestampAnalisis < 650L) {
            image.close()
            return
        }
        ultimoTimestampAnalisis = ahora

        // Si la inclinación no es adecuada, no gastar procesamiento visual
        if (anguloInclinacion > 78f || anguloInclinacion < 20f) {
            image.close()
            return
        }

        try {
            val bufferY = image.planes[0].buffer
            val width = image.width
            val height = image.height
            val rowStride = image.planes[0].rowStride
            val pixelStride = image.planes[0].pixelStride

            // Submuestreo de 8 píxeles: en una imagen de 320x240 solo evalúa 1200 puntos
            val step = 8
            var sumaLuz = 0L
            var totalMuestras = 0

            var centroMasaX = 0.0
            var centroMasaY = 0.0
            var masaTotal = 0.0

            val limiteX = width - step
            val limiteY = height - step

            for (y in 0 until limiteY step step) {
                val offsetFila = y * rowStride
                for (x in 0 until limiteX step step) {
                    val indice = offsetFila + x * pixelStride
                    if (indice < bufferY.limit()) {
                        val luminancia = bufferY.get(indice).toInt() and 0xFF
                        sumaLuz += luminancia
                        totalMuestras++

                        // Calcular contraste respecto a luminancia media estándar
                        val pesoContraste = abs(luminancia - 128)
                        if (pesoContraste > 30) {
                            centroMasaX += x * pesoContraste
                            centroMasaY += y * pesoContraste
                            masaTotal += pesoContraste
                        }
                    }
                }
            }

            if (totalMuestras > 0) {
                val luzMedia = (sumaLuz / totalMuestras).toInt()

                if (luzMedia < 30) {
                    actualizarEstado(BlindGuideState.PocaLuz)
                } else if (masaTotal > 0) {
                    val cX = (centroMasaX / masaTotal) / width // 0.0 a 1.0 (0.5 = centro)
                    val cY = (centroMasaY / masaTotal) / height // 0.0 a 1.0 (0.5 = centro)
                    val proporcionMasa = masaTotal / (totalMuestras * 128.0)

                    // Evaluar centrado
                    val margenCentro = 0.18 // Margen de tolerancia del 18%
                    val desviacionX = cX - 0.5
                    val desviacionY = cY - 0.5

                    when {
                        proporcionMasa < 0.12 -> actualizarEstado(BlindGuideState.Acercar)
                        proporcionMasa > 0.85 -> actualizarEstado(BlindGuideState.Alejar)
                        desviacionX < -margenCentro -> actualizarEstado(BlindGuideState.MoverIzquierda)
                        desviacionX > margenCentro -> actualizarEstado(BlindGuideState.MoverDerecha)
                        desviacionY < -margenCentro -> actualizarEstado(BlindGuideState.MoverAdelante)
                        desviacionY > margenCentro -> actualizarEstado(BlindGuideState.MoverAtras)
                        else -> actualizarEstado(BlindGuideState.EncuadreListo)
                    }
                } else {
                    actualizarEstado(BlindGuideState.EncuadreListo)
                }
            }
        } catch (_: Throwable) {
            // Protección contra buffers reciclados
        } finally {
            image.close()
        }
    }

    private fun actualizarEstado(nuevoEstado: BlindGuideState) {
        if (_estadoGuia.value != nuevoEstado) {
            _estadoGuia.value = nuevoEstado
        }

        val ahora = System.currentTimeMillis()
        val haCambiado = nuevoEstado.mensajeEs != ultimoMensajeHablado
        val haPasadoTiempo = ahora - ultimoTimestampMensaje > 2800L

        if (haCambiado || haPasadoTiempo) {
            ultimoTimestampMensaje = ahora
            ultimoMensajeHablado = nuevoEstado.mensajeEs
            onHablar(nuevoEstado.mensajeEs)
        }
    }

    // ─── 3. Sonar Háptico Continuo ──────────────────────────────────────────
    private fun iniciarSonarHaptico() {
        sonarJob?.cancel()
        sonarJob = coroutineScope.launch(Dispatchers.Default) {
            while (activo) {
                val factor = _estadoGuia.value.factorAlineacion
                if (factor >= 0.9f) {
                    // Encuadre perfecto: vibración continua firme y rápida
                    MotorHapticoNutriIA.vibrarLlegadaBoton(context)
                    NutriEarcons.playHeartbeat()
                    delay(350L)
                } else if (factor >= 0.4f) {
                    // Acercándose: pulso intermedio
                    MotorHapticoNutriIA.vibrarRadarProximidad(context, factor)
                    NutriEarcons.playButtonHover()
                    delay(700L)
                } else {
                    delay(1000L)
                }
            }
        }
    }

    // ─── 4. Comandos de Voz ("Foto", "Capturar", "Listo") ────────────────────
    private fun iniciarReconocedorVoz() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) return

        coroutineScope.launch(Dispatchers.Main) {
            try {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) { isListeningVoice = true }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() { isListeningVoice = false }
                        override fun onError(error: Int) {
                            isListeningVoice = false
                            // Reintentar escucha pasiva de comando con breve delay
                            if (activo) {
                                coroutineScope.launch(Dispatchers.Main) {
                                    delay(800)
                                    escucharComando()
                                }
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListeningVoice = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val texto = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                            
                            val esComando = texto.contains("foto") ||
                                    texto.contains("tomar") ||
                                    texto.contains("capturar") ||
                                    texto.contains("listo") ||
                                    texto.contains("disparar") ||
                                    texto.contains("escanear") ||
                                    texto.contains("shoot") ||
                                    texto.contains("capture")

                            if (esComando) {
                                MotorHapticoNutriIA.vibrarFuerte(context, 80L, 255)
                                onComandoCaptura()
                            } else if (activo) {
                                escucharComando()
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val texto = matches?.firstOrNull()?.lowercase(Locale.getDefault()) ?: ""
                            if (texto.contains("foto") || texto.contains("capturar") || texto.contains("tomar")) {
                                MotorHapticoNutriIA.vibrarFuerte(context, 80L, 255)
                                onComandoCaptura()
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                escucharComando()
            } catch (_: Exception) {}
        }
    }

    private fun escucharComando() {
        if (!activo || isListeningVoice) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {}
    }

    private fun detenerReconocedorVoz() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        isListeningVoice = false
    }
}
