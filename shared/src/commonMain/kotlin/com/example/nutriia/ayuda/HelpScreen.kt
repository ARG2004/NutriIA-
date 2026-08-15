package com.example.nutriia.ayuda

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.R
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.loc
import kotlinx.coroutines.delay

// ─── Colores ──────────────────────────────────────────────────────────────────

private val BgCrema      = Color(0xFFF9F8F4)
private val GreenPrimary = Color(0xFF4CAF50)
private val GreenDark    = Color(0xFF1B5E20)
private val GreenMid     = Color(0xFF2E7D32)
private val GreenLight   = Color(0xFFEAF3DE)
private val CardWhite    = Color.White
private val GrayText     = Color(0xFF777777)
private val GrayBorder   = Color(0xFFE0E0D8)

// ─── Modelos ──────────────────────────────────────────────────────────────────

data class HelpScreenItem(
    val resId: Int,
    val label: String
)

data class HelpModule(
    val icon: ImageVector,
    val iconBg: Color,
    val iconTint: Color,
    val title: String,
    val subtitle: String,
    val description: String,
    val tip: String,
    val screens: List<HelpScreenItem>
)

// ─── Datos ────────────────────────────────────────────────────────────────────

private fun buildModules(): List<HelpModule> = listOf(

    HelpModule(
        icon        = Icons.Rounded.Dashboard,
        iconBg      = Color(0xFFEAF3DE),
        iconTint    = Color(0xFF2E7D32),
        title       = "Dashboard principal",
        subtitle    = "Perfil del niño y acceso a módulos",
        description = "Al abrir NutriIA verás el perfil activo de tu hijo con su peso, talla y última medición registrada. Desliza lateralmente para cambiar entre perfiles. Desde aquí accedes a todos los módulos de seguimiento y al botón de NutriBot en la parte inferior.",
        tip         = "Los íconos de ayuda, ajustes y salir siempre están visibles en la parte superior derecha.",
        screens     = listOf(
            HelpScreenItem(R.drawable.help_screen_dashboard_modules, "Vista de módulos"),
            HelpScreenItem(R.drawable.help_screen_dashboard_data,    "Perfil con datos")
        )
    ),

    HelpModule(
        icon        = Icons.Rounded.Restaurant,
        iconBg      = Color(0xFFFFF8E1),
        iconTint    = Color(0xFFB8860B),
        title       = "Alimentación",
        subtitle    = "Sólidos, plan semanal y recetas",
        description = "Registra los alimentos sólidos que ya probó tu bebé y lleva control de posibles reacciones alérgicas. El plan semanal se genera automáticamente según la edad, alergias detectadas y alimentos ya introducidos. La sección de recetas filtra opciones aptas para tu hijo.",
        tip         = "Revisa la alerta de alérgenos pendientes para llevar una introducción segura y ordenada.",
        screens     = listOf(
            HelpScreenItem(R.drawable.help_screen_food_registered, "Alimentos registrados"),
            HelpScreenItem(R.drawable.help_screen_food_plan,       "Plan semanal"),
            HelpScreenItem(R.drawable.help_screen_food_recipes,    "Recetas mexicanas")
        )
    ),

    HelpModule(
        icon        = Icons.AutoMirrored.Rounded.TrendingUp,
        iconBg      = Color(0xFFE8F5E9),
        iconTint    = Color(0xFF2E7D32),
        title       = "Crecimiento",
        subtitle    = "Peso, talla, IMC y gráficas OMS",
        description = "Registra mediciones periódicas de peso y talla. El módulo calcula el IMC y lo ubica dentro de los percentiles de la OMS para 0–60 meses. Consulta el historial completo de mediciones y las gráficas de peso y talla por edad.",
        tip         = "Para menores de 2 años se usa peso/longitud en lugar del IMC estándar, tal como recomienda la OMS.",
        screens     = listOf(
            HelpScreenItem(R.drawable.help_screen_growth_imc,     "IMC actual"),
            HelpScreenItem(R.drawable.help_screen_growth_detail,  "Detalle"),
            HelpScreenItem(R.drawable.help_screen_growth_history, "Historial"),
            HelpScreenItem(R.drawable.help_screen_growth_charts,  "Gráficas OMS")
        )
    ),

    HelpModule(
        icon        = Icons.Rounded.PieChart,
        iconBg      = Color(0xFFEDE7F6),
        iconTint    = Color(0xFF5E35B1),
        title       = "Nutrientes",
        subtitle    = "Calorías, macros y micronutrientes",
        description = "Lleva el control diario de calorías, macronutrientes y micronutrientes. Selecciona si registras ayer, hoy o mañana. El módulo muestra la recomendación de textura y número de comidas según el rango de edad activo del niño.",
        tip         = "El conteo inicia en cero cada día. Usa «Anotar lo que comió» para registrar cada comida.",
        screens     = listOf(
            HelpScreenItem(R.drawable.help_screen_nutrients, "Registro diario")
        )
    ),

    HelpModule(
        icon        = Icons.Rounded.CameraAlt,
        iconBg      = Color(0xFFE3F2FD),
        iconTint    = Color(0xFF1565C0),
        title       = "Análisis NutriIA",
        subtitle    = "Escaneo de alimentos con IA",
        description = "Toma o sube una foto del platillo. La IA identifica el alimento, obtiene sus datos nutricionales y genera un análisis personalizado según la edad y el perfil del niño. Asegúrate de que el alimento esté bien encuadrado y los ingredientes sean visibles.",
        tip         = "Coloca el alimento dentro del marco con buena iluminación para obtener el mejor resultado.",
        screens     = listOf(
            HelpScreenItem(R.drawable.help_screen_analysis, "Escanear alimento")
        )
    ),

    HelpModule(
        icon        = Icons.Rounded.PersonAdd,
        iconBg      = Color(0xFFE8F5E9),
        iconTint    = Color(0xFF2E7D32),
        title       = "Mi nutriólogo / pediatra",
        subtitle    = "Vinculación con tu especialista",
        description = "Conecta el perfil del niño con su nutriólogo o pediatra mediante código único, correo o directorio. Una vez vinculado, el especialista puede consultar el seguimiento en tiempo real. Puedes tener más de un especialista vinculado.",
        tip         = "Solicita el código NUTRI de tu especialista para vincularlo de forma segura y directa.",
        screens     = listOf(
            HelpScreenItem(R.drawable.help_screen_specialist, "Vinculación")
        )
    ),

    HelpModule(
        icon        = Icons.Rounded.NotificationsActive,
        iconBg      = Color(0xFFFFF3E0),
        iconTint    = Color(0xFFE65100),
        title       = "Alertas",
        subtitle    = "Recordatorios inteligentes",
        description = "Programa recordatorios para tomas de comida, vacunas, citas médicas y registros de medición. Las alertas se organizan por categoría. Toca «Nueva alerta» para crear un recordatorio personalizado.",
        tip         = "Activa los permisos de notificación del sistema para recibir los recordatorios en el momento exacto.",
        screens     = listOf(
            HelpScreenItem(R.drawable.help_screen_alerts, "Mis alertas")
        )
    )
)

