package com.example.replanteosapp.data

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float?, // '?' indica que puede ser nulo
    val timestamp: Long,
    val city: String,
    val address: String
)