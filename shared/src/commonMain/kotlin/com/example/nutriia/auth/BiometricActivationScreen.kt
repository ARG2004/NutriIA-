package com.example.nutriia.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity

@Composable
fun BiometricActivationScreen(
    uid: String,
    rol: String,
    onActivado: () -> Unit,
    onOmitido: () -> Unit
) {
    val context = LocalContext.current
    val NutriaGreen = Color(0xFF689F38)
    val NutriaBgCrema = Color(0xFFF8F9F3)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = NutriaGreen
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Acceso rápido con huella",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0A2533),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Activa el inicio de sesión con tu huella digital para entrar más rápido la próxima vez.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = {
                (context as? FragmentActivity)?.let { activity ->
                    BiometricHelper.prompt(
                        activity = activity,
                        onSuccess = {
                            SessionManager.guardarSesion(context, uid)
                            onActivado()
                        },
                        onFail = {
                            // Si falla la activación, simplemente no guardamos pero permitimos continuar
                        }
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)
        ) {
            Text("Activar huella", fontWeight = FontWeight.Bold, color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(
            onClick = onOmitido,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Ahora no", color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}
