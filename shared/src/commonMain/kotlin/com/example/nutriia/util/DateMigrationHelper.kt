package com.example.nutriia.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.example.nutriia.utils.FechaUtils
import java.util.Locale

object DateMigrationHelper {
    private const val TAG = "DateMigrationHelper"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Regex for yyyy-MM-dd
    private val regexYyyyMmDd = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    // Regex for dd/MM/yyyy
    private val regexDdMmYyyy = Regex("^\\d{2}/\\d{2}/\\d{4}$")

    fun convertYyyyMmDdToDdMmYyyy(dateStr: String): String {
        if (!dateStr.matches(regexYyyyMmDd)) return dateStr
        val parts = dateStr.split("-")
        if (parts.size != 3) return dateStr
        return "${parts[2]}/${parts[1]}/${parts[0]}"
    }

    fun convertDdMmYyyyToYyyyMmDd(dateStr: String): String {
        if (!dateStr.matches(regexDdMmYyyy)) return dateStr
        val parts = dateStr.split("/")
        if (parts.size != 3) return dateStr
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }

    private suspend fun migrarCreacion(
        ref: com.google.firebase.firestore.DocumentReference,
        creadoEnRaw: Any?,
        sobrescribirCreadoEn: Boolean = false
    ) {
        try {
            val date = when (creadoEnRaw) {
                is com.google.firebase.Timestamp -> creadoEnRaw.toDate()
                is Number -> java.util.Date(creadoEnRaw.toLong())
                is String -> FechaUtils.parsearFechaHora(creadoEnRaw) ?: java.util.Date()
                else -> java.util.Date()
            }
            val fecha = FechaUtils.formatearFecha(date)
            val hora = FechaUtils.formatearHora(date)
            
            val updates = mutableMapOf<String, Any>(
                "fechaCreacion" to fecha,
                "horaCreacion"  to hora
            )
            if (sobrescribirCreadoEn) {
                val fechaHora = FechaUtils.formatearFechaHora(date)
                updates["creadoEn"] = fechaHora
            }
            ref.update(updates).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error migrando creacion para ${ref.path}", e)
        }
    }

    suspend fun migrarTodosLosDatos() {
        val uid = auth.currentUser?.uid ?: return
        Log.d(TAG, "Iniciando migración de formatos de fecha para usuario: $uid")
        try {
            // 0. Migrar usuario mismo
            val userRef = db.collection("usuarios").document(uid)
            val userSnap = userRef.get().await()
            if (userSnap.exists()) {
                val creadoEnUser = userSnap.get("creadoEn")
                migrarCreacion(userRef, creadoEnUser, sobrescribirCreadoEn = true)
            }

            // 1. Migrar nutrientes (usuario nivel raíz)
            val nutriSnap = db.collection("usuarios")
                .document(uid)
                .collection("nutrientes")
                .get()
                .await()
            for (doc in nutriSnap.documents) {
                val fecha = doc.getString("fecha") ?: ""
                if (fecha.matches(regexYyyyMmDd)) {
                    val nuevaFecha = convertYyyyMmDdToDdMmYyyy(fecha)
                    doc.reference.update("fecha", nuevaFecha).await()
                    Log.d(TAG, "Migrado nutriente ${doc.id}: $fecha -> $nuevaFecha")
                }
                val creadoEn = doc.get("creadoEn")
                migrarCreacion(doc.reference, creadoEn)
            }

            // 2. Obtener los hijos del usuario para migrar subcolecciones
            val hijosSnap = db.collection("usuarios")
                .document(uid)
                .collection("hijos")
                .get()
                .await()
            for (childDoc in hijosSnap.documents) {
                val childId = childDoc.id
                val creadoEnChild = childDoc.get("creadoEn")
                migrarCreacion(childDoc.reference, creadoEnChild, sobrescribirCreadoEn = true)
                
                // 2.1 Lactancia
                val lactSnap = db.collection("usuarios")
                    .document(uid)
                    .collection("hijos")
                    .document(childId)
                    .collection("lactancia")
                    .get()
                    .await()
                for (doc in lactSnap.documents) {
                    val date = doc.getString("date") ?: ""
                    if (date.matches(regexYyyyMmDd)) {
                        val nuevaFecha = convertYyyyMmDdToDdMmYyyy(date)
                        doc.reference.update("date", nuevaFecha).await()
                        Log.d(TAG, "Migrada lactancia ${doc.id}: $date -> $nuevaFecha")
                    }
                    val creadoEn = doc.get("createdAt") ?: doc.get("creadoEn")
                    migrarCreacion(doc.reference, creadoEn)
                }

                // 2.2 Crecimiento
                val creSnap = db.collection("usuarios")
                    .document(uid)
                    .collection("hijos")
                    .document(childId)
                    .collection("crecimiento")
                    .get()
                    .await()
                for (doc in creSnap.documents) {
                    val fecha = doc.getString("fecha") ?: ""
                    if (fecha.matches(regexYyyyMmDd)) {
                        val nuevaFecha = convertYyyyMmDdToDdMmYyyy(fecha)
                        doc.reference.update("fecha", nuevaFecha).await()
                        Log.d(TAG, "Migrado crecimiento ${doc.id}: $fecha -> $nuevaFecha")
                    }
                    val creadoEn = doc.get("creadoEn")
                    migrarCreacion(doc.reference, creadoEn)
                }

                // 2.3 Sólidos
                val solSnap = db.collection("usuarios")
                    .document(uid)
                    .collection("hijos")
                    .document(childId)
                    .collection("solidos")
                    .get()
                    .await()
                for (doc in solSnap.documents) {
                    val fechaIntroduccion = doc.getString("fechaIntroduccion") ?: ""
                    if (fechaIntroduccion.matches(regexYyyyMmDd)) {
                        val nuevaFecha = convertYyyyMmDdToDdMmYyyy(fechaIntroduccion)
                        doc.reference.update("fechaIntroduccion", nuevaFecha).await()
                        Log.d(TAG, "Migrado sólidos ${doc.id}: $fechaIntroduccion -> $nuevaFecha")
                    }
                    val creadoEn = doc.get("creadoEn")
                    migrarCreacion(doc.reference, creadoEn)
                }

                // 2.4 Alertas
                val alertSnap = db.collection("usuarios")
                    .document(uid)
                    .collection("hijos")
                    .document(childId)
                    .collection("alertas")
                    .get()
                    .await()
                for (doc in alertSnap.documents) {
                    val creadoEn = doc.get("creadoEn")
                    migrarCreacion(doc.reference, creadoEn)
                }

                // 2.5 Nutrientes (bajo hijo)
                val nutriHijoSnap = db.collection("usuarios")
                    .document(uid)
                    .collection("hijos")
                    .document(childId)
                    .collection("nutrientes")
                    .get()
                    .await()
                for (doc in nutriHijoSnap.documents) {
                    val fecha = doc.getString("fecha") ?: ""
                    if (fecha.matches(regexYyyyMmDd)) {
                        val nuevaFecha = convertYyyyMmDdToDdMmYyyy(fecha)
                        doc.reference.update("fecha", nuevaFecha).await()
                    }
                    val creadoEn = doc.get("creadoEn")
                    migrarCreacion(doc.reference, creadoEn)
                }
            }
            Log.d(TAG, "Migración de formatos de fecha y creación completada con éxito.")
        } catch (e: Exception) {
            Log.e(TAG, "Error durante la migración de fechas", e)
        }
    }

