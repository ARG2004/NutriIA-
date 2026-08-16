package com.example.nutriia.dashboard

// import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.ListAlt
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.teleconsulta.TeleconsultaButtons
import com.example.nutriia.teleconsulta.TeleconsultaViewModel
import com.example.nutriia.teleconsulta.TipoLlamada

// ─── Colores Dashboard ──────────────────────────────────────────────────────
private val NutriGreen     = Color(0xFF689F38)
private val NutriDarkGreen = Color(0xFF33691E)
private val NutriBgCrema   = Color(0xFFF8F9F3)
private val NutriCardWhite = Color.White
private val NutriPurple    = Color(0xFF9C8FE0)
private val NutriTeal      = Color(0xFF4DB6AC)
private val NutriOrange    = Color(0xFFFF8F00)

private val nutri_avatarColors = listOf(
    Color(0xFFEC9BBF), Color(0xFF9C8FE0), Color(0xFFFFAB76),
    Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFF64B5F6)
)

private data class EtStageInfo(val nombre: String, val color: Color)

@Composable
fun NutritionistDashboardScreen(
    viewModel:         NutritionistDashboardViewModel = viewModel(),
    // ── TeleconsultaViewModel compartido ─────────────────────────────────────
    teleconsultaViewModel: TeleconsultaViewModel     = viewModel(),
    onLogout:          () -> Unit                    = {},
    onPatientClick:    (PacienteResumen) -> Unit      = {},
    onNewPlan:         () -> Unit                    = {},
    onViewAllPatients: () -> Unit                    = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.init() }

    LaunchedEffect(uiState.miPerfil?.uid) {
        uiState.miPerfil?.uid?.let { uid ->
            teleconsultaViewModel.cargarHistorial(uid)
            teleconsultaViewModel.iniciarObservacionEntrantesNutriologo(uid)
        }
    }

    // ELIMINADO: TeleconsultaHostOverlay(viewModel = teleconsultaViewModel)
    // El overlay ahora vive en MainActivity para ser global.

    Scaffold(
        containerColor = NutriBgCrema,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !uiState.cargando,
                enter = fadeIn() + scaleIn(initialScale = 0.8f),
                exit = fadeOut() + scaleOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick        = onNewPlan,
                    containerColor = NutriGreen,
                    contentColor   = Color.White,
                    shape          = RoundedCornerShape(24.dp),
                    modifier       = Modifier.padding(bottom = 16.dp).shadow(12.dp, RoundedCornerShape(24.dp))
                ) {
                    Icon(Icons.Rounded.PostAdd, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nuevo plan", fontWeight = FontWeight.Bold)
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->

        if (uiState.cargando) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = NutriGreen, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier            = Modifier.fillMaxSize().padding(padding),
            contentPadding      = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                NutritionistTopBar(
                    nombre       = uiState.miPerfil?.nombre ?: "Nutriólogo/a",
                    especialidad = uiState.miPerfil?.especialidad ?: "Seguimiento activo",
                    onLogout     = onLogout
                )
            }

            item {
                uiState.miPerfil?.let { perfil ->
                    NutritionistProfileCard(
                        nombre       = perfil.nombre,
                        especialidad = perfil.especialidad,
                        codigo       = perfil.codigo
                    )
                }
            }

            if (uiState.solicitudesPendientes.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader("Solicitudes pendientes", Icons.Rounded.HowToReg, NutriOrange)
                }
                items(uiState.solicitudesPendientes, key = { it.id }) { solicitud ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInHorizontally() + fadeIn()
                    ) {
                        SolicitudPendienteCard(
                            solicitud  = solicitud,
                            onAceptar  = { viewModel.aceptarSolicitud(solicitud.id) },
                            onRechazar = { viewModel.rechazarSolicitud(solicitud.id) }
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                NutritionistStatsRow(
                    totalPatients = uiState.pacientes.size,
                    totalPlans    = uiState.planesActivos.size,
                    activeToday   = uiState.pacientes.count { it.ultimaActualizacion == "Hoy" }
                )
            }

            if (uiState.planesActivos.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(24.dp))
                    SectionHeader("Planes activos", Icons.AutoMirrored.Rounded.MenuBook, NutriGreen)
                }
                item { ActiveMealPlansRow(uiState.planesActivos) }
            }

            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(
                    title       = "Mis pacientes",
                    icon        = Icons.Rounded.ChildCare,
                    color       = NutriDarkGreen,
                    actionLabel = if (uiState.pacientes.size > 3) "Ver todos" else null,
                    onAction    = onViewAllPatients
                )
            }

            items(uiState.pacientes, key = { it.childId }) { paciente ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn()
                ) {
                    PatientCard(
                        patient                = paciente,
                        onClick                = onPatientClick,
                        nutriologoNombre       = uiState.miPerfil?.nombre ?: "",
                        teleconsultaViewModel  = teleconsultaViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun NutritionistTopBar(nombre: String, especialidad: String, onLogout: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Column {
            Text("NutriIA", fontSize = 28.sp, fontWeight = FontWeight.Black, color = NutriDarkGreen)
            Text(especialidad.ifBlank { "Nutriólogo/a" }, fontSize = 13.sp, color = Color.Gray)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TopBarCircleButton(Icons.Rounded.Settings) { }
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
            Icon(icon, null, tint = if (isLogout) Color(0xFFE57373) else NutriGreen, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun NutritionistProfileCard(nombre: String, especialidad: String, codigo: String) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape     = RoundedCornerShape(35.dp),
        colors    = CardDefaults.cardColors(containerColor = NutriCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1.2f)) {
                Text(nombre, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = NutriDarkGreen, lineHeight = 26.sp)
                Text(especialidad, fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))
                Surface(color = NutriGreen.copy(0.1f), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Fingerprint, null, tint = NutriGreen, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(codigo, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = NutriDarkGreen)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(105.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(NutriBgCrema)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.QrCode2, contentDescription = "QR Vincular", tint = NutriDarkGreen, modifier = Modifier.size(72.dp))
            }
        }
    }
}

@Composable
private fun PatientCard(
    patient:               PacienteResumen,
    onClick:               (PacienteResumen) -> Unit,
    nutriologoNombre:      String,
    teleconsultaViewModel: TeleconsultaViewModel
) {
    val avatarColor = remember { nutri_avatarColors[patient.childId.hashCode().let { if (it < 0) -it else it } % nutri_avatarColors.size] }

    val etapa = remember(patient.birthDate) {
        try {
            val (anio, mes, dia) = if (patient.birthDate.contains("/")) {
                val p = patient.birthDate.split("/").map { it.toInt() }
                Triple(p[2], p[1], p[0])
            } else {
                val p = patient.birthDate.split("-").map { it.toInt() }
                Triple(p[0], p[1], p[2])
            }
            val calNac = java.util.Calendar.getInstance().apply { set(anio, mes - 1, dia) }
            val calHoy = java.util.Calendar.getInstance()
            val diffYears = calHoy.get(java.util.Calendar.YEAR) - calNac.get(java.util.Calendar.YEAR)
            val diffMonths = calHoy.get(java.util.Calendar.MONTH) - calNac.get(java.util.Calendar.MONTH)
            val meses = diffYears * 12 + diffMonths
            when {
                meses < 6  -> EtStageInfo("Lactancia", Color(0xFF64B5F6))
                meses < 12 -> EtStageInfo("Alimentación Comp.", Color(0xFF81C784))
                meses < 36 -> EtStageInfo("Primera Infancia", Color(0xFFFFA726))
                else       -> null
            }
        } catch (_: Exception) { null }
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp).clickable { onClick(patient) },
        shape     = RoundedCornerShape(32.dp),
        colors    = CardDefaults.cardColors(containerColor = NutriCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(avatarColor.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(patient.childNombre.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 22.sp, color = avatarColor)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(patient.childNombre, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = NutriDarkGreen)
                    etapa?.let {
                        Text(it.nombre, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                    }
                }
                Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray)
            }

            Spacer(Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                BiometrySmallStat(Icons.Rounded.Scale, "${patient.weightKg} kg", "Peso")
                BiometrySmallStat(Icons.Rounded.Straighten, "${patient.heightCm} cm", "Talla")
            }

            Spacer(Modifier.height(18.dp))

            TeleconsultaButtons(
                onLlamadaAudio = {
                    teleconsultaViewModel.iniciarLlamada(
                        patient.padreUid, patient.padreNombre, patient.childId,
                        patient.childNombre, nutriologoNombre, TipoLlamada.AUDIO
                    )
                },
                onLlamadaVideo = {
                    teleconsultaViewModel.iniciarLlamada(
                        patient.padreUid, patient.padreNombre, patient.childId,
                        patient.childNombre, nutriologoNombre, TipoLlamada.VIDEO
                    )
                }
            )
        }
    }
}

