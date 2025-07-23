package com.example.trafficprediction.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.trafficprediction.R
import com.example.trafficprediction.data.FavoriteRoute
import com.example.trafficprediction.data.ThemePreference
import com.example.trafficprediction.ui.viewmodels.AuthViewModel
import com.example.trafficprediction.ui.viewmodels.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel(), // We added ThemeViewModel.
    onNavigateToLogin: () -> Unit // Callback to navigate to the Login screen.
) {
    val currentUser by authViewModel.currentUser.observeAsState()
    val userProfileData by authViewModel.userProfileData.collectAsState() // We listen to user profile data.
    val favoriteRoutes by authViewModel.favoriteRoutes.collectAsState()
    val isLoadingFavorites by authViewModel.isLoadingFavorites.collectAsState()
    val favoriteRouteError by authViewModel.favoriteRouteError.collectAsState()

    var showAddFavoriteDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) } // State for the settings dialog.
    val currentTheme by themeViewModel.themePreference.collectAsState()

    // We load favorite routes when the user changes or when the screen first opens.
    LaunchedEffect(key1 = currentUser) {
        if (currentUser != null) {
            authViewModel.loadFavoriteRoutes()
        }
    }

    if (showSettingsDialog) {
        SettingsDialog(
            currentUser = currentUser,
            currentAvatarIconName = userProfileData?.avatarIconName, // Pass the current avatar.
            onDismiss = { showSettingsDialog = false },
            onSave = { newName, selectedAvatar ->
                authViewModel.updateProfile(newName = newName, avatarIconName = selectedAvatar)
                showSettingsDialog = false
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (currentUser != null) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(id = R.string.add_favorite_route)) },
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = stringResource(id = R.string.add_favorite_route_desc)
                        )
                    },
                    onClick = { showAddFavoriteDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            if (currentUser != null) {
                // User Information and Sign Out Card.
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(80.dp)
                        ) {
                            val avatarIcon = when (userProfileData?.avatarIconName) {
                                "Face" -> Icons.Filled.Face
                                "Person" -> Icons.Filled.Person
                                else -> Icons.Default.AccountCircle
                            }
                            Icon(
                                imageVector = avatarIcon,
                                contentDescription = "User Avatar",
                                modifier = Modifier.padding(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = currentUser?.displayName ?: currentUser?.email ?: "User",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (currentUser?.email != null && currentUser?.displayName != currentUser?.email) {
                            Text(
                                text = currentUser!!.email!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Button(
                                onClick = { showSettingsDialog = true },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Settings, contentDescription = "Settings")
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text("Settings")
                            }
                            FilledTonalButton(
                                onClick = {
                                    val nextTheme = when (currentTheme) {
                                        ThemePreference.LIGHT -> ThemePreference.DARK
                                        ThemePreference.DARK -> ThemePreference.SYSTEM_DEFAULT
                                        ThemePreference.SYSTEM_DEFAULT -> ThemePreference.LIGHT
                                    }
                                    themeViewModel.updateThemePreference(nextTheme)
                                },
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.ColorLens, contentDescription = "Theme")
                                Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                                Text(
                                    when (currentTheme) {
                                        ThemePreference.LIGHT -> "Dark Theme"
                                        ThemePreference.DARK -> "System Theme"
                                        ThemePreference.SYSTEM_DEFAULT -> "Light Theme"
                                    }
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { authViewModel.signOut() },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Text("Sign Out")
                        }
                    }
                }

                Text(
                    "Favorite Routes",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .padding(top = 16.dp, bottom = 8.dp)
                        .align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (isLoadingFavorites) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (favoriteRouteError != null) {
                    Text(
                        "Error: $favoriteRouteError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Button(
                        onClick = { authViewModel.loadFavoriteRoutes() },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Retry")
                    }
                } else if (favoriteRoutes.isEmpty()) {
                    Text(
                        "No favorite routes added yet.",
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(favoriteRoutes) { route ->
                            FavoriteRouteItem(route = route, onDelete = {
                                authViewModel.deleteFavoriteRoute(route.id)
                            })
                            Divider()
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Please sign in to see your profile and favorite routes.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onNavigateToLogin) { // We use the callback here.
                        Text("Sign In / Sign Up")
                    }
                }
            }

            if (showAddFavoriteDialog) {
                AddFavoriteRouteDialog(
                    onDismiss = { showAddFavoriteDialog = false },
                    onAddRoute = { routeName, originAddress, destinationAddress ->
                        authViewModel.addFavoriteRouteFromAddresses(
                            routeName,
                            originAddress,
                            destinationAddress
                        )
                    },
                    isLoading = isLoadingFavorites,
                    error = favoriteRouteError,
                    onClearError = { authViewModel.clearFavoriteRouteError() }
                )
            }
        }
    }
}

@Composable
fun FavoriteRouteItem(route: FavoriteRoute, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(route.name, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "From: ${route.originAddress.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "To: ${route.destinationAddress.ifEmpty { "N/A" }}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Delete Route",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFavoriteRouteDialog(
    onDismiss: () -> Unit,
    onAddRoute: (name: String, originAddress: String, destinationAddress: String) -> Unit,
    isLoading: Boolean,
    error: String?,
    onClearError: () -> Unit
) {
    var routeName by remember { mutableStateOf(TextFieldValue("")) }
    var originAddress by remember { mutableStateOf(TextFieldValue("")) }
    var destinationAddress by remember { mutableStateOf(TextFieldValue("")) }
    var routeNameError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    LaunchedEffect(error) {
        if (!error.isNullOrBlank()) {
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_LONG).show()
            onClearError()
        }
    }

    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Add New Favorite Route",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = routeName,
                    onValueChange = { routeName = it; routeNameError = null },
                    label = { Text("Route Name (e.g., Home to Work)") },
                    singleLine = true,
                    isError = routeNameError != null,
                    enabled = !isLoading
                )
                if (routeNameError != null) {
                    Text(
                        routeNameError!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = originAddress,
                    onValueChange = { originAddress = it },
                    label = { Text("Origin Address") },
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = destinationAddress,
                    onValueChange = { destinationAddress = it },
                    label = { Text("Destination Address") },
                    singleLine = true,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (isLoading) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss, enabled = !isLoading) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (routeName.text.isBlank()) {
                                routeNameError = "Route name cannot be empty"
                            } else if (originAddress.text.isBlank()) {
                                routeNameError = "Origin address cannot be empty"
                            } else if (destinationAddress.text.isBlank()) {
                                routeNameError = "Destination address cannot be empty"
                            } else {
                                routeNameError = null
                                onAddRoute(
                                    routeName.text,
                                    originAddress.text,
                                    destinationAddress.text
                                )
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Text("Add Route")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentUser: com.google.firebase.auth.FirebaseUser?,
    currentAvatarIconName: String?,
    onDismiss: () -> Unit,
    onSave: (newName: String, selectedAvatar: String?) -> Unit
) {
    var newName by remember { mutableStateOf(currentUser?.displayName ?: "") }
    var selectedAvatarIdentifier by remember { mutableStateOf(currentAvatarIconName) }

    val avatarOptions = listOf(
        "AccountCircle" to Icons.Default.AccountCircle,
        "Face" to Icons.Filled.Face,
        "Person" to Icons.Filled.Person
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile Settings") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Choose Avatar:", style = MaterialTheme.typography.titleSmall)
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    avatarOptions.forEach { (name, icon) ->
                        IconButton(
                            onClick = { selectedAvatarIdentifier = name },
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (selectedAvatarIdentifier == name) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        ) {
                            Icon(
                                icon,
                                contentDescription = name,
                                tint = if (selectedAvatarIdentifier == name) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newName.isNotBlank()) {
                        onSave(newName, selectedAvatarIdentifier)
                    }
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
