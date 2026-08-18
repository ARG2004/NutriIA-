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
            ttsManager?.hablar(Voz.VOZ_ESCUCHANDO)
            iniciarEscuchaConReintento(
                voiceManager  = voiceManager,
                idioma        = idioma,
                modoAccesible = true,
                esCampoFecha  = esCampoFecha,
                esCampoHora   = esCampoHora,
                keyboardOptions = keyboardOptions,
                ttsManager    = ttsManager,
                onValorChange = onValorChange,
                onNext        = onNext,
                onCommandParsed = onCommandParsed,
                onSwitchModo    = { modoEntrada = it }
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
    LaunchedEffect(modoEntrada, voiceManagerListo, activo) {
        if (!activo) return@LaunchedEffect
        if (modoEntrada != InputModoCiego.VOZ) return@LaunchedEffect
        if (!voiceManagerListo) return@LaunchedEffect

        val instruccionCompleta = if (idioma == IdiomaVoz.INGLES) {
            "$descripcionVoz. If you prefer, say: change to keyboard, or: change to braille keyboard."
        } else {
            "$descripcionVoz. Si prefieres, di: cambiar a teclado, o: cambiar a teclado braille."
        }
        if (ttsManager != null) {
            ttsManager.hablarYEsperar(instruccionCompleta, margenMs = 800L)
        } else {
            val palabras = instruccionCompleta.split(" ").size
            delay((palabras * 90L) + 1000L)
        }

        if (tienePermiso && voiceEstado == VoiceInputState.IDLE) {
            if (ttsManager != null) {
                ttsManager.hablarYEsperar(Voz.VOZ_ESCUCHANDO, margenMs = 400L)
            } else {
                delay(1200L)
            }
            iniciarEscuchaConReintento(
                voiceManager  = voiceManager,
                idioma        = idioma,
                modoAccesible = true,
                esCampoFecha  = esCampoFecha,
                esCampoHora   = esCampoHora,
                keyboardOptions = keyboardOptions,
                ttsManager    = ttsManager,
                onValorChange = onValorChange,
                onNext        = onNext,
                onCommandParsed = onCommandParsed,
                onSwitchModo    = { modoEntrada = it }
            )
        } else if (!tienePermiso) {
            permisoLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // ── Reactivar mic cuando hay error ───────────────────────────────────────
    val errorActual = voiceManager?.errorMsg?.value ?: ""
    val errorCodigo = voiceManager?.errorCodigo?.value ?: -1
    LaunchedEffect(errorCodigo, activo) {
        if (!activo) return@LaunchedEffect
        if (errorActual.isEmpty() || errorCodigo == -1) return@LaunchedEffect
        if (modoEntrada != InputModoCiego.VOZ) return@LaunchedEffect

        val esRecuperable = voiceManager?.esErrorRecuperable() == true

        if (esRecuperable && tienePermiso) {
            delay(600L)
            if (ttsManager != null) {
                ttsManager.hablarYEsperar("Habla de nuevo.", margenMs = 500L)
            } else {
                delay(2000L)
            }
            iniciarEscuchaConReintento(
                voiceManager  = voiceManager,
                idioma        = idioma,
                modoAccesible = true,
                esCampoFecha  = esCampoFecha,
                esCampoHora   = esCampoHora,
                keyboardOptions = keyboardOptions,
                ttsManager    = ttsManager,
                onValorChange = onValorChange,
                onNext        = onNext,
                onCommandParsed = onCommandParsed,
                onSwitchModo    = { modoEntrada = it }
            )
        } else {
            ttsManager?.hablar(Voz.VOZ_ERROR_MIC)
        }
    }

    DisposableEffect(Unit) { onDispose { voiceManager?.liberar() } }

    LaunchedEffect(activo, modoEntrada) {
        if (!activo || modoEntrada != InputModoCiego.VOZ) {
            voiceManager?.detener()
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
                AccessibilityMode.MUTE -> listOf(
                    Triple(InputModoCiego.TECLADO,  "Teclado",  Icons.Rounded.Keyboard)
                )
                else -> listOf(
                    Triple(InputModoCiego.TECLADO,  "Teclado",  Icons.Rounded.Keyboard),
                    Triple(InputModoCiego.VOZ,      "Voz",      Icons.Rounded.Mic),
                    Triple(InputModoCiego.BRAILLE,  "Braille",  Icons.Rounded.GridOn)
                )
            }
        }

        if (a11yMode == AccessibilityMode.MUTE) {
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
                        Text("Confirmar y Continuar al Siguiente Campo", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
            } else {
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
                    singleLine      = !esCampoFecha && !esCampoHora,
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
                    colors          = OutlinedTextFieldDefaults.colors(
                        focusedTextColor     = Color.Black,
                        unfocusedTextColor   = Color.Black,
                        focusedBorderColor   = colorPrimario,
                        unfocusedBorderColor = Color.LightGray
                    )
                )

                if (activo) {
                    if (modoEntrada == InputModoCiego.SENAS) {
                        Spacer(Modifier.height(12.dp))
                        val soloNum = keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Number || 
                                      keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.NumberPassword ||
                                      keyboardOptions.keyboardType == androidx.compose.ui.text.input.KeyboardType.Phone
                        SignLanguageCameraView(
                            textoActual   = valor,
                            onTextoChange = onValorChange,
                            colorPrimario = colorPrimario,
                            soloNumeros   = soloNum,
                            esCampoFecha  = false,
                            onCompletado  = onNext
                        )
                    }

                    if (onNext != null) {
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
                        Text("Confirmar y Continuar al Siguiente Campo", fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    }
                }
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
                                .background(if (modoEntrada == modo) colorPrimario.copy(0.14f) else Color(0xFFF5F5F5))
                                .border(
                                    1.5.dp,
                                    if (modoEntrada == modo) colorPrimario else Color.LightGray.copy(0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable(onClickLabel = "Cambiar a modo $label") {
                                    vibrateTap(haptic)
                                    modoEntrada = modo
                                    if (modo != InputModoCiego.VOZ) {
                                        voiceManager?.detener()
                                    }
                                    when (modo) {
                                        InputModoCiego.TECLADO -> ttsManager?.hablar("Modo teclado. Escribe con el teclado. Al terminar, el botón verde Continuar está abajo.")
                                        InputModoCiego.VOZ     -> { /* LaunchedEffect lo maneja */ }
                                        InputModoCiego.BRAILLE -> ttsManager?.hablar("Modo teclado Braille. Toca los puntos para formar cada letra. Al terminar de escribir, el botón verde Continuar está abajo a la derecha.")
                                        InputModoCiego.SENAS   -> ttsManager?.hablar("Cámara de señas activada. Haz gestos frente a la cámara frontal.")
                                    }
                                 }
                                .semantics { contentDescription = "Modo $label. ${if (modoEntrada == modo) "Activo" else "Toca para activar"}" }
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

            AnimatedVisibility(visible = activo && modoEntrada == InputModoCiego.TECLADO) {
                Column {
                    OutlinedTextField(
                        value           = valor,
                        onValueChange   = { v ->
                            val resultado = if (esCampoFecha) formatearFechaDigitos(v) else v
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
                        colors          = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = colorPrimario,
                            unfocusedBorderColor = Color.LightGray
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
                            Text(valor, fontSize = 16.sp, color = Color(0xFF1B5E20), fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(12.dp))
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
                                    when (voiceEstado) {
                                        VoiceInputState.LISTENING -> {
                                            voiceManager?.detener()
                                            ttsManager?.hablar("Micrófono detenido.")
                                        }
                                        else -> {
                                            if (tienePermiso) {
                                                ttsManager?.hablar(Voz.VOZ_ESCUCHANDO)
                                                iniciarEscuchaConReintento(
                                                    voiceManager  = voiceManager,
                                                    idioma        = idioma,
                                                    modoAccesible = true,
                                                    esCampoFecha  = esCampoFecha,
                                                    esCampoHora   = esCampoHora,
                                                    keyboardOptions = keyboardOptions,
                                                    ttsManager    = ttsManager,
                                                    onValorChange = onValorChange,
                                                    onNext        = onNext,
                                                    onCommandParsed = onCommandParsed,
                                                    onSwitchModo    = { modoEntrada = it }
                                                )
                                            } else {
                                                permisoLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                            }
                                        }
                                    }
                                }
                                .semantics {
                                    contentDescription = when (voiceEstado) {
                                        VoiceInputState.LISTENING  -> "Escuchando. Toca para detener."
                                        VoiceInputState.PROCESSING -> "Procesando tu voz."
                                        else -> "Botón micrófono, centro de la pantalla. Toca para hablar y escribir $etiqueta."
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
                        text = when (voiceEstado) {
                            VoiceInputState.LISTENING  -> "Escuchando... habla ahora"
                            VoiceInputState.PROCESSING -> "Procesando..."
                            else                       -> "Toca el microfono o espera unos segundos"
                        },
                        fontSize  = 13.sp,
                        color     = Color.Gray,
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
                                onValorChange("")
                                ttsManager?.hablar("Texto borrado. Habla de nuevo.")
                            },
                            modifier = Modifier.semantics { contentDescription = "Borrar y repetir" }
                        ) {
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
    voiceManager:    VoiceInputManager?,
    idioma:          IdiomaVoz,
    modoAccesible:   Boolean   = false,
    esCampoFecha:    Boolean,
    esCampoHora:     Boolean,
    keyboardOptions: KeyboardOptions,
    ttsManager:      NutriTTS?,
    onValorChange:   (String) -> Unit,
    onNext:          (() -> Unit)? = null,
    onCommandParsed: ((String) -> Boolean)? = null,
    onSwitchModo:    ((InputModoCiego) -> Unit)? = null
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
                    voiceManager  = voiceManager,
                    idioma        = idioma,
                    modoAccesible = modoAccesible,
                    esCampoFecha  = esCampoFecha,
                    esCampoHora   = esCampoHora,
                    keyboardOptions = keyboardOptions,
                    ttsManager    = ttsManager,
                    onValorChange = onValorChange,
                    onNext        = onNext,
                    onCommandParsed = onCommandParsed,
                    onSwitchModo    = onSwitchModo
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
        } else {
            // Solo actualizamos el valor si no parece un comando a medias
            val resultado = sanitizarResultadoVoz(texto, esCampoFecha, esCampoHora, keyboardOptions, ttsManager, idioma)
            onValorChange(resultado)
            if (isFinal && resultado.isNotBlank() && onNext != null) {
                onNext.invoke()
            }
        }
    }
}

private fun sanitizarResultadoVoz(
    texto:           String,
    esCampoFecha:    Boolean,
    esCampoHora:     Boolean,
    keyboardOptions: KeyboardOptions,
    ttsManager:      NutriTTS? = null,
    idioma:          IdiomaVoz = IdiomaVoz.ESPANOL_MX
): String {
    var resultado = when {
        esCampoFecha -> parsearFecha(texto)
        esCampoHora  -> parsearHora(texto)
        else         -> texto
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
        "cero" to 0, "uno" to 1, "un" to 1, "dos" to 2, "tres" to 3, "cuatro" to 4, "cinco" to 5,
        "seis" to 6, "siete" to 7, "ocho" to 8, "nueve" to 9, "diez" to 10,
        "once" to 11, "doce" to 12, "trece" to 13, "catorce" to 14, "quince" to 15,
        "dieciséis" to 16, "dieciseis" to 16, "diecisiete" to 17, "dieciocho" to 18, "diecinueve" to 19,
        "veinte" to 20, "veintiuno" to 21, "veintidos" to 22, "veintidós" to 22,
        "veintitrés" to 23, "veintitres" to 23, "veinticuatro" to 24, "veinticinco" to 25,
        "veintiséis" to 26, "veintiseis" to 26, "veintisiete" to 27, "veintiocho" to 28, "veintinueve" to 29
    )
    
    val decenas = mapOf(
        "diez" to 10, "veinte" to 20, "treinta" to 30, "cuarenta" to 40, "cincuenta" to 50
    )
    
    if (unidades.containsKey(t)) return unidades[t]!!
    if (decenas.containsKey(t)) return decenas[t]!!
    
    val limpia = t.replace(" y ", " ")
    val partes = limpia.split(Regex("\\s+"))
    if (partes.size == 2) {
        val dec = decenas[partes[0]]
        val uni = unidades[partes[1]]
        if (dec != null && uni != null) {
            return dec + uni
        }
    }
    return 0
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