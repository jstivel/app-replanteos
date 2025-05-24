// CameraManager.kt
package com.example.replanteosapp.managers
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Callback para el resultado de la captura de imagen
interface ImageCaptureCallback {
    fun onImageCaptured(uri: Uri)
    fun onError(errorMessage: String)
}

class CameraManager(private val context: Context, private val lifecycleOwner: LifecycleOwner) {

    private val TAG = "CameraManager"
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val display: Display? by lazy {
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay
    }

    fun startCamera(previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .setTargetRotation(display?.rotation ?: Surface.ROTATION_0) // Establecer la rotación de la captura
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

    fun takePhoto(callback: ImageCaptureCallback) {
        val imageCapture = imageCapture ?: run {
            Log.e(TAG, "ImageCapture no inicializado.")
            callback.onError("Error: cámara no lista para tomar foto.")
            return
        }

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ReplanteosApp")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Error al tomar la foto: ${exc.message}", exc)
                    callback.onError(exc.message ?: "Error desconocido al tomar foto.")
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = output.savedUri
                    if (savedUri != null) {
                        Log.d(TAG, "Foto guardada en: $savedUri")
                        callback.onImageCaptured(savedUri)
                    } else {
                        val errorMessage = "Error: URI de imagen guardada es nula."
                        Log.e(TAG, errorMessage)
                        callback.onError(errorMessage)
                    }
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