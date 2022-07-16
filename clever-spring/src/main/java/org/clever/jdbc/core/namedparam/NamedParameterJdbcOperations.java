package org.clever.jdbc.core.namedparam;

import org.clever.dao.DataAccessException;
import org.clever.jdbc.core.*;
import org.clever.jdbc.support.KeyHolder;
import org.clever.jdbc.support.rowset.SqlRowSet;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 接口，指定一组基本的JDBC操作，允许使用命名参数而不是传统的“？”占位符。
 *
 * <p>这是由{@link NamedParameterJdbcTemplate}实现的经典{@link org.clever.jdbc.core.JdbcOperations}接口的替代方案。
 * 该接口通常不直接使用，但提供了一个有用的选项来增强可测试性，因为它很容易被模拟或存根。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:47 <br/>
 *
 * @see NamedParameterJdbcTemplate
 * @see org.clever.jdbc.core.JdbcOperations
 */
public interface NamedParameterJdbcOperations {
    /**
     * 公开经典的JDBC模板，以允许调用经典的JDBC操作。
     */
    JdbcOperations getJdbcOperations();

    /**
     * 执行JDBC数据访问操作，作为处理JDBC PreparedStatement的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param sql         要执行的SQL
     * @param paramSource 要绑定到查询的参数容器
     * @param action      指定操作的回调对象
     * @return 操作返回的结果对象，或null
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(String sql, SqlParameterSource paramSource, PreparedStatementCallback<T> action) throws DataAccessException;

    /**
     * 执行JDBC数据访问操作，作为处理JDBC PreparedStatement的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param sql      要执行的SQL
     * @param paramMap 要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param action   指定操作的回调对象
     * @return 操作返回的结果对象，或 {@code null}
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(String sql, Map<String, ?> paramMap, PreparedStatementCallback<T> action) throws DataAccessException;

    /**
     * 执行JDBC数据访问操作，作为处理JDBC PreparedStatement的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param sql    要执行的SQL
     * @param action 指定操作的回调对象
     * @return 操作返回的结果对象，或 {@code null}
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，使用ResultSetExtractor读取结果集。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @param rse         将提取结果的对象
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果查询失败
     */
    <T> T query(String sql, SqlParameterSource paramSource, ResultSetExtractor<T> rse) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，使用ResultSetExtractor读取结果集。
     *
     * @param sql      要执行的SQL查询
     * @param paramMap 要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param rse      将提取结果的对象
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果查询失败
     */
    <T> T query(String sql, Map<String, ?> paramMap, ResultSetExtractor<T> rse) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建准备好的语句，使用ResultSetExtractor读取结果集。
     * <p>注意：与具有相同签名的JdbcOperations方法不同，此查询变体始终使用PreparedStatement。
     * 它实际上相当于具有空参数Map的查询调用。
     *
     * @param sql 要执行的SQL查询
     * @param rse 将提取结果的对象
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果查询失败
     */
    <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建准备好的语句和绑定到查询的参数列表，并使用RowCallbackHandler按行读取结果集。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @param rch         对象，该对象将每次提取一行结果
     * @throws DataAccessException 如果查询失败
     */
    void query(String sql, SqlParameterSource paramSource, RowCallbackHandler rch) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建准备好的语句和绑定到查询的参数列表，并使用RowCallbackHandler按行读取结果集。
     *
     * @param sql      要执行的SQL查询
     * @param paramMap 要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param rch      对象，该对象将每次提取一行结果
     * @throws DataAccessException 如果查询失败
     */
    void query(String sql, Map<String, ?> paramMap, RowCallbackHandler rch) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建准备好的语句，使用RowCallbackHandler按行读取结果集。
     * <p>注意：与具有相同签名的JdbcOperations方法不同，此查询变体始终使用PreparedStatement。
     * 它实际上相当于具有空参数Map的查询调用。
     *
     * @param sql 要执行的SQL查询
     * @param rch 对象，该对象将每次提取一行结果
     * @throws DataAccessException 如果查询失败
     */
    void query(String sql, RowCallbackHandler rch) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，通过RowMapper将每一行Map到Java对象。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @param rowMapper   对象，该对象将Map每行一个对象
     * @return 包含Map对象的结果列表
     * @throws DataAccessException 如果查询失败
     */
    <T> List<T> query(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，通过RowMapper将每一行Map到Java对象。
     *
     * @param sql       要执行的SQL查询
     * @param paramMap  要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param rowMapper 对象，该对象将Map每行一个对象
     * @return 包含Map对象的结果列表
     * @throws DataAccessException 如果查询失败
     */
    <T> List<T> query(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，通过RowMapper将每一行Map到一个Java对象。
     * <p>注意：与具有相同签名的JdbcOperations方法不同，此查询变体始终使用PreparedStatement。它实际上相当于具有空参数Map的查询调用。
     *
     * @param sql       要执行的SQL查询
     * @param rowMapper 对象，该对象将Map每行一个对象
     * @return 包含Map对象的结果列表
     * @throws DataAccessException 如果查询失败
     */
    <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句和一系列要绑定到查询的参数，
     * 通过RowMapper将每一行Map到一个Java对象，并将其转换为一个可iterable和closeable流。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @param rowMapper   对象，该对象将Map每行一个对象
     * @return 包含Map对象的结果流在完全处理后需要关闭（例如，通过try with resources子句）
     * @throws DataAccessException 如果查询失败
     */
    <T> Stream<T> queryForStream(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句和一系列要绑定到查询的参数，
     * 通过RowMapper将每一行Map到一个Java对象，并将其转换为一个可iterable和closeable流。
     *
     * @param sql       要执行的SQL查询
     * @param paramMap  要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param rowMapper 对象，该对象将Map每行一个对象
     * @return 包含Map对象的结果流在完全处理后需要关闭（例如，通过try with resources子句）
     * @throws DataAccessException 如果查询失败
     */
    <T> Stream<T> queryForStream(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，通过RowMapper将单个结果行Map到Java对象。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @param rowMapper   对象，该对象将Map每行一个对象
     * @return 单个Map对象（如果给定的{@link RowMapper}返回null，则可能为null）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     */
    <T> T queryForObject(String sql, SqlParameterSource paramSource, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，通过RowMapper将单个结果行Map到Java对象。
     *
     * @param sql       要执行的SQL查询
     * @param paramMap  要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param rowMapper 对象，该对象将Map每行一个对象
     * @return 单个Map对象（如果给定的{@link RowMapper}返回null，则可能为null）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     */
    <T> T queryForObject(String sql, Map<String, ?> paramMap, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果对象。
     * <p>查询应为单行/单列查询；返回的结果将直接Map到相应的对象类型。
     *
     * @param sql          要执行的SQL查询
     * @param paramSource  要绑定到查询的参数容器
     * @param requiredType 结果对象预期匹配的类型
     * @return 所需类型的结果对象，如果是SQL null，则为null
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException  如果查询不恰好返回一行
     * @throws org.clever.jdbc.IncorrectResultSetColumnCountException 如果查询未返回包含单列的行
     * @throws DataAccessException                                    如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForObject(String, Class)
     * @see org.clever.jdbc.core.SingleColumnRowMapper
     */
    <T> T queryForObject(String sql, SqlParameterSource paramSource, Class<T> requiredType) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果对象。
     * <p>查询应为单行/单列查询；返回的结果将直接Map到相应的对象类型。
     *
     * @param sql          要执行的SQL查询
     * @param paramMap     要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param requiredType 结果对象预期匹配的类型
     * @return 所需类型的结果对象，如果是SQL null，则为null
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException  如果查询不恰好返回一行
     * @throws org.clever.jdbc.IncorrectResultSetColumnCountException 如果查询未返回包含单列的行
     * @throws DataAccessException                                    如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForObject(String, Class)
     */
    <T> T queryForObject(String sql, Map<String, ?> paramMap, Class<T> requiredType) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL中创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果Map。
     * <p>查询应为单行查询；结果行将Map到Map（每列一个条目，使用列名作为键）。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @return 结果Map（每列一个条目，使用列名作为键）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForMap(String)
     * @see org.clever.jdbc.core.ColumnMapRowMapper
     */
    Map<String, Object> queryForMap(String sql, SqlParameterSource paramSource) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL中创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果Map。
     * 当您没有域模型时，此接口定义的queryForMap()方法是合适的。否则，请考虑使用queryForObject()方法之一。
     * <p>查询应为单行查询；结果行将Map到Map（每列一个条目，使用列名作为键）。
     *
     * @param sql      要执行的SQL查询
     * @param paramMap 要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @return 结果Map（每列一个条目，使用列名作为键）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForMap(String)
     * @see org.clever.jdbc.core.ColumnMapRowMapper
     */
    Map<String, Object> queryForMap(String sql, Map<String, ?> paramMap) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将Map到结果对象的列表（每行一个条目），每个对象都与指定的元素类型匹配。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @param elementType 结果列表中所需的元素类型(例如 {@code Integer.class})
     * @return 与指定元素类型匹配的对象列表
     * @throws DataAccessException 如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForList(String, Class)
     * @see org.clever.jdbc.core.SingleColumnRowMapper
     */
    <T> List<T> queryForList(String sql, SqlParameterSource paramSource, Class<T> elementType) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将Map到结果对象的列表（每行一个条目），每个对象都与指定的元素类型匹配。
     *
     * @param sql         要执行的SQL查询
     * @param paramMap    要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @param elementType 结果列表中所需的元素类型(例如 {@code Integer.class})
     * @return 与指定元素类型匹配的对象列表
     * @throws DataAccessException 如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForList(String, Class)
     * @see org.clever.jdbc.core.SingleColumnRowMapper
     */
    <T> List<T> queryForList(String sql, Map<String, ?> paramMap, Class<T> elementType) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将Map到Map列表（每行一个条目）（每列一个条目，使用列名作为键）。
     * 列表中的每个元素都是该接口的{@code queryForMap}方法返回的形式。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @return 每行包含一个Map的列表
     * @throws DataAccessException 如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForList(String)
     */
    List<Map<String, Object>> queryForList(String sql, SqlParameterSource paramSource) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将Map到Map列表（每行一个条目）（每列一个条目，使用列名作为键）。
     * 列表中的每个元素都是该接口的{@code queryForMap}方法返回的形式。
     *
     * @param sql      要执行的SQL查询
     * @param paramMap 要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @return 每行包含一个Map的列表
     * @throws DataAccessException 如果查询失败
     * @see org.clever.jdbc.core.JdbcTemplate#queryForList(String)
     */
    List<Map<String, Object>> queryForList(String sql, Map<String, ?> paramMap) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，应为SqlRowSet。
     * <p>结果将Map到SqlRowSet，该SqlRowSet以断开连接的方式保存数据。此包装器将转换抛出的任何SQLExceptions。
     * <p>注意，对于默认实现，JDBC行集支持需要在运行时可用：默认情况下，
     * 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类，它是JDK 1.5+的一部分，
     * 也可以作为Sun的JDBC行集实现下载（rowset.jar）的一部分单独提供。
     *
     * @param sql         要执行的SQL查询
     * @param paramSource 要绑定到查询的参数容器
     * @return SqlRowSet表示(可能是 javax.sql.rowset.CachedRowSet)
     * @throws DataAccessException 如果执行查询时出现任何问题
     * @see org.clever.jdbc.core.JdbcTemplate#queryForRowSet(String)
     * @see org.clever.jdbc.core.SqlRowSetResultSetExtractor
     * @see javax.sql.rowset.CachedRowSet
     */
    SqlRowSet queryForRowSet(String sql, SqlParameterSource paramSource) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，应为SqlRowSet。
     * <p>结果将Map到SqlRowSet，该SqlRowSet以断开连接的方式保存数据。此包装器将转换抛出的任何SQLExceptions。
     * <p>注意，对于默认实现，JDBC行集支持需要在运行时可用：默认情况下，
     * 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类，它是JDK 1.5+的一部分，
     * 也可以作为Sun的JDBC行集实现下载（rowset.jar）的一部分单独提供。
     *
     * @param sql      要执行的SQL查询
     * @param paramMap 要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @return SqlRowSet表示法(可能是包裹在 javax.sql.rowset.CachedRowSet)
     * @throws DataAccessException 如果执行查询时出现任何问题
     * @see org.clever.jdbc.core.JdbcTemplate#queryForRowSet(String)
     * @see org.clever.jdbc.core.SqlRowSetResultSetExtractor
     * @see javax.sql.rowset.CachedRowSet
     */
    SqlRowSet queryForRowSet(String sql, Map<String, ?> paramMap) throws DataAccessException;

    /**
     * 通过准备好的语句发出更新，绑定给定的参数。
     *
     * @param sql         包含命名参数的SQL
     * @param paramSource 要绑定到查询的参数和SQL类型的容器
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int update(String sql, SqlParameterSource paramSource) throws DataAccessException;

    /**
     * 通过准备好的语句发出更新，绑定给定的参数。
     *
     * @param sql      包含命名参数的SQL
     * @param paramMap 要绑定到查询的参数Map（让PreparedStatement猜测相应的SQL类型）
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int update(String sql, Map<String, ?> paramMap) throws DataAccessException;

    /**
     * 通过准备好的语句发出更新，绑定给定的参数，返回生成的键。
     *
     * @param sql                包含命名参数的SQL
     * @param paramSource        要绑定到查询的参数和SQL类型的容器
     * @param generatedKeyHolder 将持有生成的keys的{@link KeyHolder}
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     * @see MapSqlParameterSource
     */
    int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder) throws DataAccessException;

    /**
     * 通过准备好的语句发出更新，绑定给定的参数，返回生成的键。
     *
     * @param sql                包含命名参数的SQL
     * @param paramSource        要绑定到查询的参数和SQL类型的容器
     * @param generatedKeyHolder 将持有生成的keys的{@link KeyHolder}
     * @param keyColumnNames     将为其生成键的列的名称
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     * @see MapSqlParameterSource
     */
    int update(String sql, SqlParameterSource paramSource, KeyHolder generatedKeyHolder, String[] keyColumnNames) throws DataAccessException;

    /**
     * 使用提供的SQL语句和提供的参数批执行批。
     *
     * @param sql         要执行的SQL语句
     * @param batchValues 包含查询的一批参数的Map数组
     * @return 一个数组，包含受批处理中每次更新影响的行数
     * (还可能包含受影响行的特殊JDBC定义的负值，例如 {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int[] batchUpdate(String sql, Map<String, ?>[] batchValues);

    /**
     * 使用提供的SQL语句和提供的参数批执行批处理。
     *
     * @param sql       要执行的SQL语句
     * @param batchArgs 包含查询参数批的{@link SqlParameterSource}数组
     * @return 一个数组，包含受批处理中每次更新影响的行数
     * (还可能包含受影响行的特殊JDBC定义的负值，例如 {@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int[] batchUpdate(String sql, SqlParameterSource[] batchArgs);
}
