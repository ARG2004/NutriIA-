package com.example.nutriia.accesibilidad

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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

// ─── Colores de accesibilidad ────────────────────────────────────────────
private val A11yGreen = Color(0xFF689F38)
private val A11yDark  = Color(0xFF33691E)
private val A11yBg    = Color(0xFFF8F9F3)
private val A11yBlue  = Color(0xFF1976D2)
private val A11yAmber = Color(0xFFFFA000)

// ═════════════════════════════════════════════════════════════════════════
// ACCESSIBILITY CONFIG SCREEN — Configuración de accesibilidad
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun AccessibilityConfigScreen(onNavigateBack: () -> Unit) {
    var ttsEnabled    by remember { mutableStateOf(true) }
    var brailleEnabled by remember { mutableStateOf(false) }
    var highContrast  by remember { mutableStateOf(false) }
    var voiceInput    by remember { mutableStateOf(true) }
    var fontSize      by remember { mutableStateOf(16f) }

    Box(Modifier.fillMaxSize().background(A11yBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(Color.White)
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = A11yGreen) }
                Spacer(Modifier.width(12.dp))
                Text("♿ Accesibilidad", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = A11yDark)
            }
            Spacer(Modifier.height(24.dp))

            // TTS
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("🔊 Texto a Voz (TTS)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(8.dp))
                    Text("Lee en voz alta los elementos de la pantalla", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (ttsEnabled) "Activado" else "Desactivado", fontWeight = FontWeight.Medium)
                        Switch(checked = ttsEnabled, onCheckedChange = { ttsEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = A11yGreen))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Braille
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("⠿ Teclado Braille", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(8.dp))
                    Text("Teclado de 6 puntos para entrada en Braille", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (brailleEnabled) "Activado" else "Desactivado", fontWeight = FontWeight.Medium)
                        Switch(checked = brailleEnabled, onCheckedChange = { brailleEnabled = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = A11yBlue))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Alto contraste
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("🎨 Alto contraste", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (highContrast) "Activado" else "Desactivado", fontWeight = FontWeight.Medium)
                        Switch(checked = highContrast, onCheckedChange = { highContrast = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = A11yAmber))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Entrada por voz
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("🎤 Entrada por voz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically) {
                        Text(if (voiceInput) "Activado" else "Desactivado", fontWeight = FontWeight.Medium)
                        Switch(checked = voiceInput, onCheckedChange = { voiceInput = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = A11yGreen))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Tamaño de fuente
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(20.dp)) {
                    Text("🔤 Tamaño de fuente", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(8.dp))
                    Text("Tamaño actual: ${fontSize.toInt()}sp", fontSize = 13.sp, color = Color.Gray)
                    Slider(value = fontSize, onValueChange = { fontSize = it },
                        valueRange = 12f..28f, steps = 7,
                        colors = SliderDefaults.colors(thumbColor = A11yGreen, activeTrackColor = A11yGreen))
                    Text("Ejemplo de texto", fontSize = fontSize.sp, color = A11yDark)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// BRAILLE KEYBOARD SCREEN — Teclado Braille de 6 puntos
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun BrailleKeyboardScreen(onNavigateBack: () -> Unit) {
    val dots = remember { mutableStateListOf(false, false, false, false, false, false) }
    var output by remember { mutableStateOf("") }

    fun decodeBraille(): String {
        val pattern = dots.mapIndexed { i, v -> if (v) (i + 1) else 0 }.filter { it > 0 }
        return when (pattern) {
            listOf(1) -> "a"; listOf(1,2) -> "b"; listOf(1,4) -> "c"
            listOf(1,4,5) -> "d"; listOf(1,5) -> "e"; listOf(1,2,4) -> "f"
            listOf(1,2,4,5) -> "g"; listOf(1,2,5) -> "h"; listOf(2,4) -> "i"
            listOf(2,4,5) -> "j"; listOf(1,3) -> "k"; listOf(1,2,3) -> "l"
            listOf(1,3,4) -> "m"; listOf(1,3,4,5) -> "n"; listOf(1,3,5) -> "o"
            listOf(1,2,3,4) -> "p"; listOf(1,2,3,4,5) -> "q"; listOf(1,2,3,5) -> "r"
            listOf(2,3,4) -> "s"; listOf(2,3,4,5) -> "t"; listOf(1,3,6) -> "u"
            listOf(1,2,3,6) -> "v"; listOf(2,4,5,6) -> "w"; listOf(1,3,4,6) -> "x"
            listOf(1,3,4,5,6) -> "y"; listOf(1,3,5,6) -> "z"
            else -> "?"
        }
    }

    Box(Modifier.fillMaxSize().background(A11yBg)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(Color.White)
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = A11yBlue) }
                Spacer(Modifier.width(12.dp))
                Text("⠿ Teclado Braille", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = A11yDark)
            }
            Spacer(Modifier.height(24.dp))

            // Output
            Card(Modifier.fillMaxWidth().height(80.dp), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.CenterStart) {
                    Text(
                        if (output.isEmpty()) "Escribe con Braille..." else output,
                        fontSize = 20.sp, color = if (output.isEmpty()) Color.Gray else A11yDark,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(32.dp))

            // 6 dots grid (2 columns x 3 rows)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (i in listOf(0, 2, 4)) {
                        BrailleDot(active = dots[i], label = "${i + 1}") { dots[i] = !dots[i] }
                        Spacer(Modifier.height(12.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    for (i in listOf(1, 3, 5)) {
                        BrailleDot(active = dots[i], label = "${i + 1}") { dots[i] = !dots[i] }
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        output += decodeBraille()
                        for (i in dots.indices) dots[i] = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = A11yBlue),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Ingresar", fontWeight = FontWeight.Bold) }

                OutlinedButton(
                    onClick = {
                        if (output.isNotEmpty()) output = output.dropLast(1)
                    },
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Borrar") }

                OutlinedButton(
                    onClick = { output += " " },
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Espacio") }
            }
        }
    }
}

@Composable
private fun BrailleDot(active: Boolean, label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(if (active) A11yBlue else Color.LightGray.copy(alpha = 0.4f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold,
            color = if (active) Color.White else Color.DarkGray)
    }
}
