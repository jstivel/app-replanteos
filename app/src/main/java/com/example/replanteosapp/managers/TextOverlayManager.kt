package com.example.replanteosapp.managers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TextOverlayManager {

    fun drawTextOverlay(
        bitmap: Bitmap,
        locationData: LocationData?,
        config: TextOverlayConfig
    ): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        textPaint.textSize = config.textSize * canvas.density // Convierte SP a PX
        textPaint.color = config.textColor
        textPaint.alpha = (config.textAlpha * 255).toInt()
        textPaint.typeface = Typeface.DEFAULT_BOLD // Opcional: negrita
        // textPaint.setShadowLayer(1f, 0f, 0f, Color.BLACK); // Opcional: sombra para mejor visibilidad

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (config.enableBackground && config.backgroundColor != null) {
            backgroundPaint.color = config.backgroundColor!!
            backgroundPaint.alpha = (config.backgroundAlpha * 255).toInt()
        } else {
            backgroundPaint.color = Color.TRANSPARENT
        }

        // Construir el texto de ubicación
        val locationText = locationData?.let {
            val latLon = "Lat: %.6f, Lon: %.6f".format(Locale.US, it.latitude, it.longitude)
            val accuracy = "Precisión: ±%.0fm".format(it.accuracy ?: 0f)
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(it.timestamp))
            val city = if (it.city.isNotEmpty()) "Ciudad: ${it.city}" else ""
            val address = if (it.address.isNotEmpty()) "Dirección: ${it.address}" else ""

            val sb = StringBuilder()
            sb.append(latLon)
            sb.append("\n").append(accuracy)
            sb.append("\nFecha: ").append(dateTime.substringBefore(" "))
            sb.append("\nHora: ").append(dateTime.substringAfter(" "))
            if (city.isNotEmpty()) sb.append("\n").append(city)
            if (address.isNotEmpty()) sb.append("\n").append(address)

            if (config.enableNote && config.noteText.isNotBlank()) {
                sb.append("\nNota: ").append(config.noteText)
            }
            sb.toString()
        } ?: "Ubicación no disponible"

        // Calcular el ancho máximo del texto (por ejemplo, el 80% del ancho del bitmap)
        val textWidth = (bitmap.width * 0.8).toInt()

        // Usar StaticLayout para manejar saltos de línea automáticos
        val staticLayout = StaticLayout.Builder.obtain(
            locationText,
            0,
            locationText.length,
            textPaint,
            textWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(false)
            .build()

        // Posición del texto (por ejemplo, en la esquina superior izquierda con un margen)
        val padding = 20f
        val x = padding
        val y = padding

        // Dibujar el fondo si está habilitado
        if (config.enableBackground && config.backgroundColor != null) {
            canvas.drawRect(
                x - padding/2, // Ajusta un poco para que el fondo sea más grande que el texto
                y - padding/2,
                x + staticLayout.width + padding/2,
                y + staticLayout.height + padding/2,
                backgroundPaint
            )
        }

        // Dibujar el texto
        canvas.save()
        canvas.translate(x, y)
        staticLayout.draw(canvas)
        canvas.restore()

        return mutableBitmap
    }
}