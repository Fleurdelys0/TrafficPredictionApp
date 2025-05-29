package com.example.trafficprediction // Paket adı güncel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons // material.icons importları
import androidx.compose.material.icons.filled.Analytics // Örnek ikonlar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place // Map ikonu için Place
import androidx.compose.material.icons.filled.ReceiptLong // Detay ikonu için örnek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy // selected için
import androidx.navigation.NavGraph.Companion.findStartDestination // selected için
import androidx.navigation.compose.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import com.example.trafficprediction.ui.screens.* // Ekranları import et
import com.example.trafficprediction.ui.theme.SkyBlueLight
import com.example.trafficprediction.ui.theme.SoftTurquoiseLight
import com.example.trafficprediction.ui.theme.TrafficPredictionTheme // Temayı import et
import com.example.trafficprediction.ui.viewmodels.TrafficViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TrafficPredictionTheme { // Tema adı güncel
                val gradientBrush = Brush.verticalGradient(
                    colors = listOf(
                        SoftTurquoiseLight, // Üst renk
                        SkyBlueLight        // Alt renk
                    )
                )
                Surface( // Surface sarmalayıcısı iyi bir pratik
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush = gradientBrush)
                ) {
                    TrafficPredictionApp()
                }
            }
        }
    }
}

// Navigasyon hedeflerini tanımla (Route isimleri ve ikonlar)
sealed class Screen(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector?) { // Icon nullable yapıldı
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Map : Screen("map", "Predict", Icons.Default.Place)
    object Predictions : Screen("predictions", "History", Icons.Default.Analytics)
    object Profile : Screen("profile", "Profile", Icons.Default.Person)
    object PredictionDetail : Screen("prediction_detail", "Details", null) // Detay ekranı, bottom bar'da görünmeyecek
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
    val trafficViewModel: TrafficViewModel = viewModel() // ViewModel'i burada oluştur

    Scaffold(
        containerColor = Color.Transparent, // Ensure Scaffold background is transparent
        topBar = {
            TopAppBar(
                title = { Text("Traffic Prediction") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Make TopAppBar transparent
                    titleContentColor = MaterialTheme.colorScheme.onSurface // Adjust title color for better contrast on gradient
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color.Transparent // Make NavigationBar transparent
            ) {
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), // Semi-transparent indicator
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        ),
                        icon = {
                            screen.icon?.let { // Check if icon is not null
                                Icon(imageVector = it, contentDescription = screen.label)
                            }
                        },
                        label = { Text(screen.label) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding -> // paddingValues yerine innerPadding daha yaygın
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route, // Başlangıç ekranı
            modifier = Modifier.padding(innerPadding), // Scaffold'dan gelen padding'i uygula
            enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -1000 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)) }
        ) {
            composable(Screen.Home.route) { HomeScreen(navController = navController, trafficViewModel = trafficViewModel) } // ViewModel eklendi
            composable(Screen.Map.route) { MapScreen(trafficViewModel = trafficViewModel) } // ViewModel'i geçir
            composable(Screen.Predictions.route) { PredictionsScreen(navController = navController, trafficViewModel = trafficViewModel) } // ViewModel'i geçir
            composable(Screen.Profile.route) { ProfileScreen() } // AuthViewModel kendi içinde viewModel() ile alınabilir, şimdilik böyle kalsın
            composable(Screen.PredictionDetail.route) {
                PredictionDetailScreen(navController = navController, trafficViewModel = trafficViewModel) // ViewModel'i geçir
            }
        }
    }
}
