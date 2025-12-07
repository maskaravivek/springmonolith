# Spring Modulith Demo

This project demonstrates how to build a modular monolith using Spring Boot 3.5.5 and Spring Modulith. It features a clean separation of concerns with distinct modules (Product and Order) while maintaining a single deployable unit.

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

## API Endpoints

- `GET /api/products/hello` - Product module greeting
- `GET /api/orders/hello` - Order module greeting

## Project Structure

```
src/main/kotlin/com/example/springmonolith/
├── SpringmonolithApplication.kt    # Main application entry point
├── product/                         # Product module
│   ├── ProductController.kt
│   └── ProductService.kt
└── order/                           # Order module
    ├── OrderController.kt
    └── OrderService.kt
```

## Modules

The application is organized into logical modules:

- **Product Module**: Handles product-related functionality
- **Order Module**: Handles order-related functionality

Each module is self-contained with its own controllers and services, demonstrating modulith boundaries.

