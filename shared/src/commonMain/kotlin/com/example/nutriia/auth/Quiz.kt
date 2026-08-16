package com.example.nutriia.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.*
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.ui.theme.parsearAlergenos
import kotlinx.coroutines.delay

private val QuizAccent = Color(0xFFEC9BBF)

private val IQ_CardBg   = Color(0xFFFFFFFF)
private val IQ_BgSoft   = Color(0xFFF5F3EE)
private val IQ_Divider  = Color(0xFFE8E8E8)
private val IQ_TextDark = Color(0xFF1B2A1B)
private val IQ_TextMid  = Color(0xFF78909C)

// ═══════════════════════════════════════════════════════════════════════════════
// HELPERS — FECHA
// ═══════════════════════════════════════════════════════════════════════════════

private fun millisToDateString(millis: Long): String {
    return com.example.nutriia.utils.FechaUtils.formatearFecha(millis)
}

private fun dateStringToMillis(dateStr: String): Long? {
    if (dateStr.length != 10) return null
    return try {
        val parts = dateStr.split("/")
        val d = parts[0].toLong()
        val m = parts[1].toLong()
        val y = parts[2].toLong()
        val days = (y - 1970) * 365 + (y - 1969) / 4 + (m - 1) * 30 + (d - 1)
        days * 86400_000L
    } catch (_: Exception) { null }
}

