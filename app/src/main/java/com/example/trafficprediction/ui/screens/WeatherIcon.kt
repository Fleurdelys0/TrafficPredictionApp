package com.example.trafficprediction.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.SevereCold
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Returns an appropriate Material Design icon based on the main weather condition
 * received from the OpenWeatherMap API.
 *
 * Reference: https://openweathermap.org/weather-conditions
 */
@Composable
fun getWeatherIcon(weatherCondition: String?): ImageVector? {
    return when (weatherCondition?.lowercase()) {
        "clear" -> Icons.Default.WbSunny
        "clouds" -> Icons.Default.Cloud
        "rain" -> Icons.Default.Umbrella
        "drizzle" -> Icons.Default.Umbrella // We use the rain icon for drizzle as well.
        "thunderstorm" -> Icons.Default.Thunderstorm
        "snow" -> Icons.Default.SevereCold
        "mist", "smoke", "haze", "dust", "fog", "sand", "ash", "squall", "tornado" -> Icons.Default.Dehaze
        // We can add other conditions (Dehaze can be used for others in the Atmosphere group).
        else -> null // No icon for unknown or null conditions, or we could use a default icon.
        // else -> Icons.Default.HelpOutline // Alternatively, an icon for unknown conditions.
    }
}
