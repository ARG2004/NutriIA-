package com.example.nutriia.chatbot

import com.example.nutriia.embarazo.PerfilEmbarazo
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val repository = ChatbotRepository()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    var currentContextId: String = ""

    fun clearChat() {
        _messages.value = emptyList()
    }

    fun sendMessage(
        query: String,
        childName: String,
        perfilEmbarazo: PerfilEmbarazo? = null,
        isEmbarazo: Boolean = false
    ) {
        if (query.isBlank()) return

        // Añadimos el mensaje del usuario
        val userMsg = ChatMessage(text = query.trim(), isUser = true)
        _messages.value = _messages.value + userMsg

        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.sendQuery(
                query = query,
                history = _messages.value,
                childName = childName,
                perfilEmbarazo = perfilEmbarazo,
                isEmbarazo = isEmbarazo
            )
            
            _isLoading.value = false
            
            result.onSuccess { responseText ->
                val botMsg = ChatMessage(text = responseText, isUser = false)
                _messages.value = _messages.value + botMsg
            }.onFailure { error ->
                val errorMsg = ChatMessage(
                    text = "Lo siento, tuve un problema al responder: ${error.message}",
                    isUser = false,
                    isError = true
                )
                _messages.value = _messages.value + errorMsg
            }
        }
    }

    fun addInitialGreeting(
        childName: String,
        isEmbarazo: Boolean = false,
        semanasEmbarazo: Int = 1
    ) {
        if (_messages.value.isEmpty()) {
            val greetingText = if (isEmbarazo || childName.contains("Embarazo", ignoreCase = true)) {
                "¡Hola! Soy NutriBot. Estoy aquí para acompañarte en tu embarazo (semana $semanasEmbarazo) y responder todas tus dudas de nutrición, síntomas, alimentos y salud. ¿En qué te puedo ayudar hoy?"
            } else {
                "¡Hola! Soy NutriBot. ¿En qué te puedo asesorar hoy sobre la nutrición de $childName o cualquier duda de salud y alimentación?"
            }
            _messages.value = listOf(
                ChatMessage(
                    text = greetingText,
                    isUser = false
                )
            )
        }
    }
}
