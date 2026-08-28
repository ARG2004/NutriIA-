package com.example.nutriia

import android.Manifest
import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableLongStateOf
import androidx.lifecycle.Lifecycle
import com.example.nutriia.seguridad.PlayIntegrityManager
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.*
import com.example.nutriia.alerta.AlertasScreen
import com.example.nutriia.analisisIA.AnalisisScreen
import com.example.nutriia.auth.*
import com.example.nutriia.configuracion.*
import com.example.nutriia.crecimiento.CrecimientoScreen
import com.example.nutriia.dashboard.NutriIADashboardScreen
import com.example.nutriia.dashboard.NutritionistDashboardScreen
import com.example.nutriia.embarazo.*
import com.example.nutriia.dashboard.PacienteResumen
import com.example.nutriia.expediente.PacienteExpedienteScreen
import com.example.nutriia.ayuda.HelpScreen
import com.example.nutriia.chatbot.NutriChatScreen
import com.example.nutriia.ginecologo.DirectorioGinecologosScreen
import com.example.nutriia.ginecologo.GinecologoDashboardScreen
import com.example.nutriia.ginecologo.GinecologoDashboardViewModel
import com.example.nutriia.ginecologo.GinecologoViewModel
import com.example.nutriia.ginecologo.VinculacionGinecologoScreen
import com.example.nutriia.ginecologo.VinculacionEmbarazo
import com.example.nutriia.ginecologo.PacienteExpedienteEmbarazoScreen
import com.example.nutriia.lactancia.LactanciaScreen
import com.example.nutriia.nutriente.NutrientesScreen
import com.example.nutriia.pediatra.PediatraScreen
import com.example.nutriia.shared.NutriSharedViewModel
import com.example.nutriia.solidos.SolidosScreen
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.ui.theme.NutriIATheme
import com.example.nutriia.teleconsulta.*
import com.example.nutriia.payment.*
import com.example.nutriia.payment.AISubscriptionViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope

// Pantallas de navegacion
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
        val hoy = java.util.Calendar.getInstance()
        val nac = java.util.Calendar.getInstance().apply { set(anio, mes - 1, dia) }
        val anios = hoy.get(java.util.Calendar.YEAR) - nac.get(java.util.Calendar.YEAR)
        val meses = hoy.get(java.util.Calendar.MONTH) - nac.get(java.util.Calendar.MONTH)
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

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        // Inicializar verificador de cédulas con contexto de aplicación
        com.example.nutriia.auth.CedulaVerifier.init(this)
        
        // Comentado para evitar el error de PERMISSION_DENIED en Firestore al intentar
        // migrar todas las cuentas sin permisos de administrador en el arranque.
        // La migración individual se ejecuta correctamente al iniciar sesión.
        // lifecycleScope.launch {
        //     try {
        //         com.example.nutriia.util.DateMigrationHelper.migrarAbsolutamenteTodo()
        //     } catch (_: Exception) {}
        // }
        
        setContent { 
            NutriIATheme { 
                NutriIAContent() 
            } 
        }

        // Si la app fue matada por el SO mientras estaba en el navegador,
        // el deep link llegará en el intent inicial.
        procesarIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        procesarIntent(intent)
    }

    private fun procesarIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            val factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory(application)
            val paymentVm = androidx.lifecycle.ViewModelProvider(this@MainActivity, factory).get(com.example.nutriia.payment.PaymentViewModel::class.java)
            paymentVm.procesarDeepLink(uri)

            val loginVm = androidx.lifecycle.ViewModelProvider(this@MainActivity, factory).get(com.example.nutriia.auth.LoginViewModel::class.java)
            val aiSubVm = androidx.lifecycle.ViewModelProvider(this@MainActivity, factory).get(com.example.nutriia.payment.AISubscriptionViewModel::class.java)
            aiSubVm.procesarDeepLink(uri.toString(), loginVm.uidUsuario)
        }
    }
}

