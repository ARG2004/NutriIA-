package com.example.nutriia.lactancia

import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.loc
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.accesibilidad.InputModoCiego
import com.example.nutriia.accesibilidad.VoiceInputManager
import com.example.nutriia.accesibilidad.VoiceInputState
import com.example.nutriia.accesibilidad.BrailleKeyboard
import com.example.nutriia.utils.FechaUtils

private val LactPink      = Color(0xFFEC9BBF)
private val LactPinkLight = Color(0xFFFCE4EC)
private val LactPinkDark  = Color(0xFFC2185B)
private val LactGreen     = Color(0xFF689F38)
private val LactDarkGreen = Color(0xFF33691E)
private val LactBg        = Color(0xFFFFF8FB)
private val LactCardWhite = Color.White
private val LactBlue      = Color(0xFF64B5F6)
private val LactOrange    = Color(0xFFFF8F00)

// ─── Spec de animación reutilizable ──────────────────────────────────────────
private val SpringSmooth = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness    = Spring.StiffnessLow
)
private val TweenStd = tween<Float>(durationMillis = 380, easing = EaseOutCubic)

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun LactanciaScreen(
    childId: String,
    childName: String,
    ageMonths: Int,
    a11yVm: AccessibilityViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    viewModel: LactanciaViewModel = viewModel()
) {
    // ─────────────────────────────────────────────────────────────────────────
    // ACCESIBILIDAD
    // ─────────────────────────────────────────────────────────────────────────
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esAccesible  = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE
    val esBlind      = a11yMode == AccessibilityMode.BLIND

    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    LaunchedEffect(childId) { viewModel.init(childId, ageMonths) }

    val todayLogs     by viewModel.todayLogs.collectAsState()
    val summary       by viewModel.summary.collectAsState()
    val omsRec        by viewModel.omsRec.collectAsState()
    val nextFeeding   by viewModel.nextFeedingIn.collectAsState()
    val uiState       by viewModel.uiState.collectAsState()
    val weekSummaries by viewModel.weeklySummaries.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showTipsSheet by remember { mutableStateOf(false) }
    var logToDelete   by remember { mutableStateOf<FeedingLog?>(null) }

    // ── Animación de entrada de pantalla ──────────────────────────────────────
    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }

    // Anuncio inicial de accesibilidad
    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar(
                loc(
                    "Módulo de lactancia para $childName. " +
                            "Aquí puedes registrar las tomas del día y ver recomendaciones de la OMS. " +
                            "El botón Registrar toma está en la parte inferior derecha.",
                    "Breastfeeding module for $childName. " +
                            "Here you can log daily feedings and view WHO recommendations. " +
                            "The Register feeding button is at the bottom right."
                )
            )
        }
    }

    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        when (uiState) {
            is LactanciaUiState.Saved   -> {
                if (esBlind) a11yVm.hablar(loc("Toma guardada correctamente.", "Feeding saved successfully."))
                snackbarHost.showSnackbar("Toma guardada ✓")
                viewModel.resetState()
            }
            is LactanciaUiState.Deleted -> {
                if (esBlind) a11yVm.hablar(loc("Toma eliminada.", "Feeding deleted."))
                snackbarHost.showSnackbar("Toma eliminada")
                viewModel.resetState()
            }
            is LactanciaUiState.Error   -> {
                val msg = (uiState as LactanciaUiState.Error).msg
                if (esBlind) a11yVm.hablar(loc("Error: $msg", "Error: $msg"))
                snackbarHost.showSnackbar(msg)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    LaunchedEffect(logToDelete) {
        logToDelete?.let { log ->
            if (esBlind) {
                a11yVm.hablar(loc(
                    "Eliminar toma. ¿Deseas eliminar la toma de las ${log.startTime}? Esta acción no se puede deshacer. Selecciona Eliminar o Cancelar.",
                    "Delete feeding. Do you want to delete the feeding from ${log.startTime}? This action cannot be undone. Select Delete or Cancel."
                ))
            }
        }
    }

    Scaffold(
        containerColor = LactBg,
        snackbarHost   = { SnackbarHost(snackbarHost) },
        floatingActionButton = {
            // ── FAB con animación de escala al entrar ─────────────────────────
            AnimatedVisibility(
                visible = screenVisible,
                enter   = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium
                    ),
                    initialScale = 0f
                ) + fadeIn(tween(300))
            ) {
                FloatingActionButton(
                    onClick        = {
                        if (esBlind) a11yVm.hablar(loc("Abriendo formulario para registrar toma.", "Opening form to register feeding."))
                        showAddDialog = true
                    },
                    containerColor = LactPink,
                    contentColor   = Color.White,
                    shape          = CircleShape,
                    modifier       = Modifier.shadow(10.dp, CircleShape)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Registrar toma", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { padding ->

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // ── TopBar con slide desde arriba ─────────────────────────────────
            item {
                AnimatedVisibility(
                    visible = screenVisible,
                    enter   = slideInVertically(tween(420, easing = EaseOutCubic)) { -it / 2 } + fadeIn(tween(420))
                ) {
                    LactanciaTopBar(childName, onNavigateBack) {
                        if (esBlind) a11yVm.hablar(loc("Mostrando consejos y recomendaciones de la OMS.", "Showing WHO tips and recommendations."))
                        showTipsSheet = true
                    }
                }
            }

            // ── Hero card con fade + slide desde abajo ────────────────────────
            item {
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(
                    visible = screenVisible,
                    enter   = slideInVertically(tween(480, delayMillis = 80, easing = EaseOutCubic)) { it / 3 } +
                            fadeIn(tween(480, delayMillis = 80))
                ) {
                    LactanciaHeroCard(summary, nextFeeding, omsRec)
                }
            }

            omsRec?.let { rec ->
                if (summary.totalSessions < rec.alertIfLessThan && todayLogs.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(12.dp))
                        AnimatedVisibility(
                            visible = true,
                            enter   = expandVertically(spring()) + fadeIn()
                        ) {
                            OmsAlertBanner(rec, summary.totalSessions)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = screenVisible,
                    enter   = fadeIn(tween(500, delayMillis = 160))
                ) {
                    DayStatsRow(summary)
                }
            }

            if (weekSummaries.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = screenVisible,
                        enter   = slideInVertically(tween(500, delayMillis = 200, easing = EaseOutCubic)) { it / 4 } +
                                fadeIn(tween(500, delayMillis = 200))
                    ) {
                        WeeklyBarChart(weekSummaries, omsRec)
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                SectionLabel("Tomas de hoy", Icons.Rounded.Schedule, LactPink)
            }

            if (todayLogs.isEmpty()) {
                item {
                    AnimatedVisibility(
                        visible = screenVisible,
                        enter   = fadeIn(tween(400, delayMillis = 300))
                    ) {
                        EmptyFeedingState()
                    }
                }
            } else {
                // ── Cards de toma con entrada escalonada (stagger) ────────────
                items(todayLogs, key = { it.id }) { log ->
                    val idx = todayLogs.indexOf(log)
                    FeedingLogCard(log = log, index = idx, esBlind = esBlind, idioma = idiomaActual) { logToDelete = log }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = screenVisible,
                    enter   = fadeIn(tween(400, delayMillis = 350))
                ) {
                    HungerCuesCard()
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = screenVisible,
                    enter   = fadeIn(tween(400, delayMillis = 400))
                ) {
                    OmsDisclaimerNote()
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Diálogos con animación de escala ─────────────────────────────────────
    if (showAddDialog) {
        if (esBlind) {
            AddFeedingBlindDialog(
                ttsManager = ttsManager,
                idioma = idiomaActual,
                onDismiss = {
                    a11yVm.hablar(loc("Registro cancelado.", "Registration cancelled."))
                    showAddDialog = false
                },
                onSave = { log ->
                    viewModel.saveFeeding(childId, log)
                    showAddDialog = false
                }
            )
        } else {
            AddFeedingDialog(
                esAccesible = esAccesible,
                esBlind = false,
                ttsManager = ttsManager,
                idioma = idiomaActual,
                onDismiss = {
                    showAddDialog = false
                },
                onSave = { log ->
                    viewModel.saveFeeding(childId, log)
                    showAddDialog = false
                }
            )
        }
    }

    if (showTipsSheet) {
        OmsTipsBottomSheet(
            omsRec    = omsRec,
            ageLabel  = omsRec?.ageLabel ?: "",
            esBlind   = esBlind,
            idioma    = idiomaActual,
            ttsManager = ttsManager,
            onDismiss = { showTipsSheet = false }
        )
    }

    logToDelete?.let { log ->
        AlertDialog(
            onDismissRequest = { logToDelete = null },
            title   = { Text("Eliminar toma", fontWeight = FontWeight.Bold) },
            text    = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFeeding(childId, log.id)
                    logToDelete = null
                }) { Text("Eliminar", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { logToDelete = null }) { Text("Cancelar", color = Color.Gray) }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LactanciaTopBar(childName: String, onBack: () -> Unit, onTips: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(LactPinkLight, LactBg)))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 48.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(LactCardWhite)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = LactPink)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Lactancia", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = LactPinkDark)
                Text(childName, fontSize = 13.sp, color = Color.Gray)
            }
            IconButton(onClick = onTips, modifier = Modifier.clip(CircleShape).background(LactCardWhite)) {
                Icon(Icons.Rounded.Lightbulb, null, tint = LactOrange)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HERO CARD — con progress animado
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun LactanciaHeroCard(
    summary: DailyFeedingSummary,
    nextFeeding: String,
    omsRec: OmsLactanciaRecommendation?
) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = LactCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.horizontalGradient(listOf(LactPink.copy(0.12f), Color.Transparent)))
                .padding(22.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier         = Modifier.size(48.dp).clip(CircleShape).background(LactPink.copy(0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.AccessTime, null, tint = LactPink, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text("Próxima toma", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text       = if (nextFeeding.isEmpty()) "Sin tomas registradas hoy" else nextFeeding,
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = LactPinkDark
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = LactPink.copy(0.15f))
                Spacer(Modifier.height(16.dp))

                omsRec?.let { rec ->
                    val targetFloat = Regex("\\d+").find(rec.feedingsPerDay)?.value?.toFloatOrNull() ?: 8f
                    val progressTarget = (summary.totalSessions.toFloat() / targetFloat.coerceAtLeast(1f)).coerceIn(0f, 1f)

                    // ── Animación del progreso circular ───────────────────────
                    val animatedProgress by animateFloatAsState(
                        targetValue   = progressTarget,
                        animationSpec = tween(900, easing = EaseOutCubic),
                        label         = "progress"
                    )
                    // ── Animación del contador de tomas ───────────────────────
                    val animatedSessions by animateIntAsState(
                        targetValue   = summary.totalSessions,
                        animationSpec = tween(700, easing = EaseOutCubic),
                        label         = "sessions"
                    )

                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Tomas hoy", fontSize = 12.sp, color = Color.Gray)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    "$animatedSessions",
                                    fontSize   = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color      = LactPinkDark
                                )
                                Text(" / ${rec.feedingsPerDay}", fontSize = 14.sp, color = Color.Gray,
                                    modifier = Modifier.padding(bottom = 6.dp))
                            }
                        }

                        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress    = { animatedProgress },
                                modifier    = Modifier.fillMaxSize(),
                                color       = if (summary.totalSessions >= rec.alertIfLessThan) LactGreen else LactPink,
                                trackColor  = LactPink.copy(0.12f),
                                strokeWidth = 6.dp
                            )
                            Text(
                                "${(animatedProgress * 100).toInt()}%",
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color      = LactPinkDark
                            )
                        }
                    }

                    if (summary.totalSessions > 0) {
                        Spacer(Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SideChip("Izq ${summary.leftSessions}",            LactPink,   Modifier.weight(1f))
                            SideChip("Der ${summary.rightSessions}",           LactBlue,   Modifier.weight(1f))
                            if (summary.formulaSessions > 0)
                                SideChip("Fórmula ${summary.formulaSessions}", LactOrange, Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SideChip(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(10.dp)).background(color.copy(0.12f))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ALERTA OMS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OmsAlertBanner(rec: OmsLactanciaRecommendation, totalToday: Int) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Warning, null, tint = LactOrange, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Tomas por debajo de lo recomendado", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE65100))
                Text("La OMS recomienda ${rec.feedingsPerDay} para esta etapa. Llevas $totalToday hoy.", fontSize = 12.sp, color = Color(0xFF5D4037))
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STATS DEL DÍA — con animación de número
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun DayStatsRow(summary: DailyFeedingSummary) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MiniStatCard("${summary.totalMinutes} min", "Tiempo total",     Icons.Rounded.Timer,       LactPink,   Modifier.weight(1f))
        MiniStatCard(
            if (summary.avgIntervalMinutes > 0) "${summary.avgIntervalMinutes} min" else "--",
            "Intervalo prom.", Icons.Rounded.Repeat, LactBlue, Modifier.weight(1f)
        )
        MiniStatCard(
            if (summary.totalFormulaMl > 0) "${summary.totalFormulaMl}ml" else "--",
            "Fórmula", Icons.Rounded.LocalDrink, LactOrange, Modifier.weight(1f)
        )
    }
}

@Composable
private fun MiniStatCard(value: String, label: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = LactCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(6.dp))
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 9.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GRÁFICA SEMANAL — barras con animación de altura
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun WeeklyBarChart(summaries: List<DailyFeedingSummary>, omsRec: OmsLactanciaRecommendation?) {
    val dayLabels = listOf("L", "M", "X", "J", "V", "S", "D")
    val maxVal    = summaries.maxOfOrNull { it.totalSessions }?.coerceAtLeast(1) ?: 1
    val target    = omsRec?.alertIfLessThan ?: 6

    // ── Trigger de animación al montar ────────────────────────────────────────
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animate = true }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = LactCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.BarChart, null, tint = LactPink, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Últimos 7 días", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = LactPinkDark)
            }
            Spacer(Modifier.height(16.dp))

            Row(
                modifier              = Modifier.fillMaxWidth().height(90.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.Bottom
            ) {
                summaries.takeLast(7).forEachIndexed { i, s ->
                    val rawFraction = s.totalSessions.toFloat() / maxVal
                    val isOk        = s.totalSessions >= target

                    // ── Cada barra se anima con delay escalonado ──────────────
                    val animFraction by animateFloatAsState(
                        targetValue   = if (animate) rawFraction.coerceIn(0.05f, 1f) else 0.05f,
                        animationSpec = tween(600, delayMillis = i * 60, easing = EaseOutCubic),
                        label         = "bar_$i"
                    )

                    Column(
                        modifier            = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text("${s.totalSessions}", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(animFraction)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(if (isOk) LactGreen else LactPink)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(dayLabels.getOrElse(i) { "" }, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(LactGreen))
                Spacer(Modifier.width(4.dp))
                Text("Meta OMS cumplida", fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.width(12.dp))
                Box(Modifier.size(8.dp).clip(CircleShape).background(LactPink))
                Spacer(Modifier.width(4.dp))
                Text("Por debajo", fontSize = 10.sp, color = Color.Gray)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LISTA DE TOMAS — stagger por índice
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(title: String, icon: ImageVector, color: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = LactPinkDark)
    }
}

@Composable
private fun EmptyFeedingState() {
    // ── Icono con pulso suave ─────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "emptyPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue  = 0.92f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "emptyScale"
    )

    Column(modifier = Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Rounded.ChildCare,
            contentDescription = null,
            tint     = LactPink.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp).graphicsLayer { scaleX = scale; scaleY = scale }
        )
        Spacer(Modifier.height(12.dp))
        Text("Sin tomas registradas hoy", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LactPinkDark)
        Spacer(Modifier.height(4.dp))
        Text("Toca el botón + para registrar la primera toma del día", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun FeedingLogCard(log: FeedingLog, index: Int, esBlind: Boolean = false, idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX, onDelete: () -> Unit) {
    val sideColor = when (log.side) {
        BreastSide.LEFT.name    -> LactPink
        BreastSide.RIGHT.name   -> LactBlue
        BreastSide.BOTH.name    -> LactGreen
        BreastSide.FORMULA.name -> LactOrange
        else                    -> LactPink
    }
    val sideIcon = when (log.side) {
        BreastSide.LEFT.name    -> Icons.AutoMirrored.Rounded.ArrowBack
        BreastSide.RIGHT.name   -> Icons.AutoMirrored.Rounded.ArrowForward
        BreastSide.BOTH.name    -> Icons.Rounded.SwapHoriz
        BreastSide.FORMULA.name -> Icons.Rounded.LocalDrink
        else                    -> Icons.Rounded.ChildCare
    }
    val sideLabel = BreastSide.entries.firstOrNull { it.name == log.side }?.label ?: log.side

    // ── Entrada escalonada por índice ─────────────────────────────────────────
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * 70L)
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter   = slideInHorizontally(tween(350, easing = EaseOutCubic)) { -60 } + fadeIn(tween(350))
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)
                .semantics(mergeDescendants = true) {
                    contentDescription = if (idioma == IdiomaVoz.INGLES) {
                        "Feeding $sideLabel at ${log.startTime}, duration ${log.durationMinutes} minutes. ${if(log.formulaMl > 0) "${log.formulaMl} milliliters." else ""} ${if(log.notes.isNotBlank()) "Note: ${log.notes}" else ""}"
                    } else {
                        "Toma de $sideLabel a las ${log.startTime}, duración ${log.durationMinutes} minutos. ${if(log.formulaMl > 0) "${log.formulaMl} mililitros." else ""} ${if(log.notes.isNotBlank()) "Nota: ${log.notes}" else ""}"
                    }
                },
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = LactCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(sideColor.copy(0.13f)), contentAlignment = Alignment.Center) {
                    Icon(sideIcon, null, tint = sideColor, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(sideLabel, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LactPinkDark)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(log.startTime, fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.width(10.dp))
                        Icon(Icons.Rounded.Timer, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("${log.durationMinutes} min", fontSize = 12.sp, color = Color.Gray)
                        if (log.formulaMl > 0) {
                            Spacer(Modifier.width(10.dp))
                            Text("${log.formulaMl} ml", fontSize = 12.sp, color = LactOrange, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (log.notes.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(log.notes, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color.LightGray)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DIÁLOGO
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AddFeedingDialog(
    esAccesible: Boolean = false,
    esBlind:     Boolean = false,
    ttsManager: NutriTTS? = null,
    idioma:     IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onDismiss:  () -> Unit,
    onSave:     (FeedingLog) -> Unit
) {
    var selectedSide by remember { mutableStateOf(BreastSide.LEFT) }
    var duration     by remember { mutableStateOf("") }
    var formulaMl    by remember { mutableStateOf("") }
    var notes        by remember { mutableStateOf("") }
    var timeInput by remember {
        val defaultVal = if (esBlind) "" else com.example.nutriia.utils.FechaUtils.horaActualIso()
        mutableStateOf(defaultVal)
    }
    val today = com.example.nutriia.utils.FechaUtils.hoyIso()

    fun loc(es: String, en: String) = idioma.loc(es, en)

    val guardarTodo = {
        val finalTime = if (timeInput.isNotBlank()) timeInput else com.example.nutriia.utils.FechaUtils.horaActualIso()
        if (esBlind) {
            ttsManager?.hablar(if (idioma == IdiomaVoz.INGLES) "Save" else "Guardar")
        }
        onSave(FeedingLog(date = today, startTime = finalTime, durationMinutes = duration.toIntOrNull() ?: 0,
            side = selectedSide.name, formulaMl = formulaMl.toIntOrNull() ?: 0, notes = notes))
    }

    LaunchedEffect(Unit) {
        if (esBlind) {
            ttsManager?.hablar(loc(
                "Formulario de registro de toma. Tipo seleccionado: izquierdo. El foco está en el campo de hora de inicio.",
                "Feeding registration form. Selected type: left. The focus is on the start time field."
            ))
        }
    }

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }

    val onCommandParsed: (String) -> Boolean = { cmd ->
        var handled = false
        val newSide = when {
            cmd.contains("pecho izquierdo") || cmd == "izquierdo" || cmd.contains("left breast") || cmd == "left" -> BreastSide.LEFT
            cmd.contains("pecho derecho") || cmd == "derecho" || cmd.contains("right breast") || cmd == "right" -> BreastSide.RIGHT
            cmd.contains("ambos pechos") || cmd == "pechos" || cmd.contains("ambos") || cmd.contains("both breasts") || cmd == "both" -> BreastSide.BOTH
            cmd.contains("fórmula") || cmd == "formula" -> BreastSide.FORMULA
            else -> null
        }
        if (newSide != null) {
            selectedSide = newSide
            if (campoActivo > 1) campoActivo = 1
            val feedback = loc("Seleccionado: ${newSide.label}", "Selected: ${newSide.label}")
            ttsManager?.hablar(feedback)
            handled = true
        }
        handled
    }
    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> timeInput
            1 -> duration
            2 -> if (selectedSide == BreastSide.FORMULA) formulaMl else notes
            3 -> notes
            else -> ""
        }
    }





    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null, tint = LactPink, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar toma", fontWeight = FontWeight.ExtraBold, color = LactPinkDark)
            }
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Tipo de toma", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BreastSide.entries.forEach { side ->
                        val selected = selectedSide == side
                        // ── Animación de color al seleccionar ─────────────────
                        val bgColor by animateColorAsState(
                            targetValue   = if (selected) LactPink else LactPink.copy(0.08f),
                            animationSpec = tween(200),
                            label         = "sideColor"
                        )
                        val sideIcon = when (side) {
                            BreastSide.LEFT    -> Icons.AutoMirrored.Rounded.ArrowBack
                            BreastSide.RIGHT   -> Icons.AutoMirrored.Rounded.ArrowForward
                            BreastSide.BOTH    -> Icons.Rounded.SwapHoriz
                            BreastSide.FORMULA -> Icons.Rounded.LocalDrink
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(bgColor)
                                .border(1.5.dp, if (selected) LactPinkDark else LactPink.copy(0.3f), RoundedCornerShape(12.dp))
                                .clickable { 
                                    selectedSide = side
                                    if (campoActivo > 1) campoActivo = 1
                                    if (esBlind) {
                                        ttsManager?.hablar(loc("Seleccionado: ${side.label}", "Selected: ${side.label}"))
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(sideIcon, null, tint = if (selected) Color.White else LactPinkDark, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.height(2.dp))
                                Text(side.label.split(" ").last(), fontSize = 9.sp,
                                    color = if (selected) Color.White else LactPinkDark,
                                    fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }

                if (esBlind) {
                    CampoTextoAccesible(
                        valor = timeInput,
                        onValorChange = { timeInput = it },
                        etiqueta = "Hora de inicio (HH:mm)",
                        descripcionVoz = if (idioma == IdiomaVoz.INGLES) "Tell me the feeding start time, for example: eight thirty in the morning, or fifteen thirty." else "Dime la hora de inicio de la toma, por ejemplo: ocho y media de la mañana, o quince treinta.",
                        ttsManager = ttsManager,
                        idioma = idioma,
                        colorPrimario = LactPink,
                        esCampoHora = true,
                        activo = campoActivo == 0,
                        onFocus = { campoActivo = 0 },
                        onNext = { campoActivo = 1 },
                        onCommandParsed = onCommandParsed
                    )
                    androidx.compose.animation.AnimatedVisibility(visible = !esBlind && esAccesible || campoActivo >= 1) {
                        CampoTextoAccesible(
                            valor = duration,
                            onValorChange = { duration = it },
                            etiqueta = "Duración (minutos)",
                            descripcionVoz = if (idioma == IdiomaVoz.INGLES) "Tell me the duration in minutes." else "Dime la duración en minutos.",
                            ttsManager = ttsManager,
                            idioma = idioma,
                            colorPrimario = LactPink,
                            activo = campoActivo == 1,
                            onFocus = { campoActivo = 1 },
                            onNext = { campoActivo = if (selectedSide == BreastSide.FORMULA) 2 else 2 },
                            onCommandParsed = onCommandParsed
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = selectedSide == BreastSide.FORMULA && (!esBlind && esAccesible || campoActivo >= 2)) {
                        CampoTextoAccesible(
                            valor = formulaMl,
                            onValorChange = { formulaMl = it },
                            etiqueta = "Cantidad (ml)",
                            descripcionVoz = if (idioma == IdiomaVoz.INGLES) "Tell me the amount of formula in milliliters." else "Dime la cantidad de fórmula en mililitros.",
                            ttsManager = ttsManager,
                            idioma = idioma,
                            colorPrimario = LactOrange,
                            activo = campoActivo == 2,
                            onFocus = { campoActivo = 2 },
                            onNext = { campoActivo = 3 },
                            onCommandParsed = onCommandParsed
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = (selectedSide == BreastSide.FORMULA && campoActivo >= 3) || (selectedSide != BreastSide.FORMULA && campoActivo >= 2)) {
                        CampoTextoAccesible(
                            valor = notes,
                            onValorChange = { notes = it },
                            etiqueta = "Notas (opcional)",
                            descripcionVoz = if (idioma == IdiomaVoz.INGLES) "All required fields complete. This note field is optional. Say your note, or say save to save." else "Todos los datos requeridos completos. Este campo de notas es opcional. Puedes dictar tu nota, o decir guardar para finalizar y guardar la toma.",
                            ttsManager = ttsManager,
                            idioma = idioma,
                            colorPrimario = LactPink,
                            activo = (selectedSide == BreastSide.FORMULA && campoActivo == 3) || (selectedSide != BreastSide.FORMULA && campoActivo == 2),
                            onNext = { guardarTodo() },
                            onCommandParsed = onCommandParsed
                        )
                    }
                } else {
                    OutlinedTextField(value = timeInput, onValueChange = { timeInput = it },
                        label = { Text("Hora de inicio") }, leadingIcon = { Icon(Icons.Rounded.AccessTime, null, tint = LactPink) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true, colors = dialogFieldColors())

                    OutlinedTextField(value = duration, onValueChange = { duration = it },
                        label = { Text("Duración (minutos)") }, leadingIcon = { Icon(Icons.Rounded.Timer, null, tint = LactPink) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = dialogFieldColors())

                    AnimatedVisibility(visible = selectedSide == BreastSide.FORMULA, enter = expandVertically(spring()) + fadeIn(), exit = shrinkVertically(spring()) + fadeOut()) {
                        OutlinedTextField(value = formulaMl, onValueChange = { formulaMl = it },
                            label = { Text("Cantidad (ml)") }, leadingIcon = { Icon(Icons.Rounded.LocalDrink, null, tint = LactOrange) },
                            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), colors = dialogFieldColors())
                    }

                    OutlinedTextField(value = notes, onValueChange = { notes = it },
                        label = { Text("Notas (opcional)") }, leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = LactPink) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), maxLines = 2, colors = dialogFieldColors())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { guardarTodo() },
                enabled = duration.isNotBlank() || selectedSide == BreastSide.FORMULA,
                colors  = ButtonDefaults.buttonColors(containerColor = LactPink),
                shape   = RoundedCornerShape(14.dp)
            ) { Text("Guardar toma", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) } }
    )
}

@Composable
private fun dialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LactPink, unfocusedBorderColor = Color.LightGray, focusedLabelColor = LactPink)

// ═══════════════════════════════════════════════════════════════════════════════
// BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmsTipsBottomSheet(
    omsRec: OmsLactanciaRecommendation?,
    ageLabel: String,
    esBlind: Boolean = false,
    idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    ttsManager: NutriTTS? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        if (esBlind) {
            if (idioma == IdiomaVoz.INGLES) {
                ttsManager?.hablar("WHO Tips. Swipe to read recommended frequency and scientific evidence.")
            } else {
                ttsManager?.hablar("Consejos de la OMS. Desliza para leer frecuencia recomendada y evidencia científica.")
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = LactBg,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
        Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 40.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(LactPink.copy(0.15f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lightbulb, null, tint = LactOrange, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Tips OMS verificados", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = LactPinkDark)
                    if (ageLabel.isNotBlank()) Text(ageLabel, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(20.dp))
            omsRec?.let { rec ->
                Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LactPink.copy(0.08f))) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Text("Frecuencia recomendada", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LactPinkDark)
                        Spacer(Modifier.height(8.dp))
                        FactRow("Tomas por día:", rec.feedingsPerDay)
                        FactRow("Intervalo mín:", "${rec.minIntervalHours}h")
                        FactRow("Intervalo máx:", "${rec.maxIntervalHours}h")
                        FactRow("Duración prom/toma:", "${rec.avgDurationMinutes} min")
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text("Evidencia científica OMS", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = LactPinkDark)
                Text("Fuente: WHO Infant and Young Child Feeding, 2023", fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.height(10.dp))
                rec.keyFacts.forEachIndexed { idx, fact ->
                    // ── Cada fact card con entrada escalonada ─────────────────
                    var factVisible by remember { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(idx * 80L)
                        factVisible = true
                    }
                    AnimatedVisibility(visible = factVisible, enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { 20 }) {
                        OmsFactCard(fact)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LactPinkDark)
    }
}

@Composable
private fun OmsFactCard(fact: String) {
    Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(LactCardWhite).padding(14.dp), verticalAlignment = Alignment.Top) {
        Icon(Icons.Rounded.CheckCircle, null, tint = LactGreen, modifier = Modifier.size(16.dp).padding(top = 1.dp))
        Spacer(Modifier.width(10.dp))
        Text(fact, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 19.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SEÑALES DE HAMBRE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun HungerCuesCard() {
    var expanded by remember { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = LactCardWhite), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded }, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChildCare, null, tint = LactPink, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Señales de hambre del bebé", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = LactPinkDark, modifier = Modifier.weight(1f))
                // ── Icono animado al expandir ──────────────────────────────────
                val rotation by animateFloatAsState(
                    targetValue   = if (expanded) 180f else 0f,
                    animationSpec = tween(250),
                    label         = "arrowRot"
                )
                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = Color.Gray, modifier = Modifier.graphicsLayer { rotationZ = rotation })
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(250)),
                exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(200))) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    fullnessCues.forEach { cue ->
                        Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = LactGreen, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(cue, fontSize = 13.sp, color = Color.DarkGray)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Fuente: UNICEF / WHO — Feeding your baby, 2023", fontSize = 10.sp, color = Color.LightGray)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// NOTA OMS CON LINKS RESPONSIVOS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun OmsDisclaimerNote() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape    = RoundedCornerShape(18.dp),
        colors   = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9))
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                Icons.Rounded.VerifiedUser, null,
                tint     = LactGreen,
                modifier = Modifier.size(20.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    "Información avalada por la OMS",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LactDarkGreen
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Las recomendaciones de este módulo están basadas en las guías oficiales de la " +
                            "OMS y UNICEF sobre alimentación infantil (2023). La lactancia exclusiva se " +
                            "recomienda los primeros 6 meses; continuar hasta los 2 años o más. " +
                            "Consulta siempre a tu pediatra.",
                    fontSize = 11.sp, color = Color(0xFF33691E), lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))

                // ── Fuente 1: WHO Infant and Young Child Feeding ──────────────
                LactOmsLinkRow(
                    label   = "WHO — Infant and Young Child Feeding, 2023",
                    url     = "https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding"
                )
                Spacer(Modifier.height(4.dp))

                // ── Fuente 2: UNICEF Early Childhood Nutrition ────────────────
                LactOmsLinkRow(
                    label   = "UNICEF — Early Childhood Nutrition, 2023",
                    url     = "https://www.unicef.org/nutrition/early-childhood-nutrition"
                )
            }
        }
    }
}

