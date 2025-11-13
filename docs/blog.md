---
title: Keeping module boundaries honest with Spring Modulith and Kotlin
date: 2025-11-13
author: The Spring Monolith Team
---

# From forbidden calls to event-driven collaboration

When we introduced Spring Modulith to our Kotlin codebase, it immediately paid off: a test failed. Specifically, `ModularityTests.verifiesModularStructure()` flagged a forbidden dependency from the `order` module to the `product` module. That red bar was the nudge we needed to tighten boundaries and adopt a collaboration style that fits strict modularity.

This post walks through what we changed, why it matters, and the Kotlin techniques we used along the way.

## The initial smell

Our `order` module injected `ProductService` and called it directly. At the same time, both `order` and `product` were declared as Spring Modulith application modules with `allowedDependencies = {}` (see `src/main/java/com/example/springmonolith/order/package-info.java` and `src/main/java/com/example/springmonolith/product/package-info.java`). In other words, “no module dependencies allowed.” The moment `order` referenced a type from `product`, Modulith’s verification test failed — exactly as designed.

Symptoms:
- Coupling across modules (“order knows about product”).
- Modularity test failure in `src/test/kotlin/com/example/springmonolith/ModularityTests.kt`.

## The decision: collaborate via events

Instead of allowing direct calls, we shifted to event collaboration:
- The `order` module publishes `OrderPlaced` whenever an order is created.
- The `product` module listens, reserves inventory, and then publishes `InventoryReserved` or `InventoryFailed`.
- The `order` module listens to these outcomes and updates its own state.

This approach preserves strict boundaries while keeping the flow of information between modules.

## Key implementation changes

All changes are in the repository; below are the highlights with file references.

### 1) Place shared events in a neutral package

We introduced DTO-style events in the base package so they’re not treated as an application module by Modulith:
- File: `src/main/kotlin/com/example/springmonolith/Events.kt`
- Types: `OrderPlaced`, `OrderItemDTO`, and the sealed `InventoryEvent` hierarchy.

Why the base package? Spring Modulith discovers application modules by package structure. By placing cross-module event types in the base package, neither `order` nor `product` depends on the other, and we avoid creating a third module by accident.

### 2) Order publishes, not calls

`OrderService` now depends on `ApplicationEventPublisher` and publishes `OrderPlaced`:
- File: `src/main/kotlin/com/example/springmonolith/order/OrderService.kt`

Excerpt:
```kotlin
events.publishEvent(
    OrderPlaced(
        orderId = cmd.orderId,
        items = cmd.items
    )
)
```

We also added a simple POST endpoint for demos:
- File: `src/main/kotlin/com/example/springmonolith/order/OrderController.kt`
- DTO: `PlaceOrderRequest(orderId: String, items: List<OrderItemDTO>)`

### 3) Product reacts and responds

`InventoryPolicy` listens to `OrderPlaced` and delegates to `InventoryService`:
- Files:
  - `src/main/kotlin/com/example/springmonolith/product/InventoryPolicy.kt`
  - `src/main/kotlin/com/example/springmonolith/product/InventoryService.kt`

The service contains the demo logic (reserve if all quantities > 0) and then publishes either `InventoryEvent.InventoryReserved` or `InventoryEvent.InventoryFailed`.

We also migrated the listener annotation to the non-deprecated package:
- Import `org.springframework.modulith.events.ApplicationModuleListener`.
- Dependency added: `org.springframework.modulith:spring-modulith-events-api` in `build.gradle.kts`.

### 4) Order updates its own state

`OrderProcessManager` listens for inventory outcomes and updates an in-memory repository:
- File: `src/main/kotlin/com/example/springmonolith/order/OrderProcessManager.kt`
- Sealed state: `OrderStatus` with `New`, `ReadyToShip`, and `Cancelled(reason)`.
- In-memory `OrderRepository` to keep the example self-contained.

### 5) Tests and docs

We kept and ran Modulith tests:
- `ModularityTests.verifiesModularStructure()` now passes.
- Module-level smoke tests remain for both modules (`OrderModuleTests.kt`, `ProductModuleTests.kt`).

We also documented the refactor in long-form notes:
- `docs/why-domain-events.md`
- `docs/step-by-step-refactor-with-events.md`

## Kotlin features used (and why developers might care)

We didn’t just change wiring — we leaned on Kotlin’s language features to keep the code compact, type‑safe, and readable.

