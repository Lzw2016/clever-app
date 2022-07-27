package org.clever.jdbc.support;

import org.clever.dao.*;
import org.clever.jdbc.BadSqlGrammarException;
import org.clever.jdbc.InvalidResultSetAccessException;
import org.clever.util.function.SingletonSupplier;
import org.clever.util.function.SupplierUtils;

import javax.sql.DataSource;
import java.lang.reflect.Constructor;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * 分析供应商特定错误代码的{@link SQLExceptionTranslator}的实现。
 * 比基于SQL状态的实现更精确，但非常特定于供应商。
 *
 * <p>此类应用以下匹配规则：
 * <ul>
 * <li>尝试由任何子类实现的自定义翻译。请注意，这个类是具体的，通常自己使用，在这种情况下，这个规则不适用。
 * <li>应用错误代码匹配。默认情况下，从SQLErrorCodesFactory获取错误代码。
 * 此工厂从类路径加载“sql-error-codes.xml”文件，从数据库元数据定义数据库名称的错误代码映射。
 * <li>回退到回退转换器。{@link SQLStateSQLExceptionTranslator}是默认的回退转换器，只分析异常的SQL状态。
 * 在引入了自己的{@code SQLException}子类层次结构的Java 6上，默认情况下我们将使用{@link SQLExceptionSubclassTranslator}，
 * 当没有遇到特定的子类时，它又会返回到clever自己的SQL状态转换。
 * </ul>
 *
 * <p>默认情况下，将从此包读取名为“sql-error-codes.xml”的配置文件。
 * 只要JDBC包是从同一个类加载器加载的，就可以通过类路径根目录中的同名文件（例如在“/WEB-INF/classes”目录中）来覆盖它。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 21:43 <br/>
 *
 * @see SQLErrorCodesFactory
 * @see SQLStateSQLExceptionTranslator
 */
public class SQLErrorCodeSQLExceptionTranslator extends AbstractFallbackSQLExceptionTranslator {
    private static final int MESSAGE_ONLY_CONSTRUCTOR = 1;
    private static final int MESSAGE_THROWABLE_CONSTRUCTOR = 2;
    private static final int MESSAGE_SQLEX_CONSTRUCTOR = 3;
    private static final int MESSAGE_SQL_THROWABLE_CONSTRUCTOR = 4;
    private static final int MESSAGE_SQL_SQLEX_CONSTRUCTOR = 5;
    /**
     * 此转换器使用的错误代码。
     */
    private SingletonSupplier<SQLErrorCodes> sqlErrorCodes;

    /**
     * 用作JavaBean的构造函数。
     * 必须设置SqlErrorCodes或DataSource属性。
     */
    public SQLErrorCodeSQLExceptionTranslator() {
        setFallbackTranslator(new SQLExceptionSubclassTranslator());
    }

    /**
     * 为给定数据源创建SQL错误代码转换器。调用此构造函数将导致从数据源获取连接以获取元数据。
     *
     * @param dataSource 用于查找元数据并确定哪些错误代码可用的数据源
     * @see SQLErrorCodesFactory
     */
    public SQLErrorCodeSQLExceptionTranslator(DataSource dataSource) {
        this();
        setDataSource(dataSource);
    }

    /**
     * 为给定的数据库产品名称创建SQL错误代码转换器。
     * 调用此构造函数将避免从数据源获取连接以获取元数据。
     *
     * @param dbName 标识错误代码条目的数据库产品名称
     * @see SQLErrorCodesFactory
     * @see java.sql.DatabaseMetaData#getDatabaseProductName()
     */
    public SQLErrorCodeSQLExceptionTranslator(String dbName) {
        this();
        setDatabaseProductName(dbName);
    }

    /**
     * 根据这些错误代码创建SQLErrorCode转换器。
     * 不需要使用连接执行数据库元数据查找。
     *
     * @param sec 错误代码
     */
    public SQLErrorCodeSQLExceptionTranslator(SQLErrorCodes sec) {
        this();
        this.sqlErrorCodes = SingletonSupplier.of(sec);
    }

