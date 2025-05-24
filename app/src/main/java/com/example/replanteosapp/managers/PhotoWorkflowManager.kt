package com.example.replanteosapp.managers

import android.net.Uri
import android.util.Log
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig // Importar
import com.example.replanteosapp.services.ImageProcessor

class PhotoWorkflowManager(
    private val cameraManager: CameraManager,
    private val locationTracker: LocationTracker,
    private val imageProcessor: ImageProcessor
) {
    private var workflowCallback: PhotoWorkflowCallback? = null
    private var currentTextOverlayConfig: TextOverlayConfig = TextOverlayConfig() // Inicializa con una configuración por defecto

    fun setWorkflowCallback(callback: PhotoWorkflowCallback) {
        this.workflowCallback = callback
    }

    fun setTextOverlayConfig(config: TextOverlayConfig) {
        this.currentTextOverlayConfig = config
        Log.d(TAG, "TextOverlayConfig actualizada en PhotoWorkflowManager: $config")
    }

    fun executePhotoCaptureWorkflow(locationData: LocationData?) {
        cameraManager.takePhoto(object : ImageCaptureCallback {
            override fun onImageCaptured(imageUri: Uri) {
                // Ahora pasamos la configuración actual a ImageProcessor
                val processedImageUri = imageProcessor.processAndSaveImage(
                    imageUri,
                    locationData,
                    currentTextOverlayConfig // Pasa la configuración aquí
                )

                if (processedImageUri != null) {
                    workflowCallback?.onPhotoProcessed(processedImageUri, locationData)
                } else {
                    // Si processedImageUri es nulo, significa que hubo un error al dibujar o guardar
                    // Decide si quieres pasar la URI original o un error más específico
                    workflowCallback?.onPhotoProcessingError("Error al procesar la imagen con texto.")
                    // Podrías considerar llamar a onLocationUnavailableForPhoto(imageUri) si el problema fue solo el texto
                }
            }

            override fun onError(errorMessage: String) {
                workflowCallback?.onPhotoProcessingError("Error al capturar la foto: $errorMessage")
            }
        })
    }

    companion object {
        private const val TAG = "PhotoWorkflowManager"
    }
}