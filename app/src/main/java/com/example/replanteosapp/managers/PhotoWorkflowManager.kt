// app/src/main/java/com/example/replanteosapp/managers/PhotoWorkflowManager.kt
package com.example.replanteosapp.managers

import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.services.ImageProcessor
import com.example.replanteosapp.presenters.MainPresenter.Companion.CameraRatios

class PhotoWorkflowManager(
    private val cameraManager: CameraManager,
    private val locationTracker: LocationTracker,
    private val imageProcessor: ImageProcessor // Ahora ImageProcessor está en services

) {
    private var workflowCallback: PhotoWorkflowCallback? = null
    private var textOverlayConfig: TextOverlayConfig = TextOverlayConfig()


    fun setWorkflowCallback(callback: PhotoWorkflowCallback) {
        this.workflowCallback = callback
    }

    fun setTextOverlayConfig(config: TextOverlayConfig) {
        this.textOverlayConfig = config
    }

    fun executePhotoCaptureWorkflow(locationData: LocationData?, captureAspectRatio: Int) {
        cameraManager.takePhoto(object : ImageCaptureCallback {
            override fun onImageCaptured(image: ImageProxy) {
                // Se ha capturado la imagen, ahora procesar y guardar
                val config = textOverlayConfig ?: TextOverlayConfig() // Usa la configuración actual o una por defecto

                // Pasa el ratio de salida deseado a ImageProcessor
                val processedImageUri = imageProcessor.processAndSaveImage(
                    imageProxy = image,
                    locationData = locationData,
                    textOverlayConfig = config,
                    desiredOutputAspectRatio = cameraManager.getCameraXCaptureAspectRatio()
                )

                if (processedImageUri != null) {
                    if (locationData != null) {
                        workflowCallback?.onPhotoProcessed(processedImageUri, locationData)
                    } else {
                        workflowCallback?.onLocationUnavailableForPhoto(processedImageUri)
                    }
                } else {
                    // Aquí la URI es null, lo que indica un error general en el procesamiento.
                    workflowCallback?.onPhotoProcessingError("Error desconocido al procesar la foto.")
                }
            }

            override fun onError(errorMessage: String) {
                workflowCallback?.onPhotoProcessingError(errorMessage)
            }
        })
    }

    companion object {
        private const val TAG = "PhotoWorkflowManager"
    }
}