package com.nalepa.publishon

import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

class MonoCode {

    @Test
    fun consecutiveSubscribeOn() {
        Mono
            .just("value")
            .subscribeOn(Schedulers.parallel())
            .doOnNext { println(Thread.currentThread().name + " ### FIRST") }
            .subscribeOn(Schedulers.boundedElastic())
            .doOnNext { println(Thread.currentThread().name + " ### SECOND") }
            .block()
    }

    @Test
    fun subscribeOnAndPublishOn() {
        Mono
            .just("value")
            .subscribeOn(Schedulers.parallel())
            .doOnNext { println(Thread.currentThread().name + " ### FIRST") }
            .publishOn(Schedulers.boundedElastic())
            .doOnNext { println(Thread.currentThread().name + " ### SECOND") }
            .block()
    }
}
