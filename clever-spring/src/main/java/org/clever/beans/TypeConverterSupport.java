package org.clever.beans;

import org.clever.core.MethodParameter;
import org.clever.core.convert.ConversionException;
import org.clever.core.convert.ConverterNotFoundException;
import org.clever.core.convert.TypeDescriptor;
import org.clever.util.Assert;

import java.lang.reflect.Field;

/**
 * TypeConverter接口的基本实现。主要用作{@link BeanWrapperImpl}的基类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/25 17:12 <br/>
 */
public abstract class TypeConverterSupport extends PropertyEditorRegistrySupport implements TypeConverter {
    TypeConverterDelegate typeConverterDelegate;

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType) throws TypeMismatchException {
        return convertIfNecessary(value, requiredType, TypeDescriptor.valueOf(requiredType));
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType, MethodParameter methodParam) throws TypeMismatchException {
        return convertIfNecessary(
                value,
                requiredType,
                (methodParam != null ? new TypeDescriptor(methodParam) : TypeDescriptor.valueOf(requiredType))
        );
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType, Field field) throws TypeMismatchException {
        return convertIfNecessary(
                value,
                requiredType,
                (field != null ? new TypeDescriptor(field) : TypeDescriptor.valueOf(requiredType))
        );
    }

    @Override
    public <T> T convertIfNecessary(Object value, Class<T> requiredType, TypeDescriptor typeDescriptor) throws TypeMismatchException {
        Assert.state(this.typeConverterDelegate != null, "No TypeConverterDelegate");
        try {
            return this.typeConverterDelegate.convertIfNecessary(null, null, value, requiredType, typeDescriptor);
        } catch (ConverterNotFoundException | IllegalStateException ex) {
            throw new ConversionNotSupportedException(value, requiredType, ex);
        } catch (ConversionException | IllegalArgumentException ex) {
            throw new TypeMismatchException(value, requiredType, ex);
        }
    }
}
