package com.example.nutriia.accesibilidad

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityManager as AndroidA11yManager
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.booleanPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

// ─── DataStore ────────────────────────────────────────────────────────────────
val Context.accessibilityDataStore by preferencesDataStore(name = "accessibility_prefs")
private val MODE_KEY    = stringPreferencesKey("accessibility_mode")
private val LANG_KEY    = stringPreferencesKey("accessibility_lang")
private val PRIMERA_VEZ = booleanPreferencesKey("primera_vez")

// ─── Modos ────────────────────────────────────────────────────────────────────
enum class AccessibilityMode(val label: String, val description: String) {
    NORMAL("Estándar",                  "Experiencia completa sin adaptaciones"),
    BLIND( "Condición visual",          "Lector de pantalla, voz y alto contraste"),
    MUTE(  "Condición auditiva",        "Sin entrada de voz, teclado visual siempre visible")
}

// ─── Idiomas ──────────────────────────────────────────────────────────────────
enum class IdiomaVoz(
    val label:       String,
    val descripcion: String,
    val localeTTS:   Locale,
    val localeVoz:   String,
    val soportado:   Boolean = true
) {
    ESPANOL_MX(
        label       = "Español Latinoamérica",
        descripcion = "Voz en español de México y Latinoamérica",
        localeTTS   = Locale.forLanguageTag("es-MX"),
        localeVoz   = "es-MX"
    ),
    ESPANOL_US(
        label       = "Español Estados Unidos",
        descripcion = "Voz en español neutro de Estados Unidos",
        localeTTS   = Locale.forLanguageTag("es-US"),
        localeVoz   = "es-US"
    ),
    INGLES(
        label       = "English",
        descripcion = "Voice in American English",
        localeTTS   = Locale.US,
        localeVoz   = "en-US"
    )
}

// ─── Repositorio ──────────────────────────────────────────────────────────────
class AccessibilityRepository(private val context: Context) {

    val modeFlow: Flow<AccessibilityMode> = context.accessibilityDataStore.data
        .map { prefs ->
            runCatching { AccessibilityMode.valueOf(prefs[MODE_KEY] ?: "NORMAL") }
                .getOrDefault(AccessibilityMode.NORMAL)
        }

    val langFlow: Flow<IdiomaVoz> = context.accessibilityDataStore.data
        .map { prefs ->
            runCatching { IdiomaVoz.valueOf(prefs[LANG_KEY] ?: "ESPANOL_MX") }
                .getOrDefault(IdiomaVoz.ESPANOL_MX)
        }

    val primeraVezFlow: Flow<Boolean> = context.accessibilityDataStore.data
        .map { prefs -> prefs[PRIMERA_VEZ] ?: true }

    suspend fun saveMode(mode: AccessibilityMode) {
        context.accessibilityDataStore.edit { it[MODE_KEY] = mode.name }
    }

    suspend fun saveLang(lang: IdiomaVoz) {
        context.accessibilityDataStore.edit { it[LANG_KEY] = lang.name }
    }

    suspend fun marcarPrimeraVezCompletada() {
        context.accessibilityDataStore.edit { it[PRIMERA_VEZ] = false }
    }
}

// ─── CompositionLocals ────────────────────────────────────────────────────────
val LocalAccessibilityMode = compositionLocalOf { AccessibilityMode.NORMAL }
val LocalIdiomaVoz         = compositionLocalOf { IdiomaVoz.ESPANOL_MX }

// ─── Helper Global de Localización ──────────────────────────────────────────
fun IdiomaVoz.loc(es: String, en: String): String =
    if (this == IdiomaVoz.INGLES) en else es

// ─── NutriTTS ─────────────────────────────────────────────────────────────────
class NutriTTS(context: Context, private var idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX) {

    private var tts: TextToSpeech? = null
    private var ready = false
    var vozActiva: String = "Iniciando..."

