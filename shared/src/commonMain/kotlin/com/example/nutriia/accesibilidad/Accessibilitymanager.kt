package com.example.nutriia.accesibilidad

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import com.example.nutriia.platform.openUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Anuncia automáticamente el título de la pantalla al entrar en TalkBack / VoiceOver.
 */
fun Modifier.anuncioPantalla(titulo: String): Modifier = this.semantics {
    paneTitle = titulo
}

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

    private val _speedFlow = MutableStateFlow(
        runCatching {
            com.example.nutriia.platform.PlatformPreferences.getString("accessibility_speed")?.toFloatOrNull() ?: 0.90f
        }.getOrDefault(0.90f)
    )
    val speedFlow: Flow<Float> = _speedFlow.asStateFlow()

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

    suspend fun saveSpeed(speed: Float) {
        _speedFlow.value = speed
        com.example.nutriia.platform.PlatformPreferences.putString("accessibility_speed", speed.toString())
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

    fun setSpeechRate(rate: Float) {
        // Multiplatform bridge integration
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

    fun estaHablando(): Boolean = bridge.isSpeaking()

    suspend fun hablarYEsperar(texto: String, margenMs: Long = 600L) {
        if (texto.isBlank()) return
        bridge.speak(texto, idioma.localeVoz)
        val palabras = texto.split(" ").filter { it.isNotBlank() }.size
        val tiempoEstimadoMs = (texto.length * 65L).coerceAtLeast(palabras * 360L) + margenMs
        var tiempoTranscurrido = 0L
        kotlinx.coroutines.delay(350L)
        tiempoTranscurrido += 350L
        while (estaHablando() && tiempoTranscurrido < 25_000L) {
            kotlinx.coroutines.delay(200L)
            tiempoTranscurrido += 200L
        }
        if (tiempoTranscurrido < tiempoEstimadoMs) {
            kotlinx.coroutines.delay(tiempoEstimadoMs - tiempoTranscurrido)
        }
    }

    fun hablarYEsperarLocalizado(esTexto: String, enTexto: String, margenMs: Long = 600L) {
        hablarLocalizado(esTexto, enTexto)
    }

    fun probarVoz() = hablarLocalizado(Voz.PRUEBA_VOZ, VozEn.PRUEBA_VOZ)
    fun silenciar() = bridge.stop()

    fun obtenerVocesDisponibles(): List<String> = listOf("Voz del Sistema (${idioma.localeVoz})")

    fun esLectorDelSistemaActivo(): Boolean = com.example.nutriia.platform.isVoiceOverActive()

    fun liberar() {
        bridge.stop()
    }
}

// ─── Detección del sistema ────────────────────────────────────────────────────
fun isVoiceOverActive(context: Any? = null): Boolean = com.example.nutriia.platform.isVoiceOverActive()
fun isTalkBackActive(context: Any? = null): Boolean = isVoiceOverActive(context)

fun abrirConfiguracionVoiceOver(context: Any? = null) {
    openUrl("app-settings:")
}
fun abrirConfiguracionTalkBack(context: Any? = null) = abrirConfiguracionVoiceOver(context)

// ─── HapticFeedback & Earcons ──────────────────────────────────────────────────
fun vibrateTap(haptic: HapticFeedback)     { try { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) } catch (_: Throwable) {}; NutriEarcons.playButtonHover() }
fun vibrateSuccess(haptic: HapticFeedback) { try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}; NutriEarcons.playSuccess() }
fun vibrateError(haptic: HapticFeedback)   { try { haptic.performHapticFeedback(HapticFeedbackType.LongPress) } catch (_: Throwable) {}; NutriEarcons.playError() }

// ─── Textos de voz — Español ──────────────────────────────────────────────────
object Voz {

    const val READBACK_CONFIRMAR = "Capturado: %s. Di 'Confirmar' o toca dos veces el botón verde sobre el puerto de carga para continuar. Di 'Corregir' para volver a dictar."
    const val READBACK_CORREGIR  = "Entendido. Di el dato nuevamente. Te escucho."

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

    // ── USUARIO GENERAL / PADRES ──────────────────────────────────────────────

    const val ACCESIBILIDAD_INTRO =
        "Hola, bienvenido a Nutr IA, tu asistente de nutrición y salud familiar. " +
                "Soy una nutria muy aplicada y me encanta ayudarte. " +
                "Antes de empezar, cuéntame cómo usas tu teléfono para adaptarme a ti. " +
                "En el centro de la pantalla tienes tres opciones de arriba a abajo: " +
                "Primera: Estándar, para uso regular. " +
                "Segunda: Modo para condición visual, donde yo leo todo en voz alta, te oriento con respecto al puerto de carga y botones físicos, y activo el micrófono solo. " +
                "Tercera: Modo para condición auditiva, con teclado siempre visible. " +
                "Toca dos veces sobre la opción que va contigo. " +
                "Debajo puedes elegir el idioma de mi voz. " +
                "Al finalizar, toca dos veces el botón verde Continuar, ubicado centrado en la parte inferior, un centímetro arriba del puerto de carga."

