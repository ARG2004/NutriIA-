package com.example.nutriia.chatbot

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.accesibilidad.AccessibilityViewModel
import com.example.nutriia.accesibilidad.CampoTextoAccesible
import com.example.nutriia.accesibilidad.IdiomaVoz
import com.example.nutriia.accesibilidad.loc

import com.example.nutriia.embarazo.PerfilEmbarazo

private val NutriaBgCrema = Color(0xFFF9F8F4)
private val NutriaGreen = Color(0xFF4CAF50)
private val NutriaDarkGreen = Color(0xFF1B5E20)
private val CardWhite = Color.White
private val UserBubbleColor = NutriaGreen
private val BotBubbleColor = CardWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutriChatScreen(
    childName: String,
    perfilEmbarazo: PerfilEmbarazo? = null,
    isEmbarazo: Boolean = false,
    onBack: () -> Unit,
    onNavigateToAnalisis: (() -> Unit)? = null,
    viewModel: ChatViewModel = viewModel()
) {
    // ─────────────────────────────────────────────────────────────────────────
    // ACCESIBILIDAD
    // ─────────────────────────────────────────────────────────────────────────
    val a11yVm: AccessibilityViewModel = viewModel()
    val a11yMode     by a11yVm.mode.collectAsState()
    val idiomaActual by a11yVm.idioma.collectAsState()
    val ttsManager   = a11yVm.ttsManager
    val esBlind      = a11yMode == AccessibilityMode.BLIND

    fun loc(es: String, en: String) = idiomaActual.loc(es, en)

    var showGuideDialog by remember { mutableStateOf(false) }
    val esModoEmbarazo = isEmbarazo || perfilEmbarazo != null || childName.contains("Embarazo", ignoreCase = true)

    val loginVm: com.example.nutriia.auth.LoginViewModel = viewModel()
    val aiSubVm: com.example.nutriia.payment.AISubscriptionViewModel = viewModel()
    val sesion by loginVm.sesionState.collectAsState()
    val intentos = sesion.intentosIaDisponibles
    val subHasta = sesion.suscripcionIaVigenteHasta ?: 0L
    val tieneSub = System.currentTimeMillis() < subHasta
    var showPaywall by remember { mutableStateOf(false) }
    val aiSubState by aiSubVm.state.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(aiSubState.pagoCompletado) {
        if (aiSubState.pagoCompletado) {
            android.widget.Toast.makeText(context, "¡Suscripción a IA activada con éxito!", android.widget.Toast.LENGTH_LONG).show()
            loginVm.recargarSesion()
            aiSubVm.reset()
        }
    }

    LaunchedEffect(aiSubState.error) {
        if (!aiSubState.error.isNullOrEmpty()) {
            android.widget.Toast.makeText(context, aiSubState.error, android.widget.Toast.LENGTH_LONG).show()
            aiSubVm.limpiarError()
        }
    }

    fun doSendChat(query: String) {
        if (tieneSub || intentos > 0) {
            if (!tieneSub) loginVm.decrementarIntentoIaLocal()
            viewModel.sendMessage(
                query = query,
                childName = childName,
                perfilEmbarazo = perfilEmbarazo,
                isEmbarazo = esModoEmbarazo
            )
        } else {
            showPaywall = true
        }
    }

    LaunchedEffect(childName, esModoEmbarazo) {
        val newContext = if (esModoEmbarazo) "embarazo" else childName
        if (viewModel.currentContextId != newContext) {
            viewModel.clearChat()
            viewModel.currentContextId = newContext
        }
        
        viewModel.addInitialGreeting(
            childName = childName,
            isEmbarazo = esModoEmbarazo,
            semanasEmbarazo = perfilEmbarazo?.semanas ?: 1
        )
        
        if (esBlind) {
            val analisisText = if (onNavigateToAnalisis != null) loc(" Además, arriba a la derecha tienes el botón de Análisis I A de alimentos por cámara.", " Also, at the top right you have the camera AI Food Analysis button.") else ""
            val introMsg = if (esModoEmbarazo) {
                loc(
                    "Bienvenida al chat de embarazo con NutriBot. Puedes hacer cualquier pregunta sobre tu gestación, síntomas o alimentación.$analisisText",
                    "Welcome to NutriBot pregnancy chat. You can ask any questions about your pregnancy, symptoms or nutrition.$analisisText"
                )
            } else {
                loc(
                    "Bienvenido al chat con NutriBot. Habla cuando quieras y di enviar para enviar tu inquietud y recibir una respuesta.$analisisText",
                    "Welcome to NutriBot chat. Talk when you want and say send to send your concern and get a response.$analisisText"
                )
            }
            a11yVm.hablar(introMsg)
        }
    }

    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Scrollea abajo automático cuando llega o se envía un mensaje
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
            // Anunciar último mensaje si es del bot y estamos en modo blind
            if (esBlind && messages.last().isUser.not()) {
                a11yVm.hablar(messages.last().text)
            }
        }
    }

    LaunchedEffect(isLoading) {
        if (isLoading && esBlind) {
            a11yVm.hablar(loc("Analizando tu pregunta...", "Analyzing your question..."))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NutriaGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(if (esModoEmbarazo) "NutriBot Embarazo" else "NutriBot", fontWeight = FontWeight.Bold, color = NutriaDarkGreen)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Regresar", tint = NutriaGreen)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            showGuideDialog = true
                            if (esBlind) {
                                a11yVm.hablar(loc("Abriendo guía de uso de NutriBot.", "Opening NutriBot user guide."))
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = loc("Guía de uso de NutriBot", "NutriBot user guide")
                        }
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.Help, contentDescription = null, tint = NutriaGreen)
                    }
                    if (onNavigateToAnalisis != null) {
                        IconButton(
                            onClick = {
                                if (esBlind) {
                                    a11yVm.hablar(loc("Abriendo análisis inteligente de alimentos por cámara.", "Opening camera intelligent food analysis."))
                                }
                                onNavigateToAnalisis()
                            },
                            modifier = Modifier.semantics {
                                contentDescription = loc("Análisis I A de alimentos", "AI food analysis")
                            }
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = NutriaGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NutriaBgCrema)
            )
        },
        containerColor = NutriaBgCrema
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                state = listState,
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(msg, idiomaActual)
                }
                
                if (isLoading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BotBubbleColor)
                                    .padding(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = NutriaGreen,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Analizando...", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // Sugerencias / Guía rápida
            val sugerencias = remember(idiomaActual, esModoEmbarazo) {
                if (esModoEmbarazo) {
                    listOf(
                        loc("¿Qué alimentos debo evitar en el embarazo?", "What foods should I avoid in pregnancy?"),
                        loc("¿Cómo aliviar náuseas del 1er trimestre?", "How to relieve 1st trimester nausea?"),
                        loc("¿Qué suplemento de hierro y ácido fólico tomar?", "What iron & folic acid supplement to take?"),
                        loc("¿Puedo tomar café durante la gestación?", "Can I drink coffee during pregnancy?")
                    )
                } else {
                    listOf(
                        loc("¿Qué alimentos puede comer a los 6 meses?", "What foods can they eat at 6 months?"),
                        loc("Receta saludable para mi bebé", "Healthy recipe for my baby"),
                        loc("¿Cómo prevenir alergias alimentarias?", "How to prevent food allergies?"),
                        loc("¿Cuánta agua debe tomar al día?", "How much water should they drink daily?")
                    )
                }
            }

            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sugerencias, key = { it }) { sug ->
                    SuggestionChip(
                        onClick = {
                            if (esBlind) {
                                a11yVm.hablar(loc("Enviando pregunta sugerida: $sug", "Sending suggested question: $sug"))
                            }
                            doSendChat(sug)
                        },
                        label = { Text(sug, fontSize = 12.sp) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = NutriaBgCrema,
                            labelColor = NutriaDarkGreen
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = NutriaGreen.copy(alpha = 0.3f)
                        )
                    )
                }
            }

            // Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (esBlind) {
                    CampoTextoAccesible(
                        valor = inputText,
                        onValorChange = { inputText = it },
                        etiqueta = loc("Tu mensaje", "Your message"),
                        descripcionVoz = loc("Dime tu pregunta para NutriBot. Di enviar para enviarla.", "Tell me your question for NutriBot. Say send to send it."),
                        ttsManager = ttsManager,
                        idioma = idiomaActual,
                        colorPrimario = NutriaGreen,
                        modifier = Modifier.weight(1f),
                        onNext = {
                            if (inputText.isNotBlank()) {
                                doSendChat(inputText)
                                inputText = ""
                            }
                        }
                    )
                } else {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = {
                            Text(
                                if (esModoEmbarazo)
                                    loc("Pregúntale a NutriBot sobre tu embarazo...", "Ask NutriBot about your pregnancy...")
                                else
                                    loc("Pregúntale algo a NutriBot...", "Ask NutriBot something...")
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutriaGreen,
                            unfocusedBorderColor = Color.LightGray
                        ),
                        keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (inputText.isNotBlank()) {
                                    doSendChat(inputText)
                                    inputText = ""
                                }
                            }
                        )
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            doSendChat(inputText)
                            inputText = ""
                        }
                    },
                    containerColor = NutriaGreen,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Enviar")
                }
            }
        }

        if (showPaywall) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showPaywall = false },
                title = {
                    androidx.compose.material3.Text(
                        text = "Límite Diario Alcanzado",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        androidx.compose.material3.Text("Has agotado tus 3 intentos gratuitos de hoy para el NutriChat Inteligente.")
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.Text("Desbloquea chat ilimitado por un mes por solo \$99 MXN.")
                    }
                },
                confirmButton = {
                    androidx.compose.material3.Button(
                        onClick = {
                            showPaywall = false
                            aiSubVm.iniciarPagoIA(ctx, loginVm.uidUsuario)
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        androidx.compose.material3.Text("Desbloquear IA - $99 MXN", color = Color.White)
                    }
                },
                dismissButton = {
                    androidx.compose.material3.TextButton(onClick = { showPaywall = false }) {
                        androidx.compose.material3.Text("Cancelar", color = Color.Gray)
                    }
                },
                shape = RoundedCornerShape(16.dp),
                containerColor = Color.White
            )
        }

        if (aiSubState.cargando) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = NutriaGreen)
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.material3.Text("Procesando pago...", color = Color.White)
                }
            }
        }
    }

    if (showGuideDialog) {
        AlertDialog(
            onDismissRequest = { showGuideDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Rounded.Help, contentDescription = null, tint = NutriaGreen, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        loc("Guía de NutriBot 🤖", "NutriBot Guide 🤖"),
                        fontWeight = FontWeight.Bold,
                        color = NutriaDarkGreen,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        loc(
                            if (esModoEmbarazo)
                                "NutriBot es tu asistente experto de nutrición materno-infantil y embarazo. Puedes preguntarle sobre alimentación en la gestación, síntomas del embarazo, suplementos o cuidados, así como cualquier duda directa aunque sea sin contexto previo."
                            else
                                "NutriBot es tu asistente experto de nutrición materno-infantil. Puedes preguntarle sobre lactancia, inicio de sólidos, recetas, alergias, embarazo y dudas generales sin necesidad de dar contexto previo.",
                            if (esModoEmbarazo)
                                "NutriBot is your expert maternal & child nutrition assistant for pregnancy. Ask about symptoms, safe foods, vitamins, and pregnancy care."
                            else
                                "NutriBot is your expert child & maternal nutrition assistant. Ask about breastfeeding, starting solids, recipes, allergies, pregnancy, and general questions."
                        ),
                        fontSize = 14.sp,
                        color = Color.DarkGray
                    )
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    Text(
                        loc("Ejemplos de preguntas:", "Example questions:"),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = NutriaDarkGreen
                    )
                    Text(
                        loc(
                            if (esModoEmbarazo)
                                "• \"¿Qué alimentos debo evitar en el embarazo?\"\n" +
                                "• \"¿Cómo aliviar las náuseas matutinas?\"\n" +
                                "• \"¿Puedo tomar café durante el embarazo?\"\n" +
                                "• \"¿Qué vitamina o ácido fólico debo tomar?\""
                            else
                                "• \"¿Cómo introducir huevo de forma segura?\"\n" +
                                "• \"¿Receta de papilla con avena y manzana?\"\n" +
                                "• \"¿Cuánta leche toma un bebé de 4 meses?\"\n" +
                                "• \"¿Qué alimentos evitar antes del año?\"",
                            if (esModoEmbarazo)
                                "• \"What foods to avoid in pregnancy?\"\n" +
                                "• \"How to relieve morning sickness?\"\n" +
                                "• \"Can I drink coffee while pregnant?\"\n" +
                                "• \"What vitamin or folic acid should I take?\""
                            else
                                "• \"How to introduce eggs safely?\"\n" +
                                "• \"Recipe for apple oatmeal puree?\"\n" +
                                "• \"How much milk does a 4-month-old drink?\"\n" +
                                "• \"What foods to avoid before age one?\""
                        ),
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        lineHeight = 18.sp
                    )
                    if (onNavigateToAnalisis != null) {
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        Text(
                            loc("Análisis Inteligente (IA):", "Intelligent Analysis (AI):"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NutriaDarkGreen
                        )
                        Text(
                            loc(
                                "Usa el ícono de chispas o estrella en la barra superior para abrir el escáner de alimentos con cámara. La IA te dará un análisis nutricional detallado del plato de comida.",
                                "Use the sparkles/star icon in the top bar to open the food camera scanner. The AI will provide a detailed nutritional analysis of the meal."
                            ),
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showGuideDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = NutriaGreen),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(loc("Entendido", "Got it"), color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun MessageBubble(message: ChatMessage, idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX) {
    val alignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bgColor = if (message.isUser) UserBubbleColor else (if (message.isError) Color(0xFFFFEBEE) else BotBubbleColor)
    val textColor = if (message.isUser) Color.White else (if (message.isError) Color(0xFFD32F2F) else Color.DarkGray)
    
    val shape = if (message.isUser) {
        RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
    } else {
        RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
    }

    val rolePrefix = if (idioma == IdiomaVoz.INGLES) {
        if (message.isUser) "You said: " else "NutriBot says: "
    } else {
        if (message.isUser) "Tú dijiste: " else "NutriBot dice: "
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Text(
            text = message.text,
            color = textColor,
            fontSize = 15.sp,
            modifier = Modifier
                .clip(shape)
                .background(bgColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .widthIn(max = 280.dp)
                .semantics {
                    contentDescription = rolePrefix + message.text
                }
        )
    }
}
