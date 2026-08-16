package com.example.nutriia.dashboard

// import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.VoiceInputManager
import com.example.nutriia.accesibilidad.VoiceInputState
import com.example.nutriia.crecimiento.CrecimientoViewModel
import com.example.nutriia.crecimiento.MedicionCrecimiento
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.ui.theme.EtapaInfo

// ═══════════════════════════════════════════════════════════════════════════════
// PALETA
// ═══════════════════════════════════════════════════════════════════════════════

private val DashBgCrema         = Color(0xFFF9F8F4)
private val DashNutriaGreen     = Color(0xFF4CAF50)
private val DashNutriaDarkGreen = Color(0xFF1B5E20)
private val DashSoftPurple      = Color(0xFF9C8FE0)
private val DashSoftTeal        = Color(0xFF4DB6AC)
private val DashOrange          = Color(0xFFFF8F00)
private val DashPink            = Color(0xFFEC9BBF)
private val DashBlue            = Color(0xFF64B5F6)
private val DashCardWhite       = Color.White
private val DashSoftGreen       = Color(0xFF81C784)
private val DashSoftOrange      = Color(0xFFFFAB76)

private val dashAvatarColors = listOf(
    DashPink, DashSoftPurple, DashSoftOrange,
    DashSoftTeal, DashSoftGreen, DashBlue
)

// ═══════════════════════════════════════════════════════════════════════════════
// MODELO INTERNO
// ═══════════════════════════════════════════════════════════════════════════════

private data class DashModule(
    val title:   String,
    val icon:    ImageVector,
    val color:   Color,
    val isReady: Boolean = true,
    val onClick: () -> Unit = {}
)

// ═══════════════════════════════════════════════════════════════════════════════
// TEXTOS DE ACCESIBILIDAD
// ═══════════════════════════════════════════════════════════════════════════════

private object VozDash {
    fun bienvenidaPadre(nombre: String, etapa: String, edad: String) =
        "Bienvenido. Entraste como padre o madre. " +
                "Estás viendo el perfil de $nombre. " +
                "Tu hijo está en etapa $etapa, con $edad de vida. " +
                "Puedes deslizar hacia los lados en la parte superior para cambiar de hijo. " +
                "Debajo hay tarjetas con información de crecimiento y módulos de seguimiento. " +
                "El botón Consultar NutriBot está en la parte inferior central. " +
                "Los botones Ajustes y Salir están en la esquina superior derecha."

    fun moduloAbierto(nombre: String) = "Abriendo módulo $nombre."

    fun cambioHijo(nombre: String, etapa: String) =
        "Ahora viendo el perfil de $nombre. Etapa: $etapa."

    const val MODULOS_SECCION =
        "Sección de módulos. " +
                "Los módulos disponibles son: Lactancia, Alimentación, Crecimiento, Nutrientes, " +
                "Análisis NutriIA, Alertas y Pediatra o Nutriólogo. " +
                "El módulo próximamente disponible es: Sueño. " +
                "Toca cualquier módulo disponible para abrirlo."
    