    const val IDIOMA_INTRO =
        "Selector de idioma de voz en el centro de la pantalla. " +
                "Puedes elegir entre Español Latinoamérica, Español de Estados Unidos o Inglés."

    const val LOGIN_INTRO =
        "Pantalla de Inicio de Sesión de Nutr IA. " +
                "En la esquina superior izquierda, junto al altavoz de llamadas, está el botón Atrás. " +
                "En el centro de la pantalla tienes dos campos: primero tu correo electrónico y debajo tu contraseña. " +
                "Hacia la derecha está el enlace para recuperar tu contraseña. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón verde Entrar. " +
                "Y justo debajo, pegado al borde del puerto de carga, el botón para Crear una cuenta nueva."

    const val LOGIN_INICIANDO    = "Un momento, estoy verificando tus credenciales de acceso."
    const val LOGIN_EXITO        = "Bienvenido. Acceso correcto. Abriendo tu panel principal."
    const val LOGIN_CAMPO_CORREO = "Campo de Correo Electrónico en el centro. El micrófono se activará solo. Di tu correo."
    const val LOGIN_CAMPO_CLAVE  = "Campo de Contraseña en el centro. El micrófono se activará solo. Di tu clave de acceso."

    const val BIOMETRICOS_INTRO =
        "Activación de acceso biométrico. " +
                "En el centro de la pantalla puedes colocar tu huella dactilar sobre el sensor del teléfono o mirar a la cámara frontal para reconocimiento facial. " +
                "A la izquierda del puerto de carga tienes el botón Omitir por ahora, y a la derecha el botón Activar Biometría."

    const val REGISTRO_TIPO_INTRO =
        "Registro de Cuenta nueva. Cuéntame cómo vas a usar Nutr IA. " +
                "Tienes cuatro opciones organizadas en el centro de la pantalla de arriba a abajo: " +
                "Primera: Soy Padre o Madre, para registrar a tu hijo y llevar su nutrición y crecimiento. " +
                "Segunda: Mamá Primeriza, para acompañamiento durante tu embarazo. " +
                "Tercera: Soy Nutriólogo o Nutrióloga, para gestionar pacientes y crear planes nutricionales. " +
                "Cuarta: Soy Ginecólogo o Ginecóloga, para seguimiento médico prenatal. " +
                "Toca dos veces la opción que corresponda contigo. " +
                "En la esquina superior izquierda tienes el botón Atrás, y en la base sobre el puerto de carga el botón 'Iniciar sesión' si ya tienes cuenta."

    const val REGISTRO_PADRE_INTRO =
        "Registro de perfil familiar. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes los campos de información guiada de arriba hacia abajo: nombre completo, teléfono de diez dígitos, correo, contraseña y el nombre de tu pequeño. " +
                "Al completar, toca dos veces el botón verde 'Crear cuenta y continuar', ubicado fijo en la parte inferior central, un centímetro arriba del puerto de carga."

    const val REGISTRO_PADRE_EXITO =
        "¡Excelente! Tu cuenta está lista. Ahora vamos a registrar los datos de salud de tu pequeño."

    const val DASHBOARD_INTRO =
        "Panel Principal de Nutr IA. " +
                "En la parte superior, bajo la cámara frontal, está tu saludo, perfil y selector de hijos. " +
                "En la esquina superior derecha encuentras el botón de Notificaciones y Alertas. " +
                "En la zona central tienes las tarjetas de acceso rápido: Lactancia, Alimentación Sólida, Crecimiento Infantil y Alertas de Salud. " +
                "En el borde inferior, justo sobre el puerto de carga, tienes la barra de navegación con cuatro pestañas fijas de izquierda a derecha: Inicio, Nutrición, Citas y Configuración. " +
                "Toca dos veces sobre cualquier tarjeta para ingresar."

    const val CRECIMIENTO_INTRO =
        "Módulo de Crecimiento Infantil. " +
                "En la esquina superior izquierda, junto al auricular de llamadas, está el botón Atrás. " +
                "En el centro de la pantalla se muestra el resumen de percentiles de la Organización Mundial de la Salud con el peso y talla actuales. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, encuentras el botón flotante verde 'Registrar Peso y Talla'. Toca dos veces para agregar una nueva medición."

    const val LACTANCIA_INTRO =
        "Módulo de Lactancia Materna. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes el cronómetro con los selectores Pecho Izquierdo y Pecho Derecho. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, está el botón verde para iniciar, pausar o guardar la toma actual. Toca dos veces para registrar."

    const val SOLIDOS_INTRO =
        "Módulo de Alimentación Complementaria y Sólidos. " +
                "En la esquina superior izquierda encuentras el botón Atrás. " +
                "En el centro puedes consultar los alimentos introducidos, recetas por etapa, plan semanal y registro de alergias. " +
                "En la parte inferior central tienes el botón flotante naranja 'Registrar alimento', un centímetro arriba del puerto de carga. Toca dos veces para registrar un nuevo alimento probado."

