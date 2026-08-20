package com.example.nutriia.embarazo

import androidx.compose.animation.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.datetime.*
import com.example.nutriia.util.CalendarEvent
import com.example.nutriia.util.PlatformCalendarManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.accesibilidad.VoiceInputManager
import com.example.nutriia.sueldo.Alergeno
import com.example.nutriia.sueldo.TipoComida

private object Emb {
    val Rosa          = Color(0xFFEC9BBF)
    val RosaOscuro    = Color(0xFFD4679A)
    val RosaClaro     = Color(0xFFFDE8F2)
    val Morado        = Color(0xFF9C8FE0)
    val Fondo         = Color(0xFFFFF5F9)
    val Teal          = Color(0xFF4DB6AC)
    val White         = Color(0xFFFFFFFF)
    val TextPrimary   = Color(0xFF4E342E)
    val TextSecondary = Color(0xFF8D6E63)
    val Border        = Color(0xFFF1E5EC)
}

@Composable
fun EmbarazoNutricionScreen(
    perfil: PerfilEmbarazo,
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
                    "Módulo de alimentación en el embarazo. Aquí puedes ver tu resumen nutricional por trimestre, registrar las comidas del día y consultar tu plan semanal recomendado.",
                    "Pregnancy nutrition module. Here you can view your trimester nutritional summary, log daily meals, and consult your recommended weekly plan."
                )
            )
        }
    }

        val repo = remember { com.example.nutriia.embarazo.EmbarazoNutricionRepository() }
    val repoSolidos = remember { com.example.nutriia.solidos.SolidosRepository() }
    val fechaHoy = remember { com.example.nutriia.utils.FechaUtils.fechaActual() }

    val uid = ""

    val alimentosHoy by remember(fechaHoy, uid) {
        repo.observarPorFecha(fechaHoy)
    }.collectAsState(initial = emptyList())

    val alimentosDisponibles by remember(uid) {
        repoSolidos.observarAlimentos("")
    }.collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var mostrarForm by remember { mutableStateOf(false) }
    var mostrarExportDialog by remember { mutableStateOf(false) }
    var mostrarAlimentoDisponibleForm by remember { mutableStateOf(false) }
    var preAlimento by remember { mutableStateOf("") }
    var preCalorias by remember { mutableStateOf("") }
    var preComidaType by remember { mutableStateOf("Desayuno") }
    var tab by rememberSaveable { mutableIntStateOf(0) }

    val resumen = remember(perfil) {
        DietaEmbarazoEngine.resumenNutricional(perfil.semanas, perfil.condiciones)
    }
    val repoIA = remember { PlanEmbarazoIARepository() }
    val necesitaAjuste = remember(perfil) { DietaEmbarazoEngine.necesitaAjusteIA(perfil) }
    var planState by remember { mutableStateOf<List<PlanDietaEmbarazoSemanal>>(emptyList()) }
    var cargandoPlanIA by remember { mutableStateOf(false) }
    var errorPlanIA by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(perfil, alimentosDisponibles) {
        val nombresDisponibles = alimentosDisponibles.map { it.nombre }
        val planBase = DietaEmbarazoEngine.generarPlanSemanal(
            semanas = perfil.semanas,
            nivel = perfil.nivelIngreso,
            region = perfil.region,
            alergenos = perfil.alergenosParsados,
            condiciones = perfil.condiciones,
            alimentosRegistrados = nombresDisponibles
        )

        if (necesitaAjuste) {
            cargandoPlanIA = true
            errorPlanIA = null
            val res = repoIA.generarPlanIA(perfil, DietaEmbarazoEngine.RECETAS)
            if (res.isSuccess) {
                planState = res.getOrThrow()
            } else {
                errorPlanIA = res.exceptionOrNull()?.message
                planState = planBase
            }
            cargandoPlanIA = false
        } else {
            planState = planBase
        }
    }
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtroTipo by remember { mutableStateOf<TipoComida?>(null) } // null = "Todas"

    val recetasFiltradas = remember(perfil, busqueda, filtroTipo) {
        DietaEmbarazoEngine.RECETAS.filter { r ->
            r.trimestreMinimo.ordinal <= DietaEmbarazoEngine.trimestrePorSemana(perfil.semanas).ordinal &&
            r.nivelMinimo.index <= perfil.nivelIngreso.index &&
            r.esSeguraParaPerfil(perfil.alergenosParsados, perfil.condiciones) &&
            (filtroTipo == null || r.tipoComida == filtroTipo) &&
            (busqueda.isBlank() || r.nombre.contains(busqueda, true) ||
                r.ingredientes.any { it.contains(busqueda, true) })
        }
    }

    Scaffold(
        containerColor = Emb.Fondo,
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    preAlimento = ""
                    preCalorias = ""
                    preComidaType = "Desayuno"
                    mostrarForm = true
                },
                containerColor = Emb.Rosa,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Registrar comida", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item { NutricionEmbarazoTopBar(resumen.trimestreLabel, perfil.semanas, onBack) }
            item { Spacer(Modifier.height(14.dp)) }

            if (resumen.alertasCondicion.isNotEmpty()) {
                item { AlertaBannerEmbarazo(resumen.alertasCondicion, Icons.Rounded.MedicalServices, Color(0xFFFFB300), titulo = "Cuidados especiales médicos") }
                item { Spacer(Modifier.height(8.dp)) }
            }
            item {
                AlertaBannerEmbarazo(
                    resumen.alimentosAEvitar.map { "${it.nombre} — ${it.motivo}" },
                    Icons.Rounded.Block, Color(0xFFE53935),
                    titulo = "Alimentos a evitar en el embarazo"
                )
            }
            item { Spacer(Modifier.height(16.dp)) }

            item { TabsNutricionEmbarazo(tab) { tab = it } }
            item { Spacer(Modifier.height(16.dp)) }

            when (tab) {
                0 -> tabResumenEmbarazo(
                    resumen = resumen,
                    alimentosHoy = alimentosHoy,
                    onAddComidaClick = {
                        preAlimento = ""
                        preCalorias = ""
                        preComidaType = "Desayuno"
                        mostrarForm = true
                    },
                    onDelete = { id ->
                        scope.launch {
                            repo.eliminar(id)
                        }
                    }
                )
                1 -> tabDisponiblesEmbarazo(
                    alimentos = alimentosDisponibles,
                    onAddClick = {
                        mostrarAlimentoDisponibleForm = true
                    },
                    onDelete = { al ->
                        scope.launch {
                            repoSolidos.eliminarAlimento("", al.id)
                        }
                    }
                )
                2 -> tabPlanSemanalEmbarazo(
                    plan = planState,
                    alimentos = alimentosDisponibles,
                    cargandoPlanIA = cargandoPlanIA,
                    necesitaAjuste = necesitaAjuste,
                    errorPlanIA = errorPlanIA,
                    onSelectMeal = { tipo, desc ->
                        val recetaCoincidente = DietaEmbarazoEngine.RECETAS.firstOrNull { it.nombre.equals(desc, ignoreCase = true) }
                        preAlimento = desc
                        preCalorias = recetaCoincidente?.kcal?.toString() ?: ""
                        preComidaType = when (tipo) {
                            "Desayuno" -> "Desayuno"
                            "Colación 1" -> "Colación 1"
                            "Comida" -> "Comida"
                            "Colación 2" -> "Colación 2"
                            else -> "Cena"
                        }
                        mostrarForm = true
                    },
                    onExport = {
                        mostrarExportDialog = true
                    },
                    onAddClick = {
                        tab = 1
                        mostrarAlimentoDisponibleForm = true
                    }
                )
                3 -> tabRecetasEmbarazo(
                    ageMonthsLabel = resumen.trimestreLabel,
                    busqueda = busqueda, onBusqueda = { busqueda = it },
                    filtroTipo = filtroTipo, onFiltro = { filtroTipo = it },
                    recetas = recetasFiltradas, alergenosPerfil = perfil.alergenosParsados
                )
            }
        }

        if (mostrarExportDialog) {
            val planTexto = buildString {
                append("📅 PLAN NUTRICIONAL SEMANAL - NutriIA\n")
                append("Trimestre: ${resumen.trimestreLabel}\n\n")
                planState.forEach { dia ->
                    append("--- ${dia.diaSemana.uppercase()} ---\n")
                    append("🌅 Desayuno: ${dia.comidas.desayuno}\n")
                    append("🍎 Colación 1: ${dia.comidas.colacion1}\n")
                    append("☀️ Comida: ${dia.comidas.comida}\n")
                    append("🥝 Colación 2: ${dia.comidas.colacion2}\n")
                    append("🌙 Cena: ${dia.comidas.cena}\n\n")
                }
                append("¡Cuida tu alimentación y la de tu bebé! 💖")
            }

            ExportarCalendarioDialog(
                onShare = {
                    com.example.nutriia.platform.openUrl("copy:$planTexto")
                    scope.launch { snackbar.showSnackbar("Plan copiado ✓") }
                    mostrarExportDialog = false
                },
                onExportarCalendario = {
                    val events = mutableListOf<CalendarEvent>()
                    val tz = TimeZone.currentSystemDefault()
                    val hoy = Clock.System.now().toLocalDateTime(tz)

                    planState.forEachIndexed { index, dia ->
                        val fechaDia = hoy.date.plus(index, DateTimeUnit.DAY).atTime(8, 0).toInstant(tz)
                        val desc = "Desayuno: ${dia.comidas.desayuno}\nComida: ${dia.comidas.comida}\nCena: ${dia.comidas.cena}"
                        
                        events.add(CalendarEvent(
                            title = "Nutrición Embarazo - ${dia.diaSemana}",
                            description = desc,
                            startDate = fechaDia.toEpochMilliseconds(),
                            endDate = fechaDia.toEpochMilliseconds() + 3600000,
                            allDay = true
                        ))
                    }

                    PlatformCalendarManager.addEvents(events) { exito ->
                        scope.launch {
                            if (exito) snackbar.showSnackbar("Exportado al calendario ✓")
                            else snackbar.showSnackbar("Error al exportar. Revisa permisos.")
                        }
                        mostrarExportDialog = false
                    }
                },
                onCerrar = { mostrarExportDialog = false }
            )
        }

        if (mostrarForm) {
            AgregarComidaEmbarazoDialog(
                initialAlimento = preAlimento,
                initialCalorias = preCalorias,
                initialComida = preComidaType,
                onGuardar = { reg ->
                    scope.launch {
                        repo.guardar(reg)
                        mostrarForm = false
                        preAlimento = ""
                        preCalorias = ""
                        preComidaType = "Desayuno"
                    }
                },
                onCerrar = {
                    mostrarForm = false
                    preAlimento = ""
                    preCalorias = ""
                    preComidaType = "Desayuno"
                },
                a11yMode = a11yMode,
                ttsManager = ttsManager,
                idiomaActual = idiomaActual
            )
        }

        if (mostrarAlimentoDisponibleForm) {
            AgregarAlimentoDisponibleDialog(
                onDismiss = { mostrarAlimentoDisponibleForm = false },
                onSave = { name, group ->
                    scope.launch {
                                                val newItem = com.example.nutriia.solidos.AlimentoIntroducido(
                             id = com.example.nutriia.platform.generateUUID(),
                             childId = "",
                             nombre = name,
                             grupo = group,
                             fechaIntroduccion = com.example.nutriia.utils.FechaUtils.hoyIso()
                        )
                        repoSolidos.guardarAlimento("", newItem)
                        mostrarAlimentoDisponibleForm = false
                    }
                },
                a11yMode = a11yMode,
                ttsManager = ttsManager,
                idiomaActual = idiomaActual
            )
        }
    }
}

