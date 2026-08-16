package androidx.activity.compose

import androidx.compose.runtime.Composable
import androidx.activity.result.contract.ActivityResultContract

interface ManagedActivityResultLauncher<I, O> {
    fun launch(input: I)
}

@Composable
fun <I, O> rememberLauncherForActivityResult(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit
): ManagedActivityResultLauncher<I, O> {
    return object : ManagedActivityResultLauncher<I, O> {
        override fun launch(input: I) {}
    }
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
