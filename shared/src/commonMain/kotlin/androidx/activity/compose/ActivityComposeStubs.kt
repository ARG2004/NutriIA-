package androidx.activity.compose

import androidx.compose.runtime.Composable

interface ManagedActivityResultLauncher<I, O> {
    fun launch(input: I)
}

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: Any,
    onResult: (O) -> Unit
): ManagedActivityResultLauncher<I, O> {
    return object : ManagedActivityResultLauncher<I, O> {
        override fun launch(input: I) {}
    }
}
