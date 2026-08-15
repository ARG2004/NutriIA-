package androidx.datastore.preferences.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

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

package androidx.datastore.preferences

import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

fun preferencesDataStore(name: String): Any = object {
    val data: Flow<Preferences> = flowOf(object : Preferences {})
}

package com.google.mediapipe.tasks.vision.handlandmarker

import android.content.Context

class HandLandmarker {
    companion object {
        fun createFromOptions(context: Context, options: Any?): HandLandmarker = HandLandmarker()
    }
    fun detectAsync(image: Any?, timestampMs: Long) {}
    fun close() {}
}

package io.github.webrtc

class PeerConnection
class MediaStream
class VideoTrack
class AudioTrack
