package androidx.lifecycle

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.StateFlow

open class ViewModel {
    val viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    open fun onCleared() {}
}

open class AndroidViewModel(val application: android.app.Application = android.app.Application()) : ViewModel() {
    fun <T : android.app.Application> getApplication(): T = application as T
}

interface LifecycleOwner

class Lifecycle {
    enum class State { INITIALIZED, CREATED, STARTED, RESUMED, DESTROYED }
    enum class Event { ON_CREATE, ON_START, ON_RESUME, ON_PAUSE, ON_STOP, ON_DESTROY, ON_ANY }
}

interface LifecycleEventObserver {
    fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event)
}

object LocalLifecycleOwner {
    val current: LifecycleOwner = object : LifecycleOwner {}
}

@Composable
fun <T> StateFlow<T>.collectAsStateWithLifecycle(): State<T> = this.collectAsState()

class ViewModelProvider(val owner: Any? = null) {
    operator fun <T : ViewModel> get(modelClass: Any?): T = modelClass as T
}
