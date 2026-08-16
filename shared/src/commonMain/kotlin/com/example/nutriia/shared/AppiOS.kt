package com.example.nutriia.shared

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.datetime.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ═══════════════════════════════════════════════════════════════════════════
//  NutriIA iOS — Enrutador y Navegación 1:1 idéntico a MainActivity.kt (Android)
// ═══════════════════════════════════════════════════════════════════════════

import com.example.nutriia.accesibilidad.*
import com.example.nutriia.alerta.AlertasScreen
import com.example.nutriia.analisisIA.AnalisisScreen
import com.example.nutriia.auth.*
import com.example.nutriia.ayuda.HelpScreen
import com.example.nutriia.chatbot.NutriChatScreen
import com.example.nutriia.configuracion.*
import com.example.nutriia.crecimiento.CrecimientoScreen
import com.example.nutriia.dashboard.NutriIADashboardScreen
import com.example.nutriia.dashboard.NutritionistDashboardScreen
import com.example.nutriia.dashboard.PacienteResumen
import com.example.nutriia.embarazo.*
import com.example.nutriia.expediente.PacienteExpedienteScreen
import com.example.nutriia.ginecologo.*
import com.example.nutriia.lactancia.LactanciaScreen
import com.example.nutriia.nutriente.NutrientesScreen
import com.example.nutriia.payment.*
import com.example.nutriia.pediatra.PediatraScreen
import com.example.nutriia.solidos.SolidosScreen
import com.example.nutriia.teleconsulta.*
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.ui.theme.NutriIATheme

// ─── Enum de Pantallas 1:1 idéntico a Android MainActivity.kt ─────────────
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

fun mesesDeVida(fechaNacimiento: String): Int {
    if (fechaNacimiento.isBlank()) return 0
    return try {
        val (dia, mes, anio) = if (fechaNacimiento.contains("/")) {
            val p = fechaNacimiento.split("/").map { it.toInt() }
            Triple(p[0], p[1], p[2])
        } else {
            val p = fechaNacimiento.split("-").map { it.toInt() }
            Triple(p[2], p[1], p[0])
        }
        val hoy = kotlinx.datetime.Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        val anios = hoy.year - anio
        val meses = hoy.monthNumber - mes
        (anios * 12 + meses).coerceAtLeast(0)
    } catch (_: Exception) { 0 }
}

fun esPantallaModuloInterno(screen: Screen): Boolean {
    return when (screen) {
        Screen.LACTANCIA, Screen.SOLIDOS, Screen.CRECIMIENTO, Screen.SUENO,
        Screen.MICRONUTRIENTES, Screen.NEURODESARROLLO, Screen.MEAL_PLANNING,
        Screen.CHAT_IA, Screen.DIARIO_VISUAL, Screen.RECORDATORIOS, Screen.NUTRIENTES,
        Screen.DIETA, Screen.CONFIGURACION, Screen.EDITAR_PERFIL, Screen.EDITAR_REGION,
        Screen.PEDIATRA_DASHBOARD, Screen.AYUDA, Screen.VINCULACION_GINECOLOGO,
        Screen.DIRECTORIO_GINECOLOGOS, Screen.NUTRICION_EMBARAZO, Screen.CITAS_EMBARAZO,
        Screen.PACIENTE_EXPEDIENTE, Screen.EXPEDIENTE_EMBARAZO -> true
        else -> false
    }
}

@Composable
fun AppiOS() {
    NutriIAiOSApp()
}

