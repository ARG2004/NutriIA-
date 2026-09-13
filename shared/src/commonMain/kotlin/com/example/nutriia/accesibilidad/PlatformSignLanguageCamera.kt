package com.example.nutriia.accesibilidad

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Vista de cámara multiplataforma que transmite puntos óseos de la mano (21 landmarks)
 * detectados en tiempo real hacia la capa común de Compose.
 */
@Composable
expect fun PlatformSignLanguageCameraPreview(
    modifier: Modifier = Modifier,
    onLandmarksDetected: (List<NormalizedPoint3D>) -> Unit
)
