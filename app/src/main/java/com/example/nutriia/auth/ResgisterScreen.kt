package com.example.nutriia.auth

import android.util.Patterns
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.focus.onFocusChanged
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.loc
import com.example.nutriia.accesibilidad.LocalAccessibilityMode
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.accesibilidad.Voz
import com.example.nutriia.accesibilidad.VozEn
import com.example.nutriia.accesibilidad.VoiceInputManager
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

// ─── COLORES ──────────────────────────────────────────────────────────────────
private val RegGreen     = Color(0xFF689F38)
private val RegDarkGreen = Color(0xFF33691E)
private val RegBgCrema   = Color(0xFFF8F9F3)
private val RegCardWhite = Color.White
private val RegPurple    = Color(0xFF9C8FE0)
private val RegTeal      = Color(0xFF4DB6AC)
private val RegOrange    = Color(0xFFFF8F00)
val RegRosa      = Color(0xFFEC9BBF)
val RegRosaGine  = Color(0xFFF06292)

private const val DEBOUNCE_MS = 2500L

// ─── Normaliza texto dictado por voz a formato de correo electrónico ──────────
fun normalizarCorreoVoz(texto: String): String {
    return texto
        .lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ü", "u")
        .replace("ñ", "n")
        .replace("arroba",    "@")
        .replace("at sign",   "@")
        .replace("at",        "@")
        .replace("punto com", ".com")
        .replace("punto net", ".net")
        .replace("punto org", ".org")
        .replace("punto mx",  ".mx")
        .replace("punto edu", ".edu")
        .replace("punto io",  ".io")
        .replace("punto co",  ".co")
        .replace("punto",     ".")
        .replace("dot com",   ".com")
        .replace("dot net",   ".net")
        .replace("dot org",   ".org")
        .replace("dot mx",    ".mx")
        .replace("dot edu",   ".edu")
        .replace("dot io",    ".io")
        .replace("dot",       ".")
        .replace("guion bajo", "_")
        .replace("guión bajo", "_")
        .replace("underscore", "_")
        .replace("guion",      "-")
        .replace("guión",      "-")
        .replace("dash",       "-")
        .replace(" ", "")
        .trim()
}

// ─── Normaliza texto dictado por voz eliminando espacios entre dígitos ───────
// Solo conserva dígitos: "1 2 3 4 5 6" → "123456"
fun normalizarCedulaVoz(texto: String): String =
    texto.filter { it.isDigit() }

private fun esCorreoDictado(texto: String): Boolean =
    texto.contains("arroba", ignoreCase = true) ||
            texto.contains("punto",   ignoreCase = true) ||
            texto.contains(" at ",    ignoreCase = true) ||
            texto.contains("dot ",    ignoreCase = true)

private fun tieneDiezDigitos(texto: String): Boolean =
    texto.filter { it.isDigit() }.length >= 10

fun coincideNombreConTitular(nombreIngresado: String, nombreTitularSEP: String): Boolean {
    if (nombreIngresado.isBlank() || nombreTitularSEP.isBlank()) return false
    fun limpiar(s: String): List<String> {
        return s.lowercase()
            .replace("á", "a").replace("é", "e").replace("í", "i")
            .replace("ó", "o").replace("ú", "u").replace("ü", "u")
            .replace("ñ", "n")
            .replace(Regex("""[^a-z0-9\s]"""), " ")
            .split(Regex("""\s+"""))
            .filter { 
                it.isNotBlank() && it !in setOf(
                    "dr", "dra", "lic", "med", "doctor", "doctora", "licenciado", "licenciada", 
                    "nut", "nutriologo", "nutriologa", "gine", "ginecologo", "ginecologa",
                    "de", "del", "la", "las", "los", "y", "san", "santa"
                ) 
            }
    }
    val palabrasIngresadas = limpiar(nombreIngresado)
    val palabrasTitular = limpiar(nombreTitularSEP)
    if (palabrasIngresadas.isEmpty() || palabrasTitular.isEmpty()) return false
    val coincidencias = palabrasIngresadas.count { palabra ->
        palabrasTitular.any { tit -> tit == palabra || (palabra.length >= 4 && (tit.startsWith(palabra) || palabra.startsWith(tit))) }
    }
    val minimoRequerido = if (palabrasIngresadas.size == 1) 1 else 2.coerceAtMost(palabrasIngresadas.size)
    return coincidencias >= minimoRequerido
}

fun esProfesionValidaNutriologo(profesionSEP: String): Boolean {
    if (profesionSEP.isBlank()) return true
    val p = profesionSEP.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ü", "u")
        .replace("ñ", "n")
    val terminosValidos = listOf("nutri", "diet", "alimento", "medic", "cirujan", "pediatr", "salud", "clinica", "gastro")
    return terminosValidos.any { p.contains(it) }
}

fun esProfesionValidaGinecologo(profesionSEP: String): Boolean {
    if (profesionSEP.isBlank()) return true
    val p = profesionSEP.lowercase()
        .replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ü", "u")
        .replace("ñ", "n")
    val terminosValidos = listOf("medic", "cirujan", "ginec", "obstetr", "parter", "salud", "feto", "perinatal", "reproductiva")
    return terminosValidos.any { p.contains(it) }
}

// ─── MODELOS DE DATOS ─────────────────────────────────────────────────────────

data class ParentRegisterData(
    val name:             String = "",
    val email:            String = "",
    val password:         String = "",
    val phone:            String = "",
    val nutritionistCode: String = "",
    val childName:        String = ""
)

data class NutritionistRegisterData(
    val name:      String = "",
    val email:     String = "",
    val password:  String = "",
    val phone:     String = "",
    val specialty: String = "",
    val licenseId: String = ""
)

data class GynecologistRegisterData(
    val name:      String = "",
    val email:     String = "",
    val password:  String = "",
    val phone:     String = "",
    val specialty: String = "",
    val licenseId: String = ""
)

data class MamaPrimerizaRegisterData(
    val name:     String = "",
    val email:    String = "",
    val password: String = "",
    val phone:    String = "",
    val semanas:  Int = 1
)

