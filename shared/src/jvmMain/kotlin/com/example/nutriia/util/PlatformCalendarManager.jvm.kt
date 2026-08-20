package com.example.nutriia.util

actual object PlatformCalendarManager {
    actual fun addEvents(events: List<CalendarEvent>, onResult: (Boolean) -> Unit) {
        // En JVM/Desktop/Android (si se usa este target para Android)
        // se implementaría vía Intent en Android o similar.
        onResult(true)
    }
}