    const val NUTRIENTES_INTRO =
        "Módulo de Micronutrientes y Vitaminas. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes el semáforo nutricional diario de hierro, zinc, calcio y vitaminas clave A, C y D. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón para anotar lo que comió hoy tu pequeño."

    const val SUENO_INTRO =
        "Módulo de Registro de Sueño Infantil. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes ver el total de horas de siestas diurnas y sueño nocturno. " +
                "En la parte inferior central, arriba del puerto de carga, tienes el botón verde para registrar una nueva sesión de sueño."

    const val RECORDATORIOS_INTRO =
        "Módulo de Recordatorios y Alertas de Salud. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se listan las vacunas, citas médicas y tomas programadas. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón para agregar una nueva alerta."

    const val ALERTAS_INTRO = RECORDATORIOS_INTRO

    const val PEDIATRA_INTRO =
        "Directorio de Nutriólogos y Pediatras. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes consultar tu especialista vinculado, o buscarlo por código, correo y directorio público. " +
                "En cada tarjeta de especialista tienes botones para agendar cita o iniciar llamada."

    const val AYUDA_INTRO =
        "Centro de Ayuda y Soporte de Nutr IA. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes respuestas a preguntas frecuentes, guía táctil de uso y botón de contacto directo con soporte técnico sobre el puerto de carga."

    const val CONFIGURACION_INTRO =
        "Pantalla de Configuración y Accesibilidad. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes cambiar el modo de accesibilidad, idioma de voz, alertas y sonido. " +
                "Al final de la lista, en la parte inferior, encuentras la opción de Cerrar sesión."

    const val SALIR_CONFIRMACION =
        "Diálogo de confirmación para cerrar sesión. " +
                "En el centro se pregunta si deseas salir de tu cuenta. " +
                "A la izquierda del puerto de carga tienes el botón Cancelar, y a la derecha del puerto de carga el botón rojo Cerrar Sesión."

    const val DIARIO_VISUAL_INTRO =
        "Módulo de Análisis de Alimentos con Inteligencia Artificial. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En la parte inferior central, arriba del puerto de carga, presiona el botón Escanear Alimento para abrir la cámara con guía de voz y asistente de plato."

    const val CITAS_INTRO =
        "Directorio de Especialistas y Teleconsultas. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes explorar pediatras, nutriólogos y ginecólogos vinculados. " +
                "En cada tarjeta de especialista tienes el botón 'Iniciar Llamada' o 'Agendar Cita'. Toca dos veces para conectar."

    const val NUTRICAT_INTRO =
        "NutriChat con Asistente Inteligente. " +
                "En la esquina superior izquierda tienes el botón Atrás. " +
                "En el centro se leen los mensajes de la conversación. " +
                "En la parte inferior está el campo de texto. A la izquierda del puerto de carga tienes el botón de micrófono para dictar tus dudas y a la derecha el botón verde Enviar."

    const val QUIZ_BIENVENIDA =
        "Evaluación inicial de tu pequeño. " +
                "Te pediré cuatro datos clave: nombre, fecha de nacimiento, peso y talla, y condiciones especiales de salud. " +
                "Toca dos veces el botón verde Continuar, ubicado en la parte inferior central, un centímetro arriba del puerto de carga para comenzar."

    const val QUIZ_NOMBRE =
        "Primer dato: ¿Cómo se llama tu hijo o hija? Di su nombre completo. El micrófono se activará en automático."

    const val QUIZ_FECHA =
        "Segundo dato: Fecha de nacimiento. Di día, mes y año. Por ejemplo: quince de marzo de dos mil veintitrés. El micrófono se activará solo."

    const val QUIZ_MEDIDAS =
        "Tercer dato: Medidas actuales. Di primero el peso en kilos y luego la talla en centímetros. El micrófono se activará solo."

    const val QUIZ_CONDICIONES =
        "Cuarto dato: Condiciones especiales. En el centro de la pantalla hay dos interruptores: Alergias Alimentarias y Condición Especial. Toca dos veces sobre ellos para activarlos o desactivarlos."

    // ── MÓDULO PEDIATRA / NUTRIÓLOGO ──────────────────────────────────────────

    const val REGISTRO_NUTRI_INTRO =
        "Registro de perfil profesional de nutrición y pediatría. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro de la pantalla ingresa tu nombre, teléfono, cédula profesional, correo y contraseña. " +
                "Al terminar, toca dos veces el botón verde 'Crear perfil profesional', ubicado fijo un centímetro arriba del puerto de carga."

    const val REGISTRO_NUTRI_EXITO =
        "Bienvenido colega. Tu perfil profesional está activo en la red de especialistas de Nutr IA."

    const val REGISTRO_ERROR_CAMPOS =
        "Atención: Hay campos pendientes o con formato incorrecto. Revisa los campos señalados en el centro de la pantalla."

