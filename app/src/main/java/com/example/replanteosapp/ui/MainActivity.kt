// MainActivity.kt
package com.example.replanteosapp.ui // Mueve MainActivity al paquete 'ui'


import android.content.IntentSender
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
import com.example.replanteosapp.presenters.MainPresenter.Companion.CameraRatios

// Importaciones de tus managers (Modelos)
import com.example.replanteosapp.managers.CameraManager
import com.example.replanteosapp.managers.LocationTracker
import com.example.replanteosapp.managers.PermissionManager
import com.example.replanteosapp.services.GeocoderService
import com.example.replanteosapp.services.ImageProcessor
import com.example.replanteosapp.managers.TextOverlayManager

// Importaciones de los nuevos Presenters y Contratos
import com.example.replanteosapp.presenters.MainContract
import com.example.replanteosapp.presenters.MainPresenter
import androidx.activity.result.IntentSenderRequest // Nueva importación
import androidx.activity.result.contract.ActivityResultContracts // Nueva importació
import com.example.replanteosapp.R
import com.google.android.gms.common.api.ResolvableApiException

import android.view.ViewGroup
import android.view.ViewTreeObserver


class MainActivity : FragmentActivity(), MainContract.View { // <--- ¡Implementa la interfaz de la Vista!

    // Referencias a Vistas UI
    private lateinit var viewFinder: androidx.camera.view.PreviewView
    private lateinit var cameraCaptureButton: Button
    private lateinit var imageViewThumbnail: ImageView
    private lateinit var viewFlashEffect: View
    private lateinit var textViewLocationDisplay: TextView
    private lateinit var settingsButton: Button
    private lateinit var ratioButton4_3: Button
    private lateinit var ratioButton16_9: Button

    // Referencia al Presenter
    private lateinit var presenter: MainContract.Presenter

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult() // Contrato para IntentSender
    ) { result ->
        // Este es el callback que se ejecuta cuando se recibe un resultado
        // Aquí pasamos el resultado al Presenter
        presenter.onLocationSettingsResolutionResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Inicialización de Vistas
        viewFinder = findViewById(R.id.viewFinder)
        cameraCaptureButton = findViewById(R.id.cameraCaptureButton)
        imageViewThumbnail = findViewById(R.id.imageViewThumbnail)
        viewFlashEffect = findViewById(R.id.viewFlashEffect)
        textViewLocationDisplay = findViewById(R.id.textViewLocationDisplay)
        settingsButton = findViewById(R.id.settingsButton)

        // Deshabilitar botón de captura al inicio (la lógica de habilitación/deshabilitación será del Presenter)
        cameraCaptureButton.isEnabled = false
        textViewLocationDisplay.text = "Buscando ubicación..."

        // Inicializar botones de ratio
        ratioButton4_3 = findViewById(R.id.ratioButton4_3)
        ratioButton16_9 = findViewById(R.id.ratioButton16_9)

        // 2. Inicialización del Presenter
        // PASAMOS LAS DEPENDENCIAS (MANAGERS) AL PRESENTER.
        // Aquí es donde un futuro sistema de Inyección de Dependencias (DI) brillaría.
        presenter = MainPresenter(
            this, // La Activity es la View que se pasa al Presenter
            PermissionManager(this),
            CameraManager(this, this),
            GeocoderService(this),
            LocationTracker(this, GeocoderService(this)), // Pasa GeocoderService al LocationTracker
            ImageProcessor(contentResolver),
            TextOverlayManager()
        )

        // 3. Configuración de Listeners (DELEGANDO AL PRESENTER)
        cameraCaptureButton.setOnClickListener { presenter.onCaptureButtonClick() }
        settingsButton.setOnClickListener { presenter.onSettingsButtonClick() }

        ratioButton4_3.setOnClickListener { presenter.onRatioButtonClicked(CameraRatios.RATIO_4_3) }
        ratioButton16_9.setOnClickListener { presenter.onRatioButtonClicked(CameraRatios.RATIO_16_9) }


        presenter.onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        // Notificar al Presenter que la Vista se reanuda
        presenter.onResume()

    }

    override fun onPause() {
        super.onPause()
        // Notificar al Presenter que la Vista se pausa
        presenter.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Notificar al Presenter que la Vista se destruye
        presenter.onDestroy()
    }



    //region Métodos de la interfaz MainContract.View (implementaciones que el Presenter llama)


    override fun showLocationText(text: String) {
        textViewLocationDisplay.text = text
        textViewLocationDisplay.isVisible = true // <-- Asegúrate de que esto esté aquí
    }
    override fun enableCaptureButton(enable: Boolean) {
        cameraCaptureButton.isEnabled = enable
    }

    override fun showThumbnail(imageUri: Uri?) {
        imageViewThumbnail.setImageURI(imageUri)
        imageViewThumbnail.isVisible = (imageUri != null)
    }

    override fun hideThumbnail() {
        imageViewThumbnail.isVisible = false
    }


    override fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLocationSettingsDialog(resolvableApiException: ResolvableApiException) {
        try {
            // Usamos el nuevo lanzador para iniciar la resolución
            locationSettingsLauncher.launch(
                IntentSenderRequest.Builder(resolvableApiException.resolution).build()
            )
        } catch (e: IntentSender.SendIntentException) {
            Log.e(TAG, "Error al lanzar IntentSenderRequest para ajustes de ubicación: ${e.message}", e)
            showToast("No se pudo solicitar la activación de ubicación.")
        }
    }

    override fun startCameraPreview(aspectRatio: Int) {
        presenter.startCamera(viewFinder, aspectRatio)
    }

    override fun showPermissionsDeniedMessage() {
        showToast("Permisos necesarios denegados.")
        cameraCaptureButton.isEnabled = false
    }

    override fun updateSettingsButtonState(enabled: Boolean) {
        settingsButton.isEnabled = enabled
    }

    //endregion
    override fun showFlashEffect() {
        viewFlashEffect.apply {
            alpha = 0.9f
            isVisible = true
            animate().alpha(0f).setDuration(200L).withEndAction { isVisible = false }
        }
    }

    override fun hideFlashEffect() { // <-- ¡Nuevo método!
        viewFlashEffect.isVisible = false
        viewFlashEffect.alpha = 0f // Asegurarse de que esté completamente transparente
    }

    override fun hideLocationText() { // <-- ¡Nuevo método!
        textViewLocationDisplay.isVisible = false
    }

    override fun setSelectedRatioButton(selectedRatio: Int) {
        // Deselecciona todos los botones primero
        ratioButton4_3.isSelected = false
        ratioButton16_9.isSelected = false

        // Selecciona el botón correcto
        when (selectedRatio) {
            CameraRatios.RATIO_4_3 -> ratioButton4_3.isSelected = true
            CameraRatios.RATIO_16_9 -> ratioButton16_9.isSelected = true
            // Puedes añadir más casos si tienes otros ratios
        }
    }


    // Puedes dejar esta constante aquí o moverla a un Companion object del Presenter si es más lógica de Presenter.
    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CHECK_SETTINGS = 1001
    }
}