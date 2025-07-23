package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.trafficprediction.data.TrafficPredictionLog
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionDetailScreen(
    navController: NavController,
    trafficViewModel: TrafficViewModel // ViewModel is now provided from outside.
) {
    val selectedLog by trafficViewModel.selectedLog.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prediction Detail") },
                navigationIcon = {
                    IconButton(onClick = {
                        trafficViewModel.clearSelectedLog() // We clear the selected log.
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
                // Optional: We could add a back button or automatic redirection here.
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
        // Log ID and User ID are intentionally hidden as per user request.
        // Spacer might not be needed since the items above were removed; adjust as necessary.

        Text("Route Information", style = MaterialTheme.typography.titleMedium)
        Divider(modifier = Modifier.padding(vertical = 4.dp))
        DetailItem("Start Address:", log.startAddress)
        DetailItem("End Address:", log.endAddress)
        DetailItem(
            "Start Coordinates:",
            "Lat: ${log.startLat ?: "N/A"}, Lng: ${log.startLng ?: "N/A"}"
        )
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
        DetailItem(
            "Predicted Speed:",
            "${String.format("%.1f", log.predictedSpeed ?: 0.0)} km/h"
        ) // Unit updated to km/h.
        // We show the estimated travel time.
        val formattedTime =
            log.estimatedTravelTimeMinutes?.let { String.format("%.0f min", it) } ?: "N/A"
        DetailItem("Est. Travel Time:", formattedTime)
        // Let's also show the segment distance (it was added to TrafficPredictionLog).
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
            modifier = Modifier.weight(0.4f) // We adjust the label width using weight.
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(0.6f)
        )
    }
}
