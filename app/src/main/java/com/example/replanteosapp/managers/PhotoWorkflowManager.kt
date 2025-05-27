// app/src/main/java/com/example/replanteosapp/managers/PhotoWorkflowManager.kt
package com.example.replanteosapp.managers

import android.net.Uri
import android.util.Log
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.services.ImageProcessor

class PhotoWorkflowManager(
    private val cameraManager: CameraManager,
    private val locationTracker: LocationTracker,
    private val imageProcessor: ImageProcessor // Ahora ImageProcessor está en services
) {

    private var workflowCallback: PhotoWorkflowCallback? = null
    private var currentTextOverlayConfig: TextOverlayConfig = TextOverlayConfig() // Almacena la configuración aquí

    fun setWorkflowCallback(callback: PhotoWorkflowCallback) {
        this.workflowCallback = callback
    }

    fun setTextOverlayConfig(config: TextOverlayConfig) {
        this.currentTextOverlayConfig = config // Actualiza la configuración cuando se le pasa
    }

    fun executePhotoCaptureWorkflow(locationData: LocationData?) {
        // Asegúrate de que CameraManager esté configurado con el callback adecuado
        // para que solo tome la foto una vez y luego notifique aquí.
        cameraManager.setImageCaptureCallback(object : ImageCaptureCallback {
            override fun onImageCaptured(imageUri: Uri) {
                Log.d(TAG, "Foto capturada: $imageUri. Procesando...")
                // Iniciar procesamiento de imagen aquí
                val processedImageUri = imageProcessor.processAndSaveImage(
                    originalImageUri = imageUri,
                    locationData = locationData, // Pasa los datos de ubicación
                    textOverlayConfig = currentTextOverlayConfig // Pasa la configuración actual
                )

                // Eliminar la imagen original no procesada si CameraManager la guardó
                // Ojo: CameraX puede borrarla por si mismo o no, dependiendo de la configuración.
                // Si la imagen original no procesada persiste, puedes eliminarla aquí
                // si no es la URI final de la imagen procesada.
                // Asegúrate de que originalImageUri NO sea la misma que processedImageUri
                // Si ImageProcessor guarda una nueva, puedes borrar la original aquí.
                // cameraManager.deleteOriginalImage(imageUri) // Si tienes un método para borrar la URI original

                if (processedImageUri != null) {
                    workflowCallback?.onPhotoProcessed(processedImageUri, locationData)
                } else {
                    workflowCallback?.onPhotoProcessingError("Fallo al procesar o guardar la imagen.")
                }
            }

            override fun onError(errorMessage: String) {
                workflowCallback?.onPhotoProcessingError("Error al capturar la foto: $errorMessage")
                Log.e(TAG, "Error en CameraManager al capturar: $errorMessage")
            }
        })

        // Aquí es donde el CameraManager toma la foto. SOLO DEBE LLAMARSE UNA VEZ.
        cameraManager.takePhoto()
    }

    companion object {
        private const val TAG = "PhotoWorkflowManager"
    }
}