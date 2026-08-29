package com.example.nutriia.crecimiento

// import android.os.Build
import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.accesibilidad.InputModoCiego
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.nutriia.resources.*

// ═══════════════════════════════════════════════════════════════════════════
// PALETA — Médico Cálido
// ═══════════════════════════════════════════════════════════════════════════
private val C_Bg         = Color(0xFFF8FAF9)
private val C_Card       = Color.White
private val C_Green      = Color(0xFF2E9E6B)
private val C_GreenLight = Color(0xFFE8F5EE)
private val C_GreenDark  = Color(0xFF1A5E40)
private val C_Teal       = Color(0xFF00ACC1)
private val C_TealLight  = Color(0xFFE0F7FA)
private val C_Amber      = Color(0xFFF59E0B)
private val C_AmberLight = Color(0xFFFEF3C7)
private val C_Red        = Color(0xFFE53935)
private val C_RedLight   = Color(0xFFFFEBEE)
private val C_Text       = Color(0xFF1C2B25)
private val C_TextSub    = Color(0xFF6B7B74)
private val C_Divider    = Color(0xFFEEF2F0)
private val C_Grid       = Color(0xFFF0F4F2)
private val C_BlueDark   = Color(0xFF1565C0)
private val C_BlueLight  = Color(0xFFE3F2FD)

// Semáforo IMC → color + microcopy humano
private data class ImcStyle(val color: Color, val light: Color, val icon: ImageVector, val mensaje: String)
private fun imcStyle(categoria: String): ImcStyle = when {
    categoria.contains("Bajo")   -> ImcStyle(C_Teal,  C_TealLight,  Icons.AutoMirrored.Rounded.TrendingDown, "Está por debajo del peso esperado")
    categoria.contains("Normal") -> ImcStyle(C_Green, C_GreenLight, Icons.Rounded.CheckCircle,  "¡Crecimiento saludable!")
    categoria.contains("Riesgo") -> ImcStyle(C_Amber, C_AmberLight, Icons.Rounded.Warning,                    "Puede ser momento de consultar al pediatra")
    else                         -> ImcStyle(C_Red,   C_RedLight,   Icons.AutoMirrored.Rounded.TrendingUp,   "Te recomendamos hablar con el pediatra")
}

// ═══════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun CrecimientoScreen(
    childId: String,
    childName: String,
    ageMonths: Int,
    sexo: Sexo? = null,
    onNavigateBack: () -> Unit,
    a11yVm: AccessibilityViewModel = viewModel(),
    viewModel: CrecimientoViewModel = viewModel()
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

    // FIX 1: se incluyen ageMonths y sexo como keys para que init() se llame
    // también cuando cambien (no solo cuando cambia childId).
    LaunchedEffect(childId, ageMonths, sexo) { viewModel.init(childId, ageMonths, sexo) }

    val historial      by viewModel.historial.collectAsState()
    val ultima         by viewModel.ultimaMedicion.collectAsState()
    val uiState        by viewModel.uiState.collectAsState()
    val interpretacion by viewModel.interpretacionActual.collectAsState()
    val sexoRegistrado by viewModel.sexoRegistrado.collectAsState()

    var showDialog by remember { mutableStateOf(false) }
    var tab        by remember { mutableIntStateOf(0) }
    var eliminar   by remember { mutableStateOf<MedicionCrecimiento?>(null) }
    var visible    by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    // Anuncio inicial de accesibilidad
    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar(
                loc(
                    "Módulo de crecimiento para $childName. " +
                            "Aquí puedes registrar y ver la evolución de peso, talla e IMC. " +
                            "El botón Registrar medición está en la parte inferior central. " +
                            "Las tres secciones son: Resumen e IMC, Historial y Gráficas OMS.",
                    "Growth module for $childName. " +
                            "Here you can log and view the evolution of weight, height, and BMI. " +
                            "The Register measurement button is at the bottom center. " +
                            "The three sections are: Summary and BMI, History, and WHO Charts."
                )
            )
        }
    }

    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uiState) {
        when (uiState) {
            is CrecimientoUiState.Saved   -> {
                if (esBlind) a11yVm.hablar(loc("Medición guardada correctamente.", "Measurement saved successfully."))
                snackbar.showSnackbar("Medición guardada ✓"); viewModel.resetState() 
            }
            is CrecimientoUiState.Deleted -> {
                if (esBlind) a11yVm.hablar(loc("Medición eliminada.", "Measurement deleted."))
                snackbar.showSnackbar("Medición eliminada");  viewModel.resetState() 
            }
            is CrecimientoUiState.Error   -> {
                val msg = (uiState as CrecimientoUiState.Error).msg
                if (esBlind) a11yVm.hablar(loc("Error: $msg", "Error: $msg"))
                snackbar.showSnackbar(msg); viewModel.resetState() 
            }
            else -> {}
        }
    }

    Scaffold(
        containerColor = C_Bg,
        snackbarHost   = { SnackbarHost(snackbar) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = visible,
                enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(300)),
                exit    = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick        = { 
                        if (esBlind) a11yVm.hablar(loc("Abriendo formulario para nueva medición.", "Opening form for new measurement."))
                        showDialog = true 
                    },
                    containerColor = C_Green,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(50.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar medición", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { pad ->
        androidx.compose.foundation.lazy.LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(pad),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter   = slideInVertically(tween(380)) { -it / 2 } + fadeIn(tween(380)),
                    exit    = slideOutVertically() + fadeOut()
                ) { TopBar(childName, historial.size, ageMonths, onNavigateBack) }
            }
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, 60)), exit = fadeOut()) {
                    Spacer(Modifier.height(10.dp))
                    if (sexoRegistrado == null) SexoAviso()
                }
            }
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter   = slideInVertically(tween(440, 80)) { it / 3 } + fadeIn(tween(440, 80)),
                    exit    = slideOutVertically() + fadeOut()
                ) {
                    Spacer(Modifier.height(12.dp))
                    ResumenCard(ultima, ageMonths, interpretacion)
                }
            }
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, 140)), exit = fadeOut()) {
                    Spacer(Modifier.height(20.dp))
                    Tabs(tab) { 
                        tab = it 
                        if (esBlind) {
                            val nombreTab = when (it) {
                                0 -> loc("Resumen e IMC", "Summary and BMI")
                                1 -> loc("Historial de mediciones", "Measurement history")
                                2 -> loc("Gráficas de crecimiento", "Growth charts")
                                else -> ""
                            }
                            a11yVm.hablar(loc("Sección $nombreTab", "Section $nombreTab"))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
            item {
                Crossfade(targetState = tab, animationSpec = tween(260), label = "tabs") { t ->
                    when (t) {
                        0 -> Column {
                            GaugeIMC(ultima, ageMonths, interpretacion)
                            Spacer(Modifier.height(16.dp))
                            EvolucionIMC(historial, ageMonths)
                        }
                        1 -> Column {
                            if (historial.isEmpty()) HistorialVacio()
                            else TimelineHistorial(historial, ageMonths) { eliminar = it }
                        }
                        2 -> Column {
                            GraficaPeso(historial, viewModel.puntosOmsPeso, sexoRegistrado)
                            Spacer(Modifier.height(16.dp))
                            GraficaTalla(historial, viewModel.puntosOmsTalla, sexoRegistrado)
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(400, 300)), exit = fadeOut()) {
                    NotaOMS()
                }
            }
        }
    }

    if (showDialog) {
        DialogoMedicion(
            esAccesible = esAccesible,
            esBlind = esBlind,
            ttsManager = ttsManager,
            idioma = idiomaActual,
            onDismiss = { 
                if (esBlind) a11yVm.hablar(loc("Registro cancelado.", "Registration cancelled."))
                showDialog = false 
            },
            onSave = { med ->
                viewModel.guardarMedicion(childId, med)
                showDialog = false
            }
        )
    }

    eliminar?.let { m ->
        AlertDialog(
            onDismissRequest = { eliminar = null },
            containerColor   = C_Card,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = C_Red, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("¿Eliminar esta medición?", fontWeight = FontWeight.Bold, color = C_Text)
                }
            },
            text = {
                Text(
                    "Se eliminará el registro del ${m.fecha}. Esta acción no se puede deshacer.",
                    color = C_TextSub, fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.eliminarMedicion(childId, m.id); eliminar = null }) {
                    Text("Eliminar", color = C_Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { eliminar = null }) { Text("Cancelar", color = C_TextSub) } }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TopBar(nombre: String, total: Int, ageMonths: Int, onBack: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(C_GreenLight, C_Bg))
    Box(
        Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 20.dp)
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.size(40.dp).clip(CircleShape)
                .background(Color.White.copy(0.7f)).align(Alignment.CenterStart)
        ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = C_Green) }

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Crecimiento", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = C_GreenDark)
            Text(nombre, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = C_Green)
            Spacer(Modifier.height(2.dp))
            val anos  = ageMonths / 12
            val meses = ageMonths % 12
            val edadTxt = when {
                anos == 0  -> "$meses ${if (meses == 1) "mes" else "meses"}"
                meses == 0 -> "$anos ${if (anos == 1) "año" else "años"}"
                else       -> "$anos a. $meses m."
            }
            Surface(shape = RoundedCornerShape(50.dp), color = C_Green.copy(0.12f)) {
                Text(
                    edadTxt,
                    Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 11.sp, color = C_GreenDark, fontWeight = FontWeight.Medium
                )
            }
        }

        Surface(
            shape    = RoundedCornerShape(50.dp),
            color    = Color.White.copy(0.8f),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            Text(
                "$total ${if (total == 1) "medición" else "mediciones"}",
                Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontSize = 11.sp, color = C_Green, fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = C_GreenLight, thickness = 1.dp)
}

