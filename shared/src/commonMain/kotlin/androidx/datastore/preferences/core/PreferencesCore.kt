package androidx.datastore.preferences.core

interface Preferences {
    operator fun <T> get(key: Key<T>): T? = null
}

interface MutablePreferences : Preferences {
    operator fun <T> set(key: Key<T>, value: T) {}
}

class Key<T>(val name: String)

fun stringPreferencesKey(name: String): Key<String> = Key(name)
fun booleanPreferencesKey(name: String): Key<Boolean> = Key(name)
fun intPreferencesKey(name: String): Key<Int> = Key(name)

suspend fun Any?.edit(transform: suspend (MutablePreferences) -> Unit): Preferences {
    val prefs = object : MutablePreferences {}
    transform(prefs)
    return prefs
}
