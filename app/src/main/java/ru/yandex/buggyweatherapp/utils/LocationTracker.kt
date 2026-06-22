package ru.yandex.buggyweatherapp.utils

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import ru.yandex.buggyweatherapp.BuildConfig
import java.util.concurrent.CopyOnWriteArrayList

class LocationTracker private constructor(

    private val context: Context
) {

    companion object {
        @Volatile
        private var instance: LocationTracker? = null

        fun getInstance(context: Context): LocationTracker {
            return instance ?: synchronized(this) {
                /**
                 * Ошибка 13.
                 * Здесь была обнаружена проблема с хранением Context внутри singleton.
                 * Если в LocationTracker передать Activity context, singleton будет хранить
                 * ссылку на Activity дольше её жизненного цикла, что может привести к утечке памяти.
                 * Чтобы решить эту проблему, я сохраняю только applicationContext.
                 */
                instance ?: LocationTracker(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }


    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager


    private val listeners =
        CopyOnWriteArrayList<(ru.yandex.buggyweatherapp.model.Location) -> Unit>()


    private val locationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {

            val newLocation = ru.yandex.buggyweatherapp.model.Location(
                latitude = location.latitude,
                longitude = location.longitude
            )


            notifyListeners(newLocation)
        }

        @Deprecated("Deprecated in Java")
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }


    fun startTracking() {
        try {
            /**
             * Ошибка 14.
             * Здесь геолокация запускалась через GPS_PROVIDER с частыми обновлениями,
             * но класс не предоставлял способ остановить получение координат.
             * Это могло приводить к лишнему сбору местоположения пользователя и расходу батареи.
             * Чтобы уменьшить риск, я перед запуском повторно снимаю старую подписку,
             * а также добавляю отдельный метод stopTracking().
             */
            stopTracking()

            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                5000, // 5 секунд
                10f, // 10 метров
                locationListener
            )


        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                Log.e("LocationTracker", "Permission denied", e)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("LocationTracker", "Error starting location tracking", e)
            }
        }
    }

    fun stopTracking() {
        try {
            locationManager.removeUpdates(locationListener)
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                Log.e("LocationTracker", "Permission denied", e)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e("LocationTracker", "Error stopping location tracking", e)
            }
        }
    }

    fun addListener(listener: (ru.yandex.buggyweatherapp.model.Location) -> Unit) {
        listeners.add(listener)
    }

    private fun notifyListeners(location: ru.yandex.buggyweatherapp.model.Location) {

        Handler(Looper.getMainLooper()).post {
            for (listener in listeners) {
                listener(location)
            }
        }
    }


}