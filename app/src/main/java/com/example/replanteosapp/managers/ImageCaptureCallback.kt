package com.example.replanteosapp.managers

import android.net.Uri
import androidx.camera.core.ImageProxy

interface ImageCaptureCallback {
    fun onImageCaptured(imageUri: ImageProxy)
    fun onError(errorMessage: String)
}
