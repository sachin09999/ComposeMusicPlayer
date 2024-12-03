package com.project.composemusicplayer

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.BottomNavigation
import androidx.compose.material.BottomNavigationItem
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.composemusicplayer.navigation.Screen
import com.project.composemusicplayer.ui.FullScreenPlayer.FullScreenPlayerView
import com.project.composemusicplayer.ui.home.HomeScreen
import com.project.composemusicplayer.ui.home.HomeViewModel
import com.project.composemusicplayer.ui.theme.ComposeMusicPlayerTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            startMusicPlayer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            // Below Android 13
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isEmpty()) {
            startMusicPlayer()
        } else {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun startMusicPlayer() {
        setContent {
            ComposeMusicPlayerTheme {
                MainScreen()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val viewModel: HomeViewModel = viewModel()
    Scaffold(
        bottomBar = { BottomNavBar(navController) },
        modifier = Modifier.navigationBarsPadding()
    ) {
        NavHost(navController = navController, startDestination = Screen.Home.route) {
            composable(Screen.Home.route) {
                TransitioningScreen {
                    HomeScreen(
                        viewModel = viewModel,
                        navController = navController
                    )
                }

            }
            composable(Screen.FullScreenPlayer.route) {
                TransitioningScreen {
                    FullScreenPlayerView(viewModel, navController = navController)
                }
            }
        }
    }
}

@Composable
fun TransitioningScreen(content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = true, // Control this based on your navigation logic
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        content()
    }
}

@Composable
fun BottomNavBar(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xAA000000)) // Semi-transparent black color
    ) {
        BottomNavigation(
            backgroundColor = Color.Transparent, // Keep the BottomNavigation background transparent
            elevation = 8.dp // Optional: Add elevation for shadow effect
        ) {
            val currentRoute = navController.currentBackStackEntry?.destination?.route

            BottomNavigationItem(
                icon = { Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White) },
                selected = currentRoute == Screen.Home.route,
                onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                },
                selectedContentColor = Color.White, // Color for selected item
                unselectedContentColor = Color.Gray // Color for unselected item
            )

            BottomNavigationItem(
                icon = { Icon(Icons.Default.MusicNote, contentDescription = "Player", tint = Color.White) },
                selected = currentRoute == Screen.FullScreenPlayer.route,
                onClick = {
                    navController.navigate(Screen.FullScreenPlayer.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                },
                selectedContentColor = Color.White, // Color for selected item
                unselectedContentColor = Color.Gray // Color for unselected item
            )
        }
    }
}


