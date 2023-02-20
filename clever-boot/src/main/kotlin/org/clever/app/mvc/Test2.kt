package org.clever.app.mvc

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class Test2 {
    companion object {
        private val log: Logger = LoggerFactory.getLogger("test")

        fun t01() {
            log.info("###-> {}", System.currentTimeMillis())
        }
    }
}