    fun isReady() = ready

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                aplicarIdioma(idioma)
                tts?.setSpeechRate(0.90f)
                tts?.setPitch(1.10f)
                ready = true
                android.util.Log.d("NutriTTS", "Listo. Voz: $vozActiva")
            }
        }
    }

    fun cambiarIdioma(nuevoIdioma: IdiomaVoz) {
        idioma = nuevoIdioma
        aplicarIdioma(nuevoIdioma)
    }

    private fun aplicarIdioma(idioma: IdiomaVoz) {
        val voces = tts?.voices
            ?.filter { it.locale.language == idioma.localeTTS.language }
            ?.sortedByDescending { it.quality }
            ?: emptyList()
 
        val vozElegida = voces.firstOrNull {
            it.locale.country == idioma.localeTTS.country
        } ?: voces.firstOrNull()
 
        if (vozElegida != null) {
            tts?.voice  = vozElegida
            vozActiva   = "${vozElegida.name} (${vozElegida.locale})"
        } else {
            tts?.language = idioma.localeTTS
            vozActiva     = "Generica ${idioma.localeTTS}"
        }
    }

    fun hablar(texto: String) {
        if (!ready || texto.isBlank()) return
        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, texto.hashCode().toString())
    }

    // ── Habla el texto según el idioma activo ─────────────────────────────────
    fun hablarLocalizado(esTexto: String, enTexto: String) {
        hablar(if (idioma == IdiomaVoz.INGLES) enTexto else esTexto)
    }

    fun hablarEnCola(texto: String) {
        if (!ready || texto.isBlank()) return
        tts?.speak(texto, TextToSpeech.QUEUE_ADD, null, texto.hashCode().toString())
    }

    // ── Versión localizada en cola ────────────────────────────────────────────
    fun hablarEnColaLocalizado(esTexto: String, enTexto: String) {
        hablarEnCola(if (idioma == IdiomaVoz.INGLES) enTexto else esTexto)
    }

    fun estaHablando(): Boolean = tts?.isSpeaking == true

    suspend fun hablarYEsperar(texto: String, margenMs: Long = 600L) {
        if (!ready || texto.isBlank()) return
        val utteranceId = "nutriia_${System.currentTimeMillis()}"
        var terminado = false

        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(id: String?)  {}
            override fun onDone(id: String?)   { if (id == utteranceId) terminado = true }
            override fun onError(id: String?)  { terminado = true }
        })

        tts?.speak(texto, TextToSpeech.QUEUE_FLUSH, null, utteranceId)

        val maxEspera = 30_000L
        var esperado  = 0L
        while (!terminado && esperado < maxEspera) {
            kotlinx.coroutines.delay(100)
            esperado += 100
        }
        kotlinx.coroutines.delay(margenMs)
    }

    // ── Versión localizada con espera ─────────────────────────────────────────
    suspend fun hablarYEsperarLocalizado(esTexto: String, enTexto: String, margenMs: Long = 600L) {
        hablarYEsperar(if (idioma == IdiomaVoz.INGLES) enTexto else esTexto, margenMs)
    }

    fun probarVoz() = hablarLocalizado(Voz.PRUEBA_VOZ, VozEn.PRUEBA_VOZ)
    fun silenciar() = tts?.stop()

    fun obtenerVocesDisponibles(): List<String> =
        (tts?.voices?.filter { it.locale.language == idioma.localeTTS.language }
            ?.map { "${it.name} — ${it.locale}" } ?: emptyList())

    fun liberar() {
        tts?.stop(); tts?.shutdown(); tts = null; ready = false
    }
}

// ─── Detección del sistema ────────────────────────────────────────────────────
fun isTalkBackActive(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AndroidA11yManager
    return am?.isEnabled == true && am.isTouchExplorationEnabled
}

fun abrirConfiguracionTalkBack(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )
}

// ─── HapticFeedback ───────────────────────────────────────────────────────────
fun vibrateTap(haptic: HapticFeedback)     = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
fun vibrateSuccess(haptic: HapticFeedback) = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
fun vibrateError(haptic: HapticFeedback)   = haptic.performHapticFeedback(HapticFeedbackType.LongPress)

fun vibrateTap(context: Context?)     = MotorHapticoNutriIA.vibrarFuerte(context, 35L, 190)
fun vibrateSuccess(context: Context?) = MotorHapticoNutriIA.vibrarFuerte(context, 65L, 255)
fun vibrateError(context: Context?)   = MotorHapticoNutriIA.vibrarFuerte(context, 85L, 255)

// ─── Textos de voz — Español ──────────────────────────────────────────────────
object Voz {