    const val DASHBOARD_NUTRI_INTRO =
        "Panel Profesional para Nutriólogos y Pediatras. " +
                "En la parte superior tienes tu perfil y resumen de consultas del día. " +
                "En la zona central puedes consultar la lista de pacientes infantiles activos, alertas nutricionales y solicitudes de vinculación. " +
                "En el borde inferior sobre el puerto de carga tienes la barra de navegación con Pacientes, Planes y Ajustes."

    const val PACIENTES_NUTRI_INTRO =
        "Directorio de Pacientes Pediátricos. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se listan tus pacientes asignados con su edad y estado de crecimiento. " +
                "En la parte inferior central, arriba del puerto de carga, tienes el botón 'Vincular nuevo paciente'."

    const val EXPEDIENTE_PACIENTE_INTRO =
        "Expediente Clínico del Paciente. " +
                "En la esquina superior izquierda encuentras el botón Atrás. " +
                "En el centro puedes revisar la curva de percentiles OMS, registro de alimentos introducidos y posibles alergias. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón 'Agregar Nota Clínica o Plan Dietético'."

    // ── MÓDULO EMBARAZO (GESTANTE) ────────────────────────────────────────────

    const val QUIZ_EMBARAZO_INTRO =
        "Evaluación inicial de tu embarazo. " +
                "Te pediré datos clave: semanas de gestación, fecha probable de parto, condiciones médicas y síntomas actuales. " +
                "Toca dos veces el botón verde Continuar, ubicado un centímetro arriba del puerto de carga para comenzar."

    const val DASHBOARD_EMBARAZO_INTRO =
        "Panel Principal de Acompañamiento en el Embarazo. " +
                "En la parte superior tienes tus semanas de gestación y tamaño comparativo de tu bebé. " +
                "En el centro tienes las tarjetas de acceso rápido: Nutrición Materna, Control de Peso, Registro de Síntomas y Citas Prenatales. " +
                "En el borde inferior sobre el puerto de carga está la barra de navegación con Inicio, Nutrición, Citas y Ajustes."

    const val AYUDA_EMBARAZO_INTRO =
        "Centro de Ayuda y Guía Materno-Infantil. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro tienes información sobre señales de alarma obstétrica, nutrición trimestral y contacto con tu ginecólogo."

    const val AJUSTES_EMBARAZO_INTRO = CONFIGURACION_INTRO

    const val GINECOLOGO_VINCULADO_INTRO =
        "Mi Ginecólogo y Equipo Obstétrico. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes consultar los datos de tu médico vinculado, cédula y horario de atención. " +
                "En la parte inferior central sobre el puerto de carga tienes el botón 'Llamar a Emergencias Obstétricas' o 'Agendar Consulta'."

    const val NUTRICION_EMBARAZO_INTRO =
        "Módulo de Nutrición Materna y Embarazo. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se describen las guías de ácido fólico, hierro, hidratación y energía recomendada por trimestre. " +
                "En la parte inferior central, arriba del puerto de carga, tienes el botón para registrar tus alimentos del día."

    const val CONTROL_PESO_EMBARAZO_INTRO =
        "Módulo de Control de Peso Gestacional. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se muestra la gráfica de ganancia de peso según tu índice de masa corporal inicial. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón 'Registrar Nuevo Peso'."

    const val SINTOMAS_EMBARAZO_INTRO =
        "Módulo de Registro de Síntomas Prenatales. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se listan los síntomas comunes como náuseas, reflujo, fatiga o contracciones. " +
                "En la parte inferior central, arriba del puerto de carga, tienes el botón 'Registrar Síntoma o Alerta'."

    const val CITAS_EMBARAZO_INTRO =
        "Módulo de Citas y Teleconsultas Prenatales. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro puedes consultar tus revisiones médicas, ultrasonidos agendados y notas del especialista. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón 'Agendar Nueva Cita'."

    const val PREGUNTAME_EMBARAZO_INTRO =
        "NutriBot Asistente de Embarazo. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se leen las respuestas sobre síntomas, nutrición y seguridad alimentaria en la gestación. " +
                "En la parte inferior está el campo de texto. A la izquierda del puerto de carga tienes el botón de micrófono y a la derecha el botón Enviar."

    const val RECORDATORIOS_EMBARAZO_INTRO =
        "Recordatorios de Suplementos y Medicamentos Prenatales. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se listan tus tomas de ácido fólico, hierro, calcio y citas médicas. " +
                "En la parte inferior central, arriba del puerto de carga, tienes el botón 'Agregar Nuevo Recordatorio'."

    // ── MÓDULO GINECÓLOGO ─────────────────────────────────────────────────────

    const val REGISTRO_GINEC_INTRO =
        "Registro de perfil profesional de Ginecología y Obstetricia. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro de la pantalla ingresa tu nombre completo, teléfono, cédula de especialidad, hospital o clínica, correo y contraseña. " +
                "Al terminar, toca dos veces el botón verde 'Crear perfil de especialista', ubicado un centímetro arriba del puerto de carga."

