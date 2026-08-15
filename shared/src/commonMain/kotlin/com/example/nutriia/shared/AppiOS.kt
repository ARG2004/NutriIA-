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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
//  NutriIA iOS — Router y Navegación idéntica a MainActivity.kt (Android)
// ═══════════════════════════════════════════════════════════════════════════

import com.example.nutriia.auth.*
import com.example.nutriia.accesibilidad.*
import com.example.nutriia.dashboard.*
import com.example.nutriia.modules.*

// ─── Enum de Pantallas 1:1 idéntico a Android MainActivity.kt ─────────────
enum class Screen {
    ACCESIBILIDAD_INICIAL, LOGIN, REGISTER_TYPE, REGISTER_PARENT, REGISTER_NUTRITIONIST, REGISTER_MAMA_PRIMERIZA,
    REGISTER_GINECOLOGO,
    QUIZ, QUIZ_MAMA_PRIMERIZA, DASHBOARD_PARENT, DASHBOARD_NUTRITIONIST, DASHBOARD_MAMA_PRIMERIZA,
    DASHBOARD_GINECOLOGO,
    VINCULACION_GINECOLOGO, DIRECTORIO_GINECOLOGOS,
    LACTANCIA, SOLIDOS, CRECIMIENTO, SUENO, MICRONUTRIENTES, NEURODESARROLLO, MEAL_PLANNING, CHAT_IA, DIARIO_VISUAL, RECORDATORIOS,
    NUTRIENTES, DIETA, CONFIGURACION, EDITAR_PERFIL, EDITAR_REGION, PEDIATRA_DASHBOARD, PACIENTE_EXPEDIENTE, EXPEDIENTE_EMBARAZO, AYUDA, PAGO_TELECONSULTA,
    BIOMETRIC_ACTIVATION, NUTRICION_EMBARAZO, CITAS_EMBARAZO, TELECONSULTA
}

fun esPantallaModuloInterno(screen: Screen): Boolean {
    return when (screen) {
        Screen.LACTANCIA, Screen.SOLIDOS, Screen.CRECIMIENTO, Screen.SUENO,
        Screen.MICRONUTRIENTES, Screen.NEURODESARROLLO, Screen.MEAL_PLANNING,
        Screen.CHAT_IA, Screen.DIARIO_VISUAL, Screen.RECORDATORIOS, Screen.NUTRIENTES,
        Screen.DIETA, Screen.CONFIGURACION, Screen.EDITAR_PERFIL, Screen.EDITAR_REGION,
        Screen.PEDIATRA_DASHBOARD, Screen.AYUDA, Screen.VINCULACION_GINECOLOGO,
        Screen.DIRECTORIO_GINECOLOGOS, Screen.NUTRICION_EMBARAZO, Screen.CITAS_EMBARAZO,
        Screen.PACIENTE_EXPEDIENTE, Screen.EXPEDIENTE_EMBARAZO, Screen.TELECONSULTA -> true
        else -> false
    }
}

@Composable
fun AppiOS() {
    NutriIAiOSApp()
}

@Composable
fun NutriIAiOSApp() {
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

    MaterialTheme {
        when (currentScreen) {
            Screen.ACCESIBILIDAD_INICIAL -> AccessibilityConfigScreen(
                onNavigateBack = { currentScreen = Screen.LOGIN }
            )
            Screen.LOGIN -> LoginScreen(
                onLogin = { currentScreen = Screen.DASHBOARD_PARENT },
                onNavigateRegister = { currentScreen = Screen.REGISTER_TYPE }
            )
            Screen.REGISTER_TYPE -> RegisterTypeScreen(
                onNavigateBack        = { currentScreen = Screen.LOGIN },
                onSelectParent        = { currentScreen = Screen.REGISTER_PARENT },
                onSelectNutritionist  = { currentScreen = Screen.REGISTER_NUTRITIONIST },
                onSelectMamaPrimeriza = { currentScreen = Screen.REGISTER_MAMA_PRIMERIZA },
                onSelectGinecologo    = { currentScreen = Screen.REGISTER_GINECOLOGO }
            )
            Screen.REGISTER_PARENT -> ParentRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegister     = { currentScreen = Screen.QUIZ }
            )
            Screen.REGISTER_MAMA_PRIMERIZA -> MamaPrimerizaRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegister     = { currentScreen = Screen.QUIZ_MAMA_PRIMERIZA }
            )
            Screen.REGISTER_NUTRITIONIST -> ProfessionalRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegister     = { currentScreen = Screen.DASHBOARD_NUTRITIONIST }
            )
            Screen.REGISTER_GINECOLOGO -> ProfessionalRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegister     = { currentScreen = Screen.DASHBOARD_GINECOLOGO }
            )

            // Quizzes de Onboarding
            Screen.QUIZ -> OnboardingQuizScreen(
                onQuizComplete = { currentScreen = Screen.DASHBOARD_PARENT },
                onCancel = { currentScreen = Screen.LOGIN }
            )
            Screen.QUIZ_MAMA_PRIMERIZA -> EmbarazoQuizScreen(
                onQuizComplete = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA },
                onCancel = { currentScreen = Screen.LOGIN }
            )

            // Dashboards
            Screen.DASHBOARD_PARENT -> ParentDashboardScreen(
                onNavigate = { dest -> currentScreen = dest },
                onLogout   = { currentScreen = Screen.LOGIN }
            )
            Screen.DASHBOARD_NUTRITIONIST -> NutritionistDashboardScreen(
                onNavigate = { dest -> currentScreen = dest },
                onLogout   = { currentScreen = Screen.LOGIN }
            )
            Screen.DASHBOARD_MAMA_PRIMERIZA -> PregnancyDashboardScreen(
                onNavigate = { dest -> currentScreen = dest },
                onLogout   = { currentScreen = Screen.LOGIN }
            )
            Screen.DASHBOARD_GINECOLOGO -> GynecologistDashboardScreen(
                onNavigate = { dest -> currentScreen = dest },
                onLogout   = { currentScreen = Screen.LOGIN }
            )

            // Módulos clínicos
            Screen.CONFIGURACION        -> AccessibilityConfigScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.SOLIDOS              -> SolidosBLWScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.CRECIMIENTO          -> GrowthCurvesScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.SUENO                -> SleepLogScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.NUTRIENTES,
            Screen.MICRONUTRIENTES,
            Screen.DIETA                -> NutrientCalcScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.CHAT_IA              -> ChatAIScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.LACTANCIA            -> LactanciaScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.PEDIATRA_DASHBOARD   -> PediatraDirScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.NUTRICION_EMBARAZO   -> EmbarazoNutricionScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
            )
            Screen.CITAS_EMBARAZO       -> CitasEmbarazoScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA }
            )
            Screen.TELECONSULTA,
            Screen.PAGO_TELECONSULTA    -> TeleconsultaScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.PACIENTE_EXPEDIENTE  -> ExpedienteScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_NUTRITIONIST }
            )
            Screen.EXPEDIENTE_EMBARAZO  -> ExpedienteScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_GINECOLOGO }
            )
            else                        -> ParentDashboardScreen(
                onNavigate = { dest -> currentScreen = dest },
                onLogout   = { currentScreen = Screen.LOGIN }
            )
        }
    }
}
