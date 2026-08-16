package kotlinx.coroutines.tasks

suspend fun <T> T.await(): T = this
