package com.example.cabbooking.appliction.viewModel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import com.example.cabbooking.collections.DriverCollection
import com.example.cabbooking.collections.MarkerCollection
import com.example.cabbooking.lisnteners.FirebaseObjectValueListener
import com.example.cabbooking.lisnteners.LatLngInterpolator
import com.example.cabbooking.models.Driver
import com.example.cabbooking.util.FirebaseValueEventListenerHelper
import com.example.cabbooking.util.GoogleMapHelper
import com.example.cabbooking.util.MarkerAnimationHelper
import com.example.cabbooking.util.UiHelper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.database.FirebaseDatabase
import com.google.maps.DistanceMatrixApi
import com.google.maps.PendingResult
import com.google.maps.model.DistanceMatrix
import com.google.maps.model.TravelMode
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

class MainActivityViewModel constructor(
    private val context: Context,
    private val uiHelper: UiHelper,
    private val locationProviderClient: FusedLocationProviderClient,
    private val driverRepo: DriverCollection,
    private val makerRepo: MarkerCollection,
    private val googleMapHelper: GoogleMapHelper
):ViewModel(),CoroutineScope,FirebaseObjectValueListener
{

    companion object {
        private const val ONLINE_DRIVERS = "online_drivers"
        private const val TAG = "MainActivityViewModel"
    }
    private val databaseReference = FirebaseDatabase
        .getInstance()
        .reference
        .child(ONLINE_DRIVERS)
    private val firebaseValueEventListener = FirebaseValueEventListenerHelper(this)

    init {
        databaseReference.addChildEventListener(firebaseValueEventListener)
    }
    private val job = SupervisorJob()
    private val _currentLocation = MediatorLiveData<Location>()
    private val _reverseGeocodeResult = MediatorLiveData<String>()
    private val _addNewMarker = MediatorLiveData<Pair<Long, MarkerOptions>>()
    val reverseGeocodeResult : LiveData<String> = _reverseGeocodeResult
    val currentLocation: LiveData<Location>  = _currentLocation
    val addNewMarker: LiveData<Pair<Long, MarkerOptions>> = _addNewMarker
    private val _calculateDistance = MediatorLiveData<String>()
    val calculateDistance : LiveData<String>  = _calculateDistance
    private val locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult?) {
            super.onLocationResult(locationResult)
            if (locationResult?.lastLocation == null) return
            _currentLocation.postValue(locationResult.lastLocation)
        }
    }
    fun requestLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationProviderClient.requestLocationUpdates(uiHelper.getLocationRequest(), locationCallback, Looper.myLooper());
    }


    fun makeReverseGeocodeRequest(latLng: LatLng, geoCoder: Geocoder) {

        //1
        launch( context=coroutineContext+ Dispatchers.IO) {

            try {
                val result = geoCoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                if (result != null && result.size > 0) {
                    val address = result[0]
                    _reverseGeocodeResult.postValue(address.getAddressLine(0).plus(" , ").plus(address.locality))
                }
            } catch (__: Exception) {
            }
        }
        }

    override val coroutineContext: CoroutineContext
        get() = job

    override fun onDriverOnline(driver: Driver) {
        if (driverRepo.insertDriver(driver)) {
            val markerOptions = googleMapHelper.getDriverMarkerOptions(LatLng(driver.lat, driver.lng), driver.angle)
            _addNewMarker.value = Pair(driver.id, markerOptions)
        }
    }

    override fun onDriverChanged(driver: Driver) {
        launch(context = coroutineContext) {
            val fetchedDriver = driverRepo.getDriverWithId(driver.id) ?: return@launch
            fetchedDriver.update(driver.lat, driver.lng, driver.angle)
            val marker = makerRepo.getMarker(fetchedDriver.id) ?: return@launch
            withContext(Dispatchers.Main) {
                marker.rotation = fetchedDriver.angle + 90
                MarkerAnimationHelper.animateMarkerToGB(
                    marker,
                    LatLng(fetchedDriver.lat, fetchedDriver.lng),
                    LatLngInterpolator.Spherical()
                )
            }
        }
    }

    override fun onDriverOffline(driver: Driver) {
            launch(context = coroutineContext) {
                if (driverRepo.removeDriver(driver.id))
                    makerRepo.removeMarker(driver.id)
            }

        }

    fun insertNewMarker(key: Long, value: Marker) {
        makerRepo.insertMarker(key, value)
    }
    fun onCameraIdle(latLng: LatLng) {
        launch(context=coroutineContext + Dispatchers.Default) {
            if (driverRepo.allDriver().isNotEmpty()) {
                val driver = driverRepo.getNearestDriver(latLng.latitude, latLng.longitude)
                driver?.let { calculateDistance(latLng, it) }
            }
        }
    }

    private fun calculateDistance(latLng: LatLng, driver: Driver) {
        launch(context=coroutineContext + Dispatchers.IO) {
            val destination = arrayOf(driver.lat.toString() + "," + driver.lng.toString())
            val origins = arrayOf(latLng.latitude.toString() + "," + latLng.longitude.toString())
            DistanceMatrixApi.getDistanceMatrix(googleMapHelper.geoContextDistanceApi(), origins, destination)
                .mode(TravelMode.DRIVING)
                .setCallback(object : PendingResult.Callback<DistanceMatrix> {
                    override fun onFailure(e: Throwable?) {
                        Log.e(TAG, "onFailure: "+e?.message.toString()    )
                    }

                    override fun onResult(result: DistanceMatrix?) {
                        if (result != null)
                            _calculateDistance.postValue(result.rows[0].elements[0].duration.humanReadable)
                    }
                })
        }
    }


}