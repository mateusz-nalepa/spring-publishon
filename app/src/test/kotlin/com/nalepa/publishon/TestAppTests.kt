package com.nalepa.publishon

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class TestAppTests(
    @LocalServerPort
    private val port: Int,
    @Autowired
    private val webClientBuilder: WebClient.Builder,
) {

    val wiremockServer =
        WireMockServer(
            options()
                .port(8081)
        )


    @Test
    fun contextLoads() {
        wiremockServer.start()
        wiremockServer.stubFor(
            get("/some-client")
                .willReturn(
                    aResponse()
                        .withBody(
                            """
                            {
                                "content": "Hello from wiremock!"
                            }
                        """.trimIndent()
                        )
                        .withHeader("Content-Type", "application/json")
                )
        )

        val webClient = webClientBuilder.build()

        // when
        val response =
            webClient
                .get()
                .uri("http://localhost:$port/data")
                .retrieve()
                .bodyToMono(AppResponse::class.java)
                .doOnError {
                    it.printStackTrace()
                }
                .block()

        // then
        requireNotNull(response)
        assert(response.text == "Hello from wiremock!")
        wiremockServer.stop()
    }

}
