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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    colorPrimario: Color       = Color(0xFF10B981),
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
            com.example.nutriia.platform.Log.d("Braille", "✅ Tabla sin conflictos: ${claves.size} caracteres")
        } else {
            com.example.nutriia.platform.Log.e("Braille", "❌ Conflictos: $conflictos")
        }
        ttsManager?.hablar(
            "Teclado Braille virtual activo. Dos columnas con 3 puntos cada una: " +
            "a la izquierda puntos 1, 2 y 3 de arriba a abajo. A la derecha puntos 4, 5 y 6 de arriba a abajo. " +
            "Abajo a la izquierda botón borrar, al centro botón agregar letra, y a la derecha botón espacio."
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Slate 900
                        Color(0xFF1E293B)  // Slate 800
                    )
                )
            )
            .border(1.5.dp, Color(0xFF334155), RoundedCornerShape(28.dp))
            .padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Cabecera de Identificación ─────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Keyboard,
                    contentDescription = null,
                    tint = colorPrimario,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "TECLADO BRAILLE (6 PUNTOS)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
            }
            Text(
                text = "${textoActual.length} caracteres",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B)
            )
        }

        // ── Display del texto ─────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF0B0F19))
                .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .semantics {
                    contentDescription = "Texto acumulado: ${textoActual.ifEmpty { "vacío" }}. Longitud: ${textoActual.length} caracteres."
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = textoActual.ifEmpty { "Toca los puntos y presiona Agregar..." },
                    color = if (textoActual.isEmpty()) Color(0xFF64748B) else Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (textoActual.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colorPrimario)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── Indicador HUD de Letra Actual ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    when {
                        puntosSeleccionados.isEmpty() -> Color(0xFF1E293B)
                        letraActual != null           -> Color(0xFF064E3B).copy(alpha = 0.65f)
                        else                          -> Color(0xFF451A03).copy(alpha = 0.65f)
                    }
                )
                .border(
                    width = if (letraActual != null) 1.5.dp else 1.dp,
                    color = when {
                        puntosSeleccionados.isEmpty() -> Color(0xFF334155)
                        letraActual != null           -> colorPrimario
                        else                          -> Color(0xFFF59E0B)
                    },
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(vertical = 10.dp, horizontal = 14.dp)
                .semantics {
                    contentDescription = when {
                        puntosSeleccionados.isEmpty() -> "Ningún punto seleccionado. Teclado listo."
                        letraActual != null -> "Letra formada: ${letraActual.uppercase()}. Puntos activos: ${puntosSeleccionados.sorted().joinToString(" y ")}"
                        else -> "Combinación no reconocida: puntos ${puntosSeleccionados.sorted().joinToString(" con ")}"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                puntosSeleccionados.isEmpty() -> {
                    Text(
                        "Toca los puntos para formar un carácter",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
                letraActual != null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "LETRA:",
                            color = Color(0xFF6EE7B7),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = letraActual.uppercase(),
                            color = Color.White,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(Modifier.width(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "Puntos ${puntosSeleccionados.sorted().joinToString(" - ")}",
                                color = Color(0xFFA7F3D0),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = "⚠ Puntos ${puntosSeleccionados.sorted().joinToString(" - ")} (Sin letra asignada)",
                        color = Color(0xFFFCD34D),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ── Los 6 Puntos Braille (Columnas 1-2-3 y 4-5-6) ──────────────────────
        Row(
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Columna Izquierda: Puntos 1, 2, 3
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics { contentDescription = "Columna izquierda: puntos táctiles 1, 2 y 3" }
            ) {
                listOf(1, 2, 3).forEach { punto ->
                    PuntoBraille(
                        numero        = punto,
                        posicionTexto = when(punto) { 1 -> "Superior"; 2 -> "Centro"; else -> "Inferior" },
                        seleccionado  = punto in puntosSeleccionados,
                        colorPrimario = colorPrimario,
                        onToggle      = {
                            vibrateTap(haptic)
                            NutriEarcons.playButtonHover()
                            val nuevosPuntos = if (punto in puntosSeleccionados)
                                puntosSeleccionados - punto
                            else
                                puntosSeleccionados + punto
                            puntosSeleccionados = nuevosPuntos
                            val letra = TABLA_BRAILLE_VERIFICADA[nuevosPuntos]
                            if (letra != null) {
                                ttsManager?.hablar("Forma letra ${letra.uppercase()}. Puntos ${nuevosPuntos.sorted().joinToString(" ")}")
                            } else {
                                if (nuevosPuntos.isEmpty()) {
                                    ttsManager?.hablar("Puntos liberados")
                                } else {
                                    val puntosTexto = nuevosPuntos.sorted().joinToString(" ")
                                    ttsManager?.hablar("Puntos $puntosTexto")
                                }
                            }
                        }
                    )
                }
            }

            // Columna Derecha: Puntos 4, 5, 6
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.semantics { contentDescription = "Columna derecha: puntos táctiles 4, 5 y 6" }
            ) {
                listOf(4, 5, 6).forEach { punto ->
                    PuntoBraille(
                        numero        = punto,
                        posicionTexto = when(punto) { 4 -> "Superior"; 5 -> "Centro"; else -> "Inferior" },
                        seleccionado  = punto in puntosSeleccionados,
                        colorPrimario = colorPrimario,
                        onToggle      = {
                            vibrateTap(haptic)
                            NutriEarcons.playButtonHover()
                            val nuevosPuntos = if (punto in puntosSeleccionados)
                                puntosSeleccionados - punto
                            else
                                puntosSeleccionados + punto
                            puntosSeleccionados = nuevosPuntos
                            val letra = TABLA_BRAILLE_VERIFICADA[nuevosPuntos]
                            if (letra != null) {
                                ttsManager?.hablar("Forma letra ${letra.uppercase()}. Puntos ${nuevosPuntos.sorted().joinToString(" ")}")
                            } else {
                                if (nuevosPuntos.isEmpty()) {
                                    ttsManager?.hablar("Puntos liberados")
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

        // ── Barra Principal de Acciones (Borrar | Agregar Letra | Espacio) ───────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            // Botón Borrar (Backspace)
            IconButton(
                onClick  = {
                    if (textoActual.isNotEmpty()) {
                        vibrateTap(haptic)
                        NutriEarcons.playButtonHover()
                        val borrada = textoActual.last().toString()
                        val nuevoTexto = textoActual.dropLast(1)
                        onTextoChange(nuevoTexto)
                        ttsManager?.hablar("Borrado $borrada. Queda: ${nuevoTexto.ifEmpty { "vacío" }}")
                    } else {
                        vibrateError(haptic)
                        NutriEarcons.playError()
                        ttsManager?.hablar("Nada que borrar")
                    }
                },
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF3F1B24))
                    .border(1.dp, Color(0xFFE11D48).copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .semantics {
                        contentDescription = "Borrar último carácter. Toca dos veces para eliminar la última letra escrita."
                    }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Backspace,
                    contentDescription = null,
                    tint = Color(0xFFFB7185),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Botón Confirmar / Agregar Letra
            Button(
                onClick  = {
                    val letra = letraActual
                    if (letra != null) {
                        vibrateSuccess(haptic)
                        NutriEarcons.playSuccess()
                        val nuevoTexto = textoActual + letra
                        onTextoChange(nuevoTexto)
                        ultimaLetraAgregada = letra
                        puntosSeleccionados = emptySet()
                        ttsManager?.hablar("Añadida letra ${letra.uppercase()}. Texto: $nuevoTexto")
                    } else if (puntosSeleccionados.isEmpty()) {
                        vibrateError(haptic)
                        NutriEarcons.playError()
                        ttsManager?.hablar("Selecciona los puntos de la letra primero")
                    } else {
                        vibrateError(haptic)
                        NutriEarcons.playError()
                        ttsManager?.hablar("Combinación no válida. Revisa los puntos seleccionados.")
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .semantics {
                        contentDescription = when {
                            letraActual != null -> "Botón verde: Agregar letra ${letraActual.uppercase()} al texto. Toca dos veces para insertar."
                            puntosSeleccionados.isEmpty() -> "Botón Agregar letra deshabilitado. Selecciona puntos primero."
                            else -> "Botón Agregar letra no disponible por combinación inválida."
                        }
                    },
                shape  = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = if (letraActual != null) colorPrimario else Color(0xFF1E293B),
                    disabledContainerColor = Color(0xFF1E293B)
                ),
                border = if (letraActual != null) {
                    androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF6EE7B7))
                } else {
                    androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                }
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (letraActual != null) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        text = when {
                            letraActual != null           -> "AGREGAR \"${letraActual.uppercase()}\""
                            puntosSeleccionados.isEmpty() -> "Selecciona puntos"
                            else                          -> "No reconocido"
                        },
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (letraActual != null) Color.White else Color(0xFF94A3B8)
                    )
                }
            }

            // Botón Espacio
            IconButton(
                onClick  = {
                    vibrateTap(haptic)
                    NutriEarcons.playButtonHover()
                    onTextoChange("$textoActual ")
                    puntosSeleccionados = emptySet()
                    ttsManager?.hablar("Espacio agregado")
                },
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF1E293B))
                    .border(1.dp, Color(0xFF475569), RoundedCornerShape(18.dp))
                    .semantics { contentDescription = "Botón agregar espacio entre palabras." }
            ) {
                Icon(
                    imageVector = Icons.Rounded.SpaceBar,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // ── Fila de Acciones Secundarias (Limpiar Patrón & Continuar) ───────────
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick  = {
                    vibrateTap(haptic)
                    NutriEarcons.playButtonHover()
                    puntosSeleccionados = emptySet()
                    ttsManager?.hablar("Patrón limpiado.")
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .semantics { contentDescription = "Limpiar puntos seleccionados del teclado Braille." },
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8))
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFF94A3B8)
                )
                Spacer(Modifier.width(6.dp))
                Text("Limpiar", color = Color(0xFF94A3B8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            if (onNext != null) {
                Button(
                    onClick = {
                        vibrateSuccess(haptic)
                        NutriEarcons.playSuccess()
                        ttsManager?.hablar("Confirmado. Continuando al siguiente campo.")
                        onNext()
                    },
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp)
                        .semantics {
                            contentDescription = "Botón verde Continuar y avanzar al siguiente paso o campo."
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colorPrimario),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF6EE7B7))
                ) {
                    Text(
                        text = "Continuar ➔",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun PuntoBraille(
    numero:        Int,
    posicionTexto: String,
    seleccionado:  Boolean,
    colorPrimario: Color,
    onToggle:      () -> Unit
) {
    val escala by animateFloatAsState(
        targetValue = if (seleccionado) 1.15f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "esc$numero"
    )
    val fondo by animateColorAsState(
        targetValue = if (seleccionado) colorPrimario else Color(0xFF1E293B),
        label = "col$numero"
    )
    val borde by animateColorAsState(
        targetValue = if (seleccionado) Color(0xFF6EE7B7) else Color(0xFF475569),
        label = "brd$numero"
    )

    Box(
        modifier = Modifier
            .size(74.dp)
            .scale(escala)
            .clip(CircleShape)
            .background(fondo)
            .border(if (seleccionado) 3.dp else 2.dp, borde, CircleShape)
            .pointerInput(Unit) { detectTapGestures(onTap = { onToggle() }) }
            .semantics {
                contentDescription = "Punto $numero, posición $posicionTexto. ${if (seleccionado) "Marcado activo" else "No seleccionado"}. Toca dos veces para cambiar."
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Indicador visual táctil central
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(if (seleccionado) Color.White.copy(alpha = 0.8f) else Color(0xFF64748B))
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = numero.toString(),
                color      = if (seleccionado) Color.White else Color(0xFFCBD5E1),
                fontSize   = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign  = TextAlign.Center
            )
        }
    }
}
