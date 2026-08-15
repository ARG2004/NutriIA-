package com.example.nutriia.shared

// Multiplatform Timestamp compatible with Firestore
data class Timestamp(
    val seconds: Long = 0L,
    val nanoseconds: Int = 0
) {
    companion object {
        fun now(): Timestamp = Timestamp(0L, 0)
    }
}
