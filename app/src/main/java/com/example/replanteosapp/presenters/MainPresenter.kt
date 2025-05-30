    // app/src/main/java/com/example/replanteosapp/presenters/MainPresenter.kt
    package com.example.replanteosapp.presenters

    import android.app.Activity
    import android.net.Uri
    import android.os.Handler
    import android.os.Looper
    import android.util.Log
    import androidx.camera.core.AspectRatio // Importación necesaria para AspectRatio
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
        private var currentCameraAspectRatio: Int = CameraRatios.RATIO_4_3

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

            view?.hideLocationText()
            view?.showFlashEffect() // Solo muestra, la vista lo ocultará con la animación

            photoWorkflowManager.setTextOverlayConfig(currentTextOverlayConfig)
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
                view?.startCameraPreview(currentCameraAspectRatio)
                locationTracker.startLocationUpdates(false)
            } else {
                view?.showPermissionsDeniedMessage()
                view?.enableCaptureButton(false)
            }
        }

        override fun startCamera(previewView: PreviewView, aspectRatio: Int) {
            this.previewView = previewView // Guarda la referencia a PreviewView
            cameraManager.startCamera(previewView, aspectRatio)
        }

        override fun onRatioButtonClicked(aspectRatio: Int) {
            if (aspectRatio != currentCameraAspectRatio) {
                currentCameraAspectRatio = aspectRatio
                view?.setSelectedRatioButton(aspectRatio)

                previewView?.let {
                    cameraManager.setAspectRatio(it, currentCameraAspectRatio)
                    // Ajusta el tamaño de la PreviewView inmediatamente después de cambiar el ratio

                } ?: run {
                    Log.e(TAG, "Error: PreviewView no disponible para cambiar el ratio.")
                    view?.showToast("Error al cambiar la relación de aspecto de la cámara.")
                }
            }
        }

        override fun onSettingsButtonClick() {
            view?.let { actualView ->
                if (actualView is MainActivity) { // Safe cast para acceder a supportFragmentManager
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
            // Asegúrate de que el tamaño de la PreviewView se ajuste cuando el layout esté listo

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

        companion object {
            private const val TAG = "MainPresenter"
            // Definimos nuestras propias constantes de relación de aspecto aquí
            object CameraRatios {
                const val RATIO_4_3 = 0 // Coincide con AspectRatio.RATIO_4_3
                const val RATIO_16_9 = 1 // Coincide con AspectRatio.RATIO_16_9

            }
        }
    }