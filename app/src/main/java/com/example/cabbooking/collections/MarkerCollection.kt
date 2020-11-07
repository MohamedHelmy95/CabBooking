package com.example.cabbooking.collections

import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarkerCollection {
    private val markerMap = mutableMapOf<Long, Marker>()

    fun insertMarker(key: Long, value: Marker) {
        if (!markerMap.containsKey(key))
            markerMap[key] = value
    }

    suspend fun removeMarker(key: Long) = withContext(Dispatchers.Main) {
        val marker = markerMap[key]
        marker?.remove()
    }

    fun getMarker(key: Long) = markerMap[key]

    fun allMarkers() = markerMap
}