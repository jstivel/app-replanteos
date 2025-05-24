// GeocoderService.kt
package com.example.replanteosapp.services

import android.content.Context
import android.location.Geocoder
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.replanteosapp.managers.GeocodingCallback
import java.io.IOException
import java.util.Locale

class GeocoderService(private val context: Context) {

    private val TAG = "GeocoderService"
    // Geocoder puede estar obsoleto en API 33+, pero sigue siendo funcional para la mayoría
    // Si necesitas soporte para API 33+, deberías usar Geocoder.getFromLocation(double, double, int, Geocoder.GeocodeListener)
    // Para simplificar, asumimos que este funciona para tu API objetivo actual.
    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())

    // Este es el método que debe coincidir con la llamada desde LocationTracker
    fun getCityAndAddress(latitude: Double, longitude: Double, callback: GeocodingCallback) {
        // Ejecutar la operación de geocodificación en un hilo de fondo
        // para evitar bloquear el hilo principal (UI thread)
        Thread {
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val city = address.locality ?: ""
                    val fullAddress = address.getAddressLine(0) ?: "" // Obtiene la línea completa de la dirección

                    // Volver al hilo principal (UI thread) para notificar el resultado
                    Handler(Looper.getMainLooper()).post {
                        callback.onAddressResult(city, fullAddress)
                    }
                } else {
                    Handler(Looper.getMainLooper()).post {
                        callback.onAddressError("No se encontraron direcciones para las coordenadas.")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error de Geocodificación: ${e.message}", e)
                Handler(Looper.getMainLooper()).post {
                    callback.onAddressError("Error de red o servicio de geocodificación: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Excepción inesperada en GeocoderService: ${e.message}", e)
                Handler(Looper.getMainLooper()).post {
                    callback.onAddressError("Error inesperado en geocodificación: ${e.message}")
                }
            }
        }.start()
    }
}