@Composable
fun NutriIAContent() {
    val context = LocalContext.current
    val app     = context.applicationContext as Application
    val factory = ViewModelProvider.AndroidViewModelFactory(app)
    val scope   = rememberCoroutineScope()

    val loginViewModel:  LoginViewModel         = viewModel(factory = factory)
    val accessibilityVm: AccessibilityViewModel = viewModel(factory = factory)
    val sharedVm:        NutriSharedViewModel   = viewModel(factory = factory)
    val teleconsultaVm:  TeleconsultaViewModel  = viewModel(factory = factory)
    val paymentVm:       PaymentViewModel       = viewModel(factory = factory)
    val cfgVm:           ConfiguracionViewModel = viewModel(factory = ConfiguracionViewModelFactory(context))
    val gineVm:          GinecologoViewModel    = viewModel(factory = factory)
    val gineDashVm:      GinecologoDashboardViewModel = viewModel(factory = factory)

    var currentScreen             by rememberSaveable { mutableStateOf(Screen.LOGIN) }
    var children                  by remember { mutableStateOf<List<ChildProfile>>(emptyList()) }
    var isAddingChild             by rememberSaveable { mutableStateOf(false) }
    var prefilledChildName        by rememberSaveable { mutableStateOf("") }
    var activeChildIndex          by rememberSaveable { mutableIntStateOf(0) }
    var saltarAccesibilidadEnQuiz by rememberSaveable { mutableStateOf(false) }
    var pantallaOrigenConfig      by rememberSaveable { mutableStateOf(Screen.DASHBOARD_PARENT) }
    var hijoParaEditar            by remember { mutableStateOf<ChildProfile?>(null) }
    var pacienteSeleccionado      by remember { mutableStateOf<PacienteResumen?>(null) }
    var iniciarLlamadaTrasExito   by rememberSaveable { mutableStateOf(false) }
    
    // Variables para Mamá Primeriza
    var semanasEmbarazo           by rememberSaveable { mutableIntStateOf(1) }
    var nombreMama                by rememberSaveable { mutableStateOf("") }
    var perfilEmbarazo            by rememberSaveable { mutableStateOf<PerfilEmbarazo?>(null) }
    var pacienteEmbarazoSeleccionado by remember { mutableStateOf<VinculacionEmbarazo?>(null) }

    val hasActiveSession = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null
    var isCheckingInitialSession by remember { mutableStateOf(hasActiveSession) }
    var showResumeSplash by remember { mutableStateOf(false) }
    var showLoginSplash by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    var lastBackgroundTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                lastBackgroundTime = System.currentTimeMillis()
            } else if (event == Lifecycle.Event.ON_START) {
                if (lastBackgroundTime != 0L) {
                    val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    // OMITIR splash si estamos en medio de un flujo de pago/llamada
                    val enFlujoLlamada = iniciarLlamadaTrasExito || teleconsultaVm.state.value.llamadaActual != null
                    if (user != null && !enFlujoLlamada) {
                        showResumeSplash = true
                    }
                }
                lastBackgroundTime = 0L
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(showResumeSplash, iniciarLlamadaTrasExito) {
        if (showResumeSplash) {
            if (iniciarLlamadaTrasExito) {
                showResumeSplash = false
            } else {
                delay(1500)
                showResumeSplash = false
            }
        }
    }

    LaunchedEffect(Unit) {
        PlayIntegrityManager.verificarIntegridadInicial(context)
    }

    var pagoNutriologoUid    by rememberSaveable { mutableStateOf("") }
    var pagoNutriologoNombre by rememberSaveable { mutableStateOf("") }
    var pagoTipoLlamada      by rememberSaveable { mutableStateOf(TipoLlamada.VIDEO) }
    var pagoIdExitoso        by rememberSaveable { mutableStateOf("") }

    val estado            by loginViewModel.estado.collectAsState()
    val accessibilityMode by accessibilityVm.mode.collectAsState()
    val primeraVez        by accessibilityVm.primeraVez.collectAsState()
    val primeraVezCargada by accessibilityVm.primeraVezCargada.collectAsState()

    val activeChild: ChildProfile? = children.getOrNull(activeChildIndex)
    LaunchedEffect(activeChild?.id) {
        activeChild?.id?.let { id ->
            if (id.isNotBlank()) {
                // Usar uid de LoginViewModel si está disponible, si no (registro nuevo) usar FirebaseAuth directamente
                val uid = loginViewModel.uidUsuario.ifBlank {
                    FirebaseAuth.getInstance().currentUser?.uid ?: ""
                }
                if (uid.isNotBlank()) sharedVm.cargarPerfil(uid, id)
            }
        }
    }

    var permissionStep by remember { mutableStateOf(0) }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        if (otorgado) accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Microphone enabled." else "Micrófono habilitado.")
        else accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Microphone denied." else "Micrófono denegado.")
        permissionStep = 2
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        if (otorgado) accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Camera enabled." else "Cámara habilitada.")
        else accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Camera denied." else "Cámara denegada.")
        permissionStep = 3
    }

    val phoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultados ->
        val otg = resultados.values.all { it }
        if (otg) accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Phone permissions enabled." else "Permisos telefónicos habilitados.")
        else accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Phone permissions denied." else "Permisos telefónicos denegados.")
        permissionStep = 4
    }

    val nearDevicesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { resultados ->
        val otg = resultados.values.all { it }
        if (otg) accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Nearby devices permission enabled." else "Permiso de dispositivos cercanos habilitado.")
        else accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Nearby devices permission denied." else "Permiso de dispositivos cercanos denegado.")
        permissionStep = 5
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { otorgado ->
        if (otorgado) accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Microphone enabled." else "Micrófono habilitado.")
        else accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Microphone denied." else "Micrófono denegado.")
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    LaunchedEffect(accessibilityMode, currentScreen) {
        if (accessibilityMode == AccessibilityMode.BLIND) {
            if (currentScreen == Screen.ACCESIBILIDAD_INICIAL) {
                permissionStep = 1
            } else {
                val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                if (permiso != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val msg = if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) {
                        "Please grant microphone permission to enable voice input."
                    } else {
                        "Por favor, concede el permiso de micrófono para activar el control por voz."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 500L)
                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        } else if (accessibilityMode == AccessibilityMode.MUTE) {
            val permiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
            if (permiso != android.content.pm.PackageManager.PERMISSION_GRANTED) cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(permissionStep, accessibilityMode, currentScreen) {
        if (accessibilityMode != AccessibilityMode.BLIND || currentScreen != Screen.ACCESIBILIDAD_INICIAL) return@LaunchedEffect

        val isIngles = accessibilityVm.idioma.value == IdiomaVoz.INGLES

        when (permissionStep) {
            1 -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionStep = 2
                } else {
                    val msg = if (isIngles) {
                        "First, I will ask for microphone permission so you can input information by voice and speak to the assistant. Please press Allow on the prompt."
                    } else {
                        "Primero, te pediré el permiso de micrófono para que puedas dictar información por voz y hablar con el asistente. Por favor, selecciona Permitir en la pantalla."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
            2 -> {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    permissionStep = 3
                } else {
                    val msg = if (isIngles) {
                        "Next is camera permission for video consultations with pediatricians and gynecologists. Please press Allow."
                    } else {
                        "Ahora te pediré el permiso de cámara para las teleconsultas en video con pediatras y ginecólogos. Por favor, selecciona Permitir."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
            }
            3 -> {
                val phonePermissions = listOf(Manifest.permission.READ_PHONE_STATE, Manifest.permission.CALL_PHONE)
                val hasPhone = phonePermissions.all { ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
                if (hasPhone) {
                    permissionStep = 4
                } else {
                    val msg = if (isIngles) {
                        "Next is phone permission to establish direct calls with specialists. Please press Allow."
                    } else {
                        "El siguiente es el permiso de teléfono para establecer llamadas directamente con especialistas. Por favor, selecciona Permitir."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        phoneLauncher.launch(phonePermissions.toTypedArray())
                    }
                }
            }
            4 -> {
                val nearPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                } else {
                    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                }
                val hasNear = nearPermissions.all { ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED }
                if (hasNear) {
                    permissionStep = 5
                } else {
                    val msg = if (isIngles) {
                        "Finally, I will ask for nearby devices permission to allow device synchronization. Please press Allow."
                    } else {
                        "Por último, te pediré el permiso de dispositivos cercanos para permitir la sincronización. Por favor, selecciona Permitir."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        nearDevicesLauncher.launch(nearPermissions.toTypedArray())
                    }
                }
            }
            5 -> {
                val msg = if (isIngles) "All set. Permissions configuration completed." else "Todo listo. Configuración de permisos completada."
                accessibilityVm.hablar(msg)
            }
        }
    }

    LaunchedEffect(Unit) {
        if (isTalkBackActive(context)) accessibilityVm.setMode(AccessibilityMode.BLIND)
    }

    LaunchedEffect(currentScreen) {
        if (esPantallaModuloInterno(currentScreen)) {
            SessionManager.guardarUltimaPantalla(context, currentScreen.name)
        }
    }

    LaunchedEffect(primeraVezCargada) {
        if (!primeraVezCargada) return@LaunchedEffect
        loginViewModel.verificarSesion { rol, hijos ->
            val ultimaGuardada = SessionManager.obtenerUltimaPantalla(context)
            val pantallaRestaurada = try {
                if (ultimaGuardada != null) Screen.valueOf(ultimaGuardada) else null
            } catch (_: Exception) { null }

            when (rol) {
                "nutriologo" -> {
                    currentScreen = if (pantallaRestaurada != null && esPantallaModuloInterno(pantallaRestaurada)) pantallaRestaurada else Screen.DASHBOARD_NUTRITIONIST
                    accessibilityVm.sincronizarDesdeFirebase()
                    if (isTalkBackActive(context)) accessibilityVm.setMode(AccessibilityMode.BLIND)
                    teleconsultaVm.iniciarObservacionEntrantesNutriologo(loginViewModel.uidUsuario)
                }
                "ginecologo" -> {
                    currentScreen = if (pantallaRestaurada != null && esPantallaModuloInterno(pantallaRestaurada)) pantallaRestaurada else Screen.DASHBOARD_GINECOLOGO
                    accessibilityVm.sincronizarDesdeFirebase()
                    if (isTalkBackActive(context)) accessibilityVm.setMode(AccessibilityMode.BLIND)
                    teleconsultaVm.iniciarObservacionEntrantesNutriologo(loginViewModel.uidUsuario)
                }
                "mama_primeriza" -> {
                    if (nombreMama.isBlank()) nombreMama = loginViewModel.nombreUsuario
                    accessibilityVm.sincronizarDesdeFirebase()
                    if (isTalkBackActive(context)) accessibilityVm.setMode(AccessibilityMode.BLIND)
                    teleconsultaVm.iniciarObservacionEntrantes(loginViewModel.uidUsuario)
                    scope.launch {
                        val p = loginViewModel.cargarPerfilEmbarazo()
                        if (p != null) {
                            perfilEmbarazo = p
                            currentScreen = if (pantallaRestaurada != null && esPantallaModuloInterno(pantallaRestaurada)) pantallaRestaurada else Screen.DASHBOARD_MAMA_PRIMERIZA
                        } else {
                            currentScreen = Screen.QUIZ_MAMA_PRIMERIZA
                        }
                        isCheckingInitialSession = false
                    }
                }
                "padre" -> {
                    children = hijos
                    saltarAccesibilidadEnQuiz = true
                    val dashboardDefault = if (hijos.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                    currentScreen = if (pantallaRestaurada != null && esPantallaModuloInterno(pantallaRestaurada)) {
                        pantallaRestaurada
                    } else {
                        dashboardDefault
                    }
                    accessibilityVm.sincronizarDesdeFirebase()
                    if (isTalkBackActive(context)) accessibilityVm.setMode(AccessibilityMode.BLIND)
                    teleconsultaVm.iniciarObservacionEntrantes(loginViewModel.uidUsuario)
                }
                else -> currentScreen = if (primeraVez) Screen.ACCESIBILIDAD_INICIAL else Screen.LOGIN
            }
            if (rol != "mama_primeriza") {
                isCheckingInitialSession = false
            }
        }
    }

    LaunchedEffect(estado) {
        val s = estado
        if (s is LoginUiState.Exito) {
            if (esPantallaModuloInterno(currentScreen)) {
                return@LaunchedEffect
            }
            // 1. Mostrar SplashOverlay inmediatamente para cubrir la pantalla de login
            showLoginSplash = true

            scope.launch {
                com.example.nutriia.util.DateMigrationHelper.migrarTodosLosDatos()
            }

            // 2. Breve espera para asegurar que el SplashOverlay cubra completamente la interfaz
            delay(120)

            // 3. Cambiar de pantalla mientras la pantalla está 100% cubierta por el SplashOverlay
            val uid = loginViewModel.uidUsuario
            val yaActivoHuella = SessionManager.obtenerUid(context) != null

            if (!yaActivoHuella && BiometricHelper.isAvailable(context)) {
                currentScreen = Screen.BIOMETRIC_ACTIVATION
            } else {
                when (s.rol) {
                    "nutriologo" -> {
                        currentScreen = Screen.DASHBOARD_NUTRITIONIST
                        teleconsultaVm.iniciarObservacionEntrantesNutriologo(loginViewModel.uidUsuario)
                    }
                    "ginecologo" -> {
                        currentScreen = Screen.DASHBOARD_GINECOLOGO
                        teleconsultaVm.iniciarObservacionEntrantesNutriologo(loginViewModel.uidUsuario)
                    }
                    "mama_primeriza" -> {
                        if (nombreMama.isBlank()) nombreMama = loginViewModel.nombreUsuario
                        teleconsultaVm.iniciarObservacionEntrantes(loginViewModel.uidUsuario)
                        val p = loginViewModel.cargarPerfilEmbarazo()
                        if (p != null) {
                            perfilEmbarazo = p
                            currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                        } else {
                            currentScreen = Screen.QUIZ_MAMA_PRIMERIZA
                        }
                    }
                    else -> {
                        children = s.hijos
                        saltarAccesibilidadEnQuiz = true
                        currentScreen = if (s.hijos.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                        teleconsultaVm.iniciarObservacionEntrantes(loginViewModel.uidUsuario)
                    }
                }
            }

            // 4. Mantener el splash visible mientras el Dashboard carga completamente en segundo plano
            delay(1380)
            showLoginSplash = false

            loginViewModel.resetEstado()
        }
    }

    CompositionLocalProvider(LocalAccessibilityMode provides accessibilityMode) {
        when (currentScreen) {
            Screen.ACCESIBILIDAD_INICIAL -> OnboardingQuizScreen(soloAccesibilidad = true, onQuizComplete = { }, onAccesibilidadCompletada = { accessibilityVm.marcarPrimeraVezCompletada(); currentScreen = Screen.REGISTER_TYPE })
            Screen.LOGIN -> NutriaLoginScreen(
                viewModel = loginViewModel, 
                onNavigateAsParent = { currentScreen = if (children.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT }, 
                onNavigateAsNutritionist = { currentScreen = Screen.DASHBOARD_NUTRITIONIST }, 
                onNavigateToRegister = { currentScreen = Screen.REGISTER_TYPE },
                onNavigateToBiometricActivation = { _, _ -> currentScreen = Screen.BIOMETRIC_ACTIVATION }
            )
            Screen.REGISTER_TYPE -> RegisterTypeScreen(
                onNavigateBack = { currentScreen = Screen.LOGIN },
                onSelectParent = { currentScreen = Screen.REGISTER_PARENT },
                onSelectNutritionist = { currentScreen = Screen.REGISTER_NUTRITIONIST },
                onSelectMamaPrimeriza = { currentScreen = Screen.REGISTER_MAMA_PRIMERIZA },
                onSelectGinecologo = { currentScreen = Screen.REGISTER_GINECOLOGO }
            )
            Screen.REGISTER_PARENT -> ParentRegisterScreen(onNavigateBack = { currentScreen = Screen.REGISTER_TYPE }, onRegisterSuccess = { data -> prefilledChildName = data.childName; saltarAccesibilidadEnQuiz = true; currentScreen = Screen.QUIZ })
            
            Screen.REGISTER_MAMA_PRIMERIZA -> MamaPrimerizaRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegisterSuccess = { data ->
                    nombreMama = data.name
                    semanasEmbarazo = data.semanas
                    currentScreen = Screen.QUIZ_MAMA_PRIMERIZA
                }
            )

            Screen.REGISTER_NUTRITIONIST -> NutritionistRegisterScreen(onNavigateBack = { currentScreen = Screen.REGISTER_TYPE }, onRegisterSuccess = { currentScreen = Screen.DASHBOARD_NUTRITIONIST })
            
            Screen.REGISTER_GINECOLOGO -> GinecologistRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegisterSuccess = { currentScreen = Screen.DASHBOARD_GINECOLOGO }
            )

            Screen.QUIZ -> OnboardingQuizScreen(isAddingChild = isAddingChild, prefilledChildName = prefilledChildName, saltarAccesibilidad = saltarAccesibilidadEnQuiz, onQuizComplete = { newProfile -> children = children + newProfile; isAddingChild = false; prefilledChildName = ""; saltarAccesibilidadEnQuiz = false; activeChildIndex = children.lastIndex; loginViewModel.guardarHijo(newProfile); currentScreen = Screen.DASHBOARD_PARENT }, onCancel = { isAddingChild = false; prefilledChildName = ""; saltarAccesibilidadEnQuiz = false; currentScreen = if (children.isEmpty()) Screen.LOGIN else Screen.DASHBOARD_PARENT })
            
            Screen.QUIZ_MAMA_PRIMERIZA -> {
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
            
            Screen.DASHBOARD_PARENT -> NutriIADashboardScreen(children = children, initialPageIndex = activeChildIndex, esNutriologo = false, onPageChange = { index -> activeChildIndex = index }, onLogout = { accessibilityVm.silenciar(); loginViewModel.cerrarSesion(); sharedVm.limpiarPerfil(); children = emptyList(); activeChildIndex = 0; saltarAccesibilidadEnQuiz = false; currentScreen = Screen.LOGIN }, onConfiguracion = { pantallaOrigenConfig = Screen.DASHBOARD_PARENT; currentScreen = Screen.CONFIGURACION }, onAddChild = { isAddingChild = true; saltarAccesibilidadEnQuiz = false; currentScreen = Screen.QUIZ }, onOpenLactancia = { idx -> activeChildIndex = idx; currentScreen = Screen.LACTANCIA }, onOpenSolidos = { idx -> activeChildIndex = idx; currentScreen = Screen.SOLIDOS }, onOpenCrecimiento = { idx -> activeChildIndex = idx; currentScreen = Screen.CRECIMIENTO }, onOpenSueno = { idx -> activeChildIndex = idx; currentScreen = Screen.SUENO }, onOpenMicronutrientes = { idx -> activeChildIndex = idx; currentScreen = Screen.NUTRIENTES }, onOpenPediatra = { idx -> activeChildIndex = idx; currentScreen = Screen.PEDIATRA_DASHBOARD }, onOpenChatIA = { idx -> activeChildIndex = idx; currentScreen = Screen.CHAT_IA }, onOpenDiario = { idx -> activeChildIndex = idx; currentScreen = Screen.DIARIO_VISUAL }, onOpenRecordatorios = { idx -> activeChildIndex = idx; currentScreen = Screen.RECORDATORIOS }, onAyuda = { currentScreen = Screen.AYUDA })
            Screen.DASHBOARD_NUTRITIONIST -> NutritionistDashboardScreen(teleconsultaViewModel = teleconsultaVm, onLogout = { accessibilityVm.silenciar(); loginViewModel.cerrarSesion(); currentScreen = Screen.LOGIN }, onPatientClick = { paciente -> pacienteSeleccionado = paciente; currentScreen = Screen.PACIENTE_EXPEDIENTE }, onNewPlan = {}, onViewAllPatients = {})
            
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
                    LaunchedEffect(Unit) {
                        val loaded = loginViewModel.cargarPerfilEmbarazo()
                        if (loaded != null) {
                            perfilEmbarazo = loaded
                        } else {
                            currentScreen = Screen.QUIZ_MAMA_PRIMERIZA
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFFEC9BBF))
                    }
                }
            }

            Screen.NUTRICION_EMBARAZO -> {
                val p = perfilEmbarazo
                if (p != null) {
                    EmbarazoNutricionScreen(
                        perfil = p,
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )
                } else {
                    currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                }
            }

            Screen.DASHBOARD_GINECOLOGO -> GinecologoDashboardScreen(
                viewModel = gineDashVm,
                teleconsultaViewModel = teleconsultaVm,
                onLogout = {
                    teleconsultaVm.detenerObservacionEntrantes()
                    accessibilityVm.silenciar()
                    loginViewModel.cerrarSesion()
                    currentScreen = Screen.LOGIN
                },
                onConfiguracion = { pantallaOrigenConfig = Screen.DASHBOARD_GINECOLOGO; currentScreen = Screen.CONFIGURACION },
                onPatientClick = { paciente ->
                    pacienteEmbarazoSeleccionado = paciente
                    currentScreen = Screen.EXPEDIENTE_EMBARAZO
                }
            )
            
            Screen.VINCULACION_GINECOLOGO -> VinculacionGinecologoScreen(
                viewModel = gineVm,
                onNavigateToDirectorio = { currentScreen = Screen.DIRECTORIO_GINECOLOGOS },
                onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
            )
            
            Screen.DIRECTORIO_GINECOLOGOS -> DirectorioGinecologosScreen(
                viewModel = gineVm,
                mamaNombre = nombreMama.ifBlank { loginViewModel.nombreUsuario },
                onBack = { currentScreen = Screen.VINCULACION_GINECOLOGO },
                onVinculado = { currentScreen = Screen.VINCULACION_GINECOLOGO }
            )

            Screen.CITAS_EMBARAZO -> {
                CitasEmbarazoScreen(
                    viewModel = gineVm,
                    teleconsultaViewModel = teleconsultaVm,
                    mamaUid = loginViewModel.uidUsuario,
                    mamaNombre = loginViewModel.nombreUsuario,
                    iniciarLlamadaAlEntrar = if (iniciarLlamadaTrasExito) pagoTipoLlamada else null,
                    pagoIdExitoso = pagoIdExitoso,
                    onLlamadaIniciada = { iniciarLlamadaTrasExito = false; pagoIdExitoso = "" },
                    onAbrirPago = { ginecologoUid, ginecologoNombre, tipo ->
                        pagoNutriologoUid = ginecologoUid
                        pagoNutriologoNombre = ginecologoNombre
                        pagoTipoLlamada = tipo
                        currentScreen = Screen.PAGO_TELECONSULTA
                    },
                    onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                )
            }

            Screen.PACIENTE_EXPEDIENTE -> { pacienteSeleccionado?.let { paciente -> PacienteExpedienteScreen(ownerUid = paciente.ownerUid, childId = paciente.childId, childNombre = paciente.childNombre, padreNombre = paciente.padreNombre, onBack = { currentScreen = Screen.DASHBOARD_NUTRITIONIST }) } ?: run { currentScreen = Screen.DASHBOARD_NUTRITIONIST } }
            Screen.EXPEDIENTE_EMBARAZO -> {
                pacienteEmbarazoSeleccionado?.let { pac ->
                    PacienteExpedienteEmbarazoScreen(
                        mamaUid = pac.mamaUid,
                        mamaNombre = pac.mamaNombre,
                        onBack = { currentScreen = Screen.DASHBOARD_GINECOLOGO }
                    )
                } ?: run {
                    currentScreen = Screen.DASHBOARD_GINECOLOGO
                }
            }
            Screen.PEDIATRA_DASHBOARD -> { activeChild?.let { child -> PediatraScreen(teleconsultaViewModel = teleconsultaVm, padreUid = loginViewModel.uidUsuario, padreNombre = loginViewModel.nombreUsuario, childId = child.id, childNombre = child.name, iniciarLlamadaAlEntrar = if (iniciarLlamadaTrasExito) pagoTipoLlamada else null, pagoNutriologoUid = pagoNutriologoUid, pagoNutriologoNombre = pagoNutriologoNombre, pagoIdExitoso = pagoIdExitoso, padreNombreCompleto = loginViewModel.nombreUsuario, onLlamadaIniciada = { iniciarLlamadaTrasExito = false; pagoIdExitoso = "" }, onAbrirPago = { nutriologoUid, nutriologoNombre, tipo -> pagoNutriologoUid = nutriologoUid; pagoNutriologoNombre = nutriologoNombre; pagoTipoLlamada = tipo; currentScreen = Screen.PAGO_TELECONSULTA }, onBack = { currentScreen = Screen.DASHBOARD_PARENT }) } ?: run { currentScreen = Screen.DASHBOARD_PARENT } }
            Screen.PAGO_TELECONSULTA -> {
                val rol = loginViewModel.rolUsuario
                if (rol == "mama_primeriza") {
                    PaymentGateScreen(
                        viewModel = paymentVm,
                        nutriologoUid = pagoNutriologoUid,
                        nutriologoNombre = pagoNutriologoNombre,
                        childId = "embarazo",
                        childNombre = "Embarazo",
                        onPagoConfirmado = {
                            iniciarLlamadaTrasExito = true
                            pagoIdExitoso = paymentVm.state.value.pagoActual?.id ?: ""
                            paymentVm.resetPago()
                            currentScreen = Screen.CITAS_EMBARAZO
                        },
                        onCancelar = { currentScreen = Screen.CITAS_EMBARAZO }
                    )
                } else {
                    activeChild?.let { child ->
                        PaymentGateScreen(
                            viewModel = paymentVm,
                            nutriologoUid = pagoNutriologoUid,
                            nutriologoNombre = pagoNutriologoNombre,
                            childId = child.id,
                            childNombre = child.name,
                            onPagoConfirmado = {
                                iniciarLlamadaTrasExito = true
                                pagoIdExitoso = paymentVm.state.value.pagoActual?.id ?: ""
                                paymentVm.resetPago()
                                currentScreen = Screen.PEDIATRA_DASHBOARD
                            },
                            onCancelar = { currentScreen = Screen.PEDIATRA_DASHBOARD }
                        )
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }
                }
            }
            Screen.CONFIGURACION -> {
                val logoutAction = {
                    teleconsultaVm.detenerObservacionEntrantes()
                    accessibilityVm.silenciar()
                    loginViewModel.cerrarSesion()
                    sharedVm.limpiarPerfil()
                    children = emptyList()
                    activeChildIndex = 0
                    saltarAccesibilidadEnQuiz = false
                    perfilEmbarazo = null
                    nombreMama = ""
                    currentScreen = Screen.LOGIN
                }
                ConfiguracionScreen(
                    children                    = children,
                    nombrePadre                 = loginViewModel.nombreUsuario,
                    emailPadre                  = loginViewModel.emailUsuario,
                    rol                         = loginViewModel.rolUsuario,
                    onBack                      = { currentScreen = pantallaOrigenConfig },
                    onEditarPerfil              = { currentScreen = Screen.EDITAR_PERFIL },
                    onCambiarPasswordDirecto    = { actual, nueva, callback ->
                        cfgVm.cambiarContrasenaDirecta(actual, nueva, callback)
                    },
                    onEnviarCorreoPassword      = {
                        cfgVm.enviarRecuperacionPassword(loginViewModel.emailUsuario)
                    },
                    onEditarHijo                = { child -> hijoParaEditar = child; currentScreen = Screen.EDITAR_REGION },
                    onAgregarHijo               = { isAddingChild = true; saltarAccesibilidadEnQuiz = true; currentScreen = Screen.QUIZ },
                    onPrivacidad                = { },
                    onCerrarSesion              = { cfgVm.cerrarSesion { logoutAction() } },
                    onEliminarCuentaConPassword = { actual, callback ->
                        cfgVm.eliminarCuentaDefinitiva(actual) { exito, msg ->
                            callback(exito, msg)
                            if (exito) {
                                logoutAction()
                            }
                        }
                    }
                )
            }
            Screen.EDITAR_PERFIL -> EditarPerfilScreen(
                nombreInicial   = loginViewModel.nombreUsuario,
                emailInicial    = loginViewModel.emailUsuario,
                telefonoInicial = loginViewModel.telefonoUsuario,
                onBack          = { currentScreen = Screen.CONFIGURACION },
                onGuardar       = { nombre, email, telefono ->
                    loginViewModel.actualizarPerfil(nombre, email, telefono)
                    android.widget.Toast.makeText(
                        context,
                        if (telefono != loginViewModel.telefonoUsuario) "Teléfono actualizado exitosamente" else "Perfil actualizado exitosamente",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    currentScreen = Screen.CONFIGURACION
                }
            )
            Screen.EDITAR_REGION -> { val hijo = hijoParaEditar; if (hijo == null) { currentScreen = Screen.CONFIGURACION } else { OnboardingQuizScreen(isAddingChild = false, saltarAccesibilidad = true, initialStep = 6, prefilledProfile = hijo, onQuizComplete = { perfilActualizado -> val hijoActualizado = hijo.copy(nivelIngreso = perfilActualizado.nivelIngreso, region = perfilActualizado.region); loginViewModel.guardarHijo(hijoActualizado) { exito -> if (exito) { children = children.map { c: ChildProfile -> if (c.id == hijoActualizado.id) hijoActualizado else c }; loginViewModel.recargarHijos() } }; hijoParaEditar = null; currentScreen = Screen.CONFIGURACION }, onCancel = { hijoParaEditar = null; currentScreen = Screen.CONFIGURACION }) } }
            Screen.LACTANCIA -> { activeChild?.let { child -> LactanciaScreen(childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }) } ?: run { currentScreen = Screen.DASHBOARD_PARENT } }
            Screen.SOLIDOS -> { activeChild?.let { child -> SolidosScreen(uid = loginViewModel.uidUsuario, childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }, sharedVm = sharedVm) } ?: run { currentScreen = Screen.DASHBOARD_PARENT } }
            Screen.CRECIMIENTO -> { activeChild?.let { child -> CrecimientoScreen(childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), sexo = child.sexo, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }) } ?: run { currentScreen = Screen.DASHBOARD_PARENT } }
            Screen.NUTRIENTES -> { activeChild?.let { child -> NutrientesScreen(childId = child.id, childName = child.name, mesesEdad = mesesDeVida(child.birthDate), onBack = { currentScreen = Screen.DASHBOARD_PARENT }, sharedVm = sharedVm) } ?: run { currentScreen = Screen.DASHBOARD_PARENT } }
            Screen.RECORDATORIOS -> {
                val rol = loginViewModel.rolUsuario
                if (rol == "mama_primeriza") {
                    AlertasScreen(
                        childId = null,
                        childName = "Mi Embarazo",
                        onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )
                } else {
                    activeChild?.let { child ->
                        AlertasScreen(
                            childId = child.id,
                            childName = child.name,
                            onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
                        )
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }
                }
            }
            Screen.DIARIO_VISUAL -> {
                val rol = loginViewModel.rolUsuario
                val esEmbarazoUser = (rol == "mama_primeriza" || perfilEmbarazo != null)
                if (esEmbarazoUser) {
                    AnalisisScreen(
                        perfilEmbarazo = perfilEmbarazo,
                        isEmbarazo = true,
                        onNavigateBack = {
                            if (rol == "mama_primeriza") {
                                currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                            } else {
                                currentScreen = Screen.DASHBOARD_PARENT
                            }
                        }
                    )
                } else {
                    activeChild?.let { child ->
                        AnalisisScreen(
                            child = child,
                            isEmbarazo = false,
                            onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
                        )
                    } ?: run {
                        currentScreen = Screen.DASHBOARD_PARENT
                    }
                }
            }
            Screen.CHAT_IA -> {
                val rol = loginViewModel.rolUsuario
                val esEmbarazoUser = (rol == "mama_primeriza" || perfilEmbarazo != null)
                if (esEmbarazoUser) {
                    NutriChatScreen(
                        childName = "Mi Embarazo",
                        perfilEmbarazo = perfilEmbarazo,
                        isEmbarazo = true,
                        onBack = {
                            if (rol == "mama_primeriza") {
                                currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                            } else {
                                currentScreen = Screen.DASHBOARD_PARENT
                            }
                        },
                        onNavigateToAnalisis = { currentScreen = Screen.DIARIO_VISUAL }
                    )
                } else {
                    activeChild?.let { child ->
                        NutriChatScreen(
                            childName = child.name,
                            perfilEmbarazo = null,
                            isEmbarazo = false,
                            onBack = { currentScreen = Screen.DASHBOARD_PARENT },
                            onNavigateToAnalisis = { currentScreen = Screen.DIARIO_VISUAL }
                        )
                    } ?: run {
                        currentScreen = Screen.DASHBOARD_PARENT
                    }
                }
            }
            Screen.AYUDA -> HelpScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
            
            Screen.BIOMETRIC_ACTIVATION -> {
                BiometricActivationScreen(
                    uid = loginViewModel.uidUsuario,
                    rol = loginViewModel.rolUsuario,
                    onActivado = {
                        val rol = loginViewModel.rolUsuario
                        currentScreen = when (rol) {
                            "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                            "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                            "mama_primeriza" -> Screen.DASHBOARD_MAMA_PRIMERIZA
                            else -> if (children.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                        }
                    },
                    onOmitido = {
                        val rol = loginViewModel.rolUsuario
                        currentScreen = when (rol) {
                            "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                            "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                            "mama_primeriza" -> Screen.DASHBOARD_MAMA_PRIMERIZA
                            else -> if (children.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                        }
                    }
                )
            }
            else -> {
                // Manejo de pantallas faltantes (SUENO, MICRONUTRIENTES, etc.)
                Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
                    Text("Pantalla en desarrollo: $currentScreen")
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(
            visible = showResumeSplash || showLoginSplash || isCheckingInitialSession,
            enter = androidx.compose.animation.fadeIn(
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.scaleIn(
                initialScale = 0.94f,
                animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ),
            exit = androidx.compose.animation.fadeOut(
                animationSpec = androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            ) + androidx.compose.animation.scaleOut(
                targetScale = 1.06f,
                animationSpec = androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.FastOutSlowInEasing)
            )
        ) {
            SplashOverlay()
        }

        // Overlay de llamada al final para máxima prioridad visual
        TeleconsultaHostOverlay(viewModel = teleconsultaVm)
    }
}

@Composable
private fun SplashOverlay(
    mensaje: String = "Cargando..."
) {
    var animIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animIn = true }

    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animIn) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(350, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "splashAlpha"
    )
    val scaleAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animIn) 1f else 0.93f,
        animationSpec = androidx.compose.animation.core.tween(400, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "splashScale"
    )

    val infiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "splashPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
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
        Image(
            painter = painterResource(id = com.example.nutriia.R.drawable.ic_splash),
            contentDescription = null,
            modifier = Modifier
                .size(290.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                },
            contentScale = ContentScale.Fit
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
