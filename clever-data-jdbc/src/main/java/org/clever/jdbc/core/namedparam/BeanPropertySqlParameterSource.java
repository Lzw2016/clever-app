package org.clever.jdbc.core.namedparam;

import org.clever.beans.BeanWrapper;
import org.clever.beans.NotReadablePropertyException;
import org.clever.beans.PropertyAccessorFactory;
import org.clever.jdbc.core.StatementCreatorUtils;
import org.clever.util.StringUtils;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link SqlParameterSource}实现，从给定JavaBean对象的bean属性中获取参数值。
 * bean属性的名称必须与参数名称匹配。
 *
 * <p>使用BeanWrapper访问下面的bean属性。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/02 23:55 <br/>
 *
 * @see NamedParameterJdbcTemplate
 * @see org.clever.beans.BeanWrapper
 */
public class BeanPropertySqlParameterSource extends AbstractSqlParameterSource {
    private final BeanWrapper beanWrapper;
    private String[] propertyNames;

    /**
     * 为给定bean创建新的BeanPropertySqlParameterSource。
     *
     * @param object 要包装的bean实例
     */
    public BeanPropertySqlParameterSource(Object object) {
        this.beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
    }

    @Override
    public boolean hasValue(String paramName) {
        return this.beanWrapper.isReadableProperty(paramName);
    }

    @Override
    public Object getValue(String paramName) throws IllegalArgumentException {
        try {
            return this.beanWrapper.getPropertyValue(paramName);
        } catch (NotReadablePropertyException ex) {
            throw new IllegalArgumentException(ex.getMessage());
        }
    }

    /**
     * 从相应的属性类型派生默认SQL类型。
     *
     * @see StatementCreatorUtils#javaTypeToSqlParameterType
     */
    @Override
    public int getSqlType(String paramName) {
        int sqlType = super.getSqlType(paramName);
        if (sqlType != TYPE_UNKNOWN) {
            return sqlType;
        }
        Class<?> propType = this.beanWrapper.getPropertyType(paramName);
        return StatementCreatorUtils.javaTypeToSqlParameterType(propType);
    }

    @Override
    public String[] getParameterNames() {
        return getReadablePropertyNames();
    }

    /**
     * 提供对包装bean的属性名称的访问。使用{@code PropertyAccessor}界面中提供的支持。
     *
     * @return 包含所有已知属性名的数组
     */
    public String[] getReadablePropertyNames() {
        if (this.propertyNames == null) {
            List<String> names = new ArrayList<>();
            PropertyDescriptor[] props = this.beanWrapper.getPropertyDescriptors();
            for (PropertyDescriptor pd : props) {
                if (this.beanWrapper.isReadableProperty(pd.getName())) {
                    names.add(pd.getName());
                }
            }
            this.propertyNames = StringUtils.toStringArray(names);
        }
        return this.propertyNames;
    }
}