// ═══════════════════════════════════════════════════════════════════════════
// AVISO SEXO
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SexoAviso() {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp)
            .clip(RoundedCornerShape(12.dp)).background(C_AmberLight)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.PersonOff, null, tint = C_Amber, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "Registra el sexo del bebé en el perfil para curvas OMS más precisas.",
            fontSize = 12.sp, color = C_Amber, lineHeight = 16.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// RESUMEN
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ResumenCard(m: MedicionCrecimiento?, meses: Int, interp: InterpretacionIMC?) {
    val style = interp?.let { imcStyle(it.categoria) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("Resumen actual", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C_Text)
        Spacer(Modifier.height(10.dp))

        Card(
            Modifier.fillMaxWidth().animateContentSize(tween(300)),
            RoundedCornerShape(22.dp),
            CardDefaults.cardColors(C_Card),
            CardDefaults.cardElevation(1.dp)
        ) {
            if (m == null) {
                Column(
                    Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.ChildCare, null, tint = C_Green.copy(0.4f), modifier = Modifier.size(40.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Aún no hay mediciones", fontWeight = FontWeight.Bold, color = C_Text)
                    Text("Toca el botón verde para empezar", fontSize = 12.sp, color = C_TextSub, textAlign = TextAlign.Center)
                }
                return@Card
            }

            Column(Modifier.fillMaxWidth().padding(20.dp)) {
                style?.let { s ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(s.light).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = org.jetbrains.compose.resources.painterResource(com.example.nutriia.resources.Res.drawable.ic_header),
                            contentDescription = null,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(s.mensaje, fontWeight = FontWeight.Bold, color = s.color, fontSize = 13.sp)
                            Text(interp.descripcion, fontSize = 11.sp, color = s.color.copy(0.8f))
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                val animPeso  by animateFloatAsState(m.pesoKg.toFloat(),  tween(800, easing = EaseOutCubic), label = "p")
                val animTalla by animateFloatAsState(m.tallaCm.toFloat(),  tween(800, easing = EaseOutCubic), label = "t")
                val animImc   by animateFloatAsState(m.imc.toFloat(),      tween(800, easing = EaseOutCubic), label = "i")

                Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
                    StatCol(Icons.Rounded.MonitorWeight, C_Amber,            C_AmberLight,            "${((animPeso * 10).toInt() / 10.0)} kg",  "Peso")
                    Box(Modifier.width(1.dp).height(60.dp).background(C_Divider))
                    StatCol(Icons.Rounded.Height,        C_Teal,             C_TealLight,             "${animTalla.toInt()} cm", "Talla")
                    Box(Modifier.width(1.dp).height(60.dp).background(C_Divider))
                    StatCol(Icons.Rounded.Analytics,     style?.color ?: C_Green, style?.light ?: C_GreenLight, "${((animImc * 10).toInt() / 10.0)}", "IMC")
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = C_Divider, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CalendarToday, null, tint = C_TextSub, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Última: ${m.fecha}", fontSize = 11.sp, color = C_TextSub)
                    Spacer(Modifier.weight(1f))
                    if (meses < 24) {
                        Surface(shape = RoundedCornerShape(50.dp), color = C_TealLight) {
                            Text(
                                "< 2 años: usar peso/longitud",
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 9.sp, color = C_Teal, fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCol(icon: ImageVector, color: Color, bg: Color, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(bg), Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 10.sp, color = C_TextSub)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// TABS
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun Tabs(selected: Int, onSelect: (Int) -> Unit) {
    val items = listOf(
        Icons.Rounded.Analytics to "IMC",
        Icons.Rounded.Timeline  to "Historial",
        Icons.AutoMirrored.Rounded.ShowChart to "Gráficas"
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEachIndexed { i, (icon, label) ->
            val sel = selected == i
            val bg  by animateColorAsState(if (sel) C_Green else C_Card,   tween(220), label = "bg$i")
            val fg  by animateColorAsState(if (sel) Color.White else C_TextSub, tween(220), label = "fg$i")
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(50.dp)).background(bg)
                    .border(1.dp, if (sel) C_Green else C_Divider, RoundedCornerShape(50.dp))
                    .clickable { onSelect(i) }.padding(vertical = 9.dp),
                Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// GRÁFICAS OMS — extendidas a 144 meses (12 años)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GraficaHeader(titulo: String, subtitulo: String, icon: ImageVector, iconColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(iconColor.copy(0.12f)), Alignment.Center) {
            Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(9.dp))
        Column {
            Text(titulo, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C_Text)
            Text(subtitulo, fontSize = 11.sp, color = C_TextSub)
        }
    }
}

@Composable
private fun GraficaPeso(historial: List<MedicionCrecimiento>, omsData: List<PuntoOMS>, sexo: Sexo?) {
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animate = true }
    val prog by animateFloatAsState(
        targetValue    = if (animate) 1f else 0f,
        animationSpec  = tween(1000, easing = EaseOutCubic),
        label          = "lp"
    )
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        GraficaHeader(
            titulo    = "Peso por edad",
            subtitulo = when (sexo) {
                Sexo.NINO -> "Niños · 0–120 meses (OMS 2006/2007)"
                Sexo.NINA -> "Niñas · 0–120 meses (OMS 2006/2007)"
                else      -> "0–120 meses (OMS 2006/2007)"
            },
            icon      = Icons.Rounded.MonitorWeight,
            iconColor = C_Amber
        )
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CardDefaults.cardColors(C_Card), CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp)) {
                GraficaCanvas(
                    historial    = historial,
                    omsData      = omsData,
                    minVal       = 2f,
                    maxVal       = 50f,
                    yLabels      = listOf("2","10","18","26","34","42","50"),
                    yValues      = listOf(2f, 10f, 18f, 26f, 34f, 42f, 50f),
                    maxMeses     = 120,
                    xLabels      = listOf("0m","12m","24m","36m","48m","60m","72m","84m","96m","108m","120m"),
                    xTicks       = listOf(0, 12, 24, 36, 48, 60, 72, 84, 96, 108, 120),
                    lineProgress = prog,
                    bebeColor    = C_Amber,
                    getValue     = { it.pesoKg },
                    leftPad      = 36.dp
                )
                Spacer(Modifier.height(10.dp))
                LeyendaFila(C_Amber)
                Spacer(Modifier.height(4.dp))
                Text(
                    "P3–P97 rango normal OMS. 0–60 m: Estándares 2006 · 61–120 m: Referencia 2007.",
                    fontSize = 10.sp, color = C_TextSub
                )
            }
        }
    }
}

@Composable
private fun GraficaTalla(historial: List<MedicionCrecimiento>, omsData: List<PuntoOMS>, sexo: Sexo?) {
    var animate by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animate = true }
    val prog by animateFloatAsState(
        targetValue   = if (animate) 1f else 0f,
        animationSpec = tween(1000, delayMillis = 100, easing = EaseOutCubic),
        label         = "lt"
    )
    Spacer(Modifier.height(4.dp))
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        GraficaHeader(
            titulo    = "Talla por edad",
            subtitulo = when (sexo) {
                Sexo.NINO -> "Niños · 0–144 meses (OMS 2006/2007)"
                Sexo.NINA -> "Niñas · 0–144 meses (OMS 2006/2007)"
                else      -> "0–144 meses (OMS 2006/2007)"
            },
            icon      = Icons.AutoMirrored.Rounded.ShowChart,
            iconColor = C_Teal
        )
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CardDefaults.cardColors(C_Card), CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp)) {
                GraficaCanvas(
                    historial    = historial,
                    omsData      = omsData,
                    minVal       = 45f,
                    maxVal       = 170f,
                    yLabels      = listOf("45","65","85","105","125","145","165"),
                    yValues      = listOf(45f, 65f, 85f, 105f, 125f, 145f, 165f),
                    maxMeses     = 144,
                    xLabels      = listOf("0m","24m","48m","72m","96m","120m","144m"),
                    xTicks       = listOf(0, 24, 48, 72, 96, 120, 144),
                    lineProgress = prog,
                    bebeColor    = C_Teal,
                    getValue     = { it.tallaCm },
                    leftPad      = 40.dp
                )
                Spacer(Modifier.height(10.dp))
                LeyendaFila(C_Teal)
                Spacer(Modifier.height(4.dp))
                Text(
                    "P3–P97 rango normal OMS. 0–60 m: Estándares 2006 · 61–144 m: Referencia 2007.",
                    fontSize = 10.sp, color = C_TextSub
                )
            }
        }
    }
}