@Composable
private fun NutricionEmbarazoTopBar(trimestreLabel: String, semanas: Int, onBack: () -> Unit) {
    val gradient = Brush.verticalGradient(listOf(Emb.RosaClaro, Emb.Fondo))
    Box(
        Modifier
            .fillMaxWidth()
            .background(gradient)
            .padding(start = 16.dp, end = 16.dp, top = 52.dp, bottom = 20.dp)
    ) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Emb.White.copy(0.8f))
                .align(Alignment.CenterStart)
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, null, tint = Emb.RosaOscuro)
        }
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Alimentación", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Emb.RosaOscuro)
            Text("Embarazo",      fontSize = 14.sp, fontWeight = FontWeight.SemiBold,  color = Emb.Morado)
            Spacer(Modifier.height(2.dp))
            Surface(shape = RoundedCornerShape(50.dp), color = Emb.Rosa.copy(0.12f)) {
                Text(
                    "$trimestreLabel · $semanas sem.",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                    fontSize = 11.sp, color = Emb.RosaOscuro, fontWeight = FontWeight.Medium
                )
            }
        }
    }
    HorizontalDivider(color = Emb.RosaClaro, thickness = 1.dp)
}

@Composable
private fun AlertaBannerEmbarazo(
    alertas: List<String>,
    icon: ImageVector,
    color: Color,
    titulo: String
) {
    val bg = color.copy(alpha = 0.05f)
    val border = color.copy(alpha = 0.15f)
    CollapseCard(bg = bg, border = border, arrowColor = color, header = {
        IconBox(icon, color, color.copy(.15f), 32.dp, 17.dp, RoundedCornerShape(10.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(titulo, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = color)
            Text("${alertas.size} " + if (alertas.size == 1) "alerta" else "alertas", fontSize = 11.sp, color = color.copy(.8f))
        }
    }) {
        HorizontalDivider(color = border, thickness = 0.5.dp)
        Spacer(Modifier.height(10.dp))
        alertas.forEach { a ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Rounded.Circle, null, tint = color.copy(0.6f), modifier = Modifier.size(8.dp).padding(top = 4.dp))
                Spacer(Modifier.width(8.dp))
                Text(a, fontSize = 12.sp, color = Emb.TextPrimary, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun TabsNutricionEmbarazo(selected: Int, onSelect: (Int) -> Unit) {
    val tabs = listOf(
        Triple("Resumen",      Icons.Rounded.Insights,              0),
        Triple("Disponibles",  Icons.Rounded.Kitchen,               1),
        Triple("Plan semanal", Icons.Rounded.CalendarMonth,         2),
        Triple("Recetas",      Icons.AutoMirrored.Rounded.MenuBook, 3)
    )
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), Arrangement.spacedBy(6.dp)) {
        tabs.forEach { (label, icon, i) ->
            val sel = selected == i
            val bg  by animateColorAsState(if (sel) Emb.Rosa else Emb.White, tween(200), label = "tb$i")
            val fg  by animateColorAsState(if (sel) Emb.White  else Emb.Rosa, tween(200), label = "tf$i")
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(if (sel) 0.dp else 1.dp, Emb.RosaClaro, RoundedCornerShape(12.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 8.dp),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = fg)
                }
            }
        }
    }
}

