package org.clever.jdbc.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * 用于将复杂类型设置为语句参数的简单接口。
 *
 * <p>实现执行设置实际值的实际工作。
 * 他们必须实现回调方法{@code setValue}，该方法可以抛出SQLExceptions，调用代码将捕获并转换这些异常。
 * 如果需要通过给定的PreparedStatement对象来创建任何特定于数据库的对象，则此回调方法可以访问基础连接。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/28 13:21 <br/>
 *
 * @see org.clever.jdbc.core.SqlTypeValue
 * @see org.clever.jdbc.core.DisposableSqlTypeValue
 */
public interface SqlValue {
    /**
     * 在给定的PreparedStatement上设置值
     *
     * @param ps         准备好的工作报表
     * @param paramIndex 需要为其设置值的参数的索引
     * @throws SQLException 如果在设置参数值时遇到SQLException
     */
    void setValue(PreparedStatement ps, int paramIndex) throws SQLException;

    /**
     * 清理此值对象持有的资源
     */
    void cleanup();
}
