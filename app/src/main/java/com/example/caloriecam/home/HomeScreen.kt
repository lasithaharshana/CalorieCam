package com.example.caloriecam.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.caloriecam.api.PredictionRecord
import com.example.caloriecam.data.PredictionRepository
import com.example.caloriecam.ui.components.LoadingCard
import com.example.caloriecam.ui.components.ModernButton
import com.example.caloriecam.ui.components.SectionHeader
import com.example.caloriecam.ui.components.StatCard
import com.example.caloriecam.ui.theme.Spacing
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun HomeScreen() {
    val predictionRepository = PredictionRepository.getInstance()
    val predictions by predictionRepository.predictions.collectAsState()
    val isLoading by predictionRepository.isLoading.collectAsState()
    val error by predictionRepository.error.collectAsState()
    val scope = rememberCoroutineScope()

    // Load predictions when the screen is first composed
    LaunchedEffect(Unit) {
        predictionRepository.loadPredictions()
    }

    // Calculate statistics
    val totalPredictions = predictions.size
    val averageCalories = if (predictions.isNotEmpty()) {
        predictions.map { it.prediction.caloriesPer100g }.average().toInt()
    } else 0
    val recentPredictions = predictions.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(Spacing.md)
    ) {
        SectionHeader(
            title = "CalorieCam",
            subtitle = "Food calorie tracker with AI",
            action = {
                ModernButton(
                    text = "Refresh",
                    icon = Icons.Default.Refresh,
                    onClick = {
                        scope.launch {
                            predictionRepository.loadPredictions()
                        }
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Statistics cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
        ) {
            StatCard(
                title = "Total Foods",
                value = totalPredictions.toString(),
                icon = Icons.Default.Restaurant,
                modifier = Modifier.weight(1f)
            )

            StatCard(
                title = "Avg Calories",
                value = averageCalories.toString(),
                subtitle = "per 100g",
                icon = Icons.Default.Analytics,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        // Recent predictions section
        SectionHeader(
            title = "Recent Foods",
            subtitle = "Latest food detections"
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Error handling
        error?.let { errorMessage ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Error: $errorMessage",
                    modifier = Modifier.padding(Spacing.md),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
        }

        // Content
        if (isLoading) {
            LoadingCard()
        } else if (recentPredictions.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "No food detected yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Use the camera to detect food!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Recent predictions list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                items(recentPredictions) { prediction ->
                    RecentFoodCard(prediction)
                }
            }
        }
    }
}

@Composable
fun RecentFoodCard(prediction: PredictionRecord) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = prediction.prediction.label.replaceFirstChar {
                        if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Confidence: ${(prediction.prediction.probability * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Text(
                text = "${prediction.prediction.caloriesPer100g} cal",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}