package com.nalepa.publishon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@SpringBootApplication
class MockServerApp

fun main(args: Array<String>) {
    runApplication<MockServerApp>(*args)
}


@RestController
class MockEndpoint {

    @GetMapping("/mock-data")
    fun mockData(): MockData =
        MockData("mock-data")
}

data class MockData(
    val data: String,
)