// ═════════════════════════════════════════════════════════════════════════════
// 1. PANTALLA DE SELECCIÓN DE TIPO DE CUENTA
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun RegisterTypeScreen(
    onNavigateBack:        () -> Unit,
    onSelectParent:        () -> Unit,
    onSelectNutritionist:  () -> Unit,
    onSelectMamaPrimeriza: () -> Unit,
    onSelectGinecologo:    () -> Unit
) {
    val a11yMode     = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val idiomaActual by a11yVm.idioma.collectAsState()

    // ── Helper local de localización ──────────────────────────────────────────
    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    LaunchedEffect(Unit) {
        if (a11yMode == AccessibilityMode.BLIND)
            a11yVm.hablar(loc(Voz.REGISTRO_TIPO_INTRO, VozEn.REGISTRO_TIPO_INTRO))
    }

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha  by animateFloatAsState(
        targetValue   = if (visible) 1f else 0f,
        animationSpec = tween(500),
        label         = "alpha"
    )
    val slideY by animateFloatAsState(
        targetValue   = if (visible) 0f else 40f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label         = "slideY"
    )

    Box(modifier = Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick  = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar(loc("Regresando a inicio de sesión.", "Going back to sign in."))
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(RegCardWhite)
                        .semantics {
                            contentDescription = loc(
                                "Botón volver. Regresa al inicio de sesión.",
                                "Back button. Returns to sign in."
                            )
                        }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = RegGreen)
                }
            }
            Spacer(Modifier.height(32.dp))

            Column(
                modifier            = Modifier
                    .offset(y = slideY.dp)
                    .alpha(alpha),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(RegGreen.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PersonAdd,
                        contentDescription = null,
                        tint     = RegGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    loc("Crear cuenta", "Create account"),
                    fontSize   = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = RegDarkGreen
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    loc("¿Cómo vas a usar NutriIA?", "How will you use NutriIA?"),
                    fontSize  = 15.sp,
                    color     = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(48.dp))

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .alpha(alpha),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AccountTypeCard(
                    title       = loc("Soy Padre / Madre", "I'm a Parent"),
                    description = loc(
                        "Registra a tu hijo/a y lleva su seguimiento nutricional personalizado.",
                        "Register your child and track their personalized nutritional progress."
                    ),
                    icon      = Icons.Rounded.FamilyRestroom,
                    iconColor = RegGreen,
                    tag       = loc("Familia", "Family"),
                    tagColor  = RegGreen,
                    onClick   = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar(loc(
                                "Abriendo registro de padre o madre.",
                                "Opening parent registration."
                            ))
                        onSelectParent()
                    }
                )
                AccountTypeCard(
                    title       = loc("Mamá Primeriza", "First-time Mom"),
                    description = loc(
                        "Seguimiento especializado durante tu embarazo y nutrición prenatal.",
                        "Specialized tracking during your pregnancy and prenatal nutrition."
                    ),
                    icon      = Icons.Rounded.Favorite,
                    iconColor = RegRosa,
                    tag       = loc("Embarazo", "Pregnancy"),
                    tagColor  = RegRosa,
                    onClick   = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar(loc(
                                "Abriendo registro de mamá primeriza.",
                                "Opening first-time mom registration."
                            ))
                        onSelectMamaPrimeriza()
                    }
                )
                AccountTypeCard(
                    title       = loc("Soy Nutriólogo/a", "I'm a Nutritionist"),
                    description = loc(
                        "Gestiona pacientes, planes de alimentación y seguimiento clínico.",
                        "Manage patients, meal plans, and clinical follow-up."
                    ),
                    icon      = Icons.Rounded.MedicalServices,
                    iconColor = RegTeal,
                    tag       = loc("Profesional", "Professional"),
                    tagColor  = RegTeal,
                    onClick   = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar(loc(
                                "Abriendo registro de nutriólogo.",
                                "Opening nutritionist registration."
                            ))
                        onSelectNutritionist()
                    }
                )
                AccountTypeCard(
                    title       = loc("Soy Ginecólogo/a", "I'm a Gynecologist"),
                    description = loc(
                        "Especialista en salud femenina y seguimiento del embarazo.",
                        "Specialist in women's health and pregnancy tracking."
                    ),
                    icon      = Icons.Rounded.Female,
                    iconColor = RegRosaGine,
                    tag       = loc("Profesional", "Professional"),
                    tagColor  = RegRosaGine,
                    onClick   = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar(loc(
                                "Abriendo registro de ginecólogo.",
                                "Opening gynecologist registration."
                            ))
                        onSelectGinecologo()
                    }
                )
            }

            Spacer(Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.padding(bottom = 32.dp)
            ) {
                Text(
                    loc("¿Ya tienes cuenta? ", "Already have an account? "),
                    color    = Color.Gray,
                    fontSize = 14.sp
                )
                TextButton(
                    onClick  = {
                        if (a11yMode == AccessibilityMode.BLIND)
                            a11yVm.hablar(loc("Regresando a inicio de sesión.", "Going back to sign in."))
                        onNavigateBack()
                    },
                    modifier = Modifier.semantics {
                        contentDescription = loc(
                            "Botón Inicia sesión. Parte inferior.",
                            "Sign in button. Bottom of screen."
                        )
                    }
                ) {
                    Text(
                        loc("Inicia sesión", "Sign in"),
                        color      = RegDarkGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountTypeCard(
    title:       String,
    description: String,
    icon:        ImageVector,
    iconColor:   Color,
    tag:         String,
    tagColor:    Color,
    onClick:     () -> Unit
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) 0.97f else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "press"
    )

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable { pressed = true; onClick() },
        shape     = RoundedCornerShape(28.dp),
        colors    = CardDefaults.cardColors(containerColor = RegCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = RegDarkGreen)
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(tagColor.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(tag, fontSize = 10.sp, color = tagColor, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(description, fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
            }
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint     = iconColor.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
    LaunchedEffect(pressed) { if (pressed) { delay(120); pressed = false } }
}

// ═════════════════════════════════════════════════════════════════════════════
// 2. REGISTRO PADRE / MADRE
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun ParentRegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateBack:    () -> Unit,
    onRegisterSuccess: (ParentRegisterData) -> Unit
) {
    var data            by remember { mutableStateOf(ParentRegisterData()) }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }
    var nameError       by remember { mutableStateOf<String?>(null) }
    var emailError      by remember { mutableStateOf<String?>(null) }
    var passwordError   by remember { mutableStateOf<String?>(null) }
    var confirmError    by remember { mutableStateOf<String?>(null) }
    var phoneError      by remember { mutableStateOf<String?>(null) }
    var childNameError  by remember { mutableStateOf<String?>(null) }
    var firebaseError   by remember { mutableStateOf<String?>(null) }

    val estado by viewModel.estado.collectAsState()

    val a11yMode     = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val esBlind      = a11yMode == AccessibilityMode.BLIND
    val esMute       = a11yMode == AccessibilityMode.MUTE
    val esAccesible  = esBlind || esMute
    val ttsManager   = if (esBlind) a11yVm.ttsManager else null

    // ── Helper local de localización ──────────────────────────────────────────
    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    val dataRef = rememberUpdatedState(data)

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }

    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> data.name
            1 -> data.phone
            2 -> data.email
            3 -> data.password
            4 -> confirmPassword
            5 -> data.childName
            6 -> data.nutritionistCode
            else -> ""
        }
    }

    // ── Debounce por campo (modo ciego) ───────────────────────────────────────
    LaunchedEffect(data.name) {
        if (!esBlind || data.name.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (data.name == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.name.isNotBlank() && campoActivo == 0 && data.name != valorInicial) campoActivo = 1
    }
    LaunchedEffect(data.phone) {
        if (!esBlind || data.phone.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (data.phone == valorInicial) return@LaunchedEffect
        if (!tieneDiezDigitos(data.phone)) return@LaunchedEffect
        delay(600L)
        if (tieneDiezDigitos(data.phone) && campoActivo == 1 && data.phone != valorInicial) campoActivo = 2
    }
    LaunchedEffect(data.email) {
        if (!esBlind || data.email.isBlank() || campoActivo != 2) return@LaunchedEffect
        if (data.email == valorInicial) return@LaunchedEffect
        if (!data.email.contains("@") || !data.email.contains(".")) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.email.contains("@") && data.email.contains(".") && campoActivo == 2 && data.email != valorInicial) campoActivo = 3
    }
    LaunchedEffect(data.password) {
        if (!esBlind || data.password.length < 6 || campoActivo != 3) return@LaunchedEffect
        if (data.password == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.password.length >= 6 && campoActivo == 3 && data.password != valorInicial) campoActivo = 4
    }
    LaunchedEffect(confirmPassword) {
        if (!esBlind || confirmPassword.length < 6 || campoActivo != 4) return@LaunchedEffect
        if (confirmPassword == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (confirmPassword.length >= 6 && campoActivo == 4 && confirmPassword != valorInicial) campoActivo = 5
    }
    LaunchedEffect(data.childName) {
        if (!esBlind || data.childName.isBlank() || campoActivo != 5) return@LaunchedEffect
        if (data.childName == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.childName.isNotBlank() && campoActivo == 5 && data.childName != valorInicial) campoActivo = 6
    }
    LaunchedEffect(data.nutritionistCode) {
        if (!esBlind || data.nutritionistCode.isBlank() || campoActivo != 6) return@LaunchedEffect
        if (data.nutritionistCode == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.nutritionistCode.isNotBlank() && campoActivo == 6 && data.nutritionistCode != valorInicial) campoActivo = 7
    }

    LaunchedEffect(Unit) {
        if (esBlind) a11yVm.hablar(loc(Voz.REGISTRO_PADRE_INTRO, VozEn.REGISTRO_PADRE_INTRO))
    }

    LaunchedEffect(campoActivo) {
        if (esBlind && campoActivo == 6) {
            a11yVm.hablar(loc(
                "Todos los campos obligatorios han sido completados. El botón verde para Crear Cuenta y Continuar está ubicado en la parte inferior de la pantalla.",
                "All required fields have been completed. The green Create Account and Continue button is located at the bottom of the screen."
            ))
        }
    }

    // ── Navegación post-registro ──────────────────────────────────────────────
    LaunchedEffect(estado) {
        when (val s = estado) {
            is RegisterUiState.Exito -> {
                if (esBlind) {
                    ttsManager?.hablarYEsperar(
                        loc(
                            "Cuenta creada exitosamente. Ahora registraremos los datos de tu hijo.",
                            "Account created successfully. Now let's register your child's information."
                        ),
                        margenMs = 500L
                    )
                }
                onRegisterSuccess(dataRef.value)
                viewModel.resetEstado()
            }
            is RegisterUiState.Error -> {
                firebaseError = s.mensaje
                if (esBlind) a11yVm.hablar(
                    loc(
                        "Error al registrar. ${s.mensaje}. Intenta de nuevo.",
                        "Registration error. ${s.mensaje}. Please try again."
                    )
                )
            }
            else -> { /* Idle / Loading */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(52.dp))
            RegisterScreenHeader(
                onBack    = {
                    if (esBlind) a11yVm.hablar(loc("Regresando.", "Going back."))
                    onNavigateBack()
                },
                icon      = Icons.Rounded.FamilyRestroom,
                iconColor = RegGreen,
                title     = loc("Registro de Padre/Madre", "Parent Registration"),
                subtitle  = loc("Crea tu cuenta familiar", "Create your family account")
            )
            Spacer(Modifier.height(28.dp))

            RegisterSectionTitle(
                loc("Datos personales", "Personal info"),
                Icons.Rounded.Person,
                RegGreen
            )
            Spacer(Modifier.height(12.dp))

            // ── Campo 1: Nombre ───────────────────────────────────────────────
            RegisterField(
                value           = data.name,
                onValueChange   = { data = data.copy(name = it); nameError = null },
                label           = loc("Nombre completo", "Full name"),
                icon            = Icons.Rounded.Person,
                error           = nameError,
                a11yLabel       = loc(
                     "Campo 1 de 6. Nombre completo. Di tu nombre y apellidos.",
                     "Field 1 of 6. Full name. Say your first and last name."
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                a11yActive      = esAccesible,
                activo          = campoActivo == 0,
                onFocus         = { campoActivo = 0 },
                onNext          = { campoActivo = 1 },
                ttsManager      = if (campoActivo == 0) ttsManager else null,
                idioma          = idiomaActual
            )

            // ── Campo 2: Teléfono ─────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 1) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.phone,
                        onValueChange   = { data = data.copy(phone = it); phoneError = null },
                        label           = loc("Teléfono", "Phone"),
                        icon            = Icons.Rounded.Phone,
                        error           = phoneError,
                        a11yLabel       = loc(
                             "Campo 2 de 6. Teléfono. Di los diez dígitos de tu número.",
                             "Field 2 of 6. Phone. Say your ten-digit phone number."
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 1,
                        onFocus         = { campoActivo = 1 },
                        onNext          = { campoActivo = 2 },
                        ttsManager      = if (campoActivo == 1) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            // ── Campo 3: Correo ───────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 2) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(
                        loc("Acceso a la cuenta", "Account access"),
                        Icons.Rounded.Lock,
                        RegPurple
                    )
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value         = data.email,
                        onValueChange = { raw ->
                            val norm = if (esCorreoDictado(raw)) normalizarCorreoVoz(raw)
                            else raw.trim().lowercase()
                            data = data.copy(email = norm)
                             emailError = null
                        },
                        label           = loc("Correo electrónico", "Email address"),
                        icon            = Icons.Rounded.Email,
                        error           = emailError,
                        a11yLabel       = loc(
                             "Campo 3 de 6. Correo electrónico. Di tu correo. Por ejemplo: juan arroba gmail punto com.",
                             "Field 3 of 6. Email address. Say your email. For example: john at gmail dot com."
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 2,
                        onFocus         = { campoActivo = 2 },
                        onNext          = { campoActivo = 3 },
                        ttsManager      = if (campoActivo == 2) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            // ── Campo 4: Clave ────────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 3) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = data.password,
                        onValueChange    = { data = data.copy(password = it); passwordError = null },
                        label            = loc("Clave de acceso", "Password"),
                        icon             = Icons.Rounded.Lock,
                        error            = passwordError,
                        a11yLabel        = loc(
                             "Campo 4 de 6. Clave de acceso. Di tu clave, mínimo seis caracteres. Usa letras y números.",
                             "Field 4 of 6. Password. Say your password, at least six characters. Use letters and numbers."
                        ),
                        isPassword       = true,
                        showPassword     = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 3,
                        onFocus          = { campoActivo = 3 },
                        onNext           = { campoActivo = 4 },
                        ttsManager       = if (campoActivo == 3) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            // ── Campo 5: Confirmar clave ──────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 4) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = confirmPassword,
                        onValueChange    = { confirmPassword = it; confirmError = null },
                        label            = loc("Confirmar clave", "Confirm password"),
                        icon             = Icons.Rounded.LockOpen,
                        error            = confirmError,
                        a11yLabel        = loc(
                             "Campo 5 de 6. Confirmar clave. Repite exactamente la misma clave de acceso.",
                             "Field 5 of 6. Confirm password. Repeat exactly the same password."
                        ),
                        isPassword       = true,
                        showPassword     = showConfirm,
                        onTogglePassword = { showConfirm = !showConfirm },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 4,
                        onFocus          = { campoActivo = 4 },
                        onNext           = { campoActivo = 5 },
                        ttsManager       = if (campoActivo == 4) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            // ── Campo 6: Nombre del hijo ──────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 5) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(
                        loc("Tu primer hijo/a", "Your first child"),
                        Icons.Rounded.ChildCare,
                        RegOrange
                    )
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.childName,
                        onValueChange   = { data = data.copy(childName = it); childNameError = null },
                        label           = loc("Nombre del niño/a", "Child's name"),
                        icon            = Icons.Rounded.Face,
                        error           = childNameError,
                        a11yLabel       = loc(
                            "Campo 6 de 6. Nombre de tu hijo o hija. Di el nombre completo.",
                            "Field 6 of 6. Your child's name. Say the full name."
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 5,
                        onFocus         = { campoActivo = 5 },
                        onNext          = { campoActivo = 6 },
                        ttsManager      = if (campoActivo == 5) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            // ── Código nutriólogo (opcional) ──────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 6) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(
                        loc("Vincular nutriólogo", "Link nutritionist"),
                        Icons.Rounded.MedicalServices,
                        RegTeal,
                        isOptional = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(RegTeal.copy(alpha = 0.07f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null, tint = RegTeal, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            loc(
                                "Puedes vincularte después desde tu perfil si no tienes el código ahora.",
                                "You can link from your profile later if you don't have the code now."
                            ),
                            fontSize   = 12.sp,
                            color      = Color.DarkGray,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value         = data.nutritionistCode,
                        onValueChange = { data = data.copy(nutritionistCode = it.uppercase()) },
                        label         = loc("Código del nutriólogo (opcional)", "Nutritionist code (optional)"),
                        icon          = Icons.Rounded.QrCode2,
                        placeholder   = "Ej. NUTRI-A3X7F2",
                        a11yLabel     = loc(
                            "Todos los datos principales han sido completados. Este campo de código es opcional. Puedes dictarlo, o decir guardar para finalizar el registro.",
                            "All required fields complete. This nutritionist code field is optional. Say the code, or say save to finish registration."
                        ),
                        a11yActive    = esAccesible,
                        activo        = campoActivo == 6,
                        onFocus       = { campoActivo = 6 },
                        onNext        = { campoActivo = 7 },
                        ttsManager    = if (campoActivo == 6) ttsManager else null,
                        idioma        = idiomaActual
                    )
                }
            }

            // ── Botón crear cuenta ────────────────────────────────────────────
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = {
                    var hasErrors = false
                    if (data.name.isBlank()) {
                        nameError  = loc("El nombre es requerido", "Name is required"); hasErrors = true
                    }
                    if (data.phone.isBlank()) {
                        phoneError = loc("El teléfono es requerido", "Phone is required"); hasErrors = true
                    } else if (!tieneDiezDigitos(data.phone)) {
                        phoneError = loc("Debe tener al menos 10 dígitos", "Must have at least 10 digits"); hasErrors = true
                    }
                    if (data.email.isBlank()) {
                        emailError = loc("El correo es requerido", "Email is required"); hasErrors = true
                    } else if (!Patterns.EMAIL_ADDRESS.matcher(data.email).matches()) {
                        emailError = loc("Correo inválido", "Invalid email"); hasErrors = true
                    }
                    if (data.password.length < 6) {
                        passwordError = loc("Mínimo 6 caracteres", "Minimum 6 characters"); hasErrors = true
                    }
                    if (confirmPassword != data.password) {
                        confirmError = loc("Las claves no coinciden", "Passwords do not match"); hasErrors = true
                    }
                    if (data.childName.isBlank()) {
                        childNameError = loc("Escribe el nombre de tu hijo/a", "Enter your child's name"); hasErrors = true
                    }
                    if (hasErrors) {
                        if (esBlind) a11yVm.hablar(loc(Voz.REGISTRO_ERROR_CAMPOS, VozEn.REGISTRO_ERROR_CAMPOS))
                    } else {
                        if (esBlind) a11yVm.hablar(loc(
                            "Creando tu cuenta. Por favor espera.",
                            "Creating your account. Please wait."
                        ))
                        viewModel.registrarPadre(data, confirmPassword)
                    }
                },
                enabled  = estado !is RegisterUiState.Loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (esBlind) 70.dp else 56.dp)
                    .semantics {
                        contentDescription = loc(
                            "Botón Crear cuenta y continuar. Parte inferior.",
                            "Create account and continue button. Bottom of screen."
                        )
                    },
                shape  = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = RegGreen,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                if (estado is RegisterUiState.Loading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        loc("Crear cuenta y continuar", "Create account and continue"),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(36.dp))
        }

        firebaseError?.let { msg ->
            AlertDialog(
                onDismissRequest = { firebaseError = null; viewModel.resetEstado() },
                title = {
                    Text(
                        loc("Error al registrar", "Registration error"),
                        fontWeight = FontWeight.Bold
                    )
                },
                text  = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { firebaseError = null; viewModel.resetEstado() }) {
                        Text(
                            loc("Entendido", "Got it"),
                            color      = RegGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 3. REGISTRO MAMÁ PRIMERIZA
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun MamaPrimerizaRegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateBack:    () -> Unit,
    onRegisterSuccess: (MamaPrimerizaRegisterData) -> Unit
) {
    var data            by remember { mutableStateOf(MamaPrimerizaRegisterData()) }
    var confirmPassword by remember { mutableStateOf("") }
    var showPassword    by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }
    var nameError       by remember { mutableStateOf<String?>(null) }
    var emailError      by remember { mutableStateOf<String?>(null) }
    var passwordError   by remember { mutableStateOf<String?>(null) }
    var confirmError    by remember { mutableStateOf<String?>(null) }
    var phoneError      by remember { mutableStateOf<String?>(null) }
    var semanasError    by remember { mutableStateOf<String?>(null) }
    var firebaseError   by remember { mutableStateOf<String?>(null) }

    val estado by viewModel.estado.collectAsState()

    val a11yMode     = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val esBlind      = a11yMode == AccessibilityMode.BLIND
    val esMute       = a11yMode == AccessibilityMode.MUTE
    val esAccesible  = esBlind || esMute
    val ttsManager   = if (esBlind) a11yVm.ttsManager else null

    fun loc(es: String, en: String) = if (idiomaActual == IdiomaVoz.INGLES) en else es

    val dataRef = rememberUpdatedState(data)
    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }
    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> data.name
            1 -> data.phone
            2 -> if (data.semanas == 0) "" else data.semanas.toString()
            3 -> data.email
            4 -> data.password
            5 -> confirmPassword
            else -> ""
        }
    }

    // ── Debounce por campo (modo ciego) ───────────────────────────────────────
    LaunchedEffect(data.name) {
        if (!esBlind || data.name.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (data.name == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.name.isNotBlank() && campoActivo == 0 && data.name != valorInicial) campoActivo = 1
    }
    LaunchedEffect(data.phone) {
        if (!esBlind || data.phone.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (data.phone == valorInicial) return@LaunchedEffect
        if (!tieneDiezDigitos(data.phone)) return@LaunchedEffect
        delay(600L)
        if (tieneDiezDigitos(data.phone) && campoActivo == 1 && data.phone != valorInicial) campoActivo = 2
    }
    LaunchedEffect(data.semanas) {
        if (!esBlind || data.semanas == 0 || campoActivo != 2) return@LaunchedEffect
        val semStr = data.semanas.toString()
        if (semStr == valorInicial) return@LaunchedEffect
        if (data.semanas !in 1..40) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.semanas in 1..40 && campoActivo == 2 && semStr != valorInicial) campoActivo = 3
    }
    LaunchedEffect(data.email) {
        if (!esBlind || data.email.isBlank() || campoActivo != 3) return@LaunchedEffect
        if (data.email == valorInicial) return@LaunchedEffect
        if (!data.email.contains("@") || !data.email.contains(".")) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.email.contains("@") && data.email.contains(".") && campoActivo == 3 && data.email != valorInicial) campoActivo = 4
    }
    LaunchedEffect(data.password) {
        if (!esBlind || data.password.length < 6 || campoActivo != 4) return@LaunchedEffect
        if (data.password == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.password.length >= 6 && campoActivo == 4 && data.password != valorInicial) campoActivo = 5
    }
    LaunchedEffect(confirmPassword) {
        if (!esBlind || confirmPassword.length < 6 || campoActivo != 5) return@LaunchedEffect
        if (confirmPassword == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (confirmPassword.length >= 6 && campoActivo == 5 && confirmPassword != valorInicial) campoActivo = 6
    }

    LaunchedEffect(campoActivo) {
        if (esBlind && campoActivo == 6) {
            ttsManager?.hablar(loc(
                "Todos los campos obligatorios han sido completados. El botón verde para Crear Cuenta y Continuar está ubicado en la parte inferior de la pantalla.",
                "All required fields have been completed. The green Create Account and Continue button is located at the bottom of the screen."
            ))
        }
    }

    LaunchedEffect(Unit) {
        if (esBlind) a11yVm.hablar(loc("Registro de mamá primeriza. Vamos a crear tu perfil prenatal.", "First-time mom registration. Let's create your prenatal profile."))
    }

    LaunchedEffect(estado) {
        when (val s = estado) {
            is RegisterUiState.Exito -> {
                if (esBlind) {
                    ttsManager?.hablarYEsperar(
                        loc("Cuenta creada exitosamente. Bienvenida a tu seguimiento prenatal.", "Account created successfully. Welcome to your prenatal tracking."),
                        margenMs = 500L
                    )
                }
                onRegisterSuccess(dataRef.value)
                viewModel.resetEstado()
            }
            is RegisterUiState.Error -> {
                firebaseError = s.mensaje
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(52.dp))
            RegisterScreenHeader(
                onBack    = onNavigateBack,
                icon      = Icons.Rounded.Favorite,
                iconColor = RegRosa,
                title     = loc("Registro Mamá Primeriza", "First-time Mom Registration"),
                subtitle  = loc("Tu acompañante en el embarazo", "Your companion during pregnancy")
            )
            Spacer(Modifier.height(28.dp))

            RegisterSectionTitle(loc("Datos personales", "Personal info"), Icons.Rounded.Person, RegRosa)
            Spacer(Modifier.height(12.dp))

            RegisterField(
                value           = data.name,
                onValueChange   = { data = data.copy(name = it); nameError = null },
                label           = loc("Nombre completo", "Full name"),
                icon            = Icons.Rounded.Person,
                error           = nameError,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                a11yActive      = esAccesible,
                activo          = campoActivo == 0,
                onFocus         = { campoActivo = 0 },
                onNext          = { campoActivo = 1 },
                ttsManager      = if (campoActivo == 0) ttsManager else null,
                idioma          = idiomaActual
            )

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 1) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.phone,
                        onValueChange   = { data = data.copy(phone = it); phoneError = null },
                        label           = loc("Teléfono", "Phone"),
                        icon            = Icons.Rounded.Phone,
                        error           = phoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 1,
                        onFocus         = { campoActivo = 1 },
                        onNext          = { campoActivo = 2 },
                        ttsManager      = if (campoActivo == 1) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 2) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(loc("Estado del embarazo", "Pregnancy status"), Icons.Rounded.CalendarMonth, RegRosa)
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = if(data.semanas == 0) "" else data.semanas.toString(),
                        onValueChange   = { data = data.copy(semanas = it.toIntOrNull() ?: 0); semanasError = null },
                        label           = loc("Semana de embarazo (1-40)", "Pregnancy week (1-40)"),
                        icon            = Icons.Rounded.Numbers,
                        error           = semanasError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 2,
                        onFocus         = { campoActivo = 2 },
                        onNext          = { campoActivo = 3 },
                        ttsManager      = if (campoActivo == 2) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 3) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(loc("Acceso a la cuenta", "Account access"), Icons.Rounded.Lock, RegRosa)
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.email,
                        onValueChange   = { data = data.copy(email = it.trim().lowercase()); emailError = null },
                        label           = loc("Correo electrónico", "Email address"),
                        icon            = Icons.Rounded.Email,
                        error           = emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 3,
                        onFocus         = { campoActivo = 3 },
                        onNext          = { campoActivo = 4 },
                        ttsManager      = if (campoActivo == 3) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 4) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = data.password,
                        onValueChange    = { data = data.copy(password = it); passwordError = null },
                        label            = loc("Contraseña", "Password"),
                        icon             = Icons.Rounded.Lock,
                        error            = passwordError,
                        isPassword       = true,
                        showPassword     = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 4,
                        onFocus          = { campoActivo = 4 },
                        onNext           = { campoActivo = 5 },
                        ttsManager       = if (campoActivo == 4) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 5) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = confirmPassword,
                        onValueChange    = { confirmPassword = it; confirmError = null },
                        label            = loc("Confirmar contraseña", "Confirm password"),
                        icon             = Icons.Rounded.LockOpen,
                        error            = confirmError,
                        isPassword       = true,
                        showPassword     = showConfirm,
                        onTogglePassword = { showConfirm = !showConfirm },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 5,
                        onFocus          = { campoActivo = 5 },
                        onNext           = { campoActivo = 6 },
                        ttsManager       = if (campoActivo == 5) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = {
                    var hasErrors = false
                    if (data.name.isBlank()) { nameError = loc("Nombre requerido", "Name required"); hasErrors = true }
                    if (data.phone.isBlank() || !tieneDiezDigitos(data.phone)) { phoneError = loc("Teléfono inválido", "Invalid phone"); hasErrors = true }
                    if (data.semanas !in 1..40) { semanasError = loc("Semana entre 1 y 40", "Week between 1 and 40"); hasErrors = true }
                    if (data.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(data.email).matches()) { emailError = loc("Email inválido", "Invalid email"); hasErrors = true }
                    if (data.password.length < 6) { passwordError = loc("Mínimo 6 caracteres", "Min 6 characters"); hasErrors = true }
                    if (confirmPassword != data.password) { confirmError = loc("No coinciden", "No match"); hasErrors = true }

                    if (!hasErrors) viewModel.registrarMamaPrimeriza(data, confirmPassword)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RegRosa)
            ) {
                if (estado is RegisterUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(loc("Crear cuenta", "Create account"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(36.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 4. REGISTRO NUTRIÓLOGO
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun NutritionistRegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateBack:    () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var data            by remember { mutableStateOf(NutritionistRegisterData()) }
    var showPassword    by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var nameError       by remember { mutableStateOf<String?>(null) }
    var emailError      by remember { mutableStateOf<String?>(null) }
    var passwordError   by remember { mutableStateOf<String?>(null) }
    var confirmError    by remember { mutableStateOf<String?>(null) }
    var phoneError      by remember { mutableStateOf<String?>(null) }
    var specialtyError  by remember { mutableStateOf<String?>(null) }
    var licenseError    by remember { mutableStateOf<String?>(null) }
    var firebaseError   by remember { mutableStateOf<String?>(null) }

    var verificadoCedulaState by remember { mutableStateOf<ResultadoCedula?>(null) }
    var buscandoCedula by remember { mutableStateOf(false) }
    var aceptoConsentimientoCedula by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(data.licenseId, aceptoConsentimientoCedula) {
        val digitos = data.licenseId.filter(Char::isDigit)
        if (digitos.length >= 6 && aceptoConsentimientoCedula) {
            delay(400L)
            buscandoCedula = true
            val res = CedulaVerifier.verificarCedulaConRateLimit(digitos)
            if (res.valida) {
                val repo = RepositorioLogin(context)
                if (repo.esCedulaRegistrada(digitos)) {
                    verificadoCedulaState = ResultadoCedula(
                        valida = false,
                        mensaje = "La cédula profesional $digitos ya pertenece a otro especialista registrado en NutrIA."
                    )
                } else {
                    verificadoCedulaState = res
                }
            } else {
                verificadoCedulaState = res
            }
            buscandoCedula = false
        } else {
            verificadoCedulaState = null
            buscandoCedula = false
        }
    }

    val estado by viewModel.estado.collectAsState()

    val a11yMode     = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val esBlind      = a11yMode == AccessibilityMode.BLIND
    val esMute       = a11yMode == AccessibilityMode.MUTE
    val esAccesible  = esBlind || esMute
    val ttsManager   = if (esBlind) a11yVm.ttsManager else null

    // ── Helper local de localización ──────────────────────────────────────────
    fun loc(es: String, en: String) = idiomaActual.loc(es, en)

    val voiceManager = remember { if (esBlind) VoiceInputManager(context) else null }

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }
    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> data.name
            1 -> data.phone
            2 -> data.specialty
            3 -> data.licenseId
            4 -> data.email
            5 -> data.password
            6 -> confirmPassword
            else -> ""
        }
    }

    LaunchedEffect(Unit) {
        if (esBlind) a11yVm.hablar(loc(Voz.REGISTRO_NUTRI_INTRO, VozEn.REGISTRO_NUTRI_INTRO))
    }

    LaunchedEffect(data.name) {
        if (!esBlind || data.name.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (data.name == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.name.isNotBlank() && campoActivo == 0 && data.name != valorInicial) campoActivo = 1
    }
    LaunchedEffect(data.phone) {
        if (!esBlind || data.phone.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (data.phone == valorInicial) return@LaunchedEffect
        if (!tieneDiezDigitos(data.phone)) return@LaunchedEffect
        delay(600L)
        if (tieneDiezDigitos(data.phone) && campoActivo == 1 && data.phone != valorInicial) campoActivo = 2
    }
    LaunchedEffect(data.specialty) {
        if (!esBlind || data.specialty.isBlank() || campoActivo != 2) return@LaunchedEffect
        if (data.specialty == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.specialty.isNotBlank() && campoActivo == 2 && data.specialty != valorInicial) campoActivo = 3
    }
    LaunchedEffect(data.licenseId) {
        if (!esBlind || data.licenseId.isBlank() || campoActivo != 3) return@LaunchedEffect
        if (data.licenseId == valorInicial) return@LaunchedEffect
        val soloDigitos = data.licenseId.filter(Char::isDigit)
        if (soloDigitos.length < 6) return@LaunchedEffect
        delay(600L)
        if (data.licenseId.isNotBlank() && campoActivo == 3 && data.licenseId != valorInicial) {
            if (!aceptoConsentimientoCedula) {
                // Anunciar que hay que aceptar la casilla de consentimiento
                ttsManager?.hablarYEsperar(
                    loc(
                        "Cédula capturada. Ahora debes aceptar que NutriIA consulte tu cédula ante la SEP. Di acepto para marcar la casilla y continuar.",
                        "License captured. You must now accept that NutriIA checks your license with SEP. Say accept to check the box and continue."
                    ),
                    margenMs = 300L
                )
                // Escuchar comando 'acepto'
                voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                    if (!isFinal) return@escuchar
                    val cmd = result.lowercase(java.util.Locale.getDefault()).trim()
                    if (cmd.contains("acepto") || cmd.contains("autorizo") || cmd.contains("sí") || cmd.contains("accept")) {
                        aceptoConsentimientoCedula = true
                        a11yVm.hablar(loc("Consentimiento aceptado. Verificando cédula ante la SEP.", "Consent accepted. Verifying license with SEP."))
                    }
                }
            } else {
                campoActivo = 4
            }
        }
    }
    // Cuando el consentimiento se acepta, avanzar al siguiente campo
    LaunchedEffect(aceptoConsentimientoCedula) {
        if (!esBlind || !aceptoConsentimientoCedula || campoActivo != 3) return@LaunchedEffect
        val soloDigitos = data.licenseId.filter(Char::isDigit)
        if (soloDigitos.length < 6) return@LaunchedEffect
        delay(500L)
        campoActivo = 4
    }

    // ── Comando de voz "registrarme" cuando el formulario está completo ────
    // (La narración se hace dentro del LaunchedEffect de escucha para evitar
    //  conflicto entre TTS y micrófono)
    LaunchedEffect(data.email) {
        if (!esBlind || data.email.isBlank() || campoActivo != 4) return@LaunchedEffect
        if (data.email == valorInicial) return@LaunchedEffect
        if (!data.email.contains("@") || !data.email.contains(".")) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.email.contains("@") && data.email.contains(".") && campoActivo == 4 && data.email != valorInicial) campoActivo = 5
    }
    LaunchedEffect(data.password) {
        if (!esBlind || data.password.length < 6 || campoActivo != 5) return@LaunchedEffect
        if (data.password == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.password.length >= 6 && campoActivo == 5 && data.password != valorInicial) campoActivo = 6
    }
    LaunchedEffect(confirmPassword) {
        if (!esBlind || confirmPassword.length < 6 || campoActivo != 6) return@LaunchedEffect
        if (confirmPassword == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (confirmPassword.length >= 6 && campoActivo == 6 && confirmPassword != valorInicial) campoActivo = 7
    }

    LaunchedEffect(estado) {
        when (val s = estado) {
            is RegisterUiState.Exito -> {
                if (esBlind) {
                    ttsManager?.hablarYEsperar(
                        loc(
                            "Perfil profesional creado exitosamente. Bienvenido a NutriIA.",
                            "Professional profile created successfully. Welcome to NutriIA."
                        ),
                        margenMs = 500L
                    )
                }
                onRegisterSuccess()
                viewModel.resetEstado()
            }
            is RegisterUiState.Error -> {
                firebaseError = s.mensaje
                if (esBlind) a11yVm.hablar(
                    loc(
                        "Error al registrar. ${s.mensaje}. Intenta de nuevo.",
                        "Registration error. ${s.mensaje}. Please try again."
                    )
                )
            }
            else -> { /* Idle / Loading */ }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(52.dp))
            RegisterScreenHeader(
                onBack    = {
                    if (esBlind) a11yVm.hablar(loc("Regresando.", "Going back."))
                    onNavigateBack()
                },
                icon      = Icons.Rounded.MedicalServices,
                iconColor = RegTeal,
                title     = loc("Registro de Nutriólogo/a", "Nutritionist Registration"),
                subtitle  = loc("Crea tu perfil profesional", "Create your professional profile")
            )
            Spacer(Modifier.height(28.dp))

            RegisterSectionTitle(
                loc("Datos personales", "Personal info"),
                Icons.Rounded.Person,
                RegGreen
            )
            Spacer(Modifier.height(12.dp))

            val nombreNoCoincideNutri = remember(data.name, verificadoCedulaState) {
                verificadoCedulaState?.valida == true &&
                verificadoCedulaState?.nombreTitular?.isNotBlank() == true &&
                data.name.isNotBlank() &&
                !coincideNombreConTitular(data.name, verificadoCedulaState?.nombreTitular ?: "")
            }

            // ── Campo 1: Nombre ───────────────────────────────────────────────
            RegisterField(
                value           = data.name,
                onValueChange   = { data = data.copy(name = it); nameError = null },
                label           = loc("Nombre completo", "Full name"),
                icon            = Icons.Rounded.Person,
                error           = nameError ?: if (nombreNoCoincideNutri) loc("Error, datos no coinciden, inténtelo de nuevo", "Error, data does not match, please try again") else null,
                readOnly        = nombreNoCoincideNutri,
                a11yLabel       = loc(
                    "Campo 1 de 7. Nombre completo. Di tu nombre y apellidos.",
                    "Field 1 of 7. Full name. Say your first and last name."
                ),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                a11yActive      = esAccesible,
                activo          = campoActivo == 0,
                onFocus         = { campoActivo = 0 },
                onNext          = { campoActivo = 1 },
                ttsManager      = if (campoActivo == 0) ttsManager else null,
                idioma          = idiomaActual
            )

            // ── Campo 2: Teléfono ─────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 1) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.phone,
                        onValueChange   = { data = data.copy(phone = it); phoneError = null },
                        label           = loc("Teléfono", "Phone"),
                        icon            = Icons.Rounded.Phone,
                        error           = phoneError,
                        a11yLabel       = loc(
                            "Campo 2 de 7. Teléfono. Di los diez dígitos de tu número.",
                            "Field 2 of 7. Phone. Say your ten-digit phone number."
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 1,
                        onFocus         = { campoActivo = 1 },
                        onNext          = { campoActivo = 2 },
                        ttsManager      = if (campoActivo == 1) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            // ── Campo 3: Especialidad ─────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 2) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(
                        loc("Datos profesionales", "Professional info"),
                        Icons.Rounded.VerifiedUser,
                        RegTeal
                    )
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.specialty,
                        onValueChange   = { data = data.copy(specialty = it); specialtyError = null },
                        label           = loc("Especialidad", "Specialty"),
                        icon            = Icons.Rounded.LocalHospital,
                        error           = specialtyError,
                        placeholder     = loc("Ej. Nutrición Pediátrica", "E.g. Pediatric Nutrition"),
                        a11yLabel       = loc(
                            "Campo 3 de 7. Especialidad. Por ejemplo Nutrición Pediátrica.",
                            "Field 3 of 7. Specialty. For example Pediatric Nutrition."
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 2,
                        onFocus         = { campoActivo = 2 },
                        onNext          = { campoActivo = 3 },
                        ttsManager      = if (campoActivo == 2) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            // ── Campo 4: Cédula ───────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 3) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value         = data.licenseId,
                        onValueChange = { raw ->
                            // En modo accesible siempre extraer solo dígitos del dictado
                            val normalizado = if (esAccesible) normalizarCedulaVoz(raw) else raw
                            data = data.copy(licenseId = normalizado)
                            licenseError = null
                        },
                        label         = loc("Cédula profesional", "Professional license"),
                        icon          = Icons.Rounded.Badge,
                        error         = licenseError ?: if (verificadoCedulaState?.valida == false) verificadoCedulaState?.mensaje else null,
                        placeholder   = "Ej. 12345678",
                        a11yLabel     = loc(
                            "Campo 4 de 7. Cédula profesional. Di tu número de cédula.",
                            "Field 4 of 7. Professional license. Say your license number."
                        ),
                        a11yActive    = esAccesible,
                        activo        = campoActivo == 3,
                        onFocus       = { campoActivo = 3 },
                        onNext        = { campoActivo = 4 },
                        ttsManager    = if (campoActivo == 3) ttsManager else null,
                        idioma        = idiomaActual
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, if (licenseError != null && !aceptoConsentimientoCedula) Color.Red else Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                            .clickable { aceptoConsentimientoCedula = !aceptoConsentimientoCedula }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = aceptoConsentimientoCedula,
                            onCheckedChange = { aceptoConsentimientoCedula = it },
                            colors = CheckboxDefaults.colors(checkedColor = RegTeal)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = loc(
                                "Acepto que NutriIA consulte mi cédula profesional en el Registro Nacional de Profesionistas (SEP) para verificar mi identidad profesional.",
                                "I accept that NutriIA checks my professional license in the National Registry of Professionals (SEP) to verify my professional identity."
                            ),
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            lineHeight = 15.sp
                        )
                    }

                    if (buscandoCedula) {
                        Row(modifier = Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = RegTeal)
                            Spacer(Modifier.width(6.dp))
                            Text("Verificando cédula ante la SEP...", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else if (verificadoCedulaState != null) {
                        TarjetaResumenCedula(verificadoCedulaState!!, nombreUsuario = data.name, esGinecologo = false)
                    }
                }
            }

            // ── Campo 5: Correo ───────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 4) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(
                        loc("Acceso a la cuenta", "Account access"),
                        Icons.Rounded.Lock,
                        RegPurple
                    )
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value         = data.email,
                        onValueChange = { raw ->
                            val norm = if (esCorreoDictado(raw)) normalizarCorreoVoz(raw)
                            else raw.trim().lowercase()
                            data = data.copy(email = norm)
                            emailError = null
                        },
                        label           = loc("Correo electrónico", "Email address"),
                        icon            = Icons.Rounded.Email,
                        error           = emailError,
                        a11yLabel       = loc(
                            "Campo 5 de 7. Correo electrónico. Di tu correo completo. Por ejemplo maria arroba hotmail punto com.",
                            "Field 5 of 7. Email address. Say your full email. For example maria at hotmail dot com."
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 4,
                        onFocus         = { campoActivo = 4 },
                        onNext          = { campoActivo = 5 },
                        ttsManager      = if (campoActivo == 4) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            // ── Campo 6: Clave ────────────────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 5) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = data.password,
                        onValueChange    = { data = data.copy(password = it); passwordError = null },
                        label            = loc("Clave de acceso", "Password"),
                        icon             = Icons.Rounded.Lock,
                        error            = passwordError,
                        a11yLabel        = loc(
                            "Campo 6 de 7. Clave de acceso. Di tu clave, mínimo seis caracteres.",
                            "Field 6 of 7. Password. Say your password, at least six characters."
                        ),
                        isPassword       = true,
                        showPassword     = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 5,
                        onFocus          = { campoActivo = 5 },
                        onNext           = { campoActivo = 6 },
                        ttsManager       = if (campoActivo == 5) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            // ── Campo 7: Confirmar clave ──────────────────────────────────────
            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 6) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = confirmPassword,
                        onValueChange    = { confirmPassword = it; confirmError = null },
                        label            = loc("Confirmar clave", "Confirm password"),
                        icon             = Icons.Rounded.LockOpen,
                        error            = confirmError,
                        a11yLabel        = loc(
                            "Campo 7 de 7. Confirmar clave. Repite exactamente la misma clave.",
                            "Field 7 of 7. Confirm password. Repeat exactly the same password."
                        ),
                        isPassword       = true,
                        showPassword     = showConfirm,
                        onTogglePassword = { showConfirm = !showConfirm },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 6,
                        onFocus          = { campoActivo = 6 },
                        onNext           = { campoActivo = 7 },
                        ttsManager       = if (campoActivo == 6) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            // ── Comando de voz "registrarme" (solo modo BLIND) ────────────────
            // Lambda compartida para ejecutar el registro (usada por botón y voz)
            val profesionInvalidaNutri = remember(verificadoCedulaState) {
                verificadoCedulaState?.valida == true &&
                verificadoCedulaState?.profesion?.isNotBlank() == true &&
                !esProfesionValidaNutriologo(verificadoCedulaState?.profesion ?: "")
            }

            val ejecutarRegistroNutri: () -> Unit = {
                var hasErrors = false
                if (data.name.isBlank()) { nameError = loc("El nombre es requerido", "Name is required"); hasErrors = true }
                if (data.phone.isBlank()) { phoneError = loc("El teléfono es requerido", "Phone is required"); hasErrors = true }
                else if (!tieneDiezDigitos(data.phone)) { phoneError = loc("Debe tener al menos 10 dígitos", "Must have at least 10 digits"); hasErrors = true }
                if (data.specialty.isBlank()) { specialtyError = loc("La especialidad es requerida", "Specialty is required"); hasErrors = true }
                val digitosCed = data.licenseId.filter(Char::isDigit)
                if (!aceptoConsentimientoCedula) { licenseError = loc("Debes autorizar la consulta de cédula profesional conforme a la LFPDPPP", "You must authorize professional license verification"); hasErrors = true }
                else if (data.licenseId.isBlank()) { licenseError = loc("La cédula profesional es requerida", "Professional license is required"); hasErrors = true }
                else if (digitosCed.length < 6) { licenseError = loc("La cédula debe contener al menos 6 dígitos", "License must have at least 6 digits"); hasErrors = true }
                else if (verificadoCedulaState == null) { licenseError = loc("Verificando cédula ante la SEP, por favor espera...", "Verifying license, please wait..."); hasErrors = true }
                else if (!verificadoCedulaState!!.valida) { licenseError = verificadoCedulaState!!.mensaje.ifBlank { loc("Cédula no válida ante la SEP", "Invalid license number") }; hasErrors = true }
                else if (nombreNoCoincideNutri) { licenseError = loc("Error, datos no coinciden, inténtelo de nuevo", "Error, data does not match, please try again"); hasErrors = true }
                else if (profesionInvalidaNutri) { licenseError = loc("Error, la cédula no corresponde a la especialidad requerida", "Error, license does not match required specialty"); hasErrors = true }
                if (data.email.isBlank()) { emailError = loc("El correo es requerido", "Email is required"); hasErrors = true }
                else if (!Patterns.EMAIL_ADDRESS.matcher(data.email).matches()) { emailError = loc("Correo inválido", "Invalid email"); hasErrors = true }
                if (data.password.length < 6) { passwordError = loc("Mínimo 6 caracteres", "Minimum 6 characters"); hasErrors = true }
                if (confirmPassword != data.password) { confirmError = loc("Las claves no coinciden", "Passwords do not match"); hasErrors = true }
                if (hasErrors) {
                    if (esBlind) a11yVm.hablar(loc(Voz.REGISTRO_ERROR_CAMPOS, VozEn.REGISTRO_ERROR_CAMPOS))
                } else {
                    if (esBlind) a11yVm.hablar(loc("Creando tu perfil. Por favor espera.", "Creating your profile. Please wait."))
                    viewModel.registrarNutriologo(
                        data                 = data,
                        confirmarPassword    = confirmPassword,
                        consentimientoCedula = aceptoConsentimientoCedula,
                        nombreTitularCedula  = verificadoCedulaState?.nombreTitular ?: "",
                        profesionCedula      = verificadoCedulaState?.profesion ?: ""
                    )
                }
            }
            if (esBlind) {
                var escuchandoRegistro by remember { mutableStateOf(false) }
                LaunchedEffect(campoActivo) {
                    if (campoActivo < 7) return@LaunchedEffect
                    // Esperar que termine el TTS antes de abrir el mic
                    ttsManager?.hablarYEsperar(
                        loc(
                            "Formulario completo. Di registrarme para crear tu cuenta.",
                            "Form complete. Say register me to create your account."
                        ),
                        margenMs = 800L
                    )
                    escuchandoRegistro = true
                    voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                        if (!isFinal) return@escuchar
                        escuchandoRegistro = false
                        val cmd = result.lowercase(java.util.Locale.getDefault()).trim()
                        if (cmd.contains("registrar") || cmd.contains("registrarme") ||
                            cmd.contains("crear") || cmd.contains("crear cuenta") ||
                            cmd.contains("enviar") || cmd.contains("finalizar")) {
                            ejecutarRegistroNutri()
                        } else {
                            // Volver a escuchar si no reconoció el comando
                            escuchandoRegistro = true
                            a11yVm.hablar(loc("No entendí. Di registrarme para crear tu cuenta.", "I didn't understand. Say register me to create your account."))
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 7) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(16.dp))
                        if (escuchandoRegistro) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RegTeal)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    loc("Escuchando... di \"registrarme\"", "Listening... say \"register me\""),
                                    fontSize = 13.sp, color = RegTeal, fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                loc("🎤 Di \"registrarme\" para completar el registro", "🎤 Say \"register me\" to complete registration"),
                                fontSize = 13.sp, color = RegTeal, fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // ── Botón crear perfil ────────────────────────────────────────────
            Spacer(Modifier.height(36.dp))
            Button(
                onClick = { ejecutarRegistroNutri() },
                enabled  = estado !is RegisterUiState.Loading && aceptoConsentimientoCedula && !nombreNoCoincideNutri && !profesionInvalidaNutri,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (esBlind) 70.dp else 56.dp)
                    .semantics {
                        contentDescription = loc(
                            "Botón Crear perfil profesional. Parte inferior.",
                            "Create professional profile button. Bottom of screen."
                        )
                    },
                shape  = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor         = RegTeal,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                if (estado is RegisterUiState.Loading) {
                    CircularProgressIndicator(
                        color       = Color.White,
                        modifier    = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        loc("Crear perfil profesional", "Create professional profile"),
                        fontWeight = FontWeight.Bold,
                        fontSize   = 16.sp
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(36.dp))
        }

        firebaseError?.let { msg ->
            AlertDialog(
                onDismissRequest = { firebaseError = null; viewModel.resetEstado() },
                title = {
                    Text(
                        loc("Error al registrar", "Registration error"),
                        fontWeight = FontWeight.Bold
                    )
                },
                text  = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { firebaseError = null; viewModel.resetEstado() }) {
                        Text(
                            loc("Entendido", "Got it"),
                            color      = RegTeal,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// 5. REGISTRO GINECÓLOGO (NUEVO)
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun GinecologistRegisterScreen(
    viewModel: RegisterViewModel = viewModel(),
    onNavigateBack:    () -> Unit,
    onRegisterSuccess: () -> Unit
) {
    var data            by remember { mutableStateOf(GynecologistRegisterData(specialty = "Ginecología y Obstetricia")) }
    var showPassword    by remember { mutableStateOf(false) }
    var showConfirm     by remember { mutableStateOf(false) }
    var confirmPassword by remember { mutableStateOf("") }
    var nameError       by remember { mutableStateOf<String?>(null) }
    var emailError      by remember { mutableStateOf<String?>(null) }
    var passwordError   by remember { mutableStateOf<String?>(null) }
    var confirmError    by remember { mutableStateOf<String?>(null) }
    var phoneError      by remember { mutableStateOf<String?>(null) }
    var specialtyError  by remember { mutableStateOf<String?>(null) }
    var licenseError    by remember { mutableStateOf<String?>(null) }
    var firebaseError   by remember { mutableStateOf<String?>(null) }

    var verificadoCedulaGineState by remember { mutableStateOf<ResultadoCedula?>(null) }
    var buscandoCedulaGine by remember { mutableStateOf(false) }
    var aceptoConsentimientoCedulaGine by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(data.licenseId, aceptoConsentimientoCedulaGine) {
        val digitos = data.licenseId.filter(Char::isDigit)
        if (digitos.length >= 6 && aceptoConsentimientoCedulaGine) {
            delay(400L)
            buscandoCedulaGine = true
            val res = CedulaVerifier.verificarCedula(digitos)
            if (res.valida) {
                val repo = RepositorioLogin(context)
                if (repo.esCedulaRegistrada(digitos)) {
                    verificadoCedulaGineState = ResultadoCedula(
                        valida = false,
                        mensaje = "La cédula profesional $digitos ya pertenece a otro especialista registrado en NutrIA."
                    )
                } else {
                    verificadoCedulaGineState = res
                }
            } else {
                verificadoCedulaGineState = res
            }
            buscandoCedulaGine = false
        } else {
            verificadoCedulaGineState = null
            buscandoCedulaGine = false
        }
    }

    val estado by viewModel.estado.collectAsState()

    val a11yMode     = LocalAccessibilityMode.current
    val a11yVm: AccessibilityViewModel = viewModel()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val esBlind      = a11yMode == AccessibilityMode.BLIND
    val esMute       = a11yMode == AccessibilityMode.MUTE
    val esAccesible  = esBlind || esMute
    val ttsManager   = if (esBlind) a11yVm.ttsManager else null

    fun loc(es: String, en: String) = idiomaActual.loc(es, en)

    val voiceManager = remember { if (esBlind) VoiceInputManager(context) else null }

    var campoActivo by remember { mutableIntStateOf(0) }
    var valorInicial by remember { mutableStateOf("") }
    LaunchedEffect(campoActivo) {
        valorInicial = when (campoActivo) {
            0 -> data.name
            1 -> data.phone
            2 -> data.specialty
            3 -> data.licenseId
            4 -> data.email
            5 -> data.password
            6 -> confirmPassword
            else -> ""
        }
    }

    // ── Debounce por campo (modo ciego) ───────────────────────────────────────
    LaunchedEffect(data.name) {
        if (!esBlind || data.name.isBlank() || campoActivo != 0) return@LaunchedEffect
        if (data.name == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.name.isNotBlank() && campoActivo == 0 && data.name != valorInicial) campoActivo = 1
    }
    LaunchedEffect(data.phone) {
        if (!esBlind || data.phone.isBlank() || campoActivo != 1) return@LaunchedEffect
        if (data.phone == valorInicial) return@LaunchedEffect
        if (!tieneDiezDigitos(data.phone)) return@LaunchedEffect
        delay(600L)
        if (tieneDiezDigitos(data.phone) && campoActivo == 1 && data.phone != valorInicial) campoActivo = 2
    }
    LaunchedEffect(data.specialty) {
        if (!esBlind || data.specialty.isBlank() || campoActivo != 2) return@LaunchedEffect
        if (data.specialty == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.specialty.isNotBlank() && campoActivo == 2 && data.specialty != valorInicial) campoActivo = 3
    }
    LaunchedEffect(data.licenseId) {
        if (!esBlind || data.licenseId.isBlank() || campoActivo != 3) return@LaunchedEffect
        if (data.licenseId == valorInicial) return@LaunchedEffect
        val soloDigitos = data.licenseId.filter(Char::isDigit)
        if (soloDigitos.length < 6) return@LaunchedEffect
        delay(600L)
        if (data.licenseId.isNotBlank() && campoActivo == 3 && data.licenseId != valorInicial) {
            if (!aceptoConsentimientoCedulaGine) {
                ttsManager?.hablarYEsperar(
                    loc(
                        "Cédula capturada. Ahora debes aceptar que NutriIA consulte tu cédula ante la SEP. Di acepto para marcar la casilla y continuar.",
                        "License captured. You must now accept that NutriIA checks your license with SEP. Say accept to check the box and continue."
                    ),
                    margenMs = 300L
                )
                voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                    if (!isFinal) return@escuchar
                    val cmd = result.lowercase(java.util.Locale.getDefault()).trim()
                    if (cmd.contains("acepto") || cmd.contains("autorizo") || cmd.contains("sí") || cmd.contains("accept")) {
                        aceptoConsentimientoCedulaGine = true
                        a11yVm.hablar(loc("Consentimiento aceptado. Verificando cédula ante la SEP.", "Consent accepted. Verifying license with SEP."))
                    }
                }
            } else {
                campoActivo = 4
            }
        }
    }
    LaunchedEffect(aceptoConsentimientoCedulaGine) {
        if (!esBlind || !aceptoConsentimientoCedulaGine || campoActivo != 3) return@LaunchedEffect
        val soloDigitos = data.licenseId.filter(Char::isDigit)
        if (soloDigitos.length < 6) return@LaunchedEffect
        delay(500L)
        campoActivo = 4
    }
    // campoActivo == 7: la narración se maneja dentro del bloque de voz 'registrarme'
    LaunchedEffect(data.email) {
        if (!esBlind || data.email.isBlank() || campoActivo != 4) return@LaunchedEffect
        if (data.email == valorInicial) return@LaunchedEffect
        if (!data.email.contains("@") || !data.email.contains(".")) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.email.contains("@") && data.email.contains(".") && campoActivo == 4 && data.email != valorInicial) campoActivo = 5
    }
    LaunchedEffect(data.password) {
        if (!esBlind || data.password.length < 6 || campoActivo != 5) return@LaunchedEffect
        if (data.password == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (data.password.length >= 6 && campoActivo == 5 && data.password != valorInicial) campoActivo = 6
    }
    LaunchedEffect(confirmPassword) {
        if (!esBlind || confirmPassword.length < 6 || campoActivo != 6) return@LaunchedEffect
        if (confirmPassword == valorInicial) return@LaunchedEffect
        delay(DEBOUNCE_MS)
        if (confirmPassword.length >= 6 && campoActivo == 6 && confirmPassword != valorInicial) campoActivo = 7
    }

    LaunchedEffect(Unit) {
        if (esBlind) a11yVm.hablar(loc("Registro de Ginecólogo/a. Crea tu perfil médico.", "Gynecologist registration. Create your medical profile."))
    }

    LaunchedEffect(estado) {
        when (val s = estado) {
            is RegisterUiState.Exito -> {
                if (esBlind) {
                    ttsManager?.hablarYEsperar(
                        loc("Perfil médico creado exitosamente. Bienvenido/a.", "Medical profile created successfully. Welcome."),
                        margenMs = 500L
                    )
                }
                onRegisterSuccess()
                viewModel.resetEstado()
            }
            is RegisterUiState.Error -> {
                firebaseError = s.mensaje
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(52.dp))
            RegisterScreenHeader(
                onBack    = onNavigateBack,
                icon      = Icons.Rounded.Female,
                iconColor = RegRosaGine,
                title     = loc("Registro de Ginecólogo/a", "Gynecologist Registration"),
                subtitle  = loc("Crea tu perfil profesional", "Create your professional profile")
            )
            Spacer(Modifier.height(28.dp))

            RegisterSectionTitle(loc("Datos personales", "Personal info"), Icons.Rounded.Person, RegRosaGine)
            Spacer(Modifier.height(12.dp))

            val nombreNoCoincideGine = remember(data.name, verificadoCedulaGineState) {
                verificadoCedulaGineState?.valida == true &&
                verificadoCedulaGineState?.nombreTitular?.isNotBlank() == true &&
                data.name.isNotBlank() &&
                !coincideNombreConTitular(data.name, verificadoCedulaGineState?.nombreTitular ?: "")
            }

            RegisterField(
                value           = data.name,
                onValueChange   = { data = data.copy(name = it); nameError = null },
                label           = loc("Nombre completo", "Full name"),
                icon            = Icons.Rounded.Person,
                error           = nameError ?: if (nombreNoCoincideGine) loc("Error, datos no coinciden, inténtelo de nuevo", "Error, data does not match, please try again") else null,
                readOnly        = nombreNoCoincideGine,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                a11yActive      = esAccesible,
                activo          = campoActivo == 0,
                onFocus         = { campoActivo = 0 },
                onNext          = { campoActivo = 1 },
                ttsManager      = if (campoActivo == 0) ttsManager else null,
                idioma          = idiomaActual
            )

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 1) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.phone,
                        onValueChange   = { data = data.copy(phone = it); phoneError = null },
                        label           = loc("Teléfono", "Phone"),
                        icon            = Icons.Rounded.Phone,
                        error           = phoneError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 1,
                        onFocus         = { campoActivo = 1 },
                        onNext          = { campoActivo = 2 },
                        ttsManager      = if (campoActivo == 1) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 2) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(loc("Datos profesionales", "Professional info"), Icons.Rounded.VerifiedUser, RegRosaGine)
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.specialty,
                        onValueChange   = { data = data.copy(specialty = it); specialtyError = null },
                        label           = loc("Especialidad", "Specialty"),
                        icon            = Icons.Rounded.LocalHospital,
                        error           = specialtyError,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 2,
                        onFocus         = { campoActivo = 2 },
                        onNext          = { campoActivo = 3 },
                        ttsManager      = if (campoActivo == 2) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 3) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.licenseId,
                        onValueChange   = { raw ->
                            // En modo accesible siempre extraer solo dígitos del dictado
                            val normalizado = if (esAccesible) normalizarCedulaVoz(raw) else raw
                            data = data.copy(licenseId = normalizado)
                            licenseError = null
                        },
                        label           = loc("Cédula profesional", "Professional license"),
                        icon            = Icons.Rounded.Badge,
                        error           = licenseError ?: if (verificadoCedulaGineState?.valida == false) verificadoCedulaGineState?.mensaje else null,
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 3,
                        onFocus         = { campoActivo = 3 },
                        onNext          = { campoActivo = 4 },
                        ttsManager      = if (campoActivo == 3) ttsManager else null,
                        idioma          = idiomaActual
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                            .border(1.dp, if (licenseError != null && !aceptoConsentimientoCedulaGine) Color.Red else Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                            .clickable { aceptoConsentimientoCedulaGine = !aceptoConsentimientoCedulaGine }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = aceptoConsentimientoCedulaGine,
                            onCheckedChange = { aceptoConsentimientoCedulaGine = it },
                            colors = CheckboxDefaults.colors(checkedColor = RegRosaGine)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = loc(
                                "Acepto que NutriIA consulte mi cédula profesional en el Registro Nacional de Profesionistas (SEP) para verificar mi identidad profesional.",
                                "I accept that NutriIA checks my professional license in the National Registry of Professionals (SEP) to verify my professional identity."
                            ),
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            lineHeight = 15.sp
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    if (buscandoCedulaGine) {
                        Row(modifier = Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = RegRosaGine)
                            Spacer(Modifier.width(6.dp))
                            Text("Verificando cédula ante la SEP...", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else if (verificadoCedulaGineState != null) {
                        TarjetaResumenCedula(verificadoCedulaGineState!!, nombreUsuario = data.name, esGinecologo = true)
                    }
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 4) {
                Column {
                    Spacer(Modifier.height(24.dp))
                    RegisterSectionTitle(loc("Acceso a la cuenta", "Account access"), Icons.Rounded.Lock, RegPurple)
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value           = data.email,
                        onValueChange   = { data = data.copy(email = it.trim().lowercase()); emailError = null },
                        label           = loc("Correo electrónico", "Email address"),
                        icon            = Icons.Rounded.Email,
                        error           = emailError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        a11yActive      = esAccesible,
                        activo          = campoActivo == 4,
                        onFocus         = { campoActivo = 4 },
                        onNext          = { campoActivo = 5 },
                        ttsManager      = if (campoActivo == 4) ttsManager else null,
                        idioma          = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 5) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = data.password,
                        onValueChange    = { data = data.copy(password = it); passwordError = null },
                        label            = loc("Clave de acceso", "Password"),
                        icon             = Icons.Rounded.Lock,
                        error            = passwordError,
                        isPassword       = true,
                        showPassword     = showPassword,
                        onTogglePassword = { showPassword = !showPassword },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 5,
                        onFocus          = { campoActivo = 5 },
                        onNext           = { campoActivo = 6 },
                        ttsManager       = if (campoActivo == 5) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            androidx.compose.animation.AnimatedVisibility(visible = !esBlind || campoActivo >= 6) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    RegisterField(
                        value            = confirmPassword,
                        onValueChange    = { confirmPassword = it; confirmError = null },
                        label            = loc("Confirmar clave", "Confirm password"),
                        icon             = Icons.Rounded.LockOpen,
                        error            = confirmError,
                        isPassword       = true,
                        showPassword     = showConfirm,
                        onTogglePassword = { showConfirm = !showConfirm },
                        a11yActive       = esAccesible,
                        activo           = campoActivo == 6,
                        onFocus          = { campoActivo = 6 },
                        onNext          = { campoActivo = 7 },
                        ttsManager       = if (campoActivo == 6) ttsManager else null,
                        idioma           = idiomaActual
                    )
                }
            }

            // ── Comando de voz "registrarme" (solo modo BLIND) ────────────────
            val profesionInvalidaGine = remember(verificadoCedulaGineState) {
                verificadoCedulaGineState?.valida == true &&
                verificadoCedulaGineState?.profesion?.isNotBlank() == true &&
                !esProfesionValidaGinecologo(verificadoCedulaGineState?.profesion ?: "")
            }

            val ejecutarRegistroGine: () -> Unit = {
                var hasErrors = false
                if (data.name.isBlank()) { nameError = loc("Nombre requerido", "Name required"); hasErrors = true }
                if (data.phone.isBlank() || !tieneDiezDigitos(data.phone)) { phoneError = loc("Teléfono inválido", "Invalid phone"); hasErrors = true }
                if (data.specialty.isBlank()) { specialtyError = loc("Especialidad requerida", "Specialty required"); hasErrors = true }
                val digitosGine = data.licenseId.filter(Char::isDigit)
                if (!aceptoConsentimientoCedulaGine) { licenseError = loc("Debes autorizar la consulta de cédula profesional conforme a la LFPDPPP", "You must authorize professional license verification"); hasErrors = true }
                else if (data.licenseId.isBlank()) { licenseError = loc("Cédula requerida", "License required"); hasErrors = true }
                else if (digitosGine.length < 6) { licenseError = loc("Mínimo 6 dígitos", "Min 6 digits"); hasErrors = true }
                else if (verificadoCedulaGineState == null) { licenseError = loc("Verificando cédula ante la SEP...", "Verifying license..."); hasErrors = true }
                else if (!verificadoCedulaGineState!!.valida) { licenseError = verificadoCedulaGineState!!.mensaje.ifBlank { loc("Cédula no válida ante la SEP", "Invalid license number") }; hasErrors = true }
                else if (nombreNoCoincideGine) { licenseError = loc("Error, datos no coinciden, inténtelo de nuevo", "Error, data does not match, please try again"); hasErrors = true }
                else if (profesionInvalidaGine) { licenseError = loc("Error, la cédula no corresponde a la especialidad requerida", "Error, license does not match required specialty"); hasErrors = true }
                if (data.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(data.email).matches()) { emailError = loc("Email inválido", "Invalid email"); hasErrors = true }
                if (data.password.length < 6) { passwordError = loc("Mínimo 6 caracteres", "Min 6 characters"); hasErrors = true }
                if (confirmPassword != data.password) { confirmError = loc("No coinciden", "No match"); hasErrors = true }
                if (hasErrors) {
                    if (esBlind) a11yVm.hablar(loc(Voz.REGISTRO_ERROR_CAMPOS, VozEn.REGISTRO_ERROR_CAMPOS))
                } else {
                    if (esBlind) a11yVm.hablar(loc("Creando tu perfil médico. Por favor espera.", "Creating your medical profile. Please wait."))
                    viewModel.registrarGinecologo(data, confirmPassword)
                }
            }
            if (esBlind) {
                var escuchandoRegistroGine by remember { mutableStateOf(false) }
                LaunchedEffect(campoActivo) {
                    if (campoActivo < 7) return@LaunchedEffect
                    // Esperar que termine el TTS antes de abrir el mic
                    ttsManager?.hablarYEsperar(
                        loc(
                            "Formulario completo. Di registrarme para crear tu cuenta.",
                            "Form complete. Say register me to create your account."
                        ),
                        margenMs = 800L
                    )
                    escuchandoRegistroGine = true
                    voiceManager?.escuchar(idiomaActual, true) { result, isFinal ->
                        if (!isFinal) return@escuchar
                        escuchandoRegistroGine = false
                        val cmd = result.lowercase(java.util.Locale.getDefault()).trim()
                        if (cmd.contains("registrar") || cmd.contains("registrarme") ||
                            cmd.contains("crear") || cmd.contains("crear cuenta") ||
                            cmd.contains("enviar") || cmd.contains("finalizar")) {
                            ejecutarRegistroGine()
                        } else {
                            escuchandoRegistroGine = true
                            a11yVm.hablar(loc("No entendí. Di registrarme para crear tu cuenta.", "I didn't understand. Say register me to create your account."))
                        }
                    }
                }
                androidx.compose.animation.AnimatedVisibility(visible = campoActivo >= 7) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(16.dp))
                        if (escuchandoRegistroGine) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = RegRosaGine)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    loc("Escuchando... di \"registrarme\"", "Listening... say \"register me\""),
                                    fontSize = 13.sp, color = RegRosaGine, fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                loc("🎤 Di \"registrarme\" para completar el registro", "🎤 Say \"register me\" to complete registration"),
                                fontSize = 13.sp, color = RegRosaGine, fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(36.dp))

            Button(
                onClick = { ejecutarRegistroGine() },
                enabled  = estado !is RegisterUiState.Loading && aceptoConsentimientoCedulaGine && !nombreNoCoincideGine && !profesionInvalidaGine,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = RegRosaGine,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                if (estado is RegisterUiState.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text(loc("Crear perfil profesional", "Create professional profile"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Spacer(Modifier.height(36.dp))
        }

        firebaseError?.let { msg ->
            AlertDialog(
                onDismissRequest = { firebaseError = null; viewModel.resetEstado() },
                title = { Text(loc("Error al registrar", "Registration error"), fontWeight = FontWeight.Bold) },
                text  = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { firebaseError = null; viewModel.resetEstado() }) {
                        Text(loc("Entendido", "Got it"), color = RegRosaGine, fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTES AUXILIARES COMPARTIDOS
// ═════════════════════════════════════════════════════════════════════════════

@Composable
private fun RegisterScreenHeader(
    onBack:    () -> Unit,
    icon:      ImageVector,
    iconColor: Color,
    title:     String,
    subtitle:  String
) {
    Row(Modifier.fillMaxWidth()) {
        IconButton(
            onClick  = onBack,
            modifier = Modifier
                .clip(CircleShape)
                .background(RegCardWhite)
                .semantics { contentDescription = "Botón volver. Esquina superior izquierda." }
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Volver", tint = RegGreen)
        }
    }
    Spacer(Modifier.height(24.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = RegDarkGreen)
            Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun RegisterSectionTitle(
    title:      String,
    icon:       ImageVector,
    color:      Color,
    isOptional: Boolean = false
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = RegDarkGreen)
        if (isOptional) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.LightGray.copy(alpha = 0.4f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text("Opcional", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            }
        }
    }
    Spacer(Modifier.height(2.dp))
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f), thickness = 0.8.dp)
}

@Composable
private fun RegisterField(
    value:           String,
    onValueChange:   (String) -> Unit,
    label:           String,
    icon:            ImageVector,
    error:           String?          = null,
    placeholder:     String?          = null,
    a11yLabel:       String           = "",
    isPassword:      Boolean          = false,
    showPassword:    Boolean          = false,
    onTogglePassword:(() -> Unit)?    = null,
    keyboardOptions: KeyboardOptions  = KeyboardOptions.Default,
    a11yActive:      Boolean          = false,
    activo:          Boolean          = true,
    readOnly:        Boolean          = false,
    onFocus:         (() -> Unit)?    = null,
    onNext:          (() -> Unit)?    = null,
    ttsManager:      NutriTTS?        = null,
    idioma:          IdiomaVoz        = IdiomaVoz.ESPANOL_MX
) {
    val finalKeyboardOptions = if (isPassword && keyboardOptions == KeyboardOptions.Default) {
        KeyboardOptions(keyboardType = KeyboardType.Password)
    } else {
        keyboardOptions
    }
    if (a11yActive) {
        CampoTextoAccesible(
            valor          = value,
            onValorChange  = { if (!readOnly) onValueChange(it) },
            etiqueta       = label,
            descripcionVoz = a11yLabel.ifEmpty { label },
            placeholder    = placeholder ?: "",
            ttsManager     = ttsManager,
            idioma         = idioma,
            colorPrimario  = RegGreen,
            keyboardOptions = finalKeyboardOptions,
            activo         = activo && !readOnly,
            onFocus        = onFocus,
            onNext         = onNext
        )
        if (error != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text     = error,
                color    = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    } else {
        OutlinedTextField(
            value         = value,
            onValueChange = { if (!readOnly) onValueChange(it) },
            readOnly      = readOnly,
            modifier      = Modifier.fillMaxWidth().let { m ->
                if (a11yLabel.isNotEmpty())
                    m.semantics {
                        contentDescription = a11yLabel + if (error != null) " Error: $error" else ""
                    }
                else m
            }.onFocusChanged {
                if (it.isFocused) onFocus?.invoke()
            },
            label         = { Text(label) },
            placeholder   = if (placeholder != null) ({ Text(placeholder, color = Color.LightGray) }) else null,
            leadingIcon   = {
                Icon(icon, contentDescription = null, tint = if (error != null) Color.Red else RegGreen)
            },
            trailingIcon  = if (isPassword && onTogglePassword != null) ({
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector        = if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (showPassword) "Ocultar clave" else "Mostrar clave",
                        tint               = Color.Gray
                    )
                }
            }) else null,
            visualTransformation = if (isPassword && !showPassword)
                PasswordVisualTransformation()
            else
                VisualTransformation.None,
            isError        = error != null,
            supportingText = if (error != null) ({
                Text(error, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }) else null,
            shape           = RoundedCornerShape(16.dp),
            keyboardOptions = finalKeyboardOptions,
            singleLine      = true,
            colors          = OutlinedTextFieldDefaults.colors(
                focusedBorderColor   = RegGreen,
                unfocusedBorderColor = Color.LightGray,
                focusedLabelColor    = RegGreen,
                cursorColor          = RegGreen,
                errorBorderColor     = Color.Red
            )
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// COMPONENTE TARJETA DE VERIFICACIÓN DE CÉDULA PROFESIONAL SEP
// ═════════════════════════════════════════════════════════════════════════════

@Composable
fun TarjetaResumenCedula(
    res: ResultadoCedula,
    nombreUsuario: String = "",
    esGinecologo: Boolean = false
) {
    val nombreNoCoincide = res.valida && res.nombreTitular.isNotBlank() && nombreUsuario.isNotBlank() && !coincideNombreConTitular(nombreUsuario, res.nombreTitular)
    val profesionNoCoincide = res.valida && res.profesion.isNotBlank() && (
        if (esGinecologo) !esProfesionValidaGinecologo(res.profesion)
        else !esProfesionValidaNutriologo(res.profesion)
    )
    val hayError = nombreNoCoincide || profesionNoCoincide

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(
                if (hayError) Color(0xFFFFEBEE) else if (res.valida) Color(0xFFF1F8E9) else Color(0xFFFFF3E0),
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                if (hayError) Color(0xFFE57373) else if (res.valida) Color(0xFFAED581) else Color(0xFFFFB74D),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (hayError) Icons.Rounded.ErrorOutline else if (res.valida) Icons.Rounded.CheckCircle else Icons.Rounded.Warning,
                contentDescription = null,
                tint = if (hayError) Color(0xFFC62828) else if (res.valida) Color(0xFF2E7D32) else Color(0xFFE65100),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                if (nombreNoCoincide) "Error, datos no coinciden, inténtelo de nuevo"
                else if (profesionNoCoincide) "Error, la cédula no corresponde a la especialidad requerida"
                else if (res.valida) "Cédula profesional verificada ante la SEP" 
                else res.mensaje,
                fontSize = 13.sp,
                color = if (hayError) Color(0xFFC62828) else if (res.valida) Color(0xFF1B5E20) else Color(0xFFE65100),
                fontWeight = FontWeight.Bold
            )
        }

        if (res.valida) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = if (hayError) Color(0xFFFFCDD2) else Color(0xFFC8E6C9), thickness = 1.dp)
            Spacer(Modifier.height(6.dp))

            if (res.cedula.isNotBlank()) RenglonDetalleCedula("Núm. Cédula", res.cedula)
            if (res.nombreTitular.isNotBlank()) RenglonDetalleCedula("Titular Oficial SEP", res.nombreTitular)
            if (res.genero.isNotBlank()) RenglonDetalleCedula("Género", res.genero)
            if (res.institucion.isNotBlank()) RenglonDetalleCedula("Institución", res.institucion)
            if (res.profesion.isNotBlank()) RenglonDetalleCedula("Profesión", res.profesion)
            if (res.entidad.isNotBlank()) RenglonDetalleCedula("Entidad Federativa", res.entidad)
            if (res.anoRegistro.isNotBlank()) RenglonDetalleCedula("Año de Registro", res.anoRegistro)

            if (nombreNoCoincide) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "El nombre ingresado no corresponde a la persona titular de la cédula ante la SEP.",
                    fontSize = 11.sp,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Medium
                )
            } else if (profesionNoCoincide) {
                Spacer(Modifier.height(6.dp))
                Text(
                    if (esGinecologo) "Para registrarse como Ginecólogo/a se requiere cédula en Medicina o Ginecología."
                    else "Para registrarse como Nutriólogo/a se requiere cédula en Nutrición, Dietética o Medicina.",
                    fontSize = 11.sp,
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun RenglonDetalleCedula(etiqueta: String, valor: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            "$etiqueta: ",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2E7D32)
        )
        Text(
            valor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Normal,
            color = Color.DarkGray
        )
    }
}

