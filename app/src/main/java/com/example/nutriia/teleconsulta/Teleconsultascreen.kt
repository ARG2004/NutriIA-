package com.example.nutriia.teleconsulta

import android.content.Context
import android.view.ViewGroup
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack
import com.example.nutriia.utils.FechaUtils
import java.text.SimpleDateFormat
import java.util.*

// ─── Paleta de llamada ─────────────────────────────────────────────────────────
private val CallBg        = Color(0xFF08111C)
private val CallBg2       = Color(0xFF0F1F33)
private val CallBg3       = Color(0xFF0D2137)
private val CallGreen     = Color(0xFF22C55E)
private val CallGreenDark = Color(0xFF15803D)
private val CallGreenGlow = Color(0xFF4ADE80)
private val CallRed       = Color(0xFFEF4444)
private val CallBlue      = Color(0xFF60A5FA)
private val CallWhite     = Color(0xFFE2E8F0)
private val CallGray      = Color(0xFF64748B)
private val CallSurface   = Color(0xFF1E293B)
private val CallSurface2  = Color(0xFF172032)

// ═════════════════════════════════════════════════════════════════════════════
// SURFACE VIEW RENDERER — Vista de video WebRTC nativa
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun WebRtcVideoView(
    videoTrack: VideoTrack?,
    modifier:   Modifier = Modifier,
    isMirror:   Boolean  = false
) {
    val eglBase = remember { EglBase.create() }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                init(eglBase.eglBaseContext, null)
                setMirror(isMirror)
                setEnableHardwareScaler(true)
            }
        },
        update = { renderer ->
            renderer.setMirror(isMirror)
            videoTrack?.addSink(renderer)
        },
        onRelease = { renderer ->
            videoTrack?.removeSink(renderer)
            renderer.release()
            eglBase.release()
        },
        modifier = modifier
    )
}

