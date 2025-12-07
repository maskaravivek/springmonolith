package com.example.springmonolith.order

@JvmInline
value class OrderId(val value: String)

/**
 * Adapter to convert shared DTO id (String) into the order module's value class.
 * Keeps the shared events simple while allowing stronger typing inside the module.
 */
fun String.asOrderId(): OrderId = OrderId(this)
