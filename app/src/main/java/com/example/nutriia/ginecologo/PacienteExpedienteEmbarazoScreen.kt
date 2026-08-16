package com.example.nutriia.ginecologo

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.embarazo.GananciaPesoCalculator
import com.example.nutriia.embarazo.SintomasAnalyzer
import com.example.nutriia.embarazo.NivelSintoma
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.embarazo.RegistroPesoEmbarazo
import com.example.nutriia.embarazo.RegistroSintomasEmbarazo
import com.google.firebase.Timestamp
import java.util.Locale

private val EmbRosa       = Color(0xFFEC9BBF)
private val EmbRosaOscuro = Color(0xFFD4679A)
private val EmbRosaClaro  = Color(0xFFFDE8F2)
private val EmbMorado     = Color(0xFF9C8FE0)
private val EmbTeal       = Color(0xFF4DB6AC)
private val EmbFondo      = Color(0xFFFFF5F9)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PacienteExpedienteEmbarazoScreen(
    mamaUid: String,
    mamaNombre: String,
    onBack: () -> Unit,
    viewModel: PacienteExpedienteEmbarazoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableIntStateOf(0) }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val esBlind = a11yMode == AccessibilityMode.BLIND

    LaunchedEffect(mamaNombre) {
        if (esBlind) {
            a11yVm.hablar("Abriendo expediente de embarazo de $mamaNombre. Aquí puedes revisar el progreso gestacional, historial de peso, bitácora de síntomas y citas médicas.")
        }
    }

    LaunchedEffect(mamaUid) {
        viewModel.setMamaUid(mamaUid)
    }

    Scaffold(
        containerColor = EmbFondo,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Expediente",
                        fontWeight = FontWeight.Bold,
                        color = EmbRosaOscuro,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmbRosa.copy(alpha = 0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Regresar",
                                tint = EmbRosaOscuro,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        if (uiState.cargando) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmbRosa, strokeWidth = 3.dp)
            }
            return@Scaffold
        }

        val perfil = uiState.perfil

        if (perfil == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.FolderOff, null, tint = Color(0xFFBDBDBD), modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("No se pudo cargar el perfil de embarazo.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── HEADER CARD (estilo dashboard embarazo) ───────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(20.dp)) {

                    // Nombre + avatar
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(EmbRosa.copy(alpha = 0.15f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Female,
                                null,
                                tint = EmbRosaOscuro,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                mamaNombre,
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color(0xFF212121)
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Spa,
                                    null,
                                    tint = EmbRosaOscuro,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "Semana ${perfil.semanas} · Trimestre ${
                                        when { perfil.semanas <= 13 -> 1; perfil.semanas <= 26 -> 2; else -> 3 }
                                    }",
                                    fontSize = 13.sp,
                                    color = EmbRosaOscuro,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Barra de progreso gestacional
                    val progress = (perfil.semanas / 40f).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = EmbRosa,
                        trackColor = EmbRosa.copy(alpha = 0.12f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Sem. 1", fontSize = 9.sp, color = Color.LightGray)
                        Text("Sem. 40", fontSize = 9.sp, color = Color.LightGray)
                    }

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFFF0F0F0))
                    Spacer(Modifier.height(16.dp))

                    // Stats row con icon backdrops
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ExpedienteStatChip(
                            icon = Icons.Rounded.Cake,
                            value = "${perfil.edad} años",
                            label = "Edad",
                            iconTint = EmbMorado,
                            bgColor = EmbMorado.copy(alpha = 0.10f)
                        )
                        ExpedienteStatChip(
                            icon = Icons.Rounded.Height,
                            value = String.format(java.util.Locale.US, "%.2f m", perfil.tallaM),
                            label = "Estatura",
                            iconTint = EmbTeal,
                            bgColor = EmbTeal.copy(alpha = 0.10f)
                        )
                        ExpedienteStatChip(
                            icon = Icons.Rounded.MonitorWeight,
                            value = String.format(java.util.Locale.US, "%.1f kg", perfil.pesoPregestacionalKg),
                            label = "Peso previo",
                            iconTint = EmbRosaOscuro,
                            bgColor = EmbRosa.copy(alpha = 0.12f)
                        )
                        ExpedienteStatChip(
                            icon = Icons.Rounded.Analytics,
                            value = String.format(java.util.Locale.US, "%.1f", perfil.imcPregestacional),
                            label = "IMC prev.",
                            iconTint = Color(0xFFF57C00),
                            bgColor = Color(0xFFFFF3E0)
                        )
                    }

                    // Condiciones de riesgo (chips)
                    if (perfil.condiciones.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.MedicalServices,
                                null,
                                tint = EmbRosaOscuro,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Condiciones de Riesgo",
                                fontSize = 11.sp,
                                color = EmbRosaOscuro,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            perfil.condiciones.forEach { cond ->
                                Surface(
                                    color = Color(0xFFFCE4EC),
                                    shape = RoundedCornerShape(20.dp),
                                    border = BorderStroke(1.dp, Color(0xFFF8BBD0))
                                ) {
                                    Text(
                                        text = cond,
                                        fontSize = 11.sp,
                                        color = Color(0xFFC2185B),
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── TABS (estilo pill / capsule) ──────────────────────────────────
            val tabs = listOf("Síntomas", "Crecimiento", "Citas")
            val tabIcons = listOf(Icons.Rounded.Favorite, Icons.Rounded.ShowChart, Icons.Rounded.CalendarToday)

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabs.forEachIndexed { idx, label ->
                        val selected = tabSeleccionado == idx
                        Surface(
                            onClick = { tabSeleccionado = idx },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) EmbRosaOscuro else Color.Transparent
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    tabIcons[idx],
                                    null,
                                    tint = if (selected) Color.White else Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    label,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── TAB CONTENT ────────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (tabSeleccionado) {
                    0 -> TabSintomas(uiState.registrosSintomas)
                    1 -> TabCrecimiento(perfil = perfil, registrosPeso = uiState.registrosPeso)
                    2 -> TabCitas(citas = uiState.citas)
                }
            }
        }
    }
}

// ── Stat chip reutilizable ─────────────────────────────────────────────────────
@Composable
private fun ExpedienteStatChip(
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color,
    bgColor: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(bgColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        Text(label, fontSize = 9.sp, color = Color.Gray)
    }
}

// ── TAB: Síntomas ─────────────────────────────────────────────────────────────
@Composable
private fun TabSintomas(registros: List<RegistroSintomasEmbarazo>) {
    if (registros.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(EmbRosa.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = EmbRosa, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Sin síntomas registrados", fontWeight = FontWeight.Bold, color = Color(0xFF424242))
                Text("La paciente no ha registrado síntomas.", fontSize = 12.sp, color = Color.Gray)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val ordenadas = registros.sortedByDescending { it.creadoEn }
        items(ordenadas) { reg ->
            val trimestre = when {
                reg.semanaGestacion <= 13 -> 1
                reg.semanaGestacion <= 26 -> 2
                else -> 3
            }
            val analisis = reg.sintomas.map { SintomasAnalyzer.analizarSintoma(it, trimestre) }
            val esGrave = analisis.any { it.nivel == NivelSintoma.URGENCIA }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(if (esGrave) 4.dp else 2.dp),
                border = if (esGrave) BorderStroke(1.5.dp, Color(0xFFEF9A9A)) else null
            ) {
                Column(Modifier.padding(16.dp)) {
                    // Header row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (esGrave) Color(0xFFFFEBEE) else EmbRosaClaro,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    if (esGrave) Icons.Rounded.Warning else Icons.Rounded.Favorite,
                                    null,
                                    tint = if (esGrave) Color(0xFFC62828) else EmbRosaOscuro,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Semana ${reg.semanaGestacion}",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF212121),
                                fontSize = 14.sp
                            )
                        }
                        Surface(
                            color = EmbRosa.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                reg.fecha,
                                fontSize = 11.sp,
                                color = EmbRosaOscuro,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Síntomas seleccionados
                    if (reg.sintomas.isNotEmpty()) {
                        Text("Síntomas", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            reg.sintomas.forEach { s ->
                                Surface(
                                    color = EmbRosaClaro,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        s,
                                        fontSize = 10.sp,
                                        color = EmbRosaOscuro,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (reg.otrosSintomasTexto.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Descripción libre", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(reg.otrosSintomasTexto, fontSize = 13.sp, color = Color(0xFF424242))
                    }

                    // Análisis clínico
                    if (analisis.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = Color(0xFFF0F0F0))
                        Spacer(Modifier.height(10.dp))
                        Text("Análisis clínico", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(6.dp))
                        analisis.forEach { det ->
                            val esAlarma = det.nivel == NivelSintoma.URGENCIA
                            Surface(
                                color = if (esAlarma) Color(0xFFFFEBEE) else Color(0xFFE8F5E9),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (esAlarma) Icons.Rounded.Warning else Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = if (esAlarma) Color(0xFFC62828) else Color(0xFF2E7D32),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            det.nombreEs,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (esAlarma) Color(0xFFC62828) else Color(0xFF2E7D32)
                                        )
                                        Text(
                                            det.detalleEs,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = if (esAlarma) Color(0xFFB71C1C) else Color(0xFF388E3C)
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
}

// ── TAB: Crecimiento y Peso ────────────────────────────────────────────────────
@Composable
private fun TabCrecimiento(perfil: PerfilEmbarazo, registrosPeso: List<RegistroPesoEmbarazo>) {
    if (registrosPeso.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(EmbTeal.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ShowChart, null, tint = EmbTeal, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Sin registros de peso", fontWeight = FontWeight.Bold, color = Color(0xFF424242))
                Text("No hay mediciones guardadas aún.", fontSize = 12.sp, color = Color.Gray)
            }
        }
        return
    }

    val scrollState = rememberScrollState()
    val imc = perfil.imcPregestacional
    val rango = GananciaPesoCalculator.rangoAjustado(imc, perfil.edad, perfil.tallaM)
    val ultimoReg = registrosPeso.maxByOrNull { it.semanaGestacion }
    val ganancia = if (ultimoReg != null) ultimoReg.pesoActualKg - perfil.pesoPregestacionalKg else 0.0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .verticalScroll(scrollState)
    ) {
        Spacer(Modifier.height(4.dp))

        // KPI cards fila
        if (ultimoReg != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmbRosa.copy(0.12f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MonitorWeight, null, tint = EmbRosaOscuro, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            String.format(java.util.Locale.US, "%.1f kg", ultimoReg.pesoActualKg),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = Color(0xFF212121)
                        )
                        Text("Peso actual", fontSize = 10.sp, color = Color.Gray)
                        Text("Sem. ${ultimoReg.semanaGestacion}", fontSize = 9.sp, color = EmbRosaOscuro, fontWeight = FontWeight.SemiBold)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(EmbTeal.copy(0.10f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.TrendingUp, null, tint = EmbTeal, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            String.format(java.util.Locale.US, "%+.1f kg", ganancia),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = if (ganancia >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                        Text("Ganancia total", fontSize = 10.sp, color = Color.Gray)
                        Text("NOM-007", fontSize = 9.sp, color = EmbTeal, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
        }

        // Gráfica de Ganancia de Peso
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(EmbRosa.copy(0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ShowChart, null, tint = EmbRosaOscuro, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Ganancia de Peso", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF212121))
                        Text("Rango NOM-007", fontSize = 10.sp, color = Color.Gray)
                    }
                }
                Spacer(Modifier.height(12.dp))

                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                        .border(BorderStroke(1.dp, Color(0xFFECECEC)), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    val w = size.width; val h = size.height
                    val paddingLeft = 32.dp.toPx(); val paddingBottom = 24.dp.toPx()
                    val graphW = w - paddingLeft; val graphH = h - paddingBottom
                    val minX = 1f; val maxX = 40f; val minY = -2f; val maxY = 20f

                    fun getX(x: Float) = paddingLeft + ((x - minX) / (maxX - minX)) * graphW
                    fun getY(y: Float) = graphH - ((y - minY) / (maxY - minY)) * graphH

                    listOf(-2f, 0f, 5f, 10f, 15f, 20f).forEach { y ->
                        drawLine(Color(0xFFE0E0E0), androidx.compose.ui.geometry.Offset(paddingLeft, getY(y)), androidx.compose.ui.geometry.Offset(w, getY(y)), 1f)
                    }
                    listOf(1f, 10f, 20f, 30f, 40f).forEach { x ->
                        drawLine(Color(0xFFE0E0E0), androidx.compose.ui.geometry.Offset(getX(x), 0f), androidx.compose.ui.geometry.Offset(getX(x), graphH), 1f)
                    }

                    clipRect(paddingLeft, 0f, w, graphH) {
                        if (!perfil.esGemelar) {
                            val bandPath = androidx.compose.ui.graphics.Path()
                            bandPath.moveTo(getX(13f), getY(1.5f))
                            bandPath.lineTo(getX(40f), getY(rango.maxKg.toFloat()))
                            bandPath.lineTo(getX(40f), getY(rango.minKg.toFloat()))
                            bandPath.lineTo(getX(13f), getY(0.5f))
                            bandPath.close()
                            drawPath(bandPath, color = Color(0xFFE8F5E9).copy(alpha = 0.8f))
                        }
                        val sorted = registrosPeso.sortedBy { it.semanaGestacion }
                        val points = sorted.map { reg ->
                            val gain = (reg.pesoActualKg - perfil.pesoPregestacionalKg).toFloat().coerceIn(minY, maxY)
                            val week = reg.semanaGestacion.toFloat().coerceIn(minX, maxX)
                            androidx.compose.ui.geometry.Offset(getX(week), getY(gain))
                        }
                        for (i in 0 until points.size - 1) {
                            drawLine(EmbRosaOscuro, points[i], points[i + 1], 3.dp.toPx())
                        }
                        points.forEach { pt ->
                            drawCircle(EmbRosaOscuro, 5.dp.toPx(), pt)
                            drawCircle(Color.White, 2.5.dp.toPx(), pt)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("Sem. 1","Sem. 10","Sem. 20","Sem. 30","Sem. 40").forEach {
                        Text(it, fontSize = 9.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // Gráfica de Altura Uterina
        val registrosConAltura = registrosPeso.filter { it.alturaUterinaCm != null }
        if (registrosConAltura.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(EmbTeal.copy(0.10f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Timeline, null, tint = EmbTeal, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Altura Uterina", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF212121))
                            Text("Dispersión vs semana gestacional", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(Color(0xFFFAFAFA), RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, Color(0xFFECECEC)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        val w = size.width; val h = size.height
                        val paddingLeft = 32.dp.toPx(); val paddingBottom = 24.dp.toPx()
                        val graphW = w - paddingLeft; val graphH = h - paddingBottom
                        val minX = 20f; val maxX = 40f; val minY = 15f; val maxY = 40f

                        fun getX(x: Float) = paddingLeft + ((x - minX) / (maxX - minX)) * graphW
                        fun getY(y: Float) = graphH - ((y - minY) / (maxY - minY)) * graphH

                        listOf(15f,20f,25f,30f,35f,40f).forEach { y ->
                            drawLine(Color(0xFFE0E0E0), androidx.compose.ui.geometry.Offset(paddingLeft, getY(y)), androidx.compose.ui.geometry.Offset(w, getY(y)), 1f)
                        }
                        listOf(20f,25f,30f,35f,40f).forEach { x ->
                            drawLine(Color(0xFFE0E0E0), androidx.compose.ui.geometry.Offset(getX(x), 0f), androidx.compose.ui.geometry.Offset(getX(x), graphH), 1f)
                        }

                        clipRect(paddingLeft, 0f, w, graphH) {
                            val bandPath = androidx.compose.ui.graphics.Path()
                            bandPath.moveTo(getX(20f), getY(22.5f)); bandPath.lineTo(getX(40f), getY(42.5f))
                            bandPath.lineTo(getX(40f), getY(37.5f)); bandPath.lineTo(getX(20f), getY(17.5f))
                            bandPath.close()
                            drawPath(bandPath, Color(0xFF81C784).copy(alpha = 0.2f))

                            registrosConAltura.forEach { rec ->
                                val rx = rec.semanaGestacion.toFloat()
                                val ry = rec.alturaUterinaCm?.toFloat() ?: 0f
                                if (rx in minX..maxX && ry in minY..maxY) {
                                    drawCircle(Color(0xFF4DB6AC), 5.dp.toPx(), androidx.compose.ui.geometry.Offset(getX(rx), getY(ry)))
                                    drawCircle(Color.White, 2.dp.toPx(), androidx.compose.ui.geometry.Offset(getX(rx), getY(ry)))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("Sem. 20","Sem. 25","Sem. 30","Sem. 35","Sem. 40").forEach {
                            Text(it, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Tabla historial
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(EmbMorado.copy(0.10f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.TableChart, null, tint = EmbMorado, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text("Historial de Mediciones", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF212121))
                }
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF0F0F0))
                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Semana", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                    Text("Peso (kg)", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                    Text("Alt. Uter.", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                }
                Spacer(Modifier.height(8.dp))

                registrosPeso.sortedByDescending { it.semanaGestacion }.forEach { rec ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = EmbRosa.copy(0.08f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                "Sem. ${rec.semanaGestacion}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmbRosaOscuro,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        Text(String.format(java.util.Locale.US, "%.1f kg", rec.pesoActualKg), fontSize = 13.sp, color = Color(0xFF212121), modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Text(
                            if (rec.alturaUterinaCm != null) "${rec.alturaUterinaCm} cm" else "—",
                            fontSize = 13.sp,
                            color = if (rec.alturaUterinaCm != null) EmbTeal else Color.LightGray,
                            fontWeight = if (rec.alturaUterinaCm != null) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.End
                        )
                    }
                    HorizontalDivider(color = Color(0xFFF5F5F5))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

// ── TAB: Citas ─────────────────────────────────────────────────────────────────
@Composable
private fun TabCitas(citas: List<CitaEmbarazo>) {
    if (citas.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(EmbTeal.copy(alpha = 0.10f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.CalendarToday, null, tint = EmbTeal, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Sin citas agendadas", fontWeight = FontWeight.Bold, color = Color(0xFF424242))
                Text("No se han registrado citas médicas.", fontSize = 12.sp, color = Color.Gray)
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val ordenadas = citas.sortedByDescending { it.fecha }
        items(ordenadas) { cita ->
            val esVideo = cita.tipo == "TELECONSULTA"
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                if (esVideo) EmbRosa.copy(0.12f) else EmbTeal.copy(0.12f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (esVideo) Icons.Rounded.Videocam else Icons.Rounded.LocationOn,
                            null,
                            tint = if (esVideo) EmbRosaOscuro else EmbTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                cita.fecha,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF212121)
                            )
                            Surface(
                                color = if (esVideo) EmbRosa.copy(0.12f) else EmbTeal.copy(0.12f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    if (esVideo) "Video" else "Presencial",
                                    fontSize = 10.sp,
                                    color = if (esVideo) EmbRosaOscuro else EmbTeal,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccessTime, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(cita.hora, fontSize = 12.sp, color = Color.Gray)
                        }
                        if (cita.motivo.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(cita.motivo, fontSize = 12.sp, color = Color(0xFF616161), maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
