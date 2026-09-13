package com.example.nutriia.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    COMPACT_SMALL,   // < 360dp (Teléfonos compactos 4.0" - 4.7")
    COMPACT_NORMAL,  // 360dp .. 599dp (Smartphones estándar 5.0" - 6.7")
    MEDIUM,          // 600dp .. 839dp (Plegables / Tablets pequeñas 7.0" - 8.4")
    EXPANDED         // >= 840dp (Tablets 9.7" - 10.2"+ y monitores)
}

data class ScreenConfiguration(
    val widthClass: WindowWidthClass,
    val screenWidthDp: Dp,
    val screenHeightDp: Dp,
    val isCompactSmall: Boolean,
    val isTablet: Boolean,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val cardSpacing: Dp,
    val cameraPreviewHeight: Dp,
    val maxContentWidth: Dp
)

val LocalScreenConfig = compositionLocalOf {
    ScreenConfiguration(
        widthClass = WindowWidthClass.COMPACT_NORMAL,
        screenWidthDp = 390.dp,
        screenHeightDp = 844.dp,
        isCompactSmall = false,
        isTablet = false,
        horizontalPadding = 16.dp,
        verticalPadding = 16.dp,
        cardSpacing = 12.dp,
        cameraPreviewHeight = 320.dp,
        maxContentWidth = 640.dp
    )
}

/**
 * Proveedor de configuración responsiva que calcula las dimensiones exactas
 * del viewport en cualquier dispositivo (desde 4.0" hasta 10.2"+).
 */
@Composable
fun ProvideScreenConfiguration(
    content: @Composable () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val width = maxWidth
        val height = maxHeight

        val isSmallScreen = width < 360.dp || height < 640.dp
        val isTabletScreen = width >= 600.dp

        val widthClass = when {
            width < 360.dp -> WindowWidthClass.COMPACT_SMALL
            width < 600.dp -> WindowWidthClass.COMPACT_NORMAL
            width < 840.dp -> WindowWidthClass.MEDIUM
            else -> WindowWidthClass.EXPANDED
        }

        val config = remember(width, height) {
            ScreenConfiguration(
                widthClass = widthClass,
                screenWidthDp = width,
                screenHeightDp = height,
                isCompactSmall = isSmallScreen,
                isTablet = isTabletScreen,
                horizontalPadding = when {
                    isSmallScreen -> 10.dp
                    isTabletScreen -> 24.dp
                    else -> 16.dp
                },
                verticalPadding = when {
                    isSmallScreen -> 10.dp
                    isTabletScreen -> 20.dp
                    else -> 16.dp
                },
                cardSpacing = when {
                    isSmallScreen -> 8.dp
                    isTabletScreen -> 16.dp
                    else -> 12.dp
                },
                cameraPreviewHeight = when {
                    isSmallScreen -> 220.dp
                    isTabletScreen -> 380.dp
                    else -> 320.dp
                },
                maxContentWidth = when {
                    width >= 1024.dp -> 860.dp
                    width >= 840.dp -> 720.dp
                    width >= 600.dp -> 600.dp
                    else -> Dp.Unspecified
                }
            )
        }

        CompositionLocalProvider(LocalScreenConfig provides config) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                content()
            }
        }
    }
}

/**
 * Modificador para centrar elegantemente formularios o pantallas en tablets,
 * evitando que el contenido se estire de forma desproporcionada.
 */
fun Modifier.responsiveContent(maxWidth: Dp = 640.dp): Modifier = this.widthIn(max = maxWidth)
