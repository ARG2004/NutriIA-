package com.example.nutriia.util

data class CalendarEvent(
    val title: String,
    val description: String,
    val startDate: Long, // Epoch millis
    val endDate: Long,   // Epoch millis
    val allDay: Boolean = false
)

expect object PlatformCalendarManager {
    fun addEvents(events: List<CalendarEvent>, onResult: (Boolean) -> Unit)
}
