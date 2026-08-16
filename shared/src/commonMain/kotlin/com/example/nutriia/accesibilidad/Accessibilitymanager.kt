package com.example.nutriia.accesibilidad

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import com.example.nutriia.platform.openUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// ─── Modos ────────────────────────────────────────────────────────────────────
enum class AccessibilityMode(val label: String, val description: String) {
    NORMAL("Estándar",                  "Experiencia completa sin adaptaciones"),
    BLIND( "Modo para personas ciegas", "Lector de pantalla, voz y alto contraste"),
    MUTE(  "Modo para personas mudas",  "Sin entrada de voz, teclado visual siempre visible")
}

// ─── Idiomas ──────────────────────────────────────────────────────────────────
enum class IdiomaVoz(
    val label:       String,
    val descripcion: String,
    val localeVoz:   String,
    val soportado:   Boolean = true
) {
    ESPANOL_MX(
        label       = "Español Latinoamérica",
        descripcion = "Voz en español de México y Latinoamérica",
        localeVoz   = "es-MX"
    ),
    ESPANOL_US(
        label       = "Español Estados Unidos",
        descripcion = "Voz en español neutro de Estados Unidos",
        localeVoz   = "es-US"
    ),
    INGLES(
        label       = "English",
        descripcion = "Voice in American English",
        localeVoz   = "en-US"
    )
}

// ─── Repositorio ──────────────────────────────────────────────────────────────
class AccessibilityRepository(context: Any? = null) {

    private val _modeFlow = MutableStateFlow(
        runCatching {
            AccessibilityMode.valueOf(
                com.example.nutriia.platform.PlatformPreferences.getString("accessibility_mode") ?: AccessibilityMode.NORMAL.name
            )
        }.getOrDefault(AccessibilityMode.NORMAL)
    )
    val modeFlow: Flow<AccessibilityMode> = _modeFlow.asStateFlow()

    private val _langFlow = MutableStateFlow(
        runCatching {
            IdiomaVoz.valueOf(
                com.example.nutriia.platform.PlatformPreferences.getString("accessibility_lang") ?: IdiomaVoz.ESPANOL_MX.name
            )
        }.getOrDefault(IdiomaVoz.ESPANOL_MX)
    )
    val langFlow: Flow<IdiomaVoz> = _langFlow.asStateFlow()

    private val _primeraVezFlow = MutableStateFlow(
        com.example.nutriia.platform.PlatformPreferences.getBoolean("accessibility_primera_vez", true)
    )
    val primeraVezFlow: Flow<Boolean> = _primeraVezFlow.asStateFlow()

    suspend fun saveMode(mode: AccessibilityMode) {
        _modeFlow.value = mode
        com.example.nutriia.platform.PlatformPreferences.putString("accessibility_mode", mode.name)
    }

    suspend fun saveLang(lang: IdiomaVoz) {
        _langFlow.value = lang
        com.example.nutriia.platform.PlatformPreferences.putString("accessibility_lang", lang.name)
    }

    suspend fun marcarPrimeraVezCompletada() {
        _primeraVezFlow.value = false
        com.example.nutriia.platform.PlatformPreferences.putBoolean("accessibility_primera_vez", false)
    }
}

// ─── CompositionLocals ────────────────────────────────────────────────────────
val LocalAccessibilityMode = compositionLocalOf { AccessibilityMode.NORMAL }
val LocalIdiomaVoz         = compositionLocalOf { IdiomaVoz.ESPANOL_MX }

// ─── Helper Global de Localización ──────────────────────────────────────────
fun IdiomaVoz.loc(es: String, en: String): String =
    if (this == IdiomaVoz.INGLES) en else es

// ─── NutriTTS ─────────────────────────────────────────────────────────────────
class NutriTTS(context: Any? = null, private var idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX) {

    private val bridge = NutriTTSBridge()
    private var ready = true
    var vozActiva: String = "Voz Nativa (${idioma.localeVoz})"

    fun isReady() = ready

    fun cambiarIdioma(nuevoIdioma: IdiomaVoz) {
        idioma = nuevoIdioma
        vozActiva = "Voz Nativa (${idioma.localeVoz})"
    }

