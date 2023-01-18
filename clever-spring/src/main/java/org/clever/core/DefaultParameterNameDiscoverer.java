package org.clever.core;

/**
 * {@link ParameterNameDiscoverer}策略接口的默认实现，使用Java 8标准反射机制(如果可用)。
 * 如果存在Kotlin反射实现，则首先将{@link KotlinReflectionParameterNameDiscoverer}添加到列表中，并用于Kotlin类和接口。
 * 当以GraalVM本机映像编译或运行时，不会使用{@code KotlinReflectionParameterNameDiscoverer}。
 * 可以通过{@link #addDiscoverer(ParameterNameDiscoverer)}添加更多的发现者
 *
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:22 <br/>
 *
 * @see StandardReflectionParameterNameDiscoverer
 * @see KotlinReflectionParameterNameDiscoverer
 */
public class DefaultParameterNameDiscoverer extends PrioritizedParameterNameDiscoverer {
    public DefaultParameterNameDiscoverer(boolean useCache) {
        // 升级到Kotlin 1.5时删除此条件包含, see https://youtrack.jetbrains.com/issue/KT-44594
        if (KotlinDetector.isKotlinReflectPresent() && !NativeDetector.inNativeImage()) {
            addDiscoverer(new KotlinReflectionParameterNameDiscoverer());
        }
        addDiscoverer(new StandardReflectionParameterNameDiscoverer());
        addDiscoverer(new LocalVariableTableParameterNameDiscoverer(useCache));
    }

    public DefaultParameterNameDiscoverer() {
        this(true);
    }
}
