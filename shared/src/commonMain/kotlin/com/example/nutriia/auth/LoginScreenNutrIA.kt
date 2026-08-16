package com.example.nutriia.auth

// import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.Voz
import kotlin.random.Random

val NutriaGreen     = Color(0xFF689F38)
val NutriaDarkGreen = Color(0xFF33691E)
val NutriaOrange    = Color(0xFFFF8F00)
val NutriaBgCrema   = Color(0xFFF8F9F3)

@Composable
fun NutriaLoginScreen(
    viewModel: LoginViewModel = viewModel(),
    onNavigateAsParent: () -> Unit       = {},
    onNavigateAsNutritionist: () -> Unit = {},
    onNavigateToRegister: () -> Unit     = {},
    onNavigateToBiometricActivation: (uid: String, rol: String) -> Unit = { _, _ -> }
) {
    var email        by remember { mutableStateOf("") }
    var password     by remember { mutableStateOf("") }
    var showError    by remember { mutableStateOf<String?>(null) }
    var showReset    by remember { mutableStateOf(false) }
    var resetEmail   by remember { mutableStateOf("") }
    var resetMessage by remember { mutableStateOf<String?>(null) }
    val estado       by viewModel.estado.collectAsState()

        val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()

    // Anuncia toda la pantalla al entrar
    LaunchedEffect(Unit) {
        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(Voz.LOGIN_INTRO)
    }

    // El estado Exito ahora se maneja principalmente en MainActivity para evitar race conditions
    // con la lista de hijos. Aquí solo manejamos feedback visual/auditivo.
    LaunchedEffect(estado) {
        when (val s = estado) {
            is LoginUiState.Exito -> {
                if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(Voz.LOGIN_EXITO)
                // Nota: La navegación y el resetEstado se delegan a MainActivity.
            }
            is LoginUiState.Error -> {
                showError = s.mensaje
                if (a11yMode == AccessibilityMode.BLIND)
                    a11yVm.hablar("Error al iniciar sesion. ${s.mensaje}. Verifica tus datos e intenta de nuevo.")
            }
            else -> {}
        }
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }
    val entranceAlpha by animateFloatAsState(if (startAnimation) 1f else 0f, tween(1000), label = "alpha")

    Box(modifier = Modifier.fillMaxSize().background(NutriaBgCrema)) {
        AnimatedMinimalistBackground()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .alpha(entranceAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(40.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                MascotaMinimalista()
                Text("Nutre su hoy, protege su manaña", fontSize = 15.sp, color = NutriaDarkGreen,
                    fontWeight = FontWeight.SemiBold, letterSpacing = 0.3.sp)
            }
            Spacer(Modifier.height(32.dp))
            Card(
                modifier  = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                shape     = RoundedCornerShape(32.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Iniciar Sesion", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
                    Spacer(Modifier.height(28.dp))

                    // Campo correo
                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        placeholder = { Text("Correo Electronico", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Campo uno de dos. Correo electrónico. " +
                                    if (email.isEmpty()) "Vacio. Toca para escribir o dictar tu correo."
                                    else "Valor: $email"
                        },
                        leadingIcon = { Icon(Icons.Default.Email, null, tint = NutriaGreen) },
                        shape = RoundedCornerShape(16.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriaGreen, unfocusedBorderColor = Color(0xFFF0F0F0),
                            focusedContainerColor = Color(0xFFFAFAFA), unfocusedContainerColor = Color(0xFFFAFAFA))
                    )
                    Spacer(Modifier.height(16.dp))

                    // Campo contrasena
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        placeholder = { Text("Contraseña", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().semantics {
                            contentDescription = "Campo dos de dos. Contraseña. " +
                                    if (password.isEmpty()) "Vacío. Toca para escribir o dictar tu contraseña."
                                    else "${password.length} caracteres escritos."
                        },
                        leadingIcon = { Icon(Icons.Default.Lock, null, tint = NutriaGreen) },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp), singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriaGreen, unfocusedBorderColor = Color(0xFFF0F0F0),
                            focusedContainerColor = Color(0xFFFAFAFA), unfocusedContainerColor = Color(0xFFFAFAFA))
                    )

                    TextButton(
                        onClick = {
                            if (a11yMode == AccessibilityMode.BLIND)
                                a11yVm.hablar("Abriendo recuperación de contraseña.")
                            showReset = true
                        },
                        modifier = Modifier.align(Alignment.End).semantics {
                            contentDescription = "Botón olvidaste tu contraseña. Lado derecho debajo del campo contraseña."
                        }
                    ) { Text("Olvidaste tu contraseña?", color = Color.Gray, fontSize = 12.sp) }

                    Spacer(Modifier.height(24.dp))

                    // Boton ENTRAR
                    Button(
                        onClick = {
                            if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(Voz.LOGIN_INICIANDO)
                            viewModel.login(email, password)
                        },
                        enabled  = estado !is LoginUiState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (a11yMode == AccessibilityMode.BLIND) 70.dp else 56.dp)
                            .semantics {
                                contentDescription = "Botón ENTRAR. Parte inferior de la tarjeta. " +
                                        if (estado is LoginUiState.Loading) "Cargando, espera."
                                        else "Toca para iniciar sesión con tus datos."
                            },
                        shape  = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NutriaGreen, disabledContainerColor = Color.LightGray),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (estado is LoginUiState.Loading) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("ENTRAR", fontWeight = FontWeight.Bold, letterSpacing = 1.sp,
                                    fontSize = if (a11yMode == AccessibilityMode.BLIND) 18.sp else 14.sp)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    // Botón Biométrico
                    if (viewModel.hayHuellaDisponible()) {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                viewModel.loginConHuella(
                                    onExito = { },
                                    onFail = {}
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .semantics {
                                    contentDescription = "Ingresar con huella digital."
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0A2533)
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Ingresar con huella", fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Icon(Icons.Default.Fingerprint, null, tint = Color.White)
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.semantics(mergeDescendants = true) {}
            ) {
                Text("Nuevo en NutriIA?", color = Color.Gray, fontSize = 14.sp)
                TextButton(
                    onClick = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar("Abriendo pantalla para crear cuenta nueva en NutriIA.")
                        onNavigateToRegister()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = "Botón Crea una cuenta. Parte inferior de la pantalla. Toca para registrarte."
                    }
                ) {
                    Text("Crea una cuenta", color = NutriaDarkGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(30.dp))
        }

        showError?.let { msg ->
            AlertDialog(
                onDismissRequest = { showError = null; viewModel.resetEstado() },
                title = { Text("Error", fontWeight = FontWeight.Bold) },
                text  = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { showError = null; viewModel.resetEstado() }) {
                        Text("Entendido", color = NutriaGreen, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }

        if (showReset) {
            LaunchedEffect(Unit) {
                if (a11yMode == AccessibilityMode.BLIND)
                    a11yVm.hablar("Dialogo recuperar contraseña. Escribe tu correo en el campo y toca Enviar. Para cancelar toca Cancelar.")
            }
            AlertDialog(
                onDismissRequest = { showReset = false; resetMessage = null },
                title = { Text("Recuperar contraseña", fontWeight = FontWeight.Bold) },
                text  = {
                    Column {
                        Text("Escribe tu correo y te enviaremos un enlace.", color = Color.Gray, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = resetEmail, onValueChange = { resetEmail = it },
                            placeholder = { Text("Correo electrónico") },
                            singleLine = true, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().semantics {
                                contentDescription = "Campo correo para recuperar contraseña. Toca para escribir."
                            }
                        )
                        resetMessage?.let {
                            Spacer(Modifier.height(8.dp))
                            Text(it, color = NutriaGreen, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar("Enviando correo de recuperacion.")
                        viewModel.recuperarContrasena(resetEmail) { ok ->
                            resetMessage = if (ok) "Correo enviado. Revisa tu bandeja."
                            else "No se pudo enviar. Verifica el correo."
                            if (a11yMode == AccessibilityMode.BLIND) a11yVm.hablar(resetMessage ?: "")
                        }
                    }) { Text("Enviar", color = NutriaGreen, fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { showReset = false; resetMessage = null }) {
                        Text("Cancelar", color = Color.Gray)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun MascotaMinimalista() {
    Box(contentAlignment = Alignment.Center,
        modifier = Modifier.size(300.dp).semantics { contentDescription = "" }) {
        com.example.nutriia.shared.NutriaMascotaHeader(modifier = Modifier.fillMaxSize())
    }
}

@Composable
fun AnimatedMinimalistBackground() {
    val it = rememberInfiniteTransition(label = "bg")
    val icons = listOf(Icons.Rounded.Eco, Icons.Rounded.Spa, Icons.Rounded.Psychology, Icons.Rounded.LocalFlorist)
    Box(modifier = Modifier.fillMaxSize().semantics { contentDescription = "" }) {
        repeat(12) { idx ->
            val sx = remember { Random.nextFloat() }
            val sy = remember { Random.nextFloat() }
            val sp = remember { Random.nextInt(6000, 10000) }
            val ty by it.animateFloat(0f, 50f,
                infiniteRepeatable(tween(sp, easing = EaseInOutSine), RepeatMode.Reverse), label = "y")
            val rz by it.animateFloat(-10f, 10f,
                infiniteRepeatable(tween(sp + 1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "r")
            Icon(icons[idx % icons.size], null,
                modifier = Modifier.offset((sx * 400).dp, (sy * 850).dp)
                    .graphicsLayer { translationY = ty; rotationZ = rz; alpha = 0.04f }.size(80.dp),
                tint = Color.Black)
        }
    }
}
