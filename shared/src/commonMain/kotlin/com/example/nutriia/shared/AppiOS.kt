package com.example.nutriia.shared

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccesibilidadInicialScreen
import com.example.nutriia.auth.*
import com.example.nutriia.dashboard.*
import com.example.nutriia.crecimiento.*
import com.example.nutriia.solidos.*
import com.example.nutriia.lactancia.*
import com.example.nutriia.nutriente.*
import com.example.nutriia.chatbot.*
import com.example.nutriia.configuracion.*

enum class Screen {
    ACCESIBILIDAD_INICIAL,
    LOGIN,
    SELECCION_ROL_REGISTRO,
    REGISTRO_PADRE,
    REGISTRO_NUTRIOLOGO,
    REGISTRO_MAMA_PRIMERIZA,
    REGISTRO_GINECOLOGO,
    ONBOARDING_QUIZ,
    BIOMETRIA_ACTIVACION,
    DASHBOARD_PARENT,
    DASHBOARD_NUTRITIONIST,
    DASHBOARD_EMBARAZO,
    DASHBOARD_GINECOLOGO,
    DIRECTORIO_GINECOLOGOS,
    PACIENTE_EXPEDIENTE,
    LACTANCIA,
    SOLIDOS,
    CRECIMIENTO,
    SUENO,
    NUTRIENTES,
    NEURODESARROLLO,
    MEAL_PLANNING,
    CHAT_IA,
    DIARIO_VISUAL,
    RECORDATORIOS,
    PEDIATRA_DASHBOARD,
    CONFIGURACION,
    AYUDA
}

