@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.platform

import platform.AVFoundation.*
import platform.Foundation.base64EncodedStringWithOptions
import platform.Photos.*
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual object PlatformImagePicker {

    private var activeDelegate: Any? = null

    actual fun launchCamera(onImageCaptured: (base64Image: String) -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                dispatch_async(dispatch_get_main_queue()) {
                    if (granted) {
                        presentPicker(
                            sourceType = if (UIImagePickerController.isSourceTypeAvailable(UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera)) {
                                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera
                            } else {
                                UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary
                            },
                            onImage = onImageCaptured
                        )
                    } else {
                        // Fallback a galería si la cámara no tiene permiso
                        launchGallery(onImageCaptured)
                    }
                }
            }
        }
    }

    actual fun launchGallery(onImageSelected: (base64Image: String) -> Unit) {
        dispatch_async(dispatch_get_main_queue()) {
            presentPicker(
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                onImage = onImageSelected
            )
        }
    }

    private fun presentPicker(
        sourceType: UIImagePickerControllerSourceType,
        onImage: (String) -> Unit
    ) {
        val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
        val picker = UIImagePickerController()
        picker.sourceType = sourceType
        picker.allowsEditing = false

        val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
            override fun imagePickerController(
                picker: UIImagePickerController,
                didFinishPickingMediaWithInfo: Map<Any?, *>
            ) {
                val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
                if (image != null) {
                    val data = UIImageJPEGRepresentation(image, 0.6)
                    if (data != null) {
                        val base64 = data.base64EncodedStringWithOptions(0u)
                        onImage(base64)
                    }
                }
                picker.dismissViewControllerAnimated(true, null)
                activeDelegate = null
            }

            override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
                picker.dismissViewControllerAnimated(true, null)
                activeDelegate = null
            }
        }

        activeDelegate = delegate
        picker.delegate = delegate
        rootVC.presentViewController(picker, animated = true, completion = null)
    }
}