    fun hablar(texto: String) {
        if (texto.isBlank()) return
        bridge.speak(texto, idioma.localeVoz)
    }

    fun hablarLocalizado(esTexto: String, enTexto: String) {
        hablar(if (idioma == IdiomaVoz.INGLES) enTexto else esTexto)
    }

    fun hablarEnCola(texto: String) {
        if (texto.isBlank()) return
        bridge.speak(texto, idioma.localeVoz)
    }

    fun hablarEnColaLocalizado(esTexto: String, enTexto: String) {
        hablarEnCola(if (idioma == IdiomaVoz.INGLES) enTexto else esTexto)
    }

    fun estaHablando(): Boolean = false

    suspend fun hablarYEsperar(texto: String, margenMs: Long = 600L) {
        if (texto.isBlank()) return
        bridge.speak(texto, idioma.localeVoz)
        kotlinx.coroutines.delay(margenMs)
    }

    fun hablarYEsperarLocalizado(esTexto: String, enTexto: String, margenMs: Long = 600L) {
        hablarLocalizado(esTexto, enTexto)
    }

    fun probarVoz() = hablarLocalizado(Voz.PRUEBA_VOZ, VozEn.PRUEBA_VOZ)
    fun silenciar() = bridge.stop()

    fun obtenerVocesDisponibles(): List<String> = listOf("Voz del Sistema (${idioma.localeVoz})")

    fun liberar() {
        bridge.stop()
    }
}

// ─── Detección del sistema ────────────────────────────────────────────────────
fun isTalkBackActive(context: Any? = null): Boolean = false

fun abrirConfiguracionTalkBack(context: Any? = null) {
    openUrl("app-settings:")
}

// ─── HapticFeedback ───────────────────────────────────────────────────────────
fun vibrateTap(haptic: HapticFeedback)     = haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
fun vibrateSuccess(haptic: HapticFeedback) = haptic.performHapticFeedback(HapticFeedbackType.LongPress)
fun vibrateError(haptic: HapticFeedback)   = haptic.performHapticFeedback(HapticFeedbackType.LongPress)

// ─── Textos de voz — Español ──────────────────────────────────────────────────
object Voz {

    const val MODO_CIEGO =
        "Hola, soy Nutr/IA, tu nutria nutriologa favorita. " +
                "Acabo de activar mi modo especial para ti. " +
                "Voy a leer todo en voz alta y el microfono se activara solo en cada campo. " +
                "Juntos vamos a cuidar la nutricion de tu familia."

    const val MODO_MUDO =
        "Modo para personas mudas activado. El teclado estara siempre visible para ti."

    const val MODO_NORMAL =
        "Modo estandar activado. Bienvenido a Nutr IA."

    const val PRUEBA_VOZ =
        "Hola, soy Nutr IA. Soy una nutria muy estudiosa y estoy aqui para ayudarte. " +
                "Vamos a cuidar juntos la alimentacion de tus pequeños."

    const val ACCESIBILIDAD_INTRO =
        "Hola, hola. Bienvenido a Nutr IA, tu asistente de nutricion infantil. " +
                "Soy una nutria muy aplicada y me encanta ayudar a las familias a comer mejor. " +
                "Antes de empezar, cuentame como usas el telefono para yo adaptarme a ti. " +
                "Abajo tienes tres opciones. " +
                "Primera: Estandar, para uso normal. " +
                "Segunda: Modo ciego, donde yo hablo todo y el microfono se activa solo. " +
                "Tercera: Modo mudo, con teclado siempre visible. " +
                "Toca la opcion que va contigo. " +
                "Debajo de los modos puedes elegir el idioma de mi voz. " +
                "Cuando termines, toca el boton verde Continuar al final de la pantalla."

    const val IDIOMA_INTRO =
        "Aqui puedes elegir en que idioma quieres que te hable. " +
                "Tengo español latinoamericano, español de Estados Unidos e ingles."