    suspend fun migrarAbsolutamenteTodo() {
        Log.d(TAG, "Iniciando migración GLOBAL de todas las cuentas existentes...")
        try {
            val usuariosSnap = db.collection("usuarios").get().await()
            Log.d(TAG, "Se encontraron ${usuariosSnap.size()} usuarios para migrar globalmente.")
            for (userDoc in usuariosSnap.documents) {
                val uid = userDoc.id
                Log.d(TAG, "Migrando globalmente usuario: $uid")
                
                // 0. Migrar usuario mismo
                val creadoEnUser = userDoc.get("creadoEn")
                migrarCreacion(userDoc.reference, creadoEnUser, sobrescribirCreadoEn = true)

                // 1. Migrar nutrientes (raíz)
                try {
                    val nutriSnap = db.collection("usuarios")
                        .document(uid)
                        .collection("nutrientes")
                        .get()
                        .await()
                    for (doc in nutriSnap.documents) {
                        val creadoEn = doc.get("creadoEn")
                        migrarCreacion(doc.reference, creadoEn)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error migrando nutrientes para usuario $uid", e)
                }

                // 2. Hijos
                try {
                    val hijosSnap = db.collection("usuarios")
                        .document(uid)
                        .collection("hijos")
                        .get()
                        .await()
                    for (childDoc in hijosSnap.documents) {
                        val childId = childDoc.id
                        val creadoEnChild = childDoc.get("creadoEn")
                        migrarCreacion(childDoc.reference, creadoEnChild, sobrescribirCreadoEn = true)
                        
                        // 2.1 Lactancia
                        try {
                            val lactSnap = db.collection("usuarios")
                                .document(uid)
                                .collection("hijos")
                                .document(childId)
                                .collection("lactancia")
                                .get()
                                .await()
                            for (doc in lactSnap.documents) {
                                val creadoEn = doc.get("createdAt") ?: doc.get("creadoEn")
                                migrarCreacion(doc.reference, creadoEn)
                            }
                        } catch (_: Exception) {}

                        // 2.2 Crecimiento
                        try {
                            val creSnap = db.collection("usuarios")
                                .document(uid)
                                .collection("hijos")
                                .document(childId)
                                .collection("crecimiento")
                                .get()
                                .await()
                            for (doc in creSnap.documents) {
                                val creadoEn = doc.get("creadoEn")
                                migrarCreacion(doc.reference, creadoEn)
                            }
                        } catch (_: Exception) {}

                        // 2.3 Sólidos
                        try {
                            val solSnap = db.collection("usuarios")
                                .document(uid)
                                .collection("hijos")
                                .document(childId)
                                .collection("solidos")
                                .get()
                                .await()
                            for (doc in solSnap.documents) {
                                val creadoEn = doc.get("creadoEn")
                                migrarCreacion(doc.reference, creadoEn)
                            }
                        } catch (_: Exception) {}

                        // 2.4 Alertas
                        try {
                            val alertSnap = db.collection("usuarios")
                                .document(uid)
                                .collection("hijos")
                                .document(childId)
                                .collection("alertas")
                                .get()
                                .await()
                            for (doc in alertSnap.documents) {
                                val creadoEn = doc.get("creadoEn")
                                migrarCreacion(doc.reference, creadoEn)
                            }
                        } catch (_: Exception) {}

                        // 2.5 Nutrientes (hijo)
                        try {
                            val nutriHijoSnap = db.collection("usuarios")
                                .document(uid)
                                .collection("hijos")
                                .document(childId)
                                .collection("nutrientes")
                                .get()
                                .await()
                            for (doc in nutriHijoSnap.documents) {
                                val creadoEn = doc.get("creadoEn")
                                migrarCreacion(doc.reference, creadoEn)
                            }
                        } catch (_: Exception) {}
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error migrando hijos para usuario $uid", e)
                }
            }
            Log.d(TAG, "Migración GLOBAL completada exitosamente.")
        } catch (e: Exception) {
            Log.e(TAG, "Error en migración GLOBAL de todas las cuentas", e)
        }
    }
}
