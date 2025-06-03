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
import com.example.replanteosapp.presenters.MainPresenter.Companion.CameraRatios // Necesario para tus constantes

class CameraManager(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private var imageCaptureCallback: ImageCaptureCallback? = null
    // currentAspectRatio no es el de CameraX, es el que le pasamos desde el Presenter.
    // Lo renombramos para mayor claridad.
    private var requestedCameraDisplayRatio: Int = AspectRatio.RATIO_4_3

    private var cameraProvider: ProcessCameraProvider? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Este sí es el AspectRatio real que CameraX está usando.
    // Lo haremos private set para que solo se actualice internamente.
    var currentCameraXAspectRatio: Int = AspectRatio.RATIO_4_3
        private set

    fun setImageCaptureCallback(callback: ImageCaptureCallback) {
        this.imageCaptureCallback = callback
    }

    fun startCamera(previewView: PreviewView, aspectRatio: Int = AspectRatio.RATIO_4_3) {
        // Al iniciar, toma el AspectRatio real que CameraX usará para la captura.
        // Ignora RATIO_FULL, ya que eso es solo para el display.
        val cameraXTargetAspectRatio = when (aspectRatio) {
            CameraRatios.RATIO_4_3 -> AspectRatio.RATIO_4_3
            CameraRatios.RATIO_16_9 -> AspectRatio.RATIO_16_9
            // Si llega RATIO_FULL o RATIO_DEFAULT, o cualquier otra cosa,
            // por defecto usa RATIO_4_3 para la captura.
            // El modo de visualización "FULL" se maneja en el Presenter/Activity cambiando el scaleType.
            else -> AspectRatio.RATIO_4_3
        }
        this.currentCameraXAspectRatio = cameraXTargetAspectRatio // Actualiza el ratio real de CameraX

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

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
                Log.d(TAG, "Cámara iniciada correctamente con ratio (CameraX): $currentCameraXAspectRatio")
            } catch (exc: Exception) {
                Log.e(TAG, "Vinculación de caso de uso fallida", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun setAspectRatio(previewView: PreviewView, newAspectRatio: Int) {
        // El newAspectRatio aquí debe ser un AspectRatio.RATIO_4_3 o AspectRatio.RATIO_16_9.
        // Si el Presenter envía RATIO_FULL, no se usa aquí para cambiar la cámara,
        // solo para cambiar el scaleType en la vista.
        if (newAspectRatio != currentCameraXAspectRatio) { // Compara con el ratio real de CameraX
            currentCameraXAspectRatio = newAspectRatio // Actualiza el ratio real de CameraX
            cameraProvider?.unbindAll()
            startCamera(previewView, currentCameraXAspectRatio) // Reinicia la cámara con el nuevo ratio de captura
            Log.d(TAG, "Relación de aspecto de CÁMARA cambiada a: $newAspectRatio")
        }
    }

    // Este método ya no es útil tal cual, ya que el ratio de salida es siempre el currentCameraXAspectRatio
    // fun getDesiredOutputAspectRatio(): Int {
    //     return currentAspectRatio // Esto ya no es preciso.
    // }

    // No es necesario un getter para el AspectRatio en sí, ya que lo pasas directamente en takePhoto.
    // Pero si lo necesitas, podrías retornar currentCameraXAspectRatio
    fun getCameraXCaptureAspectRatio(): Int {
        return currentCameraXAspectRatio
    }

    fun takePhoto(callback: ImageCaptureCallback) {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture no inicializado.")
            callback.onError("Error: cámara no lista para tomar foto.")
            return
        }

        imageCapture.takePicture( // Cambiado a 'imageCapture' sin '?'
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