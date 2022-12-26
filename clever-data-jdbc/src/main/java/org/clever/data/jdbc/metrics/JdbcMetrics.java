package org.clever.data.jdbc.metrics;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.clever.core.Conv;
import org.clever.core.SystemClock;
import org.clever.core.mapper.BeanCopyUtils;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.util.Assert;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/03/14 20:01 <br/>
 */
@Data
@Slf4j
public class JdbcMetrics {
    private final ArrayBlockingQueue<SqlExecEvent> sqlExecEventQueue = new ArrayBlockingQueue<>(4096);
    private final Thread executor;
    private final JdbcConfig.JdbcMetrics config;
    /**
     * 指标数据
     * <pre>{@code
     * ConcurrentMap<dataSourceName, ConcurrentMap<sql, SqlMetric>>
     * }</pre>
     */
    private final ConcurrentMap<String, ConcurrentMap<String, SqlMetric>> metrics = new ConcurrentHashMap<>();

    public JdbcMetrics(JdbcConfig.JdbcMetrics config) {
        Assert.notNull(config, "参数 config 不能为 null");
        this.config = config;
        this.executor = new Thread(this::statistics, "jdbc-metrics");
        this.executor.setDaemon(true);
        this.executor.start();
    }

    public List<Map<String, Object>> getAllMetrics() {
        List<Map<String, Object>> res = new ArrayList<>(config.getMaxSqlCount());
        metrics.forEach((dataSourceName, metricsItem) -> metricsItem.forEach((sql, sqlMetric) -> {
            Map<String, Object> map = BeanCopyUtils.toMap(sqlMetric);
            map.put("dataSourceName", dataSourceName);
            res.add(map);
        }));
        return res;
    }

    @SneakyThrows
    public void addSqlExecEvent(SqlExecEvent sqlExecEvent) {
        // 添加数据(在规定时间内重试，超过规定时间返回false)
        sqlExecEventQueue.offer(sqlExecEvent, 1, TimeUnit.SECONDS);
    }

    /**
     * 统计jdbc性能
     */
    private void statistics() {
        while (true) {
            try {
                // 取出数据(线程阻塞，指定中断或被唤醒)
                SqlExecEvent sqlExecEvent = sqlExecEventQueue.take();
                addMetrics(sqlExecEvent);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    /**
     * 计算新的SqlExecEvent
     */
    private void addMetrics(SqlExecEvent sqlExecEvent) {
        final long now = SystemClock.now();
        final String dataSourceName = sqlExecEvent.getDataSourceName();
        final String prepared = sqlExecEvent.getPrepared();
        final String sql = sqlExecEvent.getSql();
        final int cost = Conv.asInteger(sqlExecEvent.getCost());
        ConcurrentMap<String, SqlMetric> metricsItem = metrics.computeIfAbsent(dataSourceName, key -> new ConcurrentHashMap<>());
        SqlMetric sqlMetric = metricsItem.computeIfAbsent(prepared, this::createSqlMetric);
        sqlMetric.count.incrementAndGet();
        sqlMetric.sumCost.addAndGet(cost);
        if (sqlMetric.maxCost < cost) {
            sqlMetric.maxCost = cost;
        }
        if (sqlMetric.minCost > cost) {
            sqlMetric.minCost = cost;
        }
        SqlHistogramInfo histogramInfo = null;
        for (SqlHistogramInfo item : sqlMetric.histogramInfos) {
            if (cost >= item.gteCost && cost < item.ltCost) {
                histogramInfo = item;
                break;
            }
        }
        if (histogramInfo != null) {
            histogramInfo.count.incrementAndGet();
            synchronized (histogramInfo.topN) {
                List<SqlInfo> topList = new ArrayList<>(histogramInfo.topN.size() + 1);
                topList.add(new SqlInfo(sql, cost, now));
                topList.sort(Comparator.comparingInt(o -> o.cost));
                histogramInfo.topN.clear();
                if (topList.size() > config.getHistogramTopN()) {
                    histogramInfo.topN.addAll(topList.subList(0, config.getHistogramTopN()));
                } else {
                    histogramInfo.topN.addAll(topList);
                }
            }
        }
        // 控制 metricsItem 的大小
        if (metricsItem.size() > config.getMaxSqlCount()) {
            // TODO 根据执行平均时间，删除执行平均时间最小的数据
        }
    }

    /**
     * 创建一个新的 SqlMetric
     */
    private SqlMetric createSqlMetric(String prepared) {
        List<Integer> histogram = config.getHistogram();
        Assert.notNull(histogram, "histogram 配置不能为null");
        histogram.sort(Integer::compareTo);
        Assert.notEmpty(histogram, "histogram 配置不能为空");
        Assert.isTrue(histogram.get(0) > 0, "histogram 配置项不能 <= 0");
        SqlMetric sqlMetric = new SqlMetric(prepared);
        int gteCost = 0;
        for (Integer ltCost : histogram) {
            sqlMetric.getHistogramInfos().add(new SqlHistogramInfo(prepared, gteCost, ltCost));
            gteCost = ltCost;
        }
        sqlMetric.getHistogramInfos().add(new SqlHistogramInfo(prepared, gteCost, Integer.MAX_VALUE));
        return sqlMetric;
    }

    @Data
    public static class SqlMetric {
        /**
         * 预编译的sql语句
         */
        private final String sql;
        /**
         * 执行次数
         */
        private AtomicLong count = new AtomicLong(0);
        /**
         * 执行总时间(毫秒)
         */
        private AtomicLong sumCost = new AtomicLong();
        /**
         * 最大执行时间(毫秒)
         */
        private volatile int maxCost = Integer.MIN_VALUE;
        /**
         * 最小执行时间(毫秒)
         */
        private volatile int minCost = Integer.MAX_VALUE;
        /**
         * 直方图指标数据
         */
        private final CopyOnWriteArrayList<SqlHistogramInfo> histogramInfos = new CopyOnWriteArrayList<>();

        public SqlMetric(String sql) {
            this.sql = sql;
        }

        /**
         * 平均执行时间
         */
        public long getAvgCost() {
            return sumCost.get() / count.get();
        }
    }

    @Data
    public static class SqlHistogramInfo {
        /**
         * 预编译的sql语句
         */
        private final String sql;
        /**
         * 当前直方图区间执行时间起始值(大于等于)
         */
        private final int gteCost;
        /**
         * 当前直方图区间执行时间起始值(小于)
         */
        private final int ltCost;
        /**
         * 执行次数
         */
        private AtomicLong count = new AtomicLong(0);
        /**
         * 最耗时的TopN信息
         */
        private final CopyOnWriteArrayList<SqlInfo> topN = new CopyOnWriteArrayList<>();

        public SqlHistogramInfo(String sql, int gteCost, int ltCost) {
            this.sql = sql;
            this.gteCost = gteCost;
            this.ltCost = ltCost;
        }
    }

    @Data
    public static class SqlInfo {
        /**
         * 可执行的sql语句
         */
        private final String sql;
        /**
         * 执行耗时
         */
        private final int cost;
        /**
         * 执行sql的发生时间
         */
        private final long occurrenceTime;

        public SqlInfo(String sql, int cost, long occurrenceTime) {
            this.sql = sql;
            this.cost = cost;
            this.occurrenceTime = occurrenceTime;
        }
    }
}
