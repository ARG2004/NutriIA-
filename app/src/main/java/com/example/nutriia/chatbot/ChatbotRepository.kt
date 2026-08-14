package com.example.nutriia.chatbot

import android.util.Log
import com.example.nutriia.BuildConfig
import com.example.nutriia.accesibilidad.KeyDeobfuscator
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

private const val TAG = "NutriIA_Chatbot"

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

class ChatbotRepository {

    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun sendQuery(
        query: String,
        history: List<ChatMessage>,
        childName: String,
        perfilEmbarazo: PerfilEmbarazo? = null,
        isEmbarazo: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = KeyDeobfuscator.deobfuscate(BuildConfig.GROQ_API_KEY)
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("La API KEY de Groq no está configurada."))
            }

            val contextoEmbarazoDetalle = if (perfilEmbarazo != null) {
                """
                - Semanas de gestación: ${perfilEmbarazo.semanas}
                - Condiciones/Síntomas reportados: ${if (perfilEmbarazo.condiciones.isNotEmpty()) perfilEmbarazo.condiciones.joinToString(", ") else "Ninguna especificada"}
                - Preferencias/Restricciones: ${if (perfilEmbarazo.preferencias.isNotEmpty()) perfilEmbarazo.preferencias.joinToString(", ") else "Ninguna"}
                - Alergias: ${if (perfilEmbarazo.allergiesDetail.isNotBlank()) perfilEmbarazo.allergiesDetail else "Sin alergias declaradas"}
                - Región de México: ${perfilEmbarazo.region.label}
                - Es embarazo gemelar: ${if (perfilEmbarazo.esGemelar) "Sí" else "No"}
                """.trimIndent()
            } else if (isEmbarazo || childName.contains("Embarazo", ignoreCase = true)) {
                "- Estado de la usuaria: Mujer en periodo de gestación/embarazo."
            } else {
                "- Perfil de atención: Alimentación y nutrición para el bebé/niño(a) llamado(a) '$childName'."
            }

            val systemPrompt = """
                Eres NutriBot, el asistente experto en nutrición materno-infantil, embarazo y pediatría de la aplicación NutriIA en México.
                ${if (contextoEmbarazoDetalle.isNotBlank()) "\nDATOS DE CONTEXTO:\n$contextoEmbarazoDetalle" else "\nNombre de referencia del niño/niña: $childName"}
                
                DIRECTRICES PRINCIPALES DE ATENCIÓN:
                1. REGLA RIGUROSA DE TERMINOLOGÍA DE PREPARACIÓN DE ALIMENTOS:
                   - CEREALES Y GRANOS (Arroz, avena, trigo, maíz, quinoa, etc.): NUNCA uses la palabra "desmenuzar" ni "deshebrar" para el arroz o cereales (los cereales no se desmenuzan). Usa siempre términos culinarios correctos como: "bien cocido y suave", "machacado/aplastado con tenedor", "en papilla o puré", "granos muy blandos" o "en copos suavemente cocidos".
                   - CARNES Y PROTEÍNAS (Pollo, pavo, res, pescado): Para carnes sí utiliza "desmenuzar", "deshebrar fino", "picar finamente" o "triturar".
                   - FRUTAS Y VERDURAS: Utiliza "cocidas al vapor", "en puré/papilla", "machacadas" o "en bastones suaves para BLW".

                2. NUTRICIÓN INFANTIL Y ALIMENTACIÓN COMPLEMENTARIA:
                   - Da indicaciones claras sobre texturas seguras según la etapa del bebé ($childName).
                   - Responde sobre lactancia (materna/fórmula), inicio de sólidos (papillas, BLW), recetas infantiles y desarrollo del bebé.

                3. DUDAS DEL EMBARAZO: Si te consultan sobre el embarazo, responde de forma completa y amable sobre nutrición gestacional, alimentos recomendados y prohibidos, suplementos y alivio de síntomas.

                4. RESPUESTAS A PREGUNTAS SIN CONTEXTO O GENÉRICAS: Si el usuario hace una pregunta directa, genérica o sin antecedente previo (por ejemplo: "¿Cómo ofrecer arroz?", "¿Puedo dar avena?", "¿Puedo tomar café?", "¿Qué puedo cenar?", "Tengo náuseas"), RESPÓNDELA DIRECTAMENTE de manera clara, útil, empática y comprensible. NUNCA exijas antecedentes previos ni te niegues a responder por falta de contexto.

                5. DESCARGO DE RESPONSABILIDAD MÉDICO: Incluye un breve recordatorio amable sugiriendo consultar con el pediatra principal (o ginecólogo/obstetra en embarazo) para valoración médica personal.

                6. FORMATO Y TONO: Mantén un tono cálido, empático, profesional y estructurado (máximo 3 a 4 párrafos cortos o viñetas cuando sea apropiado).
            """.trimIndent()

            val messages = mutableListOf<Map<String, String>>()
            
            // Context system
            messages.add(mapOf("role" to "system", "content" to systemPrompt))
            
            // Agregamos contexto de historial (máximo los últimos 6 mensajes)
            val recentHistory = history.takeLast(6)
            for (msg in recentHistory) {
                if (msg.isUser) {
                    messages.add(mapOf("role" to "user", "content" to msg.text))
                } else if (!msg.isError) {
                    messages.add(mapOf("role" to "assistant", "content" to msg.text))
                }
            }
            
            // Mensaje actual
            messages.add(mapOf("role" to "user", "content" to query))

            val requestBodyJson = gson.toJson(
                mapOf(
                    "model"       to "llama-3.1-8b-instant",
                    "messages"    to messages,
                    "max_tokens"  to 500,
                    "temperature" to 0.5
                )
            )

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            val response = http.newCall(request).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "Error from Groq: ${response.code} $errorBody")
                return@withContext Result.failure(Exception("Hubo un error comunicándose con el asistente."))
            }

            val rawBody = response.body?.string() ?: ""
            val json = JsonParser.parseString(rawBody).asJsonObject
            val content = json.getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            Result.success(content.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Exception in ChatbotRepository", e)
            Result.failure(Exception("Error de red o conexión: ${e.message}"))
        }
    }
}