    const val COMANDOS_GUIA = 
        "Te escucho. Puedes decir: Lactancia, Alimentación, Crecimiento, Nutrientes, Pediatra, Análisis, Alertas, NutriBot, Ajustes, Ayuda o Salir. ¿Hacia qué módulo se va a dirigir?"
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPERS
// ═══════════════════════════════════════════════════════════════════════════════

private fun parseFechaComponents(fecha: String): Triple<Int, Int, Int> {
    return if (fecha.contains("/")) {
        val p = fecha.split("/").map { it.toInt() }
        Triple(p[2], p[1], p[0])
    } else {
        val p = fecha.split("-").map { it.toInt() }
        Triple(p[0], p[1], p[2])
    }
}

private fun calcAgeMonths(fecha: String): Int = runCatching {
    val (anio, mes, dia) = parseFechaComponents(fecha)
    val currentYear = 2026
        val currentMonth = 8
        val diffYears = currentYear - anio
        val diffMonths = currentMonth - mes
    val totalMonths = diffYears * 12 + diffMonths
    if (totalMonths < 0) 0 else totalMonths
}.getOrDefault(0)

internal fun calcularEtapa(fecha: String): EtapaInfo {
    val m = calcAgeMonths(fecha)
    return when {
        m < 6   -> EtapaInfo("Lactancia Exclusiva", "0 – 5 meses",  "")
        m < 12  -> EtapaInfo("Iniciando Sólidos",   "6 – 11 meses", "")
        m < 24  -> EtapaInfo("Primera Infancia",    "1 – 2 años",   "")
        m < 60  -> EtapaInfo("Preescolar",          "2 – 5 años",   "")
        m < 144 -> EtapaInfo("Escolar",             "5 – 12 años",  "")
        else    -> EtapaInfo("Adolescente",         "12+ años",     "")
    }
}

internal fun colorDeEtapa(nombreEtapa: String): Color = when (nombreEtapa) {
    "Lactancia Exclusiva" -> DashPink
    "Iniciando Sólidos"   -> DashOrange
    "Primera Infancia"    -> DashNutriaGreen
    "Preescolar"          -> DashBlue
    "Escolar"             -> DashSoftPurple
    else                  -> DashSoftTeal
}

internal fun obtenerEdadTexto(fecha: String): String = runCatching {
    val (anio, mes, dia) = parseFechaComponents(fecha)
    val calNac = java.util.Calendar.getInstance().apply { set(anio, mes - 1, dia) }
    val calHoy = java.util.Calendar.getInstance()
    var years = calHoy.get(java.util.Calendar.YEAR) - calNac.get(java.util.Calendar.YEAR)
    var months = calHoy.get(java.util.Calendar.MONTH) - calNac.get(java.util.Calendar.MONTH)
    var days = calHoy.get(java.util.Calendar.DAY_OF_MONTH) - calNac.get(java.util.Calendar.DAY_OF_MONTH)

    if (days < 0) {
        months -= 1
        val prevMonth = (calHoy.get(java.util.Calendar.MONTH) - 1 + 12) % 12
        val tempCal = java.util.Calendar.getInstance().apply { set(java.util.Calendar.MONTH, prevMonth) }
        days += tempCal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }
    if (months < 0) {
        years -= 1
        months += 12
    }

    when {
        years > 0 -> buildString {
            append("$years año${if (years > 1) "s" else ""}")
            if (months > 0) append(" y $months mes${if (months > 1) "es" else ""}")
        }
        months > 0 -> "$months mes${if (months > 1) "es" else ""}"
        else       -> "$days día${if (days > 1) "s" else ""}"
    }
}.getOrDefault("Edad desconocida")

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun NutriIADashboardScreen(
    children:              List<ChildProfile>,
    initialPageIndex:      Int     = 0,
    esNutriologo:          Boolean = false,
    onPageChange:          (Int) -> Unit = {},
    onLogout:              () -> Unit,
    onConfiguracion:       () -> Unit    = {},
    onAddChild:            () -> Unit,
    onOpenChatIA:          (Int) -> Unit = {},
    onOpenLactancia:       (Int) -> Unit = {},
    onOpenSolidos:         (Int) -> Unit = {},
    onOpenCrecimiento:     (Int) -> Unit = {},
    onOpenSueno:           (Int) -> Unit = {},
    onOpenMicronutrientes: (Int) -> Unit = {},
    onOpenPediatra:        (Int) -> Unit = {},
    onOpenDiario:          (Int) -> Unit = {},
    onOpenRecordatorios:   (Int) -> Unit = {},
    onEditarPerfil:        (ChildProfile) -> Unit = {},
    onAyuda:               () -> Unit = {}
) {
    if (children.isEmpty()) return

    val a11yMode         = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()

    val pagerState = rememberPagerState(
        initialPage               = initialPageIndex.coerceIn(0, children.lastIndex),
        initialPageOffsetFraction = 0f,
        pageCount                 = { children.size }
    )

    val currentPage = pagerState.currentPage
    val child       = children.getOrNull(currentPage) ?: return
    val etapa       = calcularEtapa(child.birthDate)
    val etapaColor  = colorDeEtapa(etapa.nombre)
    val edadInfo    = obtenerEdadTexto(child.birthDate)
    val ageMonths   = calcAgeMonths(child.birthDate)

    LaunchedEffect(currentPage) { onPageChange(currentPage) }

    LaunchedEffect(Unit) {
        if (a11yMode != AccessibilityMode.BLIND) return@LaunchedEffect
        val msg = if (esNutriologo)
            "Bienvenido. Entraste como nutriólogo o nutrióloga. " +
                    "Aquí puedes gestionar los perfiles de tus pacientes. " +
                    "Los botones Ajustes y Salir están en la esquina superior derecha."
        else
            VozDash.bienvenidaPadre(child.name, etapa.nombre, edadInfo)
        a11yVm.hablar(msg)
    }

    LaunchedEffect(currentPage) {
        if (a11yMode == AccessibilityMode.BLIND && currentPage > 0) {
            val e = calcularEtapa(children[currentPage].birthDate)
            a11yVm.hablar(VozDash.cambioHijo(children[currentPage].name, e.nombre))
        }
    }

    val crecimientoVm: CrecimientoViewModel = viewModel(key = "crec_${child.id}")
    LaunchedEffect(child.id) { crecimientoVm.init(child.id, ageMonths, child.sexo) }

    val ultimaMedicion by crecimientoVm.ultimaMedicion.collectAsState()
    val historialCrec  by crecimientoVm.historial.collectAsState()

    val modules = buildModuleList(
        currentPage           = currentPage,
        a11yMode              = a11yMode,
        a11yVm                = a11yVm,
        onOpenLactancia       = onOpenLactancia,
        onOpenSolidos         = onOpenSolidos,
        onOpenCrecimiento     = onOpenCrecimiento,
        onOpenSueno           = onOpenSueno,
        onOpenMicronutrientes = onOpenMicronutrientes,
        onOpenPediatra        = onOpenPediatra,
        onOpenChatIA          = onOpenChatIA,
        onOpenDiario          = onOpenDiario,
        onOpenRecordatorios   = onOpenRecordatorios
    )

    // ── Voice Commands Logic ──────────────────────────────────────────────────
    var isListening by remember { mutableStateOf(false) }
    val voiceManager = remember { VoiceInputManager() }
    val voiceState by voiceManager.estado

    // FIX: Evitar fugas de ServiceConnection al destruir el Composable
    DisposableEffect(Unit) {
        onDispose {
            voiceManager.liberar()
        }
    }

    LaunchedEffect(isListening) {
        if (isListening && a11yMode == AccessibilityMode.BLIND) {
            a11yVm.hablar(VozDash.COMANDOS_GUIA)
            // Aumentamos el delay a 9 segundos para permitir que el TTS termine la lista de comandos y la pregunta final
            kotlinx.coroutines.delay(9500)
            voiceManager.escuchar(a11yVm.idioma.value, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                isListening = false
                val cmd = result.lowercase().trim()
                when {
                    cmd.contains("lactancia") -> {
                        a11yVm.hablar(VozDash.moduloAbierto("Lactancia"))
                        onOpenLactancia(currentPage)
                    }
                    cmd.contains("alimentos") || cmd.contains("alimentación") || cmd.contains("solidos") || cmd.contains("alimentacion") -> {
                        a11yVm.hablar(VozDash.moduloAbierto("Alimentación"))
                        onOpenSolidos(currentPage)
                    }
                    cmd.contains("crecimiento") -> {
                        a11yVm.hablar(VozDash.moduloAbierto("Crecimiento"))
                        onOpenCrecimiento(currentPage)
                    }
                    cmd.contains("nutrientes") -> {
                        a11yVm.hablar(VozDash.moduloAbierto("Nutrientes"))
                        onOpenMicronutrientes(currentPage)
                    }
                    cmd.contains("pediatra") || cmd.contains("nutriólogo") || cmd.contains("medico") || cmd.contains("médico") -> {
                        a11yVm.hablar(VozDash.moduloAbierto("Pediatra o Nutriólogo"))
                        onOpenPediatra(currentPage)
                    }
                    cmd.contains("análisis") || cmd.contains("analisis") || cmd.contains("ia") || cmd.contains("diario") -> {
                        a11yVm.hablar(VozDash.moduloAbierto("Análisis NutriIA"))
                        onOpenDiario(currentPage)
                    }
                    cmd.contains("alarmas") || cmd.contains("alertas") || cmd.contains("alerta") || cmd.contains("alarma") -> {
                        a11yVm.hablar(VozDash.moduloAbierto("Alertas"))
                        onOpenRecordatorios(currentPage)
                    }
                    cmd.contains("nutribot") || cmd.contains("chat") || cmd.contains("consultar") -> {
                        a11yVm.hablar("Abriendo NutriBot.")
                        onOpenChatIA(currentPage)
                    }
                    cmd.contains("ajustes") || cmd.contains("configuración") || cmd.contains("configuracion") -> {
                        a11yVm.hablar("Abriendo ajustes.")
                        onConfiguracion()
                    }
                    cmd.contains("ayuda") -> {
                        a11yVm.hablar("Abriendo centro de ayuda.")
                        onAyuda()
                    }
                    cmd.contains("salir") || cmd.contains("cerrar sesión") -> {
                        a11yVm.hablar("Cerrando sesión.")
                        onLogout()
                    }
                    else -> a11yVm.hablar("No entendí el comando. Intenta decir abrir crecimiento o alarmas.")
                }
            }
        }
    }

    // ── FAB pulse ──────────────────────────────────────────────────────────────
    val fabInf = rememberInfiniteTransition(label = "fabPulse")
    val fabShadowAlpha by fabInf.animateFloat(
        initialValue  = 0.35f,
        targetValue   = 0.75f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "fabAlpha"
    )
    val fabScale by fabInf.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.04f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "fabScale"
    )

