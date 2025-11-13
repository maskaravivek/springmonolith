package com.example.springmonolith.product

import com.example.springmonolith.InventoryEvent
import com.example.springmonolith.OrderItemDTO
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * Simple domain service that decides if inventory can be reserved.
 * In a real system, this would check a repository or external system.
 */
@Service
class InventoryService(
    private val events: ApplicationEventPublisher
) {
    fun reserve(items: List<OrderItemDTO>): Boolean {
        // Demo logic: fail if any quantity is <= 0; otherwise succeed
        return items.all { it.quantity > 0 }
    }

    fun publishInventoryReserved(orderId: String) {
        events.publishEvent(InventoryEvent.InventoryReserved(orderId))
    }

    fun publishInventoryFailed(orderId: String, reason: String = "Insufficient stock") {
        events.publishEvent(InventoryEvent.InventoryFailed(orderId, reason))
    }
}
