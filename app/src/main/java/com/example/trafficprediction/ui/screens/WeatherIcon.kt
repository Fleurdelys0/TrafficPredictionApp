package com.example.trafficprediction.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud // Bulutlu
import androidx.compose.material.icons.filled.Dehaze // Sis, Pus, Duman vb.
import androidx.compose.material.icons.filled.HelpOutline // Bilinmeyen durum için
import androidx.compose.material.icons.filled.SevereCold // Kar için
import androidx.compose.material.icons.filled.Thunderstorm // Gök gürültülü fırtına
import androidx.compose.material.icons.filled.Umbrella // Yağmur, Çisenti için
import androidx.compose.material.icons.filled.WbSunny // Güneşli, Açık
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * OpenWeatherMap API'sinden gelen ana hava durumu koşuluna göre
 * uygun bir Material Design ikonu döndürür.
 *
 * Referans: https://openweathermap.org/weather-conditions
 */
@Composable
fun getWeatherIcon(weatherCondition: String?): ImageVector? {
    return when (weatherCondition?.lowercase()) {
        "clear" -> Icons.Default.WbSunny
        "clouds" -> Icons.Default.Cloud
        "rain" -> Icons.Default.Umbrella
        "drizzle" -> Icons.Default.Umbrella // Çisenti için de yağmur ikonu
        "thunderstorm" -> Icons.Default.Thunderstorm
        "snow" -> Icons.Default.SevereCold
        "mist", "smoke", "haze", "dust", "fog", "sand", "ash", "squall", "tornado" -> Icons.Default.Dehaze
        // Diğer durumlar eklenebilir (Atmosphere grubundaki diğerleri için Dehaze kullanılabilir)
        else -> null // Bilinmeyen veya null durumlar için ikon yok veya varsayılan bir ikon
        // else -> Icons.Default.HelpOutline // Alternatif olarak bilinmeyen için bir ikon
    }
}