// ═════════════════════════════════════════════════════════════════════════════
// HOST OVERLAY — punto de entrada único, colócalo en tu Scaffold/Screen raíz
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TeleconsultaHostOverlay(
    viewModel: TeleconsultaViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // ── Llamada ENTRANTE (padre recibe) ───────────────────────────────────────
    state.llamadaEntrante?.let { llamada ->
        LlamadaEntranteOverlay(
            llamada    = llamada,
            onAceptar  = { viewModel.responderLlamada(llamada.id, true) },
            onRechazar = { viewModel.responderLlamada(llamada.id, false) }
        )
    }

    // ── Pantalla de llamada activa (ambos lados) ──────────────────────────────
    val mostrarPantallaActiva = state.llamadaActual != null &&
            state.llamadaActual?.estado != EstadoLlamada.FINALIZADA &&
            state.llamadaActual?.estado != EstadoLlamada.RECHAZADA &&
            state.llamadaEntrante == null

    AnimatedVisibility(
        visible = mostrarPantallaActiva,
        enter   = fadeIn(tween(300)) + slideInVertically(tween(400)) { it },
        exit    = fadeOut(tween(250)) + slideOutVertically(tween(350)) { it }
    ) {
        Dialog(
            onDismissRequest = {},
            properties       = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress      = false,
                dismissOnClickOutside   = false
            )
        ) {
            TeleconsultaActiveScreen(
                state       = state,
                onSilenciar = { viewModel.toggleSilencio() },
                onCamara    = { viewModel.toggleCamara() },
                onAltavoz   = { viewModel.toggleAltavoz() },
                onGirar     = { viewModel.cambiarCamara() },
                onColgar    = {
                    viewModel.finalizarLlamada()
                    viewModel.cerrarPantallaLlamada()
                }
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// PANTALLA ACTIVA DE LLAMADA
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TeleconsultaActiveScreen(
    state:       TeleconsultaUiState,
    onSilenciar: () -> Unit,
    onCamara:    () -> Unit,
    onAltavoz:   () -> Unit,
    onGirar:     () -> Unit,
    onColgar:    () -> Unit
) {
    val llamada = state.llamadaActual ?: return
    val isVideo  = llamada.tipo == TipoLlamada.VIDEO

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(CallBg, CallBg2, CallBg3)))
    ) {

        // ── Video remoto de fondo (si es videollamada y está conectado) ────────
        if (isVideo && state.webRtcConectado && state.remoteVideoTrack != null) {
            WebRtcVideoView(
                videoTrack = state.remoteVideoTrack,
                modifier   = Modifier.fillMaxSize(),
                isMirror   = false
            )
            // Gradiente encima del video para legibilidad
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Black.copy(0.35f),
                                Color.Transparent,
                                Color.Black.copy(0.55f)
                            )
                        )
                    )
            )
        } else {
            // Fondo animado cuando no hay video
            AnimatedCallBackground(isVideo = isVideo)
        }

        Column(
            modifier            = Modifier.fillMaxSize().systemBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))

            // Badge tipo de llamada
            CallTypeBadge(tipo = llamada.tipo, estado = llamada.estado)

            Spacer(Modifier.height(36.dp))

            // Zona central según estado
            when (llamada.estado) {
                EstadoLlamada.SONANDO, EstadoLlamada.INICIANDO -> {
                    ConnectingSection(
                        nombre      = llamada.padreNombre,
                        childNombre = llamada.childNombre
                    )
                }
                EstadoLlamada.ACTIVA -> {
                    // Si hay video remoto, mostrar solo info mínima arriba
                    if (isVideo && state.webRtcConectado && state.remoteVideoTrack != null) {
                        ActiveCallMinimalHeader(
                            nombre      = llamada.padreNombre,
                            childNombre = llamada.childNombre,
                            segundos    = state.duracionSegundos
                        )
                    } else {
                        ActiveCallSection(
                            nombre        = llamada.padreNombre,
                            childNombre   = llamada.childNombre,
                            isVideo       = isVideo,
                            camaraApagada = state.camaraApagada,
                            segundos      = state.duracionSegundos,
                            webRtcConect  = state.webRtcConectado
                        )
                    }
                }
                else -> {}
            }

            Spacer(Modifier.weight(1f))

            // ── Video local pequeño (pip) ─────────────────────────────────────
            if (isVideo && state.enLlamada) {
                Box(
                    modifier = Modifier
                        .padding(end = 16.dp, bottom = 8.dp)
                        .align(Alignment.End)
                ) {
                    // Video local (self view)
                    Box(
                        modifier = Modifier
                            .size(width = 100.dp, height = 140.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CallSurface2)
                            .border(
                                1.5.dp,
                                Brush.linearGradient(listOf(CallGreen.copy(0.6f), CallBlue.copy(0.6f))),
                                RoundedCornerShape(14.dp)
                            )
                    ) {
                        if (!state.camaraApagada && CallEngineProvider.isInitialized) {
                            // Renderizamos la cámara local directamente asignando el sink al engine
                            LocalVideoSinkView(modifier = Modifier.fillMaxSize())
                        } else {
                            Box(
                                modifier         = Modifier.fillMaxSize().background(CallSurface2),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.VideocamOff,
                                    null,
                                    tint     = CallGray,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Panel de controles
            AnimatedVisibility(
                visible = true,
                enter   = slideInVertically(spring(stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn()
            ) {
                CallControlsPanel(
                    estado      = llamada.estado,
                    silenciado  = state.silenciado,
                    camaraOff   = state.camaraApagada,
                    altavozOn   = state.altavozActivo,
                    tipo        = llamada.tipo,
                    onSilenciar = onSilenciar,
                    onCamara    = onCamara,
                    onAltavoz   = onAltavoz,
                    onGirar     = onGirar,
                    onColgar    = onColgar
                )
            }
        }
    }
}

// ─── Vista de video LOCAL (self view en pip) ──────────────────────────────────
@Composable
fun LocalVideoSinkView(modifier: Modifier = Modifier) {
    val eglBase = remember { EglBase.create() }

    AndroidView(
        factory = { ctx ->
            SurfaceViewRenderer(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                init(eglBase.eglBaseContext, null)
                setMirror(true) // Siempre espejo para vista propia
                setEnableHardwareScaler(true)
            }
        },
        update = { renderer ->
            // Asignar este renderer como sink local del engine
            if (CallEngineProvider.isInitialized) {
                CallEngineProvider.engine.localVideoSink = renderer
            }
        },
        onRelease = { renderer ->
            if (CallEngineProvider.isInitialized) {
                CallEngineProvider.engine.localVideoSink = null
            }
            renderer.release()
            eglBase.release()
        },
        modifier = modifier
    )
}

// ─── Fondo animado ────────────────────────────────────────────────────────────
@Composable
private fun AnimatedCallBackground(isVideo: Boolean) {
    val inf = rememberInfiniteTransition(label = "bg")
    val o1 by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(9000), RepeatMode.Reverse), "o1")
    val o2 by inf.animateFloat(1f, 0f, infiniteRepeatable(tween(7000), RepeatMode.Reverse), "o2")
    val c1 = if (isVideo) CallBlue else CallGreen

    Box(Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .size(360.dp)
                .offset(x = (60 + o1 * 30).dp, y = (-80 + o1 * 25).dp)
                .background(Brush.radialGradient(listOf(c1.copy(0.07f), Color.Transparent)), CircleShape)
        )
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-30 + o2 * 20).dp, y = (50 - o2 * 15).dp)
                .background(Brush.radialGradient(listOf(CallGreen.copy(0.06f), Color.Transparent)), CircleShape)
        )
    }
}

