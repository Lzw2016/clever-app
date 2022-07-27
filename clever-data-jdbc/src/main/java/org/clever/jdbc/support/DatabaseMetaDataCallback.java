package org.clever.jdbc.support;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;

/**
 * JdbcUtils类使用的回调接口。
 * 此接口的实现执行提取数据库元数据的实际工作，但不需要担心异常处理。
 * JdbcUtils类将捕获并正确处理SQLExceptions。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:20 <br/>
 *
 * @param <T> the result type
 * @see JdbcUtils#extractDatabaseMetaData(javax.sql.DataSource, DatabaseMetaDataCallback)
 */
@FunctionalInterface
public interface DatabaseMetaDataCallback<T> {
    /**
     * 实现必须实现此方法来处理传入的元数据。具体实现选择做什么取决于它。
     *
     * @param dbmd 要处理的数据库元数据
     * @return 从元数据中提取的结果对象（可以是实现所需的任意对象）
     * @throws SQLException            如果在获取列值时遇到SQLException（也就是说，不需要捕获SQLException）
     * @throws MetaDataAccessException 如果提取元数据时出现其他故障（例如，反射故障）
     */
    T processMetaData(DatabaseMetaData dbmd) throws SQLException, MetaDataAccessException;
}
