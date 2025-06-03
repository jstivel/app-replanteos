package com.example.replanteosapp.ui

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

import com.example.replanteosapp.managers.CameraManager
import com.example.replanteosapp.managers.LocationTracker
import com.example.replanteosapp.managers.PermissionManager
import com.example.replanteosapp.services.GeocoderService
import com.example.replanteosapp.services.ImageProcessor
import com.example.replanteosapp.managers.TextOverlayManager

import com.example.replanteosapp.presenters.MainContract
import com.example.replanteosapp.presenters.MainPresenter
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.example.replanteosapp.R
import com.google.android.gms.common.api.ResolvableApiException
import androidx.camera.view.PreviewView // Importación necesaria para PreviewView.ScaleType


class MainActivity : FragmentActivity(), MainContract.View {

    private lateinit var viewFinder: PreviewView // Usar la importación directa
    private lateinit var cameraCaptureButton: Button
    private lateinit var imageViewThumbnail: ImageView
    private lateinit var viewFlashEffect: View
    private lateinit var textViewLocationDisplay: TextView
    private lateinit var settingsButton: Button
    private lateinit var ratioButton4_3: Button
    private lateinit var ratioButton16_9: Button
    private lateinit var ratioButtonFull: Button // Asegúrate de que el ID en XML sea ratioButtonFull

    private lateinit var presenter: MainContract.Presenter

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        presenter.onLocationSettingsResolutionResult(result.resultCode)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.viewFinder)
        cameraCaptureButton = findViewById(R.id.cameraCaptureButton)
        imageViewThumbnail = findViewById(R.id.imageViewThumbnail)
        viewFlashEffect = findViewById(R.id.viewFlashEffect)
        textViewLocationDisplay = findViewById(R.id.textViewLocationDisplay)
        settingsButton = findViewById(R.id.settingsButton)

        cameraCaptureButton.isEnabled = false
        textViewLocationDisplay.text = "Buscando ubicación..."

        ratioButton4_3 = findViewById(R.id.ratioButton4_3)
        ratioButton16_9 = findViewById(R.id.ratioButton16_9)
        ratioButtonFull = findViewById(R.id.ratioButtonFull) // Inicializar el botón "Full"

        presenter = MainPresenter(
            this,
            PermissionManager(this),
            CameraManager(this, this),
            GeocoderService(this),
            LocationTracker(this, GeocoderService(this)),
            ImageProcessor(contentResolver),
            TextOverlayManager()
        )

        cameraCaptureButton.setOnClickListener { presenter.onCaptureButtonClick() }
        settingsButton.setOnClickListener { presenter.onSettingsButtonClick() }

        ratioButton4_3.setOnClickListener { presenter.onRatioButtonClicked(CameraRatios.RATIO_4_3) }
        ratioButton16_9.setOnClickListener { presenter.onRatioButtonClicked(CameraRatios.RATIO_16_9) }
        ratioButtonFull.setOnClickListener { presenter.onRatioButtonClicked(CameraRatios.RATIO_FULL) } // Llama a RATIO_FULL

        presenter.onViewCreated()
    }

    override fun onResume() {
        super.onResume()
        presenter.onResume()
    }

    override fun onPause() {
        super.onPause()
        presenter.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.onDestroy()
    }

    //region Métodos de la interfaz MainContract.View

    override fun showLocationText(text: String) {
        textViewLocationDisplay.text = text
        textViewLocationDisplay.isVisible = true
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

    override fun showFlashEffect() {
        viewFlashEffect.apply {
            alpha = 0.9f
            isVisible = true
            animate().alpha(0f).setDuration(200L).withEndAction { isVisible = false }
        }
    }

    override fun hideFlashEffect() {
        viewFlashEffect.isVisible = false
        viewFlashEffect.alpha = 0f
    }

    override fun hideLocationText() {
        textViewLocationDisplay.isVisible = false
    }

    override fun setSelectedRatioButton(selectedRatio: Int) {
        ratioButton4_3.isSelected = false
        ratioButton16_9.isSelected = false
        ratioButtonFull.isSelected = false // Deselecciona el botón "Full"

        when (selectedRatio) {
            CameraRatios.RATIO_4_3 -> ratioButton4_3.isSelected = true
            CameraRatios.RATIO_16_9 -> ratioButton16_9.isSelected = true
            CameraRatios.RATIO_FULL -> ratioButtonFull.isSelected = true // Selecciona el botón "Full"
        }
    }

    override fun getPreviewView(): PreviewView {
        return viewFinder
    }

    // ¡NUEVA IMPLEMENTACIÓN DEL MÉTODO!
    override fun setPreviewViewScaleType(scaleType: PreviewView.ScaleType) {
        viewFinder.scaleType = scaleType
        Log.d(TAG, "PreviewView scaleType cambiado a: $scaleType")
    }

    //endregion

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CHECK_SETTINGS = 1001
    }
}