package org.clever.validation;

import org.clever.beans.ConfigurablePropertyAccessor;
import org.clever.beans.PropertyAccessorFactory;

/**
 * Errors 和 BindingResult 接口的特殊实现，支持在值对象上注册和评估绑定错误。执行直接字段访问而不是通过 JavaBean getter。
 * <p>此实现能够遍历嵌套字段。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2023/06/08 21:31 <br/>
 *
 * @see DataBinder#getBindingResult()
 * @see DataBinder#initDirectFieldAccess()
 * @see BeanPropertyBindingResult
 */
public class DirectFieldBindingResult extends AbstractPropertyBindingResult {
    private final Object target;
    private final boolean autoGrowNestedPaths;
    private transient ConfigurablePropertyAccessor directFieldAccessor;

    /**
     * @param target     要绑定到的目标对象
     * @param objectName 目标对象的名称
     */
    public DirectFieldBindingResult(Object target, String objectName) {
        this(target, objectName, true);
    }

    /**
     * @param target              要绑定到的目标对象
     * @param objectName          目标对象的名称
     * @param autoGrowNestedPaths 是否“auto-grow”包含空值的嵌套路径
     */
    public DirectFieldBindingResult(Object target, String objectName, boolean autoGrowNestedPaths) {
        super(objectName);
        this.target = target;
        this.autoGrowNestedPaths = autoGrowNestedPaths;
    }

    @Override
    public final Object getTarget() {
        return this.target;
    }

    /**
     * 返回此实例使用的 DirectFieldAccessor。如果之前不存在，则创建一个新的。
     *
     * @see #createDirectFieldAccessor()
     */
    @Override
    public final ConfigurablePropertyAccessor getPropertyAccessor() {
        if (this.directFieldAccessor == null) {
            this.directFieldAccessor = createDirectFieldAccessor();
            this.directFieldAccessor.setExtractOldValueForEditor(true);
            this.directFieldAccessor.setAutoGrowNestedPaths(this.autoGrowNestedPaths);
        }
        return this.directFieldAccessor;
    }

    /**
     * 为底层目标对象创建一个新的 DirectFieldAccessor。
     *
     * @see #getTarget()
     */
    protected ConfigurablePropertyAccessor createDirectFieldAccessor() {
        if (this.target == null) {
            throw new IllegalStateException("Cannot access fields on null target instance '" + getObjectName() + "'");
        }
        return PropertyAccessorFactory.forDirectFieldAccess(this.target);
    }
}
