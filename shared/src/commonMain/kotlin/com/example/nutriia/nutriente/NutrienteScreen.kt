package com.example.nutriia.nutriente

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.example.nutriia.utils.FechaUtils
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.accesibilidad.VoiceInputManager
import com.example.nutriia.accesibilidad.VoiceInputState
import com.example.nutriia.accesibilidad.vibrateTap
import com.example.nutriia.shared.NutriSharedViewModel
import com.example.nutriia.resources.*
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico

// ═══════════════════════════════════════════════════════════════════════════════
// TOKENS DE DISEÑO
// ═══════════════════════════════════════════════════════════════════════════════
private object Sol {
    val Bg            = Color(0xFFFFF8F2)
    val White         = Color.White
    val Border        = Color(0xFFF0E6DE)
    val Orange        = Color(0xFFE65100)
    val OrangeLight   = Color(0xFFFFE0B2)
    val Purple        = Color(0xFF6650A4)
    val PurpleLight   = Color(0xFFEDE7F6)
    val PurpleDark    = Color(0xFF4527A0)
    val PurpleMid     = Color(0xFF7E57C2)
    val Green         = Color(0xFF2E7D32)
    val Red           = Color(0xFFB71C1C)
    val Blue          = Color(0xFF1565C0)
    val TextPrimary   = Color(0xFF2D2D2D)
    val TextSecondary = Color(0xFF9E9E9E)
    val TextMuted     = Color(0xFF757575)
}

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
private fun MascotBanner(
    drawableRes: org.jetbrains.compose.resources.DrawableResource,
    titulo:      String,
    subtitulo:   String,
    accentColor: Color = Sol.Purple,
    mascotSize:  Dp    = 140.dp
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
                modifier           = Modifier
                    .size(mascotSize)
                    .graphicsLayer { translationY = -float }
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(titulo,    fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = accentColor, lineHeight = 22.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitulo, fontSize = 12.sp, color = accentColor.copy(.7f),   lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun SeccionLabel(texto: String, icon: ImageVector, color: Color = Sol.Purple) =
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = color, modifier = Modifier.size(13.dp))
        Text(texto.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.8.sp)
    }

