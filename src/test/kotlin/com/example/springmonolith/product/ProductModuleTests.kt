package com.example.springmonolith.product

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.modulith.test.ApplicationModuleTest
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig
import kotlin.test.assertTrue

/**
 * Integration test for the product module using Spring Modulith's @ApplicationModuleTest.
 * This test only bootstraps the product module and its dependencies.
 */
@ApplicationModuleTest
@SpringJUnitConfig
class ProductModuleTests {

    @Autowired
    private lateinit var productService: ProductService

    @Test
    fun contextLoads() {
        // Verify that the product service is properly injected
        assertTrue(::productService.isInitialized)
    }

    @Test
    fun testProductServiceGreeting() {
        val greeting = productService.getGreeting()
        assertTrue(greeting.contains("Product Module"))
        assertTrue(greeting.contains("📦"))
    }
}
