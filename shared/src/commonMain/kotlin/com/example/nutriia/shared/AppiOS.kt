package com.example.nutriia.shared

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

enum class IOSScreen {
    SPLASH, LOGIN, DASHBOARD, LACTANCIA, CHAT_IA, CRECIMIENTO, PERFIL
}

@Composable
fun AppiOS() {
    var currentScreen by remember { mutableStateOf(IOSScreen.SPLASH) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF2E7D32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFE8F5E9),
            onPrimaryContainer = Color(0xFF1B5E20),
            secondary = Color(0xFF00796B),
            surface = Color(0xFFF9FAFC),
            onSurface = Color(0xFF1A1C1E)
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Crossfade(targetState = currentScreen, animationSpec = tween(300)) { screen ->
                when (screen) {
                    IOSScreen.SPLASH -> SplashScreen(onFinish = { currentScreen = IOSScreen.DASHBOARD })
                    IOSScreen.LOGIN -> LoginScreen(
                        onLoginSuccess = { currentScreen = IOSScreen.DASHBOARD },
                        onSkip = { currentScreen = IOSScreen.DASHBOARD }
                    )
                    IOSScreen.DASHBOARD -> MainAppContainer(
                        currentTab = IOSScreen.DASHBOARD,
                        onTabSelected = { currentScreen = it }
                    ) {
                        DashboardContent(onNavigate = { currentScreen = it })
                    }
                    IOSScreen.LACTANCIA -> MainAppContainer(
                        currentTab = IOSScreen.LACTANCIA,
                        onTabSelected = { currentScreen = it }
                    ) {
                        LactanciaContent(onBack = { currentScreen = IOSScreen.DASHBOARD })
                    }
                    IOSScreen.CHAT_IA -> MainAppContainer(
                        currentTab = IOSScreen.CHAT_IA,
                        onTabSelected = { currentScreen = it }
                    ) {
                        ChatIAContent(onBack = { currentScreen = IOSScreen.DASHBOARD })
                    }
                    IOSScreen.CRECIMIENTO -> MainAppContainer(
                        currentTab = IOSScreen.CRECIMIENTO,
                        onTabSelected = { currentScreen = it }
                    ) {
                        CrecimientoContent(onBack = { currentScreen = IOSScreen.DASHBOARD })
                    }
                    IOSScreen.PERFIL -> MainAppContainer(
                        currentTab = IOSScreen.PERFIL,
                        onTabSelected = { currentScreen = it }
                    ) {
                        PerfilContent(onLogout = { currentScreen = IOSScreen.LOGIN })
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 1. Splash Screen
// ----------------------------------------------------
@Composable
fun SplashScreen(onFinish: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    LaunchedEffect(Unit) {
        delay(2000)
        onFinish()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF43A047))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🌱",
                    fontSize = 48.sp,
                    modifier = Modifier.animateContentSize()
                )
            }

            Text(
                text = "NutrIA",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Nutrición Infantil & Lactancia Materna OMS",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.9f),
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ----------------------------------------------------
// 2. Main App Container con Bottom Navigation
// ----------------------------------------------------
@Composable
fun MainAppContainer(
    currentTab: IOSScreen,
    onTabSelected: (IOSScreen) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color.White,
                tonalElevation = 8.dp
            ) {
                val tabs = listOf(
                    Triple(IOSScreen.DASHBOARD, "Inicio", "🏠"),
                    Triple(IOSScreen.LACTANCIA, "Lactancia", "🤱"),
                    Triple(IOSScreen.CHAT_IA, "NutriIA Chat", "💬"),
                    Triple(IOSScreen.CRECIMIENTO, "Crecimiento", "📈"),
                    Triple(IOSScreen.PERFIL, "Perfil", "👤")
                )

                tabs.forEach { (tab, label, emoji) ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { onTabSelected(tab) },
                        icon = {
                            Text(text = emoji, fontSize = if (currentTab == tab) 22.sp else 18.sp)
                        },
                        label = {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentTab == tab) Color(0xFF2E7D32) else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF2E7D32),
                            indicatorColor = Color(0xFFE8F5E9)
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            content()
        }
    }
}

// ----------------------------------------------------
// 3. Dashboard Content
// ----------------------------------------------------
@Composable
fun DashboardContent(onNavigate: (IOSScreen) -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "¡Hola, Familia! 🌿",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "Seguimiento nutricional activo",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👶", fontSize = 24.sp)
                }
            }
        }

        item {
            // Banner Guías OMS
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Estándares OMS & Lactancia",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Algoritmos validados para cada etapa del desarrollo infantil.",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                    Text("📋", fontSize = 36.sp)
                }
            }
        }

        item {
            Text(
                text = "Módulos Principales",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
        }

        item {
            // Grid de módulos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModuleCard(
                    title = "Lactancia Materna",
                    subtitle = "Cronómetro & tomas",
                    emoji = "🤱",
                    color = Color(0xFFFFF3E0),
                    iconColor = Color(0xFFE65100),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(IOSScreen.LACTANCIA) }
                )
                ModuleCard(
                    title = "NutriChat IA",
                    subtitle = "Asistente clínico",
                    emoji = "💬",
                    color = Color(0xFFE8F5E9),
                    iconColor = Color(0xFF1B5E20),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(IOSScreen.CHAT_IA) }
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModuleCard(
                    title = "Crecimiento",
                    subtitle = "Percentiles OMS",
                    emoji = "📈",
                    color = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF0D47A1),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(IOSScreen.CRECIMIENTO) }
                )
                ModuleCard(
                    title = "Alimentación",
                    subtitle = "Guías & Sólidos",
                    emoji = "🥑",
                    color = Color(0xFFF3E5F5),
                    iconColor = Color(0xFF4A148C),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(IOSScreen.CRECIMIENTO) }
                )
            }
        }

        item {
            // Consejo del día
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Recomendación de hoy",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "La lactancia a libre demanda fortalece el vínculo afectivo y asegura la ingesta calórica adecuada durante los primeros meses.",
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
fun ModuleCard(
    title: String,
    subtitle: String,
    emoji: String,
    color: Color,
    iconColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Text(emoji, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2C3E50)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

// ----------------------------------------------------
// 4. Lactancia Screen con Cronómetro
// ----------------------------------------------------
@Composable
fun LactanciaContent(onBack: () -> Unit) {
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

    fun formatTime(sec: Int): String {
        val m = sec / 60
        val s = sec % 60
        return "${if (m < 10) "0$m" else "$m"}:${if (s < 10) "0$s" else "$s"}"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "🤱 Registro de Lactancia",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )

        Text(
            text = "Controla el tiempo de succión de cada lado.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Lado Izquierdo
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isRunningLeft) Color(0xFFE8F5E9) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Lado Izquierdo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatTime(secondsLeft),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            isRunningLeft = !isRunningLeft
                            if (isRunningLeft) isRunningRight = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunningLeft) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    ) {
                        Text(if (isRunningLeft) "Pausar" else "Iniciar")
                    }
                }
            }

            // Lado Derecho
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isRunningRight) Color(0xFFE8F5E9) else Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Lado Derecho", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = formatTime(secondsRight),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            isRunningRight = !isRunningRight
                            if (isRunningRight) isRunningLeft = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunningRight) Color(0xFFC62828) else Color(0xFF2E7D32)
                        )
                    ) {
                        Text(if (isRunningRight) "Pausar" else "Iniciar")
                    }
                }
            }
        }

        // Historial rápido
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Últimas tomas registradas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2C3E50)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text("• Hoy 08:30 AM — 15 min (Izquierdo)", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Hoy 05:45 AM — 20 min (Ambos lados)", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Ayer 11:15 PM — 12 min (Derecho)", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

// ----------------------------------------------------
// 5. NutriChat IA Screen
// ----------------------------------------------------
data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatIAContent(onBack: () -> Unit) {
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("¡Hola! Soy tu asistente de nutrición NutrIA con protocolos OMS. ¿En qué te puedo orientar hoy?", false)
            )
        )
    }
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F9FA))
            .padding(16.dp)
    ) {
        Text(
            text = "💬 Asistente NutriIA",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )
        Text(
            text = "Consultas sobre alimentación, lactancia y percentiles.",
            fontSize = 12.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(12.dp))

        // Mensajes
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (msg.isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            topEnd = 14.dp,
                            bottomStart = if (msg.isUser) 14.dp else 2.dp,
                            bottomEnd = if (msg.isUser) 2.dp else 14.dp
                        ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (msg.isUser) Color(0xFF2E7D32) else Color.White
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Text(
                            text = msg.text,
                            fontSize = 14.sp,
                            color = if (msg.isUser) Color.White else Color(0xFF2C3E50),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preguntas sugeridas
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val suggestions = listOf(
                "¿Cuándo iniciar sólidos?",
                "Signos de hambre",
                "Conservar leche materna"
            )
            items(suggestions) { sug ->
                SuggestionChip(
                    onClick = {
                        messages = messages + ChatMessage(sug, true)
                        messages = messages + ChatMessage(
                            when (sug) {
                                "¿Cuándo iniciar sólidos?" -> "La OMS recomienda lactancia materna exclusiva hasta los 6 meses de edad. A partir de los 6 meses se introducen alimentos complementarios seguros."
                                "Signos de hambre" -> "Llevarse las manos a la boca, girar la cabeza buscando el pecho y movimientos de succión son signos tempranos de hambre."
                                else -> "La leche materna extraída se conserva hasta 4 horas a temperatura ambiente y hasta 4 días en refrigerador estándar."
                            },
                            false
                        )
                    },
                    label = { Text(sug, fontSize = 11.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("Escribe una duda...", fontSize = 13.sp) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText
                        inputText = ""
                        messages = messages + ChatMessage(text, true)
                        messages = messages + ChatMessage("Entendido. Siguiendo las pautas pediátricas de la OMS, evaluamos este requerimiento de acuerdo a los meses de desarrollo del bebé.", false)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) {
                Text("➤", fontSize = 16.sp)
            }
        }
    }
}

// ----------------------------------------------------
// 6. Crecimiento Screen
// ----------------------------------------------------
@Composable
fun CrecimientoContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "📈 Curvas de Crecimiento OMS",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )
        Text(
            text = "Percentiles estandarizados de peso y talla.",
            fontSize = 13.sp,
            color = Color.Gray
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Estado Actual del Bebé", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MetricItem(label = "Peso", value = "7.2 kg", subtitle = "Percentil 50 (Normal)")
                    MetricItem(label = "Talla", value = "65.5 cm", subtitle = "Percentil 55 (Normal)")
                    MetricItem(label = "C. Cefálico", value = "42.0 cm", subtitle = "Percentil 50")
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✅", fontSize = 28.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Desarrollo Óptimo",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = "La curva de ganancia ponderal sigue la trayectoria esperada por los estándares internacionales.",
                        fontSize = 12.sp,
                        color = Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}

@Composable
fun MetricItem(label: String, value: String, subtitle: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = subtitle, fontSize = 10.sp, color = Color.Gray)
    }
}

// ----------------------------------------------------
// 7. Perfil Screen
// ----------------------------------------------------
@Composable
fun PerfilContent(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "👤 Perfil de Usuario",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ARG", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text("Familia Rivera", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("Rol: Mamá / Papá", fontSize = 13.sp, color = Color.Gray)
                    Text("Bebé: 6 meses", fontSize = 13.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Configuración de la App", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(10.dp))
                Text("• Versión: 2.1.2 (iOS Native Framework)", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Motor: Compose Multiplatform + Skiko", fontSize = 13.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Guías clínicas: OMS 2026", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

// ----------------------------------------------------
// 8. Login Screen
// ----------------------------------------------------
@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onSkip: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9FAFC))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E7D32)),
            contentAlignment = Alignment.Center
        ) {
            Text("🌱", fontSize = 36.sp)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bienvenido a NutrIA",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B5E20)
        )
        Text(
            text = "Ingresa para acceder a tus registros",
            fontSize = 13.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onLoginSuccess,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continuar como Familia", fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
        }
    }
}
