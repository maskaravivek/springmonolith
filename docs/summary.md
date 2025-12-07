Step by step refactor to meet modularity constaints and demonstrate Kotlin language features

### Summary
This PR replaces a forbidden cross‑module call (order ➜ product) with event‑driven collaboration using Spring Modulith, fixes a deprecated listener import, and highlights key Kotlin features for clarity and safety. It keeps both application modules (`order`, `product`) isolated while enabling the same business flow through events.

### Context and rationale
- Problem: `ModularityTests.verifiesModularStructure()` failed because `order` directly invoked `product` via `ProductService`, despite both modules declaring `allowedDependencies = {}`.
- Decision: Adopt event collaboration. `order` publishes `OrderPlaced`; `product` reacts, attempts inventory reservation, and publishes either `InventoryReserved` or `InventoryFailed`. `order` listens to those outcomes and updates local state.
- Bonus: Use Kotlin’s strengths (data classes, sealed classes, value classes, named args, extensions) to make the model and flow explicit without adding coupling.

### Scope of changes (code)
- Shared events (neutral package so they aren’t an application module)
    - Added/updated `src/main/kotlin/com/example/springmonolith/Events.kt`
        - `OrderPlaced(orderId: String, items: List<OrderItemDTO>)`
        - `OrderItemDTO(sku: String, quantity: Int)`
        - `sealed class InventoryEvent { InventoryReserved, InventoryFailed }`

- Order module
    - `OrderService` now publishes `OrderPlaced` via `ApplicationEventPublisher` and returns the sealed supertype `OrderResult`.
    - `OrderProcessManager` listens to `InventoryEvent.*` and updates local state.
    - `OrderRepository` stores `OrderStatus` keyed by a value class `OrderId`.
    - Introduced value class and adapter: `order/Ids.kt` (`@JvmInline value class OrderId`, `String.asOrderId()`).
    - `OrderController.place(...)` now returns `OrderResult` (JSON remains `OrderAccepted` for the happy path).

- Product module
    - `InventoryPolicy` listens to `OrderPlaced` using `org.springframework.modulith.events.ApplicationModuleListener`.
    - `InventoryService.reserve(...)` uses a Kotlin extension `totalQuantity()` to keep DTOs simple but useful, and publishes outcome events.

- Kotlin extension (shared)
    - `src/main/kotlin/com/example/springmonolith/Extensions.kt`: `fun List<OrderItemDTO>.totalQuantity(): Int`.

### Build and dependency updates
- Gradle: `implementation("org.springframework.modulith:spring-modulith-events-api")` added to use the new listener annotation package.
- Modulith BOM: managed via `org.springframework.modulith:spring-modulith-bom:1.3.1`.

### Kotlin features demonstrated
- Data classes: events and DTOs (`OrderPlaced`, `OrderItemDTO`).
- Sealed classes: `InventoryEvent` outcomes and `OrderStatus` state.
- Value class: `OrderId` used internally by the order module for stronger typing without runtime cost.
- Named arguments and default values: clearer call sites (e.g., `publishInventoryFailed(orderId, reason = "...")`).
- Extension function: `List<OrderItemDTO>.totalQuantity()` used in production code (`InventoryService.reserve`).
- Note: A trailing‑lambda DSL was explored earlier but removed from the repository per product decision; the blog now treats DSLs as optional/illustrative.