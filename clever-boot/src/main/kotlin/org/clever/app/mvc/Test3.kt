package org.clever.app.mvc

import org.slf4j.Logger
import org.slf4j.LoggerFactory

object Test3 {
    private val log: Logger = LoggerFactory.getLogger("test")

    fun t01() {
        log.info("###-> {}", System.currentTimeMillis())
    }
}
