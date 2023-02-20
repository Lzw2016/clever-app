package org.clever.app.mvc

import org.clever.data.jdbc.DaoFactory
import org.clever.data.jdbc.Jdbc
import org.clever.web.support.mvc.annotation.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("test")
val jdbc: Jdbc = DaoFactory.getJdbc()

@Transactional(disabled = true)
fun t01() {
    log.info("###-> {}", System.currentTimeMillis())
    log.info("@@@@-> {}", System.currentTimeMillis())

}

@Transactional(disabled = true)
fun t02(a: String?): Any {
    log.info("### -> {}", a)
    return jdbc.queryFirst("select * from auto_increment_id")
}
