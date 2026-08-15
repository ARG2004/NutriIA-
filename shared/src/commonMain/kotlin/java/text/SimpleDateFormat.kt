package java.text

import java.util.Date
import java.util.Locale
import java.util.TimeZone

class SimpleDateFormat(val pattern: String, val locale: Locale = Locale.getDefault()) {
    var timeZone: TimeZone = TimeZone.getDefault()
    var isLenient: Boolean = true

    fun format(date: Date): String = "2026-08-15"
    fun format(calendar: java.util.Calendar): String = "2026-08-15"
    fun format(millis: Long): String = "2026-08-15"
    fun format(obj: Any?): String = "2026-08-15"
    fun parse(source: String): Date? = Date(0L)
}
