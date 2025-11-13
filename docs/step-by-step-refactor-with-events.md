### Why `ModularityTests.verifiesModularStructure()` fails
- In `OrderService.kt` lines 3 and 7–13, the `order` module directly depends on `product` by constructor‑injecting `ProductService` and calling `productService.getGreeting()`.
- Your `order` module declares `allowedDependencies = {}` (no allowed module dependencies). Any type reference from `order` to `product` is a forbidden cross‑module dependency, which Spring Modulith’s `ApplicationModules.verify()` catches.

### Target architecture (still a single deployable monolith, but modular inside)
To respect “order cannot depend on product,” choose one of these Modulith‑friendly collaboration styles:
1) Event collaboration (recommended with `allowedDependencies = {}`)
    - `order` publishes domain events (e.g., `OrderPlaced`).
    - `product` listens and reacts (reserve stock, etc.), possibly publishing a follow‑up event (e.g., `InventoryReserved`).
    - If `order` needs to react to that, it listens and updates its state. All interactions are via events — no type dependency from `order` to `product`.

2) Shared API module (if you need synchronous calls)
    - Introduce a third module `catalog-api` that exposes only an API (interfaces + DTOs). Both `order` and `product` are allowed to depend on `catalog-api`, but `order` still does not depend on `product`.
    - `product` provides the implementation of the API; `order` calls the API interface.

Given your constraint `allowedDependencies = {}` on `order`, Option 1 (events) is the cleanest because it avoids any compile‑time dependency entirely.

Below, I’ll show the event approach and simultaneously demonstrate Kotlin language features you asked for (named parameters, trailing lambdas, builders/DSLs, sealed classes, extension functions, and context parameters).

---

### Step‑by‑step refactor using domain events

#### 1) Remove the forbidden dependency from `OrderService`
Replace constructor injection of `ProductService` with `ApplicationEventPublisher` (or Spring Modulith’s event publisher). Keep `order` autonomous.

```kotlin
// src/main/kotlin/com/example/springmonolith/order/OrderService.kt
package com.example.springmonolith.order

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service

@Service
class OrderService(
    private val events: ApplicationEventPublisher
) {
    fun placeOrder(cmd: PlaceOrderCommand): OrderResult {
        val order = OrderBuilder()
            .id(cmd.orderId)
            .customer(cmd.customerId)
            .lineItems {
                // trailing lambda DSL for items
                cmd.items.forEach { add(it) }
            }
            .build()

        // Publish an event instead of calling ProductService
        events.publishEvent(OrderPlaced(orderId = order.id, items = order.items))

        // We are eventually consistent; return current status
        return OrderAccepted(orderId = order.id)
    }
}
```

Demonstrated features here:
- Named parameters (e.g., `orderId = order.id`).
- Trailing lambda DSL (`lineItems { ... }`).
- Builder/DSL pattern (`OrderBuilder`).

#### 2) Define domain events in `order` (or in a tiny `shared-kernel` if you want both modules to see the event types)

```kotlin
// src/main/kotlin/com/example/springmonolith/order/events.kt
package com.example.springmonolith.order

sealed interface DomainEvent

data class OrderPlaced(
    val orderId: OrderId,
    val items: List<OrderItem>
) : DomainEvent
```

Sealed interface `DomainEvent` lets you exhaustively when‑match on event types in listeners.

#### 3) Product module reacts via an application module listener

```kotlin
// src/main/kotlin/com/example/springmonolith/product/InventoryPolicy.kt
package com.example.springmonolith.product

import com.example.springmonolith.order.OrderPlaced
import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

@Component
class InventoryPolicy(
    private val inventory: InventoryService
) {
    @ApplicationModuleListener
    fun on(event: OrderPlaced) {
        val reserved = inventory.reserve(items = event.items)
        if (reserved) {
            // Optionally publish a follow‑up event for order to consume
            inventory.publishInventoryReserved(orderId = event.orderId)
        } else {
            inventory.publishInventoryFailed(orderId = event.orderId)
        }
    }
}
```

