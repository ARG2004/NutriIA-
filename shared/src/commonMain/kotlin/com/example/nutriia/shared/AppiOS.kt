package com.example.nutriia.shared

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun AppiOS() {
    var currentScreen by rememberSaveable { mutableStateOf(Screen.LOGIN) }
    var showSplash by remember { mutableStateOf(true) }
    var splashMessage by remember { mutableStateOf("NutrIA...") }
    var userEmail by rememberSaveable { mutableStateOf("familia@nutriia.com") }
    var userName by rememberSaveable { mutableStateOf("Familia Rivera") }
    var childName by rememberSaveable { mutableStateOf("Mateo Rivera") }

    LaunchedEffect(Unit) {
        delay(1600)
        showSplash = false
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
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = NutriaBgCrema
            ) {
                Crossfade(targetState = currentScreen, animationSpec = tween(220)) { screen ->
                    when (screen) {
                        Screen.LOGIN -> NutriaLoginScreen(
                            onLoginSuccess = { email ->
                                userEmail = email.ifBlank { "familia@nutriia.com" }
                                currentScreen = Screen.DASHBOARD_PARENT
                            },
                            onNavigateToRegister = { currentScreen = Screen.REGISTER_TYPE }
                        )
                        Screen.REGISTER_TYPE -> RegisterTypeScreen(
                            onRoleSelected = { role ->
                                when (role) {
                                    "Padre / Madre de Familia" -> currentScreen = Screen.REGISTER_PARENT
                                    "Nutriólogo Clínico" -> currentScreen = Screen.REGISTER_NUTRITIONIST
                                    "Mamá Primeriza" -> currentScreen = Screen.REGISTER_MAMA_PRIMERIZA
                                    else -> currentScreen = Screen.REGISTER_GINECOLOGO
                                }
                            },
                            onBackToLogin = { currentScreen = Screen.LOGIN }
                        )
                        Screen.REGISTER_PARENT -> GenericRegisterScreen(
                            roleTitle = "Padre / Madre de Familia",
                            onRegistered = { name, email ->
                                userName = name
                                userEmail = email
                                currentScreen = Screen.QUIZ
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )
                        Screen.REGISTER_NUTRITIONIST -> GenericRegisterScreen(
                            roleTitle = "Nutriólogo Clínico",
                            onRegistered = { name, email ->
                                userName = name
                                userEmail = email
                                currentScreen = Screen.DASHBOARD_NUTRITIONIST
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )
                        Screen.REGISTER_MAMA_PRIMERIZA -> GenericRegisterScreen(
                            roleTitle = "Mamá Primeriza",
                            onRegistered = { name, email ->
                                userName = name
                                userEmail = email
                                currentScreen = Screen.DASHBOARD_MAMA_PRIMERIZA
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )
                        Screen.REGISTER_GINECOLOGO -> GenericRegisterScreen(
                            roleTitle = "Ginecólogo Obstetra",
                            onRegistered = { name, email ->
                                userName = name
                                userEmail = email
                                currentScreen = Screen.DASHBOARD_GINECOLOGO
                            },
                            onBack = { currentScreen = Screen.REGISTER_TYPE }
                        )
                        Screen.QUIZ -> OnboardingQuizView(
                            onQuizComplete = { newChildName ->
                                childName = newChildName.ifBlank { "Mateo Rivera" }
                                currentScreen = Screen.DASHBOARD_PARENT
                            },
                            onCancel = { currentScreen = Screen.DASHBOARD_PARENT }
                        )
                        Screen.DASHBOARD_PARENT -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NutriIADashboardParentView(
                                childName = childName,
                                onNavigate = { currentScreen = it },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }
                        Screen.DASHBOARD_MAMA_PRIMERIZA -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DashboardMamaPrimerizaView(
                                onNavigate = { currentScreen = it },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }
                        Screen.DASHBOARD_NUTRITIONIST -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DashboardNutritionistView(
                                onNavigate = { currentScreen = it },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }
                        Screen.DASHBOARD_GINECOLOGO -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DashboardGinecologistView(
                                onNavigate = { currentScreen = it },
                                onLogout = { currentScreen = Screen.LOGIN }
                            )
                        }
                        Screen.LACTANCIA -> MainAppScaffold(
                            currentTab = Screen.LACTANCIA,
                            onTabSelected = { currentScreen = it }
                        ) {
                            LactanciaScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.SOLIDOS -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            SolidosScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.CRECIMIENTO -> MainAppScaffold(
                            currentTab = Screen.CRECIMIENTO,
                            onTabSelected = { currentScreen = it }
                        ) {
                            CrecimientoScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.SUENO -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            SuenoScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.NUTRIENTES, Screen.MICRONUTRIENTES -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NutrientesScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.NEURODESARROLLO -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NeurodesarrolloScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.MEAL_PLANNING, Screen.DIETA -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            MealPlanningScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.CHAT_IA -> MainAppScaffold(
                            currentTab = Screen.CHAT_IA,
                            onTabSelected = { currentScreen = it }
                        ) {
                            NutriChatScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.DIARIO_VISUAL -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            DiarioVisualScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.RECORDATORIOS -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            RecordatoriosScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.PEDIATRA_DASHBOARD -> MainAppScaffold(
                            currentTab = Screen.DASHBOARD_PARENT,
                            onTabSelected = { currentScreen = it }
                        ) {
                            PediatraScreenView(onBack = { currentScreen = Screen.DASHBOARD_PARENT })
                        }
                        Screen.CONFIGURACION -> MainAppScaffold(
                            currentTab = Screen.CONFIGURACION,
                            onTabSelected = { currentScreen = it }
                        ) {
                            ConfiguracionScreenView(
                                userEmail = userEmail,
                                userName = userName,
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
// 1. PANTALLA DE LOGIN
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun NutriaLoginScreen(
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit
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
            Spacer(Modifier.height(40.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NutriaMascotaHeader(modifier = Modifier.size(240.dp))
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Nutre su hoy, protege su mañana",
                    fontSize = 15.sp,
                    color = NutriaDarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(28.dp))

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
                    Spacer(Modifier.height(24.dp))

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

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            isLoading = true
                            onLoginSuccess(email)
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
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
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("¿Nuevo en NutrIA?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Crea una cuenta", color = NutriaDarkGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(30.dp))
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

        Spacer(Modifier.height(20.dp))

        Text("¿Cómo deseas unirte?", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
        Text("Selecciona tu rol para personalizar tu experiencia clínica y nutricional.", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        val roles = listOf(
            RoleItem("Padre / Madre de Familia", "Seguimiento nutricional, crecimiento y lactancia", Icons.Rounded.FamilyRestroom, NutriaGreen),
            RoleItem("Nutriólogo Clínico", "Directorio, expedientes y cálculo de dietas", Icons.Rounded.MedicalServices, NutriaSoftTeal),
            RoleItem("Mamá Primeriza", "Guía paso a paso desde el embarazo hasta la lactancia", Icons.Rounded.PregnantWoman, NutriaPink),
            RoleItem("Ginecólogo Obstetra", "Control prenatal y seguimiento materno-fetal", Icons.Rounded.LocalHospital, NutriaGineRosa)
        )

        roles.forEach { role ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onRoleSelected(role.title) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(14.dp)).background(role.color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(role.icon, contentDescription = null, tint = role.color, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(role.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(2.dp))
                        Text(role.subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = role.color)
                }
            }
        }
    }
}

data class RoleItem(val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

// ═══════════════════════════════════════════════════════════════════════════
// 3. FORMULARIO GENÉRICO DE REGISTRO
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun GenericRegisterScreen(
    roleTitle: String,
    onRegistered: (name: String, email: String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
        }

        Spacer(Modifier.height(20.dp))

        Text("Registro: $roleTitle", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Completa tus datos para crear tu cuenta en NutrIA.", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nombre Completo") },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    label = { Text("Correo Electrónico") },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = password, onValueChange = { password = it },
                    label = { Text("Contraseña") },
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onRegistered(name.ifBlank { "Usuario NutrIA" }, email.ifBlank { "usuario@nutriia.com" }) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Crear Cuenta", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 4. ONBOARDING QUIZ VIEW (REGISTRO DE HIJO)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun OnboardingQuizView(
    onQuizComplete: (String) -> Unit,
    onCancel: () -> Unit
) {
    var childNameInput by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("15/02/2026") }
    var weight by remember { mutableStateOf("7.8") }
    var height by remember { mutableStateOf("67.0") }

    Column(
        modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Text("👶 Datos de tu Bebé", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Esto nos permitirá calibrar las curvas OMS personalizadas.", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                OutlinedTextField(
                    value = childNameInput, onValueChange = { childNameInput = it },
                    label = { Text("Nombre del Bebé") },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(
                    value = birthDate, onValueChange = { birthDate = it },
                    label = { Text("Fecha de Nacimiento (DD/MM/AAAA)") },
                    shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weight, onValueChange = { weight = it },
                        label = { Text("Peso (kg)") },
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = height, onValueChange = { height = it },
                        label = { Text("Talla (cm)") },
                        shape = RoundedCornerShape(14.dp), modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = { onQuizComplete(childNameInput.ifBlank { "Mateo Rivera" }) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Text("Guardar y Continuar", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 5. SCAFFOLD CON BOTTOM NAVIGATION BAR
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun MainAppScaffold(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
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

data class NavTabItem(val screen: Screen, val label: String, val icon: ImageVector)

// ═══════════════════════════════════════════════════════════════════════════
// 6. DASHBOARD PRINCIPAL DE PADRES (DASHBOARD_PARENT)
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun NutriIADashboardParentView(
    childName: String,
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("¡Hola, Familia! 🌿", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                    Text("Plan Nutricional Activo OMS", fontSize = 12.sp, color = Color.Gray)
                }
                Box(
                    modifier = Modifier.size(46.dp).clip(CircleShape).background(NutriaGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ChildCare, contentDescription = "Bebé", tint = NutriaGreen, modifier = Modifier.size(28.dp))
                }
            }
        }

        // Tarjeta del Bebé Activo
        item {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = NutriaGreen),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(childName, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("6 meses y 15 días • Alimentación Complementaria", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ChipInfo(label = "7.8 kg", subtitle = "Peso")
                            ChipInfo(label = "67.0 cm", subtitle = "Talla")
                            ChipInfo(label = "42.5 cm", subtitle = "Cefálico")
                        }
                    }
                }
            }
        }

        item {
            Text("Módulos de Nutrición y Desarrollo", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        }

        // Fila 1: Lactancia y Sólidos BLW
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Lactancia Materna",
                    subtitle = "Cronómetro y tomas",
                    icon = Icons.Rounded.Favorite,
                    iconTint = NutriaOrange,
                    color = NutriaOrange.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.LACTANCIA) }
                )
                DashModuleCard(
                    title = "Sólidos & BLW",
                    subtitle = "Guías de introducción",
                    icon = Icons.Rounded.Restaurant,
                    iconTint = NutriaSoftPurple,
                    color = NutriaSoftPurple.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SOLIDOS) }
                )
            }
        }

        // Fila 2: Curvas OMS y Micronutrientes
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Curvas OMS",
                    subtitle = "Percentiles y peso",
                    icon = Icons.AutoMirrored.Rounded.ShowChart,
                    iconTint = NutriaBlue,
                    color = NutriaBlue.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.CRECIMIENTO) }
                )
                DashModuleCard(
                    title = "Micronutrientes",
                    subtitle = "Hierro, Zinc y Vitamina D",
                    icon = Icons.Rounded.Spa,
                    iconTint = NutriaGreen,
                    color = NutriaGreen.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.NUTRIENTES) }
                )
            }
        }

        // Fila 3: NutriChat IA y Diario Visual
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "NutriChat IA",
                    subtitle = "Consultas clínicas OMS",
                    icon = Icons.Rounded.ChatBubble,
                    iconTint = NutriaGreen,
                    color = NutriaGreen.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.CHAT_IA) }
                )
                DashModuleCard(
                    title = "Diario Visual",
                    subtitle = "Registro fotográfico",
                    icon = Icons.Rounded.PhotoCamera,
                    iconTint = NutriaOrange,
                    color = NutriaOrange.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.DIARIO_VISUAL) }
                )
            }
        }

        // Fila 4: Neurodesarrollo y Registro de Sueño
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Neurodesarrollo",
                    subtitle = "Hitos motores y cognitivos",
                    icon = Icons.Rounded.Psychology,
                    iconTint = NutriaSoftPurple,
                    color = NutriaSoftPurple.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.NEURODESARROLLO) }
                )
                DashModuleCard(
                    title = "Registro de Sueño",
                    subtitle = "Siestas y vigilia",
                    icon = Icons.Rounded.Bedtime,
                    iconTint = NutriaBlue,
                    color = NutriaBlue.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SUENO) }
                )
            }
        }

        // Fila 5: Recordatorios y Teleconsulta Pediatra
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Recordatorios",
                    subtitle = "Vacunas y alertas",
                    icon = Icons.Rounded.Notifications,
                    iconTint = NutriaPink,
                    color = NutriaPink.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.RECORDATORIOS) }
                )
                DashModuleCard(
                    title = "Pediatra & Citas",
                    subtitle = "Teleconsulta profesional",
                    icon = Icons.Rounded.MedicalServices,
                    iconTint = NutriaSoftTeal,
                    color = NutriaSoftTeal.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.PEDIATRA_DASHBOARD) }
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 7. DASHBOARDS DE OTROS ROLES (MAMÁ PRIMERIZA, NUTRIÓLOGO, GINECÓLOGO)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun DashboardMamaPrimerizaView(onNavigate: (Screen) -> Unit, onLogout: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("🤰 Mi Embarazo Semana a Semana", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
            Text("Semana 24 de gestación • Segundo Trimestre", fontSize = 13.sp, color = Color.Gray)
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = NutriaPink), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Bebé: Tamaño de una Mazorca de Maíz 🌽", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(6.dp))
                    Text("Peso estimado: 600g • Longitud: 30cm", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard("Nutrición Embarazo", "Ácido fólico y hierro", Icons.Rounded.Spa, NutriaGreen, NutriaGreen.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.NUTRIENTES) }
                DashModuleCard("Citas Prenatales", "Calendario y ecografías", Icons.Rounded.CalendarMonth, NutriaBlue, NutriaBlue.copy(alpha = 0.12f), Modifier.weight(1f)) { onNavigate(Screen.RECORDATORIOS) }
            }
        }
    }
}

