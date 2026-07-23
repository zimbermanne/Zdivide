package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.dualscreen.data.AppDatabase
import com.example.dualscreen.network.NetworkManager
import com.example.dualscreen.ui.screens.CalibrationScreen
import com.example.dualscreen.ui.screens.ConnectivityScreen
import com.example.dualscreen.ui.screens.DiagnosticVerdictScreen
import com.example.dualscreen.ui.screens.HomeScreen
import com.example.dualscreen.ui.screens.MirroringScreen
import com.example.dualscreen.ui.screens.SplitDisplayScreen
import com.example.dualscreen.ui.screens.TouchSyncScreen
import com.example.dualscreen.ui.screens.VideoSyncScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val networkManager = NetworkManager(applicationContext)

        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("home") {
                            HomeScreen(
                                networkManager = networkManager,
                                onNavigateToPhase = { route -> navController.navigate(route) }
                            )
                        }
                        composable("connectivity") {
                            ConnectivityScreen(
                                networkManager = networkManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("split_screen") {
                            SplitDisplayScreen(
                                networkManager = networkManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("calibration") {
                            CalibrationScreen(
                                networkManager = networkManager,
                                database = database,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("video_sync") {
                            VideoSyncScreen(
                                networkManager = networkManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("mirroring") {
                            MirroringScreen(
                                networkManager = networkManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("touch_sync") {
                            TouchSyncScreen(
                                networkManager = networkManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                        composable("diagnostics") {
                            DiagnosticVerdictScreen(
                                networkManager = networkManager,
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
