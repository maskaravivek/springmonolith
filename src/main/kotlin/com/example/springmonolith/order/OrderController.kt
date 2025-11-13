package com.example.springmonolith.order

import com.example.springmonolith.OrderItemDTO
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/orders")
class OrderController(private val orderService: OrderService) {

    @GetMapping("/hello")
    fun hello(): String {
        return orderService.getGreeting()
    }

    /**
     * Demo endpoint to place an order and trigger an OrderPlaced event.
     * Uses named parameters when calling the service.
     */
    @PostMapping
    fun place(@RequestBody request: PlaceOrderRequest): OrderAccepted {
        return orderService.placeOrder(
            cmd = PlaceOrderCommand(
                orderId = request.orderId,
                items = request.items
            )
        )
    }
}

data class PlaceOrderRequest(
    val orderId: String,
    val items: List<OrderItemDTO>
)
