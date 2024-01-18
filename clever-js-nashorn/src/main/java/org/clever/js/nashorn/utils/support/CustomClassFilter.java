package org.clever.js.nashorn.utils.support;

import jdk.nashorn.api.scripting.ClassFilter;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2020/07/25 10:10 <br/>
 */
public class CustomClassFilter implements ClassFilter {
    public final Set<String> denyAccessClass;

    public CustomClassFilter(Set<Class<?>> denyAccessClass) {
        this.denyAccessClass = denyAccessClass == null ? Collections.emptySet() : denyAccessClass.stream().map(Class::getName).collect(Collectors.toSet());
    }

    @Override
    public boolean exposeToScripts(String className) {
        return !denyAccessClass.contains(className);
    }
}
