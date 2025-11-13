package com.example.springmonolith.product

import com.example.springmonolith.OrderPlaced
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

/**
 * Application module listener that reacts to OrderPlaced events coming from the order module.
 * Note: The event type is defined in the neutral common.events package to avoid cross-module
 * dependencies between application modules with allowedDependencies = {}.
 */
@Component
class InventoryPolicy(
    private val inventory: InventoryService
) {
    @ApplicationModuleListener
    fun on(event: OrderPlaced) {
        val reserved = inventory.reserve(items = event.items)
        if (reserved) inventory.publishInventoryReserved(orderId = event.orderId)
        else inventory.publishInventoryFailed(orderId = event.orderId)
    }
}
