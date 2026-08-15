package androidx.datastore.preferences.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface Preferences {
    operator fun <T> get(key: Key<T>): T? = null
}

interface MutablePreferences : Preferences {
    operator fun <T> set(key: Key<T>, value: T) {}
    fun clear() {}
}

class Key<T>(val name: String)

fun stringPreferencesKey(name: String): Key<String> = Key(name)
fun booleanPreferencesKey(name: String): Key<Boolean> = Key(name)
fun intPreferencesKey(name: String): Key<Int> = Key(name)

interface DataStore<T> {
    val data: Flow<T>
    suspend fun updateData(transform: suspend (t: T) -> T): T
}

class PreferencesDataStore : DataStore<Preferences> {
    private val inMemoryPrefs = object : MutablePreferences {
        private val map = mutableMapOf<String, Any?>()
        override fun <T> get(key: Key<T>): T? = map[key.name] as? T
        override fun <T> set(key: Key<T>, value: T) { map[key.name] = value }
        override fun clear() { map.clear() }
    }

    override val data: Flow<Preferences> = flowOf(inMemoryPrefs)

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        return transform(inMemoryPrefs)
    }

    suspend fun edit(transform: suspend (MutablePreferences) -> Unit): Preferences {
        transform(inMemoryPrefs)
        return inMemoryPrefs
    }
}

suspend fun DataStore<Preferences>.edit(transform: suspend (MutablePreferences) -> Unit): Preferences {
    val prefs = object : MutablePreferences {}
    transform(prefs)
    return prefs
}
