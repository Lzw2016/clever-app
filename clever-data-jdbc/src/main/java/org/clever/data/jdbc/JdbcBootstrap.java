package org.clever.data.jdbc;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.clever.core.*;
import org.clever.data.jdbc.config.JdbcConfig;
import org.clever.data.jdbc.config.MybatisConfig;
import org.clever.data.jdbc.metrics.Slf4JLogger;
import org.clever.data.jdbc.mybatis.ClassPathMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.ComposeMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.FileSystemMyBatisMapperSql;
import org.clever.data.jdbc.mybatis.MyBatisMapperSql;
import org.clever.data.jdbc.support.MergeDataSourceConfig;
import org.clever.data.jdbc.support.SqlLoggerUtils;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;
import java.time.Duration;
import java.util.*;
import java.util.function.Function;

/**
 * 作者：lizw <br/>
 * 创建时间：2022/12/26 22:45 <br/>
 */
@Slf4j
public class JdbcBootstrap {
    public static JdbcBootstrap create(String rootPath, JdbcConfig jdbcConfig, MybatisConfig mybatisConfig) {
        return new JdbcBootstrap(rootPath, jdbcConfig, mybatisConfig);
    }

    public static JdbcBootstrap create(String rootPath, Environment environment) {
        MybatisConfig mybatisConfig = Binder.get(environment).bind(MybatisConfig.PREFIX, MybatisConfig.class).orElseGet(MybatisConfig::new);
        JdbcConfig jdbcConfig = Binder.get(environment).bind(JdbcConfig.PREFIX, JdbcConfig.class).orElseGet(JdbcConfig::new);
        AppContextHolder.registerBean("mybatisConfig", mybatisConfig, true);
        AppContextHolder.registerBean("jdbcConfig", jdbcConfig, true);
        return create(rootPath, jdbcConfig, mybatisConfig);
    }

    private volatile boolean initialized = false;
    @Getter
    private final String rootPath;
    @Getter
    private final JdbcConfig jdbcConfig;
    @Getter
    private final MybatisConfig mybatisConfig;

    public JdbcBootstrap(String rootPath, JdbcConfig jdbcConfig, MybatisConfig mybatisConfig) {
        Assert.isNotBlank(rootPath, "参数 rootPath 不能为空");
        Assert.notNull(jdbcConfig, "参数 jdbcConfig 不能为 null");
        Assert.notNull(mybatisConfig, "参数 mybatisConfig 不能为 null");
        this.rootPath = rootPath;
        this.jdbcConfig = jdbcConfig;
        this.mybatisConfig = mybatisConfig;
    }

