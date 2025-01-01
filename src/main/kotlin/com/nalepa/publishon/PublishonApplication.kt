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
class PublishonApplication

fun main(args: Array<String>) {
    runApplication<PublishonApplication>(*args)
}

@RestController
class AppEndpoint(
    private val webClientBuilder: WebClient.Builder,
) {


    private val webClient: WebClient = createWebClient()

    private fun createWebClient(): WebClient {
        val reactorResourceFactory = ReactorResourceFactory()
        reactorResourceFactory.loopResources = LoopResources.create("http-client")
        reactorResourceFactory.connectionProvider = ConnectionProvider.create("connProvider")

        val connector = ReactorClientHttpConnector(reactorResourceFactory) { it }

        return webClientBuilder
            .clientConnector(connector)
            .build()
    }

    @GetMapping("/data")
    fun data(): Mono<ResponseEntity<AppResponse>> {
        AppLogger.log("ENDPOINT: Start processing request")
        return getData()
            .map { AppResponse(it.content) }
            .map { ResponseEntity.ok(it) }
            .doFinally {
                AppLogger.log("ENDPOINT: Ended processing request")
            }

    }

    private fun getData(): Mono<SomeClientResponse> =
        webClient
            .get()
            .uri("http://localhost:8081/some-client")
            .retrieve()
            .bodyToMono(SomeClientResponse::class.java)
            .publishOn(Schedulers.parallel())
            .doOnNext {
                AppLogger.log("WEBCLIENT: I hava response from external service")
            }
            .map {
                heavyCpuOperation()
                it
            }
            .publishOn(Schedulers.parallel())

    private fun heavyCpuOperation() {
        AppLogger.log("CPU OPERATION: Started heavy operation")
        var bigInteger = BigInteger.ZERO
        for (i in 0..500_000) {
            bigInteger = bigInteger.add(BigInteger.valueOf(i.toLong()))
        }
        AppLogger.log("CPU OPERATION: Ended heavy operation")
    }

}

data class SomeClientResponse(
    val content: String,
)


data class AppResponse(
    val text: String,
)

object AppLogger {
    fun log(text: String) {
        println(Thread.currentThread().name + " ### $text")
    }
}