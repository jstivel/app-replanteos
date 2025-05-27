package com.example.replanteosapp.managers

import com.example.replanteosapp.data.LocationData
import com.google.android.gms.common.api.ResolvableApiException

interface LocationUpdateCallback {
    fun onLocationReceived(locationData: LocationData)
    fun onLocationError(errorMessage: String)
    fun onPermissionsDenied() // Cuando el LocationTracker detecta que no tiene permisos
    fun onLocationSettingsResolutionRequired(resolvableApiException: ResolvableApiException) // Cuando se necesita activar el GPS
}

