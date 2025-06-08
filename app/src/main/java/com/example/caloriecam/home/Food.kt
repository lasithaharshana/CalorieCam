package com.example.caloriecam.home

data class Food(
    val name: String,
    val description: String,
    val calories: Int,
    val servingSize: String = "1 serving"
)

// Dummy food data
val dummyFoodData = listOf(
    Food("Apple", "Medium sized fresh apple", 95, "1 medium"),
    Food("Banana", "Medium sized banana", 105, "1 medium"),
    Food("Grilled Chicken Breast", "Skinless, boneless", 165, "100g"),
    Food("Avocado", "Fresh, California", 240, "1 whole"),
    Food("Salmon", "Atlantic, wild-caught", 206, "100g"),
    Food("Brown Rice", "Cooked", 216, "1 cup"),
    Food("Greek Yogurt", "Plain, non-fat", 100, "6 oz"),
    Food("Broccoli", "Steamed", 55, "1 cup"),
    Food("Egg", "Large, whole", 70, "1 egg"),
    Food("Almonds", "Raw", 164, "1 oz (23 almonds)")
)