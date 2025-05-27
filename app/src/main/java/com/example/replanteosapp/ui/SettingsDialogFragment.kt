// app/src/main/java/com/example/replanteosapp/ui/SettingsDialogFragment.kt
package com.example.replanteosapp.ui

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import com.example.replanteosapp.R // Importar la clase R
import com.example.replanteosapp.data.TextOverlayConfig
import com.example.replanteosapp.presenters.SettingsContract // Importar el contrato
import com.example.replanteosapp.presenters.SettingsPresenter // Importar el Presenter

class SettingsDialogFragment(private val initialConfig: TextOverlayConfig) : DialogFragment(), SettingsContract.View {

    // Referencias a Vistas UI
    private lateinit var textSizeSeekBar: SeekBar
    private lateinit var textSizeValue: TextView
    private lateinit var textAlphaSeekBar: SeekBar
    private lateinit var textAlphaValue: TextView
    private lateinit var backgroundAlphaSeekBar: SeekBar
    private lateinit var backgroundAlphaValue: TextView
    private lateinit var enableBackgroundCheckbox: CheckBox
    private lateinit var enableNoteCheckbox: CheckBox
    private lateinit var noteEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var cancelButton: Button

    private lateinit var textColorWhite: Button
    private lateinit var textColorBlack: Button
    private lateinit var backgroundColorBlack: Button
    private lateinit var backgroundColorTransparent: Button

    // Referencia al Presenter
    private lateinit var presenter: SettingsContract.Presenter

    // Listener para comunicar el resultado de vuelta a MainPresenter
    private var listener: SettingsDialogListener? = null

    interface SettingsDialogListener {
        fun onSettingsSaved(newConfig: TextOverlayConfig)
    }

    fun setListener(listener: SettingsDialogListener) {
        this.listener = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setTitle("Configurar Texto de Ubicación")
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_settings, container, false)

        // 1. Inicialización de Vistas
        textSizeSeekBar = view.findViewById(R.id.textSizeSeekBar)
        textSizeValue = view.findViewById(R.id.textSizeValue)
        textAlphaSeekBar = view.findViewById(R.id.textAlphaSeekBar)
        textAlphaValue = view.findViewById(R.id.textAlphaValue)
        backgroundAlphaSeekBar = view.findViewById(R.id.backgroundAlphaSeekBar)
        backgroundAlphaValue = view.findViewById(R.id.backgroundAlphaValue)
        enableBackgroundCheckbox = view.findViewById(R.id.enableBackgroundCheckbox)
        enableNoteCheckbox = view.findViewById(R.id.enableNoteCheckbox)
        noteEditText = view.findViewById(R.id.noteEditText)
        saveButton = view.findViewById(R.id.saveButton)
        cancelButton = view.findViewById(R.id.cancelButton)

        textColorWhite = view.findViewById(R.id.textColorWhite)
        textColorBlack = view.findViewById(R.id.textColorBlack)
        backgroundColorBlack = view.findViewById(R.id.backgroundColorBlack)
        backgroundColorTransparent = view.findViewById(R.id.backgroundColorTransparent)

        // 2. Inicialización del Presenter
        presenter = SettingsPresenter(this, initialConfig) // Pasa la Vista y la configuración inicial

        // 3. Cargar la configuración inicial a la UI (usando el Presenter)
        // El Presenter le dirá a la Vista qué mostrar al inicio.
        loadConfigToUI(presenter.getCurrentConfig()) // La vista pregunta al presenter por la config

        // 4. Configurar Listeners (DELEGANDO AL PRESENTER)
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                presenter.onTextSizeChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        textAlphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                presenter.onTextAlphaChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        backgroundAlphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                presenter.onBackgroundAlphaChanged(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        enableBackgroundCheckbox.setOnCheckedChangeListener { _, isChecked ->
            presenter.onEnableBackgroundChecked(isChecked)
        }

        enableNoteCheckbox.setOnCheckedChangeListener { _, isChecked ->
            presenter.onEnableNoteChecked(isChecked)
        }

        noteEditText.doAfterTextChanged { editable ->
            presenter.onNoteTextChanged(editable.toString())
        }

        textColorWhite.setOnClickListener { presenter.onTextColorSelected(Color.WHITE) }
        textColorBlack.setOnClickListener { presenter.onTextColorSelected(Color.BLACK) }
        backgroundColorBlack.setOnClickListener { presenter.onBackgroundColorSelected(Color.parseColor("#80000000")) } // Color negro con 50% de alfa
        backgroundColorTransparent.setOnClickListener { presenter.onBackgroundColorSelected(null) } // null para transparente

        saveButton.setOnClickListener { presenter.onSaveButtonClick() }
        cancelButton.setOnClickListener { presenter.onCancelButtonClick() }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Notificar al Presenter que la Vista se está destruyendo
        presenter.detachView()
    }

    //region Métodos de la interfaz SettingsContract.View (implementaciones que el Presenter llama)

    override fun loadConfigToUI(config: TextOverlayConfig) {
        textSizeSeekBar.progress = ((config.textSize - 10f) / 20f * 100).toInt().coerceIn(0, 100)
        updateTextSizeValue("Tamaño: %.1fsp".format(config.textSize))

        textAlphaSeekBar.progress = (config.textAlpha * 100).toInt()
        updateTextAlphaValue("Transparencia Texto: %d%%".format((config.textAlpha * 100).toInt()))

        backgroundAlphaSeekBar.progress = (config.backgroundAlpha * 100).toInt()
        updateBackgroundAlphaValue("Transparencia Fondo: %d%%".format((config.backgroundAlpha * 100).toInt()))

        enableBackgroundCheckbox.isChecked = config.enableBackground
        enableBackgroundControls(config.enableBackground)

        enableNoteCheckbox.isChecked = config.enableNote
        enableNoteControls(config.enableNote)
        noteEditText.setText(config.noteText)
    }

    override fun updateTextSizeValue(value: String) {
        textSizeValue.text = value
    }

    override fun updateTextAlphaValue(value: String) {
        textAlphaValue.text = value
    }

    override fun updateBackgroundAlphaValue(value: String) {
        backgroundAlphaValue.text = value
    }

    override fun enableBackgroundControls(enable: Boolean) {
        backgroundAlphaSeekBar.isEnabled = enable
        backgroundColorBlack.isEnabled = enable
        backgroundColorTransparent.isEnabled = enable
    }

    override fun enableNoteControls(enable: Boolean) {
        noteEditText.isEnabled = enable
    }

    override fun dismissDialog() {
        // Notificar al listener que el diálogo se cierra y pasar la configuración final
        listener?.onSettingsSaved(presenter.getCurrentConfig())
        dismiss() // Cerrar el diálogo
    }

    //endregion

    companion object {
        const val TAG = "SettingsDialogFragment"
    }
}