//package org.clever.data.jdbc.support;
//
//import org.clever.dao.DataAccessException;
//import org.clever.jdbc.InvalidResultSetAccessException;
//import org.clever.jdbc.core.*;
//import org.clever.jdbc.datasource.DataSourceUtils;
//import org.clever.jdbc.support.JdbcUtils;
//import org.clever.jdbc.support.KeyHolder;
//import org.clever.util.Assert;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import javax.sql.DataSource;
//import java.sql.*;
//import java.util.List;
//import java.util.Map;
//import java.util.Spliterator;
//import java.util.function.Consumer;
//import java.util.stream.Stream;
//import java.util.stream.StreamSupport;
//
///**
// * TODO 是否可以删除
// * 自定义JDBC JdbcTemplate<br/>
// * 1.打印 SQLWarning<br/>
// * 2.
// * <p>
// * 作者：lizw <br/>
// * 创建时间：2022/01/30 23:58 <br/>
// */
//public class JdbcTemplate extends org.clever.jdbc.core.JdbcTemplate {
//    private final Logger logger = LoggerFactory.getLogger("JdbcTemplate");
//
//    public JdbcTemplate(DataSource dataSource) {
//        super(dataSource);
//    }
//
//    @Override
//    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
//        return execute(psc, action, true);
//    }
//
//    @Override
//    public <T> T execute(String sql, PreparedStatementCallback<T> action) throws DataAccessException {
//        return execute(new SimplePreparedStatementCreator(sql), action, true);
//    }
//
//    @Override
//    public <T> T query(PreparedStatementCreator psc, final PreparedStatementSetter pss, final ResultSetExtractor<T> rse) throws DataAccessException {
//        Assert.notNull(rse, "ResultSetExtractor must not be null");
//        logger.debug("Executing prepared SQL query");
//        return execute(psc, new PreparedStatementCallback<T>() {
//            @Override
//            public T doInPreparedStatement(PreparedStatement ps) throws SQLException {
//                ResultSet rs = null;
//                try {
//                    if (pss != null) {
//                        pss.setValues(ps);
//                    }
//                    rs = ps.executeQuery();
//                    return rse.extractData(rs);
//                } finally {
//                    JdbcUtils.closeResultSet(rs);
//                    if (pss instanceof ParameterDisposer) {
//                        ((ParameterDisposer) pss).cleanupParameters();
//                    }
//                }
//            }
//        }, true);
//    }
//
//    @Override
//    public <T> Stream<T> queryForStream(PreparedStatementCreator psc, PreparedStatementSetter pss, RowMapper<T> rowMapper) throws DataAccessException {
//        return result(execute(psc, ps -> {
//            if (pss != null) {
//                pss.setValues(ps);
//            }
//            ResultSet rs = ps.executeQuery();
//            Connection con = ps.getConnection();
//            return new ResultSetSpliterator<>(rs, rowMapper).stream().onClose(() -> {
//                JdbcUtils.closeResultSet(rs);
//                if (pss instanceof ParameterDisposer) {
//                    ((ParameterDisposer) pss).cleanupParameters();
//                }
//                JdbcUtils.closeStatement(ps);
//                DataSourceUtils.releaseConnection(con, getDataSource());
//            });
//        }, false));
//    }
//
//    @Override
//    protected int update(final PreparedStatementCreator psc, final PreparedStatementSetter pss) throws DataAccessException {
//        logger.debug("Executing prepared SQL update");
//        return updateCount(execute(psc, ps -> {
//            try {
//                if (pss != null) {
//                    pss.setValues(ps);
//                }
//                int rows = ps.executeUpdate();
//                if (logger.isTraceEnabled()) {
//                    logger.trace("SQL update affected " + rows + " rows");
//                }
//                return rows;
//            } finally {
//                if (pss instanceof ParameterDisposer) {
//                    ((ParameterDisposer) pss).cleanupParameters();
//                }
//            }
//        }, true));
//    }
//
//    @Override
//    public int update(final PreparedStatementCreator psc, final KeyHolder generatedKeyHolder) throws DataAccessException {
//        Assert.notNull(generatedKeyHolder, "KeyHolder must not be null");
//        logger.debug("Executing SQL update and returning generated keys");
//        return updateCount(execute(psc, ps -> {
//            int rows = ps.executeUpdate();
//            List<Map<String, Object>> generatedKeys = generatedKeyHolder.getKeyList();
//            generatedKeys.clear();
//            ResultSet keys = ps.getGeneratedKeys();
//            if (keys != null) {
//                try {
//                    RowMapperResultSetExtractor<Map<String, Object>> rse = new RowMapperResultSetExtractor<>(getColumnMapRowMapper(), 1);
//                    generatedKeys.addAll(result(rse.extractData(keys)));
//                } finally {
//                    JdbcUtils.closeResultSet(keys);
//                }
//            }
//            if (logger.isTraceEnabled()) {
//                logger.trace("SQL update affected " + rows + " rows and returned " + generatedKeys.size() + " keys");
//            }
//            return rows;
//        }, true));
//    }
//
//
//    private <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action, boolean closeResources) throws DataAccessException {
//        Assert.notNull(psc, "PreparedStatementCreator must not be null");
//        Assert.notNull(action, "Callback object must not be null");
//        if (logger.isDebugEnabled()) {
//            String sql = getSql(psc);
//            logger.debug("Executing prepared SQL statement" + (sql != null ? " [" + sql + "]" : ""));
//        }
//        Connection con = DataSourceUtils.getConnection(obtainDataSource());
//        PreparedStatement ps = null;
//        try {
//            ps = psc.createPreparedStatement(con);
//            applyStatementSettings(ps);
//            T result = action.doInPreparedStatement(ps);
//            handleWarnings(ps);
//            return result;
//        } catch (SQLException ex) {
//            // Release Connection early, to avoid potential connection pool deadlock
//            // in the case when the exception translator hasn't been initialized yet.
//            if (ps != null) {
//                try {
//                    handleWarnings(ps);
//                } catch (Exception ignored) {
//                }
//            }
//            if (psc instanceof ParameterDisposer) {
//                ((ParameterDisposer) psc).cleanupParameters();
//            }
//            String sql = getSql(psc);
//            psc = null;
//            org.springframework.jdbc.support.JdbcUtils.closeStatement(ps);
//            ps = null;
//            DataSourceUtils.releaseConnection(con, getDataSource());
//            con = null;
//            throw translateException("PreparedStatementCallback", sql, ex);
//        } finally {
//            if (closeResources) {
//                if (psc instanceof ParameterDisposer) {
//                    ((ParameterDisposer) psc).cleanupParameters();
//                }
//                org.springframework.jdbc.support.JdbcUtils.closeStatement(ps);
//                DataSourceUtils.releaseConnection(con, getDataSource());
//            }
//        }
//    }
//
//    @Override
//    protected void handleWarnings(Statement stmt) throws SQLException {
//        if (isIgnoreWarnings()) {
//            if (logger.isInfoEnabled()) {
//                SQLWarning warningToLog = stmt.getWarnings();
//                while (warningToLog != null) {
//                    // logger.info("SQLWarning -> {} | state=[{}] errorCode=[{}] ", warningToLog.getMessage(),
//                    // warningToLog.getSQLState(), warningToLog.getErrorCode());
//                    logger.info("SQLWarning -> {}", warningToLog.getMessage());
//                    warningToLog = warningToLog.getNextWarning();
//                }
//            }
//        } else {
//            handleWarnings(stmt.getWarnings());
//        }
//    }
//
////    @Override
////    protected DataAccessException translateException(String task, String sql, SQLException ex) {
////        DataAccessException dae = getExceptionTranslator().translate(task, sql, ex);
////        return (dae != null ? dae : new UncategorizedSQLException(task, sql, ex));
////    }
//
//    //-------------------------------------------------------------------------
//    // private methods
//    //-------------------------------------------------------------------------
//
//    private static <T> T result(T result) {
//        Assert.state(result != null, "No result");
//        return result;
//    }
//
//    private static int updateCount(Integer result) {
//        Assert.state(result != null, "No update count");
//        return result;
//    }
//
//
//    private static String getSql(Object sqlProvider) {
//        if (sqlProvider instanceof SqlProvider) {
//            return ((SqlProvider) sqlProvider).getSql();
//        } else {
//            return null;
//        }
//    }
//
//    /**
//     * PreparedStatementCreator 的简单适配器，允许使用纯 SQL 语句
//     */
//    private static class SimplePreparedStatementCreator implements PreparedStatementCreator, SqlProvider {
//        private final String sql;
//
//        public SimplePreparedStatementCreator(String sql) {
//            Assert.notNull(sql, "SQL must not be null");
//            this.sql = sql;
//        }
//
//        @Override
//        public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
//            return con.prepareStatement(this.sql);
//        }
//
//        @Override
//        public String getSql() {
//            return this.sql;
//        }
//    }
//
//    /**
//     * 用于将 ResultSet 适配为 Stream 的 queryForStream 的适配器
//     */
//    private static class ResultSetSpliterator<T> implements Spliterator<T> {
//        private final ResultSet rs;
//        private final RowMapper<T> rowMapper;
//        private int rowNum = 0;
//
//        public ResultSetSpliterator(ResultSet rs, RowMapper<T> rowMapper) {
//            this.rs = rs;
//            this.rowMapper = rowMapper;
//        }
//
//        @Override
//        public boolean tryAdvance(Consumer<? super T> action) {
//            try {
//                if (this.rs.next()) {
//                    action.accept(this.rowMapper.mapRow(this.rs, this.rowNum++));
//                    return true;
//                }
//                return false;
//            } catch (SQLException ex) {
//                throw new InvalidResultSetAccessException(ex);
//            }
//        }
//
//        @Override
//        public Spliterator<T> trySplit() {
//            return null;
//        }
//
//        @Override
//        public long estimateSize() {
//            return Long.MAX_VALUE;
//        }
//
//        @Override
//        public int characteristics() {
//            return Spliterator.ORDERED;
//        }
//
//        public Stream<T> stream() {
//            return StreamSupport.stream(this, false);
//        }
//    }
//}
