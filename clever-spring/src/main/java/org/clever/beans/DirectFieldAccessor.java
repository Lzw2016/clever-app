package org.clever.beans;

import org.clever.core.ResolvableType;
import org.clever.core.convert.TypeDescriptor;
import org.clever.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link ConfigurablePropertyAccessor} 直接访问实例字段的实现。允许直接绑定到字段而不是通过 JavaBean 设置器。
 * <p>绝大多数 {@link BeanWrapper} 功能已合并到 {@link AbstractPropertyAccessor}，这意味着现在也支持属性遍历以及集合和映射访问。
 * <p>DirectFieldAccessor 的“extractOldValueForEditor”设置的默认值为“true”，因为字段始终可以无副作用地读取。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/01/01 21:52 <br/>
 *
 * @see #setExtractOldValueForEditor
 * @see BeanWrapper
 * @see org.clever.validation.DirectFieldBindingResult
 * @see org.clever.validation.DataBinder#initDirectFieldAccess()
 */
public class DirectFieldAccessor extends AbstractNestablePropertyAccessor {
    private final Map<String, FieldPropertyHandler> fieldMap = new HashMap<>();

    /**
     * 为给定对象创建一个新的 DirectFieldAccessor。
     *
     * @param object 此 DirectFieldAccessor 包装的对象
     */
    public DirectFieldAccessor(Object object) {
        super(object);
    }

    /**
     * 为给定对象创建一个新的 DirectFieldAccessor，注册该对象所在的嵌套路径
     *
     * @param object     此 DirectFieldAccessor 包装的对象
     * @param nestedPath 对象的嵌套路径
     * @param parent     包含的 DirectFieldAccessor(不能为 {@code null})
     */
    protected DirectFieldAccessor(Object object, String nestedPath, DirectFieldAccessor parent) {
        super(object, nestedPath, parent);
    }

    @Override
    protected FieldPropertyHandler getLocalPropertyHandler(String propertyName) {
        FieldPropertyHandler propertyHandler = this.fieldMap.get(propertyName);
        if (propertyHandler == null) {
            Field field = ReflectionUtils.findField(getWrappedClass(), propertyName);
            if (field != null) {
                propertyHandler = new FieldPropertyHandler(field);
                this.fieldMap.put(propertyName, propertyHandler);
            }
        }
        return propertyHandler;
    }

    @Override
    protected DirectFieldAccessor newNestedPropertyAccessor(Object object, String nestedPath) {
        return new DirectFieldAccessor(object, nestedPath, this);
    }

    @Override
    protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
        PropertyMatches matches = PropertyMatches.forField(propertyName, getRootClass());
        throw new NotWritablePropertyException(
                getRootClass(),
                getNestedPath() + propertyName,
                matches.buildErrorMessage(),
                matches.getPossibleMatches()
        );
    }

    private class FieldPropertyHandler extends PropertyHandler {
        private final Field field;

        public FieldPropertyHandler(Field field) {
            super(field.getType(), true, true);
            this.field = field;
        }

        @Override
        public TypeDescriptor toTypeDescriptor() {
            return new TypeDescriptor(this.field);
        }

        @Override
        public ResolvableType getResolvableType() {
            return ResolvableType.forField(this.field);
        }

        @Override
        public TypeDescriptor nested(int level) {
            return TypeDescriptor.nested(this.field, level);
        }

        @Override
        public Object getValue() throws Exception {
            try {
                ReflectionUtils.makeAccessible(this.field);
                return this.field.get(getWrappedInstance());
            } catch (IllegalAccessException ex) {
                throw new InvalidPropertyException(getWrappedClass(), this.field.getName(), "Field is not accessible", ex);
            }
        }

        @Override
        public void setValue(Object value) throws Exception {
            try {
                ReflectionUtils.makeAccessible(this.field);
                this.field.set(getWrappedInstance(), value);
            } catch (IllegalAccessException ex) {
                throw new InvalidPropertyException(getWrappedClass(), this.field.getName(), "Field is not accessible", ex);
            }
        }
    }
}
