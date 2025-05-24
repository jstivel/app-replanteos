// MainActivity.kt
package com.example.replanteosapp

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity

// IMPORTACIONES NECESARIAS
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.managers.CameraManager
import com.example.replanteosapp.managers.ImageCaptureCallback
import com.example.replanteosapp.managers.LocationTracker
import com.example.replanteosapp.managers.LocationUpdateCallback
import com.example.replanteosapp.managers.PermissionManager
import com.example.replanteosapp.managers.PermissionResultCallback
import com.example.replanteosapp.services.GeocoderService
import com.example.replanteosapp.services.ImageProcessor
import com.example.replanteosapp.managers.PhotoWorkflowCallback
import com.example.replanteosapp.managers.PhotoWorkflowManager
import com.google.android.gms.common.api.ResolvableApiException

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import com.example.replanteosapp.managers.TextOverlayManager
import com.example.replanteosapp.ui.SettingsDialogFragment // ¡Asegúrate de que esta importación esté presente y sea correcta!


class MainActivity : FragmentActivity() {

    private lateinit var viewFinder: androidx.camera.view.PreviewView
    private lateinit var cameraCaptureButton: Button
    private lateinit var imageViewThumbnail: ImageView
    private lateinit var viewFlashEffect: View
    private lateinit var textViewLocationDisplay: TextView

    private lateinit var permissionManager: PermissionManager
    private lateinit var cameraManager: CameraManager
    private lateinit var geocoderService: GeocoderService
    private lateinit var locationTracker: LocationTracker
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var photoWorkflowManager: PhotoWorkflowManager

    private var lastKnownLocationData: LocationData? = null

    private lateinit var settingsButton: Button
    private lateinit var textOverlayManager: TextOverlayManager

    private var textOverlayConfig: TextOverlayConfig = TextOverlayConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inicialización de Vistas
        viewFinder = findViewById(R.id.viewFinder)
        cameraCaptureButton = findViewById(R.id.cameraCaptureButton)
        imageViewThumbnail = findViewById(R.id.imageViewThumbnail)
        viewFlashEffect = findViewById(R.id.viewFlashEffect)
        textViewLocationDisplay = findViewById(R.id.textViewLocationDisplay)
        // ELIMINADA: updateLocationButton = findViewById(R.id.updateLocationButton)
        settingsButton = findViewById(R.id.settingsButton)


        // Deshabilitar botón de captura al inicio
        cameraCaptureButton.isEnabled = false
        // ELIMINADA: updateLocationButton.isEnabled = false
        textViewLocationDisplay.text = "Buscando ubicación..."

        // 2. Inicialización de Componentes de Lógica
        permissionManager = PermissionManager(this)
        cameraManager = CameraManager(this, this)
        geocoderService = GeocoderService(this)
        locationTracker = LocationTracker(this, geocoderService)
        imageProcessor = ImageProcessor(contentResolver)
        photoWorkflowManager = PhotoWorkflowManager(cameraManager, locationTracker, imageProcessor)
        // Pasa la configuración inicial al PhotoWorkflowManager
        photoWorkflowManager.setTextOverlayConfig(textOverlayConfig)

        textOverlayManager = TextOverlayManager()
        applyTextOverlayConfig(textOverlayConfig) // Aplica la configuración inicial al TextView


