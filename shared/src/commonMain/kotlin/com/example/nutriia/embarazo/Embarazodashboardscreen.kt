package com.example.nutriia.embarazo

import kotlinx.coroutines.delay
import kotlinx.datetime.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.VoiceInputManager
import com.example.nutriia.accesibilidad.VoiceInputState
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.utils.FechaUtils
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Spa
import androidx.compose.material.icons.outlined.CheckCircle

private val EmbRosa       = Color(0xFFEC9BBF)
private val EmbRosaOscuro = Color(0xFFD4679A)
private val EmbRosaClaro  = Color(0xFFFDE8F2)
private val EmbMorado     = Color(0xFF9C8FE0)
private val EmbFondo      = Color(0xFFFFF5F9)
private val EmbTeal       = Color(0xFF4DB6AC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmbarazoDashboardScreen(
    nombreMama:      String,
    perfil:          PerfilEmbarazo = PerfilEmbarazo(),
    onLogout:        () -> Unit,
    onConfiguracion: () -> Unit,
    onOpenVinculacionGinecologo: () -> Unit = {},
    onOpenChatBot:   () -> Unit = {},
    onOpenNutricion: () -> Unit = {},
    onOpenSuplementos: () -> Unit = {},
    onOpenPeso:      () -> Unit = {},
    onOpenSintomas:  () -> Unit = {},
    onOpenCitas:     () -> Unit = {},
    onOpenRecordatorios: () -> Unit = {},
    onOpenAnalisisIA: () -> Unit = {}
) {
    val a11yVm: AccessibilityViewModel = viewModel()
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esBlind      = a11yMode == AccessibilityMode.BLIND
    val esMute       = a11yMode == AccessibilityMode.MUTE
    val esAccesible  = esBlind || esMute
    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

        var isListening by remember { mutableStateOf(false) }
    val voiceManager = remember { VoiceInputManager() }
    val voiceState by voiceManager.estado
    var showSintomasSheet by remember { mutableStateOf(false) }
    var showPesoSheet   by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.liberar()
        }
    }

    LaunchedEffect(isListening) {
        if (isListening && a11yMode == AccessibilityMode.BLIND) {
            val guia = loc(
                "Te escucho. Puedes decir: Ginecólogo, Nutrición, Peso, Síntomas, Citas, NutriBot, Recordatorios, Ajustes, Ayuda o Salir. ¿Hacia qué módulo se va a dirigir?",
                "I'm listening. You can say: Gynecologist, Nutrition, Weight, Symptoms, Appointments, NutriBot, Reminders, Settings, Help, or Logout. Which module would you like to open?"
            )
            a11yVm.hablar(guia)
            delay(9500)
            voiceManager.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                isListening = false
                val cmd = result.lowercase().trim()
                when {
                    cmd.contains("ginec") || cmd.contains("gineco") || cmd.contains("medico") || cmd.contains("médico") -> {
                        a11yVm.hablar(loc("Abriendo vinculación con ginecólogo.", "Opening gynecologist pairing."))
                        onOpenVinculacionGinecologo()
                    }
                    cmd.contains("nutricion") || cmd.contains("nutrición") || cmd.contains("alimentacion") || cmd.contains("alimentación") || cmd.contains("comida") -> {
                        a11yVm.hablar(loc("Abriendo alimentación y plan de dieta.", "Opening nutrition and meal plan."))
                        onOpenNutricion()
                    }
                    cmd.contains("peso") || cmd.contains("kilos") || cmd.contains("kg") || cmd.contains("ganancia") -> {
                        a11yVm.hablar(loc("Abriendo control de peso.", "Opening weight control."))
                        showPesoSheet = true
                    }
                    cmd.contains("sintoma") || cmd.contains("síntoma") || cmd.contains("reporte") -> {
                        a11yVm.hablar(loc("Abriendo registro de síntomas.", "Opening pregnancy symptoms log."))
                        showSintomasSheet = true
                        onOpenSintomas()
                    }
                    cmd.contains("cita") || cmd.contains("agenda") || cmd.contains("calendario") -> {
                        a11yVm.hablar(loc("Abriendo citas médicas.", "Opening medical appointments."))
                        onOpenCitas()
                    }
                    cmd.contains("nutribot") || cmd.contains("chat") || cmd.contains("pregunta") -> {
                        a11yVm.hablar(loc("Abriendo chat con NutriBot.", "Opening chat with NutriBot."))
                        onOpenChatBot()
                    }
                    cmd.contains("recordatorio") || cmd.contains("alarma") -> {
                        a11yVm.hablar(loc("Abriendo recordatorios.", "Opening pregnancy reminders."))
                        onOpenRecordatorios()
                    }
                    cmd.contains("ajustes") || cmd.contains("configuracion") || cmd.contains("configuración") -> {
                        a11yVm.hablar(loc("Abriendo ajustes.", "Opening settings."))
                        onConfiguracion()
                    }
                    cmd.contains("ayuda") -> {
                        a11yVm.hablar(loc("Abriendo ayuda.", "Opening help."))
                        onConfiguracion()
                    }
                    cmd.contains("salir") || cmd.contains("cerrar") -> {
                        a11yVm.hablar(loc("Cerrando sesión.", "Logging out."))
                        onLogout()
                    }
                    else -> a11yVm.hablar(loc(
                        "No entendí el comando. Intenta decir ginecólogo, nutrición o citas.",
                        "I didn't understand the command. Try saying gynecologist, nutrition, or appointments."
                    ))
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar(
                loc(
                    "Bienvenida al panel de control de tu embarazo. Aquí puedes ver tu progreso de gestación, registrar peso, registrar síntomas y acceder a las recomendaciones.",
                    "Welcome to your pregnancy dashboard. Here you can view your gestational progress, log weight, log symptoms, and access recommendations."
                )
            )
        }
    }

    val embDbVm: EmbarazoDashboardViewModel = viewModel()

    var semanasActuales by rememberSaveable(perfil.semanas) { mutableIntStateOf(perfil.semanas) }
    var showEditSheet   by remember { mutableStateOf(false) }

    val perfilStateRaw by embDbVm.perfilEmbarazo.collectAsState()
    val perfil = perfilStateRaw ?: perfil

    LaunchedEffect(perfil) {
        perfil?.let {
            if (it.semanas != semanasActuales) {
                semanasActuales = it.semanas
            }
        }
    }

    LaunchedEffect(semanasActuales) {
        embDbVm.actualizarSemana(semanasActuales)
    }
    
    val progress by animateFloatAsState(
        targetValue   = semanasActuales / 40f,
        animationSpec = tween(600),
        label         = "progress"
    )

    val trimestre = when {
        semanasActuales <= 13 -> 1
        semanasActuales <= 26 -> 2
        else                  -> 3
    }

    Scaffold(
        containerColor = EmbFondo,
        floatingActionButton = {
            if (a11yMode == AccessibilityMode.BLIND) {
                FloatingActionButton(
                    onClick = { isListening = !isListening },
                    containerColor = if (voiceState == VoiceInputState.LISTENING) Color.Red else EmbRosaOscuro,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 12.dp).size(72.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = CircleShape,
                            ambientColor = EmbRosaOscuro.copy(alpha = 0.5f),
                            spotColor = EmbRosaOscuro.copy(alpha = 0.5f)
                        )
                        .semantics { contentDescription = if (voiceState == VoiceInputState.LISTENING) "Detener comandos de voz" else "Activar comandos de voz para navegación. Al presionar, escucha la lista de comandos disponibles." }
                ) {
                    Icon(if (voiceState == VoiceInputState.LISTENING) Icons.Rounded.Stop else Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(34.dp))
                }
            }
        }
    ) { padding ->
        val pState = perfil
        if (pState.pesoPregestacionalKg == 0.0 || pState.tallaM == 0.0) {
            GatekeeperForm(
                perfil = pState,
                idiomaActual = idiomaActual,
                onSave = { pesoPre, estatura, fum, pesoAct ->
                    val tallaVal = estatura.toDoubleOrNull() ?: 0.0
                    val tallaNorm = if (tallaVal > 3.0) tallaVal / 100.0 else tallaVal
                    val pesoPreVal = pesoPre.toDoubleOrNull() ?: 0.0
                    val pesoActVal = pesoAct.toDoubleOrNull() ?: 0.0

                    val calculatedWeeks = if (fum.isNotBlank()) {
                        calcularSemanasDesdeFUM(fum)
                    } else {
                        pState.semanas
                    }

                    val dateFmt = FechaUtils.fechaActual()
                    val primerRegistro = RegistroPesoEmbarazo(
                        fecha = dateFmt,
                        semanaGestacion = calculatedWeeks,
                        pesoActualKg = pesoActVal,
                        fuente = "Inicial",
                        notas = "Registro inicial de peso al completar perfil."
                    )
                    embDbVm.guardarRegistroPeso(primerRegistro)

                    val perfilActualizado = pState.copy(
                        pesoPregestacionalKg = pesoPreVal,
                        tallaM = tallaNorm,
                        semanas = calculatedWeeks,
                        fechaUltimaMenstruacion = if (fum.isNotBlank()) fum else pState.fechaUltimaMenstruacion
                    )
                    embDbVm.guardarPerfilEmbarazo(perfilActualizado)
                    semanasActuales = calculatedWeeks
                },
                onLogout = onLogout,
                modifier = Modifier.padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
            // Header con gradiente y estilo visual del dashboard infantil
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "NutriIA",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmbRosaOscuro
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(EmbTeal, CircleShape)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = loc("Seguimiento activo", "Active tracking"),
                            fontSize = 12.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    HeaderActionButton(
                        icon = Icons.AutoMirrored.Rounded.HelpOutline,
                        label = loc("Ayuda", "Help"),
                        onClick = {}
                    )
                    HeaderActionButton(
                        icon = Icons.Rounded.Settings,
                        label = loc("Ajustes", "Settings"),
                        onClick = onConfiguracion
                    )
                    HeaderActionButton(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        label = loc("Salir", "Exit"),
                        onClick = onLogout
                    )
                }
            }

            // Fila de Perfil (Madre primeriza)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val primerNombre = nombreMama.split(" ").firstOrNull() ?: nombreMama
                    val inicial = primerNombre.take(1).uppercase()
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(EmbRosa.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = inicial,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmbRosaOscuro
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = primerNombre,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        Text(
                            text = loc("Mi Embarazo", "My Pregnancy"),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = EmbTeal,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Tarjeta de Condiciones Médicas / Menú Adaptado
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(EmbTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MedicalServices,
                            contentDescription = null,
                            tint = EmbTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = loc("Condiciones Médicas", "Medical Conditions"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Spacer(Modifier.width(8.dp))
                            val tieneCondicion = perfil.condiciones.isNotEmpty()
                            Surface(
                                color = (if (tieneCondicion) EmbRosa else EmbTeal).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (tieneCondicion) loc("Menú adaptado", "Adapted diet") else loc("Normal", "Standard"),
                                    color = if (tieneCondicion) EmbRosaOscuro else EmbTeal,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (perfil.condiciones.isNotEmpty()) {
                                perfil.condiciones.joinToString(", ")
                            } else {
                                loc("Menú estándar saludable sin condiciones", "Healthy standard diet with no conditions")
                            },
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Tarjeta de Información Core
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(EmbRosa.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Spa,
                            contentDescription = null,
                            tint = EmbRosaOscuro,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    
                    Text(
                        text = loc("Trimestre $trimestre", "Trimester $trimestre"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmbRosaOscuro
                    )
                    Text(
                        text = loc("$semanasActuales Semanas de Embarazo", "$semanasActuales Weeks Pregnant"),
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    
                    Spacer(Modifier.height(20.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val registrosActuales by embDbVm.registrosPeso.collectAsState()
                        val ultimoReg = registrosActuales.maxByOrNull { it.semanaGestacion }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MonitorWeight,
                                contentDescription = null,
                                tint = EmbRosaOscuro,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            val pesoTexto = if (ultimoReg != null) {
                                "${ultimoReg.pesoActualKg}"
                            } else {
                                "— kg"
                            }
                            Text(
                                text = pesoTexto,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                            Text(
                                text = if (ultimoReg != null) loc("Peso Actual", "Current Weight") else loc("Sin registro", "No log"),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(50.dp)
                                .background(Color.LightGray.copy(alpha = 0.5f))
                        )
                        
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChildCare,
                                contentDescription = null,
                                tint = EmbTeal,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            val tamanoBebe = embDbVm.obtenerTamanoBebe(semanasActuales, idiomaActual == IdiomaVoz.INGLES)
                            Text(
                                text = tamanoBebe,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                            Text(
                                text = loc("Tamaño del bebé", "Baby Size"),
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = EmbRosa,
                        trackColor = EmbRosa.copy(alpha = 0.15f)
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEditSheet = true },
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = null,
                            tint = EmbRosaOscuro,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = loc("Ajustar semana del embarazo", "Adjust pregnancy week"),
                            fontSize = 12.sp,
                            color = EmbRosaOscuro,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            val registrosPesoState by embDbVm.registrosPeso.collectAsState()
            val ultimoReg = registrosPesoState.maxByOrNull { it.semanaGestacion }

            if (ultimoReg != null) {
                Spacer(Modifier.height(16.dp))
                val ganancia = ultimoReg.pesoActualKg - perfil.pesoPregestacionalKg
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = loc("Control de Peso (NOM-007)", "Weight Control (NOM-007)"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.DarkGray
                        )
                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(EmbRosa.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.MonitorWeight, null, tint = EmbRosaOscuro, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = loc("Peso actual hoy", "Current weight today"),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${ultimoReg.pesoActualKg}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        text = "Semana ${ultimoReg.semanaGestacion}",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(EmbTeal.copy(alpha = 0.1f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.TrendingUp, null, tint = EmbTeal, modifier = Modifier.size(18.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Column(horizontalAlignment = Alignment.Start) {
                                    Text(
                                        text = loc("Ganancia", "Gain"),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Text(
                                        text = "${ganancia}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (ganancia > 0) Color(0xFF2E7D32) else Color.DarkGray
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        val imc = perfil.imcPregestacional
                        val rango = GananciaPesoCalculator.rangoAjustado(imc, perfil.edad, perfil.tallaM)
                        val gananciaEsperada = GananciaPesoCalculator.gananciaEsperadaAcumulada(ultimoReg.semanaGestacion, rango)
                        val estado = if (perfil.esGemelar) {
                            GananciaPesoCalculator.EstadoGanancia.EN_RANGO
                        } else {
                            GananciaPesoCalculator.evaluarEstado(ganancia, gananciaEsperada)
                        }

                        val estadoColor = when (estado) {
                            GananciaPesoCalculator.EstadoGanancia.POR_DEBAJO -> Color(0xFF1E88E5)
                            GananciaPesoCalculator.EstadoGanancia.EN_RANGO -> Color(0xFF43A047)
                            GananciaPesoCalculator.EstadoGanancia.POR_ARRIBA -> Color(0xFFE53935)
                            else -> Color.Gray
                        }
                        val estadoText = when (estado) {
                            GananciaPesoCalculator.EstadoGanancia.POR_DEBAJO -> loc("Ganancia baja", "Low gain")
                            GananciaPesoCalculator.EstadoGanancia.EN_RANGO -> loc("En rango saludable", "Healthy range")
                            GananciaPesoCalculator.EstadoGanancia.POR_ARRIBA -> loc("Ganancia alta", "High gain")
                            else -> loc("Sin registros", "No logs")
                        }

                        Surface(
                            color = estadoColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, estadoColor.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = loc("Estado vs. Rango NOM-007:", "NOM-007 Status:"),
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = estadoText.uppercase(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = estadoColor
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = { showPesoSheet = true },
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmbRosa)
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = loc("Registrar Peso actual", "Log Current Weight"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }



            // Tarjeta de Sugerencia / Banner con NutriBot
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EmbTeal)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lightbulb,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = loc("Tip de Embarazo Inteligente", "Smart Pregnancy Tip"),
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = loc(
                                "Monitorea tu hidratación y consume suficiente ácido fólico y hierro diariamente.",
                                "Track your hydration and ensure daily intake of folic acid and iron."
                            ),
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = onOpenChatBot,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = EmbTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = loc("Consultar NutriBot", "Consult NutriBot"),
                                color = EmbTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                // Alerta de salud
                if (perfil.condiciones.any { it.contains("Diabetes", true) || it.contains("Hipertensión", true) || it.contains("Hypertension", true) }) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        color    = Color(0xFFFF8F00).copy(alpha = 0.08f),
                        shape    = RoundedCornerShape(16.dp),
                        border   = BorderStroke(1.dp, Color(0xFFFF8F00).copy(alpha = 0.2f))
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFF8F00))
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text       = loc(
                                    "Recuerda consultar tus restricciones alimenticias con tu nutriólogo.",
                                    "Remember to consult your dietary restrictions with your nutritionist."
                                ),
                                fontSize   = 13.sp,
                                color      = Color.DarkGray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Sección "Hoy para ti"
                val accionesHoy by embDbVm.accionesHoy.collectAsState()
                if (accionesHoy.isNotEmpty()) {
                    Text(
                        text = loc("Hoy para ti", "Today for you"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        accionesHoy.forEach { accion ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(90.dp)
                                    .clickable {
                                        when (accion.modulo) {
                                            "NUTRICION" -> onOpenNutricion()
                                            "SUPLEMENTOS" -> onOpenSuplementos()
                                            "CHAT" -> onOpenChatBot()
                                            else -> {}
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = EmbRosaClaro.copy(alpha = 0.5f)),
                                border = BorderStroke(1.dp, EmbRosa.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color.White),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val icon = when (accion.modulo) {
                                            "NUTRICION" -> Icons.Outlined.Restaurant
                                            "SUPLEMENTOS" -> Icons.Outlined.Spa
                                            else -> Icons.Outlined.CheckCircle
                                        }
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = EmbRosaOscuro,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (idiomaActual == IdiomaVoz.INGLES) accion.tituloEn else accion.tituloEs,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray,
                                            maxLines = 2
                                        )
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = if (idiomaActual == IdiomaVoz.INGLES) accion.subtituloEn else accion.subtituloEs,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Módulos en grid
                val estados by embDbVm.estadoModulos.collectAsState()
                val gineEstado = estados["ginecologo"] ?: EstadoModulo("", "")
                val nutriEstado = estados["nutricion"] ?: EstadoModulo("", "")
                val pesoEstado = estados["peso"] ?: EstadoModulo("", "")
                val sintEstado = estados["sintomas"] ?: EstadoModulo("", "")
                val citasEstado = estados["citas"] ?: EstadoModulo("", "")
                val pregEstado = estados["preguntame"] ?: EstadoModulo("", "")

                Row(modifier = Modifier.fillMaxWidth()) {
                    ModuloCard(
                        modifier = Modifier.weight(1f),
                        title    = loc("Ginecólogo", "Gynecologist"),
                        icon     = Icons.Rounded.Female,
                        color    = EmbMorado,
                        badge    = gineEstado.badge,
                        subtitle = if (idiomaActual == IdiomaVoz.INGLES) gineEstado.subtituloEn else gineEstado.subtituloEs,
                        onClick  = {
                            if (esBlind) a11yVm.hablar(loc("Abriendo vinculación y expediente con ginecólogo.", "Opening gynecologist pairing and clinical records."))
                            onOpenVinculacionGinecologo()
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    ModuloCard(
                        modifier = Modifier.weight(1f),
                        title    = loc("Nutrición", "Nutrition"),
                        icon     = Icons.Rounded.Restaurant,
                        color    = EmbRosa,
                        badge    = nutriEstado.badge,
                        subtitle = if (idiomaActual == IdiomaVoz.INGLES) nutriEstado.subtituloEn else nutriEstado.subtituloEs,
                        onClick  = {
                            if (esBlind) a11yVm.hablar(loc("Abriendo alimentación y plan de dieta de embarazo.", "Opening pregnancy nutrition and meal plan."))
                            onOpenNutricion()
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ModuloCard(
                        modifier = Modifier.weight(1f),
                        title    = loc("Control de peso", "Weight control"),
                        icon     = Icons.Rounded.MonitorWeight,
                        color    = EmbMorado,
                        badge    = pesoEstado.badge,
                        subtitle = if (idiomaActual == IdiomaVoz.INGLES) pesoEstado.subtituloEn else pesoEstado.subtituloEs,
                        onClick  = {
                            if (esBlind) a11yVm.hablar(loc("Abriendo bitácora y registro de ganancia de peso.", "Opening weight gain log and record."))
                            showPesoSheet = true
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    ModuloCard(
                        modifier = Modifier.weight(1f),
                        title    = loc("Síntomas", "Symptoms"),
                        icon     = Icons.Rounded.MoodBad,
                        color    = EmbTeal,
                        badge    = sintEstado.badge,
                        subtitle = if (idiomaActual == IdiomaVoz.INGLES) sintEstado.subtituloEn else sintEstado.subtituloEs,
                        onClick  = {
                            if (esBlind) a11yVm.hablar(loc("Abriendo registro de síntomas de embarazo.", "Opening pregnancy symptoms log."))
                            showSintomasSheet = true
                            onOpenSintomas()
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ModuloCard(
                        modifier = Modifier.weight(1f),
                        title    = loc("Citas", "Appts"),
                        icon     = Icons.Rounded.Event,
                        color    = Color(0xFFFFB74D),
                        badge    = citasEstado.badge,
                        subtitle = if (idiomaActual == IdiomaVoz.INGLES) citasEstado.subtituloEn else citasEstado.subtituloEs,
                        onClick  = {
                            if (esBlind) a11yVm.hablar(loc("Abriendo agenda y citas médicas.", "Opening calendar and medical appointments."))
                            onOpenCitas()
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    ModuloCard(
                        modifier = Modifier.weight(1f),
                        title    = loc("Pregúntame", "Ask Me"),
                        icon     = Icons.Rounded.QuestionAnswer,
                        color    = EmbTeal,
                        badge    = pregEstado.badge,
                        subtitle = if (idiomaActual == IdiomaVoz.INGLES) pregEstado.subtituloEn else pregEstado.subtituloEs,
                        onClick  = {
                            if (esBlind) a11yVm.hablar(loc("Abriendo chat con NutriBot inteligencia artificial.", "Opening chat with NutriBot AI."))
                            onOpenChatBot()
                        }
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    ModuloCard(
                        modifier = Modifier.weight(1f),
                        title    = loc("Recordatorios", "Reminders"),
                        icon     = Icons.Rounded.NotificationsActive,
                        color    = EmbRosa,
                        badge    = null,
                        subtitle = loc("Alarmas de comidas y citas", "Meal and appointment alarms"),
                        onClick  = {
                            if (esBlind) a11yVm.hablar(loc("Abriendo recordatorios y alarmas de embarazo.", "Opening pregnancy reminders and alarms."))
                            onOpenRecordatorios()
                        }
                    )
                    Spacer(Modifier.width(16.dp))
                    Box(modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(24.dp))

                // Preferencias informativas
                val sinRestricciones = loc("Sin restricciones", "No restrictions")
                val prefsFiltradas   = perfil.preferencias.filter { it != sinRestricciones }
                if (prefsFiltradas.isNotEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        color    = EmbMorado.copy(alpha = 0.07f),
                        shape    = RoundedCornerShape(16.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(loc("Tus preferencias activas", "Your active preferences"), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmbMorado)
                            Spacer(Modifier.height(4.dp))
                            Text(prefsFiltradas.joinToString(", "), fontSize = 14.sp, color = Color.DarkGray)
                        }
                    }
                }

                // Checklist Trimestral
                Text(
                    text       = loc("Tareas del Trimestre $trimestre", "Trimester $trimestre Checklist"),
                    fontWeight = FontWeight.Bold,
                    fontSize   = 18.sp,
                    color      = Color.DarkGray
                )
                Spacer(Modifier.height(12.dp))
                
                val checklist = getTrimestreChecklist(trimestre, idiomaActual == IdiomaVoz.INGLES)
                checklist.forEach { item ->
                    ChecklistItem(item)
                }


                // El tip inteligente superior ya cubre el consejo de la semana, eliminando duplicidad.
                
                Spacer(Modifier.height(40.dp))
            }
        }
    }

        if (showEditSheet) {
            ModalBottomSheet(
                onDismissRequest = { showEditSheet = false },
                containerColor   = Color.White
            ) {
                Column(
                    modifier            = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(loc("Ajustar semana actual", "Adjust current week"), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(Modifier.height(32.dp))
                    Text(text = "Semana $semanasActuales", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmbRosa)
                    Slider(
                        value         = semanasActuales.toFloat(),
                        onValueChange = { semanasActuales = it.toInt() },
                        valueRange    = 1f..40f,
                        steps         = 38,
                        colors        = SliderDefaults.colors(thumbColor = EmbRosa, activeTrackColor = EmbRosa)
                    )
                    Spacer(Modifier.height(32.dp))
                    Button(
                        onClick  = {
                            val updatedPerfil = perfil.copy(semanas = semanasActuales)
                            embDbVm.guardarPerfilEmbarazo(updatedPerfil)
                            showEditSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors   = ButtonDefaults.buttonColors(containerColor = EmbRosa)
                    ) {
                        Text(loc("Guardar", "Save"), fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (showPesoSheet) {
            val registros by embDbVm.registrosPeso.collectAsState()
            val perfilState by embDbVm.perfilEmbarazo.collectAsState()

            ModalBottomSheet(
                onDismissRequest = { showPesoSheet = false },
                containerColor = Color.White
            ) {
                var pesoInput by remember { mutableStateOf("") }
                var alturaInput by remember { mutableStateOf("") }
                var errorMsg by remember { mutableStateOf("") }

                var campoActivo by remember { mutableIntStateOf(0) }
                var valorInicial by remember { mutableStateOf("") }

                LaunchedEffect(campoActivo) {
                    valorInicial = when (campoActivo) {
                        0 -> pesoInput
                        1 -> alturaInput
                        else -> ""
                    }
                }

                LaunchedEffect(pesoInput) {
                    if (!esBlind || pesoInput.isBlank() || campoActivo != 0) return@LaunchedEffect
                    if (pesoInput == valorInicial) return@LaunchedEffect
                    delay(2000L)
                    if (pesoInput.isNotBlank() && campoActivo == 0 && pesoInput != valorInicial) {
                        if (semanasActuales >= 20) {
                            campoActivo = 1
                        } else {
                            campoActivo = 2
                        }
                    }
                }

                LaunchedEffect(alturaInput) {
                    if (!esBlind || alturaInput.isBlank() || campoActivo != 1) return@LaunchedEffect
                    if (alturaInput == valorInicial) return@LaunchedEffect
                    delay(2000L)
                    if (alturaInput.isNotBlank() && campoActivo == 1 && alturaInput != valorInicial) campoActivo = 2
                }

                val ejecutarGuardarPeso: () -> Unit = {
                    errorMsg = ""
                    val p = pesoInput.toDoubleOrNull()
                    if (p == null || p <= 0) {
                        errorMsg = loc("Por favor ingresa un peso válido", "Please enter a valid weight")
                    } else {
                        val a = if (alturaInput.isNotBlank()) alturaInput.toDoubleOrNull() else null
                        if (alturaInput.isNotBlank() && (a == null || a < 15.0 || a > 40.0)) {
                            errorMsg = loc("La altura uterina debe estar entre 15 y 40 cm", "Fundal height must be between 15 and 40 cm")
                        } else {
                            val fechaStr = FechaUtils.fechaActual()
                            val reg = RegistroPesoEmbarazo(
                                fecha = fechaStr,
                                semanaGestacion = semanasActuales,
                                pesoActualKg = p,
                                alturaUterinaCm = a,
                                fuente = "auto_registro"
                            )
                            embDbVm.guardarRegistroPeso(reg)
                            if (esBlind) {
                                a11yVm.hablar(loc("Peso registrado con éxito.", "Weight logged successfully."))
                            }
                            pesoInput = ""
                            alturaInput = ""
                            showPesoSheet = false
                        }
                    }
                }

                LaunchedEffect(campoActivo) {
                    if (esBlind && (campoActivo == 2 || (campoActivo == 1 && semanasActuales < 20))) {
                        ttsManager?.hablarYEsperar(
                            loc(
                                "Campos completados. Di guardar peso o registrar ahora para guardar.",
                                "Fields completed. Say save weight or log now to save."
                            ),
                            margenMs = 800L
                        )
                        voiceManager.escuchar(idiomaActual, true) { result, isFinal ->
                            if (!isFinal) return@escuchar
                            val cmd = result.lowercase().trim()
                            if (cmd.contains("guardar") || cmd.contains("registrar") || cmd.contains("save") || cmd.contains("log")) {
                                ejecutarGuardarPeso()
                            } else {
                                a11yVm.hablar(loc("No entendí. Di guardar peso para registrar.", "I didn't understand. Say save weight to log."))
                            }
                        }
                    }
                }

                LaunchedEffect(Unit) {
                    if (esBlind && perfilState != null) {
                        val imc = perfilState!!.imcPregestacional
                        val rango = GananciaPesoCalculator.rangoAjustado(imc, perfilState!!.edad, perfilState!!.tallaM)
                        val anuncio = loc(
                            "Abriendo control de peso prenatal. Tu rango recomendado total de ganancia de peso es de ${"${rango.minKg}"} a ${"${rango.maxKg}"} kilogramos. Por favor, ingresa tu peso actual. Puedes tocar cualquier registro en el historial para escucharlo.",
                            "Opening prenatal weight control. Your total recommended weight gain range is ${"${rango.minKg}"} to ${"${rango.maxKg}"} kilograms. Please enter your current weight. You can tap any log in the history to hear it."
                        )
                        a11yVm.hablar(anuncio)
                    }
                }

                LaunchedEffect(errorMsg) {
                    if (esBlind && errorMsg.isNotEmpty()) {
                        a11yVm.hablar(errorMsg)
                    }
                }

                val scrollState = rememberScrollState()

                Column(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp)
                        .fillMaxWidth()
                        .verticalScroll(scrollState),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = loc("Control de peso prenatal", "Prenatal Weight Control"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(16.dp))

                    if (perfilState != null) {
                        val isGemelar = perfilState!!.esGemelar
                        if (isGemelar) {
                            Surface(
                                color = Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = loc(
                                        "Este cálculo no aplica en embarazo múltiple; tu ginecólogo/a te dará un rango individualizado.",
                                        "This calculation does not apply to multiple pregnancy; your gynecologist will provide an individualized range."
                                    ),
                                    fontSize = 12.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            val imc = perfilState!!.imcPregestacional
                            val rango = GananciaPesoCalculator.rangoAjustado(imc, perfilState!!.edad, perfilState!!.tallaM)
                            val ultimo = registros.maxByOrNull { it.semanaGestacion }
                            val ganancia = if (ultimo != null) ultimo.pesoActualKg - perfilState!!.pesoPregestacionalKg else 0.0
                            val gananciaEsperada = if (ultimo != null) GananciaPesoCalculator.gananciaEsperadaAcumulada(ultimo.semanaGestacion, rango) else 0.0
                            val estado = if (ultimo != null) GananciaPesoCalculator.evaluarEstado(ganancia, gananciaEsperada) else GananciaPesoCalculator.EstadoGanancia.SIN_DATOS

                            val estadoColor = when (estado) {
                                GananciaPesoCalculator.EstadoGanancia.POR_DEBAJO -> Color(0xFF1E88E5)
                                GananciaPesoCalculator.EstadoGanancia.EN_RANGO -> Color(0xFF43A047)
                                GananciaPesoCalculator.EstadoGanancia.POR_ARRIBA -> Color(0xFFE53935)
                                else -> Color.Gray
                            }
                            val estadoText = when (estado) {
                                GananciaPesoCalculator.EstadoGanancia.POR_DEBAJO -> loc("Ganancia baja", "Low gain")
                                GananciaPesoCalculator.EstadoGanancia.EN_RANGO -> loc("En rango saludable", "Healthy range")
                                GananciaPesoCalculator.EstadoGanancia.POR_ARRIBA -> loc("Ganancia alta", "High gain")
                                else -> loc("Sin registros", "No logs")
                            }

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = loc("Estado de ganancia de peso:", "Weight gain status:"),
                                        fontSize = 11.sp,
                                        color = Color.Gray
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = loc("IMC pregestacional: ${"${imc}"}", "Pre-pregnancy BMI: ${"${imc}"}"),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                        Surface(
                                            color = estadoColor.copy(alpha = 0.15f),
                                            contentColor = estadoColor,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = estadoText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = loc(
                                            "Rango recomendado total: ${"${rango.minKg}"} a ${"${rango.maxKg}"} kg",
                                            "Total recommended range: ${"${rango.minKg}"} to ${"${rango.maxKg}"} kg"
                                        ),
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }
                            }
                        }
                    }

                    if (esAccesible) {
                        CampoTextoAccesible(
                            valor = pesoInput,
                            onValorChange = { pesoInput = it },
                            etiqueta = loc("Peso actual (kg)", "Current weight (kg)"),
                            descripcionVoz = loc("Di tu peso actual hoy en kilogramos", "Speak your current weight today in kilograms"),
                            placeholder = "Ej. 67.5",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            ttsManager = ttsManager,
                            colorPrimario = EmbRosaOscuro,
                            activo = campoActivo == 0,
                            onFocus = { campoActivo = 0 },
                            onNext = {
                                if (semanasActuales >= 20) {
                                    campoActivo = 1
                                } else {
                                    campoActivo = 2
                                }
                            }
                        )

                        if (semanasActuales >= 20) {
                            androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 1) {
                                Column {
                                    Spacer(Modifier.height(12.dp))
                                    CampoTextoAccesible(
                                        valor = alturaInput,
                                        onValorChange = { alturaInput = it },
                                        etiqueta = loc("Altura uterina (cm, opcional)", "Fundal height (cm, optional)"),
                                        descripcionVoz = loc("Di la altura uterina medida por tu ginecólogo en centímetros", "Speak the fundal height measured by your gynecologist in centimeters"),
                                        placeholder = "Ej. 24",
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        ttsManager = ttsManager,
                                        colorPrimario = EmbRosaOscuro,
                                        activo = campoActivo == 1,
                                        onFocus = { campoActivo = 1 },
                                        onNext = { campoActivo = 2 }
                                    )
                                }
                            }
                        }
                    } else {
                        OutlinedTextField(
                            value = pesoInput,
                            onValueChange = { pesoInput = it },
                            label = { Text(loc("Peso actual (kg)", "Current weight (kg)")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                            ),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmbRosa, unfocusedBorderColor = Color.LightGray)
                        )

                        if (semanasActuales >= 20) {
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = alturaInput,
                                onValueChange = { alturaInput = it },
                                label = { Text(loc("Altura uterina (cm, opcional)", "Fundal height (cm, optional)")) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmbRosa, unfocusedBorderColor = Color.LightGray)
                            )
                        }
                    }

                    if (errorMsg.isNotEmpty()) {
                        Text(
                            text = errorMsg,
                            color = Color(0xFFD32F2F),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = { ejecutarGuardarPeso() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = EmbRosa),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(loc("Registrar ahora", "Log now"), fontWeight = FontWeight.Bold)
                    }

                    val registrosConAltura = registros.filter { it.alturaUterinaCm != null }
                    if (registrosConAltura.isNotEmpty() || semanasActuales >= 20) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = loc("Gráfica de Altura Uterina", "Fundal Height Chart"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(8.dp))

                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFFECECEC)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            val paddingLeft = 32.dp.toPx()
                            val paddingBottom = 24.dp.toPx()

                            val graphW = w - paddingLeft
                            val graphH = h - paddingBottom

                            val minX = 20f
                            val maxX = 40f
                            val minY = 15f
                            val maxY = 40f

                            fun getX(xVal: Float): Float {
                                val pct = (xVal - minX) / (maxX - minX)
                                return paddingLeft + pct * graphW
                            }

                            fun getY(yVal: Float): Float {
                                val pct = (yVal - minY) / (maxY - minY)
                                return graphH - pct * graphH
                            }

                            val yLines = listOf(15f, 20f, 25f, 30f, 35f, 40f)
                            yLines.forEach { yVal ->
                                val y = getY(yVal)
                                drawLine(
                                    color = Color(0xFFE0E0E0),
                                    start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                                    end = androidx.compose.ui.geometry.Offset(w, y),
                                    strokeWidth = 1f
                                )
                            }

                            val xLines = listOf(20f, 25f, 30f, 35f, 40f)
                            xLines.forEach { xVal ->
                                val x = getX(xVal)
                                drawLine(
                                    color = Color(0xFFE0E0E0),
                                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                                    end = androidx.compose.ui.geometry.Offset(x, graphH),
                                    strokeWidth = 1f
                                )
                            }

                            val bandPath = androidx.compose.ui.graphics.Path()
                            bandPath.moveTo(getX(20f), getY(20f + 2.5f))
                            bandPath.lineTo(getX(40f), getY(40f + 2.5f))
                            bandPath.lineTo(getX(40f), getY(40f - 2.5f))
                            bandPath.lineTo(getX(20f), getY(20f - 2.5f))
                            bandPath.close()

                            drawPath(bandPath, color = Color(0xFF81C784).copy(alpha = 0.2f))

                            registrosConAltura.forEach { rec ->
                                val rx = rec.semanaGestacion.toFloat()
                                val ry = rec.alturaUterinaCm?.toFloat() ?: 0f
                                if (rx in minX..maxX && ry in minY..maxY) {
                                    drawCircle(
                                        color = Color(0xFFC62828),
                                        radius = 4.dp.toPx(),
                                        center = androidx.compose.ui.geometry.Offset(getX(rx), getY(ry))
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sem. 20", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 25", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 30", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 35", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 40", fontSize = 9.sp, color = Color.Gray)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = loc(
                                "Este dato lo registra tu médico o partera en cada consulta. Aquí solo lo guardamos para tu historial — cualquier duda coméntala con tu profesional de salud.",
                                "This data is recorded by your doctor or midwife at each visit. We only save it here for your history — discuss any questions with your healthcare provider."
                            ),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }

                    if (registros.isNotEmpty() && perfilState != null) {
                        val imc = perfilState!!.imcPregestacional
                        val rango = GananciaPesoCalculator.rangoAjustado(imc, perfilState!!.edad, perfilState!!.tallaM)

                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = loc("Gráfica de Ganancia de Peso (NOM-007)", "Weight Gain Chart (NOM-007)"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(8.dp))

                        androidx.compose.foundation.Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                                .border(BorderStroke(1.dp, Color(0xFFECECEC)), RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            val w = size.width
                            val h = size.height

                            val paddingLeft = 32.dp.toPx()
                            val paddingBottom = 24.dp.toPx()

                            val graphW = w - paddingLeft
                            val graphH = h - paddingBottom

                            val minX = 1f
                            val maxX = 40f
                            val minY = -2f
                            val maxY = 20f

                            fun getX(xVal: Float): Float {
                                val pct = (xVal - minX) / (maxX - minX)
                                return paddingLeft + pct * graphW
                            }

                            fun getY(yVal: Float): Float {
                                val pct = (yVal - minY) / (maxY - minY)
                                return graphH - pct * graphH
                            }

                            val yLines = listOf(-2f, 0f, 5f, 10f, 15f, 20f)
                            yLines.forEach { yVal ->
                                val y = getY(yVal)
                                drawLine(
                                    color = Color(0xFFE0E0E0),
                                    start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                                    end = androidx.compose.ui.geometry.Offset(w, y),
                                    strokeWidth = 1f
                                )
                            }

                            val xLines = listOf(1f, 10f, 20f, 30f, 40f)
                            xLines.forEach { xVal ->
                                val x = getX(xVal)
                                drawLine(
                                    color = Color(0xFFE0E0E0),
                                    start = androidx.compose.ui.geometry.Offset(x, 0f),
                                    end = androidx.compose.ui.geometry.Offset(x, graphH),
                                    strokeWidth = 1f
                                )
                            }

                            clipRect(
                                left = paddingLeft,
                                top = 0f,
                                right = w,
                                bottom = graphH
                            ) {
                                if (!perfilState!!.esGemelar) {
                                    val bandPath = androidx.compose.ui.graphics.Path()
                                    bandPath.moveTo(getX(13f), getY(1.5f))
                                    bandPath.lineTo(getX(40f), getY(rango.maxKg.toFloat()))
                                    bandPath.lineTo(getX(40f), getY(rango.minKg.toFloat()))
                                    bandPath.lineTo(getX(13f), getY(0.5f))
                                    bandPath.close()

                                    drawPath(bandPath, color = Color(0xFFE8F5E9).copy(alpha = 0.7f))
                                }

                                val sortedRegs = registros.sortedBy { it.semanaGestacion }
                                val points = sortedRegs.map { reg ->
                                    val gain = reg.pesoActualKg - perfilState!!.pesoPregestacionalKg
                                    val clampedGain = gain.toFloat().coerceIn(minY, maxY)
                                    val clampedWeek = reg.semanaGestacion.toFloat().coerceIn(minX, maxX)
                                    androidx.compose.ui.geometry.Offset(getX(clampedWeek), getY(clampedGain))
                                }

                                for (i in 0 until points.size - 1) {
                                    drawLine(
                                        color = EmbRosaOscuro,
                                        start = points[i],
                                        end = points[i + 1],
                                        strokeWidth = 3.dp.toPx()
                                    )
                                }

                                points.forEach { pt ->
                                    drawCircle(
                                        color = EmbRosaOscuro,
                                        radius = 5.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.5.dp.toPx(),
                                        center = pt
                                    )
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sem. 1", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 10", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 20", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 30", fontSize = 9.sp, color = Color.Gray)
                            Text("Sem. 40", fontSize = 9.sp, color = Color.Gray)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = loc(
                                "El área verde representa el rango de ganancia de peso saludable recomendado por la NOM-007 según tu IMC pregestacional.",
                                "The green area represents the healthy weight gain range recommended by NOM-007 based on your pre-pregnancy BMI."
                            ),
                            fontSize = 11.sp,
                            color = Color.Gray,
                            lineHeight = 15.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )
                    }

                    if (registros.isNotEmpty()) {
                        Spacer(Modifier.height(24.dp))
                        Text(
                            text = loc("Historial de registros", "Log History"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.DarkGray,
                            modifier = Modifier.align(Alignment.Start)
                        )
                        Spacer(Modifier.height(8.dp))

                        registros.sortedByDescending { it.semanaGestacion }.forEach { rec ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(0.5.dp, Color(0xFFEEEEEE)), RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (esBlind) {
                                            val detail = if (rec.alturaUterinaCm != null) {
                                                loc(
                                                    "Semana ${rec.semanaGestacion}, peso ${rec.pesoActualKg} kilos, altura uterina ${rec.alturaUterinaCm} centímetros, fecha ${rec.fecha}",
                                                    "Week ${rec.semanaGestacion}, weight ${rec.pesoActualKg} kilos, fundal height ${rec.alturaUterinaCm} centimeters, date ${rec.fecha}"
                                                )
                                            } else {
                                                loc(
                                                    "Semana ${rec.semanaGestacion}, peso ${rec.pesoActualKg} kilos, fecha ${rec.fecha}",
                                                    "Week ${rec.semanaGestacion}, weight ${rec.pesoActualKg} kilos, date ${rec.fecha}"
                                                )
                                            }
                                            a11yVm.hablar(detail)
                                        }
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = loc(
                                            "Semana ${rec.semanaGestacion} — ${rec.pesoActualKg} kg",
                                            "Week ${rec.semanaGestacion} — ${rec.pesoActualKg} kg"
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                    if (rec.alturaUterinaCm != null) {
                                        Text(
                                            text = loc(
                                                "Altura uterina: ${rec.alturaUterinaCm} cm",
                                                "Fundal height: ${rec.alturaUterinaCm} cm"
                                            ),
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Text(
                                        text = rec.fecha,
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                                IconButton(
                                    onClick = { embDbVm.eliminarRegistroPeso(rec.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Rounded.Delete, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSintomasSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSintomasSheet = false },
                containerColor = Color.White,
                dragHandle = { BottomSheetDefaults.DragHandle() }
            ) {
                SintomasBottomSheetContent(
                    semanaActualMama = semanasActuales,
                    idiomaActual = idiomaActual,
                    onDismiss = { showSintomasSheet = false },
                    onSave = { sem, list, texto, severidad ->
                        val dateFmt = FechaUtils.fechaActual()
                        val reg = RegistroSintomasEmbarazo(
                            fecha = dateFmt,
                            semanaGestacion = sem,
                            sintomas = list,
                            otrosSintomasTexto = texto,
                            nivelSeveridadMaximo = severidad
                        )
                        embDbVm.guardarRegistroSintomas(reg)
                        showSintomasSheet = false
                    },
                    a11yMode = a11yMode,
                    ttsManager = ttsManager
                )
            }
        }
    }
}

@Composable
fun ModuloCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    color: Color,
    badge: String? = null,
    subtitle: String? = null,
    onClick: () -> Unit = {}
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "scale"
    )
    
    Card(
        modifier = modifier.aspectRatio(1f).scale(scale).clickable { pressed = true; onClick() },
        shape    = RoundedCornerShape(20.dp),
        colors   = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!badge.isNullOrBlank()) {
                Surface(
                    color = EmbRosa,
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 0.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = badge,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
            
            Column(
                modifier            = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier         = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
    LaunchedEffect(pressed) { if (pressed) { kotlinx.coroutines.delay(100); pressed = false } }
}

@Composable
private fun ChecklistItem(text: String) {
    var checked by remember { mutableStateOf(false) }
    val color by animateColorAsState(if (checked) EmbTeal else EmbRosa, label = "color")
    
    val icon = remember(text) {
        val lower = text.lowercase()
        when {
            lower.contains("visita") || lower.contains("consulta") || lower.contains("médica") -> Icons.Rounded.MedicalServices
            lower.contains("sangre") || lower.contains("análisis") || lower.contains("glucosa") || lower.contains("prueba") -> Icons.Rounded.Healing
            lower.contains("crudo") || lower.contains("alimento") || lower.contains("comida") -> Icons.Rounded.Restaurant
            lower.contains("diario") || lower.contains("plan") || lower.contains("escribir") -> Icons.Rounded.Edit
            lower.contains("ecografía") || lower.contains("imagen") || lower.contains("ultrasonido") || lower.contains("morfológica") -> Icons.Rounded.Image
            lower.contains("yoga") || lower.contains("ejercicio") || lower.contains("descansar") || lower.contains("rest") -> Icons.Rounded.Spa
            lower.contains("ropa") || lower.contains("maternity") -> Icons.Rounded.ShoppingCart
            lower.contains("dental") || lower.contains("dientes") -> Icons.Rounded.MedicalServices
            lower.contains("maleta") || lower.contains("hospital") || lower.contains("bag") -> Icons.Rounded.LocalHospital
            lower.contains("lavar") || lower.contains("ropa del bebé") -> Icons.Rounded.ChildCare
            lower.contains("pediatra") || lower.contains("buscar") -> Icons.Rounded.Search
            lower.contains("fecha") || lower.contains("parto") || lower.contains("calcular") -> Icons.Rounded.Event
            else -> Icons.Rounded.CheckCircle
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { checked = !checked },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                color = if (checked) Color.Gray else Color.DarkGray,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable


private fun getTrimestreChecklist(trimestre: Int, isEnglish: Boolean): List<String> {
    return when (trimestre) {
        1 -> if (isEnglish) listOf("First prenatal visit", "Calculate due date", "Blood tests", "Avoid raw foods", "Start a diary")
             else listOf("Primera visita prenatal", "Calcular fecha de parto", "Análisis de sangre", "Evitar alimentos crudos", "Iniciar un diario")
        2 -> if (isEnglish) listOf("Anomaly scan", "Glucose test", "Prenatal yoga", "Maternity clothes", "Dental check-up")
             else listOf("Ecografía morfológica", "Prueba de glucosa", "Yoga prenatal", "Ropa de maternidad", "Revisión dental")
        else -> if (isEnglish) listOf("Hospital bag", "Birth plan", "Wash baby clothes", "Pediatrician search", "Rest as much as possible")
             else listOf("Maleta del hospital", "Plan de parto", "Lavar ropa del bebé", "Buscar pediatra", "Descansar lo máximo posible")
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// MÓDULO DE SÍNTOMAS DEL EMBARAZO (IMSS & VIGILANCIA PRENATAL)
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SintomasBottomSheetContent(
    semanaActualMama: Int,
    idiomaActual: IdiomaVoz,
    onDismiss: () -> Unit,
    onSave: (semana: Int, sintomas: List<String>, textoLibre: String, maxSeveridad: String) -> Unit,
    a11yMode: AccessibilityMode = AccessibilityMode.NORMAL,
    ttsManager: NutriTTS? = null
) {
    val a11yVm: AccessibilityViewModel = viewModel()
    val esBlind = a11yMode == AccessibilityMode.BLIND

    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    var semanaSeleccionada by remember { mutableIntStateOf(semanaActualMama) }
    val info = remember(semanaSeleccionada) { obtenerInfoSintomas(semanaSeleccionada) }
    var sintomasSeleccionados by remember(semanaSeleccionada) { mutableStateOf(setOf<String>()) }
    var otrosSintomasTexto by remember { mutableStateOf("") }
    var expandirOtros by remember { mutableStateOf(false) }

    var tabIndex by remember { mutableIntStateOf(0) }

        val voiceManager = remember { if (esBlind) VoiceInputManager() else null }

    val analizados = remember(sintomasSeleccionados, semanaSeleccionada) {
        sintomasSeleccionados.map { SintomasAnalyzer.analizarSintoma(it, info.trimestre) }
    }

    val ejecutarGuardarSintomas: () -> Unit = {
        val maxSeveridad = when {
            analizados.any { it.nivel == NivelSintoma.URGENCIA } -> NivelSintoma.URGENCIA.name
            analizados.any { it.nivel == NivelSintoma.CONSULTA } -> NivelSintoma.CONSULTA.name
            else -> NivelSintoma.NORMAL.name
        }
        onSave(semanaSeleccionada, sintomasSeleccionados.toList(), otrosSintomasTexto, maxSeveridad)
        if (esBlind) {
            a11yVm.hablar(loc("Reporte de síntomas guardado con éxito.", "Symptom report saved successfully."))
        }
    }

    LaunchedEffect(esBlind) {
        if (!esBlind) return@LaunchedEffect
        delay(2000L)
        ttsManager?.hablarYEsperar(
            loc(
                "Para guardar tu reporte de síntomas en cualquier momento, di guardar reporte.",
                "To save your symptom report at any time, say save report."
            ),
            margenMs = 800L
        )
        voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
            if (!isFinal) return@escuchar
            val cmd = result.lowercase().trim()
            if (cmd.contains("guardar") || cmd.contains("registrar") || cmd.contains("save") || cmd.contains("finalizar")) {
                ejecutarGuardarSintomas()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar(
                loc(
                    "Abriendo registro de síntomas de embarazo. Selecciona los síntomas que sientes hoy deslizando la pantalla. Puedes marcar o desmarcar tocando cada elemento.",
                    "Opening pregnancy symptoms log. Select the symptoms you feel today by scrolling. You can check or uncheck by tapping each item."
                )
            )
        }
    }

    var firstLoadWeek by remember { mutableStateOf(true) }
    LaunchedEffect(semanaSeleccionada) {
        if (firstLoadWeek) {
            firstLoadWeek = false
            return@LaunchedEffect
        }
        if (esBlind) {
            a11yVm.hablar(loc("Semana de embarazo seleccionada: $semanaSeleccionada", "Selected pregnancy week: $semanaSeleccionada"))
        }
    }



    var firstLoadStatus by remember { mutableStateOf(true) }
    LaunchedEffect(analizados) {
        if (firstLoadStatus) {
            firstLoadStatus = false
            return@LaunchedEffect
        }
        if (esBlind) {
            val desc = when {
                analizados.isEmpty() -> loc("Embarazo Estable. No has reportado molestias hoy. ¡Todo marcha excelente!", "Pregnancy Stable. No symptoms reported today. Everything is going great!")
                analizados.any { it.nivel == NivelSintoma.URGENCIA } -> loc("Alerta Obstétrica. Has seleccionado señales de alarma. Acude a urgencias médicas de inmediato.", "Obstetric Warning. You have reported warning signs. Go to the emergency room immediately.")
                analizados.any { it.nivel == NivelSintoma.CONSULTA } -> loc("Molestia Moderada. Tienes síntomas que ameritan programar una consulta médica regular.", "Moderate Discomfort. You have symptoms that warrant scheduling a regular medical check-up.")
                else -> loc("Síntomas Comunes. Las molestias reportadas son comunes y normales para la semana $semanaSeleccionada.", "Common Symptoms. The reported symptoms are common and normal for week $semanaSeleccionada.")
            }
            a11yVm.hablar(desc)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // Cabecera del BottomSheet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = loc("Síntomas y Guía Médica", "Symptoms & Medical Guide"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.DarkGray
                )
                Text(
                    text = loc("Fuentes Oficiales Mexicanas (IMSS)", "Official Mexican Sources (IMSS)"),
                    fontSize = 12.sp,
                    color = EmbTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Rounded.Close, null, tint = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Selector de Semana Interactivo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = EmbRosaClaro.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, EmbRosa.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = loc("Semana $semanaSeleccionada", "Week $semanaSeleccionada"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmbRosaOscuro
                    )
                    Text(
                        text = loc("${info.trimestre}º Trimestre", "${info.trimestre}o Trimester"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmbMorado
                    )
                }

                Spacer(Modifier.height(8.dp))

                Slider(
                    value = semanaSeleccionada.toFloat(),
                    onValueChange = { semanaSeleccionada = it.toInt() },
                    valueRange = 1f..40f,
                    steps = 38,
                    colors = SliderDefaults.colors(
                        thumbColor = EmbRosa,
                        activeTrackColor = EmbRosa,
                        inactiveTrackColor = EmbRosa.copy(alpha = 0.15f)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1", fontSize = 10.sp, color = Color.Gray)
                    Text("13", fontSize = 10.sp, color = Color.Gray)
                    Text("26", fontSize = 10.sp, color = Color.Gray)
                    Text("40", fontSize = 10.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Tarjeta de Estado del Embarazo (Dinámica según los síntomas seleccionados)
        val analizados = remember(sintomasSeleccionados, semanaSeleccionada) {
            sintomasSeleccionados.map { SintomasAnalyzer.analizarSintoma(it, info.trimestre) }
        }

        val statusCardBgColor: Color
        val statusCardBorderColor: Color
        val statusCardIcon: ImageVector
        val statusCardIconColor: Color
        val statusCardTitle: String
        val statusCardSubtitle: String

        when {
            analizados.isEmpty() -> {
                statusCardBgColor = Color(0xFFE8F5E9)
                statusCardBorderColor = Color(0xFFC8E6C9)
                statusCardIcon = Icons.Rounded.CheckCircle
                statusCardIconColor = Color(0xFF2E7D32)
                statusCardTitle = loc("Embarazo Estable", "Pregnancy Stable")
                statusCardSubtitle = loc("No has reportado molestias hoy. ¡Todo marcha excelente!", "No symptoms reported today. Everything is going great!")
            }
            analizados.any { it.nivel == NivelSintoma.URGENCIA } -> {
                statusCardBgColor = Color(0xFFFFEBEE)
                statusCardBorderColor = Color(0xFFFFCDD2)
                statusCardIcon = Icons.Rounded.Warning
                statusCardIconColor = Color(0xFFC62828)
                statusCardTitle = loc("Alerta Obstétrica", "Obstetric Warning")
                statusCardSubtitle = loc("Has seleccionado señales de alarma. Acude a urgencias médicas de inmediato.", "You have reported warning signs. Go to the emergency room immediately.")
            }
            analizados.any { it.nivel == NivelSintoma.CONSULTA } -> {
                statusCardBgColor = Color(0xFFFFF3E0)
                statusCardBorderColor = Color(0xFFFFE0B2)
                statusCardIcon = Icons.Rounded.Info
                statusCardIconColor = Color(0xFFEF6C00)
                statusCardTitle = loc("Molestia Moderada", "Moderate Discomfort")
                statusCardSubtitle = loc("Tienes síntomas que ameritan programar una consulta médica regular.", "You have symptoms that warrant scheduling a regular medical check-up.")
            }
            else -> {
                statusCardBgColor = EmbRosaClaro.copy(alpha = 0.5f)
                statusCardBorderColor = EmbRosa.copy(alpha = 0.3f)
                statusCardIcon = Icons.Rounded.CheckCircle
                statusCardIconColor = EmbRosaOscuro
                statusCardTitle = loc("Síntomas Comunes", "Common Symptoms")
                statusCardSubtitle = loc("Las molestias reportadas son comunes y normales para la semana $semanaSeleccionada.", "The reported symptoms are common and normal for week $semanaSeleccionada.")
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = statusCardBgColor),
            border = BorderStroke(1.dp, statusCardBorderColor)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusCardIcon,
                    contentDescription = null,
                    tint = statusCardIconColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        text = statusCardTitle,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusCardIconColor
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = statusCardSubtitle,
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Pestañas (TabRow)
        TabRow(
            selectedTabIndex = tabIndex,
            containerColor = Color.Transparent,
            contentColor = EmbRosaOscuro,
            divider = {},
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[tabIndex]),
                    color = EmbRosaOscuro
                )
            }
        ) {
            Tab(
                selected = tabIndex == 0,
                onClick = {
                    tabIndex = 0
                    if (esBlind) {
                        a11yVm.hablar(loc("Mostrando pestaña Síntomas y Reporte", "Showing Symptoms and Report tab"))
                    }
                },
                text = { Text(loc("Síntomas y Reporte", "Symptoms & Report"), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = tabIndex == 1,
                onClick = {
                    tabIndex = 1
                    if (esBlind) {
                        a11yVm.hablar(loc("Mostrando pestaña Guía Médica IMSS", "Showing IMSS Medical Guide tab"))
                    }
                },
                text = { Text(loc("Guía Médica IMSS", "IMSS Medical Guide"), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(Modifier.height(16.dp))

        // Contenido de la pestaña seleccionada
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                when (tabIndex) {
                    0 -> {
                        // Síntomas comunes
                        Text(
                            text = loc("¿Qué sientes hoy? Selecciona tus síntomas:", "What do you feel today? Select your symptoms:"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        info.sintomasEs.forEachIndexed { index, s ->
                            val sText = if (idiomaActual == IdiomaVoz.INGLES) info.sintomasEn[index] else s
                            val checked = sintomasSeleccionados.contains(s)
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (checked) EmbRosaClaro.copy(alpha = 0.3f) else Color.Transparent)
                                    .clickable {
                                        sintomasSeleccionados = if (checked) {
                                            sintomasSeleccionados - s
                                        } else {
                                            sintomasSeleccionados + s
                                        }
                                        if (esBlind) {
                                            val msg = if (checked) loc("Desmarcado: $sText", "Unchecked: $sText") else loc("Marcado: $sText", "Checked: $sText")
                                            a11yVm.hablar(msg)
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (checked) EmbRosaOscuro else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sText,
                                        fontSize = 13.sp,
                                        color = Color.DarkGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = loc("Común en esta semana", "Common for this week"),
                                        fontSize = 10.sp,
                                        color = EmbRosaOscuro,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Botón para expandir otros síntomas y alarmas
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmbTeal.copy(alpha = 0.08f))
                                .clickable { 
                                    expandirOtros = !expandirOtros
                                    if (esBlind) {
                                        val msg = if (expandirOtros) {
                                            loc("Sección de señales de alarma y otros síntomas expandida. Desliza hacia abajo para revisarlos.", "Warning signs and other symptoms section expanded. Scroll down to review them.")
                                        } else {
                                            loc("Sección de señales de alarma y otros síntomas colapsada.", "Warning signs and other symptoms section collapsed.")
                                        }
                                        a11yVm.hablar(msg)
                                    }
                                }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.MedicalServices, null, tint = EmbTeal, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = loc("Reportar otros síntomas / Señales de alarma", "Report other symptoms / Warning signs"),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmbTeal
                                )
                            }
                            Icon(
                                imageVector = if (expandirOtros) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = null,
                                tint = EmbTeal,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (expandirOtros) {
                            Spacer(Modifier.height(8.dp))
                            val consultaListEs = listOf(
                                "Vómitos frecuentes (que dificultan la alimentación).",
                                "Infección vaginal (flujo con mal olor, ardor o comezón).",
                                "Ardor, dolor o molestias al orinar.",
                                "Gripe, tos, fiebre menor a 38°C o diarrea.",
                                "Aparición de ronchas o picazón persistente en la piel."
                            )
                            val consultaListEn = listOf(
                                "Frequent vomiting (making eating difficult).",
                                "Vaginal infection (smelly discharge, burning, or itching).",
                                "Burning, pain, or discomfort when urinating.",
                                "Flu, cough, fever under 38°C (100.4°F), or diarrhea.",
                                "Hives or persistent itching on the skin."
                            )
                            val alarmasEs = listOf(
                                "Hinchazón (edema) repentina de manos, cara o pies.",
                                "Ver lucecitas de colores (fosfenos) o visión borrosa.",
                                "Zumbido constante de oídos (tinnitus).",
                                "Dolor de cabeza muy intenso y persistente.",
                                "Dolor agudo en la boca del estómago.",
                                "Sangrado vaginal o salida de líquido por la vagina.",
                                "Disminución notable o ausencia de movimientos del bebé.",
                                "Contracciones dolorosas frecuentes antes de tiempo (semana 37)."
                            )
                            val alarmasEn = listOf(
                                "Sudden swelling (edema) of hands, face, or feet.",
                                "Seeing flashing lights or blurred vision.",
                                "Constant ringing in the ears (tinnitus).",
                                "Very intense and persistent headache.",
                                "Sharp pain in the upper stomach.",
                                "Vaginal bleeding or fluid leaking.",
                                "Noticeable decrease or absence of baby movements.",
                                "Frequent painful contractions before term (week 37)."
                            )

                            Text(
                                text = loc("Señales de alarma (Urgencia):", "Warning signs (Emergency):"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD32F2F),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
                            )
                            alarmasEs.forEachIndexed { index, s ->
                                val sText = if (idiomaActual == IdiomaVoz.INGLES) alarmasEn[index] else s
                                val checked = sintomasSeleccionados.contains(s)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (checked) Color(0xFFFFEBEE) else Color.Transparent)
                                        .clickable {
                                            sintomasSeleccionados = if (checked) {
                                                sintomasSeleccionados - s
                                            } else {
                                                sintomasSeleccionados + s
                                            }
                                            if (esBlind) {
                                                val msg = if (checked) loc("Desmarcado: $sText", "Unchecked: $sText") else loc("Marcado: $sText", "Checked: $sText")
                                                a11yVm.hablar(msg)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (checked) Color(0xFFD32F2F) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = sText, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = loc("Señal de Alarma - Crítico", "Warning Sign - Critical"),
                                            fontSize = 10.sp,
                                            color = Color(0xFFD32F2F),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Text(
                                text = loc("Molestias comunes (Consulta regular):", "Common discomforts (Regular visit):"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp, start = 4.dp)
                            )
                            consultaListEs.forEachIndexed { index, s ->
                                val sText = if (idiomaActual == IdiomaVoz.INGLES) consultaListEn[index] else s
                                val checked = sintomasSeleccionados.contains(s)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (checked) Color(0xFFFFF3E0) else Color.Transparent)
                                        .clickable {
                                            sintomasSeleccionados = if (checked) {
                                                sintomasSeleccionados - s
                                            } else {
                                                sintomasSeleccionados + s
                                            }
                                            if (esBlind) {
                                                val msg = if (checked) loc("Desmarcado: $sText", "Unchecked: $sText") else loc("Marcado: $sText", "Checked: $sText")
                                                a11yVm.hablar(msg)
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (checked) Icons.Rounded.CheckBox else Icons.Rounded.CheckBoxOutlineBlank,
                                        contentDescription = null,
                                        tint = if (checked) Color(0xFFFF9800) else Color.Gray,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = sText, fontSize = 13.sp, color = Color.DarkGray, fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = loc("Molestia - Consulta regular", "Discomfort - Regular visit"),
                                            fontSize = 10.sp,
                                            color = Color(0xFFFF9800),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        // Desglose de síntomas seleccionados
                        if (sintomasSeleccionados.isNotEmpty()) {
                            Spacer(Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFBFBFB)),
                                border = BorderStroke(1.dp, Color(0xFFECECEC))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Analytics, null, tint = EmbTeal, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = loc("Análisis y Consejos de Alivio", "Analysis & Relief Tips"),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))

                                    if (analizados.any { it.nivel == NivelSintoma.URGENCIA }) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                            color = Color(0xFFFFEBEE),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color(0xFFEF5350))
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Column {
                                                    Text(
                                                        text = loc("ATENCIÓN - IR A URGENCIAS", "WARNING - GO TO ER"),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFD32F2F),
                                                        fontSize = 12.sp
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    Text(
                                                        text = loc(
                                                            "Has reportado síntomas de ALERTA OBSTÉTRICA. Dirígete de inmediato al área de urgencias de tu unidad de salud u hospital más cercano.",
                                                            "You have reported OBSTETRIC WARNING signs. Go immediately to the emergency room of your nearest hospital or health clinic."
                                                        ),
                                                        fontSize = 11.sp,
                                                        color = Color.DarkGray,
                                                        lineHeight = 15.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    analizados.forEach { detalle ->
                                        val titulo = if (idiomaActual == IdiomaVoz.INGLES) detalle.nombreEn else detalle.nombreEs
                                        val desc = if (idiomaActual == IdiomaVoz.INGLES) detalle.detalleEn else detalle.detalleEs
                                        val reco = if (idiomaActual == IdiomaVoz.INGLES) detalle.recomendacionEn else detalle.recomendacionEs
                                        val nivelLabel = if (idiomaActual == IdiomaVoz.INGLES) detalle.nivel.labelEn else detalle.nivel.labelEs

                                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = titulo,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = Color.DarkGray,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Surface(
                                                    color = detalle.nivel.color.copy(alpha = 0.15f),
                                                    contentColor = detalle.nivel.color,
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Text(
                                                        text = nivelLabel,
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Text(
                                                text = desc,
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                lineHeight = 16.sp
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Row(verticalAlignment = Alignment.Top) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Lightbulb,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFFB300),
                                                    modifier = Modifier.size(13.dp).padding(top = 1.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    text = reco,
                                                    fontSize = 11.sp,
                                                    color = Color.DarkGray,
                                                    fontWeight = FontWeight.Medium,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                            Spacer(Modifier.height(6.dp))
                                            HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = loc("¿Sientes algo más?", "Feeling anything else?"),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        val esBlind = a11yMode == AccessibilityMode.BLIND
                        val esMute = a11yMode == AccessibilityMode.MUTE
                        val esAccesible = esBlind || esMute
                        if (esAccesible) {
                            CampoTextoAccesible(
                                valor = otrosSintomasTexto,
                                onValorChange = { otrosSintomasTexto = it },
                                etiqueta = loc("¿Sientes algo más?", "Feeling anything else?"),
                                descripcionVoz = loc("Di otros síntomas que experimentes hoy", "Speak other symptoms you experience today"),
                                placeholder = loc("Escríbelo aquí...", "Write it here..."),
                                ttsManager = ttsManager,
                                colorPrimario = EmbTeal
                            )
                        } else {
                            OutlinedTextField(
                                value = otrosSintomasTexto,
                                onValueChange = { otrosSintomasTexto = it },
                                placeholder = { Text(loc("Escríbelo aquí...", "Write it here..."), fontSize = 13.sp, color = Color.Gray) },
                                modifier = Modifier.fillMaxWidth().height(90.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = EmbTeal,
                                    unfocusedBorderColor = Color.LightGray,
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp)
                            )
                        }

                        Text(
                            text = loc(
                                "Nota: Las descripciones en texto libre no son analizadas de forma automática. Si experimentas dolor intenso, sangrado o hinchazón repentina, acude de inmediato a tu unidad de salud.",
                                "Note: Free-text descriptions are not automatically analyzed. If you experience severe pain, bleeding, or sudden swelling, go immediately to your nearest health clinic."
                            ),
                            fontSize = 10.sp,
                            color = Color.Gray,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)
                        )

                        Button(
                            onClick = { ejecutarGuardarSintomas() },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmbTeal)
                        ) {
                            Text(loc("Guardar reporte", "Save report"), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                    1 -> {
                        // Guía médica IMSS
                        Text(
                            text = loc("Estudios, vacunas y consultas recomendadas por el IMSS:", "Studies, vaccines & consultations recommended by IMSS:"),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        info.guiaMedicaEs.forEachIndexed { index, g ->
                            val gText = if (idiomaActual == IdiomaVoz.INGLES) info.guiaMedicaEn[index] else g
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MedicalServices,
                                    contentDescription = null,
                                    tint = EmbTeal,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(Modifier.width(12.dp))
                                Text(text = gText, fontSize = 13.sp, color = Color.DarkGray, lineHeight = 18.sp)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Tarjeta de Señales de Alarma (IMSS) - Emergencia Obstétrica
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = loc("SEÑALES DE ALARMA - IR AL HOSPITAL", "WARNING SIGNS - GO TO HOSPITAL"),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD32F2F),
                                fontSize = 13.sp
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = loc(
                                "Acude de inmediato a urgencias/hospital si presentas cualquiera de los siguientes síntomas:",
                                "Go immediately to emergency/hospital if you experience any of the following symptoms:"
                            ),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray,
                            lineHeight = 16.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        val alarmasEs = listOf(
                            "Hinchazón (edema) repentina de manos, cara o pies.",
                            "Ver lucecitas de colores (fosfenos) o visión borrosa.",
                            "Zumbido constante de oídos (tinnitus).",
                            "Dolor de cabeza muy intenso y persistente.",
                            "Dolor agudo en la boca del estómago.",
                            "Sangrado vaginal o salida de líquido por la vagina.",
                            "Disminución notable o ausencia de movimientos del bebé.",
                            "Contracciones dolorosas frecuentes antes de tiempo (semana 37)."
                        )
                        val alarmasEn = listOf(
                            "Sudden swelling (edema) of hands, face, or feet.",
                            "Seeing flashing lights or blurred vision.",
                            "Constant ringing in the ears (tinnitus).",
                            "Very intense and persistent headache.",
                            "Sharp pain in the upper stomach.",
                            "Vaginal bleeding or fluid leaking.",
                            "Noticeable decrease or absence of baby movements.",
                            "Frequent painful contractions before term (week 37)."
                        )

                        alarmasEs.forEachIndexed { index, a ->
                            val aText = if (idiomaActual == IdiomaVoz.INGLES) alarmasEn[index] else a
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("•", fontSize = 14.sp, color = Color(0xFFD32F2F), modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = aText,
                                    fontSize = 12.sp,
                                    color = Color.DarkGray,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

data class InfoSintomasSemana(
    val trimestre: Int,
    val sintomasEs: List<String>,
    val sintomasEn: List<String>,
    val guiaMedicaEs: List<String>,
    val guiaMedicaEn: List<String>
)

fun obtenerInfoSintomas(semana: Int): InfoSintomasSemana {
    return when {
        semana <= 4 -> InfoSintomasSemana(
            trimestre = 1,
            sintomasEs = listOf("Retraso menstrual importante.", "Fatiga leve y aumento de somnolencia.", "Posible sangrado de implantación (muy leve)."),
            sintomasEn = listOf("Missed period.", "Mild fatigue and increased sleepiness.", "Possible implantation bleeding (very light)."),
            guiaMedicaEs = listOf("Confirmar embarazo (prueba de orina o sangre).", "Comenzar suplementación diaria con ácido fólico y hierro.", "Evitar el consumo de alcohol, tabaco y medicamentos no recetados."),
            guiaMedicaEn = listOf("Confirm pregnancy (urine or blood test).", "Start daily folic acid and iron supplementation.", "Avoid alcohol, tobacco, and unprescribed medications.")
        )
        semana <= 8 -> InfoSintomasSemana(
            trimestre = 1,
            sintomasEs = listOf("Náuseas y/o vómitos, especialmente por la mañana.", "Hinchazón y alta sensibilidad en los senos.", "Micción más frecuente por presión uterina en la vejiga.", "Cambios de humor y aversión a ciertos olores."),
            sintomasEn = listOf("Nausea and/or vomiting, especially in the morning.", "Breast swelling and high sensitivity.", "Frequent urination due to uterine pressure on the bladder.", "Mood swings and aversion to certain odors."),
            guiaMedicaEs = listOf("Agendar primera consulta prenatal del IMSS (idealmente antes de la semana 13).", "Detección clínica general: control de peso y presión arterial.", "Realizar análisis clínicos iniciales indicados por tu médico."),
            guiaMedicaEn = listOf("Schedule first prenatal visit at IMSS (ideally before week 13).", "General clinical assessment: weight and blood pressure checks.", "Perform initial clinical laboratory tests ordered by your doctor.")
        )
        semana <= 13 -> InfoSintomasSemana(
            trimestre = 1,
            sintomasEs = listOf("Disminución paulatina de náuseas al final del trimestre.", "Cansancio o sueño persistente.", "Aumento notable del tamaño del vientre y senos."),
            sintomasEn = listOf("Gradual decrease in nausea towards the end of the trimester.", "Persistent tiredness or sleepiness.", "Noticeable increase in belly and breast size."),
            guiaMedicaEs = listOf("Completar primera ecografía básica para verificar viabilidad fetal.", "Revisión dental obligatoria.", "Mantener dieta equilibrada rica en calcio, hierro y proteínas."),
            guiaMedicaEn = listOf("Complete first basic ultrasound to verify fetal viability.", "Mandatory dental check-up.", "Maintain a balanced diet rich in calcium, iron, and proteins.")
        )
        semana <= 17 -> InfoSintomasSemana(
            trimestre = 2,
            sintomasEs = listOf("Aparición de antojos y aumento del apetito.", "Disminución de náuseas y aumento de energía general.", "Congestión nasal o sangrados nasales leves.", "Dolor leve en los costados del abdomen (crecimiento del útero)."),
            sintomasEn = listOf("Appetite changes and food cravings.", "Decrease in nausea and general increase in energy.", "Nasal congestion or minor nosebleeds.", "Mild round ligament pain on the sides of the abdomen."),
            guiaMedicaEs = listOf("Segunda consulta prenatal oficial.", "Monitoreo continuo de presión arterial para vigilar preeclampsia.", "Aplicación de vacuna contra influenza estacional (según temporada)."),
            guiaMedicaEn = listOf("Second official prenatal consultation.", "Continuous blood pressure monitoring to watch for preeclampsia.", "Seasonal influenza vaccination (if applicable).")
        )
        semana <= 22 -> InfoSintomasSemana(
            trimestre = 2,
            sintomasEs = listOf("Sensación de los primeros movimientos fetales (como burbujas).", "Hinchazón leve de tobillos o pies al final del día.", "Cambios en la pigmentación de la piel (línea alba en abdomen).", "Dolor de espalda bajo debido al cambio del centro de gravedad."),
            sintomasEn = listOf("Feeling the first baby movements (like flutters).", "Mild swelling of ankles or feet at the end of the day.", "Skin pigmentation changes (linea nigra on abdomen).", "Lower back pain due to shifting center of gravity."),
            guiaMedicaEs = listOf("Tercera consulta prenatal.", "Realizar Ecografía Estructural Fetal (entre semanas 18 y 22) para evaluar anatomía del bebé.", "Monitoreo de frecuencia cardíaca fetal."),
            guiaMedicaEn = listOf("Third prenatal consultation.", "Perform Fetal Structural Ultrasound (weeks 18-22) to evaluate baby anatomy.", "Fetal heart rate monitoring.")
        )
        semana <= 26 -> InfoSintomasSemana(
            trimestre = 2,
            sintomasEs = listOf("Calambres nocturnos en las piernas.", "Estreñimiento o acidez estomacal (reflujo).", "Movimientos del bebé más vigorosos y notorios desde el exterior."),
            sintomasEn = listOf("Nighttime leg cramps.", "Constipation or heartburn (reflux).", "Baby movements become more vigorous and visible from the outside."),
            guiaMedicaEs = listOf("Cuarta consulta prenatal.", "Realizar tamizaje para Diabetes Gestacional (curva de tolerancia a la glucosa) entre semanas 24 y 28.", "Aplicación de vacuna Tdpa (tétanos, difteria, tos ferina) a partir de la semana 20."),
            guiaMedicaEn = listOf("Fourth prenatal consultation.", "Screening for Gestational Diabetes (oral glucose tolerance test) between weeks 24 and 28.", "Tdpa vaccination (tetanus, diphtheria, pertussis) from week 20 onwards.")
        )
        semana <= 32 -> InfoSintomasSemana(
            trimestre = 3,
            sintomasEs = listOf("Dificultad leve para respirar (útero presiona diafragma).", "Contracciones de Braxton Hicks (práctica, no dolorosas ni regulares).", "Fatiga recurrente por peso y mala calidad de sueño."),
            sintomasEn = listOf("Mild shortness of breath (uterus presses on diaphragm).", "Braxton Hicks contractions (practice, painless, and irregular).", "Recurrent fatigue due to weight and poor sleep quality."),
            guiaMedicaEs = listOf("Quinta consulta prenatal.", "Monitoreo del crecimiento del fondo uterino.", "Orientación sobre lactancia materna y planificación familiar postparto."),
            guiaMedicaEn = listOf("Fifth prenatal consultation.", "Monitoring of fundal height growth.", "Guidance on breastfeeding and postpartum family planning.")
        )
        semana <= 35 -> InfoSintomasSemana(
            trimestre = 3,
            sintomasEs = listOf("Presión pélvica aumentada (bebé empieza a encajarse).", "Dolor constante en zona lumbar.", "Hinchazón de pies más notoria.", "Dificultad para encontrar posición cómoda para dormir."),
            sintomasEn = listOf("Increased pelvic pressure (baby begins to drop).", "Constant lower back pain.", "More noticeable swelling of feet.", "Difficulty finding a comfortable sleeping position."),
            guiaMedicaEs = listOf("Semana 34: Expedición de incapacidad por maternidad del IMSS (84 días naturales).", "Sexta consulta prenatal.", "Preparación final de la maleta del hospital y plan de parto."),
            guiaMedicaEn = listOf("Week 34: Issuance of IMSS maternity leave (84 calendar days).", "Sixth prenatal consultation.", "Final preparation of the hospital bag and birth plan.")
        )
        else -> InfoSintomasSemana(
            trimestre = 3,
            sintomasEs = listOf("Contracciones más frecuentes.", "Pérdida del tapón mucoso (flujo gelatinoso espeso).", "Presión intensa en la pelvis y vejiga.", "Ansiedad o nerviosismo por el parto."),
            sintomasEn = listOf("More frequent contractions.", "Loss of mucus plug (thick gelatinous discharge).", "Intense pressure on the pelvis and bladder.", "Anxiety or nervousness about labor."),
            guiaMedicaEs = listOf("Consultas semanales de control final.", "Semana 36: Médico familiar entrega hoja de referencia a Segundo Nivel (hospital de zona) para atención del parto.", "Monitoreo estricto de movimientos fetales y presión arterial."),
            guiaMedicaEn = listOf("Weekly final check-ups.", "Week 36: Family doctor provides referral sheet to Second Level (regional hospital) for childbirth care.", "Strict monitoring of fetal movements and blood pressure.")
        )
    }
}

@Composable
private fun HeaderActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = EmbRosaOscuro, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 10.sp, color = EmbRosaOscuro, fontWeight = FontWeight.Bold)
    }
}

fun calcularSemanasDesdeFUM(fumStr: String): Int {
    return try {
        val (dia, mes, anio) = if (fumStr.contains("/")) {
            val p = fumStr.split("/").map { it.toInt() }
            Triple(p[0], p[1], p[2])
        } else {
            val p = fumStr.split("-").map { it.toInt() }
            Triple(p[2], p[1], p[0])
        }
        val fumDate = kotlinx.datetime.LocalDate(anio, mes, dia)
        val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
        val diffDays = fumDate.daysUntil(today)
        val calculatedWeeks = (diffDays / 7)
        calculatedWeeks.coerceIn(1, 40)
    } catch (_: Exception) {
        1
    }
}

@Composable
fun GatekeeperForm(
    perfil: PerfilEmbarazo,
    idiomaActual: IdiomaVoz,
    onSave: (pesoPre: String, estatura: String, fum: String, pesoAct: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    val today = kotlinx.datetime.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
    val year = today.year
    val month = today.monthNumber - 1
    val day = today.dayOfMonth

    var pesoPreInput by remember { mutableStateOf("") }
    var estaturaInput by remember { mutableStateOf("") }
    var fumInput by remember { mutableStateOf("") }
    var pesoActInput by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esBlind = a11yMode == AccessibilityMode.BLIND
    val esMute = a11yMode == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute
    val voiceManager = remember { if (esBlind) VoiceInputManager() else null }

    val ejecutarGuardarGatekeeper: () -> Unit = {
        errorMsg = ""
        val pesoPre = pesoPreInput.toDoubleOrNull()
        if (pesoPre == null || pesoPre <= 30.0 || pesoPre > 200.0) {
            errorMsg = loc("Por favor ingresa un peso pre-gestacional válido (30-200 kg)", "Please enter a valid pre-pregnancy weight (30-200 kg)")
        } else {
            val estatura = estaturaInput.toDoubleOrNull()
            if (estatura == null || estatura <= 0.5 || (estatura > 3.0 && estatura < 50.0) || estatura > 250.0) {
                errorMsg = loc("Por favor ingresa una estatura válida (ej. 1.65 o 165)", "Please enter a valid height (e.g. 1.65 or 165)")
            } else {
                val pesoAct = pesoActInput.toDoubleOrNull()
                if (pesoAct == null || pesoAct <= 30.0 || pesoAct > 200.0) {
                    errorMsg = loc("Por favor ingresa un peso actual válido (30-200 kg)", "Please enter a valid current weight (30-200 kg)")
                } else {
                    if (fumInput.isNotBlank()) {
                        val p = fumInput.split("/")
                        if (p.size != 3 || p[0].toIntOrNull() !in 1..31 || p[1].toIntOrNull() !in 1..12 || (p[2].toIntOrNull() ?: 0) !in 1900..2100) {
                            errorMsg = loc("Formato de fecha FUM incorrecto. Usa DD/MM/YYYY", "Incorrect date format. Use DD/MM/YYYY")
                        }
                    }
                    if (errorMsg.isEmpty()) {
                        onSave(pesoPreInput, estaturaInput, fumInput, pesoActInput)
                    }
                }
            }
        }
    }

    LaunchedEffect(pesoPreInput, estaturaInput, pesoActInput) {
        if (!esBlind) return@LaunchedEffect
        if (pesoPreInput.isNotBlank() && estaturaInput.isNotBlank() && pesoActInput.isNotBlank()) {
            delay(1000L)
            ttsManager?.hablarYEsperar(
                loc(
                    "Perfil completado. Di guardar y continuar para guardar tus datos.",
                    "Profile completed. Say save and continue to save your data."
                ),
                margenMs = 800L
            )
            voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                val cmd = result.lowercase().trim()
                if (cmd.contains("guardar") || cmd.contains("continuar") || cmd.contains("save")) {
                    ejecutarGuardarGatekeeper()
                }
            }
        }
    }

    

    val scrollState = rememberScrollState()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFFF5F9))
            .imePadding()
            .padding(24.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = loc("Completa tu Perfil de Embarazo", "Complete your Pregnancy Profile"),
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFFD4679A),
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = loc(
                "Para poder calcular tu ganancia de peso óptima y guiarte según las normativas oficiales, por favor completa los siguientes datos básicos:",
                "To calculate your optimal weight gain and guide you according to official guidelines, please complete the following basic details:"
            ),
            fontSize = 13.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                if (esAccesible) {
                    CampoTextoAccesible(
                        valor = pesoPreInput,
                        onValorChange = { pesoPreInput = it },
                        etiqueta = loc("Peso antes del embarazo (kg)", "Pregnancy pre-weight (kg)"),
                        descripcionVoz = loc("Di tu peso antes del embarazo en kilogramos", "Speak your pre-pregnancy weight in kilograms"),
                        placeholder = "Ej. 65",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        ttsManager = ttsManager,
                        colorPrimario = Color(0xFFEC9BBF)
                    )
                    Spacer(Modifier.height(16.dp))
                    CampoTextoAccesible(
                        valor = estaturaInput,
                        onValorChange = { estaturaInput = it },
                        etiqueta = loc("Estatura (metros)", "Height (meters)"),
                        descripcionVoz = loc("Di tu estatura en metros, por ejemplo uno punto sesenta y dos", "Speak your height in meters, for example one point sixty two"),
                        placeholder = "Ej. 1.62",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        ttsManager = ttsManager,
                        colorPrimario = Color(0xFFEC9BBF)
                    )
                    if (perfil.fechaUltimaMenstruacion.isBlank()) {
                        Spacer(Modifier.height(16.dp))
                        CampoTextoAccesible(
                            valor = fumInput,
                            onValorChange = { fumInput = it },
                            etiqueta = loc("Fecha de última regla (Día/Mes/Año)", "Last period date (Day/Month/Year)"),
                            descripcionVoz = loc("Toca dos veces para seleccionar fecha", "Double tap to select date"),
                            esCampoFecha = true,
                            ttsManager = ttsManager,
                            colorPrimario = Color(0xFFEC9BBF)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    CampoTextoAccesible(
                        valor = pesoActInput,
                        onValorChange = { pesoActInput = it },
                        etiqueta = loc("Peso actual hoy (kg)", "Current weight today (kg)"),
                        descripcionVoz = loc("Di tu peso actual hoy en kilogramos", "Speak your current weight today in kilograms"),
                        placeholder = "Ej. 67",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        ttsManager = ttsManager,
                        colorPrimario = Color(0xFFEC9BBF)
                    )
                } else {
                    OutlinedTextField(
                        value = pesoPreInput,
                        onValueChange = { pesoPreInput = it },
                        label = { Text(loc("Peso antes del embarazo (kg)", "Pregnancy pre-weight (kg)")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = if (pesoPreInput.isNotBlank()) {
                            {
                                IconButton(onClick = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }) {
                                    Icon(Icons.Rounded.ArrowForward, contentDescription = "Siguiente", tint = Color(0xFFD4679A))
                                }
                            }
                        } else null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Next
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC9BBF),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = estaturaInput,
                        onValueChange = { estaturaInput = it },
                        label = { Text(loc("Estatura (ej. 165 cm o 1.65 m)", "Height (e.g., 165 cm or 1.65 m)")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = if (estaturaInput.isNotBlank()) {
                            {
                                IconButton(onClick = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }) {
                                    Icon(Icons.Rounded.ArrowForward, contentDescription = "Siguiente", tint = Color(0xFFD4679A))
                                }
                            }
                        } else null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = if (perfil.fechaUltimaMenstruacion.isBlank()) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) },
                            onDone = { focusManager.clearFocus(); ejecutarGuardarGatekeeper() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC9BBF),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )

                    if (perfil.fechaUltimaMenstruacion.isBlank()) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fumInput,
                            onValueChange = { fumInput = it },
                            label = { Text(loc("Fecha de última regla (opcional)", "Last period date (optional)")) },
                            placeholder = { Text("DD/MM/YYYY") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            trailingIcon = {
                                if (fumInput.isNotBlank()) {
                                    IconButton(onClick = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }) {
                                        Icon(Icons.Rounded.ArrowForward, contentDescription = "Siguiente", tint = Color(0xFFD4679A))
                                    }
                                } else {
                                    Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color.Gray)
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
                                imeAction = androidx.compose.ui.text.input.ImeAction.Next
                            ),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                onNext = { focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down) }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFEC9BBF),
                                unfocusedBorderColor = Color.LightGray
                            )
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = pesoActInput,
                        onValueChange = { pesoActInput = it },
                        label = { Text(loc("Peso actual hoy (kg)", "Current weight today (kg)")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        trailingIcon = if (pesoActInput.isNotBlank()) {
                            {
                                IconButton(onClick = { focusManager.clearFocus(); ejecutarGuardarGatekeeper() }) {
                                    Icon(Icons.Rounded.Check, contentDescription = "Guardar", tint = Color(0xFF2E7D32))
                                }
                            }
                        } else null,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                            imeAction = androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onDone = { focusManager.clearFocus(); ejecutarGuardarGatekeeper() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEC9BBF),
                            unfocusedBorderColor = Color.LightGray
                        )
                    )
                }
            }
        }

        if (errorMsg.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = errorMsg,
                color = Color(0xFFD32F2F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { ejecutarGuardarGatekeeper() },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4679A))
        ) {
            Text(loc("Guardar y Continuar", "Save & Continue"), fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(Modifier.height(16.dp))

        TextButton(onClick = onLogout) {
            Text(loc("Cerrar sesión", "Logout"), color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

