package com.nalepa.publishon


import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import io.micrometer.core.instrument.Meter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.cumulative.CumulativeTimer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers
import java.time.Duration

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class WaitTimeInQueueEndpointTest(
    @LocalServerPort
    private val port: Int,
    @Autowired
    private val webClientBuilder: WebClient.Builder,
    @Autowired
    private val meterRegistry: MeterRegistry,
) {

    val wiremockServer =
        WireMockServer(
            options()
                .port(8085)
        )

    @BeforeEach
    fun before() {
        setupWireMock()
    }

    @AfterEach
    fun after() {
        tearDownWireMock()
    }

    @Test
    // sometimes flaky, I don't know why :D
    fun requestsArePendingCauseThereAreNoHeavyCpuOperationAfterGettingResponseFromWebClient() {
        // given
        val webClient = webClientBuilder.build()
        val somePool = Schedulers.boundedElastic()
        val startTime = System.currentTimeMillis()
        // when
        Flux.range(0, WaitTimeConfig.numberOfRequestsToBeExecutedWhenThereIsNoCPUOperation)
            .flatMap { index ->
                webClient
                    .get()
                    .uri("http://localhost:$port/wait-time-endpoint/$index?executeCpuOperation=false")
                    .retrieve()
                    .bodyToMono(AppResponse::class.java)
            }
            .collectList()
            .publishOn(somePool)
            .block()

        // then
        val duration = Duration.ofMillis(System.currentTimeMillis() - startTime)
        println("Duration of all requests: $duration")

        val meters: Collection<Meter> =
            meterRegistry.find("reactor.netty.connection.provider.pending.connections.time").meters()

        val timer = meters.first() as CumulativeTimer
        val numberOfRequestsThatWerePending = timer.count().toInt()
        val expectedNumberOfPendingConnectionsTotal = WaitTimeConfig.numberOfRequestsToBeExecutedWhenThereIsNoCPUOperation - WaitTimeConfig.maxConnections
        println("Number of requests that were pending: $numberOfRequestsThatWerePending")
        assert(numberOfRequestsThatWerePending == expectedNumberOfPendingConnectionsTotal) {
            "Number of pending connections was: $numberOfRequestsThatWerePending but should be $expectedNumberOfPendingConnectionsTotal"
        }
    }

    @Test
    fun noRequestWasPendingCauseHttpThreadsWereBusy() {
        // given
        val webClient = webClientBuilder.build()
        val somePool = Schedulers.boundedElastic()
        val startTime = System.currentTimeMillis()
        // when
        Flux.range(0, WaitTimeConfig.numberOfRequestsToBeExecutedWhenThereAreCpuOperations)
            .flatMap { index ->
                webClient
                    .get()
                    .uri("http://localhost:$port/wait-time-endpoint/$index?executeCpuOperation=true")
                    .retrieve()
                    .bodyToMono(AppResponse::class.java)
            }
            .collectList()
            .publishOn(somePool)
            .block()

        // then
        val duration = Duration.ofMillis(System.currentTimeMillis() - startTime)
        println("Duration of all requests: $duration")

        val meters: Collection<Meter> =
            meterRegistry.find("reactor.netty.connection.provider.pending.connections.time").meters()

        val timer = meters.first() as CumulativeTimer
        val numberOfRequestsThatWerePending = timer.count().toInt()
        val expectedNumberOfPendingConnectionsTotal = 0
        println("Number of requests that were pending: $numberOfRequestsThatWerePending")
        assert(numberOfRequestsThatWerePending == expectedNumberOfPendingConnectionsTotal) {
            "Number of pending connections was: $numberOfRequestsThatWerePending but should be $expectedNumberOfPendingConnectionsTotal"
        }
    }

    private fun setupWireMock() {
        wiremockServer.start()
        wiremockServer.stubFor(
            get("/some-mock-client")
                .willReturn(
                    aResponse()
                        .withFixedDelay(100)
                        .withBody(
                            """
                            {
                                "data": "Hello from wiremock!"
                            }
                        """.trimIndent()
                        )
                        .withHeader("Content-Type", "application/json")
                )
        )
    }

    // don't do invocations like this in some commercial repo, use some more generic approach to invoke this method :D
    private fun tearDownWireMock() {
        wiremockServer.stop()
    }
}
