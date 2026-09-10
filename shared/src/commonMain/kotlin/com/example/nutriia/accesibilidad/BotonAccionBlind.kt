package com.example.nutriia.accesibilidad

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.hypot
import kotlin.math.min

/**
 * Motor Háptico Universal de NutrIA (Android, iOS y Desktop).
 */
object MotorHapticoNutriIA {

    fun vibrarFuerte(context: Any? = null, duracionMs: Long = 55L, amplitud: Int = 255) {
        PlatformHaptic.vibrarFuerte(duracionMs, amplitud)
    }

    fun vibrarBordeOEsquina(context: Any? = null) {
        PlatformHaptic.vibrarBordeOEsquina()
    }

    fun vibrarRadarProximidad(context: Any? = null, factorProximidad: Float) {
        PlatformHaptic.vibrarRadarProximidad(factorProximidad)
    }

    fun vibrarLlegadaBoton(context: Any? = null, view: Any? = null) {
        PlatformHaptic.vibrarLlegadaBoton()
    }
}

/**
 * Genera el mensaje universal de orientación espacial para botones inferiores
 * con anclaje táctil en el borde físico y puerto de carga.
 */
fun orientacionBotonInferior(
    accion: String,
    idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX
): String {
    return if (idioma == IdiomaVoz.INGLES) {
        "The button to $accion is located at the bottom center, just above the charging port. Double tap to activate."
    } else {
        "El botón para $accion se encuentra en la parte inferior central, justo arriba del puerto de carga. Toca dos veces para activar."
    }
}

/**
 * Genera el mensaje de orientación para la barra de entrada del chatbot.
 */
fun orientacionChatbot(idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX): String {
    return if (idioma == IdiomaVoz.INGLES) {
        "The message field is at the bottom, just above the charging port. To the right is the Send button, and to the left is the Voice dictation button."
    } else {
        "El campo para escribir o dictar tu mensaje está en la parte inferior, justo arriba del puerto de carga. A la derecha está el botón Enviar y a la izquierda el botón de Dictado por voz."
    }
}

/**
 * Dispara feedback táctil y sonoro en Compose.
 */
fun triggerFeedbackAccesible(haptic: HapticFeedback?, view: Any? = null) {
    try {
        haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
    } catch (_: Throwable) {}
    MotorHapticoNutriIA.vibrarLlegadaBoton()
}

/**
 * Dispara feedback táctil con máxima amplitud y sonido de clic del sistema.
 */
fun triggerFeedbackAccesible(context: Any? = null, view: Any? = null) {
    MotorHapticoNutriIA.vibrarLlegadaBoton(context, view)
}

/**
 * Modificador de Radar Háptico de Guía Espacial exclusivo para el modo BLIND.
 * Permite al usuario tocar o deslizar el dedo desde esquinas/bordes hacia los botones
 * recibiendo pulsos de radar progresivos sin bloquear ni consumir los clics normales.
 */
fun Modifier.radarHapticoBlind(
    context: Any? = null,
    esBlind: Boolean,
    botonesObjetivo: List<Rect> = emptyList(),
    margenBordePx: Float = 80f
): Modifier {
    if (!esBlind) return this

    return this.pointerInput(esBlind, botonesObjetivo) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var currentPos = down.position
            evaluarPosicionRadar(context, currentPos, size.width.toFloat(), size.height.toFloat(), botonesObjetivo, margenBordePx)

            do {
                val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                val move = event.changes.firstOrNull()
                if (move != null && move.pressed) {
                    currentPos = move.position
                    evaluarPosicionRadar(context, currentPos, size.width.toFloat(), size.height.toFloat(), botonesObjetivo, margenBordePx)
                }
            } while (event.changes.any { it.pressed })
        }
    }
}

private fun evaluarPosicionRadar(
    context: Any?,
    pos: Offset,
    anchoTotal: Float,
    altoTotal: Float,
    botonesObjetivo: List<Rect>,
    margenBordePx: Float
) {
    if (anchoTotal <= 0 || altoTotal <= 0) return

    // 1. Detectar si el dedo está en los bordes o esquinas físicas
    val enBordeIzquierdo = pos.x <= margenBordePx
    val enBordeDerecho   = pos.x >= (anchoTotal - margenBordePx)
    val enBordeSuperior  = pos.y <= margenBordePx
    val enBordeInferior  = pos.y >= (altoTotal - margenBordePx)

    if (enBordeIzquierdo || enBordeDerecho || enBordeSuperior || enBordeInferior) {
        MotorHapticoNutriIA.vibrarBordeOEsquina(context)
    }

    // 2. Si no hay botones específicos registrados, calcular cercanía hacia el borde inferior (puerto de carga)
    if (botonesObjetivo.isEmpty()) {
        val cercaniaInferior = (pos.y / altoTotal).coerceIn(0f, 1f)
        if (cercaniaInferior > 0.4f) {
            // Entre más abajo esté el dedo, más rápido e intenso vibra el radar
            val factor = ((cercaniaInferior - 0.4f) / 0.6f).coerceIn(0f, 1f)
            MotorHapticoNutriIA.vibrarRadarProximidad(context, factor)
        }
        return
    }

    // 3. Evaluar distancia a los botones registrados
    var menorDistancia = Float.MAX_VALUE
    var dentroDeBoton = false

    for (rect in botonesObjetivo) {
        if (rect.contains(pos)) {
            dentroDeBoton = true
            break
        }
        val centroX = rect.center.x
        val centroY = rect.center.y
        val d = hypot(pos.x - centroX, pos.y - centroY)
        if (d < menorDistancia) menorDistancia = d
    }

    if (dentroDeBoton) {
        MotorHapticoNutriIA.vibrarRadarProximidad(context, 1.0f)
    } else {
        val radioMaximo = min(anchoTotal, altoTotal) * 0.75f
        if (menorDistancia < radioMaximo) {
            val factor = (1.0f - (menorDistancia / radioMaximo)).coerceIn(0f, 1f)
            MotorHapticoNutriIA.vibrarRadarProximidad(context, factor)
        }
    }
}