    const val MODO_CIEGO =
        "Hola, soy Nutr IA, tu nutria nutrióloga favorita. " +
                "Acabo de activar mi modo para personas con discapacidad visual. " +
                "Voy a guiarte con orientación espacial de cada botón, leer todo en voz alta y activar el micrófono automáticamente para que uses la app con total comodidad y autonomía. " +
                "Juntos vamos a cuidar la nutrición y salud de tu familia."

    const val MODO_MUDO =
        "Modo para condición auditiva activado. El teclado estará siempre visible para ti."

    const val MODO_NORMAL =
        "Modo estándar activado. Bienvenido a Nutr IA."

    const val PRUEBA_VOZ =
        "Hola, soy Nutr IA. Soy una nutria muy estudiosa y estoy aquí para ayudarte. " +
                "Vamos a cuidar juntos la alimentación y bienestar de tus pequeños."

    const val ACCESIBILIDAD_INTRO =
        "Hola, bienvenido a Nutr IA, tu asistente de nutrición y salud familiar. " +
                "Soy una nutria muy aplicada y me encanta ayudarte. " +
                "Antes de empezar, cuéntame cómo usas tu teléfono para adaptarme a ti. " +
                "En el centro de la pantalla tienes tres opciones: " +
                "Primera: Estándar, para uso regular. " +
                "Segunda: Modo para condición visual, donde yo leo todo en voz alta, te oriento en el espacio de la pantalla y activo el micrófono solo. " +
                "Tercera: Modo para condición auditiva, con teclado siempre visible. " +
                "Toca dos veces sobre la opción que va contigo. " +
                "Debajo puedes elegir el idioma de mi voz. " +
                "Al finalizar, toca dos veces el botón verde Continuar, ubicado fijo en la parte inferior de la pantalla."

    const val IDIOMA_INTRO =
        "Selector de idioma de voz en el centro de la pantalla. " +
                "Puedes elegir entre Español Latinoamérica, Español de Estados Unidos o Inglés."

    const val LOGIN_INTRO =
        "Pantalla de Inicio de Sesión de Nutr IA. " +
                "En el centro tienes dos campos: primero tu correo electrónico y debajo tu contraseña. " +
                "Justo debajo de los campos está el botón verde Entrar. " +
                "A la derecha encuentras el enlace Olvidaste tu contraseña. " +
                "Si aún no tienes cuenta, en la parte inferior de la pantalla está el botón 'Crear una cuenta'."

    const val LOGIN_INICIANDO    = "Un momento, estoy verificando tus credenciales de acceso."
    const val LOGIN_EXITO        = "Bienvenido. Acceso correcto. Abriendo tu panel principal."
    const val LOGIN_CAMPO_CORREO = "Campo de Correo Electrónico en el centro. El micrófono se activará solo. Di tu correo."
    const val LOGIN_CAMPO_CLAVE  = "Campo de Contraseña en el centro. El micrófono se activará solo. Di tu clave de acceso."

    const val REGISTRO_TIPO_INTRO =
        "Registro de Cuenta nueva. Cuéntame cómo vas a usar Nutr IA. " +
                "Tienes cuatro opciones organizadas en el centro de la pantalla de arriba a abajo: " +
                "Primera: Soy Padre o Madre, para registrar a tu hijo y llevar su nutrición y crecimiento. " +
                "Segunda: Mamá Primeriza, para acompañamiento durante tu embarazo. " +
                "Tercera: Soy Nutriólogo o Nutrióloga, para gestionar pacientes y crear planes nutricionales. " +
                "Cuarta: Soy Ginecólogo o Ginecóloga, para seguimiento médico prenatal. " +
                "Toca dos veces la opción que corresponda contigo. " +
                "Si ya tienes cuenta, en el pie de la pantalla está el botón 'Iniciar sesión'."

    const val REGISTRO_PADRE_INTRO =
        "Registro de perfil familiar. " +
                "En el centro tienes los campos de información guiada: nombre completo, teléfono de diez dígitos, correo, contraseña y el nombre de tu pequeño. " +
                "Al completar, toca dos veces el botón verde 'Crear cuenta y continuar', ubicado fijo en la parte inferior de la pantalla."