private fun dateStringToReadable(value: String): String? {
    if (value.length != 10) return null
    val meses = listOf(
        "", "enero", "febrero", "marzo", "abril", "mayo", "junio",
        "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    )
    return runCatching {
        val p = value.split("/")
        "${p[0].toInt()} de ${meses[p[1].toInt()]} de ${p[2]}"
    }.getOrNull()
}

// ─── Validar que el string manual "DD/MM/YYYY" sea una fecha real ─────────────
// Solo acepta fechas de niños de 0 a 12 años cumplidos.
private fun esFechaValida(value: String): Boolean {
    if (value.length != 10) return false
    return runCatching {
        val p = value.split("/")
        val d = p[0].toInt(); val m = p[1].toInt(); val y = p[2].toInt()
        if (m !in 1..12 || d !in 1..31) return false
        val currentYear = 2026
        y in (currentYear - 12)..currentYear
    }.getOrDefault(false)
}

private fun edadSuperaLimite(value: String): Boolean {
    if (value.length != 10) return false
    return runCatching {
        val p = value.split("/")
        val y = p[2].toInt()
        y < (2026 - 12)
    }.getOrDefault(false)
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPERS — TEXTO VOZ
// ═══════════════════════════════════════════════════════════════════════════════

private fun parsearFechaVoz(texto: String): String {
    val t = texto.lowercase().trim()
    val meses = mapOf(
        "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4,
        "mayo" to 5, "junio" to 6, "julio" to 7, "agosto" to 8,
        "septiembre" to 9, "setiembre" to 9, "octubre" to 10,
        "noviembre" to 11, "diciembre" to 12
    )
    val numerosTexto = mapOf(
        "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
        "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14,
        "quince" to 15, "dieciséis" to 16, "dieciseis" to 16,
        "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19,
        "veinte" to 20, "veintiuno" to 21, "veintidós" to 22, "veintidos" to 22,
        "veintitrés" to 23, "veintitres" to 23, "veinticuatro" to 24,
        "veinticinco" to 25, "veintiséis" to 26, "veintiseis" to 26,
        "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
        "treinta" to 30, "treinta y uno" to 31
    )
    fun parsearNum(s: String): Int? = s.toIntOrNull() ?: numerosTexto[s.trim()]
    fun parsearAnio(s: String): Int? {
        val r = s.trim()
        if (r.matches(Regex("\\d{4}"))) return r.toInt()
        if (r.matches(Regex("\\d{2}"))) return ("20$r").toInt()
        val dm = Regex("""dos mil\s*(.*)""").find(r)
        if (dm != null) {
            val sufijo = dm.groupValues[1].trim()
            if (sufijo.isEmpty()) return 2000
            val n = numerosTexto[sufijo]
            if (n != null) return 2000 + n
            if (sufijo == "diez") return 2010
        }
        return null
    }
    val conDe = Regex("""(\S+)\s+de\s+(\w+)\s+de\s+(.+)""")
    conDe.find(t)?.let { m ->
        val dia  = parsearNum(m.groupValues[1]) ?: return@let
        val mes  = meses[m.groupValues[2]] ?: return@let
        val anio = parsearAnio(m.groupValues[3]) ?: return@let
        return corregirFormatoFecha("${dia.toString().padStart(2, '0')}/${mes.toString().padStart(2, '0')}/$anio")
    }
    for ((nombreMes, numMes) in meses) {
        val idx = t.indexOf(nombreMes)
        if (idx < 0) continue
        val parteAntes   = t.substring(0, idx).trim()
        val parteDespues = t.substring(idx + nombreMes.length).trim()
        val dia  = parsearNum(parteAntes.split(" ").lastOrNull() ?: "") ?: continue
        val anio = parsearAnio(parteDespues) ?: continue
        return corregirFormatoFecha("${dia.toString().padStart(2, '0')}/${numMes.toString().padStart(2, '0')}/$anio")
    }
    val numerico = Regex("""(\d{1,2})[/\-\s](\d{1,2})[/\-\s](\d{2,4})""")
    numerico.find(t)?.let { m ->
        val dia  = m.groupValues[1].padStart(2, '0')
        val mes  = m.groupValues[2].padStart(2, '0')
        val anio = m.groupValues[3].let { if (it.length == 2) "20$it" else it }
        return corregirFormatoFecha("$dia/$mes/$anio")
    }
    val soloDigitos = t.filter { it.isDigit() }.take(8)
    if (soloDigitos.length == 8) {
        val rawDate = "${soloDigitos.substring(0, 2)}/${soloDigitos.substring(2, 4)}/${soloDigitos.substring(4, 8)}"
        return corregirFormatoFecha(rawDate)
    }
    return texto
}

private fun corregirFormatoFecha(fecha: String): String {
    val partes = fecha.split("/")
    if (partes.size != 3) return fecha
    val diaStr = partes[0]
    val mesStr = partes[1]
    val anioStr = partes[2]
    val d = diaStr.toIntOrNull() ?: 0
    val m = mesStr.toIntOrNull() ?: 0
    
    var finalDia = d
    var finalMes = m
    
    if (m > 12 && d in 1..12) {
        finalDia = m
        finalMes = d
    }
    
    finalMes = finalMes.coerceIn(1, 12)
    finalDia = finalDia.coerceIn(1, 31)
    
    return "${finalDia.toString().padStart(2, '0')}/${finalMes.toString().padStart(2, '0')}/$anioStr"
}

private fun colorAlergeno(alergeno: Alergeno): Color = when (alergeno) {
    Alergeno.HUEVO     -> Color(0xFFFF8F00)
    Alergeno.LACTEOS   -> Color(0xFF1565C0)
    Alergeno.CACAHUATE -> Color(0xFF6D4C41)
    Alergeno.NUECES    -> Color(0xFF558B2F)
    Alergeno.TRIGO     -> Color(0xFFD4A017)
    Alergeno.SOYA      -> Color(0xFF00897B)
    Alergeno.PESCADO   -> Color(0xFF0277BD)
    Alergeno.MARISCOS  -> Color(0xFF00838F)
    Alergeno.MAIZ      -> Color(0xFFEF8C00)
    Alergeno.FRUCTOSA  -> Color(0xFFE53935)
}

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL DEL QUIZ
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun OnboardingQuizScreen(
    onQuizComplete:            (ChildProfile) -> Unit,
    isAddingChild:             Boolean  = false,
    prefilledChildName:        String   = "",
    soloAccesibilidad:         Boolean  = false,
    saltarAccesibilidad:       Boolean  = false,
    initialStep:               Int      = 0,
    prefilledProfile:          ChildProfile? = null,
    onAccesibilidadCompletada: () -> Unit = {},
    onCancel:                  () -> Unit = {}
) {
        val haptic  = LocalHapticFeedback.current

    val stepInicial = when {
        soloAccesibilidad   -> 0
        saltarAccesibilidad -> maxOf(1, initialStep)
        else                -> initialStep
    }

    var currentStep      by remember { mutableIntStateOf(stepInicial) }
    var goingForward     by remember { mutableStateOf(true) }
    var showCancelDialog by remember { mutableStateOf(false) }

    var profile by remember {
        mutableStateOf(
            prefilledProfile?.copy(name = prefilledProfile.name.ifBlank { prefilledChildName })
                ?: ChildProfile(
                    name         = prefilledChildName,
                    nivelIngreso = NivelIngreso.BASICO,
                    region       = RegionMexico.CENTRO
                )
        )
    }

    val accessibilityVm: AccessibilityViewModel = remember { AccessibilityViewModel() }
    val idiomaActual   by accessibilityVm.idioma.collectAsState()
    val modoGuardado   by accessibilityVm.mode.collectAsState()

    var selectedA11yMode by remember(modoGuardado) {
        mutableStateOf(
            if (false) AccessibilityMode.BLIND else modoGuardado
        )
    }
    val ttsManager = accessibilityVm.ttsManager
    var mostrarDialogoTalkBack by remember { mutableStateOf(false) }

    val totalSteps = if (soloAccesibilidad) 1 else 7

    val isNextEnabled = when (currentStep) {
        0, 1 -> true
        2    -> profile.name.isNotBlank()
        3    -> profile.birthDate.length == 10
        4    -> profile.weightKg.isNotBlank() && profile.heightCm.isNotBlank()
        5    -> true
        6    -> true
        else -> true
    }

    fun loc(esTexto: String, enTexto: String) =
        if (idiomaActual == IdiomaVoz.INGLES) enTexto else esTexto

    LaunchedEffect(Unit) {
        if (soloAccesibilidad)
            accessibilityVm.iniciarTTS(
                loc(
                    Voz.ACCESIBILIDAD_INTRO + " " + Voz.IDIOMA_INTRO,
                    VozEn.ACCESIBILIDAD_INTRO + " " + VozEn.IDIOMA_INTRO
                )
            )
    }

    LaunchedEffect(currentStep, selectedA11yMode, idiomaActual) {
        if (selectedA11yMode != AccessibilityMode.BLIND) return@LaunchedEffect
        if (soloAccesibilidad) return@LaunchedEffect
        val texto = when (currentStep) {
            0    -> loc(Voz.ACCESIBILIDAD_INTRO + " " + Voz.IDIOMA_INTRO, VozEn.ACCESIBILIDAD_INTRO + " " + VozEn.IDIOMA_INTRO)
            1    -> loc(Voz.QUIZ_BIENVENIDA,   VozEn.QUIZ_BIENVENIDA)
            2    -> loc(Voz.QUIZ_NOMBRE,        VozEn.QUIZ_NOMBRE)
            3    -> loc(Voz.QUIZ_FECHA,         VozEn.QUIZ_FECHA)
            4    -> loc(Voz.QUIZ_MEDIDAS,       VozEn.QUIZ_MEDIDAS)
            5    -> loc(Voz.QUIZ_CONDICIONES,   VozEn.QUIZ_CONDICIONES)
            6    -> loc(
                "Último paso, lo prometo. Selecciona el ingreso familiar y la región de México donde vives. Cuando termines, toca el botón verde Finalizar registro al final.",
                "Last step, I promise. Select your family income level and the region of Mexico where you live. When you're done, tap the green Finish registration button at the bottom."
            )
            else -> ""
        }
        if (texto.isNotEmpty()) accessibilityVm.hablar(texto)
    }

    LaunchedEffect(currentStep, isNextEnabled) {
        if (selectedA11yMode != AccessibilityMode.BLIND) return@LaunchedEffect
        if (soloAccesibilidad) return@LaunchedEffect
        if (isNextEnabled) {
            if (currentStep in listOf(2, 3, 4)) {
                accessibilityVm.hablar(loc(
                    "Paso listo. El botón verde para continuar está al final de la pantalla.",
                    "Step ready. The green continue button is at the bottom of the screen."
                ))
            }
        }
    }

    // ── Diálogo TalkBack ──────────────────────────────────────────────────────
    if (mostrarDialogoTalkBack) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoTalkBack = false },
            icon  = { Icon(Icons.Rounded.Accessibility, null, tint = NutriaGreen, modifier = Modifier.size(32.dp)) },
            title = { Text("Activar lector de pantalla", fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Para que Android lea todo en voz alta activa TalkBack en Configuración.", fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 20.sp)
                    Surface(color = NutriaGreen.copy(alpha = 0.07f), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Pasos:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                            Text("1. Toca \"Ir a Configuración\"", fontSize = 12.sp, color = Color.DarkGray)
                            Text("2. Busca TalkBack o Lector de pantalla", fontSize = 12.sp, color = Color.DarkGray)
                            Text("3. Actívalo y regresa a NutriIA", fontSize = 12.sp, color = Color.DarkGray)
                        }
                    }
                    Text("Nutri/IA ya tiene voz propia. Funciona aunque no actives TalkBack.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                }
            },
            confirmButton = {
                Button(
                    onClick = { mostrarDialogoTalkBack = false;  },
                    colors  = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ir a Configuración", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoTalkBack = false }) {
                    Text("Continuar con voz de Nutri/IA", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Diálogo CANCELAR ──────────────────────────────────────────────────────
    AnimatedVisibility(
        visible = showCancelDialog,
        enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium), 0.85f) + fadeIn(tween(200)),
        exit    = scaleOut(tween(150), 0.9f) + fadeOut(tween(150))
    ) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            containerColor   = Color.White,
            shape            = RoundedCornerShape(24.dp),
            icon = {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFFFEBEE)),
                    Alignment.Center
                ) {
                    Icon(Icons.Rounded.ExitToApp, null, tint = Color(0xFFE53935), modifier = Modifier.size(24.dp))
                }
            },
            title = {
                Text(
                    if (isAddingChild) "¿Cancelar el registro?" else "¿Abandonar el registro?",
                    fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = Color(0xFF1B2A1B)
                )
            },
            text = {
                Text(
                    "Se perderán los datos que ingresaste en este formulario.",
                    textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCancelDialog = false; onCancel() },
                    colors  = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Salir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCancelDialog = false },
                    shape   = RoundedCornerShape(12.dp),
                    border  = BorderStroke(1.dp, NutriaGreen)
                ) { Text("Quedarme", color = NutriaGreen, fontWeight = FontWeight.Bold) }
            }
        )
    }

    Box(Modifier.fillMaxSize().background(NutriaBgCrema)) {
        Column(
            modifier            = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            // ── Fila back / cancel ─────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = currentStep > 0,
                    enter   = scaleIn(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(tween(200)),
                    exit    = scaleOut(tween(150)) + fadeOut(tween(150))
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NutriaGreen.copy(0.10f))
                            .clickable(onClickLabel = "Paso anterior") {
                                vibrateTap(haptic)
                                goingForward = false
                                 currentStep--
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = NutriaGreen, modifier = Modifier.size(18.dp))
                    }
                }
                if (currentStep == 0) Spacer(Modifier.width(38.dp))

                Spacer(Modifier.weight(1f))

                TextButton(
                    onClick  = { showCancelDialog = true },
                    modifier = Modifier.semantics { contentDescription = "Cancelar y salir del registro" }
                ) {
                    Icon(Icons.Rounded.Close, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Cancelar", color = Color.Gray, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            QuizHeader(currentStep, totalSteps, isAddingChild)
            Spacer(Modifier.height(28.dp))

            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        if (targetState > initialState) {
                            (slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220)) { -it } + fadeOut(tween(220)))
                        } else {
                            (slideInHorizontally(tween(220)) { -it } + fadeIn(tween(220)))
                                .togetherWith(slideOutHorizontally(tween(220)) { it } + fadeOut(tween(220)))
                        }
                    },
                    label = "quiz_step"
                ) { step ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (step) {
                            0 -> StepAccesibilidad(
                                selected       = selectedA11yMode,
                                idiomaActual   = idiomaActual,
                                talkBackActivo = false,
                                onSelect       = { modo ->
                                    vibrateTap(haptic)
                                    selectedA11yMode = modo
                                    accessibilityVm.setMode(modo)
                                    if (modo == AccessibilityMode.BLIND && !false)
                                        mostrarDialogoTalkBack = true
                                },
                                onIdiomaSelect = { idioma -> vibrateTap(haptic); accessibilityVm.setIdioma(idioma) }
                            )
                            1 -> StepBienvenida(isAddingChild, selectedA11yMode)
                            2 -> StepNombre(
                                value         = profile.name,
                                modo          = selectedA11yMode,
                                idioma        = idiomaActual,
                                ttsManager    = ttsManager,
                                onValueChange = { profile = profile.copy(name = it) },
                                sexo          = profile.sexo,
                                onSexoChange  = { profile = profile.copy(sexo = it) },
                                descripcionVozNombre = loc(Voz.QUIZ_NOMBRE, VozEn.QUIZ_NOMBRE)
                            )
                            3 -> StepFechaNacimiento(
                                value         = profile.birthDate,
                                modo          = selectedA11yMode,
                                idioma        = idiomaActual,
                                ttsManager    = ttsManager,
                                onValueChange = { profile = profile.copy(birthDate = it) },
                                descripcionVozFecha = loc(Voz.QUIZ_FECHA, VozEn.QUIZ_FECHA)
                            )
                            4 -> StepMedidas(
                                weight         = profile.weightKg,
                                height         = profile.heightCm,
                                modo           = selectedA11yMode,
                                idioma         = idiomaActual,
                                ttsManager     = ttsManager,
                                onWeightChange = { profile = profile.copy(weightKg = it) },
                                onHeightChange = { profile = profile.copy(heightCm = it) },
                                descripcionVozPeso  = loc("di el peso en kilogramos, por ejemplo 7 punto 5", "say the weight in kilograms, for example 7 point 5"),
                                descripcionVozTalla = loc("ahora di la talla en centímetros, por ejemplo 68", "now say the height in centimeters, for example 68")
                            )
                            5 -> StepCondiciones(
                                hasAllergies             = profile.hasAllergies,
                                allergiesDetail          = profile.allergiesDetail,
                                hasConditions            = profile.hasConditions,
                                conditionsDetail         = profile.conditionsDetail,
                                onAllergiesToggle        = { profile = profile.copy(hasAllergies = it) },
                                onAllergiesDetailChange  = { profile = profile.copy(allergiesDetail = it) },
                                onConditionsToggle       = { profile = profile.copy(hasConditions = it) },
                                onConditionsDetailChange = { profile = profile.copy(conditionsDetail = it) }
                            )
                            6 -> StepIngresoRegion(
                                nivelSeleccionado  = profile.nivelIngreso,
                                regionSeleccionada = profile.region,
                                onNivelChange      = { profile = profile.copy(nivelIngreso = it) },
                                onRegionChange     = { profile = profile.copy(region = it) }
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            val esFinalPaso = currentStep == totalSteps - 1

            val textoBoton = if (soloAccesibilidad || !esFinalPaso) loc("Continuar", "Continue")
            else loc("Finalizar registro", "Finish registration")

            val labelBoton = if (soloAccesibilidad || !esFinalPaso) loc(Voz.BTN_CONTINUAR, VozEn.BTN_CONTINUAR)
            else loc(Voz.BTN_FINALIZAR, VozEn.BTN_FINALIZAR)

            Button(
                onClick = {
                    vibrateSuccess(haptic)
                    if (selectedA11yMode == AccessibilityMode.BLIND) {
                        accessibilityVm.hablar(
                            when {
                                soloAccesibilidad -> loc("Configuración guardada. Continuando.", "Settings saved. Continuing.")
                                esFinalPaso       -> loc("Finalizando registro.", "Finishing registration.")
                                else              -> loc("Avanzando al paso ${currentStep + 1}.", "Moving to step ${currentStep + 1}.")
                            }
                        )
                    }
                    if (soloAccesibilidad) {
                        accessibilityVm.setMode(selectedA11yMode)
                        onAccesibilidadCompletada()
                    } else if (currentStep < totalSteps - 1) {
                        goingForward = true
                         currentStep++
                    } else {
                        accessibilityVm.setMode(selectedA11yMode)
                        accessibilityVm.silenciar()
                        onQuizComplete(profile)
                    }
                },
                enabled  = isNextEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (selectedA11yMode == AccessibilityMode.BLIND) 70.dp else 56.dp)
                    .semantics(mergeDescendants = true) { contentDescription = labelBoton },
                shape  = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = NutriaGreen,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                Text(textoBoton, fontSize = if (selectedA11yMode == AccessibilityMode.BLIND) 18.sp else 16.sp, fontWeight = FontWeight.Bold)
                if (!esFinalPaso || soloAccesibilidad) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
                }
            }

            if (currentStep == 5) {
                TextButton(onClick = { accessibilityVm.setMode(selectedA11yMode); goingForward = true; currentStep++ }) {
                    Text(loc("Saltar por ahora", "Skip for now"), color = Color.Gray, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// QUIZ HEADER
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun QuizHeader(currentStep: Int, totalSteps: Int, isAddingChild: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.semantics(mergeDescendants = true) {}
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape).background(NutriaGreen.copy(alpha = 0.12f)).semantics { contentDescription = "" },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isAddingChild) Icons.Rounded.PersonAdd else Icons.Rounded.Eco,
                null, tint = NutriaGreen, modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (isAddingChild) "Agregar hijo/a" else "NutriIA",
            fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen
        )
        Text(
            if (isAddingChild) "Cuéntanos sobre tu nuevo pequeño/a" else "Tu asistente de nutrición infantil",
            fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
        )
        if (currentStep > 1) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier              = Modifier.semantics { invisibleToUser() },
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(totalSteps - 2) { i ->
                    val filled    = (i + 2) <= currentStep
                    val isCurrent = (i + 2) == currentStep
                    val width by animateDpAsState(if (isCurrent) 24.dp else 8.dp, spring(Spring.DampingRatioMediumBouncy), label = "dot$i")
                    val color by animateColorAsState(if (filled) NutriaGreen else Color.LightGray.copy(0.5f), tween(200), label = "dotC$i")
                    Box(Modifier.height(6.dp).width(width).clip(CircleShape).background(color))
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STEP 0 — ACCESIBILIDAD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StepAccesibilidad(
    selected:       AccessibilityMode,
    idiomaActual:   IdiomaVoz,
    talkBackActivo: Boolean,
    onSelect:       (AccessibilityMode) -> Unit,
    onIdiomaSelect: (IdiomaVoz) -> Unit
) {
    QuizStepLayout(Icons.Rounded.Accessibility, NutriaGreen, "¿Cómo usas la app?", "Adaptamos Nutri/IA a tus necesidades") {
        AnimatedVisibility(visible = talkBackActivo) {
            Column {
                Surface(color = NutriaGreen.copy(alpha = 0.10f), shape = RoundedCornerShape(12.dp)) {
                    Row(Modifier.padding(12.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CheckCircle, null, tint = NutriaGreen, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("TalkBack detectado — modo ciego activado.", fontSize = 12.sp, color = NutriaGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
        accessibilityOptions().forEach { option ->
            val isSelected = selected == option.mode
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, if (isSelected) NutriaGreen else Color.LightGray.copy(0.4f), RoundedCornerShape(16.dp))
                    .background(if (isSelected) NutriaGreen.copy(0.06f) else Color.White)
                    .clickable(onClickLabel = "Seleccionar ${option.mode.label}") { onSelect(option.mode) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "${option.mode.label}. ${option.mode.description}." + if (isSelected) " Seleccionado." else " Toca para seleccionar."
                        this.selected = isSelected
                    }
                    .padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(option.color.copy(0.12f)).semantics { contentDescription = "" }, Alignment.Center) {
                        Icon(option.icon, null, tint = if (isSelected) option.color else Color.Gray, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(option.mode.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (isSelected) NutriaGreen else NutriaDarkGreen)
                        Text(option.mode.description, fontSize = 11.sp, color = Color.Gray)
                    }
                }
                if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = NutriaGreen, modifier = Modifier.size(22.dp).semantics { contentDescription = "" })
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("Idioma de la voz", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Spacer(Modifier.height(8.dp))
        IdiomaVoz.entries.forEach { idioma ->
            val isSelected = idioma == idiomaActual
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, if (isSelected) NutriaGreen else Color.LightGray.copy(0.4f), RoundedCornerShape(12.dp))
                    .background(if (isSelected) NutriaGreen.copy(0.05f) else Color.White)
                    .clickable(onClickLabel = "Seleccionar idioma ${idioma.label}") { onIdiomaSelect(idioma) }
                    .semantics(mergeDescendants = true) {
                        contentDescription = "Idioma ${idioma.label}. ${idioma.descripcion}. " + if (isSelected) "Activo." else "Toca para seleccionar."
                    }
                    .padding(12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Language, null, tint = if (isSelected) NutriaGreen else Color.Gray, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(idioma.label, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) NutriaGreen else NutriaDarkGreen)
                        Text(idioma.descripcion, fontSize = 10.sp, color = Color.Gray)
                    }
                }
                if (isSelected) Icon(Icons.Rounded.CheckCircle, null, tint = NutriaGreen, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(color = NutriaGreen.copy(0.05f), shape = RoundedCornerShape(12.dp)) {
            Row(Modifier.padding(12.dp).semantics(mergeDescendants = true) {}, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, null, tint = NutriaGreen, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Puedes cambiar estas opciones en Ajustes.", fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp)
            }
        }
    }
}

private data class A11yOption(val mode: AccessibilityMode, val icon: ImageVector, val color: Color)
private fun accessibilityOptions() = listOf(
    A11yOption(AccessibilityMode.NORMAL, Icons.Rounded.CheckCircle,            Color(0xFF4CAF50)),
    A11yOption(AccessibilityMode.BLIND,  Icons.Rounded.RemoveRedEye,           Color(0xFF9C8FE0)),
    A11yOption(AccessibilityMode.MUTE,   Icons.AutoMirrored.Rounded.VolumeOff, Color(0xFF4DB6AC))
)

// ═══════════════════════════════════════════════════════════════════════════════
// STEP 1 — BIENVENIDA
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StepBienvenida(isAddingChild: Boolean, modo: AccessibilityMode = AccessibilityMode.NORMAL) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(120.dp).background(NutriaGreen.copy(0.08f), CircleShape).semantics { contentDescription = "" }, Alignment.Center) {
            Icon(Icons.Rounded.ChildCare, null, modifier = Modifier.size(64.dp), tint = NutriaGreen)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            if (isAddingChild) "¡Un nuevo integrante!" else "Conoce a tu pequeño/a",
            fontSize = if (modo == AccessibilityMode.BLIND) 28.sp else 26.sp,
            fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Vamos a registrar los datos de tu hijo/a para personalizar su seguimiento nutricional con IA.",
            fontSize = if (modo == AccessibilityMode.BLIND) 17.sp else 15.sp,
            color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 22.sp
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { invisibleToUser() }) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { InfoChip("Nombre", Icons.Rounded.Badge); InfoChip("Edad", Icons.Rounded.Cake) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { InfoChip("Medidas", Icons.Rounded.Straighten); InfoChip("Salud", Icons.Rounded.HealthAndSafety) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { InfoChip("Ingreso", Icons.Rounded.Payments); InfoChip("Región", Icons.Rounded.LocationOn) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STEP 2 — NOMBRE + SEXO (slider deslizante Niño ↔ Niña)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StepNombre(
    value:                String,
    onValueChange:        (String) -> Unit,
    sexo:                 Sexo?             = null,
    onSexoChange:         (Sexo?) -> Unit   = {},
    modo:                 AccessibilityMode = AccessibilityMode.NORMAL,
    idioma:               IdiomaVoz         = IdiomaVoz.ESPANOL_MX,
    ttsManager:           NutriTTS?         = null,
    descripcionVozNombre: String            = Voz.QUIZ_NOMBRE
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (modo == AccessibilityMode.MUTE) focusRequester.requestFocus() }

    var sexoActivo by remember { mutableStateOf(false) }
    LaunchedEffect(value) {
        if ((modo == AccessibilityMode.BLIND || modo == AccessibilityMode.MUTE) && value.isNotBlank() && !sexoActivo) {
            delay(2000L)
            if (value.isNotBlank() && !sexoActivo) {
                sexoActivo = true
            }
        }
    }

    QuizStepLayout(Icons.Rounded.Face, NutriaGreen, "¿Cómo se llama?", "Nombre y sexo de tu hijo/a") {
        if (modo == AccessibilityMode.BLIND || modo == AccessibilityMode.MUTE) {
            val descripcion = if (value.isNotBlank())
                if (idioma == IdiomaVoz.INGLES) "Your child's name is $value. If it's correct, tap Continue."
                else "El nombre de tu hijo es $value. Si es correcto toca Continuar."
            else descripcionVozNombre
            CampoTextoAccesible(
                valor          = value, onValorChange = onValueChange,
                etiqueta       = "Nombre", descripcionVoz = descripcion,
                placeholder    = "Ej. Sofía, Mateo...", ttsManager = if (!sexoActivo) ttsManager else null,
                idioma         = idioma, colorPrimario = NutriaGreen,
                activo         = !sexoActivo,
                onFocus        = { sexoActivo = false },
                onNext         = { sexoActivo = true }
            )
            Spacer(Modifier.height(20.dp))
            val descVozSexo = if (idioma == IdiomaVoz.INGLES) "Is your child a boy or a girl? Say boy or girl." else "¿Es niño o niña? Di niño o niña."
            CampoTextoAccesible(
                valor          = when(sexo) { Sexo.NINO -> if (idioma == IdiomaVoz.INGLES) "Boy" else "Niño"; Sexo.NINA -> if (idioma == IdiomaVoz.INGLES) "Girl" else "Niña"; null -> "" },
                onValorChange  = { txt ->
                    val normalized = txt.lowercase()
                    if (normalized.contains("niño") || normalized.contains("boy") || normalized.contains("varon") || normalized.contains("varón") || normalized.contains("masculino") || normalized.contains("nińo")) {
                        onSexoChange(Sexo.NINO)
                    } else if (normalized.contains("niña") || normalized.contains("girl") || normalized.contains("femenino")) {
                        onSexoChange(Sexo.NINA)
                    }
                },
                etiqueta       = "Sexo",
                descripcionVoz = descVozSexo,
                placeholder    = if (idioma == IdiomaVoz.INGLES) "Boy, Girl..." else "Ej. Niño, Niña...",
                ttsManager     = if (sexoActivo) ttsManager else null,
                idioma         = idioma,
                colorPrimario  = NutriaGreen,
                activo         = sexoActivo,
                onFocus        = { sexoActivo = true }
            )
        } else {
            OutlinedTextField(
                value           = value, onValueChange = onValueChange,
                placeholder     = { Text("Ej. Sofía, Mateo...") },
                modifier        = Modifier.fillMaxWidth().focusRequester(focusRequester).semantics { contentDescription = "Campo nombre del niño o niña" },
                shape           = RoundedCornerShape(16.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                colors          = OutlinedTextFieldDefaults.colors(focusedBorderColor = NutriaGreen, unfocusedBorderColor = Color.LightGray),
                leadingIcon     = { Icon(Icons.Rounded.Person, null, tint = NutriaGreen) }
            )
            Spacer(Modifier.height(20.dp))
            SexoSlider(sexo = sexo, onSexoChange = onSexoChange)
        }
    }
}

@Composable
private fun SexoSlider(sexo: Sexo?, onSexoChange: (Sexo?) -> Unit) {
    val ninoColor = Color(0xFF1565C0)
    val ninaColor = QuizAccent

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Wc, null, tint = NutriaGreen, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sexo", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
            if (sexo != null) {
                Spacer(Modifier.width(8.dp))
                AnimatedContent(
                    targetState = sexo,
                    transitionSpec = { fadeIn(tween(200)).togetherWith(fadeOut(tween(150))) },
                    label = "sexoLabel"
                ) { s ->
                    Text(
                        text      = if (s == Sexo.NINO) "· Niño" else "· Niña",
                        fontSize  = 14.sp,
                        color     = if (s == Sexo.NINO) ninoColor else ninaColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Botón Niño
            val ninoSelected = sexo == Sexo.NINO
            val ninoBg by animateColorAsState(
                targetValue   = if (ninoSelected) ninoColor else Color(0xFFF0F4FF),
                animationSpec = tween(200), label = "ninoBg"
            )
            val ninoIconTint by animateColorAsState(
                targetValue   = if (ninoSelected) Color.White else ninoColor,
                animationSpec = tween(200), label = "ninoTint"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ninoBg)
                    .border(
                        width = if (ninoSelected) 0.dp else 1.5.dp,
                        color = if (ninoSelected) Color.Transparent else ninoColor.copy(0.4f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(onClickLabel = "Seleccionar Niño") { onSexoChange(Sexo.NINO) }
                    .semantics { contentDescription = "Botón Niño. ${if (ninoSelected) "Seleccionado." else "Toca para elegir Niño."}" }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Boy, null, tint = ninoIconTint, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Niño", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ninoIconTint)
                }
            }

            // Botón Niña
            val ninaSelected = sexo == Sexo.NINA
            val ninaBg by animateColorAsState(
                targetValue   = if (ninaSelected) ninaColor else Color(0xFFFFF0F6),
                animationSpec = tween(200), label = "ninaBg"
            )
            val ninaIconTint by animateColorAsState(
                targetValue   = if (ninaSelected) Color.White else ninaColor,
                animationSpec = tween(200), label = "ninaTint"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(ninaBg)
                    .border(
                        width = if (ninaSelected) 0.dp else 1.5.dp,
                        color = if (ninaSelected) Color.Transparent else ninaColor.copy(0.4f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable(onClickLabel = "Seleccionar Niña") { onSexoChange(Sexo.NINA) }
                    .semantics { contentDescription = "Botón Niña. ${if (ninaSelected) "Seleccionada." else "Toca para elegir Niña."}" }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Girl, null, tint = ninaIconTint, modifier = Modifier.size(34.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Niña", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = ninaIconTint)
                }
            }
        }

        // Chip confirmación animado
        AnimatedVisibility(
            visible = sexo != null,
            enter   = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
            exit    = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(180))
        ) {
            val color = if (sexo == Sexo.NINO) ninoColor else ninaColor
            Row(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(color.copy(0.10f))
                    .border(1.dp, color.copy(0.3f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = color, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text       = if (sexo == Sexo.NINO) "Niño seleccionado" else "Niña seleccionada",
                    fontSize   = 12.sp,
                    color      = color,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STEP 3 — FECHA DE NACIMIENTO  (modo dual: calendario + input manual)
// El valor siempre viaja como String "DD/MM/YYYY"
// ═══════════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepFechaNacimiento(
    value:               String,
    onValueChange:       (String) -> Unit,
    modo:                AccessibilityMode = AccessibilityMode.NORMAL,
    idioma:              IdiomaVoz         = IdiomaVoz.ESPANOL_MX,
    ttsManager:          NutriTTS?         = null,
    descripcionVozFecha: String            = Voz.QUIZ_FECHA
) {
    QuizStepLayout(Icons.Rounded.Event, QuizAccent, "¿Cuándo nació?", "Fecha de nacimiento") {
        if (modo == AccessibilityMode.BLIND || modo == AccessibilityMode.MUTE) {
            CampoTextoAccesible(
                valor          = value,
                onValorChange  = { onValueChange(parsearFechaVoz(it)) },
                etiqueta       = "Fecha de nacimiento (DD/MM/AAAA)",
                descripcionVoz = descripcionVozFecha,
                placeholder    = "DD/MM/AAAA",
                ttsManager     = ttsManager,
                idioma         = idioma,
                esCampoFecha   = true,
                colorPrimario  = QuizAccent
            )
        } else {
            DatePickerDualMode(
                value         = value,
                onValueChange = onValueChange
            )
        }
    }
}

// ── DatePicker modo dual: calendario ↔ campo de texto manual ──────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDualMode(value: String, onValueChange: (String) -> Unit) {

    // Modo activo: true = calendario (DatePicker), false = texto manual
    var modoCalendario by remember { mutableStateOf(true) }

    // FocusRequester propio — se pide foco cuando el usuario cambia al modo texto
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(modoCalendario) {
        if (!modoCalendario) runCatching { focusRequester.requestFocus() }
    }

    val currentYear = 2026
    val initialMillis = remember(value) { dateStringToMillis(value) }

    // Límites: solo niños de 0 a 12 años cumplidos
    val milisMinimo = remember { com.example.nutriia.platform.currentTimeMillis() - 12L * 365 * 24 * 3600 * 1000 }
    val milisMaximo = remember { com.example.nutriia.platform.currentTimeMillis() }

    // Estado del DatePicker de M3 — siempre DisplayMode.Picker (calendario visual)
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialMillis,
        initialDisplayMode        = DisplayMode.Picker,
        yearRange                 = (currentYear - 12)..currentYear,
        selectableDates           = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                utcTimeMillis in milisMinimo..milisMaximo
            override fun isSelectableYear(year: Int) =
                year in (currentYear - 12)..currentYear
        }
    )

    // Estado del campo manual de texto
    var textoManual by remember { mutableStateOf(if (value.length == 10) value else "") }
    var errorManual by remember { mutableStateOf(false) }

    // Sincroniza el DatePicker → string cuando cambia la selección en calendario
    LaunchedEffect(datePickerState.selectedDateMillis, modoCalendario) {
        if (modoCalendario) {
            datePickerState.selectedDateMillis?.let {
                val nuevo = millisToDateString(it)
                if (nuevo != value) onValueChange(nuevo)
                textoManual = nuevo          // mantiene el campo manual sincronizado
            }
        }
    }

    val hasDate      = value.length == 10
    val readableDate = remember(value) { dateStringToReadable(value) }

    // ── Chip resumen de fecha seleccionada ────────────────────────────────────
    AnimatedVisibility(
        visible = hasDate,
        enter   = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
        exit    = shrinkVertically() + fadeOut(tween(150))
    ) {
        Surface(
            color  = QuizAccent.copy(0.09f),
            shape  = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, QuizAccent.copy(0.35f)),
            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.CheckCircle, null, tint = QuizAccent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Fecha seleccionada", fontSize = 11.sp, color = QuizAccent.copy(0.8f))
                    Text(
                        readableDate ?: value,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = QuizAccent
                    )
                }
            }
        }
    }

    // ── Toggle calendario / texto ─────────────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.LightGray.copy(0.15f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Botón Calendario
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (modoCalendario) Color.White else Color.Transparent)
                .clickable(onClickLabel = "Usar calendario") { modoCalendario = true }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.CalendarMonth, null,
                    tint     = if (modoCalendario) QuizAccent else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Calendario",
                    fontSize   = 13.sp,
                    color      = if (modoCalendario) QuizAccent else Color.Gray,
                    fontWeight = if (modoCalendario) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        // Botón Texto
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (!modoCalendario) Color.White else Color.Transparent)
                .clickable(onClickLabel = "Escribir fecha") { modoCalendario = false }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Edit, null,
                    tint     = if (!modoCalendario) QuizAccent else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    "Escribir",
                    fontSize   = 13.sp,
                    color      = if (!modoCalendario) QuizAccent else Color.Gray,
                    fontWeight = if (!modoCalendario) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    // ── Contenido según modo ──────────────────────────────────────────────────
    AnimatedContent(
        targetState  = modoCalendario,
        transitionSpec = {
            if (targetState) {
                (slideInHorizontally(tween(220)) { -it } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(200)) { it } + fadeOut(tween(180)))
            } else {
                (slideInHorizontally(tween(220)) { it } + fadeIn(tween(220)))
                    .togetherWith(slideOutHorizontally(tween(200)) { -it } + fadeOut(tween(180)))
            }
        },
        label = "dateMode"
    ) { esCalendario ->
        if (esCalendario) {
            // ── Modo CALENDARIO ───────────────────────────────────────────────
            // DatePicker embebido (sin diálogo) con colores de la app
            DatePicker(
                state          = datePickerState,
                showModeToggle = false,
                modifier       = Modifier.fillMaxWidth(),
                colors         = DatePickerDefaults.colors(
                    selectedDayContainerColor  = QuizAccent,
                    todayDateBorderColor       = QuizAccent,
                    selectedYearContainerColor = QuizAccent
                )
            )
        } else {
            // ── Modo TEXTO MANUAL ─────────────────────────────────────────────
            // errorManual: 0=sin error, 1=formato inválido, 2=mayor de 12 años, 3=fecha futura
            var errorTipo by remember { mutableIntStateOf(0) }
            Column {
                OutlinedTextField(
                    value           = textoManual,
                    onValueChange   = { input ->
                        val filtrado = input.filter { it.isDigit() || it == '/' }.take(10)
                        errorManual  = false
                        errorTipo    = 0
                        val soloDigitos = filtrado.filter { it.isDigit() }
                        val autoFormato = when {
                            soloDigitos.length >= 5 ->
                                "${soloDigitos.substring(0,2)}/${soloDigitos.substring(2,4)}/${soloDigitos.substring(4).take(4)}"
                            soloDigitos.length >= 3 ->
                                "${soloDigitos.substring(0,2)}/${soloDigitos.substring(2)}"
                            else -> soloDigitos
                        }
                        textoManual = autoFormato
                        if (autoFormato.length == 10) {
                            when {
                                esFechaValida(autoFormato) -> {
                                    onValueChange(autoFormato)
                                    dateStringToMillis(autoFormato)?.let { datePickerState.selectedDateMillis = it }
                                }
                                edadSuperaLimite(autoFormato) -> {
                                    errorManual = true; errorTipo = 2
                                }
                                else -> { errorManual = true; errorTipo = 1 }
                            }
                        }
                    },
                    label           = { Text("Fecha de nacimiento") },
                    placeholder     = { Text("DD/MM/AAAA") },
                    isError         = errorManual,
                    supportingText  = if (errorManual) {
                        {
                            Text(
                                when (errorTipo) {
                                    2    -> "Solo se pueden registrar niños de 0 a 12 años"
                                    3    -> "La fecha no puede ser futura"
                                    else -> "Fecha inválida. Usa el formato DD/MM/AAAA"
                                },
                                color    = MaterialTheme.colorScheme.error,
                                fontSize = 12.sp
                            )
                        }
                    } else null,
                    modifier        = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .semantics { contentDescription = "Campo fecha de nacimiento en formato día mes año" },
                    shape           = RoundedCornerShape(16.dp),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction    = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (!esFechaValida(textoManual)) errorManual = true
                        }
                    ),
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = QuizAccent,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor    = QuizAccent
                    ),
                    leadingIcon = {
                        Icon(Icons.Rounded.EditCalendar, null, tint = QuizAccent)
                    },
                    trailingIcon = if (esFechaValida(textoManual)) {
                        { Icon(Icons.Rounded.CheckCircle, null, tint = QuizAccent) }
                    } else null
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Escribe la fecha como: 15/03/2022",
                    fontSize = 12.sp,
                    color    = Color.Gray,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }

    // ── Nota informativa (siempre visible) ───────────────────────────────────
    Spacer(Modifier.height(14.dp))
    Surface(color = NutriaGreen.copy(0.05f), shape = RoundedCornerShape(12.dp)) {
        Row(
            Modifier.padding(12.dp).semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Info, null, tint = NutriaGreen, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                "La usamos para calcular la etapa de desarrollo.",
                fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STEP 4 — MEDIDAS  (sin cambios)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StepMedidas(
    weight:              String,
    height:              String,
    onWeightChange:      (String) -> Unit,
    onHeightChange:      (String) -> Unit,
    modo:                AccessibilityMode = AccessibilityMode.NORMAL,
    idioma:              IdiomaVoz         = IdiomaVoz.ESPANOL_MX,
    ttsManager:          NutriTTS?         = null,
    descripcionVozPeso:  String            = "el peso en kilogramos, por ejemplo 7 punto 5",
    descripcionVozTalla: String            = "la talla en centímetros, por ejemplo 68"
) {
    val focusPeso  = remember { FocusRequester() }
    val focusTalla = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (modo == AccessibilityMode.MUTE) focusPeso.requestFocus() }

    var campoMedidaActivo by remember { mutableIntStateOf(0) }
    LaunchedEffect(weight) {
        if ((modo == AccessibilityMode.BLIND || modo == AccessibilityMode.MUTE) && weight.isNotBlank() && campoMedidaActivo == 0) {
            kotlinx.coroutines.delay(600L)
            if (weight.isNotBlank() && campoMedidaActivo == 0) campoMedidaActivo = 1
        }
    }

    QuizStepLayout(Icons.Rounded.MonitorWeight, Color(0xFF7E57C2), "Peso y talla actual", "Para calcular su curva de crecimiento") {
        if (modo == AccessibilityMode.BLIND || modo == AccessibilityMode.MUTE) {
            CampoTextoAccesible(
                valor = weight, onValorChange = onWeightChange, etiqueta = "Peso", descripcionVoz = descripcionVozPeso, placeholder = "Ej. 7.5",
                ttsManager = if (campoMedidaActivo == 0) ttsManager else null, idioma = idioma, colorPrimario = Color(0xFF7E57C2),
                activo = campoMedidaActivo == 0,
                onFocus = { campoMedidaActivo = 0 },
                onNext = { campoMedidaActivo = 1 }
            )
            Spacer(Modifier.height(16.dp))
            CampoTextoAccesible(
                valor = height, onValorChange = onHeightChange, etiqueta = "Talla", descripcionVoz = descripcionVozTalla, placeholder = "Ej. 68",
                ttsManager = if (campoMedidaActivo == 1) ttsManager else null, idioma = idioma, colorPrimario = Color(0xFF7E57C2),
                activo = campoMedidaActivo == 1,
                onFocus = { campoMedidaActivo = 1 }
            )
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = weight, onValueChange = onWeightChange, placeholder = { Text("Ej. 7.5") }, label = { Text("Peso (kg)") },
                    modifier = Modifier.weight(1f).focusRequester(focusPeso).semantics { contentDescription = "Campo peso en kilogramos" },
                    shape = RoundedCornerShape(16.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusTalla.requestFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NutriaGreen, unfocusedBorderColor = Color.LightGray),
                    leadingIcon = { Icon(Icons.Rounded.Scale, null, tint = NutriaGreen) }
                )
                OutlinedTextField(
                    value = height, onValueChange = onHeightChange, placeholder = { Text("Ej. 68") }, label = { Text("Talla (cm)") },
                    modifier = Modifier.weight(1f).focusRequester(focusTalla).semantics { contentDescription = "Campo talla en centímetros" },
                    shape = RoundedCornerShape(16.dp), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NutriaGreen, unfocusedBorderColor = Color.LightGray),
                    leadingIcon = { Icon(Icons.Rounded.Straighten, null, tint = NutriaGreen) }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Si no tienes los datos exactos, puedes actualizarlos después.", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STEP 5 — CONDICIONES  (sin cambios)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StepCondiciones(
    hasAllergies:            Boolean,
    allergiesDetail:         String,
    hasConditions:           Boolean,
    conditionsDetail:        String,
    onAllergiesToggle:       (Boolean) -> Unit,
    onAllergiesDetailChange: (String) -> Unit,
    onConditionsToggle:      (Boolean) -> Unit,
    onConditionsDetailChange:(String) -> Unit
) {
    val alergenosReconocidos = remember(allergiesDetail) {
        if (allergiesDetail.isNotBlank()) parsearAlergenos(allergiesDetail) else emptyList()
    }
    QuizStepLayout(Icons.Rounded.MedicalServices, Color(0xFFFFB300), "Salud especial", "Alergias o condiciones a considerar") {
        ToggleOptionCard("¿Tiene alergias alimentarias?", Icons.Rounded.BakeryDining, hasAllergies, onAllergiesToggle)
        AnimatedVisibility(visible = hasAllergies, enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(250)), exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(200))) {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = allergiesDetail, onValueChange = onAllergiesDetailChange,
                    placeholder = { Text("Ej. leche, huevo, maní, trigo...") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Describe las alergias" },
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFB300), unfocusedBorderColor = Color.LightGray, focusedLabelColor = Color(0xFFFFB300))
                )
                AnimatedVisibility(visible = alergenosReconocidos.isNotEmpty()) {
                    Column {
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFFFF3E0)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFFE65100), modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Alérgenos reconocidos:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        }
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { items(alergenosReconocidos) { AlergenoChip(it) } }
                        Spacer(Modifier.height(4.dp))
                        Text("Estos quedarán registrados en el perfil de tu hijo/a", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
                    }
                }
                AnimatedVisibility(visible = allergiesDetail.isNotBlank() && alergenosReconocidos.isEmpty()) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.LightGray.copy(0.15f)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Info, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("No se reconoció el alérgeno. Se guardará el texto tal como lo escribiste.", fontSize = 10.sp, color = Color.Gray, lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ToggleOptionCard("¿Tiene alguna condición especial?", Icons.Rounded.AssignmentLate, hasConditions, onConditionsToggle)
        AnimatedVisibility(visible = hasConditions, enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(250)), exit = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(200))) {
            Column {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = conditionsDetail, onValueChange = onConditionsDetailChange,
                    placeholder = { Text("Ej. intolerancia a la lactosa, reflujo, bajo peso...") },
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Describe la condición" },
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Rounded.MedicalServices, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp)) },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFB300), unfocusedBorderColor = Color.LightGray)
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// STEP 6 — INGRESO FAMILIAR + REGIÓN  (sin cambios)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun StepIngresoRegion(
    nivelSeleccionado:  NivelIngreso,
    regionSeleccionada: RegionMexico,
    onNivelChange:      (NivelIngreso) -> Unit,
    onRegionChange:     (RegionMexico) -> Unit
) {
    val nivelIconos = mapOf(
        NivelIngreso.BASICO     to Icons.Rounded.AccountBalanceWallet,
        NivelIngreso.MEDIO_BAJO to Icons.Rounded.Savings,
        NivelIngreso.MEDIO      to Icons.Rounded.TrendingUp,
        NivelIngreso.ALTO       to Icons.Rounded.WorkspacePremium
    )
    val regionesVisibles = listOf(RegionMexico.NORTE, RegionMexico.CENTRO, RegionMexico.SUR)
    val regionIconos = mapOf(
        RegionMexico.NORTE  to Icons.Rounded.KeyboardArrowUp,
        RegionMexico.CENTRO to Icons.Rounded.FiberManualRecord,
        RegionMexico.SUR    to Icons.Rounded.KeyboardArrowDown
    )
    QuizStepLayout(Icons.Rounded.FamilyRestroom, NutriaGreen, "Familia y región", "Adaptamos el menú a tu presupuesto") {
        IQSectionHeader("Ingreso familiar mensual", Icons.Rounded.Payments)
        Spacer(Modifier.height(10.dp))
        NivelIngreso.entries.forEach { nivel ->
            IQNivelCard(nivel = nivel, selected = nivel == nivelSeleccionado, icono = nivelIconos[nivel] ?: Icons.Rounded.Payments, onClick = { onNivelChange(nivel) })
            Spacer(Modifier.height(8.dp))
        }
        IQFuenteNota("Rangos basados en el salario mínimo 2026: \$9,582/mes · CONASAMI (DOF 09/12/2025)")
        Spacer(Modifier.height(28.dp))
        IQSectionHeader("Región de México", Icons.Rounded.LocationOn)
        Spacer(Modifier.height(10.dp))
        regionesVisibles.forEach { reg ->
            IQRegionCard(region = reg, selected = reg == regionSeleccionada, icono = regionIconos[reg] ?: Icons.Rounded.FiberManualRecord, onClick = { onRegionChange(reg) })
            Spacer(Modifier.height(8.dp))
        }
        IQFuenteNota("Las recetas se adaptan a ingredientes típicos de tu zona · ENIGH 2024 (INEGI)")
        Spacer(Modifier.height(8.dp))
    }
}

// ── Sub-componentes del Step 6 ────────────────────────────────────────────────

@Composable
private fun IQSectionHeader(titulo: String, icono: ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(NutriaGreen.copy(0.10f)), Alignment.Center) {
            Icon(icono, null, tint = NutriaGreen, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen, letterSpacing = (-0.2).sp)
    }
}

@Composable
private fun IQNivelCard(nivel: NivelIngreso, selected: Boolean, icono: ImageVector, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (selected) NutriaGreen else IQ_Divider, tween(200), "nb")
    val bgColor     by animateColorAsState(if (selected) NutriaGreen.copy(0.07f) else IQ_CardBg, tween(200), "nbg")
    val iconTint    by animateColorAsState(if (selected) NutriaGreen else IQ_TextMid, tween(200), "ni")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor).clickable(onClickLabel = "Seleccionar ${nivel.label}") { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = "${nivel.label}. ${nivel.rangoLabel}. Presupuesto niño: ${nivel.presupuestoNinoMensual} pesos al mes. " + if (selected) "Seleccionado." else "Toca para seleccionar."
                this.selected = selected
            }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (selected) NutriaGreen.copy(0.12f) else IQ_BgSoft), Alignment.Center) {
            Icon(icono, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(nivel.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) NutriaGreen else IQ_TextDark)
            Text(nivel.rangoLabel, fontSize = 11.sp, color = IQ_TextMid, lineHeight = 15.sp)
            Spacer(Modifier.height(5.dp))
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(if (selected) NutriaGreen.copy(0.10f) else IQ_BgSoft).padding(horizontal = 8.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChildCare, null, tint = if (selected) NutriaGreen else IQ_TextMid, modifier = Modifier.size(11.dp))
                Spacer(Modifier.width(4.dp))
                Text("Niño/a: ~\$${nivel.presupuestoNinoMensual}/mes", fontSize = 10.sp, color = if (selected) NutriaGreen else IQ_TextMid, fontWeight = FontWeight.SemiBold)
            }
        }
        AnimatedVisibility(visible = selected, enter = scaleIn(tween(180)) + fadeIn(tween(180)), exit = scaleOut(tween(150)) + fadeOut(tween(150))) {
            Icon(Icons.Rounded.CheckCircle, null, tint = NutriaGreen, modifier = Modifier.size(22.dp))
        }
        if (!selected) Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, IQ_Divider, CircleShape))
    }
}

