package com.example.nutriia.shared

import kotlinx.serialization.Serializable

@Serializable
data class Timestamp(
    val seconds: Long = 0L,
    val nanoseconds: Int = 0
) : Comparable<Timestamp> {
    val time: Long get() = toEpochMillis()

    companion object {
        fun now(): Timestamp = Timestamp(com.example.nutriia.platform.currentTimeMillis() / 1000, 0)
    }

    fun toEpochMillis(): Long = seconds * 1000 + (nanoseconds / 1_000_000)
    fun toDate(): Timestamp = this

    override operator fun compareTo(other: Timestamp): Int {
        val sec = seconds.compareTo(other.seconds)
        return if (sec != 0) sec else nanoseconds.compareTo(other.nanoseconds)
    }
}