@Composable
fun NutrientesScreen(
    childId:   String,
    childName: String,
    mesesEdad: Int,
    nivel:     NivelIngreso         = NivelIngreso.BASICO,
    region:    RegionMexico         = RegionMexico.CENTRO,
    sharedVm:  NutriSharedViewModel,
    onBack:    () -> Unit,
    a11yVm:    AccessibilityViewModel = viewModel(),
    vm:        NutrientesViewModel  = viewModel()
) {
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esAccesible  = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE
    val esBlind      = a11yMode == AccessibilityMode.BLIND

    fun loc(es: String, en: String) = idiomaActual.loc(es, en)

    LaunchedEffect(childId, mesesEdad, nivel, region) {
        vm.init(childId = childId, meses = mesesEdad, nivel = nivel, region = region, sharedVm = sharedVm)
    }

    val registros     by vm.registros.collectAsState()
    val totales       by vm.totalesDia.collectAsState()
    val totalesMicros by vm.totalesMicrosDia.collectAsState()
    val fecha         by vm.fechaSeleccionada.collectAsState()
    val error         by vm.error.collectAsState()
    val recomendacion by vm.recomendacionActual.collectAsState()
    val guiaEdad      by vm.guiaEdadActual.collectAsState()

    var mostrarForm by remember { mutableStateOf(false) }
    var visible     by remember { mutableStateOf(false) }
    val snackbar    = remember { SnackbarHostState() }

        var isListening by remember { mutableStateOf(false) }
    val voiceManager = remember { VoiceInputManager() }
    val voiceState by voiceManager.estado

    LaunchedEffect(isListening) {
        if (isListening && esBlind) {
            a11yVm.hablar(loc("Te escucho, ¿qué quieres hacer?", "I'm listening, what do you want to do?"))
            delay(1500)
            voiceManager.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                isListening = false
                val cmd = result.lowercase().trim()
                when {
                    cmd.contains("anotar") || cmd.contains("registrar") || cmd.contains("log") || cmd.contains("add") -> {
                        a11yVm.hablar(loc("Abriendo formulario para anotar alimento.", "Opening form to log food."))
                        mostrarForm = true
                    }
                    cmd.contains("hoy") || cmd.contains("today") -> {
                        val hoy = FechaUtils.fechaActual()
                        vm.cambiarFecha(hoy)
                        a11yVm.hablar(loc("Cambiado a hoy.", "Changed to today."))
                    }
                    cmd.contains("ayer") || cmd.contains("yesterday") -> {
                        val ayer = FechaUtils.formatearFecha(com.example.nutriia.platform.currentTimeMillis() - 86400000L)
                        vm.cambiarFecha(ayer)
                        a11yVm.hablar(loc("Cambiado a ayer.", "Changed to yesterday."))
                    }
                    cmd.contains("resumen") || cmd.contains("summary") -> {
                        val calCount = totales.calorias.toInt()
                        a11yVm.hablar(loc("Has consumido $calCount calorías de una meta de ${recomendacion.caloriasMax} kcal.", "You have consumed $calCount calories out of a goal of ${recomendacion.caloriasMax} kcal."))
                    }
                    cmd.contains("volver") || cmd.contains("atrás") || cmd.contains("back") || cmd.contains("salir") -> {
                        onBack()
                    }
                    else -> a11yVm.hablar(loc("No entendí. Prueba con: anotar alimento, ver resumen o cambiar fecha.", "I didn't understand. Try: log food, view summary, or change date."))
                }
            }
        }
    }

    LaunchedEffect(Unit) { 
        visible = true 
        if (esBlind) {
            a11yVm.hablar(loc(
                "Módulo de nutrición para $childName. Aquí puedes llevar el control de calorías y nutrientes del día. El botón para anotar lo que comió está en la parte inferior central.",
                "Nutrition module for $childName. Here you can track daily calories and nutrients. The button to log what he ate is at the bottom center."
            ))
        }
    }

    Scaffold(
        containerColor = Sol.Bg,
        snackbarHost   = { SnackbarHost(snackbar) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                if (esBlind) {
                    FloatingActionButton(
                        onClick = { isListening = !isListening },
                        containerColor = if (voiceState == VoiceInputState.LISTENING) Color.Red else Sol.PurpleMid,
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
                        onClick        = { mostrarForm = true },
                        containerColor = Sol.Purple,
                        contentColor   = Sol.White,
                        shape          = RoundedCornerShape(20.dp),
                        modifier       = Modifier.height(52.dp).shadow(
                            8.dp, RoundedCornerShape(20.dp),
                            ambientColor = Sol.Purple.copy(.35f),
                            spotColor    = Sol.Purple.copy(.35f)
                        )
                    ) {
                        Icon(Icons.Rounded.Add, null, Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Anotar lo que comió", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                AnimatedVisibility(
                    visible = visible,
                    enter   = slideInVertically(tween(420, easing = EaseOutCubic)) { -it / 2 } + fadeIn(tween(420))
                ) {
                    NutrientesTopBar(childName, mesesEdad, onBack, registros.size)
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(300))) {
                    MascotBanner(
                        drawableRes = com.example.nutriia.resources.Res.drawable.ic_nutriente,
                        titulo      = "Nutrición de $childName",
                        subtitulo   = "Lleva el control diario de\ncalorías, macros y micronutrientes",
                        accentColor = Sol.Purple,
                        mascotSize  = 148.dp
                    )
                }
            }
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(320, 60))) {
                    FechaSelectorCard(fecha, loc("Día de hoy", "Today"), loc("Día de ayer", "Yesterday"), loc("Día de mañana", "Tomorrow")) { 
                        vm.cambiarFecha(it)
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            guiaEdad?.let { g ->
                item {
                    AnimatedVisibility(visible = visible, enter = fadeIn(tween(340, 80))) {
                        EtapaBannerCard(g)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            item {
                AnimatedVisibility(visible = visible, enter = fadeIn(tween(360, 100))) {
                    ResumenDiaCard(totales, totalesMicros, recomendacion)
                }
                Spacer(Modifier.height(8.dp))
            }
            if (registros.isEmpty()) {
                item {
                    AnimatedVisibility(visible = visible, enter = fadeIn(tween(380, 130))) {
                        EstadoVacio(Icons.Rounded.NoFood, "Aún no hay nada anotado",
                            "Toca el botón de abajo para anotar el primer alimento del día")
                    }
                }
            } else {
                item {
                    Text(
                        "Lo que comió hoy",
                        fontWeight = FontWeight.ExtraBold, fontSize = 16.sp,
                        color      = Sol.PurpleDark,
                        modifier   = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }
                itemsIndexed(registros, key = { _, it -> it.id }) { idx, reg ->
                    RegistroCard(reg, idx, onEliminar = { vm.eliminar(reg.id) })
                }
            }
        }

        if (mostrarForm) {
            AgregarRegistroDialog(
                childId   = childId,
                fecha     = fecha,
                mesesEdad = mesesEdad,
                nivel     = nivel,
                region    = region,
                esAccesible = esAccesible,
                esBlind     = esBlind,
                ttsManager = ttsManager,
                idioma    = idiomaActual,
                onGuardar = { reg -> vm.guardar(reg); mostrarForm = false },
                onCerrar  = { 
                    if (esBlind) a11yVm.hablar(loc("Registro cancelado.", "Registration cancelled."))
                    mostrarForm = false 
                }
            )
        }
    }
}

@Composable
private fun NutrientesTopBar(childName: String, meses: Int, onBack: () -> Unit, totalRegistros: Int = 0) {
    val etapa = when {
        meses < 6  -> "Lactancia"
        meses < 12 -> "Primeros sólidos"
        meses < 24 -> "Diversificación"
        meses < 60 -> "Preescolar"
        else       -> "Escolar"
    }
    val anos   = meses / 12
    val mesesR = meses % 12
    val edadTxt = when {
        anos == 0   -> "$meses ${if (meses == 1) "mes" else "meses"}"
        mesesR == 0 -> "$anos ${if (anos == 1) "año" else "años"}"
        else        -> "$anos a. $mesesR m."
    }
    val gradient = Brush.verticalGradient(listOf(Sol.PurpleLight, Sol.Bg))
    Box(
        Modifier.fillMaxWidth().background(gradient)
            .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 20.dp)
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Sol.White.copy(.8f)).align(Alignment.CenterStart)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Sol.Purple)
        }

        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Nutrientes",  fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Sol.Purple)
            Text(childName,     fontSize = 14.sp, fontWeight = FontWeight.SemiBold,  color = Sol.PurpleMid)
            Spacer(Modifier.height(2.dp))
            Surface(shape = RoundedCornerShape(50.dp), color = Sol.Purple.copy(.12f)) {
                Text(
                    "$etapa · $edadTxt",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 11.sp, color = Sol.Purple, fontWeight = FontWeight.Medium
                )
            }
        }

        Surface(shape = RoundedCornerShape(50.dp), color = Sol.White.copy(.85f), modifier = Modifier.align(Alignment.CenterEnd)) {
            Text(
                "$totalRegistros ${if (totalRegistros == 1) "registro" else "registros"}",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                fontSize = 11.sp, color = Sol.Purple, fontWeight = FontWeight.SemiBold
            )
        }
    }
    HorizontalDivider(color = Sol.PurpleLight, thickness = 1.dp)
}