// ─── Header mínimo cuando hay video de fondo ──────────────────────────────────
@Composable
private fun ActiveCallMinimalHeader(nombre: String, childNombre: String, segundos: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(nombre, color = CallWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Paciente: $childNombre", color = CallWhite.copy(0.7f), fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
        CallTimerDisplay(segundos = segundos)
    }
}

// ─── Sección conectando ────────────────────────────────────────────────────────
@Composable
private fun ConnectingSection(nombre: String, childNombre: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PulsatingAvatar(nombre = nombre, size = 110.dp, ringColor = CallGreen)
        Spacer(Modifier.height(28.dp))
        Text("Llamando a", color = CallGray, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Text(nombre, color = CallWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ChildCare, null, tint = CallGray, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Paciente: $childNombre", color = CallGray, fontSize = 13.sp)
        }
        Spacer(Modifier.height(20.dp))
        PulsatingDots()
    }
}

// ─── Sección llamada activa (sin video remoto) ────────────────────────────────
@Composable
private fun ActiveCallSection(
    nombre:       String,
    childNombre:  String,
    isVideo:      Boolean,
    camaraApagada: Boolean,
    segundos:     Int,
    webRtcConect: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    Brush.radialGradient(listOf(CallGreen.copy(0.2f), CallGreenDark.copy(0.05f))),
                    CircleShape
                )
                .border(2.dp, Brush.linearGradient(listOf(CallGreen, CallGreenGlow)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(nombre.take(1).uppercase(), fontSize = 44.sp, fontWeight = FontWeight.Black, color = CallGreen)
        }

        Spacer(Modifier.height(20.dp))
        Text(nombre, color = CallWhite, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ChildCare, null, tint = CallGray, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Paciente: $childNombre", color = CallGray, fontSize = 12.sp)
        }

        Spacer(Modifier.height(8.dp))

        // Indicador de conexión WebRTC
        if (!webRtcConect && isVideo) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier  = Modifier.size(12.dp),
                    color     = CallBlue,
                    strokeWidth = 1.5.dp
                )
                Spacer(Modifier.width(6.dp))
                Text("Estableciendo conexión…", color = CallBlue, fontSize = 11.sp)
            }
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(16.dp))
        CallTimerDisplay(segundos = segundos)
    }
}

