package com.bca.food_ordering_app.model

data class UserModel(

    val name: String ?= null,
    val password: String ?= null,
    val email: String ?= null,
    val phone: String ?= null,
    val address: String ?=null,
    val emailVerified: Boolean = false
)
