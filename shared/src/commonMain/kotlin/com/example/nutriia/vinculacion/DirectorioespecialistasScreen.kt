package com.example.nutriia.vinculacion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

// ─── Paleta NutriIA ──────────────────────────────────────────────────────────
private val DGreen     = Color(0xFF689F38)
private val DDarkGreen = Color(0xFF33691E)
private val DBgCrema   = Color(0xFFF8F9F3)
private val DCardWhite = Color.White
private val DTeal      = Color(0xFF4DB6AC)

private val avatarPool = listOf(
    Color(0xFFEC9BBF), Color(0xFF9C8FE0), Color(0xFFFFAB76),
    Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFF64B5F6)
)

/**
 * Pantalla completa del directorio de especialistas.
 */
@Composable
fun DirectorioNutriologosScreen(
    viewModel:   VinculacionViewModel = viewModel(),
    padreNombre: String,
    childId:     String,
    childNombre: String,
    onBack:      () -> Unit = {},
    onVinculado: () -> Unit = {}
) {
    val directorio          by viewModel.directorio.collectAsState()
    val cargando            by viewModel.cargandoDirectorio.collectAsState()
    val cargandoAccion      by viewModel.cargando.collectAsState()
    val nutriologoSeleccionado by viewModel.nutriologoSeleccionado.collectAsState()
    val exito               by viewModel.exito.collectAsState()
    val error               by viewModel.error.collectAsState()

    var queryTexto by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // 1. Carga inicial
    LaunchedEffect(Unit) { viewModel.cargarDirectorio() }

    // 2. Buscador con Debounce (Evita peticiones excesivas a Firebase)
    LaunchedEffect(queryTexto) {
        if (queryTexto.isBlank()) {
            viewModel.cargarDirectorio()
            return@LaunchedEffect
        }
        delay(450)
        viewModel.buscarEnDirectorio(queryTexto)
    }

    // 3. Gestión de mensajes (Snackbar)
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    // 4. Navegación tras éxito
    LaunchedEffect(exito) {
        exito?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarExito()
            onVinculado()
        }
    }

    Scaffold(
        containerColor = DBgCrema,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        topBar         = { DirectorioTopBar(onBack = onBack) }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Buscador
            item {
                Spacer(Modifier.height(12.dp))
                BuscadorDirectorio(
                    query    = queryTexto,
                    onChange = { queryTexto = it },
                    onClear  = { queryTexto = ""; viewModel.cargarDirectorio() }
                )
                Spacer(Modifier.height(12.dp))
                PacienteChip(childNombre = childNombre)
                Spacer(Modifier.height(16.dp))
            }

            // Estado de Carga
            if (cargando) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 50.dp), Alignment.Center) {
                        CircularProgressIndicator(color = DGreen, strokeWidth = 3.dp)
                    }
                }
            }
            // Lista Vacía
            else if (directorio.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No encontramos especialistas", color = Color.Gray, fontWeight = FontWeight.Medium)
                            Text("Intenta con otro nombre o especialidad", fontSize = 12.sp, color = Color.LightGray)
                        }
                    }
                }
            }
            // Resultados
            else {
                item {
                    Text(
                        "${directorio.size} especialistas encontrados",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                items(directorio, key = { it.uid }) { nutriologo ->
                    NutriologoDirectorioCard(
                        nutriologo  = nutriologo,
                        onSolicitar = { viewModel.seleccionarNutriologoDelDirectorio(nutriologo) }
                    )
                }
            }
        }
    }

    // Modal de Confirmación
    nutriologoSeleccionado?.let { nutriologo ->
        ConfirmarVinculacionSheet(
            nutriologo  = nutriologo,
            childNombre = childNombre,
            estaCargando = cargandoAccion,
            onConfirmar = {
                viewModel.solicitarVinculacion(
                    padreNombre = padreNombre,
                    childId     = childId,
                    childNombre = childNombre
                )
            },
            onDismiss = { viewModel.limpiarBusqueda() }
        )
    }
}

@Composable
private fun DirectorioTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = DDarkGreen)
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text("Especialistas", fontSize = 22.sp, fontWeight = FontWeight.Black, color = DDarkGreen)
            Text("Nutriólogos y Pediatras certificados", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun BuscadorDirectorio(query: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text("Nombre, clínica o especialidad...", fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = DGreen) },
        trailingIcon = {
            AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, null, tint = Color.Gray) }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = DGreen,
            unfocusedBorderColor = Color.LightGray.copy(0.4f),
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    )
}

@Composable
private fun PacienteChip(childNombre: String) {
    Surface(
        modifier = Modifier.padding(horizontal = 20.dp),
        color = DTeal.copy(alpha = 0.08f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, DTeal.copy(0.2f))
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.ChildCare, null, tint = DTeal, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text("Vinculando a: ", fontSize = 12.sp, color = Color.Gray)
            Text(childNombre, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DTeal)
        }
    }
}

@Composable
private fun NutriologoDirectorioCard(nutriologo: NutriologoPublico, onSolicitar: () -> Unit) {
    val avatarColor = avatarPool[nutriologo.uid.hashCode().let { if (it < 0) -it else it } % avatarPool.size]

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Avatar
            Box(Modifier.size(56.dp).background(avatarColor.copy(0.15f), CircleShape).border(2.dp, avatarColor.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Text(nutriologo.nombre.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = avatarColor)
            }

            Spacer(Modifier.width(16.dp))

            // Info
            Column(Modifier.weight(1f)) {
                Text(nutriologo.nombre, fontWeight = FontWeight.ExtraBold, color = DDarkGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(nutriologo.especialidad.ifBlank { "Especialista NutriIA" }, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, null, tint = DGreen, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Código: ${nutriologo.codigo}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DGreen)
                }
            }

            // Botón Acción
            IconButton(
                onClick = onSolicitar,
                modifier = Modifier.background(DGreen.copy(0.1f), RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.Rounded.PersonAdd, "Solicitar", tint = DGreen)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmarVinculacionSheet(
    nutriologo: NutriologoPublico,
    childNombre: String,
    estaCargando: Boolean,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DBgCrema,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(60.dp).background(DGreen.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.ContactMail, null, tint = DGreen, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Confirmar Solicitud", fontSize = 20.sp, fontWeight = FontWeight.Black, color = DDarkGreen)
            Spacer(Modifier.height(12.dp))
            Text(
                "¿Deseas enviar una solicitud de vinculación a ${nutriologo.nombre} para atender a $childNombre?",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.Gray
            )

            Spacer(Modifier.height(32.dp))

            Button(
                onClick = onConfirmar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DGreen),
                shape = RoundedCornerShape(18.dp),
                enabled = !estaCargando
            ) {
                if (estaCargando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                else {
                    Icon(Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Enviar Solicitud Ahora", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Quizás más tarde", color = Color.Gray)
            }
        }
    }
}