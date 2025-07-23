package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.trafficprediction.Screen
import com.example.trafficprediction.data.TrafficPredictionLog
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun PredictionsScreen(
    navController: NavController, // NavController added.
    trafficViewModel: TrafficViewModel // ViewModel is now provided from outside.
) {
    // We get the history states from the ViewModel.
    val historyList by trafficViewModel.predictionHistory.collectAsState()
    val isLoading by trafficViewModel.historyLoading.collectAsState()
    val errorMessage by trafficViewModel.historyErrorMessage.collectAsState()

    // We load the history when the screen first opens or when the ViewModel changes.
    LaunchedEffect(Unit) { // Using Unit as a key runs this only on initial composition.
        trafficViewModel.loadPredictionHistory()
    }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var logToDelete by remember { mutableStateOf<TrafficPredictionLog?>(null) }

    if (showDeleteDialog && logToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Deletion") },
            text = { Text("Are you sure you want to delete this prediction from your history?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        trafficViewModel.deletePredictionLog(logToDelete!!)
                        showDeleteDialog = false
                        logToDelete = null
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Prediction History",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Loading State
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        // Error State
        else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "Could not load history.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { trafficViewModel.loadPredictionHistory() }) {
                        Text("Retry")
                    }
                }
            }
        }
        // Success State (even if the list is empty)
        else {
            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No prediction history found.")
                }
            } else {
                // LazyColumn for displaying the history.
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between items.
                ) {
                    items(historyList) { logEntry -> // Process the list.
                        PredictionHistoryItem(
                            logEntry = logEntry,
                            onClick = {
                                trafficViewModel.selectLogForDetail(logEntry)
                                navController.navigate(Screen.PredictionDetail.route)
                            },
                            onDeleteRequest = {
                                logToDelete = logEntry
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
}

// Composable for displaying each item in the history list.
@Composable
fun PredictionHistoryItem(
    logEntry: TrafficPredictionLog,
    onClick: () -> Unit, // Click callback added.
    onDeleteRequest: () -> Unit // Callback for delete request added.
) {
    val cardColor = when (logEntry.estimatedCondition) {
        "Heavy" -> Color.Red.copy(alpha = 0.1f)
        "Moderate" -> Color(0xFFFFA500).copy(alpha = 0.15f) // Orange color for moderate.
        "Light" -> Color.Green.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    // Date formatter.
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Make the card clickable.
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // Increased padding.
            // Addresses.
            Text(
                text = "From: ${logEntry.startAddress.ifBlank { "N/A" }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "To: ${logEntry.endAddress.ifBlank { "N/A" }}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            // Time and Day.
            Text(
                text = "Time: ${logEntry.requestedTime} (${logEntry.requestedDayType})",
                style = MaterialTheme.typography.bodySmall
            )
            // Prediction.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) { // Column for the left side.
                    Text(
                        text = "Prediction: ${logEntry.estimatedCondition ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        // Format the speed.
                        text = "Speed: ${
                            String.format(
                                "%.1f",
                                logEntry.predictedSpeed ?: 0.0
                            )
                        } u/hr",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onDeleteRequest) { // Delete button added.
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            // Timestamp.
            logEntry.timestamp?.toDate()?.let { date ->
                Text(
                    text = "Saved: ${dateFormatter.format(date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f), // Slightly faded.
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PredictionsScreenPreview() {
    // We can create mock data for the Preview.
    // A mock NavController might be needed to provide it in the Preview,
    // or we could adjust the Preview of PredictionsScreen to be called without a NavController.
    // For now, let's comment out this Preview or call it with a simple NavController.
    // val navController = rememberNavController() // This might not work inside a Preview.
    // val previewViewModel = TrafficViewModel(Application()) // ViewModel is provided externally, so a mock might be needed in Preview.

    TrafficPredictionTheme {
        // PredictionsScreen(navController = rememberNavController(), trafficViewModel = viewModel()) // Temporary for Preview.
        // For a simple list display:
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(1) {
                PredictionHistoryItem(
                    logEntry = TrafficPredictionLog(
                        userId = "previewUser",
                        startAddress = "Start Address Preview",
                        endAddress = "End Address Preview",
                        requestedTime = "8 AM",
                        requestedDayType = "Weekday",
                        estimatedCondition = "Moderate",
                        predictedSpeed = 50.0,
                        timestamp = com.google.firebase.Timestamp.now()
                    ),
                    onClick = {},
                    onDeleteRequest = {} // Can be left empty for Preview.
                )
            }
        }
    }
}