    const val DASHBOARD_GINEC_INTRO =
        "Panel Médico de Control Obstétrico y Ginecológico. " +
                "En la parte superior tienes tu resumen de pacientes gestantes y consultas del día. " +
                "En la zona central puedes consultar las pacientes por trimestre de gestación y alertas de riesgo prenatal. " +
                "En el borde inferior sobre el puerto de carga tienes la barra de navegación con Pacientes, Consultas y Ajustes."

    const val PACIENTES_GINEC_INTRO =
        "Directorio de Pacientes Gestantes. " +
                "En la esquina superior izquierda está el botón Atrás. " +
                "En el centro se listan tus pacientes con sus semanas de embarazo y fecha probable de parto. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón 'Vincular nueva paciente'."

    const val EXPEDIENTE_PRENATAL_INTRO =
        "Expediente Clínico Prenatal de la Paciente. " +
                "En la esquina superior izquierda encuentras el botón Atrás. " +
                "En el centro puedes revisar el historial de ultrasonidos, ganancia de peso materno, presión arterial y síntomas reportados. " +
                "En la parte inferior central, un centímetro arriba del puerto de carga, tienes el botón 'Agregar Nota de Consulta o Ultrasonido'."

    // ── COMANDOS DE VOZ Y FEEDBACK ────────────────────────────────────────────

    const val VOZ_ESCUCHANDO  = "Micrófono abierto. Te escucho."
    const val VOZ_PROCESANDO  = "Procesando lo que dijiste."
    const val VOZ_LISTO       = "Información capturada correctamente."
    const val VOZ_ERROR_MIC   = "No te escuché con claridad. Toca dos veces el botón de micrófono a la izquierda del puerto de carga o usa el teclado Braille."
    const val VOZ_SIN_PERMISO = "Permiso de micrófono requerido. Ve a Configuración de tu dispositivo para activarlo."

    const val BRAILLE_INTRO =
        "Teclado Braille Virtual de seis puntos activado en la mitad inferior de la pantalla. " +
                "Columna izquierda de arriba a abajo: puntos uno, dos y tres. " +
                "Columna derecha de arriba a abajo: puntos cuatro, cinco y seis. " +
                "Toca los puntos de tu letra y presiona el botón central 'Agregar letra' para escribirla. " +
                "El botón Borrar está a la izquierda y Espacio a la derecha."

    const val BTN_ATRAS      = "Botón Atrás, ubicado en la esquina superior izquierda junto al altavoz. Toca dos veces para regresar a la pantalla anterior."
    const val BTN_CONTINUAR  = "Botón verde Continuar, ubicado centrado un centímetro arriba del puerto de carga. Toca dos veces para avanzar."
    const val BTN_FINALIZAR  = "Botón verde Guardar y Finalizar, ubicado sobre el puerto de carga. Toca dos veces para guardar."
    const val BTN_MICROFONO  = "Botón de micrófono, ubicado a la izquierda del puerto de carga. Toca dos veces para dictar por voz."
}

// ─── Textos de voz — English ──────────────────────────────────────────────────
object VozEn {

    const val READBACK_CONFIRMAR = "Captured: %s. Say 'Confirm' or double tap the green button above the charging port to proceed. Say 'Retry' to dictate again."
    const val READBACK_CORREGIR  = "Understood. Please say it again. I'm listening."

    const val MODO_CIEGO =
        "Hello, I'm NutriIA, your favorite nutrition assistant. " +
                "Visual accessibility mode is now active. " +
                "I will provide spatial orientation referencing your phone's physical hardware: charging port, corners, and physical edges, reading everything aloud with automatic microphone dictation. " +
                "Together we will take great care of your family's nutrition and health."

    const val MODO_MUDO =
        "Hearing condition mode activated. The visual keyboard will always remain visible."

    const val MODO_NORMAL =
        "Standard mode activated. Welcome to NutriIA."

    const val PRUEBA_VOZ =
        "Hello, I'm NutriIA. I'm a very dedicated assistant and I'm here to support you. " +
                "Together we will guide your family's health and nutrition."

    // ── GENERAL USER / PARENTS ────────────────────────────────────────────────

    const val ACCESIBILIDAD_INTRO =
        "Welcome to NutriIA, your family nutrition and health assistant. " +
                "Before we begin, choose your preferred interaction mode. " +
                "In the center of the screen you have three options from top to bottom: " +
                "First: Standard, for regular use. " +
                "Second: Visual accessibility mode, with spatial guidance referencing the charging port and physical buttons, full voice narration, and automatic microphone dictation. " +
                "Third: Hearing accessibility mode, with persistent visual keyboard. " +
                "Double tap the option that suits you best. " +
                "Below the modes you can select my voice language. " +
                "When ready, double tap the green Continue button located at the bottom center, one centimeter above the charging port."

    const val IDIOMA_INTRO =
        "Voice language selector in the center of the screen. " +
                "Choose between Latin American Spanish, US Spanish, or English."

