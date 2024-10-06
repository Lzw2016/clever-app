package org.clever.js.graaljs.proxy;

import org.clever.core.Assert;
import org.clever.core.reflection.ReflectionsUtils;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 作者：lizw <br/>
 * 创建时间：2024/01/20 18:46 <br/>
 */
public class BeanProxy implements ProxyObject {
    private final Object bean;

    public BeanProxy(Object bean) {
        this.bean = bean;
    }

    @Override
    public Object getMember(String key) {
        if (bean == null) {
            return null;
        }
        return ReflectionsUtils.getFieldValue(bean, key, false);
    }

    @Override
    public Object getMemberKeys() {
        if (bean == null) {
            return new ArrayListProxy(0);
        }
        Field[] fields = ReflectionsUtils.getAllField(bean.getClass());
        return new ArrayListProxy(Arrays.stream(fields).map(Field::getName).collect(Collectors.toList()));
    }

    @Override
    public boolean hasMember(String key) {
        if (bean == null) {
            return false;
        }
        return ReflectionsUtils.getAccessibleField(bean, key) != null;
    }

    @Override
    public void putMember(String key, Value value) {
        if (bean == null) {
            return;
        }
        Field field = ReflectionsUtils.getAccessibleField(bean, key);
        Assert.notNull(field, "对象字段不存在, field=" + key);
        Object obj;
        if (field.getType().isAssignableFrom(value.getClass())) {
            obj = value.asHostObject();
        } else if (value.isHostObject()) {
            obj = value;
        } else {
            obj = value.as(field.getType());
        }
        ReflectionsUtils.setFieldValue(bean, key, obj, false);
    }
}
