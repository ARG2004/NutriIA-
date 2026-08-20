@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.util

import platform.EventKit.*
import platform.Foundation.*
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object PlatformCalendarManager {

    actual fun addEvents(events: List<CalendarEvent>, onResult: (Boolean) -> Unit) {
        val eventStore = EKEventStore()
        
        // Solicitar acceso al calendario
        // Nota: En iOS 17+ se recomienda requestWriteOnlyAccessToEvents
        // Para compatibilidad usaremos el flujo estándar
        eventStore.requestAccessToEntityType(entityType = EKEntityType.EKEntityTypeEvent) { granted, error ->
            dispatch_async(dispatch_get_main_queue()) {
                if (granted && error == null) {
                    var allSuccess = true
                    events.forEach { eventData ->
                        val event = EKEvent.eventWithEventStore(eventStore)
                        event.setTitle(eventData.title)
                        event.setNotes(eventData.description)
                        event.setStartDate(NSDate.dateWithTimeIntervalSince1970(eventData.startDate / 1000.0))
                        event.setEndDate(NSDate.dateWithTimeIntervalSince1970(eventData.endDate / 1000.0))
                        event.setAllDay(eventData.allDay)
                        event.setCalendar(eventStore.defaultCalendarForNewEvents)

                        val saveError = mutableListOf<platform.Foundation.NSError?>()
                        // Usamos try-catch o verificamos el retorno del booleano de saveEvent
                        val success = eventStore.saveEvent(event = event, span = EKSpan.EKSpanThisEvent, commit = true, error = null)
                        if (!success) allSuccess = false
                    }
                    onResult(allSuccess)
                } else {
                    onResult(false)
                }
            }
        }
    }
}