private fun LazyListScope.tabResumenEmbarazo(
    resumen: ResumenNutricionalEmbarazo,
    alimentosHoy: List<com.example.nutriia.nutriente.RegistroNutrientes>,
    onAddComidaClick: () -> Unit,
    onDelete: (String) -> Unit
) {
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            StaggeredEntrance(delayMs = 50L) {
                Text("Objetivos Nutricionales Diarios", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Emb.TextPrimary)
            }
            Spacer(Modifier.height(12.dp))

            val macros = resumen.macroObjetivo
            val targetKcal = 2000.0 + macros.caloriasExtra
            val consumedKcal = alimentosHoy.sumOf { it.macros.calorias }
            val pctKcal = if (targetKcal > 0) (consumedKcal / targetKcal).toFloat().coerceIn(0f..1f) else 0f

            StaggeredEntrance(delayMs = 100L) {
                val pctKcalAnimated by animateFloatAsState(
                    targetValue = pctKcal,
                    animationSpec = tween(900, easing = EaseOutCubic),
                    label = "pctKcal"
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Emb.Border)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(80.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { pctKcalAnimated },
                                modifier = Modifier.size(80.dp),
                                color = Emb.Rosa,
                                trackColor = Emb.Rosa.copy(alpha = 0.15f),
                                strokeWidth = 8.dp
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${(pctKcalAnimated * 100).toInt()}%",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Emb.RosaOscuro
                                )
                                Text(
                                    text = "Calorías",
                                    fontSize = 9.sp,
                                    color = Emb.TextSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Meta de Energía Diaria",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emb.TextPrimary
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Consumido: ${consumedKcal.toInt()} kcal",
                                fontSize = 12.sp,
                                color = Emb.TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Objetivo: ${targetKcal.toInt()} kcal" + if (macros.caloriasExtra > 0) " (incluye +${macros.caloriasExtra.toInt()} extra)" else "",
                                fontSize = 11.sp,
                                color = Emb.TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            StaggeredEntrance(delayMs = 150L) {
                Text(
                    "Desglose de Nutrientes",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Emb.TextSecondary
                )
            }
            Spacer(Modifier.height(8.dp))

            val consumedProteinas = (consumedKcal * 0.035).coerceAtMost(macros.proteinasG)
            val pctProteinas = if (macros.proteinasG > 0) (consumedProteinas / macros.proteinasG).toFloat().coerceIn(0f..1f) else 0f

            val consumedHierro = (consumedKcal * 0.013).coerceAtMost(macros.hierroMg)
            val pctHierro = if (macros.hierroMg > 0) (consumedHierro / macros.hierroMg).toFloat().coerceIn(0f..1f) else 0f

            val consumedCalcio = (consumedKcal * 0.5).coerceAtMost(macros.calcioMg)
            val pctCalcio = if (macros.calcioMg > 0) (consumedCalcio / macros.calcioMg).toFloat().coerceIn(0f..1f) else 0f

            val consumedAcidoFolico = (consumedKcal * 0.3).coerceAtMost(macros.acidoFolicoUg)
            val pctAcidoFolico = if (macros.acidoFolicoUg > 0) (consumedAcidoFolico / macros.acidoFolicoUg).toFloat().coerceIn(0f..1f) else 0f

            val consumedAgua = (1.0 + (alimentosHoy.size * 0.4)).coerceAtMost(macros.aguaLitros)
            val pctAgua = if (macros.aguaLitros > 0) (consumedAgua / macros.aguaLitros).toFloat().coerceIn(0f..1f) else 0f

            StaggeredEntrance(delayMs = 200L) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NutrientProgressCard(
                        modifier = Modifier.weight(1f),
                        label = "Proteínas",
                        consumed = "${((((consumedProteinas) * 10).toInt()) / 10.0).toString()}g",
                        target = "${macros.proteinasG}g",
                        progress = pctProteinas,
                        icon = Icons.Rounded.FitnessCenter,
                        color = Emb.Morado
                    )
                    NutrientProgressCard(
                        modifier = Modifier.weight(1f),
                        label = "Hierro",
                        consumed = "${((((consumedHierro) * 10).toInt()) / 10.0).toString()}mg",
                        target = "${macros.hierroMg}mg",
                        progress = pctHierro,
                        icon = Icons.Rounded.Shield,
                        color = Color(0xFFE57373)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            StaggeredEntrance(delayMs = 250L) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    NutrientProgressCard(
                        modifier = Modifier.weight(1f),
                        label = "Calcio",
                        consumed = "${consumedCalcio.toInt()}mg",
                        target = "${macros.calcioMg.toInt()}mg",
                        progress = pctCalcio,
                        icon = Icons.Rounded.Egg,
                        color = Color(0xFFFFB74D)
                    )
                    NutrientProgressCard(
                        modifier = Modifier.weight(1f),
                        label = "Ácido Fólico",
                        consumed = "${consumedAcidoFolico.toInt()}µg",
                        target = "${macros.acidoFolicoUg.toInt()}µg",
                        progress = pctAcidoFolico,
                        icon = Icons.Rounded.Grain,
                        color = Emb.Teal
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            StaggeredEntrance(delayMs = 300L) {
                NutrientProgressCard(
                    label = "Agua",
                    consumed = "${((((consumedAgua) * 10).toInt()) / 10.0).toString()}L",
                    target = "${macros.aguaLitros}L",
                    progress = pctAgua,
                    icon = Icons.Rounded.WaterDrop,
                    color = Color(0xFF64B5F6)
                )
            }

            Spacer(Modifier.height(20.dp))

            Spacer(Modifier.height(16.dp))

            StaggeredEntrance(delayMs = 350L) {
                var selectedInfoTab by remember { mutableStateOf(0) }
                val a11yVm: AccessibilityViewModel = viewModel()
                val idiomaActual by a11yVm.idioma.collectAsState()
                fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

                Column {
                    // Selector de pestañas elegante y compacto
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Emb.Border.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabPill(
                            modifier = Modifier.weight(1f),
                            selected = selectedInfoTab == 0,
                            text = loc("💡 Consejos de Salud", "💡 Health Tips"),
                            onClick = { selectedInfoTab = 0 }
                        )
                        TabPill(
                            modifier = Modifier.weight(1f),
                            selected = selectedInfoTab == 1,
                            text = loc("🥑 Alimentos Clave", "🥑 Key Foods"),
                            onClick = { selectedInfoTab = 1 }
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    AnimatedContent<Int>(
                        targetState = selectedInfoTab,
                        transitionSpec = {
                            (slideInHorizontally { width -> if (targetState > initialState) width else -width } + fadeIn()).togetherWith(
                                slideOutHorizontally { width -> if (targetState > initialState) -width else width } + fadeOut()
                            )
                        },
                        label = "tabContent"
                    ) { activeTab ->
                        when (activeTab) {
                            0 -> RecomendacionesCard(resumen.alertas)
                            1 -> AlimentosSugeridosCard(resumen.alimentosClave)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            StaggeredEntrance(delayMs = 450L) {
                EmbCard {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.RestaurantMenu, null, tint = Emb.RosaOscuro, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Comidas registradas hoy", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emb.TextPrimary)
                        }
                        Spacer(Modifier.height(10.dp))
                        if (alimentosHoy.isEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("No has registrado alimentos hoy.", fontSize = 12.sp, color = Emb.TextSecondary)
                                Spacer(Modifier.height(10.dp))
                                Button(
                                    onClick = onAddComidaClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = Emb.Rosa),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Registrar Comida", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        } else {
                            alimentosHoy.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.comida, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emb.RosaOscuro)
                                        Text(item.alimento, fontSize = 13.sp, color = Emb.TextPrimary)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("${item.macros.calorias.toInt()} kcal", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emb.TextSecondary)
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { onDelete(item.id) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Rounded.DeleteOutline, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
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
}

@Composable
private fun AgregarComidaEmbarazoDialog(
    initialAlimento: String = "",
    initialCalorias: String = "",
    initialComida: String = "Desayuno",
    onGuardar: (com.example.nutriia.nutriente.RegistroNutrientes) -> Unit,
    onCerrar: () -> Unit,
    a11yMode: AccessibilityMode = AccessibilityMode.NORMAL,
    ttsManager: NutriTTS? = null,
    idiomaActual: IdiomaVoz = IdiomaVoz.ESPANOL_MX
) {
    val comidas = listOf("Desayuno", "Colación 1", "Comida", "Colación 2", "Cena")
    var comida by remember { mutableStateOf(if (initialComida in comidas) initialComida else comidas[0]) }
    var alimento by remember { mutableStateOf(initialAlimento) }
    var calorias by remember { mutableStateOf(initialCalorias) }
    val esDeRecetaPredefinida = remember { initialAlimento.isNotEmpty() }

    val recetasEmbarazo = remember { com.example.nutriia.embarazo.DietaEmbarazoEngine.RECETAS }
    val recetasSolidos = remember { com.example.nutriia.sueldo.DietaEngine.RECETAS }
    val query = alimento

    val esBlind = a11yMode == AccessibilityMode.BLIND
    val esMute = a11yMode == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }

    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> alimento
            1 -> calorias
            else -> ""
        }
    }

    LaunchedEffect(alimento) {
        if (!esBlind || alimento.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (alimento == valorInicial) return@LaunchedEffect
        delay(2500L)
        if (alimento.isNotBlank() && campoActivo == 0 && alimento != valorInicial) campoActivo = 1
    }
    LaunchedEffect(calorias) {
        if (!esBlind || calorias.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (calorias == valorInicial) return@LaunchedEffect
        delay(2000L)
        if (calorias.isNotBlank() && campoActivo == 1 && calorias != valorInicial) campoActivo = 2
    }


    val voiceManager = remember { if (esBlind) VoiceInputManager() else null }

    val ejecutarGuardarComida: () -> Unit = {
        if (alimento.isNotBlank()) {
            val kcalVal = calorias.toIntOrNull() ?: 0
            val reg = com.example.nutriia.nutriente.RegistroNutrientes(
                id = com.example.nutriia.platform.generateUUID(),
                childId = "",
                fecha = com.example.nutriia.utils.FechaUtils.fechaActual(),
                comida = comida,
                alimento = alimento,
                macros = com.example.nutriia.nutriente.Macronutrientes(
                    calorias = kcalVal.toDouble()
                )
            )
            onGuardar(reg)
            if (esBlind) {
                ttsManager?.hablar(if (idiomaActual == IdiomaVoz.INGLES) "Food registered successfully." else "Alimento registrado con éxito.")
            }
        }
    }

    LaunchedEffect(campoActivo) {
        if (esBlind && campoActivo == 2) {
            ttsManager?.hablarYEsperar(
                if (idiomaActual == IdiomaVoz.INGLES) {
                    "Fields completed. Say save or register to log food."
                } else {
                    "Campos completados. Di guardar o registrar para guardar el alimento."
                },
                margenMs = 800L
            )
            voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                val cmd = result.lowercase().trim()
                if (cmd.contains("guardar") || cmd.contains("registrar") || cmd.contains("save")) {
                    ejecutarGuardarComida()
                }
            }
        }
    }

    val filteredOpciones = remember(query) {
        val list = mutableListOf<Pair<String, Int>>()
        recetasEmbarazo.forEach { list.add(it.nombre to it.kcal) }
        recetasSolidos.forEach { list.add(it.nombre to it.kcal) }

        if (query.isBlank()) {
            list
        } else {
            list.filter { it.first.contains(query, ignoreCase = true) }
        }
    }
    
    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    AlertDialog(
        onDismissRequest = onCerrar,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Restaurant, null, tint = Emb.RosaOscuro)
                Spacer(Modifier.width(8.dp))
                Text("Registrar Alimento", fontWeight = FontWeight.Bold, color = Emb.RosaOscuro, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("¿En qué momento del día?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Emb.TextPrimary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(comidas, key = { it }) { c ->
                        val sel = comida == c
                        val bg by animateColorAsState(if (sel) Emb.Rosa else Emb.Fondo, label = "c_$c")
                        val fg by animateColorAsState(if (sel) Color.White else Emb.RosaOscuro, label = "cf_$c")
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { comida = c }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(c, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                        }
                    }
                }

                if (esAccesible) {
                    CampoTextoAccesible(
                        valor = alimento,
                        onValorChange = { if (!esDeRecetaPredefinida) alimento = it },
                        etiqueta = loc("Nombre del alimento o receta", "Food or recipe name"),
                        descripcionVoz = loc("Di el nombre de la comida o alimento", "Speak the food or recipe name"),
                        placeholder = "Ej. Avena con manzana",
                        ttsManager = ttsManager,
                        colorPrimario = Emb.RosaOscuro,
                        activo = campoActivo == 0,
                        onFocus = { campoActivo = 0 },
                        onNext = { campoActivo = 1 }
                    )
                    androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 1) {
                        CampoTextoAccesible(
                            valor = calorias,
                            onValorChange = { if (!esDeRecetaPredefinida) calorias = it },
                            etiqueta = loc("Calorías (kcal)", "Calories (kcal)"),
                            descripcionVoz = loc("Di la cantidad de calorías estimadas", "Speak the estimated calories quantity"),
                            placeholder = "Ej. 250",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            ttsManager = ttsManager,
                            colorPrimario = Emb.RosaOscuro,
                            activo = campoActivo == 1,
                            onFocus = { campoActivo = 1 },
                            onNext = { campoActivo = 2 }
                        )
                    }
                } else {
                    OutlinedTextField(
                        value = alimento,
                        onValueChange = { if (!esDeRecetaPredefinida) alimento = it },
                        readOnly = esDeRecetaPredefinida,
                        label = { Text("Nombre del alimento o receta") },
                        leadingIcon = { Icon(Icons.Rounded.Restaurant, null, tint = Emb.RosaOscuro) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (esDeRecetaPredefinida) Emb.Border else Emb.RosaOscuro,
                            unfocusedBorderColor = Emb.Border,
                            focusedLabelColor = if (esDeRecetaPredefinida) Emb.TextSecondary else Emb.RosaOscuro
                        )
                    )

                    if (!esDeRecetaPredefinida && filteredOpciones.isNotEmpty()) {
                        Column {
                            Text(
                                "Sugerencias disponibles (embarazo y sólidos):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emb.RosaOscuro,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(filteredOpciones, key = { it.first }) { opt ->
                                    SuggestionChip(
                                        onClick = {
                                            alimento = opt.first
                                            calorias = opt.second.toString()
                                        },
                                        label = { Text("${opt.first} (${opt.second} kcal)", fontSize = 11.sp) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = Emb.RosaClaro,
                                            labelColor = Emb.RosaOscuro
                                        )
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = calorias,
                        onValueChange = { if (!esDeRecetaPredefinida) calorias = it },
                        readOnly = esDeRecetaPredefinida,
                        label = { Text("Calorías (kcal)") },
                        leadingIcon = { Icon(Icons.Rounded.LocalFireDepartment, null, tint = Emb.RosaOscuro) },
                        modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (esDeRecetaPredefinida) Emb.Border else Emb.RosaOscuro,
                        unfocusedBorderColor = Emb.Border,
                        focusedLabelColor = if (esDeRecetaPredefinida) Emb.TextSecondary else Emb.RosaOscuro
                    )
                )
            }
        }
    },
        confirmButton = {
            Button(
                onClick = { ejecutarGuardarComida() },
                enabled = alimento.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emb.RosaOscuro)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onCerrar) {
                Text("Cancelar", color = Emb.RosaOscuro)
            }
        }
    )
}

private fun LazyListScope.tabPlanSemanalEmbarazo(
    plan: List<PlanDietaEmbarazoSemanal>,
    alimentos: List<com.example.nutriia.solidos.AlimentoIntroducido>,
    cargandoPlanIA: Boolean,
    necesitaAjuste: Boolean,
    errorPlanIA: String?,
    onSelectMeal: (String, String) -> Unit,
    onExport: () -> Unit,
    onAddClick: () -> Unit
) {
    if (alimentos.isEmpty()) {
        item {
            PlanSemanalVacioCard(onAddClick)
        }
        return
    }

    if (cargandoPlanIA) {
        item {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Emb.Teal)
            }
        }
        return
    }

    if (necesitaAjuste) {
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                color = Emb.Teal.copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Emb.Teal.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = Emb.Teal,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Personalizado con IA",
                        fontSize = 12.sp,
                        color = Emb.Teal,
                        fontWeight = FontWeight.Bold
                    )
                    if (errorPlanIA != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "(Offline, usando versión local)",
                            fontSize = 10.sp,
                            color = Color.Red,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    item {
        EmbCard(
            bg = Emb.RosaClaro.copy(alpha = 0.4f),
            border = Emb.Rosa.copy(alpha = 0.2f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExport() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = Emb.RosaOscuro,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Exportar al Calendario",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Emb.RosaOscuro
                )
            }
        }
        Spacer(Modifier.height(12.dp))
    }
    items(plan, key = { it.diaSemana }) { dia ->
        PlanDiaEmbarazoCard(dia, plan.indexOf(dia), onSelectMeal)
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PlanDiaEmbarazoCard(
    plan: PlanDietaEmbarazoSemanal,
    index: Int,
    onSelectMeal: (String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rot      by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "pd$index")
    var visible  by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index * 55L); visible = true }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(360, easing = EaseOutCubic)) { it / 4 } + fadeIn(tween(360))
    ) {
        EmbCard(border = if (expanded) Emb.RosaOscuro.copy(.25f) else Emb.Border) {
            Column(Modifier.clickable { expanded = !expanded }) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                            .background(if (expanded) Emb.Rosa else Emb.Rosa.copy(.1f)),
                        Alignment.Center
                    ) {
                        Text(plan.diaSemana.take(1), fontWeight = FontWeight.Black, color = if (expanded) Emb.White else Emb.Rosa, fontSize = 16.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(plan.diaSemana, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = if (expanded) Emb.RosaOscuro else Emb.TextPrimary, modifier = Modifier.weight(1f))
                    if (!expanded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            listOf(Icons.Rounded.WbSunny, Icons.Rounded.Restaurant, Icons.Rounded.Nightlight).forEach {
                                Icon(it, null, tint = Color(0xFFE0E0E0), modifier = Modifier.size(14.dp))
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Rounded.KeyboardArrowDown, null,
                        tint = if (expanded) Emb.RosaOscuro else Color(0xFFBDBDBD),
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rot })
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                    exit  = shrinkVertically() + fadeOut(tween(180))
                ) {
                    Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                        HorizontalDivider(color = Emb.Rosa.copy(.1f), thickness = 0.5.dp)
                        Spacer(Modifier.height(10.dp))
                        
                        ComidaRow("Desayuno",      plan.comidas.desayuno,  Icons.Rounded.WbSunny,          Color(0xFFFFB300)) { onSelectMeal("Desayuno", plan.comidas.desayuno) }
                        ComidaRow("Colación 1",     plan.comidas.colacion1, Icons.Rounded.EmojiFoodBeverage, Emb.Teal) { onSelectMeal("Colación 1", plan.comidas.colacion1) }
                        ComidaRow("Comida",        plan.comidas.comida,    Icons.Rounded.Restaurant,        Emb.RosaOscuro) { onSelectMeal("Comida", plan.comidas.comida) }
                        ComidaRow("Colación 2",     plan.comidas.colacion2, Icons.Rounded.EmojiFoodBeverage, Emb.Teal) { onSelectMeal("Colación 2", plan.comidas.colacion2) }
                        ComidaRow("Cena",          plan.comidas.cena,      Icons.Rounded.Nightlight,        Color(0xFF7986CB)) { onSelectMeal("Cena", plan.comidas.cena) }
                    }
                }
            }
        }
    }
}