    const val LOGIN_INTRO =
        "NutriIA Sign In screen. " +
                "In the top left corner, next to the earpiece speaker, is the Back button. " +
                "In the center of the screen you have two fields: email address at the top and password below. " +
                "Towards the right is the Forgot Password link. " +
                "At the bottom center, one centimeter above the charging port, is the green Sign In button. " +
                "And right below it, near the charging port edge, the Create Account button."

    const val LOGIN_INICIANDO    = "Verifying your account credentials. Please hold on."
    const val LOGIN_EXITO        = "Access granted. Loading your dashboard."
    const val LOGIN_CAMPO_CORREO = "Email address field in the center. Microphone will open automatically. Say your email."
    const val LOGIN_CAMPO_CLAVE  = "Password field in the center. Microphone will open automatically. Say your password."

    const val BIOMETRICOS_INTRO =
        "Biometric Authentication Setup. " +
                "Place your finger on the phone's fingerprint sensor or look at the front camera for facial recognition. " +
                "To the left of the charging port is the Skip button, and to the right is the Enable Biometrics button."

    const val REGISTRO_TIPO_INTRO =
        "New Account Registration. Select your role. " +
                "You have four options arranged vertically in the center: " +
                "First: Parent, to register your child and monitor nutrition and growth. " +
                "Second: Expectant Mom, for pregnancy tracking. " +
                "Third: Nutritionist, to manage patients and design meal plans. " +
                "Fourth: Gynecologist, for prenatal medical care. " +
                "Double tap your choice. " +
                "In the top left corner is the Back button, and at the bottom above the charging port is the Sign In button."

    const val REGISTRO_PADRE_INTRO =
        "Family Profile Registration. " +
                "In the top left corner is the Back button. " +
                "In the center are the guided fields from top to bottom: full name, 10-digit phone number, email, password, and your child's name. " +
                "When finished, double tap the green 'Create Account and Continue' button fixed at the bottom center, one centimeter above the charging port."

    const val REGISTRO_PADRE_EXITO =
        "Account created successfully. Now let's set up your child's health profile."

    const val DASHBOARD_INTRO =
        "NutriIA Main Dashboard. " +
                "At the top under the front camera you find your profile and child selector. " +
                "In the top right corner is the Alerts and Notifications button. " +
                "In the center area are your quick access cards: Breastfeeding, Solid Foods, Growth Tracking, and Health Alerts. " +
                "At the bottom edge, right above the charging port, is the fixed navigation bar with four tabs from left to right: Home, Nutrition, Appointments, and Settings. " +
                "Double tap any card to open."

    const val CRECIMIENTO_INTRO =
        "Child Growth Module. " +
                "In the top left corner, next to the call speaker, is the Back button. " +
                "In the center is the WHO percentile chart displaying current weight and height metrics. " +
                "In the bottom center, one centimeter above the charging port, is the green floating action button 'Record Weight and Height'. Double tap to log a new entry."

    const val LACTANCIA_INTRO =
        "Breastfeeding Module. " +
                "In the top left is the Back button. " +
                "In the center is the feeding timer with Left Breast and Right Breast buttons. " +
                "In the lower center, one centimeter above the charging port, is the green button to start, pause, or save the session. Double tap to activate."

    const val SOLIDOS_INTRO =
        "Complementary Feeding and Solids Module. " +
                "In the top left is the Back button. " +
                "In the center you can check introduced foods, stage-appropriate recipes, weekly plan, and allergy records. " +
                "In the bottom center, one centimeter above the charging port, is the orange floating button 'Register food'. Double tap to log a new tasted food."

    const val NUTRIENTES_INTRO =
        "Micronutrients and Vitamins Module. " +
                "In the top left is the Back button. " +
                "In the center is the daily nutritional indicator for iron, zinc, calcium, and key vitamins A, C, and D. " +
                "In the lower center, one centimeter above the charging port, is the button to log today's meals."

    const val SUENO_INTRO =
        "Infant Sleep Tracking Module. " +
                "In the top left is the Back button. " +
                "In the center you can view total daytime nap and nighttime sleep hours. " +
                "At the bottom center, above the charging port, is the green button to log a new sleep session."

    const val RECORDATORIOS_INTRO =
        "Health Reminders and Alerts Module. " +
                "In the top left is the Back button. " +
                "In the center are scheduled vaccines, appointments, and feedings. " +
                "At the bottom center, one centimeter above the charging port, is the button to add a new alert."

    const val ALERTAS_INTRO = RECORDATORIOS_INTRO

    const val PEDIATRA_INTRO =
        "My Nutritionist and Pediatrician screen. " +
                "In the top left is the Back button. " +
                "In the center you can manage your linked specialist, or search by code, email, and public directory. " +
                "On each card are buttons to book an appointment or start a call."

    const val AYUDA_INTRO =
        "Help and Support Center. " +
                "In the top left is the Back button. " +
                "In the center you can find FAQ answers, tactile usage guides, and the direct technical support button above the charging port."

