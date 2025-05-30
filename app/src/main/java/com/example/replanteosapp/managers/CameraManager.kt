// app/src/main/java/com/example/replanteosapp/managers/CameraManager.kt
package com.example.replanteosapp.managers

import android.content.Context
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
import androidx.camera.core.ImageProxy
import com.example.replanteosapp.presenters.MainPresenter.Companion.CameraRatios

class CameraManager(private val context: Context, private val lifecycleOwner: LifecycleOwner) {


    private var imageCaptureCallback: ImageCaptureCallback? = null
    private var currentAspectRatio: Int = AspectRatio.RATIO_4_3 // Valor predeterminado


    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    private var currentCameraXAspectRatio: Int = AspectRatio.RATIO_4_3 // El AspectRatio real de CameraX



    fun setImageCaptureCallback(callback: ImageCaptureCallback) {
        this.imageCaptureCallback = callback
    }
    fun startCamera(previewView: PreviewView, aspectRatio: Int = AspectRatio.RATIO_4_3) {
        this.currentAspectRatio = aspectRatio // Guarda la relación de aspecto
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)


        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            // Mapea nuestros ratios a los de CameraX para la vista previa y captura
            val cameraXAspectRatio = when (currentAspectRatio) {
                CameraRatios.RATIO_4_3 -> AspectRatio.RATIO_4_3
                CameraRatios.RATIO_16_9 -> AspectRatio.RATIO_16_9
                else -> AspectRatio.RATIO_4_3 // Valor por defecto seguro
            }
            this.currentCameraXAspectRatio = cameraXAspectRatio
            preview = Preview.Builder()
                .setTargetAspectRatio(currentCameraXAspectRatio) // Aplica la relación de aspecto de CameraX
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetAspectRatio(currentCameraXAspectRatio)
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
                Log.d(TAG, "Cámara iniciada correctamente con ratio (CameraX): $cameraXAspectRatio")
                // Ajusta el layout de PreviewView para reflejar 1:1 si es necesario visualmente
                // Esto es más un ajuste de la UI en MainActivity, no del CameraManager
            } catch (exc: Exception) {
                Log.e(TAG, "Vinculación de caso de uso fallida", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setAspectRatio(previewView: PreviewView, newAspectRatio: Int) {
        if (newAspectRatio != currentAspectRatio) {
            currentAspectRatio = newAspectRatio
            cameraProvider?.unbindAll()
            startCamera(previewView, currentAspectRatio) // Llama a startCamera con el nuevo ratio
            Log.d(TAG, "Relación de aspecto cambiada a: $newAspectRatio")
        }
    }

    fun getDesiredOutputAspectRatio(): Int {
        return currentAspectRatio
    }
    fun getCameraXAspectRatioFloat(): Float {
        return when (currentCameraXAspectRatio) {
            AspectRatio.RATIO_4_3 -> 4f / 3f
            AspectRatio.RATIO_16_9 -> 16f / 9f
            else -> 4f / 3f // Por defecto
        }
    }
    // MODIFICADO: takePhoto ahora recibe ImageCaptureCallback con un ImageProxy
    fun takePhoto(callback: ImageCaptureCallback) {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture no inicializado.")
            callback.onError("Error: cámara no lista para tomar foto.")
            return
        }

        // Usamos takePicture(executor, callback) para obtener un ImageProxy
        imageCapture?.takePicture(
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    callback.onImageCaptured(image)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Error al tomar foto: ${exception.message}", exception)
                    callback.onError("Error al tomar foto: ${exception.message}")
                }
            }
        )
    }

    fun shutdown() {
        cameraProvider?.unbindAll()
        Log.d(TAG, "CameraManager cerrado.")
    }

    companion object {
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val TAG = "CameraManager"
    }
}