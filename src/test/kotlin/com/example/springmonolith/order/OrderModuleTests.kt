package com.example.springmonolith.order

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertTrue

/**
 * Integration test for the order module using Spring Modulith's @ApplicationModuleTest.
 * This test only bootstraps the order module and its dependencies.
 */
@ApplicationModuleTest
@SpringJUnitConfig
class OrderModuleTests {

    @Autowired
    private lateinit var orderService: OrderService

    @Test
    fun contextLoads() {
        // Verify that the order service is properly injected
        assertTrue(::orderService.isInitialized)
    }

    @Test
    fun testOrderServiceGreeting() {
        val greeting = orderService.getGreeting()
        assertTrue(greeting.contains("Order Module"))
        assertTrue(greeting.contains("🛒"))
    }
}