### Data classes: expressive, immutable messages
- Where: `src/main/kotlin/com/example/springmonolith/Events.kt`
- Why: Event payloads are pure data. Kotlin’s `data class` gives us value‑based `equals/hashCode`, `copy`, and a readable `toString` for free.

Example:
```kotlin
data class OrderPlaced(
    val orderId: String,
    val items: List<OrderItemDTO>
)

data class OrderItemDTO(val sku: String, val quantity: Int)
```

### Sealed classes: finite state, exhaustive handling
- Where: `InventoryEvent` in `Events.kt`, `OrderStatus` in `order/OrderProcessManager.kt`
- Why: A sealed hierarchy models a closed set of outcomes. The compiler can enforce exhaustive `when` handling, preventing “forgotten case” bugs.

Example (conceptual handler):
```kotlin
fun handle(event: InventoryEvent): String = when (event) {
    is InventoryEvent.InventoryReserved -> "ready to ship"
    is InventoryEvent.InventoryFailed   -> "cancelled: ${event.reason}"
}
```

### Named arguments and default values: self‑documenting calls
- Where: `OrderService.placeOrder` and `OrderController.place` use named args; `InventoryService.publishInventoryFailed` shows a default value for `reason`.
- Why: Named arguments read like documentation at the call site and reduce parameter ordering mistakes. Defaults cut boilerplate for common cases.

Examples:
```kotlin
// Named args when publishing the event
events.publishEvent(OrderPlaced(orderId = cmd.orderId, items = cmd.items))

// Default parameter value (reason) keeps simple calls tidy
fun publishInventoryFailed(orderId: String, reason: String = "Insufficient stock") { /* ... */ }
```

### Extension functions: add behavior without coupling
- Why: Extensions let you keep DTOs simple while still offering convenient, testable helpers.

Example (could live near tests):
```kotlin
fun List<OrderItemDTO>.totalQuantity(): Int = sumOf { it.quantity }
```


### Value classes (optional): type safety with zero runtime overhead
- Why: Wrapping primitives such as `orderId` and `sku` prevents accidental mix‑ups.
- Note: You can introduce them incrementally without touching call sites much.

Example:
```kotlin
@JvmInline
value class OrderId(val value: String)

@JvmInline
value class Sku(val value: String)
```

Bringing these features together gives us code that reads well, is harder to misuse, and stays aligned with strict module boundaries.

## Design notes: boundaries and eventual consistency

Two important architectural choices underpin the refactor:

1) Strict boundaries. The `order` module doesn’t reference `product` types, and vice versa. The only shared types are events in the base package. This keeps compile-time dependencies clean, which is what Modulith enforces by design.

2) Eventual consistency. `placeOrder` returns immediately with the local result (`OrderAccepted`) while inventory reservation happens asynchronously. That’s acceptable because we separated the command (create order) from the policy (reserve inventory). When the product side publishes a result, `order` reconciles its state.

If you need synchronous reads later (e.g., pricing), consider a tiny `catalog-api` module that both modules may depend on, keeping the implementation in `product` and the API surface minimal.

## Deprecation fix: listener annotation

Spring Modulith deprecated `org.springframework.modulith.ApplicationModuleListener` and moved it to `org.springframework.modulith.events.ApplicationModuleListener`. We:
- Updated imports in `InventoryPolicy.kt` and `OrderProcessManager.kt`.
- Added `implementation("org.springframework.modulith:spring-modulith-events-api")` to `build.gradle.kts` (version via the Modulith BOM).

Result: forward-compatible code and cleaner imports.

## How to try it

1) Start the app.
2) Place an order (note the use of named parameters in the service call; the API payload uses simple DTOs):

```bash
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "orderId": "O-123",
    "items": [ { "sku": "P-1", "quantity": 2 } ]
  }'
```

3) Check logs: you should see `OrderPlaced` published, `product` reacting, and a follow-up `InventoryReserved` (or `InventoryFailed` if you send a non-positive quantity).

## What we learned

- Tests that enforce architecture are worth their weight — they prevent drift.
- Collaboration style is as important as code style. Events gave us autonomy and clarity.
- Kotlin’s type system (data, sealed, value-like DTOs) makes modeling flow states pleasant and safe.

## Closing thoughts

The move from direct calls to events wasn’t about adding buzzwords; it was about making boundaries explicit and collaboration intentional. With Spring Modulith keeping us honest and Kotlin keeping us expressive, the codebase is now easier to evolve without unintentionally tangling modules.

If you’re working in a modular monolith and see similar smells, consider adopting events where coupling hurts — your future self (and your tests) will thank you.