    const val LOGIN_INTRO =
        "Que bueno verte de nuevo. Soy Nutr IA y estoy lista para ayudarte. " +
                "Si es tu primera vez, toca el boton Crea una cuenta al final de la pantalla. " +
                "Si ya tienes cuenta, escribe tu correo y tu clave en los dos campos del centro. " +
                "El boton verde Entrar esta justo debajo. " +
                "Si olvidaste tu clave, hay un boton pequeño a la derecha que dice Olvidaste tu clave."

    const val LOGIN_INICIANDO    = "Un momento, estoy verificando tu cuenta."
    const val LOGIN_EXITO        = "Listo, ya entre. Vamos a ver como esta la nutricion de tu familia."
    const val LOGIN_CAMPO_CORREO = "Dime tu correo electronico."
    const val LOGIN_CAMPO_CLAVE  = "Ahora dime tu clave de acceso."

    const val REGISTRO_TIPO_INTRO =
        "Excelente, vamos a crear tu cuenta. Primero dime, como vas a usar Nutr IA. " +
                "Hay dos opciones en el centro. " +
                "Primera: Soy Padre o Madre. Para registrar a tu hijo o hija y llevar su nutricion. " +
                "Segunda: Soy Nutriologo o Nutriologa. Para gestionar pacientes y crear planes de alimentacion. " +
                "Toca la que va contigo. " +
                "Si ya tienes cuenta, el boton Inicia sesion esta hasta abajo."

    const val REGISTRO_PADRE_INTRO =
        "Que emocion, una familia nueva en Nutr IA. Vamos a crear tu cuenta de papa o mama. " +
                "Hay seis campos que llenar, yo te voy guiando uno por uno. " +
                "Primero tu nombre completo con apellidos. " +
                "Segundo tu numero de telefono de diez digitos. " +
                "Tercero tu correo electronico. " +
                "Cuarto tu clave de acceso, minimo seis caracteres. " +
                "Quinto repite la misma clave para confirmar. " +
                "Sexto el nombre de tu primer hijo o hija. " +
                "El codigo de nutriologo es opcional, puedes dejarlo vacio. " +
                "Cuando termines todos, toca el boton verde Crear cuenta al final."

    const val REGISTRO_PADRE_EXITO =
        "Perfecto, tu cuenta esta lista. Ahora vamos a registrar a tu pequeño o pequeña. " +
                "Esto es lo mas bonito para mi."

    const val REGISTRO_NUTRI_INTRO =
        "Bienvenido colega. Vamos a crear tu perfil profesional en Nutr IA. " +
                "Son siete campos en total. " +
                "Primero tu nombre completo. " +
                "Segundo tu telefono. " +
                "Tercero tu especialidad, por ejemplo Nutricion Pediatrica. " +
                "Cuarto tu cedula profesional. " +
                "Quinto tu correo electronico. " +
                "Sexto tu clave de acceso, minimo seis caracteres. " +
                "Septimo confirma tu clave. " +
                "Al terminar toca el boton verde Crear perfil profesional."

    const val REGISTRO_NUTRI_EXITO =
        "Bienvenido al equipo. Tu perfil profesional esta listo. " +
                "Juntos vamos a hacer una gran diferencia en la nutricion infantil."

    const val REGISTRO_ERROR_CAMPOS =
        "Ups, me falta informacion. Hay campos en rojo que necesitan tu atencion. " +
                "Revisalos y cuando esten listos intentamos de nuevo."

    const val QUIZ_BIENVENIDA =
        "Ahora lo mas importante: conocer a tu pequeño o pequeña. " +
                "Voy a pedirte cuatro datos: nombre, fecha de nacimiento, peso y talla, y salud especial. " +
                "Con eso yo puedo personalizar su plan de nutricion. " +
                "Toca el boton verde Continuar cuando estes listo."

    const val QUIZ_NOMBRE =
        "Primero lo primero. Como se llama tu hijo o hija. " +
                "Di el nombre completo. " +
                "El microfono se activa solito en unos segundos. " +
                "Cuando escuches la senal, di el nombre."