        // 3. Configuración de Callbacks para LocationTracker
        locationTracker.setLocationUpdateCallback(object : LocationUpdateCallback {
            override fun onLocationReceived(locationData: LocationData) {
                lastKnownLocationData = locationData
                updateLocationDisplayTextView(locationData)

                val requiredAccuracyForCapture = 100f // Precisión aceptable para tomar fotos (ej. 100m)

                // Habilitar/Deshabilitar el botón de captura basado en la precisión actual
                if (locationData.accuracy != null && locationData.accuracy <= requiredAccuracyForCapture) {
                    cameraCaptureButton.isEnabled = true
                } else {
                    cameraCaptureButton.isEnabled = false
                    textViewLocationDisplay.text = "Precisión: ±%.0fm. Mejorando...".format(locationData.accuracy ?: 0f)
                }
            }

            override fun onLocationError(errorMessage: String) {
                Log.e(TAG, "Error de ubicación en UI: $errorMessage")
                lastKnownLocationData = null
                cameraCaptureButton.isEnabled = false
                // ELIMINADA: updateLocationButton.isEnabled = false
                // ELIMINADA: isUpdatingLocation = false
                textViewLocationDisplay.text = "Error de ubicación: $errorMessage"
                // Toast.makeText(baseContext, "Error de ubicación: $errorMessage", Toast.LENGTH_LONG).show()
            }

            override fun onPermissionsDenied() {
                textViewLocationDisplay.text = "Permisos de ubicación denegados."
                lastKnownLocationData = null
                cameraCaptureButton.isEnabled = false
                // ELIMINADA: updateLocationButton.isEnabled = false
                // ELIMINADA: isUpdatingLocation = false
                Toast.makeText(baseContext, "Permisos necesarios denegados.", Toast.LENGTH_LONG).show()
            }
            override fun onLocationSettingsResolutionRequired(resolvableApiException: ResolvableApiException) {
                try {
                    startIntentSenderForResult(
                        resolvableApiException.resolution.intentSender,
                        REQUEST_CHECK_SETTINGS,
                        null,
                        0,
                        0,
                        0
                    )
                } catch (e: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error al iniciar el diálogo de resolución de ubicación: ${e.message}", e)
                    Toast.makeText(baseContext, "No se pudo solicitar la activación de ubicación.", Toast.LENGTH_LONG).show()
                }
            }
        })

        // 4. Configuración de Callbacks para PhotoWorkflowManager
        photoWorkflowManager.setWorkflowCallback(object : PhotoWorkflowCallback {
            override fun onPhotoProcessed(imageUri: Uri, locationData: LocationData?) {
                Toast.makeText(baseContext, "Foto guardada con ubicación.", Toast.LENGTH_SHORT).show()
                imageViewThumbnail.setImageURI(imageUri)
                imageViewThumbnail.isVisible = true
                Log.i(TAG, "Flujo de foto completado exitosamente.")
            }

            override fun onPhotoProcessingError(errorMessage: String) {
                Toast.makeText(baseContext, "Error en el procesamiento de la foto: $errorMessage", Toast.LENGTH_LONG).show()
                Log.e(TAG, "Error en el flujo de foto: $errorMessage")
                imageViewThumbnail.isVisible = false
            }

            override fun onLocationUnavailableForPhoto(imageUri: Uri) {
                Toast.makeText(baseContext, "Foto guardada, pero sin ubicación (o error al dibujar texto).", Toast.LENGTH_LONG).show()
                imageViewThumbnail.setImageURI(imageUri)
                imageViewThumbnail.isVisible = true
                Log.w(TAG, "Flujo de foto completado, pero sin datos de ubicación o error al dibujar texto.")
            }
        })

        // 5. Solicitud de Permisos
        permissionManager.requestPermissions(object : PermissionResultCallback {
            override fun onPermissionsGranted() {
                startCamera()
                // Iniciar actualizaciones de ubicación continuas y normales
                locationTracker.startLocationUpdates(false)
            }

            override fun onPermissionsDenied(deniedPermissions: List<String>) {
                Log.e(TAG, "Permisos denegados: $deniedPermissions")
                Toast.makeText(baseContext, "Permisos necesarios denegados.", Toast.LENGTH_LONG).show()
                cameraCaptureButton.isEnabled = false
                // ELIMINADA: updateLocationButton.isEnabled = false
            }
        })

