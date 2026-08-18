package com.example.nutriia.pediatra

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
import com.example.nutriia.teleconsulta.TeleconsultaViewModel
import com.example.nutriia.teleconsulta.TipoLlamada
import com.example.nutriia.vinculacion.EstadoVinculacion
import com.example.nutriia.vinculacion.NutriologoPublico
import com.example.nutriia.vinculacion.Vinculacion
import com.example.nutriia.vinculacion.VinculacionViewModel
import com.example.nutriia.util.PermissionHelper
import com.example.nutriia.util.PermissionType
import com.example.nutriia.util.rememberPermissionState
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.nutriia.platform.Log
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.VoiceInputManager
import com.example.nutriia.accesibilidad.VoiceInputState
import kotlinx.coroutines.delay

// ─── Paleta ───────────────────────────────────────────────────────────────────
private val PGreen     = Color(0xFF689F38)
private val PDarkGreen = Color(0xFF33691E)
private val PBgCrema   = Color(0xFFF8F9F3)
private val PCardWhite = Color.White
private val PPurple    = Color(0xFF9C8FE0)
private val PTeal      = Color(0xFF4DB6AC)
private val POrange    = Color(0xFFFF8F00)
private val PRed       = Color(0xFFE53935)

private val avatarPool = listOf(
    Color(0xFFEC9BBF), Color(0xFF9C8FE0), Color(0xFFFFAB76),
    Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFF64B5F6)
)

private enum class ModoBusqueda { CODIGO, EMAIL, DIRECTORIO }

