package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trafficprediction.data.TrafficPredictionLog
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionDetailScreen(
    navController: NavController,
    trafficViewModel: TrafficViewModel // ViewModel artık dışarıdan sağlanıyor
) {
    val selectedLog by trafficViewModel.selectedLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prediction Detail") },
                navigationIcon = {
                    IconButton(onClick = {
                        trafficViewModel.clearSelectedLog() // Seçili logu temizle
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (selectedLog == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No log selected or an error occurred.")
                // Opsiyonel: Geri butonu veya otomatik geri yönlendirme
            }
        } else {
            selectedLog?.let { log ->
                PredictionDetailContent(log = log, modifier = Modifier.padding(paddingValues))
            }
        }
    }
}

@Composable
fun PredictionDetailContent(log: TrafficPredictionLog, modifier: Modifier = Modifier) {
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // DetailItem("Log ID:", log.logId ?: "N/A") // Kullanıcı tarafından gizlenmesi istendi
        // DetailItem("User ID:", log.userId ?: "N/A") // Kullanıcı tarafından gizlenmesi istendi
        // Spacer(modifier = Modifier.height(8.dp)) // Yukarıdakiler kaldırıldığı için bu spacer'a gerek kalmayabilir, duruma göre ayarlanır.

        Text("Route Information", style = MaterialTheme.typography.titleMedium)
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        DetailItem("Start Address:", log.startAddress)
        DetailItem("End Address:", log.endAddress)
        DetailItem("Start Coordinates:", "Lat: ${log.startLat ?: "N/A"}, Lng: ${log.startLng ?: "N/A"}")
        DetailItem("End Coordinates:", "Lat: ${log.endLat ?: "N/A"}, Lng: ${log.endLng ?: "N/A"}")
        Spacer(modifier = Modifier.height(8.dp))

        Text("Request Parameters", style = MaterialTheme.typography.titleMedium)
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        DetailItem("Requested Time:", log.requestedTime)
        DetailItem("Requested Day Type:", log.requestedDayType)
        Spacer(modifier = Modifier.height(8.dp))

        Text("Prediction Result", style = MaterialTheme.typography.titleMedium)
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        DetailItem("Estimated Condition:", log.estimatedCondition ?: "N/A")
        DetailItem("Predicted Speed:", "${String.format("%.1f", log.predictedSpeed ?: 0.0)} km/h") // Birim km/h olarak güncellendi
        // Tahmini süreyi göster
        val formattedTime = log.estimatedTravelTimeMinutes?.let { String.format("%.0f min", it) } ?: "N/A"
        DetailItem("Est. Travel Time:", formattedTime)
        // Segment mesafesini de gösterelim (TrafficPredictionLog'a eklenmişti)
        val formattedDistance = log.segmentDistanceKm?.let { String.format("%.1f km", it) } ?: "N/A"
        DetailItem("Segment Distance:", formattedDistance)

        log.timestamp?.toDate()?.let { date ->
            DetailItem("Timestamp:", dateFormatter.format(date))
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.4f) // Ağırlıkla etiket genişliğini ayarla
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f)
        )
    }
}
