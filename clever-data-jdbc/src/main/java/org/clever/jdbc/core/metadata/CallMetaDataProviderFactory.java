package org.clever.jdbc.core.metadata;

import org.clever.dao.DataAccessResourceFailureException;
import org.clever.jdbc.support.JdbcUtils;
import org.clever.jdbc.support.MetaDataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.List;

/**
 * 工厂用于根据所使用的数据库类型创建 {@link CallMetaDataProvider} 实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/12/05 17:07 <br/>
 */
public final class CallMetaDataProviderFactory {
    private static final Logger logger = LoggerFactory.getLogger(CallMetaDataProviderFactory.class);
    /**
     * 过程调用支持的数据库产品列表。
     */
    public static final List<String> supportedDatabaseProductsForProcedures = Arrays.asList(
            "Apache Derby",
            "DB2",
            "Informix Dynamic Server",
            "MariaDB",
            "Microsoft SQL Server",
            "MySQL",
            "Oracle",
            "PostgreSQL",
            "Sybase"
    );
    /**
     * 函数调用支持的数据库产品列表。
     */
    public static final List<String> supportedDatabaseProductsForFunctions = Arrays.asList(
            "MariaDB",
            "Microsoft SQL Server",
            "MySQL",
            "Oracle",
            "PostgreSQL"
    );

    private CallMetaDataProviderFactory() {
    }

    /**
     * 基于数据库元数据创建一个 {@link CallMetaDataProvider}。
     *
     * @param dataSource 用于检索元数据的 JDBC 数据源
     * @param context    保存配置和元数据的类
     * @return 要使用的 CallMetaDataProvider 实现的实例
     */
    public static CallMetaDataProvider createMetaDataProvider(DataSource dataSource, final CallMetaDataContext context) {
        try {
            return JdbcUtils.extractDatabaseMetaData(dataSource, databaseMetaData -> {
                String databaseProductName = JdbcUtils.commonDatabaseName(databaseMetaData.getDatabaseProductName());
                boolean accessProcedureColumnMetaData = context.isAccessCallParameterMetaData();
                if (context.isFunction()) {
                    if (!supportedDatabaseProductsForFunctions.contains(databaseProductName)) {
                        if (logger.isInfoEnabled()) {
                            logger.info(databaseProductName + " is not one of the databases fully supported for function calls " +
                                    "-- supported are: " + supportedDatabaseProductsForFunctions
                            );
                        }
                        if (accessProcedureColumnMetaData) {
                            logger.info("Metadata processing disabled - you must specify all parameters explicitly");
                            accessProcedureColumnMetaData = false;
                        }
                    }
                } else {
                    if (!supportedDatabaseProductsForProcedures.contains(databaseProductName)) {
                        if (logger.isInfoEnabled()) {
                            logger.info(databaseProductName + " is not one of the databases fully supported for procedure calls " +
                                    "-- supported are: " + supportedDatabaseProductsForProcedures
                            );
                        }
                        if (accessProcedureColumnMetaData) {
                            logger.info("Metadata processing disabled - you must specify all parameters explicitly");
                            accessProcedureColumnMetaData = false;
                        }
                    }
                }
                CallMetaDataProvider provider;
                if ("Oracle".equals(databaseProductName)) {
                    provider = new OracleCallMetaDataProvider(databaseMetaData);
                } else if ("PostgreSQL".equals(databaseProductName)) {
                    provider = new PostgresCallMetaDataProvider((databaseMetaData));
                } else if ("Apache Derby".equals(databaseProductName)) {
                    provider = new DerbyCallMetaDataProvider((databaseMetaData));
                } else if ("DB2".equals(databaseProductName)) {
                    provider = new Db2CallMetaDataProvider((databaseMetaData));
                } else if ("HDB".equals(databaseProductName)) {
                    provider = new HanaCallMetaDataProvider((databaseMetaData));
                } else if ("Microsoft SQL Server".equals(databaseProductName)) {
                    provider = new SqlServerCallMetaDataProvider((databaseMetaData));
                } else if ("Sybase".equals(databaseProductName)) {
                    provider = new SybaseCallMetaDataProvider((databaseMetaData));
                } else {
                    provider = new GenericCallMetaDataProvider(databaseMetaData);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("Using " + provider.getClass().getName());
                }
                provider.initializeWithMetaData(databaseMetaData);
                if (accessProcedureColumnMetaData) {
                    provider.initializeWithProcedureColumnMetaData(
                            databaseMetaData, context.getCatalogName(), context.getSchemaName(), context.getProcedureName()
                    );
                }
                return provider;
            });
        } catch (MetaDataAccessException ex) {
            throw new DataAccessResourceFailureException("Error retrieving database meta-data", ex);
        }
    }
}
