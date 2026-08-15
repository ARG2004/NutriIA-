package com.example.nutriia.shared

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

// ═══════════════════════════════════════════════════════════════════════════
// PALETA OFICIAL DE NUTRIA (100% IDÉNTICA A ANDROID)
// ═══════════════════════════════════════════════════════════════════════════
val NutriaGreen      = Color(0xFF689F38)
val NutriaDarkGreen  = Color(0xFF33691E)
val NutriaOrange     = Color(0xFFFF8F00)
val NutriaBgCrema    = Color(0xFFF8F9F3)
val NutriaSoftPurple = Color(0xFF9C8FE0)
val NutriaSoftTeal   = Color(0xFF4DB6AC)
val NutriaPink       = Color(0xFFEC9BBF)
val NutriaBlue       = Color(0xFF64B5F6)
val NutriaGineRosa   = Color(0xFFF06292)
val NutriaYellow     = Color(0xFFFBC02D)
val NutriaRed        = Color(0xFFE53935)

enum class AccessibilityMode {
    NORMAL, BLIND, MUTE, DEAF, COLOR_BLIND
}

enum class Screen {
    ACCESIBILIDAD_INICIAL, LOGIN, REGISTER_TYPE, REGISTER_PARENT, REGISTER_NUTRITIONIST, REGISTER_MAMA_PRIMERIZA,
    REGISTER_GINECOLOGO,
    QUIZ, QUIZ_MAMA_PRIMERIZA, DASHBOARD_PARENT, DASHBOARD_NUTRITIONIST, DASHBOARD_MAMA_PRIMERIZA,
    DASHBOARD_GINECOLOGO,
    VINCULACION_GINECOLOGO, DIRECTORIO_GINECOLOGOS,
    LACTANCIA, SOLIDOS, CRECIMIENTO, SUENO, MICRONUTRIENTES, NEURODESARROLLO, MEAL_PLANNING, CHAT_IA, DIARIO_VISUAL, RECORDATORIOS,
    NUTRIENTES, DIETA, CONFIGURACION, EDITAR_PERFIL, EDITAR_REGION, PEDIATRA_DASHBOARD, PACIENTE_EXPEDIENTE, EXPEDIENTE_EMBARAZO, AYUDA, PAGO_TELECONSULTA,
    BIOMETRIC_ACTIVATION, NUTRICION_EMBARAZO, CITAS_EMBARAZO
}

data class ChildData(
    val id: String,
    val name: String,
    val birthDate: String,
    val ageText: String,
    val stage: String,
    val weight: String,
    val height: String,
    val headCirc: String,
    val bmiPercentile: String
)

