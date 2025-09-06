package com.example.springmonolith

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.modulith.Modulithic

@Modulithic
@SpringBootApplication
class SpringmonolithApplication

fun main(args: Array<String>) {
	runApplication<SpringmonolithApplication>(*args)
}
