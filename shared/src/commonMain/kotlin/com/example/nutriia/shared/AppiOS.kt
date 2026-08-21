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
import com.example.nutriia.alerta.AlertaViewModel
import com.example.nutriia.alerta.AlertasScreen
import com.example.nutriia.analisisIA.AnalisisScreen
import com.example.nutriia.analisisIA.AnalisisViewModel
import com.example.nutriia.auth.*
import com.example.nutriia.ayuda.HelpScreen
import com.example.nutriia.chatbot.ChatViewModel
import com.example.nutriia.chatbot.NutriChatScreen
import com.example.nutriia.configuracion.*
import com.example.nutriia.crecimiento.CrecimientoScreen
import com.example.nutriia.crecimiento.CrecimientoViewModel
import com.example.nutriia.dashboard.NutriIADashboardScreen
import com.example.nutriia.dashboard.NutritionistDashboardScreen
import com.example.nutriia.dashboard.NutritionistDashboardViewModel
import com.example.nutriia.dashboard.PacienteResumen
import com.example.nutriia.embarazo.*
import com.example.nutriia.expediente.PacienteExpedienteScreen
import com.example.nutriia.expediente.PacienteExpedienteViewModel
import com.example.nutriia.ginecologo.*
import com.example.nutriia.lactancia.LactanciaScreen
import com.example.nutriia.lactancia.LactanciaViewModel
import com.example.nutriia.nutriente.NutrientesScreen
import com.example.nutriia.nutriente.NutrientesViewModel
import com.example.nutriia.payment.*
import com.example.nutriia.platform.isVoiceOverActive
import com.example.nutriia.pediatra.PediatraDashboardViewModel
import com.example.nutriia.pediatra.PediatraScreen
import com.example.nutriia.solidos.AlimentacionViewModel
import com.example.nutriia.solidos.SolidosScreen
import com.example.nutriia.teleconsulta.*
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.ui.theme.NutriIATheme
import com.example.nutriia.util.PermissionType
import com.example.nutriia.util.PlatformPermissionHelper
import com.example.nutriia.vinculacion.VinculacionViewModel
import com.example.nutriia.vinculacion.DirectorioNutriologosScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass

object IOSViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
        val vm: ViewModel = when (modelClass) {
            LoginViewModel::class -> LoginViewModel()
            AccessibilityViewModel::class -> AccessibilityViewModel()
            NutriSharedViewModel::class -> NutriSharedViewModel()
            TeleconsultaViewModel::class -> TeleconsultaViewModel()
            VinculacionViewModel::class -> VinculacionViewModel()
            AlimentacionViewModel::class -> AlimentacionViewModel()
            PediatraDashboardViewModel::class -> PediatraDashboardViewModel()
            PaymentViewModel::class -> PaymentViewModel()
            NutrientesViewModel::class -> NutrientesViewModel()
            LactanciaViewModel::class -> LactanciaViewModel()
            PacienteExpedienteEmbarazoViewModel::class -> PacienteExpedienteEmbarazoViewModel()
            GinecologoViewModel::class -> GinecologoViewModel()
            GinecologoDashboardViewModel::class -> GinecologoDashboardViewModel()
            PacienteExpedienteViewModel::class -> PacienteExpedienteViewModel()
            EmbarazoDashboardViewModel::class -> EmbarazoDashboardViewModel()
            NutritionistDashboardViewModel::class -> NutritionistDashboardViewModel()
            CrecimientoViewModel::class -> CrecimientoViewModel()
            ConfiguracionViewModel::class -> ConfiguracionViewModel()
            ChatViewModel::class -> ChatViewModel()
            AnalisisViewModel::class -> AnalisisViewModel()
            RegisterViewModel::class -> RegisterViewModel()
            AlertaViewModel::class -> AlertaViewModel()
            else -> throw IllegalArgumentException("ViewModel no registrado para iOS: ${modelClass.simpleName}")
        }
        return vm as T
    }
}

