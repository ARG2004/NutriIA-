package com.example.nutriia.configuracion

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.*
import com.example.nutriia.ui.theme.ChildProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.nutriia.util.PermissionHelper
import com.example.nutriia.util.PermissionType
import com.example.nutriia.util.rememberPermissionState
import androidx.lifecycle.compose.LocalLifecycleOwner

// ═══════════════════════════════════════════════════════════════════════════════
// PALETA
// ═══════════════════════════════════════════════════════════════════════════════

private val CfgBg        = Color(0xFFF9F8F4)
private val CfgGreen     = Color(0xFF4CAF50)
private val CfgDarkGreen = Color(0xFF1B5E20)
private val CfgCardWhite = Color.White
private val CfgDivider   = Color(0xFFF0F0F0)
private val CfgBorder    = Color(0xFFE8E8E0)
private val CfgRed       = Color(0xFFE53935)

// ═══════════════════════════════════════════════════════════════════════════════
// MODELO SECCIÓN
// ═══════════════════════════════════════════════════════════════════════════════

private enum class ConfigSection(val label: String) {
    CUENTA("Cuenta"),
    HIJOS("Hijos registrados"),
    ACCESIBILIDAD("Accesibilidad"),
    NOTIFICACIONES("Notificaciones"),
    PERMISOS("Permisos del sistema"),
    DATOS("Privacidad"),
    SESION("Sesión")
}

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA PRINCIPAL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun ConfiguracionScreen(
    children:           List<ChildProfile>,
    nombrePadre:        String,
    emailPadre:         String,
    rol:                String                  = "padre",
    onBack:             () -> Unit,
    onEditarPerfil:     () -> Unit              = {},
    onCambiarPasswordDirecto: (contrasenaActual: String, nuevaContrasena: String, onResultado: (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onEnviarCorreoPassword: () -> Unit          = {},
    onEditarHijo:       (ChildProfile) -> Unit  = {},
    onAgregarHijo:      () -> Unit              = {},
    onPrivacidad:       () -> Unit              = {},
    onCerrarSesion:     () -> Unit,
    onEliminarCuentaConPassword: (contrasenaActual: String, onResultado: (Boolean, String) -> Unit) -> Unit = { _, _ -> }
) {
        val a11yVm: AccessibilityViewModel = viewModel()
    val modoActual   by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager

    var notifComidas          by remember { mutableStateOf(true) }
    var notifCrecimiento      by remember { mutableStateOf(true) }
    var notifRecomendaciones  by remember { mutableStateOf(false) }

    var mostrarDialogoPassword  by remember { mutableStateOf(false) }
    var mostrarDialogoCerrar    by remember { mutableStateOf(false) }
    var mostrarDialogoEliminar  by remember { mutableStateOf(false) }
    var mostrarDialogoA11y      by remember { mutableStateOf(false) }
    var mostrarPrivacidad       by remember { mutableStateOf(false) }

    var mostrarDialogoArco      by remember { mutableStateOf(false) }
    var borrandoDatosArco       by remember { mutableStateOf(false) }
    var mensajeArco             by remember { mutableStateOf<String?>(null) }
    val coroutineScopeArco = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        if (modoActual == AccessibilityMode.BLIND) {
            a11yVm.hablar(
                "Pantalla de ajustes. Aquí puedes editar tu cuenta, perfil de tus hijos, " +
                        "accesibilidad, notificaciones, privacidad y cerrar sesión."
            )
        }
    }

    // ── Política de privacidad (pantalla sobre) ───────────────────────────────
    AnimatedVisibility(
        visible = mostrarPrivacidad,
        enter   = slideInHorizontally(tween(320, easing = EaseOutCubic)) { it },
        exit    = slideOutHorizontally(tween(260, easing = EaseInCubic)) { it }
    ) {
        PrivacidadScreen(onBack = { mostrarPrivacidad = false })
    }

    // ── Contenido principal (se oculta cuando privacidad está visible) ────────
    AnimatedVisibility(
        visible = !mostrarPrivacidad,
        enter   = fadeIn(tween(200)),
        exit    = fadeOut(tween(150))
    ) {

        // ── Diálogo cerrar sesión ─────────────────────────────────────────────
        if (mostrarDialogoCerrar) {
            CfgAlertDialog(
                icon          = Icons.AutoMirrored.Rounded.ExitToApp,
                iconTint      = CfgRed,
                title         = "¿Cerrar sesión?",
                body          = "Se eliminará la sesión local. Necesitarás conexión para volver a iniciar sesión.",
                confirmLabel  = "Cerrar sesión",
                onConfirm     = { mostrarDialogoCerrar = false; onCerrarSesion() },
                onDismiss     = { mostrarDialogoCerrar = false }
            )
        }

        // ── Diálogo cambiar contraseña ─────────────────────────────────────────
        if (mostrarDialogoPassword) {
            CfgCambiarPasswordDialog(
                emailUsuario     = emailPadre,
                onCambiarDirecto = onCambiarPasswordDirecto,
                onEnviarCorreo   = onEnviarCorreoPassword,
                onDismiss        = { mostrarDialogoPassword = false }
            )
        }

        // ── Diálogo eliminar cuenta ───────────────────────────────────────────
        if (mostrarDialogoEliminar) {
            CfgEliminarCuentaDialog(
                onEliminar = onEliminarCuentaConPassword,
                onDismiss  = { mostrarDialogoEliminar = false }
            )
        }

        // ── Diálogo accesibilidad ─────────────────────────────────────────────
        if (mostrarDialogoA11y) {
            CfgAccesibilidadDialog(
                modoActual     = modoActual,
                idiomaActual   = idiomaActual,
                ttsManager     = ttsManager,
                                onModoChange   = { a11yVm.setMode(it) },
                onIdiomaChange = { a11yVm.setIdioma(it) },
                onDismiss      = { mostrarDialogoA11y = false }
            )
        }

        Box(modifier = Modifier.fillMaxSize().background(CfgBg)) {
            LazyColumn(
                modifier       = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 56.dp)
            ) {

                // Top bar
                item {
                    CfgTopBar(onBack = onBack)
                }

                // ── Cuenta ────────────────────────────────────────────────────
                item {
                    AnimatedSection(delayMs = 0) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            CfgSectionLabel(ConfigSection.CUENTA.label)
                            Spacer(Modifier.height(8.dp))
                            CfgCard {
                                CfgProfileRow(
                                    nombre  = nombrePadre,
                                    email   = emailPadre,
                                    rol     = rol,
                                    onClick = onEditarPerfil
                                )
                                CfgDividerLine()
                                CfgRow(
                                    icon     = Icons.Rounded.Person,
                                    iconBg   = Color(0xFFE3F2FD),
                                    iconTint = Color(0xFF1565C0),
                                    title    = "Editar perfil",
                                    subtitle = "Nombre, correo, teléfono",
                                    onClick  = onEditarPerfil
                                )
                                CfgDividerLine()
                                CfgRow(
                                    icon     = Icons.Rounded.Lock,
                                    iconBg   = Color(0xFFFCE4EC),
                                    iconTint = Color(0xFFC2185B),
                                    title    = "Cambiar contraseña",
                                    subtitle = "Actualiza tu seguridad",
                                    onClick  = { mostrarDialogoPassword = true },
                                    isLast   = true
                                )
                            }
                        }
                    }
                }

                // ── Hijos ─────────────────────────────────────────────────────
                item {
                    AnimatedSection(delayMs = 60) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(20.dp))
                            CfgSectionLabel(ConfigSection.HIJOS.label)
                            Spacer(Modifier.height(8.dp))
                            CfgCard {
                                children.forEachIndexed { idx, child ->
                                    CfgHijoRow(child = child, onClick = { onEditarHijo(child) })
                                    if (idx < children.lastIndex) CfgDividerLine()
                                }
                                if (children.isNotEmpty()) CfgDividerLine()
                                CfgRow(
                                    icon     = Icons.Rounded.PersonAdd,
                                    iconBg   = Color(0xFFE8F5E9),
                                    iconTint = CfgGreen,
                                    title    = "Agregar hijo/a",
                                    subtitle = "Nuevo perfil de seguimiento",
                                    onClick  = onAgregarHijo,
                                    isLast   = true
                                )
                            }
                        }
                    }
                }

                // ── Accesibilidad ─────────────────────────────────────────────
                item {
                    AnimatedSection(delayMs = 120) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(20.dp))
                            CfgSectionLabel(ConfigSection.ACCESIBILIDAD.label)
                            Spacer(Modifier.height(8.dp))
                            CfgCard {
                                CfgRow(
                                    icon     = Icons.Rounded.Accessibility,
                                    iconBg   = Color(0xFFEDE7F6),
                                    iconTint = Color(0xFF5E35B1),
                                    title    = "Modo de accesibilidad",
                                    subtitleComposable = { CfgA11yChip(modoActual) },
                                    onClick  = { mostrarDialogoA11y = true }
                                )
                                CfgDividerLine()
                                CfgRow(
                                    icon     = Icons.Rounded.Language,
                                    iconBg   = Color(0xFFE0F2F1),
                                    iconTint = Color(0xFF00695C),
                                    title    = "Idioma de la voz",
                                    subtitle = idiomaActual.label,
                                    onClick  = { mostrarDialogoA11y = true }
                                )
                                CfgDividerLine()
                                CfgToggleRow(
                                    icon     = if (modoActual == AccessibilityMode.MUTE)
                                        Icons.AutoMirrored.Rounded.VolumeOff
                                    else Icons.AutoMirrored.Rounded.VolumeUp,
                                    iconBg   = Color(0xFFE0F2F1),
                                    iconTint = Color(0xFF00695C),
                                    title    = "Voz NutriIA",
                                    subtitle = if (modoActual == AccessibilityMode.MUTE) "Silenciada" else "Narración activa",
                                    checked  = modoActual != AccessibilityMode.MUTE,
                                    onCheckedChange = { habilitada ->
                                        a11yVm.setMode(if (habilitada) AccessibilityMode.NORMAL else AccessibilityMode.MUTE)
                                    },
                                    isLast = true
                                )
                            }
                        }
                    }
                }

                // ── Notificaciones ────────────────────────────────────────────
                item {
                    AnimatedSection(delayMs = 180) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(20.dp))
                            CfgSectionLabel(ConfigSection.NOTIFICACIONES.label)
                            Spacer(Modifier.height(8.dp))
                            CfgCard {
                                CfgToggleRow(
                                    icon     = Icons.Rounded.Notifications,
                                    iconBg   = Color(0xFFFFF8E1),
                                    iconTint = Color(0xFFF57F17),
                                    title    = "Recordatorios de comidas",
                                    subtitle = "Horarios personalizados",
                                    checked  = notifComidas,
                                    onCheckedChange = { notifComidas = it }
                                )
                                CfgDividerLine()
                                CfgToggleRow(
                                    icon     = Icons.Rounded.MonitorWeight,
                                    iconBg   = Color(0xFFFFF8E1),
                                    iconTint = Color(0xFFF57F17),
                                    title    = "Alertas de crecimiento",
                                    subtitle = "Curva OMS y mediciones",
                                    checked  = notifCrecimiento,
                                    onCheckedChange = { notifCrecimiento = it }
                                )
                                CfgDividerLine()
                                CfgToggleRow(
                                    icon     = Icons.Rounded.AutoAwesome,
                                    iconBg   = Color(0xFFFFF8E1),
                                    iconTint = Color(0xFFF57F17),
                                    title    = "Recomendaciones IA",
                                    subtitle = "Tips semanales por etapa",
                                    checked  = notifRecomendaciones,
                                    onCheckedChange = { notifRecomendaciones = it },
                                    isLast   = true
                                )
                            }
                        }
                    }
                }

                // ── Permisos del sistema ──────────────────────────────────────
                item {
                    AnimatedSection(delayMs = 210) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(20.dp))
                            CfgSectionLabel(ConfigSection.PERMISOS.label)
                            Spacer(Modifier.height(8.dp))
                            CfgCard {
                                PermissionSettingRow(PermissionType.CAMERA)
                                CfgDividerLine()
                                PermissionSettingRow(PermissionType.MICROPHONE)
                                CfgDividerLine()
                                PermissionSettingRow(PermissionType.PHONE)
                                CfgDividerLine()
                                PermissionSettingRow(PermissionType.NEAR_DEVICES, isLast = true)
                            }
                        }
                    }
                }

                // ── Privacidad ────────────────────────────────────────────────
                item {
                    AnimatedSection(delayMs = 240) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(20.dp))
                            CfgSectionLabel(ConfigSection.DATOS.label)
                            Spacer(Modifier.height(8.dp))
                            CfgCard {
                                CfgRow(
                                    icon     = Icons.Rounded.Security,
                                    iconBg   = Color(0xFFE3F2FD),
                                    iconTint = Color(0xFF1565C0),
                                    title    = "Política de privacidad",
                                    subtitle = "Cómo protegemos tus datos",
                                    onClick  = { mostrarPrivacidad = true },
                                    isLast   = rol != "nutriologo" && rol != "ginecologo"
                                )
                                if (rol == "nutriologo" || rol == "ginecologo") {
                                    CfgDividerLine()
                                    CfgRow(
                                        icon     = Icons.Rounded.Badge,
                                        iconBg   = Color(0xFFFFF3E0),
                                        iconTint = Color(0xFFE65100),
                                        title    = "Derechos ARCO: Datos de Cédula",
                                        subtitle = "Solicitar eliminación de datos de verificación",
                                        onClick  = { mostrarDialogoArco = true },
                                        isLast   = true
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Sesión ────────────────────────────────────────────────────
                item {
                    AnimatedSection(delayMs = 300) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                            Spacer(Modifier.height(20.dp))
                            CfgSectionLabel(ConfigSection.SESION.label)
                            Spacer(Modifier.height(8.dp))
                            CfgCard {
                                CfgRow(
                                    icon       = Icons.AutoMirrored.Rounded.ExitToApp,
                                    iconBg     = Color(0xFFFFEBEE),
                                    iconTint   = CfgRed,
                                    title      = "Cerrar sesión",
                                    subtitle   = "Se eliminará la sesión local",
                                    titleColor = CfgRed,
                                    onClick    = { mostrarDialogoCerrar = true }
                                )
                                CfgDividerLine()
                                CfgRow(
                                    icon       = Icons.Rounded.DeleteForever,
                                    iconBg     = Color(0xFFFFEBEE),
                                    iconTint   = CfgRed,
                                    title      = "Eliminar cuenta",
                                    subtitle   = "Acción irreversible",
                                    titleColor = CfgRed,
                                    onClick    = { mostrarDialogoEliminar = true },
                                    isLast     = true
                                )
                            }
                        }
                    }
                }

                // Versión
                item {
                    AnimatedSection(delayMs = 360) {
                        Text(
                            "NutriIA v1.0.0 · Hecho con cariño en México",
                            fontSize  = 11.sp,
                            color     = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp)
                        )
                    }
                }
            }
        }

        if (mostrarDialogoArco) {
            val repoLogin = remember { com.example.nutriia.auth.RepositorioLogin() }
            val currentUserId = com.example.nutriia.auth.RepositorioLogin().obtenerUsuarioActual()?.uid ?: ""

            AlertDialog(
                onDismissRequest = { if (!borrandoDatosArco) mostrarDialogoArco = false },
                title = { Text("Derechos ARCO: Eliminación de Cédula", fontWeight = FontWeight.Bold) },
                text = {
                    Column {
                        Text(
                            "Conforme a la LFPDPPP, tienes derecho a cancelar el tratamiento de tus datos personales. " +
                                    "Al confirmar, se eliminarán tus registros de verificación de cédula y consentimiento de nuestros servidores. " +
                                    "Tu cuenta requerirá una nueva verificación para continuar prestando servicios en NutriIA (needsReverification = true)."
                        )
                        if (mensajeArco != null) {
                            Spacer(Modifier.height(8.dp))
                            Text(mensajeArco!!, color = CfgGreen, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (currentUserId.isNotBlank()) {
                                borrandoDatosArco = true
                                coroutineScopeArco.launch {
                                    val ok = repoLogin.eliminarDatosVerificacionCedula(currentUserId)
                                    borrandoDatosArco = false
                                    if (ok) {
                                        mensajeArco = "Datos de verificación eliminados correctamente."
                                        kotlinx.coroutines.delay(1500)
                                        mostrarDialogoArco = false
                                        mensajeArco = null
                                    } else {
                                        mensajeArco = "Error al eliminar datos. Intenta de nuevo."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                        enabled = !borrandoDatosArco
                    ) {
                        if (borrandoDatosArco) CircularProgressIndicator(Modifier.size(16.dp), color = Color.White)
                        else Text("Eliminar datos")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { mostrarDialogoArco = false },
                        enabled = !borrandoDatosArco
                    ) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// PANTALLA POLÍTICA DE PRIVACIDAD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun PrivacidadScreen(onBack: () -> Unit) {

    data class PolicySection(val icon: ImageVector, val iconBg: Color, val iconTint: Color, val title: String, val body: String)

    val sections = listOf(
        PolicySection(
            Icons.Rounded.Info, Color(0xFFE3F2FD), Color(0xFF1565C0),
            "¿Qué información recopilamos?",
            "NutriIA recopila únicamente los datos que tú proporcionas: nombre y fecha de nacimiento del infante, medidas de peso y talla, alimentos registrados, y correo electrónico del tutor. No recopilamos datos de ubicación, contactos ni ningún otro dato del dispositivo sin tu consentimiento explícito."
        ),
        PolicySection(
            Icons.Rounded.Storage, Color(0xFFE8F5E9), Color(0xFF2E7D32),
            "¿Cómo almacenamos tus datos?",
            "Toda la información se almacena en servidores seguros en reposo y TLS 1.3 en tránsito. Los datos del infante se guardan bajo tu cuenta y nunca se comparten con terceros sin tu autorización. Puedes solicitar la eliminación total de tus datos en cualquier momento desde esta misma pantalla."
        ),
        PolicySection(
            Icons.Rounded.Psychology, Color(0xFFEDE7F6), Color(0xFF5E35B1),
            "Uso de inteligencia artificial",
            "Las funciones de IA (análisis de alimentos, NutriBot) procesan los datos del perfil del infante para generar recomendaciones personalizadas. Este procesamiento ocurre en servidores seguros y el resultado se almacena localmente en tu cuenta. NutriIA no usa tus datos para entrenar modelos de IA de terceros."
        ),
        PolicySection(
            Icons.Rounded.Share, Color(0xFFFFF8E1), Color(0xFFF57F17),
            "Compartición de datos con especialistas",
            "Si vinculas a un nutriólogo o pediatra, ese especialista tendrá acceso de solo lectura al historial de seguimiento del infante vinculado. Puedes desvincular a un especialista en cualquier momento desde el módulo «Mi nutriólogo / pediatra» y el acceso se revocará de inmediato."
        ),
        PolicySection(
            Icons.Rounded.ChildCare, Color(0xFFFCE4EC), Color(0xFFC2185B),
            "Protección de datos de menores",
            "NutriIA trata los datos de infantes con el máximo nivel de protección. No publicamos, monetizamos ni compartimos perfiles de niños. Solo el tutor registrado y los especialistas vinculados pueden acceder a esta información. Cumplimos con la Ley Federal de Protección de Datos Personales en Posesión de los Particulares (México)."
        ),
        PolicySection(
            Icons.Rounded.Notifications, Color(0xFFF3E5F5), Color(0xFF7B1FA2),
            "Notificaciones y comunicaciones",
            "Solo te enviaremos notificaciones relacionadas con el seguimiento del infante: recordatorios de comidas, alertas de crecimiento y tips semanales, según las preferencias que configures en la sección de ajustes. No realizamos envíos de marketing sin tu consentimiento."
        ),
        PolicySection(
            Icons.Rounded.Badge, Color(0xFFE8F5E9), Color(0xFF2E7D32),
            "Verificación de Cédula Profesional (SEP)",
            "Para garantizar la idoneidad y legitimidad de los especialistas que prestan servicios en NutriIA, recabamos el número de cédula profesional. La consulta se realiza de forma directa ante la fuente de acceso público oficial del Registro Nacional de Profesionistas (SEP). Se conservan únicamente los datos mínimos indispensables (cédula, estatus de validez, nombre del titular y profesión) y el registro del consentimiento otorgado. No se almacena la respuesta técnica cruda de la SEP. Puedes ejercer el derecho de Cancelación u Oposición de estos datos desde la sección de configuración de tu perfil."
        ),
        PolicySection(
            Icons.Rounded.ManageAccounts, Color(0xFFE0F2F1), Color(0xFF00695C),
            "Tus derechos ARCO",
            "Tienes derecho a Acceder, Rectificar, Cancelar y Oponerte al tratamiento de tus datos personales (derechos ARCO). Puedes ejercerlos escribiendo a nutriia2026@gmail.com o directamente desde tu perfil para eliminar datos de verificación de cédula. Daremos respuesta en un plazo máximo de 20 días hábiles conforme a la legislación mexicana vigente."
        ),
        PolicySection(
            Icons.Rounded.Update, Color(0xFFE8F5E9), Color(0xFF2E7D32),
            "Actualizaciones a esta política",
            "Podemos actualizar esta política para reflejar cambios en la aplicación o en la legislación. Te notificaremos dentro de la app con al menos 15 días de anticipación antes de que cualquier cambio sustancial entre en vigor. La fecha de última actualización aparece al pie de esta sección."
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CfgBg)
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 48.dp)
        ) {

            // Top bar
            item {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp)
                        .padding(top = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CfgCardWhite)
                            .border(0.5.dp, CfgBorder, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication        = null
                            ) { onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.ArrowBackIosNew,
                            contentDescription = "Regresar",
                            tint     = CfgGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            "Política de privacidad",
                            fontSize   = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color      = CfgDarkGreen
                        )
                        Text(
                            "Última actualización: abril 2026",
                            fontSize = 12.sp,
                            color    = Color.Gray
                        )
                    }
                }
            }

            // Banner intro
            item {
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { visible = true }
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(400)) + slideInVertically(tween(400, easing = EaseOutCubic)) { it / 2 }
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 20.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(CfgDarkGreen)
                            .padding(20.dp)
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Shield,
                                    contentDescription = null,
                                    tint     = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tu privacidad es nuestra prioridad",
                                fontSize   = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = Color.White,
                                lineHeight = 23.sp
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "NutriIA fue diseñada desde cero con la privacidad de los infantes y sus familias como principio fundamental. Aquí explicamos con claridad qué datos manejamos y por qué.",
                                fontSize   = 13.sp,
                                color      = Color.White.copy(alpha = 0.80f),
                                lineHeight = 19.sp
                            )
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }

            // Secciones animadas
            itemsIndexed(sections) { index, section ->
                var visible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay(index * 50L)
                    visible = true
                }
                AnimatedVisibility(
                    visible = visible,
                    enter   = fadeIn(tween(300)) + slideInVertically(tween(300, easing = EaseOutCubic)) { it / 3 }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        PolicyCard(section.icon, section.iconBg, section.iconTint, section.title, section.body)
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // Pie
            item {
                Column(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    HorizontalDivider(color = CfgBorder, thickness = 0.5.dp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Para dudas o ejercer tus derechos ARCO:",
                        fontSize  = 12.sp,
                        color     = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "nutriia2026@gmail.com",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = CfgGreen
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "NutriIA v1.0.0 · Hecho con cariño en México\nCumplimiento: LFPDPPP · Última actualización: abril 2026",
                        fontSize  = 11.sp,
                        color     = Color.Gray,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

@Composable
private fun PolicyCard(
    icon:    ImageVector,
    iconBg:  Color,
    iconTint:Color,
    title:   String,
    body:    String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(CfgCardWhite)
            .border(0.5.dp, CfgBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text       = title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color      = CfgDarkGreen,
                lineHeight = 19.sp,
                modifier   = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text       = body,
            fontSize   = 13.sp,
            color      = Color(0xFF555555),
            lineHeight = 19.sp
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// HELPER: SECCIÓN ANIMADA CON STAGGER
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun AnimatedSection(delayMs: Long, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMs)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter   = fadeIn(tween(350)) + slideInVertically(tween(350, easing = EaseOutCubic)) { it / 3 }
    ) {
        content()
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// TOP BAR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgTopBar(onBack: () -> Unit) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(CfgCardWhite)
                .border(0.5.dp, CfgBorder, CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication        = null,
                    onClickLabel      = "Regresar al inicio"
                ) { onBack() }
                .semantics { contentDescription = "Botón regresar. Toca para volver al dashboard." },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ArrowBackIosNew,
                contentDescription = null,
                tint     = CfgGreen,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Ajustes", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = CfgDarkGreen)
            Text("Personaliza tu experiencia", fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// SECTION LABEL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgSectionLabel(text: String) {
    Text(
        text          = text.uppercase(),
        fontSize      = 11.sp,
        fontWeight    = FontWeight.Bold,
        color         = CfgGreen,
        letterSpacing = 0.8.sp,
        modifier      = Modifier.padding(horizontal = 4.dp)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// CARD CONTENEDOR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgCard(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CfgCardWhite)
            .border(0.5.dp, CfgBorder, RoundedCornerShape(20.dp)),
        content = content
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// FILA PERFIL
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgProfileRow(nombre: String, email: String, rol: String, onClick: () -> Unit) {
    val inicial = nombre.take(2).uppercase()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClickLabel      = "Editar perfil de $nombre"
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(CfgGreen.copy(alpha = 0.12f))
                .border(1.5.dp, CfgGreen.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(inicial, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CfgGreen)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(nombre, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = CfgDarkGreen)
            Text(email,  fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(5.dp))
            val rolLabel = if (rol == "nutriologo") "Nutriólogo/a" else "Padre / Madre"
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(CfgGreen.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(rolLabel, fontSize = 11.sp, color = CfgGreen, fontWeight = FontWeight.SemiBold)
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(22.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FILA HIJO
// ═══════════════════════════════════════════════════════════════════════════════

private val hijoColors = listOf(
    Color(0xFFEC9BBF), Color(0xFF9C8FE0), Color(0xFFFFAB76),
    Color(0xFF4DB6AC), Color(0xFF81C784), Color(0xFF64B5F6)
)

@Composable
private fun CfgHijoRow(child: ChildProfile, onClick: () -> Unit) {
    val color = hijoColors[child.name.hashCode().and(0x7FFFFFFF) % hijoColors.size]
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClickLabel      = "Editar perfil de ${child.name}"
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f))
                .border(1.5.dp, color.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(child.name.take(1).uppercase(), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(child.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = CfgDarkGreen)
            Text("Región ${child.region.label}", fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 1.dp))
            if (child.hasAllergies && child.allergiesDetail.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFF3E0))
                        .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                    Text(
                        "Alergia: ${child.allergiesDetail.take(20)}",
                        fontSize = 10.sp, color = Color(0xFFE65100), fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FILA GENÉRICA
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgRow(
    icon:               ImageVector,
    iconBg:             Color,
    iconTint:           Color,
    title:              String,
    subtitle:           String?     = null,
    titleColor:         Color       = CfgDarkGreen,
    onClick:            () -> Unit  = {},
    isLast:             Boolean     = false,
    subtitleComposable: @Composable (() -> Unit)? = null
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label         = "rowScale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,
                onClickLabel      = title
            ) { onClick() }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = titleColor)
            if (subtitleComposable != null) {
                Spacer(Modifier.height(4.dp))
                subtitleComposable()
            } else if (!subtitle.isNullOrBlank()) {
                Text(subtitle, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(20.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// FILA TOGGLE
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgToggleRow(
    icon:            ImageVector,
    iconBg:          Color,
    iconTint:        Color,
    title:           String,
    subtitle:        String,
    checked:         Boolean,
    onCheckedChange: (Boolean) -> Unit,
    isLast:          Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null
            ) { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 13.dp)
            .semantics {
                contentDescription = "$title. ${if (checked) "Activado" else "Desactivado"}. Toca para cambiar."
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = CfgDarkGreen)
            Text(subtitle, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
        Switch(
            checked         = checked,
            onCheckedChange = onCheckedChange,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = Color.White,
                checkedTrackColor   = CfgGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFBDBDBD)
            )
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// CHIP A11Y
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgA11yChip(modo: AccessibilityMode) {
    val (label, bg, fg) = when (modo) {
        AccessibilityMode.NORMAL -> Triple("Normal",         Color(0xFFE8F5E9), CfgGreen)
        AccessibilityMode.BLIND  -> Triple("Modo ciego",     Color(0xFFEDE7F6), Color(0xFF5E35B1))
        AccessibilityMode.MUTE   -> Triple("Voz silenciada", Color(0xFFE0F2F1), Color(0xFF00695C))
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(fg))
        Spacer(Modifier.width(5.dp))
        Text(label, fontSize = 11.sp, color = fg, fontWeight = FontWeight.SemiBold)
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// DIVISOR
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgDividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(0.5.dp)
            .background(CfgDivider)
    )
}

// ═══════════════════════════════════════════════════════════════════════════════
// DIÁLOGOS
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
private fun CfgAlertDialog(
    icon:         ImageVector,
    iconTint:     Color,
    title:        String,
    body:         String,
    confirmLabel: String,
    onConfirm:    () -> Unit,
    onDismiss:    () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(30.dp)) },
        title = { Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text  = { Text(body, fontSize = 14.sp, textAlign = TextAlign.Center, color = Color.Gray, lineHeight = 20.sp) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors  = ButtonDefaults.buttonColors(containerColor = CfgRed),
                shape   = RoundedCornerShape(12.dp)
            ) { Text(confirmLabel, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun CfgCambiarPasswordDialog(
    emailUsuario: String,
    onCambiarDirecto: (contrasenaActual: String, nuevaContrasena: String, onResultado: (Boolean, String) -> Unit) -> Unit,
    onEnviarCorreo: () -> Unit,
    onDismiss: () -> Unit
) {
        var contrasenaActual by remember { mutableStateOf("") }
    var nuevaContrasena by remember { mutableStateOf("") }
    var confirmarContrasena by remember { mutableStateOf("") }

    var passActualVisible by remember { mutableStateOf(false) }
    var nuevaPassVisible by remember { mutableStateOf(false) }

    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var mensajeExito by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.Lock, contentDescription = null, tint = Color(0xFFC2185B), modifier = Modifier.size(30.dp)) },
        title = { Text("Cambiar contraseña", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (mensajeError != null) {
                    Text(mensajeError!!, color = CfgRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                if (mensajeExito != null) {
                    Text(mensajeExito!!, color = CfgGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                OutlinedTextField(
                    value = contrasenaActual,
                    onValueChange = { contrasenaActual = it; mensajeError = null },
                    label = { Text("Contraseña actual") },
                    singleLine = true,
                    visualTransformation = if (passActualVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passActualVisible = !passActualVisible }) {
                            Icon(
                                if (passActualVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = nuevaContrasena,
                    onValueChange = { nuevaContrasena = it; mensajeError = null },
                    label = { Text("Nueva contraseña (mín 6 car.)") },
                    singleLine = true,
                    visualTransformation = if (nuevaPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { nuevaPassVisible = !nuevaPassVisible }) {
                            Icon(
                                if (nuevaPassVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = confirmarContrasena,
                    onValueChange = { confirmarContrasena = it; mensajeError = null },
                    label = { Text("Confirmar nueva contraseña") },
                    singleLine = true,
                    visualTransformation = if (nuevaPassVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )

                TextButton(
                    onClick = {
                        onEnviarCorreo()
                        mensajeExito = "Correo de recuperación enviado a $emailUsuario"
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("¿Olvidaste tu clave? Enviar correo", fontSize = 12.sp, color = CfgGreen)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contrasenaActual.isBlank() || nuevaContrasena.isBlank() || confirmarContrasena.isBlank()) {
                        mensajeError = "Completa todos los campos"
                        return@Button
                    }
                    if (nuevaContrasena != confirmarContrasena) {
                        mensajeError = "Las nuevas contraseñas no coinciden"
                        return@Button
                    }
                    if (nuevaContrasena.length < 6) {
                        mensajeError = "La nueva clave debe tener al menos 6 caracteres"
                        return@Button
                    }
                    cargando = true
                    mensajeError = null
                    onCambiarDirecto(contrasenaActual, nuevaContrasena) { exito, msg ->
                        cargando = false
                        if (exito) {
                            mensajeExito = "Contraseña actualizada exitosamente"
                            
                            onDismiss()
                        } else {
                            mensajeError = msg
                        }
                    }
                },
                enabled = !cargando,
                colors = ButtonDefaults.buttonColors(containerColor = CfgGreen),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Actualizar", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun CfgEliminarCuentaDialog(
    onEliminar: (contrasenaActual: String, onResultado: (Boolean, String) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    var contrasenaActual by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }
    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = CfgRed, modifier = Modifier.size(32.dp)) },
        title = { Text("¿Eliminar cuenta?", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Esta acción es irreversible. Se eliminarán permanentemente tus datos, perfil e historial.",
                    fontSize = 13.sp, textAlign = TextAlign.Center, color = Color.Gray, lineHeight = 18.sp
                )
                Surface(color = CfgRed.copy(alpha = 0.08f), shape = RoundedCornerShape(12.dp)) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, contentDescription = null, tint = CfgRed, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ingresa tu contraseña actual para confirmar la eliminación.", fontSize = 12.sp, color = CfgRed)
                    }
                }

                if (mensajeError != null) {
                    Text(mensajeError!!, color = CfgRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                OutlinedTextField(
                    value = contrasenaActual,
                    onValueChange = { contrasenaActual = it; mensajeError = null },
                    label = { Text("Contraseña actual") },
                    singleLine = true,
                    visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passVisible = !passVisible }) {
                            Icon(
                                if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (contrasenaActual.isBlank()) {
                        mensajeError = "Ingresa tu contraseña actual"
                        return@Button
                    }
                    cargando = true
                    mensajeError = null
                    onEliminar(contrasenaActual) { exito, msg ->
                        cargando = false
                        if (exito) {
                            onDismiss()
                        } else {
                            mensajeError = msg
                        }
                    }
                },
                enabled = !cargando,
                colors = ButtonDefaults.buttonColors(containerColor = CfgRed),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (cargando) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Eliminar cuenta", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = Color.Gray) }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PermissionSettingRow(type: PermissionType, isLast: Boolean = false) {
        var isGranted by remember {
        mutableStateOf(PermissionHelper.hasPermissions(permissions = PermissionHelper.getRequiredPermissions(type)))
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isGranted = PermissionHelper.hasPermissions(permissions = PermissionHelper.getRequiredPermissions(type))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val state = rememberPermissionState(type = type) {
        isGranted = true
    }

    val icon = when (type) {
        PermissionType.CAMERA -> Icons.Rounded.CameraAlt
        PermissionType.MICROPHONE -> Icons.Rounded.Mic
        PermissionType.PHONE -> Icons.Rounded.Phone
        PermissionType.NEAR_DEVICES -> Icons.Rounded.Bluetooth
    }

    val (iconBg, iconTint) = when (type) {
        PermissionType.CAMERA -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        PermissionType.MICROPHONE -> Color(0xFFEDE7F6) to Color(0xFF5E35B1)
        PermissionType.PHONE -> Color(0xFFFFF3E0) to Color(0xFFEF6C00)
        PermissionType.NEAR_DEVICES -> Color(0xFFE1F5FE) to Color(0xFF0277BD)
    }

    CfgRow(
        icon = icon,
        iconBg = iconBg,
        iconTint = iconTint,
        title = type.displayName,
        subtitle = if (isGranted) "Concedido" else "No otorgado (toca para activar)",
        titleColor = CfgDarkGreen,
        onClick = {
            if (!isGranted) {
                state.requestPermission()
            }
        },
        isLast = isLast
    )
}