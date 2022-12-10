package org.clever.jdbc.core;

import org.clever.beans.*;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.support.DefaultConversionService;
import org.clever.dao.DataRetrievalFailureException;
import org.clever.dao.InvalidDataAccessApiUsageException;
import org.clever.jdbc.support.JdbcUtils;
import org.clever.util.Assert;
import org.clever.util.ClassUtils;
import org.clever.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyDescriptor;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * {@link RowMapper} 将行转换为指定映射目标类的新实例的实现。
 * 映射的目标类必须是顶级类，并且必须具有默认或无参数构造函数。
 *
 * <p>基于从结果集元数据获得的列名与相应属性的公共设置器的匹配，映射列值。
 * 名称可以直接匹配，也可以通过使用“驼峰”大小写将用下划线分隔的部分的名称转换为相同的名称来匹配。
 *
 * <p>为许多常见类型的目标类中的字段提供映射，例如：
 * String、boolean、Boolean、byte、Byte、short、Short、int、Integer、long、Long、
 * float、Float、double、Double、BigDecimal、java. util.Date 等
 *
 * <p>为了促进没有匹配名称的列和字段之间的映射，请尝试在 SQL 语句中使用列别名，
 * 例如 “select fname as first_name from customer”
 *
 * <p>对于从数据库读取的“null”值，我们将尝试调用 setter，但对于 Java 原语，这会导致 TypeMismatchException。
 * 可以配置此类（使用 primitivesDefaultedForNullValue 属性）以捕获此异常并使用原语默认值。
 * 请注意，如果您使用生成的 bean 中的值来更新数据库，原始值将被设置为原始值的默认值而不是 null。
 *
 * <p> 请注意，此类旨在提供便利而非高性能。为了获得最佳性能，请考虑使用自定义 {@link RowMapper} 实现。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 19:23 <br/>
 *
 * @param <T> 返回对象类型
 * @see DataClassRowMapper
 */
