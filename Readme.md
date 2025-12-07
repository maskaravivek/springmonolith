# Spring Modulith Demo

A demo Spring Boot application showcasing Spring Modulith architecture with event-driven modular monolith design patterns using Kotlin.

## Description

This project demonstrates how to build a modular monolith using Spring Boot 3.5.5 and Spring Modulith. It features strict module boundaries with **zero compile-time dependencies** between modules, using domain events for inter-module communication. The codebase also showcases modern Kotlin features including data classes, sealed classes, value classes, extension functions, and named arguments.

## Tech Stack

- **Java**: 17
- **Kotlin**: 1.9.25
- **Spring Boot**: 3.5.5
- **Spring Modulith**: 1.3.1 with Events API
- **Build Tool**: Gradle

## Architecture Highlights

### Event-Driven Modularity
- **Order** and **Product** modules operate with `allowedDependencies = {}` (no cross-module dependencies)
- Communication happens through domain events published to Spring's event system
- Event flow: `OrderPlaced` → `InventoryReserved/InventoryFailed` → Order status updates

### Kotlin Features Demonstrated
- **Data classes**: Event DTOs (`OrderPlaced`, `OrderItemDTO`)
- **Sealed classes**: Type-safe event hierarchies (`InventoryEvent`, `OrderResult`)
- **Value classes** (`@JvmInline`): Strong typing without runtime overhead (`OrderId`)
- **Extension functions**: Adding behavior to DTOs (`List<OrderItemDTO>.totalQuantity()`)
- **Named arguments**: Improved readability in event construction

## Prerequisites

- Java 17 or higher
- Gradle (included via wrapper)

## Getting Started

### Build the project

```bash
./gradlew build
```

### Run the application

```bash
./gradlew bootRun
```

The application will start on `http://localhost:8080`

### Run tests

```bash
./gradlew test
```

The test suite includes modularity verification that ensures no forbidden cross-module dependencies exist.

## API Endpoints

- `GET /api/products/hello` - Product module greeting
- `GET /api/orders/hello` - Order module greeting
- `POST /api/orders` - Place an order (triggers event-driven flow)

## Project Structure

```
src/main/kotlin/com/example/springmonolith/
├── SpringmonolithApplication.kt    # Main application (@Modulithic)
├── Events.kt                        # Shared domain events (neutral package)
├── Extensions.kt                    # Kotlin extension functions
├── order/                           # Order module
│   ├── OrderController.kt          # REST endpoints
│   ├── OrderService.kt             # Publishes OrderPlaced events
│   ├── OrderProcessManager.kt      # Listens to inventory events
│   └── Ids.kt                      # Value classes & adapters
└── product/                         # Product module
    ├── ProductController.kt        # REST endpoints
    ├── ProductService.kt
    └── InventoryPolicy.kt          # Listens to OrderPlaced events
```

## Module Boundaries

### Order Module
- Publishes `OrderPlaced` events when orders are created
- Listens to `InventoryReserved` and `InventoryFailed` events from Product
- Uses value class `OrderId` for type safety
- No direct dependency on Product module

### Product Module
- Listens to `OrderPlaced` events via `@ApplicationModuleListener`
- Publishes `InventoryReserved` or `InventoryFailed` based on inventory check
- Independent inventory management logic
- No dependency on Order module

### Shared Events
- Events live in the base package (not an application module)
- Simple DTOs ensure no coupling between modules
- Extension functions add behavior without breaking boundaries

## Documentation

For detailed refactoring steps and architectural decisions, see:
- [docs/summary.md](docs/summary.md) - PR summary and Kotlin features overview
- [docs/step-by-step-refactor-with-events.md](docs/step-by-step-refactor-with-events.md) - Detailed refactoring guide
- [docs/why-domain-events.md](docs/why-domain-events.md) - Rationale for event-driven approach
