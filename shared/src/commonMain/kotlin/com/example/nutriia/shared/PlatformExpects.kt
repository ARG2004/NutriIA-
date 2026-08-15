package com.example.nutriia.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

val NutriaGreen = Color(0xFF689F38)

@Composable
expect fun NutriaMascotaHeader(modifier: Modifier = Modifier)

@Composable
expect fun NutriaSplashMascota(modifier: Modifier = Modifier)