public class BeanPropertyRowMapper<T> implements RowMapper<T> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * 我们要映射到的类
     */
    private Class<T> mappedClass;
    /**
     * 我们是否在严格验证
     */
    private boolean checkFullyPopulated = false;
    /**
     * 映射空值时我们是否默认原语
     */
    private boolean primitivesDefaultedForNullValue = false;
    /**
     * 用于将 JDBC 值绑定到 bean 属性的 ConversionService
     */
    private ConversionService conversionService = DefaultConversionService.getSharedInstance();
    /**
     * 我们为其提供映射的字段的映射
     */
    private Map<String, PropertyDescriptor> mappedFields;
    /**
     * 我们为其提供映射的一组 bean 属性
     */
    private Set<String> mappedProperties;

    /**
     * 为 bean 样式配置创建一个新的 {@code BeanPropertyRowMapper}
     *
     * @see #setMappedClass
     * @see #setCheckFullyPopulated
     */
    public BeanPropertyRowMapper() {
    }

    /**
     * 创建一个新的 {@code BeanPropertyRowMapper}，接受目标 bean 中未填充的属性
     *
     * @param mappedClass 每行应映射到的类
     */
    public BeanPropertyRowMapper(Class<T> mappedClass) {
        initialize(mappedClass);
    }

    /**
     * 创建一个新的 {@code BeanPropertyRowMapper}
     *
     * @param mappedClass         每行应映射到的类
     * @param checkFullyPopulated 我们是否严格验证所有 bean 属性都已从相应的数据库字段映射
     */
    public BeanPropertyRowMapper(Class<T> mappedClass, boolean checkFullyPopulated) {
        initialize(mappedClass);
        this.checkFullyPopulated = checkFullyPopulated;
    }

    /**
     * 设置每行应映射到的类
     */
    public void setMappedClass(Class<T> mappedClass) {
        if (this.mappedClass == null) {
            initialize(mappedClass);
        } else {
            if (this.mappedClass != mappedClass) {
                throw new InvalidDataAccessApiUsageException(
                        "The mapped class can not be reassigned to map to " +
                                mappedClass + " since it is already providing mapping for " + this.mappedClass
                );
            }
        }
    }

    /**
     * 获取我们要映射到的类
     */
    public final Class<T> getMappedClass() {
        return this.mappedClass;
    }

    /**
     * 设置我们是否严格验证所有 bean 属性是否已从相应的数据库字段映射
     * <p>默认为 {@code false}，接受目标 bean 中未填充的属性
     */
    public void setCheckFullyPopulated(boolean checkFullyPopulated) {
        this.checkFullyPopulated = checkFullyPopulated;
    }

    /**
     * 返回我们是否严格验证所有 bean 属性是否已从相应的数据库字段映射
     */
    public boolean isCheckFullyPopulated() {
        return this.checkFullyPopulated;
    }

    /**
     * 在从相应的数据库字段映射空值的情况下，设置我们是否默认 Java 原语
     * <p>默认为 {@code false}，当 null 映射到 Java 原语时抛出异常
     */
    public void setPrimitivesDefaultedForNullValue(boolean primitivesDefaultedForNullValue) {
        this.primitivesDefaultedForNullValue = primitivesDefaultedForNullValue;
    }

    /**
     * 在从相应的数据库字段映射空值的情况下，返回我们是否默认 Java 原语
     */
    public boolean isPrimitivesDefaultedForNullValue() {
        return this.primitivesDefaultedForNullValue;
    }

    /**
     * 设置一个 {@link ConversionService} 以将 JDBC 值绑定到 bean 属性，或设置 {@code null} 为无
     * <p>默认是 {@link DefaultConversionService}。这提供了对 {@code java.time} 转换和其他特殊类型的支持。
     *
     * @see #initBeanWrapper(BeanWrapper)
     */
    public void setConversionService(ConversionService conversionService) {
        this.conversionService = conversionService;
    }

    /**
     * 返回一个 {@link ConversionService} 用于将 JDBC 值绑定到 bean 属性，如果没有则返回 {@code null}
     */
    public ConversionService getConversionService() {
        return this.conversionService;
    }

    /**
     * 为给定类初始化映射元数据
     *
     * @param mappedClass 映射类
     */
    protected void initialize(Class<T> mappedClass) {
        this.mappedClass = mappedClass;
        this.mappedFields = new HashMap<>();
        this.mappedProperties = new HashSet<>();
        for (PropertyDescriptor pd : BeanUtils.getPropertyDescriptors(mappedClass)) {
            if (pd.getWriteMethod() != null) {
                String lowerCaseName = lowerCaseName(pd.getName());
                this.mappedFields.put(lowerCaseName, pd);
                String underscoreName = underscoreName(pd.getName());
                if (!lowerCaseName.equals(underscoreName)) {
                    this.mappedFields.put(underscoreName, pd);
                }
                this.mappedProperties.add(pd.getName());
            }
        }
    }

    /**
     * 从映射字段中删除指定的属性
     *
     * @param propertyName 属性名称（由属性描述符使用）
     */
    protected void suppressProperty(String propertyName) {
        if (this.mappedFields != null) {
            this.mappedFields.remove(lowerCaseName(propertyName));
            this.mappedFields.remove(underscoreName(propertyName));
        }
    }

    /**
     * 将给定名称转换为小写。默认情况下，转换将在美国区域发生
     *
     * @param name 原名
     * @return 转换后的名字
     */
    protected String lowerCaseName(String name) {
        return name.toLowerCase(Locale.US);
    }

    /**
     * 将驼峰命名的名称转换为带下划线的小写名称。任何大写字母都将转换为带有前面下划线的小写字母。
     *
     * @param name 原名
     * @return 转换后的名字
     * @see #lowerCaseName
     */
    protected String underscoreName(String name) {
        if (!StringUtils.hasLength(name)) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        result.append(Character.toLowerCase(name.charAt(0)));
        for (int i = 1; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c)) {
                result.append('_').append(Character.toLowerCase(c));
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }

    /**
     * 提取当前行中所有列的值
     * <p>利用公共设置器和结果集元数据
     *
     * @see java.sql.ResultSetMetaData
     */
    @Override
    public T mapRow(ResultSet rs, int rowNumber) throws SQLException {
        BeanWrapperImpl bw = new BeanWrapperImpl();
        initBeanWrapper(bw);
        T mappedObject = constructMappedInstance(rs, bw);
        bw.setBeanInstance(mappedObject);
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        Set<String> populatedProperties = (isCheckFullyPopulated() ? new HashSet<>() : null);
        for (int index = 1; index <= columnCount; index++) {
            String column = JdbcUtils.lookupColumnName(rsmd, index);
            String field = lowerCaseName(StringUtils.delete(column, " "));
            PropertyDescriptor pd = (this.mappedFields != null ? this.mappedFields.get(field) : null);
            if (pd != null) {
                try {
                    Object value = getColumnValue(rs, index, pd);
                    if (rowNumber == 0 && logger.isDebugEnabled()) {
                        logger.debug(
                                "Mapping column '" + column +
                                        "' to property '" + pd.getName() +
                                        "' of type '" + ClassUtils.getQualifiedName(pd.getPropertyType()) + "'"
                        );
                    }
                    try {
                        bw.setPropertyValue(pd.getName(), value);
                    } catch (TypeMismatchException ex) {
                        if (value == null && this.primitivesDefaultedForNullValue) {
                            if (logger.isDebugEnabled()) {
                                logger.debug(
                                        "Intercepted TypeMismatchException for row " + rowNumber +
                                                " and column '" + column + "' with null value when setting property '" +
                                                pd.getName() + "' of type '" +
                                                ClassUtils.getQualifiedName(pd.getPropertyType()) +
                                                "' on object: " + mappedObject, ex
                                );
                            }
                        } else {
                            throw ex;
                        }
                    }
                    if (populatedProperties != null) {
                        populatedProperties.add(pd.getName());
                    }
                } catch (NotWritablePropertyException ex) {
                    throw new DataRetrievalFailureException("Unable to map column '" + column + "' to property '" + pd.getName() + "'", ex);
                }
            }
        }
        if (populatedProperties != null && !populatedProperties.equals(this.mappedProperties)) {
            throw new InvalidDataAccessApiUsageException(
                    "Given ResultSet does not contain all fields " +
                            "necessary to populate object of " +
                            this.mappedClass + ": " + this.mappedProperties
            );
        }
        return mappedObject;
    }

    /**
     * 为当前行构造一个映射类的实例
     *
     * @param rs 要映射的结果集（为当前行预初始化）
     * @param tc 带有此 RowMapper 转换服务的 TypeConverter
     * @return 映射类的相应实例
     * @throws SQLException 如果遇到 SQLException
     */
    protected T constructMappedInstance(ResultSet rs, TypeConverter tc) throws SQLException {
        Assert.state(this.mappedClass != null, "Mapped class was not specified");
        return BeanUtils.instantiateClass(this.mappedClass);
    }

    /**
     * 初始化给定的 BeanWrapper 以用于行映射。为每一行调用。
     * <p>默认实现应用配置的 {@link ConversionService}，如果有的话。可以在子类中被覆盖。
     *
     * @param bw 要初始化的 BeanWrapper
     * @see #getConversionService()
     * @see BeanWrapper#setConversionService
     */
    protected void initBeanWrapper(BeanWrapper bw) {
        ConversionService cs = getConversionService();
        if (cs != null) {
            bw.setConversionService(cs);
        }
    }

    /**
     * 检索指定列的 JDBC 对象值。
     * <p>默认实现委托给 {@link #getColumnValue(ResultSet, int, Class)}。
     *
     * @param rs    是保存数据的 ResultSet
     * @param index 是列索引
     * @param pd    每个结果对象预期匹配的 bean 属性
     * @return 对象值
     * @throws SQLException 在提取失败的情况下
     * @see #getColumnValue(ResultSet, int, Class)
     */
    protected Object getColumnValue(ResultSet rs, int index, PropertyDescriptor pd) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index, pd.getPropertyType());
    }

    /**
     * 检索指定列的 JDBC 对象值。
     * <p>默认实现调用 {@link JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)}。
     * 子类可以重写它以预先检查特定的值类型，或者对从 {@code getResultSetValue} 返回的值进行后处理。
     *
     * @param rs        是保存数据的 ResultSet
     * @param index     是列索引
     * @param paramType 目标参数类型
     * @return 对象值
     * @throws SQLException 在提取失败的情况下
     * @see org.clever.jdbc.support.JdbcUtils#getResultSetValue(java.sql.ResultSet, int, Class)
     */
    protected Object getColumnValue(ResultSet rs, int index, Class<?> paramType) throws SQLException {
        return JdbcUtils.getResultSetValue(rs, index, paramType);
    }

    /**
     * 创建新 {@code BeanPropertyRowMapper} 的静态工厂方法
     *
     * @param mappedClass 每行应映射到的类
     * @see #newInstance(Class, ConversionService)
     */
    public static <T> BeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
        return new BeanPropertyRowMapper<>(mappedClass);
    }

    /**
     * 创建新 {@code BeanPropertyRowMapper} 的静态工厂方法
     *
     * @param mappedClass       每行应映射到的类
     * @param conversionService {@link ConversionService} 用于将 JDBC 值绑定到 bean 属性，或 {@code null} 用于无
     * @see #newInstance(Class)
     * @see #setConversionService
     */
    public static <T> BeanPropertyRowMapper<T> newInstance(Class<T> mappedClass, ConversionService conversionService) {
        BeanPropertyRowMapper<T> rowMapper = newInstance(mappedClass);
        rowMapper.setConversionService(conversionService);
        return rowMapper;
    }
}
