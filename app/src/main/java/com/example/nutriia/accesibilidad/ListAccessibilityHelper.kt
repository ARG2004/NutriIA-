package com.example.nutriia.accesibilidad

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * Añade semántica clara de conteo de lista para lectores de pantalla.
 * Ejemplo: "Elemento 1 de 8. Leche Materna, 120 ml".
 */
fun Modifier.anuncioItemLista(
    indice: Int,
    total: Int,
    descripcion: String,
    idioma: IdiomaVoz = IdiomaVoz.ESPANOL_MX
): Modifier = this.semantics {
    val textoConteo = if (idioma == IdiomaVoz.INGLES) {
        "Item ${indice + 1} of $total. $descripcion"
    } else {
        "Elemento ${indice + 1} de $total. $descripcion"
    }
    contentDescription = textoConteo
}

/**
 * Detecta cuando el usuario llega al inicio o final de una lista con scroll
 * y emite una retroalimentación auditiva sutil (Earcon).
 */
@Composable
fun TrackScrollBoundary(
    listState: LazyListState
) {
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@snapshotFlow null
            val isAtTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            val isAtBottom = layoutInfo.visibleItemsInfo.lastOrNull()?.index == totalItems - 1
            when {
                isAtTop -> "TOP"
                isAtBottom -> "BOTTOM"
                else -> null
            }
        }.collect { boundary ->
            if (boundary != null) {
                NutriEarcons.playButtonHover()
            }
        }
    }
}
