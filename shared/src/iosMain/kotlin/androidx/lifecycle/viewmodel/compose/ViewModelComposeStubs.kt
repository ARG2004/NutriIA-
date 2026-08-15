package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel

@Composable
inline fun <reified VM : ViewModel> viewModel(): VM {
    return VM::class.constructors.first().call()
}
