package com.example.nutriia.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.nutriia.accesibilidad.*

@Composable
fun LactanciaScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Lactancia & Tomas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("Cronómetro de tomas, pecho izquierdo / derecho y banco de leche materna.", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun SolidosScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Alimentación BLW & Sólidos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("Introducción de alimentos, regla de 3 días de alergias y textura por edad.", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun CrecimientoScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Curvas de Crecimiento OMS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("Gráficas percentiladas de peso, talla y perímetro cefálico comparadas con estándares OMS.", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun SuenoScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Registro de Sueño", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun NutrientesScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Nutrientes & Suplementos", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun NeurodesarrolloScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Neurodesarrollo", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun MealPlanningScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Plan Semanal de Comidas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun NutriChatScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("NutriBot IA", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Spacer(Modifier.height(16.dp))
        Text("Asistente inteligente para resolver dudas de nutrición pediátrica en tiempo real.", fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun DiarioVisualScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Diario Visual", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun RecordatoriosScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Alertas & Vacunas", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun PediatraScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Directorio Pediátrico", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}

@Composable
fun ConfiguracionScreenView(onBack: () -> Unit, onLogout: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Configuración", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Cerrar Sesión", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HelpScreenView(onBack: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(NutriaBgCrema).verticalScroll(rememberScrollState()).padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = NutriaDarkGreen) }
            Spacer(Modifier.width(8.dp))
            Text("Centro de Ayuda", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
        }
    }
}
