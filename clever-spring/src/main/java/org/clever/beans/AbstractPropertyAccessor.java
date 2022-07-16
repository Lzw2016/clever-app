package org.clever.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * {@link PropertyAccessor}接口的抽象实现。
 * 提供所有方便方法的基本实现，实际属性访问的实现留给子类
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:18 <br/>
 *
 * @see #getPropertyValue
 * @see #setPropertyValue
 */
public abstract class AbstractPropertyAccessor extends TypeConverterSupport implements ConfigurablePropertyAccessor {
    private boolean extractOldValueForEditor = false;
    private boolean autoGrowNestedPaths = false;
    boolean suppressNotWritablePropertyException = false;

    @Override
    public void setExtractOldValueForEditor(boolean extractOldValueForEditor) {
        this.extractOldValueForEditor = extractOldValueForEditor;
    }

    @Override
    public boolean isExtractOldValueForEditor() {
        return this.extractOldValueForEditor;
    }

    @Override
    public void setAutoGrowNestedPaths(boolean autoGrowNestedPaths) {
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    @Override
    public boolean isAutoGrowNestedPaths() {
        return this.autoGrowNestedPaths;
    }

    @Override
    public void setPropertyValue(PropertyValue pv) throws BeansException {
        setPropertyValue(pv.getName(), pv.getValue());
    }

    @Override
    public void setPropertyValues(Map<?, ?> map) throws BeansException {
        setPropertyValues(new MutablePropertyValues(map));
    }

    @Override
    public void setPropertyValues(PropertyValues pvs) throws BeansException {
        setPropertyValues(pvs, false, false);
    }

    @Override
    public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown) throws BeansException {
        setPropertyValues(pvs, ignoreUnknown, false);
    }

    @Override
    public void setPropertyValues(PropertyValues pvs, boolean ignoreUnknown, boolean ignoreInvalid) throws BeansException {
        List<PropertyAccessException> propertyAccessExceptions = null;
        List<PropertyValue> propertyValues = pvs instanceof MutablePropertyValues ?
                ((MutablePropertyValues) pvs).getPropertyValueList() :
                Arrays.asList(pvs.getPropertyValues());
        if (ignoreUnknown) {
            this.suppressNotWritablePropertyException = true;
        }
        try {
            for (PropertyValue pv : propertyValues) {
                // setPropertyValue may throw any BeansException, which won't be caught
                // here, if there is a critical failure such as no matching field.
                // We can attempt to deal only with less serious exceptions.
                try {
                    setPropertyValue(pv);
                } catch (NotWritablePropertyException ex) {
                    if (!ignoreUnknown) {
                        throw ex;
                    }
                    // Otherwise, just ignore it and continue...
                } catch (NullValueInNestedPathException ex) {
                    if (!ignoreInvalid) {
                        throw ex;
                    }
                    // Otherwise, just ignore it and continue...
                } catch (PropertyAccessException ex) {
                    if (propertyAccessExceptions == null) {
                        propertyAccessExceptions = new ArrayList<>();
                    }
                    propertyAccessExceptions.add(ex);
                }
            }
        } finally {
            if (ignoreUnknown) {
                this.suppressNotWritablePropertyException = false;
            }
        }
        // If we encountered individual exceptions, throw the composite exception.
        if (propertyAccessExceptions != null) {
            PropertyAccessException[] paeArray = propertyAccessExceptions.toArray(new PropertyAccessException[0]);
            throw new PropertyBatchUpdateException(paeArray);
        }
    }

    @Override
    public Class<?> getPropertyType(String propertyPath) {
        return null;
    }

    /**
     * 实际获取属性的值
     *
     * @param propertyName 要获取其值的属性的名称
     * @throws InvalidPropertyException 如果没有此类属性或属性不可读
     * @throws PropertyAccessException  如果属性有效但访问器方法失败
     */
    @Override
    public abstract Object getPropertyValue(String propertyName) throws BeansException;

    /**
     * 实际设置属性值
     *
     * @param propertyName 属性的名称
     * @param value        新值
     * @throws InvalidPropertyException 如果没有此类属性或属性不可写
     * @throws PropertyAccessException  如果属性有效，但访问器方法失败或发生类型不匹配
     */
    @Override
    public abstract void setPropertyValue(String propertyName, Object value) throws BeansException;
}
