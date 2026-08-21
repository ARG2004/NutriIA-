package com.example.nutriia.solidos

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.shared.NutriSharedViewModel
import com.example.nutriia.util.CalendarEvent
import com.example.nutriia.util.PlatformCalendarManager
import com.example.nutriia.resources.*
import com.example.nutriia.accesibilidad.InputModoCiego
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.PerfilSaludNino
import com.example.nutriia.sueldo.RecetaMexicana
import com.example.nutriia.sueldo.TipoComida
import com.example.nutriia.utils.FechaUtils

// ═══════════════════════════════════════════════════════════════════════════════
// TOKENS DE DISEÑO — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
private object Sol {
    val Bg            = Color(0xFFFFF8F2)
    val Orange        = Color(0xFFE65100)
    val OrangeLight   = Color(0xFFFFE0B2)
    val OrangeMid     = Color(0xFFFF8F00)
    val Green         = Color(0xFF558B2F)
    val GreenLight    = Color(0xFFDCEDC8)
    val DarkGreen     = Color(0xFF33691E)
    val White         = Color.White
    val Red           = Color(0xFFE53935)
    val Purple        = Color(0xFF7B1FA2)
    val Brown         = Color(0xFF6D4C41)
    val Border        = Color(0xFFF0E6DE)
    val TextPrimary   = Color(0xFF2D2D2D)
    val TextSecondary = Color(0xFF9E9E9E)
    val TextMuted     = Color(0xFF757575)
}

// ═══════════════════════════════════════════════════════════════════════════════
// ÁTOMOS — sin cambios
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

@Composable
private fun CollapseCard(
    bg:         Color = Sol.White,
    border:     Color = Sol.Border,
    arrowColor: Color = Sol.Orange,
    header:     @Composable RowScope.() -> Unit,
    content:    @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "collapseRot")
    SolCard(bg = bg, border = border) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                header()
                Icon(
                    Icons.Rounded.KeyboardArrowDown, null, tint = arrowColor,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rot }
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                exit  = shrinkVertically() + fadeOut(tween(180))
            ) { Column(Modifier.padding(top = 12.dp)) { content() } }
        }
    }
}

@Composable
private fun MascotBanner(
    drawableRes: org.jetbrains.compose.resources.DrawableResource,
    titulo:      String,
    subtitulo:   String,
    accentColor: Color = Sol.Orange
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
            Image(
                painter            = org.jetbrains.compose.resources.painterResource(drawableRes),
                contentDescription = null,
                modifier           = Modifier.size(110.dp).graphicsLayer { translationY = -float }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo,    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = accentColor, lineHeight = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitulo, fontSize = 12.sp, color = accentColor.copy(.7f),    lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun SeccionLabel(texto: String, icon: ImageVector) =
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = Sol.Orange, modifier = Modifier.size(13.dp))
        Text(texto.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sol.Orange, letterSpacing = 0.8.sp)
    }

@Composable
private fun ComidaRow(tipo: String, desc: String, icon: ImageVector, color: Color) =
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        IconBox(icon, color, color.copy(.12f), 26.dp, 13.dp, RoundedCornerShape(8.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(tipo, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.3.sp)
            Text(desc, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)
        }
    }

private fun reaccionColor(r: ReaccionAlimento) = when (r) {
    ReaccionAlimento.ALERGIA -> Sol.Red
    ReaccionAlimento.LEVE    -> Sol.OrangeMid
    ReaccionAlimento.RECHAZO -> Color(0xFF9E9E9E)
    else                     -> Sol.Green
}

