package com.example.caloriecam.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
fun HomeScreen(
    onNavigateToCamera: () -> Unit,
    modifier: Modifier = Modifier,
    predictionRepository: PredictionRepository = remember { PredictionRepository() }
) {
    val predictions by predictionRepository.predictions.collectAsState()
    val isLoading by predictionRepository.isLoading.collectAsState()
    val error by predictionRepository.error.collectAsState()
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    // Load predictions when screen is first displayed
    LaunchedEffect(Unit) {
        predictionRepository.refreshPredictions()
    }

    PullToRefreshBox(
        isRefreshing = isLoading,
        onRefresh = {
            scope.launch {
                predictionRepository.refreshPredictions()
            }
        },
        state = pullToRefreshState,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.md)
        ) {
            // Header
            SectionHeader(
                title = "CalorieCam",
                subtitle = "AI-powered food detection and calorie tracking"
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Stats Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                StatCard(
                    title = "Total Scans",
                    value = predictionRepository.getTotalCount().toString(),
                    subtitle = "Food items detected",
                    icon = Icons.Default.Restaurant,
                    modifier = Modifier.weight(1f)
                )

                StatCard(
                    title = "Today's Calories",
                    value = "${predictionRepository.getTotalCaloriesToday()}",
                    subtitle = "Calories consumed",
                    icon = Icons.Default.Timeline,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Camera Button
            ModernButton(
                text = "Scan Food",
                onClick = onNavigateToCamera,
                icon = Icons.Default.CameraAlt,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(Spacing.lg))

            // Recent Detections Section
            SectionHeader(
                title = "Recent Detections",
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
                                text = "No food detected yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(Spacing.sm))
                            Text(
                                text = "Use the camera to detect food!",
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
                            items = predictionRepository.getRecentPredictions(),
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
}