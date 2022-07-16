package org.clever.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link ParameterNameDiscoverer}实现，它连续尝试多个Discoverer委托。
 * 在addDiscoverer方法中首先添加的那些具有最高优先级。
 * 如果其中一个返回null，将尝试下一个。
 * 默认行为是，如果没有匹配的发现者，则返回null
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/05/26 14:20 <br/>
 */
public class PrioritizedParameterNameDiscoverer implements ParameterNameDiscoverer {
    /**
     * Discoverer委托
     */
    private final List<ParameterNameDiscoverer> parameterNameDiscoverers = new ArrayList<>(2);

    /**
     * 将另一个{@link ParameterNameDiscoverer}委托添加到该{@code PrioritizedParameterNameDiscoverer}检查的发现者列表中
     */
    public void addDiscoverer(ParameterNameDiscoverer pnd) {
        this.parameterNameDiscoverers.add(pnd);
    }

    @Override
    public String[] getParameterNames(Method method) {
        for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
            String[] result = pnd.getParameterNames(method);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public String[] getParameterNames(Constructor<?> ctor) {
        for (ParameterNameDiscoverer pnd : this.parameterNameDiscoverers) {
            String[] result = pnd.getParameterNames(ctor);
            if (result != null) {
                return result;
            }
        }
        return null;
    }
}
