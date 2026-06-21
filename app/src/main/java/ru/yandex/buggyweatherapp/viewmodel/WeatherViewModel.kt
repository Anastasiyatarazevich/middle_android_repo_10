package ru.yandex.buggyweatherapp.viewmodel

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import ru.yandex.buggyweatherapp.repository.LocationRepository
import ru.yandex.buggyweatherapp.repository.WeatherRepository
import ru.yandex.buggyweatherapp.utils.ImageLoader

class WeatherViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {


    private lateinit var applicationContext: Context


    private val weatherRepository = WeatherRepository()
    private val locationRepository by lazy {
        LocationRepository(applicationContext)
    }


    val weatherData = MutableLiveData<WeatherData>()
    val currentLocation = MutableLiveData<Location>()
    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String>()
    val cityName = MutableLiveData<String>()


    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())
    private var isCurrentWeatherFavorite = false

    private var refreshJob: Job? = null

    fun initialize(context: Context) {
        /**
         * Ошибка 4.
         * Здесь была обнаружена проблема с возможной утечкой памяти:
         * во ViewModel сохранялся Context, который мог быть Activity context.
         * ViewModel может жить дольше Activity, например при смене конфигурации экрана,
         * поэтому хранение ссылки на Activity может привести к утечке памяти.
         * Чтобы решить эту проблему, я сохраняю applicationContext вместо Activity context.
         */
        this.applicationContext = context.applicationContext
        this.applicationContext = context
        fetchCurrentLocationWeather()


        startAutoRefresh()
    }


    fun fetchCurrentLocationWeather() {
        isLoading.value = true
        error.value = null

        locationRepository.getCurrentLocation { location ->
            if (location != null) {
                currentLocation.value = location


                val cityNameFromLocation = locationRepository.getCityNameFromLocation(location)
                cityName.value = cityNameFromLocation

                getWeatherForLocation(location)
            } else {
                isLoading.value = false
                error.value = "Unable to get current location"
            }
        }
    }

    fun getWeatherForLocation(location: Location) {
        isLoading.value = true
        error.value = null

        weatherRepository.getWeatherData(location) { data, exception ->

            Handler(Looper.getMainLooper()).post {
                isLoading.value = false

                if (data != null) {
                    weatherData.value = applyFavoriteState(data)
                } else {
                    error.value = exception?.message ?: "Unknown error"
                }
            }
        }
    }

    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            error.value = "City name cannot be empty"
            return
        }

        isLoading.value = true
        error.value = null


        weatherRepository.getWeatherByCity(city) { data, exception ->

            isLoading.value = false

            if (data != null) {
                weatherData.value = applyFavoriteState(data)
                cityName.value = data.cityName
                currentLocation.value = Location(0.0, 0.0, data.cityName)
            } else {
                error.value = exception?.message ?: "Unknown error"
            }
        }
    }


    fun formatTemperature(temp: Double): String {
        return "${temp.toInt()}°C"
    }


    fun loadWeatherIcon(iconCode: String) {
        coroutineScope.launch {
            val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
            ImageLoader.loadImage(iconUrl)
        }
    }


    private fun startAutoRefresh() {
        /**
         * Ошибка 5.
         * Здесь была обнаружена проблема с автообновлением погоды через Timer.
         * Timer не был привязан к жизненному циклу ViewModel и мог продолжать выполнять задачи,
         * даже когда экран уже не используется. Также Android Studio предупреждает,
         * что scheduleAtFixedRate может неожиданно запускать много задач подряд,
         * когда процесс приложения переходит из cached-состояния обратно в active.
         * Чтобы решить эту проблему, я заменила Timer на корутину в viewModelScope.
         * Такая задача автоматически отменяется вместе с ViewModel.
         */

        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {
            while (isActive) {
                delay(60000)

                currentLocation.value?.let { location ->
                    getWeatherForLocation(location)
                }
            }
        }
    }


    fun toggleFavorite() {
        weatherData.value?.let { currentWeather ->
            /**
             * Ошибка 8.
             * Здесь была обнаружена проблема с сохранением состояния избранного.
             * Раньше состояние "сердечка" хранилось только внутри текущего объекта WeatherData,
             * поэтому при повороте экрана, повторной загрузке погоды или поиске другого города
             * состояние могло сбрасываться.
             * Чтобы решить эту проблему, я храню ключи избранных городов в SavedStateHandle,
             * а WeatherData обновляю через copy().
             * Так у каждого города сохраняется своё состояние "сердечка" без сильной смены архитектуры.
             */
            val favoriteKeys = getFavoriteCityKeys()
            val favoriteKey = getFavoriteKey(currentWeather)

            if (favoriteKeys.contains(favoriteKey)) {
                favoriteKeys.remove(favoriteKey)
            } else {
                favoriteKeys.add(favoriteKey)
            }

            saveFavoriteCityKeys(favoriteKeys)

            weatherData.value = currentWeather.copy(
                isFavorite = favoriteKeys.contains(favoriteKey)
            )
        }
    }

    private fun getFavoriteCityKeys(): MutableSet<String> {
        return savedStateHandle
            .get<ArrayList<String>>(FAVORITE_CITY_KEYS)
            ?.toMutableSet()
            ?: mutableSetOf()
    }

    private fun saveFavoriteCityKeys(keys: Set<String>) {
        savedStateHandle[FAVORITE_CITY_KEYS] = ArrayList(keys)
    }

    private fun getFavoriteKey(weather: WeatherData): String {
        return "${weather.cityName.trim().lowercase()}:${weather.country.trim().lowercase()}"
    }

    private fun applyFavoriteState(weather: WeatherData): WeatherData {
        val favoriteKeys = getFavoriteCityKeys()

        return weather.copy(
            isFavorite = favoriteKeys.contains(getFavoriteKey(weather))
        )
    }


    override fun onCleared() {
        super.onCleared()
        refreshJob?.cancel()
        refreshJob = null
    }

    private companion object {
        const val FAVORITE_CITY_KEYS = "favorite_city_keys"
    }
}