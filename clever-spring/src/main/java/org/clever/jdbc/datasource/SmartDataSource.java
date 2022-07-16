package org.clever.jdbc.datasource;

import javax.sql.DataSource;
import java.sql.Connection;

/**
 * 扩展{@code javax.sql.DataSource}接口，由以未包装方式返回JDBC连接的特殊数据源实现。
 * <p>使用此接口的类可以查询操作后是否应关闭连接。DataSourceUtils和JdbcTemplate类自动执行这样的检查。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/21 22:58 <br/>
 *
 * @see DataSourceUtils#releaseConnection
 */
public interface SmartDataSource extends DataSource {
    /**
     * 我们是否应该关闭从该数据源获取的此连接？
     * <p>使用SmartDataSource连接的代码应该在调用{@code close()}之前始终通过此方法执行检查。
     *
     * @param con 要检查的连接
     * @return 是否应关闭给定连接
     * @see java.sql.Connection#close()
     */
    boolean shouldClose(Connection con);
}
