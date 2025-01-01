package com.nalepa.publishon

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PublishonApplication

fun main(args: Array<String>) {
	runApplication<PublishonApplication>(*args)
}
