package com.example.trafficprediction.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
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
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.tasks.await

private enum class PointSelectionMode { START, END }

@OptIn(ExperimentalMaterial3Api::class, MapsComposeExperimentalApi::class)
@Composable
fun MapScreen(
    trafficViewModel: TrafficViewModel // ViewModel is now provided from outside.
) {
    val context = LocalContext.current
    // --- Get States from ViewModel ---
    val startAddress by trafficViewModel.startAddress.collectAsState()
    val endAddress by trafficViewModel.endAddress.collectAsState()
    val selectedTime by trafficViewModel.selectedTime.collectAsState()
    val selectedDayType by trafficViewModel.selectedDayType.collectAsState()
    val predictionResult by trafficViewModel.predictionResult.collectAsState()
    // We'll use specific loading states instead of a general isLoading.
    val isGeocoding by trafficViewModel.isGeocoding.collectAsState()
    val isFetchingPrediction by trafficViewModel.isFetchingPrediction.collectAsState()
    val errorMessage by trafficViewModel.errorMessage.collectAsState()
    // We also include isFetchingPlaces in isLoading.
    val isFetchingPlaces by trafficViewModel.isFetchingPlaces.collectAsState()
    val isLoading = isGeocoding || isFetchingPrediction || isFetchingPlaces

    val startCoordinates by trafficViewModel.startCoordinates.collectAsState()
    val endCoordinates by trafficViewModel.endCoordinates.collectAsState()
    val routePolyline by trafficViewModel.routePolyline.collectAsState()
    val nearbyPlaces by trafficViewModel.nearbyPlaces.collectAsState() // For nearby places.
    // currentUserLocation state has been removed.
    val scrollState = rememberScrollState()
    var pointSelectionMode by remember { mutableStateOf<PointSelectionMode?>(null) }
    var inputsExpanded by remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope() // coroutineScope'u buraya taşıdık


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
            if (!isGranted) {
                // coroutineScope burada zaten erişilebilir olacak
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Location permission denied. Cannot show current location.")
                }
            }
        }
    )

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var currentUserLocation by remember { mutableStateOf<LatLng?>(null) }


    val isInputValid = remember(startAddress, endAddress) {
        startAddress.isNotBlank() && endAddress.isNotBlank()
    }

    val initialCameraPosition = LatLng(39.9334, 32.8597) // Ankara, Turkey.
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(initialCameraPosition, 5f)
    }
    val mapUiSettings by remember(hasLocationPermission) { // Recompose if permission changes
        mutableStateOf(
            MapUiSettings(
                zoomControlsEnabled = true,
                myLocationButtonEnabled = false, // We use our own FAB
                compassEnabled = true
            )
        )
    }

    val mapProperties by remember(hasLocationPermission) { // Recompose if permission changes
        mutableStateOf(
            MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = hasLocationPermission, // Show blue dot if permission granted
                isTrafficEnabled = true
            )
        )
    }
    // val coroutineScope = rememberCoroutineScope() // Yukarıya taşındı
    var isMapLoaded by remember { mutableStateOf(false) }
    val estimatedCondition by remember(predictionResult) {
        derivedStateOf {
            predictionResult?.substringAfter("Condition: ")?.substringBefore('\n')
        }
    }

    LaunchedEffect(
        startCoordinates,
        endCoordinates,
        isMapLoaded
    ) { // currentUserLocation dependency removed.
        if (!isMapLoaded) return@LaunchedEffect
        try {
            val startLatLng = startCoordinates?.latitude?.let { lat ->
                startCoordinates?.longitude?.let { lng ->
                    LatLng(
                        lat,
                        lng
                    )
                }
            }
            val endLatLng = endCoordinates?.latitude?.let { lat ->
                endCoordinates?.longitude?.let { lng ->
                    LatLng(
                        lat,
                        lng
                    )
                }
            }

            if (startLatLng != null && endLatLng != null) {
                val bounds = LatLngBounds.builder().include(startLatLng).include(endLatLng).build()
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(
                            bounds,
                            100
                        )
                    )
                }
            } else if (startLatLng != null) {
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            startLatLng,
                            12f
                        )
                    )
                }
            } else if (endLatLng != null) {
                coroutineScope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            endLatLng,
                            12f
                        )
                    )
                }
            }
            // Logic for focusing on current location has been removed.
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
                    Marker(
                        state = MarkerState(position = LatLng(startLat, startLng)),
                        title = startAddress.take(30),
                        snippet = "Start Location"
                    )
                }
            }
            endCoordinates?.latitude?.let { endLat ->
                endCoordinates?.longitude?.let { endLng ->
                    Marker(
                        state = MarkerState(position = LatLng(endLat, endLng)),
                        title = endAddress.take(30),
                        snippet = "End Location",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                    )
                }
            }
            // We're removing the current straight line.
            // if (startLatLngPoly != null && endLatLngPoly != null) {
            // Polyline(points = listOf(startLatLngPoly, endLatLngPoly), color = Color.Blue, width = 8f)
            // }

            // NEW: Drawing the Route Polyline.
            val decodedPath = remember(routePolyline) {
                routePolyline?.let { decodePolyline(it) }
            }
            decodedPath?.let {
                Polyline(points = it, color = Color.Red, width = 10f, zIndex = 1f)
            }

            // Show Nearby Places as Markers.
            nearbyPlaces.forEach { placeResult -> // Variable name updated.
                placeResult.geometry?.location?.let { location ->
                    val latLng = LatLng(location.lat ?: 0.0, location.lng ?: 0.0)
                    Marker(
                        state = MarkerState(position = latLng),
                        title = placeResult.name ?: "Nearby Place",
                        snippet = placeResult.vicinity
                            ?: placeResult.types?.joinToString(), // Used vicinity for address.
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )
                }
            }
            // Current location marker removed.
            currentUserLocation?.let { loc ->
                Marker(
                    state = MarkerState(position = loc),
                    title = "My Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE) // Optional: different color for current location
                )
            }
        }

        FloatingActionButton(
            onClick = {
                if (hasLocationPermission) {
                    coroutineScope.launch {
                        try {
                            val locationResult = fusedLocationClient.getCurrentLocation(
                                Priority.PRIORITY_HIGH_ACCURACY,
                                null
                            ).await()
                            locationResult?.let { location ->
                                val newLatLng = LatLng(location.latitude, location.longitude)
                                currentUserLocation = newLatLng // Update state for potential marker
                                cameraPositionState.animate(
                                    CameraUpdateFactory.newLatLngZoom(newLatLng, 15f),
                                    1000 // Animation duration in ms
                                )
                            } ?: run {
                                snackbarHostState.showSnackbar("Could not get current location.")
                            }
                        } catch (e: SecurityException) {
                            Log.e("MapScreen", "SecurityException getting location: ${e.message}")
                            snackbarHostState.showSnackbar("Location permission error.")
                            // Request permission again if it was somehow revoked
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        } catch (e: Exception) {
                            Log.e("MapScreen", "Error getting current location: ${e.message}")
                            snackbarHostState.showSnackbar("Error getting location.")
                        }
                    }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(Icons.Filled.MyLocation, "Go to my location", tint = MaterialTheme.colorScheme.onPrimary)
        }


        val topPanelTargetHeight = if (inputsExpanded) 500.dp else 72.dp
        val topPanelHeight by animateDpAsState(targetValue = topPanelTargetHeight, label = "topPanelHeightAnimation")

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
                    Text(
                        "Predict Traffic",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Icon(
                        imageVector = if (inputsExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (inputsExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
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
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            singleLine = true,
                            enabled = !isLoading,
                            trailingIcon = {
                                IconButton(onClick = {
                                    pointSelectionMode = PointSelectionMode.START
                                }) { Icon(Icons.Filled.EditLocation, "Select Start on Map") }
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = endAddress,
                            onValueChange = trafficViewModel::onEndAddressChange,
                            label = { Text("End Location (e.g., City, Address)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Done
                            ),
                            singleLine = true,
                            enabled = !isLoading,
                            trailingIcon = {
                                IconButton(onClick = {
                                    pointSelectionMode = PointSelectionMode.END
                                }) { Icon(Icons.Filled.EditLocation, "Select End on Map") }
                            }
                        )
                        if (pointSelectionMode != null) {
                            Text(
                                "Tap on the map to select ${if (pointSelectionMode == PointSelectionMode.START) "start" else "end"} location.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Select Time:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            Modifier
                                .selectableGroup()
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TimeOptions.forEach { timeOption ->
                                Row(
                                    Modifier
                                        .height(56.dp)
                                        .selectable(
                                            selected = (timeOption == selectedTime),
                                            onClick = { trafficViewModel.onTimeChange(timeOption) },
                                            role = Role.RadioButton,
                                            enabled = !isLoading
                                        )
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (timeOption == selectedTime),
                                        onClick = null,
                                        enabled = !isLoading
                                    )
                                    Text(
                                        timeOption,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Select Day Type:",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            Modifier
                                .selectableGroup()
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DayTypeOptions.forEach { dayTypeOption ->
                                Row(
                                    Modifier
                                        .height(56.dp)
                                        .selectable(
                                            selected = (dayTypeOption == selectedDayType),
                                            onClick = {
                                                trafficViewModel.onDayTypeChange(
                                                    dayTypeOption
                                                )
                                            },
                                            role = Role.RadioButton,
                                            enabled = !isLoading
                                        )
                                        .padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (dayTypeOption == selectedDayType),
                                        onClick = null,
                                        enabled = !isLoading
                                    )
                                    Text(
                                        dayTypeOption,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        if (errorMessage != null) {
                            Text(
                                errorMessage ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Button(
                            onClick = {
                                trafficViewModel.fetchTrafficPredictionFromAddresses(); inputsExpanded =
                                false
                            },
                            enabled = !isLoading && isInputValid,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Get Prediction")
                        }

                        Spacer(Modifier.height(16.dp))
                        Text("Find Nearby:", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                trafficViewModel.fetchNearbyPlacesForCurrentLocation(
                                    "gas_station"
                                )
                            }, enabled = !isLoading && startCoordinates != null) {
                                Text("Gas")
                            }
                            Button(onClick = {
                                trafficViewModel.fetchNearbyPlacesForCurrentLocation(
                                    "restaurant"
                                )
                            }, enabled = !isLoading && startCoordinates != null) {
                                Text("Food")
                            }
                            Button(onClick = {
                                trafficViewModel.fetchNearbyPlacesForCurrentLocation(
                                    "cafe"
                                )
                            }, enabled = !isLoading && startCoordinates != null) {
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 70.dp), // Add padding to avoid FAB overlap
            contentAlignment = Alignment.Center
        ) {
            val showLoadingOrResult =
                (isGeocoding || isFetchingPrediction || predictionResult != null) && errorMessage == null

            AnimatedVisibility(
                visible = showLoadingOrResult,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut()
            ) {
                when {
                    isGeocoding -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                                Text(
                                    "Finding locations...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    isFetchingPrediction -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                                Text(
                                    "Calculating prediction...",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
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
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Text(
                                predictionResult ?: "",
                                style = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
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

@Preview(showBackground = true)
@Composable
fun MapScreenAddressInputPreview() {
    TrafficPredictionTheme {
        // This preview won't fully work due to ViewModel and context dependencies.
        // Consider creating a simpler preview or a fake ViewModel for UI elements.
        Surface(modifier = Modifier.fillMaxSize()) {
            Box {
                Text("MapScreen Preview (Limited Functionality)", modifier = Modifier.align(Alignment.Center))
                FloatingActionButton(
                    onClick = { },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Icon(Icons.Filled.MyLocation, "Go to my location")
                }
            }
        }
    }
}

// Polyline Decoding Function.
// Source: https://stackoverflow.com/a/30325653/1063730 and Google's documentation.
// This function converts Google's encoded polyline format to a List of LatLng.
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
