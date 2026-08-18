package com.example.nutriia.chatbot

import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.platform.Log
import com.example.nutriia.platform.PlatformConfig
import com.example.nutriia.platform.PlatformHttp
import com.example.nutriia.platform.generateUUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private const val TAG = "NutriIA_Chatbot"

data class ChatMessage(
    val id: String = generateUUID(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false
)

class ChatbotRepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun sendQuery(
        query: String,
        history: List<ChatMessage>,
        childName: String,
        perfilEmbarazo: PerfilEmbarazo? = null,
        isEmbarazo: Boolean = false
    ): Result<String> = withContext(Dispatchers.Default) {
        try {
            val apiKey = PlatformConfig.groqApiKey
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

            val messagesArray = buildJsonArray {
                // Mensaje del sistema
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }

                // Historial reciente (últimos 5 mensajes, igual que Android)
                val recentHistory = history.takeLast(5)
                for (msg in recentHistory) {
                    if (msg.isUser) {
                        addJsonObject {
                            put("role", "user")
                            put("content", msg.text)
                        }
                    } else if (!msg.isError) {
                        addJsonObject {
                            put("role", "assistant")
                            put("content", msg.text)
                        }
                    }
                }

                // Mensaje actual del usuario
                addJsonObject {
                    put("role", "user")
                    put("content", query)
                }
            }

            val candidateModels = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "mixtral-8x7b-32768"
            )

            var rawBody: String? = null

            for (model in candidateModels) {
                val requestBodyJson = buildJsonObject {
                    put("model", model)
                    put("messages", messagesArray)
                    put("max_tokens", 500)
                    put("temperature", 0.5)
                }.toString()

                val responseResult = PlatformHttp.postJson(
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    headers = mapOf(
                        "Authorization" to "Bearer $apiKey",
                        "Content-Type" to "application/json; charset=utf-8"
                    ),
                    jsonBody = requestBodyJson,
                    timeoutMs = 30000L
                )

                if (responseResult.isSuccess) {
                    val body = responseResult.getOrNull()
                    if (!body.isNullOrBlank()) {
                        rawBody = body
                        break
                    }
                } else {
                    Log.w(TAG, "Fallo modelo $model: ${responseResult.exceptionOrNull()?.message}")
                }
            }

            if (rawBody.isNullOrBlank()) {
                return@withContext Result.failure(Exception("Hubo un error comunicándose con el asistente."))
            }

            val jsonRoot = json.parseToJsonElement(rawBody).jsonObject
            var content = jsonRoot["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.contentOrNull ?: ""

            if (content.contains("<think>") && content.contains("</think>")) {
                content = content.substringAfter("</think>").trim()
            }

            if (content.isBlank()) {
                return@withContext Result.failure(Exception("Respuesta vacía del asistente de IA."))
            }

            Result.success(content.trim())
        } catch (e: Exception) {
            Log.e(TAG, "Exception in ChatbotRepository: ${e.message}")
            Result.failure(Exception("Error de red o conexión: ${e.message}"))
        }
    }
}
