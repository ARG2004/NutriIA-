package com.example.nutriia.accesibilidad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriia.platform.currentTimeMillis
import kotlinx.coroutines.delay

enum class CategoriaSena {
    ALIMENTACION, SALUD_ALERTA, CLINICA, CONTROL
}

data class SenaLSMInfo(
    val id: String,
    val nombre: String,
    val emoji: String,
    val descripcionLSM: String,
    val sugerenciaFrase: String,
    val categoria: CategoriaSena
)

data class ResultadoSenaComunicativa(
    val sena: SenaLSMInfo,
    val confianza: Float
)

val CATALOGO_SENAS_COMUNICATIVAS = listOf(
    SenaLSMInfo("LECHE", "Leche / Lactancia", "", "Mano en 'S'/'C' con movimiento rítmico de ordeño frente al pecho.", "Registrar toma de leche materna", CategoriaSena.ALIMENTACION),
    SenaLSMInfo("COMIDA", "Comida / Papilla", "", "Mano en 'O' aplanada (yemas unidas) llevada repetidamente hacia la boca.", "Registrar comida de sólidos", CategoriaSena.ALIMENTACION),
    SenaLSMInfo("AGUA", "Agua", "", "Letra 'W' tocando suavemente la barbilla dos veces con el costado del índice.", "Registrar toma de agua", CategoriaSena.ALIMENTACION),
    SenaLSMInfo("BEBE", "Bebé / Hijo", "", "Brazos cruzados al pecho con balanceo de vaivén meciendo al bebé.", "Seleccionar perfil de mi bebé", CategoriaSena.CLINICA),
    SenaLSMInfo("FIEBRE", "Fiebre / Temperatura", "", "Letra 'F' (variante formal) o palma/dorso colocados sobre la frente.", "Alerta: Mi bebé tiene fiebre", CategoriaSena.SALUD_ALERTA),
    SenaLSMInfo("DOCTOR", "Doctor / Pediatra", "", "Letra 'D' o 'M' tocando el pulso radial en la muñeca opuesta.", "Contactar pediatra de guardia", CategoriaSena.CLINICA),
    SenaLSMInfo("MEDICINA", "Medicina / Tratamiento", "", "Dedo medio frotando en círculo el centro de la palma contraria.", "Registrar dosis de medicamento", CategoriaSena.SALUD_ALERTA),
    SenaLSMInfo("PESO", "Peso / Medición", "", "Ambas manos palmas arriba alternando movimiento vertical de balanza.", "Registrar nuevo peso y talla", CategoriaSena.CLINICA),
    SenaLSMInfo("SI_CONFIRMAR", "Sí / Guardar", "", "Puño cerrado con pulgar hacia arriba en movimiento afirmativo.", "Confirmar y Guardar", CategoriaSena.CONTROL),
    SenaLSMInfo("NO_CANCELAR", "No / Cancelar", "", "Dedo índice oscilando lateralmente o pulgar hacia abajo.", "Cancelar / Borrar", CategoriaSena.CONTROL),
    SenaLSMInfo("AYUDA", "Ayuda / Tutorial", "", "Pulgar arriba apoyado sobre palma opuesta elevándose hacia adelante.", "Abrir centro de ayuda", CategoriaSena.CONTROL),
    SenaLSMInfo("GRACIAS", "Gracias", "", "Dedo medio tocando barbilla y proyectándose hacia el frente.", "Muchas gracias", CategoriaSena.CONTROL)
)

fun obtenerIconoSena(senaId: String): ImageVector = when (senaId.trim().uppercase()) {
    "LECHE" -> Icons.Rounded.WaterDrop
    "COMIDA" -> Icons.Rounded.Restaurant
    "AGUA" -> Icons.Rounded.Opacity
    "BEBE" -> Icons.Rounded.ChildCare
    "FIEBRE" -> Icons.Rounded.Thermostat
    "DOCTOR" -> Icons.Rounded.MedicalServices
    "MEDICINA" -> Icons.Rounded.Medication
    "PESO" -> Icons.Rounded.Scale
    "SI_CONFIRMAR" -> Icons.Rounded.CheckCircle
    "NO_CANCELAR" -> Icons.Rounded.Cancel
    "AYUDA" -> Icons.Rounded.Help
    "GRACIAS" -> Icons.Rounded.VolunteerActivism
    else -> Icons.Rounded.FrontHand
}

