package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
// import androidx.compose.ui.window.DialogProperties // KALDIRILDI
import com.example.trafficprediction.network.CurrentWeatherResponse
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun DetailedWeatherDialog(
    weatherInfo: CurrentWeatherResponse?,
    onDismissRequest: () -> Unit
) {
    if (weatherInfo == null) {
        // Eğer hava durumu bilgisi yoksa dialogu gösterme veya bir hata mesajı göster
        return
    }

    Dialog(onDismissRequest = onDismissRequest) { // DialogProperties kaldırıldı
        Card(
            // modifier = Modifier.fillMaxWidth(0.9f), // fillMaxWidth kaldırıldı, içeriğe göre boyutlansın
            // Card'ın etrafındaki boşluk için padding eklenebilir veya Dialog'un kendi varsayılanı kullanılabilir.
            // Şimdilik Card'a doğrudan padding vermeyelim, içindeki Column'a verelim.
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(16.dp), // Card içindeki asıl içerik için padding
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = weatherInfo.name ?: "Weather Details",
                        style = MaterialTheme.typography.titleLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    weatherInfo.weather?.firstOrNull()?.let { weatherDesc ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            getWeatherIcon(weatherDesc.main)?.let { icon ->
                                Icon(
                                    imageVector = icon,
                                    contentDescription = weatherDesc.description,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = "${weatherDesc.description?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                    }

                    weatherInfo.main?.temp?.let { temp ->
                        Text(
                            "Temperature: ${String.format("%.1f", temp)}°C",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    weatherInfo.main?.feelsLike?.let { feelsLike ->
                        Text(
                            "Feels Like: ${String.format("%.1f", feelsLike)}°C",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    weatherInfo.main?.humidity?.let { humidity ->
                        Text("Humidity: $humidity%", style = MaterialTheme.typography.bodyLarge)
                    }
                    weatherInfo.wind?.speed?.let { windSpeed ->
                        Text(
                            "Wind: ${String.format("%.1f", windSpeed)} m/s",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    weatherInfo.main?.pressure?.let { pressure ->
                        Text("Pressure: $pressure hPa", style = MaterialTheme.typography.bodyLarge)
                    }
                    weatherInfo.visibility?.let { visibility ->
                        Text(
                            "Visibility: ${visibility / 1000} km",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    weatherInfo.sys?.sunrise?.let { sunrise ->
                        sdf.timeZone = TimeZone.getTimeZone("UTC") // API'den gelen zaman UTC
                        val sunriseDate =
                            Date(sunrise * 1000L + (weatherInfo.timezone?.times(1000L) ?: 0L))
                        sdf.timeZone = TimeZone.getDefault() // Lokal zamana çevir
                        Text(
                            "Sunrise: ${sdf.format(sunriseDate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    weatherInfo.sys?.sunset?.let { sunset ->
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        val sunsetDate =
                            Date(sunset * 1000L + (weatherInfo.timezone?.times(1000L) ?: 0L))
                        sdf.timeZone = TimeZone.getDefault()
                        Text(
                            "Sunset: ${sdf.format(sunsetDate)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = onDismissRequest) {
                        Text("Close")
                    }
                }
            }
        }
    }