    Scaffold(
        containerColor = DashBgCrema,
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (a11yMode == AccessibilityMode.BLIND) {
                    FloatingActionButton(
                        onClick = { isListening = !isListening },
                        containerColor = if (voiceState == VoiceInputState.LISTENING) Color.Red else DashSoftPurple,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 12.dp).size(72.dp)
                            .shadow(
                                elevation = 12.dp,
                                shape = CircleShape,
                                ambientColor = DashSoftPurple.copy(alpha = 0.5f),
                                spotColor = DashSoftPurple.copy(alpha = 0.5f)
                            )
                            .semantics { contentDescription = if (voiceState == VoiceInputState.LISTENING) "Detener comandos de voz" else "Activar comandos de voz para navegación. Al presionar, escucha la lista de comandos disponibles." }
                    ) {
                        Icon(if (voiceState == VoiceInputState.LISTENING) Icons.Rounded.Stop else Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(34.dp))
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar("Abriendo NutriBot. El asistente de inteligencia artificial.")
                        onOpenChatIA(currentPage)
                    },
                    containerColor = DashNutriaGreen,
                    contentColor   = DashCardWhite,
                    shape          = CircleShape,
                    modifier       = Modifier
                        .padding(bottom = 16.dp)
                        .scale(fabScale)
                        .shadow(
                            elevation    = 16.dp,
                            shape        = CircleShape,
                            ambientColor = DashNutriaGreen.copy(fabShadowAlpha),
                            spotColor    = DashNutriaGreen.copy(fabShadowAlpha)
                        )
                        .semantics { contentDescription = "Botón Consultar NutriBot. Parte inferior central." }
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Consultar NutriBot", fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            DashboardTopBar(
                onLogout        = onLogout,
                onConfiguracion = onConfiguracion,
                onAyuda         = onAyuda,
                a11yMode        = a11yMode,
                a11yVm          = a11yVm
            )
            ChildSelectorPager(children, pagerState, onAddChild, a11yMode)

