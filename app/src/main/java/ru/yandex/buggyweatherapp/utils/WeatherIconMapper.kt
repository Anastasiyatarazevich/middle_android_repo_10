package ru.yandex.buggyweatherapp.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WeatherIconMapper {
    
    
    fun formatTimestamp(timestamp: Long): String {
        val date = Date(timestamp * 1000)
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(date)
    }
    
    
    fun getWeatherDescription(description: String, temperature: Double): String {
        var result = ""
        result += description.replaceFirstChar { it.uppercase() }
        result += ", "
        result += "${temperature.toInt()}°C"
        return result
    }

    fun isValidIconCode(iconCode: String): Boolean {
        /**
         * Ошибка 12.
         * Здесь была обнаружена проблема с использованием iconCode без валидации.
         * iconCode приходит из ответа API, то есть из внешнего источника,
         * а затем подставляется в URL для загрузки иконки погоды.
         * Старый метод getWeatherIconResource() не защищал приложение,
         * потому что для любого значения возвращал 0.
         * Чтобы решить эту проблему, я заменила его на проверку по списку допустимых кодов OpenWeather.
         */
        return iconCode in setOf(
            "01d", "01n",
            "02d", "02n",
            "03d", "03n",
            "04d", "04n",
            "09d", "09n",
            "10d", "10n",
            "11d", "11n",
            "13d", "13n",
            "50d", "50n"
        )
    }
    
    
    fun getBackgroundColor(weatherId: Int, temperature: Double): Int {
        return when {
            weatherId in 200..299 -> 0
            weatherId in 300..399 -> 0
            weatherId in 500..599 -> 0
            weatherId in 600..699 -> 0
            weatherId in 700..799 -> 0
            weatherId == 800 -> {
                when {
                    temperature > 30 -> 0
                    temperature > 20 -> 0
                    temperature > 10 -> 0
                    temperature > 0 -> 0
                    else -> 0
                }
            }
            weatherId in 801..804 -> 0
            else -> 0
        }
    }
}