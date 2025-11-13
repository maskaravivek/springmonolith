package com.example.springmonolith

/**
 * Kotlin extension function to demonstrate adding behavior to DTO collections
 * without coupling modules. Lives in the base package alongside shared DTOs.
 */
fun List<OrderItemDTO>.totalQuantity(): Int = sumOf { it.quantity }
