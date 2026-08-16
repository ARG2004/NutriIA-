package com.example.nutriia.alerta

import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.loc
import com.example.nutriia.shared.NutriaMascotaHeader
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.accesibilidad.VoiceInputManager
import com.example.nutriia.accesibilidad.VoiceInputState

// ═══════════════════════════════════════════════════════════════════════════════
// TOKENS DE DISEÑO — sistema Sol unificado con acento índigo/violeta
// ═══════════════════════════════════════════════════════════════════════════════
private object Sol {
    val Bg            = Color(0xFFFFF8F2)
    val White         = Color.White
    val Border        = Color(0xFFF0E6DE)
    // naranjas (consistencia con app)
    val Orange        = Color(0xFFE65100)
    val OrangeLight   = Color(0xFFFFE0B2)
    // índigo — acento propio de Alertas
    val Indigo        = Color(0xFF3949AB)
    val IndigoLight   = Color(0xFFE8EAF6)
    val IndigoDark    = Color(0xFF1A237E)
    val IndigoMid     = Color(0xFF5C6BC0)
    // semáforo
    val Red           = Color(0xFFE53935)
    val Green         = Color(0xFF2E7D32)
    // texto
    val TextPrimary   = Color(0xFF2D2D2D)
    val TextSecondary = Color(0xFF9E9E9E)
    val TextMuted     = Color(0xFF757575)
}

// ═══════════════════════════════════════════════════════════════════════════════
// ÁTOMOS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun IconBox(
    icon:   ImageVector,
    tint:   Color,
    bg:     Color,
    size:   Dp = 36.dp,
    iconSz: Dp = 18.dp,
    shape:  RoundedCornerShape = RoundedCornerShape(12.dp)
) = Box(Modifier.size(size).clip(shape).background(bg), Alignment.Center) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(iconSz))
}

