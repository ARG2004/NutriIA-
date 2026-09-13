package com.example.nutriia.accesibilidad

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class InputModoCiego { TECLADO, VOZ, BRAILLE, SENAS }

// ─── Campo de texto accesible ─────────────────────────────────────────────────
@Composable
fun CampoTextoAccesible(
    valor:           String,
    onValorChange:   (String) -> Unit,
    etiqueta:        String,
    descripcionVoz:  String,
    placeholder:     String           = "",
    ttsManager:      NutriTTS?        = null,
    idioma:          IdiomaVoz        = IdiomaVoz.ESPANOL_MX,
    esCampoFecha:    Boolean          = false,
    esCampoHora:     Boolean          = false,
    colorPrimario:   Color            = Color(0xFF4CAF50),
    keyboardOptions: KeyboardOptions  = KeyboardOptions.Default,
    activo:          Boolean          = true,
    onFocus:         (() -> Unit)?    = null,
    onNext:          (() -> Unit)?    = null,
    onCommandParsed: ((String) -> Boolean)? = null,
    modifier:        Modifier         = Modifier
) {
    val context = LocalContext.current
    val haptic  = LocalHapticFeedback.current
    val a11yMode = LocalAccessibilityMode.current

    val coroutineScope = rememberCoroutineScope()
    val estadoConfirmacion = remember { mutableStateOf(false) }

    var modoEntrada  by remember(a11yMode) {
        mutableStateOf(
            if (a11yMode == AccessibilityMode.BLIND) InputModoCiego.VOZ else InputModoCiego.TECLADO
        )
    }
    var valorAlActivar by remember(activo) { mutableStateOf(if (activo) valor else "") }
    var voiceManager by remember { mutableStateOf<VoiceInputManager?>(null) }
    var tienePermiso by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED
        )
    }

    val voiceEstado by remember(voiceManager) {
        derivedStateOf { voiceManager?.estado?.value ?: VoiceInputState.IDLE }
    }

    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { otorgado ->
        tienePermiso = otorgado
        if (otorgado) {
            ttsManager?.silenciar()
            iniciarEscuchaConReintento(
                voiceManager       = voiceManager,
                idioma             = idioma,
                modoAccesible      = true,
                esCampoFecha       = esCampoFecha,
                esCampoHora        = esCampoHora,
                keyboardOptions    = keyboardOptions,
                ttsManager         = ttsManager,
                valorActual        = { valor },
                onValorChange      = onValorChange,
                onNext             = onNext,
                onCommandParsed    = onCommandParsed,
                onSwitchModo       = { modoEntrada = it },
                coroutineScope     = coroutineScope,
                estadoConfirmacion = estadoConfirmacion
            )
        } else {
            ttsManager?.hablar(Voz.VOZ_SIN_PERMISO)
        }
    }

    LaunchedEffect(Unit) {
        voiceManager = VoiceInputManager(context)
    }

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarTimePicker by remember { mutableStateOf(false) }

    // ── Espera a que voiceManager esté listo antes del auto-inicio ──────────────
    var voiceManagerListo by remember { mutableStateOf(false) }
    LaunchedEffect(voiceManager) {
        if (voiceManager != null) voiceManagerListo = true
    }

    // ── AUTO-INICIO DE VOZ ────────────────────────────────────────────────────
    LaunchedEffect(modoEntrada, voiceManagerListo, activo, a11yMode) {
        if (!activo || a11yMode != AccessibilityMode.BLIND) return@LaunchedEffect
        if (modoEntrada != InputModoCiego.VOZ) return@LaunchedEffect
        if (!voiceManagerListo) return@LaunchedEffect

        val instruccionCompleta = if (idioma == IdiomaVoz.INGLES) {
            "$descripcionVoz. If you prefer, say: change to keyboard, or: change to braille keyboard."
        } else {
            "$descripcionVoz. Si prefieres, di: cambiar a teclado, o: cambiar a teclado braille."
        }
        if (ttsManager != null) {
            ttsManager.hablarYEsperar(instruccionCompleta, margenMs = 1200L)
        } else {
            val palabras = instruccionCompleta.split(" ").size
            delay((palabras * 100L) + 1200L)
        }

        if (tienePermiso && voiceEstado == VoiceInputState.IDLE) {
            delay(400L)
            ttsManager?.silenciar()
            iniciarEscuchaConReintento(
                voiceManager       = voiceManager,
                idioma             = idioma,
                modoAccesible      = true,
                esCampoFecha       = esCampoFecha,
                esCampoHora        = esCampoHora,
                keyboardOptions    = keyboardOptions,
                ttsManager         = ttsManager,
                valorActual        = { valor },
                onValorChange      = onValorChange,
                onNext             = onNext,
                onCommandParsed    = onCommandParsed,
                onSwitchModo       = { modoEntrada = it },
                coroutineScope     = coroutineScope,
                estadoConfirmacion = estadoConfirmacion
            )
        } else if (!tienePermiso) {
            permisoLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Reactivar mic cuando hay error ───────────────────────────────────────
    val errorActual = voiceManager?.errorMsg?.value ?: ""
    val errorCodigo = voiceManager?.errorCodigo?.value ?: -1
    LaunchedEffect(errorCodigo, activo, a11yMode) {
        if (!activo || a11yMode != AccessibilityMode.BLIND) return@LaunchedEffect
        if (modoEntrada != InputModoCiego.VOZ) return@LaunchedEffect
        if (errorCodigo == -1) return@LaunchedEffect

        val errorTexto = errorActual.lowercase()
        val errorReintentable = errorTexto.contains("tiempo") ||
                errorTexto.contains("reconoc") ||
                errorTexto.contains("audio") ||
                errorTexto.contains("servidor") ||
                errorTexto.contains("timeout") ||
                errorTexto.contains("no speech") ||
                errorTexto.contains("toca") ||
                errorTexto.contains("micrófono") ||
                errorCodigo in listOf(1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13)

        if (errorReintentable && tienePermiso) {
            val avisoReintento = if (idioma == IdiomaVoz.INGLES) {
                if (descripcionVoz.isNotBlank()) {
                    "I didn't hear you. $descripcionVoz. Or say: change to keyboard."
                } else {
                    "I didn't hear you. Say your $etiqueta, or say: change to keyboard."
                }
            } else {
                if (descripcionVoz.isNotBlank()) {
                    "No te escuché. $descripcionVoz. O di: cambiar a teclado."
                } else {
                    "No te escuché. Di tu $etiqueta, o di: cambiar a teclado."
                }
            }
            if (ttsManager != null) {
                ttsManager.hablarYEsperar(avisoReintento, margenMs = 800L)
                ttsManager.silenciar()
            } else {
                delay(2000L)
            }
            if (voiceEstado == VoiceInputState.IDLE) {
                iniciarEscuchaConReintento(
                    voiceManager       = voiceManager,
                    idioma             = idioma,
                    modoAccesible      = true,
                    esCampoFecha       = esCampoFecha,
                    esCampoHora        = esCampoHora,
                    keyboardOptions    = keyboardOptions,
                    ttsManager         = ttsManager,
                    valorActual        = { valor },
                    onValorChange      = onValorChange,
                    onNext             = onNext,
                    onCommandParsed    = onCommandParsed,
                    onSwitchModo       = { modoEntrada = it },
                    coroutineScope     = coroutineScope,
                    estadoConfirmacion = estadoConfirmacion
                )
            }
        } else {
            ttsManager?.hablar(Voz.VOZ_ERROR_MIC)
        }
    }

    DisposableEffect(Unit) { onDispose { voiceManager?.liberar() } }

    LaunchedEffect(activo, modoEntrada) {
        if (!activo || modoEntrada != InputModoCiego.VOZ) {
            voiceManager?.detener()
            estadoConfirmacion.value = false
        }
    }

    if (mostrarDatePicker) {
        val cal = java.util.Calendar.getInstance()
        if (valor.isNotEmpty()) {
            try {
                if (valor.contains("-")) {
                    val parts = valor.split("-").map { it.toInt() }
                    if (parts.size == 3) {
                        cal.set(parts[0], parts[1] - 1, parts[2])
                    }
                } else if (valor.contains("/")) {
                    val parts = valor.split("/").map { it.toInt() }
                    if (parts.size == 3) {
                        cal.set(parts[2], parts[1] - 1, parts[0])
                    }
                }
            } catch (e: Exception) {}
        }
        android.app.DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val formatedDate = if (etiqueta.contains("AAAA-MM-DD") || etiqueta.contains("YYYY-MM-DD") || valor.contains("-")) {
                    String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                } else {
                    String.format(java.util.Locale.US, "%02d/%02d/%04d", dayOfMonth, month + 1, year)
                }
                onValorChange(formatedDate)
                mostrarDatePicker = false
            },
            cal.get(java.util.Calendar.YEAR),
            cal.get(java.util.Calendar.MONTH),
            cal.get(java.util.Calendar.DAY_OF_MONTH)
        ).apply {
            setOnDismissListener { mostrarDatePicker = false }
            show()
        }
    }

    if (mostrarTimePicker) {
        val cal = java.util.Calendar.getInstance()
        var initH = 8
        var initM = 0
        if (valor.isNotEmpty() && valor.contains(":")) {
            try {
                val parts = valor.split(":").map { it.toInt() }
                if (parts.size >= 2) {
                    initH = parts[0]
                    initM = parts[1]
                }
            } catch (e: Exception) {}
        }
        android.app.TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                val formatedTime = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                onValorChange(formatedTime)
                mostrarTimePicker = false
            },
            initH,
            initM,
            true
        ).apply {
            setOnDismissListener { mostrarTimePicker = false }
            show()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        val opcionesDisponibles = remember(a11yMode) {
            when (a11yMode) {
                AccessibilityMode.BLIND -> listOf(
                    Triple(InputModoCiego.TECLADO,  "Teclado",  Icons.Rounded.Keyboard),
                    Triple(InputModoCiego.VOZ,      "Voz",      Icons.Rounded.Mic),
                    Triple(InputModoCiego.BRAILLE,  "Braille",  Icons.Rounded.GridOn)
                )
                AccessibilityMode.MUTE -> listOf(
                    Triple(InputModoCiego.TECLADO,  "Teclado",  Icons.Rounded.Keyboard),
                    Triple(InputModoCiego.SENAS,    "Señas",    Icons.Rounded.BackHand)
                )
                else -> emptyList()
            }
        }

        if (a11yMode == AccessibilityMode.MUTE) {
            if (activo && opcionesDisponibles.size > 1) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opcionesDisponibles.forEach { (modo, label, icon) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (modoEntrada == modo) colorPrimario.copy(0.14f) else Color(0xFFF5F5F5))
                                .border(
                                    1.5.dp,
                                    if (modoEntrada == modo) colorPrimario else Color.LightGray.copy(0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    vibrateTap(haptic)
                                    modoEntrada = modo
                                }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(icon, null, tint = if (modoEntrada == modo) colorPrimario else Color.Gray, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.height(4.dp))
                            Text(label, fontSize = 11.sp, fontWeight = if (modoEntrada == modo) FontWeight.Bold else FontWeight.Normal, color = if (modoEntrada == modo) colorPrimario else Color.Gray)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            if (modoEntrada == InputModoCiego.TECLADO) {
                if (esCampoFecha || esCampoHora) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (esCampoFecha) mostrarDatePicker = true
                                else if (esCampoHora) mostrarTimePicker = true
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = (if (activo) colorPrimario else Color.LightGray).copy(0.05f)
                        ),
                        border = BorderStroke(1.5.dp, (if (activo) colorPrimario else Color.LightGray).copy(0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (esCampoFecha) Icons.Rounded.CalendarToday else Icons.Rounded.AccessTime,
                                contentDescription = null,
                                tint = if (activo) colorPrimario else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = etiqueta,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activo) colorPrimario else Color.Gray
                                )
                                Text(
                                    text = valor.ifEmpty { "Presiona aquí para elegir..." },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value           = valor,
                        onValueChange   = onValorChange,
                        label           = { Text(etiqueta) },
                        placeholder     = { Text(placeholder) },
                        modifier        = Modifier
                            .fillMaxWidth()
                            .clickable { if (!activo) onFocus?.invoke() },
                        readOnly        = !activo,
                        enabled         = true,
                        singleLine      = true,
                        trailingIcon    = if (activo && onNext != null && valor.isNotBlank()) {
                            {
                                IconButton(
                                    onClick = {
                                        vibrateSuccess(haptic)
                                        onNext.invoke()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowForward,
                                        contentDescription = "Siguiente campo",
                                        tint = colorPrimario
                                    )
                                }
                            }
                        } else null,
                        shape           = RoundedCornerShape(16.dp),
                        keyboardOptions = keyboardOptions.copy(
                            imeAction = if (onNext != null) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = {
                                vibrateSuccess(haptic)
                                onNext?.invoke()
                            },
                            onDone = {
                                vibrateSuccess(haptic)
                                onNext?.invoke()
                            }
                        ),
                        textStyle       = androidx.compose.ui.text.TextStyle(
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF111827)
                        ),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedTextColor          = Color(0xFF111827),
                            unfocusedTextColor        = Color(0xFF111827),
                            focusedContainerColor     = Color.White,
                            unfocusedContainerColor   = Color(0xFFFAFAFA),
                            focusedBorderColor        = colorPrimario,
                            unfocusedBorderColor      = Color(0xFF94A3B8),
                            focusedLabelColor         = colorPrimario,
                            unfocusedLabelColor       = Color(0xFF475569),
                            focusedPlaceholderColor   = Color(0xFF94A3B8),
                            unfocusedPlaceholderColor = Color(0xFF94A3B8)
                        )
                    )
                }
            } else if (modoEntrada == InputModoCiego.SENAS && activo) {
                val soloNum = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number || 
                              keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword ||
                              keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Phone ||
                              esCampoFecha
                SignLanguageCameraView(
                    textoActual   = valor,
                    onTextoChange = onValorChange,
                    colorPrimario = colorPrimario,
                    soloNumeros   = soloNum,
                    esCampoFecha  = esCampoFecha,
                    onCompletado  = onNext
                )
            }

            if (activo && onNext != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        vibrateSuccess(haptic)
                        onNext.invoke()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Confirmar y Continuar", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colorPrimario.copy(0.08f))
                    .border(1.5.dp, colorPrimario.copy(0.3f), RoundedCornerShape(16.dp))
                    .clickable {
                        vibrateTap(haptic)
                        onFocus?.invoke()
                        val valorParaHablar = if (esCampoHora && valor.isNotEmpty()) formatearHoraParaVoz(valor, idioma) else valor.ifEmpty { if (idioma == IdiomaVoz.INGLES) "empty" else "vacío" }
                        val instruccionCompleta = if (idioma == IdiomaVoz.INGLES) {
                            "$descripcionVoz. If you prefer, say: change to keyboard, or: change to braille keyboard."
                        } else {
                            "$descripcionVoz. Si prefieres, di: cambiar a teclado, o: cambiar a teclado braille."
                        }
                        val locInfo = if (idioma == IdiomaVoz.INGLES) {
                            "Field: $etiqueta. Current value: $valorParaHablar. Instruction: $instruccionCompleta"
                        } else {
                            "Campo: $etiqueta. Valor actual: $valorParaHablar. Instrucción: $instruccionCompleta"
                        }
                        ttsManager?.hablar(locInfo)
                    }
                    .semantics(mergeDescendants = true) {
                        val valorParaMostrar = if (esCampoFecha && valor.isNotEmpty()) {
                            if (valor.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                                val p = valor.split("-")
                                "${p[2]}/${p[1]}/${p[0]}"
                            } else valor
                        } else valor

                        val valorParaHablar = if (esCampoHora && valor.isNotEmpty()) {
                            formatearHoraParaVoz(valor, idioma)
                        } else if (esCampoFecha && valor.isNotEmpty()) {
                            valorParaMostrar
                        } else valor.ifEmpty { if (idioma == IdiomaVoz.INGLES) "empty" else "vacío" }

                        contentDescription = if (idioma == IdiomaVoz.INGLES) {
                            "Active field: $etiqueta. Current value: $valorParaHablar. Double tap to hear full instructions. If you prefer, say: change to keyboard, or: change to braille keyboard."
                        } else {
                            "Campo activo: $etiqueta. Valor actual: $valorParaHablar. Toca dos veces para escuchar las instrucciones completas. Si prefieres, di: cambiar a teclado, o: cambiar a teclado braille."
                        }
                    }
                    .padding(12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = colorPrimario,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = etiqueta,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorPrimario
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    val valorParaMostrar = if (esCampoFecha && valor.isNotEmpty()) {
                        if (valor.matches(Regex("""\d{4}-\d{2}-\d{2}"""))) {
                            val p = valor.split("-")
                            "${p[2]}/${p[1]}/${p[0]}"
                        } else valor
                    } else valor

                    Text(
                        text = if (valorParaMostrar.isEmpty()) {
                            placeholder.ifEmpty { if (idioma == IdiomaVoz.INGLES) "Empty" else "Vacío" }
                        } else {
                            if (idioma == IdiomaVoz.INGLES) "Value: $valorParaMostrar" else "Valor: $valorParaMostrar"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (activo && opcionesDisponibles.size > 1) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    opcionesDisponibles.forEach { (modo, label, icon) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (modoEntrada == modo) colorPrimario.copy(0.16f) else Color.White)
                                .border(
                                    if (modoEntrada == modo) 2.dp else 1.dp,
                                    if (modoEntrada == modo) colorPrimario else Color(0xFFCBD5E1),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(onClickLabel = "Cambiar a modo $label") {
                                    vibrateTap(haptic)
                                    if (modo != modoEntrada) {
                                        if (modoEntrada == InputModoCiego.VOZ) {
                                            voiceManager?.detener()
                                            estadoConfirmacion.value = false
                                        }
                                        modoEntrada = modo
                                        when (modo) {
                                            InputModoCiego.TECLADO -> ttsManager?.hablar("Modo teclado. Escribe con el teclado. Al terminar, el botón verde Continuar está abajo.")
                                            InputModoCiego.VOZ     -> {
                                                voiceManager?.limpiarError()
                                                ttsManager?.hablar("Modo voz activado. Escuchando...")
                                            }
                                            InputModoCiego.BRAILLE -> ttsManager?.hablar("Modo teclado Braille. Toca los puntos para formar cada letra. Al terminar de escribir, el botón verde Continuar está abajo a la derecha.")
                                            InputModoCiego.SENAS   -> ttsManager?.hablar("Cámara de señas activada. Haz gestos frente a la cámara frontal.")
                                        }
                                    }
                                 }
                                .semantics { contentDescription = "Modo $label. ${if (modoEntrada == modo) "Activo" else "Toca para activar"}" }
                                .padding(vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                icon, null,
                                tint = if (modoEntrada == modo) colorPrimario else Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label,
                                fontSize = 11.sp,
                                fontWeight = if (modoEntrada == modo) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (modoEntrada == modo) colorPrimario else Color(0xFF334155)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            AnimatedVisibility(visible = activo && modoEntrada == InputModoCiego.TECLADO) {
                Column {
                    OutlinedTextField(
                        value           = valor,
                        onValueChange   = { v ->
                            val resultado = when {
                                esCampoFecha -> formatearFechaDigitos(v)
                                keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number ||
                                keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword -> v.filter { it.isDigit() }
                                keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Decimal -> {
                                    val cleaned = v.replace(',', '.')
                                    if (cleaned.count { it == '.' } <= 1 && cleaned.all { it.isDigit() || it == '.' }) cleaned else valor
                                }
                                else -> v
                            }
                            onValorChange(resultado)
                        },
                        label           = { Text(etiqueta) },
                        placeholder     = { Text(placeholder) },
                        singleLine      = !esCampoFecha && !esCampoHora,
                        trailingIcon    = if (activo && onNext != null && valor.isNotBlank()) {
                            {
                                IconButton(
                                    onClick = {
                                        vibrateSuccess(haptic)
                                        ttsManager?.hablar("Avanzando al siguiente campo")
                                        onNext.invoke()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowForward,
                                        contentDescription = "Siguiente campo",
                                        tint = colorPrimario
                                    )
                                }
                            }
                        } else null,
                        modifier        = Modifier.fillMaxWidth()
                            .semantics { contentDescription = "$etiqueta. Valor actual: ${valor.ifEmpty { "vacío" }}" },
                        shape           = RoundedCornerShape(16.dp),
                        keyboardOptions = keyboardOptions.copy(
                            imeAction = if (onNext != null) androidx.compose.ui.text.input.ImeAction.Next else androidx.compose.ui.text.input.ImeAction.Done
                        ),
                        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                            onNext = {
                                vibrateSuccess(haptic)
                                ttsManager?.hablar("Avanzando al siguiente campo")
                                onNext?.invoke()
                            },
                            onDone = {
                                vibrateSuccess(haptic)
                                ttsManager?.hablar("Avanzando al siguiente campo")
                                onNext?.invoke()
                            }
                        ),
                        textStyle       = androidx.compose.ui.text.TextStyle(
                            fontSize   = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF111827)
                        ),
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedTextColor          = Color(0xFF111827),
                            unfocusedTextColor        = Color(0xFF111827),
                            focusedContainerColor     = Color.White,
                            unfocusedContainerColor   = Color(0xFFFAFAFA),
                            focusedBorderColor        = colorPrimario,
                            unfocusedBorderColor      = Color(0xFF94A3B8),
                            focusedLabelColor         = colorPrimario,
                            unfocusedLabelColor       = Color(0xFF475569),
                            focusedPlaceholderColor   = Color(0xFF94A3B8),
                            unfocusedPlaceholderColor = Color(0xFF94A3B8)
                        )
                    )

                    if (onNext != null) {
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                vibrateSuccess(haptic)
                                ttsManager?.hablar("Guardando y avanzando al siguiente campo")
                                onNext.invoke()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .semantics { contentDescription = "Confirmar y continuar al siguiente campo. Toca aquí cuando termines de escribir." }
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Confirmar y Continuar al Siguiente Campo", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = activo && modoEntrada == InputModoCiego.VOZ,
                enter   = expandVertically(), exit = shrinkVertically()
            ) {
                val esTelefono = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Phone
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (valor.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorPrimario.copy(0.08f))
                                .border(1.dp, colorPrimario.copy(0.3f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                                .semantics { contentDescription = "Texto reconocido: $valor" }
                        ) {
                            val textoMostrar = if (esTelefono && valor.all { it.isDigit() }) {
                                valor.chunked(2).joinToString(" ")
                            } else valor
                            Text(
                                text = textoMostrar,
                                fontSize = 18.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // Banner de Confirmación para Teléfono
                    if (esTelefono && estadoConfirmacion.value) {
                        Surface(
                            color = Color(0xFFFFF3E0),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFFB74D)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "¿Es correcto tu número?",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Di \"Correcto\" o toca el botón para continuar. Di \"Mal\" para corregir.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF5D4037),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            estadoConfirmacion.value = false
                                            voiceManager?.detener()
                                            val confirmMsg = if (idioma == IdiomaVoz.INGLES) "Phone number confirmed. Moving to next field." else "Número confirmado. Avanzando al siguiente campo."
                                            ttsManager?.hablar(confirmMsg)
                                            onNext?.invoke()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text("✓ Correcto", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            estadoConfirmacion.value = false
                                            onValorChange("")
                                            val borrarMsg = if (idioma == IdiomaVoz.INGLES) "Number cleared. Please say your ten-digit phone number." else "Número borrado. Por favor, di tu número de diez dígitos."
                                            voiceManager?.detener()
                                            coroutineScope.launch {
                                                if (ttsManager != null) {
                                                    ttsManager.hablarYEsperar(borrarMsg, margenMs = 400L)
                                                } else {
                                                    delay(1500L)
                                                }
                                                iniciarEscuchaConReintento(
                                                    voiceManager       = voiceManager,
                                                    idioma             = idioma,
                                                    modoAccesible      = true,
                                                    esCampoFecha       = esCampoFecha,
                                                    esCampoHora        = esCampoHora,
                                                    keyboardOptions    = keyboardOptions,
                                                    ttsManager         = ttsManager,
                                                    valorActual        = { "" },
                                                    onValorChange      = onValorChange,
                                                    onNext             = onNext,
                                                    onCommandParsed    = onCommandParsed,
                                                    onSwitchModo       = { modoEntrada = it },
                                                    coroutineScope     = coroutineScope,
                                                    estadoConfirmacion = estadoConfirmacion
                                                )
                                            }
                                        },
                                        border = BorderStroke(1.dp, Color(0xFFE53935)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f).height(40.dp)
                                    ) {
                                        Text("✗ Repetir", fontSize = 13.sp, color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    Box(
                        modifier         = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(
                                    when (voiceEstado) {
                                        VoiceInputState.LISTENING  -> Color(0xFFE53935)
                                        VoiceInputState.PROCESSING -> colorPrimario.copy(0.5f)
                                        else                       -> colorPrimario
                                    }
                                )
                                .clickable {
                                    voiceManager?.limpiarError()
                                    when (voiceEstado) {
                                        VoiceInputState.LISTENING -> {
                                            voiceManager?.detener()
                                            ttsManager?.hablar("Micrófono detenido.")
                                        }
                                        else -> {
                                            if (tienePermiso) {
                                                ttsManager?.silenciar()
                                                iniciarEscuchaConReintento(
                                                    voiceManager       = voiceManager,
                                                    idioma             = idioma,
                                                    modoAccesible      = true,
                                                    esCampoFecha       = esCampoFecha,
                                                    esCampoHora        = esCampoHora,
                                                    keyboardOptions    = keyboardOptions,
                                                    ttsManager         = ttsManager,
                                                    valorActual        = { valor },
                                                    onValorChange      = onValorChange,
                                                    onNext             = onNext,
                                                    onCommandParsed    = onCommandParsed,
                                                    onSwitchModo       = { modoEntrada = it },
                                                    coroutineScope     = coroutineScope,
                                                    estadoConfirmacion = estadoConfirmacion
                                                )
                                            } else {
                                                permisoLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                }
                                .semantics {
                                    contentDescription = when (voiceEstado) {
                                        VoiceInputState.LISTENING  -> "Escuchando. Toca dos veces para detener."
                                        VoiceInputState.PROCESSING -> "Procesando tu voz."
                                        else -> "Botón micrófono en el centro de la pantalla. Toca dos veces para hablar y escribir $etiqueta."
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (voiceEstado) {
                                    VoiceInputState.LISTENING  -> Icons.Rounded.Stop
                                    VoiceInputState.PROCESSING -> Icons.Rounded.HourglassEmpty
                                    else                       -> Icons.Rounded.Mic
                                },
                                contentDescription = null,
                                tint     = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = when {
                            voiceEstado == VoiceInputState.LISTENING && estadoConfirmacion.value -> "Escuchando... Di \"correcto\" o \"mal\""
                            voiceEstado == VoiceInputState.LISTENING && esTelefono && valor.isNotEmpty() && valor.length < 10 -> "Escuchando... Llevas ${valor.length} de 10 dígitos"
                            voiceEstado == VoiceInputState.LISTENING -> "Escuchando... habla ahora"
                            voiceEstado == VoiceInputState.PROCESSING -> "Procesando..."
                            estadoConfirmacion.value -> "Di \"correcto\" o \"mal\", o toca el botón"
                            esTelefono && valor.length in 1..9 -> "Faltan ${10 - valor.length} dígitos. Continúa hablando o toca el micrófono"
                            else -> "Toca el micrófono o espera unos segundos"
                        },
                        fontSize  = 13.sp,
                        color     = if (estadoConfirmacion.value) Color(0xFFE65100) else Color.Gray,
                        fontWeight = if (estadoConfirmacion.value) FontWeight.Medium else FontWeight.Normal,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    if (esCampoFecha) {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            color = colorPrimario.copy(0.06f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                "Di: \"quince de marzo de dos mil veintitrés\"",
                                fontSize = 11.sp, color = Color.DarkGray,
                                modifier = Modifier.padding(8.dp)
                              )
                        }
                    }

                    if (voiceManager?.errorMsg?.value?.isNotEmpty() == true) {
                        Spacer(Modifier.height(8.dp))
                        Text(voiceManager!!.errorMsg.value, fontSize = 12.sp, color = Color(0xFFE53935))
                    }

                    if (valor.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                estadoConfirmacion.value = false
                                onValorChange("")
                                val msg = if (idioma == IdiomaVoz.INGLES) "Text cleared. Speak again." else "Texto borrado. Habla de nuevo."
                                voiceManager?.detener()
                                coroutineScope.launch {
                                    if (ttsManager != null) {
                                        ttsManager.hablarYEsperar(msg, margenMs = 400L)
                                    } else {
                                        delay(1500L)
                                    }
                                    iniciarEscuchaConReintento(
                                        voiceManager       = voiceManager,
                                        idioma             = idioma,
                                        modoAccesible      = true,
                                        esCampoFecha       = esCampoFecha,
                                        esCampoHora        = esCampoHora,
                                        keyboardOptions    = keyboardOptions,
                                        ttsManager         = ttsManager,
                                        valorActual        = { "" },
                                        onValorChange      = onValorChange,
                                        onNext             = onNext,
                                        onCommandParsed    = onCommandParsed,
                                        onSwitchModo       = { modoEntrada = it },
                                        coroutineScope     = coroutineScope,
                                        estadoConfirmacion = estadoConfirmacion
                                    )
                                }
                            },
                            modifier = Modifier.semantics { contentDescription = "Borrar y repetir" }
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Borrar y repetir", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = activo && modoEntrada == InputModoCiego.BRAILLE,
                enter   = expandVertically(), exit = shrinkVertically()
            ) {
                BrailleKeyboard(
                    textoActual   = valor,
                    onTextoChange = onValorChange,
                    ttsManager    = ttsManager,
                    colorPrimario = colorPrimario,
                    onNext        = onNext
                )
            }

            AnimatedVisibility(
                visible = activo && modoEntrada == InputModoCiego.SENAS,
                enter   = expandVertically(), exit = shrinkVertically()
            ) {
                val soloNum = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number || 
                              keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword ||
                              keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Phone ||
                              esCampoFecha
                SignLanguageCameraView(
                    textoActual   = valor,
                    onTextoChange = onValorChange,
                    colorPrimario = colorPrimario,
                    soloNumeros   = soloNum,
                    esCampoFecha  = esCampoFecha,
                    onCompletado  = onNext
                )
            }
        }
    }
}

// ─── Helpers de fecha — versión robusta para Google STT en español MX ─────────

/**
 * Convierte texto de voz a formato DD/MM/AAAA.
 */
private fun parsearFecha(texto: String): String {
    val t = texto.lowercase().trim()

    val meses = mapOf(
        "enero"      to "01", "febrero"   to "02", "marzo"     to "03",
        "abril"      to "04", "mayo"      to "05", "junio"     to "06",
        "julio"      to "07", "agosto"    to "08", "septiembre" to "09",
        "setiembre"  to "09", "octubre"   to "10", "noviembre"  to "11",
        "diciembre"  to "12"
    )

    val numerosTexto = mapOf(
        "uno" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
        "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14,
        "quince" to 15, "dieciséis" to 16, "dieciseis" to 16,
        "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19,
        "veinte" to 20, "veintiuno" to 21, "veintidós" to 22, "veintidos" to 22,
        "veintitrés" to 23, "veintitres" to 23, "veinticuatro" to 24,
        "veinticinco" to 25, "veintiséis" to 26, "veintiseis" to 26,
        "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
        "treinta" to 30, "treinta y uno" to 31
    )

    fun parsearAnio(raw: String): String? {
        val r = raw.trim()
        if (r.matches(Regex("\\d{4}"))) return r
        if (r.matches(Regex("\\d{2}"))) return "20$r"

        val patronDosMil = Regex("""dos mil\s*(.*)""")
        patronDosMil.find(r)?.let { m ->
            val sufijo = m.groupValues[1].trim()
            when {
                sufijo.isEmpty()        -> return "2000"
                sufijo == "diez"        -> return "2010"
                sufijo == "once"        -> return "2011"
                sufijo == "doce"        -> return "2012"
                sufijo == "trece"       -> return "2013"
                sufijo == "catorce"     -> return "2014"
                sufijo == "quince"      -> return "2015"
                sufijo == "dieciséis"   -> return "2016"
                sufijo == "dieciseis"   -> return "2016"
                sufijo == "diecisiete"  -> return "2017"
                sufijo == "dieciocho"   -> return "2018"
                sufijo == "diecinueve"  -> return "2019"
                sufijo == "veinte"      -> return "2020"
                sufijo == "veintiuno"   -> return "2021"
                sufijo == "veintidós"   -> return "2022"
                sufijo == "veintidos"   -> return "2022"
                sufijo == "veintitrés"  -> return "2023"
                sufijo == "veintitres"  -> return "2023"
                sufijo == "veinticuatro"-> return "2024"
                sufijo == "veinticinco" -> return "2025"
                sufijo == "veintiséis"  -> return "2026"
                sufijo == "veintiseis"  -> return "2026"
                sufijo == "veintisiete" -> return "2027"
                sufijo == "veintiocho"  -> return "2028"
                sufijo == "veintinueve" -> return "2029"
                sufijo == "treinta"     -> return "2030"
                else -> {
                    val num = numerosTexto[sufijo]
                    if (num != null) return "20${num.toString().padStart(2, '0')}"
                }
            }
        }
        return null
    }

    val patronTexto = Regex(
        """(\d{1,2}|[a-záéíóúñ]+(?: y [a-záéíóúñ]+)?)\s+de\s+([a-záéíóúñ]+)\s+(?:de\s+|del\s+)?(\d{2,4}|dos mil\s+.+)"""
    )
    patronTexto.find(t)?.let { m ->
        val diaRaw  = m.groupValues[1].trim()
        val mesRaw  = m.groupValues[2].trim()
        val anioRaw = m.groupValues[3].trim()

        val dia = (diaRaw.toIntOrNull() ?: numerosTexto[diaRaw])
            ?.toString()?.padStart(2, '0') ?: return@let
        val mes  = meses[mesRaw] ?: return@let
        val anio = parsearAnio(anioRaw) ?: return@let

        return corregirFormatoFecha("$dia/$mes/$anio")
    }

    val patronNum = Regex("""(\d{1,2})[/\-\s](\d{1,2})[/\-\s](\d{2,4})""")
    patronNum.find(t)?.let { m ->
        val dia  = m.groupValues[1].padStart(2, '0')
        val mes  = m.groupValues[2].padStart(2, '0')
        val anio = m.groupValues[3].let { if (it.length == 2) "20$it" else it }
        return corregirFormatoFecha("$dia/$mes/$anio")
    }

    return corregirFormatoFecha(formatearFechaDigitos(texto))
}

private fun corregirFormatoFecha(fecha: String): String {
    val partes = fecha.split("/")
    if (partes.size != 3) return fecha
    val diaStr = partes[0]
    val mesStr = partes[1]
    val anioStr = partes[2]
    val d = diaStr.toIntOrNull() ?: 0
    val m = mesStr.toIntOrNull() ?: 0
    
    var finalDia = d
    var finalMes = m
    
    if (m > 12 && d in 1..12) {
        finalDia = m
        finalMes = d
    }
    
    finalMes = finalMes.coerceIn(1, 12)
    finalDia = finalDia.coerceIn(1, 31)
    
    return "${finalDia.toString().padStart(2, '0')}/${finalMes.toString().padStart(2, '0')}/$anioStr"
}

private fun formatearFechaDigitos(input: String): String {
    val digits = input.filter { it.isDigit() }.take(8)
    if (digits.length < 2) return input
    return buildString {
        digits.forEachIndexed { i, c ->
            if (i == 2 || i == 4) append('/')
            append(c)
        }
    }
}

// ─── Helper: inicia escucha limpiando error previo ────────────────────────────
private fun iniciarEscuchaConReintento(
    voiceManager:       VoiceInputManager?,
    idioma:             IdiomaVoz,
    modoAccesible:      Boolean   = false,
    esCampoFecha:       Boolean,
    esCampoHora:        Boolean,
    keyboardOptions:    KeyboardOptions,
    ttsManager:         NutriTTS?,
    valorActual:        () -> String,
    onValorChange:      (String) -> Unit,
    onNext:             (() -> Unit)? = null,
    onCommandParsed:    ((String) -> Boolean)? = null,
    onSwitchModo:       ((InputModoCiego) -> Unit)? = null,
    coroutineScope:     kotlinx.coroutines.CoroutineScope? = null,
    estadoConfirmacion: androidx.compose.runtime.MutableState<Boolean>? = null
) {
    voiceManager?.errorMsg?.value    = ""
    voiceManager?.errorCodigo?.value = -1
    voiceManager?.escuchar(idioma, modoAccesible) { texto, isFinal ->
        val command = texto.lowercase(java.util.Locale.getDefault()).trim()
        
        // ── Intercepción de Comandos para Cambiar de Modo en Blind Mode ──────────────
        val esCmdBraille = command == "cambiar a teclado braile" || command == "cambiar a teclado braille" ||
                           command.contains("teclado braile") || command.contains("teclado braille") ||
                           command == "braile" || command == "braille"
                           
        val esCmdTeclado = command == "cambiar a teclado" || command == "cambiar a modo teclado" ||
                           command == "teclado" ||
                           (command.contains("teclado") && !command.contains("braile") && !command.contains("braille"))

        if (esCmdBraille) {
            if (isFinal) {
                ttsManager?.hablar("Cambiando a teclado braille")
                voiceManager?.detener()
                onSwitchModo?.invoke(InputModoCiego.BRAILLE)
            }
            return@escuchar
        }
        
        if (esCmdTeclado) {
            if (isFinal) {
                ttsManager?.hablar("Cambiando a teclado")
                voiceManager?.detener()
                onSwitchModo?.invoke(InputModoCiego.TECLADO)
            }
            return@escuchar
        }

        if (onCommandParsed != null && onCommandParsed.invoke(command)) {
            if (isFinal) {
                iniciarEscuchaConReintento(
                    voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                    keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                    onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                )
            }
            return@escuchar
        }

        // NO escribir nada si el texto detectado parece el inicio de un comando de cambio
        val pareceComando = command.startsWith("cambiar") || command.startsWith("modo") || 
                          command.startsWith("change") || command.startsWith("switch") ||
                          command.contains("teclado") || command.contains("keyboard") || 
                          command.contains("braille") || command.contains("braile")

        if (pareceComando && !isFinal) return@escuchar

        val esTelefono = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Phone

        // ── MANEJO ESPECIAL PARA TELÉFONO EN ESTADO DE CONFIRMACIÓN ──────────
        if (esTelefono && estadoConfirmacion?.value == true) {
            val esAfirmativo = command.contains("correcto") || command.contains("correct") ||
                               command == "si" || command == "sí" || command == "yes" ||
                               command.contains("está bien") || command.contains("esta bien") ||
                               command.contains("es correcto") || command == "bien" || command == "ok" ||
                               command == "continuar" || command == "siguiente" || command == "next"

            val esNegativo = command.contains("mal") || command.contains("incorrecto") ||
                             command.contains("repetir") || command.contains("cambiar") ||
                             command.contains("borrar") || command == "no" ||
                             command.contains("wrong") || command.contains("retry") ||
                             command.contains("otra vez") || command.contains("de nuevo") ||
                             command.contains("bad") || command.contains("clear")

            if (esAfirmativo) {
                if (isFinal) {
                    estadoConfirmacion.value = false
                    voiceManager?.detener()
                    val confirmMsg = if (idioma == IdiomaVoz.INGLES) "Phone number confirmed. Moving to next field." else "Número confirmado. Avanzando al siguiente campo."
                    ttsManager?.hablar(confirmMsg)
                    onNext?.invoke()
                }
                return@escuchar
            } else if (esNegativo) {
                if (isFinal) {
                    estadoConfirmacion.value = false
                    onValorChange("")
                    val borrarMsg = if (idioma == IdiomaVoz.INGLES) "Number cleared. Please say your ten-digit phone number." else "Número borrado. Por favor, di tu número de diez dígitos."
                    voiceManager?.detener()
                    coroutineScope?.launch {
                        if (ttsManager != null) {
                            ttsManager.hablarYEsperar(borrarMsg, margenMs = 400L)
                        } else {
                            kotlinx.coroutines.delay(2000L)
                        }
                        iniciarEscuchaConReintento(
                            voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                            keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                            onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                        )
                    }
                }
                return@escuchar
            } else {
                val nuevosDigitos = parsearTelefonoVoz(texto).filter { it.isDigit() }
                if (nuevosDigitos.length >= 7) {
                    // El usuario dictó un número completamente nuevo durante la confirmación
                    estadoConfirmacion.value = false
                } else if (isFinal) {
                    val numActual = valorActual().filter { it.isDigit() }
                    val repregunta = if (idioma == IdiomaVoz.INGLES) {
                        "Please confirm. Your phone number is ${numActual.chunked(2).joinToString(" ")}. Say 'correct' to continue, or 'retry' to start over."
                    } else {
                        "Por favor confirma. Tu número es ${numActual.chunked(2).joinToString(" ")}. Di 'correcto' para continuar, o 'repetir' para volver a ingresarlo."
                    }
                    voiceManager?.detener()
                    coroutineScope?.launch {
                        if (ttsManager != null) {
                            ttsManager.hablarYEsperar(repregunta, margenMs = 400L)
                        } else {
                            kotlinx.coroutines.delay(2500L)
                        }
                        iniciarEscuchaConReintento(
                            voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                            keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                            onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                        )
                    }
                    return@escuchar
                } else {
                    return@escuchar
                }
            }
        }

        // ── MANEJO DE COMANDO BORRAR / REPETIR EN CUALQUIER MOMENTO ──────────
        val esCmdBorrar = command == "borrar" || command == "repetir" || command == "limpiar" ||
                          command.contains("borrar todo") || command.contains("empezar de nuevo") ||
                          command.contains("reiniciar") || command.contains("clear") || command.contains("start over")

        if (esCmdBorrar) {
            if (isFinal) {
                estadoConfirmacion?.value = false
                onValorChange("")
                val msgBorrado = if (idioma == IdiomaVoz.INGLES) "Cleared. Say it again." else "Texto borrado. Habla de nuevo."
                voiceManager?.detener()
                coroutineScope?.launch {
                    if (ttsManager != null) {
                        ttsManager.hablarYEsperar(msgBorrado, margenMs = 400L)
                    } else {
                        kotlinx.coroutines.delay(1500L)
                    }
                    iniciarEscuchaConReintento(
                        voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                        keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                        onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                    )
                }
            }
            return@escuchar
        }

        val isSkip = command.contains("no lo tengo") || command.contains("no tengo") || command == "no" || 
                     command.contains("don't have it") || command.contains("dont have it") || command == "skip" || 
                     command == "omitir" || command.contains("sin notas") || command == "ninguna"
        val isSend = command.contains("enviar") || command.contains("send") || command.contains("terminar") || command.contains("finalizar")
        
        if (command == "siguiente" || command == "continuar" || command == "next" || command == "continue" || command == "ok" || 
            command.contains("guardar") || command.contains("save") || command == "listo" || command == "ready" || isSkip || isSend) {
            
            if (isFinal) {
                if (isSkip) {
                    onValorChange("")
                }
                if (onNext != null) {
                    onNext.invoke()
                } else {
                    val resultado = if (isSkip) "" else sanitizarResultadoVoz(texto, esCampoFecha, esCampoHora, keyboardOptions, ttsManager, idioma)
                    onValorChange(resultado)
                }
            }
            return@escuchar
        }

        // ── MANEJO DE ENTRADA POR VOZ SEGÚN TIPO DE CAMPO ────────────────────
        if (esTelefono) {
            val digitosPrevios = valorActual().filter { it.isDigit() }
            val nuevosDigitos = parsearTelefonoVoz(texto).filter { it.isDigit() }

            if (nuevosDigitos.isNotEmpty()) {
                val totalDigitos = if (nuevosDigitos.length >= 10 || digitosPrevios.isEmpty()) {
                    nuevosDigitos.take(10)
                } else if (nuevosDigitos.startsWith(digitosPrevios)) {
                    nuevosDigitos.take(10)
                } else {
                    (digitosPrevios + nuevosDigitos).take(10)
                }

                onValorChange(totalDigitos)

                if (isFinal) {
                    if (totalDigitos.length >= 10) {
                        // 10 DÍGITOS COMPLETOS -> PASAR A CONFIRMACIÓN
                        estadoConfirmacion?.value = true
                        voiceManager?.detener()
                        val confirmacionMsg = if (idioma == IdiomaVoz.INGLES) {
                            "Your phone number is: ${totalDigitos.chunked(2).joinToString(" ")}. Is this correct? Say 'correct' or 'yes' to continue, or say 'wrong' or 'retry' to start over."
                        } else {
                            "Tu número es: ${totalDigitos.chunked(2).joinToString(" ")}. ¿Está correcto? Di: 'correcto' o 'sí' para continuar, o di: 'mal', 'incorrecto' o 'repetir' para volver a ingresarlo."
                        }
                        coroutineScope?.launch {
                            if (ttsManager != null) {
                                ttsManager.hablarYEsperar(confirmacionMsg, margenMs = 400L)
                            } else {
                                val palabras = confirmacionMsg.split(" ").size
                                kotlinx.coroutines.delay((palabras * 120L) + 500L)
                            }
                            iniciarEscuchaConReintento(
                                voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                                keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                                onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                            )
                        }
                    } else {
                        // MENOS DE 10 DÍGITOS -> NOTIFICAR Y CONTINUAR ESCUCHANDO AUTOMÁTICAMENTE
                        val faltan = 10 - totalDigitos.length
                        val mensajeParcial = if (idioma == IdiomaVoz.INGLES) {
                            "You have ${totalDigitos.length} digits: ${totalDigitos.chunked(2).joinToString(" ")}. $faltan digits remaining. Continue saying your number."
                        } else {
                            "Llevas ${totalDigitos.length} dígitos: ${totalDigitos.chunked(2).joinToString(" ")}. Faltan $faltan dígitos. Continúa diciendo tu número."
                        }
                        voiceManager?.detener()
                        coroutineScope?.launch {
                            if (ttsManager != null) {
                                ttsManager.hablarYEsperar(mensajeParcial, margenMs = 400L)
                            } else {
                                val palabras = mensajeParcial.split(" ").size
                                kotlinx.coroutines.delay((palabras * 120L) + 500L)
                            }
                            iniciarEscuchaConReintento(
                                voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                                keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                                onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                            )
                        }
                    }
                }
            }
            return@escuchar
        }

        val esNumerico = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number ||
                         keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Decimal ||
                         keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword

        if (esNumerico) {
            val resultado = sanitizarResultadoVoz(texto, esCampoFecha, esCampoHora, keyboardOptions, ttsManager, idioma)
            if (resultado.isNotBlank()) {
                onValorChange(resultado)
                if (isFinal && onNext != null) {
                    voiceManager?.detener()
                    onNext.invoke()
                }
            } else if (isFinal) {
                // El usuario habló pero no dijo un número válido (ej. "talla mediano")
                val msgErrorNumero = if (idioma == IdiomaVoz.INGLES) {
                    "Please say a valid number, for example 68."
                } else {
                    "La medida debe ser un número. Por ejemplo sesenta y ocho. Intenta de nuevo."
                }
                voiceManager?.detener()
                coroutineScope?.launch {
                    if (ttsManager != null) {
                        ttsManager.hablarYEsperar(msgErrorNumero, margenMs = 400L)
                    } else {
                        kotlinx.coroutines.delay(2000L)
                    }
                    iniciarEscuchaConReintento(
                        voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                        keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                        onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                    )
                }
            }
        } else {
            // Otros campos (Nombre, Email, Password, Fecha, etc.)
            val resultado = sanitizarResultadoVoz(texto, esCampoFecha, esCampoHora, keyboardOptions, ttsManager, idioma)
            if (resultado.isNotBlank()) {
                onValorChange(resultado)
            }
            if (isFinal && resultado.isNotBlank() && onNext != null) {
                val esPassword = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Password || 
                                 keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword
                val esCorreo   = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Email

                if (esPassword) {
                    if (resultado.length >= 6) {
                        voiceManager?.detener()
                        onNext.invoke()
                    } else {
                        val msgClave = if (idioma == IdiomaVoz.INGLES) "Password must be at least 6 characters. Please try again." else "La clave debe tener al menos 6 caracteres. Intenta de nuevo."
                        voiceManager?.detener()
                        coroutineScope?.launch {
                            if (ttsManager != null) {
                                ttsManager.hablarYEsperar(msgClave, margenMs = 400L)
                            } else {
                                kotlinx.coroutines.delay(2000L)
                            }
                            iniciarEscuchaConReintento(
                                voiceManager, idioma, modoAccesible, esCampoFecha, esCampoHora,
                                keyboardOptions, ttsManager, valorActual, onValorChange, onNext,
                                onCommandParsed, onSwitchModo, coroutineScope, estadoConfirmacion
                            )
                        }
                    }
                } else if (esCorreo) {
                    if (resultado.contains("@") && resultado.contains(".")) {
                        voiceManager?.detener()
                        onNext.invoke()
                    }
                } else {
                    voiceManager?.detener()
                    onNext.invoke()
                }
            }
        }
    }
}

private val FRASES_ECO_SISTEMA = listOf(
    "no te escuché",
    "no te escuche",
    "di tu correo",
    "di tu nombre",
    "di tu teléfono",
    "di tu telefono",
    "di tu",
    "por ejemplo",
    "te escucho, habla cuando quieras",
    "te escucho habla cuando quieras",
    "habla cuando quieras",
    "hablacuandoquieras",
    "te escucho",
    "tees como",
    "escuchando",
    "speak whenever you're ready",
    "i'm listening",
    "listening"
)

private fun limpiarEcosDelSistema(texto: String): String {
    var limpio = texto.trim()
    for (eco in FRASES_ECO_SISTEMA) {
        if (limpio.equals(eco, ignoreCase = true)) {
            return ""
        }
        limpio = limpio.replace(eco, "", ignoreCase = true).trim()
    }
    return limpio
}

private fun parsearTelefonoVoz(texto: String): String {
    val textoSinEco = limpiarEcosDelSistema(texto)
    if (textoSinEco.isBlank()) return ""

    var t = textoSinEco.lowercase()
        .replace("-", " ")
        .replace(".", " ")
        .replace(",", " ")
        .trim()

    // Eliminar palabras y prefijos comunes
    val prefijos = listOf(
        "mi número es", "mi numero es", "mi teléfono es", "mi telefono es",
        "es el", "es", "número", "numero", "teléfono", "telefono"
    )
    for (p in prefijos) {
        if (t.startsWith(p)) {
            t = t.substring(p.length).trim()
        }
    }

    // Manejo de "doble X" y "triple X"
    val repeticiones = listOf(
        "doble cero" to "00", "doble uno" to "11", "doble dos" to "22", "doble tres" to "33",
        "doble cuatro" to "44", "doble cinco" to "55", "doble seis" to "66", "doble siete" to "77",
        "doble ocho" to "88", "doble nueve" to "99",
        "triple cero" to "000", "triple uno" to "111", "triple dos" to "222", "triple tres" to "333",
        "triple cuatro" to "444", "triple cinco" to "555", "triple seis" to "666", "triple siete" to "777",
        "triple ocho" to "888", "triple nueve" to "999"
    )
    for ((frase, rep) in repeticiones) {
        t = t.replace(frase, rep)
    }

    // Manejo de decenas compuestas con "y" (ej: "treinta y cinco" -> "35", "cuarenta y dos" -> "42")
    val decenasCompuestas = listOf(
        "veinte y" to 20, "treinta y" to 30, "cuarenta y" to 40, "cincuenta y" to 50,
        "sesenta y" to 60, "setenta y" to 70, "ochenta y" to 80, "noventa y" to 90
    )
    val unidadesMap = mapOf(
        "uno" to 1, "un" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4,
        "cinco" to 5, "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9
    )
    for ((decStr, decVal) in decenasCompuestas) {
        for ((uniStr, uniVal) in unidadesMap) {
            val compuesto = "$decStr $uniStr"
            val valorNum = (decVal + uniVal).toString()
            t = t.replace(compuesto, valorNum)
        }
    }

    val mapaPalabras = mapOf(
        "cero" to "0", "zero" to "0",
        "uno" to "1", "un" to "1", "una" to "1", "one" to "1",
        "dos" to "2", "two" to "2",
        "tres" to "3", "three" to "3",
        "cuatro" to "4", "four" to "4",
        "cinco" to "5", "five" to "5",
        "seis" to "6", "six" to "6",
        "siete" to "7", "seven" to "7",
        "ocho" to "8", "eight" to "8",
        "nueve" to "9", "nine" to "9",
        "diez" to "10", "ten" to "10",
        "once" to "11", "eleven" to "11",
        "doce" to "12", "twelve" to "12",
        "trece" to "13", "thirteen" to "13",
        "catorce" to "14", "fourteen" to "14",
        "quince" to "15", "fifteen" to "15",
        "dieciséis" to "16", "dieciseis" to "16", "sixteen" to "16",
        "diecisiete" to "17", "seventeen" to "17",
        "dieciocho" to "18", "eighteen" to "18",
        "diecinueve" to "19", "nineteen" to "19",
        "veinte" to "20", "twenty" to "20",
        "veintiuno" to "21", "veintiún" to "21", "twenty-one" to "21", "twenty one" to "21",
        "veintidós" to "22", "veintidos" to "22", "twenty-two" to "22", "twenty two" to "22",
        "veintitrés" to "23", "veintitres" to "23", "twenty-three" to "23", "twenty three" to "23",
        "veinticuatro" to "24", "twenty-four" to "24", "twenty four" to "24",
        "veinticinco" to "25", "twenty-five" to "25", "twenty five" to "25",
        "veintiséis" to "26", "veintiseis" to "26", "twenty-six" to "26", "twenty six" to "26",
        "veintisiete" to "27", "twenty-seven" to "27", "twenty seven" to "27",
        "veintiocho" to "28", "twenty-eight" to "28", "twenty eight" to "28",
        "veintinueve" to "29", "twenty-nine" to "29", "twenty nine" to "29",
        "treinta" to "30", "thirty" to "30",
        "cuarenta" to "40", "forty" to "40",
        "cincuenta" to "50", "fifty" to "50",
        "sesenta" to "60", "sixty" to "60",
        "setenta" to "70", "seventy" to "70",
        "ochenta" to "80", "eighty" to "80",
        "noventa" to "90", "ninety" to "90",
        "cien" to "100", "ciento" to "100"
    )

    val tokens = t.split(Regex("""\s+"""))
    val sb = StringBuilder()
    for (tok in tokens) {
        val cleanTok = tok.trim()
        if (cleanTok.isEmpty() || cleanTok == "y" || cleanTok == "and") continue
        
        val digitsOnly = cleanTok.filter { it.isDigit() }
        if (digitsOnly.isNotEmpty()) {
            sb.append(digitsOnly)
        } else if (mapaPalabras.containsKey(cleanTok)) {
            sb.append(mapaPalabras[cleanTok])
        }
    }
    return sb.toString()
}

private fun sanitizarResultadoVoz(
    texto:           String,
    esCampoFecha:    Boolean,
    esCampoHora:     Boolean,
    keyboardOptions: KeyboardOptions,
    ttsManager:      NutriTTS? = null,
    idioma:          IdiomaVoz = IdiomaVoz.ESPANOL_MX
): String {
    val textoLimpio = limpiarEcosDelSistema(texto)
    if (textoLimpio.isBlank()) return ""

    var resultado = when {
        esCampoFecha -> parsearFecha(textoLimpio)
        esCampoHora  -> parsearHora(textoLimpio)
        keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Phone -> parsearTelefonoVoz(textoLimpio)
        keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Decimal -> parsearNumeroDecimal(textoLimpio)
        keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number ||
        keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword -> parsearNumeroEntero(textoLimpio)
        else         -> textoLimpio
    }
    val esNumericoOClaveOCorreo = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number ||
                                 keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Phone ||
                                 keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword ||
                                 keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Decimal ||
                                 keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Password ||
                                 keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Email
    if (esNumericoOClaveOCorreo) {
        resultado = resultado.replace(" ", "")
    }
    return resultado
}

private fun parsearNumeroDecimal(texto: String): String {
    val t = texto.lowercase().trim()
        .replace("kilos", "").replace("kilo", "").replace("kg", "")
        .replace("centímetros", "").replace("centimetros", "").replace("cms", "").replace("cm", "")
        .replace("gramos", "").replace("gramo", "").replace("gr", "")
        .replace("mililitros", "").replace("mililitro", "").replace("ml", "")
        .replace("minutos", "").replace("minuto", "").replace("min", "")
        .trim()
    
    // Casos con metros para talla / estatura (e.g. "un metro diez" -> 110, "1 metro 15" -> 115, "un metro cinco" -> 105, "un metro" -> 100)
    if (t.contains("metro") || t.contains(" metros") || t.contains(" meter")) {
        val tMetro = t.replace("un metro", "100").replace("1 metro", "100")
                      .replace("dos metros", "200").replace("2 metros", "200")
                      .replace("one meter", "100").replace("1 meter", "100")
                      .replace("metro", "100").replace("metros", "200")
                      .replace(" y ", " ").replace(" con ", " ")
        val palabras = tMetro.split(Regex("\\s+"))
        var total = 0
        var found = false
        for (p in palabras) {
            val n = p.toIntOrNull() ?: parsearNumeroEspanol(p)
            if (n > 0 || p == "0" || p == "cero") {
                if (n in 1..9 && total >= 100 && palabras.size > 1 && palabras.last() == p && !t.contains("cincuenta") && !t.contains("cinco")) {
                    total += n * 10
                } else {
                    total += n
                }
                found = true
            }
        }
        if (found && total > 0) return total.toString()
    }

    // Casos con "medio" / "media" (ej. "siete y medio", "7 y medio", "siete kilos y medio" -> 7.5)
    if (t.contains("medio") || t.contains("media") || t.contains("and a half") || t.contains("half")) {
        val tSinMedio = t.replace("y medio", "").replace("con medio", "").replace("y media", "").replace("con media", "")
                         .replace("and a half", "").replace("half", "").trim()
        val entero = tSinMedio.filter { it.isDigit() }.ifEmpty {
            val n = parsearNumeroEspanol(tSinMedio)
            if (n > 0) n.toString() else "0"
        }
        return "$entero.5"
    }

    // Casos decimales con dígitos directos: "7.5", "7,5", "68.2"
    val regexDecimal = Regex("""(\d+)[,\.](\d+)""")
    regexDecimal.find(t)?.let { m ->
        return "${m.groupValues[1]}.${m.groupValues[2]}"
    }
    
    // Casos con palabras "punto", "coma", "point", "dot", " con "
    if (t.contains("punto") || t.contains("coma") || t.contains("point") || t.contains("dot") || t.contains(" con ")) {
        val partes = t.split(Regex("""\s+(?:punto|coma|point|dot|con)\s+"""))
        if (partes.size >= 2) {
            val enteroStr = partes[0].filter { it.isDigit() }.ifEmpty {
                val n = parsearNumeroEspanol(partes[0])
                if (n > 0 || partes[0].trim() == "cero") n.toString() else ""
            }
            val decStr = partes[1].filter { it.isDigit() }.ifEmpty {
                val n = parsearNumeroEspanol(partes[1])
                if (n > 0 || partes[1].trim() == "cero") n.toString() else ""
            }
            if (enteroStr.isNotBlank() && decStr.isNotBlank()) {
                val decNorm = if (decStr.length == 3 && decStr.endsWith("00")) decStr.take(1) else decStr
                return "$enteroStr.$decNorm"
            }
        }
    }
    
    // Dígitos enteros presentes en el texto (ej. "68", "75", "110", "pesa 8", "mide 70")
    val regexEntero = Regex("""\d+""")
    regexEntero.find(t)?.let { m ->
        return m.value
    }
    
    // Número expresado completamente en palabras (ej. "sesenta y ocho", "ciento diez", "setenta y cinco")
    val num = parsearNumeroEspanol(t)
    if (num > 0 || t == "cero" || t == "zero") return num.toString()
    
    // Si no contiene ningún número (ej. "talla mediano", "mediano", "grande", "chico", "no sé") -> RETORNAR VACÍO
    return ""
}

private fun parsearNumeroEntero(texto: String): String {
    val t = texto.lowercase().trim()
        .replace("kilos", "").replace("kilo", "").replace("kg", "")
        .replace("centímetros", "").replace("centimetros", "").replace("cms", "").replace("cm", "")
        .replace("minutos", "").replace("minuto", "").replace("min", "")
        .replace("mililitros", "").replace("mililitro", "").replace("ml", "")
        .trim()
    
    val digitos = t.filter { it.isDigit() }
    if (digitos.isNotBlank()) return digitos
    
    val num = parsearNumeroEspanol(t)
    if (num > 0 || t == "cero" || t == "zero") return num.toString()
    
    return ""
}

private fun parsearHora(texto: String): String {
    val t = texto.lowercase().trim()
    val tNormalizado = t.replace(".", "").replace(" ", "")
    val esTarde = t.contains("tarde") || t.contains("noche") || tNormalizado.contains("pm")
    val esManana = t.contains("mañana") || t.contains("madrugada") || tNormalizado.contains("am")

    var tLimpio = t.replace("de la tarde", "")
        .replace("de la noche", "")
        .replace("de la mañana", "")
        .replace("de la madrugada", "")
        .replace("del día", "")
        .replace("del dia", "")
        .trim()
        
    val ampms = listOf("a.m.", "p.m.", "a. m.", "p. m.", "am", "pm", "a m", "p m")
    for (ampm in ampms) {
        tLimpio = tLimpio.replace(ampm, "")
    }
    tLimpio = tLimpio.trim()

    val regexDigitos = Regex("""(\d{1,2})[:\-\s\.]+(\d{1,2})""")
    regexDigitos.find(tLimpio)?.let { m ->
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        return ajustarFormatoHora(h, min, esTarde, esManana)
    }

    if (tLimpio.matches(Regex("""\d{1,2}"""))) {
        val h = tLimpio.toInt()
        return ajustarFormatoHora(h, 0, esTarde, esManana)
    }

    val separador = when {
        tLimpio.contains(" y ") -> " y "
        tLimpio.contains(" con ") -> " con "
        else -> " "
    }
    val partes = tLimpio.split(separador)
    if (partes.isNotEmpty() && partes[0].trim().isNotBlank()) {
        val horaRaw = partes[0].trim()
        val h = parsearNumeroEspanol(horaRaw)
        
        if (h > 0 || horaRaw == "doce" || horaRaw == "cero" || horaRaw.toIntOrNull() != null) {
            val min = if (partes.size > 1) {
                val minRaw = partes.subList(1, partes.size).joinToString(separador).trim()
                parsearNumeroEspanol(minRaw)
            } else 0
            return ajustarFormatoHora(h, min, esTarde, esManana)
        }
    }

    val digitos = t.filter { it.isDigit() }
    if (digitos.length >= 3) {
        val h = digitos.dropLast(2).toIntOrNull() ?: 0
        val min = digitos.takeLast(2).toIntOrNull() ?: 0
        return ajustarFormatoHora(h, min, esTarde, esManana)
    } else if (digitos.isNotEmpty()) {
        val h = digitos.toIntOrNull() ?: 0
        return ajustarFormatoHora(h, 0, esTarde, esManana)
    }

    return tLimpio
}

private fun parsearNumeroEspanol(texto: String): Int {
    val t = texto.lowercase().trim()
    t.toIntOrNull()?.let { return it }
    
    if (t == "media" || t == "medio") return 30
    if (t == "cuarto" || t == "un cuarto") return 15
    
    val unidades = mapOf(
        "cero" to 0, "uno" to 1, "un" to 1, "una" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
        "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15,
        "dieciséis" to 16, "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19,
        "veinte" to 20, "veintiuno" to 21, "veintiún" to 21, "veintiun" to 21, "veintiuna" to 21,
        "veintidos" to 22, "veintidós" to 22,
        "veintitrés" to 23, "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25,
        "veintiséis" to 26, "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29,
        "zero" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4, "five" to 5,
        "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
        "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19
    )
    
    val decenas = mapOf(
        "diez" to 10, "veinte" to 20, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50,
        "sesenta" to 60, "setenta" to 70, "ochenta" to 80, "noventa" to 90,
        "twenty" to 20, "thirty" to 30, "forty" to 40, "fifty" to 50,
        "sixty" to 60, "seventy" to 70, "eighty" to 80, "ninety" to 90
    )

    val centenas = mapOf(
        "cien" to 100, "ciento" to 100, "doscientos" to 200, "trescientos" to 300,
        "cuatrocientos" to 400, "quinientos" to 500, "seiscientos" to 600,
        "setecientos" to 700, "ochocientos" to 800, "novecientos" to 900,
        "one hundred" to 100, "hundred" to 100
    )
    
    if (unidades.containsKey(t)) return unidades[t]!!
    if (decenas.containsKey(t)) return decenas[t]!!
    if (centenas.containsKey(t)) return centenas[t]!!
    
    var acum = 0
    val limpia = t.replace(" y ", " ").replace("-", " ")
    val palabras = limpia.split(Regex("\\s+"))
    var huboNumero = false

    for (p in palabras) {
        if (centenas.containsKey(p)) {
            acum += centenas[p]!!
            huboNumero = true
        } else if (decenas.containsKey(p)) {
            acum += decenas[p]!!
            huboNumero = true
        } else if (unidades.containsKey(p)) {
            acum += unidades[p]!!
            huboNumero = true
        } else if (p.toIntOrNull() != null) {
            acum += p.toInt()
            huboNumero = true
        }
    }
    return if (huboNumero) acum else 0
}

private fun ajustarFormatoHora(h: Int, min: Int, esTarde: Boolean, esManana: Boolean): String {
    var finalHora = h
    val finalMin = min.coerceIn(0, 59)

    if (esTarde && finalHora in 1..11) {
        finalHora += 12
    } else if (esManana && finalHora == 12) {
        finalHora = 0
    }

    finalHora = finalHora.coerceIn(0, 23)
    return "${finalHora.toString().padStart(2, '0')}:${finalMin.toString().padStart(2, '0')}"
}

private fun formatearHoraParaVoz(horaStr: String, idioma: IdiomaVoz): String {
    if (!horaStr.matches(Regex("""\d{2}:\d{2}"""))) return horaStr
    val partes = horaStr.split(":")
    val h = partes[0].toIntOrNull() ?: return horaStr
    val m = partes[1].toIntOrNull() ?: return horaStr

    val esIngles = idioma == IdiomaVoz.INGLES
    
    val periodo = when {
        h == 0 -> if (esIngles) "at night" else "de la madrugada"
        h < 6 -> if (esIngles) "in the early morning" else "de la madrugada"
        h < 12 -> if (esIngles) "in the morning" else "de la mañana"
        h == 12 -> if (esIngles) "midday" else "del mediodía"
        h < 19 -> if (esIngles) "in the afternoon" else "de la tarde"
        else -> if (esIngles) "at night" else "de la noche"
    }

    val h12 = when {
        h == 0 -> 12
        h > 12 -> h - 12
        else -> h
    }

    val minTexto = when (m) {
        0 -> if (esIngles) "o'clock" else ""
        15 -> if (esIngles) "fifteen" else "quince"
        30 -> if (esIngles) "thirty" else "treinta"
        45 -> if (esIngles) "forty-five" else "cuarenta y cinco"
        else -> if (esIngles) "$m" else "$m"
    }

    val union = if (m == 0) "" else if (esIngles) " " else " y "

    if (esIngles) {
        return "$horaStr, that is, $h12$union$minTexto $periodo"
    } else {
        return "$horaStr, es decir, $h12$union$minTexto $periodo"
    }
}