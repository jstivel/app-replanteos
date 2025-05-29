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
    private var currentTextOverlayConfig: TextOverlayConfig? = null


    fun setWorkflowCallback(callback: PhotoWorkflowCallback) {
        this.workflowCallback = callback
    }

    fun setTextOverlayConfig(config: TextOverlayConfig) {
        this.currentTextOverlayConfig = config // Actualiza la configuración cuando se le pasa
    }

    fun executePhotoCaptureWorkflow(locationData: LocationData?) {
        cameraManager.takePhoto(object : ImageCaptureCallback {
            override fun onImageCaptured(image: ImageProxy) {
                // Se ha capturado la imagen, ahora procesar y guardar
                val config = currentTextOverlayConfig ?: TextOverlayConfig() // Usa la configuración actual o una por defecto

                // Pasa el ratio de salida deseado a ImageProcessor
                val processedImageUri = imageProcessor.processAndSaveImage(
                    imageProxy = image,
                    locationData = locationData,
                    textOverlayConfig = config,
                    desiredOutputAspectRatio = cameraManager.getDesiredOutputAspectRatio() // ¡NUEVO!
                )

                if (processedImageUri != null) {
                    // Si la imagen se procesó y guardó con éxito, verifica si el texto se dibujó.
                    // (La lógica para saber si el texto se dibujó ahora está en ImageProcessor).
                    workflowCallback?.onPhotoProcessed(processedImageUri, locationData)
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