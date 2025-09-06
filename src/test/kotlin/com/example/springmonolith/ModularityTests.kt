package com.example.springmonolith

import org.junit.jupiter.api.Test
import org.springframework.modulith.core.ApplicationModules
import org.springframework.modulith.docs.Documenter

/**
 * Tests to verify the modular structure and generate documentation for the modules.
 * This follows the Spring Modulith example pattern for verifying module boundaries
 * and creating documentation.
 */
class ModularityTests {

    private val modules = ApplicationModules.of(SpringmonolithApplication::class.java)

    @Test
    fun verifiesModularStructure() {
        // This test verifies that the application follows Spring Modulith conventions
        // and that there are no violations of module boundaries
        modules.verify()
    }

    @Test
    fun createModuleDocumentation() {
        // This test generates documentation for the application modules
        // The documentation will be created in the target/spring-modulith-docs directory
        Documenter(modules).writeDocumentation()
    }

    @Test
    fun printModuleStructure() {
        // This test prints the module structure to help understand the application layout
        modules.forEach { module ->
            println("Module: ${module.displayName}")
            println("Base package: ${module.basePackage.name}")
            println("Spring beans: ${module.springBeans.size}")
            println("---")
        }
    }
}
