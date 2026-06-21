package ru.yandex.buggyweatherapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import ru.yandex.buggyweatherapp.ui.screens.WeatherScreen
import ru.yandex.buggyweatherapp.ui.theme.BuggyWeatherAppTheme
import ru.yandex.buggyweatherapp.viewmodel.WeatherViewModel

class MainActivity : ComponentActivity() {

    /**
     * Ошибка 6.
     * Здесь ViewModel создавалась вручную через WeatherViewModel().
     * Такой объект не привязан к ViewModelStore Activity и не управляется жизненным циклом Android.
     * Из-за этого состояние экрана может теряться при смене конфигурации,
     * а ресурсы ViewModel могут очищаться некорректно.
     * Чтобы решить эту проблему, я использую делегат by viewModels(),
     * который создаёт ViewModel через стандартный механизм Android.
     */
    private val weatherViewModel: WeatherViewModel by viewModels()

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        /**
         * Ошибка 7.
         * Здесь приложение запрашивало сразу два разрешения на геолокацию:
         * ACCESS_FINE_LOCATION и ACCESS_COARSE_LOCATION.
         * Для приложения погоды точная геолокация пользователя не обязательна,
         * потому что для получения прогноза достаточно примерного местоположения.
         * Чтобы решить эту проблему, я оставила запрос только ACCESS_COARSE_LOCATION
         * и убрала избыточный запрос точной геолокации. Также убрала ненужное разрешение в AndroidManifest.
         */
        if (isGranted) {
            weatherViewModel.fetchCurrentLocationWeather()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val hasFineLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasCoarseLocation) {
            locationPermissionRequest.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        enableEdgeToEdge()

        setContent {
            BuggyWeatherAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeatherScreen(
                        viewModel = weatherViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()

    }
}

@Preview(showBackground = true)
@Composable
fun WeatherAppPreview() {
    BuggyWeatherAppTheme {

        Text("Weather App Preview")
    }
}