package com.example.nutriia.configuracion
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.NutriTTS
import com.example.nutriia.accesibilidad.abrirConfiguracionTalkBack
import com.example.nutriia.accesibilidad.isTalkBackActive

// ─────────────────────────────────────────────────────────────────────────────
// Privado: opciones de accesibilidad reutilizando la misma data class del quiz
// ─────────────────────────────────────────────────────────────────────────────

private data class A11yOption(val mode: AccessibilityMode, val icon: ImageVector, val color: Color)

private fun a11yOptions() = listOf(
    A11yOption(AccessibilityMode.NORMAL, Icons.Rounded.CheckCircle,             Color(0xFF4CAF50)),
    A11yOption(AccessibilityMode.BLIND,  Icons.Rounded.RemoveRedEye,            Color(0xFF9C8FE0)),
    A11yOption(AccessibilityMode.MUTE,   Icons.AutoMirrored.Rounded.VolumeOff,  Color(0xFF4DB6AC))
)

private val GreenConfig = Color(0xFF4CAF50)

// ═══════════════════════════════════════════════════════════════════════════════
// DIÁLOGO DE ACCESIBILIDAD
// ═══════════════════════════════════════════════════════════════════════════════

@Composable
fun CfgAccesibilidadDialog(
    modoActual:    AccessibilityMode,
    idiomaActual:  IdiomaVoz,
    ttsManager:    NutriTTS?,
        onModoChange:  (AccessibilityMode) -> Unit,
    onIdiomaChange:(IdiomaVoz) -> Unit,
    onDismiss:     () -> Unit
) {
    var modoLocal   by remember(modoActual)  { mutableStateOf(modoActual)   }
    var idiomaLocal by remember(idiomaActual){ mutableStateOf(idiomaActual) }
    var mostrarTalkBackInfo by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier  = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(containerColor = Color(0xFFF9F8F4)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(GreenConfig.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Accessibility,
                            contentDescription = null,
                            tint     = GreenConfig,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Accesibilidad", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))
                        Text("Adapta NutriIA a tus necesidades", fontSize = 12.sp, color = Color.Gray)
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Banner TalkBack detectado
                AnimatedVisibility(visible = false) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(GreenConfig.copy(alpha = 0.10f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint     = GreenConfig,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "TalkBack detectado — modo ciego recomendado.",
                            fontSize   = 12.sp,
                            color      = GreenConfig,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Selección de modo
                Text("Modo de uso", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))

                a11yOptions().forEach { option ->
                    val selected = option.mode == modoLocal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .border(
                                1.5.dp,
                                if (selected) GreenConfig else Color(0xFFE0E0E0),
                                RoundedCornerShape(14.dp)
                            )
                            .background(if (selected) GreenConfig.copy(alpha = 0.06f) else Color.White)
                            .clickable {
                                modoLocal = option.mode
                                if (option.mode == AccessibilityMode.BLIND && !false)
                                    mostrarTalkBackInfo = true
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(option.color.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                option.icon,
                                contentDescription = null,
                                tint     = if (selected) option.color else Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                option.mode.label,
                                fontSize   = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color      = if (selected) GreenConfig else Color(0xFF1B5E20)
                            )
                            Text(
                                option.mode.description,
                                fontSize = 11.sp,
                                color    = Color.Gray
                            )
                        }
                        if (selected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint     = GreenConfig,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Info TalkBack si elige modo ciego sin tenerlo
                AnimatedVisibility(
                    visible = mostrarTalkBackInfo,
                    enter   = fadeIn(tween(200)),
                    exit    = fadeOut(tween(200))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF3E5F5))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Activar TalkBack (opcional)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5E35B1))
                        Text(
                            "NutriIA ya tiene su propia voz y funciona sin TalkBack. " +
                                    "Solo actívalo si usas lector de pantalla del sistema.",
                            fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp
                        )
                        TextButton(
                            onClick        = {  },
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                        ) {
                            Text("Ir a configuración de Android →", fontSize = 11.sp, color = Color(0xFF5E35B1))
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Selección de idioma
                Text("Idioma de la voz", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B5E20))

                IdiomaVoz.entries.forEach { idioma ->
                    val selected = idioma == idiomaLocal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (selected) GreenConfig else Color(0xFFE0E0E0),
                                RoundedCornerShape(12.dp)
                            )
                            .background(if (selected) GreenConfig.copy(alpha = 0.05f) else Color.White)
                            .clickable { idiomaLocal = idioma }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Language,
                            contentDescription = null,
                            tint     = if (selected) GreenConfig else Color.Gray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                idioma.label,
                                fontSize   = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color      = if (selected) GreenConfig else Color(0xFF1B5E20)
                            )
                            Text(idioma.descripcion, fontSize = 10.sp, color = Color.Gray)
                        }
                        if (selected) {
                            Icon(
                                Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint     = GreenConfig,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Nota informativa
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GreenConfig.copy(alpha = 0.05f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Info,
                        contentDescription = null,
                        tint     = GreenConfig,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Los cambios se aplican inmediatamente en toda la app.",
                        fontSize = 11.sp, color = Color.DarkGray, lineHeight = 15.sp
                    )
                }

                // Botones
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)
                    ) { Text("Cancelar") }

                    Button(
                        onClick = {
                            onModoChange(modoLocal)
                            onIdiomaChange(idiomaLocal)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = GreenConfig)
                    ) {
                        Text("Guardar", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}