package com.example.springmonolith.order

import com.example.springmonolith.OrderItemDTO
import com.example.springmonolith.OrderPlaced
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

/**
 * OrderService now publishes domain events instead of calling into other modules directly.
 *
 * Rationale:
 * - The order module is annotated with @ApplicationModule(allowedDependencies = {}),
 *   which forbids any dependency on the product module.
 * - Previously we injected ProductService, causing Spring Modulith to flag a violation.
 * - By publishing an OrderPlaced event, we decouple order from product. Other modules
 *   (e.g., product) can react to the event without order knowing about them.
 */
@Service
class OrderService(
    private val events: ApplicationEventPublisher
) {

    fun getGreeting(): String = "Hello from Order Module! 🛒"

    /**
     * Place an order and publish an OrderPlaced event.
     * Demonstrates named parameters and a tiny builder-style DSL via trailing lambda in the controller.
     */
    fun placeOrder(cmd: PlaceOrderCommand): OrderResult {
        val items = cmd.items
        // Publish an integration event with DTO payload from the shared common package
        events.publishEvent(
            OrderPlaced(
                orderId = cmd.orderId,
                items = items
            )
        )
        return OrderAccepted(orderId = cmd.orderId)
    }
}

/**
 * Command object used to place an order.
 * Using simple DTOs in the common.events package ensures no cross-module dependencies.
 */
data class PlaceOrderCommand(
    val orderId: String,
    val items: List<OrderItemDTO>
)

/** Sealed result type example kept minimal for now. */
sealed class OrderResult
data class OrderAccepted(val orderId: String) : OrderResult()