package com.example.nutriia.accesibilidad

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val TABLA_BRAILLE_VERIFICADA: Map<Set<Int>, String> = buildMap {
    // Letras (27 patrones únicos)
    put(setOf(1),         "a"); put(setOf(1,2),       "b"); put(setOf(1,4),       "c")
    put(setOf(1,4,5),     "d"); put(setOf(1,5),       "e"); put(setOf(1,2,4),     "f")
    put(setOf(1,2,4,5),   "g"); put(setOf(1,2,5),     "h"); put(setOf(2,4),       "i")
    put(setOf(2,4,5),     "j"); put(setOf(1,3),       "k"); put(setOf(1,2,3),     "l")
    put(setOf(1,3,4),     "m"); put(setOf(1,3,4,5),   "n"); put(setOf(1,3,5),     "o")
    put(setOf(1,2,3,4),   "p"); put(setOf(1,2,3,4,5), "q"); put(setOf(1,2,3,5),   "r")
    put(setOf(2,3,4),     "s"); put(setOf(2,3,4,5),   "t"); put(setOf(1,3,6),     "u")
    put(setOf(1,2,3,6),   "v"); put(setOf(2,4,5,6),   "w"); put(setOf(1,3,4,6),   "x")
    put(setOf(1,3,4,5,6), "y"); put(setOf(1,3,5,6),   "z")
    // Español especial (6 patrones únicos)
    put(setOf(1,6),       "á")
    put(setOf(1,2,4,6),   "é")
    put(setOf(3,4),       "í")
    put(setOf(3,4,5),     "ó")
    put(setOf(1,5,6),     "ú")
    put(setOf(1,4,5,6),   "ñ")
    // Números (10 patrones únicos)
    put(setOf(2),         "1")
    put(setOf(2,3),       "2")
    put(setOf(2,5),       "3")
    put(setOf(2,6),       "4")
    put(setOf(3),         "5")
    put(setOf(3,5),       "6")
    put(setOf(3,6),       "7")
    put(setOf(2,3,5),     "8")
    put(setOf(2,3,6),     "9")
    put(setOf(3,5,6),     "0")
    // Puntuación (4 patrones únicos)
    put(setOf(5),         ".")
    put(setOf(6),         ",")
    put(setOf(2,5,6),     "?")
    put(setOf(3,4,6),     "!")
}

