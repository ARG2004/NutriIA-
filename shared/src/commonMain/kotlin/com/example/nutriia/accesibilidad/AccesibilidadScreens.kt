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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

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
fun AccessibilityConfigScreen(
    onNavigateBack: () -> Unit,
    a11yVm: AccessibilityViewModel = viewModel()
) {
    val modoActual   by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val velocidad    by a11yVm.speed.collectAsState()
    val esBlind      = modoActual == AccessibilityMode.BLIND

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar("Pantalla de Configuración de Accesibilidad. Aquí puedes cambiar el modo de interacción, el idioma del asistente y ajustar la velocidad de la voz.")
        }
    }

    Box(
        Modifier
            .anuncioPantalla("Configuración de Accesibilidad")
            .fillMaxSize()
            .background(A11yBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(Color.White)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = A11yGreen)
                }
                Spacer(Modifier.width(12.dp))
                Text("♿ Accesibilidad", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = A11yDark)
            }
            Spacer(Modifier.height(24.dp))

            // Modo de interacción
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("👁️ Modo de Experiencia", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(4.dp))
                    Text("Selecciona cómo interactúas con la app", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(14.dp))

                    AccessibilityMode.entries.forEach { modo ->
                        val sel = modo == modoActual
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) A11yGreen.copy(alpha = 0.1f) else Color(0xFFF9F9F9))
                                .border(1.dp, if (sel) A11yGreen else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable {
                                    a11yVm.setMode(modo)
                                    NutriEarcons.playButtonHover()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(modo.label, fontWeight = FontWeight.Bold, color = if (sel) A11yDark else Color.DarkGray, fontSize = 14.sp)
                                Text(modo.description, fontSize = 11.sp, color = Color.Gray)
                            }
                            if (sel) {
                                Icon(Icons.Rounded.CheckCircle, "Seleccionado", tint = A11yGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Velocidad de voz TTS
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("⚡ Velocidad de Voz (TTS)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(4.dp))
                    Text("Ajusta la rapidez con la que el asistente narra", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(14.dp))

                    val velocidades = listOf(
                        0.85f to "0.85x (Lenta)",
                        1.00f to "1.0x (Normal)",
                        1.25f to "1.25x (Rápida)",
                        1.50f to "1.5x (Experto)"
                    )

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        velocidades.forEach { (vel, label) ->
                            val sel = kotlin.math.abs(velocidad - vel) < 0.05f
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (sel) A11yGreen else Color(0xFFF1F3E9))
                                    .clickable {
                                        a11yVm.setSpeed(vel)
                                        NutriEarcons.playSuccess()
                                    }
                                    .semantics {
                                        contentDescription = "Velocidad $label. ${if (sel) "Activa" else "Toca dos veces para activar"}"
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    label.split(" ")[0],
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = if (sel) Color.White else A11yDark
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            // Idioma de la voz
            Card(
                Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(Modifier.padding(20.dp)) {
                    Text("🌐 Idioma de Voz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = A11yDark)
                    Spacer(Modifier.height(4.dp))
                    Text("Voz de locución y comandos", fontSize = 13.sp, color = Color.Gray)
                    Spacer(Modifier.height(14.dp))

                    IdiomaVoz.entries.forEach { idioma ->
                        val sel = idioma == idiomaActual
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (sel) A11yGreen.copy(alpha = 0.1f) else Color(0xFFF9F9F9))
                                .border(1.dp, if (sel) A11yGreen else Color.Transparent, RoundedCornerShape(12.dp))
                                .clickable {
                                    a11yVm.setIdioma(idioma)
                                    NutriEarcons.playButtonHover()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(idioma.label, fontWeight = FontWeight.Bold, color = if (sel) A11yDark else Color.DarkGray, fontSize = 14.sp)
                                Text(idioma.descripcion, fontSize = 11.sp, color = Color.Gray)
                            }
                            if (sel) {
                                Icon(Icons.Rounded.CheckCircle, "Seleccionado", tint = A11yGreen, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
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
    var output by remember { mutableStateOf("") }

    Box(
        Modifier
            .anuncioPantalla("Teclado Braille Virtual")
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Volver al menú de accesibilidad",
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    text = "⠿ Teclado Braille",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(20.dp))

            BrailleKeyboard(
                textoActual = output,
                onTextoChange = { output = it },
                onNext = onNavigateBack,
                colorPrimario = Color(0xFF10B981)
            )
        }
    }
}
