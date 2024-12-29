package com.example.productstore

data class Event(
    val id: Int,
    val date: String,
    val category: String,
    val product: String,
    val quantity: Int,
    val purchase_price: Double,
    val sale_price: Double,
    val revenue: Double
)