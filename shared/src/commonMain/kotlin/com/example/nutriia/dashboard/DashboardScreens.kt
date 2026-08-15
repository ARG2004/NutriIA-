package com.example.nutriia.dashboard

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriia.accesibilidad.*
import com.example.nutriia.shared.Screen

data class ChildData(
    val id: String,
    val name: String,
    val birthDate: String,
    val ageText: String,
    val stage: String,
    val weight: String,
    val height: String,
    val headCirc: String,
    val bmiPercentile: String
)

data class NavTabItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector
)

@Composable
fun NutriIADashboardScreenView(
    userEmail: String,
    userName: String,
    children: List<ChildData>,
    activeChildIndex: Int,
    onChildSelected: (Int) -> Unit,
    onNavigate: (Screen) -> Unit,
    onAddChild: () -> Unit,
    onToggleOffline: () -> Unit,
    isOffline: Boolean
) {
    val pagerState = rememberPagerState(initialPage = activeChildIndex, pageCount = { children.size })
    val activeChild = children.getOrNull(pagerState.currentPage) ?: children.first()

    LaunchedEffect(pagerState.currentPage) {
        onChildSelected(pagerState.currentPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        // ── Header Superior
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(NutriaGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👶", fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("¡Hola, $userName!", fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("NutriIA Pediátrica", fontSize = 11.sp, color = Color.Gray)
                        if (isOffline) {
                            Spacer(Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(NutriaOrange.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("Modo Offline", fontSize = 9.sp, color = NutriaOrange, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                IconButton(onClick = { onNavigate(Screen.AYUDA) }) {
                    Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = "Ayuda", tint = NutriaDarkGreen)
                }
                IconButton(onClick = { onNavigate(Screen.CONFIGURACION) }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Configuración", tint = NutriaDarkGreen)
                }
            }
        }

        // ── Carrusel de Niños (HorizontalPager)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            val child = children[page]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = NutriaGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(18.dp)) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(child.name, color = Color.White, fontWeight = FontWeight.Black, fontSize = 20.sp)
                                Spacer(Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(child.stage, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            IconButton(
                                onClick = onAddChild,
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = "Agregar hijo", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text("Edad: ${child.ageText} • Nacido: ${child.birthDate}", color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp)

                        Spacer(Modifier.weight(1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ChipInfo("Peso", child.weight)
                            ChipInfo("Talla", child.height)
                            ChipInfo("P. Cefálico", child.headCirc)
                            ChipInfo("Percentil OMS", child.bmiPercentile)
                        }
                    }
                }
            }
        }

        // Indicador de Puntos del Carrusel
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(children.size) { iteration ->
                val color = if (pagerState.currentPage == iteration) NutriaGreen else Color.LightGray
                Box(
                    modifier = Modifier
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(if (pagerState.currentPage == iteration) 10.dp else 7.dp)
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // ── Tarjeta Resumen de Etapa Nutricional
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(NutriaOrange.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🥑", fontSize = 26.sp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Etapa Nutricional Recomendada", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                    Spacer(Modifier.height(2.dp))
                    Text("Alimentación Complementaria BLW (Sólidos seguros)", fontSize = 12.sp, color = Color.Gray)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(20.dp))
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Grilla con los 9 Módulos Oficiales de NutrIA
        Text(
            text = "Módulos de Salud & Nutrición",
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = NutriaDarkGreen,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Lactancia",
                    subtitle = "Cronómetro & tomas",
                    icon = Icons.Rounded.ChildCare,
                    iconTint = NutriaGreen,
                    color = NutriaGreen.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.LACTANCIA) }
                )
                DashModuleCard(
                    title = "Alimentos BLW",
                    subtitle = "Sólidos & alergias",
                    icon = Icons.Rounded.Restaurant,
                    iconTint = NutriaOrange,
                    color = NutriaOrange.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SOLIDOS) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Curvas OMS",
                    subtitle = "Peso, talla & percentil",
                    icon = Icons.AutoMirrored.Rounded.ShowChart,
                    iconTint = NutriaSoftTeal,
                    color = NutriaSoftTeal.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.CRECIMIENTO) }
                )
                DashModuleCard(
                    title = "Registro Sueño",
                    subtitle = "Siestas & descansos",
                    icon = Icons.Rounded.Bedtime,
                    iconTint = NutriaSoftPurple,
                    color = NutriaSoftPurple.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.SUENO) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Nutrientes",
                    subtitle = "Hierro, Vitamina D",
                    icon = Icons.Rounded.Medication,
                    iconTint = NutriaPink,
                    color = NutriaPink.copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.NUTRIENTES) }
                )
                DashModuleCard(
                    title = "Directorio Médico",
                    subtitle = "Pediatras & Citas",
                    icon = Icons.Rounded.MedicalServices,
                    iconTint = Color(0xFF5C6BC0),
                    color = Color(0xFF5C6BC0).copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.PEDIATRA_DASHBOARD) }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashModuleCard(
                    title = "Diario Visual",
                    subtitle = "Análisis IA platos",
                    icon = Icons.Rounded.PhotoCamera,
                    iconTint = Color(0xFF26A69A),
                    color = Color(0xFF26A69A).copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.DIARIO_VISUAL) }
                )
                DashModuleCard(
                    title = "Alertas Vacunas",
                    subtitle = "Esquema oficial MX",
                    icon = Icons.Rounded.NotificationsActive,
                    iconTint = Color(0xFFEF5350),
                    color = Color(0xFFEF5350).copy(alpha = 0.15f),
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigate(Screen.RECORDATORIOS) }
                )
            }

            DashModuleCard(
                title = "NutriBot Asistente Clínico",
                subtitle = "Consultas de nutrición infantil con inteligencia artificial",
                icon = Icons.Rounded.AutoAwesome,
                iconTint = NutriaDarkGreen,
                color = NutriaGreen.copy(alpha = 0.2f),
                modifier = Modifier.fillMaxWidth(),
                onClick = { onNavigate(Screen.CHAT_IA) }
            )
        }
    }
}

