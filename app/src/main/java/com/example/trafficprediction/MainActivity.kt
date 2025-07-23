package com.example.trafficprediction

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.trafficprediction.ui.screens.HomeScreen
import com.example.trafficprediction.ui.screens.LoginScreen
import com.example.trafficprediction.ui.screens.MapScreen
import com.example.trafficprediction.ui.screens.PredictionDetailScreen
import com.example.trafficprediction.ui.screens.PredictionsScreen
import com.example.trafficprediction.ui.screens.ProfileScreen
import com.example.trafficprediction.ui.screens.SignUpScreen
import com.example.trafficprediction.ui.theme.SkyBlueLight
import com.example.trafficprediction.ui.theme.SoftTurquoiseLight
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel
import com.google.firebase.messaging.FirebaseMessaging

class MainActivity : ComponentActivity() {

    private val requestMultiplePermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("MainActivityPermissions", "${it.key} = ${it.value}")
            }
            // Additional actions can be taken here based on permission results
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestPermissions() // Check and request permissions in onCreate

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("MainActivityFCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }
            // Get new FCM registration token
            val currentToken = task.result
            Log.d("MainActivityFCM", "Current FCM Token for testing: $currentToken")
            // THIS TOKEN CAN BE MANUALLY ENTERED INTO FIRESTORE FOR TESTING
        }
        setContent {
            TrafficPredictionTheme {
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        SoftTurquoiseLight,
                        SkyBlueLight
                    )
                )
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = gradientBrush)
                ) {
                    TrafficPredictionApp()
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val requiredPermissions = mutableListOf<String>()
        requiredPermissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        requiredPermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
            requiredPermissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val permissionsToRequest = requiredPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()

        if (permissionsToRequest.isNotEmpty()) {
            requestMultiplePermissionsLauncher.launch(permissionsToRequest)
        } else {
            Log.d("MainActivityPermissions", "All required permissions already granted.")
        }
    }
}

sealed class Screen(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector?
) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Map : Screen("map", "Predict", Icons.Default.Place)
    object Predictions : Screen("predictions", "History", Icons.Default.Analytics)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object PredictionDetail :
        Screen("prediction_detail", "Details", null) // No icon for PredictionDetail

    object Login :
        Screen("login", "Login", null) // No icon for LoginScreen, will not be in bottomNav

    object SignUp :
        Screen("signup", "Sign Up", null) // No icon for SignUpScreen, will not be in bottomNav
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Map,
    Screen.Predictions,
    Screen.Profile
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrafficPredictionApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val trafficViewModel: TrafficViewModel = viewModel()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            // Hide TopAppBar on Login and SignUp screens
            if (currentDestination?.route != Screen.Login.route && currentDestination?.route != Screen.SignUp.route) {
                TopAppBar(
                    title = { Text("Traffic Prediction") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        },
        bottomBar = {
            // Hide BottomNavigationBar on Login and SignUp screens
            if (currentDestination?.route != Screen.Login.route && currentDestination?.route != Screen.SignUp.route) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f), // Fully transparent or a slight tint
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp) // Reduced vertical padding
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)),
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            alwaysShowLabel = false, // Show labels only when selected or never
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            icon = {
                                screen.icon?.let {
                                    Icon(imageVector = it, contentDescription = screen.label)
                                }
                            },
                            label = { Text(screen.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(300)
                ) + fadeOut(animationSpec = tween(300))
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    navController = navController,
                    trafficViewModel = trafficViewModel
                )
            }
            composable(Screen.Map.route) { MapScreen(trafficViewModel = trafficViewModel) }
            composable(Screen.Predictions.route) {
                PredictionsScreen(
                    navController = navController,
                    trafficViewModel = trafficViewModel
                )
            }
            composable(Screen.Profile.route) {
                ProfileScreen( // Add onNavigateToLogin callback to ProfileScreen
                    onNavigateToLogin = { navController.navigate(Screen.Login.route) }
                )
            }
            composable(Screen.PredictionDetail.route) {
                PredictionDetailScreen(
                    navController = navController,
                    trafficViewModel = trafficViewModel
                )
            }
            composable(Screen.Login.route) {
                LoginScreen(
                    onLoginSuccess = { firebaseUser ->
                        // Navigate to home or profile screen on successful login
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.Login.route) {
                                inclusive = true
                            } // Remove Login screen from backstack
                        }
                    },
                    onNavigateToSignUp = { navController.navigate(Screen.SignUp.route) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.SignUp.route) {
                SignUpScreen( // Assuming SignUpScreen.kt exists
                    onSignUpSuccess = { firebaseUser ->
                        navController.navigate(Screen.Profile.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                            popUpTo(Screen.Login.route) {
                                inclusive = true
                            } // Also remove Login if navigated from there
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
