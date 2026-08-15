package android.app

import android.content.Context
import android.content.Intent

open class Application : Context()
open class Activity : Context()

class DatePickerDialog(
    context: Context,
    val listener: OnDateSetListener? = null,
    year: Int = 2026,
    month: Int = 0,
    dayOfMonth: Int = 1
) {
    fun interface OnDateSetListener {
        fun onDateSet(view: Any?, year: Int, month: Int, dayOfMonth: Int)
    }
    fun setOnDismissListener(listener: Any?) {}
    fun show() {}
}

class TimePickerDialog(
    context: Context,
    val listener: OnTimeSetListener? = null,
    hourOfDay: Int = 12,
    minute: Int = 0,
    is24HourView: Boolean = true
) {
    fun interface OnTimeSetListener {
        fun onTimeSet(view: Any?, hourOfDay: Int, minute: Int)
    }
    fun setOnDismissListener(listener: Any?) {}
    fun show() {}
}

class AlarmManager {
    fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent) {}
    fun cancel(operation: PendingIntent) {}
    companion object {
        const val RTC_WAKEUP = 0
    }
}

class NotificationChannel(id: String, name: CharSequence, importance: Int) {
    var description: String = ""
    var enableVibration: Boolean = true
    var enableLights: Boolean = true
    var vibrationPattern: LongArray? = null
    fun setDescription(desc: String) { description = desc }
}

class NotificationManager {
    fun createNotificationChannel(channel: NotificationChannel) {}
    fun notify(id: Int, notification: Any) {}
    fun cancel(id: Int) {}
    companion object {
        const val IMPORTANCE_DEFAULT = 3
        const val IMPORTANCE_HIGH = 4
    }
}

class PendingIntent {
    companion object {
        const val FLAG_UPDATE_CURRENT = 134217728
        const val FLAG_IMMUTABLE = 67108864
        fun getBroadcast(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent = PendingIntent()
        fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent = PendingIntent()
    }
}
