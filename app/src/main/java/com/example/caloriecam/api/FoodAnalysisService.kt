package com.example.caloriecam.api

import android.graphics.Bitmap
import android.util.Log
import com.example.caloriecam.config.NetworkConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.UUID

data class FoodAnalysisResult(
    val label: String,
    val probability: Double,
    val caloriesPer100g: Int = 0
)

data class PredictionRecord(
    val id: String,
    val filename: String,
    val prediction: FoodAnalysisResult,
    val timestamp: String
)

data class PredictionsResponse(
    val predictions: List<PredictionRecord>,
    val totalCount: Int
)

class FoodAnalysisService {
    companion object {
        private const val TAG = "FoodAnalysisService"
        private val BOUNDARY = "----WebKitFormBoundary" + UUID.randomUUID().toString().substring(0, 16)
        private const val CRLF = "\r\n"
    }

    suspend fun analyzeFood(bitmap: Bitmap): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            // Set up connection
            val url = URL(NetworkConfig.PREDICT_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.useCaches = false
            connection.connectTimeout = NetworkConfig.CONNECTION_TIMEOUT
            connection.readTimeout = NetworkConfig.READ_TIMEOUT
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")

            Log.d(TAG, "Sending multipart request to ${NetworkConfig.PREDICT_URL}")

            // Get the bitmap bytes
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
            val imageBytes = outputStream.toByteArray()

            // Write multipart form data to output stream
            connection.outputStream.use { os ->
                val writer = DataOutputStream(os)

                // Add file part
                writer.write(("--$BOUNDARY$CRLF").toByteArray(StandardCharsets.UTF_8))
                writer.write(("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"$CRLF").toByteArray(StandardCharsets.UTF_8))
                writer.write(("Content-Type: image/jpeg$CRLF$CRLF").toByteArray(StandardCharsets.UTF_8))

                // Write image bytes
                writer.write(imageBytes)
                writer.write(CRLF.toByteArray(StandardCharsets.UTF_8))

                // End of multipart form
                writer.write(("--$BOUNDARY--$CRLF").toByteArray(StandardCharsets.UTF_8))
                writer.flush()
            }

            // Check response code
            val responseCode = connection.responseCode
            Log.d(TAG, "Server response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                Log.e(TAG, "Error response: $errorResponse")
                return@withContext Result.failure(
                    Exception("Server responded with code: $responseCode")
                )
            }

            // Parse response
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "Server response: $response")

            val jsonResponse = JSONObject(response)
            val prediction = jsonResponse.getJSONObject("prediction")

            val result = FoodAnalysisResult(
                label = prediction.getString("label"),
                probability = prediction.getDouble("probability"),
                caloriesPer100g = prediction.optInt("calories_per_100g", 0)
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing food", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun getAllPredictions(): Result<PredictionsResponse> = withContext(Dispatchers.IO) {
        try {
            val url = URL(NetworkConfig.PREDICTIONS_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = NetworkConfig.CONNECTION_TIMEOUT
            connection.readTimeout = NetworkConfig.READ_TIMEOUT
            connection.setRequestProperty("Content-Type", "application/json")

            Log.d(TAG, "Fetching predictions from ${NetworkConfig.PREDICTIONS_URL}")

            val responseCode = connection.responseCode
            Log.d(TAG, "Server response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                Log.e(TAG, "Error response: $errorResponse")
                return@withContext Result.failure(
                    Exception("Server responded with code: $responseCode")
                )
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "Predictions response: $response")

            val jsonResponse = JSONObject(response)
            val predictionsArray = jsonResponse.getJSONArray("predictions")
            val totalCount = jsonResponse.getInt("total_count")

            val predictions = mutableListOf<PredictionRecord>()
            for (i in 0 until predictionsArray.length()) {
                val predictionObj = predictionsArray.getJSONObject(i)
                val predictionData = predictionObj.getJSONObject("prediction")

                val prediction = PredictionRecord(
                    id = predictionObj.getString("_id"),
                    filename = predictionObj.getString("filename"),
                    prediction = FoodAnalysisResult(
                        label = predictionData.getString("label"),
                        probability = predictionData.getDouble("probability"),
                        caloriesPer100g = predictionData.optInt("calories_per_100g", 0)
                    ),
                    timestamp = predictionObj.getString("timestamp")
                )
                predictions.add(prediction)
            }

            Result.success(PredictionsResponse(predictions, totalCount))
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching predictions", e)
            return@withContext Result.failure(e)
        }
    }

    suspend fun deletePrediction(predictionId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val url = URL("${NetworkConfig.PREDICTIONS_URL}/$predictionId")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "DELETE"
            connection.connectTimeout = NetworkConfig.CONNECTION_TIMEOUT
            connection.readTimeout = NetworkConfig.READ_TIMEOUT
            connection.setRequestProperty("Content-Type", "application/json")

            Log.d(TAG, "Deleting prediction $predictionId")

            val responseCode = connection.responseCode
            Log.d(TAG, "Delete response code: $responseCode")

            if (responseCode != HttpURLConnection.HTTP_OK) {
                val errorStream = connection.errorStream
                val errorResponse = errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                Log.e(TAG, "Error response: $errorResponse")
                return@withContext Result.failure(
                    Exception("Server responded with code: $responseCode")
                )
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            Log.d(TAG, "Delete response: $response")

            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting prediction", e)
            return@withContext Result.failure(e)
        }
    }
}