`@ApplicationModuleListener` is Modulith’s way to subscribe without creating a dependency from `order` to `product`.

#### 4) Order listens for follow‑up results (still no direct dependency)

```kotlin
// src/main/kotlin/com/example/springmonolith/order/OrderProcessManager.kt
package com.example.springmonolith.order

import org.springframework.modulith.events.ApplicationModuleListener
import org.springframework.stereotype.Component

sealed class OrderProcessEvent {
    data class InventoryReserved(val orderId: OrderId): OrderProcessEvent()
    data class InventoryFailed(val orderId: OrderId, val reason: String): OrderProcessEvent()
}

@Component
class OrderProcessManager(
    private val repo: OrderRepository
) {
    @ApplicationModuleListener
    fun onReserved(event: OrderProcessEvent.InventoryReserved) {
        repo.updateStatus(orderId = event.orderId, status = OrderStatus.ReadyToShip)
    }

    @ApplicationModuleListener
    fun onFailed(event: OrderProcessEvent.InventoryFailed) {
        repo.updateStatus(orderId = event.orderId, status = OrderStatus.Cancelled(reason = event.reason))
    }
}
```

This demonstrates sealed classes for result events and exhaustive handling in `when` blocks if you add them.

---

### Kotlin features showcase inside the domain

#### Value classes, sealed hierarchies, and extension functions

```kotlin
// src/main/kotlin/com/example/springmonolith/order/model.kt
package com.example.springmonolith.order

@JvmInline
value class OrderId(val value: String)

@JvmInline
value class CustomerId(val value: String)

data class OrderItem(val sku: Sku, val quantity: Int)

@JvmInline
value class Sku(val value: String)

sealed class OrderStatus {
    data object New : OrderStatus()
    data object ReadyToShip : OrderStatus()
    data class Cancelled(val reason: String) : OrderStatus()
}

// Extension function example
fun List<OrderItem>.totalQuantity(): Int = sumOf { it.quantity }
```

Use of value classes gives type‑safety without runtime overhead; sealed classes model finite states.

#### Builder/DSL with trailing lambdas and named parameters

```kotlin
// src/main/kotlin/com/example/springmonolith/order/builder.kt
package com.example.springmonolith.order

class OrderBuilder {
    private var id: OrderId = OrderId("UNSET")
    private var customer: CustomerId = CustomerId("UNSET")
    private val items = mutableListOf<OrderItem>()

    fun id(value: OrderId) = apply { id = value }
    fun customer(value: CustomerId) = apply { customer = value }

    fun lineItems(block: LineItemsBuilder.() -> Unit) = apply {
        items += LineItemsBuilder().apply(block).build()
    }

    fun build(): Order = Order(
        id = id,
        customer = customer,
        items = items.toList(),
        status = OrderStatus.New
    )
}

class LineItemsBuilder {
    private val items = mutableListOf<OrderItem>()
    fun add(item: OrderItem) = apply { items += item }
    fun add(sku: Sku, quantity: Int) = apply { items += OrderItem(sku, quantity) }
    fun build(): List<OrderItem> = items
}

data class Order(
    val id: OrderId,
    val customer: CustomerId,
    val items: List<OrderItem>,
    val status: OrderStatus
)
```

This DSL lets you write expressive code:

```kotlin
val result = orderService.placeOrder(
    cmd = PlaceOrderCommand(
        orderId = OrderId("O-123"),
        customerId = CustomerId("C-7"),
        items = listOf(
            OrderItem(Sku("P-1"), quantity = 2),
            OrderItem(Sku("P-2"), quantity = 1)
        )
    )
)
```

#### Context parameters (Kotlin 2 “context receivers”) for cross‑cutting concerns
If you’re on Kotlin 1.9+ with context receivers enabled (and a Gradle `-Xcontext-receivers` flag if needed), create a lightweight context for logging/tracing that threads through your domain without passing parameters everywhere.

```kotlin
// src/main/kotlin/com/example/springmonolith/common/Context.kt
package com.example.springmonolith.common

interface Logger { fun info(msg: String) }

context(Logger)
fun logDomain(message: String) { info("[domain] $message") }
```