    const val REGISTRO_PADRE_EXITO =
        "¡Excelente! Tu cuenta está lista. Ahora vamos a registrar los datos de salud de tu pequeño."

    const val REGISTRO_NUTRI_INTRO =
        "Registro de perfil profesional de nutrición. " +
                "En el centro de la pantalla ingresa tu nombre, teléfono, cédula profesional, correo y contraseña. " +
                "Al terminar, toca dos veces el botón verde 'Crear perfil profesional' en la parte inferior de la pantalla."

    const val REGISTRO_NUTRI_EXITO =
        "Bienvenido colega. Tu perfil profesional está activo en la red de especialistas de Nutr IA."

    const val REGISTRO_ERROR_CAMPOS =
        "Atención: Hay campos pendientes o con formato incorrecto. Revisa los campos señalados en el centro de la pantalla."

    const val DASHBOARD_INTRO =
        "Panel Principal de Nutr IA. " +
                "En la parte superior está tu saludo, perfil y selector de hijos. " +
                "En la zona central tienes las tarjetas de acceso rápido: Lactancia, Alimentación Sólida, Crecimiento Infantil y Alertas. " +
                "En la parte inferior de la pantalla está la barra de navegación con cuatro pestañas: Inicio, Nutrición, Citas y Configuración. " +
                "Toca dos veces sobre cualquier tarjeta para ingresar."

    const val CRECIMIENTO_INTRO =
        "Módulo de Crecimiento Infantil. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se muestra el resumen de percentiles de la Organización Mundial de la Salud con el peso y talla actuales. " +
                "En la esquina inferior derecha encuentras el botón flotante verde 'Registrar Peso y Talla'. Toca dos veces para agregar una nueva medición."

    const val LACTANCIA_INTRO =
        "Módulo de Lactancia Materna. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes el cronómetro con los selectores Pecho Izquierdo y Pecho Derecho. " +
                "En la parte central inferior está el botón verde para iniciar o guardar la toma. Toca dos veces para registrar."

    const val SOLIDOS_INTRO =
        "Módulo de Alimentación Complementaria y Sólidos. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se listan los alimentos introducidos, recetas y posibles alergias. " +
                "En la esquina inferior derecha está el botón flotante verde para registrar un nuevo alimento probado."

    const val NUTRIENTES_INTRO =
        "Módulo de Micronutrientes y Vitaminas. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes el semáforo nutricional de hierro, zinc, calcio y vitaminas clave. " +
                "En la parte inferior central tienes el botón para anotar lo que comió hoy."

    const val SUENO_INTRO =
        "Módulo de Registro de Sueño Infantil. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes ver el total de horas de siestas y noche. " +
                "En la parte inferior tienes el botón para registrar una nueva sesión de sueño."

    const val RECORDATORIOS_INTRO =
        "Módulo de Recordatorios y Alertas de Salud. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se listan las vacunas, citas y tomas programadas. " +
                "En la parte inferior central tienes el botón para agregar una nueva alerta."

    const val PEDIATRA_INTRO =
        "Pantalla Mi Nutriólogo y Pediatra. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes consultar tu especialista vinculado, o buscarlo por código, correo y directorio público."

    const val AYUDA_INTRO =
        "Centro de Ayuda y Soporte de Nutr IA. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes respuestas a preguntas frecuentes, guía de uso y botón de contacto."

    const val CITAS_EMBARAZO_INTRO =
        "Módulo de Citas y Teleconsultas de Embarazo. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes agendar consultas con tu ginecólogo o iniciar videollamadas."

    const val NUTRICION_EMBARAZO_INTRO =
        "Módulo de Nutrición Materna y Embarazo. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se describen las guías de ácido fólico, hierro, hidratación y energía por trimestre."

    const val DIARIO_VISUAL_INTRO =
        "Diario de Alimentación de tu pequeño. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes explorar el historial de comidas registradas."

    const val CITAS_INTRO =
        "Directorio de Especialistas y Teleconsultas. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes explorar pediatras, nutriólogos y ginecólogos vinculados. " +
                "En cada tarjeta de especialista tienes el botón 'Iniciar Llamada' o 'Agendar Cita'. Toca dos veces para conectar."

