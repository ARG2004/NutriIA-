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

    // ══════════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE ALIMENTO CON GROQ VISION
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun detectarAlimento(base64Image: String): Result<FoodDetectionResult> {
        return try {
            val apiKey = PlatformConfig.groqApiKey
            val prompt = """
                Eres un experto en nutrición, gastronomía Y visión por computadora.
                Tu tarea es identificar con máxima precisión el contenido de la imagen.

                REGLA CRÍTICA DE IDENTIFICACIÓN:
                1. EVALÚA PRIMERO SI LO QUE SE OBSERVA ES REALMENTE UN ALIMENTO O BEBIDA COMESTIBLE.
                2. Si NO es comida (ejemplos: control remoto, llaves, celular, tijeras, libreta, juguete, lápices, vaso o plato vacío, herramientas, calzado, muebles, manos o personas solas, etc.):
                   - `isEdible`: false
                   - `foodType`: 'objeto_no_comestible'
                   - En `foodName` describe EXACTAMENTE qué objetos se observan en la toma (ej: 'Control remoto y llaves sobre la mesa', 'Tijeras escolares y libreta', 'Plato vacío', 'Teléfono móvil').
                   - En `nonEdibleReason` describe por qué no es comestible (ej: 'Son objetos inanimados de uso cotidiano, no alimentos.').
                   - En `ingredients` pon `[]`.
                3. Si SÍ es comida o bebida:
                   - `isEdible`: true
                   - En `foodName` especifica con exactitud el platillo o ingrediente en español (ej: 'Huevos revueltos con jamón', 'Manzana picada', 'Pechuga de pollo con verduras', 'Taza de avena').
                   - En `foodType` selecciona uno de: 'desayuno'|'comida'|'cena'|'snack'|'bebida'|'fruta'|'verdura'|'cereal'|'lacteo'|'producto_empacado'.
                   - En `ingredients` enumera los ingredientes reconocidos.

                IMPORTANTE — Alimentos y no-alimentos:
                - ALIMENTOS ENTEROS Y CRUDOS (manzanas, plátanos, naranjas, aguacate, huevos crudos en cascarón, zanahorias crudas) SÍ SON COMIDA (`isEdible`: true).
                - HUEVOS: Identifícalos en todas sus variantes (revueltos, estrellados, cocidos, en cascarón). NUNCA confundas huevos revueltos con puré de papa o papas fritas.
                - Vajilla vacía, manteles, monedas, cables o controles NUNCA deben catalogarse como alimentos.

                Responde ÚNICAMENTE con un JSON con este formato exacto:
                {
                  "isEdible": true,
                  "foodName": "nombre descriptivo exacto (ej: 'Huevos revueltos con frijoles', 'Manzana roja', 'Control remoto')",
                  "foodType": "uno de: objeto_no_comestible|desayuno|comida|cena|snack|bebida|fruta|verdura|cereal|lacteo|producto_empacado",
                  "ingredients": ["ingrediente1", "ingrediente2"],
                  "confidence": 0.95,
                  "nonEdibleReason": "explicación si no es comestible o cadena vacía si es comida"
                }
                Devuelve SOLO el JSON, sin texto adicional ni markdown.
            """.trimIndent()

            val visionRemote = RemoteConfigManager.getVisionModel()
            val visionModels = (listOf(visionRemote) + listOf(
                "qwen/qwen3.8-27b",
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
                    ?: "Objeto no alimenticio"
                buildJsonObject {
                    put("isEdible", false)
                    put("foodName", nameMatch)
                    putJsonArray("ingredients") {}
                    put("foodType", "objeto_no_comestible")
                    put("confidence", 0.85)
                    put("nonEdibleReason", "No se detectaron alimentos comestibles.")
                }
            }

            val rawName = jsonObj["foodName"]?.jsonPrimitive?.contentOrNull ?: "Objeto no alimenticio"
            val finalName = if (rawName.equals("Alimento detectado", true) || rawName.equals("Alimento desconocido", true)) "Objeto no alimenticio" else rawName
            val ingArr = jsonObj["ingredients"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val parsedType = jsonObj["foodType"]?.jsonPrimitive?.contentOrNull ?: "objeto_no_comestible"
            val isEdibleVal = jsonObj["isEdible"]?.jsonPrimitive?.booleanOrNull ?: (parsedType != "objeto_no_comestible")
            val reasonVal = jsonObj["nonEdibleReason"]?.jsonPrimitive?.contentOrNull ?: ""
            val conf = jsonObj["confidence"]?.jsonPrimitive?.doubleOrNull ?: 0.90

            val esRealmenteComestible = isEdibleVal && parsedType != "objeto_no_comestible"

            val detection = FoodDetectionResult(
                foodName = finalName,
                ingredients = if (esRealmenteComestible) ingArr else emptyList(),
                foodType = if (esRealmenteComestible) parsedType else "objeto_no_comestible",
                confidence = conf,
                isEdible = esRealmenteComestible,
                nonEdibleReason = if (esRealmenteComestible) "" else (reasonVal.ifBlank { "No es un alimento comestible." })
            )
            Log.d(TAG, "[VISION] Detectado con éxito: ${detection.foodName} (Comestible: ${detection.isEdible}, ${detection.foodType})")
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
        // ── Si el objeto detectado no es comestible, evitar evaluación dietética ficticia ──
        if (food.foodType.equals("objeto_no_comestible", ignoreCase = true) || 
            food.foodName.contains("no comestible", ignoreCase = true) ||
            food.foodName.contains("objeto no alimenticio", ignoreCase = true)) {
            return Result.success(
                PediatricAnalysis(
                    recommended = false,
                    recommendedPortion = "No aplica",
                    benefits = emptyList(),
                    warnings = listOf(
                        "El elemento detectado '${food.foodName}' es un objeto no comestible.",
                        "El analizador nutricional de NutrIA está diseñado exclusivamente para evaluar alimentos, platillos y bebidas."
                    ),
                    frequency = "No aplica"
                )
            )
        }

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
        // ── Si el objeto detectado no es comestible, evitar evaluación dietética ficticia ──
        if (food.foodType.equals("objeto_no_comestible", ignoreCase = true) || 
            food.foodName.contains("no comestible", ignoreCase = true) ||
            food.foodName.contains("objeto no alimenticio", ignoreCase = true)) {
            return Result.success(
                PediatricAnalysis(
                    recommended = false,
                    recommendedPortion = "No aplica",
                    benefits = emptyList(),
                    warnings = listOf(
                        "El elemento detectado '${food.foodName}' es un objeto no comestible.",
                        "El analizador nutricional de NutrIA está diseñado exclusivamente para evaluar alimentos, platillos y bebidas."
                    ),
                    frequency = "No aplica"
                )
            )
        }

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
                  "recommended_portion": "porción recomendada para gestación",
                  "benefits": ["beneficio para madre y feto 1", "beneficio 2"],
                  "warnings": ["advertencia si aplica"],
                  "frequency": "adecuada para el trimestre"
                }
            """.trimIndent()

            val rawBody = queryGroqText(prompt, 600)
                ?: return Result.failure(Exception("Error al comunicarse con el asistente de embarazo"))

            val content = extractGroqContent(rawBody)
            val cleaned = extractJsonSubstring(content)
            val obj = runCatching { json.parseToJsonElement(cleaned).jsonObject }.getOrElse {
                buildJsonObject {
                    put("recommended", true)
                    put("recommended_portion", "Porción balanceada")
                    putJsonArray("benefits") { add("Aporte de nutrientes clave") }
                    putJsonArray("warnings") {}
                    put("frequency", "Frecuente")
                }
            }

            val isRec = obj["recommended"]?.jsonPrimitive?.booleanOrNull ?: false
            val portion = obj["recommended_portion"]?.jsonPrimitive?.contentOrNull ?: if (isRec) "Porción balanceada" else "Evitar"
            val benefits = obj["benefits"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val warnings = obj["warnings"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
            val freq = obj["frequency"]?.jsonPrimitive?.contentOrNull ?: if (isRec) "Frecuente" else "Evitar"

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
            "qwen/qwen3.8-27b",
            "qwen/qwen3.6-27b",
            "llama-3.3-70b-versatile",
            "gemma2-9b-it",
            "llama-3.1-8b-instant",
            "llama3-70b-8192",
            "llama3-8b-8192"
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