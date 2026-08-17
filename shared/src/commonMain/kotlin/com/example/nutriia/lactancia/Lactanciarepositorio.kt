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
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

class LactanciaRepository {

    private val db get() = Firebase.firestore
    private val auth get() = Firebase.auth

    private fun feedingCol(childId: String, ownerUid: String? = null) =
        db.collection("usuarios")
            .document(ownerUid ?: auth.currentUser?.uid ?: throw IllegalStateException("Usuario no autenticado"))
            .collection("hijos")
            .document(childId)
            .collection("lactancia")

    suspend fun saveFeedingLog(childId: String, log: FeedingLog, ownerUid: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val id = if (log.id.isBlank()) generateUUID() else log.id
            val docRef = feedingCol(childId, ownerUid).document(id)

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
                "createdAtMillis" to currentTimeMillis(),
                "fechaCreacion" to FechaUtils.fechaActual(),
                "horaCreacion" to FechaUtils.horaActual()
            )
            docRef.set(data)
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
            feedingCol(childId, ownerUid)
                .where { "date".equalTo(date) }
                .snapshots
                .map { snapshot ->
                    snapshot.documents.mapNotNull { doc ->
                        runCatching {
                            val data = doc.data<Map<String, Any?>>()
                            FeedingLog(
                                id = data["id"] as? String ?: doc.id,
                                childId = data["childId"] as? String ?: "",
                                userId = data["userId"] as? String ?: "",
                                date = data["date"] as? String ?: "",
                                startTime = data["startTime"] as? String ?: "",
                                durationMinutes = (data["durationMinutes"] as? Long)?.toInt() ?: 0,
                                side = data["side"] as? String ?: BreastSide.LEFT.name,
                                formulaMl = (data["formulaMl"] as? Long)?.toInt() ?: 0,
                                notes = data["notes"] as? String ?: ""
                            )
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
                            val data = doc.data<Map<String, Any?>>()
                            FeedingLog(
                                id = data["id"] as? String ?: doc.id,
                                childId = data["childId"] as? String ?: "",
                                userId = data["userId"] as? String ?: "",
                                date = data["date"] as? String ?: "",
                                startTime = data["startTime"] as? String ?: "",
                                durationMinutes = (data["durationMinutes"] as? Long)?.toInt() ?: 0,
                                side = data["side"] as? String ?: BreastSide.LEFT.name,
                                formulaMl = (data["formulaMl"] as? Long)?.toInt() ?: 0,
                                notes = data["notes"] as? String ?: ""
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
                    val data = doc.data<Map<String, Any?>>()
                    FeedingLog(
                        id = data["id"] as? String ?: doc.id,
                        childId = data["childId"] as? String ?: "",
                        userId = data["userId"] as? String ?: "",
                        date = data["date"] as? String ?: "",
                        startTime = data["startTime"] as? String ?: "",
                        durationMinutes = (data["durationMinutes"] as? Long)?.toInt() ?: 0,
                        side = data["side"] as? String ?: BreastSide.LEFT.name,
                        formulaMl = (data["formulaMl"] as? Long)?.toInt() ?: 0,
                        notes = data["notes"] as? String ?: ""
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
            val snapshot = feedingCol(childId, ownerUid)
                .where { "date".equalTo(date) }
                .get()

            val logs = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    val data = doc.data<Map<String, Any?>>()
                    FeedingLog(
                        startTime = data["startTime"] as? String ?: "",
                        durationMinutes = (data["durationMinutes"] as? Long)?.toInt() ?: 0,
                        side = data["side"] as? String ?: "",
                        formulaMl = (data["formulaMl"] as? Long)?.toInt() ?: 0
                    )
                }.getOrNull()
            }

            val summary = DailyFeedingSummary(
                date = date,
                totalSessions = logs.size,
                totalMinutes = logs.sumOf { it.durationMinutes },
                totalFormulaMl = logs.filter { it.side == BreastSide.FORMULA.name }.sumOf { it.formulaMl },
                leftSessions = logs.count { it.side == BreastSide.LEFT.name },
                rightSessions = logs.count { it.side == BreastSide.RIGHT.name },
                formulaSessions = logs.count { it.side == BreastSide.FORMULA.name },
                avgIntervalMinutes = calcAvgInterval(logs)
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeeklySummaries(childId: String, ownerUid: String? = null): Result<List<DailyFeedingSummary>> {
        return try {
            val summaries = mutableListOf<DailyFeedingSummary>()
            val now = Clock.System.now()
            val tz = TimeZone.currentSystemDefault()

            for (i in 0 until 7) {
                val instant = now.minus(i, DateTimeUnit.DAY, tz)
                val localDate = instant.toLocalDateTime(tz).date
                val dateStr = "${localDate.year}-${localDate.monthNumber.toString().padStart(2, '0')}-${localDate.dayOfMonth.toString().padStart(2, '0')}"
                getDailySummary(childId, dateStr, ownerUid).getOrNull()?.let { summaries.add(it) }
            }
            Result.success(summaries.reversed())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calcAvgInterval(logs: List<FeedingLog>): Int {
        if (logs.size < 2) return 0
        val times = logs.mapNotNull {
            val parts = it.startTime.split(":")
            if (parts.size == 2) {
                val h = parts[0].toIntOrNull() ?: 0
                val m = parts[1].toIntOrNull() ?: 0
                h * 60 + m
            } else null
        }.sorted()
        if (times.size < 2) return 0
        val diffs = times.zipWithNext { a, b -> b - a }
        return if (diffs.isNotEmpty()) diffs.average().toInt() else 0
    }
}
