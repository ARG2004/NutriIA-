package com.example.nutriia.analisisIA

import com.example.nutriia.accesibilidad.KeyDeobfuscator
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.platform.Log
import com.example.nutriia.platform.PlatformConfig
import com.example.nutriia.platform.PlatformHttp
import com.example.nutriia.platform.RemoteConfigManager
import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.ui.theme.ChildProfile
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

private const val TAG = "NutriIA_Analisis"

class AnalisisRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        allowTrailingComma = true
        allowSpecialFloatingPointValues = true
        coerceInputValues = true
    }

    private fun uid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")

    private fun colAnalisis(childId: String) =
        db.collection("usuarios").document(uid())
            .collection("hijos").document(childId)
            .collection("analisis_ia")

    private fun colCache() =
        db.collection("usuarios").document(uid()).collection("analisis_cache")

    // ══════════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE ALIMENTO CON GROQ VISION
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun detectarAlimento(base64Image: String): Result<FoodDetectionResult> {
        return try {
            val apiKey = PlatformConfig.groqApiKey
            val prompt = """
                Eres un experto en nutrición, gastronomía Y visión por computadora.
                Tu tarea es identificar con máxima precisión el contenido de la imagen.

                REGLA CRÍTICA PARA OBJETOS NO COMESTIBLES:
                - Si la foto contiene un objeto, mueble, electrónico, herramienta u artículo de escritorio (ejemplo: laptop, computadora, lámpara, organizador de escritorio con tijeras y regla, celular, juguete, libro, etc.) Y NO contiene comida:
                - DEBES identificar el objeto REAL en `foodName`.
                - En `foodType` escribe 'objeto_no_comestible'.
                - En `ingredients` pon una lista vacía `[]` o los elementos visibles.
                - En `confidence` pon tu certeza real (ej: 0.90).
                - NUNCA inventes que un objeto es comida ni devuelvas "Alimento detectado" o "Alimento desconocido". Escribe el nombre REAL del objeto.

                IMPORTANTE — Identificación de alimentos y errores comunes a evitar:
                - ALIMENTOS ENTEROS Y CRUDOS: Las manzanas, naranjas, frutas enteras, verduras crudas y los huevos en cascarón SON COMIDA. NUNCA los clasifiques como "objeto_no_comestible". Su `foodType` debe ser "fruta", "verdura", "snack" o el que corresponda.
                - HUEVO / HUEVOS: Identifica el huevo en todas sus presentaciones (en cascarón, huevo revuelto, huevo estrellado, huevo cocido). NUNCA confundas huevos revueltos con puré de papa, papas fritas ni queso derretido.
                - La LECHUGA NO es col/repollo.
                - La PIÑA en cubos NO es puré de papa.

                Analiza esta imagen y responde ÚNICAMENTE con un JSON con este formato exacto:
                {
                  "foodName": "nombre descriptivo exacto",
                  "ingredients": ["ingrediente1", "ingrediente2"],
                  "foodType": "uno de: objeto_no_comestible|desayuno|comida|cena|snack|bebida|fruta|verdura|cereal|lacteo|producto_empacado",
                  "confidence": 0.95
                }
                Devuelve SOLO el JSON, sin texto adicional ni markdown.
            """.trimIndent()

            val visionRemote = RemoteConfigManager.getVisionModel()
            val visionModels = (listOf(visionRemote) + listOf(
                "qwen/qwen3.6-27b",
                "meta-llama/llama-4-scout-17b-16e-instruct",
                "llama-3.2-11b-vision-preview",
                "llama-3.2-90b-vision-preview"
            )).distinct()

            var rawResponse: String? = null
            var lastError: String? = null

            for (modelName in visionModels) {
                val payload = buildJsonObject {
                    put("model", modelName)
                    putJsonArray("messages") {
                        addJsonObject {
                            put("role", "user")
                            putJsonArray("content") {
                                addJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") {
                                        put("url", "data:image/jpeg;base64,$base64Image")
                                    }
                                }
                                addJsonObject {
                                    put("type", "text")
                                    put("text", prompt)
                                }
                            }
                        }
                    }
                    put("max_tokens", 1000)
                    put("temperature", 0.05)
                }.toString()

                val result = PlatformHttp.postJson(
                    url = "https://api.groq.com/openai/v1/chat/completions",
                    headers = mapOf(
                        "Authorization" to "Bearer $apiKey",
                        "Content-Type" to "application/json; charset=utf-8"
                    ),
                    jsonBody = payload,
                    timeoutMs = 25000L
                )

                if (result.isSuccess) {
                    val body = result.getOrNull()
                    if (!body.isNullOrBlank()) {
                        rawResponse = body
                        break
                    }
                } else {
                    lastError = result.exceptionOrNull()?.message ?: "Error desconocido con modelo $modelName"
                    println("⚠️ [AnalisisIA] Modelo $modelName falló: $lastError")
                }
            }

            if (rawResponse.isNullOrBlank()) {
                val msg = lastError ?: "No se pudo conectar a los modelos de visión de IA."
                return Result.failure(Exception("No se pudo conectar a los modelos de visión de IA. Detalle: $msg"))
            }

            val content = extractGroqContent(rawResponse)
            val cleaned = extractJsonSubstring(content)
            val jsonObj = runCatching { json.parseToJsonElement(cleaned).jsonObject }.getOrElse {
                val nameMatch = Regex("\"foodName\"\\s*:\\s*\"([^\"]+)\"").find(cleaned)?.groupValues?.getOrNull(1)
                    ?: content.lines().firstOrNull { it.isNotBlank() && !it.startsWith("{") }?.take(40)
                    ?: "Alimento detectado"
                buildJsonObject {
                    put("foodName", nameMatch)
                    putJsonArray("ingredients") { add(nameMatch) }
                    put("foodType", "comida")
                    put("confidence", 0.90)
                }
            }

            val rawName = jsonObj["foodName"]?.jsonPrimitive?.contentOrNull ?: "Objeto detectado"
            val finalName = if (rawName.equals("Alimento detectado", true) || rawName.equals("Alimento desconocido", true)) "Objeto no alimenticio" else rawName
            val ingArr = jsonObj["ingredients"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val foodType = jsonObj["foodType"]?.jsonPrimitive?.contentOrNull ?: "objeto_no_comestible"
            val conf = jsonObj["confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.90

            val detection = FoodDetectionResult(
                foodName = finalName,
                ingredients = ingArr,
                foodType = foodType,
                confidence = conf
            )
            Log.d(TAG, "[VISION] Detectado con éxito: ${detection.foodName} (${(detection.confidence * 100).toInt()}%)")
            Result.success(detection)
        } catch (e: Exception) {
            Log.e(TAG, "[VISION] Excepción detectando alimento: ${e.message}")
            Result.failure(Exception("Error detectando alimento: ${e.message}"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // OBTENCIÓN NUTRICIONAL CON LLM
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun obtenerNutricion(foodName: String): Result<NutritionInfo> {
        return try {
            val prompt = """
                Eres un nutriólogo experto en composición de alimentos con acceso a tablas nutricionales INSP, USDA y NOM-043.
                Proporciona los valores nutricionales por 100g de: "$foodName"

                Responde ÚNICAMENTE con este JSON (sin texto adicional, sin markdown):
                {
                  "calories": 0.0,
                  "protein": 0.0,
                  "carbohydrates": 0.0,
                  "fat": 0.0,
                  "sugar": 0.0,
                  "fiber": 0.0,
                  "sodium": 0.0
                }
            """.trimIndent()

            val rawBody = queryGroqText(prompt, 250) ?: return Result.success(NutritionInfo())
            val content = extractGroqContent(rawBody)
            val cleaned = extractJsonSubstring(content)
            val obj = runCatching { json.parseToJsonElement(cleaned).jsonObject }.getOrElse {
                buildJsonObject {
                    put("calories", 100.0)
                    put("protein", 2.0)
                    put("carbohydrates", 15.0)
                    put("fat", 1.0)
                }
            }

            val info = NutritionInfo(
                calories = obj["calories"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                protein = obj["protein"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                carbohydrates = obj["carbohydrates"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                fat = obj["fat"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                sugar = obj["sugar"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                fiber = obj["fiber"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                sodium = obj["sodium"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            )
            Result.success(info)
        } catch (e: Exception) {
            Log.w(TAG, "[LLM-NUTRITION] Error: ${e.message}")
            Result.success(NutritionInfo())
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANÁLISIS PEDIÁTRICO
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun analizarParaNino(
        child: ChildProfile,
        food: FoodDetectionResult,
        nutrition: NutritionInfo
    ): Result<PediatricAnalysis> {
        return try {
            val prompt = """
                Eres un pediatra nutriólogo con experiencia clínica en México.
                Guías de referencia: NOM-043-SSA2, AAP, OMS.

                INFORMACIÓN DEL NIÑO:
                - Nombre: ${child.name}
                - Peso: ${child.weightKg} kg
                - Alergias: ${if (child.hasAllergies) child.allergiesDetail else "ninguna"}

                ALIMENTO IDENTIFICADO:
                - Nombre: ${food.foodName} (${food.foodType})
                - Calorías: ${nutrition.calories} kcal, Proteína: ${nutrition.protein}g, Azúcar: ${nutrition.sugar}g

                Devuelve ÚNICAMENTE este JSON:
                {
                  "recommended": true,
                  "recommended_portion": "porción calculada para la edad",
                  "benefits": ["beneficio 1", "beneficio 2"],
                  "warnings": ["advertencia si aplica"],
                  "frequency": "2 a 3 veces por semana"
                }
            """.trimIndent()

            val rawBody = queryGroqText(prompt, 600)
                ?: return Result.failure(Exception("Error al comunicarse con el asistente de análisis pediátrico"))

            val content = extractGroqContent(rawBody)
            val cleaned = extractJsonSubstring(content)
            val obj = runCatching { json.parseToJsonElement(cleaned).jsonObject }.getOrElse {
                buildJsonObject {
                    put("recommended", true)
                    put("recommended_portion", "Porción moderada infantil")
                    putJsonArray("benefits") { add("Aporte de nutrientes esenciales") }
                    putJsonArray("warnings") {}
                    put("frequency", "2 a 3 veces por semana")
                }
            }

            val isRec = obj["recommended"]?.jsonPrimitive?.booleanOrNull ?: false
            val portion = obj["recommended_portion"]?.jsonPrimitive?.contentOrNull ?: if (isRec) "Porción moderada infantil" else "Evitar"
            val benefits = obj["benefits"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val warnings = obj["warnings"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val freq = obj["frequency"]?.jsonPrimitive?.contentOrNull ?: if (isRec) "2 a 3 veces por semana" else "Evitar"

            val analysis = PediatricAnalysis(
                recommended = isRec,
                recommendedPortion = portion,
                benefits = benefits,
                warnings = warnings,
                frequency = freq
            )
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(Exception("Error en análisis pediátrico: ${e.message}"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANÁLISIS PARA EMBARAZO
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun analizarParaEmbarazo(
        perfil: PerfilEmbarazo?,
        food: FoodDetectionResult,
        nutrition: NutritionInfo
    ): Result<PediatricAnalysis> {
        return try {
            val semanasText = if (perfil != null) "${perfil.semanas} semanas de gestación" else "Gestación"
            val condicionesText = if (perfil != null && perfil.condiciones.isNotEmpty()) perfil.condiciones.joinToString(", ") else "ninguna"

            val prompt = """
                Eres un ginecólogo y nutriólogo materno-infantil experto en nutrición en el embarazo en México.
                Guías: NOM-007-SSA2, ACOG.

                INFORMACIÓN DE LA MUJER EMBARAZADA:
                - Estado: $semanasText
                - Condiciones médicas: $condicionesText

                ALIMENTO IDENTIFICADO:
                - Nombre: ${food.foodName} (${food.foodType})
                - Calorías: ${nutrition.calories} kcal, Proteína: ${nutrition.protein}g

                Devuelve ÚNICAMENTE este JSON:
                {
                  "recommended": true,
                  "recommended_portion": "porción sugerida en el embarazo",
                  "benefits": ["beneficio 1"],
                  "warnings": ["advertencia sobre Listeria, toxoplasmosis o mercurio si aplica"],
                  "frequency": "frecuencia recomendada"
                }
            """.trimIndent()

            val rawBody = queryGroqText(prompt, 600)
                ?: return Result.failure(Exception("Error al comunicarse con el asistente de análisis gestacional"))

            val content = extractGroqContent(rawBody)
            val cleaned = extractJsonSubstring(content)
            val obj = runCatching { json.parseToJsonElement(cleaned).jsonObject }.getOrElse {
                buildJsonObject {
                    put("recommended", true)
                    put("recommended_portion", "Porción moderada")
                    putJsonArray("benefits") { add("Nutrición prenatal recomendada") }
                    putJsonArray("warnings") {}
                    put("frequency", "3 a 4 veces por semana")
                }
            }

            val isRec = obj["recommended"]?.jsonPrimitive?.booleanOrNull ?: false
            val portion = obj["recommended_portion"]?.jsonPrimitive?.contentOrNull ?: if (isRec) "Porción moderada" else "Evitar en gestación"
            val benefits = obj["benefits"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val warnings = obj["warnings"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val freq = obj["frequency"]?.jsonPrimitive?.contentOrNull ?: if (isRec) "3 a 4 veces por semana" else "Evitar"

            val analysis = PediatricAnalysis(
                recommended = isRec,
                recommendedPortion = portion,
                benefits = benefits,
                warnings = warnings,
                frequency = freq
            )
            Result.success(analysis)
        } catch (e: Exception) {
            Result.failure(Exception("Error en análisis de embarazo: ${e.message}"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PERSISTENCIA EN FIRESTORE
    // ══════════════════════════════════════════════════════════════════════════

    fun hashAlimento(foodName: String) = foodName.lowercase().trim()
        .replace(Regex("[^a-záéíóúñü0-9]"), "_")

    suspend fun buscarEnCache(foodHash: String): Pair<NutritionInfo, PediatricAnalysis>? {
        return try {
            if (auth.currentUser == null) return null
            val snapshot = colCache().document(foodHash).get()
            if (!snapshot.exists) return null
            val nutritionStr: String = snapshot.get("nutrition") ?: return null
            val analysisStr: String = snapshot.get("analysis") ?: return null
            val nutrition = json.decodeFromString<NutritionInfo>(nutritionStr)
            val analysis = json.decodeFromString<PediatricAnalysis>(analysisStr)
            Pair(nutrition, analysis)
        } catch (_: Exception) { null }
    }

    suspend fun guardarEnCache(foodHash: String, nutrition: NutritionInfo, analysis: PediatricAnalysis) {
        try {
            if (auth.currentUser == null) return
            val data = mapOf(
                "nutrition" to json.encodeToString(nutrition),
                "analysis" to json.encodeToString(analysis),
                "actualizadoEn" to currentTimeMillis()
            )
            colCache().document(foodHash).set(data)
        } catch (_: Exception) { /* Silencioso */ }
    }

    suspend fun guardarAnalisis(childId: String, analisis: AnalisisCompleto): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val docId = analisis.id.ifBlank { generateUUID() }
            colAnalisis(childId).document(docId).set(
                mapOf(
                    "id" to docId,
                    "childId" to childId,
                    "userId" to currentUid,
                    "fecha" to analisis.fecha,
                    "foodName" to analisis.foodDetection.foodName,
                    "foodType" to analisis.foodDetection.foodType,
                    "calories" to analisis.nutrition.calories,
                    "protein" to analisis.nutrition.protein,
                    "carbs" to analisis.nutrition.carbohydrates,
                    "fat" to analisis.nutrition.fat,
                    "recommended" to analisis.analysis.recommended,
                    "portion" to analisis.analysis.recommendedPortion,
                    "warnings" to analisis.analysis.warnings,
                    "benefits" to analisis.analysis.benefits,
                    "frequency" to analisis.analysis.frequency,
                    "creadoEn" to currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error guardando análisis: ${e.message}"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════════

    private suspend fun queryGroqText(prompt: String, maxTokens: Int): String? {
        val apiKey = PlatformConfig.groqApiKey
        if (apiKey.isBlank()) return null

        val primaryRemote = RemoteConfigManager.getPrimaryModel()
        val candidateModels = (listOf(primaryRemote) + listOf(
            "openai/gpt-oss-120b",
            "openai/gpt-oss-20b",
            "qwen/qwen3.6-27b",
            "gemma2-9b-it",
            "llama-3.1-8b-instant",
            "llama3-70b-8192",
            "llama3-8b-8192",
            "groq/compound-mini"
        )).distinct()

        for (model in candidateModels) {
            val payload = buildGroqRequest(model, prompt, maxTokens)
            val result = PlatformHttp.postJson(
                url = "https://api.groq.com/openai/v1/chat/completions",
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json; charset=utf-8"
                ),
                jsonBody = payload,
                timeoutMs = 25000L
            )
            if (result.isSuccess) {
                val body = result.getOrNull()
                if (!body.isNullOrBlank()) return body
            }
        }
        return null
    }

    private fun buildGroqRequest(model: String, prompt: String, maxTokens: Int): String {
        return buildJsonObject {
            put("model", model)
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    put("content", prompt)
                }
            }
            put("max_tokens", maxTokens)
            put("temperature", 0.1)
        }.toString()
    }

    private fun extractGroqContent(rawBody: String): String {
        return try {
            val root = json.parseToJsonElement(rawBody).jsonObject
            root["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.contentOrNull ?: ""
        } catch (_: Exception) {
            rawBody
        }
    }

    private fun extractJsonSubstring(input: String): String {
        var str = input.trim()
        if (str.contains("<think>") && str.contains("</think>")) {
            str = str.substringAfter("</think>").trim()
        }
        if (str.contains("```json")) {
            str = str.substringAfter("```json").substringBefore("```").trim()
        } else if (str.contains("```")) {
            str = str.substringAfter("```").substringBefore("```").trim()
        }

        val start = str.indexOf('{')
        if (start != -1) {
            var openBrackets = 0
            var end = -1
            var inString = false
            var escape = false
            for (i in start until str.length) {
                val c = str[i]
                if (escape) {
                    escape = false
                    continue
                }
                if (c == '\\') {
                    escape = true
                    continue
                }
                if (c == '"') {
                    inString = !inString
                    continue
                }
                if (!inString) {
                    if (c == '{') openBrackets++
                    else if (c == '}') {
                        openBrackets--
                        if (openBrackets == 0) {
                            end = i
                            break
                        }
                    }
                }
            }
            if (end != -1) {
                str = str.substring(start, end + 1)
            } else {
                val lastClose = str.lastIndexOf('}')
                if (lastClose > start) {
                    str = str.substring(start, lastClose + 1)
                }
            }
        } else {
            val escaped = str.replace("\"", "\\\"").replace("\n", " ").take(100)
            return """{"foodName": "$escaped", "ingredients": ["$escaped"], "foodType": "comida", "confidence": 0.85}"""
        }

        str = str.replace(Regex(",\\s*([}\\]])"), "$1")
        str = str.replace(Regex(":\\s*,"), ": \"\",")
        str = str.replace(Regex(":\\s*}"), ": \"\"}")
        str = str.replace(Regex("\"\\s*\\n\\s*\""), "\",\n\"")
        return str
    }
}