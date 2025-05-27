// PermissionManager.kt
package com.example.replanteosapp.managers

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity



class PermissionManager(private val activity: FragmentActivity) {

    private var permissionCallback: PermissionResultCallback? = null
    private val requiredPermissions = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
        // android.Manifest.permission.WRITE_EXTERNAL_STORAGE // CONSIDERAR ELIMINAR ESTE PERMISO SI SOLO GUARDAS EN MEDIASTORE EN API 29+
    )

    // ESTO ES CRUCIAL: El ActivityResultLauncher se encarga de los requestCodes internamente.
    // NO necesitas un requestCode numérico aquí.
    private val requestPermissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissionsMap ->
            val grantedPermissions = permissionsMap.filter { it.value }.map { it.key }
            val deniedPermissions = permissionsMap.filter { !it.value }.map { it.key }

            if (deniedPermissions.isEmpty()) {
                permissionCallback?.onPermissionsGranted()
            } else {
                permissionCallback?.onPermissionsDenied(deniedPermissions)
            }
        }

    fun requestPermissions(callback: PermissionResultCallback) {
        this.permissionCallback = callback

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(activity, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isEmpty()) {
            // Todos los permisos ya están concedidos
            permissionCallback?.onPermissionsGranted()
        } else {

            requestPermissionsLauncher.launch(permissionsToRequest)
        }
    }

    fun allPermissionsGranted(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}