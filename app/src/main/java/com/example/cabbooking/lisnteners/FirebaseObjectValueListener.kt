package com.example.cabbooking.lisnteners

import com.example.cabbooking.models.Driver

interface FirebaseObjectValueListener {
    fun onDriverOnline(driver : Driver);
    fun onDriverChanged(driver :Driver);
    fun onDriverOffline(driver : Driver);
}