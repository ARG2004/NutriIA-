package com.example.nutriia.analisisIA

import androidx.compose.ui.layout.ContentScale

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ui.theme.ChildProfile
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// ══════════════════════════════════════════════════════════════════════════════
// PALETA DE COLORES — Dinámica: Verde para Niño | Rosado para Embarazo
// ══════════════════════════════════════════════════════════════════════════════

data class AnalisisColors(
    val bgBase         : Color,
    val bgCard         : Color,
    val bgCardSoft     : Color,
    val primary        : Color,
    val medium         : Color,
    val light          : Color,
    val accentOrange   : Color,
    val accentOrangeL  : Color,
    val redSoft        : Color,
    val redLight       : Color,
    val amberWarm      : Color,
    val textPrimary    : Color,
    val textSecond     : Color,
    val textHint       : Color,
    val divider        : Color,
    val shadow         : Color
)

val ChildAnalisisColors = AnalisisColors(
    bgBase         = Color(0xFFF7F9F7),
    bgCard         = Color(0xFFFFFFFF),
    bgCardSoft     = Color(0xFFF2F7F3),
    primary        = Color(0xFF2E7D52),
    medium         = Color(0xFF43A573),
    light          = Color(0xFFD4EDE1),
    accentOrange   = Color(0xFFE8703A),
    accentOrangeL  = Color(0xFFFFF0E9),
    redSoft        = Color(0xFFD94F4F),
    redLight       = Color(0xFFFDE8E8),
    amberWarm      = Color(0xFFF0A500),
    textPrimary    = Color(0xFF1A2E22),
    textSecond     = Color(0xFF4A6356),
    textHint       = Color(0xFF8BA899),
    divider        = Color(0xFFE8F0EC),
    shadow         = Color(0xFF2E7D52).copy(alpha = 0.08f)
)

val PregnancyAnalisisColors = AnalisisColors(
    bgBase         = Color(0xFFFFF5F7),   // Fondo rosado muy suave
    bgCard         = Color(0xFFFFFFFF),   // Tarjetas blancas
    bgCardSoft     = Color(0xFFFDE8ED),   // Tarjetas rosa pastel
    primary        = Color(0xFFD81B60),   // Rosa / Magenta primario brillante
    medium         = Color(0xFFE91E63),   // Rosa medio
    light          = Color(0xFFFCE4EC),   // Rosa suave (fondos)
    accentOrange   = Color(0xFFEC407A),   // Rosa coral acento
    accentOrangeL  = Color(0xFFFFF0F5),   // Rosa muy claro
    redSoft        = Color(0xFFD32F2F),   // Rojo advertencia
    redLight       = Color(0xFFFFEBEE),   // Rojo claro
    amberWarm      = Color(0xFFFF4081),   // Rosa acento botones CTA
    textPrimary    = Color(0xFF3E1220),   // Texto vino/rosa muy oscuro
    textSecond     = Color(0xFF7A3B4E),   // Texto secundario rosa oscuro
    textHint       = Color(0xFFA86B7C),   // Texto hint
    divider        = Color(0xFFF8D7E3),   // Divisores rosados
    shadow         = Color(0xFFD81B60).copy(alpha = 0.08f)
)

val LocalAnalisisColors = staticCompositionLocalOf { ChildAnalisisColors }

private val BgBase: Color @Composable get() = LocalAnalisisColors.current.bgBase
private val BgCard: Color @Composable get() = LocalAnalisisColors.current.bgCard
private val BgCardSoft: Color @Composable get() = LocalAnalisisColors.current.bgCardSoft
private val GreenPrimary: Color @Composable get() = LocalAnalisisColors.current.primary
private val GreenMedium: Color @Composable get() = LocalAnalisisColors.current.medium
private val GreenLight: Color @Composable get() = LocalAnalisisColors.current.light
private val AccentOrange: Color @Composable get() = LocalAnalisisColors.current.accentOrange
private val AccentOrangeL: Color @Composable get() = LocalAnalisisColors.current.accentOrangeL
private val RedSoft: Color @Composable get() = LocalAnalisisColors.current.redSoft
private val RedLight: Color @Composable get() = LocalAnalisisColors.current.redLight
private val AmberWarm: Color @Composable get() = LocalAnalisisColors.current.amberWarm
private val TextPrimary: Color @Composable get() = LocalAnalisisColors.current.textPrimary
private val TextSecond: Color @Composable get() = LocalAnalisisColors.current.textSecond
private val TextHint: Color @Composable get() = LocalAnalisisColors.current.textHint
private val DividerColor: Color @Composable get() = LocalAnalisisColors.current.divider
private val ShadowColor: Color @Composable get() = LocalAnalisisColors.current.shadow

