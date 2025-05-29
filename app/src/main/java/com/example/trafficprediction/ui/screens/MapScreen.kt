package com.example.trafficprediction.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme
import com.example.trafficprediction.ui.viewmodels.DayTypeOptions
import com.example.trafficprediction.ui.viewmodels.TimeOptions
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

private enum class PointSelectionMode { START, END }

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    trafficViewModel: TrafficViewModel // ViewModel artık dışarıdan sağlanıyor
) {
    // --- ViewModel'den State'leri Al ---
    val startAddress by trafficViewModel.startAddress.collectAsState()
    val endAddress by trafficViewModel.endAddress.collectAsState()
    val selectedTime by trafficViewModel.selectedTime.collectAsState()
    val selectedDayType by trafficViewModel.selectedDayType.collectAsState()
    val predictionResult by trafficViewModel.predictionResult.collectAsState()
    // val isLoading by trafficViewModel.isLoading.collectAsState() // Genel isLoading yerine spesifik olanlar kullanılacak
    val isGeocoding by trafficViewModel.isGeocoding.collectAsState()
    val isFetchingPrediction by trafficViewModel.isFetchingPrediction.collectAsState()
    val errorMessage by trafficViewModel.errorMessage.collectAsState()
    // isLoading'a _isFetchingPlaces'i de dahil et
    val isFetchingPlaces by trafficViewModel.isFetchingPlaces.collectAsState()
    val isLoading = isGeocoding || isFetchingPrediction || isFetchingPlaces

    val startCoordinates by trafficViewModel.startCoordinates.collectAsState()
    val endCoordinates by trafficViewModel.endCoordinates.collectAsState()
    val routePolyline by trafficViewModel.routePolyline.collectAsState()
    val nearbyPlaces by trafficViewModel.nearbyPlaces.collectAsState() // YENİ: Yakındaki yerler
    // val currentUserLocation by trafficViewModel.currentUserLocation.collectAsState() // Kaldırıldı
    val scrollState = rememberScrollState()
    var pointSelectionMode by remember { mutableStateOf<PointSelectionMode?>(null) }
    var inputsExpanded by remember { mutableStateOf(true) }

    // Konum izni ile ilgili kısımlar kaldırıldı

    val isInputValid = remember(startAddress, endAddress) {
        startAddress.isNotBlank() && endAddress.isNotBlank()
    }

    val initialCameraPosition = LatLng(39.9334, 32.8597) // Ankara
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCameraPosition, 5f)
    }
    val mapUiSettings by remember { mutableStateOf(MapUiSettings(zoomControlsEnabled = true, myLocationButtonEnabled = false)) }
    // Trafik katmanını etkinleştir
    val mapProperties by remember { mutableStateOf(MapProperties(mapType = MapType.NORMAL, isMyLocationEnabled = false, isTrafficEnabled = true)) }
    val coroutineScope = rememberCoroutineScope()
    var isMapLoaded by remember { mutableStateOf(false) }
    val estimatedCondition by remember(predictionResult) {
        derivedStateOf {
            predictionResult?.substringAfter("Condition: ")?.substringBefore('\n')
        }
    }

    LaunchedEffect(startCoordinates, endCoordinates, isMapLoaded) { // currentUserLocation bağımlılığı kaldırıldı
        if (!isMapLoaded) return@LaunchedEffect
        try {
            val startLatLng = startCoordinates?.latitude?.let { lat -> startCoordinates?.longitude?.let { lng -> LatLng(lat, lng) } }
            val endLatLng = endCoordinates?.latitude?.let { lat -> endCoordinates?.longitude?.let { lng -> LatLng(lat, lng) } }

            if (startLatLng != null && endLatLng != null) {
                val bounds = LatLngBounds.builder().include(startLatLng).include(endLatLng).build()
                coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(bounds, 100)) }
            } else if (startLatLng != null) {
                coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(startLatLng, 12f)) }
            } else if (endLatLng != null) {
                coroutineScope.launch { cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(endLatLng, 12f)) }
            }
            // Mevcut konuma odaklanma mantığı kaldırıldı
        } catch (e: Exception) {
            Log.e("MapScreen", "Error in LaunchedEffect camera update: ${e.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = mapUiSettings,
            onMapLoaded = { isMapLoaded = true },
            onMapClick = { latLng ->
                pointSelectionMode?.let { mode ->
                    trafficViewModel.onMapClick(latLng, mode == PointSelectionMode.START)
                    pointSelectionMode = null
                }
            }
        ) {
            startCoordinates?.latitude?.let { startLat ->
                startCoordinates?.longitude?.let { startLng ->
                    Marker(state = MarkerState(position = LatLng(startLat, startLng)), title = startAddress.take(30), snippet = "Start Location")
                }
            }
            endCoordinates?.latitude?.let { endLat ->
                endCoordinates?.longitude?.let { endLng ->
                    Marker(state = MarkerState(position = LatLng(endLat, endLng)), title = endAddress.take(30), snippet = "End Location", icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                }
            }
            // Mevcut düz çizgiyi kaldırıyoruz.
            // if (startLatLngPoly != null && endLatLngPoly != null) {
            // Polyline(points = listOf(startLatLngPoly, endLatLngPoly), color = Color.Blue, width = 8f)
            // }

            // YENİ: Rota Polyline'ını Çizme
            val decodedPath = remember(routePolyline) {
                routePolyline?.let { decodePolyline(it) }
            }
            decodedPath?.let {
                Polyline(points = it, color = Color.Red, width = 10f, zIndex = 1f)
            }

            // Yakındaki Yerleri Marker Olarak Göster
            nearbyPlaces.forEach { placeResult -> // Değişken adı güncellendi
                placeResult.geometry?.location?.let { location ->
                    val latLng = LatLng(location.lat ?: 0.0, location.lng ?: 0.0)
                    Marker(
                        state = MarkerState(position = latLng),
                        title = placeResult.name ?: "Nearby Place",
                        snippet = placeResult.vicinity ?: placeResult.types?.joinToString(), // Adres için vicinity kullanıldı
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
            }
            // Mevcut konum marker'ı kaldırıldı
        }

        // Mevcut Konum FAB'ı kaldırıldı

        val topPanelTargetHeight = if (inputsExpanded) 500.dp else 72.dp
        val topPanelHeight by animateDpAsState(targetValue = topPanelTargetHeight)

        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(topPanelHeight)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.80f),
            tonalElevation = 6.dp,
            shadowElevation = 4.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .height(56.dp)
                        .clickable { inputsExpanded = !inputsExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Predict Traffic", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    Icon(imageVector = if (inputsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, contentDescription = if (inputsExpanded) "Collapse" else "Expand", tint = MaterialTheme.colorScheme.onSurface)
                }
                AnimatedVisibility(visible = inputsExpanded) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .padding(top = 0.dp, bottom = 16.dp)
                            .verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = startAddress,
                            onValueChange = trafficViewModel::onStartAddressChange,
                            label = { Text("Start Location (e.g., City, Address)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                            singleLine = true,
                            enabled = !isLoading,
                            trailingIcon = { IconButton(onClick = { pointSelectionMode = PointSelectionMode.START }) { Icon(Icons.Filled.EditLocation, "Select Start on Map") } }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endAddress,
                            onValueChange = trafficViewModel::onEndAddressChange,
                            label = { Text("End Location (e.g., City, Address)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                            singleLine = true,
                            enabled = !isLoading,
                            trailingIcon = { IconButton(onClick = { pointSelectionMode = PointSelectionMode.END }) { Icon(Icons.Filled.EditLocation, "Select End on Map") } }
                        )
                        if (pointSelectionMode != null) {
                            Text("Tap on the map to select ${if (pointSelectionMode == PointSelectionMode.START) "start" else "end"} location.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Select Time:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        Row(Modifier.selectableGroup().fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            TimeOptions.forEach { timeOption ->
                                Row(Modifier.height(56.dp).selectable(selected = (timeOption == selectedTime), onClick = { trafficViewModel.onTimeChange(timeOption) }, role = Role.RadioButton, enabled = !isLoading).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = (timeOption == selectedTime), onClick = null, enabled = !isLoading)
                                    Text(timeOption, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Select Day Type:", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp))
                        Row(Modifier.selectableGroup().fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            DayTypeOptions.forEach { dayTypeOption ->
                                Row(Modifier.height(56.dp).selectable(selected = (dayTypeOption == selectedDayType), onClick = { trafficViewModel.onDayTypeChange(dayTypeOption) }, role = Role.RadioButton, enabled = !isLoading).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = (dayTypeOption == selectedDayType), onClick = null, enabled = !isLoading)
                                    Text(dayTypeOption, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 4.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (errorMessage != null) {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 8.dp))
                        }
                        Button(onClick = { trafficViewModel.fetchTrafficPredictionFromAddresses(); inputsExpanded = false }, enabled = !isLoading && isInputValid, modifier = Modifier.fillMaxWidth()) {
                            Text("Get Prediction")
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Find Nearby:", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = { trafficViewModel.fetchNearbyPlacesForCurrentLocation("gas_station") }, enabled = !isLoading && startCoordinates != null) {
                                Text("Gas")
                            }
                            Button(onClick = { trafficViewModel.fetchNearbyPlacesForCurrentLocation("restaurant") }, enabled = !isLoading && startCoordinates != null) {
                                Text("Food")
                            }
                            Button(onClick = { trafficViewModel.fetchNearbyPlacesForCurrentLocation("cafe") }, enabled = !isLoading && startCoordinates != null) {
                                Text("Cafe")
                            }
                        }
                        if (isFetchingPlaces) {
                            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                        }

                    }
                }
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val showLoadingOrResult = (isGeocoding || isFetchingPrediction || predictionResult != null) && errorMessage == null

            AnimatedVisibility(
                visible = showLoadingOrResult,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                when {
                    isGeocoding -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                                Text("Finding locations...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    isFetchingPrediction -> {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Column(modifier = Modifier.padding(16.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                                Text("Calculating prediction...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                    predictionResult != null -> {
                        val cardColor = when (estimatedCondition) {
                            "Heavy" -> Color.Red.copy(alpha = 0.7f)
                            "Moderate" -> Color.Yellow.copy(alpha = 0.75f)
                            "Light" -> Color.Green.copy(alpha = 0.7f)
                            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                        }
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                            Text(predictionResult ?: "", style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface), textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenAddressInputPreview() {
    TrafficPredictionTheme {
        Text("MapScreen Preview (ViewModel required)")
    }
}

// Polyline Çözme Fonksiyonu
// Kaynak: https://stackoverflow.com/a/30325653/1063730 ve Google'ın dokümantasyonu
// Bu fonksiyon, Google'ın kodlanmış polyline formatını LatLng listesine çevirir.
private fun decodePolyline(encoded: String): List<LatLng> {
    val poly = ArrayList<LatLng>()
    var index = 0
    val len = encoded.length
    var lat = 0
    var lng = 0

    while (index < len) {
        var b: Int
        var shift = 0
        var result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lat += dlat

        shift = 0
        result = 0
        do {
            b = encoded[index++].code - 63
            result = result or (b and 0x1f shl shift)
            shift += 5
        } while (b >= 0x20)
        val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
        lng += dlng

        val p = LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5)
        poly.add(p)
    }
    return poly
}
