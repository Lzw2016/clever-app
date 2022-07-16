package org.clever.boot.context.properties.bind;

import java.lang.reflect.Constructor;

/**
 * 用于确定绑定时要使用的特定构造函数的策略接口。
 *
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:09 <br/>
 */
@FunctionalInterface
public interface BindConstructorProvider {
    /**
     * 默认的 {@link BindConstructorProvider} 实现，仅当存在单个构造函数且绑定表没有现有值时才返回值。
     */
    BindConstructorProvider DEFAULT = new DefaultBindConstructorProvider();

    /**
     * 返回用于给定绑定的绑定构造函数，如果不支持构造函数绑定，则返回null。
     * @param bindable 可检查的绑定
     * @param isNestedConstructorBinding 如果此绑定嵌套在构造函数绑定中
     * @return 绑定构造函数或null
     */
    Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding);
}
