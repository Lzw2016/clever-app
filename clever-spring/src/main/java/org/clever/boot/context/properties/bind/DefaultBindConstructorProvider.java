package org.clever.boot.context.properties.bind;

import org.clever.beans.BeanUtils;
import org.clever.core.KotlinDetector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

/**
 * 默认的 {@link BindConstructorProvider} 实现
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:09 <br/>
 */
class DefaultBindConstructorProvider implements BindConstructorProvider {
    @Override
    public Constructor<?> getBindConstructor(Bindable<?> bindable, boolean isNestedConstructorBinding) {
        Class<?> type = bindable.getType().resolve();
        if (bindable.getValue() != null || type == null) {
            return null;
        }
        if (KotlinDetector.isKotlinPresent() && KotlinDetector.isKotlinType(type)) {
            return getDeducedKotlinConstructor(type);
        }
        Constructor<?>[] constructors = type.getDeclaredConstructors();
        if (constructors.length == 1 && constructors[0].getParameterCount() > 0) {
            return constructors[0];
        }
        Constructor<?> constructor = null;
        for (Constructor<?> candidate : constructors) {
            if (!Modifier.isPrivate(candidate.getModifiers())) {
                if (constructor != null) {
                    return null;
                }
                constructor = candidate;
            }
        }
        if (constructor != null && constructor.getParameterCount() > 0) {
            return constructor;
        }
        return null;
    }

    private Constructor<?> getDeducedKotlinConstructor(Class<?> type) {
        Constructor<?> primaryConstructor = BeanUtils.findPrimaryConstructor(type);
        if (primaryConstructor != null && primaryConstructor.getParameterCount() > 0) {
            return primaryConstructor;
        }
        return null;
    }
}
