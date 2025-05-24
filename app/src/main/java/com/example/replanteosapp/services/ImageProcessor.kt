package com.example.replanteosapp.services

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.managers.TextOverlayManager // Asegúrate de importar

import java.io.IOException
import java.io.OutputStream

class ImageProcessor(private val contentResolver: ContentResolver) {

    private val textOverlayManager = TextOverlayManager() // Instancia de TextOverlayManager

    fun processAndSaveImage(
        imageUri: Uri,
        locationData: LocationData?,
        textOverlayConfig: TextOverlayConfig // Ahora acepta la configuración
    ): Uri? {
        var outputStream: OutputStream? = null
        try {
            // Decodificar la imagen desde la URI
            val originalBitmap = BitmapFactory.decodeStream(contentResolver.openInputStream(imageUri))
                ?: throw IOException("No se pudo decodificar el bitmap de la URI: $imageUri")

            // Dibujar el texto de ubicación en el bitmap
            val bitmapWithOverlay = textOverlayManager.drawTextOverlay(originalBitmap, locationData, textOverlayConfig)

            // Guardar la imagen modificada
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                put(MediaStore.Images.Media.RELATIVE_PATH, "DCIM/ReplanteosApp") // Directorio personalizado
            }

            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val newImageUri = contentResolver.insert(collection, contentValues)
                ?: throw IOException("Fallo al crear una nueva entrada en MediaStore")

            outputStream = contentResolver.openOutputStream(newImageUri)
                ?: throw IOException("Fallo al abrir OutputStream para la nueva URI")

            bitmapWithOverlay.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            Log.d(TAG, "Imagen procesada y guardada en: $newImageUri")

            // Eliminar la imagen original sin el texto si es necesario (opcional)
            // contentResolver.delete(imageUri, null, null)

            return newImageUri
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar y guardar la imagen: ${e.message}", e)
            return null
        } finally {
            outputStream?.close()
        }
    }

    companion object {
        private const val TAG = "ImageProcessor"
    }
}