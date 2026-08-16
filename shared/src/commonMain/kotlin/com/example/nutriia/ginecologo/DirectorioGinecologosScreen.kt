package com.example.nutriia.ginecologo

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import kotlinx.coroutines.delay

// ─── Paleta Embarazo ─────────────────────────────────────────────────────────
private val EmbRosa       = Color(0xFFEC9BBF)
private val EmbRosaOscuro = Color(0xFFD4679A)
private val EmbMorado     = Color(0xFF9C8FE0)
private val EmbTeal       = Color(0xFF4DB6AC)
private val EmbFondo      = Color(0xFFFFF5F9)

private val avatarPool = listOf(
    Color(0xFFEC9BBF), Color(0xFF9C8FE0), Color(0xFFFFAB76),
    Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFF64B5F6)
)

@Composable
fun DirectorioGinecologosScreen(
    viewModel: GinecologoViewModel = viewModel(),
    mamaNombre: String,
    onBack: () -> Unit = {},
    onVinculado: () -> Unit = {}
) {
    val directorio      by viewModel.directorio.collectAsState()
    val cargando        by viewModel.cargandoDirectorio.collectAsState()
    val cargandoAccion  by viewModel.cargando.collectAsState()
    val ginecologoEncontrado by viewModel.ginecologoEncontrado.collectAsState()
    val exito           by viewModel.exito.collectAsState()
    val error           by viewModel.error.collectAsState()

    var queryTexto by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val esBlind = a11yMode == AccessibilityMode.BLIND

    LaunchedEffect(Unit) {
        if (esBlind) {
            a11yVm.hablar("Directorio de ginecólogos. Busca por nombre o especialidad para solicitar vinculación.")
        }
        viewModel.cargarDirectorio()
    }

    LaunchedEffect(queryTexto) {
        if (queryTexto.isBlank()) {
            viewModel.cargarDirectorio()
            return@LaunchedEffect
        }
        delay(450)
        viewModel.buscarEnDirectorio(queryTexto)
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarError()
        }
    }

    LaunchedEffect(exito) {
        exito?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.limpiarExito()
            onVinculado()
        }
    }

    Scaffold(
        containerColor = EmbFondo,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            DirectorioTopBar(onBack = onBack)
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                BuscadorDirectorio(
                    query = queryTexto,
                    onChange = { queryTexto = it },
                    onClear = { queryTexto = ""; viewModel.cargarDirectorio() }
                )
                Spacer(Modifier.height(16.dp))
            }

            if (cargando) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 50.dp), Alignment.Center) {
                        CircularProgressIndicator(color = EmbRosa, strokeWidth = 3.dp)
                    }
                }
            } else if (directorio.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 80.dp), Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No encontramos ginecólogos", color = Color.Gray, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            } else {
                item {
                    Text(
                        "${directorio.size} especialistas encontrados",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
                items(directorio, key = { it.uid }) { gine ->
                    GinecologoCard(
                        ginecologo = gine,
                        onSolicitar = { viewModel.seleccionarGinecologo(gine) }
                    )
                }
            }
        }
    }

    ginecologoEncontrado?.let { gine ->
        ConfirmarVinculacionGineSheet(
            ginecologo = gine,
            mamaNombre = mamaNombre,
            estaCargando = cargandoAccion,
            onConfirmar = { viewModel.solicitarVinculacion(mamaNombre) },
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
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = EmbRosaOscuro)
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text("Directorio Médico", fontSize = 22.sp, fontWeight = FontWeight.Black, color = EmbRosaOscuro)
            Text("Ginecólogos y Obstetras certificados", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun BuscadorDirectorio(query: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    if (esAccesible) {
        CampoTextoAccesible(
            valor = query,
            onValorChange = onChange,
            etiqueta = "Buscar ginecólogo",
            descripcionVoz = "Di el nombre o la especialidad del ginecólogo para buscar",
            placeholder = "Nombre o especialidad...",
            ttsManager = ttsManager,
            colorPrimario = EmbRosa,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
    } else {
        OutlinedTextField(
            value = query,
            onValueChange = onChange,
            placeholder = { Text("Nombre o especialidad...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = EmbRosa) },
            trailingIcon = {
                AnimatedVisibility(query.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, null, tint = Color.Gray) }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = EmbRosa,
                unfocusedBorderColor = Color.LightGray.copy(0.4f),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
    }
}

@Composable
private fun GinecologoCard(ginecologo: GinecologoPublico, onSolicitar: () -> Unit) {
    val avatarColor = avatarPool[ginecologo.uid.hashCode().let { if (it < 0) -it else it } % avatarPool.size]

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).background(avatarColor.copy(0.15f), CircleShape).border(2.dp, avatarColor.copy(0.4f), CircleShape), contentAlignment = Alignment.Center) {
                Text(ginecologo.nombre.take(1).uppercase(), fontSize = 20.sp, fontWeight = FontWeight.Black, color = avatarColor)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(ginecologo.nombre, fontWeight = FontWeight.ExtraBold, color = EmbRosaOscuro, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(ginecologo.especialidad, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Verified, null, tint = EmbTeal, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Código: ${ginecologo.codigo}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = EmbTeal)
                }
            }
            IconButton(
                onClick = onSolicitar,
                modifier = Modifier.background(EmbRosa.copy(0.1f), RoundedCornerShape(14.dp))
            ) {
                Icon(Icons.Rounded.PersonAdd, "Solicitar", tint = EmbRosa)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmarVinculacionGineSheet(
    ginecologo: GinecologoPublico,
    mamaNombre: String,
    estaCargando: Boolean,
    onConfirmar: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(60.dp).background(EmbRosa.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MedicalServices, null, tint = EmbRosa, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Confirmar Vinculación", fontSize = 20.sp, fontWeight = FontWeight.Black, color = EmbRosaOscuro)
            Spacer(Modifier.height(12.dp))
            Text(
                "¿Deseas solicitar que ${ginecologo.nombre} lleve el seguimiento de tu embarazo?",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color.Gray
            )
            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onConfirmar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmbRosa),
                shape = RoundedCornerShape(18.dp),
                enabled = !estaCargando
            ) {
                if (estaCargando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else {
                    Icon(Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Enviar Solicitud", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(12.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Cancelar", color = Color.Gray)
            }
        }
    }
}
