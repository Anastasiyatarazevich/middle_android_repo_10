package ru.yandex.buggyweatherapp.repository

import android.content.Context
import android.location.Geocoder
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import ru.yandex.buggyweatherapp.BuildConfig
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.utils.LocationTracker
import java.util.Locale

class LocationRepository(

    private val context: Context
) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)


    private var currentLocation: Location? = null


    private var locationCallback: ((Location?) -> Unit)? = null


    fun getCurrentLocation(callback: (Location?) -> Unit) {
        try {
            locationCallback = callback

            /**
             * Ошибка 10.
             * Здесь и далее в этом файле была обнаружена проблема с логированием ошибок геолокации.
             * Подробные логи могут содержать технические детали о местоположении пользователя
             * или состоянии устройства и не должны попадать в release-сборку.
             * Чтобы решить эту проблему, я оставила подробное логирование только для debug-сборки.
             */
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        val userLocation = Location(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        currentLocation = userLocation
                        callback(userLocation)
                    } else {

                        requestLocationUpdates(callback)
                    }
                }
                .addOnFailureListener { e ->
                    if (BuildConfig.DEBUG) {
                        Log.e("LocationRepository", "Error getting location", e)
                    }
                    callback(null)
                }
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                Log.e("LocationRepository", "Location permission not granted", e)
            }
            callback(null)
        }
    }


    private fun requestLocationUpdates(callback: (Location?) -> Unit) {
        try {
            /**
             * Ошибка 9.
             * Здесь была обнаружена проблема с избыточным получением геолокации пользователя.
             * Приложение запрашивало обновления с PRIORITY_HIGH_ACCURACY и не останавливало их
             * после получения первого результата.
             * Для приложения погоды достаточно примерного местоположения и одного успешного результата,
             * поэтому постоянное получение точной геолокации нарушает принцип минимального сбора данных.
             * Чтобы решить эту проблему, я заменила приоритет на PRIORITY_BALANCED_POWER_ACCURACY
             * и останавливаю обновления геолокации после первого успешного результата.
             */
            val locationRequest =
                LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(5000)
                    .build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    locationResult.lastLocation?.let { location ->
                        val userLocation = Location(
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                        currentLocation = userLocation
                        callback(userLocation)

                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }
            }


            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                Log.e("LocationRepository", "Location permission not granted", e)
            }
            callback(null)
        }
    }


    fun getCityNameFromLocation(location: Location): String? {
        try {

            val geocoder = Geocoder(context, Locale.getDefault())

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)

            return if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                if (address.locality != null) {
                    address.locality
                } else if (address.subAdminArea != null) {
                    address.subAdminArea
                } else {
                    address.adminArea
                }
            } else {
                null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("LocationRepository", "Error getting city name", e)
            }
            return null
        }
    }


    fun startLocationTracking() {
        LocationTracker.getInstance(context).startTracking()
    }


}