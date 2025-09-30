package com.example.caloriecam.config

/**
 * Network configuration for the CalorieCam app
 * Change SERVER_IP to match your backend server address
 */
object NetworkConfig {
    // Change this IP address to match your backend server
    // For local development: "127.0.0.1" or "localhost"
    // For network access: your actual IP address (e.g., "192.168.1.4")
    const val SERVER_IP = "192.168.1.4"
    const val SERVER_PORT = "5000"

    // API endpoints
    const val BASE_URL = "http://$SERVER_IP:$SERVER_PORT"
    const val PREDICT_URL = "$BASE_URL/predict"
    const val PREDICTIONS_URL = "$BASE_URL/predictions"

    // Network timeouts (in milliseconds)
    const val CONNECTION_TIMEOUT = 30000
    const val READ_TIMEOUT = 30000
}
