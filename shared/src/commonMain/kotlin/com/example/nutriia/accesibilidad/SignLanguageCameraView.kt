package com.example.nutriia.accesibilidad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SignLanguageCameraView(
    textoActual:   String,
    onTextoChange: (String) -> Unit,
    colorPrimario: Color    = Color(0xFF4CAF50),
    soloNumeros:   Boolean  = false,
    esCampoFecha:  Boolean  = false,
    onCompletado:  (() -> Unit)? = null,
    modifier:      Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val letras = if (soloNumeros) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    } else {
        ('A'..'Z').map { it.toString() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1E293B))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Asistente de Lengua de Señas (LSM)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Selecciona o ingresa los caracteres para el campo:",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 13.sp
        )
        Spacer(Modifier.height(12.dp))

        // Grid de letras
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 44.dp),
            modifier = Modifier.fillMaxWidth().heightIn(max = 160.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(letras) { letra ->
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorPrimario.copy(alpha = 0.2f))
                        .border(1.dp, colorPrimario.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            vibrateTap(haptic)
                            onTextoChange(textoActual + letra)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(letra, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Controles de edición
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    vibrateTap(haptic)
                    onTextoChange(textoActual + " ")
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Rounded.SpaceBar, contentDescription = "Espacio")
                Spacer(Modifier.width(4.dp))
                Text("Espacio")
            }

            Button(
                onClick = {
                    vibrateTap(haptic)
                    if (textoActual.isNotEmpty()) {
                        onTextoChange(textoActual.dropLast(1))
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48)),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Borrar")
                Spacer(Modifier.width(4.dp))
                Text("Borrar")
            }
        }

        if (onCompletado != null) {
            Spacer(Modifier.height(10.dp))
            Button(
                onClick = {
                    vibrateSuccess(haptic)
                    onCompletado()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colorPrimario),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.CheckCircle, contentDescription = "Confirmar")
                Spacer(Modifier.width(8.dp))
                Text("Confirmar y Continuar")
            }
        }
    }
}
