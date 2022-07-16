package org.clever.jdbc.core;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link JdbcTemplate}用于按行处理{@link java.sql.ResultSet}的行的接口。
 * 该接口的实现执行处理每行的实际工作，但不需要担心异常处理。
 * {@link java.sql.SQLException SQLExceptions}将被捕获并由调用JdbcTemplate处理。
 *
 * <p>与{@link ResultSetExtractor}不同，{@code RowCallbackHandler}对象通常是有状态的：
 * 它将结果状态保留在对象内，以供以后检查。
 * 有关用法示例，请参阅{@code RowCountCallbackHandler}。.
 *
 * <p>如果需要每行只映射一个结果对象，请考虑使用{@link RowMapper}，将它们组合到一个列表中。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:30 <br/>
 *
 * @see JdbcTemplate
 * @see RowMapper
 * @see ResultSetExtractor
 */
@FunctionalInterface
public interface RowCallbackHandler {
    /**
     * 实现必须实现此方法来处理结果集中的每一行数据。
     * 此方法不应在结果集上调用{@code next()}；它只应该提取当前行的值。
     * <p>具体实现选择做什么取决于它：一个简单的实现可能只计算行，而另一个实现可能构建XML文档。
     *
     * @param rs 要处理的结果集（针对当前行预初始化）
     * @throws SQLException 如果在获取列值时遇到SQLException（也就是说，不需要捕获SQLException）
     */
    void processRow(ResultSet rs) throws SQLException;
}