/**
 * Botón flotante accesible universal para pantallas principales en NutrIA.
 * Ofrece anclaje físico, vibración háptica, sonido de clic y anuncio por voz.
 */
@Composable
fun BotonFlotanteAccesible(
    texto: String,
    icono: ImageVector,
    colorFondo: Color,
    colorTexto: Color = Color.White,
    esBlind: Boolean,
    a11yVm: AccessibilityViewModel? = null,
    idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX,
    mensajeAnuncio: String? = null,
    onPositionChanged: ((Rect) -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alturaBoton = if (esBlind) 60.dp else 52.dp
    val paddingHorizontal = if (esBlind) 24.dp else 16.dp

    val descripcionSemantica = if (idioma == IdiomaVoz.INGLES) {
        "$texto. Button located at the bottom center, above the charging port. Double tap to activate."
    } else {
        "$texto. Botón ubicado en la parte inferior central, arriba del puerto de carga. Toca dos veces para activar."
    }

    ExtendedFloatingActionButton(
        onClick = {
            triggerFeedbackAccesible()
            if (esBlind) {
                val anuncio = mensajeAnuncio ?: if (idioma == IdiomaVoz.INGLES) {
                    "Opening form for $texto."
                } else {
                    "Abriendo formulario para $texto."
                }
                a11yVm?.hablar(anuncio)
            }
            onClick()
        },
        containerColor = colorFondo,
        contentColor   = colorTexto,
        shape          = RoundedCornerShape(if (esBlind) 24.dp else 20.dp),
        modifier       = modifier
            .height(alturaBoton)
            .shadow(
                elevation    = if (esBlind) 10.dp else 8.dp,
                shape        = RoundedCornerShape(if (esBlind) 24.dp else 20.dp),
                ambientColor = colorFondo.copy(alpha = 0.4f),
                spotColor    = colorFondo.copy(alpha = 0.4f)
            )
            .semantics {
                contentDescription = descripcionSemantica
            }
            .onGloballyPositioned { layoutCoordinates ->
                onPositionChanged?.invoke(layoutCoordinates.boundsInRoot())
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = paddingHorizontal - 16.dp)
        ) {
            Icon(
                imageVector        = icono,
                contentDescription = null,
                modifier           = Modifier.size(if (esBlind) 24.dp else 20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text       = texto,
                fontWeight = FontWeight.Bold,
                fontSize   = if (esBlind) 15.sp else 14.sp
            )
        }
    }
}

/**
 * Botón de confirmación / guardado accesible para formularios y diálogos.
 */
@Composable
fun BotonConfirmarAccesible(
    texto: String,
    icono: ImageVector? = null,
    colorFondo: Color,
    colorTexto: Color = Color.White,
    habilitado: Boolean = true,
    esBlind: Boolean = false,
    a11yVm: AccessibilityViewModel? = null,
    mensajeVozAlGuardar: String? = null,
    onPositionChanged: ((Rect) -> Unit)? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = {
            triggerFeedbackAccesible()
            if (esBlind && mensajeVozAlGuardar != null) {
                a11yVm?.hablar(mensajeVozAlGuardar)
            }
            onClick()
        },
        enabled  = habilitado,
        colors   = ButtonDefaults.buttonColors(
            containerColor = colorFondo,
            contentColor   = colorTexto
        ),
        shape    = RoundedCornerShape(14.dp),
        modifier = modifier
            .height(if (esBlind) 52.dp else 46.dp)
            .semantics {
                contentDescription = "$texto. Botón ubicado en la parte inferior. Toca dos veces para confirmar."
            }
            .onGloballyPositioned { layoutCoordinates ->
                onPositionChanged?.invoke(layoutCoordinates.boundsInRoot())
            }
    ) {
        if (icono != null) {
            Icon(icono, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(texto, fontWeight = FontWeight.Bold, fontSize = if (esBlind) 15.sp else 14.sp)
    }
}
