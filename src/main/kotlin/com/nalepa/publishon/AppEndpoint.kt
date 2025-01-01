package com.nalepa.publishon

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@RestController
class AppEndpoint(
    private val webClientBuilder: WebClient.Builder,
) {


    @GetMapping("/data")
    fun data(): Mono<ResponseEntity<AppResponse>> {
        return Mono.just(ResponseEntity.ok(AppResponse("String")))
    }

}

data class AppResponse(
    val text: String,
)