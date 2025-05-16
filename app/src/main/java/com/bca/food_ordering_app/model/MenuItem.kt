package com.bca.food_ordering_app.model

data class MenuItem(
    val foodName: String ?= null,
    val foodDescription: String ?= null,
    val foodIngredient: String ?= null,
    val foodPrice: String?= null
)