            LazyColumn(
                modifier            = Modifier.fillMaxSize(),
                contentPadding      = PaddingValues(start = 20.dp, end = 20.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                item {
                    EntranceAnimatedSection(delayMs = 0) {
                        EtapaStatusCard(
                            child          = child,
                            etapa          = etapa,
                            etapaColor     = etapaColor,
                            edad           = edadInfo,
                            ultimaMedicion = ultimaMedicion
                        )
                    }
                }
                item {
                    EntranceAnimatedSection(delayMs = 80) {
                        FamilyContextBannerCompact(
                            nivelIngreso = child.nivelIngreso,
                            region       = child.region
                        )
                    }
                }
                item {
                    EntranceAnimatedSection(delayMs = 140) {
                        AiRecommendationBanner(etapa.nombre)
                    }
                }
                item {
                    EntranceAnimatedSection(delayMs = 200) {
                        GrowthSection(
                            ultimaMedicion    = ultimaMedicion,
                            historial         = historialCrec,
                            onOpenCrecimiento = {
                                if (a11yMode == AccessibilityMode.BLIND)
                                    a11yVm.hablar(VozDash.moduloAbierto("Crecimiento"))
                                onOpenCrecimiento(currentPage)
                            }
                        )
                    }
                }
                item {
                    EntranceAnimatedSection(delayMs = 260) {
                        ModulesSection(modules)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// ANIMACIÓN DE ENTRADA REUTILIZABLE — slide up + fade
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun EntranceAnimatedSection(
    delayMs: Int,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs.toLong())
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(420, easing = EaseOutCubic)) +
                slideInVertically(tween(420, easing = EaseOutCubic)) { (it * 0.25f).toInt() }
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// LISTA DE MÓDULOS
// ═══════════════════════════════════════════════════════════════════════════════

private fun buildModuleList(
    currentPage:           Int,
    a11yMode:              AccessibilityMode,
    a11yVm:                AccessibilityViewModel,
    onOpenLactancia:       (Int) -> Unit,
    onOpenSolidos:         (Int) -> Unit,
    onOpenCrecimiento:     (Int) -> Unit,
    onOpenSueno:           (Int) -> Unit,
    onOpenMicronutrientes: (Int) -> Unit,
    onOpenPediatra:        (Int) -> Unit,
    onOpenChatIA:          (Int) -> Unit,
    onOpenDiario:          (Int) -> Unit,
    onOpenRecordatorios:   (Int) -> Unit
): List<DashModule> = listOf(
    DashModule("Lactancia",            Icons.Rounded.ChildCare,              DashPink,        true)  {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(VozDash.moduloAbierto("Lactancia"))
        onOpenLactancia(currentPage)
    },
    DashModule("Alimentación",         Icons.Rounded.Restaurant,             DashOrange,      true)  {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(VozDash.moduloAbierto("Alimentación"))
        onOpenSolidos(currentPage)
    },
    DashModule("Crecimiento",          Icons.AutoMirrored.Rounded.ShowChart, DashNutriaGreen, true)  {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(VozDash.moduloAbierto("Crecimiento"))
        onOpenCrecimiento(currentPage)
    },
    DashModule("Sueño",                Icons.Rounded.Bedtime,                DashSoftPurple,  false) {
        onOpenSueno(currentPage)
    },
    DashModule("Nutrientes",           Icons.Rounded.Medication,             DashSoftTeal,    true)  {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(VozDash.moduloAbierto("Nutrientes"))
        onOpenMicronutrientes(currentPage)
    },
    DashModule("Pediatra /\nNutriólogo", Icons.Rounded.MedicalServices,     DashBlue,        true)  {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(VozDash.moduloAbierto("Pediatra o Nutriólogo"))
        onOpenPediatra(currentPage)
    },
    DashModule("Análisis NutriIA",     Icons.Rounded.PhotoCamera,            DashSoftOrange,  true)  {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(VozDash.moduloAbierto("Análisis NutriIA"))
        onOpenDiario(currentPage)
    },
    DashModule("Alertas",              Icons.Rounded.NotificationsActive,    DashBlue,        true)  {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(VozDash.moduloAbierto("Alertas"))
        onOpenRecordatorios(currentPage)
    }
)

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DashboardTopBar(
    onLogout:        () -> Unit,
    onConfiguracion: () -> Unit              = {},
    onAyuda:         () -> Unit              = {},
    a11yMode:        AccessibilityMode       = AccessibilityMode.NORMAL,
    a11yVm:          AccessibilityViewModel? = null
) {
    // ── Punto pulsante "Seguimiento activo" ───────────────────────────────────
    val dotInf = rememberInfiniteTransition(label = "dotPulse")
    val dotAlpha by dotInf.animateFloat(
        initialValue  = 0.4f,
        targetValue   = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "dotAlpha"
    )
    val dotScale by dotInf.animateFloat(
        initialValue  = 0.85f,
        targetValue   = 1.2f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "dotScale"
    )

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("NutriIA", fontSize = 24.sp, fontWeight = FontWeight.Black, color = DashNutriaDarkGreen)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .scale(dotScale)
                        .clip(CircleShape)
                        .background(DashNutriaGreen.copy(alpha = dotAlpha))
                        .semantics { invisibleToUser() }
                )
                Spacer(Modifier.width(6.dp))
                Text("Seguimiento activo", fontSize = 12.sp, color = Color.Gray)
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            TopBarIconButton(
                icon     = Icons.AutoMirrored.Rounded.HelpOutline,
                label    = "Ayuda",
                a11yDesc = "Botón Ayuda. Esquina superior derecha.",
                onClick  = {
                    if (a11yMode == AccessibilityMode.BLIND) a11yVm?.hablar("Abriendo centro de ayuda.")
                    onAyuda()
                }
            )
            TopBarIconButton(
                icon     = Icons.Rounded.Settings,
                label    = "Ajustes",
                a11yDesc = "Botón Ajustes. Esquina superior derecha.",
                onClick  = {
                    if (a11yMode == AccessibilityMode.BLIND) a11yVm?.hablar("Abriendo ajustes.")
                    onConfiguracion()
                }
            )
            TopBarIconButton(
                icon     = Icons.AutoMirrored.Rounded.ExitToApp,
                label    = "Salir",
                a11yDesc = "Botón cerrar sesión. Esquina superior derecha.",
                onClick  = {
                    if (a11yMode == AccessibilityMode.BLIND) a11yVm?.hablar("Cerrando sesión.")
                    onLogout()
                }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TopBarIconButton(
    icon:     ImageVector,
    label:    String,
    a11yDesc: String,
    onClick:  () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.88f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "iconBtnScale"
    )
    LaunchedEffect(pressed) {
        if (pressed) { kotlinx.coroutines.delay(100); pressed = false }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.semantics { contentDescription = a11yDesc }
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(DashCardWhite)
                .clickable(onClickLabel = label) { pressed = true; onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = DashNutriaGreen, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, fontSize = 9.sp, color = DashNutriaGreen, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SELECTOR DE HIJO (PAGER)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChildSelectorPager(
    children:   List<ChildProfile>,
    pagerState: PagerState,
    onAddChild: () -> Unit,
    a11yMode:   AccessibilityMode = AccessibilityMode.NORMAL
) {
    Column {
        HorizontalPager(
            state          = pagerState,
            contentPadding = PaddingValues(horizontal = 48.dp),
            pageSpacing    = 16.dp,
            modifier       = Modifier.fillMaxWidth()
        ) { page ->
            val isSelected = pagerState.currentPage == page
            val scale by animateFloatAsState(
                targetValue   = if (isSelected) 1f else 0.9f,
                animationSpec = spring(Spring.DampingRatioMediumBouncy),
                label         = "childCardScale_$page"
            )
            ChildProfileSmallCard(children[page], page, isSelected, scale)
        }
        Row(
            modifier              = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            repeat(children.size) { i ->
                val dotSize by animateDpAsState(
                    targetValue   = if (pagerState.currentPage == i) 8.dp else 5.dp,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label         = "dot_$i"
                )
                val dotColor by animateColorAsState(
                    targetValue   = if (pagerState.currentPage == i) DashNutriaGreen else Color.LightGray,
                    animationSpec = tween(200),
                    label         = "dotColor_$i"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .size(dotSize)
                        .clip(CircleShape)
                        .background(dotColor)
                        .semantics { invisibleToUser() }
                )
            }
            Spacer(Modifier.width(12.dp))
            val addIconSize = if (a11yMode == AccessibilityMode.BLIND) 28.dp else 20.dp
            Icon(
                Icons.Rounded.AddCircle,
                contentDescription = "Añadir hijo",
                tint               = DashNutriaGreen.copy(alpha = 0.6f),
                modifier           = Modifier
                    .size(addIconSize)
                    .clickable(onClickLabel = "Registrar otro niño") { onAddChild() }
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ChildProfileSmallCard(
    child:      ChildProfile,
    index:      Int,
    isSelected: Boolean,
    scale:      Float
) {
    val color = dashAvatarColors[index % dashAvatarColors.size]
    val sexoLabel = when (child.sexo) {
        Sexo.NINO -> "Niño"
        Sexo.NINA -> "Niña"
        null      -> "Perfil activo"
    }
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .height(90.dp)
            .scale(scale)
            .semantics {
                contentDescription =
                    "${child.name}. $sexoLabel. " +
                            if (isSelected) "Perfil activo." else "Desliza para seleccionar."
            },
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isSelected) DashCardWhite else Color(0xFFE0E0E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 8.dp else 0.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.2f))
                    .border(2.dp, color, CircleShape)
                    .semantics { invisibleToUser() },
                contentAlignment = Alignment.Center
            ) {
                Text(child.name.take(1).uppercase(), fontWeight = FontWeight.Black, color = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(child.name, fontWeight = FontWeight.Bold, color = DashNutriaDarkGreen)
                Text(sexoLabel, fontSize = 11.sp, color = Color.Gray)
            }
            if (isSelected) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = DashNutriaGreen)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BANNER FAMILIAR
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun FamilyContextBannerCompact(
    nivelIngreso: NivelIngreso,
    region:       RegionMexico
) {
    val accentColor = when (nivelIngreso) {
        NivelIngreso.BASICO     -> DashNutriaGreen
        NivelIngreso.MEDIO_BAJO -> DashNutriaGreen
        NivelIngreso.MEDIO      -> DashSoftTeal
        NivelIngreso.ALTO       -> DashSoftTeal
    }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(400)) + slideInVertically(tween(400)) { -8 }
    ) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Menú adaptado para región ${region.label}." },
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = DashCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .semantics { invisibleToUser() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Región ${region.label}", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = DashNutriaDarkGreen)
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(accentColor.copy(alpha = 0.08f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Menú adaptado", fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(region.estados, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TARJETA DE ETAPA
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EtapaStatusCard(
    child:          ChildProfile,
    etapa:          EtapaInfo,
    etapaColor:     Color,
    edad:           String,
    ultimaMedicion: MedicionCrecimiento? = null
) {
    val pesoDisplay  = ultimaMedicion?.let { "${it.pesoKg} kg" }  ?: "${child.weightKg} kg"
    val tallaDisplay = ultimaMedicion?.let { "${it.tallaCm} cm" } ?: "${child.heightCm} cm"
    val esDatoReal   = ultimaMedicion != null

    // ── Icono de etapa con "latido" suave ─────────────────────────────────────
    val iconInf = rememberInfiniteTransition(label = "etapaIcon")
    val iconScale by iconInf.animateFloat(
        initialValue  = 1f,
        targetValue   = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = EaseInOutSine), RepeatMode.Reverse),
        label         = "etapaScale"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "Tarjeta de etapa. ${etapa.nombre}, ${etapa.rango}. " +
                            "Edad: $edad. Peso: $pesoDisplay. Talla: $tallaDisplay."
            },
        shape     = RoundedCornerShape(32.dp),
        colors    = CardDefaults.cardColors(containerColor = DashCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .scale(iconScale)
                    .clip(CircleShape)
                    .background(etapaColor.copy(alpha = 0.12f))
                    .semantics { invisibleToUser() },
                contentAlignment = Alignment.Center
            ) {
                Icon(etapaIcon(etapa.nombre), contentDescription = null, tint = etapaColor, modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(etapa.nombre, fontSize = 22.sp, fontWeight = FontWeight.Black, color = DashNutriaDarkGreen)
            Text(edad, fontSize = 14.sp, color = Color.Gray)
            if (etapa.rango.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(etapaColor.copy(alpha = 0.10f))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(etapa.rango, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = etapaColor)
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatCol(Icons.Rounded.Scale,      pesoDisplay,  "Peso",  DashSoftPurple)
                Box(modifier = Modifier.width(1.dp).height(40.dp).background(Color(0xFFF0F0F0)).semantics { invisibleToUser() })
                StatCol(Icons.Rounded.Straighten, tallaDisplay, "Talla", DashSoftTeal)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (esDatoReal) Icons.Rounded.CloudDone else Icons.Rounded.PersonOutline,
                    contentDescription = null,
                    tint     = if (esDatoReal) DashNutriaGreen else Color.LightGray,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (esDatoReal) "Última medición: ${ultimaMedicion?.fecha}" else "Datos del perfil",
                    fontSize = 10.sp,
                    color    = if (esDatoReal) DashNutriaGreen else Color.LightGray
                )
            }
        }
    }
}

private fun etapaIcon(nombre: String): ImageVector = when (nombre) {
    "Lactancia Exclusiva" -> Icons.Rounded.ChildCare
    "Iniciando Sólidos"   -> Icons.Rounded.Restaurant
    "Primera Infancia"    -> Icons.Rounded.EscalatorWarning
    "Preescolar"          -> Icons.Rounded.Face
    "Escolar"             -> Icons.Rounded.School
    "Adolescente"         -> Icons.Rounded.Person
    else                  -> Icons.Rounded.ChildCare
}

@Composable
fun StatCol(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp)
        Text(label, fontSize = 11.sp, color = Color.Gray)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// BANNER TIP IA — entra deslizando desde la derecha
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun AiRecommendationBanner(etapa: String) {
    val tip = when (etapa) {
        "Lactancia Exclusiva" ->
            "Durante la lactancia exclusiva, la leche materna cubre el 100% de las necesidades. Mantener tomas frecuentes fortalece el vínculo y la inmunidad."
        "Iniciando Sólidos" ->
            "Al iniciar sólidos, ofrece alimentos ricos en hierro como puré de lentejas o carne. Continúa con la lactancia como base principal."
        "Primera Infancia" ->
            "Entre 1 y 2 años, prioriza alimentos ricos en hierro, calcio y omega-3 para apoyar el desarrollo cerebral y óseo."
        "Preescolar" ->
            "En etapa preescolar, ofrece 5 comidas al día con variedad de colores en el plato para cubrir todos los micronutrientes."
        "Escolar" ->
            "La edad escolar demanda energía constante. Un desayuno completo mejora la concentración y el rendimiento académico."
        else ->
            "Una alimentación balanceada en la adolescencia apoya el crecimiento, el estado de ánimo y la salud ósea a largo plazo."
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(etapa) { visible = false; kotlinx.coroutines.delay(40); visible = true }

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(380)) + slideInHorizontally(tween(380, easing = EaseOutCubic)) { it / 4 }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Consejo NutriIA: $tip" },
            shape  = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = DashNutriaGreen)
        ) {
            Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                // ícono con pulso ligero
                val tipInf = rememberInfiniteTransition(label = "tipBulb")
                val bulbScale by tipInf.animateFloat(
                    initialValue  = 1f,
                    targetValue   = 1.15f,
                    animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
                    label         = "bulb"
                )
                Icon(
                    Icons.Rounded.TipsAndUpdates,
                    contentDescription = null,
                    tint     = Color.White,
                    modifier = Modifier.size(30.dp).scale(bulbScale)
                )
                Spacer(Modifier.width(16.dp))
                Text("Tip NutriIA: $tip", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECCIÓN CRECIMIENTO
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun GrowthSection(
    ultimaMedicion:    MedicionCrecimiento?,
    historial:         List<MedicionCrecimiento>,
    onOpenCrecimiento: () -> Unit
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Progreso OMS", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = DashNutriaDarkGreen)
            Spacer(Modifier.weight(1f))
            if (historial.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DashNutriaGreen.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(DashNutriaGreen).semantics { invisibleToUser() })
                        Spacer(Modifier.width(4.dp))
                        Text("${historial.size} medición(es)", fontSize = 10.sp, color = DashNutriaGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(
            modifier  = Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = DashCardWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (ultimaMedicion != null) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        GrowthStatItem(Icons.Rounded.Scale,      "${ultimaMedicion.pesoKg} kg",  "Peso",  DashSoftPurple)
                        Box(Modifier.width(1.dp).height(50.dp).background(Color(0xFFF0F0F0)).semantics { invisibleToUser() })
                        GrowthStatItem(Icons.Rounded.Straighten, "${ultimaMedicion.tallaCm} cm", "Talla", DashSoftTeal)
                        if (ultimaMedicion.imc > 0) {
                            Box(Modifier.width(1.dp).height(50.dp).background(Color(0xFFF0F0F0)).semantics { invisibleToUser() })
                            GrowthStatItem(Icons.Rounded.Analytics, "${((ultimaMedicion.imc * 10).toInt() / 10.0)}", "IMC", DashNutriaGreen)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    if (historial.size >= 2) {
                        val ultimas = historial.takeLast(6).sortedBy { it.fecha }
                        val maxPeso = (ultimas.maxOfOrNull { it.pesoKg } ?: 1.0).coerceAtLeast(1.0)
                        val minPeso = ultimas.minOfOrNull { it.pesoKg } ?: 0.0
                        val rango   = (maxPeso - minPeso).coerceAtLeast(0.5)

                        // ── Animación de progreso del canvas ──────────────────
                        var chartAnim by remember { mutableStateOf(false) }
                        LaunchedEffect(historial.size) { chartAnim = false; kotlinx.coroutines.delay(80); chartAnim = true }
                        val drawProgress by animateFloatAsState(
                            targetValue   = if (chartAnim) 1f else 0f,
                            animationSpec = tween(900, easing = EaseOutCubic),
                            label         = "chartProg"
                        )

                        Canvas(modifier = Modifier.fillMaxWidth().height(60.dp)) {
                            val w    = size.width
                            val h    = size.height
                            val step = if (ultimas.size > 1) w / (ultimas.size - 1) else w
                            val drawUpTo = ((ultimas.size - 1) * drawProgress).toInt()
                            for (i in 0 until drawUpTo) {
                                val x1 = i * step
                                val y1 = (h - ((ultimas[i].pesoKg     - minPeso) / rango * h).toFloat()).coerceIn(0f, h)
                                val x2 = (i + 1) * step
                                val y2 = (h - ((ultimas[i + 1].pesoKg - minPeso) / rango * h).toFloat()).coerceIn(0f, h)
                                drawLine(
                                    color = Color(0xFF4CAF50), start = Offset(x1, y1), end = Offset(x2, y2),
                                    strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round
                                )
                            }
                            for (i in 0..drawUpTo) {
                                if (i >= ultimas.size) break
                                val x = i * step
                                val y = (h - ((ultimas[i].pesoKg - minPeso) / rango * h).toFloat()).coerceIn(0f, h)
                                drawCircle(Color(0xFF4CAF50), radius = 4.dp.toPx(),   center = Offset(x, y))
                                drawCircle(Color.White,       radius = 2.5.dp.toPx(), center = Offset(x, y))
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Tendencia de peso · últimas ${ultimas.size} mediciones", fontSize = 10.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Última medición: ${ultimaMedicion.fecha}", fontSize = 11.sp, color = Color.Gray)
                } else {
                    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp).padding(8.dp)) {
                        val stroke = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.8f)
                            quadraticTo(size.width * 0.5f, size.height * 0.4f, size.width, size.height * 0.2f)
                        }
                        drawPath(path = path, color = Color.LightGray, style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))))
                        drawPath(path = path, color = DashNutriaGreen, style = stroke)
                        drawCircle(color = DashNutriaGreen, radius = 6.dp.toPx(), center = Offset(size.width, size.height * 0.2f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Sin mediciones aún — entra a Crecimiento para registrar",
                        fontSize = 12.sp, color = Color.Gray,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick  = onOpenCrecimiento,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (historial.isEmpty())
                                "Registrar primera medición de crecimiento."
                            else
                                "Ver gráficas OMS completas de crecimiento."
                        },
                    shape  = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DashNutriaGreen),
                    border = BorderStroke(1.dp, DashNutriaGreen.copy(alpha = 0.5f))
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ShowChart, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (historial.isEmpty()) "Registrar primera medición" else "Ver gráficas OMS completas",
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GrowthStatItem(icon: ImageVector, value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.10f))
                .semantics { invisibleToUser() },
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 15.sp, color = color)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// GRID DE MÓDULOS — entrada escalonada por tarjeta
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ModulesSection(modules: List<DashModule>) {
    Column {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.GridView, contentDescription = null, tint = DashNutriaGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "Módulos de seguimiento",
                fontSize   = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = DashNutriaDarkGreen,
                modifier   = Modifier.semantics { contentDescription = VozDash.MODULOS_SECCION }
            )
        }
        Spacer(Modifier.height(14.dp))

        // Enumeramos globalmente para el stagger por índice
        var globalIndex = 0
        modules.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { module ->
                    ModuleCard(
                        module      = module,
                        staggerIndex = globalIndex,
                        modifier    = Modifier.weight(1f)
                    )
                    globalIndex++
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ModuleCard(
    module:       DashModule,
    staggerIndex: Int = 0,
    modifier:     Modifier = Modifier
) {
    // ── Entrada escalonada ────────────────────────────────────────────────────
    var cardVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(staggerIndex * 55L)
        cardVisible = true
    }

    // ── Feedback táctil de press ──────────────────────────────────────────────
    var pressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue   = if (pressed) 0.93f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "modulePress_${module.title}"
    )
    LaunchedEffect(pressed) {
        if (pressed) { kotlinx.coroutines.delay(130); pressed = false }
    }

    AnimatedVisibility(
        visible  = cardVisible,
        enter    = fadeIn(tween(320, easing = EaseOutCubic)) +
                slideInVertically(tween(320, easing = EaseOutCubic)) { (it * 0.35f).toInt() },
        modifier = modifier
    ) {
        Card(
            modifier  = Modifier
                .scale(pressScale)
                // FIX: heightIn en lugar de height fijo → el texto de 2 líneas ya no se recorta
                .heightIn(min = 108.dp)
                .fillMaxWidth()
                .clickable(enabled = module.isReady) { pressed = true; module.onClick() }
                .semantics {
                    contentDescription = if (module.isReady)
                        "${module.title}. Disponible. Toca para abrir."
                    else
                        "${module.title}. Próximamente disponible."
                },
            shape     = RoundedCornerShape(22.dp),
            colors    = CardDefaults.cardColors(
                containerColor = if (module.isReady) DashCardWhite else Color(0xFFF5F5F5)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = if (module.isReady) 3.dp else 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (module.isReady)
                            Brush.verticalGradient(listOf(module.color.copy(alpha = 0.07f), Color.Transparent))
                        else
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier            = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier              = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(module.color.copy(alpha = 0.12f))
                                .semantics { invisibleToUser() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                module.icon, contentDescription = null,
                                tint     = if (module.isReady) module.color else Color.LightGray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (!module.isReady) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.LightGray.copy(alpha = 0.4f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Pronto", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(
                                Icons.Rounded.ChevronRight, contentDescription = null,
                                tint     = module.color.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Título — maxLines = 2 + softWrap para que "Pediatra /\nNutriólogo" sea visible completo
                    Text(
                        text       = module.title,
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (module.isReady) DashNutriaDarkGreen else Color.LightGray,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis,
                        softWrap   = true
                    )
                }
            }
        }
    }
}
