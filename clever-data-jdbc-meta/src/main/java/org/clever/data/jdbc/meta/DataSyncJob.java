package org.clever.data.jdbc.meta;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.SharedThreadPoolExecutor;
import org.clever.core.SystemClock;
import org.clever.core.exception.BusinessException;
import org.clever.core.exception.ExceptionUtils;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.QuerySyncState;
import org.clever.data.jdbc.meta.model.TableState;
import org.clever.data.jdbc.meta.model.TablesSyncState;
import org.clever.transaction.support.TransactionCallback;
import org.clever.util.Assert;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 数据同步任务
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/30 22:13 <br/>
 */
public class DataSyncJob {
    private static final ConcurrentHashMap<String, TablesSyncState> TABLE_SYNC_MAP = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, QuerySyncState> QUERY_SYNC_MAP = new ConcurrentHashMap<>();

    /**
     * 获取数据同步状态信息(数据库表-->表的同步)
     *
     * @param jobId 同步任务ID
     */
    public static TablesSyncState getTableSyncState(String jobId) {
        return TABLE_SYNC_MAP.get(jobId);
    }

    /**
     * 中断同步数据任务(数据库表-->表的同步)
     *
     * @param jobId 同步任务ID
     */
    public static void interruptTableSync(String jobId) {
        TablesSyncState tablesSyncState = getTableSyncState(jobId);
        if (tablesSyncState != null) {
            tablesSyncState.setInterrupt(true);
        }
    }

    /**
     * 获取数据同步状态信息(数据库sql查询-->表的同步)
     *
     * @param jobId 同步任务ID
     */
    public static QuerySyncState getQuerySyncState(String jobId) {
        return QUERY_SYNC_MAP.get(jobId);
    }

    /**
     * 中断同步数据任务(数据库sql查询-->表的同步)
     *
     * @param jobId 同步任务ID
     */
    public static void interruptQuerySync(String jobId) {
        QuerySyncState querySyncState = getQuerySyncState(jobId);
        if (querySyncState != null) {
            querySyncState.setInterrupt(true);
        }
    }

    /**
     * 创建数据同步任务(数据库表-->表的同步)
     *
     * @param source     源数据库
     * @param target     目标数据库
     * @param clearData  同步前清除数据
     * @param skipError  同步时跳过所有错误
     * @param tableNames 需要同步的表
     */
    public static TablesSyncState tableSync(Jdbc source, Jdbc target, boolean clearData, boolean skipError, String... tableNames) {
        Assert.notNull(source, "参数 source 不能为null");
        Assert.notNull(target, "参数 target 不能为null");
        Assert.notEmpty(tableNames, "参数 tableNames 不能为空");
        TablesSyncState tablesSyncState = new TablesSyncState();
        tablesSyncState.setClearData(clearData);
        tablesSyncState.setSkipError(skipError);
        for (String tableName : Arrays.stream(tableNames).collect(Collectors.toSet())) {
            if (StringUtils.isBlank(tableName)) {
                continue;
            }
            TableState tableState = new TableState();
            tableState.setTableName(tableName);
            tablesSyncState.getTableSyncInfos().add(tableState);
        }
        Assert.notEmpty(tablesSyncState.getTableSyncInfos(), "参数 tableNames 不能为空");
        TableSyncJob tableSyncJob = new TableSyncJob(source, target, tablesSyncState);
        SharedThreadPoolExecutor.getSmall().execute(tableSyncJob);
        TABLE_SYNC_MAP.put(tablesSyncState.getJobId(), tablesSyncState);
        return tablesSyncState;
    }

    /**
     * 创建数据同步任务(数据库表-->表的同步)
     *
     * @param source     源数据库
     * @param target     目标数据库
     * @param clearData  同步前清除数据
     * @param skipError  同步时跳过所有错误
     * @param tableNames 需要同步的表
     */
    public static TablesSyncState tableSync(Jdbc source, Jdbc target, boolean clearData, boolean skipError, Collection<String> tableNames) {
        return tableSync(source, target, skipError, skipError, tableNames.toArray(new String[0]));
    }

    /**
     * 创建数据同步任务(数据库sql查询-->表的同步)
     *
     * @param source          源数据库
     * @param target          目标数据库
     * @param skipError       同步时跳过所有错误
     * @param querySql        查询sql
     * @param targetTableName 目标表
     */
    public static QuerySyncState querySync(Jdbc source, Jdbc target, boolean skipError, String querySql, String targetTableName) {
        Assert.notNull(source, "参数 source 不能为null");
        Assert.notNull(target, "参数 target 不能为null");
        Assert.isNotBlank(querySql, "参数 querySql 不能为空");
        Assert.isNotBlank(targetTableName, "参数 targetTableName 不能为空");
        QuerySyncState querySyncState = new QuerySyncState();
        querySyncState.setSkipError(skipError);
        querySyncState.setQuerySql(querySql);
        querySyncState.setTargetTableName(targetTableName);
        QuerySyncJob querySyncJob = new QuerySyncJob(source, target, querySyncState);
        SharedThreadPoolExecutor.getSmall().execute(querySyncJob);
        QUERY_SYNC_MAP.put(querySyncState.getJobId(), querySyncState);
        return querySyncState;
    }

    @Getter
    @Slf4j
    public static class QuerySyncJob implements Runnable {
        /**
         * 源数据库
         */
        protected final Jdbc source;
        /**
         * 目标数据库
         */
        protected final Jdbc target;
        /**
         * 数据同步状态
         */
        private final QuerySyncState querySyncState;

