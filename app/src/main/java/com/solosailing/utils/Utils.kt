package com.solosailing.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng


fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
    val results = FloatArray(1)
    Location.distanceBetween(lat1, lon1, lat2, lon2, results)
    return results[0]
}


fun calculateAzimuth(user: LatLng, heading: Float, target: LatLng): Float {
    val results = FloatArray(2)
    Location.distanceBetween(user.latitude, user.longitude, target.latitude, target.longitude, results)
    val bearingTo = results[1]
    var rel = ((bearingTo - heading + 540) % 360) - 180
    return rel
}