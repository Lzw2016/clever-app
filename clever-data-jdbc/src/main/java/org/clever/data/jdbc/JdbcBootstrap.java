package org.clever.data.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.AppContextHolder;
import org.clever.core.AppShutdownHook;
import org.clever.core.SystemClock;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.data.jdbc.config.MybatisConfig;
import org.clever.data.jdbc.metrics.P6SpyMeter;
import org.clever.data.jdbc.metrics.Slf4JLogger;
import org.clever.data.jdbc.mybatis.ClassPathMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.ComposeMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;
import org.clever.data.jdbc.support.MergeDataSourceConfig;
import org.clever.jdbc.datasource.DataSourceTransactionManager;
import org.clever.util.Assert;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.*;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 22:45 <br/>
 */
@Slf4j
public class JdbcBootstrap {
    private volatile boolean initialized = false;
    @Getter
    private final JdbcConfig jdbcConfig;
    @Getter
    private final MybatisConfig mybatisConfig;

    public JdbcBootstrap(JdbcConfig jdbcConfig, MybatisConfig mybatisConfig) {
        Assert.notNull(jdbcConfig, "参数 jdbcConfig 不能为 null");
        Assert.notNull(mybatisConfig, "参数 mybatisConfig 不能为 null");
        this.jdbcConfig = jdbcConfig;
        this.mybatisConfig = mybatisConfig;
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        initP6Spy();
        initMybatis();
        initJdbc();
    }

    private void initP6Spy() {
        final JdbcConfig.JdbcMetrics metrics = Optional.of(jdbcConfig.getMetrics()).orElse(new JdbcConfig.JdbcMetrics());
        P6SpyMeter.init(metrics);
        Slf4JLogger.init(metrics);
    }

    private void initMybatis() {
        if (!mybatisConfig.isEnable()) {
            return;
        }
        // TODO 打印配置日志
        final Duration interval = Optional.of(mybatisConfig.getInterval()).orElse(Duration.ZERO);
        final List<MybatisConfig.MapperLocation> locations = Optional.of(mybatisConfig.getLocations()).orElse(Collections.emptyList());
        List<MyBatisMapperSql> mybatisMapperSqlList = new ArrayList<>(locations.size());
        for (MybatisConfig.MapperLocation location : locations) {
            MyBatisMapperSql mybatisMapperSql;
            if (MybatisConfig.FileType.FileSystem.equals(location.getFileType())) {
                // TODO rootPath
                mybatisMapperSql = new FileSystemMyBatisMapperSql(
                        location.getLocation(),
                        location.getFilter()
                );
            } else if (MybatisConfig.FileType.Jar.equals(location.getFileType())) {
                mybatisMapperSql = new ClassPathMyBatisMapperSql(
                        location.getLocation(),
                        location.getFilter()
                );
            } else {
                throw new RuntimeException("配置 mybatis.locations.fileType 值无效");
            }
            mybatisMapperSqlList.add(mybatisMapperSql);
        }
        ComposeMyBatisMapperSql composeMyBatisMapperSql = new ComposeMyBatisMapperSql(mybatisMapperSqlList);
        composeMyBatisMapperSql.reloadAll();
        if (mybatisConfig.isWatcher() && !interval.isZero()) {
            composeMyBatisMapperSql.startWatch(interval.toMillis());
            AppShutdownHook.addShutdownHook(composeMyBatisMapperSql::stopWatch, 100, "停止监听mapper(sql.xml)文件");
        }
        DataSourceAdmin.setMyBatisMapperSql(composeMyBatisMapperSql);
        AppContextHolder.registerBean("mybatisMapperSql", composeMyBatisMapperSql, true);
    }

    private void initJdbc() {
        if (!jdbcConfig.isEnable()) {
            return;
        }
        // TODO 打印配置日志
        final HikariConfig global = Optional.of(jdbcConfig.getGlobal()).orElse(new HikariConfig());
        final Map<String, HikariConfig> dataSource = Optional.of(jdbcConfig.getDataSource()).orElse(Collections.emptyMap());
        final Map<String, DataSource> dataSourceMap = new HashMap<>(dataSource.size());
        // 初始化配置的数据源
        final long startTime = SystemClock.now();
        if (!dataSource.isEmpty()) {
            log.info("# ==============================================================");
            log.info("# === 初始化数据源 ===");
        }
        dataSource.forEach((name, hikariConfig) -> {
            if (dataSourceMap.containsKey(name)) {
                throw new RuntimeException("DataSource 名称重复: " + name);
            }
            hikariConfig = MergeDataSourceConfig.mergeConfig(global, hikariConfig);
            if (StringUtils.isBlank(hikariConfig.getPoolName())) {
                hikariConfig.setPoolName(name);
            }
            HikariDataSource hikariDataSource = new HikariDataSource(hikariConfig);
            dataSourceMap.put(name, hikariDataSource);
        });
        // 初始化 DataSource
        for (Map.Entry<String, DataSource> entry : dataSourceMap.entrySet()) {
            String name = entry.getKey();
            DataSource ds = entry.getValue();
            DataSourceAdmin.addDataSource(name, ds);
        }
        // 默认的 DataSource
        DataSourceAdmin.setDefaultDataSourceName(jdbcConfig.getDefaultName());
        log.info("默认的 DataSource: {}", jdbcConfig.getDefaultName());
        if (!dataSource.isEmpty()) {
            log.info("# === 数据源初始化完成 | 耗时: {}ms ===", SystemClock.now() - startTime);
            log.info("# ==============================================================");
        }
        // DataSource、Jdbc 等对象注入到IOC容器
        for (String datasourceName : DataSourceAdmin.allDatasourceNames()) {
            boolean primary = Objects.equals(datasourceName, jdbcConfig.getDefaultName());
            // DataSource对象注入到Spring容器
            DataSource ds = DataSourceAdmin.getDataSource(datasourceName);
            AppContextHolder.registerBean(datasourceName, ds, primary);
            // Jdbc对象注入到Spring容器
            Jdbc jdbc = DataSourceAdmin.getJdbc(datasourceName);
            AppContextHolder.registerBean(datasourceName + "Jdbc", jdbc, primary);
            // QueryDSL对象注入到Spring容器
            QueryDSL queryDSL = DataSourceAdmin.getQueryDSL(datasourceName);
            AppContextHolder.registerBean(datasourceName + "Dsl", queryDSL, primary);
            // TransactionManager对象注入到Spring容器
            DataSourceTransactionManager transactionManager = jdbc.getTransactionManager();
            AppContextHolder.registerBean(datasourceName + "TX", transactionManager, primary);
        }
    }
}
