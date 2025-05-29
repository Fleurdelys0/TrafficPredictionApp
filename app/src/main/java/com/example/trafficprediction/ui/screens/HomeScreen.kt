package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf // EKLENDİ
import androidx.compose.runtime.remember // EKLENDİ
import androidx.compose.runtime.setValue // EKLENDİ (by remember için)
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Dehaze
import androidx.compose.material.icons.filled.SevereCold
import androidx.compose.material.icons.filled.Thunderstorm
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.clickable // Tıklanabilirlik için
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trafficprediction.Screen
import com.example.trafficprediction.data.TrafficPredictionLog
import com.example.trafficprediction.data.TrafficRepository // RouteTrafficInfo için
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme
import com.example.trafficprediction.ui.viewmodels.AuthViewModel
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import androidx.lifecycle.viewmodel.compose.viewModel // viewModel() için

@Composable
fun HomeScreen(
    navController: NavController,
    trafficViewModel: TrafficViewModel,
    authViewModel: AuthViewModel = viewModel() // AuthViewModel'i enjekte et
) {
    val historyList by trafficViewModel.predictionHistory.collectAsState()
    val isLoadingHistory by trafficViewModel.historyLoading.collectAsState()
    val istanbulTrafficSummary by trafficViewModel.istanbulTrafficSummary.collectAsState()
    val isSummaryLoading by trafficViewModel.isSummaryLoading.collectAsState()

    // Hava durumu state'leri
    val weatherCondition by trafficViewModel.weatherCondition.collectAsState()
    val weatherTemperature by trafficViewModel.weatherTemperature.collectAsState()
    val isFetchingWeather by trafficViewModel.isFetchingWeather.collectAsState()
    val detailedWeatherInfo by trafficViewModel.detailedWeatherInfo.collectAsState() // Detaylı bilgi
    var showDetailedWeatherDialog by remember { mutableStateOf(false) } // Dialog gösterme state'i

    // Kullanıcı adı state'i
    val authState by authViewModel.authUiState.collectAsState()
    val displayName = when (val state = authState) {
        is com.example.trafficprediction.ui.viewmodels.AuthUiState.SignedIn -> state.user.displayName ?: "User"
        else -> "User"
    }

    LaunchedEffect(key1 = trafficViewModel) {
        trafficViewModel.loadPredictionHistory()
        trafficViewModel.loadIstanbulTrafficSummary()
        trafficViewModel.fetchCurrentWeatherForHomeScreen() // Hava durumunu yükle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Traffic Dashboard",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp) // Biraz azaltıldı
        )

        // Karşılama ve Hava Durumu Mesajı
        Row( // Bu dış Row, tüm satırı kaplar ve içeriğini ortalar
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clickable(enabled = detailedWeatherInfo != null && !isFetchingWeather) {
                    if (detailedWeatherInfo != null) {
                        showDetailedWeatherDialog = true
                    }
                },
            horizontalArrangement = Arrangement.Center, // İçeriği (yani iç Row'u) ortala
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row( // Bu iç Row, ikonu ve metinleri yan yana dizer
                verticalAlignment = Alignment.CenterVertically
            ) {
                val weatherIcon = getWeatherIcon(weatherCondition)
                if (weatherIcon != null && !isFetchingWeather) {
                    Icon(
                        imageVector = weatherIcon,
                        contentDescription = weatherCondition,
                        modifier = Modifier.size(36.dp).padding(end = 8.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column(horizontalAlignment = Alignment.Start) { // Metinleri sola yasla (ikonla hizalı olması için)
                    Text(
                        text = "Welcome $displayName",
                        style = MaterialTheme.typography.headlineSmall
                        // textAlign Center idi, Start'a çevrildi, Row ortalayacak
                    )
                    val weatherString = if (isFetchingWeather) {
                        "Loading weather..."
                    } else {
                        weatherCondition?.let { condition ->
                            weatherTemperature?.let { temp ->
                                "Today: ${condition.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} (${String.format("%.0f", temp)}°C)"
                            } ?: "Today: ${condition.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
                        } ?: "Weather data not available."
                    }
                    Text(
                        text = weatherString,
                        style = MaterialTheme.typography.bodyLarge
                        // textAlign Center idi, Start'a çevrildi
                    )
                }
            }
        }
        // ---

        // Detaylı Hava Durumu Dialogu
        if (showDetailedWeatherDialog) {
            DetailedWeatherDialog(
                weatherInfo = detailedWeatherInfo,
                onDismissRequest = { showDetailedWeatherDialog = false }
            )
        }

        TrafficSummaryCard(summary = istanbulTrafficSummary, isLoading = isSummaryLoading)

        Spacer(modifier = Modifier.height(24.dp)) // Biraz azaltıldı

        Button(
            onClick = { navController.navigate(Screen.Map.route) },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Icon(Icons.Filled.Map, contentDescription = "Predict", modifier = Modifier.size(ButtonDefaults.IconSize))
            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
            Text("Get Traffic Prediction")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Recent Predictions",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isLoadingHistory && historyList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (historyList.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No recent predictions found.")
                    }
                }
            } else {
                items(historyList.take(3)) { logEntry ->
                    RecentPredictionItem(
                        logEntry = logEntry,
                        onClick = {
                            trafficViewModel.selectLogForDetail(logEntry)
                            navController.navigate(Screen.PredictionDetail.route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun TrafficSummaryCard(
    summary: List<TrafficRepository.RouteTrafficInfo>,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f) // Biraz farklı bir renk
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Istanbul Traffic Hotspots", // Başlık güncellendi
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (summary.isEmpty()) {
                Text("Could not load traffic summary for Istanbul at the moment.", style = MaterialTheme.typography.bodyMedium)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    summary.forEach { routeInfo ->
                        RouteTrafficStatusItem(routeInfo = routeInfo)
                    }
                }
            }
        }
    }
}

@Composable
fun RouteTrafficStatusItem(routeInfo: TrafficRepository.RouteTrafficInfo) {
    val durationNoTrafficSec = routeInfo.durationInSeconds
    val durationWithTrafficSec = routeInfo.durationInTrafficInSeconds

    val trafficStatusText: String
    val statusColor: Color

    if (durationNoTrafficSec == null || durationWithTrafficSec == null || durationNoTrafficSec == 0) {
        trafficStatusText = "Data N/A"
        statusColor = Color.Gray
    } else {
        val delayRatio = durationWithTrafficSec.toDouble() / durationNoTrafficSec.toDouble()
        val delayMinutes = (durationWithTrafficSec - durationNoTrafficSec) / 60

        trafficStatusText = when {
            delayRatio > 1.8 -> "Heavy (+${delayMinutes}min)"
            delayRatio > 1.3 -> "Moderate (+${delayMinutes}min)"
            else -> "Light"
        }
        statusColor = when {
            delayRatio > 1.8 -> Color.Red.copy(alpha = 0.9f)
            delayRatio > 1.3 -> Color(0xFFFFA500).copy(alpha = 0.9f) // Orange
            else -> Color.Green.copy(alpha = 0.8f)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = routeInfo.routeName,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier.size(12.dp),
            shape = CircleShape, // Nokta şeklinde
            color = statusColor
        ) {}
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = trafficStatusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RecentPredictionItem(
    logEntry: TrafficPredictionLog,
    onClick: () -> Unit
) {
    val cardColor = when (logEntry.estimatedCondition) {
        "Heavy Traffic", "Heavy" -> Color.Red.copy(alpha = 0.25f)
        "Moderate Traffic", "Moderate" -> Color(0xFFFFA500).copy(alpha = 0.3f)
        "Light Traffic", "Light" -> Color.Green.copy(alpha = 0.25f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors( containerColor = cardColor )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "From: ${logEntry.startAddress.ifBlank { "N/A" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Text(
                    text = "To: ${logEntry.endAddress.ifBlank { "N/A" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = logEntry.estimatedCondition ?: "N/A",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenRichPreview() {
    TrafficPredictionTheme {
        val navController = rememberNavController()
        // AuthViewModel ve TrafficViewModel'in sahte implementasyonları veya mock'ları gerekebilir
        // Şimdilik basit bir Column içinde gösterelim
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("HomeScreen Preview", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))

            // Örnek Karşılama Mesajı
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Sunny",
                    modifier = Modifier.size(36.dp).padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Welcome Preview User",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Today is Sunny (25°C)",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }


            TrafficSummaryCard(summary = listOf(
                TrafficRepository.RouteTrafficInfo("15 Temmuz Köprüsü", 600, 1200, "Yoğun trafik"),
                TrafficRepository.RouteTrafficInfo("FSM Köprüsü", 500, 700, "Orta trafik"),
                TrafficRepository.RouteTrafficInfo("E-5 Avcılar", 1800, 2000, "Akıcı trafik")
            ), isLoading = false)
            Spacer(Modifier.height(16.dp))
            RecentPredictionItem(
                logEntry = TrafficPredictionLog(
                    startAddress = "Ankara Kızılay",
                    endAddress = "İstanbul Taksim",
                    estimatedCondition = "Heavy Traffic"
                ),
                onClick = {}
            )
        }
    }
}
