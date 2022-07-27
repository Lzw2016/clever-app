package org.clever.jdbc.core;

import org.clever.dao.DataAccessException;
import org.clever.jdbc.support.KeyHolder;
import org.clever.jdbc.support.rowset.SqlRowSet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 指定基本JDBC操作集的接口。
 *
 * <p>由{@link JdbcTemplate}实现。不经常直接使用，但这是增强可测试性的一个有用选项，因为它很容易被模仿或存根。
 *
 * <p>或者，可以模拟标准JDBC基础结构。然而，模拟该接口的工作量要少得多。
 * 作为测试数据访问代码的模拟对象方法的替代方法
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 21:38 <br/>
 *
 * @see JdbcTemplate
 */
public interface JdbcOperations {
    //-------------------------------------------------------------------------
    // Methods dealing with a plain java.sql.Connection
    //-------------------------------------------------------------------------

    /**
     * 执行JDBC数据访问操作，作为处理JDBC连接的回调操作实现。
     * 这允许在托管JDBC环境中实现任意数据访问操作：
     * 即，参与事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param action 指定操作的回调对象
     * @return 操作返回的结果对象，如果没有，则为null
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(ConnectionCallback<T> action) throws DataAccessException;

    //-------------------------------------------------------------------------
    // Methods dealing with static SQL (java.sql.Statement)
    //-------------------------------------------------------------------------

    /**
     * 执行JDBC数据访问操作，作为处理JDBC语句的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param action 指定操作的回调
     * @return 操作返回的结果对象，如果没有，则为null
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(StatementCallback<T> action) throws DataAccessException;

    /**
     * 发出单个SQL execute，通常是DDL语句。
     *
     * @param sql 要执行的静态SQL
     * @throws DataAccessException 如果有任何问题
     */
    void execute(String sql) throws DataAccessException;

    /**
     * 执行给定静态SQL的查询，使用ResultSetExtractor读取结果集。
     * <p>使用JDBC语句，而不是PreparedStatement。
     * 如果要使用PreparedStatement执行静态查询，请使用参数数组为null的重载{@code query}方法。
     *
     * @param sql 要执行的SQL查询
     * @param rse 将提取所有结果行的回调
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果执行查询时出现任何问题
     * @see #query(String, ResultSetExtractor, Object...)
     */
    <T> T query(String sql, ResultSetExtractor<T> rse) throws DataAccessException;

    /**
     * 执行给定静态SQL的查询，使用RowCallbackHandler按行读取结果集。
     * <p>使用JDBC语句，而不是PreparedStatement。
     * 如果要使用PreparedStatement执行静态查询，请使用参数数组为null的重载{@code query}方法。
     *
     * @param sql 要执行的SQL查询
     * @param rch 一次提取一行结果的回调
     * @throws DataAccessException 如果执行查询时出现任何问题
     * @see #query(String, RowCallbackHandler, Object...)
     */
    void query(String sql, RowCallbackHandler rch) throws DataAccessException;

