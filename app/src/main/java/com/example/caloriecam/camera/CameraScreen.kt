package com.example.caloriecam.camera

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraScreen(
    cameraViewModel: CameraPreviewViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Define permissions needed based on Android version
    val permissionsToRequest = remember {
        mutableListOf(android.Manifest.permission.CAMERA).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
                add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                add(android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
    }

    // Request permissions only when camera screen is accessed
    val permissionsState = rememberMultiplePermissionsState(permissions = permissionsToRequest)

    // Check if camera permission granted
    val cameraPermissionGranted = permissionsState.permissions.find {
        it.permission == android.Manifest.permission.CAMERA
    }?.status?.isGranted ?: false

    // Track if we should show rationale
    val shouldShowRationale = permissionsState.permissions.any {
        !it.status.isGranted && it.status.shouldShowRationale
    }

    val capturedImage by cameraViewModel.capturedImage.collectAsState()
    var showImagePreview by remember { mutableStateOf(false) }

    // Collect analysis state
    val isAnalyzing by cameraViewModel.isAnalyzing.collectAsState()
    val analysisResult by cameraViewModel.analysisResult.collectAsState()

    // Clear all states when entering camera screen
    LaunchedEffect(Unit) {
        cameraViewModel.clearAllStates()
    }

    // When a new image is captured, show the preview dialog
    LaunchedEffect(capturedImage) {
        if (capturedImage != null) {
            showImagePreview = true
        }
    }

    // Show result popup when analysis is complete - but don't clear state immediately
    LaunchedEffect(analysisResult) {
        analysisResult?.let { result ->
            // Show popup with results
            val foodName = result.label.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            val probability = (result.probability * 100).toInt()

            // Wait for analysis to complete fully before clearing
            kotlinx.coroutines.delay(1000) // Show result for 1 second

            // Create a toast message
            val message = "Food detected: $foodName with ${probability}% confidence"
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()

            // Clear the analysis result after showing toast
            cameraViewModel.clearAnalysisResult()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (cameraPermissionGranted) {
            // Camera Preview with all controls
            CameraPreviewWithControls(
                viewModel = cameraViewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Image preview dialog
            if (capturedImage != null && showImagePreview) {
                Dialog(onDismissRequest = {
                    showImagePreview = false
                    cameraViewModel.clearCapturedImage()
                }) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Preview",
                                style = MaterialTheme.typography.headlineSmall,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Display the captured image
                            Image(
                                bitmap = capturedImage!!.asImageBitmap(),
                                contentDescription = "Captured image",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(300.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                Button(
                                    onClick = {
                                        showImagePreview = false
                                        cameraViewModel.clearCapturedImage()
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Cancel,
                                        contentDescription = "Cancel"
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Cancel")
                                }

                                val isAnalyzing by cameraViewModel.isAnalyzing.collectAsState()

                                Button(
                                    onClick = {
                                        cameraViewModel.analyzeImage()
                                        showImagePreview = false
                                    },
                                    enabled = !isAnalyzing
                                ) {
                                    if (isAnalyzing) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Analyze"
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Analyze")
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Permission request screen
            PermissionRequestScreen(
                shouldShowRationale = shouldShowRationale,
                onRequestPermission = { permissionsState.launchMultiplePermissionRequest() }
            )
        }
    }

    // Show loading dialog when analyzing
    if (isAnalyzing) {
        Dialog(onDismissRequest = { /* Cannot dismiss while loading */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .padding(8.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Analyzing image...",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please wait while we analyze your image",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequestScreen(
    shouldShowRationale: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .padding(16.dp)
                .width(300.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (shouldShowRationale) {
                        "Camera and storage permissions are required to scan food items and access your gallery"
                    } else {
                        "We need camera and storage permissions to help you track your calories"
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Grant Permissions")
                }
            }
        }
    }
}

@Composable
fun CameraPreviewWithControls(
    viewModel: CameraPreviewViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraReady by viewModel.cameraReady.collectAsState()

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.processImageFromGallery(context, uri)
        }
    }

    // Handle lifecycle events and start camera
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.startCamera(context, lifecycleOwner)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Initial camera setup
        viewModel.startCamera(context, lifecycleOwner)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Use PreviewView for camera display
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }

                // Connect the preview use case to the PreviewView
                viewModel.preview.setSurfaceProvider(previewView.surfaceProvider)

                previewView
            },
            modifier = Modifier.fillMaxSize()
        )

        // Show loading indicator when camera is not ready
        if (!cameraReady) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Preparing camera...",
                        color = Color.White
                    )
                }
            }
        }

        // Camera Controls at the bottom
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Gallery picker button
            IconButton(
                onClick = { galleryLauncher.launch("image/*") },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 32.dp)
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Image,
                    contentDescription = "Pick from Gallery",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            // Capture button
            IconButton(
                onClick = { viewModel.capturePhoto(context) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .size(72.dp)
                    .background(Color.White, CircleShape)
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
            }

            // Camera switch button
            IconButton(
                onClick = { viewModel.toggleCamera(context, lifecycleOwner) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 32.dp)
                    .size(56.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
