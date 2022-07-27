package org.clever.jdbc.support;

import org.clever.core.io.ClassPathResource;
import org.clever.core.io.Resource;
import org.clever.util.Assert;
import org.clever.util.ConcurrentReferenceHashMap;
import org.clever.util.PatternMatchUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于基于从{@link DatabaseMetaData}获取的“databaseProductName”创建{@link SQLErrorCodes}的工厂。
 *
 * <p>返回由名为“sql-error-codes.xml”的配置文件中定义的供应商代码填充的{@code SQLErrorCodes}。
 * 如果未被类路径根目录中的文件覆盖（例如在“/WEB-INF/classes”目录中），则读取此包中的默认文件。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:14 <br/>
 *
 * @see DatabaseMetaData#getDatabaseProductName()
 */
public class SQLErrorCodesFactory {
    /**
     * 自定义SQL错误代码文件的名称，从类路径的根目录加载（例如从“/WEB-INF/classes”目录）。
     */
    public static final String SQL_ERROR_CODE_OVERRIDE_PATH = "sql-error-codes.xml";
    /**
     * 默认SQL错误代码文件的名称，从类路径加载。
     */
    public static final String SQL_ERROR_CODE_DEFAULT_PATH = "org/clever/jdbc/support/sql-error-codes.xml";
    private static final Logger logger = LoggerFactory.getLogger(SQLErrorCodesFactory.class);
    /**
     * 跟踪单个实例，以便我们可以将其返回给请求它的类。
     */
    private static final SQLErrorCodesFactory instance = new SQLErrorCodesFactory();

    /**
     * 返回singleton实例。
     */
    public static SQLErrorCodesFactory getInstance() {
        return instance;
    }

    /**
     * 映射以保留配置文件中定义的所有数据库的错误代码。
     * 键是数据库产品名称，值是SQLErrorCodes实例。
     */
    private final Map<String, SQLErrorCodes> errorCodesMap;
    /**
     * 映射以缓存每个数据源的SQLErrorCodes实例。
     */
    private final Map<DataSource, SQLErrorCodes> dataSourceCache = new ConcurrentReferenceHashMap<>(16);