// ─── Panel de controles ───────────────────────────────────────────────────────
@Composable
private fun CallControlsPanel(
    estado:      EstadoLlamada,
    silenciado:  Boolean,
    camaraOff:   Boolean,
    altavozOn:   Boolean,
    tipo:        TipoLlamada,
    onSilenciar: () -> Unit,
    onCamara:    () -> Unit,
    onAltavoz:   () -> Unit,
    onGirar:     () -> Unit,
    onColgar:    () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color.Transparent, CallSurface.copy(0.95f))),
                RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp)
            )
            .padding(top = 36.dp, bottom = 32.dp, start = 28.dp, end = 28.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            AnimatedVisibility(
                visible = estado == EstadoLlamada.ACTIVA,
                enter   = fadeIn(tween(300)) + expandVertically(),
                exit    = fadeOut(tween(200)) + shrinkVertically()
            ) {
                Column {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallCtrlBtn(
                            icon    = if (silenciado) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                            label   = if (silenciado) "Sin mic" else "Silenciar",
                            active  = silenciado,
                            onClick = onSilenciar
                        )
                        if (tipo == TipoLlamada.VIDEO) {
                            CallCtrlBtn(
                                icon    = if (camaraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
                                label   = if (camaraOff) "Sin cám" else "Cámara",
                                active  = camaraOff,
                                onClick = onCamara
                            )
                            CallCtrlBtn(
                                icon    = Icons.Rounded.Cameraswitch,
                                label   = "Girar",
                                active  = false,
                                onClick = onGirar
                            )
                        }
                        CallCtrlBtn(
                            icon    = if (altavozOn) Icons.AutoMirrored.Rounded.VolumeUp else Icons.AutoMirrored.Rounded.VolumeOff,
                            label   = if (altavozOn) "Altavoz" else "Auricular",
                            active  = !altavozOn,
                            onClick = onAltavoz
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                }
            }

            // Botón colgar
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .background(CallRed, CircleShape)
                    .shadow(16.dp, CircleShape, spotColor = CallRed.copy(0.6f))
                    .clickable { onColgar() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.CallEnd, null, tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(6.dp))
            Text(
                if (estado == EstadoLlamada.SONANDO) "Cancelar llamada" else "Colgar",
                color    = CallGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun CallCtrlBtn(
    icon:    ImageVector,
    label:   String,
    active:  Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.width(70.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(
                    if (active) CallWhite.copy(0.14f) else CallSurface,
                    CircleShape
                )
                .border(
                    1.dp,
                    if (active) CallWhite.copy(0.25f) else Color(0xFF2D3F55),
                    CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = if (active) CallWhite else CallGray, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = CallGray, fontSize = 9.sp, textAlign = TextAlign.Center)
    }
}

// ─── Helpers visuales ─────────────────────────────────────────────────────────
@Composable
private fun PulsatingAvatar(nombre: String, size: Dp, ringColor: Color) {
    val inf = rememberInfiniteTransition(label = "pAvatar")

    @Composable
    fun Ring(delayMs: Int, maxScale: Float) {
        val p by inf.animateFloat(
            0f, 1f,
            infiniteRepeatable(tween(2200, delayMillis = delayMs), RepeatMode.Restart),
            "r$delayMs"
        )
        Box(
            modifier = Modifier
                .size(size)
                .scale(1f + p * (maxScale - 1f))
                .alpha((1f - p) * 0.45f)
                .border(1.5.dp, ringColor, CircleShape)
        )
    }

    Box(contentAlignment = Alignment.Center) {
        Ring(0, 2.6f); Ring(700, 2.1f); Ring(1400, 1.65f)
        Box(
            modifier = Modifier
                .size(size)
                .background(
                    Brush.radialGradient(listOf(ringColor.copy(0.22f), ringColor.copy(0.06f))),
                    CircleShape
                )
                .border(2.dp, ringColor.copy(0.75f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(nombre.take(1).uppercase(), fontSize = (size.value * 0.35f).sp, fontWeight = FontWeight.Black, color = ringColor)
        }
    }
}

@Composable
private fun PulsatingDots() {
    val inf = rememberInfiniteTransition(label = "dots")

    @Composable
    fun Dot(delayMs: Int) {
        val a by inf.animateFloat(
            0.25f, 1f,
            infiniteRepeatable(tween(550, delayMillis = delayMs), RepeatMode.Reverse),
            "d$delayMs"
        )
        Box(Modifier.size(8.dp).alpha(a).background(CallGreen, CircleShape))
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Dot(0); Dot(183); Dot(366)
    }
}

@Composable
private fun CallTimerDisplay(segundos: Int) {
    val h = segundos / 3600
    val m = (segundos % 3600) / 60
    val s = segundos % 60
    val texto = if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)

    Surface(shape = RoundedCornerShape(20.dp), color = CallSurface.copy(0.85f)) {
        Row(
            modifier          = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val inf = rememberInfiniteTransition(label = "dot")
            val a by inf.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), "dl")
            Box(Modifier.size(7.dp).alpha(a).background(CallGreen, CircleShape))
            Spacer(Modifier.width(10.dp))
            Text(texto, color = CallWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CallTypeBadge(tipo: TipoLlamada, estado: EstadoLlamada) {
    val icon  = if (tipo == TipoLlamada.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Call
    val color = if (tipo == TipoLlamada.VIDEO) CallBlue else CallGreen
    val label = when (estado) {
        EstadoLlamada.SONANDO -> if (tipo == TipoLlamada.VIDEO) "Videollamada…" else "Llamada de audio…"
        EstadoLlamada.ACTIVA  -> if (tipo == TipoLlamada.VIDEO) "Videollamada en curso" else "Llamada de audio en curso"
        else                  -> if (tipo == TipoLlamada.VIDEO) "Videollamada" else "Llamada de audio"
    }

    Surface(shape = RoundedCornerShape(20.dp), color = color.copy(0.15f)) {
        Row(
            modifier          = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// OVERLAY DE LLAMADA ENTRANTE (padre recibe)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun LlamadaEntranteOverlay(
    llamada:   SolicitudLlamada,
    onAceptar:  () -> Unit,
    onRechazar: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress      = false,
            dismissOnClickOutside   = false
        )
    ) {
        Box(
            modifier         = Modifier.fillMaxSize().background(Color(0xD9020810)),
            contentAlignment = Alignment.Center
        ) {
            IncomingCallCard(llamada = llamada, onAceptar = onAceptar, onRechazar = onRechazar)
        }
    }
}

@Composable
private fun IncomingCallCard(
    llamada:    SolicitudLlamada,
    onAceptar:  () -> Unit,
    onRechazar: () -> Unit
) {
    val isVideo     = llamada.tipo == TipoLlamada.VIDEO
    val accentColor = if (isVideo) CallBlue else CallGreen
    val inf = rememberInfiniteTransition(label = "incard")

    Card(
        modifier  = Modifier.fillMaxWidth(0.9f).animateContentSize(),
        shape     = RoundedCornerShape(36.dp),
        colors    = CardDefaults.cardColors(containerColor = Color(0xFF0C1A2E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 48.dp)
    ) {
        Column(
            modifier            = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = RoundedCornerShape(14.dp), color = accentColor.copy(0.15f)) {
                Row(
                    modifier          = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                        null, tint = accentColor, modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(7.dp))
                    Text(
                        if (isVideo) "Videollamada entrante" else "Llamada de audio entrante",
                        color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            // Avatar pulsante
            Box(contentAlignment = Alignment.Center) {
                @Composable
                fun InRing(delayMs: Int, maxScale: Float) {
                    val p by inf.animateFloat(
                        0f, 1f,
                        infiniteRepeatable(tween(1800, delayMillis = delayMs), RepeatMode.Restart),
                        "ir$delayMs"
                    )
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .scale(1f + p * (maxScale - 1f))
                            .alpha((1f - p) * 0.4f)
                            .border(1.5.dp, CallGreen, CircleShape)
                    )
                }
                InRing(0, 2.5f); InRing(600, 2.0f); InRing(1200, 1.6f)

                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .background(CallGreen.copy(0.16f), CircleShape)
                        .border(2.dp, CallGreen.copy(0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        llamada.nutriologoNombre.take(1).uppercase(),
                        fontSize = 34.sp, fontWeight = FontWeight.Black, color = CallGreen
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(llamada.nutriologoNombre, color = CallWhite, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Nutriólogo · Paciente: ${llamada.childNombre}",
                color = CallGray, fontSize = 12.sp, textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(40.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Rechazar
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .background(CallRed, CircleShape)
                            .shadow(12.dp, CircleShape, spotColor = CallRed.copy(0.5f))
                            .clickable { onRechazar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CallEnd, null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Rechazar", color = CallRed, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                // Aceptar
                val acceptScale by inf.animateFloat(
                    1f, 1.07f,
                    infiniteRepeatable(tween(700), RepeatMode.Reverse), "asc"
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(66.dp)
                            .scale(acceptScale)
                            .background(CallGreen, CircleShape)
                            .shadow(16.dp, CircleShape, spotColor = CallGreen.copy(0.55f))
                            .clickable { onAceptar() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                            null, tint = Color.White, modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Aceptar", color = CallGreen, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// BOTONES DE INICIO DE LLAMADA — para Dashboard y PediatraScreen
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TeleconsultaButtons(
    onLlamadaAudio: () -> Unit,
    onLlamadaVideo: () -> Unit,
    compact:        Boolean = false
) {
    if (compact) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallCallBtn(Icons.Rounded.Call,     CallGreen, "Audio", onLlamadaAudio)
            SmallCallBtn(Icons.Rounded.Videocam, CallBlue,  "Video", onLlamadaVideo)
        }
    } else {
        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick  = onLlamadaAudio,
                modifier = Modifier.weight(1f).height(44.dp),
                border   = BorderStroke(1.5.dp, CallGreen),
                shape    = RoundedCornerShape(13.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = CallGreen)
            ) {
                Icon(Icons.Rounded.Call, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick  = onLlamadaVideo,
                modifier = Modifier.weight(1f).height(44.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = CallBlue),
                shape    = RoundedCornerShape(13.dp)
            ) {
                Icon(Icons.Rounded.Videocam, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun SmallCallBtn(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(color.copy(0.12f), RoundedCornerShape(10.dp))
            .border(1.dp, color.copy(0.3f), RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = color, modifier = Modifier.size(16.dp))
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// HISTORIAL DE TELECONSULTAS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun HistorialTeleconsultasSection(historial: List<SolicitudLlamada>) {
    if (historial.isEmpty()) {
        Box(
            modifier         = Modifier.fillMaxWidth().padding(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Rounded.VideoCall,
                    null,
                    tint     = Color(0xFF689F38).copy(0.25f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(Modifier.height(10.dp))
                Text("Sin teleconsultas registradas", color = Color.Gray, fontSize = 13.sp)
            }
        }
        return
    }

    LazyColumn(
        contentPadding      = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(historial, key = { it.id }) { llamada ->
            TeleconsultaHistorialRow(llamada = llamada)
        }
    }
}

@Composable
private fun TeleconsultaHistorialRow(llamada: SolicitudLlamada) {
    val (badgeColor, badgeLabel) = when (llamada.estado) {
        EstadoLlamada.FINALIZADA -> Color(0xFF689F38) to "Completada"
        EstadoLlamada.RECHAZADA  -> Color(0xFFEF4444) to "Rechazada"
        EstadoLlamada.PERDIDA    -> Color(0xFFFF8F00) to "Perdida"
        EstadoLlamada.ACTIVA     -> CallGreen         to "En curso"
        else                     -> CallGray          to llamada.estado.name
    }
    val icon  = if (llamada.tipo == TipoLlamada.VIDEO) Icons.Rounded.Videocam else Icons.Rounded.Call
    val fecha = remember(llamada.creadoEn) {
        if (llamada.creadoEn == 0L) "—"
        else FechaUtils.formatearFechaHora(Date(llamada.creadoEn))
    }
    val duracion = remember(llamada.duracionSegundos) {
        val m = llamada.duracionSegundos / 60
        val s = llamada.duracionSegundos % 60
        if (llamada.duracionSegundos == 0) "—" else "%02d:%02d min".format(m, s)
    }

    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(42.dp).background(badgeColor.copy(0.12f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = badgeColor, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(llamada.padreNombre, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF33691E))
                Text("Paciente: ${llamada.childNombre}", fontSize = 11.sp, color = Color.Gray)
                Text(fecha, fontSize = 10.sp, color = Color.LightGray)
            }
            Column(horizontalAlignment = Alignment.End) {
                Surface(shape = RoundedCornerShape(8.dp), color = badgeColor.copy(0.12f)) {
                    Text(
                        badgeLabel,
                        fontSize   = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color      = badgeColor,
                        modifier   = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(duracion, fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}