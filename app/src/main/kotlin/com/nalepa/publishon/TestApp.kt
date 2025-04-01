package com.nalepa.publishon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.http.ResponseEntity
import org.springframework.http.client.ReactorResourceFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.netty.resources.ConnectionProvider
import reactor.netty.resources.LoopResources
import java.math.BigInteger

@SpringBootApplication
class TestApp

fun main(args: Array<String>) {
    runApplication<TestApp>(*args)
}


data class MockServerResponse(
    val data: String,
)

data class AppResponse(
    val data: String,
)

object TestAppLogger {
    fun log(caller: Any, text: String) {
//        println(Thread.currentThread().name + " ### ${caller.javaClass.name} ### $text")
    }
}

