package com.example.nutriia.auth

import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriia.accesibilidad.*
import kotlin.random.Random

data class RoleItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
fun NutriaLoginScreen(
    onLoginSuccess: (email: String, rol: String) -> Unit,
    onNavigateToRegister: () -> Unit,
    onBiometricLogin: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showReset by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    val ttsBridge = remember { NutriTTSBridge() }

    LaunchedEffect(Unit) {
        ttsBridge.speak(Voz.LOGIN_INTRO)
    }

    Box(modifier = Modifier.fillMaxSize().background(NutriaBgCrema)) {
        AnimatedMinimalistBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(50.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                NutriaMascotaHeader(modifier = Modifier.size(230.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Nutre su hoy, protege su mañana",
                    fontSize = 15.sp,
                    color = NutriaDarkGreen,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Iniciar Sesión",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = NutriaDarkGreen
                    )
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("Correo Electrónico", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = "Email", tint = NutriaGreen) },
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriaGreen,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        placeholder = { Text("Contraseña", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = "Contraseña", tint = NutriaGreen) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Mostrar contraseña",
                                    tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriaGreen,
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedContainerColor = Color(0xFFFAFAFA),
                            unfocusedContainerColor = Color(0xFFFAFAFA)
                        )
                    )

                    TextButton(
                        onClick = { showReset = true },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("¿Olvidaste tu contraseña?", color = Color.Gray, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                isLoading = true
                                ttsBridge.speak(Voz.LOGIN_EXITO)
                                val roleDetected = when {
                                    email.contains("nutri") -> "nutriologo"
                                    email.contains("gine") -> "ginecologo"
                                    email.contains("mama") -> "mama_primeriza"
                                    else -> "padre"
                                }
                                onLoginSuccess(email, roleDetected)
                            },
                            enabled = !isLoading,
                            modifier = Modifier.weight(1f).height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NutriaGreen,
                                disabledContainerColor = Color.LightGray
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("ENTRAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, fontSize = 15.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                                }
                            }
                        }

                        IconButton(
                            onClick = onBiometricLogin,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(NutriaGreen.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Rounded.Fingerprint, contentDescription = "Touch ID", tint = NutriaGreen, modifier = Modifier.size(30.dp))
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text("¿Nuevo en NutrIA?", color = Color.Gray, fontSize = 14.sp)
                TextButton(onClick = onNavigateToRegister) {
                    Text("Crea una cuenta", color = NutriaDarkGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
        }

        if (showReset) {
            AlertDialog(
                onDismissRequest = { showReset = false; resetMessage = null },
                title = { Text("Recuperar contraseña", fontWeight = FontWeight.Bold, color = NutriaDarkGreen) },
                text = {
                    Column {
                        Text("Escribe tu correo y te enviaremos las instrucciones.", color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail,
                            onValueChange = { resetEmail = it },
                            placeholder = { Text("ejemplo@correo.com") },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        resetMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = NutriaGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        resetMessage = "Correo de recuperación enviado con éxito."
                    }) { Text("Enviar", color = NutriaGreen, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showReset = false; resetMessage = null }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                shape = RoundedCornerShape(24.dp)
            )
        }
    }
}

@Composable
fun NutriaMascotaHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(NutriaGreen.copy(alpha = 0.10f))
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.Eco,
                contentDescription = "Mascota NutriIA",
                tint = NutriaGreen,
                modifier = Modifier.size(110.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text("NutrIA", fontSize = 28.sp, fontWeight = FontWeight.Black, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun AnimatedMinimalistBackground() {
    val it = rememberInfiniteTransition(label = "bg")
    val icons = listOf(Icons.Rounded.Eco, Icons.Rounded.Spa, Icons.Rounded.Psychology, Icons.Rounded.LocalFlorist)
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(8) { idx ->
            val sx = remember { Random.nextFloat() }
            val sy = remember { Random.nextFloat() }
            val ty by it.animateFloat(0f, 40f, infiniteRepeatable(tween(7000 + idx * 500, easing = EaseInOutSine), RepeatMode.Reverse), label = "y")
            val rz by it.animateFloat(-8f, 8f, infiniteRepeatable(tween(8000 + idx * 500, easing = EaseInOutSine), RepeatMode.Reverse), label = "r")
            Icon(
                icons[idx % icons.size],
                null,
                modifier = Modifier
                    .offset((sx * 360).dp, (sy * 700).dp)
                    .graphicsLayer {
                        translationY = ty
                        rotationZ = rz
                        alpha = 0.04f
                    }
                    .size(70.dp),
                tint = Color.Black
            )
        }
    }
}

@Composable
fun BiometricActivationScreen(
    onActivated: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(NutriaGreen.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(60.dp))
        }

        Spacer(Modifier.height(24.dp))
        Text("Activar Face ID / Touch ID", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        Spacer(Modifier.height(8.dp))
        Text("Accede de forma rápida y segura a los expedientes de tu familia sin ingresar tu contraseña.", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(Modifier.height(36.dp))

        Button(
            onClick = onActivated,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)
        ) {
            Text("Activar Seguridad Biométrica", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text("Ahora no", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Composable
fun RegisterTypeScreen(
    onRoleSelected: (String) -> Unit,
    onBackToLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(44.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onBackToLogin,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = NutriaGreen)
            }
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(NutriaGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.PersonAdd, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(16.dp))
        Text("Crear cuenta", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
        Spacer(Modifier.height(4.dp))
        Text("¿Cómo vas a usar NutriIA?", fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center)

        Spacer(Modifier.height(28.dp))

        AccountTypeCard(
            title = "Soy Padre / Madre",
            subtitle = "Registra a tu hijo/a y lleva su seguimiento nutricional personalizado.",
            icon = Icons.Rounded.FamilyRestroom,
            iconColor = NutriaGreen,
            badge = "Familia",
            badgeColor = NutriaGreen,
            onClick = { onRoleSelected("Padre / Madre de Familia") }
        )

        Spacer(Modifier.height(12.dp))

        AccountTypeCard(
            title = "Mamá Primeriza",
            subtitle = "Seguimiento especializado durante tu embarazo y nutrición prenatal.",
            icon = Icons.Rounded.Favorite,
            iconColor = NutriaPink,
            badge = "Embarazo",
            badgeColor = NutriaPink,
            onClick = { onRoleSelected("Mamá Primeriza") }
        )

        Spacer(Modifier.height(12.dp))

        AccountTypeCard(
            title = "Soy Nutriólogo/a",
            subtitle = "Gestiona pacientes, planes de alimentación y seguimiento clínico.",
            icon = Icons.Rounded.MedicalServices,
            iconColor = NutriaSoftTeal,
            badge = "Profesional",
            badgeColor = NutriaSoftTeal,
            onClick = { onRoleSelected("Nutriólogo Clínico") }
        )

        Spacer(Modifier.height(12.dp))

        AccountTypeCard(
            title = "Soy Ginecólogo/a",
            subtitle = "Especialista en salud femenina y seguimiento del embarazo.",
            icon = Icons.Rounded.Female,
            iconColor = Color(0xFFF06292),
            badge = "Profesional",
            badgeColor = Color(0xFFF06292),
            onClick = { onRoleSelected("Ginecólogo Obstetra") }
        )

        Spacer(Modifier.height(28.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("¿Ya tienes cuenta? ", color = Color.Gray, fontSize = 14.sp)
            TextButton(onClick = onBackToLogin) {
                Text("Inicia sesión", color = NutriaDarkGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun AccountTypeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    badge: String,
    badgeColor: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NutriaDarkGreen)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(badge, fontSize = 10.sp, color = badgeColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(subtitle, fontSize = 12.sp, color = Color.Gray, lineHeight = 16.sp)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = iconColor.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ParentRegisterScreen(
    onRegistered: (String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var childName by remember { mutableStateOf("") }
    var nutritionistCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(44.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(NutriaGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.FamilyRestroom, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Registro de Padre/Madre", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                Text("Crea tu cuenta familiar", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))

        RegisterSectionHeader(icon = Icons.Rounded.Person, title = "Datos personales", color = NutriaGreen)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = name, onValueChange = { name = it }, label = "Nombre completo", leadingIcon = Icons.Rounded.Person)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = phone, onValueChange = { phone = it }, label = "Teléfono", leadingIcon = Icons.Rounded.Phone)

        Spacer(Modifier.height(20.dp))

        RegisterSectionHeader(icon = Icons.Rounded.Lock, title = "Acceso a la cuenta", color = NutriaSoftPurple)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = email, onValueChange = { email = it }, label = "Correo electrónico", leadingIcon = Icons.Rounded.Email)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(
            value = password,
            onValueChange = { password = it },
            label = "Clave de acceso",
            leadingIcon = Icons.Rounded.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = { showPassword = !showPassword }
        )
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirmar clave",
            leadingIcon = Icons.Rounded.LockReset,
            isPassword = true,
            showPassword = showConfirm,
            onTogglePassword = { showConfirm = !showConfirm }
        )

        Spacer(Modifier.height(20.dp))

        RegisterSectionHeader(icon = Icons.Rounded.ChildCare, title = "Tu primer hijo/a", color = NutriaOrange)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = childName, onValueChange = { childName = it }, label = "Nombre del niño/a", leadingIcon = Icons.Rounded.Face)

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RegisterSectionHeader(icon = Icons.Rounded.MedicalServices, title = "Vincular nutriólogo", color = NutriaSoftTeal)
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text("Opcional", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))

        Surface(
            color = NutriaSoftTeal.copy(alpha = 0.08f),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, contentDescription = null, tint = NutriaSoftTeal, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text("Puedes vincularte después desde tu perfil si no tienes el código ahora.", fontSize = 11.sp, color = NutriaDarkGreen)
            }
        }

        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = nutritionistCode, onValueChange = { nutritionistCode = it }, label = "Código del nutriólogo (opcional)", leadingIcon = Icons.Rounded.QrCodeScanner)

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onRegistered(name.ifBlank { "Familia Rivera" }, email.ifBlank { "familia@nutriia.com" }) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Crear cuenta y continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun MamaPrimerizaRegisterScreen(
    onRegistered: (String, String, Int) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var semanas by remember { mutableStateOf("1") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(44.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(NutriaPink.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Favorite, contentDescription = null, tint = NutriaPink, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Registro Mamá Primeriza", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                Text("Tu acompañante en el embarazo", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))

        RegisterSectionHeader(icon = Icons.Rounded.Person, title = "Datos personales", color = NutriaPink)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = name, onValueChange = { name = it }, label = "Nombre completo", leadingIcon = Icons.Rounded.Person)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = phone, onValueChange = { phone = it }, label = "Teléfono", leadingIcon = Icons.Rounded.Phone)

        Spacer(Modifier.height(20.dp))

        RegisterSectionHeader(icon = Icons.Rounded.DateRange, title = "Estado del embarazo", color = NutriaGreen)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = semanas, onValueChange = { semanas = it }, label = "Semana de embarazo (1-40)", leadingIcon = Icons.Rounded.Tag)

        Spacer(Modifier.height(20.dp))

        RegisterSectionHeader(icon = Icons.Rounded.Lock, title = "Acceso a la cuenta", color = NutriaSoftPurple)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = email, onValueChange = { email = it }, label = "Correo electrónico", leadingIcon = Icons.Rounded.Email)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(
            value = password,
            onValueChange = { password = it },
            label = "Contraseña",
            leadingIcon = Icons.Rounded.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = { showPassword = !showPassword }
        )
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirmar contraseña",
            leadingIcon = Icons.Rounded.LockReset,
            isPassword = true,
            showPassword = showConfirm,
            onTogglePassword = { showConfirm = !showConfirm }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onRegistered(name.ifBlank { "Mamá NutrIA" }, email.ifBlank { "mama@nutriia.com" }, semanas.toIntOrNull() ?: 1) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaPink)
        ) {
            Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun ProfessionalRegisterScreen(
    roleTitle: String,
    profesionRequerida: String,
    accentColor: Color,
    onRegistered: (String, String, String) -> Unit,
    onBack: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showConfirm by remember { mutableStateOf(false) }
    var especialidad by remember { mutableStateOf("") }
    var cedula by remember { mutableStateOf("") }
    var aceptoSep by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(44.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen)
            }
            Spacer(Modifier.width(14.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (profesionRequerida.contains("Nutri")) Icons.Rounded.MedicalServices else Icons.Rounded.Female,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("Registro de $roleTitle", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                Text("Crea tu perfil profesional", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(24.dp))

        RegisterSectionHeader(icon = Icons.Rounded.Person, title = "Datos personales", color = accentColor)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = name, onValueChange = { name = it }, label = "Nombre completo", leadingIcon = Icons.Rounded.Person)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = phone, onValueChange = { phone = it }, label = "Teléfono", leadingIcon = Icons.Rounded.Phone)

        Spacer(Modifier.height(20.dp))

        RegisterSectionHeader(icon = Icons.Rounded.VerifiedUser, title = "Datos profesionales", color = NutriaGreen)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = especialidad, onValueChange = { especialidad = it }, label = "Especialidad", leadingIcon = Icons.Rounded.AddBox)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = cedula, onValueChange = { cedula = it }, label = "Cédula profesional", leadingIcon = Icons.Rounded.Badge)

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
                .clickable { aceptoSep = !aceptoSep }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = aceptoSep, onCheckedChange = { aceptoSep = it }, colors = CheckboxDefaults.colors(checkedColor = accentColor))
            Spacer(Modifier.width(8.dp))
            Text(
                "Acepto que NutriIA consulte mi cédula profesional en el Registro Nacional de Profesionistas (SEP) para verificar mi identidad profesional.",
                fontSize = 11.sp,
                color = Color.Gray,
                lineHeight = 15.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        RegisterSectionHeader(icon = Icons.Rounded.Lock, title = "Acceso a la cuenta", color = NutriaSoftPurple)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(value = email, onValueChange = { email = it }, label = "Correo electrónico", leadingIcon = Icons.Rounded.Email)
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(
            value = password,
            onValueChange = { password = it },
            label = "Clave de acceso",
            leadingIcon = Icons.Rounded.Lock,
            isPassword = true,
            showPassword = showPassword,
            onTogglePassword = { showPassword = !showPassword }
        )
        Spacer(Modifier.height(10.dp))
        RegOutlinedField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirmar clave",
            leadingIcon = Icons.Rounded.LockReset,
            isPassword = true,
            showPassword = showConfirm,
            onTogglePassword = { showConfirm = !showConfirm }
        )

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = { onRegistered(name.ifBlank { "Dr. Profesional" }, email.ifBlank { "doctor@nutriia.com" }, cedula) },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Crear perfil profesional", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun RegisterSectionHeader(icon: ImageVector, title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
    }
}

@Composable
fun RegOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    isPassword: Boolean = false,
    showPassword: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(label, color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(leadingIcon, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        } else null,
        visualTransformation = if (isPassword && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NutriaGreen,
            unfocusedBorderColor = Color(0xFFEEEEEE),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        )
    )
}
