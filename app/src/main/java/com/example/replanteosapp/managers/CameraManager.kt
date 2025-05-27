// app/src/main/java/com/example/replanteosapp/managers/CameraManager.kt
package com.example.replanteosapp.managers

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.camera.core.ImageProxy // <-- ¡Importación crucial!

class CameraManager(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private val TAG = "CameraManager"
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCaptureCallback: ImageCaptureCallback? = null

    fun setImageCaptureCallback(callback: ImageCaptureCallback) {
        this.imageCaptureCallback = callback
    }
    // No necesitas el 'display' lazy si solo lo usas para setTargetRotation una vez en takePhoto
    // val display: Display? by lazy { ... }

    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            // Aquí debes usar el display.rotation del previewView, no del WindowManager
            // ya que CameraX lo gestiona internamente para la Preview

            imageCapture = ImageCapture.Builder()
                // Asegúrate de que el ratio de captura coincida con tu PreviewView si quieres fidelidad total
                .setTargetAspectRatio(AspectRatio.RATIO_4_3) // O AspectRatio.RATIO_16_9, o AspectRatio.RATIO_16_9
                .setTargetRotation(previewView.display.rotation)
                .build()

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                Log.d(TAG, "Cámara iniciada correctamente.")
            } catch (exc: Exception) {
                Log.e(TAG, "Vinculación de caso de uso fallida", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    // MODIFICADO: takePhoto ahora recibe ImageCaptureCallback con un ImageProxy
    fun takePhoto(callback: ImageCaptureCallback) {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture no inicializado.")
            callback.onError("Error: cámara no lista para tomar foto.")
            return
        }

        // Usamos takePicture(executor, callback) para obtener un ImageProxy
        imageCapture.takePicture(
            cameraExecutor, // Ejecuta en el hilo del CameraExecutor
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error al tomar la foto: ${exc.message}", exc)
                    callback.onError(exc.message ?: "Error desconocido al tomar foto.")
                }

                override fun onCaptureSuccess(image: ImageProxy) {
                    Log.d(TAG, "Foto capturada como ImageProxy: ${image.format}, ${image.width}x${image.height}")
                    // Pasamos el ImageProxy al callback. ImageProcessor lo procesará y cerrará.
                    callback.onImageCaptured(image) // <-- Nuevo método para ImageCaptureCallback
                }
            }
        )
    }

    fun shutdown() {
        cameraExecutor.shutdown()
        Log.d(TAG, "Executor de cámara apagado.")
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}