@Composable
private fun FechaSelectorCard(
    fecha: String, 
    labelHoy: String = "Hoy",
    labelAyer: String = "Ayer",
    labelManana: String = "Mañana",
    onCambiar: (String) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    fun desplazar(dias: Int): String {
        val ms = com.example.nutriia.platform.currentTimeMillis() + (dias.toLong() * 86400000L)
        return FechaUtils.formatearFecha(ms)
    }
    val hoy    = desplazar(0)
    val ayer   = desplazar(-1)
    val manana = desplazar(1)

    SolCard {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.Rounded.CalendarToday, Sol.Purple, Sol.Purple.copy(.1f), 36.dp, 18.dp)
                Spacer(Modifier.width(10.dp))
                Text("¿Qué día estás registrando?", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Sol.PurpleDark)
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(labelAyer to ayer, labelHoy to hoy, labelManana to manana).forEach { (label, valor) ->
                    val sel = fecha == valor
                    val bg  by animateColorAsState(if (sel) Sol.Purple else Sol.PurpleLight, tween(200), label = "fd_$label")
                    val fg  by animateColorAsState(if (sel) Sol.White  else Sol.PurpleDark,  tween(200), label = "ft_$label")
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(bg)
                            .clickable { 
                                vibrateTap(haptic)
                                onCambiar(valor) 
                            }
                            .semantics { contentDescription = "$label. ${if (sel) "Seleccionado" else "Toca para cambiar"}" }
                            .padding(vertical = 12.dp),
                        Alignment.Center
                    ) { Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = fg) }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Sol.PurpleLight).padding(10.dp),
                Alignment.Center
            ) { Text(fecha, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Sol.Purple) }
        }
    }
}

