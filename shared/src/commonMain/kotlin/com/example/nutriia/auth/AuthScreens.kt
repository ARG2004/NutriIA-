package com.example.nutriia.auth

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

// ─── COLORES (idénticos a Android ResgisterScreen.kt) ────────────────────
private val RegGreen     = Color(0xFF689F38)
private val RegDarkGreen = Color(0xFF33691E)
private val RegBgCrema   = Color(0xFFF8F9F3)
private val RegCardWhite = Color.White
private val RegPurple    = Color(0xFF9C8FE0)
private val RegTeal      = Color(0xFF4DB6AC)
private val RegOrange    = Color(0xFFFF8F00)
private val RegRosa      = Color(0xFFEC9BBF)
private val RegRosaGine  = Color(0xFFF06292)

// ─── MODELOS DE DATOS (idénticos a Android) ──────────────────────────────
data class ParentRegisterData(
    val name: String = "", val email: String = "",
    val password: String = "", val phone: String = "",
    val nutritionistCode: String = "", val childName: String = ""
)
data class MamaPrimerizaRegisterData(
    val name: String = "", val email: String = "",
    val password: String = "", val phone: String = "", val semanas: Int = 1
)
data class NutritionistRegisterData(
    val name: String = "", val email: String = "",
    val password: String = "", val phone: String = "",
    val specialty: String = "", val licenseId: String = ""
)