@Composable
private fun GraficaCanvas(
    historial:    List<MedicionCrecimiento>,
    omsData:      List<PuntoOMS>,
    minVal:       Float,
    maxVal:       Float,
    yLabels:      List<String>,
    yValues:      List<Float>,
    maxMeses:     Int,
    xLabels:      List<String>,
    xTicks:       List<Int>,
    lineProgress: Float,
    bebeColor:    Color,
    getValue:     (MedicionCrecimiento) -> Double,
    leftPad:      Dp
) {
    val chartH = 240.dp
    val botPad = 28.dp
    Box(Modifier.fillMaxWidth()) {
        Column(Modifier.width(leftPad).height(chartH - botPad), Arrangement.SpaceBetween) {
            yLabels.reversed().forEach {
                Text(it, fontSize = 9.sp, color = C_TextSub, textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().padding(end = 4.dp))
            }
        }
        Box(
            Modifier.fillMaxWidth().height(chartH)
                .padding(start = leftPad, bottom = botPad)
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    fun xFor(mes: Int)   = (mes.toFloat() / maxMeses.toFloat()) * w
                    fun yFor(v: Double)  = h - ((v.toFloat() - minVal) / (maxVal - minVal)) * h

                    yValues.forEach { v ->
                        val y = h - ((v - minVal) / (maxVal - minVal)) * h
                        drawLine(C_Grid, Offset(0f, y), Offset(w, y), 1.dp.toPx())
                    }
                    xTicks.forEach { mes ->
                        drawLine(C_Grid, Offset(xFor(mes), 0f), Offset(xFor(mes), h), 1.dp.toPx())
                    }

                    val pColores = listOf(
                        omsData.map { it.p3  } to Color(0xFFEF5350),
                        omsData.map { it.p15 } to C_Amber,
                        omsData.map { it.p50 } to C_Green,
                        omsData.map { it.p85 } to C_Amber,
                        omsData.map { it.p97 } to Color(0xFFEF5350)
                    )
                    omsData.forEachIndexed { i, pt ->
                        if (i < omsData.size - 1) {
                            val sig = omsData[i + 1]
                            pColores.forEach { (vals, col) ->
                                drawLine(
                                    col.copy(0.4f),
                                    Offset(xFor(pt.meses), yFor(vals[i])),
                                    Offset(xFor(sig.meses), yFor(vals[i + 1])),
                                    1.6.dp.toPx()
                                )
                            }
                        }
                    }

                    val sorted = historial.sortedBy { it.fecha }
                    val total  = sorted.size
                    if (total >= 2) {
                        val upTo = (lineProgress * (total - 1)).toInt().coerceAtMost(total - 2)
                        for (i in 0..upTo) {
                            val a     = sorted[i]; val b = sorted[i + 1]
                            val alpha = if (i == upTo) lineProgress * (total - 1) - upTo else 1f
                            drawLine(
                                bebeColor.copy(alpha),
                                Offset(xFor(calcMeses(a.fecha)), yFor(getValue(a))),
                                Offset(xFor(calcMeses(b.fecha)), yFor(getValue(b))),
                                2.5.dp.toPx(), cap = StrokeCap.Round
                            )
                        }
                    }
                    sorted.forEach { m ->
                        val mx = calcMeses(m.fecha)
                        drawCircle(bebeColor.copy(lineProgress),   5.dp.toPx(), Offset(xFor(mx), yFor(getValue(m))))
                        drawCircle(Color.White.copy(lineProgress),  3.dp.toPx(), Offset(xFor(mx), yFor(getValue(m))))
                    }
                }
        )
        Row(
            Modifier.fillMaxWidth().align(Alignment.BottomStart).padding(start = leftPad),
            Arrangement.SpaceBetween
        ) {
            xLabels.forEach { Text(it, fontSize = 9.sp, color = C_TextSub) }
        }
    }
}