@Composable
private fun IQRegionCard(region: RegionMexico, selected: Boolean, icono: ImageVector, onClick: () -> Unit) {
    val borderColor by animateColorAsState(if (selected) NutriaGreen else IQ_Divider, tween(200), "rb")
    val bgColor     by animateColorAsState(if (selected) NutriaGreen.copy(0.07f) else IQ_CardBg, tween(200), "rbg")
    val iconTint    by animateColorAsState(if (selected) NutriaGreen else IQ_TextMid, tween(200), "ri")
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor).clickable(onClickLabel = "Seleccionar región ${region.label}") { onClick() }
            .semantics(mergeDescendants = true) {
                contentDescription = "Región ${region.label}. ${region.estados}. " + if (selected) "Seleccionada." else "Toca para seleccionar."
                this.selected = selected
            }.padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(if (selected) NutriaGreen.copy(0.12f) else IQ_BgSoft), Alignment.Center) {
            Icon(icono, null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(region.label, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = if (selected) NutriaGreen else IQ_TextDark)
            Text(region.estados, fontSize = 11.sp, color = IQ_TextMid, lineHeight = 15.sp)
        }
        AnimatedVisibility(visible = selected, enter = scaleIn(tween(180)) + fadeIn(tween(180)), exit = scaleOut(tween(150)) + fadeOut(tween(150))) {
            Icon(Icons.Rounded.CheckCircle, null, tint = NutriaGreen, modifier = Modifier.size(22.dp))
        }
        if (!selected) Box(Modifier.size(22.dp).clip(CircleShape).border(1.5.dp, IQ_Divider, CircleShape))
    }
}