@Composable
private fun EtapaBannerCard(guia: com.example.nutriia.solidos.GuiaEdad) {
    SolCard(bg = Sol.Purple, border = Color.Transparent) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconBox(Icons.Rounded.ChildCare, Sol.White, Sol.White.copy(.2f), 48.dp, 26.dp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(guia.rangoLabel,      color = Sol.White,         fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Text("Textura: ${guia.texturaLabel}", color = Sol.White.copy(.85f), fontSize = 12.sp)
                Text(guia.frecuenciaLabel, color = Sol.White.copy(.7f), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ResumenDiaCard(
    totales:       Macronutrientes,
    micros:        Micronutrientes,
    recomendacion: RecomendacionConectada
) {
    val metaCal  = recomendacion.caloriasMax.toDouble()
    val progreso by animateFloatAsState(
        targetValue   = if (metaCal > 0) (totales.calorias / metaCal).coerceIn(0.0, 1.0).toFloat() else 0f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label         = "calProg"
    )
    val pct = (progreso * 100).toInt()

    SolCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.Rounded.BarChart, Sol.Purple, Sol.Purple.copy(.1f), 42.dp, 22.dp)
                Spacer(Modifier.width(10.dp))
                Text("¿Cómo va el día de hoy?", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Sol.PurpleDark)
            }
            HorizontalDivider(color = Sol.Border)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${totales.calorias.toInt()}", fontWeight = FontWeight.Black, fontSize = 40.sp, color = Sol.Orange)
                        Spacer(Modifier.width(6.dp))
                        Text("/ ${recomendacion.caloriasMax} kcal", fontSize = 15.sp, color = Sol.TextSecondary, modifier = Modifier.padding(bottom = 5.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(Sol.Orange.copy(.15f))) {
                        Box(Modifier.fillMaxWidth(progreso).height(12.dp).clip(RoundedCornerShape(6.dp)).background(Sol.Orange))
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = when {
                            pct == 0   -> "Aún no hay registros"
                            pct < 50   -> "Menos de la mitad · sigue anotando"
                            pct < 85   -> "Buen avance 👍"
                            pct <= 100 -> "¡Meta del día alcanzada! ✓"
                            else       -> "Ya superó la meta del día"
                        },
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        color = when {
                            pct == 0   -> Sol.TextSecondary
                            pct < 85   -> Sol.Orange
                            pct <= 100 -> Sol.Green
                            else       -> Sol.Red
                        }
                    )
                }
                Spacer(Modifier.width(14.dp))
                Box(Modifier.size(68.dp), Alignment.Center) {
                    CircularProgressIndicator(
                        progress    = { progreso },
                        modifier    = Modifier.fillMaxSize(),
                        color       = if (totales.calorias > recomendacion.caloriasMax) Sol.Red else Sol.Orange,
                        trackColor  = Sol.Orange.copy(.15f),
                        strokeWidth = 7.dp
                    )
                    Text("$pct%", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = if (totales.calorias > recomendacion.caloriasMax) Sol.Red else Sol.Orange)
                }
            }
            HorizontalDivider(color = Sol.Border)
            SeccionLabel("Nutrientes principales", Icons.Rounded.FitnessCenter)
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MacroTarjeta(Icons.Rounded.FitnessCenter, totales.proteinas,     recomendacion.proteinasG,                              "g", "Proteína", Sol.Red,          Modifier.weight(1f))
                MacroTarjeta(Icons.Rounded.Opacity,       totales.grasas,        recomendacion.caloriasMax * recomendacion.grasasPorc / 400, "g", "Grasas",   Color(0xFFAB47BC), Modifier.weight(1f))
                MacroTarjeta(Icons.Rounded.Grain,         totales.carbohidratos, recomendacion.caloriasMax * recomendacion.carbosPorc / 400, "g", "Energía",  Sol.Green,        Modifier.weight(1f))
            }
            HorizontalDivider(color = Sol.Border)
            SeccionLabel("Vitaminas y minerales", Icons.Rounded.Science)
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                MicroCirculo("Hierro", micros.hierro,    recomendacion.hierroMg, "mg", Sol.Red)
                MicroCirculo("Calcio", micros.calcio,    recomendacion.calcioMg, "mg", Sol.Blue)
                MicroCirculo("Zinc",   micros.zinc,      recomendacion.zincMg,   "mg", Color(0xFF6A1B9A))
                MicroCirculo("Vit C",  micros.vitaminaC, 50.0,                    "mg", Sol.Green)
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Sol.PurpleLight).padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Info, null, tint = Sol.Purple, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text("Los círculos se llenan conforme anotas los alimentos del día.", fontSize = 12.sp, color = Sol.Purple, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun MacroTarjeta(
    icon:     ImageVector,
    valor:    Double,
    meta:     Double,
    unidad:   String,
    etiqueta: String,
    color:    Color,
    modifier: Modifier = Modifier
) {
    val prog by animateFloatAsState(
        targetValue   = if (meta > 0) (valor / meta).coerceIn(0.0, 1.0).toFloat() else 0f,
        animationSpec = tween(800, easing = EaseOutCubic),
        label         = "mt_$etiqueta"
    )
    Box(modifier.clip(RoundedCornerShape(14.dp)).background(color.copy(.08f)).padding(10.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(5.dp))
            Text("${valor.toInt()}", fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
            Text(unidad, fontSize = 10.sp, color = color.copy(.7f))
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(color.copy(.18f))) {
                Box(Modifier.fillMaxWidth(prog).height(6.dp).clip(RoundedCornerShape(3.dp)).background(color))
            }
            Spacer(Modifier.height(3.dp))
            Text(etiqueta, fontSize = 9.sp, color = Sol.TextMuted, textAlign = TextAlign.Center)
            Text("/${meta.toInt()}", fontSize = 9.sp, color = Sol.TextSecondary)
        }
    }
}

