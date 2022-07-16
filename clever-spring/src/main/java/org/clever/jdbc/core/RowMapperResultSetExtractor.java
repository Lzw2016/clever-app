package org.clever.jdbc.core;

import org.clever.util.Assert;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * ResultSetExtractor接口的适配器实现，
 * 该接口委托给一个行映射器，该行映射器应该为每一行创建一个对象。
 * 每个对象都会添加到此ResultSetExtractor的结果列表中。
 *
 * <p>对于数据库表中每行一个对象的典型情况非常有用。结果列表中的条目数将与行数匹配。
 * <p>注意，行映射器对象通常是无状态的，因此可以重用；只有RowMapperResultSetExtractor适配器是有状态的。
 *
 * <p>JdbcTemplate的使用示例：
 * <pre>{@code
 * JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * RowMapper rowMapper = new UserRowMapper();  // reusable object
 *
 * List allUsers = (List) jdbcTemplate.query(
 *     "select * from user",
 *     new RowMapperResultSetExtractor(rowMapper, 10));
 *
 * User user = (User) jdbcTemplate.queryForObject(
 *     "select * from user where id=?", new Object[] {id},
 *     new RowMapperResultSetExtractor(rowMapper, 1));
 * }</pre>
 *
 * <p>或者，考虑从{@code jdbc.object}包中将MappingSqlQuery子类化：
 * 您可以在那里拥有可执行的查询对象（包含行映射逻辑），而不是使用单独的JdbcTemplate和RowMapper对象。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:40 <br/>
 *
 * @param <T> the result element type
 * @see RowMapper
 * @see JdbcTemplate
 */
public class RowMapperResultSetExtractor<T> implements ResultSetExtractor<List<T>> {
    private final RowMapper<T> rowMapper;
    private final int rowsExpected;

    /**
     * 创建新的RowMapperResultSetExtractor。
     *
     * @param rowMapper 为每行创建对象的行映射器
     */
    public RowMapperResultSetExtractor(RowMapper<T> rowMapper) {
        this(rowMapper, 0);
    }

    /**
     * 创建新的RowMapperResultSetExtractor。
     *
     * @param rowMapper    为每行创建对象的行映射器
     * @param rowsExpected 预期行数（仅用于优化集合处理）
     */
    public RowMapperResultSetExtractor(RowMapper<T> rowMapper, int rowsExpected) {
        Assert.notNull(rowMapper, "RowMapper is required");
        this.rowMapper = rowMapper;
        this.rowsExpected = rowsExpected;
    }

    @Override
    public List<T> extractData(ResultSet rs) throws SQLException {
        List<T> results = (this.rowsExpected > 0 ? new ArrayList<>(this.rowsExpected) : new ArrayList<>());
        int rowNum = 0;
        while (rs.next()) {
            results.add(this.rowMapper.mapRow(rs, rowNum++));
        }
        return results;
    }
}
