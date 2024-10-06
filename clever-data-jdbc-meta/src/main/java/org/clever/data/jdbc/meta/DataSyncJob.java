package org.clever.data.jdbc.meta;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppShutdownHook;
import org.clever.core.Assert;
import org.clever.core.OrderIncrement;
import org.clever.core.SystemClock;
import org.clever.core.exception.BusinessException;
import org.clever.core.exception.ExceptionUtils;
import org.clever.core.function.ZeroConsumer;
import org.clever.core.job.DaemonExecutor;
import org.clever.core.thread.SharedThreadPoolExecutor;
import org.clever.core.tuples.TupleTwo;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.*;
import org.clever.data.jdbc.meta.utils.MetaDataUtils;
import org.springframework.transaction.support.TransactionCallback;

import java.util.*;
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

    static {
        DaemonExecutor daemonExecutor = new DaemonExecutor("data-sync-clear");
        daemonExecutor.scheduleAtFixedRate(DataSyncJob::clearExpireState, 1000 * 60 * 5);
        AppShutdownHook.addShutdownHook(() -> {
            daemonExecutor.shutdown();
            TABLE_SYNC_MAP.keySet().forEach(DataSyncJob::interruptTableSync);
            QUERY_SYNC_MAP.keySet().forEach(DataSyncJob::interruptQuerySync);
        }, OrderIncrement.MIN, "停止DataSyncJob");
    }

    /**
     * 清除过期的任务状态数据
     */
    public static void clearExpireState() {
        final long ttl = 1000 * 60 * 60 * 24;
        final long now = SystemClock.now();
        final Set<String> removeIds = new HashSet<>();
        TABLE_SYNC_MAP.forEach((id, state) -> {
            Long startTime = state.getStartTime();
            if (startTime != null && (now - startTime) > ttl) {
                removeIds.add(id);
            }
        });
        removeIds.forEach(TABLE_SYNC_MAP::remove);
        removeIds.clear();
        QUERY_SYNC_MAP.forEach((id, state) -> {
            Long startTime = state.getStartTime();
            if (startTime != null && (now - startTime) > ttl) {
                removeIds.add(id);
            }
        });
        removeIds.forEach(QUERY_SYNC_MAP::remove);
        removeIds.clear();
    }

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

    /**
     * 数据库表结构同步
     *
     * @param source           源数据库
     * @param target           目标数据库
     * @param sourceSchemaName 源数据库 schemaName
     * @param targetSchemaName 目标数据库 schemaName
     * @param sequence         是否同步序列
     * @param tableNames       需要同步的表
     * @return 需要执行的 ddl 语句
     */
    public static String structSync(Jdbc source, Jdbc target, String sourceSchemaName, String targetSchemaName, boolean sequence, String... tableNames) {
        Assert.notNull(source, "参数 source 不能为null");
        Assert.notNull(target, "参数 target 不能为null");
        Assert.isNotBlank(sourceSchemaName, "参数 sourceSchemaName 不能为空");
        Assert.isNotBlank(targetSchemaName, "参数 targetSchemaName 不能为空");
        Assert.notEmpty(tableNames, "参数 tableNames 不能为空");
        StringBuilder ddl = new StringBuilder();
        ZeroConsumer addLine = () -> {
            if (ddl.length() > 0) {
                ddl.append(DataBaseMetaData.LINE);
            }
        };
        Set<String> tabs = Arrays.stream(tableNames).collect(Collectors.toSet());
        DataBaseMetaData sourceMeta = MetaDataUtils.createMetaData(source);
        DataBaseMetaData targetMeta = MetaDataUtils.createMetaData(target);
        Schema sourceSchema = sourceMeta.getSchema(sourceSchemaName, tabs);
        Schema targetSchema = targetMeta.getSchema(targetSchemaName, tabs);
        Assert.notNull(sourceSchema, String.format("源数据库不存在 schema=%s", sourceSchemaName));
        Assert.notNull(targetSchema, String.format("目标数据库不存在 schema=%s", targetSchemaName));
        List<Table> addTabs = new ArrayList<>();
        List<Table> delTabs = new ArrayList<>();
        List<TupleTwo<Table, Table>> alterTabs = new ArrayList<>();
        for (Table sourceTable : sourceSchema.getTables()) {
            Table targetTable = targetSchema.getTable(sourceTable.getName());
            if (targetTable == null) {
                addTabs.add(sourceTable);
            } else {
                alterTabs.add(TupleTwo.creat(sourceTable, targetTable));
            }
        }
        for (Table targetTable : targetSchema.getTables()) {
            Table sourceTable = sourceSchema.getTable(targetTable.getName());
            if (sourceTable == null) {
                delTabs.add(targetTable);
            }
        }
        // 新增表
        for (Table table : addTabs) {
            String str = targetMeta.createTable(table);
            ddl.append(str);
            if (StringUtils.isNotBlank(str)) {
                addLine.call();
            }
        }
        if (!addTabs.isEmpty()) {
            addLine.call();
        }
        // 修改表
        for (TupleTwo<Table, Table> alterTab : alterTabs) {
            String str = targetMeta.alterTable(alterTab.getValue1(), alterTab.getValue2());
            ddl.append(str);
            if (StringUtils.isNotBlank(str)) {
                addLine.call();
            }
        }
        if (!alterTabs.isEmpty()) {
            addLine.call();
        }
        // 删除表
        for (Table table : delTabs) {
            String str = targetMeta.dropTable(table);
            ddl.append(str);
            if (StringUtils.isNotBlank(str)) {
                addLine.call();
            }
        }
        if (!delTabs.isEmpty()) {
            addLine.call();
        }
        // 处理序列
        if (sequence) {
            List<Sequence> addSequences = new ArrayList<>();
            List<Sequence> delSequences = new ArrayList<>();
            List<TupleTwo<Sequence, Sequence>> alterSequences = new ArrayList<>();
            for (Sequence sourceSequence : sourceSchema.getSequences()) {
                Sequence targetSequence = targetSchema.getSequence(sourceSequence.getName());
                if (targetSequence == null) {
                    addSequences.add(sourceSequence);
                } else {
                    alterSequences.add(TupleTwo.creat(sourceSequence, targetSequence));
                }
            }
            for (Sequence targetSequence : targetSchema.getSequences()) {
                Sequence sourceSequence = sourceSchema.getSequence(targetSequence.getName());
                if (sourceSequence == null) {
                    delSequences.add(targetSequence);
                }
            }
            // 新增序列
            for (Sequence seq : addSequences) {
                String str = targetMeta.createSequence(seq);
                ddl.append(str);
                if (StringUtils.isNotBlank(str)) {
                    addLine.call();
                }
            }
            if (!addSequences.isEmpty()) {
                addLine.call();
            }
            // 修改序列
            for (TupleTwo<Sequence, Sequence> alterSequence : alterSequences) {
                String str = targetMeta.alterSequence(alterSequence.getValue1(), alterSequence.getValue2());
                ddl.append(str);
                if (StringUtils.isNotBlank(str)) {
                    addLine.call();
                }
            }
            if (!alterSequences.isEmpty()) {
                addLine.call();
            }
            // 删除序列
            for (Sequence seq : delSequences) {
                String str = targetMeta.dropSequence(seq);
                ddl.append(str);
                if (StringUtils.isNotBlank(str)) {
                    addLine.call();
                }
            }
            if (!delSequences.isEmpty()) {
                addLine.call();
            }
        }
        return ddl.toString();
    }

    /**
     * 数据库表结构同步
     *
     * @param source           源数据库
     * @param target           目标数据库
     * @param sourceSchemaName 源数据库 schemaName
     * @param targetSchemaName 目标数据库 schemaName
     * @param sequence         是否同步序列
     * @param tableNames       需要同步的表
     * @return 需要执行的 ddl 语句
     */
    public static String structSync(Jdbc source, Jdbc target, String sourceSchemaName, String targetSchemaName, boolean sequence, Collection<String> tableNames) {
        return structSync(source, target, sourceSchemaName, targetSchemaName, sequence, tableNames.toArray(new String[0]));
    }

    /**
     * 获取数据库存储过程创建语句(包含: 存储过程、函数)
     *
     * @param source           源数据库
     * @param sourceSchemaName 源数据库 schemaName
     * @param procedureNames   存储过程名称(为空表示获取所有的存储过程)
     * @return 需要执行的 ddl 语句
     */
    public static String procedureDLL(Jdbc source, String sourceSchemaName, String... procedureNames) {
        Assert.notNull(source, "参数 source 不能为null");
        Assert.isNotBlank(sourceSchemaName, "参数 sourceSchemaName 不能为空");
        StringBuilder ddl = new StringBuilder();
        ZeroConsumer addLine = () -> {
            if (ddl.length() > 0) {
                ddl.append(DataBaseMetaData.LINE);
            }
        };
        DataBaseMetaData sourceMeta = MetaDataUtils.createMetaData(source);
        Schema schema = sourceMeta.getSchema(sourceSchemaName, Collections.singletonList("000"));
        if (procedureNames == null || procedureNames.length == 0) {
            for (Procedure procedure : schema.getProcedures()) {
                ddl.append(sourceMeta.createProcedure(procedure));
                addLine.call();
            }
        } else {
            for (String name : Arrays.stream(procedureNames).collect(Collectors.toSet())) {
                Procedure procedure = schema.getProcedure(name);
                if (procedure != null) {
                    ddl.append(sourceMeta.createProcedure(procedure));
                    addLine.call();
                }
            }
        }
        return ddl.toString();
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
