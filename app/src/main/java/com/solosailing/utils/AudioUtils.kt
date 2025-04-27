package com.solosailing.utils

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import kotlin.math.*


fun calculateDistance(a: LatLng, b: LatLng): Float {
    val result = FloatArray(2)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
    return result[0]
}


fun calculateAzimuth(user: LatLng, heading: Float, target: LatLng): Float {
    val results = FloatArray(2)
    Location.distanceBetween(user.latitude, user.longitude, target.latitude, target.longitude, results)
    val bearingTo = results[1]
    var rel = ((bearingTo - heading + 540) % 360) - 180
    return rel
}