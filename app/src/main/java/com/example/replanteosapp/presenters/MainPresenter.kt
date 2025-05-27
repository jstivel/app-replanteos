// app/src/main/java/com/example/replanteosapp/presenters/MainPresenter.kt
package com.example.replanteosapp.presenters

import android.app.Activity
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.view.PreviewView
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
                    // ASEGURAMOS QUE EL TEXTO DE UBICACIÓN SE VUELVA A MOSTRAR AQUÍ
                    updateLocationDisplayText(locationData) // Usa la ubicación de la foto si disponible, sino la última conocida.
                    Log.i(TAG, "Flujo de foto completado exitosamente.")
                }
            }

            override fun onPhotoProcessingError(errorMessage: String) {
                Handler(Looper.getMainLooper()).post {
                    view?.showToast("Error en el procesamiento de la foto: $errorMessage")
                    view?.hideThumbnail()
                    // ASEGURAMOS QUE EL TEXTO DE UBICACIÓN SE VUELVA A MOSTRAR AQUÍ
                    updateLocationDisplayText(lastKnownLocationData) // Si hubo error, usa la última ubicación conocida
                    Log.e(TAG, "Error en el flujo de foto: $errorMessage")
                }
            }
            override fun onLocationUnavailableForPhoto(imageUri: Uri) {
                Handler(Looper.getMainLooper()).post {
                    view?.showToast("Foto guardada, pero sin ubicación en la imagen.")
                    view?.showThumbnail(imageUri)
                    updateLocationDisplayText(lastKnownLocationData) // Vuelve a mostrar la última ubicación conocida
                    Log.w(TAG, "Foto guardada, pero la ubicación no pudo ser dibujada.")
                }
            }

        })
    }

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

    override fun onCaptureButtonClick() {
        if (lastKnownLocationData == null) {
            view?.showToast("Esperando ubicación para tomar foto.")
            return
        }

        view?.hideLocationText() // Ocultar el texto de ubicación
        view?.showFlashEffect()

        // 3. Pasa la configuración actual al PhotoWorkflowManager
        photoWorkflowManager.setTextOverlayConfig(currentTextOverlayConfig)

        // 4. Inicia el flujo de captura de la foto
        photoWorkflowManager.executePhotoCaptureWorkflow(lastKnownLocationData)
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

    override fun startCamera(previewView: PreviewView) {
        cameraManager.startCamera(previewView)
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
            view?.showLocationText(displayText.toString()) // <-- ESTO DEBERÍA HACERLO VISIBLE
        } ?: run {
            // Si locationData es null, muestra un mensaje predeterminado y asegúrate de que sea visible
            view?.showLocationText("Obteniendo ubicación...")
            view?.enableCaptureButton(false) // Deshabilita el botón si no hay ubicación
        }
    }

    companion object {
        private const val TAG = "MainPresenter"
    }
}