@Composable
fun NutriIAiOSApp() {
    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 1/11] Iniciando NutriIAiOSApp()...")

    val loginViewModel: LoginViewModel = remember {
        com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 2/11] Instanciando LoginViewModel()...")
        try {
            LoginViewModel()
        } catch (t: Throwable) {
            com.example.nutriia.platform.Log.e("AppiOS", "❌ Error instanciando LoginViewModel: ${t.message}", t)
            throw t
        }
    }

    val sharedVm: NutriSharedViewModel = remember {
        com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 3/11] Instanciando NutriSharedViewModel()...")
        try {
            NutriSharedViewModel()
        } catch (t: Throwable) {
            com.example.nutriia.platform.Log.e("AppiOS", "❌ Error instanciando NutriSharedViewModel: ${t.message}", t)
            throw t
        }
    }

    val teleconsultaVm: TeleconsultaViewModel = remember {
        com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 4/11] Instanciando TeleconsultaViewModel()...")
        try {
            TeleconsultaViewModel()
        } catch (t: Throwable) {
            com.example.nutriia.platform.Log.e("AppiOS", "❌ Error instanciando TeleconsultaViewModel: ${t.message}", t)
            throw t
        }
    }

    val accessibilityVm: AccessibilityViewModel = remember {
        com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 5/11] Instanciando AccessibilityViewModel()...")
        try {
            AccessibilityViewModel()
        } catch (t: Throwable) {
            com.example.nutriia.platform.Log.e("AppiOS", "❌ Error instanciando AccessibilityViewModel: ${t.message}", t)
            throw t
        }
    }

    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 6/11] Colectando StateFlows de Accesibilidad...")
    val accessibilityMode by accessibilityVm.mode.collectAsState()
    val primeraVez by accessibilityVm.primeraVez.collectAsState()
    val primeraVezCargada by accessibilityVm.primeraVezCargada.collectAsState()

    var isCheckingInitialSession by remember { mutableStateOf(true) }
    var showLoginSplash by remember { mutableStateOf(false) }
    var showResumeSplash by remember { mutableStateOf(false) }

    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    var children by remember { mutableStateOf<List<ChildProfile>>(emptyList()) }
    var activeChildIndex by remember { mutableIntStateOf(0) }
    val activeChild = children.getOrNull(activeChildIndex)

    var isAddingChild by remember { mutableStateOf(false) }
    var prefilledChildName by remember { mutableStateOf("") }
    var saltarAccesibilidadEnQuiz by remember { mutableStateOf(false) }
    var hijoParaEditar by remember { mutableStateOf<ChildProfile?>(null) }
    var pantallaOrigenConfig by remember { mutableStateOf(Screen.DASHBOARD_PARENT) }
    var perfilEmbarazo by remember { mutableStateOf<PerfilEmbarazo?>(null) }
    var nombreMama by remember { mutableStateOf("") }
    var semanasEmbarazo by remember { mutableIntStateOf(1) }
    var pacienteSeleccionado by remember { mutableStateOf<PacienteResumen?>(null) }

    val scope = rememberCoroutineScope()

    // ─── Control de Sesión Inicial y Primera Vez (Accesibilidad) ───────────
    LaunchedEffect(primeraVezCargada) {
        if (!primeraVezCargada) return@LaunchedEffect
        delay(1200) // Animación del Splash inicial
        if (primeraVez) {
            currentScreen = Screen.ACCESIBILIDAD_INICIAL
        } else {
            currentScreen = Screen.LOGIN
        }
        isCheckingInitialSession = false
    }

    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 7/11] Configurando ViewModelStore y ViewModelStoreOwner...")
    val viewModelStore = remember { androidx.lifecycle.ViewModelStore() }
    val viewModelStoreOwner = remember {
        object : androidx.lifecycle.ViewModelStoreOwner {
            override val viewModelStore: androidx.lifecycle.ViewModelStore = viewModelStore
        }
    }

    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 8/11] Entrando a NutriIATheme...")
    NutriIATheme {
        com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 9/11] Entrando a CompositionLocalProvider...")
        CompositionLocalProvider(
            androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner provides viewModelStoreOwner,
            LocalAccessibilityMode provides accessibilityMode
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 10/11] Renderizando pantalla: $currentScreen...")
                when (currentScreen) {
                    Screen.ACCESIBILIDAD_INICIAL -> OnboardingQuizScreen(
                        soloAccesibilidad = true,
                        onQuizComplete = { },
                        onAccesibilidadCompletada = {
                            accessibilityVm.marcarPrimeraVezCompletada()
                            currentScreen = Screen.LOGIN
                        }
                    )
                    Screen.LOGIN -> NutriaLoginScreen(
                        viewModel = loginViewModel,
                        a11yVm = accessibilityVm,
                        onNavigateAsParent = {
                            showLoginSplash = true
                            currentScreen = if (children.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                            scope.launch {
                                delay(1200)
                                showLoginSplash = false
                            }
                        },
                        onNavigateAsNutritionist = {
                            showLoginSplash = true
                            currentScreen = Screen.DASHBOARD_NUTRITIONIST
                            scope.launch {
                                delay(1200)
                                showLoginSplash = false
                            }
                        },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER_TYPE },
                        onNavigateToBiometricActivation = { _, _ -> currentScreen = Screen.BIOMETRIC_ACTIVATION }
                    )
                    Screen.REGISTER_TYPE -> RegisterTypeScreen(
                        onNavigateBack        = { currentScreen = Screen.LOGIN },
                        onSelectParent        = { currentScreen = Screen.REGISTER_PARENT },
                        onSelectNutritionist  = { currentScreen = Screen.REGISTER_NUTRITIONIST },
                        onSelectMamaPrimeriza = { currentScreen = Screen.REGISTER_MAMA_PRIMERIZA },
                        onSelectGinecologo    = { currentScreen = Screen.REGISTER_GINECOLOGO }
                    )
                    Screen.REGISTER_PARENT -> ParentRegisterScreen(
                        onNavigateBack    = { currentScreen = Screen.REGISTER_TYPE },
                        onRegisterSuccess = { data ->
                            prefilledChildName = data.childName
                            saltarAccesibilidadEnQuiz = true
                            currentScreen = Screen.QUIZ
                        }
                    )
                    Screen.REGISTER_MAMA_PRIMERIZA -> MamaPrimerizaRegisterScreen(
                        onNavigateBack    = { currentScreen = Screen.REGISTER_TYPE },
                        onRegisterSuccess = { data ->
                            nombreMama = data.name
                            semanasEmbarazo = data.semanas
                            currentScreen = Screen.QUIZ_MAMA_PRIMERIZA
                        }
                    )
                    Screen.REGISTER_NUTRITIONIST -> NutritionistRegisterScreen(
                        onNavigateBack    = { currentScreen = Screen.REGISTER_TYPE },
                        onRegisterSuccess = { currentScreen = Screen.DASHBOARD_NUTRITIONIST }
                    )
                    Screen.REGISTER_GINECOLOGO -> GinecologistRegisterScreen(
                        onNavigateBack    = { currentScreen = Screen.REGISTER_TYPE },
                        onRegisterSuccess = { currentScreen = Screen.DASHBOARD_GINECOLOGO }
                    )

                    // Quizzes
                    Screen.QUIZ -> OnboardingQuizScreen(
                        isAddingChild = isAddingChild,
                        prefilledChildName = prefilledChildName,
                        saltarAccesibilidad = saltarAccesibilidadEnQuiz,
                        onQuizComplete = { newProfile ->
                            children = children + newProfile
                            isAddingChild = false
                            prefilledChildName = ""
                            saltarAccesibilidadEnQuiz = false
                            activeChildIndex = children.lastIndex
                            loginViewModel.guardarHijo(newProfile)
                            currentScreen = Screen.DASHBOARD_PARENT
                        },
                        onCancel = {
                            isAddingChild = false
                            prefilledChildName = ""
                            saltarAccesibilidadEnQuiz = false
                            currentScreen = if (children.isEmpty()) Screen.LOGIN else Screen.DASHBOARD_PARENT
                        }
                    )
                    Screen.QUIZ_MAMA_PRIMERIZA -> EmbarazoQuizScreen(
                        semanasIniciales = semanasEmbarazo,
                        onQuizComplete = { perfil ->
                            perfilEmbarazo = perfil
                            currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                            loginViewModel.guardarPerfilEmbarazo(perfil)
                        },
                        onCancel = { currentScreen = Screen.LOGIN }
                    )

                    // Dashboards
                    Screen.DASHBOARD_PARENT -> NutriIADashboardScreen(
                        children = children,
                        initialPageIndex = activeChildIndex,
                        esNutriologo = false,
                        onPageChange = { index -> activeChildIndex = index },
                        onLogout = {
                            accessibilityVm.silenciar()
                            loginViewModel.cerrarSesion()
                            sharedVm.limpiarPerfil()
                            children = emptyList()
                            activeChildIndex = 0
                            saltarAccesibilidadEnQuiz = false
                            currentScreen = Screen.LOGIN
                        },
                        onConfiguracion = {
                            pantallaOrigenConfig = Screen.DASHBOARD_PARENT
                            currentScreen = Screen.CONFIGURACION
                        },
                        onAddChild = {
                            isAddingChild = true
                            saltarAccesibilidadEnQuiz = false
                            currentScreen = Screen.QUIZ
                        },
                        onOpenLactancia = { idx -> activeChildIndex = idx; currentScreen = Screen.LACTANCIA },
                        onOpenSolidos = { idx -> activeChildIndex = idx; currentScreen = Screen.SOLIDOS },
                        onOpenCrecimiento = { idx -> activeChildIndex = idx; currentScreen = Screen.CRECIMIENTO },
                        onOpenSueno = { idx -> activeChildIndex = idx; currentScreen = Screen.SUENO },
                        onOpenMicronutrientes = { idx -> activeChildIndex = idx; currentScreen = Screen.NUTRIENTES },
                        onOpenPediatra = { idx -> activeChildIndex = idx; currentScreen = Screen.PEDIATRA_DASHBOARD },
                        onOpenChatIA = { idx -> activeChildIndex = idx; currentScreen = Screen.CHAT_IA },
                        onOpenDiario = { idx -> activeChildIndex = idx; currentScreen = Screen.DIARIO_VISUAL },
                        onOpenRecordatorios = { idx -> activeChildIndex = idx; currentScreen = Screen.RECORDATORIOS },
                        onAyuda = { currentScreen = Screen.AYUDA }
                    )
                    Screen.DASHBOARD_NUTRITIONIST -> NutritionistDashboardScreen(
                        teleconsultaViewModel = teleconsultaVm,
                        onLogout = {
                            accessibilityVm.silenciar()
                            loginViewModel.cerrarSesion()
                            currentScreen = Screen.LOGIN
                        },
                        onPatientClick = { paciente ->
                            pacienteSeleccionado = paciente
                            currentScreen = Screen.PACIENTE_EXPEDIENTE
                        },
                        onNewPlan = {},
                        onViewAllPatients = {}
                    )
                    Screen.DASHBOARD_MAMA_PRIMERIZA -> {
                        val p = perfilEmbarazo
                        if (p != null) {
                            EmbarazoDashboardScreen(
                                nombreMama = nombreMama.ifBlank { loginViewModel.nombreUsuario },
                                perfil = p,
                                onLogout = {
                                    accessibilityVm.silenciar()
                                    loginViewModel.cerrarSesion()
                                    perfilEmbarazo = null
                                    nombreMama = ""
                                    currentScreen = Screen.LOGIN
                                },
                                onConfiguracion = {
                                    pantallaOrigenConfig = Screen.DASHBOARD_MAMA_PRIMERIZA
                                    currentScreen = Screen.CONFIGURACION
                                },
                                onOpenVinculacionGinecologo = { currentScreen = Screen.VINCULACION_GINECOLOGO },
                                onOpenChatBot = { currentScreen = Screen.CHAT_IA },
                                onOpenNutricion = { currentScreen = Screen.NUTRICION_EMBARAZO },
                                onOpenRecordatorios = { currentScreen = Screen.RECORDATORIOS },
                                onOpenCitas = { currentScreen = Screen.CITAS_EMBARAZO },
                                onOpenAnalisisIA = { currentScreen = Screen.DIARIO_VISUAL }
                            )
                        } else {
                            EmbarazoQuizScreen(
                                semanasIniciales = semanasEmbarazo,
                                onQuizComplete = { perfil ->
                                    perfilEmbarazo = perfil
                                    currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                                    loginViewModel.guardarPerfilEmbarazo(perfil)
                                },
                                onCancel = { currentScreen = Screen.LOGIN }
                            )
                        }
                    }
                    Screen.DASHBOARD_GINECOLOGO -> GinecologoDashboardScreen(
                        teleconsultaViewModel = teleconsultaVm,
                        onLogout = {
                            accessibilityVm.silenciar()
                            loginViewModel.cerrarSesion()
                            currentScreen = Screen.LOGIN
                        },
                        onPatientClick = { paciente ->
                            pacienteSeleccionado = PacienteResumen(
                                ownerUid = paciente.mamaUid,
                                childId = paciente.mamaUid,
                                childNombre = paciente.mamaNombre,
                                padreNombre = paciente.mamaNombre
                            )
                            currentScreen = Screen.EXPEDIENTE_EMBARAZO
                        },
                        onConfiguracion = {
                            pantallaOrigenConfig = Screen.DASHBOARD_GINECOLOGO
                            currentScreen = Screen.CONFIGURACION
                        }
                    )

                    // Módulos Clínicos
                    Screen.LACTANCIA -> activeChild?.let { child ->
                        LactanciaScreen(childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.SOLIDOS -> activeChild?.let { child ->
                        SolidosScreen(uid = loginViewModel.uidUsuario, childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }, sharedVm = sharedVm)
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.CRECIMIENTO -> activeChild?.let { child ->
                        CrecimientoScreen(childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), sexo = child.sexo, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.NUTRIENTES -> activeChild?.let { child ->
                        NutrientesScreen(childId = child.id, childName = child.name, mesesEdad = mesesDeVida(child.birthDate), onBack = { currentScreen = Screen.DASHBOARD_PARENT }, sharedVm = sharedVm)
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.CHAT_IA -> {
                        val rol = loginViewModel.rolUsuario
                        if (rol == "mama_primeriza") {
                            NutriChatScreen(childName = "Mi Bebé", onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }, onNavigateToAnalisis = null)
                        } else {
                            activeChild?.let { child ->
                                NutriChatScreen(childName = child.name, onBack = { currentScreen = Screen.DASHBOARD_PARENT }, onNavigateToAnalisis = { currentScreen = Screen.DIARIO_VISUAL })
                            } ?: run { currentScreen = Screen.DASHBOARD_PARENT }
                        }
                    }

                    Screen.DIARIO_VISUAL -> activeChild?.let { child ->
                        AnalisisScreen(child = child, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.RECORDATORIOS -> {
                        val rol = loginViewModel.rolUsuario
                        if (rol == "mama_primeriza") {
                            AlertasScreen(childId = null, childName = "Mi Embarazo", onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA })
                        } else {
                            activeChild?.let { child ->
                                AlertasScreen(childId = child.id, childName = child.name, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                            } ?: run { currentScreen = Screen.DASHBOARD_PARENT }
                        }
                    }

                    Screen.PEDIATRA_DASHBOARD -> activeChild?.let { child ->
                        PediatraScreen(
                            padreUid = loginViewModel.uidUsuario,
                            padreNombre = loginViewModel.nombreUsuario,
                            childId = child.id,
                            childNombre = child.name,
                            onAbrirPago = { _, _, _ -> },
                            onBack = { currentScreen = Screen.DASHBOARD_PARENT }
                        )
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.NUTRICION_EMBARAZO -> EmbarazoNutricionScreen(
                        perfil = PerfilEmbarazo(),
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )
                    Screen.CITAS_EMBARAZO -> CitasEmbarazoScreen(
                        teleconsultaViewModel = teleconsultaVm,
                        mamaUid = loginViewModel.uidUsuario,
                        mamaNombre = loginViewModel.nombreUsuario,
                        onAbrirPago = { _, _, _ -> },
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )
                    Screen.VINCULACION_GINECOLOGO -> VinculacionGinecologoScreen(
                        onNavigateToDirectorio = { currentScreen = Screen.DIRECTORIO_GINECOLOGOS },
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )
                    Screen.DIRECTORIO_GINECOLOGOS -> DirectorioGinecologosScreen(
                        mamaNombre = loginViewModel.nombreUsuario,
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA },
                        onVinculado = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )

                    Screen.PACIENTE_EXPEDIENTE -> pacienteSeleccionado?.let { pac ->
                        PacienteExpedienteScreen(
                            ownerUid = pac.ownerUid,
                            childId = pac.childId,
                            childNombre = pac.childNombre,
                            padreNombre = pac.padreNombre.ifBlank { "Tutor del Paciente" },
                            onBack = { currentScreen = Screen.DASHBOARD_NUTRITIONIST },
                            sharedViewModel = sharedVm
                        )
                    } ?: run { currentScreen = Screen.DASHBOARD_NUTRITIONIST }

                    Screen.EXPEDIENTE_EMBARAZO -> pacienteSeleccionado?.let { pac ->
                        PacienteExpedienteEmbarazoScreen(
                            mamaUid = pac.ownerUid,
                            mamaNombre = pac.childNombre,
                            onBack = { currentScreen = Screen.DASHBOARD_GINECOLOGO }
                        )
                    } ?: run { currentScreen = Screen.DASHBOARD_GINECOLOGO }

                    Screen.CONFIGURACION -> ConfiguracionScreen(
                        children = children,
                        nombrePadre = loginViewModel.nombreUsuario.ifBlank { "Usuario NutriIA" },
                        emailPadre = loginViewModel.emailUsuario.ifBlank { "usuario@nutriia.com" },
                        rol = loginViewModel.rolUsuario.ifBlank { "padre" },
                        onBack = { currentScreen = pantallaOrigenConfig },
                        onEditarPerfil = { currentScreen = Screen.EDITAR_PERFIL },
                        onEditarHijo = { child ->
                            hijoParaEditar = child
                            currentScreen = Screen.EDITAR_REGION
                        },
                        onAgregarHijo = {
                            isAddingChild = true
                            saltarAccesibilidadEnQuiz = false
                            currentScreen = Screen.QUIZ
                        },
                        onCerrarSesion = {
                            loginViewModel.cerrarSesion()
                            currentScreen = Screen.LOGIN
                        }
                    )
                    Screen.AYUDA -> HelpScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    Screen.BIOMETRIC_ACTIVATION -> BiometricActivationScreen(
                        uid = loginViewModel.uidUsuario,
                        rol = loginViewModel.rolUsuario,
                        onActivado = { currentScreen = Screen.DASHBOARD_PARENT },
                        onOmitido = { currentScreen = Screen.DASHBOARD_PARENT }
                    )
                    else -> NutriIADashboardScreen(
                        children = children,
                        initialPageIndex = activeChildIndex,
                        esNutriologo = false,
                        onPageChange = { index -> activeChildIndex = index },
                        onLogout = {
                            loginViewModel.cerrarSesion()
                            currentScreen = Screen.LOGIN
                        },
                        onConfiguracion = { currentScreen = Screen.CONFIGURACION },
                        onAddChild = { currentScreen = Screen.QUIZ },
                        onOpenLactancia = { currentScreen = Screen.LACTANCIA },
                        onOpenSolidos = { currentScreen = Screen.SOLIDOS },
                        onOpenCrecimiento = { currentScreen = Screen.CRECIMIENTO },
                        onOpenSueno = { currentScreen = Screen.SUENO },
                        onOpenMicronutrientes = { currentScreen = Screen.NUTRIENTES },
                        onOpenPediatra = { currentScreen = Screen.PEDIATRA_DASHBOARD },
                        onOpenChatIA = { currentScreen = Screen.CHAT_IA },
                        onOpenDiario = { currentScreen = Screen.DIARIO_VISUAL },
                        onOpenRecordatorios = { currentScreen = Screen.RECORDATORIOS },
                        onAyuda = { currentScreen = Screen.AYUDA }
                    )
                }

                // ─── Splash Overlay Animado (Arranque y Transiciones) ─────────
                AnimatedVisibility(
                    visible = showResumeSplash || showLoginSplash || isCheckingInitialSession,
                    enter = fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) + scaleIn(initialScale = 0.94f, animationSpec = tween(400, easing = FastOutSlowInEasing)),
                    exit = fadeOut(animationSpec = tween(450, easing = FastOutSlowInEasing)) + scaleOut(targetScale = 1.06f, animationSpec = tween(450, easing = FastOutSlowInEasing))
                ) {
                    SplashOverlay()
                }
            }
        }
    }
}

@Composable
private fun SplashOverlay(
    mensaje: String = "Iniciando NutriIA..."
) {
    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 11/11] Componiendo SplashOverlay...")
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    },
                contentAlignment = Alignment.Center
            ) {
                NutriaSplashMascota(modifier = Modifier.size(180.dp))
            }

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                color = Color(0xFF689F38),
                modifier = Modifier.size(44.dp),
                strokeWidth = 4.dp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = mensaje,
                color = Color(0xFF555555),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
