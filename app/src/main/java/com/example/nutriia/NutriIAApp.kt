package com.example.nutriia

import android.app.Application
import com.example.nutriia.offline.OfflineManager
import com.google.firebase.firestore.FirebaseFirestore

class NutriIAApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 1. Iniciar el monitor de red
        OfflineManager.init(this)

        // 2. Habilitar índices automáticos para mejorar rendimiento  offline
        FirebaseFirestore.getInstance()
            .persistentCacheIndexManager
            ?.enableIndexAutoCreation()
    }
}