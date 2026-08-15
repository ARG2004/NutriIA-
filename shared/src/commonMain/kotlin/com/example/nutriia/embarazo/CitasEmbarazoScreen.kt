package com.example.nutriia.embarazo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Add
import com.example.nutriia.ginecologo.CitaEmbarazo
import com.example.nutriia.ginecologo.VinculacionEmbarazo
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.ginecologo.GinecologoViewModel
import com.example.nutriia.payment.PaymentAwareTeleconsultaButtons
import com.example.nutriia.teleconsulta.TeleconsultaViewModel
import com.example.nutriia.teleconsulta.TipoLlamada
import com.example.nutriia.util.PermissionHelper
import com.example.nutriia.util.PermissionType
import com.example.nutriia.util.rememberPermissionState
import kotlinx.coroutines.delay
import com.example.nutriia.accesibilidad.*

private val EmbRosa       = Color(0xFFEC9BBF)
private val EmbRosaOscuro = Color(0xFFD4679A)
private val EmbFondo      = Color(0xFFFFF5F9)
private val EmbTeal       = Color(0xFF4DB6AC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitasEmbarazoScreen(
    viewModel: GinecologoViewModel = viewModel(),
    teleconsultaViewModel: TeleconsultaViewModel,
    mamaUid: String,
    mamaNombre: String,
    iniciarLlamadaAlEntrar: TipoLlamada? = null,
    pagoIdExitoso: String = "",
    onLlamadaIniciada: () -> Unit = {},
    onAbrirPago: (ginecologoUid: String, ginecologoNombre: String, tipo: TipoLlamada) -> Unit,
    onBack: () -> Unit
) {
    val a11yVm: AccessibilityViewModel = viewModel()
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esBlind      = a11yMode == AccessibilityMode.BLIND

    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar(
                loc(
                    "Módulo de citas de embarazo. Aquí puedes ver y agendar citas médicas con tu ginecólogo vinculado.",
                    "Pregnancy appointments module. Here you can view and schedule medical appointments with your linked gynecologist."
                )
            )
        }
    }

    val vinculacion by viewModel.vinculacionActual.collectAsStateWithLifecycle()
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val citas by viewModel.citasDeLaMama.collectAsStateWithLifecycle()
    var showAgendarDialog by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    var permissionCheckStep by remember { mutableIntStateOf(0) }

    val cameraState = rememberPermissionState(
        type = PermissionType.CAMERA,
        onDismissed = { permissionCheckStep = 1 }
    ) {
        permissionCheckStep = 1
    }
    val micState = rememberPermissionState(
        type = PermissionType.MICROPHONE,
        onDismissed = { permissionCheckStep = 2 }
    ) {
        permissionCheckStep = 2
    }
    val phoneState = rememberPermissionState(
        type = PermissionType.PHONE,
        onDismissed = { permissionCheckStep = 3 }
    ) {
        permissionCheckStep = 3
    }
    val nearDevicesState = rememberPermissionState(
        type = PermissionType.NEAR_DEVICES,
        onDismissed = { permissionCheckStep = 4 }
    ) {
        permissionCheckStep = 4
    }

    LaunchedEffect(permissionCheckStep) {
        delay(500)
        when (permissionCheckStep) {
            0 -> {
                val hasCam = PermissionHelper.hasPermissions(context, PermissionHelper.getRequiredPermissions(PermissionType.CAMERA))
                if (!hasCam) {
                    cameraState.requestPermission()
                } else {
                    permissionCheckStep = 1
                }
            }
            1 -> {
                val hasMic = PermissionHelper.hasPermissions(context, PermissionHelper.getRequiredPermissions(PermissionType.MICROPHONE))
                if (!hasMic) {
                    micState.requestPermission()
                } else {
                    permissionCheckStep = 2
                }
            }
            2 -> {
                val hasPhone = PermissionHelper.hasPermissions(context, PermissionHelper.getRequiredPermissions(PermissionType.PHONE))
                if (!hasPhone) {
                    phoneState.requestPermission()
                } else {
                    permissionCheckStep = 3
                }
            }
            3 -> {
                val hasNear = PermissionHelper.hasPermissions(context, PermissionHelper.getRequiredPermissions(PermissionType.NEAR_DEVICES))
                if (!hasNear) {
                    nearDevicesState.requestPermission()
                } else {
                    permissionCheckStep = 4
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.initComoMama()
    }

    // Si viene de regreso tras pagar exitosamente, iniciar llamada
    LaunchedEffect(vinculacion, iniciarLlamadaAlEntrar, pagoIdExitoso) {
        val v = vinculacion ?: return@LaunchedEffect
        val tipo = iniciarLlamadaAlEntrar ?: return@LaunchedEffect
        if (pagoIdExitoso.isNotBlank()) {
            teleconsultaViewModel.iniciarLlamadaComoPadre(
                padreUid = mamaUid,
                padreNombre = mamaNombre,
                nutriologoUid = v.ginecologoUid,
                nutriologoNombre = v.ginecologoNombre,
                childId = "embarazo",
                childNombre = "Embarazo",
                pagoId = pagoIdExitoso,
                tipo = tipo
            )
            onLlamadaIniciada()
        }
    }

    Scaffold(
        containerColor = EmbFondo,
        topBar = {
            TopAppBar(
                title = { Text("Mis Citas Médicas", fontWeight = FontWeight.Bold, color = EmbRosaOscuro) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = EmbRosaOscuro)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        if (cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmbRosa)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val v = vinculacion
            if (v == null || v.estado != com.example.nutriia.ginecologo.EstadoVinculacionEmbarazo.ACTIVO) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 40.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(EmbRosa.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.CalendarToday, null, tint = EmbRosa, modifier = Modifier.size(50.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        "Sin ginecólogo vinculado",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Debes vincularte con un ginecólogo desde el módulo correspondiente para poder agendar citas.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(24.dp)) {
                        Text(
                            "Ginecólogo/a:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            v.ginecologoNombre,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                        
                        Spacer(Modifier.height(20.dp))
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        Spacer(Modifier.height(20.dp))

                        if (v.proximaCitaFecha.isNotBlank()) {
                            Text(
                                "Detalles de la Cita",
                                fontSize = 14.sp,
                                color = EmbRosaOscuro,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(12.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = EmbRosa.copy(alpha = 0.05f)),
                                border = BorderStroke(1.dp, EmbRosa.copy(alpha = 0.2f))
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            if (v.proximaCitaTipo == "TELECONSULTA") Icons.Rounded.Videocam else Icons.Rounded.LocationOn,
                                            null,
                                            tint = EmbRosaOscuro,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            if (v.proximaCitaTipo == "TELECONSULTA") "Video Consulta Online" else "Consulta Presencial",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                        Column {
                                            Text("Fecha", fontSize = 10.sp, color = Color.Gray)
                                            Text(v.proximaCitaFecha, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                        }
                                        Column {
                                            Text("Hora", fontSize = 10.sp, color = Color.Gray)
                                            Text(v.proximaCitaHora, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
                                        }
                                    }
                                    if (v.proximaCitaMotivo.isNotBlank()) {
                                        Spacer(Modifier.height(10.dp))
                                        Text("Motivo / Notas", fontSize = 10.sp, color = Color.Gray)
                                        Text(v.proximaCitaMotivo, fontSize = 13.sp, color = Color.DarkGray)
                                    }

                                    if (v.proximaCitaTipo == "TELECONSULTA") {
                                        Spacer(Modifier.height(16.dp))
                                        Text(
                                            "Esta es una videollamada de teleconsulta. Requiere realizar el pago de la sesión para iniciar.",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            lineHeight = 15.sp
                                        )
                                        PaymentAwareTeleconsultaButtons(
                                            nutriologoUid = v.ginecologoUid,
                                            nutriologoNombre = v.ginecologoNombre,
                                            padreUid = mamaUid,
                                            padreNombre = mamaNombre,
                                            childId = "embarazo",
                                            childNombre = "Embarazo",
                                            teleconsultaViewModel = teleconsultaViewModel,
                                            onAbrirPago = onAbrirPago
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No tienes citas agendadas próximamente.",
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showAgendarDialog = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = EmbRosaOscuro)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Agendar Cita", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }

                // --- HISTORIAL DE CITAS ---
                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.History,
                        contentDescription = null,
                        tint = EmbRosaOscuro,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Historial de Citas",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.DarkGray
                    )
                }
                Spacer(Modifier.height(12.dp))

                if (citas.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No se han registrado citas en el historial.",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                } else {
                    val ordenadas = citas.sortedByDescending { it.fecha }
                    ordenadas.forEach { cita ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(EmbTeal.copy(0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (cita.tipo == "TELECONSULTA") Icons.Rounded.Videocam else Icons.Rounded.LocationOn,
                                        contentDescription = null,
                                        tint = EmbTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(16.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Fecha: ${cita.fecha} a las ${cita.hora}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.DarkGray
                                    )
                                    Text(
                                        "Modalidad: ${if (cita.tipo == "TELECONSULTA") "Videollamada" else "Presencial"}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    if (cita.motivo.isNotBlank()) {
                                        Text(
                                            "Motivo: ${cita.motivo}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAgendarDialog && vinculacion != null) {
        AgendarCitaDialog(
            vinculacion = vinculacion!!,
            onDismiss = { showAgendarDialog = false },
            onConfirm = { fecha, hora, motivo, tipo ->
                viewModel.agendarCita(vinculacion!!.id, fecha, hora, motivo, tipo)
                showAgendarDialog = false
            },
            a11yMode = a11yMode,
            ttsManager = ttsManager,
            idiomaActual = idiomaActual
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendarCitaDialog(
    vinculacion: VinculacionEmbarazo,
    onDismiss: () -> Unit,
    onConfirm: (fecha: String, hora: String, motivo: String, tipo: String) -> Unit,
    a11yMode: AccessibilityMode = AccessibilityMode.NORMAL,
    ttsManager: NutriTTS? = null,
    idiomaActual: IdiomaVoz = IdiomaVoz.ESPANOL_MX
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var fecha by remember { mutableStateOf(vinculacion.proximaCitaFecha.ifBlank { "" }) }
    var hora by remember { mutableStateOf(vinculacion.proximaCitaHora.ifBlank { "" }) }
    var motivo by remember { mutableStateOf(vinculacion.proximaCitaMotivo.ifBlank { "" }) }
    var tipo by remember { mutableStateOf(vinculacion.proximaCitaTipo.ifBlank { "TELECONSULTA" }) }

    val EmbRosa       = Color(0xFFEC9BBF)
    val EmbRosaOscuro = Color(0xFFD4679A)
    val esBlind = a11yMode == AccessibilityMode.BLIND
    val esMute = a11yMode == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute
    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }

    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> fecha
            1 -> hora
            2 -> motivo
            else -> ""
        }
    }

    LaunchedEffect(fecha) {
        if (!esBlind || fecha.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (fecha == valorInicial) return@LaunchedEffect
        delay(1500L)
        if (fecha.isNotBlank() && campoActivo == 0 && fecha != valorInicial) campoActivo = 1
    }
    LaunchedEffect(hora) {
        if (!esBlind || hora.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (hora == valorInicial) return@LaunchedEffect
        delay(1500L)
        if (hora.isNotBlank() && campoActivo == 1 && hora != valorInicial) campoActivo = 2
    }
    LaunchedEffect(motivo) {
        if (!esBlind || motivo.isBlank() || campoActivo != 2) return@LaunchedEffect
        if (motivo == valorInicial) return@LaunchedEffect
        delay(2500L)
        if (motivo.isNotBlank() && campoActivo == 2 && motivo != valorInicial) campoActivo = 3
    }

    val voiceManager = remember { if (esBlind) VoiceInputManager(context) else null }

    val ejecutarConfirmarCita: () -> Unit = {
        if (fecha.isNotBlank() && hora.isNotBlank()) {
            onConfirm(fecha, hora, motivo, tipo)
            if (esBlind) {
                ttsManager?.hablar(loc("Cita agendada con éxito.", "Appointment scheduled successfully."))
            }
        }
    }

    LaunchedEffect(campoActivo) {
        if (esBlind && campoActivo == 3) {
            ttsManager?.hablarYEsperar(
                loc(
                    "Campos completados. Di guardar cita o confirmar para agendar.",
                    "Fields completed. Say save appointment or confirm to schedule."
                ),
                margenMs = 800L
            )
            voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                val cmd = result.lowercase(java.util.Locale.getDefault()).trim()
                if (cmd.contains("guardar") || cmd.contains("confirmar") || cmd.contains("agendar") || cmd.contains("save")) {
                    ejecutarConfirmarCita()
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        title = {
            Text(
                "Agendar Cita",
                fontWeight = FontWeight.Black,
                color = EmbRosaOscuro,
                fontSize = 20.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Ginecólogo/a: ${vinculacion.ginecologoNombre}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                if (esAccesible) {
                    CampoTextoAccesible(
                        valor = fecha,
                        onValorChange = { fecha = it },
                        etiqueta = loc("Fecha de la cita (AAAA-MM-DD)", "Appointment Date (YYYY-MM-DD)"),
                        descripcionVoz = loc("Toca dos veces para seleccionar fecha", "Double tap to select date"),
                        esCampoFecha = true,
                        ttsManager = ttsManager,
                        colorPrimario = EmbRosaOscuro,
                        activo = campoActivo == 0,
                        onFocus = { campoActivo = 0 },
                        onNext = { campoActivo = 1 }
                    )
                    androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 1) {
                        CampoTextoAccesible(
                            valor = hora,
                            onValorChange = { hora = it },
                            etiqueta = loc("Hora de la cita (HH:MM)", "Appointment Time (HH:MM)"),
                            descripcionVoz = loc("Toca dos veces para seleccionar hora", "Double tap to select time"),
                            esCampoHora = true,
                            ttsManager = ttsManager,
                            colorPrimario = EmbRosaOscuro,
                            activo = campoActivo == 1,
                            onFocus = { campoActivo = 1 },
                            onNext = { campoActivo = 2 }
                        )
                    }
                    androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 2) {
                        CampoTextoAccesible(
                            valor = motivo,
                            onValorChange = { motivo = it },
                            etiqueta = loc("Motivo / Notas", "Reason / Notes"),
                            descripcionVoz = loc("Di el motivo o notas de la cita", "Speak the appointment reason or notes"),
                            ttsManager = ttsManager,
                            colorPrimario = EmbRosaOscuro,
                            activo = campoActivo == 2,
                            onFocus = { campoActivo = 2 },
                            onNext = { campoActivo = 3 }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = fecha,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Fecha") },
                        placeholder = { Text("Seleccionar fecha") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmbRosa,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                val cal = java.util.Calendar.getInstance()
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        fecha = String.format(java.util.Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    cal.get(java.util.Calendar.YEAR),
                                    cal.get(java.util.Calendar.MONTH),
                                    cal.get(java.util.Calendar.DAY_OF_MONTH)
                                ).show()
                            }) {
                                Icon(Icons.Rounded.CalendarToday, null, tint = EmbRosa)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = hora,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Hora") },
                        placeholder = { Text("Seleccionar hora") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmbRosa,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        trailingIcon = {
                            IconButton(onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hourOfDay, minute ->
                                        hora = String.format(java.util.Locale.US, "%02d:%02d", hourOfDay, minute)
                                    },
                                    12, 0, true
                                ).show()
                            }) {
                                Icon(Icons.Rounded.AccessTime, null, tint = EmbRosa)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = motivo,
                        onValueChange = { motivo = it },
                        label = { Text("Motivo / Notas") },
                        placeholder = { Text("Ej. Control prenatal mensual") },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmbRosa,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column {
                    Text("Tipo de consulta", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = tipo == "TELECONSULTA",
                            onClick = { tipo = "TELECONSULTA" },
                            label = { Text("Videollamada") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmbRosa.copy(alpha = 0.2f),
                                selectedLabelColor = EmbRosaOscuro
                            ),
                            leadingIcon = {
                                if (tipo == "TELECONSULTA") {
                                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                        FilterChip(
                            selected = tipo == "PRESENCIAL",
                            onClick = { tipo = "PRESENCIAL" },
                            label = { Text("Presencial") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmbRosa.copy(alpha = 0.2f),
                                selectedLabelColor = EmbRosaOscuro
                            ),
                            leadingIcon = {
                                if (tipo == "PRESENCIAL") {
                                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { ejecutarConfirmarCita() },
                enabled = fecha.isNotBlank() && hora.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmbRosaOscuro)
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}
