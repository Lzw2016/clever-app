package org.clever.data.dynamic.sql.reflection.factory;


import org.clever.data.dynamic.sql.exception.ReflectionException;
import org.clever.data.dynamic.sql.reflection.Reflector;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.*;
import java.util.stream.Collectors;

public class DefaultObjectFactory implements ObjectFactory, Serializable {
    @Override
    public <T> T create(Class<T> type) {
        return create(type, null, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T create(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        Class<?> classToCreate = resolveInterface(type);
        return (T) instantiateClass(classToCreate, constructorArgTypes, constructorArgs);
    }

    @Override
    public <T> boolean isCollection(Class<T> type) {
        return Collection.class.isAssignableFrom(type);
    }

    private <T> T instantiateClass(Class<T> type, List<Class<?>> constructorArgTypes, List<Object> constructorArgs) {
        try {
            Constructor<T> constructor;
            if (constructorArgTypes == null || constructorArgs == null) {
                constructor = type.getDeclaredConstructor();
                try {
                    return constructor.newInstance();
                } catch (IllegalAccessException e) {
                    if (Reflector.canControlMemberAccessible()) {
                        constructor.setAccessible(true);
                        return constructor.newInstance();
                    } else {
                        throw e;
                    }
                }
            }
            constructor = type.getDeclaredConstructor(constructorArgTypes.toArray(new Class[0]));
            try {
                return constructor.newInstance(constructorArgs.toArray(new Object[0]));
            } catch (IllegalAccessException e) {
                if (Reflector.canControlMemberAccessible()) {
                    constructor.setAccessible(true);
                    return constructor.newInstance(constructorArgs.toArray(new Object[0]));
                } else {
                    throw e;
                }
            }
        } catch (Exception e) {
            String argTypes = Optional.ofNullable(constructorArgTypes)
                    .orElseGet(Collections::emptyList)
                    .stream().map(Class::getSimpleName)
                    .collect(Collectors.joining(","));
            String argValues = Optional.ofNullable(constructorArgs)
                    .orElseGet(Collections::emptyList)
                    .stream().map(String::valueOf)
                    .collect(Collectors.joining(","));
            throw new ReflectionException("Error instantiating " + type + " with invalid types (" + argTypes + ") or values (" + argValues + "). Cause: " + e, e);
        }
    }

    protected Class<?> resolveInterface(Class<?> type) {
        Class<?> classToCreate;
        if (type == List.class || type == Collection.class || type == Iterable.class) {
            classToCreate = ArrayList.class;
        } else if (type == Map.class) {
            classToCreate = HashMap.class;
        } else if (type == SortedSet.class) {
            classToCreate = TreeSet.class;
        } else if (type == Set.class) {
            classToCreate = HashSet.class;
        } else {
            classToCreate = type;
        }
        return classToCreate;
    }
}