@Composable
private fun MicroCirculo(label: String, valor: Double, meta: Double, unidad: String, color: Color) {
    val prog by animateFloatAsState(
        targetValue   = if (meta > 0) (valor / meta).coerceIn(0.0, 1.0).toFloat() else 0f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label         = "mc_$label"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(60.dp), Alignment.Center) {
            CircularProgressIndicator(
                progress    = { prog },
                modifier    = Modifier.fillMaxSize(),
                color       = color,
                trackColor  = color.copy(.15f),
                strokeWidth = 5.dp
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${valor.toInt()}", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = color)
                Text(unidad, fontSize = 8.sp, color = color.copy(.7f))
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(label,             fontSize = 11.sp, color = Sol.TextPrimary, fontWeight = FontWeight.SemiBold)
        Text("/${meta.toInt()}$unidad", fontSize = 9.sp,  color = Sol.TextSecondary)
    }
}

@Composable
private fun RegistroCard(reg: RegistroNutrientes, index: Int, onEliminar: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index.coerceAtMost(10) * 55L); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter   = slideInHorizontally(tween(340, easing = EaseOutCubic)) { -60 } + fadeIn(tween(340))
    ) {
        SolCard(shape = RoundedCornerShape(16.dp)) {
            Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBox(
                    icon   = iconoPorComida(reg.comida),
                    tint   = Sol.Purple,
                    bg     = Sol.Purple.copy(.12f),
                    size   = 54.dp, iconSz = 28.dp,
                    shape  = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(reg.alimento, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Sol.TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Chip(Icons.Rounded.Restaurant, reg.comida, Sol.Purple)
                        if (reg.macros.calorias > 0)
                            Chip(Icons.Rounded.LocalFireDepartment, "${reg.macros.calorias.toInt()} kcal", Sol.Orange)
                    }
                }
                IconButton(onEliminar, Modifier.size(34.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(17.dp))
                }
            }
        }
    }
}

