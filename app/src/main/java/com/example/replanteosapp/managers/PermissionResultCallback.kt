package com.example.replanteosapp.managers

interface PermissionResultCallback {
    fun onPermissionsGranted()
    fun onPermissionsDenied(deniedPermissions: List<String>)
}

