package org.clever.app

import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("test")

fun t01() {
    log.info("###-> {}", System.currentTimeMillis())
    log.info("@@@@-> {}", System.currentTimeMillis())
}
