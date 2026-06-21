package ru.yandex.buggyweatherapp.model

data class WeatherData(
    val cityName: String,
    val country: String,
    val temperature: Double,
    val feelsLike: Double,
    val minTemp: Double,
    val maxTemp: Double,
    val humidity: Int,
    val pressure: Int,
    val windSpeed: Double,
    val windDirection: Int,
    val description: String,
    val icon: String,
    val rain: Double? = null,
    val snow: Double? = null,
    val cloudiness: Int,
    val sunriseTime: Long,
    val sunsetTime: Long,
    val timezone: Int,
    val timestamp: Long,

    /**
     * Ошибка 3.
     * Здесь было обнаружено небезопасное хранение полного raw-ответа API в модели WeatherData.
     * В ответе API могут находиться данные о местоположении пользователя и другая служебная информация.
     * Чтобы решить эту проблему, я удалила поле rawApiData и оставила в модели только те данные,
     * которые действительно нужны для отображения погоды на экране.
     */
    
    var isFavorite: Boolean = false,
    var isSelected: Boolean = false
)