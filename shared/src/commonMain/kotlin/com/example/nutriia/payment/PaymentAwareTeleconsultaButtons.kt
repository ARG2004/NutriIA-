package com.example.nutriia.payment

import com.example.nutriia.platform.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriia.teleconsulta.TeleconsultaViewModel
import com.example.nutriia.teleconsulta.TipoLlamada
import kotlinx.coroutines.launch

private val CallGreen = Color(0xFF22C55E)
private val CallBlue  = Color(0xFF60A5FA)

@Composable
fun PaymentAwareTeleconsultaButtons(
    nutriologoUid:         String,
    nutriologoNombre:      String,
    padreUid:              String,
    padreNombre:           String,
    childId:               String,
    childNombre:           String,
    teleconsultaViewModel: TeleconsultaViewModel,
    onAbrirPago:           (nutriologoUid: String, nutriologoNombre: String, tipo: TipoLlamada) -> Unit
) {
    val scope       = rememberCoroutineScope()
    val repo        = remember { PaymentRepository() }
    var verificando by remember { mutableStateOf(false) }

    fun manejarClick(tipo: TipoLlamada) {
        scope.launch {
            verificando = true
            repo.obtenerPagoVigente(nutriologoUid, childId).fold(
                onSuccess = { pagoVigente ->
                    Log.d("PaymentAwareButtons", "pagoVigente encontrado: id=${pagoVigente?.id}, estado=${pagoVigente?.estado}")
                    if (pagoVigente != null) {
                        // Tiene un pago sin usar → Iniciar llamada directamente
                        teleconsultaViewModel.iniciarLlamadaComoPadre(
                            padreUid         = padreUid,
                            padreNombre      = padreNombre,
                            nutriologoUid    = nutriologoUid,
                            nutriologoNombre = nutriologoNombre,
                            childId          = childId,
                            childNombre      = childNombre,
                            pagoId           = pagoVigente.id,
                            tipo             = tipo
                        )
                    } else {
                        // No tiene pago vigente → ir a pasarela
                        onAbrirPago(nutriologoUid, nutriologoNombre, tipo)
                    }
                },
                onFailure = {
                    Log.e("PaymentAwareButtons", "Error obteniendo pago vigente", it)
                    onAbrirPago(nutriologoUid, nutriologoNombre, tipo)
                }
            )
            verificando = false
        }
    }

    Row(
        modifier              = Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        OutlinedButton(
            onClick  = { manejarClick(TipoLlamada.AUDIO) },
            enabled  = !verificando,
            modifier = Modifier.weight(1f).height(44.dp),
            border   = BorderStroke(1.5.dp, CallGreen),
            shape    = RoundedCornerShape(13.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = CallGreen)
        ) {
            if (verificando) {
                CircularProgressIndicator(color = CallGreen, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Call, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Audio", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Button(
            onClick  = { manejarClick(TipoLlamada.VIDEO) },
            enabled  = !verificando,
            modifier = Modifier.weight(1f).height(44.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = CallBlue),
            shape    = RoundedCornerShape(13.dp)
        ) {
            if (verificando) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Rounded.Videocam, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Video", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    Text(
        "$130.00 MXN · pago por sesión",
        fontSize = 10.sp,
        color    = Color.Gray,
        modifier = Modifier.padding(top = 4.dp)
    )
}
