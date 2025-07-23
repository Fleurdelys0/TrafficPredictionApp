package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
        // If there's no weather information, we don't show the dialog or we could show an error message.
        return
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(28.dp), // Large corner radius, Material 3 style.
            modifier = Modifier, // .fillMaxWidth(0.9f) // Optional: fill width.
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp), // Elevation, Material 3 style.
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface // Material 3 color system.
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = weatherInfo.name ?: "Weather Details",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                weatherInfo.weather?.firstOrNull()?.let { weatherDesc ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        getWeatherIcon(weatherDesc.main)?.let { icon ->
                            Icon(
                                imageVector = icon,
                                contentDescription = weatherDesc.description,
                                modifier = Modifier.size(48.dp), // Adjusted size.
                                tint = MaterialTheme.colorScheme.primary // Theme color.
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            text = "${weatherDesc.description?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                weatherInfo.main?.temp?.let { temp ->
                    Text(
                        "Temperature: ${String.format("%.1f", temp)}°C",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                weatherInfo.main?.feelsLike?.let { feelsLike ->
                    Text(
                        "Feels Like: ${String.format("%.1f", feelsLike)}°C",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                weatherInfo.main?.humidity?.let { humidity ->
                    Text(
                        "Humidity: $humidity%",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                weatherInfo.wind?.speed?.let { windSpeed ->
                    Text(
                        "Wind: ${String.format("%.1f", windSpeed)} m/s",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                weatherInfo.main?.pressure?.let { pressure ->
                    Text(
                        "Pressure: $pressure hPa",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                weatherInfo.visibility?.let { visibility ->
                    Text(
                        "Visibility: ${visibility / 1000} km",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                weatherInfo.sys?.sunrise?.let { sunrise ->
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val sunriseDate =
                        Date(sunrise * 1000L + (weatherInfo.timezone?.times(1000L) ?: 0L))
                    sdf.timeZone = TimeZone.getDefault()
                    Text(
                        "Sunrise: ${sdf.format(sunriseDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                weatherInfo.sys?.sunset?.let { sunset ->
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    val sunsetDate =
                        Date(sunset * 1000L + (weatherInfo.timezone?.times(1000L) ?: 0L))
                    sdf.timeZone = TimeZone.getDefault()
                    Text(
                        "Sunset: ${sdf.format(sunsetDate)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = onDismissRequest,
                    shape = RoundedCornerShape(16.dp) // Button shape, Material 3 style.
                ) {
                    Text("Close", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