// ─── Pantalla principal ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(onNavigateBack: () -> Unit) {

    // ─────────────────────────────────────────────────────────────────────────
    // ACCESIBILIDAD
    // ─────────────────────────────────────────────────────────────────────────
    val a11yVm: AccessibilityViewModel = viewModel()
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esBlind      = a11yMode == AccessibilityMode.BLIND

    fun loc(es: String, en: String) = idiomaActual.loc(es, en)

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar(loc(
                "Centro de ayuda de NutriIA. Aquí puedes aprender a usar cada módulo de la aplicación. Selecciona una categoría para escuchar su descripción.",
                "NutriIA Help Center. Here you can learn how to use each module. Select a category to hear its description."
            ))
        }
    }

    val modules = remember { buildModules() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text       = loc("Sección de ayuda", "Help Section"),
                        fontWeight = FontWeight.SemiBold,
                        fontSize   = 17.sp,
                        color      = GreenDark
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Box(
                            modifier         = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(GreenLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = loc("Regresar", "Go back"),
                                tint               = GreenPrimary,
                                modifier           = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgCrema)
            )
        },
        containerColor = BgCrema
    ) { innerPadding ->

        LazyColumn(
            modifier            = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding      = PaddingValues(
                start  = 16.dp,
                end    = 16.dp,
                top    = 20.dp,
                bottom = 48.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            item {
                HeroCard(idiomaActual)
                Spacer(Modifier.height(16.dp))
            }

            item {
                SectionLabel(loc("Asistente inteligente", "Smart Assistant"))
                Spacer(Modifier.height(8.dp))
                NutriBotBanner(idiomaActual, esBlind, a11yVm)
                Spacer(Modifier.height(20.dp))
                SectionLabel(loc("Módulos del sistema", "System Modules"))
                Spacer(Modifier.height(8.dp))
            }

            itemsIndexed(modules) { index, module ->
                AnimatedModuleCard(
                    module = module, 
                    staggerIndex = index, 
                    esBlind = esBlind, 
                    a11yVm = a11yVm, 
                    idioma = idiomaActual
                )
            }

            item {
                Spacer(Modifier.height(12.dp))
                SectionLabel(loc("Información general", "General Information"))
                Spacer(Modifier.height(8.dp))
                WhatIsNutriaCard(idiomaActual)
            }
        }
    }
}