@Composable
private fun NutritionistStatsRow(totalPatients: Int, totalPlans: Int, activeToday: Int) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        StatCard(Modifier.weight(1f), "$totalPatients", "Pacientes", Icons.Rounded.People, NutriPurple)
        StatCard(Modifier.weight(1f), "$totalPlans", "Planes", Icons.AutoMirrored.Rounded.ListAlt, NutriTeal)
        StatCard(Modifier.weight(1f), "$activeToday", "Activos", Icons.Rounded.Bolt, NutriOrange)
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String, icon: ImageVector, color: Color) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = NutriCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, null, tint = color.copy(0.6f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Black, color = NutriDarkGreen)
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector, color: Color, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, color = NutriDarkGreen, modifier = Modifier.weight(1f))
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) {
                Text(actionLabel, color = NutriGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun ActiveMealPlansRow(planes: List<com.example.nutriia.vinculacion.PlanAlimentario>) {
    LazyRow(
        contentPadding        = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(planes, key = { it.id }) { plan ->
            Card(
                modifier  = Modifier.width(150.dp),
                shape     = RoundedCornerShape(24.dp),
                colors    = CardDefaults.cardColors(containerColor = NutriCardWhite),
                elevation = CardDefaults.cardElevation(1.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Icon(Icons.Rounded.Restaurant, null, tint = NutriOrange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(8.dp))
                    Text(plan.titulo.ifBlank { "Plan" }, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                    Text("${plan.comidas.size} comidas", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun BiometrySmallStat(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = NutriGreen, modifier = Modifier.size(18.dp))
        Text(value, fontWeight = FontWeight.Black, fontSize = 14.sp, color = NutriDarkGreen)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
private fun SolicitudPendienteCard(solicitud: com.example.nutriia.vinculacion.Vinculacion, onAceptar: () -> Unit, onRechazar: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = NutriOrange.copy(alpha = 0.05f)),
        border    = BorderStroke(1.dp, NutriOrange.copy(alpha = 0.1f))
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(NutriOrange.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PersonAdd, null, tint = NutriOrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(solicitud.padreNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text("Hijo/a: ${solicitud.childNombre}", fontSize = 11.sp, color = Color.Gray)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onRechazar) { Icon(Icons.Rounded.Cancel, null, tint = Color(0xFFEF5350)) }
                IconButton(onClick = onAceptar) { Icon(Icons.Rounded.CheckCircle, null, tint = NutriGreen) }
            }
        }
    }
}