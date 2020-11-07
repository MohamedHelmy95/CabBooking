package com.example.cabbooking.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.location.LocationRequest

class UiHelper {
  lateinit  var context:Context
    fun isPlayServicesAvailable(context : Context) : Boolean {
       this.context=context
        val googleApiAvailability = GoogleApiAvailability.getInstance()
        val status = googleApiAvailability.isGooglePlayServicesAvailable(context)
        return ConnectionResult.SUCCESS == status
    }
    fun isHaveLocationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    fun getLocationRequest() : LocationRequest {
        val locationRequest = LocationRequest.create()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 3000
        return locationRequest
    }
}