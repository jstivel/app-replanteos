package com.example.replanteosapp.presenters

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.view.PreviewView
import com.example.replanteosapp.R
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.managers.CameraManager
import com.example.replanteosapp.managers.LocationTracker
import com.example.replanteosapp.managers.LocationUpdateCallback
import com.example.replanteosapp.managers.PermissionManager
import com.example.replanteosapp.managers.PermissionResultCallback
import com.example.replanteosapp.managers.PhotoWorkflowCallback
import com.example.replanteosapp.managers.PhotoWorkflowManager
import com.example.replanteosapp.services.GeocoderService
import com.example.replanteosapp.services.ImageProcessor
import com.example.replanteosapp.managers.TextOverlayManager
import com.example.replanteosapp.ui.MainActivity
import com.example.replanteosapp.ui.SettingsDialogFragment
import com.google.android.gms.common.api.ResolvableApiException

class MainPresenter(
    private var view: MainContract.View?,
    private val permissionManager: PermissionManager,
    private val cameraManager: CameraManager,
    private val geocoderService: GeocoderService,
    private val locationTracker: LocationTracker,
    private val imageProcessor: ImageProcessor,
    private val textOverlayManager: TextOverlayManager

) : MainContract.Presenter {

    private var lastKnownLocationData: LocationData? = null
    private var currentTextOverlayConfig: TextOverlayConfig = TextOverlayConfig()
    private lateinit var photoWorkflowManager: PhotoWorkflowManager
    private var previewView: PreviewView? = null

    // El ratio actual seleccionado por el usuario en la UI (4:3, 16:9, o FULL)
    private var currentDisplayRatio: Int = CameraRatios.RATIO_4_3

    // El ratio real que CameraX usará para la captura (siempre 4:3 o 16:9)
    private var currentCaptureRatio: Int = AspectRatio.RATIO_4_3 // Inicia con 4:3

    init {
        photoWorkflowManager = PhotoWorkflowManager(cameraManager, locationTracker, imageProcessor)
        photoWorkflowManager.setTextOverlayConfig(currentTextOverlayConfig)
        setupManagerCallbacks()
    }

    private fun setupManagerCallbacks() {
        locationTracker.setLocationUpdateCallback(object : LocationUpdateCallback {
            override fun onLocationReceived(locationData: LocationData) {
                lastKnownLocationData = locationData
                val requiredAccuracyForCapture = 100f
                if (locationData.accuracy != null && locationData.accuracy <= requiredAccuracyForCapture) {
                    view?.enableCaptureButton(true)
                    updateLocationDisplayText(locationData)
                } else {
                    view?.enableCaptureButton(false)
                    view?.showLocationText("Precisión: ±%.0fm. Mejorando...".format(locationData.accuracy ?: 0f))
                }
            }

            override fun onLocationError(errorMessage: String) {
                Log.e(TAG, "Error de ubicación en Presenter: $errorMessage")
                lastKnownLocationData = null
                view?.enableCaptureButton(false)
                view?.showLocationText("Error de ubicación: $errorMessage")
            }

            override fun onPermissionsDenied() {
                view?.showLocationText("Permisos de ubicación denegados.")
                view?.enableCaptureButton(false)
                view?.showPermissionsDeniedMessage()
            }

            override fun onLocationSettingsResolutionRequired(resolvableApiException: ResolvableApiException) {
                view?.showLocationSettingsDialog(resolvableApiException)
            }
        })

        photoWorkflowManager.setWorkflowCallback(object : PhotoWorkflowCallback {
            override fun onPhotoProcessed(imageUri: Uri, locationData: LocationData?) {
                Handler(Looper.getMainLooper()).post {
                    view?.showToast("Foto guardada con ubicación.")
                    view?.showThumbnail(imageUri)
                    updateLocationDisplayText(locationData)
                    Log.i(TAG, "Flujo de foto completado exitosamente.")
                }
            }

            override fun onPhotoProcessingError(errorMessage: String) {
                Handler(Looper.getMainLooper()).post {
                    view?.showToast("Error en el procesamiento de la foto: $errorMessage")
                    view?.hideThumbnail()
                    updateLocationDisplayText(lastKnownLocationData)
                    Log.e(TAG, "Error en el flujo de foto: $errorMessage")
                }
            }
            override fun onLocationUnavailableForPhoto(imageUri: Uri) {
                Handler(Looper.getMainLooper()).post {
                    view?.showToast("Foto guardada, pero sin ubicación en la imagen.")
                    view?.showThumbnail(imageUri)
                    updateLocationDisplayText(lastKnownLocationData)
                    Log.w(TAG, "Foto guardada, pero la ubicación no pudo ser dibujada.")
                }
            }
        })
    }

    override fun onViewCreated() {
        Log.d(TAG, "MainPresenter: onViewCreated")
        permissionManager.requestPermissions(object : PermissionResultCallback {
            override fun onPermissionsGranted() {
                this@MainPresenter.onPermissionsResult(true)
                // Al iniciar, establece el scaleType inicial (generalmente fitCenter)
                view?.setPreviewViewScaleType(PreviewView.ScaleType.FIT_CENTER)
            }
            override fun onPermissionsDenied(deniedPermissions: List<String>) {
                this@MainPresenter.onPermissionsResult(false)
            }
        })
        view?.updateSettingsButtonState(true)
    }

    override fun onResume() {
        if (permissionManager.allPermissionsGranted()) {
            locationTracker.startLocationUpdates(false)
            // Cuando se reanuda, reinicia la cámara con el último ratio de captura
            view?.startCameraPreview(currentCaptureRatio)
            // Y aplica el último scaleType de visualización
            previewView?.let {
                view?.setPreviewViewScaleType(getScaleTypeForDisplayRatio(currentDisplayRatio))
            }
        }
    }

    override fun onPause() {
        locationTracker.stopLocationUpdates()
        cameraManager.shutdown() // Asegúrate de apagar la cámara al pausar
    }

    override fun onDestroy() {
        view = null
        locationTracker.stopLocationUpdates()
        cameraManager.shutdown()
    }

    override fun onCaptureButtonClick() {
        if (lastKnownLocationData == null) {
            view?.showToast("Esperando ubicación para tomar foto.")
            return
        }

        view?.hideLocationText()
        view?.showFlashEffect() // Solo muestra, la vista lo ocultará con la animación

        photoWorkflowManager.setTextOverlayConfig(currentTextOverlayConfig)
        // Pasa el currentCaptureRatio para que ImageProcessor sepa qué AspectRatio se usó para la captura
        photoWorkflowManager.executePhotoCaptureWorkflow(lastKnownLocationData, currentCaptureRatio)
    }

    override fun onLocationSettingsResolutionResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "Usuario aceptó los cambios de configuración de ubicación.")
                locationTracker.startLocationUpdates(false)
            }
            Activity.RESULT_CANCELED -> {
                Log.w(TAG, "Usuario denegó los cambios de configuración de ubicación.")
                view?.showToast("La ubicación no está disponible.")
                view?.showLocationText("Ubicación no disponible.")
            }
        }
    }

    override fun onPermissionsResult(permissionsGranted: Boolean) {
        if (permissionsGranted) {
            view?.startCameraPreview(currentCaptureRatio) // Inicia con el ratio de captura por defecto
            locationTracker.startLocationUpdates(false)
        } else {
            view?.showPermissionsDeniedMessage()
            view?.enableCaptureButton(false)
        }
    }

    override fun startCamera(previewView: PreviewView, aspectRatio: Int) {
        this.previewView = previewView // Guarda la referencia a PreviewView
        cameraManager.startCamera(previewView, aspectRatio)
        // Al iniciar la cámara, también establecemos el scaleType
        view?.setPreviewViewScaleType(getScaleTypeForDisplayRatio(currentDisplayRatio))
    }

    override fun onRatioButtonClicked(aspectRatio: Int) {
        Log.d(TAG, "MainPresenter: Botón de ratio ${aspectRatio} presionado.")

        // Solo procede si el ratio seleccionado es diferente al actual
        if (aspectRatio != currentDisplayRatio) {
            currentDisplayRatio = aspectRatio // Actualiza el ratio seleccionado en la UI
            view?.setSelectedRatioButton(aspectRatio) // Actualiza el UI de los botones

            previewView?.let {
                when (currentDisplayRatio) {
                    CameraRatios.RATIO_4_3 -> {
                        currentCaptureRatio = AspectRatio.RATIO_4_3 // Define el ratio de captura
                        cameraManager.setAspectRatio(it, currentCaptureRatio) // Reinicia la cámara con el nuevo ratio de captura
                        view?.setPreviewViewScaleType(PreviewView.ScaleType.FIT_CENTER) // Muestra con fitCenter
                    }
                    CameraRatios.RATIO_16_9 -> {
                        currentCaptureRatio = AspectRatio.RATIO_16_9 // Define el ratio de captura
                        cameraManager.setAspectRatio(it, currentCaptureRatio) // Reinicia la cámara con el nuevo ratio de captura
                        view?.setPreviewViewScaleType(PreviewView.ScaleType.FIT_CENTER) // Muestra con fitCenter
                    }
                    CameraRatios.RATIO_FULL -> {
                        // Para FULL, no cambiamos el ratio de captura de la cámara (se mantiene el último 4:3 o 16:9).
                        // Solo cambiamos cómo se visualiza.
                        view?.setPreviewViewScaleType(PreviewView.ScaleType.FILL_CENTER) // Muestra con fillCenter (recorta)
                        // Opcional: Si quieres forzar un ratio de captura específico para el modo FULL (ej. 16:9), hazlo aquí:
                        // if (currentCaptureRatio != AspectRatio.RATIO_16_9) {
                        //    currentCaptureRatio = AspectRatio.RATIO_16_9
                        //    cameraManager.setAspectRatio(it, currentCaptureRatio)
                        // }
                    }
                    else -> {
                        // Por si acaso, si se selecciona un ratio desconocido, vuelve al default 4:3 y fitCenter
                        currentCaptureRatio = AspectRatio.RATIO_4_3
                        cameraManager.setAspectRatio(it, currentCaptureRatio)
                        view?.setPreviewViewScaleType(PreviewView.ScaleType.FIT_CENTER)
                    }
                }
            } ?: run {
                Log.e(TAG, "Error: PreviewView no disponible para cambiar el ratio/scaleType.")
                view?.showToast("Error al cambiar la relación de aspecto de la cámara.")
            }
        }
    }

    override fun onSettingsButtonClick() {
        view?.let { actualView ->
            if (actualView is MainActivity) {
                val settingsDialog = SettingsDialogFragment(currentTextOverlayConfig.copy())
                settingsDialog.setListener(object : SettingsDialogFragment.SettingsDialogListener {
                    override fun onSettingsSaved(newConfig: TextOverlayConfig) {
                        this@MainPresenter.onSettingsDialogClosed(newConfig)
                    }
                })
                settingsDialog.show(actualView.supportFragmentManager, SettingsDialogFragment.TAG)
            }
        }
    }

    override fun onPreviewViewLayoutReady() {
        // Este método ya no es estrictamente necesario, pero se mantiene si se necesita alguna lógica futura de layout.
    }


    override fun onSettingsDialogClosed(newConfig: TextOverlayConfig) {
        this.currentTextOverlayConfig = newConfig
        photoWorkflowManager.setTextOverlayConfig(newConfig)
        updateLocationDisplayText(lastKnownLocationData)
    }

    private fun updateLocationDisplayText(locationData: LocationData?) {
        locationData?.let {
            val locationText = "Lat: %.6f, Lon: %.6f".format(java.util.Locale.US, it.latitude, it.longitude)
            val accuracyText = "Precisión: ±%.0fm".format(it.accuracy ?: 0f)
            val dateTime = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(it.timestamp))
            val cityText = if (it.city.isNotEmpty()) "Ciudad: ${it.city}" else ""
            val addressText = if (it.address.isNotEmpty()) "Dirección: ${it.address}" else ""

            val displayText = StringBuilder()
            displayText.append(locationText)
            displayText.append("\n").append(accuracyText)
            displayText.append("\nFecha: ").append(dateTime.substringBefore(" "))
            displayText.append("\nHora: ").append(dateTime.substringAfter(" "))
            if (cityText.isNotEmpty()) displayText.append("\n").append(cityText)
            if (addressText.isNotEmpty()) displayText.append("\n").append(addressText)

            if (currentTextOverlayConfig.enableNote && currentTextOverlayConfig.noteText.isNotBlank()) {
                displayText.append("\nNota: ").append(currentTextOverlayConfig.noteText)
            }
            view?.showLocationText(displayText.toString())
        } ?: run {
            view?.showLocationText("Obteniendo ubicación...")
            view?.enableCaptureButton(false)
        }
    }

    private fun getScaleTypeForDisplayRatio(displayRatio: Int): PreviewView.ScaleType {
        return when (displayRatio) {
            CameraRatios.RATIO_FULL -> PreviewView.ScaleType.FILL_CENTER
            else -> PreviewView.ScaleType.FIT_CENTER
        }
    }


    companion object {
        private const val TAG = "MainPresenter"
        // Definimos nuestras propias constantes de relación de aspecto aquí
        object CameraRatios {
            const val RATIO_4_3 = AspectRatio.RATIO_4_3 // Coincide con AspectRatio.RATIO_4_3 (valor 0)
            const val RATIO_16_9 = AspectRatio.RATIO_16_9 // Coincide con AspectRatio.RATIO_16_9 (valor 1)
            const val RATIO_FULL = 3 // Nuevo valor único para el modo "Full"
        }
    }
}