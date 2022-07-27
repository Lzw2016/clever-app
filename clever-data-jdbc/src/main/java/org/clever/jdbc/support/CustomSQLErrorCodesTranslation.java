package org.clever.jdbc.support;

import org.clever.dao.DataAccessException;
import org.clever.util.StringUtils;

/**
 * 用于保存特定数据库的自定义JDBC错误代码转换的JavaBean。
 * “exceptionClass”属性定义将为errorCodes属性中指定的错误代码列表引发哪个异常。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 22:03 <br/>
 *
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class CustomSQLErrorCodesTranslation {
    private String[] errorCodes = new String[0];
    private Class<?> exceptionClass;

    /**
     * 将SQL错误代码设置为匹配。
     */
    public void setErrorCodes(String... errorCodes) {
        this.errorCodes = StringUtils.sortStringArray(errorCodes);
    }

    /**
     * 返回要匹配的SQL错误代码。
     */
    public String[] getErrorCodes() {
        return this.errorCodes;
    }

    /**
     * 为指定的错误代码设置异常类。
     */
    public void setExceptionClass(Class<?> exceptionClass) {
        if (exceptionClass != null && !DataAccessException.class.isAssignableFrom(exceptionClass)) {
            throw new IllegalArgumentException(
                    "Invalid exception class [" + exceptionClass +
                            "]: needs to be a subclass of [org.clever.dao.DataAccessException]"
            );
        }
        this.exceptionClass = exceptionClass;
    }

    /**
     * 返回指定错误代码的异常类。
     */
    public Class<?> getExceptionClass() {
        return this.exceptionClass;
    }
}