@Composable
private fun Chip(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(10.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SolCard(
    modifier:    Modifier = Modifier,
    bg:          Color = Sol.White,
    border:      Color = Sol.Border,
    borderWidth: Dp = 1.dp,
    shape:       RoundedCornerShape = RoundedCornerShape(18.dp),
    content:     @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    shape    = shape,
    colors   = CardDefaults.cardColors(containerColor = bg),
    border   = BorderStroke(borderWidth, border),
    content  = content
)

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun AlertasScreen(
    childId:        String?,
    childName:      String?,
    onNavigateBack: () -> Unit,
    viewModel:      AlertaViewModel = viewModel()
) {
    // ─────────────────────────────────────────────────────────────────────────
    // ACCESIBILIDAD
    // ─────────────────────────────────────────────────────────────────────────
    val a11yVm: AccessibilityViewModel = viewModel()
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esBlind      = a11yMode == AccessibilityMode.BLIND
    val esMute       = a11yMode == AccessibilityMode.MUTE
    val esAccesible  = esBlind || esMute

    fun loc(es: String, en: String) = idiomaActual.loc(es, en)

    LaunchedEffect(Unit) {
        viewModel.init(childId)

        if (esBlind) {
            val nombreParaA11y = childName ?: loc("tu embarazo", "your pregnancy")
            a11yVm.hablar(loc(
                "Módulo de alertas para $nombreParaA11y. Puedes programar recordatorios de tipo: Toma o Comida, Vacuna, Cita Médica y Medición. El botón para programar una nueva alerta se encuentra en la parte inferior central de la pantalla.",
                "Alerts module for $nombreParaA11y. You can schedule reminders for: Feeding or Meal, Vaccine, Medical Appointment, and Measurement. The button to schedule a new alert is located at the bottom center of the screen."
            ))
        }
    }

    val alertas by viewModel.alertas.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var tabSeleccionada by remember { mutableStateOf<TipoAlerta?>(null) }
    var showAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showDialog      by remember { mutableStateOf(false) }
    var alertaAEliminar by remember { mutableStateOf<Alerta?>(null) }
    var alertaAEditar   by remember { mutableStateOf<Alerta?>(null) }
    var visible         by remember { mutableStateOf(false) }
    val snackbar        = remember { SnackbarHostState() }

    val checkAndRun: (() -> Unit) -> Unit = { action -> action() }

    // ── Voice Commands Logic ──────────────────────────────────────────────────
    var isListening by remember { mutableStateOf(false) }
    val voiceManager = remember { VoiceInputManager() }
    val voiceState by voiceManager.estado

    LaunchedEffect(isListening) {
        if (isListening && esBlind) {
            a11yVm.hablar(loc("Te escucho, dime qué sección abrir o si quieres crear una alerta.", "I'm listening, tell me which section to open or if you want to create an alert."))
            kotlinx.coroutines.delay(1500)
            voiceManager.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                isListening = false
                val cmd = result.lowercase(java.util.Locale.getDefault()).trim()
                when {
                    cmd.contains("todas") || cmd.contains("all") -> {
                        tabSeleccionada = null
                        a11yVm.hablar(loc("Mostrando todas las alertas.", "Showing all alerts."))
                    }
                    cmd.contains("toma") || cmd.contains("comida") || cmd.contains("feeding") -> {
                        tabSeleccionada = TipoAlerta.TOMA_COMIDA
                        a11yVm.hablar(loc("Sección toma o comida.", "Feeding or meal section."))
                    }
                    cmd.contains("vacuna") || cmd.contains("vaccine") -> {
                        tabSeleccionada = TipoAlerta.VACUNA
                        a11yVm.hablar(loc("Sección vacunas.", "Vaccines section."))
                    }
                    cmd.contains("cita") || cmd.contains("médica") || cmd.contains("medica") || cmd.contains("appointment") -> {
                        tabSeleccionada = TipoAlerta.CITA_MEDICA
                        a11yVm.hablar(loc("Sección citas médicas.", "Medical appointments section."))
                    }
                    cmd.contains("medición") || cmd.contains("medicion") || cmd.contains("measurement") -> {
                        tabSeleccionada = TipoAlerta.MEDICION
                        a11yVm.hablar(loc("Sección mediciones.", "Measurements section."))
                    }
                    cmd.contains("nueva") || cmd.contains("crear") || cmd.contains("new") || cmd.contains("create") || cmd.contains("agregar") || cmd.contains("add") -> {
                        checkAndRun {
                            alertaAEditar = null
                            showDialog = true
                        }
                    }
                    cmd.contains("volver") || cmd.contains("atrás") || cmd.contains("back") || cmd.contains("salir") -> {
                        onNavigateBack()
                    }
                    else -> a11yVm.hablar(loc("No entendí. Prueba con: todas, vacunas, citas o nueva alerta.", "I didn't understand. Try with: all, vaccines, appointments, or new alert."))
                }
            }
        }
    }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(showAlarmPermissionDialog) {
        if (showAlarmPermissionDialog && esBlind) {
            a11yVm.hablar(loc(
                "Permiso de Alarmas Exactas. Para garantizar que los recordatorios de comidas y medicamentos suenen exactamente a la hora programada, NutriIA requiere el permiso de Alarmas Exactas. ¿Deseas ir a la configuración para habilitarlo? Selecciona Configuración o Cancelar.",
                "Exact Alarms Permission. To guarantee that food and medication reminders sound exactly at the scheduled time, NutriIA requires the Exact Alarms permission. Do you want to go to settings to enable it? Select Settings or Cancel."
            ))
        }
    }

    LaunchedEffect(alertaAEliminar) {
        alertaAEliminar?.let { a ->
            if (esBlind) {
                a11yVm.hablar(loc(
                    "Eliminar alerta. ¿Deseas eliminar la alerta titulada ${a.titulo}? Se cancelará la notificación programada. Selecciona Eliminar o Cancelar.",
                    "Delete alert. Do you want to delete the alert titled ${a.titulo}? The scheduled notification will be cancelled. Select Delete or Cancel."
                ))
            }
        }
    }
    LaunchedEffect(uiState) {
        when (uiState) {
            is AlertaUiState.Saved   -> {
                if (esBlind) a11yVm.hablar(loc("Alerta guardada.", "Alert saved."))
                snackbar.showSnackbar("Alerta guardada ✓");  viewModel.resetState() 
            }
            is AlertaUiState.Deleted -> {
                if (esBlind) a11yVm.hablar(loc("Alerta eliminada.", "Alert removed."))
                snackbar.showSnackbar("Alerta eliminada");   viewModel.resetState() 
            }
            is AlertaUiState.Error   -> {
                val msg = (uiState as AlertaUiState.Error).msg
                if (esBlind) a11yVm.hablar(loc("Error: $msg", "Error: $msg"))
                snackbar.showSnackbar(msg); viewModel.resetState() 
            }
            else -> {}
        }
    }

    val alertasFiltradas = remember(alertas, tabSeleccionada) {
        if (tabSeleccionada == null) alertas else alertas.filter { it.tipo == tabSeleccionada }
    }

    Scaffold(
        containerColor = Sol.Bg,
        snackbarHost   = { SnackbarHost(snackbar) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (esBlind) {
                    FloatingActionButton(
                        onClick = { isListening = !isListening },
                        containerColor = if (voiceState == VoiceInputState.LISTENING) Color.Red else Sol.IndigoMid,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 8.dp).size(56.dp)
                            .semantics { contentDescription = if (voiceState == VoiceInputState.LISTENING) "Detener comandos de voz" else "Activar comandos de voz para navegación" }
                    ) {
                        Icon(if (voiceState == VoiceInputState.LISTENING) Icons.Rounded.Stop else Icons.Rounded.Mic, contentDescription = null)
                    }
                }
                AnimatedVisibility(
                    visible = visible,
                    enter   = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(300))
                ) {
                    ExtendedFloatingActionButton(
                        onClick        = { 
                            if (esBlind) a11yVm.hablar(loc("Abriendo formulario para nueva alerta.", "Opening form for new alert."))
                            checkAndRun {
                                alertaAEditar = null
                                showDialog = true
                            }
                        },
                        containerColor = Sol.Indigo,
                        contentColor   = Sol.White,
                        shape          = RoundedCornerShape(20.dp),
                        modifier       = Modifier.height(52.dp).shadow(
                            8.dp, RoundedCornerShape(20.dp),
                            ambientColor = Sol.Indigo.copy(.35f),
                            spotColor    = Sol.Indigo.copy(.35f)
                        )
                    ) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Nueva alerta", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {

            // TopBar
            AnimatedVisibility(
                visible = visible,
                enter   = slideInVertically(tween(400, easing = EaseOutCubic)) { -it / 2 } + fadeIn(tween(400))
            ) {
                AlertasTopBar(childName ?: "Mi Embarazo", alertas.size, onNavigateBack)
            }

            // Tabs
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(360, 80))) {
                AlertasTabs(tabSeleccionada, alertas) { 
                    tabSeleccionada = it 
                    if (esBlind) {
                        val label = it?.label ?: loc("Todas", "All")
                        a11yVm.hablar(loc("Sección $label", "Section $label"))
                    }
                }
            }

            // Lista
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp, top = 8.dp)
            ) {
                // Mascota
                item {
                    AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
                        MascotBanner(
                            drawableRes = 0,
                            titulo      = "Alertas y recordatorios",
                            subtitulo   = "Programa notificaciones para comidas,\nvacunas, citas y mediciones",
                            accentColor = Sol.Indigo
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                }
                if (alertasFiltradas.isEmpty()) {
                    item {
                        AnimatedVisibility(visible = visible, enter = fadeIn(tween(380, 120))) {
                            EstadoVacio(tabSeleccionada)
                        }
                    }
                } else if (tabSeleccionada == null) {
                    // Vista agrupada por tipo
                    TipoAlerta.entries.forEach { tipo ->
                        val grupo = alertasFiltradas.filter { it.tipo == tipo }
                        if (grupo.isNotEmpty()) {
                            item {
                                GrupoHeader(tipo)
                                Spacer(Modifier.height(6.dp))
                            }
                            items(grupo, key = { it.id }) { alerta ->
                                AlertaCard(
                                    alerta     = alerta,
                                    onToggle   = {
                                        checkAndRun {
                                            viewModel.toggleActiva(alerta)
                                        }
                                    },
                                    onEditar   = { 
                                        if (esBlind) a11yVm.hablar(loc("Editando ${alerta.titulo}", "Editing ${alerta.titulo}"))
                                        checkAndRun {
                                            alertaAEditar = alerta
                                            showDialog = true
                                        }
                                    },
                                    onEliminar = { alertaAEliminar = alerta }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                            item { Spacer(Modifier.height(6.dp)) }
                        }
                    }
                } else {
                    items(alertasFiltradas, key = { it.id }) { alerta ->
                        AlertaCard(
                            alerta     = alerta,
                            onToggle   = {
                                checkAndRun {
                                    viewModel.toggleActiva(alerta)
                                }
                            },
                            onEditar   = { 
                                if (esBlind) a11yVm.hablar(loc("Editando ${alerta.titulo}", "Editing ${alerta.titulo}"))
                                checkAndRun {
                                    alertaAEditar = alerta
                                    showDialog = true
                                }
                            },
                            onEliminar = { alertaAEliminar = alerta }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    // Diálogo crear/editar
    if (showDialog) {
        AlertaDialog(
            childId     = childId,
            childName   = childName,
            alertaEdit  = alertaAEditar,
            tipoInicial = tabSeleccionada,
            esBlind     = esBlind,
            esMute      = esMute,
            ttsManager  = ttsManager,
            idioma      = idiomaActual,
            onDismiss   = { 
                if (esBlind) a11yVm.hablar(loc("Registro cancelado.", "Registration cancelled."))
                showDialog = false; alertaAEditar = null 
            },
            onSave      = { nueva -> viewModel.guardar(nueva); showDialog = false; alertaAEditar = null }
        )
    }

    // Confirmar eliminación
    alertaAEliminar?.let { a ->
        AlertDialog(
            onDismissRequest = { alertaAEliminar = null },
            shape            = RoundedCornerShape(22.dp),
            icon  = { Icon(Icons.Rounded.DeleteOutline, null, tint = Sol.Red) },
            title = { Text("Eliminar alerta", fontWeight = FontWeight.Bold) },
            text  = { Text("¿Eliminar \"${a.titulo}\"? Se cancelará la notificación programada.") },
            confirmButton = {
                TextButton({ viewModel.eliminar(a); alertaAEliminar = null }) {
                    Text("Eliminar", color = Sol.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ alertaAEliminar = null }) { Text("Cancelar") } }
        )
    }

}

// ═══════════════════════════════════════════════════════════════════════════════
// MASCOT BANNER — misma firma que SolidosScreen / NutrientesScreen
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun MascotBanner(
    drawableRes: Int = 0,
    titulo:      String,
    subtitulo:   String,
    accentColor: Color = Sol.Indigo,
    mascotSize:  androidx.compose.ui.unit.Dp = 80.dp
) {
    val inf   = rememberInfiniteTransition(label = "mb")
    val float by inf.animateFloat(
        initialValue  = 0f,
        targetValue   = 8f,
        animationSpec = infiniteRepeatable(tween(2200, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "mbf"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(accentColor.copy(.08f), accentColor.copy(.03f))))
            .border(1.dp, accentColor.copy(.12f), RoundedCornerShape(24.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NutriaMascotaHeader(
                modifier = Modifier
                    .size(mascotSize)
                    .graphicsLayer { translationY = -float }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo,    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = accentColor, lineHeight = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitulo, fontSize = 12.sp, color = accentColor.copy(.7f), lineHeight = 17.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR — mismo patrón que SolidosScreen / NutrientesScreen
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AlertasTopBar(childName: String, totalAlertas: Int, onBack: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(Sol.IndigoLight, Sol.Bg))
    Box(
        Modifier.fillMaxWidth().background(gradient)
            .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 20.dp)
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Sol.White.copy(.8f)).align(Alignment.CenterStart)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Sol.Indigo)
        }

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Alertas",   fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Sol.Indigo)
            Text(childName,   fontSize = 14.sp, fontWeight = FontWeight.SemiBold,  color = Sol.IndigoMid)
            Spacer(Modifier.height(2.dp))
            Surface(shape = RoundedCornerShape(50.dp), color = Sol.Indigo.copy(.12f)) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.NotificationsActive, null, tint = Sol.Indigo, modifier = Modifier.size(11.dp))
                    Text("Recordatorios inteligentes", fontSize = 11.sp, color = Sol.Indigo, fontWeight = FontWeight.Medium)
                }
            }
        }

        Surface(shape = RoundedCornerShape(50.dp), color = Sol.White.copy(.85f), modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(
                "$totalAlertas ${if (totalAlertas == 1) "alerta" else "alertas"}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontSize = 11.sp, color = Sol.Indigo, fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = Sol.IndigoLight, thickness = 1.dp)
}

// ═══════════════════════════════════════════════════════════════════════════════
// TABS — mismo estilo que TabsSolidos pero con ScrollableTabRow
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AlertasTabs(
    seleccionada: TipoAlerta?,
    alertas:      List<Alerta>,
    onSelect:     (TipoAlerta?) -> Unit
) {
    val tabs: List<TipoAlerta?> = listOf(null) + TipoAlerta.entries

    Column {
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(seleccionada),
            containerColor   = Sol.Bg,
            contentColor     = Sol.Indigo,
            edgePadding      = 16.dp,
            indicator        = { tabPositions ->
                val idx = tabs.indexOf(seleccionada)
                if (idx in tabPositions.indices) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[idx]),
                        color    = seleccionada?.color ?: Sol.Indigo
                    )
                }
            },
            divider = {}
        ) {
            tabs.forEach { tipo ->
                val isSelected = seleccionada == tipo
                val count      = if (tipo == null) alertas.size else alertas.count { it.tipo == tipo }
                val color      = tipo?.color ?: Sol.Indigo

                Tab(selected = isSelected, onClick = { onSelect(tipo) }, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Row(
                        modifier              = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = tipo?.icon ?: Icons.Rounded.GridView,
                            contentDescription = null,
                            tint     = if (isSelected) color else Sol.TextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text       = tipo?.label ?: "Todas",
                            fontSize   = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) color else Sol.TextSecondary
                        )
                        if (count > 0) {
                            Box(
                                Modifier.clip(CircleShape)
                                    .background(if (isSelected) color else Sol.TextSecondary.copy(.25f))
                                    .padding(horizontal = 5.dp, vertical = 2.dp),
                                Alignment.Center
                            ) {
                                Text("$count", fontSize = 9.sp, color = if (isSelected) Sol.White else Sol.TextMuted, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
        HorizontalDivider(color = Sol.IndigoLight, thickness = 1.dp)
        Spacer(Modifier.height(4.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HEADER DE GRUPO
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun GrupoHeader(tipo: TipoAlerta) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBox(tipo.icon, tipo.color, tipo.color.copy(.15f), 28.dp, 14.dp, RoundedCornerShape(8.dp))
        Spacer(Modifier.width(10.dp))
        Text(tipo.label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Sol.IndigoDark)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TARJETA DE ALERTA — patrón AlimentoCard de SolidosScreen
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AlertaCard(
    alerta:     Alerta,
    onToggle:   () -> Unit,
    onEditar:   () -> Unit,
    onEliminar: () -> Unit
) {
    val color = alerta.tipo.color
    val alpha by animateFloatAsState(
        targetValue   = if (alerta.activa) 1f else 0.45f,
        animationSpec = tween(300),
        label         = "alpha_${alerta.id}"
    )
    SolCard(
        border = if (alerta.activa) color.copy(.22f) else Sol.Border,
        shape  = RoundedCornerShape(16.dp)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {

            // Ícono de tipo
            IconBox(
                icon   = alerta.tipo.icon,
                tint   = color.copy(alpha = alpha),
                bg     = color.copy(alpha * .12f),
                size   = 54.dp, iconSz = 28.dp,
                shape  = RoundedCornerShape(16.dp)
            )
            Spacer(Modifier.width(12.dp))

            // Contenido
            Column(Modifier.weight(1f)) {
                Text(alerta.titulo, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = Sol.TextPrimary.copy(alpha = alpha), maxLines = 1)
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Chip(Icons.Rounded.AccessTime, formatHora12h(alerta.hora), color.copy(alpha = alpha))
                    if (alerta.fechaUnica != null)
                        Chip(Icons.Rounded.CalendarToday, alerta.fechaUnica, Sol.TextMuted.copy(alpha = alpha))
                    else
                        Chip(Icons.Rounded.Repeat, alerta.diasSemana.joinToString("") { it.short }, Sol.TextMuted.copy(alpha = alpha))
                }
                if (alerta.descripcion.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(alerta.descripcion, fontSize = 11.sp, color = Sol.TextSecondary.copy(alpha = alpha), maxLines = 1)
                }
            }

            // Acciones
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Switch(
                    checked         = alerta.activa,
                    onCheckedChange = { onToggle() },
                    colors          = SwitchDefaults.colors(
                        checkedTrackColor   = color,
                        checkedThumbColor   = Sol.White,
                        uncheckedTrackColor = Color.LightGray.copy(.4f)
                    ),
                    modifier = Modifier.scale(.78f)
                )
                Row {
                    IconButton(onEditar,   Modifier.size(30.dp)) { Icon(Icons.Rounded.Edit,          null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(15.dp)) }
                    IconButton(onEliminar, Modifier.size(30.dp)) { Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(15.dp)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO VACÍO — mismo patrón que SolidosScreen
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun EstadoVacio(tipo: TipoAlerta?) {
    val inf = rememberInfiniteTransition(label = "ev")
    val sc  by inf.animateFloat(.94f, 1.06f,
        infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "evs")
    val icono = tipo?.icon ?: Icons.Rounded.NotificationsNone
    val color = tipo?.color ?: Sol.Indigo

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(color.copy(.08f))
                .graphicsLayer { scaleX = sc; scaleY = sc },
            Alignment.Center
        ) {
            Icon(icono, null, tint = color.copy(.5f), modifier = Modifier.size(40.dp))
        }
        Text(
            if (tipo == null) "Sin alertas aún" else "Sin alertas de ${tipo.label}",
            color = color, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, fontSize = 15.sp
        )
        Text(
            "Toca el botón de abajo para crear un recordatorio.",
            color = Sol.TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center, lineHeight = 17.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DIÁLOGO CREAR / EDITAR
// ═══════════════════════════════════════════════════════════════════════════════
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertaDialog(
    childId:     String?,
    childName:   String?,
    alertaEdit:  Alerta?,
    tipoInicial: TipoAlerta?,
    esBlind:     Boolean   = false,
    esMute:      Boolean   = false,
    ttsManager:  NutriTTS? = null,
    idioma:      IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onDismiss:   () -> Unit,
    onSave:      (Alerta) -> Unit
) {
    val esEdicion   = alertaEdit != null
    val esAccesible = esBlind || esMute

    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    LaunchedEffect(Unit) {
        if (esBlind) {
            val intro = if (esEdicion) {
                loc("Formulario de edición de alerta para el campo título.", "Alert editing form for title field.")
            } else {
                loc("Formulario de creación de nueva alerta para el campo título.", "New alert creation form for title field.")
            }
            ttsManager?.hablar(intro)
        }
    }

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }

    var titulo      by remember { mutableStateOf(alertaEdit?.titulo      ?: "") }
    var descripcion by remember { mutableStateOf(alertaEdit?.descripcion ?: "") }
    var tipo        by remember { mutableStateOf(alertaEdit?.tipo ?: tipoInicial ?: TipoAlerta.TOMA_COMIDA) }
    var hora        by remember { mutableStateOf(alertaEdit?.hora ?: "") }
    var diasSel     by remember { mutableStateOf(alertaEdit?.diasSemana  ?: DiasSemana.entries.toList()) }
    var fechaUnica  by remember { mutableStateOf(alertaEdit?.fechaUnica  ?: "") }
    var esUnica     by remember { mutableStateOf(alertaEdit?.fechaUnica  != null) }
    var showTimePicker by remember { mutableStateOf(false) }

    val onCommandParsed: (String) -> Boolean = { cmd ->
        var handled = false
        val clean = cmd.lowercase(java.util.Locale.getDefault()).trim()
        val newTipo = when {
            clean.contains("comida") || clean.contains("toma") || clean.contains("feeding") || clean == "food" -> TipoAlerta.TOMA_COMIDA
            clean.contains("vacuna") || clean == "vaccine" -> TipoAlerta.VACUNA
            clean.contains("cita") || clean.contains("pediatra") || clean.contains("appointment") -> TipoAlerta.CITA_MEDICA
            clean.contains("medición") || clean.contains("medicion") || clean.contains("crecimiento") || clean.contains("measurement") -> TipoAlerta.MEDICION
            else -> null
        }
        if (newTipo != null) {
            tipo = newTipo
            if (campoActivo > 1) campoActivo = 1
            val feedback = loc("Categoría seleccionada: ${newTipo.label}", "Category selected: ${newTipo.label}")
            ttsManager?.hablar(feedback)
            handled = true
        }
        
        val newField = when {
            clean.contains("ir a título") || clean.contains("ir a nombre") || clean.contains("go to title") -> 0
            clean.contains("ir a hora") || clean.contains("go to time") || clean.contains("go to hour") -> 1
            clean.contains("ir a descripción") || clean.contains("ir a nota") || clean.contains("go to description") || clean.contains("go to note") -> 2
            clean.contains("ir a fecha") || clean.contains("go to date") -> 3
            else -> null
        }
        if (newField != null) {
            campoActivo = newField
            val fieldName = when (newField) {
                0 -> loc("Título", "Title")
                1 -> loc("Hora", "Time")
                2 -> loc("Descripción", "Description")
                3 -> loc("Fecha", "Date")
                else -> ""
            }
            ttsManager?.hablar(loc("Moviendo a $fieldName", "Moving to $fieldName"))
            handled = true
        }
        handled
    }

    val guardarTodo = {
        if (titulo.isNotBlank()) {
            try {
                if (esBlind) {
                    ttsManager?.hablar(if (idioma == IdiomaVoz.INGLES) "Save" else "Guardar")
                }
                val horaSegura = if (hora.isNotBlank() && hora.contains(":")) hora else "08:00"
                onSave(Alerta(
                    id          = alertaEdit?.id       ?: java.util.UUID.randomUUID().toString(),
                    childId     = childId ?: "",
                    childName   = childName ?: "Mi Embarazo",
                    tipo        = tipo,
                    titulo      = titulo.trim(),
                    descripcion = descripcion.trim(),
                    hora        = horaSegura,
                    diasSemana  = if (esUnica) emptyList() else diasSel,
                    fechaUnica  = if (esUnica && fechaUnica.length == 10) fechaUnica else null,
                    activa      = alertaEdit?.activa   ?: true,
                    creadaEn    = alertaEdit?.creadaEn ?: System.currentTimeMillis()
                ))
            } catch (e: Exception) {
                android.util.Log.e("AlertaScreen", "Error al guardar alerta", e)
            }
        }
    }

    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> titulo
            1 -> hora
            2 -> descripcion
            3 -> fechaUnica
            else -> ""
        }
    }

    // Auto-avanzar solo en modo ciego (Voz). Desactivado en Modo Mudo (Señas).
    LaunchedEffect(titulo) {
        if (!esBlind || titulo.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (titulo == valorInicial) return@LaunchedEffect
        delay(2000L)
        if (titulo.isNotBlank() && campoActivo == 0 && titulo != valorInicial) {
            campoActivo = 1
        }
    }

    LaunchedEffect(hora) {
        if (!esBlind || hora.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (hora == valorInicial) return@LaunchedEffect
        if (!hora.matches(Regex("""\d{2}:\d{2}"""))) return@LaunchedEffect
        delay(2000L)
        if (hora.isNotBlank() && campoActivo == 1 && hora != valorInicial) {
            campoActivo = 2
        }
    }

    LaunchedEffect(descripcion) {
        if (!esBlind || descripcion.isBlank() || campoActivo != 2) return@LaunchedEffect
        if (descripcion == valorInicial) return@LaunchedEffect
        delay(2000L)
        if (descripcion.isNotBlank() && campoActivo == 2 && descripcion != valorInicial) {
            if (esUnica) {
                campoActivo = 3
            }
        }
    }

    val horaInicial   = hora.split(":").getOrElse(0) { "8" }.toIntOrNull()  ?: 8
    val minutoInicial = hora.split(":").getOrElse(1) { "00" }.toIntOrNull() ?: 0
    val timeState = rememberTimePickerState(
        initialHour   = horaInicial,
        initialMinute = minutoInicial,
        is24Hour      = false
    )

    // Colores de campo unificados por tipo seleccionado
    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = tipo.color,
        unfocusedBorderColor = Sol.Border,
        focusedLabelColor    = tipo.color,
        cursorColor          = tipo.color
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape            = RoundedCornerShape(28.dp),
        containerColor   = Sol.Bg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(
                    icon   = if (esEdicion) Icons.Rounded.Edit else Icons.Rounded.NotificationAdd,
                    tint   = tipo.color,
                    bg     = tipo.color.copy(.1f),
                    size   = 34.dp, iconSz = 18.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    if (esEdicion) loc("Editar alerta", "Edit alert") else loc("Nueva alerta", "New alert"),
                    fontWeight = FontWeight.ExtraBold, color = Sol.IndigoDark, fontSize = 17.sp
                )
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {

                // Selector de tipo
                item {
                    Text(loc("Categoría", "Category"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Sol.TextMuted)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TipoAlerta.entries.forEach { t ->
                            val sel = tipo == t
                            val bg  by animateColorAsState(if (sel) t.color.copy(.15f) else Color(0xFFF5F5F5), tween(180), label = "tb_${t.name}")
                            Box(
                                Modifier.weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bg)
                                    .border(if (sel) 1.5.dp else 0.dp, t.color, RoundedCornerShape(14.dp))
                                    .clickable { 
                                        tipo = t
                                        if (campoActivo > 1) campoActivo = 1
                                        if (esBlind) ttsManager?.hablar(loc("Seleccionado: ${t.label}", "Selected: ${t.label}"))
                                    }
                                    .padding(vertical = 12.dp),
                                Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(t.icon, null, tint = if (sel) t.color else Sol.TextSecondary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.height(3.dp))
                                    Text(t.label.split(" ").first(), fontSize = 9.sp,
                                        color = if (sel) t.color else Sol.TextSecondary,
                                        fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }

                // Título
                item {
                    if (esAccesible) {
                        CampoTextoAccesible(
                            valor          = titulo,
                            onValorChange  = { titulo = it },
                            etiqueta       = loc("Título de la alerta", "Alert title"),
                            descripcionVoz = loc("Di el nombre de este recordatorio.", "Say the name of this reminder."),
                            ttsManager     = ttsManager,
                            idioma         = idioma,
                            colorPrimario  = tipo.color,
                            activo         = campoActivo == 0,
                            onFocus        = { campoActivo = 0 },
                            onNext         = { campoActivo = 1 },
                            onCommandParsed = onCommandParsed
                        )
                    } else {
                        OutlinedTextField(
                            value         = titulo, onValueChange = { titulo = it },
                            label         = { Text("Título") },
                            placeholder   = {
                                Text(when (tipo) {
                                    TipoAlerta.TOMA_COMIDA -> "Ej. Toma de las 8am"
                                    TipoAlerta.VACUNA      -> "Ej. Vacuna Pentavalente"
                                    TipoAlerta.CITA_MEDICA -> "Ej. Cita con el pediatra"
                                    TipoAlerta.MEDICION    -> "Ej. Pesar a $childName"
                                })
                            },
                            leadingIcon   = { Icon(tipo.icon, null, tint = tipo.color, modifier = Modifier.size(18.dp)) },
                            modifier      = Modifier.fillMaxWidth(),
                            shape         = RoundedCornerShape(14.dp),
                            singleLine    = true,
                            colors        = fc
                        )
                    }
                }

                // Descripción
                if (!esBlind || campoActivo >= 2) {
                    item {
                        if (esAccesible) {
                            CampoTextoAccesible(
                                valor          = descripcion,
                                onValorChange  = { descripcion = it },
                                etiqueta       = loc("Nota opcional", "Optional note"),
                                descripcionVoz = if (esUnica) {
                                    loc("Di la descripción del recordatorio.", "Say the reminder description.")
                                } else {
                                    loc("Campos completos. Di la descripción, o di guardar alerta para que lo guarde.", "Fields complete. Say the description, or say save alert to save it.")
                                },
                                ttsManager     = ttsManager,
                                idioma         = idioma,
                                colorPrimario  = tipo.color,
                                activo         = campoActivo == 2,
                                onFocus        = { campoActivo = 2 },
                                onNext         = {
                                    if (esUnica) {
                                        campoActivo = 3
                                    } else {
                                        guardarTodo()
                                    }
                                },
                                onCommandParsed = onCommandParsed
                            )
                        } else {
                            OutlinedTextField(
                                value         = descripcion, onValueChange = { descripcion = it },
                                label         = { Text("Nota (opcional)") },
                                leadingIcon   = { Icon(Icons.AutoMirrored.Rounded.Notes, null, tint = Sol.TextMuted, modifier = Modifier.size(18.dp)) },
                                modifier      = Modifier.fillMaxWidth(),
                                shape         = RoundedCornerShape(14.dp),
                                maxLines      = 2,
                                colors        = fc
                            )
                        }
                    }
                }

                // Hora
                if (!esBlind || campoActivo >= 1) {
                    item {
                    if (esAccesible) {
                        CampoTextoAccesible(
                            valor          = hora,
                            onValorChange  = { hora = it },
                            etiqueta       = loc("Hora (HH:mm)", "Time (HH:mm)"),
                            descripcionVoz = loc("Dime la hora del recordatorio, por ejemplo: ocho y media de la mañana, o quince treinta.", "Tell me the reminder time, for example: eight thirty in the morning, or fifteen thirty."),
                            ttsManager     = ttsManager,
                            idioma         = idioma,
                            esCampoHora    = true,
                            colorPrimario  = tipo.color,
                            activo         = campoActivo == 1,
                            onFocus        = { campoActivo = 1 },
                            onNext         = { campoActivo = 2 },
                            onCommandParsed = onCommandParsed
                        )
                        } else {
                            Text(loc("Hora de la alerta", "Alert time"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Sol.TextMuted)
                            Spacer(Modifier.height(6.dp))
                            OutlinedCard(
                                onClick  = { showTimePicker = true },
                                shape    = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border   = BorderStroke(1.dp, tipo.color.copy(.4f))
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconBox(Icons.Rounded.AccessTime, tipo.color, tipo.color.copy(.1f), 32.dp, 16.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Text(formatHora12h(hora), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Sol.IndigoDark)
                                    Spacer(Modifier.weight(1f))
                                    Chip(
                                        Icons.Rounded.Schedule,
                                        if ((hora.split(":")[0].toIntOrNull() ?: 0) < 12) "AM" else "PM",
                                        tipo.color
                                    )
                                }
                            }
                        }
                    }
                }

                // Fecha única / repetitiva
                item {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBox(Icons.Rounded.DateRange, Sol.IndigoDark, Sol.IndigoLight, 28.dp, 14.dp, RoundedCornerShape(8.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(loc("Fecha específica", "Specific date"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Sol.IndigoDark)
                        }
                        Switch(
                            checked         = esUnica,
                            onCheckedChange = { esUnica = it },
                            colors          = SwitchDefaults.colors(checkedTrackColor = tipo.color, checkedThumbColor = Sol.White)
                        )
                    }
                }

                if (esUnica && (!esBlind || campoActivo >= 3)) {
                    item {
                        if (esAccesible) {
                            CampoTextoAccesible(
                                valor          = fechaUnica,
                                onValorChange  = { if (it.length <= 10) fechaUnica = it },
                                etiqueta       = loc("Fecha", "Date"),
                                descripcionVoz = loc("Campos completos. Di la fecha, o di guardar alerta para que lo guarde.", "Fields complete. Say the date, or say save alert to save it."),
                                ttsManager     = ttsManager,
                                idioma         = idioma,
                                esCampoFecha   = true,
                                colorPrimario  = tipo.color,
                                activo         = campoActivo == 3,
                                onFocus        = { campoActivo = 3 },
                                onNext         = { guardarTodo() },
                                onCommandParsed = onCommandParsed
                            )
                        } else {
                            OutlinedTextField(
                                value         = fechaUnica,
                                onValueChange = { if (it.length <= 10) fechaUnica = it },
                                label         = { Text("Fecha (DD/MM/AAAA)") },
                                leadingIcon   = { Icon(Icons.Rounded.CalendarToday, null, tint = tipo.color, modifier = Modifier.size(18.dp)) },
                                placeholder   = { Text("DD/MM/AAAA") },
                                modifier      = Modifier.fillMaxWidth(),
                                shape         = RoundedCornerShape(14.dp),
                                singleLine    = true,
                                colors        = fc
                            )
                        }
                    }
                } else {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Repeat, null, tint = Sol.TextMuted, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(loc("Repetir los días", "Repeat days"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Sol.TextMuted)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            DiasSemana.entries.forEach { dia ->
                                val sel   = diasSel.contains(dia)
                                val color = if (sel) tipo.color else Color.LightGray
                                Box(
                                    Modifier.size(36.dp).clip(CircleShape)
                                        .background(if (sel) tipo.color.copy(.12f) else Color(0xFFF0F0F0))
                                        .border(1.5.dp, color.copy(if (sel) .6f else .3f), CircleShape)
                                        .clickable { 
                                            diasSel = if (sel) diasSel - dia else diasSel + dia 
                                            if (esBlind) ttsManager?.hablar(loc("${if (sel) "Quitado" else "Agregado"}: ${dia.label}", "${if (sel) "Removed" else "Added"}: ${dia.label}"))
                                        },
                                    Alignment.Center
                                ) {
                                    Text(dia.short, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = color)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { guardarTodo() },
                enabled  = titulo.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = tipo.color),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(loc("Guardar", "Save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(loc("Cancelar", "Cancel"), color = Sol.TextMuted) } }
    )

    // TimePicker dialog
    if (showTimePicker) {
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton({
                    hora = "%02d:%02d".format(timeState.hour, timeState.minute)
                    showTimePicker = false
                }) { Text("OK", color = tipo.color, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton({ showTimePicker = false }) { Text(loc("Cancelar", "Cancel"), color = Sol.TextMuted) }
            }
        ) {
            TimePicker(
                state    = timeState,
                colors   = TimePickerDefaults.colors(
                    clockDialColor                       = tipo.color.copy(.08f),
                    selectorColor                        = tipo.color,
                    timeSelectorSelectedContainerColor   = tipo.color,
                    timeSelectorUnselectedContainerColor = Color(0xFFF5F5F5)
                ),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

/** "14:30" → "2:30 PM"  |  "08:05" → "8:05 AM" */
private fun formatHora12h(hora24: String): String {
    val hh  = hora24.split(":").getOrElse(0) { "8" }.toIntOrNull()  ?: 8
    val mm  = hora24.split(":").getOrElse(1) { "00" }.toIntOrNull() ?: 0
    val h12 = when { hh == 0 -> 12; hh > 12 -> hh - 12; else -> hh }
    return "%d:%02d %s".format(h12, mm, if (hh < 12) "AM" else "PM")
}