        public QuerySyncJob(Jdbc source, Jdbc target, QuerySyncState querySyncState) {
            this.source = source;
            this.target = target;
            this.querySyncState = querySyncState;
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        public void run() {
            try {
                doSync();
                querySyncState.setSuccess(true);
            } catch (Exception e) {
                querySyncState.setSuccess(false);
                querySyncState.setErrorMsg(ExceptionUtils.getStackTraceAsString(e));
                log.error("数据同步失败", e);
            } finally {
                querySyncState.setEndTime(SystemClock.now());
            }
        }

        private void doSync() {
            final boolean skipError = querySyncState.isSkipError();
            querySyncState.setStartTime(SystemClock.now());
            // 查询同步数据量
            if (querySyncState.isInterrupt()) {
                return;
            }
            try {
                int count = (int) source.queryCount(querySyncState.getQuerySql());
                querySyncState.setDataCount(count);
            } catch (Exception e) {
                if (!skipError) {
                    throw ExceptionUtils.unchecked(e);
                }
                log.error(e.getMessage(), e);
            }
            // 开始同步数据
            if (querySyncState.isInterrupt()) {
                return;
            }
            source.beginReadOnlyTX(sourceStatus -> {
                // 开始同步数据
                try {
                    querySyncState.setCurrentIdx(0);
                    querySyncState.setSyncCount(0);
                    source.queryForCursor(querySyncState.getQuerySql(), 1000, batchData -> {
                        if (querySyncState.isInterrupt()) {
                            throw new BusinessException("中断同步");
                        }
                        // 单线程
                        try {
                            int count = target.beginTX((TransactionCallback<Integer>) status -> target.batchInsertTable(querySyncState.getTargetTableName(), batchData.getRowDataList()));
                            querySyncState.setSyncCount(querySyncState.getSyncCount() + count);
                            querySyncState.setCurrentIdx(batchData.getRowCount());
                        } catch (Exception e) {
                            if (!skipError) {
                                throw ExceptionUtils.unchecked(e);
                            }
                            log.error(e.getMessage(), e);
                        }
                    });
                } catch (Exception e) {
                    if (!skipError) {
                        throw ExceptionUtils.unchecked(e);
                    }
                    log.error(e.getMessage(), e);
                }
            });
        }
    }

    @Getter
    @Slf4j
    public static class TableSyncJob implements Runnable {
        /**
         * 源数据库
         */
        private final Jdbc source;
        /**
         * 目标数据库
         */
        private final Jdbc target;
        /**
         * 数据同步状态
         */
        private final TablesSyncState tablesSyncState;

        public TableSyncJob(Jdbc source, Jdbc target, TablesSyncState tablesSyncState) {
            this.source = source;
            this.target = target;
            this.tablesSyncState = tablesSyncState;
        }

        @SuppressWarnings("DuplicatedCode")
        @Override
        public void run() {
            try {
                doSync();
                tablesSyncState.setSuccess(true);
            } catch (Exception e) {
                tablesSyncState.setSuccess(false);
                tablesSyncState.setErrorMsg(ExceptionUtils.getStackTraceAsString(e));
                log.error("数据同步失败", e);
            } finally {
                tablesSyncState.setEndTime(SystemClock.now());
            }
        }

        private void doSync() {
            final boolean clearData = tablesSyncState.isClearData();
            final boolean skipError = tablesSyncState.isSkipError();
            List<TableState> tableSyncInfos = tablesSyncState.getTableSyncInfos();
            tablesSyncState.setStartTime(SystemClock.now());
            // 查询同步数据量
            for (TableState table : tableSyncInfos) {
                if (tablesSyncState.isInterrupt()) return;
                try {
                    String queryCountSql = String.format("select count(1) from %s", table.getTableName());
                    table.setDataCount(source.queryInt(queryCountSql));
                } catch (Exception e) {
                    if (!skipError) {
                        throw ExceptionUtils.unchecked(e);
                    }
                    log.error(e.getMessage(), e);
                }
            }
            // 开始同步数据
            for (TableState table : tableSyncInfos) {
                if (tablesSyncState.isInterrupt()) return;
                tablesSyncState.setCurrentSync(table);
                table.setStartTime(SystemClock.now());
                source.beginReadOnlyTX(sourceStatus -> {
                    // 同步之前清空表数据
                    if (clearData) {
                        try {
                            String deleteSql = String.format("delete from %s", table.getTableName());
                            int count = target.beginTX((TransactionCallback<Integer>) status -> target.update(deleteSql));
                            table.setDeleteCount(count);
                        } catch (Exception e) {
                            if (!skipError) {
                                throw ExceptionUtils.unchecked(e);
                            }
                            log.error(e.getMessage(), e);
                        }
                    }
                    // 开始同步数据
                    try {
                        table.setCurrentIdx(0);
                        table.setSyncCount(0);
                        String querySql = String.format("select * from %s", table.getTableName());
                        source.queryForCursor(querySql, 1000, batchData -> {
                            if (tablesSyncState.isInterrupt()) {
                                throw new BusinessException("中断同步");
                            }
                            // 单线程
                            try {
                                int count = target.beginTX((TransactionCallback<Integer>) status -> target.batchInsertTable(table.getTableName(), batchData.getRowDataList()));
                                table.setSyncCount(table.getSyncCount() + count);
                                table.setCurrentIdx(batchData.getRowCount());
                            } catch (Exception e) {
                                if (!skipError) {
                                    throw ExceptionUtils.unchecked(e);
                                }
                                log.error(e.getMessage(), e);
                            }
                        });
                    } catch (Exception e) {
                        if (!skipError) {
                            throw ExceptionUtils.unchecked(e);
                        }
                        log.error(e.getMessage(), e);
                    }
                });
                table.setEndTime(SystemClock.now());
            }
        }
    }
}
