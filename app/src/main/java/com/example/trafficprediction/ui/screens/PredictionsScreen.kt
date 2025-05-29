package com.example.trafficprediction.ui.screens

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items // items fonksiyonu için
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete // Silme ikonu için
import androidx.compose.material.icons.filled.Warning // Hata ikonu için
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.trafficprediction.Screen // Navigasyon için
import com.example.trafficprediction.data.TrafficPredictionLog // Log data class'ı
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import java.text.SimpleDateFormat // Tarih formatlama için
import java.util.* // Date ve Locale için

@Composable
fun PredictionsScreen(
    navController: NavController, // NavController eklendi
    trafficViewModel: TrafficViewModel // ViewModel artık dışarıdan sağlanıyor
) {
    // ViewModel'den geçmiş state'lerini al
    val historyList by trafficViewModel.predictionHistory.collectAsState()
    val isLoading by trafficViewModel.historyLoading.collectAsState()
    val errorMessage by trafficViewModel.historyErrorMessage.collectAsState()

    // Ekran ilk açıldığında veya ViewModel değiştiğinde geçmişi yükle
    LaunchedEffect(Unit) { // Key olarak Unit, sadece ilk açılışta çalıştırır
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

        // Yükleme Durumu
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        // Hata Durumu
        else if (errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
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
        // Başarılı Durum (Liste Boş Olsa Bile)
        else {
            if (historyList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No prediction history found.")
                }
            } else {
                // Geçmişi Listeleyen LazyColumn
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp) // Elemanlar arası boşluk
                ) {
                    items(historyList) { logEntry -> // Listeyi işle
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

// Geçmiş listesindeki her bir elemanı gösteren Composable
@Composable
fun PredictionHistoryItem(
    logEntry: TrafficPredictionLog,
    onClick: () -> Unit, // Tıklama callback'i eklendi
    onDeleteRequest: () -> Unit // Silme isteği için callback eklendi
) {
    val cardColor = when (logEntry.estimatedCondition) {
        "Heavy" -> Color.Red.copy(alpha = 0.1f)
        "Moderate" -> Color(0xFFFFA500).copy(alpha = 0.15f) // Orange
        "Light" -> Color.Green.copy(alpha = 0.1f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    // Tarih formatlayıcı
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // Kartı tıklanabilir yap
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { // Increased padding
            // Adresler
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
            // Zaman ve Gün
            Text(
                text = "Time: ${logEntry.requestedTime} (${logEntry.requestedDayType})",
                style = MaterialTheme.typography.bodySmall
            )
            // Tahmin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) { // Sol taraf için Column
                    Text(
                        text = "Prediction: ${logEntry.estimatedCondition ?: "N/A"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        // Hızı formatla
                        text = "Speed: ${String.format("%.1f", logEntry.predictedSpeed ?: 0.0)} u/hr",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                IconButton(onClick = onDeleteRequest) { // Silme butonu eklendi
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete Log",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }
            // Zaman Damgası
            logEntry.timestamp?.toDate()?.let { date ->
                Text(
                    text = "Saved: ${dateFormatter.format(date)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = LocalContentColor.current.copy(alpha = 0.7f), // Biraz soluk
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PredictionsScreenPreview() {
    // Preview için sahte veri oluşturabiliriz
    // NavController'ı Preview'da sağlamak için sahte bir NavController gerekebilir
    // veya PredictionsScreen'in Preview'ını NavController olmadan çağıracak şekilde düzenleyebiliriz.
    // Şimdilik bu Preview'ı yorum satırına alalım veya basit bir NavController ile çağıralım.
    // val navController = rememberNavController() // Bu Preview içinde çalışmayabilir.
    // val previewViewModel = TrafficViewModel(Application()) // ViewModel dışarıdan sağlanacağı için Preview'da mock gerekebilir

    TrafficPredictionTheme {
        // PredictionsScreen(navController = rememberNavController(), trafficViewModel = viewModel()) // Preview için geçici
        // Basit bir liste gösterimi için:
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
                    onDeleteRequest = {} // Preview için boş bırakılabilir
                )
            }
        }
    }
}
