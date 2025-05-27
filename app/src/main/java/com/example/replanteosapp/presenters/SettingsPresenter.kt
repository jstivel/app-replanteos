// app/src/main/java/com/example/replanteosapp/presenters/SettingsPresenter.kt
package com.example.replanteosapp.presenters

import android.graphics.Color
import com.example.replanteosapp.data.TextOverlayConfig

class SettingsPresenter(
    private var view: SettingsContract.View?,
    initialConfig: TextOverlayConfig // La configuración que recibe al abrirse
) : SettingsContract.Presenter {

    // Mantener una copia temporal de la configuración que se está modificando en el diálogo
    private var tempConfig: TextOverlayConfig = initialConfig.copy()

    // El método attachView ya se llama en el constructor, no es necesario un attachView explícito aquí
    // pero se mantiene para la convención MVP si se necesita re-adjuntar la vista.
    override fun attachView(view: SettingsContract.View) {
        this.view = view
    }

    override fun detachView() {
        this.view = null // Prevenir fugas de memoria
    }

    override fun onTextSizeChanged(progress: Int) {
        tempConfig.textSize = 10f + (progress / 100f) * 20f // Rango de 10sp a 30sp
        view?.updateTextSizeValue("Tamaño: %.1fsp".format(tempConfig.textSize))
    }

    override fun onTextAlphaChanged(progress: Int) {
        tempConfig.textAlpha = progress / 100f
        view?.updateTextAlphaValue("Transparencia Texto: %d%%".format((tempConfig.textAlpha * 100).toInt()))
    }

    override fun onBackgroundAlphaChanged(progress: Int) {
        tempConfig.backgroundAlpha = progress / 100f
        view?.updateBackgroundAlphaValue("Transparencia Fondo: %d%%".format((tempConfig.backgroundAlpha * 100).toInt()))
    }

    override fun onEnableBackgroundChecked(isChecked: Boolean) {
        tempConfig.enableBackground = isChecked
        view?.enableBackgroundControls(isChecked) // Habilitar/deshabilitar controles de fondo en la Vista
    }

    override fun onEnableNoteChecked(isChecked: Boolean) {
        tempConfig.enableNote = isChecked
        view?.enableNoteControls(isChecked) // Habilitar/deshabilitar EditText de nota en la Vista
    }

    override fun onNoteTextChanged(text: String) {
        tempConfig.noteText = text
    }

    override fun onTextColorSelected(color: Int) {
        tempConfig.textColor = color
        // No hay un feedback visual directo para este cambio en el diálogo, pero el modelo se actualiza.
    }

    override fun onBackgroundColorSelected(color: Int?) {
        tempConfig.backgroundColor = color
        // Similar al color de texto, no hay feedback directo en el diálogo.
    }

    override fun onSaveButtonClick() {
        // La lógica de guardar y cerrar el diálogo es responsabilidad de la Vista
        view?.dismissDialog()
    }

    override fun onCancelButtonClick() {
        // La lógica de cancelar y cerrar el diálogo es responsabilidad de la Vista
        view?.dismissDialog() // Simplemente cerrar el diálogo
    }

    override fun getCurrentConfig(): TextOverlayConfig {
        return tempConfig.copy() // Devuelve una copia para que la Vista muestre el estado actual
    }

    companion object {
        private const val TAG = "SettingsPresenter"
    }
}