        // 6. Listeners de botones
        cameraCaptureButton.setOnClickListener { executePhotoWorkflow() }
        // ELIMINADA: updateLocationButton.setOnClickListener { startLocationRefresh() }
        settingsButton.setOnClickListener {
            val dialog = SettingsDialogFragment(textOverlayConfig)
            dialog.show(supportFragmentManager, SettingsDialogFragment.TAG)
        }
    }

    private fun applyTextOverlayConfig(config: TextOverlayConfig) {
        textViewLocationDisplay.textSize = config.textSize
        textViewLocationDisplay.setTextColor(config.textColor)

        if (config.enableBackground && config.backgroundColor != null) {
            val alpha = (config.backgroundAlpha * 255).toInt()
            val colorWithAlpha = (alpha shl 24) or (config.backgroundColor!! and 0x00FFFFFF)
            textViewLocationDisplay.setBackgroundColor(colorWithAlpha)
        } else {
            textViewLocationDisplay.setBackgroundColor(Color.TRANSPARENT)
        }
        textViewLocationDisplay.alpha = config.textAlpha
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
        locationTracker.stopLocationUpdates() // Asegurarse de detener las actualizaciones al destruir la actividad
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        // Reiniciar las actualizaciones de ubicación solo si los permisos están concedidos
        if (permissionManager.allPermissionsGranted()) {
            locationTracker.startLocationUpdates(false) // Siempre inicia normales y continuas
        }
    }

    override fun onPause() {
        super.onPause()
        locationTracker.stopLocationUpdates() // Detener actualizaciones cuando la app no está en primer plano
    }

    private fun startCamera() {
        cameraManager.startCamera(viewFinder)
    }

    private fun executePhotoWorkflow() {
        if (lastKnownLocationData == null) {
            Toast.makeText(this, "Esperando ubicación para tomar foto.", Toast.LENGTH_SHORT).show()
            return
        }

        viewFlashEffect.apply {
            alpha = 0.9f
            isVisible = true
            animate()
                .alpha(0f)
                .setDuration(200L)
                .withEndAction { isVisible = false }
        }
        photoWorkflowManager.executePhotoCaptureWorkflow(lastKnownLocationData)
    }

    @Deprecated("Deprecated in API 29")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    Log.d(TAG, "Usuario aceptó los cambios de configuración de ubicación.")
                    locationTracker.startLocationUpdates(false) // Reintentar con la solicitud normal y continua
                }
                Activity.RESULT_CANCELED -> {
                    Log.w(TAG, "Usuario denegó los cambios de configuración de ubicación.")
                    Toast.makeText(this, "La ubicación no está disponible.", Toast.LENGTH_LONG).show()
                    // ELIMINADA: updateLocationButton.isEnabled = true
                    // ELIMINADA: isUpdatingLocation = false
                    textViewLocationDisplay.text = "Ubicación no disponible."
                }
            }
        }
    }

    // ELIMINADO TODO ESTE MÉTODO:
    // private fun startLocationRefresh() {
    //     cameraCaptureButton.isEnabled = false
    //     updateLocationButton.isEnabled = false
    //     textViewLocationDisplay.text = "Buscando ubicación de alta precisión..."
    //     isUpdatingLocation = true
    //     locationTracker.stopLocationUpdates()
    //     locationTracker.startLocationUpdates(true)
    // }


    private fun updateLocationDisplayTextView(locationData: LocationData?) {
        locationData?.let {
            val locationText = "Lat: %.6f, Lon: %.6f".format(Locale.US, it.latitude, it.longitude)
            val accuracyText = "Precisión: ±%.0fm".format(it.accuracy ?: 0f)
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp))
            val cityText = if (it.city.isNotEmpty()) "Ciudad: ${it.city}" else ""
            val addressText = if (it.address.isNotEmpty()) "Dirección: ${it.address}" else ""

            val displayText = StringBuilder()
            displayText.append(locationText)
            displayText.append("\n").append(accuracyText)
            displayText.append("\nFecha: ").append(dateTime.substringBefore(" "))
            displayText.append("\nHora: ").append(dateTime.substringAfter(" "))
            if (cityText.isNotEmpty()) displayText.append("\n").append(cityText)
            if (addressText.isNotEmpty()) displayText.append("\n").append(addressText)

            if (textOverlayConfig.enableNote && textOverlayConfig.noteText.isNotBlank()) {
                displayText.append("\nNota: ").append(textOverlayConfig.noteText)
            }
            textViewLocationDisplay.text = displayText.toString()
        } ?: run {
            textViewLocationDisplay.text = "Obteniendo ubicación..."
        }
    }

    // Nuevo método para actualizar la configuración del texto y aplicarla
    fun updateTextOverlayConfig(newConfig: TextOverlayConfig) {
        this.textOverlayConfig = newConfig // Actualiza la instancia
        applyTextOverlayConfig(newConfig) // Aplica a la UI
        photoWorkflowManager.setTextOverlayConfig(newConfig) // Pasa al manager para la próxima foto
    }


    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CHECK_SETTINGS = 1001
    }
}