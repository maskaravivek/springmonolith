package com.example.springmonolith.order

import com.example.springmonolith.InventoryEvent
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

/**
 * OrderProcessManager listens to inventory outcomes and updates local state.
 * This demonstrates handling follow-up events without depending on the product module.
 */
@Component
class OrderProcessManager(
    private val repo: OrderRepository
) {
    @ApplicationModuleListener
    fun onReserved(event: InventoryEvent.InventoryReserved) {
        repo.updateStatus(event.orderId, OrderStatus.ReadyToShip)
    }

    @ApplicationModuleListener
    fun onFailed(event: InventoryEvent.InventoryFailed) {
        repo.updateStatus(event.orderId, OrderStatus.Cancelled(reason = event.reason))
    }
}

/** Simple sealed status hierarchy for the order module. */
sealed class OrderStatus {
    data object New : OrderStatus()
    data object ReadyToShip : OrderStatus()
    data class Cancelled(val reason: String) : OrderStatus()
}

/** Minimal in-memory repository to demonstrate state changes. */
@Component
class OrderRepository {
    private val state: MutableMap<String, OrderStatus> = mutableMapOf()

    fun updateStatus(orderId: String, status: OrderStatus) {
        state[orderId] = status
    }

    fun statusOf(orderId: String): OrderStatus? = state[orderId]
}
