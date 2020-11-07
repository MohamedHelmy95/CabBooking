package com.example.cabbooking.util

import android.content.res.Resources
import android.location.Location
import com.example.cabbooking.R
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.GeoApiContext

class GoogleMapHelper(private val resources : Resources) {
    companion object {
        private const val ZOOM_LEVEL = 18
        private const val TILT_LEVEL = 25
        private val geoApiContextBuilder = GeoApiContext.Builder()
    }
    fun buildCameraUpdate(location: Location): CameraUpdate {
        val cameraPosition = CameraPosition.Builder()
            .target(LatLng(location.latitude, location.longitude))
            .tilt(TILT_LEVEL.toFloat())
            .zoom(ZOOM_LEVEL.toFloat())
            .build()
        return CameraUpdateFactory.newCameraPosition(cameraPosition)
    }
    private fun getMarkerOptions(position: LatLng, resource: Int): MarkerOptions {
        return MarkerOptions()
            .icon(BitmapDescriptorFactory.fromResource(resource))
            .position(position)
    }

    fun getUserMarker(location: Location): MarkerOptions {
        return getMarkerOptions(LatLng(location.latitude, location.longitude), R.drawable.blue_dot)
    }

    fun defaultMapSettings(googleMap: GoogleMap) {
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isRotateGesturesEnabled = true
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isTiltGesturesEnabled = true
        googleMap.uiSettings.isCompassEnabled = false
        googleMap.isBuildingsEnabled = true
    }
    fun getDriverMarkerOptions(position: LatLng, angle: Float): MarkerOptions {
        val options = getMarkerOptions(position, R.drawable.caronmap)
        options.flat(true)
        options.rotation(angle + 90)
        return options
    }

    fun geoContextDistanceApi(): GeoApiContext? {

            return geoApiContextBuilder
                .apiKey(resources.getString(R.string.matrix_maps_Api_key))
                .build()
        }

}


