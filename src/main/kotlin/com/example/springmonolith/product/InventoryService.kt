package com.example.springmonolith.product

import com.example.springmonolith.InventoryEvent
import com.example.springmonolith.OrderItemDTO
import com.example.springmonolith.totalQuantity
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
        // Demo logic: all quantities must be > 0 and total must stay under a simple cap
        val allPositive = items.all { it.quantity > 0 }
        val total = items.totalQuantity()
        return allPositive && total <= 1000
    }

    fun publishInventoryReserved(orderId: String) {
        events.publishEvent(InventoryEvent.InventoryReserved(orderId))
    }

    fun publishInventoryFailed(orderId: String, reason: String = "Insufficient stock") {
        events.publishEvent(InventoryEvent.InventoryFailed(orderId, reason))
    }
}
