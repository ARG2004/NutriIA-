package com.example.nutriia.accesibilidad

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val NutriaGreen      = Color(0xFF689F38)
val NutriaDarkGreen  = Color(0xFF33691E)
val NutriaOrange     = Color(0xFFFF8F00)
val NutriaBgCrema    = Color(0xFFF8F9F3)
val NutriaSoftPurple = Color(0xFF9C8FE0)
val NutriaSoftTeal   = Color(0xFF4DB6AC)
val NutriaPink       = Color(0xFFEC9BBF)

enum class AccessibilityMode {
    NORMAL, BLIND, MUTE, DEAF, COLOR_BLIND
}

object Voz {
    const val MODO_CIEGO = "Hola, soy Nutr IA, tu nutria nutriologa favorita. Acabo de activar mi modo especial para ti. Voy a leer todo en voz alta y el microfono se activara solo en cada campo. Juntos vamos a cuidar la nutricion de tu familia."
    const val MODO_MUDO = "Modo para personas mudas activado. El teclado estara siempre visible para ti."
    const val MODO_NORMAL = "Modo estandar activado. Bienvenido a Nutr IA."
    const val ACCESIBILIDAD_INTRO = "Hola, bienvenido a Nutr IA, tu asistente de nutricion infantil. Antes de empezar, cuentame como usas el telefono para adaptarme a ti. Selecciona Estandar, Modo ciego o Modo mudo. Toca la opcion que va contigo y presiona Continuar."
    const val LOGIN_INTRO = "Que bueno verte de nuevo. Escribe tu correo y tu clave en los dos campos. El boton verde Entrar esta justo debajo."
    const val LOGIN_EXITO = "Listo, ya entre. Vamos a ver como esta la nutricion de tu familia."
    const val QUIZ_BIENVENIDA = "Ahora lo mas importante: conocer a tu pequeño o pequeña. Voy a pedirte cuatro datos: nombre, fecha de nacimiento, peso y talla."
}

data class A11yOption(val mode: AccessibilityMode, val title: String, val subtitle: String, val icon: ImageVector, val color: Color)

