package org.clever.beans;

import org.clever.core.ResolvableType;
import org.clever.core.convert.Property;
import org.clever.core.convert.TypeDescriptor;
import org.clever.util.ReflectionUtils;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.security.*;

/**
 * 默认{@link BeanWrapper}实现，应该足以满足所有典型用例。缓存内省结果以提高效率。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 15:39 <br/>
 *
 * @see #registerCustomEditor
 * @see #setPropertyValues
 * @see #setPropertyValue
 * @see #getPropertyValue
 * @see #getPropertyType
 * @see BeanWrapper
 * @see PropertyEditorRegistrySupport
 */
public class BeanWrapperImpl extends AbstractNestablePropertyAccessor implements BeanWrapper {
    /**
     * 缓存此对象的内省结果，以防止每次遇到JavaBeans内省的成本
     */
    private CachedIntrospectionResults cachedIntrospectionResults;
    /**
     * 用于调用属性方法的安全上下文
     */
    private AccessControlContext acc;

    /**
     * 创建新的空BeanWrapperImpl。之后需要设置包装实例。注册默认编辑器
     *
     * @see #setWrappedInstance
     */
    public BeanWrapperImpl() {
        this(true);
    }

    /**
     * 创建新的空BeanWrapperImpl。之后需要设置包装实例
     * <p>
     * Create a new empty BeanWrapperImpl. Wrapped instance needs to be set afterwards.
     *
     * @param registerDefaultEditors 是否注册默认编辑器
     * @see #setWrappedInstance
     */
    public BeanWrapperImpl(boolean registerDefaultEditors) {
        super(registerDefaultEditors);
    }

    /**
     * 为给定对象创建新的BeanWrapperImpl
     *
     * @param object 此BeanWrapperImpl包裹的对象
     */
    public BeanWrapperImpl(Object object) {
        super(object);
    }

    /**
     * 创建新的BeanWrapperImpl，包装指定类的新实例
     *
     * @param clazz 要实例化和包装的类
     */
    public BeanWrapperImpl(Class<?> clazz) {
        super(clazz);
    }

    /**
     * 为给定对象创建新的BeanWrapperImpl，注册对象所在的嵌套路径
     *
     * @param object     此BeanWrapper包装的对象
     * @param nestedPath 对象的嵌套路径
     * @param rootObject 路径顶部的根对象
     */
    public BeanWrapperImpl(Object object, String nestedPath, Object rootObject) {
        super(object, nestedPath, rootObject);
    }

    /**
     * 为给定对象创建新的BeanWrapperImpl，注册对象所在的嵌套路径
     *
     * @param object     此BeanWrapper包装的对象
     * @param nestedPath 对象的嵌套路径
     * @param parent     包含BeanWrapper(不能为null)
     */
    private BeanWrapperImpl(Object object, String nestedPath, BeanWrapperImpl parent) {
        super(object, nestedPath, parent);
        setSecurityContext(parent.acc);
    }

    /**
     * 设置要保留的bean实例，无需展开{@link java.util.Optional}
     *
     * @param object 实际目标对象
     * @see #setWrappedInstance(Object)
     */
    public void setBeanInstance(Object object) {
        this.wrappedObject = object;
        this.rootObject = object;
        this.typeConverterDelegate = new TypeConverterDelegate(this, this.wrappedObject);
        setIntrospectionClass(object.getClass());
    }

    @Override
    public void setWrappedInstance(Object object, String nestedPath, Object rootObject) {
        super.setWrappedInstance(object, nestedPath, rootObject);
        setIntrospectionClass(getWrappedClass());
    }

    /**
     * 将类设置为内省。需要在目标对象更改时调用
     */
    protected void setIntrospectionClass(Class<?> clazz) {
        if (this.cachedIntrospectionResults != null && this.cachedIntrospectionResults.getBeanClass() != clazz) {
            this.cachedIntrospectionResults = null;
        }
    }

    /**
     * 获取包装对象的延迟初始化CachedIntrospectionResults实例
     */
    private CachedIntrospectionResults getCachedIntrospectionResults() {
        if (this.cachedIntrospectionResults == null) {
            this.cachedIntrospectionResults = CachedIntrospectionResults.forClass(getWrappedClass());
        }
        return this.cachedIntrospectionResults;
    }

    /**
     * 设置在调用包装实例方法期间使用的安全上下文。可以为null
     */
    public void setSecurityContext(AccessControlContext acc) {
        this.acc = acc;
    }

    /**
     * 返回在调用包装实例方法期间使用的安全上下文。可以为null
     */
    public AccessControlContext getSecurityContext() {
        return this.acc;
    }

