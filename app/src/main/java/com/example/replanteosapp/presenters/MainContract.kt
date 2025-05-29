// app/src/main/java/com/example/replanteosapp/presenters/MainContract.kt
package com.example.replanteosapp.presenters

import android.net.Uri
import androidx.camera.view.PreviewView
import com.example.replanteosapp.data.TextOverlayConfig

interface MainContract {

    // Define lo que la VISTA (MainActivity) debe poder hacer.
    // El Presenter llamará a estos métodos para actualizar la UI.
    interface View {
        fun showLocationText(text: String)
        fun enableCaptureButton(enable: Boolean)
        fun showThumbnail(imageUri: Uri?)
        fun hideThumbnail()
        fun showFlashEffect()
        fun showToast(message: String)
        fun showLocationSettingsDialog(resolvableApiException: com.google.android.gms.common.api.ResolvableApiException)
        fun showPermissionsDeniedMessage()
        fun updateSettingsButtonState(enabled: Boolean)

        fun hideFlashEffect()
        fun hideLocationText()
        fun startCameraPreview(aspectRatio: Int)
        fun setSelectedRatioButton(selectedRatio: Int)

    }

    // Define lo que el PRESENTER (MainPresenter) debe poder hacer.
    // La VISTA (MainActivity) llamará a estos métodos en respuesta a eventos de UI/ciclo de vida.
    interface Presenter {
        fun onViewCreated() // Se llama cuando la Activity se crea
        fun onResume() // Se llama cuando la Activity se reanuda
        fun onPause() // Se llama cuando la Activity se pausa
        fun onDestroy() // Se llama cuando la Activity se destruye

        fun onCaptureButtonClick() // Cuando el usuario pulsa el botón de captura
        fun onLocationSettingsResolutionResult(resultCode: Int) // Resultado del diálogo de ajustes de ubicación
        fun onPermissionsResult(permissionsGranted: Boolean) // Resultado de la solicitud de permisos
        fun onSettingsButtonClick() // Cuando el usuario pulsa el botón de ajustes
        fun onSettingsDialogClosed(newConfig: TextOverlayConfig) // Cuando el diálogo de ajustes devuelve una nueva configuración

        fun startCamera(previewView: PreviewView, aspectRatio: Int) // MODIFICADO: Pasa la relación de aspecto
        fun onRatioButtonClicked(aspectRatio: Int)
    }
}