@Composable
private fun LeyendaFila(bebeColor: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LeyendaDot(bebeColor, "Tu bebé")
        LeyendaDot(C_Green.copy(0.7f), "P50 (mediana)")
        LeyendaDot(C_Amber.copy(0.7f), "P15/P85")
        LeyendaDot(Color(0xFFEF5350).copy(0.7f), "P3/P97")
    }
}

@Composable
private fun LeyendaDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = C_TextSub)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HISTORIAL — timeline vertical
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun TimelineHistorial(
    historial: List<MedicionCrecimiento>,
    ageMonths: Int,
    onDelete: (MedicionCrecimiento) -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("Historial de mediciones", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C_Text)
        Spacer(Modifier.height(12.dp))

        val sorted = historial.sortedWith(
            compareByDescending<MedicionCrecimiento> { it.fechaEpoch() }
                .thenByDescending { it.creadoEn?.seconds ?: 0L }
        )
        sorted.forEachIndexed { idx, m ->
            val interp = interpretarIMC(m.imc, ageMonths)
            val style  = imcStyle(interp.categoria)

            // FIX 2: usar m.id como key en lugar de Unit para que cada nueva
            // medición arranque su propia animación de entrada sin afectar
            // las que ya estaban visibles.
            var rowVisible by remember(m.id) { mutableStateOf(false) }
            LaunchedEffect(m.id) { kotlinx.coroutines.delay(idx * 60L); rowVisible = true }

            AnimatedVisibility(
                visible = rowVisible,
                enter   = slideInHorizontally(tween(320, easing = EaseOutCubic)) { -40 } + fadeIn(tween(320)),
                exit    = slideOutHorizontally() + fadeOut()
            ) {
                Row(Modifier.fillMaxWidth()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(12.dp).clip(CircleShape)
                            .background(if (idx == 0) C_Green else C_Divider))
                        if (idx < sorted.size - 1) {
                            Box(Modifier.width(2.dp).height(76.dp).background(C_Divider))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Card(
                        Modifier.weight(1f).padding(bottom = if (idx < sorted.size - 1) 4.dp else 0.dp),
                        RoundedCornerShape(16.dp),
                        CardDefaults.cardColors(C_Card),
                        CardDefaults.cardElevation(1.dp)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(m.fecha, fontWeight = FontWeight.Bold, color = C_Text, fontSize = 13.sp)
                                    if (idx == 0) {
                                        Spacer(Modifier.width(6.dp))
                                        Surface(shape = RoundedCornerShape(50.dp), color = C_GreenLight) {
                                            Text("Última", Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                fontSize = 9.sp, color = C_Green, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    MiniStat(Icons.Rounded.MonitorWeight, "${m.pesoKg} kg", C_Amber)
                                    MiniStat(Icons.Rounded.Height, "${m.tallaCm} cm", C_Teal)
                                    if (m.imc > 0) MiniStat(style.icon, "${((m.imc * 10).toInt() / 10.0)}", style.color)
                                }
                                if (m.notas.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(m.notas, fontSize = 11.sp, color = C_TextSub)
                                }
                            }
                            IconButton(onClick = { onDelete(m) }) {
                                Icon(Icons.Rounded.DeleteOutline, null, tint = C_Divider)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniStat(icon: ImageVector, text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(3.dp))
        Text(text, fontSize = 12.sp, color = color, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun HistorialVacio() {
    val inf = rememberInfiniteTransition(label = "p")
    val scale by inf.animateFloat(
        initialValue  = 0.95f,
        targetValue   = 1.05f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "s"
    )
    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.ChildCare, null, tint = C_Green.copy(0.3f),
            modifier = Modifier.size(64.dp).graphicsLayer { scaleX = scale; scaleY = scale })
        Spacer(Modifier.height(12.dp))
        Text("Aún no hay mediciones", fontWeight = FontWeight.Bold, color = C_Text, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("Toca el botón verde para registrar la primera",
            fontSize = 13.sp, color = C_TextSub, textAlign = TextAlign.Center)
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// IMC — Gauge circular + evolución
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun GaugeIMC(m: MedicionCrecimiento?, meses: Int, interp: InterpretacionIMC?) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(C_GreenLight), Alignment.Center) {
                Icon(Icons.Rounded.Analytics, null, tint = C_Green, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(9.dp))
            Column {
                Text("Índice de Masa Corporal", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C_Text)
                Text("Calculado con peso y talla actuales", fontSize = 11.sp, color = C_TextSub)
            }
        }
        Spacer(Modifier.height(8.dp))

        Card(Modifier.fillMaxWidth(), RoundedCornerShape(22.dp), CardDefaults.cardColors(C_Card), CardDefaults.cardElevation(1.dp)) {
            if (m == null || m.imc == 0.0) {
                Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                    Text("Registra peso y talla para ver el IMC", color = C_TextSub, textAlign = TextAlign.Center)
                }
                return@Card
            }

            val style = interp?.let { imcStyle(it.categoria) }
            val animImc   by animateFloatAsState(m.imc.toFloat(),   tween(900, easing = EaseOutCubic), label = "imc")
            val arcProgress by animateFloatAsState(
                ((m.imc.toFloat() - 10f) / 22f).coerceIn(0f, 1f),
                tween(1000, easing = EaseOutCubic), label = "arc"
            )

            Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(180.dp).align(Alignment.CenterHorizontally)) {
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val stroke  = 18.dp.toPx()
                        val inset   = stroke / 2
                        val topLeft = Offset(inset, inset)
                        val arcSize = Size(size.width - stroke, size.height - stroke)
                        drawArc(C_Divider, 150f, 240f, false, topLeft, arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                        drawArc(style?.color ?: C_Green, 150f, 240f * arcProgress, false, topLeft, arcSize,
                            style = Stroke(stroke, cap = StrokeCap.Round))
                    }
                    Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${((animImc * 10).toInt() / 10.0)}", fontSize = 40.sp, fontWeight = FontWeight.Black,
                            color = style?.color ?: C_Green)
                        Text("kg/m²", fontSize = 12.sp, color = C_TextSub)
                    }
                }

                Spacer(Modifier.height(4.dp))
                Image(
                    painter = org.jetbrains.compose.resources.painterResource(com.example.nutriia.resources.Res.drawable.ic_crecimiento),
                    contentDescription = null,
                    modifier = Modifier.size(120.dp)
                )
                Spacer(Modifier.height(8.dp))

                style?.let { s ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(s.icon, null, tint = s.color, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(s.mensaje, fontWeight = FontWeight.Bold, color = s.color, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(interp?.descripcion ?: "", fontSize = 12.sp, color = C_TextSub, textAlign = TextAlign.Center)
                }

                if (meses < 24) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(C_TealLight)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = C_Teal, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("La OMS recomienda peso-para-longitud en menores de 2 años.",
                            fontSize = 11.sp, color = C_Teal)
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(4.dp)) {
                    listOf("Bajo\npeso" to C_Teal, "Normal" to C_Green, "Sobre-\npeso" to C_Amber, "Obesidad" to C_Red)
                        .forEach { (label, color) ->
                            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
                                Spacer(Modifier.height(4.dp))
                                Text(label, fontSize = 9.sp, color = color, textAlign = TextAlign.Center, lineHeight = 12.sp)
                            }
                        }
                }
            }
        }
    }
}

@Composable
private fun EvolucionIMC(historial: List<MedicionCrecimiento>, meses: Int) {
    val conIMC = historial.filter { it.imc > 0 }.take(6)
    if (conIMC.isEmpty()) return

    Spacer(Modifier.height(4.dp))
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Text("Evolución del IMC", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, color = C_Text)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(20.dp), CardDefaults.cardColors(C_Card), CardDefaults.cardElevation(1.dp)) {
            Column(Modifier.padding(16.dp)) {
                conIMC.reversed().forEachIndexed { idx, m ->
                    val interp = interpretarIMC(m.imc, meses)
                    val style  = imcStyle(interp.categoria)
                    // FIX 3: misma corrección que en TimelineHistorial — key por id
                    var vis by remember(m.id) { mutableStateOf(false) }
                    LaunchedEffect(m.id) { kotlinx.coroutines.delay(idx * 70L); vis = true }

                    AnimatedVisibility(
                        visible = vis,
                        enter   = fadeIn(tween(300)) + slideInHorizontally(tween(300)) { -30 },
                        exit    = fadeOut() + slideOutHorizontally()
                    ) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(style.icon, null, tint = style.color, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(m.fecha, fontSize = 12.sp, color = C_TextSub, modifier = Modifier.weight(1f))
                            Text("${((m.imc * 100).toInt() / 100.0)}", fontWeight = FontWeight.Bold, color = style.color, fontSize = 14.sp)
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(50.dp), color = style.light) {
                                Text(interp.categoria, Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontSize = 10.sp, color = style.color, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    if (m != conIMC.first()) HorizontalDivider(color = C_Divider, thickness = 0.5.dp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// DIÁLOGO — nueva medición
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun DialogoMedicion(
    esAccesible: Boolean = false,
    esBlind:     Boolean = false,
    ttsManager: NutriTTS? = null,
    idioma:     IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onDismiss:  () -> Unit, 
    onSave:     (MedicionCrecimiento) -> Unit
) {
    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es
    var fecha by remember { mutableStateOf(com.example.nutriia.utils.FechaUtils.hoyIso()) }
    var peso  by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var circC by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }
    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> fecha
            1 -> peso
            2 -> talla
            3 -> circC
            4 -> notas
            else -> ""
        }
    }



    var yaGuardando by remember { mutableStateOf(false) }
    val guardarTodo = {
        if (!yaGuardando) {
            if (peso.isNotBlank() && talla.isNotBlank()) {
                yaGuardando = true
                if (esBlind) {
                    ttsManager?.hablar(loc("Guardar", "Save"))
                }
                onSave(MedicionCrecimiento(
                    id        = com.example.nutriia.platform.generateUUID(),
                    fecha     = fecha,
                    pesoKg    = peso.replace(",", ".").toDoubleOrNull()  ?: 0.0,
                    tallaCm   = talla.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    circCefCm = circC.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    notas     = notas
                ))
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = C_Card,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(C_GreenLight), Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = C_Green, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Nueva medición", fontWeight = FontWeight.ExtraBold, color = C_Text)
                    Text("Peso y talla son obligatorios", fontSize = 11.sp, color = C_TextSub)
                }
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (esBlind) {
                    CampoTextoAccesible(
                        valor          = fecha,
                        onValorChange  = { fecha = it },
                        etiqueta       = "Fecha (DD/MM/AAAA)",
                        descripcionVoz = "Di la fecha de la medición.",
                        ttsManager     = ttsManager,
                        idioma         = idioma,
                        esCampoFecha   = true,
                        colorPrimario  = C_Green,
                        activo         = campoActivo == 0,
                        onFocus        = { campoActivo = 0 },
                        onNext         = { campoActivo = 1 }
                    )
                    androidx.compose.animation.AnimatedVisibility(visible = !esBlind && esAccesible || campoActivo >= 1) {
                        CampoTextoAccesible(
                            valor          = peso,
                            onValorChange  = { peso = it },
                            etiqueta       = "Peso (kg)",
                            descripcionVoz = "Di el peso en kilogramos.",
                            ttsManager     = ttsManager,
                            idioma         = idioma,
                            colorPrimario  = C_Green,
                            activo         = campoActivo == 1,
                            onFocus        = { campoActivo = 1 },
                            onNext         = { campoActivo = 2 }
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = !esBlind && esAccesible || campoActivo >= 2) {
                        CampoTextoAccesible(
                            valor          = talla,
                            onValorChange  = { talla = it },
                            etiqueta       = "Talla (cm)",
                            descripcionVoz = "Di la talla en centímetros.",
                            ttsManager     = ttsManager,
                            idioma         = idioma,
                            colorPrimario  = C_Green,
                            activo         = campoActivo == 2,
                            onFocus        = { campoActivo = 2 },
                            onNext         = { campoActivo = 3 }
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = !esBlind && esAccesible || campoActivo >= 3) {
                        CampoTextoAccesible(
                            valor          = circC,
                            onValorChange  = { circC = it },
                            etiqueta       = "Perímetro cefálico (opcional)",
                            descripcionVoz = loc("Di el perímetro cefálico en centímetros, o di no lo tengo para continuar.", "Say the head circumference in centimeters, or say I don't have it to continue."),
                            ttsManager     = ttsManager,
                            idioma         = idioma,
                            colorPrimario  = C_Green,
                            activo         = campoActivo == 3,
                            onFocus        = { campoActivo = 3 },
                            onNext         = { campoActivo = 4 }
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = !esBlind && esAccesible || campoActivo >= 4) {
                        CampoTextoAccesible(
                            valor          = notas,
                            onValorChange  = { notas = it },
                            etiqueta       = "Notas (opcional)",
                            descripcionVoz = loc("Todos los datos requeridos completos. Este campo de notas es opcional. Puedes dictar tu nota, o decir guardar para finalizar y guardar la medición.", "All required fields complete. This note field is optional. Say your note, or say save to save it."),
                            ttsManager     = ttsManager,
                            idioma         = idioma,
                            colorPrimario  = C_Green,
                            activo         = campoActivo == 4,
                            onFocus        = { campoActivo = 4 },
                            onNext         = { guardarTodo() }
                        )
                    }
                } else {
                                        CampoMedicion("Fecha (AAAA-MM-DD)", Icons.Rounded.CalendarToday, fecha) { fecha = it }
                    CampoMedicion("Peso (kg)", Icons.Rounded.MonitorWeight, peso, KeyboardType.Decimal) { peso = it }
                    CampoMedicion("Talla (cm)", Icons.Rounded.Height, talla, KeyboardType.Decimal) { talla = it }
                    CampoMedicion("Circ. cefálica (cm)  —  opcional", Icons.Rounded.Tag, circC, KeyboardType.Decimal) { circC = it }
                    CampoMedicion("Notas  —  opcional", Icons.Rounded.Edit, notas) { notas = it }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { guardarTodo() },
                enabled = peso.isNotBlank() && talla.isNotBlank(),
                colors  = ButtonDefaults.buttonColors(containerColor = C_Green),
                shape   = RoundedCornerShape(50.dp)
            ) { Text("Guardar medición", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onDismiss) { Text("Cancelar", color = C_TextSub) } }
    )
}

@Composable
private fun CampoMedicion(
    label: String, icon: ImageVector, value: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onChange,
        label           = { Text(label, fontSize = 12.sp) },
        leadingIcon     = { Icon(icon, null, tint = C_Green, modifier = Modifier.size(18.dp)) },
        modifier        = Modifier.fillMaxWidth(),
        shape           = RoundedCornerShape(14.dp),
        singleLine      = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors          = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = C_Green,
            unfocusedBorderColor = C_Divider,
            focusedLabelColor    = C_Green
        )
    )
}

// ═══════════════════════════════════════════════════════════════════════════
// NOTA OMS — FUENTES COMPLETAS Y VERIFICABLES
// ═══════════════════════════════════════════════════════════════════════════

private data class FuenteInfo(
    val titulo:      String,
    val descripcion: String,
    val rango:       String,
    val indicadores: String,
    val url:         String,
    val urlPdf:      String? = null,
    val labelPdf:    String? = null,
    val cita:        String? = null
)

private val FUENTES = listOf(
    FuenteInfo(
        titulo      = "WHO Child Growth Standards 2006",
        descripcion = "Estándares de crecimiento infantil basados en niños de 6 países " +
                "criados en condiciones óptimas (lactancia materna, no fumadores, etc.).",
        rango       = "0–60 meses (0–5 años)",
        indicadores = "Peso/edad · Talla/edad · IMC/edad · Perímetro cefálico",
        url         = "https://www.who.int/tools/child-growth-standards",
        cita        = "WHO Multicentre Growth Reference Study Group. " +
                "WHO Child Growth Standards. Geneva: WHO; 2006."
    ),
    FuenteInfo(
        titulo      = "WHO Growth Reference 2007 — Peso/edad",
        descripcion = "Tablas de peso-para-edad para 5–10 años. La OMS limita este " +
                "indicador a 10 años porque más allá el peso no distingue entre " +
                "talla y masa corporal.",
        rango       = "61–120 meses (5–10 años)",
        indicadores = "Peso/edad",
        url         = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/weight-for-age-5to10-years",
        urlPdf      = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/weight-for-age-(5-10-years)/wfa-boys--5-10years-per.pdf",
        labelPdf    = "PDF niños — wfa-boys-5-10years-per.pdf",
        cita        = "de Onis M et al. Development of a WHO growth reference for " +
                "school-aged children and adolescents. " +
                "Bull World Health Organ. 2007;85(9):660–667."
    ),
    FuenteInfo(
        titulo      = "WHO Growth Reference 2007 — Peso/edad (niñas)",
        descripcion = "Tablas de peso-para-edad para niñas 5–10 años.",
        rango       = "61–120 meses (5–10 años)",
        indicadores = "Peso/edad",
        url         = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/weight-for-age-5to10-years",
        urlPdf      = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/weight-for-age-(5-10-years)/wfa-girls-5-10years-per.pdf",
        labelPdf    = "PDF niñas — wfa-girls-5-10years-per.pdf",
        cita        = "de Onis M et al. Bull World Health Organ. 2007;85(9):660–667."
    ),
    FuenteInfo(
        titulo      = "WHO Growth Reference 2007 — Talla/edad (niños)",
        descripcion = "Tablas de talla-para-edad para niños 5–19 años. " +
                "Se usa interpolación lineal para meses intermedios.",
        rango       = "61–144 meses (5–12 años) en esta app",
        indicadores = "Talla/edad",
        url         = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/height-for-age",
        urlPdf      = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/height-for-age-(5-19-years)/hfa-boys-5-19years-per.pdf",
        labelPdf    = "PDF niños — hfa-boys-5-19years-per.pdf",
        cita        = "de Onis M et al. Bull World Health Organ. 2007;85(9):660–667."
    ),
    FuenteInfo(
        titulo      = "WHO Growth Reference 2007 — Talla/edad (niñas)",
        descripcion = "Tablas de talla-para-edad para niñas 5–19 años.",
        rango       = "61–144 meses (5–12 años) en esta app",
        indicadores = "Talla/edad",
        url         = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/height-for-age",
        urlPdf      = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/height-for-age-(5-19-years)/hfa-girls-5-19years-per.pdf",
        labelPdf    = "PDF niñas — hfa-girls-5-19years-per.pdf",
        cita        = "de Onis M et al. Bull World Health Organ. 2007;85(9):660–667."
    ),
    FuenteInfo(
        titulo      = "WHO Growth Reference 2007 — IMC/edad (niños)",
        descripcion = "Tablas de IMC-para-edad para niños 5–19 años. " +
                "A los 19 años el P85 coincide con IMC 25 (sobrepeso adulto) " +
                "y el P97 con IMC 30 (obesidad adulta).",
        rango       = "61–144 meses (5–12 años) en esta app",
        indicadores = "IMC/edad",
        url         = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/bmi-for-age",
        urlPdf      = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/bmi-for-age-(5-19-years)/bmifa-boys-5-19years-per.pdf",
        labelPdf    = "PDF niños — bmifa-boys-5-19years-per.pdf",
        cita        = "de Onis M et al. Bull World Health Organ. 2007;85(9):660–667."
    ),
    FuenteInfo(
        titulo      = "WHO Growth Reference 2007 — IMC/edad (niñas)",
        descripcion = "Tablas de IMC-para-edad para niñas 5–19 años.",
        rango       = "61–144 meses (5–12 años) en esta app",
        indicadores = "IMC/edad",
        url         = "https://www.who.int/tools/growth-reference-data-for-5to19-years/indicators/bmi-for-age",
        urlPdf      = "https://cdn.who.int/media/docs/default-source/child-growth/growth-reference-5-19-years/bmi-for-age-(5-19-years)/bmifa-girls-5-19years-per.pdf",
        labelPdf    = "PDF niñas — bmifa-girls-5-19years-per.pdf",
        cita        = "de Onis M et al. Bull World Health Organ. 2007;85(9):660–667."
    ),
    FuenteInfo(
        titulo      = "WHO — Alimentación del lactante y niño pequeño",
        descripcion = "Directrices sobre lactancia materna y alimentación complementaria " +
                "que contextualizan la interpretación del crecimiento temprano.",
        rango       = "0–24 meses",
        indicadores = "Contexto nutricional",
        url         = "https://www.who.int/news-room/fact-sheets/detail/infant-and-young-child-feeding",
        cita        = "WHO. Infant and Young Child Feeding. Fact sheet. Geneva: WHO; 2023."
    )
)

// ═══════════════════════════════════════════════════════════════════════════
// NOTA OMS — FUENTES COMPLETAS Y VERIFICABLES
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun NotaOMS() {
        var fuentesExpanded by remember { mutableStateOf(false) }       // ← NUEVO

    Column(
        Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Cabecera verde (sin cambios) ──────────────────────────────────
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(18.dp), CardDefaults.cardColors(C_GreenLight)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.VerifiedUser, null, tint = C_Green, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Fuentes científicas utilizadas",
                        fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = C_GreenDark)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Todos los percentiles, curvas y criterios de interpretación " +
                                "provienen de tablas oficiales descargadas directamente de " +
                                "cdn.who.int. Ningún valor fue estimado ni extrapolado de " +
                                "fuentes secundarias.",
                        fontSize = 12.sp, color = C_GreenDark.copy(0.85f), lineHeight = 17.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.clip(RoundedCornerShape(10.dp)).background(C_Green.copy(0.12f))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, null, tint = C_GreenDark, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Se aplica interpolación lineal para obtener valores en " +
                                    "meses no tabulados. Los percentiles de corte son P3, P15, P85 y P97.",
                            fontSize = 10.sp, color = C_GreenDark, lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // ── Tarjeta desplegable — FUENTES ─────────────────────────────────
        Card(
            Modifier.fillMaxWidth(),
            RoundedCornerShape(18.dp),
            CardDefaults.cardColors(C_Card),
            CardDefaults.cardElevation(1.dp)
        ) {
            Column(Modifier.fillMaxWidth().animateContentSize(tween(320))) {

                // Fila-cabecera con toggle
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { fuentesExpanded = !fuentesExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(C_BlueLight),
                        Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = C_Green, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Referencias bibliográficas",
                            fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = C_Text
                        )
                        Text(
                            "${FUENTES.size} fuentes OMS verificadas",
                            fontSize = 11.sp, color = C_TextSub
                        )
                    }
                    // Chevron animado
                    val rotate by animateFloatAsState(
                        targetValue   = if (fuentesExpanded) 180f else 0f,
                        animationSpec = tween(280),
                        label         = "chevron"
                    )
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = if (fuentesExpanded) "Colapsar" else "Expandir",
                        tint     = C_Green,
                        modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = rotate }
                    )
                }

                // Contenido expandible
                if (fuentesExpanded) {
                    HorizontalDivider(color = C_Divider, thickness = 0.5.dp)
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FUENTES.forEach { fuente ->
                            FuenteCard(fuente = fuente)
                        }
                    }
                }
            }
        }

        // ── Aviso médico (sin cambios) ────────────────────────────────────
        Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(C_RedLight)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.MedicalServices, null, tint = C_Red, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Esta app es una herramienta de seguimiento, no de diagnóstico. " +
                            "Consulta siempre a tu pediatra ante cualquier duda sobre el " +
                            "crecimiento de tu hijo/a.",
                    fontSize = 11.sp, color = C_Red, lineHeight = 16.sp, fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun FuenteCard(fuente: FuenteInfo) {
    Card(Modifier.fillMaxWidth(), RoundedCornerShape(16.dp), CardDefaults.cardColors(C_Card), CardDefaults.cardElevation(1.dp)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(C_BlueLight), Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = C_BlueDark, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(fuente.titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = C_Text)
            }
            Spacer(Modifier.height(8.dp))
            Text(fuente.descripcion, fontSize = 11.sp, color = C_TextSub, lineHeight = 16.sp)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                MetaChip(Icons.Rounded.CalendarToday, fuente.rango,       C_Green)
                MetaChip(Icons.Rounded.Analytics,     fuente.indicadores, C_Teal)
            }
            fuente.cita?.let { cita ->
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(C_BlueLight)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Rounded.FormatQuote, null, tint = C_BlueDark, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(cita, fontSize = 10.sp, color = C_BlueDark, lineHeight = 14.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = C_Divider, thickness = 0.5.dp)
            Spacer(Modifier.height(10.dp))
            LinkRow(icon = Icons.Rounded.Language, label = "Página oficial OMS", url = fuente.url)
            fuente.urlPdf?.let { pdf ->
                Spacer(Modifier.height(6.dp))
                LinkRow(
                    icon  = Icons.Rounded.PictureAsPdf,
                    label = fuente.labelPdf ?: "Tabla de percentiles (PDF)",
                    url   = pdf,
                                        color = C_Red
                )
            }
        }
    }
}

