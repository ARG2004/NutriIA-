package com.example.nutriia.expediente

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import com.example.nutriia.crecimiento.MedicionCrecimiento
import com.example.nutriia.crecimiento.interpretarIMC
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.shared.NutriSharedViewModel
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RecetaMexicana
import com.example.nutriia.sueldo.TipoComida
import com.example.nutriia.utils.FechaUtils
import java.text.SimpleDateFormat
import java.util.*

// ─── Paleta de Colores Premium NutriIA (Estilos Dashboard) ───────────────────
private val EGreen     = Color(0xFF4CAF50) // DashNutriaGreen
private val EDarkGreen = Color(0xFF1B5E20) // DashNutriaDarkGreen
private val EBgCrema   = Color(0xFFF9F8F4) // DashBgCrema
private val ECardWhite = Color.White
private val EPurple    = Color(0xFF9C8FE0) // DashSoftPurple
private val ETeal      = Color(0xFF4DB6AC) // DashSoftTeal
private val EOrange    = Color(0xFFFF8F00) // DashOrange
private val EBgAlim    = Color(0xFFF9F8F4) // DashBgCrema

private val avatarPool = listOf(
    Color(0xFFEC9BBF), Color(0xFF9C8FE0), Color(0xFFFFAB76),
    Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFF64B5F6)
)

// ─── Colores / iconos por tipo de comida ─────────────────────────────────────
private fun colorParaTipo(tipo: TipoComida) = when (tipo) {
    TipoComida.DESAYUNO -> Color(0xFFFFB300)
    TipoComida.COMIDA   -> EOrange
    TipoComida.CENA     -> Color(0xFF7986CB)
    TipoComida.COLACION -> EGreen
}
private fun iconParaTipo(tipo: TipoComida) = when (tipo) {
    TipoComida.DESAYUNO -> Icons.Rounded.WbSunny
    TipoComida.COMIDA   -> Icons.Rounded.Restaurant
    TipoComida.CENA     -> Icons.Rounded.Nightlight
    TipoComida.COLACION -> Icons.Rounded.EmojiFoodBeverage
}
private fun labelParaTipo(tipo: TipoComida) = when (tipo) {
    TipoComida.DESAYUNO -> "Desayuno"
    TipoComida.COMIDA   -> "Comida"
    TipoComida.CENA     -> "Cena"
    TipoComida.COLACION -> "Colación"
}

// ════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL
// ════════════════════════════════════════════════════════════════════════════