@Composable
fun NutritionistDashboardScreenView(onNavigate: (Screen) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("🩺 Panel Nutriólogo", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                Text("Gestión de Expedientes Pediátricos", fontSize = 13.sp, color = Color.Gray)
            }
            IconButton(onClick = { onNavigate(Screen.CONFIGURACION) }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Ajustes", tint = NutriaDarkGreen)
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = NutriaSoftTeal.copy(alpha = 0.15f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Verified, contentDescription = null, tint = NutriaSoftTeal, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Cédula Profesional Verificada SEP", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                    Text("Autorizado para emitir diagnósticos nutricionales", fontSize = 11.sp, color = Color.Gray)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Text("Pacientes Asignados (4)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Spacer(Modifier.height(10.dp))

        val pacientes = listOf(
            Triple("Santiago Rivera", "10 meses", "Lactancia & BLW"),
            Triple("Valentina López", "14 meses", "Alimentación Completa"),
            Triple("Mateo Gómez", "6 meses", "Inicio de Sólidos"),
            Triple("Sofia Hernández", "24 meses", "Nutrición Transición")
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pacientes) { p ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onNavigate(Screen.PACIENTE_EXPEDIENTE) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(NutriaGreen.copy(0.12f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, contentDescription = null, tint = NutriaGreen)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(p.first, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${p.second} • ${p.third}", fontSize = 12.sp, color = Color.Gray)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = NutriaGreen)
                    }
                }
            }
        }
    }
}

@Composable
fun EmbarazoDashboardScreenView(onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(20.dp)) {
        Text("💖 Panel Gestacional", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Seguimiento prenatal & nutrición de la mamá", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(16.dp))

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = NutriaPink), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text("Semana 24 de Gestación", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Segundo Trimestre • Faltan 112 días", color = Color.White.copy(0.9f), fontSize = 12.sp)
                Spacer(Modifier.height(14.dp))
                LinearProgressIndicator(progress = { 24 / 40f }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape), color = Color.White, trackColor = Color.White.copy(0.3f))
            }
        }
    }
}

@Composable
fun GinecologoDashboardScreenView(onNavigate: (Screen) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(20.dp)) {
        Text("🩺 Panel Ginecología Obstétrica", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Text("Seguimiento Clínico Materno-Fetal", fontSize = 13.sp, color = Color.Gray)
    }
}

@Composable
fun DirectorioGinecologosScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás") }
        Text("Directorio de Ginecología", fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PacienteExpedienteScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).padding(20.dp)) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás") }
        Text("Expediente Pediátrico", fontSize = 20.sp, fontWeight = FontWeight.Bold)
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
        Column(modifier = Modifier.padding(14.dp)) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2C3E50))
            Spacer(Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = Color.Gray)
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
fun MainAppScaffold(
    currentTab: Screen,
    isOffline: Boolean,
    onTabSelected: (Screen) -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        bottomBar = {
            BottomNavBar(currentTab = currentTab, onTabSelected = onTabSelected)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            content()
        }
    }
}

@Composable
fun BottomNavBar(
    currentTab: Screen,
    onTabSelected: (Screen) -> Unit
) {
    val items = listOf(
        NavTabItem(Screen.DASHBOARD_PARENT, "Inicio", Icons.Rounded.Home),
        NavTabItem(Screen.LACTANCIA, "Lactancia", Icons.Rounded.ChildCare),
        NavTabItem(Screen.SOLIDOS, "Sólidos", Icons.Rounded.Restaurant),
        NavTabItem(Screen.CRECIMIENTO, "Curvas OMS", Icons.AutoMirrored.Rounded.ShowChart),
        NavTabItem(Screen.CHAT_IA, "NutriBot", Icons.Rounded.AutoAwesome)
    )

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.screen
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.screen) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NutriaGreen,
                    selectedTextColor = NutriaDarkGreen,
                    indicatorColor = NutriaGreen.copy(alpha = 0.15f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}
