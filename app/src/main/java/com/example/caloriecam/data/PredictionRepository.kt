package com.example.caloriecam.data

import android.util.Log
import com.example.caloriecam.api.FoodAnalysisService
import com.example.caloriecam.api.PredictionRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository to manage food prediction data from MongoDB
 */
class PredictionRepository {
    private val foodAnalysisService = FoodAnalysisService()

    private val _predictions = MutableStateFlow<List<PredictionRecord>>(emptyList())
    val predictions: StateFlow<List<PredictionRecord>> = _predictions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    companion object {
        private const val TAG = "PredictionRepository"
    }

    /**
     * Fetch all predictions from the server
     */
    suspend fun refreshPredictions() {
        _isLoading.value = true
        _error.value = null

        try {
            val result = foodAnalysisService.getAllPredictions()
            result.onSuccess { response ->
                _predictions.value = response.predictions
                Log.d(TAG, "Successfully fetched ${response.predictions.size} predictions")
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to fetch predictions"
                Log.e(TAG, "Error fetching predictions", exception)
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            Log.e(TAG, "Exception while fetching predictions", e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Delete a prediction by ID
     */
    suspend fun deletePrediction(predictionId: String) {
        _isLoading.value = true
        _error.value = null

        try {
            val result = foodAnalysisService.deletePrediction(predictionId)
            result.onSuccess {
                // Remove the deleted prediction from local state
                val currentPredictions = _predictions.value.toMutableList()
                currentPredictions.removeAll { it.id == predictionId }
                _predictions.value = currentPredictions
                Log.d(TAG, "Successfully deleted prediction: $predictionId")
            }.onFailure { exception ->
                _error.value = exception.message ?: "Failed to delete prediction"
                Log.e(TAG, "Error deleting prediction", exception)
            }
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            Log.e(TAG, "Exception while deleting prediction", e)
        } finally {
            _isLoading.value = false
        }
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }

    /**
     * Get recent predictions (last 5)
     */
    fun getRecentPredictions(): List<PredictionRecord> {
        return _predictions.value.take(5)
    }

    /**
     * Get total count of predictions
     */
    fun getTotalCount(): Int {
        return _predictions.value.size
    }
}