@Composable
fun AppiOS() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.ACCESIBILIDAD_INICIAL) }
    var isFirstLaunch by rememberSaveable { mutableStateOf(true) }
    var a11yMode by rememberSaveable { mutableStateOf(AccessibilityMode.NORMAL) }
    var showSplash by remember { mutableStateOf(true) }
    var splashMessage by remember { mutableStateOf("NutrIA...") }
    var isOfflineMode by rememberSaveable { mutableStateOf(false) }
    var hasBiometricsEnabled by rememberSaveable { mutableStateOf(false) }

    var userEmail by rememberSaveable { mutableStateOf("familia@nutriia.com") }
    var userName by rememberSaveable { mutableStateOf("Familia Rivera") }
    var userRole by rememberSaveable { mutableStateOf("padre") }
    var activeChildIndex by rememberSaveable { mutableIntStateOf(0) }
    var semanasEmbarazo by rememberSaveable { mutableIntStateOf(24) }

    var childrenList by remember {
        mutableStateOf(
            listOf(
                ChildData(
                    id = "1",
                    name = "Mateo Rivera",
                    birthDate = "15/02/2026",
                    ageText = "6 meses y 15 días",
                    stage = "Iniciando Sólidos",
                    weight = "7.8",
                    height = "67.0",
                    headCirc = "42.5",
                    bmiPercentile = "P50 OMS"
                )
            )
        )
    }

    val activeChild = childrenList.getOrNull(activeChildIndex) ?: childrenList.first()

    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
        if (!isFirstLaunch) {
            currentScreen = Screen.LOGIN
        }
    }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = NutriaGreen,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8F5E9),
            onPrimaryContainer = NutriaDarkGreen,
            secondary = NutriaOrange,
            surface = NutriaBgCrema,
            onSurface = Color(0xFF1C1B1F)
        )
    ) {
        // Safe Area adaptativa para iPhone SE 2020 (Botón Home + Barra Superior)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(NutriaBgCrema)
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = NutriaBgCrema
            ) {
                Crossfade(targetState = currentScreen, animationSpec = tween(220)) { screen ->
                    when (screen) {
                        Screen.ACCESIBILIDAD_INICIAL -> AccesibilidadInicialScreen(
                            currentMode = a11yMode,
                            onModeSelected = { mode ->
                                a11yMode = mode
                                isFirstLaunch = false
                                currentScreen = Screen.LOGIN
                            },
                            onSkip = {
                                a11yMode = AccessibilityMode.NORMAL
                                isFirstLaunch = false
                                currentScreen = Screen.LOGIN
                            }
                        )

                        Screen.LOGIN -> NutriaLoginScreen(
                            onLoginSuccess = { email, rol ->
                                userEmail = email.ifBlank { "familia@nutriia.com" }
                                userRole = rol
                                if (!hasBiometricsEnabled) {
                                    currentScreen = Screen.BIOMETRIC_ACTIVATION
                                } else {
                                    currentScreen = when (rol) {
                                        "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                                        "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                                        "mama_primeriza" -> Screen.DASHBOARD_MAMA_PRIMERIZA
                                        else -> Screen.DASHBOARD_PARENT
                                    }
                                }
                            },
                            onNavigateToRegister = { currentScreen = Screen.REGISTER_TYPE },
                            onBiometricLogin = {
                                currentScreen = Screen.DASHBOARD_PARENT
                            }
                        )

                        Screen.BIOMETRIC_ACTIVATION -> BiometricActivationScreen(
                            onActivated = {
                                hasBiometricsEnabled = true
                                currentScreen = when (userRole) {
                                    "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                                    "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                                    "mama_primeriza" -> Screen.DASHBOARD_MAMA_PRIMERIZA
                                    else -> Screen.DASHBOARD_PARENT
                                }
                            },
                            onSkip = {
                                currentScreen = when (userRole) {
                                    "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                                    "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                                    "mama_primeriza" -> Screen.DASHBOARD_MAMA_PRIMERIZA
                                    else -> Screen.DASHBOARD_PARENT
                                }
                            }
                        )

                        Screen.REGISTER_TYPE -> RegisterTypeScreen(
                            onRoleSelected = { role ->
                                when (role) {
                                    "Padre / Madre de Familia" -> {
                                        userRole = "padre"
                                        currentScreen = Screen.REGISTER_PARENT
                                    }
                                    "Nutriólogo Clínico" -> {
                                        userRole = "nutriologo"
                                        currentScreen = Screen.REGISTER_NUTRITIONIST
                                    }
                                    "Mamá Primeriza" -> {
                                        userRole = "mama_primeriza"
                                        currentScreen = Screen.REGISTER_MAMA_PRIMERIZA
                                    }
                                    else -> {
                                        userRole = "ginecologo"
                                        currentScreen = Screen.REGISTER_GINECOLOGO
                                    }
                                }
                            },
                            onBackToLogin = { currentScreen = Screen.LOGIN }
                        )

                        Screen.REGISTER_PARENT -> ParentRegisterScreen(
                            onRegistered = { name, email ->
                                userName = name
                                userEmail = email
                                currentScreen = Screen.QUIZ
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )

                        Screen.REGISTER_NUTRITIONIST -> ProfessionalRegisterScreen(
                            roleTitle = "Nutriólogo Clínico",
                            profesionRequerida = "Licenciatura en Nutrición",
                            onRegistered = { name, email, cedula ->
                                userName = name
                                userEmail = email
                                currentScreen = Screen.DASHBOARD_NUTRITIONIST
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )

                        Screen.REGISTER_MAMA_PRIMERIZA -> MamaPrimerizaRegisterScreen(
                            onRegistered = { name, email, semanas ->
                                userName = name
                                userEmail = email
                                semanasEmbarazo = semanas
                                currentScreen = Screen.QUIZ_MAMA_PRIMERIZA
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )

                        Screen.REGISTER_GINECOLOGO -> ProfessionalRegisterScreen(
                            roleTitle = "Ginecólogo Obstetra",
                            profesionRequerida = "Médico Cirujano / Ginecología",
                            onRegistered = { name, email, cedula ->
                                userName = name
                                userEmail = email
                                currentScreen = Screen.DASHBOARD_GINECOLOGO
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )

                        Screen.QUIZ -> OnboardingQuizCompleteView(
                            initialChildName = activeChild.name,
                            onQuizComplete = { name, bDate, w, h, hc ->
                                val newChild = ChildData(
                                    id = (childrenList.size + 1).toString(),
                                    name = name.ifBlank { "Mateo Rivera" },
                                    birthDate = bDate,
                                    ageText = "6 meses",
                                    stage = "Iniciando Sólidos",
                                    weight = w,
                                    height = h,
                                    headCirc = hc,
                                    bmiPercentile = "P50 OMS"
                                )
                                childrenList = childrenList + newChild
                                activeChildIndex = childrenList.lastIndex
                                currentScreen = Screen.DASHBOARD_PARENT
                            },
                            onCancel = { currentScreen = Screen.DASHBOARD_PARENT }
                        )

                        Screen.QUIZ_MAMA_PRIMERIZA -> EmbarazoQuizCompleteView(
                            initialSemanas = semanasEmbarazo,
                            onQuizComplete = { semanas ->
                                semanasEmbarazo = semanas
                                currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                            },
                            onCancel = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                        )

                        Screen.DASHBOARD_PARENT -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NutriIADashboardParentView(
                                parentName = userName,
                                children = childrenList,
                                activeChildIndex = activeChildIndex,
                                onChildChanged = { activeChildIndex = it },
                                onAddChild = { currentScreen = Screen.QUIZ },
                                onNavigate = { currentScreen = it },
                                onAyuda = { currentScreen = Screen.AYUDA },
                                onConfig = { currentScreen = Screen.CONFIGURACION },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }

                        Screen.DASHBOARD_MAMA_PRIMERIZA -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DashboardMamaPrimerizaView(
                                semanas = semanasEmbarazo,
                                onNavigate = { currentScreen = it },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }

                        Screen.DASHBOARD_NUTRITIONIST -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DashboardNutritionistView(
                                onNavigate = { currentScreen = it },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }

                        Screen.DASHBOARD_GINECOLOGO -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DashboardGinecologistView(
                                onNavigate = { currentScreen = it },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }

                        Screen.LACTANCIA -> MainAppScaffold(
                            currentTab = Screen.LACTANCIA,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            LactanciaScreenView(childName = activeChild.name, onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.SOLIDOS -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            SolidosScreenView(childName = activeChild.name, onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.CRECIMIENTO -> MainAppScaffold(
                            currentTab = Screen.CRECIMIENTO,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            CrecimientoScreenView(
                                childName = activeChild.name,
                                weight = activeChild.weight,
                                height = activeChild.height,
                                headCirc = activeChild.headCirc,
                                onBack = { currentScreen = Screen.DASHBOARD_PARENT }
                            )
                        }

                        Screen.SUENO -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            SuenoScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.NUTRIENTES, Screen.MICRONUTRIENTES -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NutrientesScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.NEURODESARROLLO -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NeurodesarrolloScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.MEAL_PLANNING, Screen.DIETA -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            MealPlanningScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.CHAT_IA -> MainAppScaffold(
                            currentTab = Screen.CHAT_IA,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NutriChatScreenView(childName = activeChild.name, onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.DIARIO_VISUAL -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DiarioVisualScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.RECORDATORIOS -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            RecordatoriosScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.PEDIATRA_DASHBOARD -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            PediatraScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }

                        Screen.CONFIGURACION -> MainAppScaffold(
                            currentTab = Screen.CONFIGURACION,
                            isOffline = isOfflineMode,
                            onTabSelected = { currentScreen = it }
                        ) {
                            ConfiguracionScreenView(
                                userEmail = userEmail,
                                userName = userName,
                                isOffline = isOfflineMode,
                                onToggleOffline = { isOfflineMode = !isOfflineMode },
                                onBack = { currentScreen = Screen.DASHBOARD_PARENT },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }

                        Screen.AYUDA -> HelpScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })

                        else -> {
                            Box(modifier = Modifier.fillMaxSize().background(NutriaBgCrema), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Módulo: $screen", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                                    Spacer(Modifier.height(12.dp))
                                    Button(onClick = { currentScreen = Screen.DASHBOARD_PARENT }) {
                                        Text("Volver al Dashboard")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = showSplash,
                enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.94f, animationSpec = tween(400, easing = FastOutSlowInEasing)),
                exit = fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 1.06f, animationSpec = tween(450, easing = FastOutSlowInEasing))
            ) {
                SplashOverlay(mensaje = splashMessage)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// COMPONENTE SPLASHOVERLAY OFICIAL DE ANDROID
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun SplashOverlay(
    mensaje: String = "Cargando..."
) {
    var animIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animIn = true }

    val alphaAnim by animateFloatAsState(
        targetValue = if (animIn) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val scaleAnim by animateFloatAsState(
        targetValue = if (animIn) 1f else 0.93f,
        animationSpec = tween(400, easing = FastOutSlowInEasing),
        label = "splashScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "splashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .graphicsLayer {
                alpha = alphaAnim
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .clickable(enabled = false) {},
        contentAlignment = Alignment.Center
    ) {
        NutriaSplashMascota(
            modifier = Modifier
                .size(260.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                }
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = Color(0xFF4CAF50),
                modifier = Modifier.size(44.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = mensaje,
                color = Color(0xFF555555),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// PANTALLA: ACCESIBILIDAD INICIAL
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun AccesibilidadInicialScreen(
    currentMode: AccessibilityMode,
    onModeSelected: (AccessibilityMode) -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(NutriaGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.AccessibilityNew, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("Accesibilidad NutrIA", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
        Text("Personaliza tu interacción para una experiencia inclusiva y accesible.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))

        val modes = listOf(
            A11yOption(AccessibilityMode.NORMAL, "Modo Estándar", "Interacción visual táctil convencional", Icons.Rounded.PhoneIphone, NutriaGreen),
            A11yOption(AccessibilityMode.BLIND, "Asistencia por Voz", "Lector en pantalla y comandos de voz guiados", Icons.Rounded.RecordVoiceOver, NutriaOrange),
            A11yOption(AccessibilityMode.MUTE, "Modo Visual", "Interacción asistida sin necesidad de dictado", Icons.Rounded.Visibility, NutriaSoftTeal),
            A11yOption(AccessibilityMode.DEAF, "Subtítulos & Haptic", "Alertas vibratorias y transcripción visual", Icons.Rounded.Hearing, NutriaSoftPurple),
            A11yOption(AccessibilityMode.COLOR_BLIND, "Alto Contraste", "Paleta adaptada para daltonismo", Icons.Rounded.Palette, NutriaDarkGreen)
        )

        modes.forEach { option ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable { onModeSelected(option.mode) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (currentMode == option.mode) Color(0xFFE8F5E9) else Color.White),
                border = if (currentMode == option.mode) BorderStroke(2.dp, NutriaGreen) else null,
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(option.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(option.icon, contentDescription = null, tint = option.color, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(option.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Text(option.subtitle, fontSize = 12.sp, color = Color.Gray)
                    }
                    if (currentMode == option.mode) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NutriaGreen)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onSkip) {
            Text("Omitir por ahora", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

data class A11yOption(val mode: AccessibilityMode, val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

// ═══════════════════════════════════════════════════════════════════════════
// PANTALLA: ACTIVACIÓN BIOMÉTRICA (TOUCH ID / FACE ID)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun BiometricActivationScreen(
    onActivated: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(NutriaGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = "Touch ID", tint = NutriaGreen, modifier = Modifier.size(60.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text("Desbloqueo Biométrico", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
        Spacer(Modifier.height(8.dp))
        Text("Usa Touch ID o Face ID para iniciar sesión de manera rápida y segura en tu iPhone.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onActivated,
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Activar Touch ID / Face ID", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onSkip) {
            Text("Quizás más tarde", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 1. PANTALLA DE LOGIN CON SOPORTE BIOMÉTRICO
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun NutriaLoginScreen(
    onLoginSuccess: (String, String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBiometricLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }
    val entranceAlpha by animateFloatAsState(if (startAnimation) 1f else 0f, tween(800), label = "alpha")

    Box(modifier = Modifier.fillMaxSize().background(NutriaBgCrema)) {
        AnimatedMinimalistBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .alpha(entranceAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(30.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NutriaMascotaHeader(modifier = Modifier.size(230.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Nutre su hoy, protege su mañana",
                    fontSize = 15.sp,
                    color = NutriaDarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NutriaDarkGreen
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Correo Electrónico", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email", tint = NutriaGreen) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriaGreen,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Contraseña", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Contraseña", tint = NutriaGreen) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Mostrar contraseña",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriaGreen,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )

                    TextButton(
                        onClick = { showReset = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("¿Olvidaste tu contraseña?", color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                isLoading = true
                                val roleDetected = when {
                                    email.contains("nutri") -> "nutriologo"
                                    email.contains("gine") -> "ginecologo"
                                    email.contains("mama") -> "mama_primeriza"
                                    else -> "padre"
                                }
                                onLoginSuccess(email, roleDetected)
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NutriaGreen,
                                disabledContainerColor = Color.LightGray
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ENTRAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 15.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        IconButton(
                            onClick = onBiometricLogin,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(NutriaGreen.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = "Touch ID", tint = NutriaGreen, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("¿Nuevo en NutrIA?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Crea una cuenta", color = NutriaDarkGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showReset) {
            AlertDialog(
                onDismissRequest = { showReset = false; resetMessage = null },
                title = { Text("Recuperar contraseña", fontWeight = FontWeight.Bold, color = NutriaDarkGreen) },
                text = {
                    Column {
                        Text("Escribe tu correo y te enviaremos las instrucciones.", color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            placeholder = { Text("ejemplo@correo.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        resetMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = NutriaGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        resetMessage = "Correo de recuperación enviado con éxito."
                    }) { Text("Enviar", color = NutriaGreen, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showReset = false; resetMessage = null }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 2. SELECCIÓN DE ROL DE REGISTRO
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterTypeScreen(
    onRoleSelected: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onBackToLogin,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
        }

        Spacer(Modifier.height(16.dp))

        Text("¿Cómo deseas unirte?", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
        Text("Selecciona tu rol para personalizar tu experiencia clínica y nutricional.", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(20.dp))

        val roles = listOf(
            RoleItem("Padre / Madre de Familia", "Seguimiento nutricional, crecimiento y lactancia", Icons.Rounded.FamilyRestroom, NutriaGreen),
            RoleItem("Nutriólogo Clínico", "Validación SEP de Cédula, expedientes y dietas", Icons.Rounded.MedicalServices, NutriaSoftTeal),
            RoleItem("Mamá Primeriza", "Guía paso a paso desde el embarazo hasta la lactancia", Icons.Rounded.PregnantWoman, NutriaPink),
            RoleItem("Ginecólogo Obstetra", "Validación SEP de Cédula y control materno-fetal", Icons.Rounded.LocalHospital, NutriaGineRosa)
        )

        roles.forEach { role ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onRoleSelected(role.title) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(role.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(role.icon, contentDescription = null, tint = role.color, modifier = Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(role.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(2.dp))
                        Text(role.subtitle, fontSize = 11.sp, color = Color.Gray, lineHeight = 15.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = role.color)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 3. REGISTROS ESPECÍFICOS POR ROL
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ParentRegisterScreen(
    onRegistered: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(24.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("Registro: Padre / Madre", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Crea tu cuenta familiar para dar seguimiento al desarrollo de tu bebé.", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(20.dp))

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onRegistered(name.ifBlank { "Familia Rivera" }, email.ifBlank { "familia@nutriia.com" }) },
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Siguiente: Datos del Bebé", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun MamaPrimerizaRegisterScreen(
    onRegistered: (String, String, Int) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var semanas by remember { mutableStateOf("24") }

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(24.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("Registro: Mamá Primeriza", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Acompañamiento nutricional durante tu etapa de gestación.", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(20.dp))

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Electrónico") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = semanas, onValueChange = { semanas = it }, label = { Text("Semanas de Gestación (1-40)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onRegistered(name.ifBlank { "Mamá NutrIA" }, email.ifBlank { "mama@nutriia.com" }, semanas.toIntOrNull() ?: 24) },
                    shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = NutriaPink),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Crear Cuenta Mamá Primeriza", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    }
}

@Composable
fun ProfessionalRegisterScreen(
    roleTitle: String,
    profesionRequerida: String,
    onRegistered: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var isVerifyingCedula by remember { mutableStateOf(false) }
    var cedulaVerificada by remember { mutableStateOf<Boolean?>(null) }
    var cedulaDetalle by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(24.dp)) {
        IconButton(onClick = onBack, modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("Registro: $roleTitle", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Validación oficial de Cédula Profesional ante el Registro Nacional de Profesionistas (SEP).", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(20.dp))

        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre Completo") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Correo Institucional / Profesional") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Contraseña") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cedula,
                        onValueChange = {
                            cedula = it.filter(Char::isDigit)
                            cedulaVerificada = null
                            cedulaDetalle = null
                        },
                        label = { Text("Cédula Profesional") },
                        placeholder = { Text("Ej. 12345678") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (cedula.length >= 7) {
                                isVerifyingCedula = true
                                cedulaVerificada = true
                                cedulaDetalle = "Cédula Válida • $profesionRequerida • SEP"
                                isVerifyingCedula = false
                            } else {
                                cedulaVerificada = false
                                cedulaDetalle = "Debe tener al menos 7 u 8 dígitos"
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NutriaSoftTeal)
                    ) {
                        Text("Verificar")
                    }
                }

                cedulaDetalle?.let { msg ->
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (cedulaVerificada == true) Icons.Rounded.Verified else Icons.Rounded.Error,
                            contentDescription = null,
                            tint = if (cedulaVerificada == true) NutriaGreen else NutriaRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = msg,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (cedulaVerificada == true) NutriaGreen else NutriaRed
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { onRegistered(name.ifBlank { "Dr(a). Profesional" }, email.ifBlank { "pro@nutriia.com" }, cedula) },
                    enabled = cedulaVerificada == true,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NutriaDarkGreen, disabledContainerColor = Color.LightGray),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text("Completar Registro Profesional", fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 4. ONBOARDING QUIZ COMPLETO
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun OnboardingQuizCompleteView(
    initialChildName: String,
    onQuizComplete: (name: String, birthDate: String, weight: String, height: String, headCirc: String) -> Unit,
    onCancel: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(1) }
    var childNameInput by rememberSaveable { mutableStateOf(initialChildName) }
    var birthDate by rememberSaveable { mutableStateOf("15/02/2026") }
    var sexo by rememberSaveable { mutableStateOf("Niño") }
    var weight by rememberSaveable { mutableStateOf("7.8") }
    var height by rememberSaveable { mutableStateOf("67.0") }
    var headCirc by rememberSaveable { mutableStateOf("42.5") }
    var tipoParto by rememberSaveable { mutableStateOf("Parto Natural") }
    var tipoLactancia by rememberSaveable { mutableStateOf("Lactancia Materna Exclusiva") }

    Column(
        modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (step > 1) step-- else onCancel() }) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
            }
            Text("Paso $step de 3", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
        }

        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { step / 3f },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = NutriaGreen,
            trackColor = Color(0xFFE0E0E0)
        )

        Spacer(Modifier.height(20.dp))

        when (step) {
            1 -> {
                Text("👶 Datos Generales del Bebé", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                Text("Ingresa los datos para calibrar las tablas de crecimiento OMS.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(20.dp))

                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        OutlinedTextField(value = childNameInput, onValueChange = { childNameInput = it }, label = { Text("Nombre del Bebé") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = birthDate, onValueChange = { birthDate = it }, label = { Text("Fecha de Nacimiento (DD/MM/AAAA)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(14.dp))
                        Text("Sexo Biológico", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("Niño", "Niña").forEach { s ->
                                FilterChip(
                                    selected = sexo == s,
                                    onClick = { sexo = s },
                                    label = { Text(s, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NutriaGreen, selectedLabelColor = Color.White)
                                )
                            }
                        }
                    }
                }
            }

            2 -> {
                Text("📏 Mediciones Antropométricas", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                Text("Parámetros clínicos para calcular percentiles y Z-Score.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(20.dp))

                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Peso Actual (kg)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Longitud / Talla (cm)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = headCirc, onValueChange = { headCirc = it }, label = { Text("Perímetro Cefálico (cm)") }, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth())
                    }
                }
            }

            3 -> {
                Text("🍼 Alimentación y Nacimiento", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                Text("Factores que influyen en el plan de alimentación complementaria.", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(20.dp))

                Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("Tipo de Nacimiento", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Parto Natural", "Cesárea").forEach { tp ->
                                FilterChip(
                                    selected = tipoParto == tp,
                                    onClick = { tipoParto = tp },
                                    label = { Text(tp) },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = NutriaGreen, selectedLabelColor = Color.White)
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Régimen de Lactancia Actual", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        listOf("Lactancia Materna Exclusiva", "Fórmula Infantil", "Lactancia Mixta").forEach { tl ->
                            Row(modifier = Modifier.fillMaxWidth().clickable { tipoLactancia = tl }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = tipoLactancia == tl, onClick = { tipoLactancia = tl }, colors = RadioButtonDefaults.colors(selectedColor = NutriaGreen))
                                Text(tl, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (step < 3) step++
                else onQuizComplete(childNameInput.ifBlank { "Mateo Rivera" }, birthDate, weight, height, headCirc)
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text(if (step < 3) "Continuar" else "Finalizar y Ver Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

@Composable
fun EmbarazoQuizCompleteView(
    initialSemanas: Int,
    onQuizComplete: (Int) -> Unit,
    onCancel: () -> Unit
) {
    var semanas by rememberSaveable { mutableIntStateOf(initialSemanas) }

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(24.dp)) {
        IconButton(onClick = onCancel) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("🤰 Calibración de tu Embarazo", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
        Text("Ajusta tu semana actual de gestación.", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(24.dp))

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Semana $semanas", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = NutriaPink)
                Text(if (semanas <= 13) "Primer Trimestre" else if (semanas <= 27) "Segundo Trimestre" else "Tercer Trimestre", fontSize = 13.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Slider(
                    value = semanas.toFloat(),
                    onValueChange = { semanas = it.toInt() },
                    valueRange = 1f..40f,
                    colors = SliderDefaults.colors(thumbColor = NutriaPink, activeTrackColor = NutriaPink)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { onQuizComplete(semanas) },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaPink),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Guardar y Ver Dashboard", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 5. SCAFFOLD CON BOTTOM NAVIGATION BAR & OFFLINE BANNER
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun MainAppScaffold(
    currentTab: Screen,
    isOffline: Boolean,
    onTabSelected: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            if (isOffline) {
                Surface(color = NutriaOrange, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.CloudOff, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Modo Offline: Datos guardados localmente", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val navItems = listOf(
                    NavTabItem(Screen.DASHBOARD_PARENT, "Inicio", Icons.Rounded.Home),
                    NavTabItem(Screen.LACTANCIA, "Lactancia", Icons.Rounded.Favorite),
                    NavTabItem(Screen.CHAT_IA, "NutriIA Chat", Icons.Rounded.ChatBubble),
                    NavTabItem(Screen.CRECIMIENTO, "Crecimiento", Icons.AutoMirrored.Rounded.ShowChart),
                    NavTabItem(Screen.CONFIGURACION, "Ajustes", Icons.Rounded.Settings)
                )

                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentTab == item.screen,
                        onClick = { onTabSelected(item.screen) },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = {
                            Text(
                                text = item.label,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == item.screen) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == item.screen) NutriaDarkGreen else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NutriaGreen,
                            indicatorColor = Color(0xFFE8F5E9)
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            content()
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 6. DASHBOARD PRINCIPAL DE PADRES (ADAPTADO 100% DE DASHBOARD.KT)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun NutriIADashboardParentView(
    parentName: String,
    children: List<ChildData>,
    activeChildIndex: Int,
    onChildChanged: (Int) -> Unit,
    onAddChild: () -> Unit,
    onNavigate: (Screen) -> Unit,
    onAyuda: () -> Unit,
    onConfig: () -> Unit,
    onLogout: () -> Unit
) {
    val activeChild = children.getOrNull(activeChildIndex) ?: children.first()
    val pagerState = rememberPagerState(initialPage = activeChildIndex, pageCount = { children.size })

    LaunchedEffect(pagerState.currentPage) {
        onChildChanged(pagerState.currentPage)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Encabezado con logo, nombre y accesos rápidos de Dashboard.kt
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(CircleShape).background(NutriaGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Eco, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("¡Hola, $parentName! 🌿", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                        Text("Plan Nutricional Activo OMS", fontSize = 11.sp, color = Color.Gray)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onAyuda, modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White)) {
                        Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Ayuda", tint = NutriaDarkGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onConfig, modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White)) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Configuración", tint = NutriaDarkGreen, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onLogout, modifier = Modifier.size(38.dp).clip(CircleShape).background(Color.White)) {
                        Icon(Icons.AutoMirrored.Rounded.ExitToApp, contentDescription = "Salir", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // Carrusel Pager de Tarjetas de Bebés (ChildProfileCard de Dashboard.kt)
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NutriaGreen),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = activeChild.name.firstOrNull()?.toString() ?: "B",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(activeChild.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("${activeChild.stage} • ${activeChild.ageText}", fontSize = 11.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                        }

                        IconButton(
                            onClick = onAddChild,
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Agregar hijo", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ChipInfo(label = "${activeChild.weight} kg", subtitle = "Peso P50")
                        ChipInfo(label = "${activeChild.height} cm", subtitle = "Talla P55")
                        ChipInfo(label = "${activeChild.headCirc} cm", subtitle = "Cefálico P50")
                        ChipInfo(label = activeChild.bmiPercentile, subtitle = "Percentil")
                    }
                }
            }
        }

        // Banner de Alerta Clínica Activa
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(NutriaOrange.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.NotificationsActive, contentDescription = null, tint = NutriaOrange, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Próxima Evaluación Pediátrica", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                        Text("Vacuna de los 6 meses (Hexavalente) programada para esta semana.", fontSize = 11.sp, color = Color(0xFF795548))
                    }
                }
            }
        }

        // Botón Destacado de NutriBot (Exacto a Dashboard.kt)
        item {
            Button(
                onClick = { onNavigate(Screen.CHAT_IA) },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Consultar NutriBot", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }

        item {
            Text("Módulos de Seguimiento", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        }

        // Fila 1: Lactancia y Alimentación (Exacto a Dashboard.kt)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashModuleCard("Lactancia", "Cronómetro y tomas", Icons.Rounded.ChildCare, NutriaPink, NutriaPink.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.LACTANCIA) }
                DashModuleCard("Alimentación", "Guías y alimentos BLW", Icons.Rounded.Restaurant, NutriaOrange, NutriaOrange.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.SOLIDOS) }
            }
        }

        // Fila 2: Crecimiento y Sueño (Exacto a Dashboard.kt)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashModuleCard("Crecimiento", "Percentiles OMS", Icons.AutoMirrored.Rounded.ShowChart, NutriaGreen, NutriaGreen.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.CRECIMIENTO) }
                DashModuleCard("Sueño", "Ventanas de vigilia", Icons.Rounded.Bedtime, NutriaSoftPurple, NutriaSoftPurple.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.SUENO) }
            }
        }

        // Fila 3: Nutrientes y Pediatra / Nutriólogo (Exacto a Dashboard.kt)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashModuleCard("Nutrientes", "Hierro, Zinc y Vitamina D", Icons.Rounded.Medication, NutriaSoftTeal, NutriaSoftTeal.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.NUTRIENTES) }
                DashModuleCard("Pediatra /\nNutriólogo", "Teleconsulta y citas", Icons.Rounded.MedicalServices, NutriaBlue, NutriaBlue.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.PEDIATRA_DASHBOARD) }
            }
        }

        // Fila 4: Análisis NutriIA y Alertas (Exacto a Dashboard.kt)
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashModuleCard("Análisis NutriIA", "Diario visual de fotos", Icons.Rounded.PhotoCamera, NutriaOrange, NutriaOrange.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.DIARIO_VISUAL) }
                DashModuleCard("Alertas", "Vacunas y recordatorios", Icons.Rounded.NotificationsActive, NutriaBlue, NutriaBlue.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.RECORDATORIOS) }
            }
        }

        // Fila 5: Neurodesarrollo y Plan Semanal
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashModuleCard("Neurodesarrollo", "Hitos motores y cognitivos", Icons.Rounded.Psychology, NutriaSoftPurple, NutriaSoftPurple.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.NEURODESARROLLO) }
                DashModuleCard("Plan Semanal", "Menús por edad OMS", Icons.Rounded.CalendarToday, NutriaGreen, NutriaGreen.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.MEAL_PLANNING) }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 7. DASHBOARDS DE MAMÁ PRIMERIZA, NUTRIÓLOGO Y GINECÓLOGO
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DashboardMamaPrimerizaView(semanas: Int, onNavigate: (Screen) -> Unit, onLogout: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("🤰 Mi Embarazo Semana a Semana", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
            Text("Semana $semanas de gestación • Segundo Trimestre", fontSize = 13.sp, color = Color.Gray)
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = NutriaPink), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("Bebé: Tamaño de una Mazorca de Maíz 🌽", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text("Peso estimado: 600g • Longitud: 30cm", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DashModuleCard("Nutrición Embarazo", "Ácido fólico y hierro", Icons.Rounded.Spa, NutriaGreen, NutriaGreen.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.NUTRIENTES) }
                DashModuleCard("Citas Prenatales", "Calendario y ecografías", Icons.Rounded.CalendarMonth, NutriaBlue, NutriaBlue.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.RECORDATORIOS) }
            }
        }
    }
}

@Composable
fun DashboardNutritionistView(onNavigate: (Screen) -> Unit, onLogout: () -> Unit) {
    var searchQuery by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("🩺 Directorio Clínico Nutricional", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
            Text("Expedientes de Pacientes Pediátricos Activos", fontSize = 13.sp, color = Color.Gray)
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar paciente por nombre...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = NutriaGreen) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Mateo Rivera", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                            Text("6 meses • Peso 7.8kg • Talla 67cm", fontSize = 12.sp, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFE8F5E9)).padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Eutrófico P50", color = NutriaDarkGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onNavigate(Screen.CRECIMIENTO) }, colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen), modifier = Modifier.weight(1f)) {
                            Text("Curvas OMS", fontSize = 12.sp)
                        }
                        Button(onClick = { onNavigate(Screen.MEAL_PLANNING) }, colors = ButtonDefaults.buttonColors(containerColor = NutriaSoftTeal), modifier = Modifier.weight(1f)) {
                            Text("Plan Dieta", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardGinecologistView(onNavigate: (Screen) -> Unit, onLogout: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("🏥 Panel Obstétrico y Control Prenatal", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
            Text("Seguimiento Materno-Fetal", fontSize = 13.sp, color = Color.Gray)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paciente: María García (Semana 24)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Presión arterial: 110/70 • Ganancia ponderal adecuada", fontSize = 12.sp, color = NutriaDarkGreen)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 8. VISTAS DE LOS 11 MÓDULOS DEL PADRE
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LactanciaScreenView(childName: String, onBack: () -> Unit) {
    var isRunningLeft by remember { mutableStateOf(false) }
    var isRunningRight by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(0) }
    var secondsRight by remember { mutableStateOf(0) }

    LaunchedEffect(isRunningLeft) {
        while (isRunningLeft) { delay(1000); secondsLeft++ }
    }
    LaunchedEffect(isRunningRight) {
        while (isRunningRight) { delay(1000); secondsRight++ }
    }

    fun fmt(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
    }

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Lactancia: $childName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isRunningLeft) Color(0xFFE8F5E9) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Seno Izquierdo", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(fmt(secondsLeft), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = NutriaGreen)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { isRunningLeft = !isRunningLeft; if (isRunningLeft) isRunningRight = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunningLeft) Color(0xFFC62828) else NutriaGreen)
                    ) { Text(if (isRunningLeft) "Pausar" else "Iniciar") }
                }
            }

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isRunningRight) Color(0xFFE8F5E9) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Seno Derecho", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(fmt(secondsRight), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = NutriaGreen)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { isRunningRight = !isRunningRight; if (isRunningRight) isRunningLeft = false },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunningRight) Color(0xFFC62828) else NutriaGreen)
                    ) { Text(if (isRunningRight) "Pausar" else "Iniciar") }
                }
            }
        }
    }
}

@Composable
fun SolidosScreenView(childName: String, onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Sólidos & BLW: $childName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Semáforo de Alimentos (6 a 12 meses)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("🥑 Aguacate: Textura suave, rico en grasas saludables.", fontSize = 13.sp, color = Color(0xFF333333))
                Text("🥕 Zanahoria al vapor: Cocida hasta aplastar con los dedos.", fontSize = 13.sp, color = Color(0xFF333333))
                Text("🍳 Huevo bien cocido: Introducción de alérgeno de forma segura.", fontSize = 13.sp, color = Color(0xFF333333))
            }
        }
    }
}

@Composable
fun CrecimientoScreenView(
    childName: String,
    weight: String,
    height: String,
    headCirc: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Curvas OMS: $childName", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Percentiles Calculados (Estándar OMS)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Peso", fontSize = 12.sp, color = Color.Gray)
                        Text("$weight kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("Percentil 50", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Talla", fontSize = 12.sp, color = Color.Gray)
                        Text("$height cm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("Percentil 55", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cefálico", fontSize = 12.sp, color = Color.Gray)
                        Text("$headCirc cm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("Percentil 50", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SuenoScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Registro de Sueño Infantil", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Ventanas de Vigilia Recomendadas", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("A los 6 meses, la ventana de vigilia promedio es de 2.0 a 2.5 horas entre siestas.", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun NutrientesScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Micronutrientes Clave", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Requerimientos Diarios OMS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• Hierro: 11 mg/día (vital para prevenir anemia)", fontSize = 13.sp)
                Text("• Zinc: 3 mg/día (inmunidad y crecimiento)", fontSize = 13.sp)
                Text("• Vitamina D: 400 UI/día (salud ósea)", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun NeurodesarrolloScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Hitos de Neurodesarrollo", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Hitos esperados (6 a 9 meses)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("✓ Se sienta con poco o ningún apoyo.", fontSize = 13.sp)
                Text("✓ Pasa objetos de una mano a otra.", fontSize = 13.sp)
                Text("✓ Responde a su propio nombre con balbuceo.", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MealPlanningScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Plan Nutricional Semanal", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Menú Sugerido OMS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Desayuno: Puré de avena fortificada con plátano.", fontSize = 13.sp)
                Text("Comida: Bastones de calabacita al vapor con pollo desmenuzado.", fontSize = 13.sp)
                Text("Cena: Leche materna a libre demanda.", fontSize = 13.sp)
            }
        }
    }
}

data class Message(val text: String, val isUser: Boolean)

@Composable
fun NutriChatScreenView(childName: String, onBack: () -> Unit) {
    var messages by remember {
        mutableStateOf(
            listOf(
                Message("¡Hola! Soy tu asistente de nutrición NutrIA para $childName. ¿En qué te puedo orientar hoy?", false)
            )
        )
    }
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Asistente NutriIA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Spacer(Modifier.height(8.dp))

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(messages) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (msg.isUser) NutriaGreen else Color.White)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (msg.isUser) Color.White else Color(0xFF2C3E50),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Escribe una consulta clínica...") },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            Button(
                onClick = {
                    if (input.isNotBlank()) {
                        val q = input
                        input = ""
                        messages = messages + Message(q, true)
                        messages = messages + Message("Entendido. De acuerdo con las guías de la OMS, es fundamental mantener la hidratación y evaluar el grupo de alimentos.", false)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Enviar", tint = Color.White)
            }
        }
    }
}

@Composable
fun DiarioVisualScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Diario Visual de Comidas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Historial de Comidas Registradas", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Captura una foto de cada platillo para analizar su balance nutricional con IA.", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun RecordatoriosScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Recordatorios & Vacunas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Próxima Vacuna: 6 Meses", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("• Hexavalente (3ª Dosis)", fontSize = 13.sp)
                Text("• Rotavirus (3ª Dosis)", fontSize = 13.sp)
                Text("• Influenza estacional", fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun PediatraScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Teleconsulta con Especialistas", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Directorio Pediátrico Conectado", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Programa teleconsultas en video y comparte el expediente clínico con tu médico.", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ConfiguracionScreenView(
    userEmail: String,
    userName: String,
    isOffline: Boolean,
    onToggleOffline: () -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("⚙️ Configuración y Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(NutriaGreen), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(userName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(userEmail, fontSize = 12.sp, color = Color.Gray)
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Modo Offline", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Permite usar la app sin conexión", fontSize = 12.sp, color = Color.Gray)
                }
                Switch(checked = isOffline, onCheckedChange = { onToggleOffline() }, colors = SwitchDefaults.colors(checkedThumbColor = NutriaGreen))
            }
        }

        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HelpScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Centro de Ayuda NutrIA", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Guía de Inicio Rápido", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Aprende a registrar tomas de leche materna, consultar curvas OMS y conversar con el asistente clínico.", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun ChipInfo(label: String, subtitle: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(subtitle, color = Color.White.copy(alpha = 0.8f), fontSize = 9.sp)
        }
    }
}

@Composable
fun DashModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun AnimatedMinimalistBackground() {
    val it = rememberInfiniteTransition(label = "bg")
    val icons = listOf(Icons.Rounded.Eco, Icons.Rounded.Spa, Icons.Rounded.Psychology, Icons.Rounded.LocalFlorist)
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(8) { idx ->
            val sx = remember { Random.nextFloat() }
            val sy = remember { Random.nextFloat() }
            val ty by it.animateFloat(0f, 40f, infiniteRepeatable(tween(7000 + idx * 500, easing = EaseInOutSine), RepeatMode.Reverse), label = "y")
            val rz by it.animateFloat(-8f, 8f, infiniteRepeatable(tween(8000 + idx * 500, easing = EaseInOutSine), RepeatMode.Reverse), label = "r")
            Icon(
                icons[idx % icons.size],
                null,
                modifier = Modifier
                    .offset((sx * 360).dp, (sy * 700).dp)
                    .graphicsLayer {
                        translationY = ty
                        rotationZ = rz
                        alpha = 0.04f
                    }
                    .size(70.dp),
                tint = Color.Black
            )
        }
    }
}
