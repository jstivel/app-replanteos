package com.example.replanteosapp.services

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageProxy // <-- ¡Importación crucial!
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.managers.TextOverlayManager
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer // <-- ¡Importación crucial!
import java.text.SimpleDateFormat
import java.util.Locale

class ImageProcessor(private val contentResolver: ContentResolver) {

    private val textOverlayManager = TextOverlayManager() // Esto está bien ahora, TextOverlayManager no necesita Context

    // MODIFICADO: Ahora recibe un ImageProxy. ESTA ES LA CLAVE.
    fun processAndSaveImage(
        imageProxy: ImageProxy, // <--- EL PARÁMETRO DEBE SER ImageProxy
        locationData: LocationData?,
        textOverlayConfig: TextOverlayConfig
    ): Uri? {
        var outputStream: OutputStream? = null
        try {
            val originalBitmap = imageProxyToBitmap(imageProxy)
                ?: throw IOException("No se pudo convertir ImageProxy a Bitmap.")

            // La rotación basada en imageProxy.imageInfo.rotationDegrees es correcta
            val rotatedBitmap = rotateBitmap(originalBitmap, imageProxy.imageInfo.rotationDegrees)

            // Dibuja el texto de ubicación en el bitmap
            val bitmapWithOverlay = textOverlayManager.drawTextOverlay(rotatedBitmap, locationData, textOverlayConfig)

            // Guarda la imagen modificada
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                // Usar RELATIVE_PATH para Android 10+
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ReplanteosApp")
            }

            val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val newImageUri = contentResolver.insert(collection, contentValues)
                ?: throw IOException("Fallo al crear una nueva entrada en MediaStore")

            outputStream = contentResolver.openOutputStream(newImageUri)
                ?: throw IOException("Fallo al abrir OutputStream para la nueva URI")

            bitmapWithOverlay.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            Log.d(TAG, "Imagen procesada y guardada en: $newImageUri")

            return newImageUri
        } catch (e: Exception) {
            Log.e(TAG, "Error al procesar y guardar la imagen: ${e.message}", e)
            return null
        } finally {
            // ¡MUY IMPORTANTE! Cerrar el ImageProxy cuando hayas terminado con él.
            // Esta llamada libera los recursos de memoria.
            imageProxy.close() // <--- ASEGURARSE DE QUE ESTO SE EJECUTE
            outputStream?.close()
        }
    }

    // Helper para convertir ImageProxy a Bitmap
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    // Helper para rotar el bitmap si es necesario
    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())

        // Calcula el nuevo bounding box para el bitmap rotado
        val rotatedRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        matrix.mapRect(rotatedRect)

        val newWidth = rotatedRect.width().toInt()
        val newHeight = rotatedRect.height().toInt()

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Esta función no se llama en el flujo principal, es solo para depuración si la necesitas
    private fun saveBitmapToTempFile(bitmap: Bitmap, prefix: String): Uri? {
        val name = prefix + SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/DEBUG_ReplanteosApp") // Directorio separado para depuración
        }

        val collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = contentResolver.insert(collection, contentValues)
        return uri?.also {
            contentResolver.openOutputStream(it)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            }
        }
    }

    companion object {
        private const val TAG = "ImageProcessor"
    }
}