private fun LazyListScope.tabRecetasEmbarazo(
    ageMonthsLabel: String,
    busqueda:      String,
    onBusqueda:    (String) -> Unit,
    filtroTipo:    TipoComida?,
    onFiltro:      (TipoComida?) -> Unit,
    recetas:       List<RecetaEmbarazo>,
    alergenosPerfil: List<Alergeno>
) {
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = busqueda, onValueChange = onBusqueda,
                placeholder = { Text("Buscar receta o ingrediente...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, null, tint = Emb.RosaOscuro, modifier = Modifier.size(19.dp)) },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = Emb.RosaOscuro, unfocusedBorderColor = Emb.RosaClaro,
                    focusedLabelColor    = Emb.RosaOscuro, cursorColor          = Emb.RosaOscuro
                )
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    val sel = filtroTipo == null
                    val bg  by animateColorAsState(if (sel) Emb.Rosa else Emb.White, tween(180), label = "cfNull")
                    val fg  by animateColorAsState(if (sel) Emb.White  else Emb.Rosa, tween(180), label = "cffNull")
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .border(1.dp, if (sel) Color.Transparent else Emb.RosaClaro, RoundedCornerShape(20.dp))
                            .clickable { onFiltro(null) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.GridView, null, tint = fg, modifier = Modifier.size(13.dp))
                        Text("Todas", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    }
                }
                
                items(TipoComida.entries, key = { it.name }) { f ->
                    val sel = filtroTipo == f
                    val bg  by animateColorAsState(if (sel) Emb.Rosa else Emb.White, tween(180), label = "cf${f.name}")
                    val fg  by animateColorAsState(if (sel) Emb.White  else Emb.Rosa, tween(180), label = "cff${f.name}")
                    val ic: ImageVector = when (f) {
                        TipoComida.DESAYUNO -> Icons.Rounded.WbSunny
                        TipoComida.COMIDA   -> Icons.Rounded.Restaurant
                        TipoComida.CENA     -> Icons.Rounded.Nightlight
                        TipoComida.COLACION -> Icons.Rounded.EmojiFoodBeverage
                    }
                    val label = when (f) {
                        TipoComida.DESAYUNO -> "Desayuno"
                        TipoComida.COMIDA   -> "Comida"
                        TipoComida.CENA     -> "Cena"
                        TipoComida.COLACION -> "Colación"
                    }
                    Row(
                        Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .border(1.dp, if (sel) Color.Transparent else Emb.RosaClaro, RoundedCornerShape(20.dp))
                            .clickable { onFiltro(f) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Icon(ic, null, tint = fg, modifier = Modifier.size(13.dp))
                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = fg)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }
    if (recetas.isEmpty()) {
        item { EstadoVacio(Icons.Rounded.SearchOff, "Sin recetas para este filtro", "Prueba con otro filtro o ingrediente") }
        return
    }
    item {
        Text(
            "${recetas.size} receta${if (recetas.size != 1) "s" else ""} disponible${if (recetas.size != 1) "s" else ""} · $ageMonthsLabel",
            fontSize = 12.sp, color = Emb.TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(6.dp))
    }
    items(recetas, key = { it.nombre }) { r -> RecetaEmbarazoCard(r, alergenosPerfil, recetas.indexOf(r)) }
}

@Composable
private fun RecetaEmbarazoCard(receta: RecetaEmbarazo, alergenosPerfil: List<Alergeno>, index: Int) {
    var expanded     by remember { mutableStateOf(false) }
    val rot          by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "rr$index")
    var visible      by remember { mutableStateOf(false) }
    val tieneAlergia  = receta.alergenos.any { it in alergenosPerfil }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(index.coerceAtMost(8) * 50L); visible = true }
    val (tipoColor, tipoIcon, tipoLabel) = when (receta.tipoComida) {
        TipoComida.DESAYUNO -> Triple(Color(0xFFFFB300), Icons.Rounded.WbSunny,          "Desayuno")
        TipoComida.COMIDA   -> Triple(Emb.RosaOscuro,     Icons.Rounded.Restaurant,        "Comida")
        TipoComida.CENA     -> Triple(Color(0xFF7986CB), Icons.Rounded.Nightlight,        "Cena")
        TipoComida.COLACION -> Triple(Emb.Teal,           Icons.Rounded.EmojiFoodBeverage, "Colación")
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(320, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(320))
    ) {
        EmbCard(bg = if (tieneAlergia) Color(0xFFFFFBF5) else Emb.White, border = if (expanded) Emb.RosaOscuro.copy(.3f) else Emb.Border) {
            Column(Modifier.clickable { expanded = !expanded }.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBox(tipoIcon, tipoColor, tipoColor.copy(.12f), 54.dp, 28.dp, RoundedCornerShape(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(receta.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emb.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(5.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Chip(tipoIcon, tipoLabel, tipoColor)
                            Chip(Icons.Rounded.ChildCare, "desde ${receta.trimestreMinimo.label.lowercase()}", Emb.TextSecondary)
                            if (tieneAlergia) Chip(Icons.Rounded.Warning, "Alérgeno", Emb.RosaOscuro)
                        }
                    }
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${receta.kcal} kcal", fontSize = 11.sp, color = Emb.TextSecondary)
                        Icon(Icons.Rounded.KeyboardArrowDown, null,
                            tint = if (expanded) Emb.RosaOscuro else Color(0xFFBDBDBD),
                            modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rot })
                    }
                }
                AnimatedVisibility(
                    visible = expanded,
                    enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(200)),
                    exit  = shrinkVertically() + fadeOut(tween(160))
                ) {
                    Column(Modifier.padding(top = 14.dp)) {
                        HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 0.5.dp)
                        Spacer(Modifier.height(12.dp))
                        if (tieneAlergia) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF3E0)).padding(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(Icons.Rounded.Warning, null, tint = Color(0xFFFFB300), modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Contiene: ${receta.alergenos.joinToString(", ") { it.label }}. Introducir con supervisión médica.",
                                    fontSize = 12.sp, color = Color(0xFFBF360C), lineHeight = 17.sp
                                )
                            }
                            Spacer(Modifier.height(10.dp))
                        }
                        SeccionLabel("Ingredientes", Icons.Rounded.ShoppingCart)
                        Spacer(Modifier.height(6.dp))
                        receta.ingredientes.forEach { ing ->
                            Row(Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
                                Box(Modifier.padding(top = 6.dp).size(5.dp).clip(CircleShape).background(Emb.RosaOscuro))
                                Spacer(Modifier.width(8.dp))
                                Text(ing, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        SeccionLabel("Preparación", Icons.AutoMirrored.Rounded.MenuBook)
                        Spacer(Modifier.height(6.dp))
                        Text(receta.preparacion, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 19.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFF1F8E9)).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(receta.fuente, fontSize = 10.sp, color = Color(0xFF2E7D32), lineHeight = 14.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun IconBox(
    icon:   ImageVector,
    tint:   Color,
    bg:     Color,
    size:   Dp = 36.dp,
    iconSz: Dp = 18.dp,
    shape:  RoundedCornerShape = RoundedCornerShape(12.dp)
) = Box(Modifier.size(size).clip(shape).background(bg), Alignment.Center) {
    Icon(icon, null, tint = tint, modifier = Modifier.size(iconSz))
}

@Composable
private fun Chip(icon: ImageVector, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(.12f))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(10.dp))
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmbCard(
    modifier:    Modifier = Modifier,
    bg:          Color = Emb.White,
    border:      Color = Emb.Border,
    borderWidth: Dp = 1.dp,
    shape:       RoundedCornerShape = RoundedCornerShape(18.dp),
    content:     @Composable ColumnScope.() -> Unit
) = Card(
    modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
    shape    = shape,
    colors   = CardDefaults.cardColors(containerColor = bg),
    border   = BorderStroke(borderWidth, border),
    content  = content
)

@Composable
private fun CollapseCard(
    bg:         Color = Emb.White,
    border:     Color = Emb.Border,
    arrowColor: Color = Emb.RosaOscuro,
    header:     @Composable RowScope.() -> Unit,
    content:    @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rot by animateFloatAsState(if (expanded) 180f else 0f, tween(250), label = "collapseRot")
    EmbCard(bg = bg, border = border) {
        Column(modifier = Modifier.clickable { expanded = !expanded }.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                header()
                Icon(
                    Icons.Rounded.KeyboardArrowDown, null, tint = arrowColor,
                    modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = rot }
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                exit  = shrinkVertically() + fadeOut(tween(180))
            ) { Column(Modifier.padding(top = 12.dp)) { content() } }
        }
    }
}

@Composable
private fun SeccionLabel(texto: String, icon: ImageVector) =
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Icon(icon, null, tint = Emb.RosaOscuro, modifier = Modifier.size(13.dp))
        Text(texto.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Emb.RosaOscuro, letterSpacing = 0.8.sp)
    }