    /**
     * 设置此转换器的数据源。
     * <p>设置此属性将导致从数据源获取连接以获取元数据。
     *
     * @param dataSource 用于查找元数据并确定哪些错误代码可用的数据源
     * @see SQLErrorCodesFactory#getErrorCodes(DataSource)
     * @see java.sql.DatabaseMetaData#getDatabaseProductName()
     */
    public void setDataSource(DataSource dataSource) {
        this.sqlErrorCodes = SingletonSupplier.of(() -> SQLErrorCodesFactory.getInstance().resolveErrorCodes(dataSource));
        this.sqlErrorCodes.get();  // try early initialization - otherwise the supplier will retry later
    }

    /**
     * 设置此转换器的数据库产品名称。
     * <p>设置此属性将避免从数据源获取连接以获取元数据。
     *
     * @param dbName 标识错误代码条目的数据库产品名称
     * @see SQLErrorCodesFactory#getErrorCodes(String)
     * @see java.sql.DatabaseMetaData#getDatabaseProductName()
     */
    public void setDatabaseProductName(String dbName) {
        this.sqlErrorCodes = SingletonSupplier.of(SQLErrorCodesFactory.getInstance().getErrorCodes(dbName));
    }

    /**
     * 设置用于翻译的自定义错误代码。
     *
     * @param sec 要使用的自定义错误代码
     */
    public void setSqlErrorCodes(SQLErrorCodes sec) {
        this.sqlErrorCodes = SingletonSupplier.ofNullable(sec);
    }

    /**
     * 返回此转换器使用的错误代码。
     * 通常通过数据源确定。
     *
     * @see #setDataSource
     */
    public SQLErrorCodes getSqlErrorCodes() {
        return SupplierUtils.resolve(this.sqlErrorCodes);
    }

