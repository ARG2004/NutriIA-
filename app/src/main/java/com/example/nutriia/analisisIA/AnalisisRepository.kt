package com.example.nutriia.analisisIA

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import android.util.Log
import com.example.nutriia.BuildConfig
import com.example.nutriia.accesibilidad.KeyDeobfuscator
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ui.theme.ChildProfile
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

private const val TAG = "NutriIA_Analisis"

class AnalisisRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun colAnalisis(childId: String) =
        uid().let { uid ->
            db.collection("usuarios").document(uid)
                .collection("hijos").document(childId)
                .collection("analisis_ia")
        }

    private fun colCache() =
        db.collection("usuarios").document(uid()).collection("analisis_cache")

    private fun uid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado")

    // ══════════════════════════════════════════════════════════════════════════
    // DETECCIÓN DE ALIMENTO — Prompt mejorado con ejemplos de confusión comunes
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun detectarAlimento(imageFile: File): Result<FoodDetectionResult> {
        return try {
            val apiKey = KeyDeobfuscator.deobfuscate(BuildConfig.GROQ_API_KEY)
            if (apiKey.isBlank())
                return Result.failure(Exception("GROQ_API_KEY no configurada en local.properties"))

            val imageBytes  = compressImageFile(imageFile)
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

            Log.d(TAG, "[VISION] Enviando imagen comprimida a Groq Vision (${imageBytes.size / 1024}KB)...")

            val prompt = """
                Eres un experto en nutrición, gastronomía Y visión por computadora.
                Tu tarea es identificar con máxima precisión el contenido de la imagen.

                REGLA CRÍTICA PARA OBJETOS NO COMESTIBLES:
                - Si la foto contiene un objeto, mueble, electrónico, herramienta u artículo de escritorio (ejemplo: laptop, computadora, lámpara, organizador de escritorio con tijeras y regla, celular, juguete, libro, etc.) Y NO contiene comida:
                - DEBES identificar el objeto REAL en `foodName` (ejemplo: 'Organizador de escritorio con tijeras y regla', 'Laptop / Computadora portátil', 'Lámpara de escritorio', 'Teléfono celular').
                - En `foodType` escribe 'objeto_no_comestible'.
                - En `ingredients` pon una lista vacía `[]` o los elementos visibles (ej: ['tijeras', 'regla', 'portalápices']).
                - En `confidence` pon tu certeza real (ej: 0.90).
                - NUNCA inventes que un objeto es comida ni devuelvas "Alimento detectado" o "Alimento desconocido". Escribe el nombre REAL del objeto.

                IMPORTANTE — Identificación de alimentos y errores comunes a evitar:
                - HUEVO / HUEVOS: Identifica el huevo en todas sus presentaciones (huevo revuelto, huevo estrellado/frito, huevo cocido/duro, omelette, huevo a la mexicana con jitomate/cebolla). NUNCA confundas huevos revueltos o estrellados con puré de papa, papas fritas, queso derretido, mantequilla ni crepas.
                - La LECHUGA NO es col/repollo.
                - La PIÑA en cubos NO es puré de papa.
                - La CARNE MOLIDA NO son tortitas de harina.

                Analiza esta imagen y responde ÚNICAMENTE con un JSON con este formato exacto:
                {
                  "foodName": "nombre descriptivo exacto (ej: 'Huevo revuelto con verduras', 'Huevo estrellado', 'Huevo cocido', 'Laptop Lenovo negra', 'Lechuga romana')",
                  "ingredients": ["ingrediente1", "ingrediente2"],
                  "foodType": "uno de: objeto_no_comestible|desayuno|comida|cena|snack|bebida|fruta|verdura|cereal|lacteo|producto_empacado",
                  "confidence": 0.95,
                  "isProcessed": false,
                  "origin": "uno de: mexicano|estadounidense|internacional|desconocido"
                }

                Devuelve SOLO el JSON, sin texto adicional ni markdown. NO incluyas etiquetas <think> ni explicaciones. Responde comenzando con '{'.
            """.trimIndent()

            val visionModels = listOf(
                "llama-3.2-11b-vision-preview",
                "llama-3.2-90b-vision-preview"
            )

            var rawBody: String? = null
            var lastErrorCode = 404
            var lastErrorMsg = "No se pudo conectar a Groq Vision"

            for (modelName in visionModels) {
                val requestMap = mapOf(
                    "model" to modelName,
                    "messages" to listOf(
                        mapOf(
                            "role" to "user",
                            "content" to listOf(
                                mapOf(
                                    "type"      to "image_url",
                                    "image_url" to mapOf("url" to "data:image/jpeg;base64,$base64Image")
                                ),
                                mapOf("type" to "text", "text" to prompt)
                            )
                        )
                    ),
                    "max_tokens" to 1000,
                    "temperature" to 0.05
                )
                val requestBody = gson.toJson(requestMap)

                for (attempt in 1..2) {
                    try {
                        val response = http.newCall(
                            Request.Builder()
                                .url("https://api.groq.com/openai/v1/chat/completions")
                                .addHeader("Authorization", "Bearer $apiKey")
                                .addHeader("User-Agent", "NutriIA/1.0 (Android)")
                                .post(requestBody.toRequestBody(jsonMediaType))
                                .build()
                        ).execute()

                        Log.d(TAG, "[VISION] Modelo '$modelName' (Intento $attempt) HTTP ${response.code}")

                        if (response.isSuccessful) {
                            rawBody = response.body?.string()
                            if (!rawBody.isNullOrEmpty()) break
                        } else {
                            lastErrorCode = response.code
                            lastErrorMsg = response.body?.string() ?: ""
                            Log.w(TAG, "[VISION] Modelo '$modelName' fallo: HTTP ${response.code} $lastErrorMsg")

                            if (response.code == 429 && attempt == 1) {
                                Log.w(TAG, "[VISION] Rate Limit 429 alcanzado. Esperando 6.5s para reintento automático...")
                                kotlinx.coroutines.delay(6500)
                                continue
                            }
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "[VISION] Excepción con modelo '$modelName': ${e.message}")
                    }
                    break
                }
                if (!rawBody.isNullOrEmpty()) break
            }

            if (rawBody.isNullOrEmpty()) {
                val userMsg = if (lastErrorCode == 429) {
                    "Límite de peticiones por minuto alcanzado (Groq Rate Limit). Por favor espera 6 segundos e intenta de nuevo."
                } else {
                    "Error Groq Vision: $lastErrorCode"
                }
                return Result.failure(Exception(userMsg))
            }
            Log.d(TAG, "[VISION] OK: ${rawBody.take(400)}")

            val content = extractGroqContent(rawBody)
            val cleaned = extractJsonSubstring(content)

            val jsonElement = try {
                JsonParser.parseString(cleaned)
            } catch (_: Exception) {
                JsonParser.parseString("{}")
            }

            val detection = if (jsonElement.isJsonObject) {
                val obj = jsonElement.asJsonObject
                val ingArr = if (obj.has("ingredients") && obj.get("ingredients").isJsonArray) obj.getAsJsonArray("ingredients") else null
                val rawName = if (obj.has("foodName") && !obj.get("foodName").isJsonNull) obj.get("foodName").asString else "Objeto detectado"
                val finalName = if (rawName.equals("Alimento detectado", ignoreCase = true) || rawName.equals("Alimento desconocido", ignoreCase = true)) "Objeto no alimenticio" else rawName

                FoodDetectionResult(
                    foodName    = finalName,
                    ingredients = if (ingArr != null) (0 until ingArr.size()).map { ingArr[it].asString } else emptyList(),
                    foodType    = if (obj.has("foodType") && !obj.get("foodType").isJsonNull) obj.get("foodType").asString else "objeto_no_comestible",
                    confidence  = if (obj.has("confidence") && !obj.get("confidence").isJsonNull) obj.get("confidence").asDouble else 0.90
                )
            } else {
                FoodDetectionResult(
                    foodName    = cleaned.take(60).ifBlank { "Objeto no alimenticio" },
                    ingredients = emptyList(),
                    foodType    = "objeto_no_comestible",
                    confidence  = 0.80
                )
            }

            Log.d(TAG, "[VISION] Detectado: ${detection.foodName} (${(detection.confidence * 100).toInt()}%)")
            Result.success(detection)

        } catch (e: Exception) {
            Log.e(TAG, "[VISION] Excepción: ${e.message}", e)
            Result.failure(Exception("Error detectando alimento: ${e.message}"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // NUTRICIÓN — Open Food Facts primero, luego LLM con datos USDA/INSP
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun obtenerNutricion(foodName: String): Result<NutritionInfo> {
        val offResult = buscarEnOpenFoodFacts(foodName)
        if (offResult != null) {
            Log.d(TAG, "[NUTRITION] Encontrado en Open Food Facts: $foodName")
            return Result.success(offResult)
        }
        Log.d(TAG, "[NUTRITION] No encontrado en OFF, estimando con LLM: $foodName")
        return estimarNutricionConLLM(foodName)
    }

    private suspend fun buscarEnOpenFoodFacts(foodName: String): NutritionInfo? {
        return try {
            val query = URLEncoder.encode(foodName, "UTF-8")
            val url = "https://world.openfoodfacts.org/cgi/search.pl" +
                    "?search_terms=$query" +
                    "&search_simple=1" +
                    "&action=process" +
                    "&json=1" +
                    "&page_size=5" +
                    "&lc=es,en" +
                    "&fields=product_name,nutriments,serving_size"

            Log.d(TAG, "[OFF] Buscando: $foodName")

            val response = http.newCall(
                Request.Builder().url(url).get()
                    .addHeader("User-Agent", "NutriIA-Android/2.0 (contacto@nutriia.app)")
                    .build()
            ).execute()

            if (!response.isSuccessful) return null

            val body     = response.body?.string() ?: return null
            val json     = JsonParser.parseString(body).asJsonObject
            val products = json.getAsJsonArray("products") ?: return null

            if (products.size() == 0) return null

            for (i in 0 until minOf(products.size(), 5)) {
                val product    = products[i].asJsonObject
                val nutriments = product.getAsJsonObject("nutriments") ?: continue

                val calories = nutriments.get("energy-kcal_100g")?.asDouble
                    ?: nutriments.get("energy-kcal")?.asDouble
                    ?: continue

                if (calories <= 0) continue

                return NutritionInfo(
                    calories      = calories,
                    protein       = nutriments.get("proteins_100g")?.asDouble ?: 0.0,
                    carbohydrates = nutriments.get("carbohydrates_100g")?.asDouble ?: 0.0,
                    fat           = nutriments.get("fat_100g")?.asDouble ?: 0.0,
                    sugar         = nutriments.get("sugars_100g")?.asDouble ?: 0.0,
                    fiber         = nutriments.get("fiber_100g")?.asDouble ?: 0.0,
                    sodium        = (nutriments.get("sodium_100g")?.asDouble ?: 0.0) * 1000
                )
            }
            null
        } catch (e: Exception) {
            Log.w(TAG, "[OFF] Error: ${e.message}")
            null
        }
    }

    private suspend fun estimarNutricionConLLM(foodName: String): Result<NutritionInfo> {
        return try {
            val apiKey = KeyDeobfuscator.deobfuscate(BuildConfig.GROQ_API_KEY)

            val prompt = """
                Eres un nutriólogo experto en composición de alimentos con acceso a:
                - Tablas nutricionales de México: INSP, INCMNSZ, NOM-043-SSA2
                - Tablas de EE.UU.: USDA FoodData Central, FDA
                - Datos de marcas comerciales mexicanas y estadounidenses

                REFERENCIA DE VALORES CORRECTOS (úsalos como guía de precisión):
                - Lechuga orejona/romana cruda: ~17 kcal, 1.2g prot, 2.9g carbs, 0.3g grasa
                - Lechuga iceberg cruda: ~14 kcal, 0.9g prot, 2.9g carbs, 0.1g grasa
                - Piña fresca cruda: ~50 kcal, 0.5g prot, 13g carbs, 0.1g grasa, 9.8g azúcar, 1.4g fibra
                - Papa cocida sin cáscara: ~86 kcal, 1.9g prot, 20g carbs, 0.1g grasa
                - Zanahoria cruda rallada: ~41 kcal, 0.9g prot, 10g carbs, 0.2g grasa, 4.7g azúcar
                - Col/repollo crudo: ~25 kcal, 1.3g prot, 5.8g carbs, 0.1g grasa
                - Carne molida res 80/20 cocida: ~254 kcal, 26g prot, 0g carbs, 17g grasa
                - Tortilla de maíz (25g c/u): ~52 kcal, 1.4g prot, 11g carbs, 0.7g grasa
                - Tortilla de harina (Tia Rosa): ~305 kcal, 7.7g prot, 44.8g carbs, 10.5g grasa (por 100g)
                - Aguacate Hass: ~160 kcal, 2g prot, 9g carbs, 15g grasa, 1.8g azúcar
                - Mango Ataulfo: ~60 kcal, 0.8g prot, 15g carbs, 0.4g grasa, 13g azúcar
                - Leche Alpura deslactosada: ~48 kcal, 3.1g prot, 4.8g carbs, 1.8g grasa (por 100ml)
                - Coca-Cola 600ml: ~240 kcal, 0g prot, 62g carbs, 0g grasa, 62g azúcar (por 100ml: 40 kcal)
                - Huevo entero cocido (100g): ~155 kcal, 12.6g prot, 1.1g carbs, 10.6g grasa
                - Huevo revuelto / estrellado (100g): ~148 kcal, 10g prot, 1g carbs, 11g grasa
                - Manzana: ~52 kcal, 0.3g prot, 14g carbs, 0.2g grasa, 10.4g azúcar

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

                REGLAS CRÍTICAS:
                - USA los valores de referencia de arriba cuando el alimento coincida
                - Para verduras frescas: calorias suelen ser bajas (10-50 kcal/100g)
                - Para frutas frescas: calorías 40-80 kcal/100g, azúcar 5-15g
                - Para carnes cocidas: proteína alta (20-30g), grasa variable
                - Para productos empacados de marca: usa los datos del empaque si los conoces
                - sodium en miligramos, todo lo demás en gramos o kcal
                - Si el alimento es variable, usa el valor promedio más común
                - NO inflés ni deflés los valores: la precisión es crítica para niños
            """.trimIndent()

            val requestBody = buildGroqRequest("openai/gpt-oss-120b", prompt, 250)

            val response = http.newCall(
                Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("User-Agent", "NutriIA/1.0 (Android)")
                    .post(requestBody.toRequestBody(jsonMediaType))
                    .build()
            ).execute()

            if (!response.isSuccessful) return Result.success(NutritionInfo())

            val rawBody = response.body?.string() ?: return Result.success(NutritionInfo())
            val content = extractGroqContent(rawBody)
            val cleaned = content.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val result = gson.fromJson(cleaned, NutritionInfo::class.java)
            Log.d(TAG, "[LLM-NUTRITION] Estimado para $foodName: ${result.calories} kcal")
            Result.success(result)

        } catch (e: Exception) {
            Log.w(TAG, "[LLM-NUTRITION] Error: ${e.message}")
            Result.success(NutritionInfo())
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ANÁLISIS PEDIÁTRICO
    // ══════════════════════════════════════════════════════════════════════════

    suspend fun analizarParaNino(
        child    : ChildProfile,
        food     : FoodDetectionResult,
        nutrition: NutritionInfo
    ): Result<PediatricAnalysis> {
        return try {
            val apiKey = KeyDeobfuscator.deobfuscate(BuildConfig.GROQ_API_KEY)
            if (apiKey.isBlank())
                return Result.failure(Exception("GROQ_API_KEY no configurada en local.properties"))

            val ageMonths = calcAgeMonths(child.birthDate)
            val ageText   = if (ageMonths >= 12) "${ageMonths / 12} años y ${ageMonths % 12} meses"
            else "$ageMonths meses"

            val allergiesText  = if (child.hasAllergies) child.allergiesDetail else "ninguna conocida"
            val conditionsText = if (child.hasConditions) child.conditionsDetail else "ninguna"

            val prompt = """
                Eres un pediatra nutriólogo con experiencia clínica en México, experto en
                alimentación infantil y conocimiento de las guías internacionales de nutrición.

                Guías de referencia:
                - México: NOM-043-SSA2, Secretaría de Salud, INSP, INCMNSZ
                - Internacional: AAP (American Academy of Pediatrics), USDA MyPlate,
                  OMS/OPS guías de alimentación complementaria y escolar

                INFORMACIÓN DEL NIÑO:
                - Edad: $ageText
                - Peso: ${child.weightKg} kg
                - Alergias conocidas: $allergiesText
                - Condiciones médicas: $conditionsText

                ALIMENTO IDENTIFICADO:
                - Nombre: ${food.foodName}
                - Tipo de alimento: ${food.foodType}
                - Ingredientes detectados: ${food.ingredients.joinToString(", ")}
                - ¿Es producto procesado?: ${if (food.ingredients.isEmpty()) "no determinado" else "basado en ingredientes"}

                VALORES NUTRICIONALES (por 100g o 100ml):
                - Calorías: ${nutrition.calories} kcal
                - Proteína: ${nutrition.protein} g
                - Carbohidratos: ${nutrition.carbohydrates} g
                - Grasas totales: ${nutrition.fat} g
                - Azúcares: ${nutrition.sugar} g
                - Fibra: ${nutrition.fiber} g
                - Sodio: ${nutrition.sodium} mg

                CRITERIOS DE ANÁLISIS según edad:
                
                LACTANTES (< 6 meses): Solo leche materna o fórmula.
                INICIO COMPLEMENTARIA (6-8 meses): Purés suaves, sin sal ni azúcar añadida.
                  Evitar: miel, mariscos, lácteos como bebida, frutos secos enteros.
                BEBÉS (9-11 meses): Trozos pequeños blandos, variedad.
                NIÑOS 1-3 años: Porciones pequeñas, evitar sodio >700mg/día, azúcar añadida.
                  Evitar: bebidas azucaradas, comida muy picante, embutidos en exceso.
                PREESCOLARES 3-5 años: Porciones adaptadas. Límite sodio <900mg/día.
                ESCOLARES 6-12 años: Porciones normales. Límite sodio <1200mg/día.

                ALERTAS AUTOMÁTICAS que debes considerar:
                - Sodio > 300mg por porción típica → advertencia
                - Azúcar añadida > 6g por porción → advertencia
                - Grasas saturadas altas → mencionar moderación
                - Alimentos ultra-procesados → mencionar frecuencia baja
                - Bebidas azucaradas (refrescos, jugos) → advertencia en menores

                REGLAS OBLIGATORIAS:
                - DEBES calcular y especificar una porción sugerida adaptada de forma estricta a un niño de $ageText (${child.weightKg} kg).
                - recommended_portion NUNCA debe estar vacío. Si se recomienda, indica la cantidad exacta (ej: '1/2 taza (70g)', '1 pieza pequeña'). Si NO se recomienda, escribe 'Evitar o 0g (No recomendado a esta edad)'.
                - Devuelve ÚNICAMENTE este JSON (sin markdown ni etiquetas <think>):
                {
                  "recommended": true,
                  "recommended_portion": "porción calculada para la edad de $ageText",
                  "benefits": ["beneficio específico para edad de $ageText 1", "beneficio 2"],
                  "warnings": ["advertencia si aplica"],
                  "frequency": "frecuencia adaptada para $ageText"
                }

                - Si no hay advertencias, deja el arreglo warnings vacío: []
                - Máximo 3 benefits y 3 warnings
                - Devuelve SOLO el JSON, sin texto adicional ni <think>.
            """.trimIndent()

            val requestBody = buildGroqRequest("openai/gpt-oss-120b", prompt, 700)

            Log.d(TAG, "[LLM] Analizando ${food.foodName} para niño de $ageText...")

            val response = http.newCall(
                Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("User-Agent", "NutriIA/1.0 (Android)")
                    .post(requestBody.toRequestBody(jsonMediaType))
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "(sin cuerpo)"
                return Result.failure(Exception("Error Groq LLM: ${response.code} - $errorBody"))
            }

            val rawBody = response.body?.string() ?: ""
            val content = extractGroqContent(rawBody)
            val cleaned = extractJsonSubstring(content)

            val jsonElement = try { JsonParser.parseString(cleaned) } catch (_: Exception) { JsonParser.parseString("{}") }
            val result = if (jsonElement.isJsonObject) jsonElement.asJsonObject else JsonObject()

            val benefitsArr = if (result.has("benefits") && result.get("benefits").isJsonArray) result.getAsJsonArray("benefits") else null
            val warningsArr = if (result.has("warnings") && result.get("warnings").isJsonArray) result.getAsJsonArray("warnings") else null

            val rawPortion = result.get("recommended_portion")?.asString ?: ""
            val isRec = result.get("recommended")?.asBoolean ?: false
            val portionText = if (rawPortion.isNotBlank()) rawPortion else if (isRec) "Porción pequeña adaptada para $ageText" else "0g / Evitar en esta etapa"

            val analysis = PediatricAnalysis(
                recommended        = isRec,
                recommendedPortion = portionText,
                benefits           = (0 until (benefitsArr?.size() ?: 0)).map { benefitsArr!![it].asString },
                warnings           = (0 until (warningsArr?.size() ?: 0)).map { warningsArr!![it].asString },
                frequency          = result.get("frequency")?.asString ?: if (isRec) "2-3 veces por semana" else "Evitar"
            )

            Log.d(TAG, "[LLM] Análisis listo para $ageText. Recomendado: ${analysis.recommended}, Porción: ${analysis.recommendedPortion}")
            Result.success(analysis)

        } catch (e: Exception) {
            Log.e(TAG, "[LLM] Excepción: ${e.message}", e)
            Result.failure(Exception("Error en análisis pediátrico: ${e.message}"))
        }
    }

    suspend fun analizarParaEmbarazo(
        perfil   : PerfilEmbarazo?,
        food     : FoodDetectionResult,
        nutrition: NutritionInfo
    ): Result<PediatricAnalysis> {
        return try {
            val apiKey = KeyDeobfuscator.deobfuscate(BuildConfig.GROQ_API_KEY)
            if (apiKey.isBlank())
                return Result.failure(Exception("GROQ_API_KEY no configurada en local.properties"))

            val semanasText = if (perfil != null) "${perfil.semanas} semanas de gestación (Trimestre ${if (perfil.semanas <= 13) 1 else if (perfil.semanas <= 27) 2 else 3})" else "Periodo de embarazo/gestación"
            val condicionesText = if (perfil != null && perfil.condiciones.isNotEmpty()) perfil.condiciones.joinToString(", ") else "ninguna declarada"
            val alergiasText = if (perfil != null && perfil.allergiesDetail.isNotBlank()) perfil.allergiesDetail else "ninguna declarada"

            val prompt = """
                Eres un ginecólogo y nutriólogo materno-infantil experto en salud y nutrición durante el embarazo en México.
                Guías de referencia: NOM-007-SSA2, INSP, NOM-043-SSA2, ACOG (American College of Obstetricians and Gynecologists).

                INFORMACIÓN DE LA MUJER EMBARAZADA:
                - Estado: $semanasText
                - Condiciones médicas / Síntomas: $condicionesText
                - Alergias / Restricciones: $alergiasText

                ALIMENTO IDENTIFICADO:
                - Nombre: ${food.foodName}
                - Tipo de alimento: ${food.foodType}
                - Ingredientes detectados: ${food.ingredients.joinToString(", ")}

                VALORES NUTRICIONALES (por 100g o 100ml):
                - Calorías: ${nutrition.calories} kcal
                - Proteína: ${nutrition.protein} g
                - Carbohidratos: ${nutrition.carbohydrates} g
                - Grasas totales: ${nutrition.fat} g
                - Azúcares: ${nutrition.sugar} g
                - Fibra: ${nutrition.fiber} g
                - Sodio: ${nutrition.sodium} mg

                CRITERIOS DE SEGURIDAD Y NUTRICIÓN EN EMBARAZO:
                - SEGURIDAD ALIMENTARIA (REGLA RIGUROSA DE ALTO RIESGO):
                  * Evitar o marcar con advertencia crítica: Pescado/marisco crudo (sushi), carnes crudas/poco cocidas, huevo crudo/poco cocido, lácteos o quesos no pasteurizados (riesgo de Listeria o Toxoplasmosis), pescado con alto contenido de mercurio, alcohol o cafeína excesiva (>200mg/día).
                  * Si el alimento es de alto riesgo → recommended = false, con advertencia clara sobre Listeria, Toxoplasmosis o Mercurio.
                - BENEFICIOS RECOMENDADOS EN EMBARAZO:
                  * Aporte de ácido fólico, hierro, calcio, proteína, fibra (alivio de estreñimiento), omega-3 o hidratación.
                - PORCIÓN SUGERIDA EN EMBARAZO:
                  * recommended_portion NUNCA debe estar vacío. Si es recomendado, indica porción adecuada (ej: '1 porción de 150g', '1 pieza mediana'). Si NO es recomendado, indica 'Evitar durante el embarazo (0g)'.

                REGLAS OBLIGATORIAS:
                - Devuelve ÚNICAMENTE este JSON (sin markdown ni etiquetas <think>):
                {
                  "recommended": true,
                  "recommended_portion": "porción adecuada para el embarazo",
                  "benefits": ["beneficio específico 1", "beneficio 2"],
                  "warnings": ["advertencia sobre seguridad en embarazo si aplica"],
                  "frequency": "frecuencia recomendada en la gestación"
                }

                - Si no hay advertencias, deja el arreglo warnings vacío: []
                - Máximo 3 benefits y 3 warnings
                - Devuelve SOLO el JSON, sin texto adicional ni <think>.
            """.trimIndent()

            val requestBody = buildGroqRequest("openai/gpt-oss-120b", prompt, 700)

            Log.d(TAG, "[LLM] Analizando ${food.foodName} para embarazo ($semanasText)...")

            val response = http.newCall(
                Request.Builder()
                    .url("https://api.groq.com/openai/v1/chat/completions")
                    .addHeader("Authorization", "Bearer $apiKey")
                    .addHeader("User-Agent", "NutriIA/1.0 (Android)")
                    .post(requestBody.toRequestBody(jsonMediaType))
                    .build()
            ).execute()

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "(sin cuerpo)"
                return Result.failure(Exception("Error Groq LLM: ${response.code} - $errorBody"))
            }

            val rawBody = response.body?.string() ?: ""
            val content = extractGroqContent(rawBody)
            val cleaned = extractJsonSubstring(content)

            val jsonElement = try { JsonParser.parseString(cleaned) } catch (_: Exception) { JsonParser.parseString("{}") }
            val result = if (jsonElement.isJsonObject) jsonElement.asJsonObject else JsonObject()

            val benefitsArr = if (result.has("benefits") && result.get("benefits").isJsonArray) result.getAsJsonArray("benefits") else null
            val warningsArr = if (result.has("warnings") && result.get("warnings").isJsonArray) result.getAsJsonArray("warnings") else null

            val rawPortion = result.get("recommended_portion")?.asString ?: ""
            val isRec = result.get("recommended")?.asBoolean ?: false
            val portionText = if (rawPortion.isNotBlank()) rawPortion else if (isRec) "Porción moderada para el embarazo" else "0g / Evitar en el embarazo"

            val analysis = PediatricAnalysis(
                recommended        = isRec,
                recommendedPortion = portionText,
                benefits           = (0 until (benefitsArr?.size() ?: 0)).map { benefitsArr!![it].asString },
                warnings           = (0 until (warningsArr?.size() ?: 0)).map { warningsArr!![it].asString },
                frequency          = result.get("frequency")?.asString ?: if (isRec) "3-4 veces por semana" else "Evitar en la gestación"
            )

            Log.d(TAG, "[LLM] Análisis embarazo listo. Recomendado: ${analysis.recommended}, Porción: ${analysis.recommendedPortion}")
            Result.success(analysis)

        } catch (e: Exception) {
            Log.e(TAG, "[LLM-Embarazo] Excepción: ${e.message}", e)
            Result.failure(Exception("Error en análisis de embarazo: ${e.message}"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CACHÉ Y PERSISTENCIA
    // ══════════════════════════════════════════════════════════════════════════

    fun hashAlimento(foodName: String) = foodName.lowercase().trim()
        .replace(Regex("[^a-záéíóúñü0-9]"), "_")

    suspend fun buscarEnCache(foodHash: String): Pair<NutritionInfo, PediatricAnalysis>? {
        return try {
            if (auth.currentUser == null) return null
            val doc = colCache().document(foodHash).get().await()
            if (!doc.exists()) return null
            val creadoEn = doc.getLong("creadoEn") ?: return null
            val diasTranscurridos = (System.currentTimeMillis() - creadoEn) / (1000 * 60 * 60 * 24)
            if (diasTranscurridos > 7) return null
            val nutrition = gson.fromJson(doc.getString("nutrition") ?: return null, NutritionInfo::class.java)
            val analysis  = gson.fromJson(doc.getString("analysis")  ?: return null, PediatricAnalysis::class.java)
            Pair(nutrition, analysis)
        } catch (e: Exception) { null }
    }

    suspend fun guardarEnCache(foodHash: String, nutrition: NutritionInfo, analysis: PediatricAnalysis) {
        try {
            if (auth.currentUser == null) return
            colCache().document(foodHash).set(
                mapOf(
                    "foodHash"  to foodHash,
                    "nutrition" to gson.toJson(nutrition),
                    "analysis"  to gson.toJson(analysis),
                    "creadoEn"  to System.currentTimeMillis()
                )
            ).await()
        } catch (_: Exception) {}
    }

    suspend fun guardarAnalisis(childId: String, analisis: AnalisisCompleto): Result<Unit> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val docId = analisis.id.ifBlank { UUID.randomUUID().toString() }
            colAnalisis(childId).document(docId).set(
                mapOf(
                    "id"          to docId,
                    "childId"     to childId,
                    "userId"      to currentUid,
                    "fecha"       to analisis.fecha,
                    "foodName"    to analisis.foodDetection.foodName,
                    "foodType"    to analisis.foodDetection.foodType,
                    "calories"    to analisis.nutrition.calories,
                    "protein"     to analisis.nutrition.protein,
                    "carbs"       to analisis.nutrition.carbohydrates,
                    "fat"         to analisis.nutrition.fat,
                    "recommended" to analisis.analysis.recommended,
                    "portion"     to analisis.analysis.recommendedPortion,
                    "warnings"    to analisis.analysis.warnings,
                    "benefits"    to analisis.analysis.benefits,
                    "frequency"   to analisis.analysis.frequency,
                    "creadoEn"    to Timestamp.now()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Error guardando análisis: ${e.message}"))
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HELPERS PRIVADOS
    // ══════════════════════════════════════════════════════════════════════════

    private fun buildGroqRequest(model: String, prompt: String, maxTokens: Int): String {
        return gson.toJson(
            mapOf(
                "model"       to model,
                "messages"    to listOf(mapOf("role" to "user", "content" to prompt)),
                "max_tokens"  to maxTokens,
                "temperature" to 0.1
            )
        )
    }

    private fun extractGroqContent(rawBody: String): String {
        val json = JsonParser.parseString(rawBody).asJsonObject
        return json.getAsJsonArray("choices")
            .get(0).asJsonObject
            .getAsJsonObject("message")
            .get("content").asString
    }

    private fun extractJsonSubstring(input: String): String {
        var str = input.trim()
        if (str.contains("```json")) {
            str = str.substringAfter("```json").substringBefore("```").trim()
        } else if (str.contains("```")) {
            str = str.substringAfter("```").substringBefore("```").trim()
        }
        val start = str.indexOf('{')
        val end = str.lastIndexOf('}')
        return if (start != -1 && end != -1 && end >= start) {
            str.substring(start, end + 1)
        } else {
            str
        }
    }

    private fun calcAgeMonths(fechaNacimiento: String): Int {
        return try {
            val (anio, mes, dia) = if (fechaNacimiento.contains("/")) {
                val p = fechaNacimiento.split("/").map { it.toInt() }
                Triple(p[2], p[1], p[0])
            } else {
                val p = fechaNacimiento.split("-").map { it.toInt() }
                Triple(p[0], p[1], p[2])
            }
            val calNac = java.util.Calendar.getInstance().apply { set(anio, mes - 1, dia) }
            val calHoy = java.util.Calendar.getInstance()
            val diffYears = calHoy.get(java.util.Calendar.YEAR) - calNac.get(java.util.Calendar.YEAR)
            val diffMonths = calHoy.get(java.util.Calendar.MONTH) - calNac.get(java.util.Calendar.MONTH)
            val totalMonths = diffYears * 12 + diffMonths
            if (totalMonths < 0) 0 else totalMonths
        } catch (_: Exception) { 0 }
    }

    private fun compressImageFile(file: File): ByteArray {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_UNDEFINED
            )
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)

            var sampleSize = 1
            val maxDim = 1024
            while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
                ?: return file.readBytes()

            val finalBitmap = if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                bitmap.recycle()
                rotated
            } else {
                bitmap
            }

            val baos = ByteArrayOutputStream()
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val bytes = baos.toByteArray()

            // Overwrite original file so UI displays image 100% upright
            file.writeBytes(bytes)

            finalBitmap.recycle()
            bytes
        } catch (_: Exception) {
            file.readBytes()
        }
    }
}