@Composable
private fun ComidaRow(
    tipo: String,
    desc: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) = Row(
    Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(10.dp))
        .clickable { onClick() }
        .padding(vertical = 6.dp, horizontal = 4.dp),
    verticalAlignment = Alignment.CenterVertically
) {
    IconBox(icon, color, color.copy(.12f), 26.dp, 13.dp, RoundedCornerShape(8.dp))
    Spacer(Modifier.width(10.dp))
    Column(Modifier.weight(1f)) {
        Text(tipo, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = color, letterSpacing = 0.3.sp)
        Text(desc, fontSize = 13.sp, color = Color(0xFF424242), lineHeight = 18.sp)
    }
}

@Composable
private fun EstadoVacio(
    icon:     ImageVector,
    texto:    String,
    subtexto: String
) {
    val inf = rememberInfiniteTransition(label = "ev")
    val sc  by inf.animateFloat(.94f, 1.06f, infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), label = "evs")
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Emb.Rosa.copy(.08f)).graphicsLayer { scaleX = sc; scaleY = sc },
            Alignment.Center
        ) {
            Icon(icon, null, tint = Emb.Rosa.copy(.5f), modifier = Modifier.size(40.dp))
        }
        Text(texto, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Emb.TextPrimary, textAlign = TextAlign.Center)
        Text(subtexto, fontSize = 13.sp, color = Emb.TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun MacroCard(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    EmbCard(modifier = modifier) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBox(icon, color, color.copy(alpha = 0.12f), size = 40.dp, iconSz = 20.dp, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, fontSize = 11.sp, color = Emb.TextSecondary, fontWeight = FontWeight.Medium)
                Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Emb.TextPrimary)
            }
        }
    }
}
@Composable
private fun NutrientProgressCard(
    modifier: Modifier = Modifier,
    label: String,
    consumed: String,
    target: String,
    progress: Float,
    icon: ImageVector,
    color: Color
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(900, easing = EaseOutCubic),
        label = "nutrientProgress"
    )
    val pct = (progress * 100).toInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(color.copy(alpha = 0.1f)),
                        Alignment.Center
                    ) {
                        Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(label, fontSize = 12.sp, color = Emb.TextPrimary, fontWeight = FontWeight.Bold)
                        Text("$consumed / $target", fontSize = 11.sp, color = Emb.TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
                
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$pct%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.1f)
            )
        }
    }
}

