package org.clever.jdbc.core;

import org.clever.beans.BeanUtils;
import org.clever.beans.TypeConverter;
import org.clever.core.MethodParameter;
import org.clever.core.convert.ConversionService;
import org.clever.core.convert.TypeDescriptor;
import org.clever.util.Assert;

import java.lang.reflect.Constructor;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * {@link RowMapper} 将行转换为指定映射目标类的新实例的实现。
 * 映射的目标类必须是顶级类，并且可以公开具有与列名或经典 bean 属性设置器（甚至两者的组合）相对应的命名参数的数据类构造函数。
 *
 * <p>请注意，此类扩展了 {@link org.clever.jdbc.core.BeanPropertyRowMapper}，
 * 因此可以作为任何映射目标类的通用选择，灵活地适应映射类中的构造函数样式与 setter 方法。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/11/26 19:21 <br/>
 *
 * @param <T> 返回对象类型
 */
public class DataClassRowMapper<T> extends BeanPropertyRowMapper<T> {
    private Constructor<T> mappedConstructor;
    private String[] constructorParameterNames;
    private TypeDescriptor[] constructorParameterTypes;

    /**
     * 为 bean 样式配置创建一个新的 {@code DataClassRowMapper}
     *
     * @see #setMappedClass
     * @see #setConversionService
     */
    public DataClassRowMapper() {
    }

    /**
     * 创建一个新的 {@code DataClassRowMapper}
     *
     * @param mappedClass 每行应映射到的类
     */
    public DataClassRowMapper(Class<T> mappedClass) {
        super(mappedClass);
    }

    @Override
    protected void initialize(Class<T> mappedClass) {
        super.initialize(mappedClass);
        this.mappedConstructor = BeanUtils.getResolvableConstructor(mappedClass);
        int paramCount = this.mappedConstructor.getParameterCount();
        if (paramCount > 0) {
            this.constructorParameterNames = BeanUtils.getParameterNames(this.mappedConstructor);
            for (String name : this.constructorParameterNames) {
                suppressProperty(name);
            }
            this.constructorParameterTypes = new TypeDescriptor[paramCount];
            for (int i = 0; i < paramCount; i++) {
                this.constructorParameterTypes[i] = new TypeDescriptor(new MethodParameter(this.mappedConstructor, i));
            }
        }
    }

    @Override
    protected T constructMappedInstance(ResultSet rs, TypeConverter tc) throws SQLException {
        Assert.state(this.mappedConstructor != null, "Mapped constructor was not initialized");
        Object[] args;
        if (this.constructorParameterNames != null && this.constructorParameterTypes != null) {
            args = new Object[this.constructorParameterNames.length];
            for (int i = 0; i < args.length; i++) {
                String name = this.constructorParameterNames[i];
                int index;
                try {
                    // Try direct name match first
                    index = rs.findColumn(lowerCaseName(name));
                } catch (SQLException ex) {
                    // Try underscored name match instead
                    index = rs.findColumn(underscoreName(name));
                }
                TypeDescriptor td = this.constructorParameterTypes[i];
                Object value = getColumnValue(rs, index, td.getType());
                args[i] = tc.convertIfNecessary(value, td.getType(), td);
            }
        } else {
            args = new Object[0];
        }
        return BeanUtils.instantiateClass(this.mappedConstructor, args);
    }

    /**
     * 创建新 {@code DataClassRowMapper} 的静态工厂方法
     *
     * @param mappedClass 每行应映射到的类
     * @see #newInstance(Class, ConversionService)
     */
    public static <T> DataClassRowMapper<T> newInstance(Class<T> mappedClass) {
        return new DataClassRowMapper<>(mappedClass);
    }

    /**
     * 创建新 {@code DataClassRowMapper} 的静态工厂方法
     *
     * @param mappedClass       每行应映射到的类
     * @param conversionService {@link ConversionService} 用于将 JDBC 值绑定到 bean 属性，或 {@code null} 用于无
     * @see #newInstance(Class)
     * @see #setConversionService
     */
    public static <T> DataClassRowMapper<T> newInstance(Class<T> mappedClass, ConversionService conversionService) {
        DataClassRowMapper<T> rowMapper = newInstance(mappedClass);
        rowMapper.setConversionService(conversionService);
        return rowMapper;
    }
}
