package com.example.nutriia.lactancia

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class LactanciaRepository {

    private val feedingsState = MutableStateFlow<Map<String, List<FeedingLog>>>(emptyMap())

    suspend fun saveFeedingLog(childId: String, log: FeedingLog, ownerUid: String? = null): Result<String> {
        val id = if (log.id.isEmpty()) com.example.nutriia.platform.generateUUID() else log.id
        val newLog = log.copy(id = id)
        val current = feedingsState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != id } + newLog
        feedingsState.value = feedingsState.value + (childId to updated)
        return Result.success(id)
    }

    suspend fun deleteFeedingLog(childId: String, logId: String, ownerUid: String? = null): Result<Unit> {
        val current = feedingsState.value[childId] ?: emptyList()
        val updated = current.filter { it.id != logId }
        feedingsState.value = feedingsState.value + (childId to updated)
        return Result.success(Unit)
    }

    fun observeFeedingsByDate(childId: String, date: String, ownerUid: String? = null): Flow<List<FeedingLog>> {
        return feedingsState.map { map ->
            (map[childId] ?: emptyList()).filter { it.date == date }.sortedByDescending { it.startTime }
        }
    }

    fun observeAllFeedings(childId: String, ownerUid: String? = null): Flow<List<FeedingLog>> {
        return feedingsState.map { map ->
            (map[childId] ?: emptyList()).sortedByDescending { it.date + it.startTime }
        }
    }
}
