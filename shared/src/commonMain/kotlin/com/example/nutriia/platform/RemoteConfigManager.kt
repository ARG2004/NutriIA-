package com.example.nutriia.platform

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.remoteconfig.remoteConfig

object RemoteConfigManager {

    private val config get() = Firebase.remoteConfig

    suspend fun fetchConfigs() {
        try {
            config.setDefaults(
                "groq_primary_model" to "openai/gpt-oss-120b",
                "groq_vision_model" to "qwen/qwen3.6-27b",
                "groq_fallback_models" to "openai/gpt-oss-20b,qwen/qwen3.6-27b,gemma2-9b-it,llama-3.1-8b-instant,llama3-70b-8192,llama3-8b-8192,groq/compound-mini"
            )
            config.fetchAndActivate()
        } catch (_: Throwable) {
            // Falla silenciosa si no hay red, usa valores locales por defecto
        }
    }

    fun getPrimaryModel(): String {
        return try {
            val m = config.getValue("groq_primary_model").asString()
            if (m.isNotBlank()) m else "openai/gpt-oss-120b"
        } catch (_: Throwable) {
            "openai/gpt-oss-120b"
        }
    }

    fun getVisionModel(): String {
        return try {
            val m = config.getValue("groq_vision_model").asString()
            if (m.isNotBlank()) m else "qwen/qwen3.6-27b"
        } catch (_: Throwable) {
            "qwen/qwen3.6-27b"
        }
    }
}
