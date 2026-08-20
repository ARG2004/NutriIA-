package com.example.nutriia.alerta

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.catch
import com.example.nutriia.platform.Log

class AlertaRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun coleccion(childId: String?) =
        if (!childId.isNullOrEmpty()) {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("hijos")
                .document(childId)
                .collection("alertas")
        } else {
            db.collection("usuarios")
                .document(auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("alertas")
        }

    suspend fun guardar(alerta: Alerta): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(alerta.childId).document(alerta.id).set(alerta)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun eliminar(childId: String?, alertaId: String): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(alertaId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleActiva(childId: String?, alertaId: String, activa: Boolean): Result<Unit> {
        return try {
            auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Usuario no autenticado"))
            coleccion(childId).document(alertaId).update("activa" to activa)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observarPorHijo(childId: String?): Flow<List<Alerta>> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            Log.e("AlertaRepo", "No se puede observar: Usuario no autenticado")
            return flowOf(emptyList())
        }
        
        Log.i("AlertaRepo", "Iniciando observación para hijo: $childId (uid: $uid)")
        
        return try {
            coleccion(childId).snapshots
                .map { snapshot ->
                    Log.i("AlertaRepo", "Snapshot recibido con ${snapshot.documents.size} documentos")
                    snapshot.documents.mapNotNull { doc ->
                        alertaDesdeDoc(doc)
                    }.sortedBy { it.hora }
                }
                .onStart { Log.i("AlertaRepo", "Flow de alertas iniciado") }
                .catch { e -> 
                    Log.e("AlertaRepo", "Error en snapshots de alertas: ${e.message}")
                    emit(emptyList())
                }
        } catch (e: Exception) {
            Log.e("AlertaRepo", "Error fatal iniciando observación: ${e.message}")
            flowOf(emptyList())
        }
    }

    private fun alertaDesdeDoc(doc: dev.gitlive.firebase.firestore.DocumentSnapshot): Alerta? {
        return try {
            val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
            val titulo = runCatching { doc.get<String?>("titulo") }.getOrNull() ?: ""
            if (titulo.isBlank()) return null

            val cId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: ""
            val cName = runCatching { doc.get<String?>("childName") }.getOrNull() ?: ""
            val desc = runCatching { doc.get<String?>("descripcion") }.getOrNull() ?: ""
            val hr = runCatching { doc.get<String?>("hora") }.getOrNull() ?: "08:00"
            val act = runCatching { doc.get<Boolean?>("activa") }.getOrNull() ?: true
            val tStr = runCatching { doc.get<String?>("tipo") }.getOrNull() ?: TipoAlerta.TOMA_COMIDA.name
            val t = runCatching { TipoAlerta.valueOf(tStr) }.getOrDefault(TipoAlerta.TOMA_COMIDA)
            
            val cEn = runCatching { doc.get<Long?>("creadaEn") }.getOrNull()
                ?: runCatching { doc.get<Double?>("creadaEn")?.toLong() }.getOrNull()
                ?: 0L

            @Suppress("UNCHECKED_CAST")
            val dRaw = runCatching { doc.get<List<String>?>("diasSemana") }.getOrNull() ?: emptyList()
            val dSemana = dRaw.mapNotNull { n -> DiasSemana.entries.find { it.name == n } }
            val fUnica = runCatching { doc.get<String?>("fechaUnica") }.getOrNull()

            Alerta(
                id = id,
                childId = cId,
                childName = cName,
                tipo = t,
                titulo = titulo,
                descripcion = desc,
                hora = hr,
                diasSemana = dSemana,
                fechaUnica = fUnica,
                activa = act,
                creadaEn = cEn
            )
        } catch (e: Exception) {
            Log.e("AlertaRepo", "Error parseando doc ${doc.id}: ${e.message}")
            null
        }
    }

    suspend fun obtenerTodasActivas(): List<Alerta> {
        val currentUid = auth.currentUser?.uid ?: return emptyList()
        return try {
            val alertsList = mutableListOf<Alerta>()

            val rootSnap = db.collection("usuarios")
                .document(currentUid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("alertas")
                .get()

            for (doc in rootSnap.documents) {
                if (doc.get<Boolean?>("activa") == true) {
                    alertaDesdeDoc(doc)?.let { alertsList.add(it) }
                }
            }

            val hijosSnap = db.collection("usuarios")
                .document(currentUid)
                .collection("hijos")
                .get()

            for (childDoc in hijosSnap.documents) {
                val childAlertsSnap = db.collection("usuarios")
                    .document(currentUid)
                    .collection("hijos")
                    .document(childDoc.id)
                    .collection("alertas")
                    .get()

                for (doc in childAlertsSnap.documents) {
                    if (doc.get<Boolean?>("activa") == true) {
                        alertaDesdeDoc(doc)?.let { alertsList.add(it) }
                    }
                }
            }

            alertsList
        } catch (e: Exception) {
            emptyList()
        }
    }
}
