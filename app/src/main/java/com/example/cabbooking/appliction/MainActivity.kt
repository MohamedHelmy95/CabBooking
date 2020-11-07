package com.example.cabbooking.appliction

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.annotation.SuppressLint
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Observer
import com.example.cabbooking.R
import com.example.cabbooking.appliction.viewModel.MainActivityViewModel
import com.example.cabbooking.collections.DriverCollection
import com.example.cabbooking.collections.MarkerCollection
import com.example.cabbooking.lisnteners.LatLngInterpolator
import com.example.cabbooking.util.GoogleMapHelper
import com.example.cabbooking.util.MarkerAnimationHelper
import com.example.cabbooking.util.UiHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), GoogleMap.OnCameraIdleListener, GoogleMap.OnCameraMoveStartedListener {
    companion object {
        private const val MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 5655
        private val TAG = MainActivity::class.java.simpleName
    }
    private lateinit var currentPlaceTextView:TextView
    private var firstTimeFlag = true
    private var googleMap : GoogleMap? = null
    private val uiHelper:UiHelper= UiHelper()
    lateinit var viewModel:MainActivityViewModel
    lateinit var googleMapHelper: GoogleMapHelper
    lateinit var driverCollection:DriverCollection
    lateinit var markerCollection: MarkerCollection
    private var currentLocationMarker: Marker? = null
    private val geoCoderValue = lazy {
        Geocoder(this)
    }
    @SuppressLint("ShowToast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val supportMapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync { googleMap ->
            googleMap.setOnCameraIdleListener(this)
            googleMap.setOnCameraMoveStartedListener(this)
            this.googleMap = googleMap
        }
        currentPlaceTextView=findViewById(R.id.currentPlaceTextView)
        googleMapHelper= GoogleMapHelper(this.resources)
        driverCollection= DriverCollection()
        markerCollection= MarkerCollection()
        viewModel= MainActivityViewModel(
            this.applicationContext, uiHelper, LocationServices.getFusedLocationProviderClient(this),driverCollection,markerCollection,googleMapHelper )
        if(!uiHelper.isPlayServicesAvailable(this.applicationContext)) {
            Toast.makeText(this, "Play Services is not installed!", Toast.LENGTH_SHORT)
            finish()
        } else requestLocationUpdates()
        viewModel.currentLocation
            .observe(this, Observer<Location> { location ->
                if (firstTimeFlag) {
                    firstTimeFlag = false;
                    animateCamera(location);
                }
                showOrAnimateMarker(location);
            })


        viewModel.reverseGeocodeResult
            .observe(this, Observer<String> {
                currentPlaceTextView.text = it
            })
        viewModel.addNewMarker
            .observe(this,{ markerPair ->
                val marker = googleMap?.addMarker(markerPair.second)
                if (marker != null) {
                    viewModel.insertNewMarker(markerPair.first, marker)
                }
            })
        // 1
        viewModel.calculateDistance
            .observe(this, Observer { distance ->
                pinTimeTextView.text = distance
                pinTimeTextView.visibility = VISIBLE
                pinProgressLoader.visibility = GONE
            })
    }
    private fun requestLocationUpdates() {
        if (!uiHelper.isHaveLocationPermission()) {
            ActivityCompat.requestPermissions(
                this, arrayOf(ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
            )
            return
        }
        viewModel.requestLocationUpdates()
    }
    private fun animateCamera(location: Location) {
        val cameraUpdate = googleMapHelper.buildCameraUpdate(location)
        googleMap?.animateCamera(cameraUpdate, 10, null)
    }
    private fun showOrAnimateMarker(location: Location) {
        if(currentLocationMarker == null){
            currentLocationMarker = googleMap?.addMarker(googleMapHelper.getUserMarker(location))
        } else {
            MarkerAnimationHelper.animateMarkerToGB(

                currentLocationMarker,
                location,
                LatLngInterpolator.Spherical()
            )
        }
    }
    override fun onCameraIdle() {
        val position = googleMap?.cameraPosition!!.target
        val latlng= LatLng(position.latitude,position.longitude)
        viewModel.makeReverseGeocodeRequest(position, geoCoderValue.value)
        viewModel.onCameraIdle(latlng)

    }

    override fun onCameraMoveStarted(p0: Int) {
        pinTimeTextView.visibility = GONE
        pinProgressLoader.visibility = VISIBLE
    }

}