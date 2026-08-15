package com.example.nutriia.shared

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image as SkiaImage
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
private fun loadBundleImageBitmap(resourceName: String, ext: String = "webp"): ImageBitmap? {
    val path = NSBundle.mainBundle.pathForResource(resourceName, ext) ?: return null
    val data = NSData.dataWithContentsOfFile(path) ?: return null
    val length = data.length.toInt()
    if (length <= 0) return null
    val bytes = ByteArray(length)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), data.bytes, data.length)
    }
    return try {
        SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    } catch (_: Throwable) {
        null
    }
}

@Composable
actual fun NutriaMascotaHeader(modifier: Modifier) {
    val bitmap = remember { loadBundleImageBitmap("ic_nutria") ?: loadBundleImageBitmap("ic_splash") }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Logo NutrIA",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(3.dp, NutriaGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Eco,
                    contentDescription = "NutrIA",
                    tint = NutriaGreen,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
    }
}

@Composable
actual fun NutriaSplashMascota(modifier: Modifier) {
    val bitmap = remember { loadBundleImageBitmap("ic_splash") ?: loadBundleImageBitmap("ic_nutria") }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Splash NutrIA",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9))
                    .border(3.dp, NutriaGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Eco,
                    contentDescription = null,
                    tint = NutriaGreen,
                    modifier = Modifier.size(90.dp)
                )
            }
        }
    }
}
