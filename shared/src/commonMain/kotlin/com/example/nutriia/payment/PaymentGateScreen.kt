package com.example.nutriia.payment

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private val PGreen     = Color(0xFF689F38)
private val PDarkGreen = Color(0xFF33691E)
private val PBgCrema   = Color(0xFFF8F9F3)
private val PayPalBlue = Color(0xFF003087)
private val PayPalGold = Color(0xFFFFB700)

@Composable
fun PaymentGateScreen(
    viewModel:        PaymentViewModel = viewModel(),
    nutriologoUid:    String,
    nutriologoNombre: String,
    childId:          String,
    childNombre:      String,
    onPagoConfirmado: () -> Unit,
    onCancelar:       () -> Unit
) {
    val state   by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }

    // Crear el pago pendiente al entrar a la pantalla
    LaunchedEffect(Unit) {
        viewModel.iniciarPago(nutriologoUid = nutriologoUid, childId = childId)
    }

    // Cuando el pago se confirma (deep link regresó exitoso)
    LaunchedEffect(state.pagoCompletado) {
        if (state.pagoCompletado) onPagoConfirmado()
    }

    // Errores
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        containerColor = PBgCrema,
        snackbarHost   = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    viewModel.onPagoCancelado()
                    onCancelar()
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Cancelar", tint = PDarkGreen)
                }
                Spacer(Modifier.width(4.dp))
                Column {
                    Text("Pago seguro", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PDarkGreen)
                    Text("Teleconsulta con $nutriologoNombre · $childNombre",
                        fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(Modifier.weight(1f))
                Surface(color = PGreen.copy(0.08f), shape = RoundedCornerShape(10.dp)) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Lock, null, tint = PGreen, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("SSL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PDarkGreen)
                    }
                }
            }

            // ── Resumen del cobro ──────────────────────────────────────────────
            Card(
                modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                shape     = RoundedCornerShape(20.dp),
                colors    = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PGreen.copy(0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.VideoCall, null, tint = PGreen, modifier = Modifier.size(24.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Teleconsulta", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PDarkGreen)
                        Text("Dr. $nutriologoNombre · $childNombre",
                            fontSize = 11.sp, color = Color.Gray)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${"%.2f".format(PaymentViewModel.PRECIO_CENTAVOS / 100.0)} ${PaymentViewModel.MONEDA}",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize   = 18.sp,
                            color      = PDarkGreen
                        )
                        Text("pago único", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ── Contenido central ──────────────────────────────────────────────
            when {
                // Creando el registro en Firestore...
                state.cargando -> {
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = PGreen, strokeWidth = 3.dp)
                            Spacer(Modifier.height(16.dp))
                            Text("Preparando pago…", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                // Pago completado (deep link recibido, confirmando en Firestore)
                state.pagoCompletado -> {
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                null,
                                tint     = PGreen,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("¡Pago confirmado!", fontWeight = FontWeight.Bold,
                                fontSize = 18.sp, color = PDarkGreen)
                            Text("Iniciando teleconsulta…", fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                }

                // Listo para pagar — el pago pendiente ya existe en Firestore
                state.pagoActual != null -> {
                    Column(
                        modifier            = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Logo PayPal (simulado con texto + color)
                        Surface(
                            shape  = RoundedCornerShape(16.dp),
                            color  = PayPalBlue,
                            modifier = Modifier.width(160.dp).height(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "PayPal",
                                    color      = Color.White,
                                    fontSize   = 26.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(Modifier.height(24.dp))

                        Text(
                            "Serás redirigido al sitio seguro de PayPal para completar tu pago.",
                            fontSize   = 14.sp,
                            color      = Color.Gray,
                            textAlign  = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            "Al completar el pago en PayPal, regresarás automáticamente a la app.",
                            fontSize   = 12.sp,
                            color      = Color.LightGray,
                            textAlign  = TextAlign.Center,
                            lineHeight = 18.sp
                        )

                        Spacer(Modifier.height(32.dp))

                        // ── Botón principal — abre el navegador ───────────────
                        Button(
                            onClick  = { viewModel.abrirPayPalEnNavegador(context) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            colors   = ButtonDefaults.buttonColors(containerColor = PayPalGold),
                            shape    = RoundedCornerShape(16.dp)
                        ) {
                            Icon(Icons.Rounded.OpenInBrowser, null,
                                tint = PayPalBlue, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Pagar con PayPal",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize   = 16.sp,
                                color      = PayPalBlue
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        // ── Botón cancelar ────────────────────────────────────
                        TextButton(
                            onClick  = {
                                viewModel.onPagoCancelado()
                                onCancelar()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancelar", color = Color.Gray)
                        }

                        Spacer(Modifier.height(24.dp))

                        // Nota de seguridad
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Rounded.Security, null,
                                tint = Color.LightGray, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                "Pago procesado por PayPal — NutriIA nunca almacena tus datos bancarios",
                                fontSize  = 10.sp,
                                color     = Color.LightGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Error al crear el pago
                else -> {
                    Box(Modifier.fillMaxWidth().weight(1f), Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(Icons.Rounded.ErrorOutline, null,
                                tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(16.dp))
                            Text("No se pudo iniciar el pago.",
                                color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.iniciarPago(nutriologoUid, childId) },
                                colors  = ButtonDefaults.buttonColors(containerColor = PGreen)
                            ) {
                                Text("Reintentar")
                            }
                        }
                    }
                }
            }
        }
    }
}