    const val QUIZ_FECHA =
        "Perfecto. Ahora dime cuando nacio tu pequeño o pequeña. " +
                "Di la fecha con dia, mes y año. " +
                "Por ejemplo: quince de marzo de dos mil veintitres. " +
                "El microfono se activa solo."

    const val QUIZ_MEDIDAS =
        "Casi casi. Ahora necesito el peso y la talla actuales. " +
                "Primero di el peso, espera a que termine y luego diga ahora la talla. " +
                "El microfono se activa solo."

    const val QUIZ_CONDICIONES =
        "Penúltimo paso. Necesito saber si tu hijo o hija tiene algo especial de salud. " +
                "Hay dos interruptores: alergias alimentarias y condición especial. " +
                "Tócalos para activar o desactivar."

    const val VOZ_ESCUCHANDO  = "Te escucho, habla cuando quieras."
    const val VOZ_PROCESANDO  = "Dejame entender lo que dijiste."
    const val VOZ_LISTO       = "Listo, lo anote."
    const val VOZ_ERROR_MIC   = "No te escuche bien. Toca el circulo del microfono para intentarlo de nuevo."
    const val VOZ_SIN_PERMISO = "Necesito permiso para usar el microfono. Ve a Configuracion y activalo."

    const val BRAILLE_INTRO =
        "Teclado Braille activado. " +
                "Hay seis puntos grandes organizados en dos columnas. " +
                "Columna izquierda de arriba hacia abajo: punto uno, punto dos, punto tres. " +
                "Columna derecha de arriba hacia abajo: punto cuatro, punto cinco, punto seis. " +
                "Toca los puntos de tu letra y cuando aparezca la correcta " +
                "toca el boton central Agregar para confirmarla. " +
                "El boton Borrar esta a la izquierda y el boton Espacio a la derecha."

    const val BTN_CONTINUAR = "Boton verde Continuar. Al final de la pantalla."
    const val BTN_FINALIZAR = "Boton verde Finalizar registro. Al final de la pantalla."
}

// ─── Textos de voz — English ──────────────────────────────────────────────────
object VozEn {

    const val MODO_CIEGO =
        "Hello, I'm NutriIA, your favorite nutrition assistant. " +
                "I just activated my special mode for you. " +
                "I will read everything aloud and the microphone will activate automatically in each field. " +
                "Together we will take care of your family's nutrition."

    const val MODO_MUDO =
        "Mute mode activated. The keyboard will always be visible for you."

    const val MODO_NORMAL =
        "Standard mode activated. Welcome to NutriIA."

    const val PRUEBA_VOZ =
        "Hello, I'm NutriIA. I'm a very studious little otter and I'm here to help you. " +
                "Together we will take care of your children's nutrition."

    const val ACCESIBILIDAD_INTRO =
        "Hello, hello. Welcome to NutriIA, your child nutrition assistant. " +
                "I'm a very dedicated otter and I love helping families eat better. " +
                "Before we start, tell me how you use your phone so I can adapt to you. " +
                "Below you have three options. " +
                "First: Standard, for normal use. " +
                "Second: Blind mode, where I speak everything and the microphone activates automatically. " +
                "Third: Mute mode, with the keyboard always visible. " +
                "Tap the option that works for you. " +
                "Below the modes you can choose the language of my voice. " +
                "When you're done, tap the green Continue button at the bottom of the screen."

    const val IDIOMA_INTRO =
        "Here you can choose what language you want me to speak. " +
                "I have Latin American Spanish, United States Spanish, and English."

    const val LOGIN_INTRO =
        "Good to see you again. I'm NutriIA and I'm ready to help you. " +
                "If it's your first time, tap the Create an account button at the bottom of the screen. " +
                "If you already have an account, type your email and password in the two fields in the center. " +
                "The green Sign In button is right below. " +
                "If you forgot your password, there is a small button on the right that says Forgot your password."

    const val LOGIN_INICIANDO    = "One moment, I'm verifying your account."
    const val LOGIN_EXITO        = "All set, you're in. Let's see how your family's nutrition is doing."
    const val LOGIN_CAMPO_CORREO = "Tell me your email address."
    const val LOGIN_CAMPO_CLAVE  = "Now tell me your password."

