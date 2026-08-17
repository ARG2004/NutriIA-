package com.example.nutriia.platform

expect object PlatformImagePicker {
    fun launchCamera(onImageCaptured: (base64Image: String) -> Unit)
    fun launchGallery(onImageSelected: (base64Image: String) -> Unit)
}