@Composable
private fun MetaChip(icon: ImageVector, text: String, color: Color) {
    Surface(shape = RoundedCornerShape(50.dp), color = color.copy(0.10f)) {
        Row(Modifier.padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = color, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(4.dp))
            Text(text, fontSize = 9.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun LinkRow(
    icon:  ImageVector,
    label: String,
    url:   String,
        color: Color = C_Green
) {
    Row(
        Modifier.clip(RoundedCornerShape(6.dp))
            .clickable { com.example.nutriia.platform.openUrl(url) }
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = color.copy(0.6f), modifier = Modifier.size(10.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// HELPER
// ═══════════════════════════════════════════════════════════════════════════

private fun calcMeses(fecha: String): Int = com.example.nutriia.shared.calcularEdadMeses(fecha)

// ═══════════════════════════════════════════════════════════════════════════════
// MODO PARA PERSONAS CIEGAS (BLIND MODE)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CrecimientoBlindDialog(
    childId:    String,
    ttsManager: NutriTTS? = null,
    idioma:     IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onDismiss:  () -> Unit,
    onSave:     (MedicionCrecimiento) -> Unit
) {
    var fecha by remember { mutableStateOf(com.example.nutriia.utils.FechaUtils.hoyIso()) }
    var peso  by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var circC by remember { mutableStateOf("") }
    var notas by remember { mutableStateOf("") }
    
    var campoActivo by remember { mutableIntStateOf(0) }

    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    var yaGuardando by remember { mutableStateOf(false) }
    val guardarTodo = {
        if (!yaGuardando) {
            if (peso.isNotBlank() && talla.isNotBlank()) {
                yaGuardando = true
                ttsManager?.hablar(loc("Guardando medición.", "Saving measurement."))
                onSave(MedicionCrecimiento(
                    id        = com.example.nutriia.platform.generateUUID(),
                    childId   = childId,
                    fecha     = fecha,
                    pesoKg    = peso.replace(",", ".").toDoubleOrNull()  ?: 0.0,
                    tallaCm   = talla.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    circCefCm = circC.replace(",", ".").toDoubleOrNull() ?: 0.0,
                    notas     = notas
                ))
            } else {
                val falta = if (peso.isBlank()) loc("peso", "weight") else loc("talla", "height")
                ttsManager?.hablar(loc("Falta completar el campo $falta para poder guardar.", "You need to complete the $falta field before saving."))
            }
        }
    }

    LaunchedEffect(Unit) {
        ttsManager?.hablarYEsperar(loc(
            "Formulario de nueva medición de crecimiento. Iniciando en el campo de fecha.",
            "New growth measurement form. Starting at the date field."
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
                        color = C_Green.copy(0.1f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.OpenInNew, null, tint = C_Green, modifier = Modifier.padding(8.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = loc("Nueva medición", "New measurement"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = C_Text
                        )
                        Text(
                            text = loc("Peso y talla son obligatorios", "Weight and height are mandatory"),
                            fontSize = 12.sp,
                            color = C_TextSub
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Campos Dinámicos
                val currentEtiqueta = when(campoActivo) {
                    0 -> loc("Fecha (DD/MM/AAAA)", "Date (DD/MM/AAAA)")
                    1 -> loc("Peso (kg)", "Weight (kg)")
                    2 -> loc("Talla (cm)", "Height (cm)")
                    3 -> loc("Perímetro cefálico (cm) - opcional", "Head circumference (cm) - optional")
                    4 -> loc("Notas adicionales - opcional", "Additional notes - optional")
                    else -> ""
                }
                
                val currentDescVoz = when(campoActivo) {
                    0 -> loc("Di la fecha de la medición.", "Say the measurement date.")
                    1 -> loc("Dime el peso en kilogramos.", "Tell me the weight in kilograms.")
                    2 -> loc("Dime la talla en centímetros.", "Tell me the height in centimeters.")
                    3 -> loc("Di el perímetro cefálico en centímetros, o di siguiente para omitir.", "Say the head circumference in centimeters, or say next to skip.")
                    4 -> loc("Dicta una nota, o di guardar para finalizar.", "Say a note, or say save to finish.")
                    else -> ""
                }

                CampoTextoAccesible(
                    valor = when(campoActivo) {
                        0 -> fecha
                        1 -> peso
                        2 -> talla
                        3 -> circC
                        4 -> notas
                        else -> ""
                    },
                    onValorChange = { v ->
                        when(campoActivo) {
                            0 -> fecha = v
                            1 -> peso = v
                            2 -> talla = v
                            3 -> circC = v
                            4 -> notas = v
                        }
                    },
                    etiqueta = currentEtiqueta,
                    descripcionVoz = currentDescVoz,
                    ttsManager = ttsManager,
                    idioma = idioma,
                    colorPrimario = C_Green,
                    esCampoFecha = campoActivo == 0,
                    keyboardOptions = KeyboardOptions(keyboardType = if (campoActivo in 1..3) KeyboardType.Decimal else KeyboardType.Text),
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
                        Text(loc("Cancelar", "Cancel"), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = C_TextSub)
                    }
                    
                    Button(
                        onClick = { guardarTodo() },
                        enabled = peso.isNotBlank() && talla.isNotBlank(),
                        modifier = Modifier.weight(1f).height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = C_Green.copy(0.05f)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, C_Green.copy(0.2f))
                    ) {
                        Text(loc("Guardar medición", "Save measurement"), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = C_Green)
                    }
                }
            }
        }
    }
}

