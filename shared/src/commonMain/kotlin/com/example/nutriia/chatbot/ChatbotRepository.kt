package com.example.nutriia.chatbot

import com.example.nutriia.platform.Log
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.embarazo.PerfilEmbarazo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "NutriIA_Chatbot"

data class ChatMessage(
    val id: String = generateUUID(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

class ChatbotRepository {

    suspend fun sendQuery(
        query: String,
        history: List<ChatMessage>,
        childName: String,
        perfilEmbarazo: PerfilEmbarazo? = null,
        isEmbarazo: Boolean = false
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            val q = query.lowercase()
            val respuesta = when {
                q.contains("arroz") || q.contains("avena") || q.contains("cereal") ->
                    "Para ofrecer cereales como el arroz o la avena a $childName, asegúrate de que estén bien cocidos, suaves y machacados con un tenedor o en papilla suave. Nunca ofrezcas granos duros ni secos. Consulta siempre con tu pediatra."
                q.contains("pollo") || q.contains("carne") || q.contains("proteina") ->
                    "Las proteínas como el pollo o la carne deben cocerse completamente y ofrecerse finamente deshebradas, picadas muy finitas o en puré suave según los meses de $childName para prevenir atragantamientos."
                q.contains("agua") || q.contains("sed") ->
                    "A partir de los 6 meses con el inicio de la alimentación complementaria, se pueden ofrecer pequeños sorbos de agua simple potable durante las comidas."
                q.contains("nauseas") || q.contains("vomito") || isEmbarazo ->
                    "Durante el embarazo, las comidas pequeñas y frecuentes bajas en grasa y ricas en fibra ayudan a controlar las náuseas. Mantén una buena hidratación a lo largo del día y consulta a tu ginecólogo."
                else ->
                    "¡Hola! Para la nutrición de $childName, es recomendable introducir un alimento nuevo a la vez y observar tolerancia durante 2 a 3 días. Recuerda mantener la higiene y texturas adecuadas para su edad. ¿Tienes alguna pregunta sobre un alimento en específico?"
            }
            Result.success(respuesta)
        } catch (e: Exception) {
            Log.e(TAG, "Exception in ChatbotRepository", e)
            Result.failure(Exception("Error al procesar la consulta: ${e.message}"))
        }
    }
}
