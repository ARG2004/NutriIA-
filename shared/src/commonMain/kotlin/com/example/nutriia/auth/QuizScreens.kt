package com.example.nutriia.auth

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import com.example.nutriia.data.ChildProfile
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico

private val QuizGreen = Color(0xFF689F38)
private val QuizDark  = Color(0xFF33691E)
private val QuizBg    = Color(0xFFF8F9F3)
private val QuizRosa  = Color(0xFFEC9BBF)
private val QuizBlue  = Color(0xFF1976D2)

// ═════════════════════════════════════════════════════════════════════════
// QUIZ FAMILIA / PADRES — Alta y personalización del perfil del bebé
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun OnboardingQuizScreen(
    onQuizComplete: (ChildProfile) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var nombreHijo by remember { mutableStateOf("") }
    var fechaNac by remember { mutableStateOf("15/08/2025") }
    var sexo by remember { mutableStateOf("NINO") }
    var peso by remember { mutableStateOf("8.5") }
    var talla by remember { mutableStateOf("72") }
    var tieneAlergias by remember { mutableStateOf(false) }
    var detalleAlergias by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(QuizBg)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (step > 0) step-- else onCancel() },
                    modifier = Modifier.clip(CircleShape).background(Color.White)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = QuizGreen)
                }
                Spacer(Modifier.width(12.dp))
                Text("Perfil del Bebé (${step + 1}/3)", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = QuizDark)
            }
            Spacer(Modifier.height(24.dp))

            when (step) {
                0 -> {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("1. Datos básicos del bebé 👶", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QuizDark)
                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = nombreHijo, onValueChange = { nombreHijo = it },
                                label = { Text("Nombre del bebé") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fechaNac, onValueChange = { fechaNac = it },
                                label = { Text("Fecha de nacimiento (DD/MM/AAAA)") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(16.dp))

                            Text("Sexo:", fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                FilterChip(
                                    selected = sexo == "NINO", onClick = { sexo = "NINO" },
                                    label = { Text("👦 Niño") }
                                )
                                FilterChip(
                                    selected = sexo == "NINA", onClick = { sexo = "NINA" },
                                    label = { Text("👧 Niña") }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("2. Medidas iniciales 📏", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QuizDark)
                            Spacer(Modifier.height(16.dp))

                            OutlinedTextField(
                                value = peso, onValueChange = { peso = it },
                                label = { Text("Peso actual (kg)") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = talla, onValueChange = { talla = it },
                                label = { Text("Talla / Longitud (cm)") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                2 -> {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("3. Alergias y Alimentos ⚠️", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QuizDark)
                            Spacer(Modifier.height(16.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("¿Tiene alguna alergia o intolerancia?", fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                                Switch(checked = tieneAlergias, onCheckedChange = { tieneAlergias = it })
                            }

                            if (tieneAlergias) {
                                Spacer(Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = detalleAlergias, onValueChange = { detalleAlergias = it },
                                    label = { Text("Ej: Huevo, Lácteos, Cacahuate...") },
                                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (step < 2) {
                        step++
                    } else {
                        onQuizComplete(
                            ChildProfile(
                                id = "child_1",
                                name = nombreHijo.ifBlank { "Mi Bebé" },
                                birthDate = fechaNac,
                                sexo = sexo,
                                weightKg = peso,
                                heightCm = talla,
                                hasAllergies = tieneAlergias,
                                allergiesDetail = detalleAlergias
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QuizGreen)
            ) {
                Text(if (step < 2) "Siguiente" else "Completar y entrar al panel", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// QUIZ EMBARAZO — Personalización de gestación y condiciones maternas
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun EmbarazoQuizScreen(
    semanasIniciales: Int = 1,
    onQuizComplete: (PerfilEmbarazo) -> Unit,
    onCancel: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    var semanas by remember { mutableIntStateOf(semanasIniciales) }
    var fum by remember { mutableStateOf("01/01/2026") }
    var tieneDiabetes by remember { mutableStateOf(false) }
    var tieneHipertension by remember { mutableStateOf(false) }
    var tieneNauseas by remember { mutableStateOf(true) }

    Box(Modifier.fillMaxSize().background(QuizBg)) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (step > 0) step-- else onCancel() },
                    modifier = Modifier.clip(CircleShape).background(Color.White)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Atrás", tint = QuizRosa)
                }
                Spacer(Modifier.width(12.dp))
                Text("Cuestionario Prenatal (${step + 1}/2)", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = QuizDark)
            }
            Spacer(Modifier.height(24.dp))

            when (step) {
                0 -> {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("1. Estado de gestación 🤰", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QuizDark)
                            Spacer(Modifier.height(16.dp))

                            Text("Semana actual: $semanas", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = QuizRosa)
                            Slider(
                                value = semanas.toFloat(),
                                onValueChange = { semanas = it.toInt() },
                                valueRange = 1f..40f,
                                steps = 38,
                                colors = SliderDefaults.colors(thumbColor = QuizRosa, activeTrackColor = QuizRosa)
                            )
                            Spacer(Modifier.height(12.dp))

                            OutlinedTextField(
                                value = fum, onValueChange = { fum = it },
                                label = { Text("Fecha última menstruación (FUM)") },
                                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }
                1 -> {
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(20.dp)) {
                            Text("2. Síntomas y condiciones 🩺", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = QuizDark)
                            Spacer(Modifier.height(16.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Náuseas matutinas / vómito", modifier = Modifier.weight(1f))
                                Switch(checked = tieneNauseas, onCheckedChange = { tieneNauseas = it })
                            }
                            Spacer(Modifier.height(8.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Diabetes gestacional", modifier = Modifier.weight(1f))
                                Switch(checked = tieneDiabetes, onCheckedChange = { tieneDiabetes = it })
                            }
                            Spacer(Modifier.height(8.dp))

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Hipertensión / Preclampsia", modifier = Modifier.weight(1f))
                                Switch(checked = tieneHipertension, onCheckedChange = { tieneHipertension = it })
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = {
                    if (step < 1) {
                        step++
                    } else {
                        val conds = mutableListOf<String>()
                        if (tieneDiabetes) conds.add("diabetes gestacional")
                        if (tieneHipertension) conds.add("hipertension")
                        if (tieneNauseas) conds.add("nauseas")

                        onQuizComplete(
                            PerfilEmbarazo(
                                semanas = semanas,
                                fechaUltimaMenstruacion = fum,
                                condiciones = conds
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = QuizRosa)
            ) {
                Text(if (step < 1) "Siguiente" else "Completar y ver mi plan prenatal", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null)
            }
        }
    }
}
