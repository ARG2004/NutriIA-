package com.example.nutriia.accesibilidad

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker.HandLandmarkerOptions
import kotlinx.coroutines.delay
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

private const val MODEL_URL = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
private const val MODEL_FILENAME = "hand_landmarker.task"

/**
 * Vista de Cámara para Señas LSM con escritura por retención sostenida (350ms).
 * Permite escribir texto en el campo del formulario sin cerrarse nunca automáticamente ante pausas o errores.
 * El campo solo se confirma y avanza cuando el usuario presiona explícitamente el botón "Confirmar y Continuar".
 */
@Composable
fun SignLanguageCameraView(
    textoActual:   String,
    onTextoChange: (String) -> Unit,
    colorPrimario: Color    = Color(0xFF4CAF50),
    soloNumeros:   Boolean  = false,
    esCampoFecha:  Boolean  = false,
    onCompletado:  (() -> Unit)? = null,
    modifier:      Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val haptic = LocalHapticFeedback.current

    var modeloDescargado by remember { mutableStateOf(false) }
    var progresoDescarga by remember { mutableFloatStateOf(0f) }
    var mensajeDescarga by remember { mutableStateOf("Verificando componentes...") }
    
    var camaraPermisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    var letraDetectada by remember { mutableStateOf<String?>(null) }
    var confianzaDetectada by remember { mutableFloatStateOf(0f) }
    var progresoConfirmacion by remember { mutableFloatStateOf(0f) }
    var ultimaLetraConfirmada by remember { mutableStateOf("") }
    var ultimoTiempoEscritura by remember { mutableLongStateOf(0L) }
    var sinManoInicio by remember { mutableLongStateOf(0L) }
    var espacioInsertado by remember { mutableStateOf(false) }
    var classificationBuffer by remember { mutableStateOf(listOf<String>()) }

    var imageWidth by remember { mutableIntStateOf(1) }
    var imageHeight by remember { mutableIntStateOf(1) }

    var landmarksDibujo by remember { mutableStateOf<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>(emptyList()) }
    var landmarksHistory by remember { mutableStateOf<List<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>>(emptyList()) }
    var indexTrail by remember { mutableStateOf<List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>>(emptyList()) }

    var landmarker by remember { mutableStateOf<HandLandmarker?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val launcherPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido -> camaraPermisoConcedido = concedido }

    // ── 1. Descarga del Modelo HandLandmarker en almacenamiento privado ──────
    LaunchedEffect(Unit) {
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        if (modelFile.exists() && modelFile.length() > 0) {
            modeloDescargado = true
        } else {
            mensajeDescarga = "Descargando reconocedor de señas LSM..."
            val client = OkHttpClient()
            val request = Request.Builder().url(MODEL_URL).build()
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("SignLangCamera", "Error descargando modelo", e)
                    mensajeDescarga = "Error al descargar reconocedor"
                }
                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.byteStream()?.use { input ->
                            modelFile.outputStream().use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytes = 0L
                                val fileSize = response.body?.contentLength() ?: -1L
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytes += bytesRead
                                    if (fileSize > 0) {
                                        progresoDescarga = totalBytes.toFloat() / fileSize.toFloat()
                                    }
                                }
                            }
                        }
                        modeloDescargado = true
                    } else {
                        mensajeDescarga = "Error de conexión al descargar"
                    }
                }
            })
        }
    }

    // ── 2. Inicialización de MediaPipe HandLandmarker ──────
    LaunchedEffect(modeloDescargado, camaraPermisoConcedido) {
        if (!modeloDescargado || !camaraPermisoConcedido) return@LaunchedEffect
        val modelFile = File(context.filesDir, MODEL_FILENAME)
        if (!modelFile.exists()) return@LaunchedEffect

        try {
            val mappedByteBuffer = java.io.FileInputStream(modelFile).use { fis ->
                fis.channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, fis.channel.size())
            }
            val baseOptions = BaseOptions.builder()
                .setModelAssetBuffer(mappedByteBuffer)
                .build()

            val options = HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setNumHands(1)
                .setMinHandDetectionConfidence(0.35f)
                .setMinHandPresenceConfidence(0.35f)
                .setMinTrackingConfidence(0.35f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setResultListener { result, _ ->
                    if (result.landmarks().isNotEmpty()) {
                        sinManoInicio = 0L
                        espacioInsertado = false
                        val rawHand = result.landmarks()[0]
                        val worldHand = if (result.worldLandmarks().isNotEmpty()) result.worldLandmarks()[0] else null
                        val smoothedHand = aplicarEMA(rawHand, landmarksDibujo, 0.45f)
                        landmarksHistory = (landmarksHistory + listOf(smoothedHand)).takeLast(35)
                        landmarksDibujo = smoothedHand

                        if (smoothedHand.size >= 21) {
                            indexTrail = (indexTrail + smoothedHand[8]).takeLast(35)
                        }
                        val res = SignLanguageClassifier.clasificarConConfianza(
                            landmarks2D = smoothedHand,
                            landmarks3D = worldHand,
                            soloNumeros = soloNumeros,
                            esCampoFecha = esCampoFecha,
                            historialPuntos = landmarksHistory,
                            debug = true,
                            context = context
                        )
                        val rawLetra = if (res != null && res.confianza >= 0.65f) res.letra else ""
                        val esLetraDinamica = rawLetra in setOf("j", "ll", "rr", "ñ", "x", "q", "z")

                        if (esLetraDinamica) {
                            classificationBuffer = emptyList()
                            letraDetectada = rawLetra
                            confianzaDetectada = res?.confianza ?: 0.65f
                        } else {
                            classificationBuffer = (classificationBuffer + rawLetra).takeLast(4)

                            val counts = classificationBuffer.groupingBy { it }.eachCount()
                            val dominant = counts.maxByOrNull { it.value }

                            if (dominant != null && dominant.value >= 3 && dominant.key.isNotEmpty()) {
                                letraDetectada = dominant.key
                                confianzaDetectada = res?.confianza ?: 0.65f
                            } else {
                                letraDetectada = null
                                confianzaDetectada = 0f
                            }
                        }
                    } else {
                        classificationBuffer = emptyList()
                        letraDetectada = null
                        confianzaDetectada = 0f
                        landmarksDibujo = emptyList()
                        landmarksHistory = emptyList()
                        indexTrail = emptyList()

                        // Auto-espacio a los 3.0s de retirar la mano (sin cerrar jamás el campo)
                        val ahora = SystemClock.uptimeMillis()
                        if (sinManoInicio == 0L) {
                            sinManoInicio = ahora
                        } else if (!espacioInsertado && (ahora - sinManoInicio) > 3000L) {
                            if (textoActual.isNotEmpty() && !textoActual.endsWith(" ")) {
                                vibrateTap(haptic)
                                onTextoChange(textoActual + " ")
                                espacioInsertado = true
                            }
                        }
                    }
                }
                .setErrorListener { e ->
                    Log.e("SignLangCamera", "Error en detección LIVE_STREAM", e)
                }
                .build()
            landmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e("SignLangCamera", "Error iniciando MediaPipe Landmarker", e)
        }
    }

    // ── 3. Liberación de Recursos Anti-crashes ──────
    DisposableEffect(Unit) {
        onDispose {
            try {
                val cameraProvider = ProcessCameraProvider.getInstance(context).get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                Log.e("SignLangCamera", "Error desvinculando CameraX en dispose", e)
            }
            try {
                landmarker?.close()
            } catch (e: Exception) {
                Log.e("SignLangCamera", "Error liberando landmarker", e)
            }
            try {
                if (!cameraExecutor.isShutdown) {
                    cameraExecutor.shutdown()
                }
            } catch (e: Exception) {
                Log.e("SignLangCamera", "Error cerrando cameraExecutor", e)
            }
        }
    }

    // ── 4. Escritura Automática al Sostener la Seña (350ms) ──────
    LaunchedEffect(letraDetectada) {
        if (letraDetectada == null) {
            progresoConfirmacion = 0f
            return@LaunchedEffect
        }

        val ahora = SystemClock.uptimeMillis()
        if (letraDetectada == ultimaLetraConfirmada && (ahora - ultimoTiempoEscritura) < 400L) {
            progresoConfirmacion = 0f
            return@LaunchedEffect
        }

        val letraOriginal = letraDetectada
        val duracionMs = 500L
        val intervaloMs = 20L
        val pasos = (duracionMs / intervaloMs).toInt()
        var errores = 0

        for (i in 1..pasos) {
            delay(intervaloMs)
            if (letraDetectada != letraOriginal) {
                errores++
                if (errores > 10) {
                    progresoConfirmacion = 0f
                    return@LaunchedEffect
                }
            }
            progresoConfirmacion = i.toFloat() / pasos
        }

        letraOriginal?.let {
            vibrateSuccess(haptic)
            onTextoChange(textoActual + it)
            ultimaLetraConfirmada = it
            ultimoTiempoEscritura = SystemClock.uptimeMillis()
            progresoConfirmacion = 0f
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1E2F))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display del Texto Escrito en el Formulario
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF151525))
                .padding(12.dp)
                .semantics { contentDescription = "Texto escrito: ${textoActual.ifEmpty { "vacío" }}" }
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = textoActual.ifEmpty { "Haz señas para escribir..." },
                    color = if (textoActual.isEmpty()) Color.Gray else Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (textoActual.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            vibrateTap(haptic)
                            onTextoChange(textoActual.dropLast(1))
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = Color.LightGray)
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Contenedor de la Cámara con Overlay del Esqueleto
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF10101C)),
            contentAlignment = Alignment.Center
        ) {
            when {
                !modeloDescargado -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            progress = { progresoDescarga },
                            color = colorPrimario,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(mensajeDescarga, color = Color.LightGray, fontSize = 12.sp)
                    }
                }
                !camaraPermisoConcedido -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Icon(Icons.Rounded.Videocam, null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Permiso de cámara requerido", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Necesitamos la cámara para ver tus señas.", color = Color.Gray, fontSize = 11.sp)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { launcherPermiso.launch(Manifest.permission.CAMERA) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorPrimario)
                        ) {
                            Text("Otorgar permiso", fontSize = 11.sp)
                        }
                    }
                }
                else -> {
                    AndroidView(
                        factory = { ctx ->
                            val previewView = PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FIT_CENTER
                            }
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                            try {
                                                val currentLandmarker = landmarker
                                                if (currentLandmarker != null && !cameraExecutor.isShutdown) {
                                                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                                                    val bitmap = imageProxy.toBitmap()
                                                    // Paso 1: Rotar alrededor del CENTRO del bitmap (no del origen)
                                                    val rotMatrix = Matrix().apply {
                                                        if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat(), bitmap.width / 2f, bitmap.height / 2f)
                                                    }
                                                    val rotBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, rotMatrix, true)
                                                    // Paso 2: Espejar horizontalmente con el centro del bitmap YA ROTADO
                                                    val mirrorMatrix = Matrix().apply {
                                                        postScale(-1f, 1f, rotBitmap.width / 2f, rotBitmap.height / 2f)
                                                    }
                                                    val rotatedBitmap = Bitmap.createBitmap(rotBitmap, 0, 0, rotBitmap.width, rotBitmap.height, mirrorMatrix, true)
                                                    imageWidth = rotatedBitmap.width
                                                    imageHeight = rotatedBitmap.height
                                                    val mpImage = BitmapImageBuilder(rotatedBitmap).build()
                                                    currentLandmarker.detectAsync(mpImage, SystemClock.uptimeMillis())
                                                }
                                            } catch (e: Exception) {
                                                Log.e("SignLangCamera", "Error en analisis de imagen", e)
                                            } finally {
                                                try { imageProxy.close() } catch (_: Exception) {}
                                            }
                                        }
                                    }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_FRONT_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch (e: Exception) {
                                    Log.e("SignLangCamera", "Error en CameraX binding", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                            previewView
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Overlay de Landmarks
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val imgW = imageWidth.toFloat()
                        val imgH = imageHeight.toFloat()
                        val canvasRatio = canvasWidth / canvasHeight
                        val imageRatio = imgW / imgH

                        val scaledWidth: Float
                        val scaledHeight: Float
                        val left: Float
                        val top: Float

                        if (imageRatio > canvasRatio) {
                            scaledWidth = canvasWidth
                            scaledHeight = canvasWidth / imageRatio
                            left = 0f
                            top = (canvasHeight - scaledHeight) / 2f
                        } else {
                            scaledHeight = canvasHeight
                            scaledWidth = canvasHeight * imageRatio
                            left = (canvasWidth - scaledWidth) / 2f
                            top = 0f
                        }

                        val points = landmarksDibujo
                        if (points.size >= 21) {
                            val screenPoints = points.map {
                                Offset(
                                    x = left + it.x() * scaledWidth,
                                    y = top + it.y() * scaledHeight
                                )
                            }
                            val conexiones = listOf(
                                Pair(0, 1), Pair(1, 2), Pair(2, 3), Pair(3, 4),
                                Pair(0, 5), Pair(5, 6), Pair(6, 7), Pair(7, 8),
                                Pair(0, 9), Pair(9, 10), Pair(10, 11), Pair(11, 12),
                                Pair(0, 13), Pair(13, 14), Pair(14, 15), Pair(15, 16),
                                Pair(0, 17), Pair(17, 18), Pair(18, 19), Pair(19, 20),
                                Pair(5, 9), Pair(9, 13), Pair(13, 17)
                            )
                            conexiones.forEach { (i1, i2) ->
                                drawLine(
                                    color = colorPrimario.copy(alpha = 0.85f),
                                    start = screenPoints[i1],
                                    end = screenPoints[i2],
                                    strokeWidth = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                            screenPoints.forEach { pt ->
                                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                                drawCircle(color = colorPrimario, radius = 2.dp.toPx(), center = pt)
                            }
                        }
                    }

                    // Banner de Vista Previa con Anillo Radial de Confirmación
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        androidx.compose.animation.AnimatedVisibility(
                            visible = letraDetectada != null,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut()
                        ) {
                            Surface(
                                color = Color.Black.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, colorPrimario)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(36.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { progresoConfirmacion },
                                            color = colorPrimario,
                                            strokeWidth = 3.dp,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text(
                                            text = letraDetectada?.uppercase() ?: "",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Seña ${letraDetectada?.uppercase() ?: ""} (${(confianzaDetectada * 100).toInt()}%)",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Mantén la seña para escribir...",
                                            color = Color.LightGray,
                                            fontSize = 10.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── 5. PANEL DE CONTROL LIMPIO Y BOTÓN DE CONFIRMACIÓN Y AVANCE ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Espacio
            OutlinedButton(
                onClick = {
                    vibrateTap(haptic)
                    if (textoActual.isNotEmpty() && !textoActual.endsWith(" ")) {
                        onTextoChange(textoActual + " ")
                    }
                },
                border = BorderStroke(1.dp, Color.Gray.copy(0.4f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.SpaceBar, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Espacio", color = Color.White, fontSize = 12.sp, maxLines = 1)
            }

            // Botón Borrar Carácter
            OutlinedButton(
                onClick = {
                    vibrateTap(haptic)
                    if (textoActual.isNotEmpty()) {
                        onTextoChange(textoActual.dropLast(1))
                    }
                },
                border = BorderStroke(1.dp, Color.Gray.copy(0.4f)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Borrar", color = Color.White, fontSize = 12.sp, maxLines = 1)
            }
        }

        Spacer(Modifier.height(8.dp))

        // BOTÓN PRINCIPAL EXPLÍCITO DE CONFIRMACIÓN Y AVANCE DE CAMPO
        if (onCompletado != null) {
            Button(
                onClick = {
                    vibrateSuccess(haptic)
                    onCompletado.invoke()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Confirmar y Continuar al Siguiente Campo", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}

// ─── Helper: Filtro EMA (Exponential Moving Average) para suavizar temblor en landmarks ───
private fun aplicarEMA(
    actual: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    previo: List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark>,
    alpha: Float = 0.45f
): List<com.google.mediapipe.tasks.components.containers.NormalizedLandmark> {
    if (previo.isEmpty() || previo.size != actual.size) return actual
    return actual.mapIndexed { i, curr ->
        val prev = previo[i]
        val xSmooth = alpha * curr.x() + (1f - alpha) * prev.x()
        val ySmooth = alpha * curr.y() + (1f - alpha) * prev.y()
        val zSmooth = alpha * curr.z() + (1f - alpha) * prev.z()
        com.google.mediapipe.tasks.components.containers.NormalizedLandmark.create(xSmooth, ySmooth, zSmooth)
    }
}
