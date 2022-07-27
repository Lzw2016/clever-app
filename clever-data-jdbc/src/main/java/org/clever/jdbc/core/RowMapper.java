package org.clever.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}用于按行映射结果集的行的接口。
 * 该接口的实现执行将每一行映射到{@link ResultSet}对象的实际工作，但不需要担心异常处理。
 * {@link SQLException SQLExceptions}将被捕获并由调用JdbcTemplate处理。
 *
 * <p>通常用于{@link JdbcTemplate}的查询方法或存储过程的out参数。
 * 行映射器对象通常是无状态的，因此可以重用；它们是在单个位置实现行映射逻辑的理想选择。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:30 <br/>
 *
 * @param <T> the result type
 * @see JdbcTemplate
 * @see RowCallbackHandler
 * @see ResultSetExtractor
 */
@FunctionalInterface
public interface RowMapper<T> {
    /**
     * 实现必须实现此方法来映射结果集中的每一行数据。
     * 此方法不应在结果集上调用{@code next()}；它只应该映射当前行的值。
     *
     * @param rs     要映射的结果集（针对当前行预初始化）
     * @param rowNum 当前行的编号
     * @return 当前行的结果对象（可能为空）
     * @throws SQLException 如果在获取列值时遇到SQLException（也就是说，不需要捕获SQLException）
     */
    T mapRow(ResultSet rs, int rowNum) throws SQLException;
}
