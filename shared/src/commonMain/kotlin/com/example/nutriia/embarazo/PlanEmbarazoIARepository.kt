package com.example.nutriia.embarazo

import android.util.Log
import com.example.nutriia.BuildConfig
import com.example.nutriia.accesibilidad.KeyDeobfuscator
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
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

class PlanEmbarazoIARepository {

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val gson = Gson()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun generarPlanIA(
        perfil: PerfilEmbarazo,
        recetasBase: List<RecetaEmbarazo>
    ): Result<List<PlanDietaEmbarazoSemanal>> = withContext(Dispatchers.IO) {
        try {
            val apiKey = KeyDeobfuscator.deobfuscate(BuildConfig.GROQ_API_KEY)
            if (apiKey.isBlank()) {
                return@withContext Result.failure(Exception("La API KEY de Groq no está configurada."))
            }

            val listadoRecetasTexto = recetasBase.joinToString("\n") { r ->
                "- ${r.nombre} (${r.tipoComida}): Ingredientes: ${r.ingredientes.joinToString(", ")}. Kcal: ${r.kcal}."
            }

            val systemPrompt = """
                Eres un asistente de Inteligencia Artificial experto en nutrición clínica materno-infantil y ginecología.
                Debes generar un plan de alimentación semanal personalizado y seguro para una mujer embarazada basándote en su perfil de salud.
                
                Perfil de la paciente:
                - Semanas de gestación: ${perfil.semanas} semanas (Trimestre: ${if (perfil.semanas <= 13) 1 else if (perfil.semanas <= 27) 2 else 3})
                - Edad: ${perfil.edad} años
                - Talla: ${perfil.tallaM} metros
                - Peso pre-gestacional: ${perfil.pesoPregestacionalKg} kg (IMC pre-gestacional: ${String.format("%.2f", perfil.imcPregestacional)})
                - ¿Embarazo gemelar?: ${if (perfil.esGemelar) "Sí" else "No"}
                - Condiciones clínicas y alergias: ${perfil.condiciones.joinToString(", ")} ${perfil.allergiesDetail}
                - Otras condiciones: ${perfil.otrasCondicionesTexto}
                
                Leyes y guías de referencia (NOM-007-SSA2-2016):
                - Si tiene diabetes gestacional: Restringir carbohidratos simples, evitar azúcares añadidos y jugos. Priorizar carbohidratos complejos y fibra.
                - Si tiene anemia: Aumentar ingredientes ricos en hierro (carnes rojas magras, leguminosas, espinacas) y vitamina C para mejorar la absorción. Evitar inhibidores de hierro (como té negro/té verde/café con alimentos).
                - Si reporta náuseas: Evitar alimentos fritos, muy grasosos o de olor penetrante (como pescado fuerte). Priorizar porciones pequeñas, secas y frecuentes.
                
                Recetas de referencia sugeridas de la base de datos de NutriIA:
                $listadoRecetasTexto
                
                Instrucciones:
                1. Diseña un menú de 7 días (Lunes a Domingo) con 5 comidas al día: desayuno, colacion1, comida, colacion2, cena.
                2. Los nombres de las comidas sugeridas deben basarse en las recetas de referencia o variaciones sumamente saludables de las mismas, adaptadas a su perfil clínico.
                3. Ajusta los macronutrientes y objetivos calóricos diarios (macros) de forma coherente para el trimestre de gestación y la condición clínica de la paciente.
                4. Debes responder EXCLUSIVAMENTE con un objeto JSON que contenga una lista "dias" que contenga 7 objetos con la estructura exacta de PlanDietaEmbarazoSemanal. No agregues explicaciones externas ni markdown fuera del bloque JSON.
                
                Estructura del JSON:
                {
                  "dias": [
                    {
                      "diaSemana": "Lunes",
                      "comidas": {
                        "desayuno": "Nombre del plato",
                        "colacion1": "Nombre de la colacion",
                        "comida": "Nombre del plato principal",
                        "colacion2": "Nombre de la colacion",
                        "cena": "Nombre del plato"
                      },
                      "macros": {
                        "caloriasExtra": 350,
                        "proteinasG": 71.0,
                        "hierroMg": 30.0,
                        "calcioMg": 1000.0,
                        "acidoFolicoUg": 600.0,
                        "omega3G": 0.3,
                        "aguaLitros": 2.3
                      },
                      "trimestre": "SEGUNDO"
                    }
                  ]
                }
                
                Asegúrate de usar para el trimestre uno de estos strings exactos: "PRIMERO", "SEGUNDO", "TERCERO".
            """.trimIndent()

            val messages = listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to "Genera el plan de dieta semanal en formato JSON.")
            )

            val requestBodyJson = gson.toJson(
                mapOf(
                    "model"            to "llama-3.3-70b-versatile",
                    "messages"         to messages,
                    "max_tokens"       to 1800,
                    "temperature"      to 0.3,
                    "response_format"  to mapOf("type" to "json_object")
                )
            )

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(requestBodyJson.toRequestBody(jsonMediaType))
                .build()

            val response = http.newCall(request).execute()
            if (!response.isSuccessful) {
                val errBody = response.body?.string() ?: ""
                Log.e("PlanEmbarazoIA", "Error response from Groq: ${response.code} $errBody")
                return@withContext Result.failure(Exception("Error de red con el asistente de IA: ${response.code}"))
            }

            val rawBody = response.body?.string() ?: ""
            val jsonRoot = JsonParser.parseString(rawBody).asJsonObject
            val jsonContent = jsonRoot.getAsJsonArray("choices")
                .get(0).asJsonObject
                .getAsJsonObject("message")
                .get("content").asString

            val responseObj = gson.fromJson(jsonContent, PlanIAResponse::class.java)
            if (responseObj?.dias == null || responseObj.dias.isEmpty()) {
                return@withContext Result.failure(Exception("La IA no devolvió un plan de dieta estructurado."))
            }

            Result.success(responseObj.dias)
        } catch (e: Exception) {
            Log.e("PlanEmbarazoIA", "Exception generating plan", e)
            Result.failure(e)
        }
    }
}

data class PlanIAResponse(
    val dias: List<PlanDietaEmbarazoSemanal> = emptyList()
)
