package org.clever.app.mvc

import org.clever.data.jdbc.DaoFactory
import org.clever.data.jdbc.Jdbc
import org.clever.data.jdbc.QueryDSL
import org.clever.security.impl.model.EnumConstant
import org.clever.security.impl.model.entity.SysUser
import org.clever.security.impl.model.query.QSysUser.sysUser
import org.clever.web.mvc.annotation.Transactional
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

val log: Logger = LoggerFactory.getLogger("test")

@Transactional(disabled = true)
fun t01() {
    log.info("###-> {}", System.currentTimeMillis())
    log.info("@@@@-> {}", System.currentTimeMillis())
}

@Transactional(disabled = true)
fun t02(a: String?): Any {
    val jdbc: Jdbc = DaoFactory.getJdbc()
    log.info("### -> {}", a)
    return jdbc.queryFirst("select * from auto_increment_id")
}

fun t03(): Any {
    val queryDSL: QueryDSL = DaoFactory.getQueryDSL()
    val user = SysUser()
    user.id = queryDSL.nextId(sysUser.tableName)
    user.loginName = "admin"
    user.password = "123456"
    user.userName = "管理员"
    user.isEnable = EnumConstant.ENABLED_1
    user.createBy = -1
    user.createAt = Date()
    queryDSL.insert(sysUser).populate(user).execute()
    return queryDSL.selectFrom(sysUser).where(sysUser.id.eq(user.id)).fetch()
}
