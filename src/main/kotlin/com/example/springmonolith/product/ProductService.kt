package com.example.springmonolith.product

import org.springframework.stereotype.Service

@Service
class ProductService {

    fun getGreeting(): String {
        return "Hello from Product Module! 📦"
    }
}
