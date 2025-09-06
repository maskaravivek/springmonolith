package com.example.springmonolith.order

import org.springframework.stereotype.Service

@Service
class OrderService {

    fun getGreeting(): String {
        return "Hello from Order Module! 🛒"
    }
}
