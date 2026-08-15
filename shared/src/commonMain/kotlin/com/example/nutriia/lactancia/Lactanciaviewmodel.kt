package com.example.nutriia.lactancia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class LactanciaUiState {
    object Idle    : LactanciaUiState()
    object Loading : LactanciaUiState()
    object Saved   : LactanciaUiState()
    object Deleted : LactanciaUiState()
    data class Error(val msg: String) : LactanciaUiState()
}

class LactanciaViewModel(application: Application) : AndroidViewModel(application) {

    // Usa directamente LactanciaRepository — el offline lo maneja Firestore
    private val repo = LactanciaRepository()

    private val _uiState         = MutableStateFlow<LactanciaUiState>(LactanciaUiState.Idle)
    val uiState: StateFlow<LactanciaUiState> = _uiState.asStateFlow()

    private val _todayLogs       = MutableStateFlow<List<FeedingLog>>(emptyList())
    val todayLogs: StateFlow<List<FeedingLog>> = _todayLogs.asStateFlow()

    private val _summary         = MutableStateFlow(DailyFeedingSummary())
    val summary: StateFlow<DailyFeedingSummary> = _summary.asStateFlow()

    private val _weeklySummaries = MutableStateFlow<List<DailyFeedingSummary>>(emptyList())
    val weeklySummaries: StateFlow<List<DailyFeedingSummary>> = _weeklySummaries.asStateFlow()

    private val _nextFeedingIn   = MutableStateFlow("")
    val nextFeedingIn: StateFlow<String> = _nextFeedingIn.asStateFlow()

    private val _omsRec          = MutableStateFlow<OmsLactanciaRecommendation?>(null)
    val omsRec: StateFlow<OmsLactanciaRecommendation?> = _omsRec.asStateFlow()

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val today: String get() = dateFmt.format(Date())

    private var currentChildId: String? = null
    private var observeJob: Job? = null

    // ── Init ──────────────────────────────────────────────────────────────────

    fun init(childId: String, ageMonths: Int) {
        _omsRec.value = omsLactanciaData
            .filter { it.minAgeMonths <= ageMonths.coerceAtLeast(0) }
            .maxByOrNull { it.minAgeMonths }
            ?: omsLactanciaData.firstOrNull()

        if (childId == currentChildId) return
        currentChildId = childId

        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observeFeedingsByDate(childId, today)
                .catch { e ->
                    _uiState.value = LactanciaUiState.Error(e.message ?: "Error al cargar tomas")
                    emit(emptyList())
                }
                .collect { logs ->
                    _todayLogs.value = logs
                    recalcSummary(logs)
                    recalcNextFeeding(logs)
                }
        }

        loadWeekly(childId)
    }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun saveFeeding(childId: String, log: FeedingLog) {
        viewModelScope.launch {
            _uiState.value = LactanciaUiState.Loading
            repo.saveFeedingLog(childId, log)
                .onSuccess { _uiState.value = LactanciaUiState.Saved }
                .onFailure { _uiState.value = LactanciaUiState.Error(it.message ?: "Error al guardar") }
        }
    }

    fun deleteFeeding(childId: String, logId: String) {
        viewModelScope.launch {
            repo.deleteFeedingLog(childId, logId)
                .onSuccess { _uiState.value = LactanciaUiState.Deleted }
                .onFailure { _uiState.value = LactanciaUiState.Error(it.message ?: "Error al eliminar") }
        }
    }

    fun resetState() { _uiState.value = LactanciaUiState.Idle }

    // ── Resumen semanal ───────────────────────────────────────────────────────

    private fun loadWeekly(childId: String) {
        viewModelScope.launch {
            try {
                val dias = (0..6).map { offset ->
                    val cal = java.util.Calendar.getInstance()
                    cal.add(java.util.Calendar.DAY_OF_YEAR, -offset)
                    dateFmt.format(cal.time)
                }
                val flows = dias.map { dia ->
                    repo.observeFeedingsByDate(childId, dia)
                        .catch { emit(emptyList()) }
                        .map { logs -> buildSummary(dia, logs) }
                }
                combine(flows) { summaries ->
                    summaries.toList().sortedBy { it.date }
                }.collect { semana ->
                    _weeklySummaries.value = semana
                }
            } catch (e: Exception) { /* resumen semanal no crítico */ }
        }
    }

    // ── Cálculos internos ─────────────────────────────────────────────────────

    private fun recalcSummary(logs: List<FeedingLog>) {
        _summary.value = buildSummary(today, logs)
    }

    private fun buildSummary(date: String, logs: List<FeedingLog>): DailyFeedingSummary {
        val sorted    = logs.sortedBy { it.startTime }
        val intervals = if (sorted.size >= 2) {
            sorted.zipWithNext { a, b ->
                val tA = parseTimeToMinutes(a.startTime)
                val tB = parseTimeToMinutes(b.startTime)
                if (tB > tA) tB - tA else 0
            }.filter { it > 0 }
        } else emptyList()

        return DailyFeedingSummary(
            date               = date,
            totalSessions      = logs.size,
            totalMinutes       = logs.sumOf { it.durationMinutes },
            totalFormulaMl     = logs.filter { it.side == BreastSide.FORMULA.name }.sumOf { it.formulaMl },
            leftSessions       = logs.count { it.side == BreastSide.LEFT.name },
            rightSessions      = logs.count { it.side == BreastSide.RIGHT.name },
            formulaSessions    = logs.count { it.side == BreastSide.FORMULA.name },
            avgIntervalMinutes = if (intervals.isNotEmpty()) intervals.average().toInt() else 0
        )
    }

    private fun recalcNextFeeding(logs: List<FeedingLog>) {
        if (logs.isEmpty()) { _nextFeedingIn.value = ""; return }
        val rec = _omsRec.value ?: run { _nextFeedingIn.value = ""; return }

        val lastLog     = logs.maxByOrNull { it.startTime } ?: return
        val lastMinutes = parseTimeToMinutes(lastLog.startTime)
        if (lastMinutes < 0) { _nextFeedingIn.value = ""; return }

        val intervalMinutes = (rec.minIntervalHours * 60).toInt()
        val nextMinutes     = lastMinutes + intervalMinutes

        val cal    = java.util.Calendar.getInstance()
        val nowMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 +
                cal.get(java.util.Calendar.MINUTE)

        val diffMin = nextMinutes - nowMin
        _nextFeedingIn.value = when {
            diffMin <= 0  -> "Es hora de la toma"
            diffMin < 60  -> "En $diffMin min"
            else          -> "En ${diffMin / 60}h ${diffMin % 60}min"
        }
    }

    private fun parseTimeToMinutes(time: String): Int {
        return try {
            val parts = time.split(":")
            if (parts.size != 2) return -1
            parts[0].toInt() * 60 + parts[1].toInt()
        } catch (e: Exception) { -1 }
    }
}