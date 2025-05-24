// app/src/main/java/com/example/replanteosapp/managers/PhotoWorkflowCallback.kt
package com.example.replanteosapp.managers

import android.net.Uri
import com.example.replanteosapp.data.LocationData

interface PhotoWorkflowCallback {
    /**
     * Se llama cuando la foto ha sido capturada, procesada con la ubicación y guardada exitosamente.
     * @param imageUri La URI de la imagen final guardada en el almacenamiento.
     * @param locationData Los datos de ubicación que se usaron para la foto (puede ser null si no había).
     */
    fun onPhotoProcessed(imageUri: Uri, locationData: LocationData?)

    /**
     * Se llama si ocurre un error en cualquier etapa del flujo de trabajo de la foto (captura, procesamiento, guardado).
     * @param errorMessage Un mensaje descriptivo del error.
     */
    fun onPhotoProcessingError(errorMessage: String)

    /**
     * Opcional: Se llama si la foto se guardó pero la ubicación no estaba disponible
     * o no se pudo dibujar correctamente sobre la imagen.
     * @param imageUri La URI de la imagen original o la imagen sin el texto de ubicación.
     */
    fun onLocationUnavailableForPhoto(imageUri: Uri)
}