    const val CONFIGURACION_INTRO =
        "Settings and Accessibility. " +
                "In the top left is the Back button. " +
                "In the center you can customize accessibility modes, voice language, alerts, and sounds. " +
                "At the end of the list at the bottom is the Sign Out option."

    const val SALIR_CONFIRMACION =
        "Sign Out Confirmation Dialog. " +
                "In the center you are asked to confirm signing out. " +
                "To the left of the charging port is the Cancel button, and to the right of the charging port is the red Sign Out button."

    const val DIARIO_VISUAL_INTRO =
        "AI Food Nutrition Analysis Module. " +
                "In the top left is the Back button. " +
                "At the bottom center, above the charging port, press the Scan Food button to open the camera with voice plate guidance."

    const val CITAS_INTRO =
        "Specialists Directory and Telehealth. " +
                "In the top left is the Back button. " +
                "In the center you can browse connected pediatricians, nutritionists, and gynecologists. " +
                "On each specialist card is the 'Start Call' or 'Book Appointment' button. Double tap to connect."

    const val NUTRICAT_INTRO =
        "NutriChat AI Assistant. " +
                "In the top left is the Back button. " +
                "In the center are conversation messages. " +
                "At the bottom is the text input. To the left of the charging port is the microphone button to dictate questions, and to the right is the green Send button."

    const val QUIZ_BIENVENIDA =
        "Initial Child Assessment. " +
                "I will ask for four key details: name, date of birth, current weight and height, and special health conditions. " +
                "Double tap the green Continue button located at the bottom center, one centimeter above the charging port to start."

    const val QUIZ_NOMBRE =
        "First step: What is your child's full name? The microphone will open automatically."

    const val QUIZ_FECHA =
        "Second step: Date of birth. Say day, month, and year. For example: March 15th 2023. The microphone will open automatically."

    const val QUIZ_MEDIDAS =
        "Third step: Current measurements. Say weight in kilograms first, then height in centimeters. The microphone will open automatically."

    const val QUIZ_CONDICIONES =
        "Fourth step: Special conditions. In the center are two switches: Food Allergies and Special Health Condition. Double tap to toggle."

    // ── PEDIATRICIAN / NUTRITIONIST MODULE ────────────────────────────────────

    const val REGISTRO_NUTRI_INTRO =
        "Professional Nutrition and Pediatric Profile Registration. " +
                "In the top left corner is the Back button. " +
                "Enter your full name, phone number, professional license, email, and password in the center fields. " +
                "Double tap the green 'Create Professional Profile' button fixed one centimeter above the charging port."

    const val REGISTRO_NUTRI_EXITO =
        "Welcome to the team. Your professional profile is now active on NutriIA."

    const val REGISTRO_ERROR_CAMPOS =
        "Attention: Some fields are incomplete or invalid. Please check the highlighted fields in the center."

    const val DASHBOARD_NUTRI_INTRO =
        "Professional Dashboard for Nutritionists and Pediatricians. " +
                "At the top you have your profile and today's consultation summary. " +
                "In the center area you can review active pediatric patients, nutritional alerts, and connection requests. " +
                "At the bottom edge above the charging port is the navigation bar with Patients, Meal Plans, and Settings."

    const val PACIENTES_NUTRI_INTRO =
        "Pediatric Patients Directory. " +
                "In the top left is the Back button. " +
                "In the center are your assigned patients with age and growth status. " +
                "At the bottom center, above the charging port, is the 'Link New Patient' button."

    const val EXPEDIENTE_PACIENTE_INTRO =
        "Patient Clinical Record. " +
                "In the top left is the Back button. " +
                "In the center you can check WHO percentile curves, introduced foods, and allergy logs. " +
                "At the bottom center, one centimeter above the charging port, is the 'Add Clinical Note or Meal Plan' button."

    // ── PREGNANCY MODULE (EXPECTANT MOM) ──────────────────────────────────────

    const val QUIZ_EMBARAZO_INTRO =
        "Initial Pregnancy Assessment. " +
                "I will ask for key details: gestational weeks, estimated due date, medical conditions, and current symptoms. " +
                "Double tap the green Continue button located one centimeter above the charging port to start."

    const val DASHBOARD_EMBARAZO_INTRO =
        "Pregnancy Tracking Main Dashboard. " +
                "At the top you see your current gestational weeks and baby size comparison. " +
                "In the center area are quick access cards: Maternal Nutrition, Weight Tracking, Symptom Log, and Prenatal Appointments. " +
                "At the bottom edge above the charging port is the navigation bar with Home, Nutrition, Appointments, and Settings."

    const val AYUDA_EMBARAZO_INTRO =
        "Maternal and Child Care Help Center. " +
                "In the top left is the Back button. " +
                "In the center you have obstetric warning signs, trimester nutrition guides, and direct contact with your gynecologist."

    const val AJUSTES_EMBARAZO_INTRO = CONFIGURACION_INTRO

