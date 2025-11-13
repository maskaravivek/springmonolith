# Why refactor to domain events?

Date: 2025-11-13

## Problem
- The `order` module directly depended on the `product` module via `ProductService`.
- Both modules are annotated as Spring Modulith application modules with `allowedDependencies = {}`.
- `ApplicationModules.verify()` therefore failed due to a forbidden cross‑module dependency.

## Decision
Adopt event collaboration between modules:
- `order` publishes `OrderPlaced`.
- `product` listens, attempts reservation, and publishes either `InventoryReserved` or `InventoryFailed`.
- `order` listens to inventory outcomes and updates its local state.

## Implementation outline
1. Moved shared event DTOs into the base package `com.example.springmonolith` (file: `src/main/kotlin/com/example/springmonolith/Events.kt`). Placing them at the base avoids introducing another application module while keeping types reusable.
2. `OrderService` now depends only on `ApplicationEventPublisher` and publishes `OrderPlaced` when `placeOrder()` is called. It no longer knows about `product`.
3. `product` module contains `InventoryPolicy` (a Modulith `@ApplicationModuleListener`) which reacts to `OrderPlaced` using an `InventoryService`. That service publishes either `InventoryReserved` or `InventoryFailed`.
4. `order` module contains an `OrderProcessManager` that listens to inventory events and updates an in‑memory `OrderRepository` to showcase state transitions.
5. `OrderController` provides a POST endpoint to place an order using a simple request DTO, demonstrating Kotlin named parameters.

## Listener annotation moved (deprecation fix)
- Spring Modulith deprecated `org.springframework.modulith.ApplicationModuleListener` (since 1.1) and moved it to `org.springframework.modulith.events.ApplicationModuleListener`.
- Code changes:
  - Updated imports in `order/OrderProcessManager.kt` and `product/InventoryPolicy.kt` to use `org.springframework.modulith.events.ApplicationModuleListener`.
  - Added Gradle dependency `implementation("org.springframework.modulith:spring-modulith-events-api")` (version managed by the Modulith BOM) to make the new annotation available at compile time.
  - All tests pass after the migration.

## Why this complies with Spring Modulith
- There is no type dependency from `order` to `product`, or vice‑versa.
- Both modules collaborate exclusively via events in the base package, which is not an application module.
- `ModularityTests.verifiesModularStructure()` now passes.

## Kotlin features demonstrated
- Named parameters (e.g., when building `PlaceOrderCommand`).
- Sealed classes for `OrderStatus` and inventory events.
- Data classes and value‑like DTOs (`OrderItemDTO`).
- Trailing lambdas/DSLs can be added incrementally (a tiny start is in the service/controller).

## Next ideas (optional)
- Extract a tiny `catalog-api` module for synchronous reads if needed, allowing only `order -> catalog-api` and `product -> catalog-api`.
- Use context receivers for lightweight logging/tracing across domain functions.
- Add module tests that assert event publication/handling with Modulith’s testing support.
