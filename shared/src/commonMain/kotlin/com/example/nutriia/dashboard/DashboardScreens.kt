package com.example.nutriia.dashboard

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriia.shared.Screen

// ─── Colores Dashboard ───────────────────────────────────────────────────
private val DashGreen    = Color(0xFF689F38)
private val DashDark     = Color(0xFF33691E)
private val DashBg       = Color(0xFFF8F9F3)
private val DashTeal     = Color(0xFF4DB6AC)
private val DashOrange   = Color(0xFFFF8F00)
private val DashRosa     = Color(0xFFEC9BBF)
private val DashRosaGine = Color(0xFFF06292)
private val DashBlue     = Color(0xFF1976D2)
private val DashPurple   = Color(0xFF9C8FE0)

// ═════════════════════════════════════════════════════════════════════════
// PARENT DASHBOARD — Dashboard principal para padres/familia
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun ParentDashboardScreen(
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(DashBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("¡Hola! 👋", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DashDark)
                    Text("Panel de familia", fontSize = 14.sp, color = Color.Gray)
                }
                IconButton(onClick = { onNavigate(Screen.ACCESIBILIDAD_INICIAL) }) {
                    Icon(Icons.Rounded.Accessibility, null, tint = DashGreen, modifier = Modifier.size(28.dp))
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Logout, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            // Quick stats
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Días activos", "42", DashGreen, Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                StatCard("Comidas", "156", DashOrange, Icons.Rounded.Restaurant, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Peso (kg)", "8.2", DashTeal, Icons.Rounded.MonitorWeight, Modifier.weight(1f))
                StatCard("Talla (cm)", "72", DashBlue, Icons.Rounded.Straighten, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))

            Text("Módulos", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = DashDark)
            Spacer(Modifier.height(12.dp))

            DashModule("🍎 Sólidos BLW",       "Alimentación complementaria",    DashGreen,  Icons.Rounded.Restaurant)      { onNavigate(Screen.SOLIDOS) }
            DashModule("📈 Curvas de crecimiento","Peso, talla y perímetro",      DashBlue,   Icons.Rounded.ShowChart)        { onNavigate(Screen.CRECIMIENTO) }
            DashModule("😴 Registro de sueño",  "Patrones de descanso",           DashPurple, Icons.Rounded.Bedtime)          { onNavigate(Screen.SUENO) }
            DashModule("🥗 Calculadora nutricional","Valores y nutrientes",       DashOrange, Icons.Rounded.Calculate)        { onNavigate(Screen.NUTRIENTES) }
            DashModule("🤖 Chat con IA",         "Asistente nutricional",         DashTeal,   Icons.Rounded.SmartToy)         { onNavigate(Screen.CHAT_IA) }
            DashModule("🍼 Lactancia",           "Registro de tomas",             DashRosa,   Icons.Rounded.WaterDrop)        { onNavigate(Screen.LACTANCIA) }
            DashModule("👨‍⚕️ Directorio pediatras","Encuentra especialistas",     DashRosaGine, Icons.Rounded.LocalHospital) { onNavigate(Screen.PEDIATRA_DASHBOARD) }
            DashModule("📹 Teleconsulta",        "Videollamada con nutriólogo",    DashBlue,   Icons.Rounded.VideoCall)        { onNavigate(Screen.TELECONSULTA) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// NUTRITIONIST DASHBOARD
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun NutritionistDashboardScreen(
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(DashBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Panel profesional 🏥", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DashDark)
                    Text("Nutriólogo certificado", fontSize = 14.sp, color = DashTeal)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Logout, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Pacientes", "24", DashTeal, Icons.Rounded.People, Modifier.weight(1f))
                StatCard("Consultas hoy", "5", DashOrange, Icons.Rounded.EventNote, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))

            DashModule("📋 Expedientes",          "Historiales clínicos",    DashTeal,   Icons.Rounded.FolderShared)  { onNavigate(Screen.PACIENTE_EXPEDIENTE) }
            DashModule("📹 Teleconsulta",          "Videollamada con pacientes", DashBlue,   Icons.Rounded.VideoCall) { onNavigate(Screen.TELECONSULTA) }
            DashModule("🤖 Chat IA clínico",       "Asistente de diagnóstico", DashPurple, Icons.Rounded.SmartToy)   { onNavigate(Screen.CHAT_IA) }
            DashModule("🥗 Calculadora nutricional","Planes alimenticios",   DashOrange, Icons.Rounded.Calculate)    { onNavigate(Screen.NUTRIENTES) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// PREGNANCY DASHBOARD
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun PregnancyDashboardScreen(
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(DashBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Mi embarazo 🤰", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DashDark)
                    Text("Semana 24 de gestación", fontSize = 14.sp, color = DashRosa)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Logout, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Semana", "24", DashRosa, Icons.Rounded.CalendarMonth, Modifier.weight(1f))
                StatCard("Peso (kg)", "62.5", DashTeal, Icons.Rounded.MonitorWeight, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))

            DashModule("🥗 Nutrición embarazo", "Plan alimenticio prenatal", DashRosa,   Icons.Rounded.Restaurant)   { onNavigate(Screen.NUTRICION_EMBARAZO) }
            DashModule("📅 Citas y controles",   "Agenda de revisiones",     DashTeal,   Icons.Rounded.EventNote)    { onNavigate(Screen.CITAS_EMBARAZO) }
            DashModule("🤖 Chat IA prenatal",    "Consultas sobre embarazo", DashPurple, Icons.Rounded.SmartToy)     { onNavigate(Screen.CHAT_IA) }
            DashModule("📹 Teleconsulta",        "Videollamada con doctor",  DashBlue,   Icons.Rounded.VideoCall)    { onNavigate(Screen.TELECONSULTA) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// GYNECOLOGIST DASHBOARD
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun GynecologistDashboardScreen(
    onNavigate: (Screen) -> Unit,
    onLogout: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(DashBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Panel ginecología 👩‍⚕️", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = DashDark)
                    Text("Especialista certificado", fontSize = 14.sp, color = DashRosaGine)
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Rounded.Logout, null, tint = Color.Gray, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Pacientes", "18", DashRosaGine, Icons.Rounded.People, Modifier.weight(1f))
                StatCard("Citas hoy", "4", DashOrange, Icons.Rounded.EventNote, Modifier.weight(1f))
            }
            Spacer(Modifier.height(24.dp))

            DashModule("📋 Expedientes embarazo", "Historiales de gestación", DashRosaGine, Icons.Rounded.FolderShared) { onNavigate(Screen.EXPEDIENTE_EMBARAZO) }
            DashModule("📹 Teleconsulta",          "Videollamada con pacientes", DashBlue, Icons.Rounded.VideoCall)     { onNavigate(Screen.TELECONSULTA) }
            DashModule("🤖 Chat IA clínico",       "Asistente de diagnóstico", DashPurple, Icons.Rounded.SmartToy)     { onNavigate(Screen.CHAT_IA) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ─── Componentes reutilizables ───────────────────────────────────────────
@Composable
private fun StatCard(label: String, value: String, color: Color, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.padding(16.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF212121))
            Text(label, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun DashModule(title: String, subtitle: String, color: Color, icon: ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        onClick = onClick
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = color, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF212121))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = color.copy(alpha = 0.5f))
        }
    }
}
