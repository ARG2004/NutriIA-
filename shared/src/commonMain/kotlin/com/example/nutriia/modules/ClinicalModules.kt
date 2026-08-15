package com.example.nutriia.modules

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Colores módulos ─────────────────────────────────────────────────────
private val ModGreen  = Color(0xFF689F38)
private val ModDark   = Color(0xFF33691E)
private val ModBg     = Color(0xFFF8F9F3)
private val ModTeal   = Color(0xFF4DB6AC)
private val ModOrange = Color(0xFFFF8F00)
private val ModRosa   = Color(0xFFEC9BBF)
private val ModBlue   = Color(0xFF1976D2)
private val ModPurple = Color(0xFF9C8FE0)

// ─── Header reutilizable ─────────────────────────────────────────────────
@Composable
private fun ModuleHeader(title: String, color: Color, onBack: () -> Unit) {
    Spacer(Modifier.height(52.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack, modifier = Modifier.clip(CircleShape).background(Color.White)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Text(title, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ModDark)
    }
    Spacer(Modifier.height(24.dp))
}

// ═════════════════════════════════════════════════════════════════════════
// SÓLIDOS BLW
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun SolidosBLWScreen(onNavigateBack: () -> Unit) {
    val foods = listOf(
        "🥑 Aguacate" to "6+ meses · Cortar en tiras",
        "🍌 Plátano"  to "6+ meses · Maduro, en trozos",
        "🥦 Brócoli"  to "6+ meses · Al vapor, floretes",
        "🍠 Camote"   to "6+ meses · Cocido en bastones",
        "🥕 Zanahoria" to "7+ meses · Cocida blanda",
        "🍗 Pollo"    to "7+ meses · Desmenuzado o tiras",
        "🐟 Pescado"  to "8+ meses · Sin espinas, desmenuzado",
        "🥚 Huevo"    to "6+ meses · Cocido, en tiritas"
    )
    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("🍎 Sólidos BLW", ModGreen, onNavigateBack)
            foods.forEach { (food, desc) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(food, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(desc, fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CURVAS DE CRECIMIENTO
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun GrowthCurvesScreen(onNavigateBack: () -> Unit) {
    var peso by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var perimetro by remember { mutableStateOf("") }
    val registros = remember { mutableStateListOf<Triple<String, String, String>>() }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("📈 Curvas de crecimiento", ModBlue, onNavigateBack)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Nuevo registro", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ModDark)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(value = peso, onValueChange = { peso = it }, label = { Text("Peso (kg)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = talla, onValueChange = { talla = it }, label = { Text("Talla (cm)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = perimetro, onValueChange = { perimetro = it }, label = { Text("Perímetro cefálico (cm)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        if (peso.isNotBlank()) { registros.add(Triple(peso, talla, perimetro)); peso = ""; talla = ""; perimetro = "" }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ModBlue)) {
                        Text("Guardar registro", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            registros.forEachIndexed { i, (p, t, c) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp)) {
                        Text("#${i + 1}", fontWeight = FontWeight.Bold, color = ModBlue, modifier = Modifier.width(40.dp))
                        Text("$p kg", modifier = Modifier.weight(1f))
                        Text("$t cm", modifier = Modifier.weight(1f))
                        Text("$c cm", modifier = Modifier.weight(1f))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// REGISTRO DE SUEÑO
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun SleepLogScreen(onNavigateBack: () -> Unit) {
    var hours by remember { mutableStateOf("") }
    var quality by remember { mutableStateOf(3f) }
    val logs = remember { mutableStateListOf<Pair<String, Float>>() }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("😴 Registro de sueño", ModPurple, onNavigateBack)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(value = hours, onValueChange = { hours = it }, label = { Text("Horas de sueño") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Text("Calidad: ${quality.toInt()}/5", fontSize = 14.sp, color = Color.Gray)
                    Slider(value = quality, onValueChange = { quality = it }, valueRange = 1f..5f, steps = 3,
                        colors = SliderDefaults.colors(thumbColor = ModPurple, activeTrackColor = ModPurple))
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        if (hours.isNotBlank()) { logs.add(Pair(hours, quality)); hours = "" }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ModPurple)) {
                        Text("Registrar", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            logs.forEachIndexed { i, (h, q) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp)) {
                        Text("Noche ${i + 1}", fontWeight = FontWeight.Bold, color = ModPurple, modifier = Modifier.weight(1f))
                        Text("$h hrs", modifier = Modifier.weight(1f))
                        Text("⭐ ${q.toInt()}/5")
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CALCULADORA NUTRICIONAL
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun NutrientCalcScreen(onNavigateBack: () -> Unit) {
    var food by remember { mutableStateOf("") }
    var grams by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<Triple<String, String, String>>() }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("🥗 Calculadora nutricional", ModOrange, onNavigateBack)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(value = food, onValueChange = { food = it }, label = { Text("Alimento") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = grams, onValueChange = { grams = it }, label = { Text("Gramos") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        if (food.isNotBlank()) {
                            val g = grams.toFloatOrNull() ?: 100f
                            val kcal = "${(g * 1.2f).toInt()} kcal"
                            results.add(Triple(food, "${g.toInt()}g", kcal)); food = ""; grams = ""
                        }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ModOrange)) {
                        Text("Calcular", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            results.forEach { (f, g, k) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp)) {
                        Text(f, fontWeight = FontWeight.Bold, color = ModOrange, modifier = Modifier.weight(1f))
                        Text(g, modifier = Modifier.weight(1f))
                        Text(k, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CHAT CON IA
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun ChatAIScreen(onNavigateBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(
        Pair(false, "¡Hola! 🤖 Soy NutriIA, tu asistente nutricional. ¿En qué te puedo ayudar?")
    ) }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                ModuleHeader("🤖 Chat con IA", ModTeal, onNavigateBack)
            }
            Column(Modifier.weight(1f).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
                messages.forEach { (isUser, text) ->
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) ModTeal else Color.White
                            )
                        ) {
                            Text(
                                text, modifier = Modifier.padding(12.dp),
                                color = if (isUser) Color.White else Color(0xFF212121),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = input, onValueChange = { input = it },
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                    placeholder = { Text("Escribe tu pregunta...") }, singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            messages.add(Pair(true, input))
                            messages.add(Pair(false, "Analizando tu consulta sobre \"$input\"... 🔍"))
                            input = ""
                        }
                    },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(ModTeal)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, null, tint = Color.White)
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// LACTANCIA
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun LactanciaScreen(onNavigateBack: () -> Unit) {
    var minutes by remember { mutableStateOf("") }
    var side by remember { mutableStateOf("Izquierdo") }
    val feedings = remember { mutableStateListOf<Pair<String, String>>() }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("🍼 Lactancia", ModRosa, onNavigateBack)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    OutlinedTextField(value = minutes, onValueChange = { minutes = it },
                        label = { Text("Minutos de toma") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Izquierdo", "Derecho", "Biberón").forEach { s ->
                            FilterChip(
                                selected = side == s, onClick = { side = s },
                                label = { Text(s) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ModRosa.copy(alpha = 0.2f)
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        if (minutes.isNotBlank()) { feedings.add(Pair(minutes, side)); minutes = "" }
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ModRosa)) {
                        Text("Registrar toma", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            feedings.forEachIndexed { i, (m, s) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 2.dp), shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(12.dp)) {
                        Text("Toma ${i + 1}", fontWeight = FontWeight.Bold, color = ModRosa, modifier = Modifier.weight(1f))
                        Text("$m min · $s")
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// DIRECTORIO PEDIATRAS
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun PediatraDirScreen(onNavigateBack: () -> Unit) {
    val specialists = listOf(
        Triple("Dra. María López", "Pediatría general", "⭐ 4.9"),
        Triple("Dr. Carlos Mendoza", "Nutriología pediátrica", "⭐ 4.8"),
        Triple("Dra. Ana García", "Gastroenterología infantil", "⭐ 4.7"),
        Triple("Dr. Roberto Sánchez", "Endocrinología pediátrica", "⭐ 4.9"),
        Triple("Dra. Laura Torres", "Alergología infantil", "⭐ 4.6")
    )

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("👨‍⚕️ Directorio de pediatras", Color(0xFFF06292), onNavigateBack)
            specialists.forEach { (name, spec, rating) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFF06292).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, tint = Color(0xFFF06292), modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(spec, fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(rating, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ModOrange)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// EMBARAZO NUTRICIÓN
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun EmbarazoNutricionScreen(onNavigateBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("🥗 Nutrición prenatal", ModRosa, onNavigateBack)

            val meals = listOf(
                "🌅 Desayuno" to "Avena con frutas, huevo cocido, jugo de naranja",
                "🍎 Colación AM" to "Manzana con mantequilla de almendra",
                "☀️ Comida" to "Pollo a la plancha, arroz integral, ensalada verde",
                "🍪 Colación PM" to "Yogurt natural con granola y miel",
                "🌙 Cena" to "Crema de verduras, pan integral, té de manzanilla"
            )
            meals.forEach { (meal, desc) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Text(meal, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ModDark)
                        Spacer(Modifier.height(4.dp))
                        Text(desc, fontSize = 13.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = ModRosa.copy(alpha = 0.1f))) {
                Column(Modifier.padding(20.dp)) {
                    Text("💊 Suplementos recomendados", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ModDark)
                    Spacer(Modifier.height(8.dp))
                    Text("• Ácido fólico: 400 µg/día", fontSize = 13.sp)
                    Text("• Hierro: 27 mg/día", fontSize = 13.sp)
                    Text("• Calcio: 1000 mg/día", fontSize = 13.sp)
                    Text("• Vitamina D: 600 UI/día", fontSize = 13.sp)
                    Text("• DHA/Omega-3: 200 mg/día", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CITAS EMBARAZO
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun CitasEmbarazoScreen(onNavigateBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("📅 Citas y controles", ModTeal, onNavigateBack)

            val citas = listOf(
                Triple("Semana 12", "Ultrasonido primer trimestre", "✅ Completada"),
                Triple("Semana 16", "Control prenatal", "✅ Completada"),
                Triple("Semana 20", "Ultrasonido estructural", "✅ Completada"),
                Triple("Semana 24", "Curva de glucosa", "📌 Próxima"),
                Triple("Semana 28", "Control tercer trimestre", "⏳ Pendiente"),
                Triple("Semana 32", "Ultrasonido crecimiento", "⏳ Pendiente"),
                Triple("Semana 36", "Monitoreo fetal", "⏳ Pendiente")
            )
            citas.forEach { (week, desc, status) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(week, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ModDark)
                            Text(desc, fontSize = 12.sp, color = Color.Gray)
                        }
                        Text(status, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// TELECONSULTA
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun TeleconsultaScreen(onNavigateBack: () -> Unit) {
    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ModuleHeader("📹 Teleconsulta", ModBlue, onNavigateBack)
            Spacer(Modifier.height(40.dp))

            Box(
                Modifier.size(120.dp).clip(CircleShape).background(ModBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.VideoCall, null, tint = ModBlue, modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Teleconsulta en vivo", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ModDark)
            Spacer(Modifier.height(8.dp))
            Text("Conecta con tu nutriólogo por videollamada", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { /* WebRTC call */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ModBlue)
            ) {
                Icon(Icons.Rounded.VideoCall, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Iniciar videollamada", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { /* schedule */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(24.dp), tint = ModBlue)
                Spacer(Modifier.width(8.dp))
                Text("Agendar cita", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ModBlue)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// EXPEDIENTE
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun ExpedienteScreen(onNavigateBack: () -> Unit) {
    val patients = listOf(
        Triple("Ana Martínez", "8 meses · 8.2 kg", "Última visita: hace 3 días"),
        Triple("Carlos López Jr.", "14 meses · 10.1 kg", "Última visita: hace 1 semana"),
        Triple("Sofia Hernández", "6 meses · 7.0 kg", "Última visita: hoy"),
        Triple("Diego Torres", "22 meses · 11.8 kg", "Última visita: hace 2 semanas")
    )

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("📋 Expedientes clínicos", ModTeal, onNavigateBack)
            patients.forEach { (name, info, visit) ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(ModTeal.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, tint = ModTeal, modifier = Modifier.size(28.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(info, fontSize = 12.sp, color = Color.Gray)
                            Text(visit, fontSize = 11.sp, color = ModTeal)
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = ModTeal.copy(alpha = 0.5f))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
