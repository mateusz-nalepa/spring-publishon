package com.nalepa.publishon

import org.springframework.http.ResponseEntity
import org.springframework.http.client.ReactorResourceFactory
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.resources.ConnectionProvider
import reactor.netty.resources.LoopResources
import java.time.Duration


object WaitTimeLogger {
    fun log(caller: Any, text: String) {
        println(Thread.currentThread().name + " ### ${caller.javaClass.name} ### $text")
    }
}

//10 watkow
//10 connection
//20 pending
//17 requestow

object WaitTimeConfig {
    val availableThreads =
        Runtime.getRuntime().availableProcessors()
            .also {
                if (it < 2) {
                    throw RuntimeException("number fo threads should be at least 2")
                }
            }

    val maxConnections = availableThreads * 4
    val pendingAcquireMaxCount = maxConnections * 2

    val numberOfRequestsToBeExecutedWhenThereIsNoCPUOperation = pendingAcquireMaxCount - 1
    val numberOfRequestsToBeExecutedWhenThereAreCpuOperations = maxConnections - 1
}

@RestController
class WaitTimeInQueueEndpoint(
    private val webClientBuilder: WebClient.Builder,
) {

    private val webClient: WebClient = createWebClient()

    private fun createWebClient(): WebClient {
        val reactorResourceFactory =
            ReactorResourceFactory().apply {
                loopResources = LoopResources.create("http-client")
                connectionProvider = ConnectionProvider
                    .builder("SomeConnectionPool")
                    .maxConnections(WaitTimeConfig.maxConnections)
                    .pendingAcquireTimeout(Duration.ofHours(1))
                    .pendingAcquireMaxCount(WaitTimeConfig.pendingAcquireMaxCount)
                    .metrics(true)
                    .build()
            }
        val connector = ReactorClientHttpConnector(reactorResourceFactory) { it }

        return webClientBuilder
            .clientConnector(connector)
            .build()
    }

    @GetMapping("/wait-time-endpoint/{index}")
    fun endpoint(
        @PathVariable index: Int,
        @RequestParam executeCpuOperation: Boolean,
    ): Mono<ResponseEntity<AppResponse>> =
        getData(index)
            .map {
                tryToExecuteHeavyCpuOperation(executeCpuOperation)
                it
            }
            .map { AppResponse(it.data) }
            .map { ResponseEntity.ok(it) }

    private fun getData(index: Int): Mono<MockServerResponse> {
        val startTime = System.currentTimeMillis()

        return webClient
            .get()
            .uri("http://localhost:8085/some-mock-client")
            .retrieve()
            .bodyToMono(MockServerResponse::class.java)
            .doOnNext {
                val duration = Duration.ofMillis(System.currentTimeMillis() - startTime)
                WaitTimeLogger.log(this, "Processing request number: $index took: $duration")
            }
    }

    private fun tryToExecuteHeavyCpuOperation(executeCpuOperation: Boolean) {
        if (executeCpuOperation) {
            WaitTimeLogger.log(this, "CPU OPERATION: Start heavy operation")
            Thread.sleep(5000)
        } else {
            WaitTimeLogger.log(this, "CPU OPERATION: Skipping")
        }
    }

}
