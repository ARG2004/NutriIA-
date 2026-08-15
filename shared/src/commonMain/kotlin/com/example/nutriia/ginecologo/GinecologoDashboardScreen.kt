package com.example.nutriia.ginecologo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.teleconsulta.TeleconsultaViewModel
import com.example.nutriia.teleconsulta.TeleconsultaButtons
import com.example.nutriia.teleconsulta.TipoLlamada

// ─── Colores Dashboard Ginecólogo (Paleta Embarazo) ─────────────────────────
private val EmbRosa       = Color(0xFFEC9BBF)
private val EmbRosaOscuro = Color(0xFFD4679A)
private val EmbMorado     = Color(0xFF9C8FE0)
private val EmbTeal       = Color(0xFF4DB6AC)
private val EmbFondo      = Color(0xFFFFF5F9)
private val CardWhite     = Color.White

@Composable
fun GinecologoDashboardScreen(
    viewModel: GinecologoDashboardViewModel = viewModel(),
    teleconsultaViewModel: TeleconsultaViewModel,
    onLogout: () -> Unit = {},
    onConfiguracion: () -> Unit = {},
    onPatientClick: (VinculacionEmbarazo) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var pacienteParaCita by remember { mutableStateOf<VinculacionEmbarazo?>(null) }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val esBlind = a11yMode == AccessibilityMode.BLIND

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar("Panel del ginecólogo. Aquí puedes revisar tus pacientes, solicitudes pendientes y agendar citas de seguimiento prenatal.")
        }
        viewModel.init()
    }

    Scaffold(
        containerColor = EmbFondo
    ) { padding ->
        if (uiState.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator(color = EmbRosa, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            // Top Bar: nombre + especialidad + botones
            item {
                GinecologoTopBar(
                    nombre = uiState.miPerfil?.nombre ?: "Ginecólogo/a",
                    especialidad = uiState.miPerfil?.especialidad ?: "Ginecología y Obstetricia",
                    onLogout = onLogout,
                    onConfiguracion = onConfiguracion
                )
            }

            // Card resumen: Mamás vinculadas y Solicitudes pendientes
            item {
                GinecologoStatsRow(
                    totalMamas = uiState.vinculacionesActivas.size,
                    totalPendientes = uiState.solicitudesPendientes.size
                )
            }

            // Sección: Solicitudes pendientes
            if (uiState.solicitudesPendientes.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionHeader("Solicitudes pendientes", Icons.Rounded.NotificationsActive, EmbRosaOscuro)
                }
                items(uiState.solicitudesPendientes, key = { it.id }) { solicitud ->
                    SolicitudEmbarazoPendienteCard(
                        solicitud = solicitud,
                        onAceptar = { viewModel.aceptarSolicitud(solicitud.id) },
                        onRechazar = { viewModel.rechazarSolicitud(solicitud.id) }
                    )
                }
            }

            // Sección: Mis pacientes (embarazo)
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader("Mis pacientes (embarazo)", Icons.Rounded.Female, EmbMorado)
            }

            if (uiState.vinculacionesActivas.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("Aún no tienes mamás vinculadas", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                items(uiState.vinculacionesActivas, key = { it.id }) { vinculacion ->
                    PacienteEmbarazoCard(
                        vinculacion = vinculacion,
                        miNombre = uiState.miPerfil?.nombre ?: "Ginecólogo/a",
                        teleconsultaViewModel = teleconsultaViewModel,
                        onAgendarClick = { pacienteParaCita = vinculacion },
                        onCancelarCitaClick = { viewModel.cancelarCita(vinculacion.id) },
                        onPatientClick = onPatientClick
                    )
                }
            }
        }
    }

    pacienteParaCita?.let { vinculacion ->
        AgendarCitaDialog(
            vinculacion = vinculacion,
            onDismiss = { pacienteParaCita = null },
            onConfirm = { fecha, hora, motivo, tipo ->
                viewModel.agendarCita(vinculacion.id, fecha, hora, motivo, tipo)
                pacienteParaCita = null
            }
        )
    }
}

@Composable
private fun GinecologoTopBar(nombre: String, especialidad: String, onLogout: () -> Unit, onConfiguracion: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("NutriIA", fontSize = 26.sp, fontWeight = FontWeight.Black, color = EmbRosaOscuro)
            Text(especialidad.ifBlank { "Ginecología" }, fontSize = 13.sp, color = Color.Gray)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopBarCircleButton(Icons.Rounded.Settings, onClick = onConfiguracion)
            TopBarCircleButton(Icons.AutoMirrored.Rounded.ExitToApp, isLogout = true, onClick = onLogout)
        }
    }
}

@Composable
private fun TopBarCircleButton(icon: ImageVector, isLogout: Boolean = false, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.White,
        shadowElevation = 1.dp,
        modifier = Modifier.size(44.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = if (isLogout) Color(0xFFE57373) else EmbRosa, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun GinecologoStatsRow(totalMamas: Int, totalPendientes: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(Modifier.weight(1f), "$totalMamas", "Mamás vinculadas", Icons.Rounded.People, EmbMorado)
        StatCard(Modifier.weight(1f), "$totalPendientes", "Solicitudes", Icons.Rounded.HourglassTop, EmbRosa)
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, icon: ImageVector, color: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color.copy(0.6f), modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color.DarkGray)
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.DarkGray)
    }
}

