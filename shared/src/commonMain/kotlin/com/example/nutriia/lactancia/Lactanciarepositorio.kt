package com.example.nutriia.lactancia

import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.utils.FechaUtils
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class LactanciaRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun feedingCol(childId: String, ownerUid: String? = null) =
        db.collection("usuarios")
            .document(ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
            .collection("hijos")
            .document(childId)
            .collection("lactancia_tomas")

    private fun manualPumpingCol(childId: String, ownerUid: String? = null) =
        db.collection("usuarios")
            .document(ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
            .collection("hijos")
            .document(childId)
            .collection("extraccion_manual")

    suspend fun saveFeedingLog(childId: String, log: FeedingLog, ownerUid: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val id = if (log.id.isEmpty()) generateUUID() else log.id
            val ref = feedingCol(childId, ownerUid).document(id)

            val data = mapOf<String, Any?>(
                "id" to id,
                "childId" to childId,
                "userId" to currentUid,
                "date" to log.date,
                "startTime" to log.startTime,
                "durationMinutes" to log.durationMinutes,
                "side" to log.side,
                "formulaMl" to log.formulaMl,
                "notes" to log.notes,
                "creadoEnMillis" to currentTimeMillis(),
                "fechaCreacion" to FechaUtils.fechaActual(),
                "horaCreacion" to FechaUtils.horaActual()
            )
            ref.set(data)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFeedingLog(childId: String, logId: String, ownerUid: String? = null): Result<Unit> {
        return try {
            feedingCol(childId, ownerUid).document(logId).delete()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeFeedingsByDate(childId: String, date: String, ownerUid: String? = null): Flow<List<FeedingLog>> {
        return try {
            val dateSlash = com.example.nutriia.util.DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(date)
            val dateIso = com.example.nutriia.util.DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(date)

            feedingCol(childId, ownerUid)
                .snapshots
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
                            val docDate = runCatching { doc.get<String?>("date") }.getOrNull() ?: ""
                            if (docDate == date || docDate == dateSlash || docDate == dateIso) {
                                val cId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: ""
                                val uId = runCatching { doc.get<String?>("userId") }.getOrNull() ?: ""
                                val sTime = runCatching { doc.get<String?>("startTime") }.getOrNull() ?: ""
                                val dur = runCatching { doc.get<Long?>("durationMinutes")?.toInt() }.getOrNull() ?: 0
                                val side = runCatching { doc.get<String?>("side") }.getOrNull() ?: BreastSide.LEFT.name
                                val fMl = runCatching { doc.get<Long?>("formulaMl")?.toInt() }.getOrNull() ?: 0
                                val notes = runCatching { doc.get<String?>("notes") }.getOrNull() ?: ""

                                FeedingLog(
                                    id = id,
                                    childId = cId,
                                    userId = uId,
                                    date = docDate,
                                    startTime = sTime,
                                    durationMinutes = dur,
                                    side = side,
                                    formulaMl = fMl,
                                    notes = notes
                                )
                            } else null
                        }.getOrNull()
                    }.sortedByDescending { it.startTime }
                }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    fun observeAllFeedings(childId: String, ownerUid: String? = null): Flow<List<FeedingLog>> {
        return try {
            feedingCol(childId, ownerUid)
                .snapshots
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
                            val cId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: ""
                            val uId = runCatching { doc.get<String?>("userId") }.getOrNull() ?: ""
                            val docDate = runCatching { doc.get<String?>("date") }.getOrNull() ?: ""
                            val sTime = runCatching { doc.get<String?>("startTime") }.getOrNull() ?: ""
                            val dur = runCatching { doc.get<Long?>("durationMinutes")?.toInt() }.getOrNull() ?: 0
                            val side = runCatching { doc.get<String?>("side") }.getOrNull() ?: BreastSide.LEFT.name
                            val fMl = runCatching { doc.get<Long?>("formulaMl")?.toInt() }.getOrNull() ?: 0
                            val notes = runCatching { doc.get<String?>("notes") }.getOrNull() ?: ""

                            FeedingLog(
                                id = id,
                                childId = cId,
                                userId = uId,
                                date = docDate,
                                startTime = sTime,
                                durationMinutes = dur,
                                side = side,
                                formulaMl = fMl,
                                notes = notes
                            )
                        }.getOrNull()
                    }.sortedByDescending { it.date + it.startTime }
                }
        } catch (e: Exception) {
            flowOf(emptyList())
        }
    }

    suspend fun getLastFeedings(childId: String, limit: Long = 10, ownerUid: String? = null): Result<List<FeedingLog>> {
        return try {
            val snapshot = feedingCol(childId, ownerUid).get()
            val logs = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val id = runCatching { doc.get<String?>("id") }.getOrNull() ?: doc.id
                    val cId = runCatching { doc.get<String?>("childId") }.getOrNull() ?: ""
                    val uId = runCatching { doc.get<String?>("userId") }.getOrNull() ?: ""
                    val docDate = runCatching { doc.get<String?>("date") }.getOrNull() ?: ""
                    val sTime = runCatching { doc.get<String?>("startTime") }.getOrNull() ?: ""
                    val dur = runCatching { doc.get<Long?>("durationMinutes")?.toInt() }.getOrNull() ?: 0
                    val side = runCatching { doc.get<String?>("side") }.getOrNull() ?: BreastSide.LEFT.name
                    val fMl = runCatching { doc.get<Long?>("formulaMl")?.toInt() }.getOrNull() ?: 0
                    val notes = runCatching { doc.get<String?>("notes") }.getOrNull() ?: ""

                    FeedingLog(
                        id = id,
                        childId = cId,
                        userId = uId,
                        date = docDate,
                        startTime = sTime,
                        durationMinutes = dur,
                        side = side,
                        formulaMl = fMl,
                        notes = notes
                    )
                }.getOrNull()
            }.sortedByDescending { it.date + it.startTime }.take(limit.toInt())
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailySummary(childId: String, date: String, ownerUid: String? = null): Result<DailyFeedingSummary> {
        return try {
            val snapshot = feedingCol(childId, ownerUid).get()
            val logs = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val docDate = runCatching { doc.get<String?>("date") }.getOrNull() ?: ""
                    if (docDate == date) {
                        val sTime = runCatching { doc.get<String?>("startTime") }.getOrNull() ?: ""
                        val dur = runCatching { doc.get<Long?>("durationMinutes")?.toInt() }.getOrNull() ?: 0
                        val side = runCatching { doc.get<String?>("side") }.getOrNull() ?: ""
                        val fMl = runCatching { doc.get<Long?>("formulaMl")?.toInt() }.getOrNull() ?: 0

                        FeedingLog(
                            startTime = sTime,
                            durationMinutes = dur,
                            side = side,
                            formulaMl = fMl
                        )
                    } else null
                }.getOrNull()
            }

            val totalSessions = logs.size
            val totalMinutes = logs.sumOf { it.durationMinutes }
            val totalFormulaMl = logs.sumOf { it.formulaMl }
            val leftSessions = logs.count { it.side.equals("LEFT", ignoreCase = true) || it.side.contains("izq", ignoreCase = true) }
            val rightSessions = logs.count { it.side.equals("RIGHT", ignoreCase = true) || it.side.contains("der", ignoreCase = true) }
            val formulaSessions = logs.count { it.formulaMl > 0 }

            Result.success(
                DailyFeedingSummary(
                    date = date,
                    totalSessions = totalSessions,
                    totalMinutes = totalMinutes,
                    totalFormulaMl = totalFormulaMl,
                    leftSessions = leftSessions,
                    rightSessions = rightSessions,
                    formulaSessions = formulaSessions
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
