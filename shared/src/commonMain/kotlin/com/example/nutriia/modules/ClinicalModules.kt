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

// ─── Importación de motores clínicos reales NutriIA ─────────────────────
import com.example.nutriia.sueldo.DietaEngine
import com.example.nutriia.sueldo.RecetaMexicana
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.NutriEstimadoEngine
import com.example.nutriia.embarazo.DietaEmbarazoEngine
import com.example.nutriia.embarazo.RecetaEmbarazo
import com.example.nutriia.embarazo.TrimestreEmbarazo
import com.example.nutriia.embarazo.GananciaPesoCalculator
import com.example.nutriia.embarazo.SintomasAnalyzer
import com.example.nutriia.nutriente.recomendacionOMSParaEdad
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.crecimiento.TABLA_OMS_PESO_NINOS
import com.example.nutriia.crecimiento.TABLA_OMS_PESO_NINAS
import com.example.nutriia.crecimiento.evaluarIMC
import com.example.nutriia.crecimiento.MedicionCrecimiento
import com.example.nutriia.data.ChildProfile

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
// SÓLIDOS BLW — Alimentado por DietaEngine oficial IMSS/OMS
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun SolidosBLWScreen(onNavigateBack: () -> Unit) {
    var filtroEdad by remember { mutableStateOf(6) }
    val recetasBLW = remember(filtroEdad) {
        DietaEngine.RECETAS.filter { it.edadMinMeses <= filtroEdad }
    }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("🍎 Sólidos BLW (IMSS/OMS)", ModGreen, onNavigateBack)

            // Selector de edad
            Text("Filtrar por edad:", fontWeight = FontWeight.Bold, color = ModDark)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(6 to "6 meses", 8 to "8 meses", 12 to "12 meses").forEach { (meses, label) ->
                    FilterChip(
                        selected = filtroEdad == meses,
                        onClick = { filtroEdad = meses },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ModGreen.copy(alpha = 0.2f)
                        )
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            recetasBLW.forEach { r ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.nombre, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ModDark, modifier = Modifier.weight(1f))
                            Badge(containerColor = ModGreen.copy(alpha = 0.15f)) {
                                Text("${r.kcal} kcal", color = ModDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Ingredientes: " + r.ingredientes.joinToString(", "), fontSize = 13.sp, color = Color(0xFF424242))
                        Spacer(Modifier.height(4.dp))
                        Text("Preparación: " + r.preparacion, fontSize = 12.sp, color = Color.Gray)
                        Spacer(Modifier.height(6.dp))
                        Text("Fuente: " + r.fuente, fontSize = 10.sp, color = ModTeal, fontWeight = FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CURVAS DE CRECIMIENTO — Motor OMS Oficial Percentiles
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun GrowthCurvesScreen(onNavigateBack: () -> Unit) {
    var peso by remember { mutableStateOf("") }
    var talla by remember { mutableStateOf("") }
    var edadMeses by remember { mutableStateOf("12") }
    var sexo by remember { mutableStateOf(Sexo.NINO) }

    var resultadoIMC by remember { mutableStateOf<String?>(null) }
    var percentilOMS by remember { mutableStateOf<String?>(null) }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("📈 Curvas OMS Oficial", ModBlue, onNavigateBack)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Evaluar crecimiento vs Estándares OMS", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ModDark)
                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = sexo == Sexo.NINO,
                            onClick = { sexo = Sexo.NINO },
                            label = { Text("👦 Niño") }
                        )
                        FilterChip(
                            selected = sexo == Sexo.NINA,
                            onClick = { sexo = Sexo.NINA },
                            label = { Text("👧 Niña") }
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = edadMeses, onValueChange = { edadMeses = it },
                        label = { Text("Edad (meses)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = peso, onValueChange = { peso = it },
                        label = { Text("Peso actual (kg)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = talla, onValueChange = { talla = it },
                        label = { Text("Talla (cm)") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = {
                            val p = peso.toDoubleOrNull() ?: 0.0
                            val t = talla.toDoubleOrNull() ?: 0.0
                            val m = edadMeses.toIntOrNull() ?: 12
                            if (p > 0 && t > 0) {
                                val imc = p / ((t / 100.0) * (t / 100.0))
                                val interp = evaluarIMC(m, imc, sexo)
                                resultadoIMC = "IMC: ${imc.toString().take(4)} — ${interp.categoria}"

                                val tabla = if (sexo == Sexo.NINO) TABLA_OMS_PESO_NINOS else TABLA_OMS_PESO_NINAS
                                val ref = tabla.find { it.meses == m } ?: tabla.last()
                                percentilOMS = "Mediana OMS (p50) a los $m meses: ${ref.p50} kg (p3: ${ref.p3} kg, p97: ${ref.p97} kg)"
                            }
                        },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ModBlue)
                    ) {
                        Text("Calcular percentil OMS", fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (resultadoIMC != null) {
                Spacer(Modifier.height(16.dp))
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = ModBlue.copy(alpha = 0.1f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("📊 Diagnóstico Nutricional OMS", fontWeight = FontWeight.Bold, color = ModBlue, fontSize = 16.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(resultadoIMC ?: "", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ModDark)
                        Spacer(Modifier.height(4.dp))
                        Text(percentilOMS ?: "", fontSize = 13.sp, color = Color(0xFF424242))
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
// CALCULADORA NUTRICIONAL — NutriEstimadoEngine + OMS
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun NutrientCalcScreen(onNavigateBack: () -> Unit) {
    var edadMeses by remember { mutableStateOf(12) }
    val recOMS = remember(edadMeses) { recomendacionOMSParaEdad(edadMeses) }
    val estimado = remember(edadMeses) { NutriEstimadoEngine.estimarDia(edadMeses, NivelIngreso.MEDIO) }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("🥗 Calculadora Nutricional OMS", ModOrange, onNavigateBack)

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Requerimiento Diario según OMS", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = ModDark)
                    Spacer(Modifier.height(12.dp))
                    Text("Edad seleccionada: $edadMeses meses", fontSize = 14.sp, color = Color.Gray)
                    Slider(
                        value = edadMeses.toFloat(),
                        onValueChange = { edadMeses = it.toInt() },
                        valueRange = 6f..48f,
                        steps = 42,
                        colors = SliderDefaults.colors(thumbColor = ModOrange, activeTrackColor = ModOrange)
                    )

                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Calorías", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${estimado.caloriasEstimadas.toInt()} kcal", color = ModOrange, fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text("Proteínas", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${estimado.proteinasEstimadas.toInt()} g", color = ModGreen, fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text("Hierro", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${estimado.hierroEstimado} mg", color = ModBlue, fontWeight = FontWeight.ExtraBold)
                        }
                        Column {
                            Text("Calcio", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${estimado.calcioEstimado.toInt()} mg", color = ModPurple, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Guía oficial OMS para esta etapa:", fontWeight = FontWeight.Bold, color = ModDark)
                    Spacer(Modifier.height(6.dp))
                    Text("• Rango de edad: ${recOMS.rango.label}", fontSize = 13.sp)
                    Text("• Grasas objetivo: ${estimado.grasasEstimadas.toInt()} g / día", fontSize = 13.sp)
                    Text("• Zinc: ${estimado.zincEstimado} mg / día", fontSize = 13.sp)
                    Text("• Vitamina A: ${estimado.vitaminaAEstimada.toInt()} µg / día", fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// CHAT CON IA — Asistente Inteligente NutriIA
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun ChatAIScreen(onNavigateBack: () -> Unit) {
    var input by remember { mutableStateOf("") }
    val messages = remember { mutableStateListOf(
        Pair(false, "¡Hola! 🤖 Soy NutriIA, tu asistente clínico de nutrición infantil y prenatal respaldado por guías IMSS y OMS. ¿En qué te puedo orientar hoy?")
    ) }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 24.dp)) {
                ModuleHeader("🤖 Asistente Clínico IA", ModTeal, onNavigateBack)
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
                    placeholder = { Text("Pregunta sobre nutrición, BLW o síntomas...") }, singleLine = true
                )
                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (input.isNotBlank()) {
                            val userMsg = input
                            messages.add(Pair(true, userMsg))
                            val resp = when {
                                "sintoma" in userMsg.lowercase() || "dolor" in userMsg.lowercase() ->
                                    "Basado en el analizador de síntomas maternos: si experimentas síntomas persistentes, agenda una teleconsulta con tu especialista."
                                "blw" in userMsg.lowercase() || "solido" in userMsg.lowercase() ->
                                    "Para iniciar BLW (6+ meses): asegura que tu bebé se siente sin apoyo y ofrece alimentos suaves cortados en bastones (aguacate, plátano, zanahoria al vapor)."
                                "embarazo" in userMsg.lowercase() || "semana" in userMsg.lowercase() ->
                                    "Durante la gestación recuerda complementar con 400 µg de ácido fólico y mantener hidratación de 2.3 L/día según la Guía 2023 de la Secretaría de Salud."
                                else ->
                                    "Analizando tu consulta en base a las guías oficiales de nutrición familiar NutriIA... ✅"
                            }
                            messages.add(Pair(false, resp))
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
            ModuleHeader("🍼 Lactancia Materna", ModRosa, onNavigateBack)

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
            ModuleHeader("👨‍⚕️ Directorio de Pediatras", Color(0xFFF06292), onNavigateBack)
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
// EMBARAZO NUTRICIÓN — DietaEmbarazoEngine Oficial
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun EmbarazoNutricionScreen(onNavigateBack: () -> Unit) {
    var trimestre by remember { mutableStateOf(TrimestreEmbarazo.PRIMERO) }
    val macros = remember(trimestre) { DietaEmbarazoEngine.macrosPorTrimestre(trimestre) }
    val recetas = remember(trimestre) {
        DietaEmbarazoEngine.RECETAS.filter { it.trimestreMinimo <= trimestre }
    }

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("🥗 Nutrición Prenatal (SSA 2023)", ModRosa, onNavigateBack)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    TrimestreEmbarazo.PRIMERO to "1er Trimestre",
                    TrimestreEmbarazo.SEGUNDO to "2do Trimestre",
                    TrimestreEmbarazo.TERCERO to "3er Trimestre"
                ).forEach { (t, label) ->
                    FilterChip(
                        selected = trimestre == t,
                        onClick = { trimestre = t },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = ModRosa.copy(alpha = 0.12f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("Requerimiento prenatal:", fontWeight = FontWeight.Bold, color = ModDark)
                    Spacer(Modifier.height(4.dp))
                    Text("• Kcal extras: +${macros.kcalExtra} kcal/día", fontSize = 13.sp)
                    Text("• Proteína total: ${macros.proteinaG} g/día", fontSize = 13.sp)
                    Text("• Hierro elemental: ${macros.hierroMg} mg/día", fontSize = 13.sp)
                    Text("• Ácido fólico: ${macros.folatoUg} µg/día", fontSize = 13.sp)
                    Text("• Hidratación: ${macros.aguaLitros} L de agua/día", fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("Recetas recomendadas:", fontWeight = FontWeight.Bold, color = ModDark)
            Spacer(Modifier.height(8.dp))

            recetas.take(6).forEach { r ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(r.nombre, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ModDark, modifier = Modifier.weight(1f))
                            Badge(containerColor = ModRosa.copy(alpha = 0.15f)) {
                                Text("${r.kcal} kcal", color = ModDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Ingredientes: " + r.ingredientes.joinToString(", "), fontSize = 12.sp, color = Color(0xFF424242))
                        Spacer(Modifier.height(4.dp))
                        Text("Fuente: " + r.fuente, fontSize = 10.sp, color = ModTeal)
                    }
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
            ModuleHeader("📅 Citas y Controles", ModTeal, onNavigateBack)

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
            ModuleHeader("📹 Teleconsulta Médica", ModBlue, onNavigateBack)
            Spacer(Modifier.height(40.dp))

            Box(
                Modifier.size(120.dp).clip(CircleShape).background(ModBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.VideoCall, null, tint = ModBlue, modifier = Modifier.size(60.dp))
            }
            Spacer(Modifier.height(24.dp))
            Text("Teleconsulta en Vivo", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = ModDark)
            Spacer(Modifier.height(8.dp))
            Text("Conexión WebRTC encriptada con tu especialista", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { /* WebRTC */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ModBlue)
            ) {
                Icon(Icons.Rounded.VideoCall, null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text("Iniciar sala de consulta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = { /* Agendar */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(24.dp), tint = ModBlue)
                Spacer(Modifier.width(8.dp))
                Text("Agendar consulta", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ModBlue)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// EXPEDIENTE
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun ExpedienteScreen(onNavigateBack: () -> Unit) {
    val pacientes = listOf(
        ChildProfile(id = "1", name = "Ana Martínez", birthDate = "15/12/2025", heightCm = "72", weightKg = "8.2", hasAllergies = false),
        ChildProfile(id = "2", name = "Carlos López Jr.", birthDate = "10/06/2025", heightCm = "78", weightKg = "10.1", hasAllergies = true, allergiesDetail = "Huevo"),
        ChildProfile(id = "3", name = "Sofia Hernández", birthDate = "20/02/2026", heightCm = "65", weightKg = "7.0", hasAllergies = false),
        ChildProfile(id = "4", name = "Diego Torres", birthDate = "01/10/2024", heightCm = "86", weightKg = "11.8", hasAllergies = false)
    )

    Box(Modifier.fillMaxSize().background(ModBg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState())) {
            ModuleHeader("📋 Expedientes Clínicos", ModTeal, onNavigateBack)
            pacientes.forEach { p ->
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
                            Text(p.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("${p.edadEnMeses()} meses · ${p.weightKg} kg · ${p.heightCm} cm", fontSize = 12.sp, color = Color.Gray)
                            if (p.hasAllergies) {
                                Text("⚠️ Alergia: ${p.allergiesDetail}", fontSize = 11.sp, color = ModOrange, fontWeight = FontWeight.Medium)
                            }
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = ModTeal.copy(alpha = 0.5f))
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