// ══════════════════════════════════════════════════════════════════════════════
// PANTALLA RAÍZ
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun AnalisisScreen(
    child          : ChildProfile? = null,
    perfilEmbarazo : PerfilEmbarazo? = null,
    isEmbarazo     : Boolean = false,
    onNavigateBack : () -> Unit,
    viewModel      : AnalisisViewModel = viewModel()
) {
    val esModoEmbarazo = isEmbarazo || perfilEmbarazo != null
    val colors = if (esModoEmbarazo) PregnancyAnalisisColors else ChildAnalisisColors

    CompositionLocalProvider(LocalAnalisisColors provides colors) {
                        val uiState by viewModel.uiState.collectAsState()

        val a11yMode = LocalAccessibilityMode.current
        val a11yVm: AccessibilityViewModel = viewModel()
        val esBlind = a11yMode == AccessibilityMode.BLIND
        val esMute = a11yMode == AccessibilityMode.MUTE
        val esAccesible = esBlind || esMute

        val targetNombre = if (esModoEmbarazo) "tu embarazo" else (child?.name ?: "tu bebé")

        LaunchedEffect(uiState) {
            if (esBlind) {
                when (val state = uiState) {
                    is AnalisisUiState.Idle -> {
                        a11yVm.hablar("Módulo de Análisis de Alimento con Inteligencia Artificial para $targetNombre. Coloca el alimento frente a la cámara y presiona el botón inferior para escanear e iniciar el análisis.")
                    }
                    is AnalisisUiState.Capturando -> {
                        a11yVm.hablar("Cámara activa. Alinea el alimento al centro de la pantalla y presiona el botón central inferior de captura.")
                    }
                    is AnalisisUiState.Analizando -> {
                        a11yVm.hablar("Analizando alimento con Inteligencia Artificial para $targetNombre. Por favor, espera unos segundos.")
                    }
                    is AnalisisUiState.Exito -> {
                        val food = state.resultado.foodDetection
                        val nutrition = state.resultado.nutrition
                        val analysis = state.resultado.analysis
                        val recomText = if (analysis.recommended) "Recomendado para $targetNombre" else "No recomendado para $targetNombre"
                        a11yVm.hablar("Análisis completado con éxito. Se detectó ${food.foodName}. ${recomText}. Calorías estimadas: ${nutrition.calories.toInt()} kilocalorías. Proteínas: ${"%.1f".format(nutrition.protein)} gramos, Carbohidratos: ${"%.1f".format(nutrition.carbohydrates)} gramos, Grasas: ${"%.1f".format(nutrition.fat)} gramos.")
                    }
                    is AnalisisUiState.Guardado -> {
                        a11yVm.hablar("Análisis guardado exitosamente en el diario nutricional.")
                    }
                    is AnalisisUiState.Error -> {
                        a11yVm.hablar("Ocurrió un error en el análisis. Detalle: ${state.mensaje}. Presiona el botón de abajo para reintentar.")
                    }
                }
            }
        }

        val tieneCamara = true

        

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgBase)
        ) {
            AnimatedContent(
                targetState = uiState,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300, easing = EaseOut)) togetherWith
                            fadeOut(animationSpec = tween(200))
                },
                label = "screen_transition"
            ) { state ->
                when (state) {
                    is AnalisisUiState.Idle -> PantallaInicial(
                        child          = child,
                        perfilEmbarazo = perfilEmbarazo,
                        isEmbarazo     = esModoEmbarazo,
                        onTomarFoto    = {
                            viewModel.abrirCamara()
                        },
                        onVolver       = { viewModel.resetear(); onNavigateBack() }
                    )
                    is AnalisisUiState.Capturando -> PantallaCaptura(
                                                onIniciarCamara = { },
                        onCapturar      = { viewModel.analizarFoto("", child, perfilEmbarazo, esModoEmbarazo) },
                        onCancelar      = { viewModel.cancelarCamara() }
                    )
                    is AnalisisUiState.Analizando -> PantallaAnalizando(
                        mensaje    = state.mensaje,
                        onCancelar = { viewModel.cancelarAnalisis() }
                    )
                    is AnalisisUiState.Exito -> PantallaResultado(
                        resultado  = state.resultado,
                        child      = child,
                        isEmbarazo = esModoEmbarazo,
                        onGuardar  = { viewModel.guardarEnHistorial(if (esModoEmbarazo) "embarazo" else (child?.id ?: "embarazo")) },
                        onNuevo    = { viewModel.resetear() },
                        onVolver   = { viewModel.resetear(); onNavigateBack() }
                    )
                    is AnalisisUiState.Guardado -> PantallaGuardado(
                        onNuevo  = { viewModel.resetear() },
                        onVolver = { viewModel.resetear(); onNavigateBack() }
                    )
                    is AnalisisUiState.Error -> PantallaError(
                        mensaje      = state.mensaje,
                        onReintentar = { viewModel.resetear() }
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// PANTALLA INICIAL
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaInicial(
    child          : ChildProfile? = null,
    perfilEmbarazo : PerfilEmbarazo? = null,
    isEmbarazo     : Boolean = false,
    onTomarFoto    : () -> Unit,
    onVolver       : () -> Unit
) {
    var mostrarGuia by remember { mutableStateOf(false) }

    val a11yMode = LocalAccessibilityMode.current
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    if (mostrarGuia) {
        PantallaGuiaFoto(onListo = { mostrarGuia = false; onTomarFoto() })
        return
    }

    // Animación de entrada
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header con gradiente suave ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(GreenLight, BgBase)
                    )
                )
                .padding(horizontal = 20.dp)
                .padding(top = 52.dp, bottom = 28.dp)
        ) {
            // Botón volver
            FilledTonalIconButton(
                onClick  = onVolver,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(if (esAccesible) 64.dp else 40.dp),
                colors   = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = BgCard,
                    contentColor   = GreenPrimary
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", modifier = Modifier.size(if (esAccesible) 30.dp else 20.dp))
            }

            Column(
                modifier            = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(4.dp))

                // Ícono animado con pulso suave
                val pulso by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue  = 1f,
                    targetValue   = 1.04f,
                    animationSpec = infiniteRepeatable(
                        animation  = tween(2000, easing = EaseInOutSine),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(pulso)
                        .clip(CircleShape)
                        .background(GreenPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.FoodBank,
                        contentDescription = null,
                        tint     = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    "Analizar Alimento",
                    fontSize   = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary,
                    textAlign  = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        if (isEmbarazo) Icons.Rounded.Favorite else Icons.Rounded.ChildCare,
                        contentDescription = null,
                        tint     = GreenMedium,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        if (isEmbarazo) "Para Tu Embarazo (${perfilEmbarazo?.semanas ?: 1} sem)" else "Para ${child?.name ?: "tu bebé"}",
                        fontSize = 13.sp,
                        color    = GreenMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // ── Guía de foto ──
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                "¿Cómo obtener un mejor resultado?",
                fontSize   = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextPrimary
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GuiaTarjeta(
                    modifier = Modifier.weight(1f),
                    icono    = Icons.Outlined.CheckCircle,
                    titulo   = "Correcto",
                    items    = listOf("Alimento dentro del marco", "Ingredientes visibles"),
                    esOk     = true
                )
                GuiaTarjeta(
                    modifier = Modifier.weight(1f),
                    icono    = Icons.Outlined.Cancel,
                    titulo   = "Incorrecto",
                    items    = listOf("Alimento muy cerca", "Partes no visibles"),
                    esOk     = false
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Pasos ──
            Text(
                "¿Cómo funciona?",
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color      = TextSecond
            )
            Spacer(Modifier.height(10.dp))

            val targetNombrePasos = if (isEmbarazo) "tu embarazo" else (child?.name ?: "tu bebé")
            val pasos = listOf(
                Triple(Icons.Outlined.CameraAlt,       "Toma una foto del alimento",    GreenPrimary),
                Triple(Icons.Outlined.AutoAwesome,     "La IA identifica el alimento",  Color(0xFF7B68EE)),
                Triple(Icons.Outlined.Analytics,       "Obtiene datos nutricionales",   Color(0xFF20B2AA)),
                Triple(Icons.Outlined.MonitorHeart,    "Análisis personalizado para $targetNombrePasos", AccentOrange)
            )

            pasos.forEachIndexed { i, (icon, texto, color) ->
                PasoItem(numero = i + 1, icon = icon, texto = texto, color = color)
                if (i < pasos.lastIndex) Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(28.dp))

            // ── Botón CTA principal ──
            Button(
                onClick  = { mostrarGuia = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (esAccesible) 70.dp else 54.dp),
                shape  = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(if (esAccesible) 26.dp else 20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Escanear Alimento",
                    fontWeight = FontWeight.SemiBold,
                    fontSize   = if (esAccesible) 18.sp else 16.sp,
                    color      = Color.White
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun GuiaTarjeta(
    modifier : Modifier,
    icono    : ImageVector,
    titulo   : String,
    items    : List<String>,
    esOk     : Boolean
) {
    val bgColor     = if (esOk) GreenLight else RedLight
    val accentColor = if (esOk) GreenPrimary else RedSoft
    val iconCheck   = if (esOk) Icons.Outlined.CheckCircle else Icons.Outlined.Cancel

    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icono, null, tint = accentColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    titulo,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = accentColor
                )
            }
            Spacer(Modifier.height(8.dp))
            items.forEach { item ->
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier          = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        iconCheck,
                        null,
                        tint     = accentColor,
                        modifier = Modifier.size(13.dp).padding(top = 1.dp)
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(item, fontSize = 11.sp, color = TextSecond, lineHeight = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun PasoItem(numero: Int, icon: ImageVector, texto: String, color: Color) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(containerColor = BgCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                texto,
                fontWeight = FontWeight.Medium,
                color      = TextPrimary,
                fontSize   = 13.sp,
                modifier   = Modifier.weight(1f)
            )
            Box(
                modifier         = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "$numero",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// GUÍA DE FOTO
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaGuiaFoto(onListo: () -> Unit) {
    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(BgBase)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(56.dp))

        Box(
            modifier         = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.PhotoCamera,
                null,
                tint     = GreenPrimary,
                modifier = Modifier.size(26.dp)
            )
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Consejos para mejor resultado",
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Sigue estas recomendaciones para una detección más precisa",
            fontSize  = 13.sp,
            color     = TextSecond,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(28.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // OK
            Column(
                modifier            = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.78f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(GreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.RiceBowl,
                        null,
                        tint     = GreenMedium,
                        modifier = Modifier.size(56.dp)
                    )
                    MarcoEsquinas(color = GreenPrimary)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        null,
                        tint     = GreenPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Correcto",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = GreenPrimary
                    )
                }
            }

            // NO OK
            Column(
                modifier            = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.78f)
                        .clip(RoundedCornerShape(18.dp))
                        .background(RedLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Outlined.RiceBowl,
                        null,
                        tint     = RedSoft.copy(alpha = 0.5f),
                        modifier = Modifier.size(88.dp)
                    )
                    MarcoEsquinas(color = RedSoft)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.Cancel,
                        null,
                        tint     = RedSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Incorrecto",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = RedSoft
                    )
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        // Tips en 2 columnas
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                TipItem("Alimento dentro del marco", true)
                Spacer(Modifier.height(6.dp))
                TipItem("Todos los ingredientes visibles", true)
            }
            Column(Modifier.weight(1f)) {
                TipItem("Alimento muy cerca o cortado", false)
                Spacer(Modifier.height(6.dp))
                TipItem("Ingredientes no visibles", false)
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick  = onListo,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape  = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Text(
                "Entendido, continuar",
                fontWeight = FontWeight.SemiBold,
                fontSize   = 15.sp,
                color      = Color.White
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                null,
                tint     = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun TipItem(texto: String, esOk: Boolean) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            if (esOk) Icons.Rounded.CheckCircle else Icons.Rounded.Cancel,
            null,
            tint     = if (esOk) GreenPrimary else RedSoft,
            modifier = Modifier.size(15.dp).padding(top = 1.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(texto, fontSize = 12.sp, color = TextSecond, lineHeight = 16.sp)
    }
}

@Composable
private fun MarcoEsquinas(color: Color) {
    Box(modifier = Modifier.fillMaxSize().padding(14.dp)) {
        val grosor = 2.5.dp
        val largo  = 18.dp
        // TL
        Box(Modifier.align(Alignment.TopStart)) {
            Box(Modifier.width(largo).height(grosor).clip(RoundedCornerShape(1.dp)).background(color))
            Box(Modifier.width(grosor).height(largo).clip(RoundedCornerShape(1.dp)).background(color))
        }
        // TR
        Box(Modifier.align(Alignment.TopEnd)) {
            Box(Modifier.width(largo).height(grosor).clip(RoundedCornerShape(1.dp)).background(color).align(Alignment.TopEnd))
            Box(Modifier.width(grosor).height(largo).clip(RoundedCornerShape(1.dp)).background(color).align(Alignment.TopEnd))
        }
        // BL
        Box(Modifier.align(Alignment.BottomStart)) {
            Box(Modifier.width(grosor).height(largo).clip(RoundedCornerShape(1.dp)).background(color).align(Alignment.BottomStart))
            Box(Modifier.width(largo).height(grosor).clip(RoundedCornerShape(1.dp)).background(color).align(Alignment.BottomStart))
        }
        // BR
        Box(Modifier.align(Alignment.BottomEnd)) {
            Box(Modifier.width(largo).height(grosor).clip(RoundedCornerShape(1.dp)).background(color).align(Alignment.BottomEnd))
            Box(Modifier.width(grosor).height(largo).clip(RoundedCornerShape(1.dp)).background(color).align(Alignment.BottomEnd))
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// CÁMARA
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaCaptura(
        onIniciarCamara : () -> Unit = {},
    onCapturar      : () -> Unit,
    onCancelar      : () -> Unit
) {
    val a11yMode = LocalAccessibilityMode.current
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("Cámara no disponible", color = Color.White)
        }

        // Overlays
        Box(
            modifier = Modifier
                .fillMaxWidth().height(140.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(0.65f), Color.Transparent)))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth().height(200.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.75f))))
        )

        // Botón cerrar
        FilledTonalIconButton(
            onClick  = onCancelar,
            modifier = Modifier.align(Alignment.TopStart).padding(16.dp, 44.dp).size(if (esAccesible) 64.dp else 40.dp),
            colors   = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = Color.White.copy(alpha = 0.15f),
                contentColor   = Color.White
            )
        ) {
            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(if (esAccesible) 30.dp else 20.dp))
        }

        // Marco de enfoque con pulso suave
        val marcaAlpha by rememberInfiniteTransition(label = "frame").animateFloat(
            initialValue  = 0.6f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(tween(1800, easing = EaseInOutSine), RepeatMode.Reverse),
            label         = "alpha"
        )
        Box(
            modifier         = Modifier
                .size(240.dp)
                .align(Alignment.Center)
                .graphicsLayer(alpha = marcaAlpha),
            contentAlignment = Alignment.Center
        ) {
            MarcoEsquinas(color = Color.White)
        }
        Text(
            "Centra el alimento aquí",
            fontSize  = 12.sp,
            color     = Color.White.copy(0.8f),
            modifier  = Modifier.align(Alignment.Center).padding(top = 220.dp)
        )

        // Botón captura
        Column(
            modifier            = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 44.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Toca para escanear", color = Color.White.copy(0.7f), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Box(
                modifier         = Modifier
                    .size(if (esAccesible) 96.dp else 74.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(0.2f)),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(
                    onClick        = onCapturar,
                    shape          = CircleShape,
                    modifier       = Modifier.size(if (esAccesible) 82.dp else 62.dp),
                    containerColor = GreenPrimary,
                    contentColor   = Color.White
                ) {
                    Icon(
                        Icons.Outlined.CameraAlt,
                        null,
                        modifier = Modifier.size(if (esAccesible) 36.dp else 28.dp)
                    )
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ANALIZANDO
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaAnalizando(mensaje: String, onCancelar: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "analyzing")

    val rotation by infiniteTransition.animateFloat(
        initialValue  = 0f,
        targetValue   = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing)),
        label         = "rotation"
    )
    val pulso by infiniteTransition.animateFloat(
        initialValue  = 0.95f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val progressAlpha by infiniteTransition.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(BgBase)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier
                .size(110.dp)
                .scale(pulso)
                .clip(CircleShape)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color       = GreenPrimary,
                modifier    = Modifier.size(82.dp),
                strokeWidth = 2.5.dp
            )
            Icon(
                Icons.Outlined.AutoAwesome,
                null,
                tint     = AccentOrange,
                modifier = Modifier.size(34.dp)
            )
        }

        Spacer(Modifier.height(32.dp))

        Text(
            "Analizando...",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            mensaje,
            fontSize  = 14.sp,
            color     = TextSecond,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(24.dp))

        // Barra de progreso elegante
        LinearProgressIndicator(
            modifier   = Modifier
                .fillMaxWidth(0.55f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .graphicsLayer(alpha = progressAlpha),
            color      = GreenPrimary,
            trackColor = DividerColor
        )

        Spacer(Modifier.height(36.dp))

        TextButton(
            onClick = onCancelar,
            colors  = ButtonDefaults.textButtonColors(contentColor = TextHint)
        ) {
            Icon(Icons.Rounded.Close, null, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(6.dp))
            Text("Cancelar", fontSize = 13.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// RESULTADO
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaResultado(
    resultado  : AnalisisCompleto,
    child      : ChildProfile? = null,
    isEmbarazo : Boolean = false,
    onGuardar  : () -> Unit,
    onNuevo    : () -> Unit,
    onVolver   : () -> Unit
) {
    val food      = resultado.foodDetection
    val nutrition = resultado.nutrition
    val analysis  = resultado.analysis

    val targetNombre = if (isEmbarazo) "Tu Embarazo" else (child?.name ?: "tu bebé")
    val (recomColor, recomBg, recomBadge, recomLabel) = when {
        analysis.recommended -> listOf(GreenPrimary, GreenLight, "✓", "Recomendado para $targetNombre")
        !analysis.recommended && analysis.warnings.isNotEmpty() -> listOf(RedSoft, RedLight, "✕", "No recomendado para $targetNombre")
        else -> listOf(AccentOrange, AccentOrangeL, "!", "Con precaución para $targetNombre")
    }

    val mealChipText = when (food.foodType.lowercase()) {
        "objeto_no_comestible" -> "📦 Objeto no comestible"
        "desayuno" -> "☕ Desayuno / Café"
        "comida"   -> "🍲 Comida / Almuerzo"
        "cena"     -> "🌙 Cena"
        "snack"    -> "🍎 Colación / Snack"
        "bebida"   -> "🥤 Bebida"
        "fruta"    -> "🍓 Fruta fresca"
        "verdura"  -> "🥗 Verdura"
        "cereal"   -> "🌾 Cereal / Grano"
        "lacteo"   -> "🥛 Lácteo"
        else       -> "🍽️ Platillo o elemento"
    }

    val foodBitmap: androidx.compose.ui.graphics.ImageBitmap? = null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgBase)
            .verticalScroll(rememberScrollState())
    ) {
        // ── Header con botón volver y chip de tiempo de comida ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(GreenLight.copy(0.7f), BgBase)))
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 12.dp)
        ) {
            val a11yMode = LocalAccessibilityMode.current
            val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

            FilledTonalIconButton(
                onClick  = onVolver,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(if (esAccesible) 64.dp else 40.dp),
                colors   = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = BgCard,
                    contentColor   = GreenPrimary
                )
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", modifier = Modifier.size(if (esAccesible) 30.dp else 20.dp))
            }

            // Chip superior (Inspirado en la imagen de referencia)
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(BgCard)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text       = mealChipText,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // ── Tarjeta Fotografía del Platillo (Inspirado en referencia) ──
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(22.dp),
            colors    = CardDefaults.cardColors(containerColor = BgCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column {
                if (foodBitmap != null) {
                    Image(
                        bitmap             = foodBitmap,
                        contentDescription = food.foodName,
                        modifier           = Modifier
                            .fillMaxWidth()
                            .height(230.dp)
                            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
                        contentScale       = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Brush.linearGradient(listOf(GreenLight, BgCardSoft))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.RiceBowl,
                            null,
                            tint = GreenMedium,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = food.foodName,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = TextPrimary,
                            modifier   = Modifier.weight(1f)
                        )

                        // Badge recomendación
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(recomBg as Color)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text       = recomLabel as String,
                                fontSize   = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color      = recomColor as Color
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))
                    Text(
                        text     = "Detección IA (${"%.0f".format(food.confidence * 100)}% certeza)",
                        fontSize = 12.sp,
                        color    = TextSecond
                    )
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Tarjeta de Resumen de Macronutrientes (Fila estilo Imagen de referencia) ──
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = BgCard),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                MacroStatColumn("${nutrition.calories.toInt()} kcal", "Calorías", AmberWarm)
                DividerVertical()
                MacroStatColumn("${"%.1f".format(nutrition.carbohydrates)} g", "Carbohidratos", Color(0xFF7B68EE))
                DividerVertical()
                MacroStatColumn("${"%.1f".format(nutrition.protein)} g", "Proteína", Color(0xFF20B2AA))
                DividerVertical()
                MacroStatColumn("${"%.1f".format(nutrition.fat)} g", "Grasas", RedSoft)
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Lista de Ingredientes / Desglose de Alimentos (Estilo Imagen de referencia) ──
        if (food.ingredients.isNotEmpty()) {
            Card(
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text       = "Ingredientes y componentes",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = TextPrimary
                    )
                    Spacer(Modifier.height(12.dp))

                    val approxCalPerItem = if (food.ingredients.isNotEmpty()) (nutrition.calories / food.ingredients.size).toInt() else 0

                    for ((index, ing) in food.ingredients.withIndex()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GreenLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text     = "${index + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color    = GreenPrimary
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    text       = ing.replaceFirstChar { it.uppercase() },
                                    fontSize   = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color      = TextPrimary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (approxCalPerItem > 0) {
                                    Text(
                                        text     = "~$approxCalPerItem kcal",
                                        fontSize = 13.sp,
                                        color    = TextSecond,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(8.dp))
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint               = TextHint,
                                    modifier           = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (index < food.ingredients.size - 1) {
                            HorizontalDivider(color = DividerColor, thickness = 0.8.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── Análisis Pediátrico ──
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = recomBg as Color),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ChildCare,
                        null,
                        tint     = recomColor as Color,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Recomendación para $targetNombre",
                        fontWeight = FontWeight.Bold,
                        color      = recomColor,
                        fontSize   = 14.sp
                    )
                }

                val portionToDisplay = if (analysis.recommendedPortion.isNotBlank()) analysis.recommendedPortion else if (analysis.recommended) "Porción pequeña adaptada para su edad" else "0g / No recomendado"
                Spacer(Modifier.height(10.dp))
                InfoRow(Icons.Outlined.DinnerDining, "Porción recomendada", portionToDisplay, recomColor as Color)
                if (analysis.frequency.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    InfoRow(Icons.Outlined.EventRepeat, "Frecuencia sugerida", analysis.frequency, recomColor as Color)
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Beneficios y Advertencias ──
        if (analysis.benefits.isNotEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = BgCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Beneficios principales", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    for (b in analysis.benefits) {
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(GreenMedium))
                            Spacer(Modifier.width(10.dp))
                            Text(b, fontSize = 13.sp, color = TextSecond, lineHeight = 18.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        if (analysis.warnings.isNotEmpty()) {
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = RedLight),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Advertencias de salud", fontWeight = FontWeight.Bold, color = RedSoft, fontSize = 14.sp)
                    Spacer(Modifier.height(10.dp))
                    for (w in analysis.warnings) {
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Box(modifier = Modifier.padding(top = 6.dp).size(6.dp).clip(CircleShape).background(RedSoft))
                            Spacer(Modifier.width(10.dp))
                            Text(w, fontSize = 13.sp, color = TextSecond, lineHeight = 18.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // ── Botones de Acción ──
        val a11yMode = LocalAccessibilityMode.current
        val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Button(
                onClick  = onGuardar,
                modifier = Modifier.fillMaxWidth().height(if (esAccesible) 70.dp else 52.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Outlined.BookmarkAdd, null, tint = Color.White, modifier = Modifier.size(if (esAccesible) 24.dp else 19.dp))
                Spacer(Modifier.width(8.dp))
                Text("Guardar en diario nutricional", fontWeight = FontWeight.Bold, fontSize = if (esAccesible) 16.sp else 15.sp, color = Color.White)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick  = onNuevo,
                modifier = Modifier.fillMaxWidth().height(if (esAccesible) 70.dp else 50.dp),
                shape    = RoundedCornerShape(16.dp),
                colors   = ButtonDefaults.outlinedButtonColors(contentColor = GreenPrimary),
                border   = androidx.compose.foundation.BorderStroke(1.5.dp, GreenPrimary.copy(alpha = 0.4f))
            ) {
                Icon(Icons.Outlined.CameraAlt, null, modifier = Modifier.size(if (esAccesible) 22.dp else 18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Analizar otro platillo", fontWeight = FontWeight.SemiBold, fontSize = if (esAccesible) 16.sp else 14.sp)
            }
        }
        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun MacroStatColumn(valor: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = valor,
            fontSize   = 16.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text     = label,
            fontSize = 11.sp,
            color    = TextSecond,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DividerVertical() {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(28.dp)
            .background(DividerColor)
    )
}

// ── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun MacroPill(valor: String, unidad: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier         = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(valor, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = color)
                Text(unidad, fontSize = 9.sp, color = color.copy(alpha = 0.65f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = TextHint)
    }
}

@Composable
private fun LeyendaDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = TextSecond)
    }
}

@Composable
private fun MiniNutriChip(modifier: Modifier, label: String, valor: String, color: Color) {
    Column(
        modifier            = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(valor, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
        Text(label, fontSize = 10.sp, color = TextSecond)
    }
}

@Composable
private fun ChipsIngredientes(items: List<String>) {
    var row  = mutableListOf<String>()
    val rows = mutableListOf<List<String>>()
    items.forEach { item ->
        row.add(item)
        if (row.size == 3) { rows.add(row.toList()); row = mutableListOf() }
    }
    if (row.isNotEmpty()) rows.add(row)

    rows.forEach { rowItems ->
        Row(
            modifier              = Modifier.padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            rowItems.forEach { item ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(BgCardSoft)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(item, fontSize = 12.sp, color = TextSecond)
                }
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, valor: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier         = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BgCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 10.sp, color = TextHint)
            Text(valor, fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// GUARDADO
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaGuardado(onNuevo: () -> Unit, onVolver: () -> Unit) {
    val scale by rememberInfiniteTransition(label = "saved").animateFloat(
        initialValue  = 0.96f,
        targetValue   = 1.04f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val a11yMode = LocalAccessibilityMode.current
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(BgBase)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier
                .size(96.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(GreenLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                null,
                tint     = GreenPrimary,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "¡Guardado!",
            fontSize   = 24.sp,
            fontWeight = FontWeight.Bold,
            color      = GreenPrimary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "El análisis fue guardado en el historial del perfil.",
            fontSize  = 14.sp,
            color     = TextSecond,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick   = onNuevo,
            modifier  = Modifier.fillMaxWidth().height(if (esAccesible) 70.dp else 52.dp),
            shape     = RoundedCornerShape(14.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Outlined.CameraAlt, null, tint = Color.White, modifier = Modifier.size(if (esAccesible) 24.dp else 18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Analizar otro alimento", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = if (esAccesible) 16.sp else 14.sp)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(
            onClick = onVolver,
            modifier = Modifier.height(if (esAccesible) 70.dp else 48.dp),
            colors  = ButtonDefaults.textButtonColors(contentColor = TextSecond)
        ) {
            Icon(Icons.Rounded.Home, null, modifier = Modifier.size(if (esAccesible) 22.dp else 16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Volver al inicio", fontSize = if (esAccesible) 16.sp else 14.sp)
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// ERROR
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun PantallaError(mensaje: String, onReintentar: () -> Unit) {
    val a11yMode = LocalAccessibilityMode.current
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .background(BgBase)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier         = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(RedLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                null,
                tint     = RedSoft,
                modifier = Modifier.size(50.dp)
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            "Algo salió mal",
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold,
            color      = RedSoft
        )
        Spacer(Modifier.height(8.dp))
        Text(
            mensaje,
            fontSize  = 13.sp,
            color     = TextSecond,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick   = onReintentar,
            modifier  = Modifier.fillMaxWidth().height(if (esAccesible) 70.dp else 52.dp),
            shape     = RoundedCornerShape(14.dp),
            colors    = ButtonDefaults.buttonColors(containerColor = GreenPrimary),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Icon(Icons.Outlined.Refresh, null, tint = Color.White, modifier = Modifier.size(if (esAccesible) 24.dp else 18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reintentar", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = if (esAccesible) 16.sp else 14.sp)
        }
    }
}