// app/src/main/java/com/example/replanteosapp/managers/TextOverlayManager.kt
package com.example.replanteosapp.managers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.data.TextOverlayConfig
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class TextOverlayManager {

    fun formatLocationText(locationData: LocationData, config: TextOverlayConfig): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

        val latitude = "Lat: %.6f".format(Locale.US, locationData.latitude)
        val longitude = "Lon: %.6f".format(Locale.US, locationData.longitude)
        val accuracy = "Precisión: ±%.0fm".format(locationData.accuracy ?: 0f)
        val date = "Fecha: ${dateFormat.format(Date(locationData.timestamp))}"
        val time = "Hora: ${timeFormat.format(Date(locationData.timestamp))}"
        val city = if (locationData.city.isNotEmpty()) "Ciudad: ${locationData.city}" else ""
        val address = if (locationData.address.isNotEmpty()) "Dirección: ${locationData.address}" else ""

        val builder = StringBuilder()
        builder.append(latitude).append(", ").append(longitude)
        builder.append("\n").append(accuracy)
        builder.append("\n").append(date)
        builder.append("\n").append(time)
        if (city.isNotEmpty()) builder.append("\n").append(city)
        if (address.isNotEmpty()) builder.append("\n").append(address)
        if (config.enableNote && config.noteText.isNotBlank()) {
            builder.append("\nNota: ").append(config.noteText)
        }
        return builder.toString()
    }

    fun drawTextOverlay(originalBitmap: Bitmap, locationData: LocationData?, config: TextOverlayConfig): Bitmap {
        val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)

        if (locationData == null) {
            return mutableBitmap
        }

        val textToDraw = formatLocationText(locationData, config)

        val textPaint = Paint().apply {
            color = Color.WHITE
            // Ajusta el tamaño del texto. Un buen punto de partida es relativo al ancho de la imagen.
            // Probemos con un tamaño un poco más pequeño para empezar a 30f.
            textSize = mutableBitmap.width / 30f // Ajuste dinámico del tamaño de fuente
            typeface = Typeface.DEFAULT_BOLD
            isAntiAlias = true
            setShadowLayer(2f, 1f, 1f, Color.BLACK) // Sombra suave para que el texto sea legible
        }

        val lines = textToDraw.split("\n")

        // Calcular la altura real de una línea de texto (sin espacio adicional)
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.bottom - fontMetrics.top // Altura de la línea

        // Calcular el ancho máximo de las líneas de texto
        var maxLineWidth = 0f
        for (line in lines) {
            maxLineWidth = max(maxLineWidth, textPaint.measureText(line))
        }

        // Padding dinámico basado en el ancho de la imagen (por ejemplo, 2% del ancho)
        val padding = mutableBitmap.width * 0.02f

        // Pintura para el fondo del texto (semitransparente negro)
        val backgroundPaint = Paint().apply {
            color = Color.parseColor("#80000000") // ARGB: 80 = 50% de opacidad, 000000 = negro
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        // 1. Calcular el ancho total del área del texto (incluyendo padding)
        val totalTextDisplayWidth = maxLineWidth + (padding * 2)

        // 2. Calcular la altura total del área del texto (incluyendo padding)
        // Usamos fontMetrics.leading para el interlineado natural
        val totalTextDisplayHeight = (lines.size * (lineHeight + fontMetrics.leading)) + (padding * 2)

        // 3. Calcular las coordenadas para el rectángulo de fondo
        // Centrar horizontalmente: (ancho_canvas - ancho_fondo) / 2
        val backgroundRectLeft = (canvas.width - totalTextDisplayWidth) / 2
        val backgroundRectTop = canvas.height - totalTextDisplayHeight // Anclado a la parte inferior
        val backgroundRectRight = backgroundRectLeft + totalTextDisplayWidth
        val backgroundRectBottom = canvas.height.toFloat() // Llega hasta el borde inferior de la imagen

        // Dibuja el fondo del texto
        canvas.drawRect(backgroundRectLeft, backgroundRectTop, backgroundRectRight, backgroundRectBottom, backgroundPaint)

        // 4. Calcular las coordenadas Y para dibujar el texto
        // La primera línea de texto se dibuja desde la parte superior del fondo + padding
        var currentTextY = backgroundRectTop + padding + (-fontMetrics.top) // Ajuste para la altura del texto

        // Dibuja cada línea de texto
        for (line in lines) {
            // Centrar cada línea de texto horizontalmente dentro del área del fondo
            val textX = backgroundRectLeft + (totalTextDisplayWidth - textPaint.measureText(line)) / 2
            canvas.drawText(line, textX, currentTextY, textPaint)
            currentTextY += (lineHeight + fontMetrics.leading) // Mueve hacia abajo para la siguiente línea
        }

        return mutableBitmap
    }
}