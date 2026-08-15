package androidx.datastore.preferences

import androidx.datastore.preferences.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesDataStore
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

fun preferencesDataStore(name: String): ReadOnlyProperty<Any?, DataStore<Preferences>> {
    val store = PreferencesDataStore()
    return ReadOnlyProperty { _, _ -> store }
}
