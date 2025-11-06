package com.example.springmonolith.order

import com.example.springmonolith.product.ProductService
import org.springframework.stereotype.Service

@Service
class OrderService(private val productService: ProductService) {

    fun getGreeting(): String {
        // This demonstrates a FORBIDDEN cross-module dependency!
        // The order module declares allowedDependencies = {} so it cannot depend on product.
        // Spring Modulith's ModularityTests.verifiesModularStructure() will catch this violation.
        return "Hello from Order Module! 🛒 (and ${productService.getGreeting()})"
    }
}