// ═════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL — Mi Nutriólogo / Pediatra
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun PediatraScreen(
    vinculacionViewModel:  VinculacionViewModel  = viewModel(),
    teleconsultaViewModel: TeleconsultaViewModel = viewModel(),
    a11yVm:                AccessibilityViewModel = viewModel(),
    padreUid:              String,
    padreNombre:           String,
    childId:               String,
    childNombre:           String,
    iniciarLlamadaAlEntrar: TipoLlamada?         = null,
    pagoNutriologoUid:      String               = "",
    pagoNutriologoNombre:   String               = "",
    pagoIdExitoso:          String               = "",
    padreNombreCompleto:    String               = padreNombre,
    onLlamadaIniciada:      () -> Unit           = {},
    onAbrirPago:           (nutriologoUid: String, nutriologoNombre: String, tipo: TipoLlamada) -> Unit,
    onAbrirDirectorio:     () -> Unit             = {},
    onBack:                () -> Unit = {}
) {
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val esBlind      = a11yMode == AccessibilityMode.BLIND
    val esMute       = a11yMode == AccessibilityMode.MUTE
    val esAccesible  = esBlind || esMute

    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    val vinculaciones        by vinculacionViewModel.vinculaciones.collectAsState()
    val nutriologoEncontrado by vinculacionViewModel.nutriologoEncontrado.collectAsState()
    val directorio           by vinculacionViewModel.directorio.collectAsState()
    val cargando             by vinculacionViewModel.cargando.collectAsState()
    val cargandoDir          by vinculacionViewModel.cargandoDirectorio.collectAsState()
    val error                by vinculacionViewModel.error.collectAsState()
    val exito                by vinculacionViewModel.exito.collectAsState()
    val nutriologoSel        by vinculacionViewModel.nutriologoSeleccionado.collectAsState()

    val vinculacionesHijo = vinculaciones.filter { it.childId == childId || it.childId.isBlank() }
    val activas           = vinculacionesHijo.filter { it.estado == EstadoVinculacion.ACTIVO }
    val pendientes        = vinculacionesHijo.filter { it.estado == EstadoVinculacion.PENDIENTE }

    var modoBusqueda by remember { mutableStateOf(ModoBusqueda.CODIGO) }
    var textoCodigo  by remember { mutableStateOf("") }
    var textoEmail   by remember { mutableStateOf("") }
    var queryDir     by remember { mutableStateOf("") }
    var mostrarEscannerQR by remember { mutableStateOf(false) }

    var permissionCheckStep by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        vinculacionViewModel.initComoPadre()
        vinculacionViewModel.cargarDirectorio()
    }

    LaunchedEffect(modoBusqueda) {
        if (modoBusqueda == ModoBusqueda.DIRECTORIO) {
            vinculacionViewModel.cargarDirectorio()
        }
    }

    LaunchedEffect(queryDir) {
        if (queryDir.isBlank()) {
            vinculacionViewModel.cargarDirectorio()
        } else {
            kotlinx.coroutines.delay(400)
            vinculacionViewModel.buscarEnDirectorio(queryDir)
        }
    }

    // ── Voice Commands Logic ──────────────────────────────────────────────────
    var isListening by remember { mutableStateOf(false) }
    val voiceManager = remember { VoiceInputManager() }
    val voiceState by voiceManager.estado

    DisposableEffect(Unit) {
        onDispose {
            voiceManager.liberar()
        }
    }

    LaunchedEffect(isListening) {
        if (isListening && esBlind) {
            val cmdGuia = loc(
                "Te escucho. Puedes decir: modo código, modo correo, modo directorio, buscar especialista, escanear Q R, o volver atrás. ¿Hacia qué opción se va a dirigir?",
                "I'm listening. You can say: code mode, email mode, directory mode, search specialist, scan Q R, or go back. Which option are you heading to?"
            )
            a11yVm.hablar(cmdGuia)
            delay(9500)
            voiceManager.escuchar(idiomaActual, true) { result, isFinal ->
                if (!isFinal) return@escuchar
                isListening = false
                val cmd = result.lowercase().trim()
                when {
                    cmd.contains("código") || cmd.contains("codigo") || cmd.contains("code") -> {
                        modoBusqueda = ModoBusqueda.CODIGO
                        a11yVm.hablar(loc("Cambiado a búsqueda por código.", "Changed to search by code."))
                    }
                    cmd.contains("correo") || cmd.contains("email") -> {
                        modoBusqueda = ModoBusqueda.EMAIL
                        a11yVm.hablar(loc("Cambiado a búsqueda por correo.", "Changed to search by email."))
                    }
                    cmd.contains("directorio") || cmd.contains("directory") -> {
                        modoBusqueda = ModoBusqueda.DIRECTORIO
                        a11yVm.hablar(loc("Cambiado a directorio de especialistas.", "Changed to specialists directory."))
                    }
                    cmd.contains("buscar") || cmd.contains("search") -> {
                        if (modoBusqueda == ModoBusqueda.CODIGO && textoCodigo.isNotBlank()) {
                            vinculacionViewModel.buscarPorCodigo(textoCodigo)
                            a11yVm.hablar(loc("Buscando especialista...", "Searching specialist..."))
                        } else if (modoBusqueda == ModoBusqueda.EMAIL && textoEmail.isNotBlank()) {
                            vinculacionViewModel.buscarPorEmail(textoEmail)
                            a11yVm.hablar(loc("Buscando especialista...", "Searching specialist..."))
                        } else {
                            a11yVm.hablar(loc("Primero ingresa un código o correo.", "First enter a code or email."))
                        }
                    }
                    cmd.contains("escanear") || cmd.contains("qr") || cmd.contains("scan") -> {
                        if (modoBusqueda == ModoBusqueda.CODIGO) {
                            mostrarEscannerQR = true
                            a11yVm.hablar(loc("Abriendo escáner QR.", "Opening QR scanner."))
                        } else {
                            a11yVm.hablar(loc("Cambia a modo código para escanear QR.", "Switch to code mode to scan QR."))
                        }
                    }
                    cmd.contains("volver") || cmd.contains("atrás") || cmd.contains("back") || cmd.contains("salir") -> {
                        onBack()
                    }
                    else -> a11yVm.hablar(loc("No entendí. Prueba con: modo código, modo directorio, buscar o escanear QR.", "I didn't understand. Try: code mode, directory mode, search or scan QR."))
                }
            }
        }
    }

    val cameraState = rememberPermissionState(
        type = PermissionType.CAMERA,
        onDismissed = { permissionCheckStep = 1 }
    ) {
        permissionCheckStep = 1
    }
    val micState = rememberPermissionState(
        type = PermissionType.MICROPHONE,
        onDismissed = { permissionCheckStep = 2 }
    ) {
        permissionCheckStep = 2
    }
    val phoneState = rememberPermissionState(
        type = PermissionType.PHONE,
        onDismissed = { permissionCheckStep = 3 }
    ) {
        permissionCheckStep = 3
    }
    val nearDevicesState = rememberPermissionState(
        type = PermissionType.NEAR_DEVICES,
        onDismissed = { permissionCheckStep = 4 }
    ) {
        permissionCheckStep = 4
    }

    LaunchedEffect(permissionCheckStep) {
        kotlinx.coroutines.delay(500)
        when (permissionCheckStep) {
            0 -> {
                val hasCam = PermissionHelper.hasPermissions(permissions = PermissionHelper.getRequiredPermissions(PermissionType.CAMERA))
                if (!hasCam) {
                    cameraState.requestPermission()
                } else {
                    permissionCheckStep = 1
                }
            }
            1 -> {
                val hasMic = PermissionHelper.hasPermissions(permissions = PermissionHelper.getRequiredPermissions(PermissionType.MICROPHONE))
                if (!hasMic) {
                    micState.requestPermission()
                } else {
                    permissionCheckStep = 2
                }
            }
            2 -> {
                val hasPhone = PermissionHelper.hasPermissions(permissions = PermissionHelper.getRequiredPermissions(PermissionType.PHONE))
                if (!hasPhone) {
                    phoneState.requestPermission()
                } else {
                    permissionCheckStep = 3
                }
            }
            3 -> {
                val hasNear = PermissionHelper.hasPermissions(permissions = PermissionHelper.getRequiredPermissions(PermissionType.NEAR_DEVICES))
                if (!hasNear) {
                    nearDevicesState.requestPermission()
                } else {
                    permissionCheckStep = 4
                }
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vinculacionViewModel.initComoPadre()
        vinculacionViewModel.cargarDirectorio()
        if (esBlind) {
            a11yVm.hablar(loc(
                "Módulo de especialista para $childNombre. Aquí puedes buscar y vincularte con tu pediatra o nutriólogo. Puedes usar comandos de voz activando el botón del micrófono.",
                "Specialist module for $childNombre. Here you can search and link with your pediatrician or nutritionist. You can use voice commands by activating the microphone button."
            ))
        }
    }

    LaunchedEffect(iniciarLlamadaAlEntrar) {
        if (iniciarLlamadaAlEntrar != null && pagoNutriologoUid.isNotBlank() && pagoIdExitoso.isNotBlank()) {
            teleconsultaViewModel.iniciarLlamadaComoPadre(
                padreUid         = padreUid,
                padreNombre      = padreNombreCompleto,
                nutriologoUid    = pagoNutriologoUid,
                nutriologoNombre = pagoNutriologoNombre,
                childId          = childId,
                childNombre      = childNombre,
                pagoId           = pagoIdExitoso,
                tipo             = iniciarLlamadaAlEntrar
            )
            onLlamadaIniciada()
        }
    }

    LaunchedEffect(queryDir) {
        kotlinx.coroutines.delay(350)
        if (modoBusqueda == ModoBusqueda.DIRECTORIO) {
            vinculacionViewModel.buscarEnDirectorio(queryDir)
        }
    }

    LaunchedEffect(error) {
        error?.let { snackbarHostState.showSnackbar(it); vinculacionViewModel.limpiarError() }
    }

    LaunchedEffect(exito) {
        exito?.let { snackbarHostState.showSnackbar(it); vinculacionViewModel.limpiarExito() }
    }

    LaunchedEffect(modoBusqueda) {
        vinculacionViewModel.limpiarBusqueda()
        textoCodigo = ""
        textoEmail  = ""
        queryDir    = ""
        if (modoBusqueda == ModoBusqueda.DIRECTORIO) {
            vinculacionViewModel.cargarDirectorio()
        }
    }

    Scaffold(
        containerColor = PBgCrema,
        snackbarHost   = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (esBlind) {
                FloatingActionButton(
                    onClick = { isListening = !isListening },
                    containerColor = if (voiceState == VoiceInputState.LISTENING) Color.Red else PGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 16.dp).size(64.dp)
                        .semantics { contentDescription = if (voiceState == VoiceInputState.LISTENING) "Detener comandos de voz" else "Activar comandos de voz para navegación. Al presionar, escucha la lista de comandos disponibles." }
                ) {
                    Icon(if (voiceState == VoiceInputState.LISTENING) Icons.Rounded.Stop else Icons.Rounded.Mic, contentDescription = null, modifier = Modifier.size(30.dp))
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier       = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            item { PediatraHeader(childNombre = childNombre, onBack = onBack) }

            // ── Especialistas activos: mostramos info + llamar directamente ────
            if (activas.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(20.dp))
                    SeccionTitulo(icono = Icons.Rounded.VerifiedUser, titulo = "Mis especialistas", color = PGreen)
                }
                items(activas, key = { it.id }) { vinc ->
                    VinculacionActivaCard(
                        vinculacion           = vinc,
                        padreUid              = padreUid,
                        padreNombre           = padreNombre,
                        childId               = childId,
                        childNombre           = childNombre,
                        teleconsultaViewModel = teleconsultaViewModel,
                        onAbrirPago           = { nutriologoUid, nutriologoNombre, tipo ->
                            onAbrirPago(nutriologoUid, nutriologoNombre, tipo)
                        },
                        onRevocar             = { vinculacionViewModel.revocarVinculacion(vinc.id) }
                    )
                }
            }

            if (pendientes.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    SeccionTitulo(icono = Icons.Rounded.HourglassBottom, titulo = "Solicitudes enviadas", color = POrange)
                }
                items(pendientes, key = { it.id }) { vinc ->
                    VinculacionPendienteCard(vinculacion = vinc)
                }
            }
            
            if (mostrarEscannerQR) {
                item {
                    QrScannerDialog(
                        onDismiss = { mostrarEscannerQR = false },
                        onCodeScanned = { code ->
                            textoCodigo = code
                            mostrarEscannerQR = false
                            vinculacionViewModel.buscarPorCodigo(code)
                        }
                    )
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                SeccionTitulo(icono = Icons.Rounded.PersonSearch, titulo = "Agregar especialista", color = PDarkGreen)
                Spacer(Modifier.height(12.dp))
            }

            item {
                SelectorModoBusqueda(
                    modoActual   = modoBusqueda,
                    onSeleccionar = { nuevo ->
                        if (nuevo == ModoBusqueda.DIRECTORIO) {
                            // En iOS navegamos a pantalla completa separada
                            onAbrirDirectorio()
                        } else {
                            modoBusqueda = nuevo
                        }
                    }
                )
                Spacer(Modifier.height(16.dp))
            }

            when (modoBusqueda) {

                ModoBusqueda.CODIGO -> {
                    item {
                        BusquedaPorCodigo(
                            texto    = textoCodigo,
                            onTexto  = { textoCodigo = it },
                            cargando = cargando,
                            onBuscar = { vinculacionViewModel.buscarPorCodigo(textoCodigo) },
                            onScanQRClick = { mostrarEscannerQR = true }
                        )
                    }
                    nutriologoEncontrado?.let { nutri ->
                        item {
                            Spacer(Modifier.height(12.dp))
                            NutriologoEncontradoCard(
                                nutriologo  = nutri,
                                childNombre = childNombre,
                                onSolicitar = {
                                    vinculacionViewModel.solicitarVinculacion(
                                        padreNombre = padreNombre,
                                        childId     = childId,
                                        childNombre = childNombre
                                    )
                                },
                                onLimpiar = { vinculacionViewModel.limpiarBusqueda() }
                            )
                        }
                    }
                }

                ModoBusqueda.EMAIL -> {
                    item {
                        BusquedaPorEmail(
                            texto    = textoEmail,
                            onTexto  = { textoEmail = it },
                            cargando = cargando,
                            onBuscar = { vinculacionViewModel.buscarPorEmail(textoEmail) }
                        )
                    }
                    nutriologoEncontrado?.let { nutri ->
                        item {
                            Spacer(Modifier.height(12.dp))
                            NutriologoEncontradoCard(
                                nutriologo  = nutri,
                                childNombre = childNombre,
                                onSolicitar = {
                                    vinculacionViewModel.solicitarVinculacion(
                                        padreNombre = padreNombre,
                                        childId     = childId,
                                        childNombre = childNombre
                                    )
                                },
                                onLimpiar = { vinculacionViewModel.limpiarBusqueda() }
                            )
                        }
                    }
                }

                ModoBusqueda.DIRECTORIO -> {
                    item {
                        BuscadorDirectorioInline(
                            query    = queryDir,
                            onChange = { queryDir = it },
                            onClear  = { queryDir = ""; vinculacionViewModel.cargarDirectorio() }
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (cargandoDir) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) {
                                CircularProgressIndicator(color = PGreen)
                            }
                        }
                    } else if (directorio.isEmpty()) {
                        item {
                            DirectorioVacio(
                                query = queryDir,
                                onRecargar = { vinculacionViewModel.cargarDirectorio() }
                            )
                        }
                    } else {
                        item {
                            Text(
                                "${directorio.size} especialista(s)",
                                fontSize = 11.sp, color = Color.Gray,
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        items(directorio, key = { it.uid }) { nutri ->
                            NutriologoDirectorioRow(
                                nutriologo  = nutri,
                                onSolicitar = { vinculacionViewModel.seleccionarNutriologoDelDirectorio(nutri) }
                            )
                        }
                    }
                }
            }
        }
    }

    // BottomSheet solo para DIRECTORIO — no toca vinculación
    if (modoBusqueda == ModoBusqueda.DIRECTORIO) {
        nutriologoSel?.let { nutri ->
            ConfirmarVinculacionBottomSheet(
                nutriologo  = nutri,
                childNombre = childNombre,
                onConfirmar = {
                    vinculacionViewModel.solicitarVinculacion(
                        padreNombre = padreNombre,
                        childId     = childId,
                        childNombre = childNombre
                    )
                },
                onDismiss = { vinculacionViewModel.limpiarSeleccion() }
            )
        }
    }
}

