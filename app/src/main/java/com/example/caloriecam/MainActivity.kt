package com.example.caloriecam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.example.caloriecam.camera.CameraPreviewViewModel
import com.example.caloriecam.navigation.BottomNavBar
import com.example.caloriecam.navigation.NavGraph
import com.example.caloriecam.ui.theme.CalorieCamTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus

@OptIn(ExperimentalPermissionsApi::class)
val PermissionStatus.shouldShowRationale: Boolean
    get() = when (this) {
        is PermissionStatus.Denied -> this.shouldShowRationale
        else -> false
    }

class MainActivity : ComponentActivity() {
    private val cameraViewModel: CameraPreviewViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CalorieCamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    androidx.compose.material3.Scaffold(
                        bottomBar = { BottomNavBar(navController = navController) }
                    ) { padding ->
                        NavGraph(
                            navController = navController,
                            cameraViewModel = cameraViewModel,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }
        }
    }
}
