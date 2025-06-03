package com.example.replanteosapp.services

import android.content.ContentResolver
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.RectF
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.ImageProxy
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.managers.TextOverlayManager
import java.io.IOException
import java.io.OutputStream
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.Locale
// Ya no necesitas importar CameraRatios si no se usa para lógica de recorte aquí
// import com.example.replanteosapp.presenters.MainPresenter.Companion.CameraRatios

class ImageProcessor(private val contentResolver: ContentResolver) {

    private val textOverlayManager = TextOverlayManager()

    fun processAndSaveImage(
        imageProxy: ImageProxy,
        locationData: LocationData?,
        textOverlayConfig: TextOverlayConfig,
        // Este parámetro 'desiredOutputAspectRatio' ya no se usa para recorte en ImageProcessor
        // Podrías eliminarlo si no lo pasas desde PhotoWorkflowManager
        desiredOutputAspectRatio: Int // Se mantiene por compatibilidad si PhotoWorkflowManager lo sigue enviando.
    ): Uri? {
        var outputStream: OutputStream? = null
        try {
            val originalBitmap = imageProxyToBitmap(imageProxy)
                ?: throw IOException("No se pudo convertir ImageProxy a Bitmap.")

            val rotatedBitmap = rotateBitmap(originalBitmap, imageProxy.imageInfo.rotationDegrees)

            // NO HAY LÓGICA DE RECORTE AQUÍ. El bitmap final es el rotado.
            val finalBitmap = rotatedBitmap

            val bitmapWithOverlay = textOverlayManager.drawTextOverlay(finalBitmap, locationData, textOverlayConfig)

            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "IMG_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
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
            imageProxy.close()
            outputStream?.close()
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap? {
        val planeProxy = image.planes[0]
        val buffer: ByteBuffer = planeProxy.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }

    private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
        if (rotationDegrees == 0) return bitmap

        val matrix = Matrix()
        matrix.postRotate(rotationDegrees.toFloat())

        val rotatedRect = RectF(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
        matrix.mapRect(rotatedRect)

        // No es estrictamente necesario newWidth y newHeight si solo usas createBitmap con matrix
        // val newWidth = rotatedRect.width().toInt()
        // val newHeight = rotatedRect.height().toInt()

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    // Este método saveBitmapToTempFile y cropBitmapToSquare (si existiera) ya no se usan ni son necesarios
    // private fun saveBitmapToTempFile(bitmap: Bitmap, prefix: String): Uri? { /* ... */ }

    companion object {
        private const val TAG = "ImageProcessor"
    }
}