    const val NUTRICAT_INTRO =
        "NutriChat con Asistente Inteligente. " +
                "En la esquina superior izquierda tienes el botón Atrás. " +
                "En el centro se leen los mensajes de la conversación. " +
                "En la parte inferior está el campo de texto con micrófono para dictar tus dudas y a su derecha el botón verde Enviar."

    const val CONFIGURACION_INTRO =
        "Pantalla de Configuración y Accesibilidad. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes cambiar el modo de accesibilidad, idioma de voz, alertas y sonido. " +
                "Al final de la lista está la opción de cerrar sesión."

    const val QUIZ_BIENVENIDA =
        "Evaluación inicial de tu pequeño. " +
                "Te pediré cuatro datos clave: nombre, fecha de nacimiento, peso y talla, y condiciones especiales de salud. " +
                "Toca dos veces el botón verde Continuar, ubicado en la parte inferior de la pantalla para comenzar."

    const val QUIZ_NOMBRE =
        "Primer dato: ¿Cómo se llama tu hijo o hija? Di su nombre completo. El micrófono se activará en automático."

    const val QUIZ_FECHA =
        "Segundo dato: Fecha de nacimiento. Di día, mes y año. Por ejemplo: quince de marzo de dos mil veintitrés. El micrófono se activará solo."

    const val QUIZ_MEDIDAS =
        "Tercer dato: Medidas actuales. Di primero el peso en kilos y luego la talla en centímetros. El micrófono se activará solo."

    const val QUIZ_CONDICIONES =
        "Cuarto dato: Condiciones especiales. En el centro de la pantalla hay dos interruptores: Alergias Alimentarias y Condición Especial. Toca dos veces sobre ellos para activarlos o desactivarlos."

    const val VOZ_ESCUCHANDO  = "Micrófono abierto. Te escucho."
    const val VOZ_PROCESANDO  = "Procesando lo que dijiste."
    const val VOZ_LISTO       = "Información capturada correctamente."
    const val VOZ_ERROR_MIC   = "No te escuché con claridad. Toca dos veces el botón de micrófono en la parte central inferior o usa el teclado Braille."
    const val VOZ_SIN_PERMISO = "Permiso de micrófono requerido. Ve a Configuración de tu dispositivo para activarlo."

    const val BRAILLE_INTRO =
        "Teclado Braille Virtual de seis puntos activado en la mitad inferior de la pantalla. " +
                "Columna izquierda de arriba a abajo: puntos uno, dos y tres. " +
                "Columna derecha de arriba a abajo: puntos cuatro, cinco y seis. " +
                "Toca los puntos de tu letra y presiona el botón central 'Agregar letra' para escribirla. " +
                "El botón Borrar está a la izquierda y Espacio a la derecha."

    const val BTN_ATRAS      = "Botón Atrás, ubicado en la esquina superior izquierda. Toca dos veces para regresar a la pantalla anterior."
    const val BTN_CONTINUAR  = "Botón verde Continuar, ubicado fijo en la parte inferior de la pantalla. Toca dos veces para avanzar."
    const val BTN_FINALIZAR  = "Botón verde Guardar y Finalizar, ubicado en la parte inferior de la pantalla. Toca dos veces para guardar."
    const val BTN_MICROFONO  = "Botón de micrófono, ubicado en la parte central inferior. Toca dos veces para dictar por voz."
}

// ─── Textos de voz — English ──────────────────────────────────────────────────
object VozEn {

    const val MODO_CIEGO =
        "Hello, I'm NutriIA, your favorite nutrition assistant. " +
                "Visual accessibility mode is now active. " +
                "I will provide spatial orientation for every button, read everything aloud, and automatically activate the microphone for seamless navigation. " +
                "Together we will take great care of your family's nutrition and health."

    const val MODO_MUDO =
        "Hearing condition mode activated. The visual keyboard will always remain visible."

    const val MODO_NORMAL =
        "Standard mode activated. Welcome to NutriIA."

    const val PRUEBA_VOZ =
        "Hello, I'm NutriIA. I'm a very dedicated assistant and I'm here to support you. " +
                "Together we will guide your family's health and nutrition."