// ─── Enum de Pantallas 1:1 idéntico a Android MainActivity.kt ─────────────
enum class Screen {
    ACCESIBILIDAD_INICIAL, LOGIN, REGISTER_TYPE, REGISTER_PARENT, REGISTER_NUTRITIONIST, REGISTER_MAMA_PRIMERIZA,
    REGISTER_GINECOLOGO,
    QUIZ, QUIZ_MAMA_PRIMERIZA, DASHBOARD_PARENT, DASHBOARD_NUTRITIONIST, DASHBOARD_MAMA_PRIMERIZA,
    DASHBOARD_GINECOLOGO,
    VINCULACION_GINECOLOGO, DIRECTORIO_GINECOLOGOS,
    LACTANCIA, SOLIDOS, CRECIMIENTO, SUENO, MICRONUTRIENTES, NEURODESARROLLO, MEAL_PLANNING, CHAT_IA, DIARIO_VISUAL, RECORDATORIOS,
    NUTRIENTES, DIETA, CONFIGURACION, EDITAR_PERFIL, EDITAR_REGION, PEDIATRA_DASHBOARD, PACIENTE_EXPEDIENTE, EXPEDIENTE_EMBARAZO, AYUDA, PAGO_TELECONSULTA,
    BIOMETRIC_ACTIVATION, NUTRICION_EMBARAZO, CITAS_EMBARAZO, DIRECTORIO_NUTRIOLOGOS
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
    var savedCrashLog by remember { mutableStateOf(com.example.nutriia.platform.CrashStorage.loadCrash()) }

    if (savedCrashLog != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF181A20))
                .padding(horizontal = 20.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🚨", fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Crash Previo Detectado",
                        color = Color(0xFFFF5252),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Se guardó la información del último cierre inesperado de Kotlin en NSUserDefaults:",
                    color = Color(0xFFB0BEC5),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF23272F)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = savedCrashLog ?: "",
                        color = Color(0xFF80D8FF),
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        com.example.nutriia.platform.CrashStorage.clearCrash()
                        savedCrashLog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF689F38)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Limpiar Registro y Entrar a NutriIA", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(30.dp))
            }
        }
    } else {
        NutriIAiOSApp()
    }
}