@Composable
private fun LactOmsLinkRow(label: String, url: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .clickable {
                com.example.nutriia.platform.openUrl(url)
            }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Rounded.OpenInNew,
            contentDescription = "Abrir fuente",
            tint     = LactGreen,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            label,
            fontSize       = 10.sp,
            color          = LactGreen,
            fontWeight     = FontWeight.Medium,
            textDecoration = TextDecoration.Underline
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODO PARA PERSONAS CIEGAS (BLIND MODE)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AddFeedingBlindDialog(
    ttsManager: NutriTTS? = null,
    idioma:     IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onDismiss:  () -> Unit,
    onSave:     (FeedingLog) -> Unit
) {
    var selectedSide by remember { mutableStateOf(BreastSide.LEFT) }
    var duration     by remember { mutableStateOf("") }
    var formulaMl    by remember { mutableStateOf("") }
    var notes        by remember { mutableStateOf("") }
    var timeInput    by remember { mutableStateOf("") }
    
    var campoActivo  by remember { mutableIntStateOf(0) } // 0: side, 1: time, 2: duration, 3: formula, 4: notes

    val today = FechaUtils.hoyIso()

    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    val guardarTodo = {
        val finalTime = if (timeInput.isNotBlank()) timeInput else FechaUtils.horaActualIso()
        ttsManager?.hablar(loc("Guardando toma.", "Saving feeding."))
        onSave(FeedingLog(
            date = today, 
            startTime = finalTime, 
            durationMinutes = if (selectedSide == BreastSide.FORMULA) 0 else (duration.toIntOrNull() ?: 0),
            side = selectedSide.name, 
            formulaMl = formulaMl.toIntOrNull() ?: 0, 
            notes = notes
        ))
    }

    // Guía inicial por voz
    LaunchedEffect(Unit) {
        ttsManager?.hablarYEsperar(loc(
            "Modo para personas ciegas activado. Formulario de registro de toma. " +
            "Primero, selecciona el tipo de toma. Las opciones son: izquierdo, derecho, ambos pechos o fórmula.",
            "Blind mode activated. Feeding registration form. " +
            "First, select the feeding type. Options are: left, right, both breasts, or formula."
        ), 1000L)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF181A20) // Fondo oscuro tipo Android
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, null, tint = LactPink, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = loc("Registrar toma", "Register feeding"),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = LactPink
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Selector de Tipo de Toma
                Text(
                    text = loc("Tipo de toma", "Feeding type"),
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BreastSide.entries.forEach { side ->
                        val isSelected = selectedSide == side
                        val sideIcon = when (side) {
                            BreastSide.LEFT    -> Icons.AutoMirrored.Rounded.ArrowBack
                            BreastSide.RIGHT   -> Icons.AutoMirrored.Rounded.ArrowForward
                            BreastSide.BOTH    -> Icons.Rounded.SwapHoriz
                            BreastSide.FORMULA -> Icons.Rounded.LocalDrink
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) LactPink else Color(0xFF262A34))
                                .border(1.dp, if (isSelected) LactPink else Color.Gray.copy(0.3f), RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedSide = side
                                    ttsManager?.hablar(loc("Seleccionado: ${side.label}", "Selected: ${side.label}"))
                                    if (campoActivo == 0) campoActivo = 1
                                }
                                .semantics { contentDescription = "Tipo ${side.label}. ${if(isSelected) "Seleccionado" else "Toca para seleccionar"}" },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(sideIcon, null, tint = if (isSelected) Color.White else LactPink, modifier = Modifier.size(20.dp))
                                Text(side.label.split(" ").last(), fontSize = 10.sp, color = if (isSelected) Color.White else LactPink, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                // Campo Dinámico con Voz / Teclado / Braille
                val currentEtiqueta = when(campoActivo) {
                    1 -> loc("Hora de inicio (HH:mm)", "Start time (HH:mm)")
                    2 -> if (selectedSide == BreastSide.FORMULA) loc("Cantidad (ml)", "Amount (ml)") else loc("Duración (minutos)", "Duration (minutes)")
                    3 -> loc("Notas (opcional)", "Notes (optional)")
                    else -> loc("Selecciona tipo", "Select type")
                }
                
                val currentDescVoz = when(campoActivo) {
                    1 -> loc("Di la hora de inicio, por ejemplo: ocho y media.", "Say the start time, for example: eight thirty.")
                    2 -> if (selectedSide == BreastSide.FORMULA) loc("Di la cantidad en mililitros.", "Say the amount in milliliters.") 
                         else loc("Di la duración en minutos.", "Say the duration in minutes.")
                    3 -> loc("Campo opcional. Di tu nota o di guardar para finalizar.", "Optional field. Say your note or say save to finish.")
                    else -> ""
                }

                if (campoActivo > 0) {
                    CampoTextoAccesible(
                        valor = when(campoActivo) {
                            1 -> timeInput
                            2 -> if (selectedSide == BreastSide.FORMULA) formulaMl else duration
                            3 -> notes
                            else -> ""
                        },
                        onValorChange = { v ->
                            when(campoActivo) {
                                1 -> timeInput = v
                                2 -> if (selectedSide == BreastSide.FORMULA) formulaMl = v else duration = v
                                3 -> notes = v
                            }
                        },
                        etiqueta = currentEtiqueta,
                        descripcionVoz = currentDescVoz,
                        ttsManager = ttsManager,
                        idioma = idioma,
                        colorPrimario = LactPink,
                        esCampoHora = campoActivo == 1,
                        keyboardOptions = KeyboardOptions(keyboardType = if (campoActivo == 1 || campoActivo == 2) KeyboardType.Number else KeyboardType.Text),
                        onNext = {
                            if (campoActivo == 3) {
                                guardarTodo()
                            } else {
                                campoActivo++
                            }
                        },
                        onCommandParsed = { cmd ->
                            if (cmd.contains("guardar") || cmd.contains("save")) {
                                guardarTodo()
                                true
                            } else false
                        }
                    )
                }

                Spacer(Modifier.height(40.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                    ) {
                        Text(loc("Cancelar", "Cancel"), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { guardarTodo() },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF262A34)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, LactPink.copy(0.3f))
                    ) {
                        Text(loc("Guardar toma", "Save feeding"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = LactPink)
                    }
                }
            }
        }
    }
}

