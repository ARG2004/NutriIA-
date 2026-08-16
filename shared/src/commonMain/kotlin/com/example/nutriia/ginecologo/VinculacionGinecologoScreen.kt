package com.example.nutriia.ginecologo

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.teleconsulta.TeleconsultaViewModel

// ─── Colores Embarazo ────────────────────────────────────────────────────────
private val EmbRosa       = Color(0xFFEC9BBF)
private val EmbRosaOscuro = Color(0xFFD4679A)
private val EmbMorado     = Color(0xFF9C8FE0)
private val EmbTeal       = Color(0xFF4DB6AC)
private val EmbFondo      = Color(0xFFFFF5F9)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VinculacionGinecologoScreen(
    viewModel: GinecologoViewModel = viewModel(),
    onNavigateToDirectorio: () -> Unit,
    onBack: () -> Unit
) {
    val vinculacion by viewModel.vinculacionActual.collectAsState()
    val cargando    by viewModel.cargando.collectAsState()
    val exito       by viewModel.exito.collectAsState()
    val error       by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val esBlind = a11yMode == AccessibilityMode.BLIND

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar("Módulo de vinculación con ginecólogo. Aquí puedes ver tu ginecólogo vinculado o buscar uno nuevo en el directorio.")
        }
        viewModel.initComoMama()
    }

    LaunchedEffect(exito) {
        exito?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarExito()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    Scaffold(
        containerColor = EmbFondo,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Mi Ginecólogo/a", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver")
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
            when {
                vinculacion == null || vinculacion?.estado == EstadoVinculacionEmbarazo.RECHAZADO || vinculacion?.estado == EstadoVinculacionEmbarazo.REVOCADO -> {
                    EstadoSinVinculo(onNavigateToDirectorio)
                }
                vinculacion?.estado == EstadoVinculacionEmbarazo.PENDIENTE -> {
                    EstadoPendiente(vinculacion!!, onRevocar = { viewModel.revocarVinculacion(vinculacion!!.id) })
                }
                vinculacion?.estado == EstadoVinculacionEmbarazo.ACTIVO -> {
                    EstadoActivo(
                        vinculacion = vinculacion!!,
                        onRevocar = { viewModel.revocarVinculacion(vinculacion!!.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EstadoSinVinculo(onNavigateToDirectorio: () -> Unit) {
    val a11yMode = LocalAccessibilityMode.current
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

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
            Icon(Icons.Rounded.VerifiedUser, null, tint = EmbRosa, modifier = Modifier.size(50.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Sin Ginecólogo Vinculado",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Vincula un ginecólogo de confianza para compartir tu historial y dar seguimiento en tiempo real.",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNavigateToDirectorio,
            colors = ButtonDefaults.buttonColors(containerColor = EmbRosaOscuro),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(if (esAccesible) 70.dp else 48.dp)
        ) {
            Text("Buscar Ginecólogo", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EstadoPendiente(
    vinculacion: VinculacionEmbarazo,
    onRevocar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(EmbMorado.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = EmbMorado, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Solicitud Enviada", fontWeight = FontWeight.Black, color = EmbMorado)
                    Text("Pendiente de aprobación", fontSize = 12.sp, color = Color.Gray)
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(Modifier.height(20.dp))
            Text("Ginecólogo/a:", fontSize = 12.sp, color = Color.Gray)
            Text(vinculacion.ginecologoNombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            Spacer(Modifier.height(16.dp))
            Text(
                "Una vez que el profesional apruebe tu vinculación, se habilitará el canal de comunicación.",
                fontSize = 13.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(32.dp))
            OutlinedButton(
                onClick = onRevocar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
            ) {
                Text("Cancelar Solicitud")
            }
        }
    }
}

@Composable
private fun EstadoActivo(
    vinculacion: VinculacionEmbarazo,
    onRevocar: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(EmbTeal.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = EmbTeal, modifier = Modifier.size(28.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("Vínculo Activo", fontWeight = FontWeight.Black, color = EmbTeal)
                    Text("Médico de cabecera", fontSize = 12.sp, color = Color.Gray)
                }
            }
            
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            Spacer(Modifier.height(20.dp))
            
            Text("Ginecólogo/a:", fontSize = 12.sp, color = Color.Gray)
            Text(vinculacion.ginecologoNombre, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
            
            Spacer(Modifier.height(24.dp))
            
            Surface(
                color = EmbTeal.copy(alpha = 0.05f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Info, null, tint = EmbTeal, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "Tu médico ahora puede ver tu progreso, registrar citas y hacer seguimiento de tu embarazo.",
                        fontSize = 12.sp,
                        color = Color.DarkGray,
                        lineHeight = 16.sp
                    )
                }
            }
            
            Spacer(Modifier.height(32.dp))
            
            TextButton(
                onClick = onRevocar,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("Revocar vínculo médico", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}
