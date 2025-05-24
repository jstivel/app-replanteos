// app/src/main/java/com/example/replanteosapp/data/TextOverlayConfig.kt
package com.example.replanteosapp.data

import android.graphics.Color
import java.io.Serializable // Para pasar entre actividades si es necesario

data class TextOverlayConfig(
    var textSize: Float = 14f, // Tamaño de letra predeterminado en sp
    var textColor: Int = Color.WHITE, // Color de letra predeterminado
    var backgroundColor: Int? = Color.parseColor("#80000000"), // Color de fondo predeterminado (negro semi-transparente)
    // Null para sin fondo
    var backgroundAlpha: Float = 0.5f, // Transparencia del fondo (0.0 a 1.0)
    var textAlpha: Float = 1.0f, // Transparencia del texto (0.0 a 1.0)
    var enableBackground: Boolean = true, // Controla si el fondo está habilitado
    var enableNote: Boolean = false, // Controla si la nota adicional está habilitada
    var noteText: String = "", // Texto de la nota adicional
    var offsetX: Float = 0f, // Desplazamiento X en píxeles (para futura posición)
    var offsetY: Float = 0f  // Desplazamiento Y en píxeles (para futura posición)
) : Serializable // Permite pasar este objeto entre componentes Android