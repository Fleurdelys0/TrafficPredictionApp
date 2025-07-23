package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.trafficprediction.Screen
import com.example.trafficprediction.data.TrafficPredictionLog
import com.example.trafficprediction.data.TrafficRepository
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme
import com.example.trafficprediction.ui.viewmodels.AuthViewModel
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    trafficViewModel: TrafficViewModel,
    authViewModel: AuthViewModel = viewModel() // We inject AuthViewModel here.
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val historyList by trafficViewModel.predictionHistory.collectAsState()
    val isLoadingHistory by trafficViewModel.historyLoading.collectAsState()
    val istanbulTrafficSummary by trafficViewModel.istanbulTrafficSummary.collectAsState()
    val isSummaryLoading by trafficViewModel.isSummaryLoading.collectAsState()

    // Weather states for the initial display (uses start/end or default location)
    val initialWeatherCondition by trafficViewModel.weatherCondition.collectAsState()
    val initialWeatherTemperature by trafficViewModel.weatherTemperature.collectAsState()
    val isFetchingInitialWeather by trafficViewModel.isFetchingWeather.collectAsState()

    // States for current device location weather dialog
    val currentLocationWeatherDetails by trafficViewModel.currentLocationWeatherDetails.collectAsState()
    val isFetchingCurrentLocationWeather by trafficViewModel.isFetchingCurrentLocationWeather.collectAsState()
    var showDetailedWeatherDialog by remember { mutableStateOf(false) }

    // User name state.
    val currentUser by authViewModel.currentUser.observeAsState()
    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "User"

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (isGranted) {
                trafficViewModel.updateCurrentUserDeviceLocation { latLng ->
                    if (latLng != null) {
                        trafficViewModel.fetchWeatherForCurrentUserDeviceLocation()
                    }
                    showDetailedWeatherDialog = true
                }
            } else {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Location permission denied. Cannot show current location weather.")
                }
            }
        }
    )

    LaunchedEffect(key1 = Unit) {
        trafficViewModel.loadPredictionHistory()
        trafficViewModel.loadIstanbulTrafficSummary()
        trafficViewModel.fetchCurrentWeatherForHomeScreen()
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .clickable {
                        if (hasLocationPermission) {
                            trafficViewModel.updateCurrentUserDeviceLocation { latLng ->
                                if (latLng != null) {
                                    trafficViewModel.fetchWeatherForCurrentUserDeviceLocation()
                                }
                                showDetailedWeatherDialog = true
                            }
                        } else {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        }
                    },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val displayWeatherCondition = if (showDetailedWeatherDialog && currentLocationWeatherDetails != null) {
                        currentLocationWeatherDetails?.weather?.firstOrNull()?.main
                    } else {
                        initialWeatherCondition
                    }
                    val displayTemperature = if (showDetailedWeatherDialog && currentLocationWeatherDetails != null) {
                        currentLocationWeatherDetails?.main?.temp
                    } else {
                        initialWeatherTemperature
                    }
                    val isLoadingDisplayWeather = if (showDetailedWeatherDialog) isFetchingCurrentLocationWeather else isFetchingInitialWeather
                    val weatherIcon = getWeatherIcon(displayWeatherCondition)

                    if (weatherIcon != null && !isLoadingDisplayWeather) {
                        Icon(
                            imageVector = weatherIcon,
                            contentDescription = displayWeatherCondition,
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else if (isLoadingDisplayWeather) {
                         Icon(
                            imageVector = Icons.Filled.MyLocation,
                            contentDescription = "Fetching current location weather",
                            modifier = Modifier
                                .size(36.dp)
                                .padding(end = 8.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.Start) {
                        Text(
                            text = "Welcome $displayName",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        val weatherString = if (isLoadingDisplayWeather) {
                            if (showDetailedWeatherDialog) "Loading current location weather..." else "Loading weather..."
                        } else {
                            displayWeatherCondition?.let { condition ->
                                displayTemperature?.let { temp ->
                                    "Tap for Current: ${condition.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} (${String.format("%.0f",temp)}°C)"
                                } ?: "Tap for Current: ${condition.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}"
                            } ?: "Tap to get current location weather."
                        }
                        Text(
                            text = weatherString,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            if (showDetailedWeatherDialog) {
                DetailedWeatherDialog(
                    weatherInfo = currentLocationWeatherDetails,
                    onDismissRequest = { showDetailedWeatherDialog = false }
                )
            }

            TrafficSummaryCard(summary = istanbulTrafficSummary, isLoading = isSummaryLoading)

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { navController.navigate(Screen.Map.route) },
                shape = RoundedCornerShape(50),
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(56.dp),
                contentPadding = PaddingValues(
                    horizontal = 24.dp,
                    vertical = 12.dp
                )
            ) {
                Icon(
                    Icons.Filled.Map,
                    contentDescription = "Predict",
                    modifier = Modifier.size(ButtonDefaults.IconSize)
                )
                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                Text("Get Traffic Prediction", style = MaterialTheme.typography.labelLarge)
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
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (historyList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

@Composable
fun TrafficSummaryCard(
    summary: List<TrafficRepository.RouteTrafficInfo>,
    isLoading: Boolean
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Istanbul Traffic Hotspots",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (summary.isEmpty()) {
                Text(
                    "Could not load traffic summary for Istanbul at the moment.",
                    style = MaterialTheme.typography.bodyMedium
                )
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
            shape = CircleShape,
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
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardColor.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "From: ${logEntry.startAddress.ifBlank { "N/A" }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To: ${logEntry.endAddress.ifBlank { "N/A" }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = logEntry.estimatedCondition ?: "N/A",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            logEntry.timestamp?.let { firebaseTimestamp ->
                val sdf = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
                Text(
                    text = "Predicted: ${sdf.format(firebaseTimestamp.toDate())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenRichPreview() {
    TrafficPredictionTheme {
        val navController = rememberNavController()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("HomeScreen Preview", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "Sunny",
                    modifier = Modifier
                        .size(36.dp)
                        .padding(end = 8.dp),
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
            TrafficSummaryCard(
                summary = listOf(
                    TrafficRepository.RouteTrafficInfo(
                        "15 Temmuz Köprüsü",
                        600,
                        1200,
                        "Yoğun trafik"
                    ),
                    TrafficRepository.RouteTrafficInfo("FSM Köprüsü", 500, 700, "Orta trafik"),
                    TrafficRepository.RouteTrafficInfo("E-5 Avcılar", 1800, 2000, "Akıcı trafik")
                ), isLoading = false
            )
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
