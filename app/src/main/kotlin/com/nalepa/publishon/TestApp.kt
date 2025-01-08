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

@RestController
class AppEndpoint(
    private val webClientBuilder: WebClient.Builder,
) {

    private val webClient: WebClient = createWebClient()

    /**
     * By default, there's a one pool for server & client
     * I've created pool for the client in order to have custom thread names
     */
    private fun createWebClient(): WebClient {
        val reactorResourceFactory =
            ReactorResourceFactory().apply {
                loopResources = LoopResources.create("http-client")
                connectionProvider = ConnectionProvider.create("connProvider")
            }
        val connector = ReactorClientHttpConnector(reactorResourceFactory) { it }

        return webClientBuilder
            .clientConnector(connector)
            .build()
    }

    @GetMapping("/data-without-publishon")
    fun withoutPublishOn(): Mono<ResponseEntity<AppResponse>> =
        process(false)

    @GetMapping("/data-with-publishon")
    fun withPublishOn(): Mono<ResponseEntity<AppResponse>> =
        process(true)

    private fun process(usePublishOn: Boolean): Mono<ResponseEntity<AppResponse>> {
        TestAppLogger.log(this, "")
        TestAppLogger.log(this, "ENDPOINT: Start processing request")
        return getData()
            .usePublishOn(usePublishOn)
            .map {
                heavyCpuOperation()
                it
            }
            .map { AppResponse(it.data) }
            .map { ResponseEntity.ok(it) }
            .doFinally {
                TestAppLogger.log(this, "ENDPOINT: Ended processing request")
            }
    }

    private fun <T> Mono<T>.usePublishOn(usePublishOn: Boolean): Mono<T> =
        if (usePublishOn) {
            this.publishOn(Schedulers.parallel())
        } else {
            this
        }

    private fun getData(): Mono<MockServerResponse> =
        webClient
            .get()
            .uri("http://localhost:8081/mock-data")
            .retrieve()
            .bodyToMono(MockServerResponse::class.java)
            .doOnNext {
                TestAppLogger.log(this, "WEBCLIENT: I hava response from external service")
            }

    private fun heavyCpuOperation() {
        TestAppLogger.log(this, "CPU OPERATION: Started heavy operation")
        var bigInteger = BigInteger.ZERO
        for (i in 0..500_000) {
            bigInteger = bigInteger.add(BigInteger.valueOf(i.toLong()))
        }
        TestAppLogger.log(this, "CPU OPERATION: Ended heavy operation")
    }

}

data class MockServerResponse(
    val data: String,
)

data class AppResponse(
    val data: String,
)

object TestAppLogger {
    fun log(caller: Any, text: String) {
        println(Thread.currentThread().name + " ### ${caller.javaClass.name} ### $text")
    }
}