    const val REGISTRO_TIPO_INTRO =
        "Excellent, let's create your account. First tell me, how are you going to use NutriIA. " +
                "There are two options in the center. " +
                "First: I'm a Parent. To register your child and track their nutrition. " +
                "Second: I'm a Nutritionist. To manage patients and create meal plans. " +
                "Tap the one that fits you. " +
                "If you already have an account, the Sign In button is at the bottom."

    const val REGISTRO_PADRE_INTRO =
        "How exciting, a new family in NutriIA. Let's create your parent account. " +
                "There are six fields to fill in, I'll guide you one by one. " +
                "First your full name with last names. " +
                "Second your ten-digit phone number. " +
                "Third your email address. " +
                "Fourth your password, at least six characters. " +
                "Fifth repeat the same password to confirm. " +
                "Sixth the name of your first child. " +
                "The nutritionist code is optional, you can leave it empty. " +
                "When you finish all of them, tap the green Create account button at the bottom."

    const val REGISTRO_PADRE_EXITO =
        "Perfect, your account is ready. Now let's register your little one. " +
                "This is my favorite part."

    const val REGISTRO_NUTRI_INTRO =
        "Welcome, colleague. Let's create your professional profile in NutriIA. " +
                "There are seven fields in total. " +
                "First your full name. " +
                "Second your phone number. " +
                "Third your specialty, for example Pediatric Nutrition. " +
                "Fourth your professional license number. " +
                "Fifth your email address. " +
                "Sixth your password, at least six characters. " +
                "Seventh confirm your password. " +
                "When you're done tap the green Create professional profile button."

    const val REGISTRO_NUTRI_EXITO =
        "Welcome to the team. Your professional profile is ready. " +
                "Together we're going to make a big difference in children's nutrition."

    const val REGISTRO_ERROR_CAMPOS =
        "Oops, I'm missing some information. There are fields in red that need your attention. " +
                "Review them and when they're ready let's try again."

    const val QUIZ_BIENVENIDA =
        "Now the most important part: getting to know your little one. " +
                "I'm going to ask you for four pieces of information: name, date of birth, weight and height, and special health needs. " +
                "With that I can personalize their nutrition plan. " +
                "Tap the green Continue button when you're ready."

    const val QUIZ_NOMBRE =
        "First things first. What is your child's name. " +
                "Say the full name. " +
                "The microphone will activate on its own in a few seconds. " +
                "When you hear the signal, say the name."

    const val QUIZ_FECHA =
        "Perfect. Now tell me when your little one was born. " +
                "Say the date with day, month and year. " +
                "For example: March fifteenth two thousand twenty three. " +
                "The microphone will activate on its own."

    const val QUIZ_MEDIDAS =
        "Almost there. Now I need the current weight and height. " +
                "First say the weight, wait for it to finish and then say now the height. " +
                "The microphone will activate on its own."

    const val QUIZ_CONDICIONES =
        "Second to last step. I need to know if your child has any special health conditions. " +
                "There are two switches: food allergies and special condition. " +
                "Tap them to turn on or off."

    const val VOZ_ESCUCHANDO  = "I'm listening, speak whenever you're ready."
    const val VOZ_PROCESANDO  = "Let me understand what you said."
    const val VOZ_LISTO       = "Got it, noted."
    const val VOZ_ERROR_MIC   = "I didn't hear you well. Tap the microphone circle to try again."
    const val VOZ_SIN_PERMISO = "I need permission to use the microphone. Go to Settings and enable it."

    const val BRAILLE_INTRO =
        "Braille keyboard activated. " +
                "There are six large dots organized in two columns. " +
                "Left column from top to bottom: dot one, dot two, dot three. " +
                "Right column from top to bottom: dot four, dot five, dot six. " +
                "Tap the dots for your letter and when the correct one appears " +
                "tap the center Add button to confirm it. " +
                "The Delete button is on the left and the Space button is on the right."

    const val BTN_CONTINUAR = "Green Continue button. At the bottom of the screen."
    const val BTN_FINALIZAR = "Green Finish registration button. At the bottom of the screen."
}