@Composable
fun AppiOS() {
    var currentScreen by remember { mutableStateOf(Screen.ACCESIBILIDAD_INICIAL) }
    var userEmail by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("Familia NutriIA") }
    var userRole by remember { mutableStateOf("padre") }
    var accessibilityMode by remember { mutableStateOf(AccessibilityMode.NORMAL) }
    var isOfflineMode by remember { mutableStateOf(false) }

    val defaultChildren = remember {
        mutableStateListOf(
            ChildData(
                id = "1",
                name = "Santiago",
                birthDate = "15/02/2026",
                ageText = "10 meses",
                stage = "Alimentación Complementaria",
                weight = "7.8 kg",
                height = "67.0 cm",
                headCirc = "42.5 cm",
                bmiPercentile = "p50"
            )
        )
    }
    var activeChildIndex by remember { mutableIntStateOf(0) }

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentScreen) {
            Screen.ACCESIBILIDAD_INICIAL -> {
                AccesibilidadInicialScreen(
                    currentMode = accessibilityMode,
                    onModeSelected = { mode ->
                        accessibilityMode = mode
                        currentScreen = Screen.LOGIN
                    },
                    onSkip = { currentScreen = Screen.LOGIN }
                )
            }

            Screen.LOGIN -> {
                NutriaLoginScreen(
                    onLoginSuccess = { email, role ->
                        userEmail = email
                        userRole = role
                        currentScreen = Screen.BIOMETRIA_ACTIVACION
                    },
                    onNavigateToRegister = { currentScreen = Screen.SELECCION_ROL_REGISTRO },
                    onBiometricLogin = { currentScreen = Screen.DASHBOARD_PARENT }
                )
            }

            Screen.SELECCION_ROL_REGISTRO -> {
                RegisterTypeScreen(
                    onNavigateBack = { currentScreen = Screen.LOGIN },
                    onSelectParent = { currentScreen = Screen.REGISTRO_PADRE },
                    onSelectNutritionist = { currentScreen = Screen.REGISTRO_NUTRIOLOGO },
                    onSelectMamaPrimeriza = { currentScreen = Screen.REGISTRO_MAMA_PRIMERIZA },
                    onSelectGinecologo = { currentScreen = Screen.REGISTRO_GINECOLOGO }
                )
            }

            Screen.REGISTRO_PADRE -> {
                ParentRegisterScreen(
                    onRegistered = { name, email ->
                        userName = name
                        userEmail = email
                        userRole = "padre"
                        currentScreen = Screen.ONBOARDING_QUIZ
                    },
                    onBack = { currentScreen = Screen.SELECCION_ROL_REGISTRO }
                )
            }

            Screen.REGISTRO_MAMA_PRIMERIZA -> {
                MamaPrimerizaRegisterScreen(
                    onRegistered = { name, email, _ ->
                        userName = name
                        userEmail = email
                        userRole = "mama_primeriza"
                        currentScreen = Screen.DASHBOARD_EMBARAZO
                    },
                    onBack = { currentScreen = Screen.SELECCION_ROL_REGISTRO }
                )
            }

            Screen.REGISTRO_NUTRIOLOGO -> {
                ProfessionalRegisterScreen(
                    roleTitle = "Nutriólogo Clínico",
                    profesionRequerida = "Nutriólogo",
                    accentColor = NutriaSoftTeal,
                    onRegistered = { name, email, _ ->
                        userName = name
                        userEmail = email
                        userRole = "nutriologo"
                        currentScreen = Screen.DASHBOARD_NUTRITIONIST
                    },
                    onBack = { currentScreen = Screen.SELECCION_ROL_REGISTRO }
                )
            }

            Screen.REGISTRO_GINECOLOGO -> {
                ProfessionalRegisterScreen(
                    roleTitle = "Ginecólogo Obstetra",
                    profesionRequerida = "Ginecólogo",
                    accentColor = RegRosaGine,
                    onRegistered = { name, email, _ ->
                        userName = name
                        userEmail = email
                        userRole = "ginecologo"
                        currentScreen = Screen.DASHBOARD_GINECOLOGO
                    },
                    onBack = { currentScreen = Screen.SELECCION_ROL_REGISTRO }
                )
            }

            Screen.ONBOARDING_QUIZ -> {
                OnboardingQuizCompleteView(
                    initialChildName = "Santiago",
                    onQuizComplete = { name, birthDate, weight, height, headCirc ->
                        defaultChildren[0] = defaultChildren[0].copy(
                            name = name,
                            birthDate = birthDate,
                            weight = "$weight kg",
                            height = "$height cm",
                            headCirc = "$headCirc cm"
                        )
                        currentScreen = Screen.BIOMETRIA_ACTIVACION
                    },
                    onCancel = { currentScreen = Screen.DASHBOARD_PARENT }
                )
            }

            Screen.BIOMETRIA_ACTIVACION -> {
                BiometricActivationScreen(
                    onActivated = {
                        currentScreen = when (userRole) {
                            "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                            "mama_primeriza" -> Screen.DASHBOARD_EMBARAZO
                            "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                            else -> Screen.DASHBOARD_PARENT
                        }
                    },
                    onSkip = {
                        currentScreen = when (userRole) {
                            "nutriologo" -> Screen.DASHBOARD_NUTRITIONIST
                            "mama_primeriza" -> Screen.DASHBOARD_EMBARAZO
                            "ginecologo" -> Screen.DASHBOARD_GINECOLOGO
                            else -> Screen.DASHBOARD_PARENT
                        }
                    }
                )
            }

            Screen.DASHBOARD_PARENT -> {
                MainAppScaffold(currentTab = Screen.DASHBOARD_PARENT, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    NutriIADashboardScreenView(
                        userEmail = userEmail,
                        userName = userName,
                        children = defaultChildren,
                        activeChildIndex = activeChildIndex,
                        onChildSelected = { activeChildIndex = it },
                        onNavigate = { currentScreen = it },
                        onAddChild = { currentScreen = Screen.ONBOARDING_QUIZ },
                        onToggleOffline = { isOfflineMode = !isOfflineMode },
                        isOffline = isOfflineMode
                    )
                }
            }

            Screen.DASHBOARD_NUTRITIONIST -> {
                MainAppScaffold(currentTab = Screen.DASHBOARD_NUTRITIONIST, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    NutritionistDashboardScreenView(onNavigate = { currentScreen = it })
                }
            }

            Screen.DASHBOARD_EMBARAZO -> {
                MainAppScaffold(currentTab = Screen.DASHBOARD_EMBARAZO, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    EmbarazoDashboardScreenView(onNavigate = { currentScreen = it })
                }
            }

            Screen.DASHBOARD_GINECOLOGO -> {
                MainAppScaffold(currentTab = Screen.DASHBOARD_GINECOLOGO, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    GinecologoDashboardScreenView(onNavigate = { currentScreen = it })
                }
            }

            Screen.DIRECTORIO_GINECOLOGOS -> {
                DirectorioGinecologosScreenView(onBack = { currentScreen = Screen.DASHBOARD_EMBARAZO })
            }

            Screen.PACIENTE_EXPEDIENTE -> {
                PacienteExpedienteScreenView(onBack = { currentScreen = Screen.DASHBOARD_NUTRITIONIST })
            }

            Screen.LACTANCIA -> {
                MainAppScaffold(currentTab = Screen.LACTANCIA, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    LactanciaScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                }
            }

            Screen.SOLIDOS -> {
                MainAppScaffold(currentTab = Screen.SOLIDOS, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    SolidosScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                }
            }

            Screen.CRECIMIENTO -> {
                MainAppScaffold(currentTab = Screen.CRECIMIENTO, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    CrecimientoScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                }
            }

            Screen.SUENO -> {
                SuenoScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }

            Screen.NUTRIENTES -> {
                NutrientesScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }

            Screen.NEURODESARROLLO -> {
                NeurodesarrolloScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }

            Screen.MEAL_PLANNING -> {
                MealPlanningScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }

            Screen.CHAT_IA -> {
                MainAppScaffold(currentTab = Screen.CHAT_IA, isOffline = isOfflineMode, onTabSelected = { currentScreen = it }) {
                    NutriChatScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                }
            }

            Screen.DIARIO_VISUAL -> {
                DiarioVisualScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }

            Screen.RECORDATORIOS -> {
                RecordatoriosScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }

            Screen.PEDIATRA_DASHBOARD -> {
                PediatraScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }

            Screen.CONFIGURACION -> {
                ConfiguracionScreenView(
                    onBack = { currentScreen = Screen.DASHBOARD_PARENT },
                    onLogout = { currentScreen = Screen.LOGIN }
                )
            }

            Screen.AYUDA -> {
                HelpScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
            }
        }
    }
}