@Composable
fun AccesibilidadInicialScreen(
    currentMode: AccessibilityMode,
    onModeSelected: (AccessibilityMode) -> Unit,
    onSkip: () -> Unit
) {
    var idiomaSeleccionado by remember { mutableStateOf("ESPANOL_MX") }
    var modoActual by remember(currentMode) { mutableStateOf(currentMode) }
    val ttsBridge = remember { NutriTTSBridge() }

    LaunchedEffect(Unit) {
        ttsBridge.speak(Voz.ACCESIBILIDAD_INTRO)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NutriaBgCrema)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = {
                ttsBridge.stop()
                onSkip()
            }) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancelar", color = Color.Gray, fontSize = 13.sp)
            }
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(NutriaGreen.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Eco, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(36.dp))
        }

        Spacer(Modifier.height(8.dp))
        Text("NutriIA", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = NutriaDarkGreen)
        Text("Tu asistente de nutrición infantil", fontSize = 13.sp, color = Color.Gray)

        Spacer(Modifier.height(20.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(NutriaGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Accessibility, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("¿Cómo usas la app?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                Text("Adaptamos Nutri/IA a tus necesidades", fontSize = 12.sp, color = Color.Gray)
            }
        }

        Spacer(Modifier.height(16.dp))

        val opcionesAccesibilidad = listOf(
            Triple(AccessibilityMode.NORMAL, "Estándar", "Experiencia completa sin adaptaciones"),
            Triple(AccessibilityMode.BLIND, "Modo para personas ciegas", "Lector de pantalla, voz y alto contraste"),
            Triple(AccessibilityMode.MUTE, "Modo para personas mudas", "Sin entrada de voz, teclado visual siempre visible")
        )

        opcionesAccesibilidad.forEach { (mode, label, description) ->
            val isSelected = modoActual == mode
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clickable {
                        modoActual = mode
                        val prompt = when (mode) {
                            AccessibilityMode.BLIND -> Voz.MODO_CIEGO
                            AccessibilityMode.MUTE -> Voz.MODO_MUDO
                            else -> Voz.MODO_NORMAL
                        }
                        ttsBridge.speak(prompt)
                    },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFF1F8E9) else Color.White),
                border = BorderStroke(1.5.dp, if (isSelected) NutriaGreen else Color.LightGray.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) NutriaGreen.copy(0.15f) else Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                when (mode) {
                                    AccessibilityMode.NORMAL -> Icons.Rounded.CheckCircle
                                    AccessibilityMode.BLIND -> Icons.Rounded.Visibility
                                    else -> Icons.Rounded.VolumeOff
                                },
                                contentDescription = null,
                                tint = if (isSelected) NutriaGreen else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isSelected) NutriaGreen else NutriaDarkGreen)
                            Spacer(Modifier.height(2.dp))
                            Text(description, fontSize = 11.sp, color = Color.Gray)
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Text("Idioma de la voz", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen, modifier = Modifier.align(Alignment.Start))
        Spacer(Modifier.height(10.dp))

        val opcionesIdioma = listOf(
            Triple("ESPANOL_MX", "Español Latinoamérica", "Voz en español de México y Latinoamérica"),
            Triple("ESPANOL_US", "Español Estados Unidos", "Voz en español neutro de Estados Unidos")
        )

        opcionesIdioma.forEach { (key, label, description) ->
            val isSelected = idiomaSeleccionado == key
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .clickable { idiomaSeleccionado = key },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isSelected) Color(0xFFF1F8E9) else Color.White),
                border = BorderStroke(1.5.dp, if (isSelected) NutriaGreen else Color.LightGray.copy(alpha = 0.4f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE8F5E9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                            Text(description, fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    if (isSelected) {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Button(
            onClick = {
                ttsBridge.stop()
                onModeSelected(modoActual)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Continuar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = Color.White)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private val TABLA_BRAILLE_VERIFICADA: Map<Set<Int>, String> = buildMap {
    put(setOf(1), "a"); put(setOf(1, 2), "b"); put(setOf(1, 4), "c")
    put(setOf(1, 4, 5), "d"); put(setOf(1, 5), "e"); put(setOf(1, 2, 4), "f")
    put(setOf(1, 2, 4, 5), "g"); put(setOf(1, 2, 5), "h"); put(setOf(2, 4), "i")
    put(setOf(2, 4, 5), "j"); put(setOf(1, 3), "k"); put(setOf(1, 2, 3), "l")
    put(setOf(1, 3, 4), "m"); put(setOf(1, 3, 4, 5), "n"); put(setOf(1, 3, 5), "o")
    put(setOf(1, 2, 3, 4), "p"); put(setOf(1, 2, 3, 4, 5), "q"); put(setOf(1, 2, 3, 5), "r")
    put(setOf(2, 3, 4), "s"); put(setOf(2, 3, 4, 5), "t"); put(setOf(1, 3, 6), "u")
    put(setOf(1, 2, 3, 6), "v"); put(setOf(2, 4, 5, 6), "w"); put(setOf(1, 3, 4, 6), "x")
    put(setOf(1, 3, 4, 5, 6), "y"); put(setOf(1, 3, 5, 6), "z")
    put(setOf(1, 6), "á"); put(setOf(1, 2, 4, 6), "é"); put(setOf(3, 4), "í")
    put(setOf(3, 4, 5), "ó"); put(setOf(1, 5, 6), "ú"); put(setOf(1, 4, 5, 6), "ñ")
    put(setOf(2), "1"); put(setOf(2, 3), "2"); put(setOf(2, 5), "3")
    put(setOf(2, 6), "4"); put(setOf(3), "5"); put(setOf(3, 5), "6")
    put(setOf(3, 6), "7"); put(setOf(2, 3, 5), "8"); put(setOf(2, 3, 6), "9"); put(setOf(3, 5, 6), "0")
    put(setOf(5), "."); put(setOf(6), ","); put(setOf(2, 5, 6), "?"); put(setOf(3, 4, 6), "!")
}

@Composable
fun BrailleKeyboard(
    textoActual: String,
    onTextoChange: (String) -> Unit,
    colorPrimario: Color = NutriaGreen,
    onNext: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var puntosSeleccionados by remember { mutableStateOf(setOf<Int>()) }
    val letraActual = TABLA_BRAILLE_VERIFICADA[puntosSeleccionados].takeIf { puntosSeleccionados.isNotEmpty() }
    val ttsBridge = remember { NutriTTSBridge() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1A1A2E))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Teclado Braille (6 puntos)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            if (letraActual != null) {
                Text(
                    "Letra: \"$letraActual\"",
                    color = NutriaOrange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(1, 2, 3).forEach { p ->
                    BrailleDotButton(
                        punto = p,
                        seleccionado = puntosSeleccionados.contains(p),
                        onToggle = {
                            puntosSeleccionados = if (puntosSeleccionados.contains(p)) puntosSeleccionados - p else puntosSeleccionados + p
                            ttsBridge.speak("Punto $p")
                        }
                    )
                }
            }
            Spacer(Modifier.width(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(4, 5, 6).forEach { p ->
                    BrailleDotButton(
                        punto = p,
                        seleccionado = puntosSeleccionados.contains(p),
                        onToggle = {
                            puntosSeleccionados = if (puntosSeleccionados.contains(p)) puntosSeleccionados - p else puntosSeleccionados + p
                            ttsBridge.speak("Punto $p")
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = {
                    puntosSeleccionados = emptySet()
                    ttsBridge.speak("Limpiado")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) {
                Text("Limpiar", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    letraActual?.let { l ->
                        onTextoChange(textoActual + l)
                        ttsBridge.speak("Letra $l")
                        puntosSeleccionados = emptySet()
                    }
                },
                enabled = letraActual != null,
                modifier = Modifier.weight(1.3f),
                colors = ButtonDefaults.buttonColors(containerColor = colorPrimario)
            ) {
                Text("Insertar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            Button(
                onClick = {
                    onTextoChange(textoActual + " ")
                    ttsBridge.speak("Espacio")
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
            ) {
                Icon(Icons.Rounded.SpaceBar, contentDescription = "Espacio", tint = Color.White)
            }

            Button(
                onClick = {
                    if (textoActual.isNotEmpty()) {
                        onTextoChange(textoActual.dropLast(1))
                        ttsBridge.speak("Borrado")
                    }
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
            ) {
                Icon(Icons.Rounded.Backspace, contentDescription = "Borrar", tint = Color.White)
            }
        }
    }
}

@Composable
fun BrailleDotButton(
    punto: Int,
    seleccionado: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(if (seleccionado) NutriaGreen else Color(0xFF2E2E48))
            .border(2.dp, if (seleccionado) Color.White else Color(0xFF4A4A6A), CircleShape)
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$punto",
            color = if (seleccionado) Color.White else Color.LightGray,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )
    }
}

@Composable
fun CampoTextoAccesible(
    valor: String,
    onValorChange: (String) -> Unit,
    etiqueta: String,
    placeholder: String = "",
    esPassword: Boolean = false,
    a11yMode: AccessibilityMode = AccessibilityMode.NORMAL,
    colorPrimario: Color = NutriaGreen,
    modifier: Modifier = Modifier
) {
    var mostrarBraille by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(etiqueta, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
            if (a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE) {
                TextButton(
                    onClick = { mostrarBraille = !mostrarBraille },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = NutriaOrange, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(if (mostrarBraille) "Cerrar Braille" else "Teclado Braille", fontSize = 12.sp, color = NutriaOrange, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = valor,
            onValueChange = onValorChange,
            placeholder = { Text(placeholder, fontSize = 14.sp) },
            singleLine = true,
            visualTransformation = if (esPassword) PasswordVisualTransformation() else VisualTransformation.None,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colorPrimario,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )

        AnimatedVisibility(visible = mostrarBraille) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
                BrailleKeyboard(
                    textoActual = valor,
                    onTextoChange = onValorChange,
                    colorPrimario = colorPrimario
                )
            }
        }
    }
}