    public synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        final JdbcConfig.P6SpyLog p6spylog = initP6SpyLog();
        final JdbcConfig.JdbcMetrics metrics = initMetrics();
        Slf4JLogger.init(p6spylog, metrics);
        initMybatis();
        initJdbc();
    }

    private JdbcConfig.P6SpyLog initP6SpyLog() {
        final JdbcConfig.P6SpyLog p6spylog = Optional.ofNullable(jdbcConfig.getP6spylog()).orElseGet(() -> {
            jdbcConfig.setP6spylog(new JdbcConfig.P6SpyLog());
            return jdbcConfig.getP6spylog();
        });
        Function<Collection<String>, List<String>> getStrings = list -> {
            List<String> sqlList = new ArrayList<>();
            if (list != null) {
                for (String item : list) {
                    sqlList.add("    - " + SqlLoggerUtils.deleteWhitespace(item));
                }
            }
            return sqlList;
        };
        List<String> logs = new ArrayList<>();
        logs.add("p6SpyLog: ");
        logs.add("  enable           : " + p6spylog.isEnable());
        logs.add("  slow             : " + p6spylog.getSlow() + "ms");
        logs.add("  ignoreSql        : ");
        logs.addAll(getStrings.apply(p6spylog.getIgnoreSql()));
        logs.add("  ignoreContainsSql: ");
        logs.addAll(getStrings.apply(p6spylog.getIgnoreContainsSql()));
        logs.add("  ignoreThread     : ");
        logs.addAll(getStrings.apply(p6spylog.getIgnoreThread()));
        BannerUtils.printConfig(log, "p6spy打印sql日志配置", logs.toArray(new String[0]));
        return p6spylog;
    }

    private JdbcConfig.JdbcMetrics initMetrics() {
        final JdbcConfig.JdbcMetrics metrics = Optional.ofNullable(jdbcConfig.getMetrics()).orElseGet(() -> {
            jdbcConfig.setMetrics(new JdbcConfig.JdbcMetrics());
            return jdbcConfig.getMetrics();
        });
        if (metrics.isEnable()) {
            BannerUtils.printConfig(log, "jdbc性能监控配置",
                new String[]{
                    "metrics: ",
                    "  enable       : " + true,
                    "  maxSqlCount  : " + metrics.getMaxSqlCount(),
                    "  histogram    : " + metrics.getHistogram(),
                    "  histogramTopN: " + metrics.getHistogramTopN(),
                }
            );
        }
        return metrics;
    }

    private void initMybatis() {
        final Duration interval = Optional.ofNullable(mybatisConfig.getInterval()).orElse(Duration.ZERO);
        final List<MybatisConfig.MapperLocation> locations = Optional.ofNullable(mybatisConfig.getLocations()).orElse(Collections.emptyList());
        // 打印配置日志
        List<String> logs = new ArrayList<>();
        logs.add("mybatis: ");
        logs.add("  enable   : " + mybatisConfig.isEnable());
        logs.add("  watcher  : " + mybatisConfig.isWatcher());
        logs.add("  interval : " + StrFormatter.toPlainString(interval));
        logs.add("  locations: ");
        for (MybatisConfig.MapperLocation location : locations) {
            String path = location.getLocation();
            if (MybatisConfig.FileType.FileSystem.equals(location.getFileType())) {
                path = ResourcePathUtils.getAbsolutePath(rootPath, path);
            }
            logs.add("    - fileType: " + location.getFileType());
            logs.add("      location: " + path);
            logs.add("      filter  : " + location.getFilter());
        }
        if (mybatisConfig.isEnable()) {
            BannerUtils.printConfig(log, "mybatis配置", logs.toArray(new String[0]));
        }
        if (!mybatisConfig.isEnable()) {
            return;
        }
        if (locations.isEmpty()) {
            return;
        }
        List<MyBatisMapperSql> mybatisMapperSqlList = new ArrayList<>(locations.size());
        for (MybatisConfig.MapperLocation location : locations) {
            MyBatisMapperSql mybatisMapperSql;
            if (MybatisConfig.FileType.FileSystem.equals(location.getFileType())) {
                mybatisMapperSql = new FileSystemMyBatisMapperSql(
                    ResourcePathUtils.getAbsolutePath(rootPath, location.getLocation()),
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
            AppShutdownHook.addShutdownHook(composeMyBatisMapperSql::stopWatch, OrderIncrement.NORMAL, "停止监听mapper(sql.xml)文件");
        }
        DataSourceAdmin.setMyBatisMapperSql(composeMyBatisMapperSql);
        AppContextHolder.registerBean("mybatisMapperSql", composeMyBatisMapperSql, true);
    }

    private void initJdbc() {
        final HikariConfig global = Optional.ofNullable(jdbcConfig.getGlobal()).orElseGet(() -> {
            jdbcConfig.setGlobal(new HikariConfig());
            return jdbcConfig.getGlobal();
        });
        final Map<String, HikariConfig> dataSource = Optional.ofNullable(jdbcConfig.getDataSource()).orElse(Collections.emptyMap());
        // 合并数据源配置
        dataSource.forEach((name, config) -> {
            config = MergeDataSourceConfig.mergeConfig(global, config);
            if (StringUtils.isBlank(config.getPoolName())) {
                config.setPoolName(name);
            }
        });
        // 打印配置日志
        List<String> logs = new ArrayList<>();
        logs.add("jdbc: ");
        logs.add("  enable     : " + jdbcConfig.isEnable());
        logs.add("  defaultName: " + jdbcConfig.getDefaultName());
        logs.add("  dataSource : ");
        dataSource.forEach((name, config) -> {
            logs.add("    " + name + ": ");
            logs.add("      jdbcUrl        : " + config.getJdbcUrl());
            logs.add("      username       : " + config.getUsername());
            logs.add("      minimumIdle    : " + config.getMinimumIdle());
            logs.add("      maximumPoolSize: " + config.getMaximumPoolSize());
        });
        if (jdbcConfig.isEnable()) {
            BannerUtils.printConfig(log, "jdbc数据源配置", logs.toArray(new String[0]));
        }
        if (!jdbcConfig.isEnable()) {
            return;
        }
        final Map<String, DataSource> dataSourceMap = new HashMap<>(dataSource.size());
        // 初始化 HikariDataSource
        final long startTime = SystemClock.now();
        dataSource.forEach((name, hikariConfig) -> {
            if (dataSourceMap.containsKey(name)) {
                throw new RuntimeException("DataSource 名称重复: " + name);
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
            log.info("jdbc数据源初始化完成 | 耗时: {}ms", SystemClock.now() - startTime);
        }
        // DataSource、Jdbc 等对象注入到IOC容器
        for (String datasourceName : DataSourceAdmin.allDatasourceNames()) {
            boolean primary = Objects.equals(datasourceName, jdbcConfig.getDefaultName());
            // DataSource对象注入到IOC容器
            DataSource ds = DataSourceAdmin.getDataSource(datasourceName);
            AppContextHolder.registerBean(datasourceName, ds, primary);
            // Jdbc对象注入到IOC容器
            Jdbc jdbc = DataSourceAdmin.getJdbc(datasourceName);
            AppContextHolder.registerBean(datasourceName + "Jdbc", jdbc, primary);
            // QueryDSL对象注入到IOC容器
            QueryDSL queryDSL = DataSourceAdmin.getQueryDSL(datasourceName);
            AppContextHolder.registerBean(datasourceName + "Dsl", queryDSL, primary);
            // TransactionManager对象注入到IOC容器
            DataSourceTransactionManager transactionManager = jdbc.getTransactionManager();
            AppContextHolder.registerBean(datasourceName + "TX", transactionManager, primary);
        }
    }
}
