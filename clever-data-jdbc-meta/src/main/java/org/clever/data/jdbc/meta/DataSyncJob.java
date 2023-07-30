package org.clever.data.jdbc.meta;

import lombok.extern.slf4j.Slf4j;
import org.clever.core.SystemClock;
import org.clever.core.exception.BusinessException;
import org.clever.core.exception.ExceptionUtils;
import org.clever.data.jdbc.Jdbc;
import org.clever.data.jdbc.meta.model.DataSyncInfo;
import org.clever.data.jdbc.meta.model.TableSyncInfo;

import java.util.List;

/**
 * 数据同步任务
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/07/30 22:13 <br/>
 */
@Slf4j
public class DataSyncJob implements Runnable {
    private final DataSyncInfo dataSyncInfo;
    private final Jdbc source;
    private final Jdbc target;

    public DataSyncJob(DataSyncInfo dataSyncInfo, Jdbc source, Jdbc target) {
        this.dataSyncInfo = dataSyncInfo;
        this.source = source;
        this.target = target;
    }

    @Override
    public void run() {
        try {
            doSync();
            dataSyncInfo.setSuccess(true);
        } catch (Exception e) {
            dataSyncInfo.setSuccess(false);
            log.error("数据同步失败", e);
        } finally {
            dataSyncInfo.setEndTime(SystemClock.now());
        }
    }

    private void doSync() {
        final boolean clearData = dataSyncInfo.isClearData();
        final boolean skipError = dataSyncInfo.isSkipError();
        List<TableSyncInfo> tableSyncInfos = dataSyncInfo.getTableSyncInfos();
        dataSyncInfo.setStartTime(SystemClock.now());
        // 查询同步数据量
        for (TableSyncInfo table : tableSyncInfos) {
            if (dataSyncInfo.isInterrupt()) return;
            try {
                table.setDataCount(source.queryInt(String.format("select count(1) from %s", table.getTableName())));
            } catch (Exception e) {
                if (!skipError) {
                    throw ExceptionUtils.unchecked(e);
                }
                log.error(e.getMessage(), e);
            }
        }
        // 开始同步数据
        for (TableSyncInfo table : tableSyncInfos) {
            if (dataSyncInfo.isInterrupt()) return;
            dataSyncInfo.setCurrentSync(table);
            table.setStartTime(SystemClock.now());
            source.beginReadOnlyTX(sourceStatus -> {
                // 同步之前清空表数据
                if (clearData) {
                    try {
                        table.setDeleteCount(target.update(String.format("delete from %s", table.getTableName())));
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
                    source.queryForCursor(String.format("select * from %s", table.getTableName()), 1000, batchData -> {
                        if (dataSyncInfo.isInterrupt()) {
                            throw new BusinessException("中断同步");
                        }
                        // 单线程
                        try {
                            table.setSyncCount(table.getSyncCount() + target.batchInsertTable(table.getTableName(), batchData.getRowDataList()));
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

//    private static final ConcurrentHashMap<String, DataSyncInfo> SYNC_INFO_MAP = new ConcurrentHashMap<>();
//
//   public static DataSyncInfo dataSync(Jdbc source, Jdbc target, List<String> tableNames, SyncConfig syncConfig) {
//        DataSyncInfo dataSyncInfo = new DataSyncInfo([
//                jobId         : IDCreateUtils.uuid(),
//                syncConfig    : syncConfig,
//                tableSyncInfos: tableNames.stream()
//                .map({
//                        new TableSyncInfo([tableName: it])
//                        })
//                        .collect(Collectors.toList()),
//        ])
//        SyncJob syncJob = new SyncJob(dataSyncInfo, source, target)
//        SYNC_INFO_MAP.put(dataSyncInfo.jobId, dataSyncInfo)
//        EXECUTOR.execute(syncJob)
//        return dataSyncInfo
//    }
//
//    public   static DataSyncInfo getDataSyncInfo(String jobId) {
//        return SYNC_INFO_MAP.get(jobId);
//    }
}
