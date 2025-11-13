package com.example.springmonolith

/**
 * Shared event DTOs placed in the base package so they are NOT considered an
 * application module by Spring Modulith. This allows strict modules (order, product)
 * with allowedDependencies = {} to collaborate via events without cross-module deps.
 */

sealed interface DomainEvent

// ---- Order -> Product ----
data class OrderPlaced(
    val orderId: String,
    val items: List<OrderItemDTO>
) : DomainEvent

data class OrderItemDTO(val sku: String, val quantity: Int)

// ---- Product -> Order ----
sealed class InventoryEvent : DomainEvent {
    data class InventoryReserved(val orderId: String) : InventoryEvent()
    data class InventoryFailed(val orderId: String, val reason: String) : InventoryEvent()
}