@Composable
fun NutriIAiOSApp() {
    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 1/11] Iniciando NutriIAiOSApp()...")

    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 7/11] Configurando ViewModelStore y ViewModelStoreOwner...")
    val viewModelStore = remember { androidx.lifecycle.ViewModelStore() }
    val viewModelStoreOwner = remember {
        object : androidx.lifecycle.ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
            override val viewModelStore: androidx.lifecycle.ViewModelStore = viewModelStore
            override val defaultViewModelProviderFactory: ViewModelProvider.Factory = IOSViewModelFactory
            override val defaultViewModelCreationExtras: CreationExtras = CreationExtras.Empty
        }
    }

    val loginViewModel: LoginViewModel = viewModel(modelClass = LoginViewModel::class, viewModelStoreOwner = viewModelStoreOwner)
    val sharedVm: NutriSharedViewModel = viewModel(modelClass = NutriSharedViewModel::class, viewModelStoreOwner = viewModelStoreOwner)
    val teleconsultaVm: TeleconsultaViewModel = viewModel(modelClass = TeleconsultaViewModel::class, viewModelStoreOwner = viewModelStoreOwner)
    val accessibilityVm: AccessibilityViewModel = viewModel(modelClass = AccessibilityViewModel::class, viewModelStoreOwner = viewModelStoreOwner)
    val paymentVm: PaymentViewModel = viewModel(modelClass = PaymentViewModel::class, viewModelStoreOwner = viewModelStoreOwner)
    val cfgVm: ConfiguracionViewModel = viewModel(modelClass = ConfiguracionViewModel::class, viewModelStoreOwner = viewModelStoreOwner)

    com.example.nutriia.platform.Log.i("AppiOS", "🟢 [PASO 6/11] Colectando StateFlows de Accesibilidad...")
    val estado by loginViewModel.estado.collectAsState()
    val accessibilityMode by accessibilityVm.mode.collectAsState()
    val primeraVez by accessibilityVm.primeraVez.collectAsState()
    val primeraVezCargada by accessibilityVm.primeraVezCargada.collectAsState()

    LaunchedEffect(primeraVezCargada) {
        if (primeraVezCargada && isVoiceOverActive() && accessibilityMode != AccessibilityMode.BLIND) {
            accessibilityVm.setMode(AccessibilityMode.BLIND)
        }
    }

    var isCheckingInitialSession by remember { mutableStateOf(true) }
    var showLoginSplash by remember { mutableStateOf(false) }
    var showResumeSplash by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf<String?>(null) }

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
    var pagoNutriologoUid by remember { mutableStateOf(com.example.nutriia.auth.SessionManager.obtenerPagoUid() ?: "") }
    var pagoNutriologoNombre by remember { mutableStateOf(com.example.nutriia.auth.SessionManager.obtenerPagoNombre() ?: "") }
    var pagoIdExitoso by remember { mutableStateOf(com.example.nutriia.auth.SessionManager.obtenerPagoIdExitoso() ?: "") }
    var pagoPantallaRetorno by remember { mutableStateOf(Screen.PEDIATRA_DASHBOARD) }
    var pagoTipoLlamada by remember { mutableStateOf<TipoLlamada?>(
        com.example.nutriia.auth.SessionManager.obtenerPagoTipo()?.let { runCatching { TipoLlamada.valueOf(it) }.getOrNull() }
    ) }

    // Sincronizar estados de pago con persistencia local para sobrevivir a reinicios (Safari)
    LaunchedEffect(pagoNutriologoUid, pagoNutriologoNombre, pagoIdExitoso, pagoTipoLlamada) {
        com.example.nutriia.platform.Log.i("AppiOS", "Persistiendo estado de pago: uid=$pagoNutriologoUid, exitoso=$pagoIdExitoso")
        com.example.nutriia.auth.SessionManager.guardarEstadoPago(
            uid = pagoNutriologoUid,
            nombre = pagoNutriologoNombre,
            idExitoso = pagoIdExitoso,
            tipo = pagoTipoLlamada?.name
        )
    }

    val scope = rememberCoroutineScope()

    // ─── Sincronizar Perfil de Hijo Activo con NutriSharedViewModel ──────
    LaunchedEffect(activeChild?.id) {
        activeChild?.id?.let { id ->
            if (id.isNotBlank()) {
                val uid = loginViewModel.uidUsuario.ifBlank {
                    com.example.nutriia.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""
                }
                if (uid.isNotBlank()) sharedVm.cargarPerfil(uid, id)
            }
        }
    }

    // ─── Persistir Última Pantalla Interna ────────────────────────────────
    LaunchedEffect(currentScreen) {
        if (esPantallaModuloInterno(currentScreen)) {
            SessionManager.guardarUltimaPantalla(screenName = currentScreen.name)
        }
    }

    // ─── Auto-descarte de Toast Flotante ──────────────────────────────────
    LaunchedEffect(toastMessage) {
        if (toastMessage != null) {
            delay(2500)
            toastMessage = null
        }
    }

    // ─── Solicitud Secuencial de Permisos Narrada en Modo BLIND ───────────
    var permissionStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(accessibilityMode, currentScreen) {
        if (accessibilityMode == AccessibilityMode.BLIND) {
            if (currentScreen == Screen.ACCESIBILIDAD_INICIAL) {
                permissionStep = 1
            } else {
                if (!PlatformPermissionHelper.hasPermission(PermissionType.MICROPHONE)) {
                    val msg = if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) {
                        "Please grant microphone permission to enable voice input."
                    } else {
                        "Por favor, concede el permiso de micrófono para activar el control por voz."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 500L)
                        PlatformPermissionHelper.requestPermission(PermissionType.MICROPHONE) { otorgado ->
                            if (otorgado) accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Microphone enabled." else "Micrófono habilitado.")
                            else accessibilityVm.hablar(if (accessibilityVm.idioma.value == IdiomaVoz.INGLES) "Microphone denied." else "Micrófono denegado.")
                        }
                    }
                }
            }
        } else if (accessibilityMode == AccessibilityMode.MUTE) {
            if (!PlatformPermissionHelper.hasPermission(PermissionType.CAMERA)) {
                PlatformPermissionHelper.requestPermission(PermissionType.CAMERA) { }
            }
        }
    }

    LaunchedEffect(permissionStep, accessibilityMode, currentScreen) {
        if (accessibilityMode != AccessibilityMode.BLIND || currentScreen != Screen.ACCESIBILIDAD_INICIAL) return@LaunchedEffect

        val isIngles = accessibilityVm.idioma.value == IdiomaVoz.INGLES

        when (permissionStep) {
            1 -> {
                if (PlatformPermissionHelper.hasPermission(PermissionType.MICROPHONE)) {
                    permissionStep = 2
                } else {
                    val msg = if (isIngles) {
                        "First, I will ask for microphone permission so you can input information by voice and speak to the assistant. Please press Allow on the prompt."
                    } else {
                        "Primero, te pediré el permiso de micrófono para que puedas dictar información por voz y hablar con el asistente. Por favor, selecciona Permitir en la pantalla."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        PlatformPermissionHelper.requestPermission(PermissionType.MICROPHONE) { otorgado ->
                            if (otorgado) accessibilityVm.hablar(if (isIngles) "Microphone enabled." else "Micrófono habilitado.")
                            else accessibilityVm.hablar(if (isIngles) "Microphone denied." else "Micrófono denegado.")
                            permissionStep = 2
                        }
                    }
                }
            }
            2 -> {
                if (PlatformPermissionHelper.hasPermission(PermissionType.CAMERA)) {
                    permissionStep = 3
                } else {
                    val msg = if (isIngles) {
                        "Next is camera permission for video consultations with pediatricians and gynecologists. Please press Allow."
                    } else {
                        "Ahora te pediré el permiso de cámara para las teleconsultas en video con pediatras y ginecólogos. Por favor, selecciona Permitir."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        PlatformPermissionHelper.requestPermission(PermissionType.CAMERA) { otorgado ->
                            if (otorgado) accessibilityVm.hablar(if (isIngles) "Camera enabled." else "Cámara habilitada.")
                            else accessibilityVm.hablar(if (isIngles) "Camera denied." else "Cámara denegada.")
                            permissionStep = 3
                        }
                    }
                }
            }
            3 -> {
                if (PlatformPermissionHelper.hasPermission(PermissionType.PHONE)) {
                    permissionStep = 4
                } else {
                    val msg = if (isIngles) {
                        "Next is phone permission to establish direct calls with specialists. Please press Allow."
                    } else {
                        "El siguiente es el permiso de teléfono para establecer llamadas directamente con especialistas. Por favor, selecciona Permitir."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        PlatformPermissionHelper.requestPermission(PermissionType.PHONE) { otorgado ->
                            if (otorgado) accessibilityVm.hablar(if (isIngles) "Phone permissions enabled." else "Permisos telefónicos habilitados.")
                            else accessibilityVm.hablar(if (isIngles) "Phone permissions denied." else "Permisos telefónicos denegados.")
                            permissionStep = 4
                        }
                    }
                }
            }
            4 -> {
                if (PlatformPermissionHelper.hasPermission(PermissionType.NEAR_DEVICES)) {
                    permissionStep = 5
                } else {
                    val msg = if (isIngles) {
                        "Finally, I will ask for nearby devices permission to allow device synchronization. Please press Allow."
                    } else {
                        "Por último, te pediré el permiso de dispositivos cercanos para permitir la sincronización. Por favor, selecciona Permitir."
                    }
                    scope.launch {
                        accessibilityVm.ttsManager?.hablarYEsperar(msg, 800L)
                        PlatformPermissionHelper.requestPermission(PermissionType.NEAR_DEVICES) { otorgado ->
                            if (otorgado) accessibilityVm.hablar(if (isIngles) "Nearby devices permission enabled." else "Permiso de dispositivos cercanos habilitado.")
                            else accessibilityVm.hablar(if (isIngles) "Nearby devices permission denied." else "Permiso de dispositivos cercanos denegado.")
                            permissionStep = 5
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        com.example.nutriia.platform.RemoteConfigManager.fetchConfigs()
        // Solicitar permisos de notificación globales en iOS al arrancar
        PlatformPermissionHelper.requestPermission(PermissionType.NOTIFICATIONS) { }
    }

    // ─── Control de Sesión Inicial y Primera Vez (Accesibilidad) ───────────
    LaunchedEffect(primeraVezCargada) {
        if (!primeraVezCargada) return@LaunchedEffect
        loginViewModel.verificarSesion { rol, hijos ->
            val ultimaGuardada = SessionManager.obtenerUltimaPantalla()
            val pantallaRestaurada = try {
                if (ultimaGuardada != null) Screen.valueOf(ultimaGuardada) else null
            } catch (_: Exception) { null }

            when (rol) {
                "nutriologo" -> {
                    currentScreen = if (pantallaRestaurada != null && esPantallaModuloInterno(pantallaRestaurada)) pantallaRestaurada else Screen.DASHBOARD_NUTRITIONIST
                    accessibilityVm.sincronizarDesdeFirebase()
                    teleconsultaVm.iniciarObservacionEntrantesNutriologo(loginViewModel.uidUsuario)
                }
                "ginecologo" -> {
                    currentScreen = if (pantallaRestaurada != null && esPantallaModuloInterno(pantallaRestaurada)) pantallaRestaurada else Screen.DASHBOARD_GINECOLOGO
                    accessibilityVm.sincronizarDesdeFirebase()
                    teleconsultaVm.iniciarObservacionEntrantesNutriologo(loginViewModel.uidUsuario)
                }
                "mama_primeriza" -> {
                    if (nombreMama.isBlank()) nombreMama = loginViewModel.nombreUsuario
                    accessibilityVm.sincronizarDesdeFirebase()
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
                    teleconsultaVm.iniciarObservacionEntrantes(loginViewModel.uidUsuario)
                }
                else -> {
                    if (currentScreen == Screen.LOGIN || currentScreen == Screen.ACCESIBILIDAD_INICIAL) {
                        currentScreen = if (primeraVez) Screen.ACCESIBILIDAD_INICIAL else Screen.LOGIN
                    }
                }
            }
            if (rol != "mama_primeriza") {
                isCheckingInitialSession = false
            }
        }
    }

    // Manejar Deep Links globales (PayPal, etc.)
    LaunchedEffect(Unit) {
        DeepLinkManager.links.collect { url ->
            if (url.startsWith("nutriia://pago")) {
                paymentVm.procesarDeepLink(url)
            }
        }
    }

    // Reactivar observación de llamadas al resumir la app en iOS
    LaunchedEffect(showResumeSplash) {
        if (!showResumeSplash && loginViewModel.uidUsuario.isNotBlank()) {
            val rol = loginViewModel.rolUsuario
            if (rol == "nutriologo" || rol == "ginecologo") {
                teleconsultaVm.iniciarObservacionEntrantesNutriologo(loginViewModel.uidUsuario)
            } else {
                teleconsultaVm.iniciarObservacionEntrantes(loginViewModel.uidUsuario)
            }
        }
    }

    // ─── Reacción a Login Exitoso con Activación Biométrica ───────────────
    LaunchedEffect(estado) {
        val s = estado
        if (s is LoginUiState.Exito) {
            if (esPantallaModuloInterno(currentScreen)) return@LaunchedEffect
            showLoginSplash = true

            // Sincronizar hijos y sesión de inmediato para evitar que el quiz aparezca erróneamente
            children = s.hijos
            if (s.hijos.isNotEmpty()) {
                activeChildIndex = 0
            }
            saltarAccesibilidadEnQuiz = true
            SessionManager.guardarSesion(loginViewModel.uidUsuario)
            SessionManager.guardarUltimoUid(loginViewModel.uidUsuario)

            // Limpieza preventiva de modelos compartidos si es necesario
            sharedVm.limpiarPerfil()

            delay(120)

            val yaActivoHuella = SessionManager.huellaYaConfirmada() || SessionManager.esBiometricoActivo()
            if (!yaActivoHuella && BiometricHelper.isAvailable()) {
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
                        currentScreen = if (s.hijos.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                        teleconsultaVm.iniciarObservacionEntrantes(loginViewModel.uidUsuario)
                    }
                }
            }
            delay(1380)
            showLoginSplash = false
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
                            viewModelStore.clear() // Mata todos los ViewModels y listeners
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
                            viewModelStore.clear() // Mata todos los ViewModels y listeners
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
                                    viewModelStore.clear() // Mata todos los ViewModels y listeners
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
                            viewModelStore.clear() // Mata todos los ViewModels y listeners
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
                        LactanciaScreen(childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), a11yVm = accessibilityVm, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.SOLIDOS -> activeChild?.let { child ->
                        SolidosScreen(uid = loginViewModel.uidUsuario, childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), sharedVm = sharedVm, a11yVm = accessibilityVm, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.CRECIMIENTO -> activeChild?.let { child ->
                        CrecimientoScreen(childId = child.id, childName = child.name, ageMonths = mesesDeVida(child.birthDate), sexo = child.sexo, a11yVm = accessibilityVm, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                    Screen.NUTRIENTES -> activeChild?.let { child ->
                        NutrientesScreen(childId = child.id, childName = child.name, mesesEdad = mesesDeVida(child.birthDate), sharedVm = sharedVm, a11yVm = accessibilityVm, onBack = { currentScreen = Screen.DASHBOARD_PARENT })
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
                            AlertasScreen(childId = null, childName = "Mi Embarazo", uid = loginViewModel.uidUsuario, a11yVm = accessibilityVm, onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA })
                        } else {
                            activeChild?.let { child ->
                                AlertasScreen(childId = child.id, childName = child.name, uid = loginViewModel.uidUsuario, a11yVm = accessibilityVm, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                            } ?: run { currentScreen = Screen.DASHBOARD_PARENT }
                        }
                    }

                    Screen.PEDIATRA_DASHBOARD -> activeChild?.let { child ->
                        PediatraScreen(
                            padreUid = loginViewModel.uidUsuario,
                            padreNombre = loginViewModel.nombreUsuario,
                            childId = child.id,
                            childNombre = child.name,
                            a11yVm = accessibilityVm,
                            iniciarLlamadaAlEntrar = pagoTipoLlamada,
                            pagoNutriologoUid = pagoNutriologoUid,
                            pagoNutriologoNombre = pagoNutriologoNombre,
                            pagoIdExitoso = pagoIdExitoso,
                            onLlamadaIniciada = {
                                pagoIdExitoso = ""
                                pagoTipoLlamada = null
                            },
                            onAbrirPago = { nutriologoUid, nutriologoNombre, tipo ->
                                pagoNutriologoUid = nutriologoUid
                                pagoNutriologoNombre = nutriologoNombre
                                pagoTipoLlamada = tipo
                                pagoPantallaRetorno = Screen.PEDIATRA_DASHBOARD
                                currentScreen = Screen.PAGO_TELECONSULTA
                            },
                            onAbrirDirectorio = { currentScreen = Screen.DIRECTORIO_NUTRIOLOGOS },
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
                        onAbrirPago = { gineUid, gineNombre, tipo ->
                            pagoNutriologoUid = gineUid
                            pagoNutriologoNombre = gineNombre
                            pagoTipoLlamada = tipo
                            pagoPantallaRetorno = Screen.CITAS_EMBARAZO
                            currentScreen = Screen.PAGO_TELECONSULTA
                        },
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )
                    Screen.PAGO_TELECONSULTA -> {
                        val childId = activeChild?.id ?: loginViewModel.uidUsuario
                        val childName = activeChild?.name ?: loginViewModel.nombreUsuario
                        PaymentGateScreen(
                            nutriologoUid = pagoNutriologoUid,
                            nutriologoNombre = pagoNutriologoNombre,
                            childId = childId,
                            childNombre = childName,
                            onPagoConfirmado = {
                                pagoIdExitoso = "PAGO_EXITOSO"
                                currentScreen = pagoPantallaRetorno
                            },
                            onCancelar = {
                                currentScreen = pagoPantallaRetorno
                            }
                        )
                    }
                    Screen.VINCULACION_GINECOLOGO -> VinculacionGinecologoScreen(
                        onNavigateToDirectorio = { currentScreen = Screen.DIRECTORIO_GINECOLOGOS },
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )
                    Screen.DIRECTORIO_GINECOLOGOS -> DirectorioGinecologosScreen(
                        mamaNombre = loginViewModel.nombreUsuario,
                        onBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA },
                        onVinculado = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
                    )

                    Screen.DIRECTORIO_NUTRIOLOGOS -> activeChild?.let { child ->
                        DirectorioNutriologosScreen(
                            padreNombre = loginViewModel.nombreUsuario,
                            childId = child.id,
                            childNombre = child.name,
                            onBack = { currentScreen = Screen.PEDIATRA_DASHBOARD },
                            onVinculado = { currentScreen = Screen.PEDIATRA_DASHBOARD }
                        )
                    } ?: run { currentScreen = Screen.PEDIATRA_DASHBOARD }

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

                    Screen.CONFIGURACION -> {
                        val logoutAction = {
                            teleconsultaVm.detenerObservacionEntrantes()
                            accessibilityVm.silenciar()
                            loginViewModel.cerrarSesion()
                            viewModelStore.clear() // Mata todos los ViewModels y listeners
                            children = emptyList()
                            activeChildIndex = 0
                            saltarAccesibilidadEnQuiz = false
                            perfilEmbarazo = null
                            nombreMama = ""
                            currentScreen = Screen.LOGIN
                        }
                        ConfiguracionScreen(
                            children = children,
                            nombrePadre = loginViewModel.nombreUsuario.ifBlank { "Usuario NutriIA" },
                            emailPadre = loginViewModel.emailUsuario.ifBlank { "usuario@nutriia.com" },
                            rol = loginViewModel.rolUsuario.ifBlank { "padre" },
                            onBack = { currentScreen = pantallaOrigenConfig },
                            onEditarPerfil = { currentScreen = Screen.EDITAR_PERFIL },
                            onCambiarPasswordDirecto = { actual, nueva, callback ->
                                cfgVm.cambiarContrasenaDirecta(actual, nueva) { exito, msg ->
                                    callback(exito, msg)
                                    if (exito) {
                                        toastMessage = "Contraseña actualizada exitosamente"
                                    }
                                }
                            },
                            onEnviarCorreoPassword = {
                                cfgVm.enviarRecuperacionPassword(loginViewModel.emailUsuario)
                                toastMessage = "Correo de recuperación enviado"
                            },
                            onEditarHijo = { child ->
                                hijoParaEditar = child
                                currentScreen = Screen.EDITAR_REGION
                            },
                            onAgregarHijo = {
                                isAddingChild = true
                                saltarAccesibilidadEnQuiz = true
                                currentScreen = Screen.QUIZ
                            },
                            onPrivacidad = { },
                            onCerrarSesion = { cfgVm.cerrarSesion { logoutAction() } },
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
                            toastMessage = if (telefono != loginViewModel.telefonoUsuario) "Teléfono actualizado exitosamente" else "Perfil actualizado exitosamente"
                            currentScreen = Screen.CONFIGURACION
                        }
                    )
                    Screen.EDITAR_REGION -> {
                        val hijo = hijoParaEditar
                        if (hijo == null) {
                            currentScreen = Screen.CONFIGURACION
                        } else {
                            OnboardingQuizScreen(
                                isAddingChild = false,
                                saltarAccesibilidad = true,
                                initialStep = 6,
                                prefilledProfile = hijo,
                                onQuizComplete = { perfilActualizado ->
                                    val hijoActualizado = hijo.copy(
                                        nivelIngreso = perfilActualizado.nivelIngreso,
                                        region = perfilActualizado.region
                                    )
                                    loginViewModel.guardarHijo(hijoActualizado) { exito ->
                                        if (exito) {
                                            children = children.map { c -> if (c.id == hijoActualizado.id) hijoActualizado else c }
                                            loginViewModel.recargarHijos()
                                        }
                                    }
                                    hijoParaEditar = null
                                    toastMessage = "Región actualizada exitosamente"
                                    currentScreen = Screen.CONFIGURACION
                                },
                                onCancel = {
                                    hijoParaEditar = null
                                    currentScreen = Screen.CONFIGURACION
                                }
                            )
                        }
                    }
                    Screen.AYUDA -> HelpScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                    Screen.BIOMETRIC_ACTIVATION -> BiometricActivationScreen(
                        uid = loginViewModel.uidUsuario,
                        rol = loginViewModel.rolUsuario,
                        onActivado = {
                            SessionManager.marcarBiometricoActivo(activo = true)
                            SessionManager.marcarHuellaConfirmada()
                            SessionManager.guardarUltimoUid(loginViewModel.uidUsuario)
                            val rol = loginViewModel.rolUsuario
                            currentScreen = when (rol) {
                                "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                                "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                                "mama_primeriza" -> Screen.DASHBOARD_MAMA_PRIMERIZA
                                else -> if (children.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                            }
                        },
                        onOmitido = {
                            SessionManager.marcarActivacionHuellaMostrada()
                            SessionManager.guardarUltimoUid(loginViewModel.uidUsuario)
                            val rol = loginViewModel.rolUsuario
                            currentScreen = when (rol) {
                                "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                                "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                                "mama_primeriza" -> Screen.DASHBOARD_MAMA_PRIMERIZA
                                else -> if (children.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                            }
                        }
                    )
                    else -> NutriIADashboardScreen(
                        children = children,
                        initialPageIndex = activeChildIndex,
                        esNutriologo = false,
                        onPageChange = { index -> activeChildIndex = index },
                        onLogout = {
                            loginViewModel.cerrarSesion()
                            viewModelStore.clear()
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

                // Overlay de Teleconsulta para iOS
                TeleconsultaHostOverlay(viewModel = teleconsultaVm)

                // ─── Toast Flotante Multiplataforma ──────────────────────────
                AnimatedVisibility(
                    visible = toastMessage != null,
                    enter = fadeIn(tween(250)) + slideInVertically(initialOffsetY = { -it }),
                    exit = fadeOut(tween(250)) + slideOutVertically(targetOffsetY = { -it }),
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp, start = 16.dp, end = 16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1E232A),
                        shadowElevation = 8.dp,
                        border = BorderStroke(1.dp, Color(0xFF689F38))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("✅", fontSize = 16.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = toastMessage ?: "",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
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
