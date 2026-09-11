package com.example.nutriia.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun BiometricActivationScreen(
    uid: String,
    rol: String,
    onActivado: () -> Unit,
    onOmitido: () -> Unit
) {
    val NutriaGreen = Color(0xFF689F38)
    val NutriaBgCrema = Color(0xFFF8F9F3)
    val NutriaDarkGreen = Color(0xFF1B5E20)

    val tipoBiometria = remember { BiometricHelper.obtenerTipoBiometria() }
    val esFaceId = tipoBiometria == TipoBiometria.FACE_ID
    val tituloBiometrico = if (esFaceId) "Activar Face ID" else "Activar Touch ID / Huella"
    val descBiometrico = if (esFaceId) {
        "Inicia sesión al instante con reconocimiento facial en tu dispositivo de forma rápida y segura."
    } else {
        "Inicia sesión al instante usando tu huella digital de forma rápida y segura."
    }

    var autenticando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var exitoActivacion by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(NutriaGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    exitoActivacion -> Icons.Rounded.CheckCircle
                    esFaceId -> Icons.Default.Face
                    else -> Icons.Default.Fingerprint
                },
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = if (exitoActivacion) NutriaDarkGreen else NutriaGreen
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = if (exitoActivacion) "¡Acceso Activado!" else tituloBiometrico,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A2533),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (exitoActivacion) "Tu dispositivo ha quedado vinculado con éxito." else descBiometrico,
            fontSize = 15.sp,
            color = Color(0xFF556970),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        AnimatedVisibility(
            visible = mensajeError != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            mensajeError?.let { msg ->
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFC62828),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(36.dp))

        Button(
            onClick = {
                if (autenticando) return@Button
                autenticando = true
                mensajeError = null

                BiometricHelper.prompt(
                    titulo = "Confirma tu identidad para activar $tituloBiometrico",
                    onSuccess = {
                        autenticando = false
                        exitoActivacion = true
                        SessionManager.marcarBiometricoActivo(activo = true)
                        SessionManager.marcarHuellaConfirmada()
                        SessionManager.marcarActivacionHuellaMostrada()
                        SessionManager.guardarSesion(uid = uid)
                        SessionManager.guardarUltimoUid(uid = uid)

                        scope.launch {
                            delay(600)
                            onActivado()
                        }
                    },
                    onFail = {
                        autenticando = false
                        mensajeError = "No se pudo verificar tu identidad. Puedes intentarlo de nuevo o continuar con tu contraseña."
                    }
                )
            },
            enabled = !autenticando && !exitoActivacion,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)
        ) {
            if (autenticando) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = if (esFaceId) "Habilitar Face ID" else "Habilitar Touch ID / Huella",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        TextButton(
            onClick = {
                SessionManager.marcarActivacionHuellaMostrada()
                SessionManager.guardarSesion(uid = uid)
                SessionManager.guardarUltimoUid(uid = uid)
                onOmitido()
            },
            enabled = !autenticando,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Ahora no", color = Color(0xFF78909C), fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
    }
}
