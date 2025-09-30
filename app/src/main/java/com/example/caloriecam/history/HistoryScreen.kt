package com.example.caloriecam.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.example.caloriecam.ui.components.*
import com.example.caloriecam.ui.theme.Spacing
import com.example.caloriecam.data.PredictionRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    predictionRepository: PredictionRepository = remember { PredictionRepository() }
) {
    val predictions by predictionRepository.predictions.collectAsState()
    val isLoading by predictionRepository.isLoading.collectAsState()
    val error by predictionRepository.error.collectAsState()
    val scope = rememberCoroutineScope()

    // Load predictions when screen is first displayed
    LaunchedEffect(Unit) {
        predictionRepository.refreshPredictions()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.md)
    ) {
        // Header with refresh action
        SectionHeader(
            title = "Food History",
            subtitle = "Your food detection history",
            action = {
                IconButton(
                    onClick = {
                        scope.launch {
                            predictionRepository.refreshPredictions()
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh history"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Stats
        if (predictions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                StatCard(
                    title = "Total Items",
                    value = predictions.size.toString(),
                    subtitle = "Food detections",
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Avg Calories",
                    value = if (predictions.isNotEmpty()) {
                        "${predictions.map { it.prediction.caloriesPer100g }.average().toInt()}"
                    } else "0",
                    subtitle = "Per 100g",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))
        }

        // Error handling
        error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Error: $errorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            predictionRepository.clearError()
                            scope.launch {
                                predictionRepository.refreshPredictions()
                            }
                        }
                    ) {
                        Text("Retry")
                    }
                }
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // Content
        when {
            isLoading && predictions.isEmpty() -> {
                LoadingCard()
            }
            predictions.isEmpty() -> {
                ModernCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Spacing.lg),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No food history yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "Start by taking a photo of your food!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Spacing.md)
                ) {
                    items(
                        items = predictions,
                        key = { it.id }
                    ) { prediction ->
                        FoodItemCard(
                            prediction = prediction,
                            onDelete = { predictionId ->
                                scope.launch {
                                    predictionRepository.deletePrediction(predictionId)
                                }
                            },
                            onClick = {
                                // Optional: Navigate to detailed view
                            }
                        )
                    }

                    if (isLoading) {
                        item {
                            LoadingCard()
                        }
                    }
                }
            }
        }
    }
}
