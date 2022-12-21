package org.clever.app

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("test")

//@SneakyThrows
fun t01() {
    log.info("###-> {}", System.currentTimeMillis())
    log.info("@@@@-> {}", System.currentTimeMillis())

    val a = object : Runnable {
        override fun run() {
        }
    }
    log.info("@@@@-> {}", a)

}