@Composable
private fun TabPill(
    modifier: Modifier = Modifier,
    selected: Boolean,
    text: String,
    onClick: () -> Unit
) {
    val bg = if (selected) Emb.RosaOscuro else Color.Transparent
    val tc = if (selected) Color.White else Emb.TextSecondary
    val scale by animateFloatAsState(if (selected) 1f else 0.95f, label = "pillScale")
    
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = tc
        )
    }
}

@Composable
private fun RecomendacionesCard(alertas: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Emb.Border)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            alertas.forEachIndexed { index, alerta ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(alerta) {
                    kotlinx.coroutines.delay(index * 100L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter = slideInHorizontally(animationSpec = tween(400, easing = EaseOutCubic)) { -it / 5 } + fadeIn(tween(400)),
                    exit = fadeOut(tween(200))
                ) {
                    val (icon, iconColor, bgIconColor) = remember(alerta) {
                        when {
                            alerta.contains("ácido fólico", ignoreCase = true) || alerta.contains("folico", ignoreCase = true) -> 
                                Triple(Icons.Rounded.Spa, Emb.Teal, Emb.Teal.copy(alpha = 0.08f))
                            alerta.contains("hierro", ignoreCase = true) -> 
                                Triple(Icons.Rounded.Shield, Color(0xFFE57373), Color(0xFFE57373).copy(alpha = 0.08f))
                            alerta.contains("hidratación", ignoreCase = true) || alerta.contains("agua", ignoreCase = true) || alerta.contains("litros", ignoreCase = true) -> 
                                Triple(Icons.Rounded.Opacity, Color(0xFF64B5F6), Color(0xFF64B5F6).copy(alpha = 0.08f))
                            alerta.contains("cafeína", ignoreCase = true) || alerta.contains("café", ignoreCase = true) || alerta.contains("cafeina", ignoreCase = true) -> 
                                Triple(Icons.Rounded.LocalCafe, Color(0xFF8D6E63), Color(0xFF8D6E63).copy(alpha = 0.08f))
                            alerta.contains("comidas", ignoreCase = true) || alerta.contains("desayuno", ignoreCase = true) || alerta.contains("colaciones", ignoreCase = true) -> 
                                Triple(Icons.Rounded.Restaurant, Emb.Morado, Emb.Morado.copy(alpha = 0.08f))
                            else -> 
                                Triple(Icons.Rounded.Info, Color(0xFFFFB74D), Color(0xFFFFB74D).copy(alpha = 0.08f))
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bgIconColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(bgIconColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, null, tint = iconColor, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = alerta,
                            fontSize = 13.sp,
                            color = Emb.TextPrimary,
                            lineHeight = 18.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlimentosSugeridosCard(alimentos: List<String>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Emb.Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Alimentos recomendados hoy:",
                fontSize = 13.sp,
                color = Emb.TextSecondary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(alimentos) { index, al ->
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(al) {
                        kotlinx.coroutines.delay(index * 80L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible,
                        enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)) + fadeIn(tween(350)),
                        exit = fadeOut()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Emb.Teal.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, Emb.Teal.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Emb.Teal),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(12.dp))
                                }
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = al,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Emb.TextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StaggeredEntrance(
    delayMs: Long,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(delayMs)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(tween(450, easing = EaseOutCubic)) { it / 3 } + fadeIn(tween(450)),
        exit = fadeOut(tween(200))
    ) {
        content()
    }
}

@Composable
private fun ExportarCalendarioDialog(
    onShare: () -> Unit,
    onExportarCalendario: () -> Unit,
    onCerrar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCerrar,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CloudDownload, null, tint = Emb.RosaOscuro)
                Spacer(Modifier.width(8.dp))
                Text("Exportar Plan Semanal", fontWeight = FontWeight.Bold, color = Emb.RosaOscuro, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Selecciona el formato en el que deseas exportar tu calendario nutricional de lunes a domingo:",
                    fontSize = 13.sp,
                    color = Emb.TextSecondary,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                
                // Option 1: Share
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onShare() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Emb.Fondo),
                    border = BorderStroke(1.dp, Emb.RosaClaro)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Share, null, tint = Emb.RosaOscuro, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Compartir directamente", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emb.TextPrimary)
                            Text("Enviar por WhatsApp, Gmail, Telegram...", fontSize = 11.sp, color = Emb.TextSecondary)
                        }
                    }
                }
                
                // Option 2: Export to Calendar
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onExportarCalendario() },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Emb.Fondo),
                    border = BorderStroke(1.dp, Emb.RosaClaro)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.CalendarMonth, null, tint = Emb.RosaOscuro, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Exportar al Calendario", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Emb.TextPrimary)
                            Text("Insertar en el calendario de tu celular", fontSize = 11.sp, color = Emb.TextSecondary)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCerrar) {
                Text("Cerrar", color = Emb.RosaOscuro, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AgregarAlimentoDisponibleDialog(
    onDismiss: () -> Unit,
    onSave: (String, com.example.nutriia.solidos.GrupoAlimento) -> Unit,
    a11yMode: AccessibilityMode = AccessibilityMode.NORMAL,
    ttsManager: NutriTTS? = null,
    idiomaActual: IdiomaVoz = IdiomaVoz.ESPANOL_MX
) {
    var nombre by remember { mutableStateOf("") }
    var grupo by remember { mutableStateOf(com.example.nutriia.solidos.GrupoAlimento.VERDURAS) }
    val fc = OutlinedTextFieldDefaults.colors(
        focusedBorderColor   = Emb.RosaOscuro, unfocusedBorderColor = Emb.RosaClaro,
        focusedLabelColor    = Emb.RosaOscuro, cursorColor          = Emb.RosaOscuro
    )
    val esBlind = a11yMode == AccessibilityMode.BLIND
    val esMute = a11yMode == AccessibilityMode.MUTE
    val esAccesible = esBlind || esMute
    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es


    val voiceManager = remember { if (esBlind) VoiceInputManager() else null }

    LaunchedEffect(nombre) {
        if (!esBlind || nombre.isBlank()) return@LaunchedEffect
        delay(2500L)
        if (nombre.isNotBlank()) {
            ttsManager?.hablarYEsperar(
                loc(
                    "Nombre de alimento capturado. Di guardar para registrarlo.",
                    "Food name captured. Say save to log it."
                ),
                margenMs = 800L
            )
            voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                val cmd = result.lowercase().trim()
                if (cmd.contains("guardar") || cmd.contains("registrar") || cmd.contains("save")) {
                    onSave(nombre, grupo)
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AddCircle, null, tint = Emb.RosaOscuro)
                Spacer(Modifier.width(10.dp))
                Text("Registrar alimento disponible", fontWeight = FontWeight.Bold, color = Emb.RosaOscuro, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (esAccesible) {
                    CampoTextoAccesible(
                        valor = nombre,
                        onValorChange = { nombre = it },
                        etiqueta = loc("Nombre del alimento", "Food name"),
                        descripcionVoz = loc("Di el nombre del alimento disponible en casa", "Speak the food name available at home"),
                        placeholder = "Ej. Plátano",
                        ttsManager = ttsManager,
                        colorPrimario = Emb.RosaOscuro
                    )
                } else {
                    OutlinedTextField(
                        nombre, { nombre = it }, Modifier.fillMaxWidth(),
                        label = { Text("Nombre del alimento") },
                        leadingIcon = { Icon(Icons.Rounded.Restaurant, null, tint = Emb.RosaOscuro) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = fc
                    )
                }

                Text("Grupo alimenticio", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Emb.TextSecondary)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(com.example.nutriia.solidos.GrupoAlimento.entries, key = { it.name }) { g ->
                        val sel = grupo == g
                        val bg = if (sel) Color(g.colorHex) else Emb.Fondo
                        val fg = if (sel) Color.White else Color(g.colorHex)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(bg)
                                .clickable { grupo = g }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(g.label, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = fg)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(nombre, grupo) },
                enabled = nombre.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Emb.RosaOscuro)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Emb.RosaOscuro)
            }
        }
    )
}

@Composable
private fun AlimentosDisponiblesHeader(total: Int, onAddClick: () -> Unit) {
    EmbCard(bg = Emb.RosaClaro.copy(alpha = 0.4f), border = Emb.Rosa.copy(alpha = 0.2f)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBox(Icons.Rounded.Kitchen, Emb.RosaOscuro, Emb.RosaOscuro.copy(.12f), 48.dp, 24.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Despensa e Ingredientes", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Emb.TextPrimary)
                    Text("$total alimentos registrados en casa", fontSize = 12.sp, color = Emb.TextSecondary)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Registra los alimentos que tienes disponibles en casa. El plan semanal se adaptará automáticamente para sugerirte recetas que utilicen estos ingredientes.",
                fontSize = 11.sp,
                color = Emb.TextSecondary,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Emb.Rosa),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Registrar Alimento", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun AlimentoDisponibleCard(
    alimento: com.example.nutriia.solidos.AlimentoIntroducido,
    onDelete: () -> Unit
) {
    val color = Color(alimento.grupo.colorHex)
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Emb.Border)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(color.copy(alpha = 0.12f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Rounded.Restaurant, null, tint = color, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(alimento.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Emb.TextPrimary)
                    Text(alimento.grupo.label, fontSize = 11.sp, color = Emb.TextSecondary)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(50.dp),
                    color = Emb.Teal.copy(0.12f)
                ) {
                    Text(
                        "Disponible",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 9.sp,
                        color = Emb.Teal,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun LazyListScope.tabDisponiblesEmbarazo(
    alimentos: List<com.example.nutriia.solidos.AlimentoIntroducido>,
    onAddClick: () -> Unit,
    onDelete: (com.example.nutriia.solidos.AlimentoIntroducido) -> Unit
) {
    item {
        Column(Modifier.padding(horizontal = 16.dp)) {
            AlimentosDisponiblesHeader(total = alimentos.size, onAddClick = onAddClick)
        }
        Spacer(Modifier.height(12.dp))
    }
    
    if (alimentos.isEmpty()) {
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Emb.Rosa.copy(.08f)),
                    Alignment.Center
                ) {
                    Icon(Icons.Rounded.Kitchen, null, tint = Emb.Rosa.copy(.5f), modifier = Modifier.size(40.dp))
                }
                Text("No hay alimentos registrados", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Emb.TextPrimary)
                Text("Registra lo que tienes en casa para adaptar tu plan semanal.", fontSize = 12.sp, color = Emb.TextSecondary, textAlign = TextAlign.Center)
            }
        }
    } else {
        items(alimentos, key = { it.id }) { al ->
            AlimentoDisponibleCard(alimento = al, onDelete = { onDelete(al) })
        }
    }
}

@Composable
private fun PlanSemanalVacioCard(onAddClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F9)),
        border = BorderStroke(1.dp, Emb.RosaClaro)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(Emb.Rosa.copy(.1f)),
                Alignment.Center
            ) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = Emb.Rosa, modifier = Modifier.size(32.dp))
            }
            Text("Aún no tienes plan semanal", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Emb.RosaOscuro, textAlign = TextAlign.Center)
            Text(
                "Registra los alimentos e ingredientes que tienes disponibles en casa para generar un menú semanal adaptado.",
                fontSize = 13.sp, color = Emb.TextSecondary, textAlign = TextAlign.Center, lineHeight = 19.sp
            )
            
            // Steps
            Column(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StepRow("1", "Registra tus alimentos", Icons.Rounded.Kitchen)
                StepRow("2", "Agrega lo que tienes en casa", Icons.Rounded.AddCircleOutline)
                StepRow("3", "Aquí verás tu menú semanal personalizado", Icons.Rounded.RestaurantMenu)
            }
            
            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = Emb.Rosa),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Registrar primer alimento", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun StepRow(step: String, label: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Emb.Border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Emb.Rosa),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(12.dp))
        Icon(icon, null, tint = Emb.RosaOscuro, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 12.sp, color = Emb.TextPrimary, fontWeight = FontWeight.Medium)
    }
}
