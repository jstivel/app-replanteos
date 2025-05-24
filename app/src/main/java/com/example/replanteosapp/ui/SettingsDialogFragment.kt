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
import com.example.replanteosapp.R
import com.example.replanteosapp.data.TextOverlayConfig // Importar la clase de configuración
import com.example.replanteosapp.MainActivity // Para llamar a updateTextOverlayConfig

class SettingsDialogFragment(private var currentConfig: TextOverlayConfig) : DialogFragment() {

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

    // Guardar los cambios temporales aquí
    private var tempConfig: TextOverlayConfig = currentConfig.copy()

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

        // Inicializar vistas
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


        // Cargar y aplicar la configuración actual
        loadConfigToUI(tempConfig)

        // Configurar listeners de SeekBar
        textSizeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // Rango de 10sp a 30sp
                tempConfig.textSize = 10f + (progress / 100f) * 20f // Calcula el tamaño en SP
                textSizeValue.text = "Tamaño: %.1fsp".format(tempConfig.textSize)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        textAlphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tempConfig.textAlpha = progress / 100f
                textAlphaValue.text = "Transparencia Texto: %d%%".format((tempConfig.textAlpha * 100).toInt())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        backgroundAlphaSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tempConfig.backgroundAlpha = progress / 100f
                backgroundAlphaValue.text = "Transparencia Fondo: %d%%".format((tempConfig.backgroundAlpha * 100).toInt())
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Configurar listeners de CheckBox
        enableBackgroundCheckbox.setOnCheckedChangeListener { _, isChecked ->
            tempConfig.enableBackground = isChecked
            backgroundAlphaSeekBar.isEnabled = isChecked // Habilitar/deshabilitar SeekBar de fondo
            backgroundColorBlack.isEnabled = isChecked
            backgroundColorTransparent.isEnabled = isChecked
        }

        enableNoteCheckbox.setOnCheckedChangeListener { _, isChecked ->
            tempConfig.enableNote = isChecked
            noteEditText.isEnabled = isChecked
        }

        // Configurar listener de EditText para la nota
        noteEditText.doAfterTextChanged { editable ->
            tempConfig.noteText = editable.toString()
        }

        // Configurar listeners de botones de color
        textColorWhite.setOnClickListener { tempConfig.textColor = Color.WHITE }
        textColorBlack.setOnClickListener { tempConfig.textColor = Color.BLACK }
        backgroundColorBlack.setOnClickListener { tempConfig.backgroundColor = Color.parseColor("#80000000") } // Negro semi-transparente
        backgroundColorTransparent.setOnClickListener { tempConfig.backgroundColor = null } // Sin color de fondo específico

        // Configurar botones de acción
        saveButton.setOnClickListener {
            // Notificar a MainActivity con la nueva configuración
            (activity as? MainActivity)?.updateTextOverlayConfig(tempConfig)
            dismiss() // Cerrar el diálogo
        }

        cancelButton.setOnClickListener {
            dismiss() // Cerrar el diálogo sin guardar cambios
        }

        return view
    }

    private fun loadConfigToUI(config: TextOverlayConfig) {
        // Cargar tamaño de texto (mapeo inverso: SP a SeekBar progress)
        textSizeSeekBar.progress = ((config.textSize - 10f) / 20f * 100).toInt().coerceIn(0, 100)
        textSizeValue.text = "Tamaño: %.1fsp".format(config.textSize)

        // Cargar transparencia de texto
        textAlphaSeekBar.progress = (config.textAlpha * 100).toInt()
        textAlphaValue.text = "Transparencia Texto: %d%%".format((config.textAlpha * 100).toInt())

        // Cargar transparencia de fondo
        backgroundAlphaSeekBar.progress = (config.backgroundAlpha * 100).toInt()
        backgroundAlphaValue.text = "Transparencia Fondo: %d%%".format((config.backgroundAlpha * 100).toInt())

        // Cargar estado de fondo y nota
        enableBackgroundCheckbox.isChecked = config.enableBackground
        backgroundAlphaSeekBar.isEnabled = config.enableBackground // Habilitar/deshabilitar
        backgroundColorBlack.isEnabled = config.enableBackground
        backgroundColorTransparent.isEnabled = config.enableBackground

        enableNoteCheckbox.isChecked = config.enableNote
        noteEditText.isEnabled = config.enableNote
        noteEditText.setText(config.noteText)
    }

    companion object {
        const val TAG = "SettingsDialogFragment"
    }
}