@Composable
private fun SolicitudEmbarazoPendienteCard(solicitud: VinculacionEmbarazo, onAceptar: () -> Unit, onRechazar: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = EmbRosa.copy(alpha = 0.05f)),
        border = BorderStroke(1.dp, EmbRosa.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(EmbRosa.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PersonAdd, null, tint = EmbRosa, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(solicitud.mamaNombre, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Solicitud de vinculación", fontSize = 12.sp, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRechazar) { Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFE57373)) }
                IconButton(onClick = onAceptar) { Icon(Icons.Rounded.CheckCircle, null, tint = EmbTeal) }
            }
        }
    }
}

@Composable
private fun PacienteEmbarazoCard(
    vinculacion: VinculacionEmbarazo,
    miNombre: String,
    teleconsultaViewModel: TeleconsultaViewModel,
    onAgendarClick: () -> Unit,
    onCancelarCitaClick: () -> Unit,
    onPatientClick: (VinculacionEmbarazo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(EmbMorado.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Text(vinculacion.mamaNombre.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp, color = EmbMorado)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(vinculacion.mamaNombre, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
                    Text("Vínculo activo", fontSize = 12.sp, color = EmbTeal, fontWeight = FontWeight.Medium)
                }
                Icon(
                    if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    null,
                    tint = Color.LightGray
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    HorizontalDivider(
                        color = Color.LightGray.copy(alpha = 0.2f),
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    if (vinculacion.proximaCitaFecha.isNotBlank()) {
                        Text(
                            "Próxima Cita Agendada",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmbRosaOscuro
                        )
                        Spacer(Modifier.height(6.dp))
                        
                        Text(
                            "Fecha: ${vinculacion.proximaCitaFecha} a las ${vinculacion.proximaCitaHora}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.DarkGray
                        )
                        
                        Text(
                            "Tipo: ${if (vinculacion.proximaCitaTipo == "TELECONSULTA") "Video Consulta Online" else "Consulta Presencial"}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        if (vinculacion.proximaCitaMotivo.isNotBlank()) {
                            Text(
                                "Motivo: ${vinculacion.proximaCitaMotivo}",
                                fontSize = 12.sp,
                                color = Color.Gray
                            )
                        }

                        if (vinculacion.proximaCitaTipo == "TELECONSULTA") {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Iniciar teleconsulta (Llamar a paciente):",
                                fontSize = 11.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                            TeleconsultaButtons(
                                onLlamadaAudio = {
                                    teleconsultaViewModel.iniciarLlamada(
                                        padreUid = vinculacion.mamaUid,
                                        padreNombre = vinculacion.mamaNombre,
                                        childId = "embarazo",
                                        childNombre = "Embarazo",
                                        nutriologoNombre = miNombre,
                                        tipo = TipoLlamada.AUDIO
                                    )
                                },
                                onLlamadaVideo = {
                                    teleconsultaViewModel.iniciarLlamada(
                                        padreUid = vinculacion.mamaUid,
                                        padreNombre = vinculacion.mamaNombre,
                                        childId = "embarazo",
                                        childNombre = "Embarazo",
                                        nutriologoNombre = miNombre,
                                        tipo = TipoLlamada.VIDEO
                                    )
                                }
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = onCancelarCitaClick,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f))
                        ) {
                            Text("Cancelar Cita", fontSize = 12.sp)
                        }
                    } else {
                        Text(
                            "No hay citas programadas para esta paciente.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onAgendarClick,
                            modifier = Modifier.fillMaxWidth().height(38.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmbRosa)
                        ) {
                            Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Agendar Cita", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { onPatientClick(vinculacion) },
                        modifier = Modifier.fillMaxWidth().height(38.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmbTeal)
                    ) {
                        Icon(Icons.Rounded.FolderShared, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ver Expediente Clínico", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgendarCitaDialog(
    vinculacion: VinculacionEmbarazo,
    onDismiss: () -> Unit,
    onConfirm: (fecha: String, hora: String, motivo: String, tipo: String) -> Unit
) {
    val context = LocalContext.current
    var fecha by remember { mutableStateOf(vinculacion.proximaCitaFecha.ifBlank { "" }) }
    var hora by remember { mutableStateOf(vinculacion.proximaCitaHora.ifBlank { "" }) }
    var motivo by remember { mutableStateOf(vinculacion.proximaCitaMotivo.ifBlank { "" }) }
    var tipo by remember { mutableStateOf(vinculacion.proximaCitaTipo.ifBlank { "TELECONSULTA" }) }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

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
                    "Paciente: ${vinculacion.mamaNombre}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

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

                if (esAccesible) {
                    CampoTextoAccesible(
                        valor = motivo,
                        onValorChange = { motivo = it },
                        etiqueta = "Motivo / Notas",
                        descripcionVoz = "Di el motivo de la cita, por ejemplo: control prenatal mensual",
                        placeholder = "Ej. Control prenatal mensual",
                        ttsManager = a11yVm.ttsManager,
                        colorPrimario = EmbRosa,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
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
                onClick = {
                    if (fecha.isNotBlank() && hora.isNotBlank()) {
                        onConfirm(fecha, hora, motivo, tipo)
                    }
                },
                enabled = fecha.isNotBlank() && hora.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = EmbRosa),
                shape = RoundedCornerShape(12.dp),
                modifier = if (esAccesible) Modifier.height(56.dp) else Modifier
            ) {
                Text("Confirmar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    )
}

