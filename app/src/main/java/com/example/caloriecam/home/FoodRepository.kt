package com.example.caloriecam.home

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Repository to manage food items
 */
class FoodRepository {
    // Store both predefined and detected food items
    private val _foods = MutableStateFlow<List<Food>>(dummyFoodData)
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()

    // Add a new food item from detection
    fun addFood(food: Food) {
        val currentList = _foods.value.toMutableList()
        currentList.add(0, food) // Add at the beginning (newest first)
        _foods.value = currentList
    }

    companion object {
        // Singleton instance
        private var INSTANCE: FoodRepository? = null

        fun getInstance(): FoodRepository {
            return INSTANCE ?: synchronized(this) {
                val instance = FoodRepository()
                INSTANCE = instance
                instance
            }
        }
    }
}