    /**
     * 创建 {@link SQLErrorCodesFactory} 类的新实例。
     * <p>
     * 不公开强制执行 Singleton 设计模式。
     * 将是私有的，除非允许通过覆盖 {@link #loadResource(String)} 方法进行测试。
     * <p><b>不要在应用程序代码中进行子类化。</b>
     *
     * @see #loadResource(String)
     */
    protected SQLErrorCodesFactory() {
        Map<String, SQLErrorCodes> errorCodes = new HashMap<>(11);
        SQLErrorCodes sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductName("DB2*");
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("-007,-029,-097,-104,-109,-115,-128,-199,-204,-206,-301,-408,-441,-491", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("-803", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("-407,-530,-531,-532,-543,-544,-545,-603,-667", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("-904,-971", ","));
        sqlErrorCodes.setTransientDataAccessResourceCodes(StringUtils.delimitedListToStringArray("-1035,-1218,-30080,-30081", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("-911,-913", ","));
        errorCodes.put("DB2", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductName("Apache Derby");
        sqlErrorCodes.setUseSqlStateForTranslation(true);
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("42802,42821,42X01,42X02,42X03,42X04,42X05,42X06,42X07,42X08", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("23505", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("22001,22005,23502,23503,23513,X0Y32", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("04501,08004,42Y07", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("40XL1", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("40001", ","));
        errorCodes.put("Derby", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("42000,42001,42101,42102,42111,42112,42121,42122,42132", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("23001,23505", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("22001,22003,22012,22018,22025,23000,23002,23003,23502,23503,23506,23507,23513", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("90046,90100,90117,90121,90126", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("50200", ","));
        errorCodes.put("H2", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductNames("SAP HANA", "SAP DB");
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("257,259,260,261,262,263,264,267,268,269,270,271,272,273,275,276,277,278," +
                "278,279,280,281,282,283,284,285,286,288,289,290,294,295,296,297,299,308,309," +
                "313,315,316,318,319,320,321,322,323,324,328,329,330,333,335,336,337,338,340," +
                "343,350,351,352,362,368", ","));
        sqlErrorCodes.setPermissionDeniedCodes(StringUtils.delimitedListToStringArray("10,258", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("301", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("461,462", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("-813,-709,-708,1024,1025,1026,1027,1029,1030,1031", ","));
        sqlErrorCodes.setInvalidResultSetAccessCodes(StringUtils.delimitedListToStringArray("-11210,582,587,588,594", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("131", ","));
        sqlErrorCodes.setCannotSerializeTransactionCodes(StringUtils.delimitedListToStringArray("138,143", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("133", ","));
        errorCodes.put("HDB", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductNames("HSQL Database Engine");
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("-22,-28", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("-104", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("-9", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("-80", ","));
        errorCodes.put("HSQL", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductNames("Informix Dynamic Server");
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("-201,-217,-696", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("-239,-268,-6017", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("-692,-11030", ","));
        errorCodes.put("Informix", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductNames("Microsoft SQL Server");
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("156,170,207,208,209", ","));
        sqlErrorCodes.setPermissionDeniedCodes(StringUtils.delimitedListToStringArray("229", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("2601,2627", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("544,8114,8115", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("4060", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("1222", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("1205", ","));
        errorCodes.put("MS-SQL", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductNames("MySQL", "MariaDB");
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("1054,1064,1146", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("1062", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("630,839,840,893,1169,1215,1216,1217,1364,1451,1452,1557", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("1", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("1205,3572", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("1213", ","));
        errorCodes.put("MySQL", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("900,903,904,917,936,942,17006,6550", ","));
        sqlErrorCodes.setInvalidResultSetAccessCodes(StringUtils.delimitedListToStringArray("17003", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("1400,1722,2291,2292", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("17002,17447", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("1", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("54,30006", ","));
        sqlErrorCodes.setCannotSerializeTransactionCodes(StringUtils.delimitedListToStringArray("8177", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("60", ","));
        errorCodes.put("Oracle", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setUseSqlStateForTranslation(true);
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("03000,42000,42601,42602,42622,42804,42P01", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("21000,23505", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("23000,23502,23503,23514", ","));
        sqlErrorCodes.setDataAccessResourceFailureCodes(StringUtils.delimitedListToStringArray("53000,53100,53200,53300", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("55P03", ","));
        sqlErrorCodes.setCannotSerializeTransactionCodes(StringUtils.delimitedListToStringArray("40001", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("40P01", ","));
        errorCodes.put("PostgreSQL", sqlErrorCodes);

        sqlErrorCodes = new SQLErrorCodes();
        sqlErrorCodes.setDatabaseProductNames("Sybase SQL Server", "Adaptive Server Enterprise", "ASE", "SQL Server", "sql server");
        sqlErrorCodes.setBadSqlGrammarCodes(StringUtils.delimitedListToStringArray("101,102,103,104,105,106,107,108,109,110,111,112,113,116,120,121,123,207,208,213,257,512", ","));
        sqlErrorCodes.setDuplicateKeyCodes(StringUtils.delimitedListToStringArray("2601,2615,2626", ","));
        sqlErrorCodes.setDataIntegrityViolationCodes(StringUtils.delimitedListToStringArray("233,511,515,530,546,547,2615,2714", ","));
        sqlErrorCodes.setTransientDataAccessResourceCodes(StringUtils.delimitedListToStringArray("921,1105", ","));
        sqlErrorCodes.setCannotAcquireLockCodes(StringUtils.delimitedListToStringArray("12205", ","));
        sqlErrorCodes.setDeadlockLoserCodes(StringUtils.delimitedListToStringArray("1205", ","));
        errorCodes.put("Sybase", sqlErrorCodes);
        this.errorCodesMap = errorCodes;
    }

    /**
     * 从类路径加载给定资源。
     * <p>不被应用程序开发人员覆盖，应用程序开发人员应该从静态{@link #getInstance()}方法获取此类的实例。
     * <p>受测试性保护。
     *
     * @param path 资源路径；自定义路径或以下路径之一 {@link #SQL_ERROR_CODE_DEFAULT_PATH} 或 {@link #SQL_ERROR_CODE_OVERRIDE_PATH}.
     * @return 资源，如果找不到资源，则为null
     * @see #getInstance
     */
    protected Resource loadResource(String path) {
        return new ClassPathResource(path, getClass().getClassLoader());
    }

    /**
     * 返回给定数据库的{@link SQLErrorCodes}实例。
     * <p>不需要数据库元数据查找。
     *
     * @param databaseName 数据库名称 (不能是 {@code null})
     * @return 给定数据库的{@code SQLErrorCodes}实例（从不为null；可能为空）
     * @throws IllegalArgumentException 如果提供的数据库名称为 {@code null}
     */
    public SQLErrorCodes getErrorCodes(String databaseName) {
        Assert.notNull(databaseName, "Database product name must not be null");
        SQLErrorCodes sec = this.errorCodesMap.get(databaseName);
        if (sec == null) {
            for (SQLErrorCodes candidate : this.errorCodesMap.values()) {
                if (PatternMatchUtils.simpleMatch(candidate.getDatabaseProductNames(), databaseName)) {
                    sec = candidate;
                    break;
                }
            }
        }
        if (sec != null) {
            checkCustomTranslatorRegistry(databaseName, sec);
            if (logger.isDebugEnabled()) {
                logger.debug("SQL error codes for '" + databaseName + "' found");
            }
            return sec;
        }
        // Could not find the database among the defined ones.
        if (logger.isDebugEnabled()) {
            logger.debug("SQL error codes for '" + databaseName + "' not found");
        }
        return new SQLErrorCodes();
    }

    /**
     * 返回给定数据源的{@link SQLErrorCodes}，
     * 从{@link DatabaseMetaData}中计算“databaseProductName”，
     * 如果未找到SQLErrorCodes，则返回空的错误代码实例。
     *
     * @param dataSource 识别数据库的数据源
     * @return 对应的{@code SQLErrorCodes}对象（从不为null；可能为空）
     * @see DatabaseMetaData#getDatabaseProductName()
     */
    public SQLErrorCodes getErrorCodes(DataSource dataSource) {
        SQLErrorCodes sec = resolveErrorCodes(dataSource);
        return (sec != null ? sec : new SQLErrorCodes());
    }

    /**
     * 返回给定数据源的{@link SQLErrorCodes}，
     * 从{@link DatabaseMetaData}中计算“databaseProductName”，
     * 如果出现JDBC元数据访问问题，则返回null。
     *
     * @param dataSource 识别数据库的数据源
     * @return 对应的{@code SQLErrorCodes}对象，如果JDBC元数据访问问题，则为null
     * @see DatabaseMetaData#getDatabaseProductName()
     */
    public SQLErrorCodes resolveErrorCodes(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource must not be null");
        if (logger.isDebugEnabled()) {
            logger.debug("Looking up default SQLErrorCodes for DataSource [" + identify(dataSource) + "]");
        }
        // Try efficient lock-free access for existing cache entry
        SQLErrorCodes sec = this.dataSourceCache.get(dataSource);
        if (sec == null) {
            synchronized (this.dataSourceCache) {
                // Double-check within full dataSourceCache lock
                sec = this.dataSourceCache.get(dataSource);
                if (sec == null) {
                    // We could not find it - got to look it up.
                    try {
                        String name = JdbcUtils.extractDatabaseMetaData(
                                dataSource, DatabaseMetaData::getDatabaseProductName
                        );
                        if (StringUtils.hasLength(name)) {
                            return registerDatabase(dataSource, name);
                        }
                    } catch (MetaDataAccessException ex) {
                        logger.warn("Error while extracting database name", ex);
                    }
                    return null;
                }
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("SQLErrorCodes found in cache for DataSource [" + identify(dataSource) + "]");
        }
        return sec;
    }

    /**
     * 将指定的数据库名称与给定的数据源相关联。
     *
     * @param dataSource   识别数据库的数据源
     * @param databaseName 错误代码定义文件中规定的相应数据库名称（不得为空）
     * @return 对应的{@code SQLErrorCodes}对象（从不为null）
     * @see #unregisterDatabase(DataSource)
     */
    public SQLErrorCodes registerDatabase(DataSource dataSource, String databaseName) {
        SQLErrorCodes sec = getErrorCodes(databaseName);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "Caching SQL error codes for DataSource [" + identify(dataSource) +
                            "]: database product name is '" + databaseName + "'"
            );
        }
        this.dataSourceCache.put(dataSource, sec);
        return sec;
    }

    /**
     * 如果已注册，请清除指定数据源的缓存。
     *
     * @param dataSource 识别数据库的数据源
     * @return 已删除的对应{@code SQLErrorCodes}对象，如果未注册，则为null
     * @see #registerDatabase(DataSource, String)
     */
    public SQLErrorCodes unregisterDatabase(DataSource dataSource) {
        return this.dataSourceCache.remove(dataSource);
    }

    /**
     * 为给定的数据源构建标识字符串，主要用于日志记录。
     *
     * @param dataSource 要内省的数据源
     * @return 标识字符串
     */
    private String identify(DataSource dataSource) {
        return dataSource.getClass().getName() + '@' + Integer.toHexString(dataSource.hashCode());
    }

    /**
     * 检查{@link CustomSQLExceptionTranslatorRegistry}中的任何条目。
     */
    private void checkCustomTranslatorRegistry(String databaseName, SQLErrorCodes errorCodes) {
        SQLExceptionTranslator customTranslator = CustomSQLExceptionTranslatorRegistry.getInstance().findTranslatorForDatabase(databaseName);
        if (customTranslator != null) {
            if (errorCodes.getCustomSqlExceptionTranslator() != null && logger.isDebugEnabled()) {
                logger.debug(
                        "Overriding already defined custom translator '" +
                                errorCodes.getCustomSqlExceptionTranslator().getClass().getSimpleName() +
                                " with '" + customTranslator.getClass().getSimpleName() +
                                "' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'"
                );
            } else if (logger.isTraceEnabled()) {
                logger.trace(
                        "Using custom translator '" + customTranslator.getClass().getSimpleName() +
                                "' found in the CustomSQLExceptionTranslatorRegistry for database '" + databaseName + "'"
                );
            }
            errorCodes.setCustomSqlExceptionTranslator(customTranslator);
        }
    }
}
