package com.example.nutriia.shared

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
fun AppiOS() {
    NutriIAiOSApp()
}

@Composable
fun NutriIAiOSApp() {
    val loginViewModel: LoginViewModel = remember { LoginViewModel() }
    val sharedVm: NutriSharedViewModel = remember { NutriSharedViewModel() }
    val teleconsultaVm: TeleconsultaViewModel = remember { TeleconsultaViewModel() }
    val accessibilityVm: AccessibilityViewModel = remember { AccessibilityViewModel() }

    var currentScreen by rememberSaveable { mutableStateOf(Screen.LOGIN) }
    var children by remember { mutableStateOf<List<ChildProfile>>(emptyList()) }
    var activeChildIndex by rememberSaveable { mutableIntStateOf(0) }
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

    NutriIATheme {
        Box(modifier = Modifier.fillMaxSize()) {
            when (currentScreen) {
                Screen.ACCESIBILIDAD_INICIAL -> OnboardingQuizScreen(
                    soloAccesibilidad = true,
                    onQuizComplete = { },
                    onAccesibilidadCompletada = {
                        accessibilityVm.marcarPrimeraVezCompletada()
                        currentScreen = Screen.REGISTER_TYPE
                    }
                )
                Screen.LOGIN -> NutriaLoginScreen(
                    viewModel = loginViewModel,
                    onNavigateAsParent = {
                        currentScreen = if (children.isEmpty()) Screen.QUIZ else Screen.DASHBOARD_PARENT
                    },
                    onNavigateAsNutritionist = { currentScreen = Screen.DASHBOARD_NUTRITIONIST },
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
                    onLogout = {
                        accessibilityVm.silenciar()
                        loginViewModel.cerrarSesion()
                        currentScreen = Screen.LOGIN
                    },
                    onPacienteClick = { paciente ->
                        pacienteSeleccionado = PacienteResumen(
                            id = paciente.pacienteUid,
                            nombre = paciente.pacienteNombre,
                            edad = "${paciente.semanasGestacion} semanas",
                            genero = "Femenino",
                            ultimaConsulta = "Hoy",
                            tieneAlerta = false
                        )
                        currentScreen = Screen.EXPEDIENTE_EMBARAZO
                    },
                    onConfiguracion = {
                        pantallaOrigenConfig = Screen.DASHBOARD_GINECOLOGO
                        currentScreen = Screen.CONFIGURACION
                    },
                    onVerDirectorio = { currentScreen = Screen.DIRECTORIO_GINECOLOGOS }
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
                    PediatraScreen(childId = child.id, childName = child.name, onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                } ?: run { currentScreen = Screen.DASHBOARD_PARENT }

                Screen.NUTRICION_EMBARAZO -> EmbarazoNutricionScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA })
                Screen.CITAS_EMBARAZO -> CitasEmbarazoScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA })
                Screen.VINCULACION_GINECOLOGO -> VinculacionGinecologoScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }, onVerDirectorio = { currentScreen = Screen.DIRECTORIO_GINECOLOGOS })
                Screen.DIRECTORIO_GINECOLOGOS -> DirectorioGinecologosScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA })

                Screen.PACIENTE_EXPEDIENTE -> pacienteSeleccionado?.let { pac ->
                    PacienteExpedienteScreen(paciente = pac, onNavigateBack = { currentScreen = Screen.DASHBOARD_NUTRITIONIST })
                } ?: run { currentScreen = Screen.DASHBOARD_NUTRITIONIST }

                Screen.EXPEDIENTE_EMBARAZO -> pacienteSeleccionado?.let { pac ->
                    PacienteExpedienteEmbarazoScreen(paciente = pac, onNavigateBack = { currentScreen = Screen.DASHBOARD_GINECOLOGO })
                } ?: run { currentScreen = Screen.DASHBOARD_GINECOLOGO }

                Screen.CONFIGURACION -> ConfiguracionScreen(
                    onVolver = { currentScreen = pantallaOrigenConfig },
                    onEditarPerfil = { currentScreen = Screen.EDITAR_PERFIL },
                    onEditarRegion = {
                        val child = activeChild
                        if (child != null) {
                            hijoParaEditar = child
                            currentScreen = Screen.EDITAR_REGION
                        }
                    },
                    onCerrarSesion = {
                        loginViewModel.cerrarSesion()
                        currentScreen = Screen.LOGIN
                    }
                )
                Screen.AYUDA -> HelpScreen(onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT })
                Screen.BIOMETRIC_ACTIVATION -> BiometricActivationScreen(
                    onActivationSuccess = { currentScreen = Screen.DASHBOARD_PARENT },
                    onSkip = { currentScreen = Screen.DASHBOARD_PARENT }
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
        }
    }
}