// ═══════════════════════════════════════════════════════════════════════════════
// EXPORTAR PLAN — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
private fun exportarPlanComoTexto(
    plan:      List<PlanSemanalSolidos>,
    childName: String,
    ageMonths: Int
): String {
    val sb = StringBuilder()
    sb.appendLine("📅 Plan semanal de alimentación")
    sb.appendLine("👶 $childName · $ageMonths meses")
    sb.appendLine("─".repeat(32))
    plan.forEach { dia ->
        sb.appendLine()
        sb.appendLine("▸ ${dia.diaSemana.uppercase()}")
        if (dia.porcionLabel.isNotBlank()) sb.appendLine("  Porción: ${dia.porcionLabel}")
        if (dia.texturaLabel.isNotBlank()) sb.appendLine("  Textura: ${dia.texturaLabel}")
        sb.appendLine("  🌅 Desayuno:  ${dia.desayuno}")
        sb.appendLine("  🍽 Almuerzo:  ${dia.almuerzo}")
        sb.appendLine("  🥤 Merienda:  ${dia.merienda}")
        if (dia.colacion2.isNotBlank()) sb.appendLine("  ☕ Col. tarde: ${dia.colacion2}")
        sb.appendLine("  🌙 Cena:      ${dia.cena}")
    }
    sb.appendLine()
    sb.appendLine("─".repeat(32))
    sb.appendLine("Generado con NutriIA · basado en guías OMS")
    return sb.toString()
}

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
fun SolidosScreen(
    uid:            String,
    childId:        String,
    childName:      String,
    ageMonths:      Int,
    onNavigateBack: () -> Unit,
    sharedVm:       NutriSharedViewModel,
    a11yVm:         AccessibilityViewModel = viewModel(),
    viewModel:      AlimentacionViewModel = viewModel()
) {
    // ─────────────────────────────────────────────────────────────────────────
    // ACCESIBILIDAD
    // ─────────────────────────────────────────────────────────────────────────
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esAccesible  = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE
    val esBlind      = a11yMode == AccessibilityMode.BLIND

    fun loc(es: String, en: String) =
        if (idiomaActual == IdiomaVoz.INGLES) en else es

    // ─────────────────────────────────────────────────────────────────────────
    // Estados del ViewModel
    // ─────────────────────────────────────────────────────────────────────────
    val perfilSalud           by sharedVm.perfilSalud.collectAsState()
    val alimentosIntroducidos by viewModel.alimentosIntroducidos.collectAsState()
    val recetasFiltradas      by viewModel.recetasFiltradas.collectAsState()
    val alimentosConAlergia   by viewModel.alimentosConAlergiaNino.collectAsState()
    val planSemanal           by viewModel.planSemanalDesdeRegistrados.collectAsState()
    val alertasAlergenos      by viewModel.alertasAlergenos.collectAsState()
    val uiState               by viewModel.uiState.collectAsState()
    val busquedaReceta        by viewModel.busquedaReceta.collectAsState()
    val filtroTipo            by viewModel.filtroTipoReceta.collectAsState()
    val alergenosNino         by viewModel.alergenosNino.collectAsState()

    
    LaunchedEffect(uid, childId, ageMonths) { viewModel.init(uid, childId, ageMonths, sharedVm) }

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar(
                loc(
                    "Módulo de alimentación para $childName. " +
                            "Aquí puedes registrar alimentos introducidos, ver el plan semanal y explorar recetas. " +
                            "El botón Registrar alimento está en la parte inferior central. " +
                            "Las tres secciones son: Registrados, Plan semanal y Recetas.",
                    "Food module for $childName. " +
                            "Here you can log introduced foods, view the weekly plan, and browse recipes. " +
                            "The Register food button is at the bottom center. " +
                            "The three sections are: Registered, Weekly plan, and Recipes."
                )
            )
        }
    }

    var tab         by remember { mutableIntStateOf(0) }
    var showAgregar by remember { mutableStateOf(false) }
    var aEliminar   by remember { mutableStateOf<AlimentoIntroducido?>(null) }
    var aReaccion   by remember { mutableStateOf<AlimentoIntroducido?>(null) }
    var visible     by remember { mutableStateOf(false) }
    val snackbar        = remember { SnackbarHostState() }
    val scope           = rememberCoroutineScope()

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(uiState) {
        when (uiState) {
            is AlimentacionUiState.Saved -> {
                if (esBlind) a11yVm.hablar(
                    loc("Alimento guardado correctamente.", "Food saved successfully.")
                )
                snackbar.showSnackbar("Alimento guardado ✓")
                viewModel.resetState()
            }
            is AlimentacionUiState.Deleted -> {
                if (esBlind) a11yVm.hablar(
                    loc("Alimento eliminado.", "Food removed.")
                )
                snackbar.showSnackbar("Eliminado")
                viewModel.resetState()
            }
            is AlimentacionUiState.Error -> {
                val msg = (uiState as AlimentacionUiState.Error).msg
                if (esBlind) a11yVm.hablar(loc("Error: $msg", "Error: $msg"))
                snackbar.showSnackbar(msg)
                viewModel.resetState()
            }
            else -> {}
        }
    }

    val onExportarPlan: () -> Unit = {
        val texto = exportarPlanComoTexto(planSemanal, childName, ageMonths)
        com.example.nutriia.platform.openUrl("copy:$texto")
        scope.launch { snackbar.showSnackbar("Plan generado ✓") }
    }
    val onExportarCalendario: () -> Unit = {
        val events = mutableListOf<CalendarEvent>()
        val tz = TimeZone.currentSystemDefault()
        val hoy = Clock.System.now().toLocalDateTime(tz)

        planSemanal.forEachIndexed { index, dia ->
            val fechaDia = hoy.date.plus(index, DateTimeUnit.DAY).atTime(8, 0).toInstant(tz)
            
            val descripcion = "Desayuno: ${dia.desayuno}\nAlmuerzo: ${dia.almuerzo}\nMerienda: ${dia.merienda}\nCena: ${dia.cena}"

            events.add(CalendarEvent(
                title = "Alimentación $childName - ${dia.diaSemana}",
                description = descripcion,
                startDate = fechaDia.toEpochMilliseconds(),
                endDate = fechaDia.toEpochMilliseconds() + 3600000, // 1 hora
                allDay = true
            ))
        }

        PlatformCalendarManager.addEvents(events) { exito ->
            if (exito) {
                scope.launch { snackbar.showSnackbar("Plan exportado al calendario con éxito") }
            } else {
                scope.launch { snackbar.showSnackbar("No se pudo exportar al calendario. Revisa los permisos.") }
            }
        }
    }

    Scaffold(
        containerColor = Sol.Bg,
        snackbarHost   = { SnackbarHost(snackbar) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn(tween(300))
            ) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (esBlind) a11yVm.hablar(
                            loc(
                                "Abriendo formulario para registrar alimento.",
                                "Opening form to register food."
                            )
                        )
                        showAgregar = true
                    },
                    containerColor = Sol.Orange,
                    contentColor   = Sol.White,
                    shape          = RoundedCornerShape(20.dp),
                    modifier       = Modifier
                        .height(52.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp),
                            ambientColor = Sol.Orange.copy(.35f),
                            spotColor    = Sol.Orange.copy(.35f))
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar alimento", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInVertically(tween(400, easing = EaseOutCubic)) { -it / 2 } + fadeIn(tween(400))
                ) {
                    SolidosTopBar(childName, ageMonths, onNavigateBack, alimentosIntroducidos.size)
                }
            }
            item { Spacer(Modifier.height(14.dp)) }

            val alergPendientes = alertasAlergenos.filter { a ->
                alimentosIntroducidos.none { it.nombre.equals(a.nombre, ignoreCase = true) }
            }
            if (alergPendientes.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible = visible, enter = expandVertically() + fadeIn(tween(320, 100))) {
                        AlertaBanner(
                            alergPendientes.size, "alérgeno(s) pendiente(s) de introducir",
                            Color(0xFFFFF3E0), Color(0xFFFFE0B2),
                            Icons.Rounded.NotificationImportant, Sol.OrangeMid
                        ) {
                            alergPendientes.forEach { a ->
                                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(8.dp).clip(CircleShape).background(Color(a.grupo.colorHex)))
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(a.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4E342E))
                                        if (a.consejo.isNotBlank())
                                            Text(a.consejo, fontSize = 11.sp, color = Color(0xFF8D6E63), lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            if (alimentosConAlergia.isNotEmpty()) {
                item {
                    AnimatedVisibility(visible = visible, enter = fadeIn(tween(320, 140))) {
                        AlertaBanner(
                            alimentosConAlergia.size, "alimento(s) excluido(s) por alergia",
                            Color(0xFFFFEBEE), Color(0xFFFFCDD2),
                            Icons.Rounded.Block, Sol.Red,
                            sub = "No aparecen en el plan ni en la lista"
                        ) {
                            alimentosConAlergia.forEach { a ->
                                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, null, tint = Sol.Red, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(a.nombre, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4E342E))
                                        a.tipoAlergeno?.let { Text("Alérgeno: ${it.label}", fontSize = 11.sp, color = Sol.Red) }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(360, 80))) {
                    TabsSolidos(tab) { nuevoTab ->
                        tab = nuevoTab
                        if (esBlind) {
                            val nombreTab = when (nuevoTab) {
                                0 -> loc("Alimentos registrados", "Registered foods")
                                1 -> loc("Plan semanal",          "Weekly plan")
                                2 -> loc("Recetas mexicanas",     "Mexican recipes")
                                else -> ""
                            }
                            a11yVm.hablar(loc("Sección $nombreTab", "Section $nombreTab"))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            when (tab) {
                0 -> tabRegistrados(
                    visible, alimentosIntroducidos,
                    onAgregar  = { showAgregar = true },
                    onDelete   = { aEliminar = it },
                    onReaccion = { aReaccion = it }
                )
                1 -> tabPlanSemanal(
                    visible, ageMonths, perfilSalud, alimentosIntroducidos, planSemanal,
                    onAgregar            = { tab = 0; showAgregar = true },
                    onExportarPlan       = onExportarPlan,
                    onExportarCalendario = onExportarCalendario
                )
                2 -> tabRecetas(
                    visible, ageMonths, busquedaReceta, filtroTipo, recetasFiltradas, alergenosNino,
                    onBusqueda = { viewModel.setBusquedaReceta(it) },
                    onFiltro   = { viewModel.setFiltroTipoReceta(it) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(380, 260))) {
                    NotaOms()
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    // ── Diálogo agregar — pasa esBlind, ttsManager e idioma ──────────────────
    if (showAgregar) {
        if (esBlind) {
            SolidoBlindDialog(
                childId = childId,
                ttsManager = ttsManager,
                idioma = idiomaActual,
                onDismiss = {
                    a11yVm.hablar(loc("Registro cancelado.", "Registration cancelled."))
                    showAgregar = false
                },
                onSave = { alim ->
                    viewModel.guardarAlimento(childId, alim)
                    showAgregar = false
                }
            )
        } else {
            AgregarAlimentoDialog(
                esAccesible = esAccesible,
                esBlind     = false,
                ttsManager  = ttsManager,
                idioma     = idiomaActual,
                onDismiss  = {
                    showAgregar = false
                }
            ) { a ->
                viewModel.guardarAlimento(childId, a)
                showAgregar = false
            }
        }
    }

    aEliminar?.let { a ->
        AlertDialog(
            onDismissRequest = { aEliminar = null },
            shape   = RoundedCornerShape(22.dp),
            title   = { Text("Eliminar alimento", fontWeight = FontWeight.Bold) },
            text    = { Text("¿Eliminar \"${a.nombre}\" del registro?") },
            confirmButton = {
                TextButton({
                    viewModel.eliminarAlimento(childId, a.id)
                    aEliminar = null
                }) {
                    Text("Eliminar", color = Sol.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton({ aEliminar = null }) { Text("Cancelar") } }
        )
    }
    aReaccion?.let { a ->
        ReaccionDialog(a, { aReaccion = null }) { r ->
            viewModel.actualizarReaccion(childId, a.id, r)
            aReaccion = null
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun SolidosTopBar(childName: String, meses: Int, onBack: () -> Unit, totalAlimentos: Int = 0) {
    val etapa = when {
        meses < 6   -> "Aún no inicia"
        meses < 12  -> "Iniciando sólidos"
        meses < 24  -> "Diversificación"
        meses < 144 -> "Alimentación familiar"
        else        -> "Adolescencia"
    }
    val anos   = meses / 12
    val mesesR = meses % 12
    val edadTxt = when {
        anos == 0   -> "$meses ${if (meses == 1) "mes" else "meses"}"
        mesesR == 0 -> "$anos ${if (anos == 1) "año" else "años"}"
        else        -> "$anos a. $mesesR m."
    }
    val gradient = Brush.verticalGradient(listOf(Sol.OrangeLight, Sol.Bg))
    Box(
        Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 20.dp)
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Sol.White.copy(0.8f))
                .align(Alignment.CenterStart)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Sol.Orange)
        }
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Alimentación", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Sol.Orange)
            Text(childName,      fontSize = 14.sp, fontWeight = FontWeight.SemiBold,  color = Sol.OrangeMid)
            Spacer(Modifier.height(2.dp))
            Surface(shape = RoundedCornerShape(50.dp), color = Sol.Orange.copy(0.12f)) {
                Text(
                    "$etapa · $edadTxt",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 11.sp, color = Sol.Orange, fontWeight = FontWeight.Medium
                )
            }
        }
        Surface(
            shape    = RoundedCornerShape(50.dp),
            color    = Sol.White.copy(0.85f),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                "$totalAlimentos ${if (totalAlimentos == 1) "alimento" else "alimentos"}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontSize = 11.sp, color = Sol.Orange, fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = Sol.OrangeLight, thickness = 1.dp)
}

// ═══════════════════════════════════════════════════════════════════════════════
// ALERTA BANNER — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AlertaBanner(
    count:   Int,
    label:   String,
    bg:      Color,
    border:  Color,
    icon:    ImageVector,
    color:   Color,
    sub:     String = "",
    content: @Composable ColumnScope.() -> Unit
) {
    CollapseCard(bg, border, color, header = {
        IconBox(icon, color, color.copy(.15f), 32.dp, 17.dp, RoundedCornerShape(10.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text("$count $label", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
            if (sub.isNotBlank()) Text(sub, fontSize = 11.sp, color = color.copy(.8f))
        }
    }) {
        HorizontalDivider(color = border, thickness = 0.5.dp)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TABS — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun TabsSolidos(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Registrados",  Icons.Rounded.CheckCircle,           0),
        Triple("Plan semanal", Icons.Rounded.CalendarMonth,         1),
        Triple("Recetas",      Icons.AutoMirrored.Rounded.MenuBook, 2)
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.spacedBy(8.dp)) {
        tabs.forEach { (label, icon, i) ->
            val sel = selected == i
            val bg  by animateColorAsState(if (sel) Sol.Orange else Sol.White, tween(200), label = "tb$i")
            val fg  by animateColorAsState(if (sel) Sol.White  else Sol.Orange, tween(200), label = "tf$i")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(bg)
                    .border(if (sel) 0.dp else 1.dp, Sol.OrangeLight, RoundedCornerShape(14.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 0 — REGISTRADOS — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
private fun LazyListScope.tabRegistrados(
    visible:   Boolean,
    lista:     List<AlimentoIntroducido>,
    onAgregar: () -> Unit,
    onDelete:  (AlimentoIntroducido) -> Unit,
    onReaccion:(AlimentoIntroducido) -> Unit
) {
    item {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
            MascotBanner(
                drawableRes = com.example.nutriia.resources.Res.drawable.ic_registro,
                titulo      = "Alimentos introducidos",
                subtitulo   = "Registra todo lo que tu bebé ya probó\ny lleva el control de reacciones",
                accentColor = Sol.Orange
            )
        }
    }
    if (lista.isEmpty()) {
        item {
            EstadoVacio(
                Icons.Rounded.Restaurant,
                "Aún no has registrado ningún alimento",
                "Toca el botón de abajo para empezar",
                onAgregar
            )
        }
        return
    }
    item {
        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(tween(380)) { it / 4 } + fadeIn(tween(380))
        ) { ResumenGruposCard(lista) }
        Spacer(Modifier.height(10.dp))
    }
    val conReaccion = lista.filter { it.reaccion != ReaccionAlimento.NINGUNA }
    if (conReaccion.isNotEmpty()) {
        item {
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(320, 60))) {
                ReaccionesCard(conReaccion)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
    items(lista, key = { it.id }) { a ->
        AlimentoCard(a, lista.indexOf(a), { onDelete(a) }, { onReaccion(a) })
    }
}

@Composable
private fun ResumenGruposCard(lista: List<AlimentoIntroducido>) {
    val porGrupo  = GrupoAlimento.entries.associateWith { g -> lista.count { it.grupo == g } }.filter { it.value > 0 }
    val animCount by animateIntAsState(lista.size, tween(600, easing = EaseOutCubic), label = "cnt")
    SolCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.ListAlt, null, tint = Sol.Orange, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("$animCount alimentos introducidos", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Sol.Orange)
            }
            Spacer(Modifier.height(14.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(porGrupo.entries.toList(), key = { it.key.name }) { (grupo, count) ->
                    val animC by animateIntAsState(count, tween(700, easing = EaseOutCubic), label = "gc${grupo.name}")
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(Color(grupo.colorHex).copy(.14f))
                                .border(2.dp, Color(grupo.colorHex), CircleShape),
                            Alignment.Center
                        ) { Text("$animC", fontWeight = FontWeight.Black, color = Color(grupo.colorHex), fontSize = 18.sp) }
                        Spacer(Modifier.height(5.dp))
                        Text(grupo.label, fontSize = 9.sp, color = Sol.TextSecondary, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReaccionesCard(lista: List<AlimentoIntroducido>) {
    SolCard(bg = Color(0xFFFFF3E0), border = Color(0xFFFFE0B2)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.Rounded.Warning, Sol.OrangeMid, Sol.OrangeMid.copy(.15f), 32.dp, 17.dp, RoundedCornerShape(10.dp))
                Spacer(Modifier.width(10.dp))
                Text("Alimentos con reacción", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Sol.Orange)
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color(0xFFFFE0B2), thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))
            lista.forEach { a ->
                val rc = reaccionColor(a.reaccion)
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Circle, null, tint = rc.copy(.5f), modifier = Modifier.size(8.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(a.nombre, fontSize = 13.sp, color = Color(0xFF424242), modifier = Modifier.weight(1f))
                    Chip(Icons.Rounded.Circle, a.reaccion.label, rc)
                }
            }
        }
    }
}

@Composable
private fun AlimentoCard(
    a:         AlimentoIntroducido,
    index:     Int,
    onDelete:  () -> Unit,
    onReaccion:() -> Unit
) {
    val gc      = Color(a.grupo.colorHex)
    val rc      = reaccionColor(a.reaccion)
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index.coerceAtMost(10) * 55L); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(tween(340, easing = EaseOutCubic)) { -60 } + fadeIn(tween(340))
    ) {
        SolCard(shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.Rounded.Restaurant, gc, gc.copy(.12f), 54.dp, 28.dp, RoundedCornerShape(16.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(a.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Sol.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CalendarToday, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(11.dp))
                            Spacer(Modifier.width(3.dp))
                            Text(a.fechaIntroduccion, fontSize = 11.sp, color = Sol.TextSecondary)
                        }
                        Chip(Icons.Rounded.Circle, a.reaccion.label, rc)
                    }
                    if (a.notas.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(a.notas, fontSize = 11.sp, color = Sol.TextSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onReaccion, Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.Edit, null, tint = Sol.OrangeMid, modifier = Modifier.size(17.dp))
                    }
                    IconButton(onDelete, Modifier.size(34.dp)) {
                        Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 1 — PLAN SEMANAL — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
private fun LazyListScope.tabPlanSemanal(
    visible:              Boolean,
    ageMonths:            Int,
    perfilSalud:          PerfilSaludNino,
    lista:                List<AlimentoIntroducido>,
    plan:                 List<PlanSemanalSolidos>,
    onAgregar:            () -> Unit,
    onExportarPlan:       () -> Unit,
    onExportarCalendario: () -> Unit
) {
    val guia = guiaParaEdad(ageMonths)

    item {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
            SolCard(bg = Sol.Orange, border = Color.Transparent) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBox(Icons.Rounded.CalendarMonth, Sol.White, Sol.White.copy(.2f), 48.dp, 26.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(guia.rangoLabel,      color = Sol.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        Text(guia.frecuenciaLabel, color = Sol.White.copy(.85f), fontSize = 12.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    item {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(320, 60))) {
            CollapseCard(Color(0xFFFFF8F0), Sol.OrangeLight, Sol.Orange,
                header = {
                    IconBox(Icons.Rounded.Lightbulb, Sol.Orange, Sol.Orange.copy(.12f), 32.dp, 17.dp, RoundedCornerShape(10.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Tip para $ageMonths meses", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Sol.Orange, modifier = Modifier.weight(1f))
                }
            ) {
                HorizontalDivider(color = Sol.OrangeLight, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                TipSubCard(Icons.Rounded.Blender, Sol.Purple, "Textura: ${guia.texturaLabel}", guia.texturaDescripcion, "Ejemplos: ${guia.texturaEjemplos}")
                Spacer(Modifier.height(6.dp))
                TipSubCard(Icons.Rounded.SetMeal, Sol.Green, "Porción OMS: ${guia.porcionLabel}", guia.porcionProgresion)
                Spacer(Modifier.height(6.dp))
                TipSubCard(Icons.Rounded.ChildCare, Color(0xFF1565C0), null, guia.lactanciaLabel)
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (perfilSalud.tieneAlergias) {
        item {
            SolCard(bg = Color(0xFFFFEBEE), border = Color(0xFFFFCDD2)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Block, null, tint = Sol.Red, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Plan adaptado — excluye: ${perfilSalud.alergenos.joinToString(", ") { it.label }}",
                        fontSize = 11.sp, color = Sol.Red, fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (lista.isEmpty()) {
        item { PlanVacioCard(onAgregar) }
        return
    }

    item {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(300, 80))) {
            MascotBanner(
                drawableRes = com.example.nutriia.resources.Res.drawable.ic_plansemana,
                titulo      = "Plan semanal personalizado",
                subtitulo   = "Basado en los alimentos que ya introdujiste,\nadaptado a la edad y alergias",
                accentColor = Sol.Green
            )
        }
    }
    item {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(320, 100))) {
            SolCard(bg = Sol.Green, border = Color.Transparent) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBox(Icons.Rounded.CheckCircle, Sol.White, Sol.White.copy(.2f), 48.dp, 26.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Plan basado en ${lista.size} alimento(s)", color = Sol.White, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                        Text("Toca el ícono para agregar al calendario",  color = Sol.White.copy(.8f), fontSize = 11.sp)
                    }
                    IconButton(
                        onClick  = onExportarCalendario,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Sol.White.copy(.2f))
                    ) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = Sol.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
    }

    items(plan, key = { it.diaSemana }) { dia -> PlanDiaCard(dia, plan.indexOf(dia)) }
}

@Composable
private fun TipSubCard(icon: ImageVector, color: Color, titulo: String?, descripcion: String, extra: String? = null) {
    val (bg, border) = when (color) {
        Sol.Purple -> Color(0xFFF3E5F5) to Color(0xFFE1BEE7)
        Sol.Green  -> Color(0xFFE8F5E9) to Sol.GreenLight
        else       -> Color(0xFFE3F2FD) to Color(0xFFBBDEFB)
    }
    Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = bg), border = BorderStroke(1.dp, border)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(icon, color, color.copy(.12f), 26.dp, 14.dp, RoundedCornerShape(8.dp))
                if (titulo != null) { Spacer(Modifier.width(8.dp)); Text(titulo, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = color) }
            }
            Spacer(Modifier.height(6.dp))
            Text(descripcion, fontSize = 11.sp, color = color.copy(.85f), lineHeight = 16.sp)
            if (extra != null) { Spacer(Modifier.height(4.dp)); Text(extra, fontSize = 10.sp, color = color.copy(.7f), lineHeight = 15.sp) }
        }
    }
}

@Composable
private fun PlanVacioCard(onRegistrar: () -> Unit) {
    SolCard(bg = Color(0xFFFFF3E0), border = Sol.OrangeLight) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            IconBox(Icons.AutoMirrored.Rounded.MenuBook, Sol.Orange, Sol.Orange.copy(.1f), 64.dp, 32.dp, RoundedCornerShape(20.dp))
            Text("Aún no tienes plan semanal", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Sol.Orange, textAlign = TextAlign.Center)
            Text(
                "Registra los alimentos que ya le diste a tu bebé para generar un plan personalizado.",
                fontSize = 13.sp, color = Color(0xFF5D4037), textAlign = TextAlign.Center, lineHeight = 19.sp
            )
            listOf(
                Triple("1", "Registra un alimento",         Icons.Rounded.Add),
                Triple("2", "Agrega lo que ya probó",       Icons.Rounded.CheckCircle),
                Triple("3", "Aquí verás el plan semanal",   Icons.Rounded.CalendarMonth)
            ).forEach { (n, t, ic) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Sol.White)
                        .border(1.dp, Sol.Border, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(Sol.Orange), Alignment.Center) {
                        Text(n, color = Sol.White, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Icon(ic, null, tint = Sol.Orange.copy(.6f), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(t, fontSize = 13.sp, color = Color(0xFF424242), fontWeight = FontWeight.Medium)
                }
            }
            Button(
                onRegistrar,
                Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sol.Orange),
                shape  = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar primer alimento", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun PlanDiaCard(plan: PlanSemanalSolidos, index: Int) {
    var expanded by remember { mutableStateOf(false) }
    val rot      by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "pd$index")
    var visible  by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 55L); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(360, easing = EaseOutCubic)) { it / 4 } + fadeIn(tween(360))
    ) {
        SolCard(border = if (expanded) Sol.Orange.copy(.25f) else Sol.Border) {
            Column(Modifier.clickable { expanded = !expanded }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (expanded) Sol.Orange else Sol.Orange.copy(.1f)),
                        Alignment.Center
                    ) {
                        Text(plan.diaSemana.take(1), fontWeight = FontWeight.Black, color = if (expanded) Sol.White else Sol.Orange, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(plan.diaSemana, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (expanded) Sol.Orange else Sol.TextPrimary, modifier = Modifier.weight(1f))
                    if (!expanded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(Icons.Rounded.WbSunny, Icons.Rounded.Restaurant, Icons.Rounded.Nightlight).forEach {
                                Icon(it, null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, null,
                        tint = if (expanded) Sol.Orange else Color(0xFFBDBDBD),
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rot })
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                    exit  = shrinkVertically() + fadeOut(tween(180))
                ) {
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                        HorizontalDivider(color = Sol.Orange.copy(.1f), thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                        if (plan.porcionLabel.isNotBlank() || plan.texturaLabel.isNotBlank()) {
                            Row(Modifier.padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (plan.porcionLabel.isNotBlank()) Chip(Icons.Rounded.SetMeal, plan.porcionLabel, Sol.Green)
                                if (plan.texturaLabel.isNotBlank()) Chip(Icons.Rounded.Blender, plan.texturaLabel, Sol.Purple)
                            }
                        }
                        ComidaRow("Desayuno",      plan.desayuno,  Icons.Rounded.WbSunny,          Color(0xFFFFB300))
                        ComidaRow("Almuerzo",      plan.almuerzo,  Icons.Rounded.Restaurant,        Sol.Orange)
                        ComidaRow("Merienda",      plan.merienda,  Icons.Rounded.EmojiFoodBeverage, Sol.Green)
                        if (plan.colacion2.isNotBlank()) ComidaRow("Colación tarde", plan.colacion2, Icons.Rounded.Coffee, Sol.Brown)
                        ComidaRow("Cena",          plan.cena,      Icons.Rounded.Nightlight,        Color(0xFF7986CB))
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TAB 2 — RECETAS — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
private fun LazyListScope.tabRecetas(
    visible:       Boolean,
    ageMonths:     Int,
    busqueda:      String,
    filtroTipo:    FiltroTipoReceta,
    recetas:       List<RecetaMexicana>,
    alergenosNino: List<Alergeno>,
    onBusqueda:    (String) -> Unit,
    onFiltro:      (FiltroTipoReceta) -> Unit
) {
    item {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
            MascotBanner(
                drawableRes = com.example.nutriia.resources.Res.drawable.ic_recetas,
                titulo      = "Recetas mexicanas para bebés",
                subtitulo   = "Filtradas por edad y alergias,\nlistas para preparar en casa",
                accentColor = Sol.OrangeMid
            )
        }
    }
    item {
        AnimatedVisibility(visible = visible, enter = fadeIn(tween(320, 120))) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                OutlinedTextField(
                    value = busqueda, onValueChange = onBusqueda,
                    placeholder = { Text("Buscar receta o ingrediente...", fontSize = 13.sp) },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Sol.Orange, modifier = Modifier.size(19.dp)) },
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = Sol.Orange, unfocusedBorderColor = Sol.OrangeLight,
                        focusedLabelColor    = Sol.Orange, cursorColor          = Sol.Orange
                    )
                )
                Spacer(Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(FiltroTipoReceta.entries, key = { it.name }) { f ->
                        val sel = filtroTipo == f
                        val bg  by animateColorAsState(if (sel) Sol.Orange else Sol.White, tween(180), label = "cf${f.name}")
                        val fg  by animateColorAsState(if (sel) Sol.White  else Sol.Orange, tween(180), label = "cff${f.name}")
                        val ic: ImageVector = when (f) {
                            FiltroTipoReceta.TODAS    -> Icons.Rounded.GridView
                            FiltroTipoReceta.DESAYUNO -> Icons.Rounded.WbSunny
                            FiltroTipoReceta.COMIDA   -> Icons.Rounded.Restaurant
                            FiltroTipoReceta.CENA     -> Icons.Rounded.Nightlight
                            FiltroTipoReceta.COLACION -> Icons.Rounded.EmojiFoodBeverage
                        }
                        Row(
                            Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(bg)
                                .border(1.dp, if (sel) Color.Transparent else Sol.OrangeLight, RoundedCornerShape(20.dp))
                                .clickable { onFiltro(f) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment     = Alignment.CenterVertically
                        ) {
                            Icon(ic, null, tint = fg, modifier = Modifier.size(13.dp))
                            Text(f.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    if (recetas.isEmpty()) {
        item { EstadoVacio(Icons.Rounded.SearchOff, "Sin recetas para este filtro", "Prueba con otro filtro o ingrediente") }
        return
    }
    item {
        Text(
            "${recetas.size} receta${if (recetas.size != 1) "s" else ""} disponible${if (recetas.size != 1) "s" else ""} · aptas para $ageMonths meses",
            fontSize = 12.sp, color = Sol.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(6.dp))
    }
    items(recetas, key = { it.nombre }) { r -> RecetaCard(r, alergenosNino, recetas.indexOf(r)) }
}

@Composable
private fun RecetaCard(receta: RecetaMexicana, alergenosNino: List<Alergeno>, index: Int) {
    var expanded     by remember { mutableStateOf(false) }
    val rot          by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "rr$index")
    var visible      by remember { mutableStateOf(false) }
    val tieneAlergia  = receta.alergenos.any { it in alergenosNino }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index.coerceAtMost(8) * 50L); visible = true }
    val (tipoColor, tipoIcon, tipoLabel) = when (receta.tipoComida) {
        TipoComida.DESAYUNO -> Triple(Color(0xFFFFB300), Icons.Rounded.WbSunny,          "Desayuno")
        TipoComida.COMIDA   -> Triple(Sol.Orange,         Icons.Rounded.Restaurant,        "Comida")
        TipoComida.CENA     -> Triple(Color(0xFF7986CB), Icons.Rounded.Nightlight,        "Cena")
        TipoComida.COLACION -> Triple(Sol.Green,          Icons.Rounded.EmojiFoodBeverage, "Colación")
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(320))
    ) {
        SolCard(bg = if (tieneAlergia) Color(0xFFFFFBF5) else Sol.White, border = if (expanded) Sol.Orange.copy(.3f) else Sol.Border) {
            Column(Modifier.clickable { expanded = !expanded }.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(tipoIcon, tipoColor, tipoColor.copy(.12f), 54.dp, 28.dp, RoundedCornerShape(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(receta.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Sol.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Chip(tipoIcon, tipoLabel, tipoColor)
                            Chip(Icons.Rounded.ChildCare, "desde ${receta.edadMinMeses}m", Sol.TextMuted)
                            if (tieneAlergia) Chip(Icons.Rounded.Warning, "Alérgeno", Sol.Orange)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${receta.kcal} kcal", fontSize = 11.sp, color = Sol.TextSecondary)
                        Icon(Icons.Rounded.KeyboardArrowDown, null,
                            tint = if (expanded) Sol.Orange else Color(0xFFBDBDBD),
                            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rot })
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                    exit  = shrinkVertically() + fadeOut(tween(160))
                ) {
                    Column(Modifier.padding(top = 14.dp)) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        Spacer(Modifier.height(12.dp))
                        if (tieneAlergia) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF3E0)).padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Rounded.Warning, null, tint = Sol.Orange, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Contiene: ${receta.alergenos.joinToString(", ") { it.label }}. Introducir con supervisión médica.",
                                    fontSize = 12.sp, color = Color(0xFFBF360C), lineHeight = 17.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        SeccionLabel("Ingredientes", Icons.Rounded.ShoppingCart)
                        Spacer(Modifier.height(6.dp))
                        receta.ingredientes.forEach { ing ->
                            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Sol.Orange))
                                Spacer(Modifier.width(8.dp))
                                Text(ing, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SeccionLabel("Preparación", Icons.AutoMirrored.Rounded.MenuBook)
                        Spacer(Modifier.height(6.dp))
                        Text(receta.preparacion, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 19.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F8E9)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, null, tint = Sol.Green, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(receta.fuente, fontSize = 10.sp, color = Sol.DarkGreen, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ESTADO VACÍO — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun EstadoVacio(
    icon:     ImageVector,
    texto:    String,
    subtexto: String,
    onAccion: (() -> Unit)? = null
) {
    val inf = rememberInfiniteTransition(label = "ev")
    val sc  by inf.animateFloat(.94f, 1.06f, infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "evs")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Sol.Orange.copy(.08f)).graphicsLayer { scaleX = sc; scaleY = sc },
            Alignment.Center
        ) {
            Icon(icon, null, tint = Sol.Orange.copy(.5f), modifier = Modifier.size(40.dp))
        }
        Text(texto,    color = Sol.Orange,        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, fontSize = 15.sp)
        Text(subtexto, color = Sol.TextSecondary, fontSize   = 12.sp,               textAlign = TextAlign.Center, lineHeight = 17.sp)
        if (onAccion != null) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick  = onAccion,
                modifier = Modifier.height(46.dp),
                border   = BorderStroke(1.5.dp, Sol.Orange),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Add, null, tint = Sol.Orange, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Registrar alimento", color = Sol.Orange, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DIÁLOGO AGREGAR — CON SOPORTE BRAILLE / VOZ / TECLADO EN MODO CIEGO
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun AgregarAlimentoDialog(
    esAccesible: Boolean   = false,
    esBlind:     Boolean   = false,
    ttsManager:  NutriTTS? = null,
    idioma:      IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onDismiss:   () -> Unit,
    onSave:      (AlimentoIntroducido) -> Unit
) {
    var nombre   by remember { mutableStateOf("") }
    var grupo    by remember { mutableStateOf(GrupoAlimento.VERDURAS) }
    var fecha    by remember { mutableStateOf(FechaUtils.fechaActual()) }
    var reaccion by remember { mutableStateOf(ReaccionAlimento.NINGUNA) }
    var notas    by remember { mutableStateOf("") }
    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = Sol.Orange, unfocusedBorderColor = Sol.OrangeLight,
        focusedLabelColor    = Sol.Orange, cursorColor          = Sol.Orange
    )

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }
    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> nombre
            1 -> fecha
            2 -> grupo.label
            3 -> reaccion.label
            4 -> notas
            else -> ""
        }
    }



    val guardarTodo = {
        if (nombre.isNotBlank()) {
            if (esBlind) {
                ttsManager?.hablar(if (idioma == IdiomaVoz.INGLES) "Save" else "Guardar")
            }
            onSave(
                AlimentoIntroducido(
                    nombre            = nombre.trim(),
                    grupo             = grupo,
                    fechaIntroduccion = fecha,
                    reaccion          = reaccion,
                    notas             = notas.trim()
                )
            )
        }
    }

    AlertDialog(
        onDismiss,
        shape          = RoundedCornerShape(28.dp),
        containerColor = Sol.Bg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.Rounded.AddCircle, Sol.Orange, Sol.Orange.copy(.1f), 34.dp, 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Registrar alimento",
                    fontWeight = FontWeight.ExtraBold,
                    color      = Sol.Orange,
                    fontSize   = 17.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.imePadding().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {

                // ── NOMBRE ────────────────────────────────────────────────────
                if (esAccesible) {
                    CampoTextoAccesible(
                        valor          = nombre,
                        onValorChange  = { nombre = it },
                        etiqueta       = "Nombre del alimento",
                        descripcionVoz = "Di el nombre del alimento que vas a registrar.",
                        ttsManager     = if (campoActivo == 0) ttsManager else null,
                        idioma         = idioma,
                        colorPrimario  = Sol.Orange,
                        activo         = campoActivo == 0,
                        onFocus        = { campoActivo = 0 },
                        onNext         = { campoActivo = 1 }
                    )
                } else {
                    OutlinedTextField(
                        nombre, { nombre = it }, Modifier.fillMaxWidth(),
                        label       = { Text("Nombre del alimento") },
                        leadingIcon = { Icon(Icons.Rounded.Restaurant, null, tint = Sol.Orange) },
                        shape       = RoundedCornerShape(14.dp),
                        singleLine  = true,
                        colors      = fc
                    )
                }

                // ── FECHA ─────────────────────────────────────────────────────
                androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 1) {
                    Column {
                        if (esBlind) {
                            CampoTextoAccesible(
                                valor          = fecha,
                                onValorChange  = { fecha = it },
                                etiqueta       = "Fecha (DD/MM/AAAA)",
                                descripcionVoz = "Di la fecha en que le diste este alimento. Por ejemplo: quince de marzo de dos mil veinticuatro.",
                                ttsManager     = if (campoActivo == 1) ttsManager else null,
                                idioma         = idioma,
                                esCampoFecha   = true,
                                colorPrimario  = Sol.Orange,
                                activo         = campoActivo == 1,
                                onFocus        = { campoActivo = 1 },
                                onNext         = { campoActivo = 2 }
                            )
                        } else {
                                        Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            fecha, {}, Modifier.fillMaxWidth(),
                            readOnly    = true,
                            label       = { Text("Fecha") },
                            leadingIcon = { Icon(Icons.Rounded.CalendarToday, null, tint = Sol.Orange) },
                            shape       = RoundedCornerShape(14.dp),
                            singleLine  = true,
                            colors      = fc
                        )
                    }
                }
            }
        }

                // ── GRUPO ALIMENTICIO ──────────────────
                if (!esBlind || campoActivo >= 2) {
                    if (esBlind) {
                        var grupoTexto by remember(grupo) { mutableStateOf(grupo.label) }
                        CampoTextoAccesible(
                            valor          = grupoTexto,
                            onValorChange  = { spoken ->
                                val clean = spoken.lowercase().trim()
                                val matched = when {
                                    clean.contains("verdura") -> GrupoAlimento.VERDURAS
                                    clean.contains("fruta")   -> GrupoAlimento.FRUTAS
                                    clean.contains("cereal")  -> GrupoAlimento.CEREALES
                                    clean.contains("prote") || clean.contains("carne") || clean.contains("huevo") -> GrupoAlimento.PROTEINAS
                                    clean.contains("lact") || clean.contains("leche") || clean.contains("queso")   -> GrupoAlimento.LACTEOS
                                    clean.contains("legum") || clean.contains("frijol") || clean.contains("lenteja") -> GrupoAlimento.LEGUMBRES
                                    else -> GrupoAlimento.OTROS
                                }
                                grupo = matched
                                grupoTexto = matched.label
                                campoActivo = 3
                            },
                            etiqueta       = "Grupo alimenticio",
                            descripcionVoz = "Dime el grupo alimenticio, por ejemplo: frutas, verduras, cereales, proteínas, lácteos, legumbres u otros.",
                            ttsManager     = if (campoActivo == 2) ttsManager else null,
                            idioma         = idioma,
                            colorPrimario  = Sol.Orange,
                            activo         = campoActivo == 2,
                            onFocus        = { campoActivo = 2 },
                            onNext         = { campoActivo = 3 }
                        )
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(GrupoAlimento.entries, key = { it.name }) { g ->
                                val sel    = grupo == g
                                val chipBg by animateColorAsState(
                                    if (sel) Color(g.colorHex) else Color(g.colorHex).copy(.1f),
                                    tween(180), label = "cb${g.name}"
                                )
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(chipBg)
                                        .border(1.dp, Color(g.colorHex), RoundedCornerShape(12.dp))
                                        .clickable { grupo = g }
                                        .padding(horizontal = 10.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        g.label,
                                        fontSize   = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color      = if (sel) Sol.White else Color(g.colorHex)
                                    )
                                }
                            }
                        }
                    }
                }

                // ── REACCIÓN ──────────────────────────
                if (!esBlind || campoActivo >= 3) {
                    if (esBlind) {
                        var reaccionTexto by remember(reaccion) { mutableStateOf(reaccion.label) }
                        CampoTextoAccesible(
                            valor          = reaccionTexto,
                            onValorChange  = { spoken ->
                                val clean = spoken.lowercase().trim()
                                val matched = when {
                                    clean.contains("ning") || clean.contains("sin") || clean.contains("no tuvo") -> ReaccionAlimento.NINGUNA
                                    clean.contains("leve") -> ReaccionAlimento.LEVE
                                    clean.contains("aler") -> ReaccionAlimento.ALERGIA
                                    clean.contains("rechaz") || clean.contains("no le gus") || clean.contains("escup") -> ReaccionAlimento.RECHAZO
                                    clean.contains("acept") || clean.contains("bien") -> ReaccionAlimento.ACEPTADO
                                    else -> ReaccionAlimento.NINGUNA
                                }
                                reaccion = matched
                                reaccionTexto = matched.label
                                campoActivo = 4
                            },
                            etiqueta       = "Reacción observada",
                            descripcionVoz = "Dime si tuvo alguna reacción: ninguna, leve, alergia, rechazo o aceptado.",
                            ttsManager     = if (campoActivo == 3) ttsManager else null,
                            idioma         = idioma,
                            colorPrimario  = Sol.Orange,
                            activo         = campoActivo == 3,
                            onNext         = { campoActivo = 4 }
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            ReaccionAlimento.entries.forEach { r ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (reaccion == r) Sol.Orange.copy(.06f) else Color.Transparent)
                                        .clickable { reaccion = r }
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        reaccion == r, { reaccion = r },
                                        colors = RadioButtonDefaults.colors(selectedColor = Sol.Orange)
                                    )
                                    Text(r.label, fontSize = 13.sp, color = Color(0xFF424242))
                                }
                            }
                        }
                    }
                }

                androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 4) {
                    Column {
                        if (esBlind) {
                            CampoTextoAccesible(
                                valor          = notas,
                                onValorChange  = { notas = it },
                                etiqueta       = "Notas (opcional)",
                                descripcionVoz = if (idioma == IdiomaVoz.INGLES) "All required fields complete. This note field is optional. Say your note, or say save to save." else "Todos los datos requeridos completos. Este campo de notas es opcional. Puedes dictar tu nota, o decir guardar para finalizar y guardar el alimento.",
                                ttsManager     = if (campoActivo == 4) ttsManager else null,
                                idioma         = idioma,
                                colorPrimario  = Sol.Orange,
                                activo         = campoActivo == 4,
                                onNext         = { guardarTodo() }
                            )
                        } else {
                            OutlinedTextField(
                                notas, { notas = it }, Modifier.fillMaxWidth(),
                                label       = { Text("Notas (opcional)") },
                                leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = Sol.Orange) },
                                shape       = RoundedCornerShape(14.dp),
                                maxLines    = 2,
                                colors      = fc
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = { guardarTodo() },
                modifier = Modifier.height(44.dp),
                enabled  = nombre.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = Sol.Orange),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onDismiss) { Text("Cancelar", color = Sol.TextSecondary) }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// DIÁLOGO REACCIÓN — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun ReaccionDialog(
    alimento: AlimentoIntroducido,
    onDismiss:() -> Unit,
    onSave:   (ReaccionAlimento) -> Unit
) {
    var reaccion by remember { mutableStateOf(alimento.reaccion) }
    AlertDialog(
        onDismiss,
        shape          = RoundedCornerShape(24.dp),
        containerColor = Sol.Bg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Edit, null, tint = Sol.Orange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Reacción — ${alimento.nombre}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                ReaccionAlimento.entries.forEach { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (reaccion == r) Sol.Orange.copy(.06f) else Color.Transparent)
                            .clickable { reaccion = r }
                            .padding(horizontal = 6.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(reaccion == r, { reaccion = r }, colors = RadioButtonDefaults.colors(selectedColor = Sol.Orange))
                        Text(r.label, fontSize = 14.sp, color = Color(0xFF424242))
                    }
                }
            }
        },
        confirmButton = {
            Button(
                { onSave(reaccion) },
                Modifier.height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Sol.Orange),
                shape  = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = Sol.TextSecondary) } }
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// NOTA OMS — sin cambios
// ═══════════════════════════════════════════════════════════════════════════════
@Composable
private fun NotaOms() {
    
    SolCard(bg = Color(0xFFF1F8E9), border = Sol.GreenLight) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            IconBox(Icons.Rounded.VerifiedUser, Sol.Green, Sol.Green.copy(.12f), 36.dp, 19.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Información avalada por la OMS", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Sol.DarkGreen)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Edades de introducción, alertas de alérgenos y recomendaciones basadas en guías OMS · UNICEF (2023). " +
                            "La alimentación complementaria inicia a los 6 meses manteniendo la lactancia. " +
                            "Consulta siempre a tu pediatra.",
                    fontSize = 11.sp, color = Color(0xFF388E3C), lineHeight = 16.sp
                )
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = Sol.GreenLight, thickness = 0.5.dp)
                Spacer(Modifier.height(8.dp))
                listOf(
                    "WHO — Infant and Young Child Feeding, 2023"       to FuentesSolidos.WHO_URL,
                    "UNICEF — Early Childhood Nutrition, 2023"          to FuentesSolidos.UNICEF_URL,
                    "ESPGHAN — Complementary Feeding Guidelines, 2017" to FuentesSolidos.ESPGHAN_URL
                ).forEachIndexed { i, (label, url) ->
                    if (i > 0) Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { com.example.nutriia.platform.openUrl(url) }
                            .padding(vertical = 3.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = Sol.Green, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            label,
                            fontSize        = 10.sp,
                            color           = Sol.Green,
                            fontWeight      = FontWeight.Medium,
                            textDecoration  = TextDecoration.Underline
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MODO PARA PERSONAS CIEGAS (BLIND MODE)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun SolidoBlindDialog(
    childId:    String,
    ttsManager: NutriTTS? = null,
    idioma:     IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onDismiss:  () -> Unit,
    onSave:     (AlimentoIntroducido) -> Unit
) {
    var nombre   by remember { mutableStateOf("") }
    var grupo    by remember { mutableStateOf(GrupoAlimento.VERDURAS) }
    var fecha    by remember { mutableStateOf(FechaUtils.fechaActual()) }
    var reaccion by remember { mutableStateOf(ReaccionAlimento.NINGUNA) }
    var notas    by remember { mutableStateOf("") }
    
    var campoActivo by remember { mutableIntStateOf(0) }

    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    val guardarTodo = {
        if (nombre.isNotBlank()) {
            ttsManager?.hablar(loc("Guardando alimento.", "Saving food."))
            onSave(AlimentoIntroducido(
                id                = com.example.nutriia.platform.generateUUID(),
                childId           = childId,
                nombre            = nombre.trim(),
                grupo             = grupo,
                fechaIntroduccion = fecha,
                reaccion          = reaccion,
                notas             = notas.trim()
            ))
        } else {
            ttsManager?.hablar(loc("Falta el nombre del alimento para poder guardar.", "Food name is missing. Please provide it before saving."))
        }
    }

    LaunchedEffect(Unit) {
        ttsManager?.hablarYEsperar(loc(
            "Formulario de registro de nuevo alimento. El foco está en el nombre.",
            "New food registration form. Focus is on the name."
        ), 800L)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 20.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Sol.Orange.copy(0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.AddCircle, null, tint = Sol.Orange, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = loc("Registrar alimento", "Register food"),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Sol.Orange
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Campos Dinámicos
                val currentEtiqueta = when(campoActivo) {
                    0 -> loc("Nombre del alimento", "Food name")
                    1 -> loc("Fecha de introducción", "Introduction date")
                    2 -> loc("Grupo alimenticio", "Food group")
                    3 -> loc("Reacción observada", "Observed reaction")
                    4 -> loc("Notas adicionales - opcional", "Additional notes - optional")
                    else -> ""
                }
                
                val currentDescVoz = when(campoActivo) {
                    0 -> loc("Di el nombre del alimento que el pequeño probó.", "Say the name of the food the little one tried.")
                    1 -> loc("Di la fecha. Por ejemplo: veinte de agosto.", "Tell me the date. For example: August twentieth.")
                    2 -> loc("Dime el grupo: frutas, verduras, cereales, proteínas, lácteos o legumbres.", "Tell me the group: fruits, vegetables, cereals, proteins, dairy or legumes.")
                    3 -> loc("Dime la reacción: ninguna, aceptado, rechazo, leve o alergia.", "Tell me the reaction: none, accepted, rejected, mild or allergy.")
                    4 -> loc("Dicta una nota, o di guardar para finalizar.", "Say a note, or say save to finish.")
                    else -> ""
                }

                if (campoActivo == 2) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(currentEtiqueta, color = Sol.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                            GrupoAlimento.entries.forEach { g ->
                                val isSelected = grupo == g
                                Box(
                                    modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(g.colorHex) else Color(0xFFF8F9FA))
                                        .border(2.dp, if (isSelected) Color(g.colorHex) else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { 
                                            grupo = g
                                            ttsManager?.hablar(loc("Seleccionado: ${g.label}", "Selected: ${g.label}"))
                                            campoActivo = 3
                                        }.padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(g.label, fontSize = 11.sp, color = if (isSelected) Color.White else Color(g.colorHex), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else if (campoActivo == 3) {
                    Column(Modifier.fillMaxWidth()) {
                        Text(currentEtiqueta, color = Sol.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), maxItemsInEachRow = 3) {
                            ReaccionAlimento.entries.forEach { r ->
                                val isSelected = reaccion == r
                                Box(
                                    modifier = Modifier.padding(vertical = 4.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Sol.Orange else Color(0xFFF8F9FA))
                                        .border(2.dp, if (isSelected) Sol.Orange else Color.Transparent, RoundedCornerShape(12.dp))
                                        .clickable { 
                                            reaccion = r
                                            ttsManager?.hablar(loc("Seleccionado: ${r.label}", "Selected: ${r.label}"))
                                            campoActivo = 4
                                        }.padding(horizontal = 14.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(r.label, fontSize = 11.sp, color = if (isSelected) Color.White else Sol.Orange, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    CampoTextoAccesible(
                        valor = when(campoActivo) {
                            0 -> nombre
                            1 -> fecha
                            4 -> notas
                            else -> ""
                        },
                        onValorChange = { v ->
                            when(campoActivo) {
                                0 -> nombre = v
                                1 -> fecha = v
                                4 -> notas = v
                            }
                        },
                        etiqueta = currentEtiqueta,
                        descripcionVoz = currentDescVoz,
                        ttsManager = ttsManager,
                        idioma = idioma,
                        colorPrimario = Sol.Orange,
                        esCampoFecha = campoActivo == 1,
                        onNext = {
                            if (campoActivo == 4) {
                                guardarTodo()
                            } else {
                                campoActivo++
                            }
                        },
                        onCommandParsed = { cmd ->
                            val command = cmd.lowercase().trim()
                            if (command == "guardar" || command == "finalizar" || command == "listo" || 
                                command == "save" || command == "finish" || command == "done") {
                                guardarTodo()
                                true
                            } else false
                        }
                    )
                }

                Spacer(Modifier.height(32.dp))

                // Footer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Text(loc("Cancelar", "Cancel"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Sol.TextSecondary)
                    }
                    
                    Button(
                        onClick = { guardarTodo() },
                        enabled = nombre.isNotBlank(),
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Sol.Orange.copy(0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Sol.Orange.copy(0.2f))
                    ) {
                        Text(loc("Guardar", "Save"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Sol.Orange)
                    }
                }
            }
        }
    }
}