    const val ACCESIBILIDAD_INTRO =
        "Welcome to NutriIA, your family nutrition and health assistant. " +
                "Before we begin, choose your preferred interaction mode. " +
                "In the center of the screen you have three options: " +
                "First: Standard, for regular use. " +
                "Second: Visual accessibility mode, with spatial guidance, full voice narration, and automatic microphone dictation. " +
                "Third: Hearing accessibility mode, with persistent visual keyboard. " +
                "Double tap the option that suits you best. " +
                "Below the modes you can select my voice language. " +
                "When ready, double tap the green Continue button at the bottom of the screen."

    const val IDIOMA_INTRO =
        "Voice language selector in the center of the screen. " +
                "Choose between Latin American Spanish, US Spanish, or English."

    const val LOGIN_INTRO =
        "NutriIA Sign In screen. " +
                "In the center you have two fields: email address at the top and password below. " +
                "Right below the fields is the green Sign In button. " +
                "To the right is the Forgot Password link. " +
                "If you don't have an account yet, tap the Create Account button at the very bottom."

    const val LOGIN_INICIANDO    = "Verifying your account credentials. Please hold on."
    const val LOGIN_EXITO        = "Access granted. Loading your dashboard."
    const val LOGIN_CAMPO_CORREO = "Email address field in the center. Microphone will open automatically. Say your email."
    const val LOGIN_CAMPO_CLAVE  = "Password field in the center. Microphone will open automatically. Say your password."

    const val REGISTRO_TIPO_INTRO =
        "New Account Registration. Select your role. " +
                "You have four options arranged vertically in the center: " +
                "First: Parent, to register your child and monitor nutrition and growth. " +
                "Second: Expectant Mom, for pregnancy tracking. " +
                "Third: Nutritionist, to manage patients and design meal plans. " +
                "Fourth: Gynecologist, for prenatal medical care. " +
                "Double tap your choice. " +
                "If you already have an account, the Sign In button is at the bottom of the screen."

    const val REGISTRO_PADRE_INTRO =
        "Family Profile Registration. " +
                "In the center are the guided fields: full name, 10-digit phone number, email, password, and your child's name. " +
                "When finished, double tap the green 'Create Account and Continue' button fixed at the bottom of the screen."

    const val REGISTRO_PADRE_EXITO =
        "Account created successfully. Now let's set up your child's health profile."

    const val REGISTRO_NUTRI_INTRO =
        "Professional Nutrition Profile Registration. " +
                "Enter your full name, phone number, professional license, email, and password in the center fields. " +
                "Double tap the green 'Create Professional Profile' button at the bottom to complete."

    const val REGISTRO_NUTRI_EXITO =
        "Welcome to the team. Your professional profile is now active on NutriIA."

    const val REGISTRO_ERROR_CAMPOS =
        "Attention: Some fields are incomplete or invalid. Please check the highlighted fields in the center."

    const val DASHBOARD_INTRO =
        "NutriIA Main Dashboard. " +
                "At the top you find your profile and child selector. " +
                "In the center area are your quick access cards: Breastfeeding, Solid Foods, Growth Tracking, and Health Alerts. " +
                "At the bottom of the screen is the navigation bar with four tabs: Home, Nutrition, Appointments, and Settings. " +
                "Double tap any card to open."

    const val CRECIMIENTO_INTRO =
        "Child Growth Module. " +
                "In the top left corner is the Back button. " +
                "In the center is the WHO percentile chart displaying current weight and height metrics. " +
                "In the bottom right corner is the green floating action button 'Record Weight and Height'. Double tap to log a new entry."

    const val LACTANCIA_INTRO =
        "Breastfeeding Module. " +
                "In the top left is the Back button. " +
                "In the center is the feeding timer with Left Breast and Right Breast buttons. " +
                "In the lower center is the green button to start or save the session. Double tap to activate."

    const val SOLIDOS_INTRO =
        "Complementary Feeding and Solids Module. " +
                "In the top left is the Back button. " +
                "In the center are introduced foods, recipes, and recorded sensitivities. " +
                "In the bottom right is the green floating button to log a new tasted food."

    const val NUTRIENTES_INTRO =
        "Micronutrients and Vitamins Module. " +
                "In the top left is the Back button. " +
                "In the center is the nutritional indicator for iron, zinc, calcium, and key vitamins. " +
                "In the lower center is the button to log meals."

