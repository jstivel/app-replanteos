// app/src/main/java/com/example/replanteosapp/managers/PhotoWorkflowManager.kt
package com.example.replanteosapp.managers

import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageProxy
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
        // CORRECCIÓN: Aquí es donde pasamos el callback al takePhoto de CameraManager
        cameraManager.takePhoto(object : ImageCaptureCallback {
            override fun onImageCaptured(image: ImageProxy) { // <-- Ahora recibe ImageProxy
                Log.d(TAG, "ImageProxy capturada. Procesando...")
                // Iniciar procesamiento de imagen aquí
                val processedImageUri = imageProcessor.processAndSaveImage(
                    imageProxy  = image, // <--- Pasar el ImageProxy
                    locationData = locationData,
                    textOverlayConfig = currentTextOverlayConfig
                )

                // CERRAR EL IMAGEPROXY DESPUÉS DE PROCESARLO
                // Es crucial llamar a image.close() para liberar los recursos.
                // ImageProcessor debería manejar esto, pero si no lo hace, asegúrate aquí.
                image.close()

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

        // ELIMINA ESTA LÍNEA si estaba repetida: cameraManager.takePhoto()
        // No necesitas llamar a cameraManager.setImageCaptureCallback() por separado aquí
        // porque ya se lo pasas directamente a takePhoto.
    }

    companion object {
        private const val TAG = "PhotoWorkflowManager"
    }
}