    @Override
    protected DataAccessException doTranslate(String task, String sql, SQLException ex) {
        SQLException sqlEx = ex;
        if (sqlEx instanceof BatchUpdateException && sqlEx.getNextException() != null) {
            SQLException nestedSqlEx = sqlEx.getNextException();
            if (nestedSqlEx.getErrorCode() > 0 || nestedSqlEx.getSQLState() != null) {
                sqlEx = nestedSqlEx;
            }
        }
        // First, try custom translation from overridden method.
        DataAccessException dae = customTranslate(task, sql, sqlEx);
        if (dae != null) {
            return dae;
        }
        // Next, try the custom SQLException translator, if available.
        SQLErrorCodes sqlErrorCodes = getSqlErrorCodes();
        if (sqlErrorCodes != null) {
            SQLExceptionTranslator customTranslator = sqlErrorCodes.getCustomSqlExceptionTranslator();
            if (customTranslator != null) {
                DataAccessException customDex = customTranslator.translate(task, sql, sqlEx);
                if (customDex != null) {
                    return customDex;
                }
            }
        }
        // Check SQLErrorCodes with corresponding error code, if available.
        if (sqlErrorCodes != null) {
            String errorCode;
            if (sqlErrorCodes.isUseSqlStateForTranslation()) {
                errorCode = sqlEx.getSQLState();
            } else {
                // Try to find SQLException with actual error code, looping through the causes.
                // E.g. applicable to java.sql.DataTruncation as of JDK 1.6.
                SQLException current = sqlEx;
                while (current.getErrorCode() == 0 && current.getCause() instanceof SQLException) {
                    current = (SQLException) current.getCause();
                }
                errorCode = Integer.toString(current.getErrorCode());
            }
            if (errorCode != null) {
                // Look for defined custom translations first.
                CustomSQLErrorCodesTranslation[] customTranslations = sqlErrorCodes.getCustomTranslations();
                if (customTranslations != null) {
                    for (CustomSQLErrorCodesTranslation customTranslation : customTranslations) {
                        if (Arrays.binarySearch(customTranslation.getErrorCodes(), errorCode) >= 0
                                && customTranslation.getExceptionClass() != null) {
                            DataAccessException customException = createCustomException(
                                    task, sql, sqlEx, customTranslation.getExceptionClass()
                            );
                            if (customException != null) {
                                logTranslation(task, sql, sqlEx, true);
                                return customException;
                            }
                        }
                    }
                }
                // Next, look for grouped error codes.
                if (Arrays.binarySearch(sqlErrorCodes.getBadSqlGrammarCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new BadSqlGrammarException(task, (sql != null ? sql : ""), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getInvalidResultSetAccessCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new InvalidResultSetAccessException(task, (sql != null ? sql : ""), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getDuplicateKeyCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new DuplicateKeyException(buildMessage(task, sql, sqlEx), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getDataIntegrityViolationCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new DataIntegrityViolationException(buildMessage(task, sql, sqlEx), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getPermissionDeniedCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new PermissionDeniedDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getDataAccessResourceFailureCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new DataAccessResourceFailureException(buildMessage(task, sql, sqlEx), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getTransientDataAccessResourceCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new TransientDataAccessResourceException(buildMessage(task, sql, sqlEx), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getCannotAcquireLockCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new CannotAcquireLockException(buildMessage(task, sql, sqlEx), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getDeadlockLoserCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new DeadlockLoserDataAccessException(buildMessage(task, sql, sqlEx), sqlEx);
                } else if (Arrays.binarySearch(sqlErrorCodes.getCannotSerializeTransactionCodes(), errorCode) >= 0) {
                    logTranslation(task, sql, sqlEx, false);
                    return new CannotSerializeTransactionException(buildMessage(task, sql, sqlEx), sqlEx);
                }
            }
        }
        // We couldn't identify it more precisely - let's hand it over to the SQLState fallback translator.
        if (logger.isDebugEnabled()) {
            String codes;
            if (sqlErrorCodes != null && sqlErrorCodes.isUseSqlStateForTranslation()) {
                codes = "SQL state '" + sqlEx.getSQLState() + "', error code '" + sqlEx.getErrorCode();
            } else {
                codes = "Error code '" + sqlEx.getErrorCode() + "'";
            }
            logger.debug("Unable to translate SQLException with " + codes + ", will now try the fallback translator");
        }
        return null;
    }

    /**
     * 子类可以重写此方法，以尝试从{@link SQLException}到{@link DataAccessException}的自定义映射。
     *
     * @param task  描述正在尝试的任务的可读文本
     * @param sql   导致问题的SQL查询或更新(可能是 {@code null})
     * @param sqlEx 有问题的SQLException
     * @return 如果没有应用自定义转换，则为null，否则自定义转换将导致{@link DataAccessException}。
     * 此异常应包括sqlEx参数作为嵌套的根本原因。这个实现总是返回null，这意味着转换器总是返回默认的错误代码。
     */
    protected DataAccessException customTranslate(String task, String sql, SQLException sqlEx) {
        return null;
    }

    /**
     * 基于{@link CustomSQLErrorCodesTranslation}定义中的给定异常类创建自定义{@link DataAccessException}。
     *
     * @param task           描述正在尝试的任务的可读文本
     * @param sql            导致问题的SQL查询或更新(可能是 {@code null})
     * @param sqlEx          有问题的SQLException
     * @param exceptionClass 要使用的异常类，如{@link CustomSQLErrorCodesTranslation}定义中所定义
     * @return 如果无法创建自定义异常，则返回null，否则返回结果{@link DataAccessException}。此异常应包括{@code sqlEx}参数作为嵌套的根本原因。
     * @see CustomSQLErrorCodesTranslation#setExceptionClass
     */
    protected DataAccessException createCustomException(String task, String sql, SQLException sqlEx, Class<?> exceptionClass) {
        // Find appropriate constructor for the given exception class
        try {
            int constructorType = 0;
            Constructor<?>[] constructors = exceptionClass.getConstructors();
            for (Constructor<?> constructor : constructors) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1
                        && String.class == parameterTypes[0]
                        && constructorType < MESSAGE_ONLY_CONSTRUCTOR) {
                    constructorType = MESSAGE_ONLY_CONSTRUCTOR;
                }
                if (parameterTypes.length == 2
                        && String.class == parameterTypes[0]
                        && Throwable.class == parameterTypes[1]
                        && constructorType < MESSAGE_THROWABLE_CONSTRUCTOR) {
                    constructorType = MESSAGE_THROWABLE_CONSTRUCTOR;
                }
                if (parameterTypes.length == 2
                        && String.class == parameterTypes[0]
                        && SQLException.class == parameterTypes[1]
                        && constructorType < MESSAGE_SQLEX_CONSTRUCTOR) {
                    constructorType = MESSAGE_SQLEX_CONSTRUCTOR;
                }
                if (parameterTypes.length == 3
                        && String.class == parameterTypes[0]
                        && String.class == parameterTypes[1]
                        && Throwable.class == parameterTypes[2]
                        && constructorType < MESSAGE_SQL_THROWABLE_CONSTRUCTOR) {
                    constructorType = MESSAGE_SQL_THROWABLE_CONSTRUCTOR;
                }
                if (parameterTypes.length == 3
                        && String.class == parameterTypes[0]
                        && String.class == parameterTypes[1]
                        && SQLException.class == parameterTypes[2]
                        && constructorType < MESSAGE_SQL_SQLEX_CONSTRUCTOR) {
                    constructorType = MESSAGE_SQL_SQLEX_CONSTRUCTOR;
                }
            }
            // invoke constructor
            Constructor<?> exceptionConstructor;
            switch (constructorType) {
                case MESSAGE_SQL_SQLEX_CONSTRUCTOR:
                    Class<?>[] messageAndSqlAndSqlExArgsClass = new Class<?>[]{String.class, String.class, SQLException.class};
                    Object[] messageAndSqlAndSqlExArgs = new Object[]{task, sql, sqlEx};
                    exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndSqlExArgsClass);
                    return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndSqlExArgs);
                case MESSAGE_SQL_THROWABLE_CONSTRUCTOR:
                    Class<?>[] messageAndSqlAndThrowableArgsClass = new Class<?>[]{String.class, String.class, Throwable.class};
                    Object[] messageAndSqlAndThrowableArgs = new Object[]{task, sql, sqlEx};
                    exceptionConstructor = exceptionClass.getConstructor(messageAndSqlAndThrowableArgsClass);
                    return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlAndThrowableArgs);
                case MESSAGE_SQLEX_CONSTRUCTOR:
                    Class<?>[] messageAndSqlExArgsClass = new Class<?>[]{String.class, SQLException.class};
                    Object[] messageAndSqlExArgs = new Object[]{task + ": " + sqlEx.getMessage(), sqlEx};
                    exceptionConstructor = exceptionClass.getConstructor(messageAndSqlExArgsClass);
                    return (DataAccessException) exceptionConstructor.newInstance(messageAndSqlExArgs);
                case MESSAGE_THROWABLE_CONSTRUCTOR:
                    Class<?>[] messageAndThrowableArgsClass = new Class<?>[]{String.class, Throwable.class};
                    Object[] messageAndThrowableArgs = new Object[]{task + ": " + sqlEx.getMessage(), sqlEx};
                    exceptionConstructor = exceptionClass.getConstructor(messageAndThrowableArgsClass);
                    return (DataAccessException) exceptionConstructor.newInstance(messageAndThrowableArgs);
                case MESSAGE_ONLY_CONSTRUCTOR:
                    Class<?>[] messageOnlyArgsClass = new Class<?>[]{String.class};
                    Object[] messageOnlyArgs = new Object[]{task + ": " + sqlEx.getMessage()};
                    exceptionConstructor = exceptionClass.getConstructor(messageOnlyArgsClass);
                    return (DataAccessException) exceptionConstructor.newInstance(messageOnlyArgs);
                default:
                    if (logger.isWarnEnabled()) {
                        logger.warn(
                                "Unable to find appropriate constructor of custom exception class ["
                                        + exceptionClass.getName() + "]"
                        );
                    }
                    return null;
            }
        } catch (Throwable ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Unable to instantiate custom exception class [" + exceptionClass.getName() + "]", ex);
            }
            return null;
        }
    }

    private void logTranslation(String task, String sql, SQLException sqlEx, boolean custom) {
        if (logger.isDebugEnabled()) {
            String intro = custom ? "Custom translation of" : "Translating";
            logger.debug(
                    intro + " SQLException with SQL state '" + sqlEx.getSQLState() +
                            "', error code '" + sqlEx.getErrorCode() + "', message [" + sqlEx.getMessage() + "]" +
                            (sql != null ? "; SQL was [" + sql + "]" : "") + " for task [" + task + "]"
            );
        }
    }
}
