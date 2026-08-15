package androidx.core.content

import android.content.Context

object ContextCompat {
    const val PERMISSION_GRANTED = 0
    const val PERMISSION_DENIED = -1

    fun checkSelfPermission(context: Context, permission: String): Int = PERMISSION_GRANTED
    fun getSystemService(context: Context, serviceClass: Any): Any? = null
}
