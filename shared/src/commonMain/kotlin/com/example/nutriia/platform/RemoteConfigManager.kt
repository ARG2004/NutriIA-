package com.example.nutriia.platform

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore

object RemoteConfigManager {

    private var cachedPrimaryModel: String = "openai/gpt-oss-120b"
    private var cachedVisionModel: String = "qwen/qwen3.6-27b"

    suspend fun fetchConfigs() {
        try {
            val doc = Firebase.firestore.collection("config_app").document("modelos_ia").get()
            if (doc.exists) {
                val data = doc.data<Map<String, Any?>>()
                val primary = data["groq_primary_model"] as? String
                val vision = data["groq_vision_model"] as? String
                if (!primary.isNullOrBlank()) cachedPrimaryModel = primary
                if (!vision.isNullOrBlank()) cachedVisionModel = vision
            }
        } catch (_: Throwable) {
            // Falla silenciosa si no hay red o no existe el documento, usa valores locales por defecto
        }
    }

    fun getPrimaryModel(): String = cachedPrimaryModel

    fun getVisionModel(): String = cachedVisionModel
}
