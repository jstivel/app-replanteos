package com.example.replanteosapp.presenters

import android.app.Activity
import android.net.Uri
import android.util.Log
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.managers.CameraManager
import com.example.replanteosapp.managers.ImageCaptureCallback
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
import androidx.camera.view.PreviewView
import android.os.Handler
import android.os.Looper

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

    init {
        photoWorkflowManager = PhotoWorkflowManager(cameraManager, locationTracker, imageProcessor)
        photoWorkflowManager.setTextOverlayConfig(currentTextOverlayConfig)

        setupManagerCallbacks()
    }
    override fun startCamera(previewView: PreviewView) {
        // Le delegamos al CameraManager la tarea de iniciar la cámara con el PreviewView
        cameraManager.startCamera(previewView)
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
                Log.e(TAG, "Error de ubicación en Presenter: $errorMessage") // TAG ahora accesible aquí
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
                    Log.i(TAG, "Flujo de foto completado exitosamente.")
                }
            }

            override fun onPhotoProcessingError(errorMessage: String) {
                Handler(Looper.getMainLooper()).post {
                    view?.showToast("Error en el procesamiento de la foto: $errorMessage")
                    view?.hideThumbnail()
                    Log.e(TAG, "Error en el flujo de foto: $errorMessage")
                }
            }

            override fun onLocationUnavailableForPhoto(imageUri: Uri) {
                Handler(Looper.getMainLooper()).post {
                    view?.showToast("Foto guardada, pero sin ubicación (o error al dibujar texto).")
                    view?.showThumbnail(imageUri)
                    Log.w(TAG, "Flujo de foto completado, pero sin datos de ubicación o error al dibujar texto.")
                }
            }
        })
    }

    //region Métodos del ciclo de vida de la Vista (llamados por MainActivity)

    override fun onViewCreated() {
        permissionManager.requestPermissions(object : PermissionResultCallback {
            override fun onPermissionsGranted() {
                this@MainPresenter.onPermissionsResult(true)
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
        }
    }

    override fun onPause() {
        locationTracker.stopLocationUpdates()
    }

    override fun onDestroy() {
        view = null
        locationTracker.stopLocationUpdates()
        cameraManager.shutdown()
    }

    //endregion

    //region Métodos de eventos de UI (llamados por MainActivity)

    override fun onCaptureButtonClick() {
        if (lastKnownLocationData == null) {
            view?.showToast("Esperando ubicación para tomar foto.")
            return
        }
        view?.showFlashEffect()
        photoWorkflowManager.setTextOverlayConfig(currentTextOverlayConfig)
        photoWorkflowManager.executePhotoCaptureWorkflow(lastKnownLocationData)
    }

    override fun onLocationSettingsResolutionResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> {
                Log.d(TAG, "Usuario aceptó los cambios de configuración de ubicación.") // TAG ahora accesible aquí
                locationTracker.startLocationUpdates(false)
            }
            Activity.RESULT_CANCELED -> {
                Log.w(TAG, "Usuario denegó los cambios de configuración de ubicación.") // TAG ahora accesible aquí
                view?.showToast("La ubicación no está disponible.")
                view?.showLocationText("Ubicación no disponible.")
            }
        }
    }

    override fun onPermissionsResult(permissionsGranted: Boolean) {
        if (permissionsGranted) {
            view?.startCameraPreview()
            locationTracker.startLocationUpdates(false)
        } else {
            view?.showPermissionsDeniedMessage()
            view?.enableCaptureButton(false)
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

    override fun onSettingsDialogClosed(newConfig: TextOverlayConfig) {
        this.currentTextOverlayConfig = newConfig
        photoWorkflowManager.setTextOverlayConfig(newConfig)
        updateLocationDisplayText(lastKnownLocationData)
    }

    //endregion

    // Helper method para formatear y mostrar el texto de ubicación
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
        }
    }

    companion object {
        private const val TAG = "MainPresenter" // El TAG está correctamente aquí
    }
}