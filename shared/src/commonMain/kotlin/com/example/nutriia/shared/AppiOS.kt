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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
// PALETA DE COLORES OFICIAL DE NUTRIA
// ═══════════════════════════════════════════════════════════════════════════
val NutriaGreen     = Color(0xFF689F38)
val NutriaDarkGreen = Color(0xFF33691E)
val NutriaOrange    = Color(0xFFFF8F00)
val NutriaBgCrema   = Color(0xFFF8F9F3)
val NutriaSoftPurple= Color(0xFF9C8FE0)
val NutriaSoftTeal  = Color(0xFF4DB6AC)
val NutriaPink      = Color(0xFFEC9BBF)
val NutriaBlue      = Color(0xFF64B5F6)
val NutriaGineRosa  = Color(0xFFF06292)

enum class Screen {
    LOGIN, REGISTER_TYPE, REGISTER_FORM, DASHBOARD, LACTANCIA, CHAT_IA, CRECIMIENTO, PERFIL
}

@Composable
fun AppiOS() {
    var currentScreen by remember { mutableStateOf(Screen.LOGIN) }
    var selectedRole by remember { mutableStateOf("Padre") }
    var userEmail by remember { mutableStateOf("familia@nutriia.com") }

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
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = NutriaBgCrema
        ) {
            Crossfade(targetState = currentScreen, animationSpec = tween(250)) { screen ->
                when (screen) {
                    Screen.LOGIN -> NutriaLoginScreen(
                        onLoginSuccess = { email ->
                            userEmail = email.ifBlank { "familia@nutriia.com" }
                            currentScreen = Screen.DASHBOARD
                        },
                        onNavigateToRegister = { currentScreen = Screen.REGISTER_TYPE }
                    )
                    Screen.REGISTER_TYPE -> RegisterTypeScreen(
                        onRoleSelected = { role ->
                            selectedRole = role
                            currentScreen = Screen.REGISTER_FORM
                        },
                        onBackToLogin = { currentScreen = Screen.LOGIN }
                    )
                    Screen.REGISTER_FORM -> RegisterFormScreen(
                        role = selectedRole,
                        onRegistered = { email ->
                            userEmail = email
                            currentScreen = Screen.DASHBOARD
                        },
                        onBack = { currentScreen = Screen.REGISTER_TYPE }
                    )
                    Screen.DASHBOARD -> MainAppScaffold(
                        currentTab = Screen.DASHBOARD,
                        onTabSelected = { currentScreen = it }
                    ) {
                        DashboardView(
                            userEmail = userEmail,
                            onNavigate = { currentScreen = it }
                        )
                    }
                    Screen.LACTANCIA -> MainAppScaffold(
                        currentTab = Screen.LACTANCIA,
                        onTabSelected = { currentScreen = it }
                    ) {
                        LactanciaView(onBack = { currentScreen = Screen.DASHBOARD })
                    }
                    Screen.CHAT_IA -> MainAppScaffold(
                        currentTab = Screen.CHAT_IA,
                        onTabSelected = { currentScreen = it }
                    ) {
                        NutriChatIAView(onBack = { currentScreen = Screen.DASHBOARD })
                    }
                    Screen.CRECIMIENTO -> MainAppScaffold(
                        currentTab = Screen.CRECIMIENTO,
                        onTabSelected = { currentScreen = it }
                    ) {
                        CrecimientoOMSView(onBack = { currentScreen = Screen.DASHBOARD })
                    }
                    Screen.PERFIL -> MainAppScaffold(
                        currentTab = Screen.PERFIL,
                        onTabSelected = { currentScreen = it }
                    ) {
                        PerfilView(
                            userEmail = userEmail,
                            role = selectedRole,
                            onLogout = { currentScreen = Screen.LOGIN }
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 1. PANTALLA DE LOGIN REAL (IDÉNTICA A ANDROID)
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
            Spacer(Modifier.height(50.dp))

            // Logo & Slogan
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, NutriaGreen, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌱", fontSize = 52.sp)
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "NutrIA",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NutriaDarkGreen
                )
                Text(
                    text = "Nutre su hoy, protege su mañana",
                    fontSize = 14.sp,
                    color = NutriaDarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(28.dp))

            // Tarjeta de Iniciar Sesión
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

                    // Campo Correo
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Correo Electrónico", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = NutriaGreen) },
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

                    // Campo Contraseña
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Contraseña", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = NutriaGreen) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    null,
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

                    // Olvidaste contraseña
                    TextButton(
                        onClick = { showReset = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("¿Olvidaste tu contraseña?", color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(16.dp))

                    // Botón ENTRAR
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
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }

            // Crear cuenta
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

        // Modal Recuperar Contraseña
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
// 2. PANTALLA DE SELECCIÓN DE ROL DE REGISTRO
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
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color.White)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = NutriaDarkGreen)
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "¿Cómo deseas unirte?",
            fontSize = 26.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NutriaDarkGreen
        )
        Text(
            text = "Selecciona tu rol para personalizar tu experiencia clínica y nutricional.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Spacer(Modifier.height(24.dp))

        val roles = listOf(
            Triple("Padre / Madre de Familia", "Seguimiento nutricional, crecimiento y lactancia", NutriaGreen),
            Triple("Nutriólogo Clínico", "Directorio, expedientes y cálculo de dietas", NutriaSoftTeal),
            Triple("Mamá Primeriza", "Guía paso a paso desde el embarazo hasta la lactancia", NutriaPink),
            Triple("Ginecólogo Obstetra", "Control prenatal y seguimiento materno-fetal", NutriaGineRosa)
        )

        roles.forEach { (title, subtitle, color) ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onRoleSelected(title) },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(color.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            when (title) {
                                "Padre / Madre de Familia" -> "👨‍👩‍👧"
                                "Nutriólogo Clínico" -> "🩺"
                                "Mamá Primeriza" -> "🤰"
                                else -> "🏥"
                            },
                            fontSize = 24.sp
                        )
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
                        Spacer(Modifier.height(2.dp))
                        Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = color)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 3. FORMULARIO DE REGISTRO
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterFormScreen(
    role: String,
    onRegistered: (String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.White)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = NutriaDarkGreen)
        }

        Spacer(Modifier.height(20.dp))

        Text("Registro como $role", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Completa tus datos para crear tu cuenta.", fontSize = 13.sp, color = Color.Gray)

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
                    onClick = { onRegistered(email.ifBlank { "usuario@nutriia.com" }) },
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
// 4. SCAFFOLD PRINCIPAL CON BOTTOM NAVIGATION BAR
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
                val items = listOf(
                    Triple(Screen.DASHBOARD, "Inicio", "🏠"),
                    Triple(Screen.LACTANCIA, "Lactancia", "🤱"),
                    Triple(Screen.CHAT_IA, "NutriIA Chat", "💬"),
                    Triple(Screen.CRECIMIENTO, "Crecimiento", "📈"),
                    Triple(Screen.PERFIL, "Perfil", "👤")
                )

                items.forEach { (tab, label, emoji) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Text(emoji, fontSize = if (currentTab == tab) 22.sp else 18.sp)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == tab) NutriaDarkGreen else Color.Gray
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
// 5. DASHBOARD REAL DE NUTRIA
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun DashboardView(
    userEmail: String,
    onNavigate: (Screen) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera con selector de hijo
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "¡Hola, Familia! 🌿",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NutriaDarkGreen
                    )
                    Text(
                        text = "Plan Nutricional Activo OMS",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NutriaGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👶", fontSize = 24.sp)
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
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Mateo Rivera", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(Modifier.height(4.dp))
                        Text("6 meses y 15 días • Alimentación Complementaria", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ChipInfo(label = "7.8 kg", subtitle = "Peso")
                            ChipInfo(label = "67.0 cm", subtitle = "Talla")
                            ChipInfo(label = "42.5 cm", subtitle = "C. Cefálico")
                        }
                    }
                }
            }
        }

        // Módulos
        item {
            Text("Módulos Principales", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Lactancia Materna",
                    subtitle = "Cronómetro y tomas",
                    emoji = "🤱",
                    color = NutriaOrange.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.LACTANCIA) }
                )
                DashModuleCard(
                    title = "NutriChat IA",
                    subtitle = "Consultas clínicas OMS",
                    emoji = "💬",
                    color = NutriaGreen.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.CHAT_IA) }
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Curvas OMS",
                    subtitle = "Percentiles y peso",
                    emoji = "📈",
                    color = NutriaBlue.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.CRECIMIENTO) }
                )
                DashModuleCard(
                    title = "Sólidos & BLW",
                    subtitle = "Guías de introducción",
                    emoji = "🥑",
                    color = NutriaSoftPurple.copy(alpha = 0.12f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.CRECIMIENTO) }
                )
            }
        }

        // Consejo del día
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Recomendación de la OMS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "A los 6 meses, los alimentos deben ser ricos en hierro y zinc, como carnes magras, legumbres y cereales fortificados.",
                        fontSize = 13.sp,
                        color = Color(0xFF555555),
                        lineHeight = 18.sp
                    )
                }
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
    emoji: String,
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
                Text(emoji, fontSize = 24.sp)
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 11.sp, color = Color.Gray)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 6. LACTANCIA VIEW CON CRONÓMETRO REAL
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun LactanciaView(onBack: () -> Unit) {
    var isRunningLeft by remember { mutableStateOf(false) }
    var isRunningRight by remember { mutableStateOf(false) }
    var secondsLeft by remember { mutableStateOf(0) }
    var secondsRight by remember { mutableStateOf(0) }

    LaunchedEffect(isRunningLeft) {
        while (isRunningLeft) {
            delay(1000)
            secondsLeft++
        }
    }

    LaunchedEffect(isRunningRight) {
        while (isRunningRight) {
            delay(1000)
            secondsRight++
        }
    }

    fun fmt(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("🤱 Registro de Lactancia", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Seno Izquierdo
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
                        onClick = {
                            isRunningLeft = !isRunningLeft
                            if (isRunningLeft) isRunningRight = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunningLeft) Color(0xFFC62828) else NutriaGreen)
                    ) {
                        Text(if (isRunningLeft) "Pausar" else "Iniciar")
                    }
                }
            }

            // Seno Derecho
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
                        onClick = {
                            isRunningRight = !isRunningRight
                            if (isRunningRight) isRunningLeft = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isRunningRight) Color(0xFFC62828) else NutriaGreen)
                    ) {
                        Text(if (isRunningRight) "Pausar" else "Iniciar")
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 7. NUTRI-CHAT IA VIEW
// ═══════════════════════════════════════════════════════════════════════════
data class Message(val text: String, val isUser: Boolean)

@Composable
fun NutriChatIAView(onBack: () -> Unit) {
    var messages by remember {
        mutableStateOf(
            listOf(
                Message("¡Hola! Soy tu asistente de nutrición NutrIA basado en guías OMS. ¿Cómo te puedo orientar?", false)
            )
        )
    }
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(16.dp)
    ) {
        Text("💬 Asistente NutriIA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) NutriaGreen else Color.White
                        )
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Escribe una consulta...") },
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
                        messages = messages + Message("Entendido. Siguiendo las directrices OMS, evaluamos esto de acuerdo a la etapa del lactante.", false)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)
            ) {
                Text("➤")
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 8. CRECIMIENTO OMS VIEW
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun CrecimientoOMSView(onBack: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("📈 Curvas de Crecimiento OMS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Percentiles Actuales", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Peso", fontSize = 12.sp, color = Color.Gray)
                        Text("7.8 kg", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("Percentil 50", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Talla", fontSize = 12.sp, color = Color.Gray)
                        Text("67.0 cm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("Percentil 55", fontSize = 10.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Cefálico", fontSize = 12.sp, color = Color.Gray)
                        Text("42.5 cm", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaGreen)
                        Text("Percentil 50", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
// 9. PERFIL VIEW
// ═══════════════════════════════════════════════════════════════════════════
@Composable
fun PerfilView(
    userEmail: String,
    role: String,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("👤 Perfil de Usuario", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(54.dp).clip(CircleShape).background(NutriaGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🌱", fontSize = 28.sp)
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(userEmail, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Rol: $role", fontSize = 13.sp, color = Color.Gray)
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

// ═══════════════════════════════════════════════════════════════════════════
// 10. FONDO ANIMADO MINIMALISTA (IDÉNTICO A ANDROID)
// ═══════════════════════════════════════════════════════════════════════════
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