@Composable
fun PacienteExpedienteScreen(
    ownerUid:        String,
    childId:         String,
    childNombre:     String,
    padreNombre:     String,
    onBack:          () -> Unit,
    sharedViewModel: NutriSharedViewModel? = null,
    viewModel:       PacienteExpedienteViewModel = viewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val esBlind = a11yMode == AccessibilityMode.BLIND

    LaunchedEffect(childNombre) {
        if (esBlind) {
            a11yVm.hablar("Abriendo expediente clínico del paciente $childNombre. Padre o tutor: $padreNombre. Aquí puedes revisar la biometría, historial de crecimiento e introducir observaciones y planes de alimentación.")
        }
    }

    LaunchedEffect(childId) {
        viewModel.cargar(
            ownerUid        = ownerUid,
            childId         = childId,
            childNombre     = childNombre,
            padreNombre     = padreNombre,
            sharedViewModel = sharedViewModel
        )
    }

    LaunchedEffect(ui.error) {
        ui.error?.let {
            snackbar.showSnackbar("Error: $it")
            viewModel.limpiarMensajes()
        }
    }
    LaunchedEffect(ui.exito) {
        ui.exito?.let {
            snackbar.showSnackbar(it)
            viewModel.limpiarMensajes()
        }
    }

    val meses = ui.edadMeses

    val etapaLabel = when {
        meses < 6  -> "Lactancia exclusiva"
        meses < 12 -> "Iniciando sólidos"
        else       -> "Alimentación familiar"
    }

    val avatarColor = avatarPool[
        childId.hashCode().let { if (it < 0) -it else it } % avatarPool.size
    ]

    Scaffold(
        containerColor = if (ui.tabSeleccionado == 0) EBgCrema else EBgAlim,
        snackbarHost   = { SnackbarHost(snackbar) }
    ) { padding ->

        if (ui.cargando) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator(color = EGreen)
            }
            return@Scaffold
        }

        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // ── Header ───────────────────────────────────────────────────
            item {
                ExpedienteHeader(
                    childNombre  = childNombre,
                    padreNombre  = padreNombre,
                    meses        = meses,
                    etapaLabel   = etapaLabel,
                    hasAllergies = ui.hasAllergies,
                    avatarColor  = avatarColor,
                    birthDate    = ui.birthDate,
                    nivelIngreso = ui.nivelIngreso,
                    onBack       = onBack
                )
            }

            // ── Tabs ──────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                ExpedienteTabs(
                    selected = ui.tabSeleccionado,
                    onSelect = { viewModel.seleccionarTab(it) }
                )
                Spacer(Modifier.height(8.dp))
            }

            // ── Contenido según tab ───────────────────────────────────────
            if (ui.tabSeleccionado == 0) {

                // ╔══════════════════════════════╗
                // ║       TAB 0 — RESUMEN        ║
                // ╚══════════════════════════════╝

                item {
                    Spacer(Modifier.height(12.dp))
                    SeccionTitulo(Icons.Rounded.Assessment, "Biometría")
                    Spacer(Modifier.height(12.dp))
                    BiometriaCard(
                        meses              = ui.edadMeses,
                        etapaLabel         = etapaLabel,
                        weightKg           = ui.weightKg,
                        heightCm           = ui.heightCm,
                        ultimaMedicionCrec = ui.ultimaMedicionCrec
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    SeccionTitulo(Icons.Rounded.Timeline, "Historial de Crecimiento e IMC")
                    Spacer(Modifier.height(12.dp))
                    HistorialCrecimientoExpediente(
                        historial = ui.historialCrecimiento,
                        meses = ui.edadMeses
                    )
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    SeccionTitulo(Icons.Rounded.RestaurantMenu, "Alimentos Registrados")
                    Text(
                        "Historial de introducción de sólidos del paciente.",
                        fontSize = 11.sp, color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                }

                if (ui.alimentosIntrod.isEmpty()) {
                    item { EmptyState("No hay alimentos registrados.") }
                } else {
                    items(ui.alimentosIntrod, key = { it.nombre + it.fechaMs }) { alimento -> AlimentoRegistradoRow(alimento) }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    SeccionTitulo(Icons.Rounded.HistoryEdu, "Notas Clínicas")
                    Spacer(Modifier.height(12.dp))
                }

                if (ui.notasConsulta.isEmpty()) {
                    item { EmptyState("Sin historial de notas.") }
                } else {
                    items(ui.notasConsulta, key = { it.id.ifBlank { it.fechaMs.toString() } }) { nota -> NotaCard(nota) }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    AnimatedVisibility(!ui.mostrarFormaNota) {
                        Button(
                            onClick  = { viewModel.mostrarFormaNota() },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(52.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = EGreen),
                            shape    = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Nueva Nota", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                item {
                    AnimatedVisibility(ui.mostrarFormaNota) {
                        FormaNota(
                            guardando  = ui.guardandoNota,
                            onGuardar  = { texto -> viewModel.guardarNota(ownerUid, childId, texto) },
                            onCancelar = { viewModel.ocultarFormaNota() }
                        )
                    }
                }

            } else {

                // ╔═══════════════════════════════════╗
                // ║    TAB 1 — ALIMENTACIÓN           ║
                // ╚═══════════════════════════════════╝

                item {
                    Spacer(Modifier.height(12.dp))
                    EtapaBanner(meses = meses, etapa = etapaLabel)
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    SeccionTituloNaranja(Icons.Rounded.CalendarMonth, "Plan Semanal del Motor")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Generado por DietaEngine según alimentos tolerados y edad.",
                        fontSize = 11.sp, color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (ui.planSemanal.isEmpty()) {
                    item { EmptyState("Sin plan generado. El niño aún no tiene alimentos registrados.") }
                } else {
                    items(ui.planSemanal, key = { it.diaSemana }) { dia -> PlanDiaCard(dia) }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    SeccionTituloNaranja(Icons.AutoMirrored.Rounded.MenuBook, "Recetas Sugeridas para ${meses}m")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Filtradas por edad, región Puebla y alimentos disponibles.",
                        fontSize = 11.sp, color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (ui.recetasSugeridas.isEmpty()) {
                    item { EmptyState("No hay recetas disponibles para esta etapa.") }
                } else {
                    items(ui.recetasSugeridas, key = { it.nombre }) { receta -> RecetaSugeridaCard(receta) }
                }

                item {
                    Spacer(Modifier.height(24.dp))
                    SeccionTituloNaranja(Icons.Rounded.HistoryEdu, "Mis Indicaciones de Alimentación")
                    Spacer(Modifier.height(10.dp))
                }

                if (ui.entradasAlimentacion.isEmpty()) {
                    item { EmptyState("Aún no has agregado recetas ni observaciones.") }
                } else {
                    items(ui.entradasAlimentacion, key = { it.id.ifBlank { it.fechaMs.toString() } }) { entrada ->
                        EntradaAlimentacionCard(
                            entrada    = entrada,
                            onEliminar = { viewModel.eliminarEntradaAlimentacion(entrada.id, entrada.tipo, entrada.titulo) }
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(20.dp))
                    AnimatedVisibility(!ui.mostrarFormaAlim) {
                        Column(
                            modifier            = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick  = { viewModel.mostrarFormaAlim("receta_nutriologo") },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors   = ButtonDefaults.buttonColors(containerColor = EOrange),
                                shape    = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.Add, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Agregar Receta Personalizada", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick  = { viewModel.mostrarFormaAlim("observacion_alimentacion") },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                border   = BorderStroke(1.5.dp, EGreen),
                                shape    = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Rounded.EditNote, null, tint = EGreen)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Agregar Observación Nutricional",
                                    fontWeight = FontWeight.Bold,
                                    color      = EGreen
                                )
                            }
                        }
                    }
                }

                item {
                    AnimatedVisibility(ui.mostrarFormaAlim) {
                        if (ui.tipoEntradaAlim == "receta_nutriologo") {
                            FormaRecetaPersonalizada(
                                guardando  = ui.guardandoEntradaAlim,
                                onGuardar  = { titulo, ingredientes, preparacion, tipoComida, kcal ->
                                    viewModel.guardarRecetaPersonalizada(titulo, ingredientes, preparacion, tipoComida, kcal)
                                },
                                onCancelar = { viewModel.ocultarFormaAlim() }
                            )
                        } else {
                            FormaAlimentacion(
                                tipo       = ui.tipoEntradaAlim,
                                guardando  = ui.guardandoEntradaAlim,
                                onGuardar  = { titulo, contenido ->
                                    viewModel.guardarEntradaAlimentacion(titulo, contenido, ui.tipoEntradaAlim)
                                },
                                onCancelar = { viewModel.ocultarFormaAlim() }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// COMPONENTES COMPARTIDOS
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun ExpedienteHeader(
    childNombre:  String,
    padreNombre:  String,
    meses:        Int,
    etapaLabel:   String,
    hasAllergies: Boolean,
    avatarColor:  Color,
    birthDate:    String,        // ← NUEVO
    nivelIngreso: NivelIngreso,  // ← NUEVO
    onBack:       () -> Unit
) {
    // Etiqueta amigable para nivel de ingreso
    val nivelLabel = when (nivelIngreso) {
        NivelIngreso.BASICO -> "Nivel Básico"
        NivelIngreso.MEDIO  -> "Nivel Medio"
        NivelIngreso.ALTO   -> "Nivel Alto"
        else -> "Nivel Básico"
    }

    // Formatear fecha de nacimiento de forma legible
    val nacimientoStr = remember(birthDate) {
        if (birthDate.isBlank()) return@remember ""
        val formatos = listOf(
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            SimpleDateFormat("MM/dd/yyyy", Locale.getDefault())
        )
        for (fmt in formatos) {
            try {
                fmt.isLenient = false
                val date = fmt.parse(birthDate) ?: continue
                return@remember SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-MX")).format(date)
            } catch (_: Exception) { continue }
        }
        birthDate // fallback: mostrar tal cual
    }

    // Header con degradado premium verde y esquinas inferiores redondeadas
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        EDarkGreen, // Color(0xFF1B5E20)
                        Color(0xFF2E7D32)  // Medium green
                    )
                )
            )
            .padding(start = 16.dp, end = 20.dp, top = 48.dp, bottom = 24.dp)
    ) {
        Column {
            // Botón de atrás con estilo circular traslúcido (Glassmorphism)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Volver",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Avatar circular premium con borde y sombra suave
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(avatarColor.copy(alpha = 0.25f))
                        .border(2.5.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        childNombre.take(1).uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize   = 28.sp,
                        color      = Color.White
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    // Nombre del niño
                    Text(
                        childNombre,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Black,
                        color      = Color.White,
                        lineHeight = 28.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    // Tutor
                    Text(
                        "Tutor: $padreNombre",
                        fontSize = 13.sp,
                        color    = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    // Fecha de nacimiento
                    if (nacimientoStr.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Cake,
                                contentDescription = null,
                                tint     = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                nacimientoStr,
                                fontSize = 12.sp,
                                color    = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    // Chips de estado elegantes
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.horizontalScroll(rememberScrollState())
                    ) {
                        ChipHeader("$meses meses")
                        ChipHeader(etapaLabel)
                        ChipHeader(nivelLabel, Color(0xFF80DEEA))
                        if (hasAllergies) ChipHeader("Alergias", Color(0xFFFFAB91))
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpedienteTabs(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Resumen",      Icons.Rounded.Person,         0),
        Triple("Alimentación", Icons.Rounded.RestaurantMenu, 1)
    )
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tabs.forEach { (label, icon, i) ->
            val sel = selected == i
            val bg  by animateColorAsState(
                if (sel) if (i == 0) EDarkGreen else EOrange else Color.White,
                tween(220), label = "tab$i"
            )
            val fg  by animateColorAsState(
                if (sel) Color.White else if (i == 0) EDarkGreen else EOrange,
                tween(220), label = "tabfg$i"
            )
            val isFirst = i == 0
            val shape = RoundedCornerShape(20.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .shadow(if (sel) 4.dp else 1.dp, shape)
                    .clip(shape)
                    .background(bg)
                    .border(
                        1.5.dp,
                        if (sel) Color.Transparent
                        else if (isFirst) EDarkGreen.copy(.25f) else EOrange.copy(.25f),
                        shape
                    )
                    .clickable { onSelect(i) }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                    Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            }
        }
    }
}

@Composable
private fun ChipHeader(label: String, color: Color = Color.White) {
    Surface(
        shape  = RoundedCornerShape(10.dp),
        color  = color.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            label,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            color      = Color.White,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun SeccionTitulo(icon: ImageVector, titulo: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = EGreen, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = EDarkGreen)
    }
}

@Composable
private fun SeccionTituloNaranja(icon: ImageVector, titulo: String) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = EOrange, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFBF360C))
    }
}

@Composable
private fun MedidaCard(modifier: Modifier, valor: String, label: String, color: Color) {
    val icon = when (label) {
        "Peso"  -> Icons.Rounded.Scale
        "Talla" -> Icons.Rounded.Straighten
        else    -> Icons.Rounded.CalendarMonth
    }
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier            = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(valor, fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF212121))
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun BiometriaCard(
    meses: Int,
    etapaLabel: String,
    weightKg: Double,
    heightCm: Double,
    ultimaMedicionCrec: String
) {
    // Calcular edad legible en años y meses
    val edadTexto = remember(meses) {
        val years = meses / 12
        val months = meses % 12
        when {
            years > 0 -> "${years} año${if (years > 1) "s" else ""}${if (months > 0) " y $months mes${if (months > 1) "es" else ""}" else ""}"
            else -> "${months} mes${if (months > 1) "es" else ""}"
        }
    }

    val tieneMedicion = ultimaMedicionCrec.isNotBlank()
    val fechaMedicion = remember(ultimaMedicionCrec) {
        if (tieneMedicion && ultimaMedicionCrec.contains("—")) {
            ultimaMedicionCrec.split("—").lastOrNull()?.trim() ?: ""
        } else ""
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(32.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icono de etapa circular verde suave
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ChildCare,
                    contentDescription = null,
                    tint = EGreen,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            // Nombre de la etapa
            Text(
                etapaLabel,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = EDarkGreen
            )
            // Edad legible
            Text(
                edadTexto,
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(20.dp))
            // Peso y Talla con separador vertical
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Peso
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Scale,
                        contentDescription = null,
                        tint = EPurple,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${weightKg} kg",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF212121)
                    )
                    Text(
                        "Peso",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Divisor vertical
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(Color(0xFFE0E0E0))
                )

                // Talla
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.Straighten,
                        contentDescription = null,
                        tint = ETeal,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${heightCm} cm",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = Color(0xFF212121)
                    )
                    Text(
                        "Talla",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Información de la última medición (si existe)
            if (tieneMedicion && fechaMedicion.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.CloudDone,
                        contentDescription = null,
                        tint = EGreen,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Última medición: $fechaMedicion",
                        fontSize = 11.sp,
                        color = EGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.PersonOutline,
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Datos del perfil base",
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun HistorialCrecimientoExpediente(
    historial: List<MedicionCrecimiento>,
    meses: Int
) {
    if (historial.isEmpty()) {
        Card(
            modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = ECardWhite),
            border    = BorderStroke(1.dp, Color(0xFFEEEEEE)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Box(Modifier.fillMaxWidth().padding(24.dp), Alignment.Center) {
                Text(
                    "No se han registrado mediciones en el historial de crecimiento.",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val ultimas = remember(historial) { historial.take(6) }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(
                "Últimas mediciones",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = Color.Gray
            )
            Spacer(Modifier.height(10.dp))
            ultimas.forEachIndexed { idx, m ->
                val interp = remember(m.imc) { interpretarIMC(m.imc, meses) }
                val style = remember(interp.categoria) {
                    when {
                        interp.categoria.contains("Bajo")   -> Triple(ETeal, Color(0xFFE0F2F1), Icons.AutoMirrored.Rounded.TrendingDown)
                        interp.categoria.contains("Normal") -> Triple(EGreen, Color(0xFFE8F5E9), Icons.Rounded.CheckCircle)
                        interp.categoria.contains("Riesgo") -> Triple(EOrange, Color(0xFFFFF3E0), Icons.Rounded.Warning)
                        else                                -> Triple(Color(0xFFE53935), Color(0xFFFFEBEE), Icons.AutoMirrored.Rounded.TrendingUp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contenedor del Icono
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(style.second),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            style.third,
                            contentDescription = null,
                            tint = style.first,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    // Peso y Talla + Fecha
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${m.pesoKg} kg · ${m.tallaCm} cm",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF212121)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            m.fecha,
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    // IMC & Categoría
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "IMC: ${"%.1f".format(m.imc)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = style.first
                        )
                        Spacer(Modifier.height(2.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = style.first.copy(.10f)
                        ) {
                            Text(
                                interp.categoria,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = style.first,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                if (idx < ultimas.size - 1) {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                }
            }
        }
    }
}

private fun obtenerIconoAlimento(nombre: String): ImageVector {
    val n = nombre.lowercase()
    return when {
        n.contains("manzana") || n.contains("papaya") || n.contains("pera") || 
        n.contains("guayaba") || n.contains("plátano") || n.contains("platano") ||
        n.contains("fruta") -> Icons.Rounded.Eco
        
        n.contains("zanahoria") || n.contains("calabacita") || n.contains("calabaza") || 
        n.contains("chayote") || n.contains("chícharo") || n.contains("chicharo") || 
        n.contains("nopal") || n.contains("verdura") -> Icons.Rounded.Grass
        
        n.contains("pollo") || n.contains("res") || n.contains("carne") -> Icons.Rounded.Restaurant
        n.contains("pescado") || n.contains("sierra") || n.contains("sardina") || 
        n.contains("mojarra") || n.contains("atún") || n.contains("atun") -> Icons.Rounded.SetMeal
        
        n.contains("huevo") -> Icons.Rounded.Egg
        
        n.contains("avena") || n.contains("amaranto") || n.contains("arroz") || 
        n.contains("tortilla") || n.contains("trigo") || n.contains("maíz") || 
        n.contains("maiz") -> Icons.Rounded.Grain
        
        n.contains("leche") || n.contains("yogur") || n.contains("yogurt") || 
        n.contains("queso") || n.contains("lácteo") || n.contains("lacteo") -> Icons.Rounded.LocalCafe
        
        else -> Icons.Rounded.Dining
    }
}

@Composable
private fun AlimentoRegistradoRow(alimento: AlimentoIntroducido) {
    val (colorPrincipal, colorBg, badgeLabel) = when (alimento.estado) {
        "Aceptado"  -> Triple(EGreen, Color(0xFFE8F5E9), "Aceptado")
        "Rechazado" -> Triple(Color(0xFFE53935), Color(0xFFFFEBEE), "Rechazado")
        else        -> Triple(EOrange, Color(0xFFFFF3E0), "En prueba")
    }

    val icon = remember(alimento.nombre) { obtenerIconoAlimento(alimento.nombre) }
    val fechaIntroduccion = remember(alimento.fechaMs) {
        if (alimento.fechaMs == 0L) ""
        else FechaUtils.formatearFecha(alimento.fechaMs)
    }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor del Icono con fondo de color suave según el estado
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = colorPrincipal,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            // Nombre y Fecha
            Column(Modifier.weight(1f)) {
                Text(
                    alimento.nombre.replaceFirstChar { it.uppercase() },
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = Color(0xFF212121)
                )
                if (fechaIntroduccion.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "Introducido el $fechaIntroduccion",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            // Badge del Estado
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorPrincipal.copy(.12f)
            ) {
                Text(
                    badgeLabel,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Black,
                    color      = colorPrincipal,
                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun NotaCard(nota: NotaConsulta) {
    val fecha = remember(nota.fechaMs) {
        if (nota.fechaMs == 0L) ""
        else FechaUtils.formatearFecha(nota.fechaMs)
    }
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // Barra lateral izquierda decorativa
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(96.dp)
                    .background(EDarkGreen)
            )
            Column(Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        nota.autorNombre,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize   = 14.sp,
                        color      = EDarkGreen,
                        modifier   = Modifier.weight(1f)
                    )
                    Text(fecha, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    nota.texto,
                    fontSize = 13.sp,
                    color    = Color(0xFF424242),
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun FormaNota(guardando: Boolean, onGuardar: (String) -> Unit, onCancelar: () -> Unit) {
    var texto by remember { mutableStateOf("") }
    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esBlind = a11yMode == AccessibilityMode.BLIND
    val esMute = a11yMode == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Nueva Nota de Evolución", fontWeight = FontWeight.Black, fontSize = 16.sp, color = EDarkGreen)
            Spacer(Modifier.height(12.dp))
            if (esAccesible) {
                CampoTextoAccesible(
                    valor = texto,
                    onValorChange = { texto = it },
                    etiqueta = "Detalles del progreso clínico",
                    descripcionVoz = "Di los detalles del progreso clínico del lactante",
                    placeholder = "Ej. El paciente tolera bien las papillas",
                    ttsManager = ttsManager,
                    colorPrimario = EGreen
                )
            } else {
                OutlinedTextField(
                    value         = texto,
                    onValueChange = { texto = it },
                    placeholder   = { Text("Detalles del progreso clínico del lactante...", color = Color.LightGray) },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 3,
                    shape         = RoundedCornerShape(16.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = EGreen,
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        cursorColor          = EGreen
                    )
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick  = onCancelar,
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick  = { onGuardar(texto) },
                    enabled  = texto.isNotBlank() && !guardando,
                    modifier = Modifier.weight(1f).height(if (esAccesible) 70.dp else 46.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = EGreen),
                    shape    = RoundedCornerShape(16.dp)
                ) {
                    if (guardando) {
                        CircularProgressIndicator(Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Guardar Nota", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(msg: String) {
    Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
        Text(msg, fontSize = 12.sp, color = Color.Gray)
    }
}

// ════════════════════════════════════════════════════════════════════════════
// COMPONENTES TAB ALIMENTACIÓN
// ════════════════════════════════════════════════════════════════════════════

@Composable
private fun EtapaBanner(meses: Int, etapa: String) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier          = Modifier
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(EOrange, Color(0xFFF57C00))
                    )
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ChildCare, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(etapa, color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(2.dp))
                Text("$meses meses de vida · DietaEngine activa", color = Color.White.copy(.88f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PlanDiaCard(dia: PlanDiaResumen) {
    var expanded by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(240), label = "rot")

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        border    = BorderStroke(1.dp, if (expanded) EOrange.copy(.4f) else Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(Modifier.clickable { expanded = !expanded }) {
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier         = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(if (expanded) EOrange else EOrange.copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        dia.diaSemana.take(1),
                        fontWeight = FontWeight.Black,
                        color      = if (expanded) Color.White else EOrange,
                        fontSize   = 16.sp
                    )
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    dia.diaSemana,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 15.sp,
                    color      = if (expanded) EOrange else Color(0xFF212121),
                    modifier   = Modifier.weight(1f)
                )
                Icon(
                    Icons.Rounded.KeyboardArrowDown, null,
                    tint     = if (expanded) EOrange else Color.Gray,
                    modifier = Modifier.size(22.dp).graphicsLayerRotation(rot)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                exit    = shrinkVertically() + fadeOut(tween(150))
            ) {
                Column(
                    modifier            = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = EOrange.copy(.15f), thickness = 0.5.dp)
                    Spacer(Modifier.height(4.dp))
                    PlanComidaRow("Desayuno",   dia.desayuno,  Icons.Rounded.WbSunny,          Color(0xFFFFB300))
                    PlanComidaRow("Almuerzo",   dia.almuerzo,  Icons.Rounded.Restaurant,        EOrange)
                    PlanComidaRow("Colación",   dia.colacion1, Icons.Rounded.EmojiFoodBeverage, EGreen)
                    if (dia.colacion2.isNotBlank())
                        PlanComidaRow("Col. tarde", dia.colacion2, Icons.Rounded.Coffee,        Color(0xFF8D6E63))
                    PlanComidaRow("Cena",       dia.cena,      Icons.Rounded.Nightlight,        Color(0xFF7986CB))
                }
            }
        }
    }
}

@Composable
private fun PlanComidaRow(tipo: String, desc: String, icon: ImageVector, color: Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            modifier         = Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(color.copy(.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(tipo, fontSize = 10.sp, fontWeight = FontWeight.Black, color = color)
            Spacer(Modifier.height(1.dp))
            Text(desc, fontSize = 13.sp, color = Color(0xFF424242), fontWeight = FontWeight.Medium, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun RecetaSugeridaCard(receta: RecetaMexicana) {
    var expanded by remember { mutableStateOf(false) }
    val rot   by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "rrot")
    val color  = colorParaTipo(receta.tipoComida)
    val icon   = iconParaTipo(receta.tipoComida)
    val label  = labelParaTipo(receta.tipoComida)

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        border    = BorderStroke(1.dp, if (expanded) color.copy(.4f) else Color(0xFFEEEEEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(Modifier.clickable { expanded = !expanded }.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(color.copy(.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        receta.nombre,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = Color(0xFF212121),
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MiniChip(icon, label, color)
                        MiniChip(Icons.Rounded.ChildCare, "desde ${receta.edadMinMeses}m", Color.Gray)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("${receta.kcal} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(Modifier.height(6.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown, null,
                        tint     = if (expanded) color else Color.Gray,
                        modifier = Modifier.size(20.dp).graphicsLayerRotation(rot)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                exit    = shrinkVertically() + fadeOut(tween(150))
            ) {
                Column(Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                    Spacer(Modifier.height(12.dp))

                    Text("Ingredientes", fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
                    Spacer(Modifier.height(6.dp))
                    receta.ingredientes.forEach { ing ->
                        Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                            Text("• ", fontSize = 14.sp, fontWeight = FontWeight.Black, color = color)
                            Text(ing, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Preparación", fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
                    Spacer(Modifier.height(6.dp))
                    Text(receta.preparacion, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 19.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier          = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(EGreen.copy(.08f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.MenuBook, contentDescription = null, tint = EGreen, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(receta.fuente, fontSize = 11.sp, color = EDarkGreen, fontWeight = FontWeight.Medium, lineHeight = 15.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EntradaAlimentacionCard(
    entrada: EntradaAlimentacion,
    onEliminar: (() -> Unit)? = null
) {
    val color   = if (entrada.esReceta) EOrange else EGreen
    val icon    = if (entrada.esReceta) Icons.AutoMirrored.Rounded.MenuBook else Icons.Rounded.EditNote
    val bgColor = if (entrada.esReceta) Color(0xFFFFF8F2) else Color(0xFFF4FBF4)
    val border  = if (entrada.esReceta) Color(0xFFFFE5D0) else Color(0xFFD4EED4)

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(24.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        border    = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(color.copy(.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (entrada.titulo.isNotBlank()) entrada.titulo
                        else if (entrada.esReceta) "Receta personalizada"
                        else "Observación nutricional",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp,
                        color      = color
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        "${entrada.autorNombre} · ${entrada.fechaStr}",
                        fontSize = 11.sp,
                        color    = color.copy(.75f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Surface(shape = RoundedCornerShape(8.dp), color = color.copy(.15f)) {
                    Text(
                        if (entrada.esReceta) "Receta" else "Observación",
                        fontSize   = 10.sp,
                        fontWeight = FontWeight.Black,
                        color      = color,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (onEliminar != null) {
                    Spacer(Modifier.width(10.dp))
                    IconButton(
                        onClick  = onEliminar,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = "Eliminar",
                            tint               = Color(0xFFD32F2F),
                            modifier           = Modifier.size(18.dp)
                        )
                    }
                }
            }
            if (entrada.contenido.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = border, thickness = 0.5.dp)
                Spacer(Modifier.height(10.dp))
                Text(
                    entrada.contenido,
                    fontSize = 13.sp,
                    color    = Color(0xFF424242),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

@Composable
private fun FormaAlimentacion(
    tipo:       String,
    guardando:  Boolean,
    onGuardar:  (titulo: String, contenido: String) -> Unit,
    onCancelar: () -> Unit
) {
    val esReceta  = tipo == "receta_nutriologo"
    val color     = if (esReceta) EOrange else EGreen
    var titulo    by remember { mutableStateOf("") }
    var contenido by remember { mutableStateOf("") }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esBlind = a11yMode == AccessibilityMode.BLIND
    val esMute = a11yMode == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute

    fun loc(es: String, en: String) = if (a11yVm.idioma.value == com.example.nutriia.accesibilidad.IdiomaVoz.INGLES) en else es

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (esReceta) Icons.AutoMirrored.Rounded.MenuBook else Icons.Rounded.EditNote,
                    null,
                    tint     = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (esReceta) "Nueva Receta Personalizada" else "Nueva Observación Nutricional",
                    fontWeight = FontWeight.Bold,
                    color      = color
                )
            }
            Spacer(Modifier.height(12.dp))

            if (esAccesible) {
                CampoTextoAccesible(
                    valor = titulo,
                    onValorChange = { titulo = it },
                    etiqueta = if (esReceta) loc("Nombre de la receta", "Recipe name") else loc("Título de la observación", "Observation title"),
                    descripcionVoz = if (esReceta) loc("Di el nombre de la receta", "Speak recipe name") else loc("Di el título de la observación", "Speak observation title"),
                    placeholder = if (esReceta) "Ej. Papilla de manzana" else "Ej. Reporte semanal",
                    ttsManager = ttsManager,
                    colorPrimario = color
                )
                Spacer(Modifier.height(10.dp))
                CampoTextoAccesible(
                    valor = contenido,
                    onValorChange = { contenido = it },
                    etiqueta = if (esReceta) loc("Detalles de la receta", "Recipe details") else loc("Detalles de la observación", "Observation details"),
                    descripcionVoz = if (esReceta) loc("Di ingredientes y preparación", "Speak ingredients and preparation") else loc("Di detalles de la observación", "Speak observation details"),
                    placeholder = if (esReceta) "Ej. Ingredientes..." else "Ej. Recomendaciones...",
                    ttsManager = ttsManager,
                    colorPrimario = color
                )
            } else {
                OutlinedTextField(
                    value         = titulo,
                    onValueChange = { titulo = it },
                    placeholder   = { Text(if (esReceta) "Nombre de la receta..." else "Título de la observación...") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = color,
                        unfocusedBorderColor = color.copy(.4f),
                        cursorColor          = color
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value         = contenido,
                    onValueChange = { contenido = it },
                    placeholder   = {
                        Text(
                            if (esReceta)
                                "Ingredientes, preparación, notas clínicas..."
                            else
                                "Observaciones sobre el progreso alimentario, recomendaciones, ajustes al plan..."
                        )
                    },
                    modifier  = Modifier.fillMaxWidth(),
                    minLines  = 4,
                    shape     = RoundedCornerShape(12.dp),
                    colors    = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = color,
                        unfocusedBorderColor = color.copy(.4f),
                        cursorColor          = color
                    )
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
                    Text("Cancelar", color = Color.Gray)
                }
                Button(
                    onClick  = { onGuardar(titulo, contenido) },
                    enabled  = contenido.isNotBlank() && !guardando,
                    modifier = Modifier.weight(1f).height(if (esAccesible) 70.dp else 46.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = color),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White)
                    else Text("Guardar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun FormaRecetaPersonalizada(
    guardando:  Boolean,
    onGuardar:  (titulo: String, ingredientes: String, preparacion: String, tipoComida: TipoComida, kcal: Int) -> Unit,
    onCancelar: () -> Unit
) {
    var titulo       by remember { mutableStateOf("") }
    var ingredientes by remember { mutableStateOf("") }
    var preparacion  by remember { mutableStateOf("") }
    var kcalStr      by remember { mutableStateOf("") }
    var tipoComida   by remember { mutableStateOf(TipoComida.COMIDA) }
    var tipoExpanded by remember { mutableStateOf(false) }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esBlind = a11yMode == AccessibilityMode.BLIND
    val esMute = a11yMode == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute

    fun loc(es: String, en: String) = if (a11yVm.idioma.value == com.example.nutriia.accesibilidad.IdiomaVoz.INGLES) en else es

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = ECardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.MenuBook, null, tint = EOrange, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Nueva Receta Personalizada", fontWeight = FontWeight.Bold, color = EOrange)
            }
            Spacer(Modifier.height(12.dp))

            if (esAccesible) {
                CampoTextoAccesible(
                    valor = titulo,
                    onValorChange = { titulo = it },
                    etiqueta = loc("Nombre de la receta", "Recipe name"),
                    descripcionVoz = loc("Di el nombre de la receta", "Speak recipe name"),
                    placeholder = "Ej. Papilla de pollo",
                    ttsManager = ttsManager,
                    colorPrimario = EOrange
                )
                Spacer(Modifier.height(10.dp))
                CampoTextoAccesible(
                    valor = ingredientes,
                    onValorChange = { ingredientes = it },
                    etiqueta = loc("Ingredientes", "Ingredients"),
                    descripcionVoz = loc("Di los ingredientes separados por comas", "Speak ingredients separated by commas"),
                    placeholder = "Ej. Zanahoria, papa, pollo",
                    ttsManager = ttsManager,
                    colorPrimario = EOrange
                )
                Spacer(Modifier.height(10.dp))
                CampoTextoAccesible(
                    valor = preparacion,
                    onValorChange = { preparacion = it },
                    etiqueta = loc("Preparación", "Preparation"),
                    descripcionVoz = loc("Di los pasos de preparación", "Speak preparation steps"),
                    placeholder = "Ej. Hervir el pollo y las verduras...",
                    ttsManager = ttsManager,
                    colorPrimario = EOrange
                )
            } else {
                OutlinedTextField(
                    value         = titulo,
                    onValueChange = { titulo = it },
                    placeholder   = { Text("Nombre de la receta...") },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EOrange, unfocusedBorderColor = EOrange.copy(.4f), cursorColor = EOrange
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value         = ingredientes,
                    onValueChange = { ingredientes = it },
                    label         = { Text("Ingredientes") },
                    placeholder   = { Text("Zanahoria, papa, pollo\n(separar con comas)") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 2,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EOrange, unfocusedBorderColor = EOrange.copy(.4f),
                        cursorColor = EOrange, focusedLabelColor = EOrange
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value         = preparacion,
                    onValueChange = { preparacion = it },
                    label         = { Text("Preparación") },
                    placeholder   = { Text("Pasos de preparación, notas clínicas...") },
                    modifier      = Modifier.fillMaxWidth(),
                    minLines      = 3,
                    shape         = RoundedCornerShape(12.dp),
                    colors        = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EOrange, unfocusedBorderColor = EOrange.copy(.4f),
                        cursorColor = EOrange, focusedLabelColor = EOrange
                    )
                )
            }
            Spacer(Modifier.height(10.dp))

            // Tipo de comida + Kcal
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Dropdown tipo de comida
                Box(Modifier.weight(1f)) {
                    OutlinedTextField(
                        value         = labelParaTipo(tipoComida),
                        onValueChange = {},
                        readOnly      = true,
                        label         = { Text("Tipo") },
                        trailingIcon  = {
                            Icon(
                                Icons.Rounded.ArrowDropDown, null,
                                Modifier.clickable { tipoExpanded = true },
                                tint = EOrange
                            )
                        },
                        modifier      = Modifier.fillMaxWidth().clickable { tipoExpanded = true }.height(if (esAccesible) 70.dp else 56.dp),
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EOrange, unfocusedBorderColor = EOrange.copy(.4f),
                            focusedLabelColor = EOrange
                        )
                    )
                    DropdownMenu(
                        expanded   = tipoExpanded,
                        onDismissRequest = { tipoExpanded = false }
                    ) {
                        TipoComida.entries.forEach { tipo ->
                            DropdownMenuItem(
                                text    = { Text(labelParaTipo(tipo)) },
                                onClick = { tipoComida = tipo; tipoExpanded = false },
                                leadingIcon = { Icon(iconParaTipo(tipo), null, tint = colorParaTipo(tipo), modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }

                // Kcal (opcional)
                if (esAccesible) {
                    CampoTextoAccesible(
                        valor = kcalStr,
                        onValorChange = { kcalStr = it.filter { c -> c.isDigit() } },
                        etiqueta = "Kcal",
                        descripcionVoz = "Di las calorías",
                        placeholder = "Calorías",
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        ttsManager = ttsManager,
                        colorPrimario = EOrange,
                        modifier = Modifier.weight(0.6f)
                    )
                } else {
                    OutlinedTextField(
                        value         = kcalStr,
                        onValueChange = { kcalStr = it.filter { c -> c.isDigit() } },
                        label         = { Text("Kcal") },
                        placeholder   = { Text("Opc.") },
                        modifier      = Modifier.weight(0.6f),
                        singleLine    = true,
                        shape         = RoundedCornerShape(12.dp),
                        colors        = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EOrange, unfocusedBorderColor = EOrange.copy(.4f),
                            focusedLabelColor = EOrange, cursorColor = EOrange
                        )
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextButton(onClick = onCancelar, modifier = Modifier.weight(1f)) {
                    Text("Cancelar", color = Color.Gray)
                }
                Button(
                    onClick  = {
                        onGuardar(
                            titulo,
                            ingredientes,
                            preparacion,
                            tipoComida,
                            kcalStr.toIntOrNull() ?: 0
                        )
                    },
                    enabled  = titulo.isNotBlank() && preparacion.isNotBlank() && !guardando,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = EOrange),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    if (guardando) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White)
                    else Text("Guardar Receta", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MiniChip(icon: ImageVector, label: String, color: Color) {
    Row(
        modifier          = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(9.dp))
        Text(label, fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun Modifier.graphicsLayerRotation(degrees: Float): Modifier =
    this.graphicsLayer { rotationZ = degrees }