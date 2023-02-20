package org.clever.app.mvc

import org.clever.data.jdbc.DaoFactory
import org.clever.data.jdbc.Jdbc
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.servlet.http.HttpServletRequest

val log: Logger = LoggerFactory.getLogger("test")
val jdbc: Jdbc = DaoFactory.getJdbc()

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

fun t02(request: HttpServletRequest): Any {
    return jdbc.queryFirst("select * from auto_increment_id")
}