@Composable
fun DashboardNutritionistView(onNavigate: (Screen) -> Unit, onLogout: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("🩺 Directorio Clínico Nutricional", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
            Text("Expedientes de Pacientes Pediátricos Activos", fontSize = 13.sp, color = Color.Gray)
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Paciente: Mateo Rivera (6m)", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Estado Nutricional: Eutrófico (Percentil 50 OMS)", fontSize = 12.sp, color = NutriaGreen)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onNavigate(Screen.CRECIMIENTO) }, colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)) {
                        Text("Ver Expediente & Curvas")
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardGinecologistView(onNavigate: (Screen) -> Unit, onLogout: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
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
// 8. VISTAS DE LOS 11 MÓDULOS DEL PADRE CON LÓGICA CLÍNICA REAL
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun LactanciaScreenView(onBack: () -> Unit) {
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

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Lactancia Materna", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isRunningLeft) Color(0xFFE8F5E9) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Seno Izquierdo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(fmt(secondsLeft), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = NutriaGreen)
                    Spacer(Modifier.height(12.dp))
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
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Seno Derecho", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(fmt(secondsRight), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = NutriaGreen)
                    Spacer(Modifier.height(12.dp))
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
fun SolidosScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Alimentación Complementaria & BLW", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
fun CrecimientoScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Curvas de Crecimiento OMS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Percentiles Actuales de Mateo", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Peso", fontSize = 12.sp, color = Color.Gray)
                        Text("7.8 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("P50 OMS", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Talla", fontSize = 12.sp, color = Color.Gray)
                        Text("67.0 cm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("P55 OMS", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cefálico", fontSize = 12.sp, color = Color.Gray)
                        Text("42.5 cm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("P50 OMS", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SuenoScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Registro de Sueño Infantil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Micronutrientes Clave", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Hitos de Neurodesarrollo", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Plan Nutricional Semanal", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
fun NutriChatScreenView(onBack: () -> Unit) {
    var messages by remember {
        mutableStateOf(
            listOf(
                Message("¡Hola! Soy tu asistente de nutrición NutrIA basado en guías OMS. ¿En qué te puedo orientar hoy?", false)
            )
        )
    }
    var input by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Asistente NutriIA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Spacer(Modifier.height(10.dp))

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
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Diario Visual de Comidas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Recordatorios & Vacunas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Teleconsulta con Especialistas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("⚙️ Configuración y Perfil", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)

        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(54.dp).clip(CircleShape).background(NutriaGreen), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(userName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(userEmail, fontSize = 13.sp, color = Color.Gray)
                }
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
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Text("Centro de Ayuda NutrIA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
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
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
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
