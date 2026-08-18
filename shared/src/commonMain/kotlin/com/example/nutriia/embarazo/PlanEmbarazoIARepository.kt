package com.example.nutriia.embarazo

import com.example.nutriia.platform.Log
import com.example.nutriia.platform.PlatformConfig
import com.example.nutriia.platform.PlatformHttp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

private const val TAG = "PlanEmbarazoIA"

class PlanEmbarazoIARepository {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun generarPlanIA(
        perfil: PerfilEmbarazo,
        recetasBase: List<RecetaEmbarazo>
    ): Result<List<PlanDietaEmbarazoSemanal>> = withContext(Dispatchers.Default) {
        try {
            val apiKey = PlatformConfig.groqApiKey
            if (apiKey.isBlank()) {
                Log.w(TAG, "Groq API key no configurada, usando motor de reglas local.")
                return@withContext generarPlanFallback(perfil)
            }

            val listadoRecetasTexto = recetasBase.take(30).joinToString("\n") { r ->
                "- ${r.nombre} (${r.tipoComida}): Ingredientes: ${r.ingredientes.joinToString(", ")}. Kcal: ${r.kcal}."
            }

            val imcTexto = if (perfil.tallaM > 0) {
                val imc = perfil.pesoPregestacionalKg / (perfil.tallaM * perfil.tallaM)
                ((imc * 100).toLong() / 100.0).toString()
            } else "N/D"

            val systemPrompt = """
                Eres un asistente de Inteligencia Artificial experto en nutrición clínica materno-infantil y ginecología.
                Debes generar un plan de alimentación semanal personalizado y seguro para una mujer embarazada basándote en su perfil de salud.
                
                Perfil de la paciente:
                - Semanas de gestación: ${perfil.semanas} semanas (Trimestre: ${if (perfil.semanas <= 13) 1 else if (perfil.semanas <= 27) 2 else 3})
                - Edad: ${perfil.edad} años
                - Talla: ${perfil.tallaM} metros
                - Peso pre-gestacional: ${perfil.pesoPregestacionalKg} kg (IMC pre-gestacional: $imcTexto)
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

            val messagesArray = buildJsonArray {
                addJsonObject {
                    put("role", "system")
                    put("content", systemPrompt)
                }
                addJsonObject {
                    put("role", "user")
                    put("content", "Genera el plan de dieta semanal en formato JSON.")
                }
            }

            val requestBodyJson = buildJsonObject {
                put("model", "openai/gpt-oss-120b")
                put("messages", messagesArray)
                put("max_tokens", 1800)
                put("temperature", 0.3)
                putJsonObject("response_format") {
                    put("type", "json_object")
                }
            }.toString()

            val responseResult = PlatformHttp.postJson(
                url = "https://api.groq.com/openai/v1/chat/completions",
                headers = mapOf(
                    "Authorization" to "Bearer $apiKey",
                    "Content-Type" to "application/json; charset=utf-8"
                ),
                jsonBody = requestBodyJson,
                timeoutMs = 35000L
            )

            if (responseResult.isFailure) {
                Log.w(TAG, "Fallo al consultar Groq LLM (${responseResult.exceptionOrNull()?.message}), activando fallback.")
                return@withContext generarPlanFallback(perfil)
            }

            val rawBody = responseResult.getOrNull() ?: ""
            val jsonRoot = json.parseToJsonElement(rawBody).jsonObject
            val content = jsonRoot["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
                ?.get("message")?.jsonObject
                ?.get("content")?.jsonPrimitive?.contentOrNull ?: ""

            if (content.isBlank()) {
                return@withContext generarPlanFallback(perfil)
            }

            val contentJson = json.parseToJsonElement(content).jsonObject
            val diasArray = contentJson["dias"]?.jsonArray

            if (diasArray == null || diasArray.isEmpty()) {
                return@withContext generarPlanFallback(perfil)
            }

            val listaPlan = mutableListOf<PlanDietaEmbarazoSemanal>()
            val trimestrePorDefecto = when {
                perfil.semanas <= 13 -> TrimestreEmbarazo.PRIMERO
                perfil.semanas <= 27 -> TrimestreEmbarazo.SEGUNDO
                else -> TrimestreEmbarazo.TERCERO
            }

            for (diaElement in diasArray) {
                val diaObj = diaElement.jsonObject
                val diaSemana = diaObj["diaSemana"]?.jsonPrimitive?.contentOrNull ?: "Lunes"
                val comidasObj = diaObj["comidas"]?.jsonObject
                val macrosObj = diaObj["macros"]?.jsonObject
                val trimStr = diaObj["trimestre"]?.jsonPrimitive?.contentOrNull ?: trimestrePorDefecto.name

                val comidas = ComidasDiariasEmbarazo(
                    desayuno = comidasObj?.get("desayuno")?.jsonPrimitive?.contentOrNull ?: "Desayuno saludable",
                    colacion1 = comidasObj?.get("colacion1")?.jsonPrimitive?.contentOrNull ?: "Fruta de temporada",
                    comida = comidasObj?.get("comida")?.jsonPrimitive?.contentOrNull ?: "Comida balanceada",
                    colacion2 = comidasObj?.get("colacion2")?.jsonPrimitive?.contentOrNull ?: "Yogurt con semillas",
                    cena = comidasObj?.get("cena")?.jsonPrimitive?.contentOrNull ?: "Cena ligera"
                )

                val macros = MacroObjetivoEmbarazo(
                    caloriasExtra = macrosObj?.get("caloriasExtra")?.jsonPrimitive?.intOrNull ?: 350,
                    proteinasG = macrosObj?.get("proteinasG")?.jsonPrimitive?.doubleOrNull ?: 71.0,
                    hierroMg = macrosObj?.get("hierroMg")?.jsonPrimitive?.doubleOrNull ?: 30.0,
                    calcioMg = macrosObj?.get("calcioMg")?.jsonPrimitive?.doubleOrNull ?: 1000.0,
                    acidoFolicoUg = macrosObj?.get("acidoFolicoUg")?.jsonPrimitive?.doubleOrNull ?: 600.0,
                    omega3G = macrosObj?.get("omega3G")?.jsonPrimitive?.doubleOrNull ?: 0.3,
                    aguaLitros = macrosObj?.get("aguaLitros")?.jsonPrimitive?.doubleOrNull ?: 2.3
                )

                val trimestre = runCatching { TrimestreEmbarazo.valueOf(trimStr) }.getOrDefault(trimestrePorDefecto)

                listaPlan.add(
                    PlanDietaEmbarazoSemanal(
                        diaSemana = diaSemana,
                        comidas = comidas,
                        macros = macros,
                        trimestre = trimestre
                    )
                )
            }

            if (listaPlan.isEmpty()) {
                return@withContext generarPlanFallback(perfil)
            }

            Result.success(listaPlan)
        } catch (e: Exception) {
            Log.e(TAG, "Exception en generarPlanIA: ${e.message}, usando fallback local.", e)
            generarPlanFallback(perfil)
        }
    }

    private fun generarPlanFallback(perfil: PerfilEmbarazo): Result<List<PlanDietaEmbarazoSemanal>> {
        return try {
            val plan = DietaEmbarazoEngine.generarPlanSemanal(
                semanas = perfil.semanas,
                nivel = perfil.nivelIngreso,
                region = perfil.region,
                alergenos = perfil.alergenosParsados,
                condiciones = perfil.condiciones,
                alimentosRegistrados = emptyList()
            )
            Result.success(plan)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