private fun iconoPorComida(comida: String): ImageVector = when {
    comida.contains("desayuno", ignoreCase = true) -> Icons.Rounded.WbSunny
    comida.contains("media",    ignoreCase = true) -> Icons.Rounded.Coffee
    comida.contains("almuerzo", ignoreCase = true) -> Icons.Rounded.Restaurant
    comida.contains("merienda", ignoreCase = true) -> Icons.Rounded.EmojiFoodBeverage
    comida.contains("cena",     ignoreCase = true) -> Icons.Rounded.Nightlight
    else                                           -> Icons.Rounded.Restaurant
}

@Composable
private fun EstadoVacio(icon: ImageVector, texto: String, subtexto: String) {
    val inf = rememberInfiniteTransition(label = "ev")
    val sc  by inf.animateFloat(.94f, 1.06f, infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "evs")
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Sol.Purple.copy(.08f)).graphicsLayer { 
            scaleX = sc
            scaleY = sc 
        }, Alignment.Center) {
            Icon(icon, null, tint = Sol.Purple.copy(.5f), modifier = Modifier.size(40.dp))
        }
        Text(texto,    color = Sol.Purple,        fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, fontSize = 15.sp)
        Text(subtexto, color = Sol.TextSecondary, fontSize = 12.sp,                 textAlign = TextAlign.Center, lineHeight = 17.sp)
    }
}

