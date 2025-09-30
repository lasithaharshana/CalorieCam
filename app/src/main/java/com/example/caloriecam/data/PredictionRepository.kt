package com.example.caloriecam.data

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

    // Store predictions from server
    private val _predictions = MutableStateFlow<List<PredictionRecord>>(emptyList())
    val predictions: StateFlow<List<PredictionRecord>> = _predictions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    suspend fun loadPredictions() {
        _isLoading.value = true
        _error.value = null

        try {
            val result = foodAnalysisService.getAllPredictions()
            result.fold(
                onSuccess = { response ->
                    _predictions.value = response.predictions
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to load predictions"
                }
            )
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
        } finally {
            _isLoading.value = false
        }
    }

    suspend fun deletePrediction(predictionId: String): Boolean {
        _error.value = null

        return try {
            val result = foodAnalysisService.deletePrediction(predictionId)
            result.fold(
                onSuccess = {
                    // Remove from local list
                    val currentList = _predictions.value.toMutableList()
                    currentList.removeAll { it.id == predictionId }
                    _predictions.value = currentList
                    true
                },
                onFailure = { exception ->
                    _error.value = exception.message ?: "Failed to delete prediction"
                    false
                }
            )
        } catch (e: Exception) {
            _error.value = e.message ?: "Unknown error occurred"
            false
        }
    }

    fun clearError() {
        _error.value = null
    }

    companion object {
        // Singleton instance
        private var INSTANCE: PredictionRepository? = null

        fun getInstance(): PredictionRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = PredictionRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}
