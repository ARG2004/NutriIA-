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
//  NutriIA iOS — Router principal
//  Importa las pantallas de cada módulo KMP y las conecta con navegación
// ═══════════════════════════════════════════════════════════════════════════

import com.example.nutriia.auth.*
import com.example.nutriia.accesibilidad.*
import com.example.nutriia.dashboard.*
import com.example.nutriia.modules.*

// ─── Colores del sistema NutriIA ─────────────────────────────────────────
private val NutriGreen     = Color(0xFF689F38)
private val NutriDarkGreen = Color(0xFF33691E)
private val NutriBgCrema   = Color(0xFFF8F9F3)
private val NutriWhite     = Color.White

// ─── Pantallas disponibles ───────────────────────────────────────────────
enum class Screen {
    LOGIN, REGISTER_TYPE, REGISTER_PARENT, REGISTER_MAMA, REGISTER_PROFESSIONAL,
    DASHBOARD_PARENT, DASHBOARD_NUTRITIONIST, DASHBOARD_PREGNANCY, DASHBOARD_GYNECOLOGIST,
    ACCESSIBILITY_CONFIG, BRAILLE_KEYBOARD,
    SOLIDOS_BLW, GROWTH_CURVES, SLEEP_LOG, NUTRIENT_CALC, CHAT_AI,
    LACTANCIA, PEDIATRA_DIR, EMBARAZO_NUTRICION, CITAS_EMBARAZO,
    TELECONSULTA, EXPEDIENTE
}

@Composable
fun NutriIAiOSApp() {
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }

    MaterialTheme {
        when (currentScreen) {
            Screen.LOGIN               -> LoginScreen(
                onLogin          = { currentScreen = Screen.DASHBOARD_PARENT },
                onNavigateRegister = { currentScreen = Screen.REGISTER_TYPE }
            )
            Screen.REGISTER_TYPE       -> RegisterTypeScreen(
                onNavigateBack        = { currentScreen = Screen.LOGIN },
                onSelectParent        = { currentScreen = Screen.REGISTER_PARENT },
                onSelectNutritionist  = { currentScreen = Screen.REGISTER_PROFESSIONAL },
                onSelectMamaPrimeriza = { currentScreen = Screen.REGISTER_MAMA },
                onSelectGinecologo    = { currentScreen = Screen.REGISTER_PROFESSIONAL }
            )
            Screen.REGISTER_PARENT     -> ParentRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegister     = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.REGISTER_MAMA       -> MamaPrimerizaRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegister     = { currentScreen = Screen.DASHBOARD_PREGNANCY }
            )
            Screen.REGISTER_PROFESSIONAL -> ProfessionalRegisterScreen(
                onNavigateBack = { currentScreen = Screen.REGISTER_TYPE },
                onRegister     = { currentScreen = Screen.DASHBOARD_NUTRITIONIST }
            )
            Screen.DASHBOARD_PARENT    -> ParentDashboardScreen(
                onNavigate   = { dest -> currentScreen = dest },
                onLogout     = { currentScreen = Screen.LOGIN }
            )
            Screen.DASHBOARD_NUTRITIONIST -> NutritionistDashboardScreen(
                onNavigate   = { dest -> currentScreen = dest },
                onLogout     = { currentScreen = Screen.LOGIN }
            )
            Screen.DASHBOARD_PREGNANCY -> PregnancyDashboardScreen(
                onNavigate   = { dest -> currentScreen = dest },
                onLogout     = { currentScreen = Screen.LOGIN }
            )
            Screen.DASHBOARD_GYNECOLOGIST -> GynecologistDashboardScreen(
                onNavigate   = { dest -> currentScreen = dest },
                onLogout     = { currentScreen = Screen.LOGIN }
            )
            Screen.ACCESSIBILITY_CONFIG -> AccessibilityConfigScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.BRAILLE_KEYBOARD    -> BrailleKeyboardScreen(
                onNavigateBack = { currentScreen = Screen.ACCESSIBILITY_CONFIG }
            )
            Screen.SOLIDOS_BLW         -> SolidosBLWScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.GROWTH_CURVES       -> GrowthCurvesScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.SLEEP_LOG           -> SleepLogScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.NUTRIENT_CALC       -> NutrientCalcScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.CHAT_AI             -> ChatAIScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.LACTANCIA           -> LactanciaScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.PEDIATRA_DIR        -> PediatraDirScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.EMBARAZO_NUTRICION  -> EmbarazoNutricionScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PREGNANCY }
            )
            Screen.CITAS_EMBARAZO      -> CitasEmbarazoScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PREGNANCY }
            )
            Screen.TELECONSULTA        -> TeleconsultaScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_PARENT }
            )
            Screen.EXPEDIENTE          -> ExpedienteScreen(
                onNavigateBack = { currentScreen = Screen.DASHBOARD_NUTRITIONIST }
            )
        }
    }
}
