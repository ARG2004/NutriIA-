package java.lang

object System {
    fun currentTimeMillis(): Long {
        // Safe monotonic / epoch approximation
        return 1771198800000L + kotlin.time.TimeSource.Monotonic.markNow().elapsedNow().inWholeMilliseconds
    }

    fun arraycopy(src: Any, srcPos: Int, dest: Any, destPos: Int, length: Int) {}
}