    const val GINECOLOGO_VINCULADO_INTRO =
        "My Gynecologist and Obstetric Care Team. " +
                "In the top left is the Back button. " +
                "In the center you can review your linked doctor's details and office hours. " +
                "At the bottom center above the charging port are buttons to Call Obstetric Emergency or Book Consultation."

    const val NUTRICION_EMBARAZO_INTRO =
        "Maternal Nutrition and Pregnancy Module. " +
                "In the top left is the Back button. " +
                "In the center are trimester guidelines for folic acid, iron, hydration, and recommended energy intake. " +
                "At the bottom center, above the charging port, is the button to log today's meals."

    const val CONTROL_PESO_EMBARAZO_INTRO =
        "Gestational Weight Tracking Module. " +
                "In the top left is the Back button. " +
                "In the center is the weight gain curve according to your pre-pregnancy BMI. " +
                "At the bottom center, one centimeter above the charging port, is the 'Log New Weight' button."

    const val SINTOMAS_EMBARAZO_INTRO =
        "Prenatal Symptom Tracking Module. " +
                "In the top left is the Back button. " +
                "In the center are common symptoms such as nausea, heartburn, fatigue, or contractions. " +
                "At the bottom center, above the charging port, is the 'Log Symptom or Alert' button."

    const val CITAS_EMBARAZO_INTRO =
        "Prenatal Appointments and Telehealth Module. " +
                "In the top left is the Back button. " +
                "In the center you can check scheduled checkups, ultrasounds, and specialist notes. " +
                "At the bottom center, one centimeter above the charging port, is the 'Book New Appointment' button."

    const val PREGUNTAME_EMBARAZO_INTRO =
        "NutriBot Pregnancy Assistant. " +
                "In the top left is the Back button. " +
                "In the center are answers on symptoms, nutrition, and food safety during pregnancy. " +
                "At the bottom is the text input. To the left of the charging port is the microphone button and to the right is the Send button."

    const val RECORDATORIOS_EMBARAZO_INTRO =
        "Prenatal Medication and Supplement Reminders. " +
                "In the top left is the Back button. " +
                "In the center are your scheduled doses of folic acid, iron, calcium, and medical appointments. " +
                "At the bottom center, above the charging port, is the 'Add New Reminder' button."

    // ── GYNECOLOGIST MODULE ───────────────────────────────────────────────────

    const val REGISTRO_GINEC_INTRO =
        "Gynecology and Obstetrics Professional Registration. " +
                "In the top left is the Back button. " +
                "Enter your full name, phone number, medical license, clinic or hospital, email, and password in the center. " +
                "When finished, double tap the green 'Create Specialist Profile' button one centimeter above the charging port."

    const val DASHBOARD_GINEC_INTRO =
        "Gynecological and Obstetric Medical Dashboard. " +
                "At the top is your active pregnant patients summary and daily appointments. " +
                "In the center area you can review patients by trimester and prenatal risk alerts. " +
                "At the bottom edge above the charging port is the navigation bar with Patients, Consultations, and Settings."

    const val PACIENTES_GINEC_INTRO =
        "Pregnant Patients Directory. " +
                "In the top left is the Back button. " +
                "In the center are your patients with their current gestational weeks and estimated delivery dates. " +
                "At the bottom center, one centimeter above the charging port, is the 'Link New Patient' button."

    const val EXPEDIENTE_PRENATAL_INTRO =
        "Prenatal Patient Medical Record. " +
                "In the top left is the Back button. " +
                "In the center you can review ultrasound logs, maternal weight gain, blood pressure, and reported symptoms. " +
                "At the bottom center, one centimeter above the charging port, is the 'Add Consultation Note or Ultrasound' button."

    // ── VOICE COMMANDS AND BUTTON ANCHORS ─────────────────────────────────────

    const val VOZ_ESCUCHANDO  = "Microphone open. Listening to you."
    const val VOZ_PROCESANDO  = "Processing your voice."
    const val VOZ_LISTO       = "Information captured successfully."
    const val VOZ_ERROR_MIC   = "I didn't hear you clearly. Double tap the microphone button to the left of the charging port or use the virtual Braille keyboard."
    const val VOZ_SIN_PERMISO = "Microphone permission required. Please enable it in your device Settings."

    const val BRAILLE_INTRO =
        "Virtual 6-dot Braille Keyboard active in the lower half of the screen. " +
                "Left column from top to bottom: dots one, two, and three. " +
                "Right column from top to bottom: dots four, five, and six. " +
                "Tap your letter dots and press the center 'Add Letter' button to confirm. " +
                "Delete button is on the left and Space on the right."

    const val BTN_ATRAS      = "Back button, located in the top left corner next to the call speaker. Double tap to return to the previous screen."
    const val BTN_CONTINUAR  = "Green Continue button, fixed one centimeter above the charging port. Double tap to proceed."
    const val BTN_FINALIZAR  = "Green Save and Finish button, located above the charging port. Double tap to save."
    const val BTN_MICROFONO  = "Microphone button, located to the left of the charging port. Double tap to dictate by voice."
}