@Composable
fun BrailleKeyboard(
    textoActual:   String,
    onTextoChange: (String) -> Unit,
    ttsManager:    NutriTTS?   = null,
    colorPrimario: Color       = Color(0xFF4CAF50),
    onNext:        (() -> Unit)? = null,
    modifier:      Modifier    = Modifier
) {
    val haptic = LocalHapticFeedback.current

    var puntosSeleccionados by remember { mutableStateOf(setOf<Int>()) }
    var ultimaLetraAgregada by remember { mutableStateOf("") }

    val letraActual: String? = TABLA_BRAILLE_VERIFICADA[puntosSeleccionados]
        .takeIf { puntosSeleccionados.isNotEmpty() }

    LaunchedEffect(Unit) {
        val claves = TABLA_BRAILLE_VERIFICADA.keys.toList()
        val conflictos = claves.groupBy { it }.filter { it.value.size > 1 }
        if (conflictos.isEmpty()) {
            android.util.Log.d("Braille", "✅ Tabla sin conflictos: ${claves.size} caracteres")
        } else {
            android.util.Log.e("Braille", "❌ Conflictos: $conflictos")
        }
        ttsManager?.hablar("Teclado Braille activo. Selecciona los puntos de tu letra. El botón Limpiar patrón está abajo a la izquierda, y el botón verde Continuar está abajo a la derecha.")
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A2E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Display del texto ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF16213E))
                .padding(14.dp)
                .semantics { contentDescription = "Texto escrito: ${textoActual.ifEmpty { "vacío" }}" }
        ) {
            Text(
                text       = textoActual.ifEmpty { "Toca los puntos y confirma cada letra..." },
                color      = if (textoActual.isEmpty()) Color.Gray else Color.White,
                fontSize   = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── Indicador de letra actual ─────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    when {
                        puntosSeleccionados.isEmpty() -> Color(0xFF2D2D44)
                        letraActual != null           -> colorPrimario.copy(0.2f)
                        else                          -> Color(0xFF3D1515)
                    }
                )
                .padding(10.dp)
                .semantics {
                    contentDescription = when {
                        puntosSeleccionados.isEmpty() -> "Sin puntos seleccionados"
                        letraActual != null -> "Letra: ${letraActual.uppercase()}. Puntos: ${puntosSeleccionados.sorted().joinToString(" y ")}"
                        else -> "Combinación no reconocida: ${puntosSeleccionados.sorted().joinToString("-")}"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                puntosSeleccionados.isEmpty() -> Text(
                    "Selecciona los puntos de tu letra",
                    color = Color.Gray, fontSize = 13.sp, textAlign = TextAlign.Center
                )
                letraActual != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("→ ", color = Color.Gray, fontSize = 16.sp)
                    Text(
                        letraActual.uppercase(),
                        color = colorPrimario, fontSize = 32.sp, fontWeight = FontWeight.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "(${puntosSeleccionados.sorted().joinToString("-")})",
                        color = Color.Gray, fontSize = 11.sp
                    )
                }
                else -> Text(
                    "⚠ Puntos: ${puntosSeleccionados.sorted().joinToString("-")} — sin letra",
                    color = Color(0xFFFF6B6B), fontSize = 12.sp, textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Los 6 puntos ──────────────────────────────────────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.semantics { contentDescription = "Columna izquierda: puntos 1, 2, 3" }
            ) {
                listOf(1, 2, 3).forEach { punto ->
                    PuntoBraille(
                        numero        = punto,
                        seleccionado  = punto in puntosSeleccionados,
                        colorPrimario = colorPrimario,
                        onToggle      = {
                            vibrateTap(haptic)
                            val nuevosPuntos = if (punto in puntosSeleccionados)
                                puntosSeleccionados - punto
                            else
                                puntosSeleccionados + punto
                            puntosSeleccionados = nuevosPuntos
                            val letra = TABLA_BRAILLE_VERIFICADA[nuevosPuntos]
                            if (letra != null) {
                                ttsManager?.hablar("Forma la letra ${letra.uppercase()}")
                            } else {
                                if (nuevosPuntos.isEmpty()) {
                                    ttsManager?.hablar("Ningún punto seleccionado")
                                } else {
                                    val puntosTexto = nuevosPuntos.sorted().joinToString(" ")
                                    ttsManager?.hablar("Puntos $puntosTexto")
                                }
                            }
                        }
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.semantics { contentDescription = "Columna derecha: puntos 4, 5, 6" }
            ) {
                listOf(4, 5, 6).forEach { punto ->
                    PuntoBraille(
                        numero        = punto,
                        seleccionado  = punto in puntosSeleccionados,
                        colorPrimario = colorPrimario,
                        onToggle      = {
                            vibrateTap(haptic)
                            val nuevosPuntos = if (punto in puntosSeleccionados)
                                puntosSeleccionados - punto
                            else
                                puntosSeleccionados + punto
                            puntosSeleccionados = nuevosPuntos
                            val letra = TABLA_BRAILLE_VERIFICADA[nuevosPuntos]
                            if (letra != null) {
                                ttsManager?.hablar("Forma la letra ${letra.uppercase()}")
                            } else {
                                if (nuevosPuntos.isEmpty()) {
                                    ttsManager?.hablar("Ningún punto seleccionado")
                                } else {
                                    val puntosTexto = nuevosPuntos.sorted().joinToString(" ")
                                    ttsManager?.hablar("Puntos $puntosTexto")
                                }
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ── Botones de acción ─────────────────────────────────────────────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick  = {
                    vibrateTap(haptic)
                    if (textoActual.isNotEmpty()) {
                        val borrada = textoActual.last().toString()
                        onTextoChange(textoActual.dropLast(1))
                        ttsManager?.hablar("Borrado: $borrada")
                    } else {
                        ttsManager?.hablar("No hay texto que borrar")
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF3D1515))
                    .semantics { contentDescription = "Borrar último carácter" }
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, null,
                    tint = Color(0xFFFF6B6B), modifier = Modifier.size(24.dp))
            }

            Button(
                onClick  = {
                    val letra = letraActual
                    if (letra != null) {
                        vibrateSuccess(haptic)
                        onTextoChange(textoActual + letra)
                        ultimaLetraAgregada = letra
                        puntosSeleccionados = emptySet()
                        ttsManager?.hablar("Agregado: ${letra.uppercase()}")
                    } else if (puntosSeleccionados.isEmpty()) {
                        ttsManager?.hablar("Selecciona los puntos primero")
                    } else {
                        vibrateError(haptic)
                        ttsManager?.hablar("Combinación no reconocida. Intenta otros puntos.")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .semantics {
                        contentDescription = when {
                            letraActual != null -> "Agregar letra ${letraActual.uppercase()}."
                            puntosSeleccionados.isEmpty() -> "Selecciona puntos primero"
                            else -> "Combinación inválida"
                        }
                    },
                shape  = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = if (letraActual != null) colorPrimario else Color(0xFF2D2D44),
                    disabledContainerColor = Color(0xFF2D2D44)
                )
            ) {
                Text(
                    text = when {
                        letraActual != null           -> "Agregar \"${letraActual.uppercase()}\""
                        puntosSeleccionados.isEmpty() -> "Selecciona puntos"
                        else                          -> "No reconocido"
                    },
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = Color.White
                )
            }

            IconButton(
                onClick  = {
                    vibrateTap(haptic)
                    onTextoChange("$textoActual ")
                    puntosSeleccionados = emptySet()
                    ttsManager?.hablar("Espacio agregado")
                },
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF2D2D44))
                    .semantics { contentDescription = "Agregar espacio." }
            ) {
                Icon(Icons.Rounded.SpaceBar, null,
                    tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(
                onClick  = {
                    puntosSeleccionados = emptySet()
                    ttsManager?.hablar("Patrón limpiado.")
                },
                modifier = Modifier.weight(1f).semantics { contentDescription = "Limpiar puntos seleccionados" }
            ) {
                Text("Limpiar patrón", color = Color.Gray, fontSize = 12.sp)
            }

            if (onNext != null) {
                Button(
                    onClick = {
                        vibrateSuccess(haptic)
                        ttsManager?.hablar("Guardando y avanzando al siguiente campo")
                        onNext()
                    },
                    modifier = Modifier.weight(1.3f).height(46.dp).semantics { contentDescription = "Botón verde Continuar y avanzar al siguiente campo. Toca aquí al terminar de escribir." },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimario)
                ) {
                    Text("Continuar / Siguiente", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun PuntoBraille(
    numero:       Int,
    seleccionado: Boolean,
    colorPrimario: Color,
    onToggle:     () -> Unit
) {
    val escala  by animateFloatAsState(if (seleccionado) 1.18f else 1f, spring(), label = "esc$numero")
    val fondo   by animateColorAsState(if (seleccionado) colorPrimario else Color(0xFF2D2D44), label = "col$numero")
    val borde   by animateColorAsState(if (seleccionado) colorPrimario else Color(0xFF3D3D5C), label = "brd$numero")

    Box(
        modifier = Modifier
            .size(68.dp)
            .scale(escala)
            .clip(CircleShape)
            .background(fondo)
            .border(2.dp, borde, CircleShape)
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggle() }) }
            .semantics {
                contentDescription = "Punto $numero. ${if (seleccionado) "Seleccionado" else "No seleccionado"}"
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = numero.toString(),
            color      = if (seleccionado) Color.White else Color.Gray,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Black,
            textAlign  = TextAlign.Center
        )
    }
}
