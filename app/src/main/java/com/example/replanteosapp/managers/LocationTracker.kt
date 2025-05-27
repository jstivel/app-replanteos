// LocationTracker.kt
package com.example.replanteosapp.managers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import android.util.Log
import com.example.replanteosapp.data.LocationData
import com.example.replanteosapp.services.GeocoderService
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import java.util.concurrent.TimeUnit



// Interfaz para los callbacks de geocodificación
interface GeocodingCallback {
    fun onAddressResult(city: String, address: String)
    fun onAddressError(errorMessage: String)
}

class LocationTracker(
    private val context: Context,
    private val geocoderService: GeocoderService
) {
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    private val settingsClient: SettingsClient = LocationServices.getSettingsClient(context)
    private var locationUpdateCallback: LocationUpdateCallback? = null

    fun setLocationUpdateCallback(callback: LocationUpdateCallback) {
        this.locationUpdateCallback = callback
    }

    // El LocationRequest ahora será fijo para actualizaciones cada segundo de alta precisión
    private lateinit var highAccuracyLocationRequest: LocationRequest

    init {
        // Inicializa el LocationRequest una vez
        highAccuracyLocationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            TimeUnit.SECONDS.toMillis(1) // Intervalo de actualización de 1 segundo
        )
            .setMinUpdateIntervalMillis(1000) // Asegura que no sea más rápido de 1 segundo
            .setMaxUpdateDelayMillis(TimeUnit.SECONDS.toMillis(2)) // No retrasar más de 2 segundos
            .setMinUpdateDistanceMeters(0f) // Actualizar incluso si el dispositivo no se mueve
            .build()
    }

    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.lastLocation?.let { location ->
                processLocation(location)
            }
        }
        override fun onLocationAvailability(locationAvailability: LocationAvailability) {
            if (!locationAvailability.isLocationAvailable) {
                Log.w(TAG, "Los servicios de ubicación no están disponibles (callback de disponibilidad).")
                locationUpdateCallback?.onLocationError("Servicios de ubicación no disponibles.")
            }
        }
    }

    private fun processLocation(location: Location) {
        geocoderService.getCityAndAddress(location.latitude, location.longitude, object : GeocodingCallback {
            override fun onAddressResult(city: String, address: String) {
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time,
                    city = city,
                    address = address
                )
                locationUpdateCallback?.onLocationReceived(locationData)
            }

            override fun onAddressError(errorMessage: String) {
                Log.w(TAG, "Error al obtener dirección: $errorMessage")
                val locationData = LocationData(
                    latitude = location.latitude,
                    longitude = location.longitude,
                    accuracy = location.accuracy,
                    timestamp = location.time,
                    city = "",
                    address = ""
                )
                locationUpdateCallback?.onLocationReceived(locationData) // Aún envía la ubicación sin dirección
            }
        })
    }

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(isPrecise: Boolean = false) { // El parámetro isPrecise ya no tiene un efecto significativo
        if (!checkPermissions()) {
            locationUpdateCallback?.onPermissionsDenied()
            return
        }

        // Siempre usa el LocationRequest de alta precisión/1 segundo
        val currentLocationRequest = highAccuracyLocationRequest

        val builder = LocationSettingsRequest.Builder().addLocationRequest(currentLocationRequest)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            Log.d(TAG, "Configuración de ubicación adecuada. Iniciando actualizaciones.")
            fusedLocationClient.requestLocationUpdates(currentLocationRequest, locationCallback, Looper.getMainLooper())
                .addOnSuccessListener { Log.d(TAG, "Solicitud de actualizaciones de ubicación exitosa (cada segundo).") }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error al solicitar actualizaciones de ubicación: ${e.message}", e)
                    locationUpdateCallback?.onLocationError("Fallo al iniciar actualizaciones: ${e.message}")
                }
        }
        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    Log.w(TAG, "Resolución de ajustes de ubicación requerida.")
                    locationUpdateCallback?.onLocationSettingsResolutionRequired(exception)
                } catch (sendEx: IntentSender.SendIntentException) {
                    Log.e(TAG, "Error al intentar resolver la configuración de ubicación: ${sendEx.message}", sendEx)
                    locationUpdateCallback?.onLocationError("Error al intentar resolver configuración de ubicación.")
                }
            } else if (exception is ApiException) {
                Log.e(TAG, "Error de la API de ubicación: ${exception.statusCode} - ${exception.message}", exception)
                locationUpdateCallback?.onLocationError("Error de ubicación: ${exception.message}")
            }
        }
    }

    fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
            .addOnSuccessListener { Log.d(TAG, "Actualizaciones de ubicación detenidas.") }
            .addOnFailureListener { e -> Log.e(TAG, "Error al detener actualizaciones de ubicación: ${e.message}", e) }
    }

    @SuppressLint("MissingPermission")
    fun getLastKnownLocation(callback: (LocationData?) -> Unit) {
        if (!checkPermissions()) {
            callback(null)
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    geocoderService.getCityAndAddress(location.latitude, location.longitude, object : GeocodingCallback {
                        override fun onAddressResult(city: String, address: String) {
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                timestamp = location.time,
                                city = city,
                                address = address
                            )
                            callback(locationData)
                        }

                        override fun onAddressError(errorMessage: String) {
                            Log.w(TAG, "Error al obtener dirección para última ubicación conocida: $errorMessage")
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                accuracy = location.accuracy,
                                timestamp = location.time,
                                city = "",
                                address = ""
                            )
                            callback(locationData)
                        }
                    })
                } else {
                    Log.w(TAG, "No se encontró última ubicación conocida.")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error al obtener última ubicación conocida: ${e.message}", e)
                callback(null)
            }
    }

    private fun checkPermissions(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "LocationTracker"
    }
}