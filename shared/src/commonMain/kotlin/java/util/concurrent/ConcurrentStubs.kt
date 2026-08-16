package java.util.concurrent

import java.lang.Runnable

enum class TimeUnit {
    NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS;
    fun toMillis(duration: Long): Long = duration
    fun toSeconds(duration: Long): Long = duration
}

interface Executor {
    fun execute(command: Runnable)
}

interface ExecutorService : Executor {
    fun shutdown()
}

object Executors {
    fun newSingleThreadExecutor(): ExecutorService = object : ExecutorService {
        override fun execute(command: Runnable) { command.run() }
        override fun shutdown() {}
    }
    fun newFixedThreadPool(nThreads: Int): ExecutorService = object : ExecutorService {
        override fun execute(command: Runnable) { command.run() }
        override fun shutdown() {}
    }
}