    /**
     * 执行给定静态SQL的查询，通过行映射器将每一行映射到结果对象。
     * <p>使用JDBC语句，而不是PreparedStatement。如果要使用PreparedStatement执行静态查询，
     * 请使用重载{@code query} 方法，将null作为参数数组。
     *
     * @param sql       要执行的SQL查询
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果列表
     * @throws DataAccessException 如果执行查询有任何问题
     * @see #query(String, RowMapper, Object...)
     */
    <T> List<T> query(String sql, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 执行给定静态SQL的查询，通过行映射器将每一行映射到一个结果对象，并将其转换为一个可iterable和closeable流。
     * <p>使用JDBC语句，而不是PreparedStatement。如果要使用PreparedStatement执行静态查询，
     * 请使用重载{@code query} 方法，将null作为参数数组。
     *
     * @param sql       要执行的SQL查询
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果流在完全处理后需要关闭（例如，通过try with resources子句）
     * @throws DataAccessException 如果执行查询有任何问题
     * @see #queryForStream(String, RowMapper, Object...)
     */
    <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 执行给定静态SQL的查询，通过行映射器将单个结果行映射到结果对象。
     * <p>使用JDBC语句，而不是PreparedStatement。如果要使用PreparedStatement执行静态查询，
     * 请使用重载{@link #queryForObject(String, RowMapper, Object...)}方法，将null作为参数数组。
     *
     * @param sql       要执行的SQL查询
     * @param rowMapper 每行映射一个对象的回调
     * @return 单个映射对象（如果给定的行映射器返回null，则可能为null）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果执行查询有任何问题
     * @see #queryForObject(String, RowMapper, Object...)
     */
    <T> T queryForObject(String sql, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 在给定静态SQL的情况下，对结果对象执行查询。
     * <p>使用JDBC语句，而不是PreparedStatement。如果要使用PreparedStatement执行静态查询，
     * 请使用重载{@link #queryForObject(String, Class, Object...)}方法，将null作为参数数组。
     * <p>此方法对于运行具有已知结果的静态SQL非常有用。查询应为单行单列查询；返回的结果将直接映射到相应的对象类型。
     *
     * @param sql          要执行的SQL查询
     * @param requiredType 结果对象预期匹配的类型
     * @return 所需类型的结果对象，如果是SQL null，则为null
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException  如果查询不恰好返回一行
     * @throws org.clever.jdbc.IncorrectResultSetColumnCountException 如果查询未返回包含单列的行
     * @throws DataAccessException                                    如果执行查询有任何问题
     * @see #queryForObject(String, Class, Object...)
     */
    <T> T queryForObject(String sql, Class<T> requiredType) throws DataAccessException;

    /**
     * 在给定静态SQL的情况下，对结果映射执行查询。
     * <p>使用JDBC语句，而不是PreparedStatement。如果要使用PreparedStatement执行静态查询，
     * 请使用重载{@link #queryForMap(String, Object...)}方法，将null作为参数数组。
     * <p>查询应为单行查询；结果行将映射到映射（每列一个条目，使用列名作为键）。
     *
     * @param sql 要执行的SQL查询
     * @return 结果映射（每列一个条目，列名为键）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果执行查询有任何问题
     * @see #queryForMap(String, Object...)
     * @see ColumnMapRowMapper
     */
    Map<String, Object> queryForMap(String sql) throws DataAccessException;

    /**
     * 在给定静态SQL的情况下，对结果列表执行查询。
     * <p>使用JDBC语句，而不是PreparedStatement。如果要使用PreparedStatement执行静态查询，
     * 请使用重载{@code queryForList}方法，将null作为参数数组。
     * <p>结果将映射到结果对象的列表（每行一个条目），每个对象都与指定的元素类型匹配。
     *
     * @param sql         要执行的SQL查询
     * @param elementType 结果列表中所需的元素类型 (例如 {@code Integer.class})
     * @return 与指定元素类型匹配的对象列表
     * @throws DataAccessException 如果执行查询有任何问题
     * @see #queryForList(String, Class, Object...)
     * @see SingleColumnRowMapper
     */
    <T> List<T> queryForList(String sql, Class<T> elementType) throws DataAccessException;

    /**
     * 在给定静态SQL的情况下，对结果列表执行查询。
     * <p>使用JDBC语句，而不是PreparedStatement。如果要使用PreparedStatement执行静态查询，
     * 请使用重载{@code queryForList}方法，将null作为参数数组。
     * <p>结果将映射到映射列表（每行一个条目）（每列一个条目，使用列名作为键）。
     * 列表中的每个元素都是该接口的{@code queryForMap}方法返回的形式。
     *
     * @param sql 要执行的SQL查询
     * @return 每行包含一个映射的列表
     * @throws DataAccessException 如果执行查询有任何问题
     * @see #queryForList(String, Object...)
     */
    List<Map<String, Object>> queryForList(String sql) throws DataAccessException;

    /**
     * 在给定静态SQL的情况下，对SqlRowSet执行查询。
     * <p>使用JDBC语句，而不是PreparedStatement。
     * 如果要使用PreparedStatement执行静态查询，请使用重载{@code queryForRowSet}方法，将null作为参数数组。
     * <p>结果将映射到SqlRowSet，该SqlRowSet以断开连接的方式保存数据。此包装器将转换抛出的任何SQLExceptions。
     * <p>注意，对于默认实现，JDBC行集支持需要在运行时可用：默认情况下，
     * 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类，它是JDK 1.5+的一部分，
     * 也可以作为Sun的JDBC行集实现下载（rowset.jar）的一部分单独提供。
     *
     * @param sql 要执行的SQL查询
     * @return SqlRowSet表示法(可能是包裹在 javax.sql.rowset.CachedRowSet)
     * @throws DataAccessException 如果执行查询有任何问题
     * @see #queryForRowSet(String, Object...)
     * @see SqlRowSetResultSetExtractor
     * @see javax.sql.rowset.CachedRowSet
     */
    SqlRowSet queryForRowSet(String sql) throws DataAccessException;

    /**
     * 发出单个SQL更新操作（例如insert、update或delete语句）。
     *
     * @param sql 要执行的静态SQL
     * @return 受影响的行数
     * @throws DataAccessException 如果有任何问题.
     */
    int update(String sql) throws DataAccessException;

    /**
     * 使用批处理在单个JDBC语句上发出多个SQL更新。
     * <p>如果JDBC驱动程序不支持批量更新，则会退回到单个语句上的单独更新。
     *
     * @param sql 定义将执行的SQL语句数组。
     * @return 受每条语句影响的行数的数组
     * @throws DataAccessException 执行批处理如果有任何问题
     */
    int[] batchUpdate(String... sql) throws DataAccessException;

    //-------------------------------------------------------------------------
    // Methods dealing with prepared statements
    //-------------------------------------------------------------------------

    /**
     * 执行JDBC数据访问操作，作为处理JDBC PreparedStatement的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param psc    在给定连接的情况下创建PreparedStatement的回调
     * @param action 指定操作的回调
     * @return 操作返回的结果对象，如果没有，则为null
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) throws DataAccessException;

    /**
     * 执行JDBC数据访问操作，作为处理JDBC PreparedStatement的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param sql    要执行的SQL
     * @param action 指定操作的回调
     * @return 操作返回的结果对象，如果没有，则为null
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException;

    /**
     * 使用准备好的语句进行查询，使用ResultSetExtractor读取结果集。
     * <p>PreparedStatementCreator可以直接实现，也可以通过PreparedStatementCreatorFactory进行配置。
     *
     * @param psc 在给定连接的情况下创建PreparedStatement的回调
     * @param rse 将提取结果的回调
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果有任何问题
     */
    <T> T query(PreparedStatementCreator psc, ResultSetExtractor<T> rse) throws DataAccessException;

    /**
     * 使用准备好的语句进行查询，使用ResultSetExtractor读取结果集。
     *
     * @param sql 要执行的SQL查询
     * @param pss 知道如何在准备好的语句上设置值的回调。如果为null，则假设SQL不包含任何绑定参数。
     *            即使没有绑定参数，也可以使用此回调设置获取大小和其他性能选项。
     * @param rse 将提取结果的回调
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果有任何问题
     */
    <T> T query(String sql, PreparedStatementSetter pss, ResultSetExtractor<T> rse) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数，使用ResultSetExtractor读取结果集。
     *
     * @param sql      要执行的SQL查询
     * @param args     要绑定到查询的参数
     * @param argTypes 参数的SQL类型 (常数来自 {@code java.sql.Types})
     * @param rse      将提取结果的回调
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果查询失败
     * @see java.sql.Types
     */
    <T> T query(String sql, Object[] args, int[] argTypes, ResultSetExtractor<T> rse) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数，使用ResultSetExtractor读取结果集。
     *
     * @param sql  要执行的SQL查询
     * @param rse  将提取结果的回调
     * @param args 要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型);
     *             还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 由ResultSetExtractor返回的任意结果对象
     * @throws DataAccessException 如果查询失败
     */
    <T> T query(String sql, ResultSetExtractor<T> rse, Object... args) throws DataAccessException;

    /**
     * 使用准备好的语句进行查询，使用RowCallbackHandler按行读取结果集。
     * <p>PreparedStatementCreator可以直接实现，也可以通过PreparedStatementCreatorFactory进行配置。
     *
     * @param psc 在给定连接的情况下创建PreparedStatement的回调
     * @param rch 一次提取一行结果的回调
     * @throws DataAccessException 如果有任何问题
     */
    void query(PreparedStatementCreator psc, RowCallbackHandler rch) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和PreparedStatementSetter实现中创建准备好的语句，
     * PreparedStatementSetter实现知道如何将值绑定到查询，并使用RowCallbackHandler逐行读取结果集
     *
     * @param sql 要执行的SQL查询
     * @param pss 知道如何在准备好的语句上设置值的回调。如果为null，则假设SQL不包含任何绑定参数。
     *            即使没有绑定参数，也可以使用此回调设置获取大小和其他性能选项。
     * @param rch 一次提取一行结果的回调
     * @throws DataAccessException 如果查询失败
     */
    void query(String sql, PreparedStatementSetter pss, RowCallbackHandler rch) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 使用RowCallbackHandler按行读取结果集。
     *
     * @param sql      要执行的SQL查询
     * @param args     要绑定到查询的参数
     * @param argTypes 参数的SQL类型 (常数来自 {@code java.sql.Types})
     * @param rch      一次提取一行结果的回调
     * @throws DataAccessException 如果查询失败
     * @see java.sql.Types
     */
    void query(String sql, Object[] args, int[] argTypes, RowCallbackHandler rch) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 使用RowCallbackHandler按行读取结果集。
     *
     * @param sql  要执行的SQL查询
     * @param rch  一次提取一行结果的回调
     * @param args 要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型);
     *             还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @throws DataAccessException 如果查询失败
     */
    void query(String sql, RowCallbackHandler rch, Object... args) throws DataAccessException;

    /**
     * 使用准备好的语句进行查询，通过行映射器将每一行映射到结果对象。
     * <p>PreparedStatementCreator可以直接实现，也可以通过PreparedStatementCreatorFactory进行配置。
     *
     * @param psc       在给定连接的情况下创建PreparedStatement的回调
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果列表
     * @throws DataAccessException 如果有任何问题
     */
    <T> List<T> query(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和PreparedStatementSetter实现中创建准备好的语句，
     * PreparedStatementSetter实现知道如何将值绑定到查询，并通过行映射器将每一行映射到结果对象。
     *
     * @param sql       要执行的SQL查询
     * @param pss       知道如何在准备好的语句上设置值的回调。如果为null，则假设SQL不包含任何绑定参数。
     *                  即使没有绑定参数，也可以使用此回调设置获取大小和其他性能选项。
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果列表
     * @throws DataAccessException 如果查询失败
     */
    <T> List<T> query(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 通过行映射器将每一行映射到结果对象。
     *
     * @param sql       要执行的SQL查询
     * @param args      要绑定到查询的参数
     * @param argTypes  参数的SQL类型
     *                  (常数来自 {@code java.sql.Types})
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果列表
     * @throws DataAccessException 如果查询失败
     * @see java.sql.Types
     */
    <T> List<T> query(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数,通过行映射器将每一行映射到结果对象。
     *
     * @param sql       要执行的SQL查询
     * @param rowMapper 每行映射一个对象的回调
     * @param args      要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型);
     *                  还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 包含映射对象的结果列表
     * @throws DataAccessException 如果查询失败
     */
    <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException;

    /**
     * 使用准备好的语句进行查询，通过行映射器将每一行映射到一个结果对象，并将其转换为一个可iterable和closeable流。
     * <p>PreparedStatementCreator可以直接实现，也可以通过PreparedStatementCreatorFactory进行配置。
     *
     * @param psc       在给定连接的情况下创建PreparedStatement的回调
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果流在完全处理后需要关闭（例如，通过try with resources子句）
     * @throws DataAccessException 如果有任何问题
     */
    <T> Stream<T> queryForStream(PreparedStatementCreator psc, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和PreparedStatementSetter实现中创建一个准备好的语句，
     * PreparedStatementSetter实现知道如何将值绑定到查询，通过行映射器将每一行映射到一个结果对象，并将其转换为一个可iterable和closeable流。
     *
     * @param sql       要执行的SQL查询
     * @param pss       知道如何在准备好的语句上设置值的回调。如果为null，则假设SQL不包含任何绑定参数。
     *                  即使没有绑定参数，也可以使用此回调设置获取大小和其他性能选项。
     * @param rowMapper 每行映射一个对象的回调
     * @return 包含映射对象的结果流在完全处理后需要关闭（例如，通过try with resources子句）
     * @throws DataAccessException 如果查询失败
     */
    <T> Stream<T> queryForStream(String sql, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 通过行映射器将每一行映射到一个结果对象，并将其转化为一个可编辑且可关闭的流。
     *
     * @param sql       要执行的SQL查询
     * @param rowMapper 每行映射一个对象的回调
     * @param args      要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型);
     *                  还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 包含映射对象的结果流在完全处理后需要关闭（例如，通过try with resources子句）
     * @throws DataAccessException 如果查询失败
     */
    <T> Stream<T> queryForStream(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 通过行映射器将单个结果行映射到结果对象。
     *
     * @param sql       要执行的SQL查询
     * @param args      要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型)
     * @param argTypes  参数的SQL类型 (常数来自 {@code java.sql.Types})
     * @param rowMapper 每行映射一个对象的回调
     * @return 单个映射对象（如果给定的行映射器返回null，则可能为null）
     * {@link RowMapper} returned {@code} null)
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     */
    <T> T queryForObject(String sql, Object[] args, int[] argTypes, RowMapper<T> rowMapper) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 通过行映射器将单个结果行映射到结果对象。
     *
     * @param sql       要执行的SQL查询
     * @param rowMapper 每行映射一个对象的回调
     * @param args      要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型);
     *                  还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 单个映射对象（如果给定的行映射器返回null，则可能为null）
     * {@link RowMapper} returned {@code} null)
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     */
    <T> T queryForObject(String sql, RowMapper<T> rowMapper, Object... args) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 应为结果对象。
     * <p>查询应为单行单列查询；返回的结果将直接映射到相应的对象类型。
     *
     * @param sql          要执行的SQL查询
     * @param args         要绑定到查询的参数
     * @param argTypes     参数的SQL类型
     *                     (常数来自 {@code java.sql.Types})
     * @param requiredType 结果对象预期匹配的类型
     * @return 所需类型的结果对象，如果是SQL null，则为null
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException  如果查询不恰好返回一行
     * @throws org.clever.jdbc.IncorrectResultSetColumnCountException 如果查询未返回包含单列的行
     * @throws DataAccessException                                    如果查询失败
     * @see #queryForObject(String, Class)
     * @see java.sql.Types
     */
    <T> T queryForObject(String sql, Object[] args, int[] argTypes, Class<T> requiredType) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 应为结果对象。
     * <p>查询应为单行单列查询；返回的结果将直接映射到相应的对象类型。
     *
     * @param sql          要执行的SQL查询
     * @param requiredType 结果对象预期匹配的类型
     * @param args         要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型);
     *                     还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 所需类型的结果对象，如果是SQL null，则为null
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException  如果查询不恰好返回一行
     * @throws org.clever.jdbc.IncorrectResultSetColumnCountException 如果查询未返回包含单列的行
     * @throws DataAccessException                                    如果查询失败
     * @see #queryForObject(String, Class)
     */
    <T> T queryForObject(String sql, Class<T> requiredType, Object... args) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL和要绑定到查询的参数, 需要结果映射。
     * <p>查询应为单行查询；结果行将映射到映射（每列一个条目，使用列名作为键）。
     *
     * @param sql      要执行的SQL查询
     * @param args     要绑定到查询的参数
     * @param argTypes 参数的SQL类型 (常数来自 {@code java.sql.Types})
     * @return 结果映射（每列一个条目，列名为键）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     * @see #queryForMap(String)
     * @see ColumnMapRowMapper
     * @see java.sql.Types
     */
    Map<String, Object> queryForMap(String sql, Object[] args, int[] argTypes) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL中创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果映射。
     * <p>当您没有域模型时，此接口定义的{@code queryForMap}方法是合适的。否则，请考虑使用{@code queryForObject}方法。
     * <p>查询应为单行查询；结果行将映射到映射（每列一个条目，使用列名作为键）。
     *
     * @param sql  要执行的SQL查询
     * @param args 要绑定到查询的参数 (让PreparedStatement猜测相应的SQL类型);
     *             还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 结果映射（每列一个条目，使用列名作为键）
     * @throws org.clever.dao.IncorrectResultSizeDataAccessException 如果查询不恰好返回一行
     * @throws DataAccessException                                   如果查询失败
     * @see #queryForMap(String)
     * @see ColumnMapRowMapper
     */
    Map<String, Object> queryForMap(String sql, Object... args) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将映射到结果对象的列表（每行一个条目），每个对象都与指定的元素类型匹配。
     *
     * @param sql         要执行的SQL查询
     * @param args        要绑定到查询的参数
     * @param argTypes    参数的SQL类型 (常数来自 {@code java.sql.Types})
     * @param elementType 结果列表中所需的元素类型 (例如 {@code Integer.class})
     * @return 与指定元素类型匹配的对象列表
     * @throws DataAccessException 如果查询失败
     * @see #queryForList(String, Class)
     * @see SingleColumnRowMapper
     */
    <T> List<T> queryForList(String sql, Object[] args, int[] argTypes, Class<T> elementType) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将映射到结果对象的列表（每行一个条目），每个对象都与指定的元素类型匹配。
     *
     * @param sql         要执行的SQL查询
     * @param elementType 结果列表中所需的元素类型（例如，{@code Integer.class}）
     * @param args        绑定到查询的参数（让PreparedStatement猜测相应的SQL类型）；
     *                    还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 与指定元素类型匹配的对象列表
     * @throws DataAccessException 如果查询失败
     * @see #queryForList(String, Class)
     * @see SingleColumnRowMapper
     */
    <T> List<T> queryForList(String sql, Class<T> elementType, Object... args) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将映射到映射列表（每行一个条目）（每列一个条目，使用列名作为键）。
     * 列表中的每个元素都是该接口的{@code queryForMap}方法返回的形式。
     *
     * @param sql      要执行的SQL查询
     * @param args     要绑定到查询的参数
     * @param argTypes 参数的SQL类型 (常数来自 {@code java.sql.Types})
     * @return 每行包含一个映射的列表
     * @throws DataAccessException 如果查询失败
     * @see #queryForList(String)
     * @see java.sql.Types
     */
    List<Map<String, Object>> queryForList(String sql, Object[] args, int[] argTypes) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，需要一个结果列表。
     * <p>结果将映射到映射列表（每行一个条目）（每列一个条目，使用列名作为键）。
     * 列表中的每个元素都是该接口的{@code queryForMap}方法返回的形式。
     *
     * @param sql  要执行的SQL查询
     * @param args 绑定到查询的参数（让PreparedStatement猜测相应的SQL类型）；
     *             还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 每行包含一个映射的列表
     * @throws DataAccessException 如果查询失败
     * @see #queryForList(String)
     */
    List<Map<String, Object>> queryForList(String sql, Object... args) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，应为SqlRowSet。
     * <p>结果将映射到SqlRowSet，该SqlRowSet以断开连接的方式保存数据。此包装器将转换抛出的任何SQLExceptions。
     * <p>注意，对于默认实现，JDBC行集支持需要在运行时可用：默认情况下，
     * 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类，它是JDK 1.5+的一部分，
     * 也可以作为Sun的JDBC行集实现下载（rowset.jar）的一部分单独提供。
     *
     * @param sql      要执行的SQL查询
     * @param args     要绑定到查询的参数
     * @param argTypes 参数的SQL类型(常数来自 {@code java.sql.Types})
     * @return SqlRowSet表示法 (可能是包裹在 {@code javax.sql.rowset.CachedRowSet})
     * @throws DataAccessException 如果执行查询时出现任何问题
     * @see #queryForRowSet(String)
     * @see SqlRowSetResultSetExtractor
     * @see javax.sql.rowset.CachedRowSet
     * @see java.sql.Types
     */
    SqlRowSet queryForRowSet(String sql, Object[] args, int[] argTypes) throws DataAccessException;

    /**
     * 查询给定的SQL以从SQL创建一个准备好的语句，并创建一个参数列表以绑定到查询，应为SqlRowSet。
     * <p>结果将映射到SqlRowSet，该SqlRowSet以断开连接的方式保存数据。此包装器将转换抛出的任何SQLExceptions。
     * <p>注意，对于默认实现，JDBC行集支持需要在运行时可用：默认情况下，
     * 使用Sun的{@code com.sun.rowset.CachedRowSetImpl}类，它是JDK 1.5+的一部分，
     * 也可以作为Sun的JDBC行集实现下载（rowset.jar）的一部分单独提供。
     *
     * @param sql  要执行的SQL查询
     * @param args 绑定到查询的参数（让PreparedStatement猜测相应的SQL类型）；
     *             还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return SqlRowSet表示法 (可能是包裹在 {@code javax.sql.rowset.CachedRowSet})
     * @throws DataAccessException 如果执行查询时出现任何问题
     * @see #queryForRowSet(String)
     * @see SqlRowSetResultSetExtractor
     * @see javax.sql.rowset.CachedRowSet
     */
    SqlRowSet queryForRowSet(String sql, Object... args) throws DataAccessException;

    /**
     * 使用PreparedStatementCreator发出单个SQL更新操作（例如insert、update或delete语句），以提供SQL和任何必需的参数。
     * <p>PreparedStatementCreator可以直接实现，也可以通过PreparedStatementCreatorFactory进行配置。
     *
     * @param psc 提供SQL和任何必要参数的回调
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int update(PreparedStatementCreator psc) throws DataAccessException;

    /**
     * 使用PreparedStatementCreator发出update语句，以提供SQL和任何必需的参数。生成的密钥将放入给定的密钥保持器中。
     * <p>请注意，给定的PreparedStatementCreator必须创建一个具有激活的生成键提取的语句（JDBC 3.0特性）。
     * 这可以直接完成，也可以通过使用PreparedStatementCreatorFactory完成。
     *
     * @param psc                提供SQL和任何必要参数的回调
     * @param generatedKeyHolder 将持有生成的key的KeyHolder
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int update(PreparedStatementCreator psc, KeyHolder generatedKeyHolder) throws DataAccessException;

    /**
     * 使用PreparedStatementSetter发出update语句，以使用给定的SQL设置绑定参数。
     * 比使用PreparedStatementCreator更简单，因为此方法将创建PreparedStatement：PreparedStatementSetter只需要设置参数。
     *
     * @param sql 包含绑定参数的SQL
     * @param pss 设置绑定参数的助手。如果为空，则使用静态SQL运行更新。
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int update(String sql, PreparedStatementSetter pss) throws DataAccessException;

    /**
     * 通过准备好的语句发出单个SQL更新操作（例如insert、update或delete语句），绑定给定的参数。
     *
     * @param sql      包含绑定参数的SQL
     * @param args     要绑定到查询的参数
     * @param argTypes 参数的SQL类型（来自{@code java.sql.Types}的常量）
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     * @see java.sql.Types
     */
    int update(String sql, Object[] args, int[] argTypes) throws DataAccessException;

    /**
     * 通过准备好的语句发出单个SQL更新操作（例如insert、update或delete语句），绑定给定的参数。
     *
     * @param sql  包含绑定参数的SQL
     * @param args 绑定到查询的参数（让PreparedStatement猜测相应的SQL类型）；
     *             还可能包含{@link SqlParameterValue}对象，这些对象不仅指示参数值，还指示SQL类型和可选的scale
     * @return 受影响的行数
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int update(String sql, Object... args) throws DataAccessException;

    /**
     * 在单个PreparedStatement上发出多个更新语句，使用批更新和BatchPreparedStatementSetter设置值。
     * <p>如果JDBC驱动程序不支持批量更新，则会退回到单个PreparedStatement上的单独更新。
     *
     * @param sql 定义将被重用的PreparedStatement。批处理中的所有语句将使用相同的SQL。
     * @param pss 对象设置由该方法创建的PreparedStatement的参数
     * @return 受每条语句影响的行数的数组
     * (还可能包含受影响行的特殊JDBC定义的负值，例如{@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int[] batchUpdate(String sql, BatchPreparedStatementSetter pss) throws DataAccessException;

    /**
     * 使用提供的SQL语句和提供的参数批执行批处理。
     *
     * @param sql       要执行的SQL语句
     * @param batchArgs 包含查询参数批的对象数组列表
     * @return 一个数组，包含受批处理中每次更新影响的行数
     * (还可能包含受影响行的特殊JDBC定义的负值，例如{@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int[] batchUpdate(String sql, List<Object[]> batchArgs) throws DataAccessException;

    /**
     * 使用提供的SQL语句和提供的参数批执行批处理。
     *
     * @param sql       要执行的SQL语句。
     * @param batchArgs 包含查询参数批的对象数组列表
     * @param argTypes  参数的SQL类型（来自{@code java.sql.Types}的常量）
     * @return 一个数组，包含受批处理中每次更新影响的行数
     * (也可能包含受影响行的特殊JDBC定义的负值，例如{@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    int[] batchUpdate(String sql, List<Object[]> batchArgs, int[] argTypes) throws DataAccessException;

    /**
     * 使用提供的SQL语句和提供的参数集合执行多个批处理。
     * 参数值将使用ParameterizedPreparedStatementSetter设置。
     * 每个批次的大小应如“batchSize”所示。
     *
     * @param sql       要执行的SQL语句。
     * @param batchArgs 包含查询参数批的对象数组列表
     * @param batchSize 批量大小
     * @param pss       要使用的ParameterizedPreparedStatementSetter
     * @return 对于每个批，一个数组包含另一个数组，该数组包含受批中每个更新影响的行数
     * (也可能包含受影响行的特殊JDBC定义的负值，例如{@link java.sql.Statement#SUCCESS_NO_INFO}/{@link java.sql.Statement#EXECUTE_FAILED})
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    <T> int[][] batchUpdate(String sql,
                            Collection<T> batchArgs,
                            int batchSize,
                            ParameterizedPreparedStatementSetter<T> pss) throws DataAccessException;

    //-------------------------------------------------------------------------
    // Methods dealing with callable statements
    //-------------------------------------------------------------------------

    /**
     * 执行JDBC数据访问操作，作为处理JDBC CallableStatement的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param csc    在给定连接的情况下创建CallableStatement的回调
     * @param action 指定操作的回调
     * @return 操作返回的结果对象，如果没有，则为null
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(CallableStatementCreator csc, CallableStatementCallback<T> action) throws DataAccessException;

    /**
     * 执行JDBC数据访问操作，作为处理JDBC CallableStatement的回调操作实现。
     * 这允许在托管JDBC环境中在单个语句上实现任意数据访问操作：
     * 即，参与托管事务并将JDBC SQLException转换为DataAccessException层次结构。
     * <p>回调操作可以返回结果对象，例如域对象或域对象的集合。
     *
     * @param callString 要执行的SQL调用字符串
     * @param action     指定操作的回调
     * @return 操作返回的结果对象，如果没有，则为null
     * @throws DataAccessException 如果有任何问题
     */
    <T> T execute(String callString, CallableStatementCallback<T> action) throws DataAccessException;

    /**
     * 使用CallableStatementCreator执行SQL调用，以提供SQL和任何必需的参数。
     *
     * @param csc                提供SQL和任何必要参数的回调
     * @param declaredParameters 声明的SqlParameter对象列表
     * @return 提取参数的Map
     * @throws DataAccessException 如果发布更新时出现任何问题
     */
    Map<String, Object> call(CallableStatementCreator csc, List<SqlParameter> declaredParameters) throws DataAccessException;
}
