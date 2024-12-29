package com.example.productstore

data class Product(
    val name: String,
    val quantity: Int,
    val purchasePrice: Double,
    val salePrice: Double,
    val category: String
)