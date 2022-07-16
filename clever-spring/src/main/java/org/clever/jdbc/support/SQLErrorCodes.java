package org.clever.jdbc.support;

import org.clever.util.ReflectionUtils;
import org.clever.util.StringUtils;

/**
 * 用于保存特定数据库的JDBC错误代码的JavaBean。
 * 此类的实例通常通过bean工厂加载。
 *
 * <p>由{@link SQLErrorCodeSQLExceptionTranslator}使用。
 * 该包中的文件“sql-error-codes.xml”包含各种数据库的默认{@code SQLErrorCodes}实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/06/27 21:46 <br/>
 *
 * @see SQLErrorCodesFactory
 * @see SQLErrorCodeSQLExceptionTranslator
 */
public class SQLErrorCodes {
    private String[] databaseProductNames;
    private boolean useSqlStateForTranslation = false;
    private String[] badSqlGrammarCodes = new String[0];
    private String[] invalidResultSetAccessCodes = new String[0];
    private String[] duplicateKeyCodes = new String[0];
    private String[] dataIntegrityViolationCodes = new String[0];
    private String[] permissionDeniedCodes = new String[0];
    private String[] dataAccessResourceFailureCodes = new String[0];
    private String[] transientDataAccessResourceCodes = new String[0];
    private String[] cannotAcquireLockCodes = new String[0];
    private String[] deadlockLoserCodes = new String[0];
    private String[] cannotSerializeTransactionCodes = new String[0];
    private CustomSQLErrorCodesTranslation[] customTranslations;
    private SQLExceptionTranslator customSqlExceptionTranslator;

    /**
     * 如果数据库名称包含空格，则设置此属性，在这种情况下，我们不能使用bean名称进行查找。
     */
    public void setDatabaseProductName(String databaseProductName) {
        this.databaseProductNames = new String[]{databaseProductName};
    }

    public String getDatabaseProductName() {
        return (this.databaseProductNames != null && this.databaseProductNames.length > 0 ? this.databaseProductNames[0] : null);
    }

    /**
     * 设置此属性以指定包含空格的多个数据库名称，在这种情况下，我们不能使用bean名称进行查找。
     */
    public void setDatabaseProductNames(String... databaseProductNames) {
        this.databaseProductNames = databaseProductNames;
    }

    public String[] getDatabaseProductNames() {
        return this.databaseProductNames;
    }

    /**
     * 对于不提供错误代码但提供SQL状态（包括PostgreSQL）的数据库，请将此属性设置为true。
     */
    public void setUseSqlStateForTranslation(boolean useStateCodeForTranslation) {
        this.useSqlStateForTranslation = useStateCodeForTranslation;
    }

    public boolean isUseSqlStateForTranslation() {
        return this.useSqlStateForTranslation;
    }

    public void setBadSqlGrammarCodes(String... badSqlGrammarCodes) {
        this.badSqlGrammarCodes = StringUtils.sortStringArray(badSqlGrammarCodes);
    }

    public String[] getBadSqlGrammarCodes() {
        return this.badSqlGrammarCodes;
    }

    public void setInvalidResultSetAccessCodes(String... invalidResultSetAccessCodes) {
        this.invalidResultSetAccessCodes = StringUtils.sortStringArray(invalidResultSetAccessCodes);
    }

    public String[] getInvalidResultSetAccessCodes() {
        return this.invalidResultSetAccessCodes;
    }

    public String[] getDuplicateKeyCodes() {
        return this.duplicateKeyCodes;
    }

    public void setDuplicateKeyCodes(String... duplicateKeyCodes) {
        this.duplicateKeyCodes = duplicateKeyCodes;
    }

    public void setDataIntegrityViolationCodes(String... dataIntegrityViolationCodes) {
        this.dataIntegrityViolationCodes = StringUtils.sortStringArray(dataIntegrityViolationCodes);
    }

    public String[] getDataIntegrityViolationCodes() {
        return this.dataIntegrityViolationCodes;
    }

    public void setPermissionDeniedCodes(String... permissionDeniedCodes) {
        this.permissionDeniedCodes = StringUtils.sortStringArray(permissionDeniedCodes);
    }

    public String[] getPermissionDeniedCodes() {
        return this.permissionDeniedCodes;
    }

    public void setDataAccessResourceFailureCodes(String... dataAccessResourceFailureCodes) {
        this.dataAccessResourceFailureCodes = StringUtils.sortStringArray(dataAccessResourceFailureCodes);
    }

    public String[] getDataAccessResourceFailureCodes() {
        return this.dataAccessResourceFailureCodes;
    }

    public void setTransientDataAccessResourceCodes(String... transientDataAccessResourceCodes) {
        this.transientDataAccessResourceCodes = StringUtils.sortStringArray(transientDataAccessResourceCodes);
    }

    public String[] getTransientDataAccessResourceCodes() {
        return this.transientDataAccessResourceCodes;
    }

    public void setCannotAcquireLockCodes(String... cannotAcquireLockCodes) {
        this.cannotAcquireLockCodes = StringUtils.sortStringArray(cannotAcquireLockCodes);
    }

    public String[] getCannotAcquireLockCodes() {
        return this.cannotAcquireLockCodes;
    }

    public void setDeadlockLoserCodes(String... deadlockLoserCodes) {
        this.deadlockLoserCodes = StringUtils.sortStringArray(deadlockLoserCodes);
    }

    public String[] getDeadlockLoserCodes() {
        return this.deadlockLoserCodes;
    }

    public void setCannotSerializeTransactionCodes(String... cannotSerializeTransactionCodes) {
        this.cannotSerializeTransactionCodes = StringUtils.sortStringArray(cannotSerializeTransactionCodes);
    }

    public String[] getCannotSerializeTransactionCodes() {
        return this.cannotSerializeTransactionCodes;
    }

    public void setCustomTranslations(CustomSQLErrorCodesTranslation... customTranslations) {
        this.customTranslations = customTranslations;
    }

    public CustomSQLErrorCodesTranslation[] getCustomTranslations() {
        return this.customTranslations;
    }

    public void setCustomSqlExceptionTranslatorClass(Class<? extends SQLExceptionTranslator> customTranslatorClass) {
        if (customTranslatorClass != null) {
            try {
                this.customSqlExceptionTranslator = ReflectionUtils.accessibleConstructor(customTranslatorClass).newInstance();
            } catch (Throwable ex) {
                throw new IllegalStateException("Unable to instantiate custom translator", ex);
            }
        } else {
            this.customSqlExceptionTranslator = null;
        }
    }

    public void setCustomSqlExceptionTranslator(SQLExceptionTranslator customSqlExceptionTranslator) {
        this.customSqlExceptionTranslator = customSqlExceptionTranslator;
    }

    public SQLExceptionTranslator getCustomSqlExceptionTranslator() {
        return this.customSqlExceptionTranslator;
    }
}
