package com.example.trafficprediction.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trafficprediction.data.ThemePreference
import com.example.trafficprediction.ui.viewmodels.ThemeViewModel

private val DarkColorScheme = darkColorScheme(
    primary = MatteTurquoiseDark,
    secondary = MatteBlueDark,
    tertiary = SoftGreenDark,
    background = DarkSlateGreyBackground,
    surface = DarkGreySurface,
    onPrimary = LightGreyText,
    onSecondary = LightGreyText,
    onTertiary = LightGreyText,
    onBackground = LightGreyText,
    onSurface = LightGreyText
)

private val LightColorScheme = lightColorScheme(
    primary = SoftTurquoiseLight,
    secondary = SkyBlueLight,
    tertiary = PaleGreenLight,
    background = LightGreyBackground,
    surface = WhiteSurface,
    onPrimary = DarkGreyText,
    onSecondary = DarkGreyText,
    onTertiary = DarkGreyText,
    onBackground = DarkGreyText,
    onSurface = DarkGreyText
)

@Composable
fun TrafficPredictionTheme(
    themeViewModel: ThemeViewModel = viewModel(), // ViewModel'i enjekte et
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Dinamik renk şimdilik kapalı kalsın
    content: @Composable () -> Unit
) {
    val themePreference by themeViewModel.themePreference.collectAsState()

    val useDarkTheme = when (themePreference) {
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
        ThemePreference.SYSTEM_DEFAULT -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
