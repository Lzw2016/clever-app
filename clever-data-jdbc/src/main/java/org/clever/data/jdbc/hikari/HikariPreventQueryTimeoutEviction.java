package org.clever.data.jdbc.hikari;

import com.zaxxer.hikari.SQLExceptionOverride;

import java.sql.SQLException;
import java.sql.SQLTimeoutException;

/**
 * HikariPC遇到{@link SQLTimeoutException}时会自动关闭数据库连接，
 * 而Oracle关闭数据库连接时会做一个隐式提交，这就会导致事务部分提交(数据不一致)。
 * 实现{@link SQLExceptionOverride}接口自定义在出现{@link SQLTimeoutException}异常时不要关闭数据库连接
 * <pre>{@code
 * config.setExceptionOverrideClassName(HikariPreventQueryTimeoutEviction.class.getName());
 * HikariDataSource dataSource = new HikariDataSource(config);
 * }</pre>
 * 或者使用配置文件
 * <pre>{@code
 * datasource:
 *   exception-override-class-name: 'org.clever.data.jdbc.hikari.HikariPreventQueryTimeoutEviction'
 * }</pre>
 * 作者：lizw <br/>
 * 创建时间：2022/05/18 13:26 <br/>
 *
 * @see "https://github.com/brettwooldridge/HikariCP/issues/1308"
 * @see "https://github.com/brettwooldridge/HikariCP/issues/1388"
 * @see "https://github.com/brettwooldridge/HikariCP/issues/1489#issuecomment-581437268"
 * @see "https://github.com/brettwooldridge/HikariCP/blob/dev/src/test/java/com/zaxxer/hikari/pool/TestConnections.java"
 */
public class HikariPreventQueryTimeoutEviction implements SQLExceptionOverride {
    @java.lang.Override
    public Override adjudicate(SQLException ex) {
        if (ex instanceof SQLTimeoutException) {
            return Override.DO_NOT_EVICT;
        }
        return Override.CONTINUE_EVICT;
    }
}