    /**
     * 将指定属性的给定值转换为后者的类型。
     * 此方法仅用于BeanFactory中的优化。使用{@code convertIfNecessary}方法进行编程转换
     *
     * @param value        要转换的值
     * @param propertyName 目标属性(请注意，此处不支持嵌套或索引属性)
     * @throws TypeMismatchException 如果类型转换失败
     */
    public Object convertForProperty(Object value, String propertyName) throws TypeMismatchException {
        CachedIntrospectionResults cachedIntrospectionResults = getCachedIntrospectionResults();
        PropertyDescriptor pd = cachedIntrospectionResults.getPropertyDescriptor(propertyName);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName, "No property '" + propertyName + "' found");
        }
        TypeDescriptor td = cachedIntrospectionResults.getTypeDescriptor(pd);
        if (td == null) {
            td = cachedIntrospectionResults.addTypeDescriptor(pd, new TypeDescriptor(property(pd)));
        }
        return convertForProperty(propertyName, null, value, td);
    }

    private Property property(PropertyDescriptor pd) {
        GenericTypeAwarePropertyDescriptor gpd = (GenericTypeAwarePropertyDescriptor) pd;
        return new Property(gpd.getBeanClass(), gpd.getReadMethod(), gpd.getWriteMethod(), gpd.getName());
    }

    @Override
    protected BeanPropertyHandler getLocalPropertyHandler(String propertyName) {
        PropertyDescriptor pd = getCachedIntrospectionResults().getPropertyDescriptor(propertyName);
        return (pd != null ? new BeanPropertyHandler(pd) : null);
    }

    @Override
    protected BeanWrapperImpl newNestedPropertyAccessor(Object object, String nestedPath) {
        return new BeanWrapperImpl(object, nestedPath, this);
    }

    @Override
    protected NotWritablePropertyException createNotWritablePropertyException(String propertyName) {
        PropertyMatches matches = PropertyMatches.forProperty(propertyName, getRootClass());
        throw new NotWritablePropertyException(getRootClass(), getNestedPath() + propertyName, matches.buildErrorMessage(), matches.getPossibleMatches());
    }

    @Override
    public PropertyDescriptor[] getPropertyDescriptors() {
        return getCachedIntrospectionResults().getPropertyDescriptors();
    }

    @Override
    public PropertyDescriptor getPropertyDescriptor(String propertyName) throws InvalidPropertyException {
        BeanWrapperImpl nestedBw = (BeanWrapperImpl) getPropertyAccessorForPropertyPath(propertyName);
        String finalPath = getFinalPath(nestedBw, propertyName);
        PropertyDescriptor pd = nestedBw.getCachedIntrospectionResults().getPropertyDescriptor(finalPath);
        if (pd == null) {
            throw new InvalidPropertyException(getRootClass(), getNestedPath() + propertyName, "No property '" + propertyName + "' found");
        }
        return pd;
    }

    private class BeanPropertyHandler extends PropertyHandler {
        private final PropertyDescriptor pd;

        public BeanPropertyHandler(PropertyDescriptor pd) {
            super(pd.getPropertyType(), pd.getReadMethod() != null, pd.getWriteMethod() != null);
            this.pd = pd;
        }

        @Override
        public ResolvableType getResolvableType() {
            return ResolvableType.forMethodReturnType(this.pd.getReadMethod());
        }

        @Override
        public TypeDescriptor toTypeDescriptor() {
            return new TypeDescriptor(property(this.pd));
        }

        @Override
        public TypeDescriptor nested(int level) {
            return TypeDescriptor.nested(property(this.pd), level);
        }

        @Override
        public Object getValue() throws Exception {
            Method readMethod = this.pd.getReadMethod();
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(readMethod);
                    return null;
                });
                try {
                    return AccessController.doPrivileged(
                            (PrivilegedExceptionAction<Object>) () -> readMethod.invoke(getWrappedInstance(), (Object[]) null),
                            acc
                    );
                } catch (PrivilegedActionException pae) {
                    throw pae.getException();
                }
            } else {
                ReflectionUtils.makeAccessible(readMethod);
                return readMethod.invoke(getWrappedInstance(), (Object[]) null);
            }
        }

        @Override
        public void setValue(Object value) throws Exception {
            Method writeMethod = (
                    this.pd instanceof GenericTypeAwarePropertyDescriptor ?
                            ((GenericTypeAwarePropertyDescriptor) this.pd).getWriteMethodForActualAccess() :
                            this.pd.getWriteMethod()
            );
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
                    ReflectionUtils.makeAccessible(writeMethod);
                    return null;
                });
                try {
                    AccessController.doPrivileged((PrivilegedExceptionAction<Object>) () -> writeMethod.invoke(getWrappedInstance(), value), acc);
                } catch (PrivilegedActionException ex) {
                    throw ex.getException();
                }
            } else {
                ReflectionUtils.makeAccessible(writeMethod);
                writeMethod.invoke(getWrappedInstance(), value);
            }
        }
    }
}