// ─── Hero card ────────────────────────────────────────────────────────────────

@Composable
private fun HeroCard(idioma: IdiomaVoz) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(420)) + slideInVertically(tween(420, easing = EaseOutCubic)) { it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(GreenDark)
                .padding(22.dp)
        ) {
            Column {
                Box(
                    modifier         = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Info,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text       = loc("Bienvenido al centro de ayuda", "Welcome to the Help Center"),
                    fontSize   = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                    lineHeight = 26.sp
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text       = loc("Explora cada módulo y aprende cómo dar el mejor seguimiento nutricional a tu infante.", "Explore each module and learn how to provide the best nutritional monitoring for your child."),
                    fontSize   = 13.sp,
                    color      = Color.White.copy(alpha = 0.78f),
                    lineHeight = 19.sp
                )
            }
        }
    }
}

// ─── NutriBot banner ──────────────────────────────────────────────────────────

@Composable
private fun NutriBotBanner(idioma: IdiomaVoz, esBlind: Boolean, a11yVm: AccessibilityViewModel) {
    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    Card(
        modifier  = Modifier.fillMaxWidth().clickable {
            if (esBlind) {
                a11yVm.hablar(loc("Puedes consultar tus dudas con NutriBot desde el dashboard principal.", "You can consult NutriBot from the main dashboard."))
            }
        },
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(0.5.dp, GrayBorder)
    ) {
        Row(
            modifier          = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier         = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GreenLight),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector        = Icons.Rounded.SmartToy,
                    contentDescription = null,
                    tint               = GreenDark,
                    modifier           = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("NutriBot", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GreenDark)
                Spacer(Modifier.height(2.dp))
                Text(
                    text     = loc("Consulta dudas sobre nutrición infantil al instante", "Consult questions about child nutrition instantly"),
                    fontSize = 12.sp,
                    color    = GrayText
                )
            }
            Icon(
                imageVector        = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint               = GrayText,
                modifier           = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Etiqueta de sección ──────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Medium,
        color         = GrayText,
        letterSpacing = 0.08.sp
    )
}

// ─── Tarjeta de módulo con animación stagger ──────────────────────────────────

@Composable
private fun AnimatedModuleCard(
    module: HelpModule, 
    staggerIndex: Int, 
    esBlind: Boolean, 
    a11yVm: AccessibilityViewModel,
    idioma: IdiomaVoz
) {

    var visible  by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }

    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    LaunchedEffect(Unit) {
        delay(staggerIndex * 55L)
        visible = true
    }

    val chevronDeg by animateFloatAsState(
        targetValue   = if (expanded) 90f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label         = "chevron_$staggerIndex"
    )

    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(340)) + slideInVertically(tween(340, easing = EaseOutCubic)) { it / 3 }
    ) {
        Card(
            modifier  = Modifier.fillMaxWidth().semantics {
                contentDescription = loc("${module.title}. ${module.subtitle}. Toca para expandir.", "${module.title}. ${module.subtitle}. Tap to expand.")
            },
            shape     = RoundedCornerShape(14.dp),
            colors    = CardDefaults.cardColors(containerColor = CardWhite),
            elevation = CardDefaults.cardElevation(0.dp),
            border    = BorderStroke(
                width = if (expanded) 1.dp else 0.5.dp,
                color = if (expanded) GreenPrimary else GrayBorder
            )
        ) {
            Column {

                // Header táctil
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication        = null
                        ) { 
                            expanded = !expanded 
                            if (esBlind && expanded) {
                                a11yVm.hablar(module.description)
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier         = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(module.iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector        = module.icon,
                            contentDescription = null,
                            tint               = module.iconTint,
                            modifier           = Modifier.size(19.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = module.title,
                            fontSize   = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = GreenDark
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(text = module.subtitle, fontSize = 12.sp, color = GrayText)
                    }
                    Icon(
                        imageVector        = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint               = if (expanded) GreenPrimary else GrayText,
                        modifier           = Modifier
                            .size(20.dp)
                            .rotate(chevronDeg)
                    )
                }

                // Contenido expandible
                AnimatedVisibility(
                    visible = expanded,
                    enter   = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(tween(220)),
                    exit    = shrinkVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeOut(tween(160))
                ) {
                    Column(
                        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 16.dp)
                    ) {
                        if (module.screens.isNotEmpty()) {
                            ScreenCarousel(screens = module.screens, idioma = idioma)
                            Spacer(Modifier.height(14.dp))
                        }

                        Text(
                            text       = module.description,
                            fontSize   = 13.sp,
                            color      = Color(0xFF555555),
                            lineHeight = 19.sp
                        )

                        Spacer(Modifier.height(12.dp))
                        TipStrip(module.tip)
                    }
                }
            }
        }
    }
}

