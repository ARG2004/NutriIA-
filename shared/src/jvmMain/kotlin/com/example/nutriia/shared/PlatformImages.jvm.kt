package com.example.nutriia.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.example.nutriia.resources.Res
import com.example.nutriia.resources.ic_header
import com.example.nutriia.resources.ic_splash
import org.jetbrains.compose.resources.painterResource

@Composable
actual fun NutriaMascotaHeader(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(Res.drawable.ic_header),
            contentDescription = "Logo NutrIA",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
actual fun NutriaSplashMascota(modifier: Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(Res.drawable.ic_splash),
            contentDescription = "Splash NutrIA",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
