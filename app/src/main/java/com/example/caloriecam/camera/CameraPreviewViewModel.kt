package com.example.caloriecam.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
import androidx.camera.core.CameraSelector.DEFAULT_FRONT_CAMERA
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CameraPreviewViewModel : ViewModel() {
    // Used to set up a link between the Camera and your UI.
    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    val surfaceRequest: StateFlow<SurfaceRequest?> = _surfaceRequest

    // Track current camera selection
    private val _cameraSelector = MutableStateFlow(DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector

    private val cameraPreviewUseCase = Preview.Builder().build().apply {
        setSurfaceProvider { newSurfaceRequest ->
            _surfaceRequest.update { newSurfaceRequest }
        }
    }
    private var cameraJob: kotlinx.coroutines.Job? = null


    suspend fun bindToCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        try {
            val processCameraProvider = ProcessCameraProvider.awaitInstance(appContext)

            // Unbind all cameras first to prevent conflicts
            processCameraProvider.unbindAll()

            processCameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector.value, cameraPreviewUseCase
            )

            // Cancellation signals we're done with the camera
            try { awaitCancellation() } finally { processCameraProvider.unbindAll() }
        } catch (e: Exception) {
            android.util.Log.e("CameraViewModel", "Camera binding failed", e)
        }
    }

    fun toggleCamera(appContext: Context, lifecycleOwner: LifecycleOwner) {
        cameraJob?.cancel()
        val newCamera = if (cameraSelector.value == DEFAULT_BACK_CAMERA) {
            DEFAULT_FRONT_CAMERA
        } else {
            DEFAULT_BACK_CAMERA
        }

        _cameraSelector.value = newCamera
        // Create new binding
        cameraJob = kotlinx.coroutines.MainScope().launch {
            bindToCamera(appContext, lifecycleOwner)
        }
    }
}