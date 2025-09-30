package com.example.caloriecam.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.caloriecam.api.FoodAnalysisResult
import com.example.caloriecam.api.FoodAnalysisService
import com.example.caloriecam.home.Food
import com.example.caloriecam.home.FoodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraPreviewViewModel : ViewModel() {
    // Track current camera selection
    private val _cameraSelector = MutableStateFlow(CameraSelector.DEFAULT_BACK_CAMERA)
    val cameraSelector: StateFlow<CameraSelector> = _cameraSelector

    // Track captured image
    private val _capturedImage = MutableStateFlow<Bitmap?>(null)
    val capturedImage: StateFlow<Bitmap?> = _capturedImage

    // Track analysis result
    private val _analysisResult = MutableStateFlow<FoodAnalysisResult?>(null)
    val analysisResult: StateFlow<FoodAnalysisResult?> = _analysisResult

    // Track analysis state
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing

    // Camera executor for background operations
    private lateinit var cameraExecutor: ExecutorService

    // Food analysis service
    private val foodAnalysisService = FoodAnalysisService()

    // Food repository
    private val foodRepository = FoodRepository.getInstance()

    // Keep Preview and ImageCapture use cases as class members
    val preview = Preview.Builder().build()
    val imageCapture = ImageCapture.Builder()
        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
        .build()

    // Track if camera is ready
    private val _cameraReady = MutableStateFlow(false)
    val cameraReady: StateFlow<Boolean> = _cameraReady

    // Track if food was added to home screen
    private val _foodAddedToHomeScreen = MutableStateFlow<Food?>(null)
    val foodAddedToHomeScreen: StateFlow<Food?> = _foodAddedToHomeScreen

    init {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun startCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            try {
                // Used to bind the lifecycle of cameras to the lifecycle owner
                val cameraProvider = cameraProviderFuture.get()

                // Unbind any previous use cases
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector.value,
                    preview,
                    imageCapture
                )

                // Camera is now ready
                _cameraReady.value = true
                Log.d("CameraViewModel", "Camera setup successful")

            } catch (e: Exception) {
                Log.e("CameraViewModel", "Camera setup failed", e)
                _cameraReady.value = false
            }
        }, ContextCompat.getMainExecutor(context))
    }

    fun toggleCamera(context: Context, lifecycleOwner: LifecycleOwner) {
        _cameraSelector.value = if (cameraSelector.value == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Restart camera with new selector
        startCamera(context, lifecycleOwner)
    }

    fun capturePhoto(context: Context) {
        // Clear previous analysis state when capturing a new photo
        clearAllStates()

        val executor = ContextCompat.getMainExecutor(context)

        imageCapture.takePicture(
            executor,
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    viewModelScope.launch {
                        val bitmap = imageProxyToBitmap(image)
                        _capturedImage.value = bitmap
                        Log.d("CameraViewModel", "Photo capture successful")
                    }
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e("CameraViewModel", "Photo capture failed", exception)
                }
            }
        )
    }

    fun processImageFromGallery(context: Context, imageUri: Uri) {
        // Clear previous analysis state when selecting from gallery
        clearAllStates()

        viewModelScope.launch {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
                }

                _capturedImage.value = bitmap
                Log.d("CameraViewModel", "Gallery image processed successfully")
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error processing gallery image", e)
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

        val rotationDegrees = image.imageInfo.rotationDegrees
        if (rotationDegrees == 0) return bitmap

        val matrix = android.graphics.Matrix()
        matrix.postRotate(rotationDegrees.toFloat())
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    fun clearCapturedImage() {
        _capturedImage.value = null
    }

    fun clearAnalysisState() {
        _analysisResult.value = null
        _isAnalyzing.value = false
    }

    // Add new function to clear all states
    fun clearAllStates() {
        _capturedImage.value = null
        _analysisResult.value = null
        _isAnalyzing.value = false
        _foodAddedToHomeScreen.value = null
    }

    fun analyzeImage() {
        val currentImage = _capturedImage.value ?: return

        _isAnalyzing.value = true
        viewModelScope.launch {
            try {
                val result = foodAnalysisService.analyzeFood(currentImage)
                if (result.isSuccess) {
                    val foodAnalysis = result.getOrNull()
                    _analysisResult.value = foodAnalysis

                    // Create a Food object from the result and add to repository
                    foodAnalysis?.let { analysis ->
                        val food = Food(
                            name = analysis.label.capitalize(),
                            description = "Detected with ${(analysis.probability * 100).toInt()}% confidence",
                            calories = 0,  // We don't have calorie info from the prediction
                            servingSize = "1 serving"
                        )
                        _foodAddedToHomeScreen.value = food

                        // Add to repository so it shows up in the home screen
                        foodRepository.addFood(food)
                    }
                } else {
                    Log.e("CameraViewModel", "Analysis failed", result.exceptionOrNull())
                }
            } catch (e: Exception) {
                Log.e("CameraViewModel", "Error during analysis", e)
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    fun clearAnalysisResult() {
        _analysisResult.value = null
        _foodAddedToHomeScreen.value = null
    }

    override fun onCleared() {
        super.onCleared()
        if (::cameraExecutor.isInitialized) {
            cameraExecutor.shutdown()
        }
    }
}
