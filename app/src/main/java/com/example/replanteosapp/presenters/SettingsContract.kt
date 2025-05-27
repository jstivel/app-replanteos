// app/src/main/java/com/example/replanteosapp/presenters/SettingsContract.kt
package com.example.replanteosapp.presenters

import com.example.replanteosapp.data.TextOverlayConfig

interface SettingsContract {

    // Define lo que la VISTA del diálogo (SettingsDialogFragment) debe poder hacer.
    // El Presenter llamará a estos métodos para actualizar la UI del diálogo.
    interface View {
        fun loadConfigToUI(config: TextOverlayConfig)
        fun updateTextSizeValue(value: String)
        fun updateTextAlphaValue(value: String)
        fun updateBackgroundAlphaValue(value: String)
        fun enableBackgroundControls(enable: Boolean)
        fun enableNoteControls(enable: Boolean)
        fun dismissDialog() // Para que la vista se cierre a sí misma
    }

    // Define lo que el PRESENTER del diálogo (SettingsPresenter) debe poder hacer.
    // La VISTA del diálogo (SettingsDialogFragment) llamará a estos métodos en respuesta a eventos de UI.
    interface Presenter {
        fun attachView(view: View) // Se llama cuando el diálogo se infla
        fun detachView() // Se llama cuando el diálogo se destruye

        fun onTextSizeChanged(progress: Int)
        fun onTextAlphaChanged(progress: Int)
        fun onBackgroundAlphaChanged(progress: Int)
        fun onEnableBackgroundChecked(isChecked: Boolean)
        fun onEnableNoteChecked(isChecked: Boolean)
        fun onNoteTextChanged(text: String)
        fun onTextColorSelected(color: Int) // Usaremos Color.WHITE o Color.BLACK
        fun onBackgroundColorSelected(color: Int?) // Usaremos Color.BLACK o null para transparente

        fun onSaveButtonClick()
        fun onCancelButtonClick()

        fun getCurrentConfig(): TextOverlayConfig // Para que la Vista pueda obtener la config inicial
    }
}