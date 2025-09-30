package com.example.caloriecam.api

import android.graphics.Bitmap
import android.util.Log
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
    val probability: Double
)

class FoodAnalysisService {
    companion object {
        // Server configuration - change this IP address as needed
        private const val SERVER_IP = "192.168.1.4" // Using 10.0.2.2 instead of 127.0.0.1 to access the host machine from Android emulator
        private const val SERVER_PORT = "5000"
        private const val SERVER_URL = "http://$SERVER_IP:$SERVER_PORT/predict"

        private const val TAG = "FoodAnalysisService"
        private  val BOUNDARY = "----WebKitFormBoundary" + UUID.randomUUID().toString().substring(0, 16)
        private const val CRLF = "\r\n"
    }

    suspend fun analyzeFood(bitmap: Bitmap): Result<FoodAnalysisResult> = withContext(Dispatchers.IO) {
        try {
            // Set up connection
            val url = URL(SERVER_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.doInput = true
            connection.useCaches = false
            connection.setRequestProperty("Connection", "Keep-Alive")
            connection.setRequestProperty("Cache-Control", "no-cache")
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$BOUNDARY")

            Log.d(TAG, "Sending multipart request to $SERVER_URL")

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
                probability = prediction.getDouble("probability")
            )

            Result.success(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing food", e)
            return@withContext Result.failure(e)
        }
    }
}
