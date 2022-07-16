package org.clever.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 用来来发现方法和构造函数的参数名<br/>
 * 参数名发现并不总是可能的，但可以尝试各种策略，
 * 例如查找可能在编译时发出的调试信息，
 * 以及查找可选地伴随AspectJ注释方法的argName annotation值
 * 作者：lizw <br/>
 * 创建时间：2022/04/22 11:17 <br/>
 */
public interface ParameterNameDiscoverer {
    /**
     * 返回方法的参数名，如果无法确定，则返回null<br/>
     * 如果参数名仅适用于给定方法的某些参数，而不适用于其他参数，则数组中的单个条目可能为空<br/>
     * 但是，建议在可行的情况下使用存根参数名称<br/>
     *
     * @param method 查找的参数名的方法
     * @return 如果名称可以解析，则为参数名称数组；如果名称不能解析，则为null
     */
    String[] getParameterNames(Method method);

    /**
     * 返回构造函数的参数名，如果无法确定，则返回null<br/>
     * 如果参数名仅适用于给定构造函数的某些参数，而不适用于其他参数，则数组中的单个条目可能为空<br/>
     * 但是，建议在可行的情况下使用存根参数名称。
     *
     * @param ctor 要为其查找参数名的构造函数
     * @return 如果名称可以解析，则为参数名称数组；如果名称不能解析，则为null
     */
    String[] getParameterNames(Constructor<?> ctor);
}
