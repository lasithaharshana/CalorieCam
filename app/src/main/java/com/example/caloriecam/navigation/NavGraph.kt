package com.example.caloriecam.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.caloriecam.camera.CameraScreen
import com.example.caloriecam.home.HomeScreen
import com.example.caloriecam.camera.CameraPreviewViewModel

@Composable
fun NavGraph(
    navController: NavHostController,
    cameraViewModel: CameraPreviewViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(route = Screen.Home.route) {
            HomeScreen()
        }
        
        composable(route = Screen.Camera.route) {
            CameraScreen(cameraViewModel = cameraViewModel)
        }
    }
}

