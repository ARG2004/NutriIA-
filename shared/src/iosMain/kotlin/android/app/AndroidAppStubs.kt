package android.app

import android.content.Context
import android.content.Intent

open class Application : Context()
open class Activity : Context()

class AlarmManager {
    fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent) {}
    fun cancel(operation: PendingIntent) {}
    companion object {
        const val RTC_WAKEUP = 0
    }
}

class NotificationChannel(id: String, name: CharSequence, importance: Int) {
    fun setDescription(description: String) {}
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
