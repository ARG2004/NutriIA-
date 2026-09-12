package com.example.nutriia.accesibilidad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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

enum class CategoriaSena {
    ALIMENTACION, SALUD_ALERTA, CLINICA, CONTROL
}

data class SenaLSMInfo(
    val id: String,
    val nombre: String,
    val emoji: String,
    val descripcionLSM: String,
    val sugerenciaFrase: String,
    val categoria: CategoriaSena
)

data class ResultadoSenaComunicativa(
    val sena: SenaLSMInfo,
    val confianza: Float
)

val CATALOGO_SENAS_COMUNICATIVAS = listOf(
    SenaLSMInfo("LECHE", "Leche / Lactancia", "🍼", "Mano en 'S'/'C' con movimiento rítmico de ordeño frente al pecho.", "Registrar toma de leche materna", CategoriaSena.ALIMENTACION),
    SenaLSMInfo("COMIDA", "Comida / Papilla", "🥣", "Mano en 'O' aplanada (yemas unidas) llevada repetidamente hacia la boca.", "Registrar comida de sólidos", CategoriaSena.ALIMENTACION),
    SenaLSMInfo("AGUA", "Agua", "💧", "Letra 'W' tocando suavemente la barbilla dos veces con el costado del índice.", "Registrar toma de agua", CategoriaSena.ALIMENTACION),
    SenaLSMInfo("BEBE", "Bebé / Hijo", "👶", "Brazos cruzados al pecho con balanceo de vaivén meciendo al bebé.", "Seleccionar perfil de mi bebé", CategoriaSena.CLINICA),
    SenaLSMInfo("FIEBRE", "Fiebre / Temperatura", "🤒", "Letra 'F' (variante formal) o palma/dorso colocados sobre la frente.", "Alerta: Mi bebé tiene fiebre", CategoriaSena.SALUD_ALERTA),
    SenaLSMInfo("DOCTOR", "Doctor / Pediatra", "🩺", "Letra 'D' o 'M' tocando el pulso radial en la muñeca opuesta.", "Contactar pediatra de guardia", CategoriaSena.CLINICA),
    SenaLSMInfo("MEDICINA", "Medicina / Tratamiento", "💊", "Dedo medio frotando en círculo el centro de la palma contraria.", "Registrar dosis de medicamento", CategoriaSena.SALUD_ALERTA),
    SenaLSMInfo("PESO", "Peso / Medición", "⚖️", "Ambas manos palmas arriba alternando movimiento vertical de balanza.", "Registrar nuevo peso y talla", CategoriaSena.CLINICA),
    SenaLSMInfo("SI_CONFIRMAR", "Sí / Guardar", "👍", "Puño cerrado con pulgar hacia arriba en movimiento afirmativo.", "Confirmar y Guardar", CategoriaSena.CONTROL),
    SenaLSMInfo("NO_CANCELAR", "No / Cancelar", "👎", "Dedo índice oscilando lateralmente o pulgar hacia abajo.", "Cancelar / Borrar", CategoriaSena.CONTROL),
    SenaLSMInfo("AYUDA", "Ayuda / Tutorial", "✋", "Pulgar arriba apoyado sobre palma opuesta elevándose hacia adelante.", "Abrir centro de ayuda", CategoriaSena.CONTROL),
    SenaLSMInfo("GRACIAS", "Gracias", "🙏", "Dedo medio tocando barbilla y proyectándose hacia el frente.", "Muchas gracias", CategoriaSena.CONTROL)
)

enum class ModoSeñaLSM {
    ABECEDARIO, COMUNICATIVO
}

@Composable
fun SignLanguageCameraView(
    textoActual:   String,
    onTextoChange: (String) -> Unit,
    colorPrimario: Color    = Color(0xFF4CAF50),
    soloNumeros:   Boolean  = false,
    esCampoFecha:  Boolean  = false,
    onCompletado:  (() -> Unit)? = null,
    onSenaComunicativaDetectada: ((SenaLSMInfo) -> Unit)? = null,
    modifier:      Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var modoSeleccionado by remember { mutableStateOf(ModoSeñaLSM.ABECEDARIO) }

    val letras = if (soloNumeros) {
        listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0")
    } else {
        ('A'..'Z').map { it.toString() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1E293B))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lengua de Señas Mexicana (LSM)",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Spacer(Modifier.height(8.dp))

        // Selector de Modo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) colorPrimario else Color.Transparent)
                    .clickable {
                        vibrateTap(haptic)
                        modoSeleccionado = ModoSeñaLSM.ABECEDARIO
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔤 Abecedario (A-Z)",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) FontWeight.Bold else FontWeight.Normal
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (modoSeleccionado == ModoSeñaLSM.COMUNICATIVO) colorPrimario else Color.Transparent)
                    .clickable {
                        vibrateTap(haptic)
                        modoSeleccionado = ModoSeñaLSM.COMUNICATIVO
                    }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "💬 Señas LSM",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = if (modoSeleccionado == ModoSeñaLSM.COMUNICATIVO) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (modoSeleccionado == ModoSeñaLSM.ABECEDARIO) {
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
        } else {
            // Catálogo rápido de señas comunicativas
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                CATALOGO_SENAS_COMUNICATIVAS.take(6).forEach { sena ->
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                vibrateTap(haptic)
                                onSenaComunicativaDetectada?.invoke(sena)
                                val espacio = if (textoActual.isNotEmpty() && !textoActual.endsWith(" ")) " " else ""
                                onTextoChange(textoActual + espacio + sena.nombre)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(sena.emoji, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sena.nombre, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(sena.sugerenciaFrase, color = Color.LightGray, fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }
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
