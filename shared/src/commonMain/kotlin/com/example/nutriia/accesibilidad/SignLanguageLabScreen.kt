package com.example.nutriia.accesibilidad

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LabBg = Color(0xFF0F172A)
private val LabCardBg = Color(0xFF1E293B)
private val LabGreen = Color(0xFF10B981)
private val LabBlue = Color(0xFF38BDF8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignLanguageLabScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var textoEscritoPrueba by remember { mutableStateOf("") }
    var senaSeleccionadaGuia by remember { mutableStateOf<SenaLSMInfo?>(null) }
    var categoriaFiltro by remember { mutableStateOf<CategoriaSena?>(null) }
    var senaReconocidaReciente by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Laboratorio de Señas LSM",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "Lengua de Señas Mexicana • DIELSEME / CONADIS",
                            fontSize = 11.sp,
                            color = LabGreen
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vibrateTap(haptic)
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LabBg)
            )
        },
        containerColor = LabBg,
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            // Banner Informativo
            item {
                Surface(
                    color = LabCardBg,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = LabGreen.copy(alpha = 0.15f),
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.SignLanguage, contentDescription = null, tint = LabGreen, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Reconocimiento Natural en Tiempo Real",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Prueba el abecedario A-Z relajado y las señas comunicativas de NutrIA con tu cámara.",
                                color = Color.LightGray,
                                fontSize = 11.sp,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            // Vista de Cámara / Interacción
            item {
                Text(
                    text = "📹 Prueba Interactiva",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(8.dp))

                SignLanguageCameraView(
                    textoActual = textoEscritoPrueba,
                    onTextoChange = { nuevoTexto ->
                        textoEscritoPrueba = nuevoTexto
                    },
                    colorPrimario = LabGreen,
                    onSenaComunicativaDetectada = { sena ->
                        senaReconocidaReciente = "${sena.emoji} ${sena.nombre}"
                    }
                )
            }

            // Notificación de última seña reconocida
            if (senaReconocidaReciente != null) {
                item {
                    Surface(
                        color = LabGreen.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, LabGreen.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = LabGreen, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Última seña detectada: $senaReconocidaReciente",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Catálogo Oficial de Señas Comunicativas LSM
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📖 Catálogo Oficial de Señas LSM",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "12 Conceptos",
                            color = LabBlue,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Filtros por Categoría
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            FilterChip(
                                selected = categoriaFiltro == null,
                                onClick = {
                                    vibrateTap(haptic)
                                    categoriaFiltro = null
                                },
                                label = { Text("Todas (12)") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LabGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                        items(CategoriaSena.values()) { cat ->
                            val label = when (cat) {
                                CategoriaSena.ALIMENTACION -> "🍼 Alimentación"
                                CategoriaSena.SALUD_ALERTA -> "🚨 Salud y Alertas"
                                CategoriaSena.CLINICA -> "🩺 Clínica"
                                CategoriaSena.CONTROL -> "⚙️ Control"
                            }
                            FilterChip(
                                selected = categoriaFiltro == cat,
                                onClick = {
                                    vibrateTap(haptic)
                                    categoriaFiltro = cat
                                },
                                label = { Text(label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = LabGreen,
                                    selectedLabelColor = Color.Black
                                )
                            )
                        }
                    }
                }
            }

            // Lista de Tarjetas de Señas Comunicativas
            val senasFiltradas = if (categoriaFiltro == null) {
                CATALOGO_SENAS_COMUNICATIVAS
            } else {
                CATALOGO_SENAS_COMUNICATIVAS.filter { it.categoria == categoriaFiltro }
            }

            items(senasFiltradas) { sena ->
                val esSeleccionada = senaSeleccionadaGuia?.id == sena.id
                Surface(
                    color = if (esSeleccionada) Color(0xFF1E3A5F) else LabCardBg,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (esSeleccionada) LabBlue else Color(0xFF334155)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            vibrateTap(haptic)
                            senaSeleccionadaGuia = if (esSeleccionada) null else sena
                        }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(sena.emoji, fontSize = 28.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sena.nombre,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = sena.sugerenciaFrase,
                                    color = LabGreen,
                                    fontSize = 12.sp
                                )
                            }
                            Icon(
                                if (esSeleccionada) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                contentDescription = null,
                                tint = Color.LightGray
                            )
                        }

                        if (esSeleccionada) {
                            Spacer(Modifier.height(10.dp))
                            HorizontalDivider(color = Color(0xFF334155))
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = "Cómo realizar la seña (Parámetros LSM):",
                                color = LabBlue,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = sena.descripcionLSM,
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
}