// ─── Header ───────────────────────────────────────────────────────────────────

@Composable
private fun PediatraHeader(childNombre: String, onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 48.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = PDarkGreen)
        }
        Spacer(Modifier.width(4.dp))
        Column {
            Text("Mi Nutriólogo / Pediatra", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = PDarkGreen)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ChildCare, null, tint = PTeal, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text("Para: $childNombre", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

// ─── Sección título ───────────────────────────────────────────────────────────

@Composable
private fun SeccionTitulo(
    icono:  androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    color:  Color
) {
    Row(
        modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icono, null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(titulo, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = PDarkGreen)
    }
}

// ─── Selector de modo ─────────────────────────────────────────────────────────

@Composable
private fun SelectorModoBusqueda(modoActual: ModoBusqueda, onSeleccionar: (ModoBusqueda) -> Unit) {
    val modos = listOf(
        Triple(ModoBusqueda.CODIGO,     Icons.Rounded.QrCode2,           "Código"),
        Triple(ModoBusqueda.EMAIL,      Icons.Rounded.Email,              "Correo"),
        Triple(ModoBusqueda.DIRECTORIO, Icons.Rounded.FormatListBulleted, "Directorio")
    )

    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        modos.forEach { (modo, icon, label) ->
            val selected = modoActual == modo
            Surface(
                onClick         = { onSeleccionar(modo) },
                modifier        = Modifier.weight(1f),
                shape           = RoundedCornerShape(16.dp),
                color           = if (selected) PGreen else PCardWhite,
                shadowElevation = if (selected) 4.dp else 1.dp
            ) {
                Column(
                    modifier            = Modifier.padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(icon, null, tint = if (selected) Color.White else Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(label, fontSize = 11.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color.White else Color.Gray)
                }
            }
        }
    }
}

// ─── Búsqueda por Código ──────────────────────────────────────────────────────

@Composable
private fun BusquedaPorCodigo(
    texto: String,
    onTexto: (String) -> Unit,
    cargando: Boolean,
    onBuscar: () -> Unit,
    onScanQRClick: () -> Unit
) {
    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Ingresa el código del especialista", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        if (esAccesible) {
            CampoTextoAccesible(
                valor = texto,
                onValorChange = { onTexto(it.uppercase()) },
                etiqueta = "Código del especialista",
                descripcionVoz = "Di el código del especialista o toca dos veces el botón de la izquierda para escanear código QR",
                placeholder = "NUTRI-XXXX-XXXXXX",
                ttsManager = ttsManager,
                colorPrimario = PGreen
            )
        } else {
            OutlinedTextField(
                value         = texto,
                onValueChange = { onTexto(it.uppercase()) },
                placeholder   = { Text("NUTRI-XXXX-XXXXXX", fontSize = 14.sp) },
                leadingIcon   = {
                    IconButton(onClick = onScanQRClick) {
                        Icon(Icons.Rounded.QrCode2, "Escanear QR", tint = PGreen)
                    }
                },
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PGreen, unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedContainerColor = PCardWhite, unfocusedContainerColor = PCardWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick  = onBuscar,
            enabled  = texto.isNotBlank() && !cargando,
            modifier = Modifier.fillMaxWidth().height(if (esAccesible) 70.dp else 50.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PGreen),
            shape    = RoundedCornerShape(14.dp)
        ) {
            if (cargando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            else { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Buscar", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun QrScannerDialog(
    onDismiss: () -> Unit,
    onCodeScanned: (String) -> Unit
) {
    var manualCode by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Escanear o ingresar código QR", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E1E1E)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.QrCodeScanner,
                            contentDescription = "Código QR",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Ingresa o pega el código QR del especialista a vincular",
                            textAlign = TextAlign.Center,
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = manualCode,
                    onValueChange = { manualCode = it },
                    label = { Text("Código o enlace") },
                    placeholder = { Text("Ej. NUT-12345 o nutriia://vincular/...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawValue = manualCode.trim()
                    if (rawValue.isNotEmpty()) {
                        val cleanCode = if (rawValue.startsWith("nutriia://vincular/")) {
                            rawValue.substringAfter("nutriia://vincular/")
                        } else {
                            rawValue
                        }
                        onCodeScanned(cleanCode)
                    }
                },
                enabled = manualCode.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PGreen)
            ) {
                Text("Vincular", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

// ─── Búsqueda por Email ───────────────────────────────────────────────────────

@Composable
private fun BusquedaPorEmail(texto: String, onTexto: (String) -> Unit, cargando: Boolean, onBuscar: () -> Unit) {
    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Ingresa el correo del especialista", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        if (esAccesible) {
            CampoTextoAccesible(
                valor = texto,
                onValorChange = onTexto,
                etiqueta = "Correo del especialista",
                descripcionVoz = "Di el correo del especialista",
                placeholder = "ejemplo@correo.com",
                ttsManager = ttsManager,
                colorPrimario = PGreen
            )
        } else {
            OutlinedTextField(
                value         = texto,
                onValueChange = onTexto,
                placeholder   = { Text("ejemplo@correo.com", fontSize = 14.sp) },
                leadingIcon   = { Icon(Icons.Rounded.Email, null, tint = PGreen) },
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PGreen, unfocusedBorderColor = Color(0xFFDDDDDD),
                    focusedContainerColor = PCardWhite, unfocusedContainerColor = PCardWhite
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.height(10.dp))
        Button(
            onClick  = onBuscar,
            enabled  = texto.isNotBlank() && !cargando,
            modifier = Modifier.fillMaxWidth().height(if (esAccesible) 70.dp else 50.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = PGreen),
            shape    = RoundedCornerShape(14.dp)
        ) {
            if (cargando) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
            else { Icon(Icons.Rounded.Search, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Buscar", fontWeight = FontWeight.Bold) }
        }
    }
}

// ─── Buscador inline Directorio ───────────────────────────────────────────────

@Composable
private fun BuscadorDirectorioInline(query: String, onChange: (String) -> Unit, onClear: () -> Unit) {
    val a11yMode = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val ttsManager = a11yVm.ttsManager
    val esAccesible = a11yMode == AccessibilityMode.BLIND || a11yMode == AccessibilityMode.MUTE

    if (esAccesible) {
        CampoTextoAccesible(
            valor = query,
            onValorChange = onChange,
            etiqueta = "Buscar en directorio",
            descripcionVoz = "Di el nombre o la especialidad para buscar",
            placeholder = "Nombre o especialidad...",
            ttsManager = ttsManager,
            colorPrimario = PGreen,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
    } else {
        OutlinedTextField(
            value         = query,
            onValueChange = onChange,
            placeholder   = { Text("Nombre o especialidad…", fontSize = 13.sp) },
            leadingIcon   = { Icon(Icons.Rounded.Search, null, tint = PGreen) },
            trailingIcon  = {
                AnimatedVisibility(query.isNotBlank(), enter = fadeIn(), exit = fadeOut()) {
                    IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, "Limpiar", tint = Color.Gray) }
                }
            },
            singleLine = true,
            shape      = RoundedCornerShape(16.dp),
            colors     = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PGreen, unfocusedBorderColor = Color(0xFFDDDDDD),
                focusedContainerColor = PCardWhite, unfocusedContainerColor = PCardWhite
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
        )
    }
}

// ─── Nutriólogo encontrado por código/email ───────────────────────────────────

@Composable
private fun NutriologoEncontradoCard(
    nutriologo:  NutriologoPublico,
    childNombre: String,
    onSolicitar: () -> Unit,
    onLimpiar:   () -> Unit
) {
    val color = avatarPool[nutriologo.uid.hashCode().let { if (it < 0) -it else it } % avatarPool.size]

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = PGreen.copy(alpha = 0.06f)),
        border    = BorderStroke(1.dp, PGreen.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(50.dp).clip(CircleShape)
                        .background(color.copy(alpha = 0.18f)).border(2.dp, color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(nutriologo.nombre.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 20.sp, color = color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(nutriologo.nombre, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = PDarkGreen)
                    Text(nutriologo.especialidad.ifBlank { "Especialista" }, fontSize = 12.sp, color = Color.Gray)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                            .background(PGreen.copy(alpha = 0.10f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.QrCode2, null, tint = PGreen, modifier = Modifier.size(11.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(nutriologo.codigo, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PDarkGreen)
                    }
                }
                IconButton(onClick = onLimpiar) {
                    Icon(Icons.Rounded.Close, "Limpiar", tint = Color.LightGray)
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(12.dp))

            Text(
                "Se vinculará con el expediente de $childNombre",
                fontSize = 12.sp, color = Color.Gray,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick  = onSolicitar,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PGreen),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enviar solicitud de vinculación", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Row directorio ───────────────────────────────────────────────────────────

@Composable
private fun NutriologoDirectorioRow(nutriologo: NutriologoPublico, onSolicitar: () -> Unit) {
    val color = avatarPool[nutriologo.uid.hashCode().let { if (it < 0) -it else it } % avatarPool.size]

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = PCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.16f)).border(2.dp, color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(nutriologo.nombre.take(1).uppercase(), fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(nutriologo.nombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PDarkGreen, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(nutriologo.especialidad.ifBlank { "Especialista" }, fontSize = 11.sp, color = Color.Gray, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(6.dp))
                        .background(PGreen.copy(alpha = 0.08f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.QrCode2, null, tint = PGreen, modifier = Modifier.size(10.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(nutriologo.codigo, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = PDarkGreen)
                }
            }
            Spacer(Modifier.width(8.dp))
            FilledTonalButton(
                onClick        = onSolicitar,
                colors         = ButtonDefaults.filledTonalButtonColors(containerColor = PGreen.copy(alpha = 0.12f)),
                shape          = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier       = Modifier.height(36.dp)
            ) {
                Icon(Icons.Rounded.PersonAdd, null, tint = PGreen, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text("Solicitar", fontSize = 11.sp, color = PGreen, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DirectorioVacio(query: String, onRecargar: () -> Unit = {}) {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Rounded.SearchOff, null, tint = PGreen.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(12.dp))
            Text(
                if (query.isBlank()) "No hay especialistas registrados aún" else "Sin resultados para \"$query\"",
                fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 40.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onRecargar,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PGreen)
            ) {
                Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Actualizar directorio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Vinculación ACTIVA — con botones de teleconsulta ─────────────────────────
// NOTA: NO se toca la lógica de vinculación. Solo se añaden los botones de llamada.

@Composable
private fun VinculacionActivaCard(
    vinculacion:           Vinculacion,
    padreUid:              String,
    padreNombre:           String,
    childId:               String,
    childNombre:           String,
    teleconsultaViewModel: TeleconsultaViewModel,
    onAbrirPago:           (nutriologoUid: String, nutriologoNombre: String, tipo: TipoLlamada) -> Unit,
    onRevocar:             () -> Unit
) {
    var showConfirmacion by remember { mutableStateOf(false) }

    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(containerColor = PGreen.copy(alpha = 0.06f)),
        border    = BorderStroke(1.dp, PGreen.copy(alpha = 0.25f))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {

            // ── Info del especialista ──────────────────────────────────────────
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(PGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = PGreen, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(vinculacion.nutriologoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PDarkGreen)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(7.dp).clip(CircleShape).background(PGreen))
                        Spacer(Modifier.width(5.dp))
                        Text("Vinculado activamente", fontSize = 11.sp, color = PGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
                IconButton(onClick = { showConfirmacion = true }) {
                    Icon(Icons.Rounded.LinkOff, "Revocar", tint = PRed.copy(alpha = 0.6f))
                }
            }

            // ── Botones de teleconsulta ────────────────────────────────────────
            // El padre puede iniciar llamada al nutriólogo vinculado.
            Spacer(Modifier.height(4.dp))
            HorizontalDivider(
                color     = PGreen.copy(alpha = 0.12f),
                thickness = 0.5.dp,
                modifier  = Modifier.padding(vertical = 8.dp)
            )

            // Los botones desde el lado del padre: llaman al nutriólogo.
            com.example.nutriia.payment.PaymentAwareTeleconsultaButtons(
                nutriologoUid         = vinculacion.nutriologoUid,
                nutriologoNombre      = vinculacion.nutriologoNombre,
                padreUid              = padreUid,
                padreNombre           = padreNombre,
                childId               = childId,
                childNombre           = childNombre,
                teleconsultaViewModel = teleconsultaViewModel,
                onAbrirPago           = onAbrirPago
            )
        }
    }

    if (showConfirmacion) {
        AlertDialog(
            onDismissRequest = { showConfirmacion = false },
            shape            = RoundedCornerShape(24.dp),
            containerColor   = PBgCrema,
            title            = { Text("¿Revocar vinculación?", fontWeight = FontWeight.ExtraBold, color = PDarkGreen) },
            text             = {
                Text(
                    "Se eliminará el acceso de ${vinculacion.nutriologoNombre} al expediente.",
                    fontSize = 13.sp, color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { onRevocar(); showConfirmacion = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = PRed),
                    shape   = RoundedCornerShape(12.dp)
                ) { Text("Revocar", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmacion = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}

// ─── Vinculación pendiente ────────────────────────────────────────────────────

@Composable
private fun VinculacionPendienteCard(vinculacion: Vinculacion) {
    Card(
        modifier  = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp),
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(containerColor = POrange.copy(alpha = 0.06f)),
        border    = BorderStroke(1.dp, POrange.copy(alpha = 0.25f))
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(POrange.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.HourglassBottom, null, tint = POrange, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(vinculacion.nutriologoNombre, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PDarkGreen)
                Text("Esperando respuesta…", fontSize = 11.sp, color = POrange)
            }
            Surface(shape = RoundedCornerShape(8.dp), color = POrange.copy(alpha = 0.12f)) {
                Text("PENDIENTE", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, color = POrange)
            }
        }
    }
}

// ─── BottomSheet confirmación (solo DIRECTORIO) ───────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmarVinculacionBottomSheet(
    nutriologo:  NutriologoPublico,
    childNombre: String,
    onConfirmar: () -> Unit,
    onDismiss:   () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor   = PBgCrema,
        shape            = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        tonalElevation   = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp, top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(40.dp, 4.dp).background(Color.LightGray.copy(0.4f), CircleShape))
            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.size(64.dp).background(PGreen.copy(0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.ContactMail, null, tint = PGreen, modifier = Modifier.size(32.dp))
            }

            Spacer(Modifier.height(16.dp))
            Text("Confirmar Vinculación", fontSize = 22.sp, fontWeight = FontWeight.Black, color = PDarkGreen)
            Spacer(Modifier.height(8.dp))
            Text(
                "Estás por enviar una solicitud de acceso al expediente de tu pequeño.",
                textAlign = TextAlign.Center, fontSize = 14.sp, color = Color.Gray
            )

            Spacer(Modifier.height(28.dp))

            Surface(
                color = PCardWhite, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFEEEEEE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    SheetInfoRow(Icons.Rounded.Person, "Especialista", nutriologo.nombre)
                    Spacer(Modifier.height(12.dp))
                    SheetInfoRow(Icons.Rounded.ChildCare, "Paciente", childNombre)
                    Spacer(Modifier.height(12.dp))
                    SheetInfoRow(Icons.Rounded.Badge, "Código", nutriologo.codigo)
                }
            }

            Spacer(Modifier.height(32.dp))

            Button(
                onClick  = onConfirmar,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors   = ButtonDefaults.buttonColors(containerColor = PGreen),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Rounded.Send, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Enviar solicitud", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick  = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border   = BorderStroke(1.dp, Color.LightGray),
                shape    = RoundedCornerShape(16.dp)
            ) {
                Text("Cancelar", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun SheetInfoRow(
    icon:  androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(PGreen.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = PGreen, modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 14.sp, color = PDarkGreen, fontWeight = FontWeight.Bold)
        }
    }
}