    const val SUENO_INTRO =
        "Infant Sleep Tracking Module. " +
                "In the top left is the Back button. " +
                "In the center you can view total daytime nap and nighttime sleep hours. " +
                "At the bottom is the button to log a new sleep session."

    const val RECORDATORIOS_INTRO =
        "Health Reminders and Alerts Module. " +
                "In the top left is the Back button. " +
                "In the center are scheduled vaccines, appointments, and feedings. " +
                "At the bottom center is the button to add a new alert."

    const val PEDIATRA_INTRO =
        "My Nutritionist and Pediatrician screen. " +
                "In the top left is the Back button. " +
                "In the center you can manage your linked specialist, or search by code, email, and public directory."

    const val AYUDA_INTRO =
        "Help and Support Center. " +
                "In the top left is the Back button. " +
                "In the center you can find FAQ answers, usage guides, and contact options."

    const val CITAS_EMBARAZO_INTRO =
        "Pregnancy Appointments and Telehealth Module. " +
                "In the top left is the Back button. " +
                "In the center you can book consultations with your gynecologist or start video calls."

    const val NUTRICION_EMBARAZO_INTRO =
        "Maternal Nutrition and Pregnancy Module. " +
                "In the top left is the Back button. " +
                "In the center are trimester guidelines for folic acid, iron, hydration, and energy intake."

    const val DIARIO_VISUAL_INTRO =
        "Child Feeding Journal. " +
                "In the top left is the Back button. " +
                "In the center you can review logged meals history."

    const val CITAS_INTRO =
        "Specialists Directory and Telehealth. " +
                "In the top left is the Back button. " +
                "In the center you can browse connected pediatricians, nutritionists, and gynecologists. " +
                "On each specialist card is the green 'Start Call' or 'Book Appointment' button. Double tap to connect."

    const val NUTRICAT_INTRO =
        "NutriChat AI Assistant. " +
                "In the top left is the Back button. " +
                "In the center are conversation messages. " +
                "At the bottom is the text input with microphone to dictate questions, and the green Send button to its right."

    const val CONFIGURACION_INTRO =
        "Settings and Accessibility. " +
                "In the top left is the Back button. " +
                "In the center you can customize accessibility modes, voice language, alerts, and sounds. " +
                "At the end of the list is the Sign Out option."

    const val QUIZ_BIENVENIDA =
        "Initial Child Assessment. " +
                "I will ask for four key details: name, date of birth, current weight and height, and special health conditions. " +
                "Double tap the green Continue button at the bottom of the screen to start."

    const val QUIZ_NOMBRE =
        "First step: What is your child's full name? The microphone will open automatically."

    const val QUIZ_FECHA =
        "Second step: Date of birth. Say day, month, and year. For example: March 15th 2023. The microphone will open automatically."

    const val QUIZ_MEDIDAS =
        "Third step: Current measurements. Say weight in kilograms first, then height in centimeters. The microphone will open automatically."

    const val QUIZ_CONDICIONES =
        "Fourth step: Special conditions. In the center are two switches: Food Allergies and Special Health Condition. Double tap to toggle."

    const val VOZ_ESCUCHANDO  = "Microphone open. Listening to you."
    const val VOZ_PROCESANDO  = "Processing your voice."
    const val VOZ_LISTO       = "Information captured successfully."
    const val VOZ_ERROR_MIC   = "I didn't hear you clearly. Double tap the microphone button in the lower center or use the virtual Braille keyboard."
    const val VOZ_SIN_PERMISO = "Microphone permission required. Please enable it in your device Settings."

    const val BRAILLE_INTRO =
        "Virtual 6-dot Braille Keyboard active in the lower half of the screen. " +
                "Left column from top to bottom: dots one, two, and three. " +
                "Right column from top to bottom: dots four, five, and six. " +
                "Tap your letter dots and press the center 'Add Letter' button to confirm. " +
                "Delete button is on the left and Space on the right."

    const val BTN_ATRAS      = "Back button, located in the top left corner. Double tap to return to the previous screen."
    const val BTN_CONTINUAR  = "Green Continue button, fixed at the bottom of the screen. Double tap to proceed."
    const val BTN_FINALIZAR  = "Green Save and Finish button, located at the bottom of the screen. Double tap to save."
    const val BTN_MICROFONO  = "Microphone button, located in the bottom center. Double tap to dictate by voice."
}