package codes.chrishorner.socketweather.data

import android.content.Context
import android.location.Location
import codes.chrishorner.socketweather.util.arePlayServicesAvailable
import codes.chrishorner.socketweather.util.await
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.emptyFlow

private val request = LocationRequest()
    .setPriority(PRIORITY_BALANCED_POWER_ACCURACY)
    .setFastestInterval(1000)
    .setSmallestDisplacement(100f)

private var cachedLocation: DeviceLocation? = null

fun getDeviceLocationUpdates(context: Context): Flow<DeviceLocation> {
  if (!arePlayServicesAvailable(context)) return emptyFlow()

  return channelFlow {
    cachedLocation?.let { offer(it) }

    val client = LocationServices.getFusedLocationProviderClient(context)
    val lastKnownLocation: Location? = try {
      client.lastLocation.await()
    } catch (e: Exception) {
      null
    }

    if (lastKnownLocation != null && !isClosedForSend) {
      val deviceLocation = DeviceLocation(lastKnownLocation.latitude, lastKnownLocation.longitude)
      cachedLocation = deviceLocation
      send(deviceLocation)
    }

    val callback = object : LocationCallback() {

      override fun onLocationResult(result: LocationResult) {
        val deviceLocation = DeviceLocation(result.lastLocation.latitude, result.lastLocation.longitude)
        cachedLocation = deviceLocation
        offer(deviceLocation)
      }

      override fun onLocationAvailability(availability: LocationAvailability) {}
    }

    if (!isClosedForSend) {
      client.requestLocationUpdates(request, callback, null)
    }

    awaitClose { client.removeLocationUpdates(callback) }
  }
}