@Composable
private fun AgregarRegistroDialog(
    childId:   String,
    fecha:     String,
    mesesEdad: Int,
    nivel:     NivelIngreso,
    region:      RegionMexico,
    esAccesible: Boolean   = false,
    esBlind:     Boolean   = false,
    ttsManager: NutriTTS? = null,
    idioma:    IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    onGuardar: (RegistroNutrientes) -> Unit,
    onCerrar:  () -> Unit
) {
    val comidas      = listOf("Desayuno", "Media mañana", "Almuerzo", "Merienda", "Cena")
    var comida       by remember { mutableStateOf(comidas[0]) }
    var alimento     by remember { mutableStateOf("") }
    var mostrarNutri by remember { mutableStateOf(false) }
    val solidosList  by remember(childId) {
        com.example.nutriia.solidos.SolidosRepository().observarAlimentos(childId)
    }.collectAsState(initial = emptyList())
    var calorias  by remember { mutableStateOf("") }
    var proteinas by remember { mutableStateOf("") }
    var grasas    by remember { mutableStateOf("") }
    var carbos    by remember { mutableStateOf("") }
    var hierro    by remember { mutableStateOf("") }
    var calcio    by remember { mutableStateOf("") }
    var vitA      by remember { mutableStateOf("") }
    var vitC      by remember { mutableStateOf("") }
    var zinc      by remember { mutableStateOf("") }

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }
    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> alimento
            1 -> calorias
            2 -> proteinas
            else -> ""
        }
    }

    LaunchedEffect(alimento) {
        if (!esBlind || alimento.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (alimento == valorInicial) return@LaunchedEffect
        delay(2000L)
        if (alimento.isNotBlank() && campoActivo == 0 && alimento != valorInicial) {
            campoActivo = if (mostrarNutri) 1 else 3
        }
    }

    val guardarTodo = {
        if (alimento.isNotBlank()) {
            if (esBlind) {
                ttsManager?.hablar(if (idioma == IdiomaVoz.INGLES) "Save" else "Guardar")
            }
            onGuardar(
                RegistroNutrientes(
                    childId  = childId,
                    fecha    = fecha,
                    comida   = comida,
                    alimento = alimento,
                    macros   = Macronutrientes(
                        calorias      = calorias.toDoubleOrNull()  ?: 0.0,
                        proteinas     = proteinas.toDoubleOrNull() ?: 0.0,
                        grasas        = grasas.toDoubleOrNull()    ?: 0.0,
                        carbohidratos = carbos.toDoubleOrNull()    ?: 0.0
                    ),
                    micros   = Micronutrientes(
                        hierro    = hierro.toDoubleOrNull() ?: 0.0,
                        calcio    = calcio.toDoubleOrNull() ?: 0.0,
                        vitaminaA = vitA.toDoubleOrNull()   ?: 0.0,
                        vitaminaC = vitC.toDoubleOrNull()   ?: 0.0,
                        zinc      = zinc.toDoubleOrNull()   ?: 0.0
                    )
                )
            )
        }
    }

    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = Sol.Purple,
        unfocusedBorderColor = Sol.PurpleLight,
        focusedLabelColor    = Sol.Purple,
        cursorColor          = Sol.Purple
    )

    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    AlertDialog(
        onDismissRequest = onCerrar,
        shape            = RoundedCornerShape(28.dp),
        containerColor   = Sol.Bg,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.Rounded.AddCircle, Sol.Purple, Sol.Purple.copy(.1f), 34.dp, 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(loc("¿Qué comió?", "What did he eat?"), fontWeight = FontWeight.ExtraBold, color = Sol.Purple, fontSize = 17.sp)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                item {
                    Text(loc("¿En qué momento del día?", "At what time of day?"), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Sol.PurpleDark)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        comidas.chunked(3).forEach { fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                fila.forEach { c ->
                                    val sel = comida == c
                                    val bg by animateColorAsState(if (sel) Sol.Purple else Sol.PurpleLight, tween(180), label = "c_$c")
                                    val fg by animateColorAsState(if (sel) Sol.White  else Sol.PurpleDark,  tween(180), label = "ct_$c")
                                    Box(
                                        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(bg)
                                            .clickable { 
                                                comida = c 
                                                if (esBlind) ttsManager?.hablar(loc("Seleccionado: $c", "Selected: $c"))
                                            }.padding(vertical = 11.dp),
                                        Alignment.Center
                                    ) { Text(c, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = fg, textAlign = TextAlign.Center) }
                                }
                            }
                        }
                    }
                }
                item {
                    if (esAccesible) {
                        CampoTextoAccesible(
                            valor          = alimento,
                            onValorChange  = { alimento = it },
                            etiqueta       = loc("Nombre del alimento", "Food name"),
                            descripcionVoz = loc("Di el nombre de lo que comió.", "Say the name of what he ate."),
                            ttsManager     = ttsManager,
                            idioma         = idioma,
                            colorPrimario  = Sol.Purple,
                            activo         = campoActivo == 0,
                            onFocus        = { campoActivo = 0 },
                            onNext         = { if (mostrarNutri) campoActivo = 1 else guardarTodo() }
                        )
                    } else {
                        OutlinedTextField(
                            value = alimento, onValueChange = { alimento = it },
                            label       = { Text("Nombre del alimento o platillo") },
                            leadingIcon = { Icon(Icons.Rounded.Restaurant, null, tint = Sol.Purple) },
                            modifier    = Modifier.fillMaxWidth(),
                            shape       = RoundedCornerShape(14.dp),
                            singleLine  = true,
                            colors      = fc
                        )
                    }
                }
                item {
                    val safeSolids = solidosList.filter { it.reaccion != com.example.nutriia.solidos.ReaccionAlimento.ALERGIA }
                    val query = alimento
                    val filteredSolids = if (query.isBlank()) safeSolids else safeSolids.filter { it.nombre.contains(query, ignoreCase = true) }
                    if (filteredSolids.isNotEmpty()) {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(loc("Sugerencias:", "Suggestions:"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Sol.Purple, modifier = Modifier.padding(bottom = 6.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                items(filteredSolids) { solid ->
                                    SuggestionChip(
                                        onClick = { alimento = solid.nombre },
                                        label = { Text(solid.nombre, fontSize = 11.sp) }
                                    )
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
                enabled  = alimento.isNotBlank(),
                colors   = ButtonDefaults.buttonColors(containerColor = Sol.Purple),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Icon(Icons.Rounded.Check, null, Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text(loc("Guardar", "Save"), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onCerrar) { Text(loc("Cancelar", "Cancel"), color = Sol.TextMuted) } }
    )
}

@Composable
private fun CampoNutri(
    value:    String,
    onValue:  (String) -> Unit,
    label:    String,
    modifier: Modifier,
    colors:   TextFieldColors
) = OutlinedTextField(
    value         = value,
    onValueChange = onValue,
    label         = { Text(label, fontSize = 11.sp) },
    modifier      = modifier,
    shape         = RoundedCornerShape(12.dp),
    singleLine    = true,
    colors        = colors
)
