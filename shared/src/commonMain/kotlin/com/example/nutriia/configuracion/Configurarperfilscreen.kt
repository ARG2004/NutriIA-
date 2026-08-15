package com.example.nutriia.configuracion
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val EpGreen     = Color(0xFF4CAF50)
private val EpDarkGreen = Color(0xFF1B5E20)
private val EpBg        = Color(0xFFF9F8F4)

@Composable
fun EditarPerfilScreen(
    nombreInicial:  String,
    emailInicial:   String,
    telefonoInicial:String = "",
    onBack:         () -> Unit,
    onGuardar:      (nombre: String, email: String, telefono: String) -> Unit
) {
    var nombre   by remember { mutableStateOf(nombreInicial) }
    var telefono by remember { mutableStateOf(telefonoInicial) }

    val puedeGuardar = nombre.isNotBlank()

    Box(modifier = Modifier.fillMaxSize().background(EpBg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Top bar
            Row(
                modifier          = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp).padding(top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Regresar", tint = EpGreen, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text("Editar perfil", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = EpDarkGreen)
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Avatar grande
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(88.dp)
                            .clip(CircleShape)
                            .background(EpGreen.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            nombre.take(2).uppercase(),
                            fontSize   = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color      = EpGreen
                        )
                    }
                }

                // Nombre
                OutlinedTextField(
                    value           = nombre,
                    onValueChange   = { nombre = it },
                    label           = { Text("Nombre completo") },
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(16.dp),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    leadingIcon     = { Icon(Icons.Rounded.Person, contentDescription = null, tint = EpGreen) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = EpGreen,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor    = EpGreen
                    )
                )

                // Email (solo lectura — solo se muestra, no se edita por seguridad Firebase)
                OutlinedTextField(
                    value         = emailInicial,
                    onValueChange = {},
                    label         = { Text("Correo electrónico") },
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(16.dp),
                    enabled       = false,
                    singleLine    = true,
                    leadingIcon   = { Icon(Icons.Rounded.Email, contentDescription = null, tint = Color.LightGray) },
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledBorderColor = Color.LightGray,
                        disabledTextColor   = Color.Gray,
                        disabledLabelColor  = Color.LightGray
                    )
                )
                Text(
                    "El correo electrónico es el identificador de tu cuenta y no se puede modificar por seguridad.",
                    fontSize = 11.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp)
                )

                // Teléfono
                OutlinedTextField(
                    value           = telefono,
                    onValueChange   = { input ->
                        val soloDigitos = input.filter(Char::isDigit)
                        val digitosPrevios = telefono.filter(Char::isDigit)
                        if (soloDigitos.length <= 10 || soloDigitos.length < digitosPrevios.length) {
                            telefono = soloDigitos.take(10)
                        }
                    },
                    label           = { Text("Teléfono (opcional)") },
                    modifier        = Modifier.fillMaxWidth(),
                    shape           = RoundedCornerShape(16.dp),
                    singleLine      = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon     = { Icon(Icons.Rounded.Phone, contentDescription = null, tint = EpGreen) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = EpGreen,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor    = EpGreen
                    )
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick  = { onGuardar(nombre, emailInicial, telefono) },
                    enabled  = puedeGuardar,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape    = RoundedCornerShape(18.dp),
                    colors   = ButtonDefaults.buttonColors(
                        containerColor         = EpGreen,
                        disabledContainerColor = Color.LightGray
                    )
                ) {
                    Icon(Icons.Rounded.Save, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar cambios", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}