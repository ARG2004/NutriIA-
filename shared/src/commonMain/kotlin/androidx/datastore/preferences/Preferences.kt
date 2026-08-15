package androidx.datastore.preferences

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun preferencesDataStore(name: String): Any = object {
    val data: Flow<Preferences> = flowOf(object : Preferences {})
}
