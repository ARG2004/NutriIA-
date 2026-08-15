package androidx.compose.ui.viewinterop

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T : Any> AndroidView(
    factory: (Any) -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = {}
) {
    Box(modifier = modifier)
}
