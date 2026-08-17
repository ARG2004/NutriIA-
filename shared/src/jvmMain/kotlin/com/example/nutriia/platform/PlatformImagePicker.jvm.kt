package com.example.nutriia.platform

actual object PlatformImagePicker {
    actual fun launchCamera(onImageCaptured: (base64Image: String) -> Unit) {}
    actual fun launchGallery(onImageSelected: (base64Image: String) -> Unit) {}
}
