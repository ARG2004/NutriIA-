package com.example.nutriia.lactancia

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.nutriia.util.DateMigrationHelper
import com.google.firebase.firestore.Source
import com.example.nutriia.offline.OfflineManager

class LactanciaRepository {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun feedingCol(childId: String, ownerUid: String? = null) =
        db.collection("usuarios")
            .document(ownerUid ?: auth.currentUser?.uid
            ?: throw IllegalStateException("Usuario no autenticado"))
            .collection("hijos")
            .document(childId)
            .collection("lactancia")

    suspend fun saveFeedingLog(childId: String, log: FeedingLog, ownerUid: String? = null): Result<String> {
        return try {
            val currentUid = auth.currentUser?.uid
                ?: return Result.failure(IllegalStateException("Usuario no autenticado"))

            val docRef = if (log.id.isEmpty()) feedingCol(childId, ownerUid).document()
            else feedingCol(childId, ownerUid).document(log.id)

            val data = hashMapOf(
                "id"              to docRef.id,
                "childId"         to childId,
                "userId"          to currentUid,
                "date"            to DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(log.date),
                "startTime"       to log.startTime,
                "durationMinutes" to log.durationMinutes,
                "side"            to log.side,
                "formulaMl"       to log.formulaMl,
                "notes"           to log.notes,
                "createdAt"       to Timestamp.now(),
                "fechaCreacion"   to com.example.nutriia.utils.FechaUtils.fechaActual(),
                "horaCreacion"    to com.example.nutriia.utils.FechaUtils.horaActual()
            )
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFeedingLog(childId: String, logId: String, ownerUid: String? = null): Result<Unit> {
        return try {
            feedingCol(childId, ownerUid).document(logId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeFeedingsByDate(childId: String, date: String, ownerUid: String? = null): Flow<List<FeedingLog>> =
        callbackFlow {
            if (auth.currentUser == null) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            val listener = try {
                feedingCol(childId, ownerUid)
                    .whereEqualTo("date", DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(date))
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) { trySend(emptyList()); return@addSnapshotListener }
                        val logs = snapshot?.documents?.mapNotNull { doc ->
                            runCatching {
                                FeedingLog(
                                    id              = doc.getString("id") ?: doc.id,
                                    childId         = doc.getString("childId") ?: "",
                                    userId          = doc.getString("userId") ?: "",
                                    date            = DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(doc.getString("date") ?: ""),
                                    startTime       = doc.getString("startTime") ?: "",
                                    durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                                    side            = doc.getString("side") ?: BreastSide.LEFT.name,
                                    formulaMl       = doc.getLong("formulaMl")?.toInt() ?: 0,
                                    notes           = doc.getString("notes") ?: "",
                                    createdAt       = doc.getTimestamp("createdAt")
                                )
                            }.getOrNull()
                        } ?: emptyList()
                        val sortedLogs = logs.sortedByDescending { it.startTime }
                        trySend(sortedLogs)
                    }
            } catch (e: Exception) {
                trySend(emptyList())
                close()
                return@callbackFlow
            }

            awaitClose { listener.remove() }
        }.catch { emit(emptyList()) }

    suspend fun getLastFeedings(childId: String, limit: Long = 10, ownerUid: String? = null): Result<List<FeedingLog>> {
        return try {
            if (auth.currentUser == null)
                return Result.failure(IllegalStateException("Usuario no autenticado"))

            val source = if (OfflineManager.hayConexion()) Source.DEFAULT else Source.CACHE
            val snapshot = feedingCol(childId, ownerUid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get(source).await()

            val logs = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    FeedingLog(
                        id              = doc.getString("id") ?: doc.id,
                        childId         = doc.getString("childId") ?: "",
                        date            = DateMigrationHelper.convertDdMmYyyyToYyyyMmDd(doc.getString("date") ?: ""),
                        startTime       = doc.getString("startTime") ?: "",
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                        side            = doc.getString("side") ?: BreastSide.LEFT.name,
                        formulaMl       = doc.getLong("formulaMl")?.toInt() ?: 0,
                        notes           = doc.getString("notes") ?: "",
                        createdAt       = doc.getTimestamp("createdAt")
                    )
                }.getOrNull()
            }
            Result.success(logs)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDailySummary(childId: String, date: String, ownerUid: String? = null): Result<DailyFeedingSummary> {
        return try {
            if (auth.currentUser == null)
                return Result.failure(IllegalStateException("Usuario no autenticado"))

            val source = if (OfflineManager.hayConexion()) Source.DEFAULT else Source.CACHE
            val snapshot = feedingCol(childId, ownerUid)
                .whereEqualTo("date", DateMigrationHelper.convertYyyyMmDdToDdMmYyyy(date))
                .get(source).await()

            val logs = snapshot.documents.mapNotNull { doc ->
                runCatching {
                    FeedingLog(
                        startTime       = doc.getString("startTime") ?: "",
                        durationMinutes = doc.getLong("durationMinutes")?.toInt() ?: 0,
                        side            = doc.getString("side") ?: "",
                        formulaMl       = doc.getLong("formulaMl")?.toInt() ?: 0
                    )
                }.getOrNull()
            }

            val summary = DailyFeedingSummary(
                date               = date,
                totalSessions      = logs.size,
                totalMinutes       = logs.sumOf { it.durationMinutes },
                totalFormulaMl     = logs.filter { it.side == BreastSide.FORMULA.name }.sumOf { it.formulaMl },
                leftSessions       = logs.count { it.side == BreastSide.LEFT.name },
                rightSessions      = logs.count { it.side == BreastSide.RIGHT.name },
                formulaSessions    = logs.count { it.side == BreastSide.FORMULA.name },
                avgIntervalMinutes = calcAvgInterval(logs)
            )
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getWeeklySummaries(childId: String, ownerUid: String? = null): Result<List<DailyFeedingSummary>> {
        return try {
            if (auth.currentUser == null)
                return Result.failure(IllegalStateException("Usuario no autenticado"))

            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val cal = java.util.Calendar.getInstance()
            val summaries = mutableListOf<DailyFeedingSummary>()

            repeat(7) {
                val date = fmt.format(cal.time)
                getDailySummary(childId, date, ownerUid).getOrNull()?.let { summaries.add(it) }
                cal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            }
            Result.success(summaries.reversed())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calcAvgInterval(logs: List<FeedingLog>): Int {
        if (logs.size < 2) return 0
        val fmt    = SimpleDateFormat("HH:mm", Locale.getDefault())
        val sorted = logs.mapNotNull {
            runCatching { fmt.parse(it.startTime)?.time }.getOrNull()
        }.sorted()
        if (sorted.size < 2) return 0
        val diffs = sorted.zipWithNext { a, b -> ((b - a) / 60000).toInt() }
        return diffs.average().toInt()
    }
}