// ═════════════════════════════════════════════════════════════════════════
// LOGIN SCREEN
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onNavigateRegister: () -> Unit
) {
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passVisible by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(80.dp))
            // Logo
            Box(
                Modifier.size(96.dp).clip(CircleShape)
                    .background(RegGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Eco, null, tint = RegGreen, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("NutriIA", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = RegDarkGreen)
            Text("Nutrición inteligente para tu familia", fontSize = 14.sp, color = Color.Gray)
            Spacer(Modifier.height(40.dp))

            // Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RegCardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("Iniciar sesión", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = RegDarkGreen)
                    Spacer(Modifier.height(20.dp))

                    OutlinedTextField(
                        value = email, onValueChange = { email = it },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = RegGreen) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = RegGreen) },
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(
                                    if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    null, tint = Color.Gray
                                )
                            }
                        },
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = onLogin,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RegGreen)
                    ) {
                        Icon(Icons.Rounded.Login, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            TextButton(onClick = onNavigateRegister) {
                Text("¿No tienes cuenta? ", color = Color.Gray, fontSize = 14.sp)
                Text("Regístrate", color = RegGreen, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// REGISTER TYPE SCREEN — Selección de tipo de cuenta
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun RegisterTypeScreen(
    onNavigateBack: () -> Unit,
    onSelectParent: () -> Unit,
    onSelectNutritionist: () -> Unit,
    onSelectMamaPrimeriza: () -> Unit,
    onSelectGinecologo: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(RegCardWhite)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = RegGreen)
                }
            }
            Spacer(Modifier.height(32.dp))

            Box(
                Modifier.size(72.dp).clip(CircleShape).background(RegGreen.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.PersonAdd, null, tint = RegGreen, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Crear cuenta", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = RegDarkGreen)
            Spacer(Modifier.height(8.dp))
            Text("¿Cómo vas a usar NutriIA?", fontSize = 15.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))

            // Tarjetas de tipo
            RoleCard("👨‍👩‍👧 Familia",        "Papá / Mamá con hijos",         RegGreen,    Icons.Rounded.FamilyRestroom, onSelectParent)
            Spacer(Modifier.height(12.dp))
            RoleCard("🤰 Embarazo",        "Mamá primeriza en gestación",   RegRosa,     Icons.Rounded.Favorite,       onSelectMamaPrimeriza)
            Spacer(Modifier.height(12.dp))
            RoleCard("🏥 Nutriólogo",       "Profesional de nutrición",      RegTeal,     Icons.Rounded.LocalHospital,  onSelectNutritionist)
            Spacer(Modifier.height(12.dp))
            RoleCard("👩‍⚕️ Ginecólogo",      "Especialista en ginecología",   RegRosaGine, Icons.Rounded.MedicalServices, onSelectGinecologo)
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun RoleCard(
    title: String, subtitle: String,
    color: Color, icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = RegCardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(color.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = color, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF212121))
                Text(subtitle, fontSize = 13.sp, color = Color.Gray)
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = color, modifier = Modifier.size(24.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// PARENT REGISTER SCREEN — 1:1 con ResgisterScreen.kt sección Familia
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun ParentRegisterScreen(
    onNavigateBack: () -> Unit,
    onRegister: () -> Unit
) {
    var data by remember { mutableStateOf(ParentRegisterData()) }
    var passVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(RegCardWhite)
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = RegGreen) }
                Spacer(Modifier.weight(1f))
                Badge(containerColor = RegGreen.copy(alpha = 0.15f)) {
                    Text("  Familia  ", color = RegGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RegCardWhite),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    // ── 👤 Datos personales ──
                    Text("👤 Datos personales", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.name, onValueChange = { data = data.copy(name = it) },
                        label = { Text("Nombre completo") },
                        leadingIcon = { Icon(Icons.Rounded.Person, null, tint = RegGreen) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = data.phone, onValueChange = { data = data.copy(phone = it) },
                        label = { Text("Teléfono") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = RegGreen) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    // ── 🔒 Acceso a la cuenta ──
                    Text("🔒 Acceso a la cuenta", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.email, onValueChange = { data = data.copy(email = it) },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = RegGreen) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = data.password, onValueChange = { data = data.copy(password = it) },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = RegGreen) },
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                            }
                        },
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    // ── 👶 Tu primer hijo/a ──
                    Text("👶 Tu primer hijo/a", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.childName, onValueChange = { data = data.copy(childName = it) },
                        label = { Text("Nombre del hijo/a") },
                        leadingIcon = { Icon(Icons.Rounded.ChildCare, null, tint = RegGreen) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    // ── 🏥 Vincular nutriólogo ──
                    Text("🏥 Vincular nutriólogo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.nutritionistCode, onValueChange = { data = data.copy(nutritionistCode = it) },
                        label = { Text("Código del nutriólogo (opcional)") },
                        leadingIcon = { Icon(Icons.Rounded.Badge, null, tint = RegGreen) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = acceptTerms, onCheckedChange = { acceptTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = RegGreen))
                        Text("Acepto los términos y condiciones", fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RegGreen),
                        enabled = acceptTerms && data.name.isNotBlank() && data.email.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.HowToReg, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// MAMA PRIMERIZA REGISTER SCREEN — 1:1 sección Embarazo
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun MamaPrimerizaRegisterScreen(
    onNavigateBack: () -> Unit,
    onRegister: () -> Unit
) {
    var data by remember { mutableStateOf(MamaPrimerizaRegisterData()) }
    var passVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(RegCardWhite)
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = RegRosa) }
                Spacer(Modifier.weight(1f))
                Badge(containerColor = RegRosa.copy(alpha = 0.15f)) {
                    Text("  Embarazo  ", color = RegRosa, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RegCardWhite),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("👤 Datos personales", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.name, onValueChange = { data = data.copy(name = it) },
                        label = { Text("Nombre completo") },
                        leadingIcon = { Icon(Icons.Rounded.Person, null, tint = RegRosa) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = data.phone, onValueChange = { data = data.copy(phone = it) },
                        label = { Text("Teléfono") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = RegRosa) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    Text("🔒 Acceso a la cuenta", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.email, onValueChange = { data = data.copy(email = it) },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = RegRosa) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = data.password, onValueChange = { data = data.copy(password = it) },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = RegRosa) },
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                            }
                        },
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    Text("📅 Estado del embarazo", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    Text("Semanas de gestación: ${data.semanas}", fontSize = 14.sp, color = Color.Gray)
                    Slider(
                        value = data.semanas.toFloat(),
                        onValueChange = { data = data.copy(semanas = it.toInt()) },
                        valueRange = 1f..42f,
                        steps = 40,
                        colors = SliderDefaults.colors(thumbColor = RegRosa, activeTrackColor = RegRosa)
                    )

                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = acceptTerms, onCheckedChange = { acceptTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = RegRosa))
                        Text("Acepto los términos y condiciones", fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RegRosa),
                        enabled = acceptTerms && data.name.isNotBlank() && data.email.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.HowToReg, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Crear cuenta", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════
// PROFESSIONAL REGISTER SCREEN — Nutriólogo/Ginecólogo (sección Profesional)
// ═════════════════════════════════════════════════════════════════════════
@Composable
fun ProfessionalRegisterScreen(
    onNavigateBack: () -> Unit,
    onRegister: () -> Unit
) {
    var data by remember { mutableStateOf(NutritionistRegisterData()) }
    var passVisible by remember { mutableStateOf(false) }
    var acceptTerms by remember { mutableStateOf(false) }
    var sepVerified by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(RegBgCrema)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(52.dp))
            Row(Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.clip(CircleShape).background(RegCardWhite)
                ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Volver", tint = RegTeal) }
                Spacer(Modifier.weight(1f))
                Badge(containerColor = RegTeal.copy(alpha = 0.15f)) {
                    Text("  Profesional  ", color = RegTeal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = RegCardWhite),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text("👤 Datos personales", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.name, onValueChange = { data = data.copy(name = it) },
                        label = { Text("Nombre completo") },
                        leadingIcon = { Icon(Icons.Rounded.Person, null, tint = RegTeal) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = data.phone, onValueChange = { data = data.copy(phone = it) },
                        label = { Text("Teléfono") },
                        leadingIcon = { Icon(Icons.Rounded.Phone, null, tint = RegTeal) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    Text("🔒 Acceso a la cuenta", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.email, onValueChange = { data = data.copy(email = it) },
                        label = { Text("Correo electrónico") },
                        leadingIcon = { Icon(Icons.Rounded.Email, null, tint = RegTeal) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = data.password, onValueChange = { data = data.copy(password = it) },
                        label = { Text("Contraseña") },
                        leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = RegTeal) },
                        trailingIcon = {
                            IconButton(onClick = { passVisible = !passVisible }) {
                                Icon(if (passVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                            }
                        },
                        visualTransformation = if (passVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true
                    )

                    Spacer(Modifier.height(24.dp))
                    Text("🛡️ Datos profesionales", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = RegDarkGreen)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = data.specialty, onValueChange = { data = data.copy(specialty = it) },
                        label = { Text("Especialidad") },
                        leadingIcon = { Icon(Icons.Rounded.WorkspacePremium, null, tint = RegTeal) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = data.licenseId, onValueChange = { data = data.copy(licenseId = it) },
                        label = { Text("Cédula profesional") },
                        leadingIcon = { Icon(Icons.Rounded.Badge, null, tint = RegTeal) },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = sepVerified, onCheckedChange = { sepVerified = it },
                            colors = CheckboxDefaults.colors(checkedColor = RegTeal))
                        Text("Verificar cédula con SEP", fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(24.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = acceptTerms, onCheckedChange = { acceptTerms = it },
                            colors = CheckboxDefaults.colors(checkedColor = RegTeal))
                        Text("Acepto los términos y condiciones", fontSize = 13.sp, color = Color.Gray)
                    }

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onRegister,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RegTeal),
                        enabled = acceptTerms && data.name.isNotBlank() && data.email.isNotBlank() && data.licenseId.isNotBlank()
                    ) {
                        Icon(Icons.Rounded.HowToReg, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Crear cuenta profesional", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}
