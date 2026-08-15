package com.google.mediapipe.tasks.components.containers

class NormalizedLandmark(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val presence: Float = 1f,
    val visibility: Float = 1f
) {
    fun x(): Float = x
    fun y(): Float = y
    fun z(): Float = z

    companion object {
        fun create(x: Float, y: Float, z: Float): NormalizedLandmark = NormalizedLandmark(x, y, z)
        fun create(x: Float, y: Float, z: Float, presence: Float, visibility: Float): NormalizedLandmark =
            NormalizedLandmark(x, y, z, presence, visibility)
    }
}

class Landmark(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val presence: Float = 1f,
    val visibility: Float = 1f
) {
    fun x(): Float = x
    fun y(): Float = y
    fun z(): Float = z

    companion object {
        fun create(x: Float, y: Float, z: Float): Landmark = Landmark(x, y, z)
        fun create(x: Float, y: Float, z: Float, presence: Float, visibility: Float): Landmark =
            Landmark(x, y, z, presence, visibility)
    }
}