Use it in your service:

```kotlin
// src/main/kotlin/com/example/springmonolith/order/OrderService.kt
context(Logger)
fun validate(order: Order) {
    logDomain("Validating order ${order.id}")
}
```

Call site shows context usage clearly:

```kotlin
someLogger.run {
    validate(order)
}
```

This demonstrates context parameters without polluting your function signatures with explicit logger args.

---

### Optional: Synchronous collaboration via shared API module
If you later decide you need a synchronous read (e.g., price lookup), add a tiny API module and allow `order -> catalog-api`, `product -> catalog-api`:

```kotlin
// catalog-api
package com.example.springmonolith.catalog.api

interface ProductCatalog {
    fun currentPrice(sku: String): Money
    fun info(sku: String): ProductInfo
}

data class ProductInfo(val sku: String, val name: String)
@JvmInline value class Money(val cents: Long)
```

- `order` depends only on `catalog-api`.
- `product` implements `ProductCatalog`.
- Keep `order`’s `allowedDependencies = { "catalog-api" }`, still not depending on `product`.

This is classic Ports & Adapters and still complies with Modulith rules.

---

### Update the greeting example without violating boundaries
Instead of calling `ProductService` from `OrderService`, keep greetings local per module for demo endpoints:

```kotlin
// order
fun getGreeting(): String = "Hello from Order Module! 🛒"

// product
fun getGreeting(): String = "Hello from Product Module! 📦"
```

If you want to show a combined message for a UI, do it in a web/controller layer that composes two module endpoints via HTTP calls, or better, render two separate widgets on the page — composition at the UI, not at the domain.

---

### Tests and documentation
- Keep `ModularityTests.verifiesModularStructure()` as is; it should pass once the direct dependency is removed.
- Add module‑level tests per Modulith examples:

```kotlin
// src/test/kotlin/com/example/springmonolith/order/OrderModuleTests.kt
@Test
fun `publishes OrderPlaced when placing an order`() {
    // use ApplicationEvents or Modulith’s EventPublicationRegistry to assert publications
}
```

- Use `Documenter(modules).writeDocumentation()` to generate architecture diagrams. Consider adding `@NamedInterface` on types you deliberately expose and let the `Documenter` include that in the generated docs.

---

### What else to demonstrate (ideas list)
- `@NamedInterface` in Spring Modulith to explicitly expose stable interfaces from a module while keeping everything else internal.
- Spring events vs Modulith events: show how Modulith’s event publication registry enables reliable tests (assert that events were published and handled).
- (Optional) Coroutines in adapters: e.g., a `suspend` `InventoryService.reserve()` using IO, with structured concurrency in use‑cases.

```kotlin
sealed class OrderResult { data class OrderAccepted(val orderId: OrderId) : OrderResult(); data class Rejected(val reason: String) : OrderResult() }
```
- Kotlin time and value classes for domain measurements (e.g., `@JvmInline value class Quantity(val value: Int)`).
- Extension properties for computed fields (e.g., `Order.totalQuantity`).
- Test data builders using DSLs and named args to make tests highly readable.
- Contract tests for the shared API (`catalog-api`) to keep product’s implementation honest.
- Anti‑corruption layer example: if you simulate an external pricing service, show an adapter translating external DTOs into your domain using extension functions.

---

### Summary of concrete changes to make now
1) Remove `ProductService` from `OrderService` and replace the call with event publication.
2) Introduce `OrderPlaced` event (sealed `DomainEvent`).
3) Add a listener in `product` to react to `OrderPlaced`; optionally publish `InventoryReserved`/`InventoryFailed`.
4) Add an `OrderProcessManager` in `order` to handle follow‑up events and update `OrderStatus` (sealed class).
5) Add Kotlin DSL/builder, value classes, extension functions, and context receivers to showcase Kotlin idioms cleanly.
6) Keep greetings per module; avoid cross‑module calls in domain code.

After these changes, `modules.verify()` should pass and you’ll have a richer Kotlin + Modulith showcase without violating modular boundaries.