// ─── Carrusel amplio con puntos indicadores ───────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ScreenCarousel(screens: List<HelpScreenItem>, idioma: IdiomaVoz) {

    val pagerState = rememberPagerState(pageCount = { screens.size })
    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    Column(horizontalAlignment = Alignment.CenterHorizontally) {

        HorizontalPager(
            state          = pagerState,
            modifier       = Modifier.fillMaxWidth().semantics {
                contentDescription = loc("Carrusel de imágenes. Desliza para ver capturas de pantalla.", "Image carousel. Swipe to see screenshots.")
            },
            pageSpacing    = 12.dp,
            contentPadding = PaddingValues(horizontal = 0.dp)
        ) { page ->
            val screen = screens[page]
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                // Marco con fondo suave — imagen a tamaño real sin recorte
                Box(
                    modifier         = Modifier
                        .fillMaxWidth()
                        .height(340.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F0EC)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter            = painterResource(id = screen.resId),
                        contentDescription = loc("Imagen de ${screen.label}", "Image of ${screen.label}"),
                        contentScale       = ContentScale.Fit,
                        modifier           = Modifier.fillMaxSize()
                    )
                }

                Spacer(Modifier.height(10.dp))

                // Nombre de la pantalla
                Text(
                    text       = screen.label,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color      = GreenMid
                )
            }
        }

        // Puntos paginadores animados
        if (screens.size > 1) {
            Spacer(Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                repeat(screens.size) { index ->
                    val selected = pagerState.currentPage == index
                    val dotWidth by animateDpAsState(
                        targetValue   = if (selected) 20.dp else 6.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMedium),
                        label         = "dot_w_$index"
                    )
                    val dotColor by animateColorAsState(
                        targetValue   = if (selected) GreenPrimary else Color(0xFFCCCCC4),
                        animationSpec = tween(200),
                        label         = "dot_c_$index"
                    )
                    Box(
                        modifier = Modifier
                            .width(dotWidth)
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(dotColor)
                    )
                }
            }
        }
    }
}

// ─── Tip strip ────────────────────────────────────────────────────────────────

@Composable
private fun TipStrip(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
            .background(GreenLight)
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .heightIn(min = 40.dp)
                .background(GreenPrimary)
        )
        Text(
            text       = text,
            fontSize   = 12.sp,
            color      = GreenMid,
            lineHeight = 17.sp,
            modifier   = Modifier.padding(horizontal = 10.dp, vertical = 10.dp)
        )
    }
}

// ─── ¿Qué es NutriIA? ─────────────────────────────────────────────────────────

@Composable
private fun WhatIsNutriaCard(idioma: IdiomaVoz) {
    fun loc(es: String, en: String) = if (idioma == IdiomaVoz.INGLES) en else es

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(0.dp),
        border    = BorderStroke(0.5.dp, GrayBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier         = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(GreenLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.Info,
                        contentDescription = null,
                        tint               = GreenDark,
                        modifier           = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text       = loc("¿Qué es NutriIA?", "What is NutriIA?"),
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color      = GreenDark
                    )
                    Text(
                        text     = loc("Propósito y recomendaciones de uso", "Purpose and usage recommendations"),
                        fontSize = 12.sp,
                        color    = GrayText
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text       = loc("NutriIA es un asistente inteligente diseñado para padres y nutriólogos en México. Ayuda a dar seguimiento al desarrollo de infantes mediante análisis visuales e inteligencia artificial, adaptado a las recomendaciones de la OMS y al contexto regional mexicano.", "NutriIA is a smart assistant designed for parents and nutritionists in Mexico. It helps track child development through visual analysis and AI, adapted to WHO recommendations and the Mexican regional context."),
                fontSize   = 13.sp,
                color      = Color(0xFF555555),
                lineHeight = 19.sp
            )
            Spacer(Modifier.height(12.dp))
            TipStrip(loc("El contenido sugerido por NutriBot es orientativo. Siempre consulta con tu pediatra o nutriólogo ante cualquier duda clínica.", "The content suggested by NutriBot is for guidance. Always consult with your pediatrician or nutritionist for any clinical doubts."))
        }
    }
}