@Composable
private fun IQFuenteNota(texto: String) {
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(NutriaGreen.copy(0.05f)).padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Info, null, tint = NutriaGreen, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text(texto, fontSize = 10.sp, color = IQ_TextMid, lineHeight = 14.sp)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// COMPONENTES COMPARTIDOS  (sin cambios)
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AlergenoChip(alergeno: Alergeno) {
    val color = colorAlergeno(alergeno)
    Row(
        Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(0.12f)).border(1.dp, color.copy(0.4f), RoundedCornerShape(20.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Block, null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(alergeno.label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuizStepLayout(icon: ImageVector, iconColor: Color, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(iconColor.copy(0.12f)).semantics { contentDescription = "" }, Alignment.Center) {
                Icon(icon, null, tint = iconColor, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                Text(subtitle, fontSize = 13.sp, color = Color.Gray)
            }
        }
        Spacer(Modifier.height(28.dp))
        content()
    }
}

@Composable
fun ToggleOptionCard(label: String, icon: ImageVector, isSelected: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .border(1.5.dp, if (isSelected) NutriaGreen else Color.LightGray.copy(0.4f), RoundedCornerShape(16.dp))
            .background(if (isSelected) NutriaGreen.copy(0.04f) else Color.White)
            .clickable(onClickLabel = if (isSelected) "Desactivar $label" else "Activar $label") { onToggle(!isSelected) }
            .semantics(mergeDescendants = true) {
                contentDescription = "$label. ${if (isSelected) "Activado" else "Desactivado"}"
                toggleableState    = ToggleableState(isSelected)
            }.padding(16.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, null, tint = if (isSelected) NutriaGreen else Color.Gray, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NutriaDarkGreen)
        }
        Switch(
            checked = isSelected, onCheckedChange = onToggle,
            colors  = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NutriaGreen, uncheckedTrackColor = Color.LightGray.copy(0.3f))
        )
    }
}

@Composable
fun InfoChip(text: String, icon: ImageVector) {
    Row(
        Modifier.clip(RoundedCornerShape(12.dp)).background(NutriaGreen.copy(0.08f)).border(1.dp, NutriaGreen.copy(0.1f), RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = NutriaGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, color = NutriaDarkGreen, fontWeight = FontWeight.Bold)
    }
}