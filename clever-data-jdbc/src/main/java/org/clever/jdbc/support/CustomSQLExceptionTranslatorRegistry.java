package org.clever.jdbc.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * 与特定数据库关联的自定义{@link SQLExceptionTranslator}实例的注册表，
 * 允许基于名为“sql-error-codes.xml”的配置文件中包含的值重写转换。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:22 <br/>
 *
 * @see SQLErrorCodesFactory
 */
public final class CustomSQLExceptionTranslatorRegistry {
    private static final Logger logger = LoggerFactory.getLogger(CustomSQLExceptionTranslatorRegistry.class);
    /**
     * 跟踪单个实例，以便我们可以将其返回给请求它的类。
     */
    private static final CustomSQLExceptionTranslatorRegistry instance = new CustomSQLExceptionTranslatorRegistry();

    /**
     * 返回singleton实例
     */
    public static CustomSQLExceptionTranslatorRegistry getInstance() {
        return instance;
    }

    /**
     * Map注册表以保存自定义转换器特定的数据库。Key是中定义的数据库产品名称 {@link SQLErrorCodesFactory}.
     */
    private final Map<String, SQLExceptionTranslator> translatorMap = new HashMap<>();

    /**
     * 创建{@link CustomSQLExceptionTranslatorRegistry}类的新实例。
     * <p>不公开以强制实施单例设计模式。
     */
    private CustomSQLExceptionTranslatorRegistry() {
    }

    /**
     * 为指定的数据库名称注册新的自定义转换器。
     *
     * @param dbName     数据库名称
     * @param translator 自定义转换器
     */
    public void registerTranslator(String dbName, SQLExceptionTranslator translator) {
        SQLExceptionTranslator replaced = this.translatorMap.put(dbName, translator);
        if (logger.isDebugEnabled()) {
            if (replaced != null) {
                logger.debug(
                        "Replacing custom translator [" + replaced + "] for database '" + dbName +
                                "' with [" + translator + "]"
                );
            } else {
                logger.debug(
                        "Adding custom translator of type [" + translator.getClass().getName() +
                                "] for database '" + dbName + "'"
                );
            }
        }
    }

    /**
     * 查找指定数据库的自定义转换器。
     *
     * @param dbName 数据库名称
     * @return 自定义转换器，如果未找到，则为null
     */
    public SQLExceptionTranslator findTranslatorForDatabase(String dbName) {
        return this.translatorMap.get(dbName);
    }
}