enum class ModoSeñaLSM {
    ABECEDARIO, COMUNICATIVO
}

@Composable
fun SignLanguageCameraView(
    textoActual:   String,
    onTextoChange: (String) -> Unit,
    colorPrimario: Color    = Color(0xFF4CAF50),
    soloNumeros:   Boolean  = false,
    esCampoFecha:  Boolean  = false,
    onCompletado:  (() -> Unit)? = null,
    onSenaComunicativaDetectada: ((SenaLSMInfo) -> Unit)? = null,
    modifier:      Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var modoSeleccionado by remember { mutableStateOf(ModoSeñaLSM.ABECEDARIO) }
    var mostrarTecladoManual by remember { mutableStateOf(false) }

    var letraDetectada by remember { mutableStateOf<String?>(null) }
    var senaComunicativaDetectada by remember { mutableStateOf<ResultadoSenaComunicativa?>(null) }
    var confianzaDetectada by remember { mutableFloatStateOf(0f) }
    var progresoConfirmacion by remember { mutableFloatStateOf(0f) }
    var ultimaLetraConfirmada by remember { mutableStateOf("") }
    var ultimoTiempoEscritura by remember { mutableLongStateOf(0L) }
    var sinManoInicio by remember { mutableLongStateOf(0L) }
    var espacioInsertado by remember { mutableStateOf(false) }
    var classificationBuffer by remember { mutableStateOf(listOf<String>()) }

    var landmarksDibujo by remember { mutableStateOf<List<NormalizedPoint3D>>(emptyList()) }
    var landmarksHistory by remember { mutableStateOf<List<List<NormalizedPoint3D>>>(emptyList()) }

    val letras = if (soloNumeros) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    } else {
        ('A'..'Z').map { it.toString() }
    }

    // ── Autoescritura al sostener la seña dactilológica (300ms) ───────────────
    LaunchedEffect(letraDetectada, modoSeleccionado) {
        if (modoSeleccionado != ModoSeñaLSM.ABECEDARIO || letraDetectada == null) {
            progresoConfirmacion = 0f
            return@LaunchedEffect
        }

        val ahora = currentTimeMillis()
        if (letraDetectada == ultimaLetraConfirmada && (ahora - ultimoTiempoEscritura) < 350L) {
            progresoConfirmacion = 0f
            return@LaunchedEffect
        }

        val letraOriginal = letraDetectada
        val duracionMs = 300L
        val intervaloMs = 15L
        val pasos = (duracionMs / intervaloMs).toInt()
        var errores = 0

        for (i in 1..pasos) {
            delay(intervaloMs)
            if (letraDetectada != letraOriginal) {
                errores++
                if (errores > 8) {
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
            ultimoTiempoEscritura = currentTimeMillis()
            progresoConfirmacion = 0f
        }
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val width = maxWidth
        val isSmallScreen = width < 360.dp
        val cameraHeight = when {
            width < 360.dp -> 220.dp
            width >= 600.dp -> 360.dp
            else -> 300.dp
        }
        val cardPadding = if (isSmallScreen) 8.dp else 12.dp

        Column(
            modifier = Modifier
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF1E1E2F))
                .padding(cardPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Selector de Modo: Abecedario vs Señas LSM
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF151525))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) colorPrimario else Color.Transparent)
                        .clickable {
                            vibrateTap(haptic)
                            modoSeleccionado = ModoSeñaLSM.ABECEDARIO
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Abecedario (A-Z)",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (modoSeleccionado == ModoSeñaLSM.COMUNICATIVO) colorPrimario else Color.Transparent)
                        .clickable {
                            vibrateTap(haptic)
                            modoSeleccionado = ModoSeñaLSM.COMUNICATIVO
                        }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.VolunteerActivism,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Señas LSM",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = if (modoSeleccionado == ModoSeñaLSM.COMUNICATIVO) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

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
                        text = textoActual.ifEmpty {
                            if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) "Haz señas frente a la cámara para escribir..."
                            else "Haz una seña comunicativa (ej. Leche, Comida, Agua)..."
                        },
                        color = if (textoActual.isEmpty()) Color.Gray else Color.White,
                        fontSize = 14.sp,
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

            // Contenedor de Cámara en Vivo con Visor Adaptativo y Overlay del Esqueleto 3D
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(cameraHeight)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF10101C)),
                contentAlignment = Alignment.Center
            ) {
            // Vista de Cámara Nativa (Apple Vision / AVFoundation en iOS)
            PlatformSignLanguageCameraPreview(
                modifier = Modifier.fillMaxSize(),
                onLandmarksDetected = { rawPoints ->
                    if (rawPoints.size >= 21) {
                        sinManoInicio = 0L
                        espacioInsertado = false

                        val smoothedHand = aplicarEMA(rawPoints, landmarksDibujo, 0.45f)
                        landmarksHistory = (landmarksHistory + listOf(smoothedHand)).takeLast(35)
                        landmarksDibujo = smoothedHand

                        if (modoSeleccionado == ModoSeñaLSM.COMUNICATIVO) {
                            val resCom = SignLanguageClassifier.clasificarSenaComunicativa(
                                landmarks = smoothedHand,
                                historialPuntos = landmarksHistory
                            )
                            if (resCom != null && resCom.confianza >= 0.70f) {
                                senaComunicativaDetectada = resCom
                                confianzaDetectada = resCom.confianza
                            } else {
                                senaComunicativaDetectada = null
                                confianzaDetectada = 0f
                            }
                        } else {
                            val res = SignLanguageClassifier.clasificarConConfianza(
                                landmarks = smoothedHand,
                                soloNumeros = soloNumeros,
                                esCampoFecha = esCampoFecha,
                                historialPuntos = landmarksHistory
                            )
                            val rawLetra = if (res != null && res.confianza >= 0.55f) res.letra else ""
                            val esLetraDinamica = rawLetra in setOf("j", "ll", "rr", "ñ", "x", "q", "z")

                            if (esLetraDinamica) {
                                classificationBuffer = emptyList()
                                letraDetectada = rawLetra
                                confianzaDetectada = res?.confianza ?: 0.65f
                            } else {
                                classificationBuffer = (classificationBuffer + rawLetra).takeLast(3)
                                val counts = classificationBuffer.groupingBy { it }.eachCount()
                                val dominant = counts.maxByOrNull { it.value }

                                if (dominant != null && dominant.value >= 2 && dominant.key.isNotEmpty()) {
                                    letraDetectada = dominant.key
                                    confianzaDetectada = res?.confianza ?: 0.65f
                                } else {
                                    letraDetectada = null
                                    confianzaDetectada = 0f
                                }
                            }
                        }
                    } else {
                        classificationBuffer = emptyList()
                        letraDetectada = null
                        senaComunicativaDetectada = null
                        confianzaDetectada = 0f
                        landmarksDibujo = emptyList()
                        landmarksHistory = emptyList()

                        val ahora = currentTimeMillis()
                        if (sinManoInicio == 0L) {
                            sinManoInicio = ahora
                        } else if (!espacioInsertado && (ahora - sinManoInicio) > 3000L) {
                            if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO && textoActual.isNotEmpty() && !textoActual.endsWith(" ")) {
                                vibrateTap(haptic)
                                onTextoChange(textoActual + " ")
                                espacioInsertado = true
                            }
                        }
                    }
                }
            )

            // Overlay de Landmarks (Esqueleto Óseo de la Mano)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                val points = landmarksDibujo
                if (points.size >= 21) {
                    val screenPoints = points.map {
                        Offset(
                            x = it.x * canvasWidth,
                            y = it.y * canvasHeight
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
                        if (i1 < screenPoints.size && i2 < screenPoints.size) {
                            drawLine(
                                color = colorPrimario.copy(alpha = 0.85f),
                                start = screenPoints[i1],
                                end = screenPoints[i2],
                                strokeWidth = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    screenPoints.forEach { pt ->
                        drawCircle(color = Color.White, radius = 4.dp.toPx(), center = pt)
                        drawCircle(color = colorPrimario, radius = 2.dp.toPx(), center = pt)
                    }
                }
            }

            // Banner de Vista Previa (Modo Abecedario con Indicador Circular de Tiempo)
            if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
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

            // Banner de Sugerencia Inteligente (Modo Comunicativo LSM)
            if (modoSeleccionado == ModoSeñaLSM.COMUNICATIVO) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = senaComunicativaDetectada != null,
                        enter = fadeIn() + scaleIn(),
                        exit = fadeOut() + scaleOut()
                    ) {
                        senaComunicativaDetectada?.let { resCom ->
                            Surface(
                                color = Color(0xFF151525).copy(alpha = 0.95f),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.5.dp, colorPrimario)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(colorPrimario.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = obtenerIconoSena(resCom.sena.id),
                                                contentDescription = null,
                                                tint = colorPrimario,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = resCom.sena.nombre,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = resCom.sena.descripcionLSM,
                                                color = Color.LightGray,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                        Surface(
                                            color = colorPrimario.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "${(resCom.confianza * 100).toInt()}%",
                                                color = colorPrimario,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            vibrateSuccess(haptic)
                                            onSenaComunicativaDetectada?.invoke(resCom.sena)
                                            val espacio = if (textoActual.isNotEmpty() && !textoActual.endsWith(" ")) " " else ""
                                            onTextoChange(textoActual + espacio + resCom.sena.nombre)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = colorPrimario),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().height(36.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "Insertar: ${resCom.sena.sugerenciaFrase}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // Selector / Desplegable de Teclado Manual Alternativo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    vibrateTap(haptic)
                    mostrarTecladoManual = !mostrarTecladoManual
                }
                .background(Color(0xFF151525))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Keyboard, null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = if (mostrarTecladoManual) "Ocultar teclado táctil de apoyo" else "Mostrar teclado táctil de apoyo",
                    color = Color.LightGray,
                    fontSize = 11.sp
                )
            }
            Text(
                text = if (mostrarTecladoManual) "▲" else "▼",
                color = Color.LightGray,
                fontSize = 10.sp
            )
        }

        AnimatedVisibility(visible = mostrarTecladoManual) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 40.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 140.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(letras) { letra ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorPrimario.copy(alpha = 0.2f))
                                    .border(1.dp, colorPrimario.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable {
                                        vibrateTap(haptic)
                                        onTextoChange(textoActual + letra)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(letra, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CATALOGO_SENAS_COMUNICATIVAS.take(4).forEach { sena ->
                            Surface(
                                color = Color(0xFF151525),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        vibrateTap(haptic)
                                        onSenaComunicativaDetectada?.invoke(sena)
                                        val espacio = if (textoActual.isNotEmpty() && !textoActual.endsWith(" ")) " " else ""
                                        onTextoChange(textoActual + espacio + sena.nombre)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = obtenerIconoSena(sena.id),
                                        contentDescription = null,
                                        tint = colorPrimario,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(sena.nombre, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Botones de Control: Espacio y Borrar ───────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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

        // ── Botón Principal de Confirmación y Avance ───────────────────────────
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
                Text("Confirmar y Continuar", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
    }
}
}

private fun aplicarEMA(
    actual: List<NormalizedPoint3D>,
    previo: List<NormalizedPoint3D>,
    alpha: Float = 0.45f
): List<NormalizedPoint3D> {
    if (previo.isEmpty() || previo.size != actual.size) return actual
    return actual.mapIndexed { i, curr ->
        val prev = previo[i]
        val xSmooth = alpha * curr.x + (1f - alpha) * prev.x
        val ySmooth = alpha * curr.y + (1f - alpha) * prev.y
        val zSmooth = alpha * curr.z + (1f - alpha) * prev.z
        NormalizedPoint3D(xSmooth, ySmooth, zSmooth)
    }
}
