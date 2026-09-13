package com.example.nutriia.accesibilidad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun PlatformSignLanguageCameraPreview(
    modifier: Modifier,
    onLandmarksDetected: (List<NormalizedPoint3D>) -> Unit
) {
    Box(
        modifier = modifier.background(Color(0xFF10101C)),
        contentAlignment = Alignment.Center
    ) {
        Text("Cámara LSM no disponible en entorno de escritorio", color = Color.Gray)
    }
}
