package com.example.nutriia.embarazo

import kotlinx.coroutines.delay
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.*
import com.example.nutriia.auth.*
import com.example.nutriia.auth.StepIngresoRegion

@Composable
fun EmbarazoQuizScreen(
    semanasIniciales: Int = 1,
    onQuizComplete: (PerfilEmbarazo) -> Unit,
    onCancel: () -> Unit
) {
        val haptic = LocalHapticFeedback.current
    val accessibilityVm: AccessibilityViewModel = viewModel()
    val idiomaActual by accessibilityVm.idioma.collectAsState()
    val modoGuardado by accessibilityVm.mode.collectAsState()

    var currentStep by remember { mutableIntStateOf(0) }
    val totalSteps = 8
    
    var perfil by remember { mutableStateOf(PerfilEmbarazo(semanas = semanasIniciales)) }
    
    var selectedA11yMode by remember(modoGuardado) {
        mutableStateOf(if (false) AccessibilityMode.BLIND else modoGuardado)
    }
    val ttsManager = accessibilityVm.ttsManager

    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    LaunchedEffect(currentStep, selectedA11yMode, idiomaActual) {
        if (selectedA11yMode != AccessibilityMode.BLIND) return@LaunchedEffect
        val texto = when (currentStep) {
            0 -> loc(Voz.ACCESIBILIDAD_INTRO + " " + Voz.IDIOMA_INTRO, VozEn.ACCESIBILIDAD_INTRO + " " + VozEn.IDIOMA_INTRO)
            1 -> loc("Tu embarazo, tu bienestar. Te acompañaremos en cada etapa de este camino.", "Your pregnancy, your well-being. We will accompany you in every stage of this journey.")
            2 -> loc("¿En qué semana estás? Di el número de semana, por ejemplo doce.", "Which week are you in? Say the week number, for example twelve.")
            3 -> loc("Datos médicos. Cuéntanos tu edad, estatura y peso anterior al embarazo.", "Medical details. Tell us your age, height, and pre-pregnancy weight.")
            4 -> loc("Condiciones de salud. Selecciona si tienes alguna condición especial.", "Health conditions. Select if you have any special condition.")
            5 -> loc("Preferencias alimenticias. Cuéntanos si sigues alguna dieta o restricción.", "Food preferences. Tell us if you follow any diet or restriction.")
            6 -> loc("Alergias. Escribe si tienes alguna alergia alimentaria.", "Allergies. Write if you have any food allergy.")
            7 -> loc("Nivel de ingreso y región. Selecciona tu nivel y región para estimar tu plan.", "Income level and region. Select your level and region to estimate your plan.")
            else -> ""
        }
        if (texto.isNotEmpty()) accessibilityVm.hablar(texto)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFF8F9F3))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (currentStep > 0) {
                    Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFF689F38).copy(0.1f))
                        .clickable { currentStep-- }, contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Color(0xFF689F38), modifier = Modifier.size(18.dp))
                    }
                } else Spacer(Modifier.width(38.dp))
                
                TextButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, null, tint = Color.Gray, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(loc("Cancelar", "Cancel"), color = Color.Gray, fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            QuizHeader(currentStep, totalSteps, false)
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
                    }, label = "quiz_anim"
                ) { step ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (step) {
                            0 -> StepAccesibilidad(
                                selected = selectedA11yMode,
                                idiomaActual = idiomaActual,
                                talkBackActivo = false,
                                onSelect = { modo ->
                                     selectedA11yMode = modo
                                     accessibilityVm.setMode(modo)
                                },
                                onIdiomaSelect = { accessibilityVm.setIdioma(it) }
                            )
                            1 -> StepBienvenidaEmbarazo(idiomaActual == IdiomaVoz.INGLES)
                            2 -> StepSemanaEmbarazo(
                                semanas = perfil.semanas,
                                onSemanasChange = { perfil = perfil.copy(semanas = it) },
                                modo = selectedA11yMode,
                                ttsManager = ttsManager,
                                isEnglish = idiomaActual == IdiomaVoz.INGLES
                            )
                            3 -> StepDatosMedicosEmbarazo(
                                edad = perfil.edad,
                                onEdadChange = { perfil = perfil.copy(edad = it) },
                                tallaM = perfil.tallaM,
                                onTallaMChange = { perfil = perfil.copy(tallaM = it) },
                                pesoPregestacionalKg = perfil.pesoPregestacionalKg,
                                onPesoPregestacionalKgChange = { perfil = perfil.copy(pesoPregestacionalKg = it) },
                                esGemelar = perfil.esGemelar,
                                onEsGemelarChange = { perfil = perfil.copy(esGemelar = it) },
                                modo = selectedA11yMode,
                                ttsManager = ttsManager,
                                isEnglish = idiomaActual == IdiomaVoz.INGLES
                            )
                            4 -> StepCondicionesEmbarazo(
                                seleccionadas = perfil.condiciones,
                                onSeleccionChange = { perfil = perfil.copy(condiciones = it) },
                                isEnglish = idiomaActual == IdiomaVoz.INGLES
                            )
                            5 -> StepPreferenciasEmbarazo(
                                seleccionadas = perfil.preferencias,
                                onSeleccionChange = { perfil = perfil.copy(preferencias = it) },
                                isEnglish = idiomaActual == IdiomaVoz.INGLES
                            )
                            6 -> StepAlergiasEmbarazo(
                                allergiesDetail = perfil.allergiesDetail,
                                onAllergiesDetailChange = { perfil = perfil.copy(allergiesDetail = it) },
                                modo = selectedA11yMode,
                                ttsManager = ttsManager,
                                isEnglish = idiomaActual == IdiomaVoz.INGLES
                            )
                            7 -> StepIngresoRegion(
                                nivelSeleccionado  = perfil.nivelIngreso,
                                regionSeleccionada = perfil.region,
                                onNivelChange      = { perfil = perfil.copy(nivelIngreso = it) },
                                onRegionChange     = { perfil = perfil.copy(region = it) }
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                    }
                }
            }

            Button(
                onClick = {
                    if (currentStep < totalSteps - 1) {
                        currentStep++
                    } else {
                        onQuizComplete(perfil)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(if (selectedA11yMode == AccessibilityMode.BLIND || selectedA11yMode == AccessibilityMode.MUTE) 70.dp else 56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF689F38))
            ) {
                Text(
                    text = if (currentStep == totalSteps - 1) loc("Comenzar seguimiento", "Start tracking") else loc("Continuar", "Continue"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (currentStep < totalSteps - 1) {
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun StepBienvenidaEmbarazo(isEnglish: Boolean) {
    fun loc(es: String, en: String) = if (isEnglish) en else es
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(Modifier.size(110.dp).background(Color(0xFFEC9BBF).copy(0.12f), CircleShape), Alignment.Center) {
            Icon(Icons.Rounded.Favorite, null, modifier = Modifier.size(56.dp), tint = Color(0xFFEC9BBF))
        }
        Spacer(Modifier.height(24.dp))
        Text(loc("Tu embarazo, tu bienestar", "Your pregnancy, your well-being"), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF33691E), textAlign = TextAlign.Center)
        Spacer(Modifier.height(12.dp))
        Text(loc("Te acompañaremos con recomendaciones personalizadas de nutrición y salud para cada etapa.", "We will accompany you with personalized nutrition and health recommendations for each stage."), fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center, lineHeight = 22.sp)
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { InfoChip(loc("Semana", "Week"), Icons.Rounded.CalendarToday); InfoChip(loc("Nutrición", "Nutrition"), Icons.Rounded.Restaurant) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { InfoChip(loc("Síntomas", "Symptoms"), Icons.Rounded.MoodBad); InfoChip(loc("Citas", "Appts"), Icons.Rounded.Event) }
        }
    }
}

@Composable
private fun StepSemanaEmbarazo(semanas: Int, onSemanasChange: (Int) -> Unit, modo: AccessibilityMode, ttsManager: NutriTTS?, isEnglish: Boolean) {
    fun loc(es: String, en: String) = if (isEnglish) en else es
    val trimestre = when { semanas <= 13 -> 1; semanas <= 26 -> 2; else -> 3 }
    val trimestreTexto = when(trimestre) {
        1 -> loc("Primer trimestre", "First trimester")
        2 -> loc("Segundo trimestre", "Second trimester")
        else -> loc("Tercer trimestre", "Third trimester")
    }

    QuizStepLayout(Icons.Rounded.ChildFriendly, Color(0xFFEC9BBF), loc("¿En qué semana estás?", "Which week are you in?"), loc("Esto nos permite ajustar tus planes", "This allows us to adjust your plans")) {
        if (modo == AccessibilityMode.BLIND || modo == AccessibilityMode.MUTE) {
            CampoTextoAccesible(
                valor = if(semanas==0) "" else semanas.toString(),
                onValorChange = { onSemanasChange(it.toIntOrNull() ?: 1) },
                etiqueta = loc("Semana", "Week"),
                descripcionVoz = loc("Di el número de semana, por ejemplo doce", "Say the week number, for example twelve"),
                placeholder = "1-40",
                ttsManager = ttsManager,
                colorPrimario = Color(0xFFEC9BBF)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(semanas.toString(), fontSize = 64.sp, fontWeight = FontWeight.Black, color = Color(0xFFEC9BBF))
                Surface(color = Color(0xFFEC9BBF).copy(0.1f), shape = RoundedCornerShape(8.dp)) {
                    Text(trimestreTexto, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), color = Color(0xFFEC9BBF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(32.dp))
                Slider(
                    value = semanas.toFloat(),
                    onValueChange = { onSemanasChange(it.toInt()) },
                    valueRange = 1f..40f,
                    steps = 38,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFFEC9BBF), activeTrackColor = Color(0xFFEC9BBF))
                )
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(getEmbDevelopment(semanas, isEnglish), fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), lineHeight = 20.sp)
    }
}

@Composable
private fun StepCondicionesEmbarazo(seleccionadas: List<String>, onSeleccionChange: (List<String>) -> Unit, isEnglish: Boolean) {
    fun loc(es: String, en: String) = if (isEnglish) en else es
    val ninguna = loc("Ninguna por ahora", "None for now")
    val opciones = listOf(
        loc("Náuseas o vómitos", "Nausea or vomiting"),
        loc("Diabetes gestacional", "Gestational diabetes"),
        loc("Hipertensión", "Hypertension"),
        loc("Anemia", "Anemia"),
        ninguna
    )

    QuizStepLayout(Icons.Rounded.MedicalServices, Color(0xFFFF8F00), loc("Condiciones de salud", "Health conditions"), loc("Selecciona si presentas alguna", "Select if you have any")) {
        opciones.forEach { opcion ->
            ToggleOptionCard(opcion, Icons.Rounded.Check, seleccionadas.contains(opcion)) { selected ->
                val nuevaLista = if (opcion == ninguna) {
                    if (selected) listOf(opcion) else emptyList()
                } else {
                    val temp = seleccionadas.toMutableList()
                    temp.remove(ninguna)
                    if (selected) { if(!temp.contains(opcion)) temp.add(opcion) } else temp.remove(opcion)
                    temp
                }
                onSeleccionChange(nuevaLista)
            }
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(16.dp))
        Surface(color = Color(0xFFFF8F00).copy(0.05f), shape = RoundedCornerShape(12.dp)) {
            Text(loc("Esta información personaliza tus recomendaciones nutricionales", "This information personalizes your nutritional recommendations"), modifier = Modifier.padding(12.dp), fontSize = 12.sp, color = Color.DarkGray, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun StepPreferenciasEmbarazo(seleccionadas: List<String>, onSeleccionChange: (List<String>) -> Unit, isEnglish: Boolean) {
    fun loc(es: String, en: String) = if (isEnglish) en else es
    val sinRestricciones = loc("Sin restricciones", "No restrictions")
    val opciones = listOf(
        sinRestricciones,
        loc("Vegetariana", "Vegetarian"),
        loc("Vegana", "Vegan"),
        loc("Sin gluten", "Gluten-free"),
        loc("Sin lactosa", "Lactose-free"),
        loc("Bajo en sodio", "Low sodium")
    )

    QuizStepLayout(Icons.Rounded.Restaurant, Color(0xFF4DB6AC), loc("Preferencias alimenticias", "Food preferences"), loc("Cuéntanos tus hábitos actuales", "Tell us your current habits")) {
        opciones.forEach { opcion ->
            ToggleOptionCard(opcion, Icons.Rounded.RestaurantMenu, seleccionadas.contains(opcion)) { selected ->
                val nuevaLista = if (opcion == sinRestricciones) {
                    if (selected) listOf(opcion) else emptyList()
                } else {
                    val temp = seleccionadas.toMutableList()
                    temp.remove(sinRestricciones)
                    if (selected) { if(!temp.contains(opcion)) temp.add(opcion) } else temp.remove(opcion)
                    temp
                }
                onSeleccionChange(nuevaLista)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun getEmbDevelopment(semana: Int, isEnglish: Boolean): String {
    return if (isEnglish) {
        when {
            semana <= 4 -> "Your baby is currently an embryo, smaller than a grain of rice."
            semana <= 8 -> "Major organs like the heart and brain are forming rapidly."
            semana <= 12 -> "The baby's skeletal system and muscles are developing."
            semana <= 20 -> "The baby can now hear your heartbeat and moving digits."
            semana <= 28 -> "Your baby's eyes are opening and can sense light."
            else -> "The baby is gaining weight and preparing for the big day."
        }
    } else {
        when {
            semana <= 4 -> "Tu bebé es actualmente un embrión, más pequeño que un grano de arroz."
            semana <= 8 -> "Los órganos principales como el corazón y el cerebro se forman rápido."
            semana <= 12 -> "El sistema óseo y los músculos del bebé se están desarrollando."
            semana <= 20 -> "El bebé ya puede escuchar tus latidos y mueve sus deditos."
            semana <= 28 -> "Los ojos de tu bebé se abren y puede percibir la luz."
            else -> "El bebé está ganando peso y preparándose para el gran día."
        }
    }
}

@Composable
private fun StepAlergiasEmbarazo(
    allergiesDetail: String,
    onAllergiesDetailChange: (String) -> Unit,
    modo: AccessibilityMode,
    ttsManager: NutriTTS?,
    isEnglish: Boolean
) {
    fun loc(es: String, en: String) = if (isEnglish) en else es
    com.example.nutriia.auth.QuizStepLayout(
        Icons.Rounded.Warning, Color(0xFFFFB300),
        loc("Tus alergias", "Your allergies"),
        loc("Para que el menú prenatal las evite", "So the prenatal menu avoids them")
    ) {
        if (modo == AccessibilityMode.BLIND || modo == AccessibilityMode.MUTE) {
            CampoTextoAccesible(
                valor = allergiesDetail,
                onValorChange = onAllergiesDetailChange,
                etiqueta = loc("Tus Alergias", "Your Allergies"),
                descripcionVoz = loc("Di tus alergias alimentarias separadas por comas, o deja vacío si no tienes", "Speak your food allergies separated by commas, or leave empty if none"),
                placeholder = loc("Ej. leche, mariscos", "E.g. milk, shellfish"),
                ttsManager = ttsManager,
                colorPrimario = Color(0xFFFFB300)
            )
        } else {
            OutlinedTextField(
                value = allergiesDetail,
                onValueChange = onAllergiesDetailChange,
                placeholder = { Text(loc("Ej. leche, mariscos, maní...", "E.g. milk, shellfish, peanuts...")) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFFB300), modifier = Modifier.size(20.dp)) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFFFB300), unfocusedBorderColor = Color.LightGray)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            loc("Si no tienes alergias, deja este campo vacío.", "If you have no allergies, leave this field empty."),
            fontSize = 12.sp, color = Color.Gray
        )
    }
}

@Composable
private fun StepDatosMedicosEmbarazo(
    edad: Int,
    onEdadChange: (Int) -> Unit,
    tallaM: Double,
    onTallaMChange: (Double) -> Unit,
    pesoPregestacionalKg: Double,
    onPesoPregestacionalKgChange: (Double) -> Unit,
    esGemelar: Boolean,
    onEsGemelarChange: (Boolean) -> Unit,
    modo: AccessibilityMode,
    ttsManager: NutriTTS?,
    isEnglish: Boolean
) {
    fun loc(es: String, en: String) = if (isEnglish) en else es
    var tallaText by remember { mutableStateOf(if (tallaM > 0.0) String.format(java.util.Locale.US, "%.2f", tallaM) else "") }
    var pesoText by remember { mutableStateOf(if (pesoPregestacionalKg > 0.0) String.format(java.util.Locale.US, "%.1f", pesoPregestacionalKg) else "") }
    var edadText by remember { mutableStateOf(if (edad > 0) edad.toString() else "") }

    val esBlind = modo == AccessibilityMode.BLIND
    val esMute = modo == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute


    val voiceManager = remember { if (esBlind) VoiceInputManager() else null }

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }

    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> edadText
            1 -> tallaText
            2 -> pesoText
            else -> ""
        }
    }

    LaunchedEffect(edadText) {
        if (!esBlind || edadText.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (edadText == valorInicial) return@LaunchedEffect
        delay(2000L)
        if (edadText.isNotBlank() && campoActivo == 0 && edadText != valorInicial) campoActivo = 1
    }
    LaunchedEffect(tallaText) {
        if (!esBlind || tallaText.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (tallaText == valorInicial) return@LaunchedEffect
        delay(2000L)
        if (tallaText.isNotBlank() && campoActivo == 1 && tallaText != valorInicial) campoActivo = 2
    }
    LaunchedEffect(pesoText) {
        if (!esBlind || pesoText.isBlank() || campoActivo != 2) return@LaunchedEffect
        if (pesoText == valorInicial) return@LaunchedEffect
        delay(2000L)
        if (pesoText.isNotBlank() && campoActivo == 2 && pesoText != valorInicial) campoActivo = 3
    }

    LaunchedEffect(campoActivo) {
        if (esBlind && campoActivo == 3) {
            ttsManager?.hablarYEsperar(
                loc(
                    "Todos los datos médicos completados. Di continuar para avanzar.",
                    "All medical details completed. Say continue to proceed."
                ),
                margenMs = 800L
            )
            voiceManager?.escuchar(if (isEnglish) IdiomaVoz.INGLES else IdiomaVoz.ESPANOL_MX, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                val cmd = result.lowercase(java.util.Locale.getDefault()).trim()
                if (cmd.contains("continuar") || cmd.contains("guardar") || cmd.contains("siguiente") || cmd.contains("comenzar") || cmd.contains("continue") || cmd.contains("next")) {
                    ttsManager?.hablar(loc("Avanzando.", "Proceeding."))
                }
            }
        }
    }

    com.example.nutriia.auth.QuizStepLayout(
        Icons.Rounded.Info,
        Color(0xFFEC9BBF),
        loc("Datos de peso y talla", "Weight and height details"),
        loc("Para calcular tu ganancia de peso saludable", "To calculate your healthy weight gain")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (esAccesible) {
                CampoTextoAccesible(
                    valor = edadText,
                    onValorChange = {
                        edadText = it
                        it.toIntOrNull()?.let { valEdad -> onEdadChange(valEdad) }
                    },
                    etiqueta = loc("Edad (años)", "Age (years)"),
                    descripcionVoz = loc("Di tu edad en años", "Speak your age in years"),
                    placeholder = "Ej. 28",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    ttsManager = ttsManager,
                    colorPrimario = Color(0xFFEC9BBF),
                    activo = campoActivo == 0,
                    onFocus = { campoActivo = 0 },
                    onNext = { campoActivo = 1 }
                )
                androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 1) {
                    CampoTextoAccesible(
                        valor = tallaText,
                        onValorChange = {
                            tallaText = it
                            it.toDoubleOrNull()?.let { valTalla ->
                                val tallaMapeada = if (valTalla > 3.0) valTalla / 100.0 else valTalla
                                onTallaMChange(tallaMapeada)
                            }
                        },
                        etiqueta = loc("Estatura (metros)", "Height (meters)"),
                        descripcionVoz = loc("Di tu estatura en metros, por ejemplo uno punto sesenta y cinco", "Speak your height in meters, for example one point sixty five"),
                        placeholder = "Ej. 1.65",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        ttsManager = ttsManager,
                        colorPrimario = Color(0xFFEC9BBF),
                        activo = campoActivo == 1,
                        onFocus = { campoActivo = 1 },
                        onNext = { campoActivo = 2 }
                    )
                }
                androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 2) {
                    CampoTextoAccesible(
                        valor = pesoText,
                        onValorChange = {
                            pesoText = it
                            it.toDoubleOrNull()?.let { valPeso -> onPesoPregestacionalKgChange(valPeso) }
                        },
                        etiqueta = loc("Peso pregestacional (kg)", "Pre-pregnancy weight (kg)"),
                        descripcionVoz = loc("Di tu peso antes del embarazo en kilogramos", "Speak your weight in kilograms before pregnancy"),
                        placeholder = "Ej. 60",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        ttsManager = ttsManager,
                        colorPrimario = Color(0xFFEC9BBF),
                        activo = campoActivo == 2,
                        onFocus = { campoActivo = 2 },
                        onNext = { campoActivo = 3 }
                    )
                }
            } else {
                // Edad
                OutlinedTextField(
                    value = edadText,
                    onValueChange = {
                        edadText = it
                        it.toIntOrNull()?.let { valEdad -> onEdadChange(valEdad) }
                    },
                    label = { Text(loc("Edad (años)", "Age (years)")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFEC9BBF), unfocusedBorderColor = Color.LightGray)
                )

                // Talla (m)
                OutlinedTextField(
                    value = tallaText,
                    onValueChange = {
                        tallaText = it
                        it.toDoubleOrNull()?.let { valTalla ->
                            val tallaMapeada = if (valTalla > 3.0) valTalla / 100.0 else valTalla
                            onTallaMChange(tallaMapeada)
                        }
                    },
                    label = { Text(loc("Estatura (metros, ej. 1.65)", "Height (meters, e.g. 1.65)")) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFEC9BBF), unfocusedBorderColor = Color.LightGray)
                )

                // Peso pregestacional (kg)
                Column {
                    OutlinedTextField(
                        value = pesoText,
                        onValueChange = {
                            pesoText = it
                            it.toDoubleOrNull()?.let { valPeso -> onPesoPregestacionalKgChange(valPeso) }
                        },
                        label = { Text(loc("Peso pregestacional (kg)", "Pre-pregnancy weight (kg)")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFEC9BBF), unfocusedBorderColor = Color.LightGray)
                    )
                    Text(
                        text = loc("Si no lo sabes exacto, usa tu peso actual.", "If you don't know exactly, use your current weight."),
                        fontSize = 11.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp)
                    )
                }
            }

            // Embarazo múltiple (Gemelar o más)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(16.dp))
                    .clickable { onEsGemelarChange(!esGemelar) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = loc("¿Embarazo Múltiple?", "Multiple Pregnancy?"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = loc("Gemelar o más bebés", "Twins or more babies"),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Switch(
                    checked = esGemelar,
                    onCheckedChange = onEsGemelarChange,
                    colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFFEC9BBF), checkedTrackColor = Color(0xFFEC9BBF).copy(alpha = 0.5f))
                )
            }
        }
    }
}
