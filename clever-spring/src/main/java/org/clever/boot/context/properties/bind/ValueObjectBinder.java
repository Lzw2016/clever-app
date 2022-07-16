package org.clever.boot.context.properties.bind;

import kotlin.reflect.KFunction;
import kotlin.reflect.KParameter;
import kotlin.reflect.jvm.ReflectJvmMapping;
import org.clever.beans.BeanUtils;
import org.clever.boot.context.properties.source.ConfigurationPropertyName;
import org.clever.core.*;
import org.clever.core.annotation.MergedAnnotation;
import org.clever.core.annotation.MergedAnnotations;
import org.clever.core.convert.ConversionException;
import org.clever.util.Assert;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;

/**
 * 用于不可变值对象的 {@link DataObjectBinder}
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 23:10 <br/>
 */
class ValueObjectBinder implements DataObjectBinder {
    private final BindConstructorProvider constructorProvider;

    ValueObjectBinder(BindConstructorProvider constructorProvider) {
        this.constructorProvider = constructorProvider;
    }

    @Override
    public <T> T bind(ConfigurationPropertyName name,
                      Bindable<T> target,
                      Binder.Context context,
                      DataObjectPropertyBinder propertyBinder) {
        ValueObject<T> valueObject = ValueObject.get(target, this.constructorProvider, context);
        if (valueObject == null) {
            return null;
        }
        context.pushConstructorBoundTypes(target.getType().resolve());
        List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
        List<Object> args = new ArrayList<>(parameters.size());
        boolean bound = false;
        for (ConstructorParameter parameter : parameters) {
            Object arg = parameter.bind(propertyBinder);
            bound = bound || arg != null;
            arg = (arg != null) ? arg : getDefaultValue(context, parameter);
            args.add(arg);
        }
        context.clearConfigurationProperty();
        context.popConstructorBoundTypes();
        return bound ? valueObject.instantiate(args) : null;
    }

    @Override
    public <T> T create(Bindable<T> target, Binder.Context context) {
        ValueObject<T> valueObject = ValueObject.get(target, this.constructorProvider, context);
        if (valueObject == null) {
            return null;
        }
        List<ConstructorParameter> parameters = valueObject.getConstructorParameters();
        List<Object> args = new ArrayList<>(parameters.size());
        for (ConstructorParameter parameter : parameters) {
            args.add(getDefaultValue(context, parameter));
        }
        return valueObject.instantiate(args);
    }

    private <T> T getDefaultValue(Binder.Context context, ConstructorParameter parameter) {
        ResolvableType type = parameter.getType();
        Annotation[] annotations = parameter.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof DefaultValue) {
                String[] defaultValue = ((DefaultValue) annotation).value();
                if (defaultValue.length == 0) {
                    return getNewInstanceIfPossible(context, type);
                }
                return convertDefaultValue(context.getConverter(), defaultValue, type, annotations);
            }
        }
        return null;
    }

    private <T> T convertDefaultValue(BindConverter converter,
                                      String[] defaultValue,
                                      ResolvableType type,
                                      Annotation[] annotations) {
        try {
            return converter.convert(defaultValue, type, annotations);
        } catch (ConversionException ex) {
            // Try again in case ArrayToObjectConverter is not in play
            if (defaultValue.length == 1) {
                return converter.convert(defaultValue[0], type, annotations);
            }
            throw ex;
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T getNewInstanceIfPossible(Binder.Context context, ResolvableType type) {
        Class<T> resolved = (Class<T>) type.resolve();
        Assert.state(
                resolved == null || isEmptyDefaultValueAllowed(resolved),
                () -> "Parameter of type " + type + " must have a non-empty default value."
        );
        T instance = create(Bindable.of(type), context);
        if (instance != null) {
            return instance;
        }
        return (resolved != null) ? BeanUtils.instantiateClass(resolved) : null;
    }

    private boolean isEmptyDefaultValueAllowed(Class<?> type) {
        return !type.isPrimitive() && !type.isEnum() && !isAggregate(type) && !type.getName().startsWith("java.lang");
    }

    private boolean isAggregate(Class<?> type) {
        return type.isArray() || Map.class.isAssignableFrom(type) || Collection.class.isAssignableFrom(type);
    }

    /**
     * 正在绑定的值对象。
     *
     * @param <T> 值对象类型
     */
    private abstract static class ValueObject<T> {
        private final Constructor<T> constructor;

        protected ValueObject(Constructor<T> constructor) {
            this.constructor = constructor;
        }

        T instantiate(List<Object> args) {
            return BeanUtils.instantiateClass(this.constructor, args.toArray());
        }

        abstract List<ConstructorParameter> getConstructorParameters();

        @SuppressWarnings("unchecked")
        static <T> ValueObject<T> get(Bindable<T> bindable,
                                      BindConstructorProvider constructorProvider,
                                      Binder.Context context) {
            Class<T> type = (Class<T>) bindable.getType().resolve();
            if (type == null || type.isEnum() || Modifier.isAbstract(type.getModifiers())) {
                return null;
            }
            Constructor<?> bindConstructor = constructorProvider.getBindConstructor(
                    bindable, context.isNestedConstructorBinding()
            );
            if (bindConstructor == null) {
                return null;
            }
            if (KotlinDetector.isKotlinType(type)) {
                return KotlinValueObject.get((Constructor<T>) bindConstructor, bindable.getType());
            }
            return DefaultValueObject.get(bindConstructor, bindable.getType());
        }
    }

    /**
     * 了解Kotlin特定构造的{@link ValueObject}实现。
     */
    private static final class KotlinValueObject<T> extends ValueObject<T> {
        private static final Annotation[] ANNOTATION_ARRAY = new Annotation[0];
        private final List<ConstructorParameter> constructorParameters;

        private KotlinValueObject(Constructor<T> primaryConstructor, KFunction<T> kotlinConstructor, ResolvableType type) {
            super(primaryConstructor);
            this.constructorParameters = parseConstructorParameters(kotlinConstructor, type);
        }

        private List<ConstructorParameter> parseConstructorParameters(KFunction<T> kotlinConstructor, ResolvableType type) {
            List<KParameter> parameters = kotlinConstructor.getParameters();
            List<ConstructorParameter> result = new ArrayList<>(parameters.size());
            for (KParameter parameter : parameters) {
                String name = getParameterName(parameter);
                ResolvableType parameterType = ResolvableType.forType(
                        ReflectJvmMapping.getJavaType(parameter.getType()), type
                );
                Annotation[] annotations = parameter.getAnnotations().toArray(ANNOTATION_ARRAY);
                result.add(new ConstructorParameter(name, parameterType, annotations));
            }
            return Collections.unmodifiableList(result);
        }

        private String getParameterName(KParameter parameter) {
            return MergedAnnotations.from(parameter, parameter.getAnnotations().toArray(ANNOTATION_ARRAY))
                    .get(Name.class)
                    .getValue(MergedAnnotation.VALUE, String.class)
                    .orElseGet(parameter::getName);
        }

        @Override
        List<ConstructorParameter> getConstructorParameters() {
            return this.constructorParameters;
        }

        static <T> ValueObject<T> get(Constructor<T> bindConstructor, ResolvableType type) {
            KFunction<T> kotlinConstructor = ReflectJvmMapping.getKotlinFunction(bindConstructor);
            if (kotlinConstructor != null) {
                return new KotlinValueObject<>(bindConstructor, kotlinConstructor, type);
            }
            return DefaultValueObject.get(bindConstructor, type);
        }
    }

    /**
     * 仅使用标准Java反射调用的默认{@link ValueObject}实现。
     */
    private static final class DefaultValueObject<T> extends ValueObject<T> {
        private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
        private final List<ConstructorParameter> constructorParameters;

        private DefaultValueObject(Constructor<T> constructor, ResolvableType type) {
            super(constructor);
            this.constructorParameters = parseConstructorParameters(constructor, type);
        }

        private static List<ConstructorParameter> parseConstructorParameters(Constructor<?> constructor, ResolvableType type) {
            String[] names = PARAMETER_NAME_DISCOVERER.getParameterNames(constructor);
            Assert.state(names != null, () -> "Failed to extract parameter names for " + constructor);
            Parameter[] parameters = constructor.getParameters();
            List<ConstructorParameter> result = new ArrayList<>(parameters.length);
            for (int i = 0; i < parameters.length; i++) {
                String name = MergedAnnotations.from(parameters[i])
                        .get(Name.class)
                        .getValue(MergedAnnotation.VALUE, String.class)
                        .orElse(names[i]);
                ResolvableType parameterType = ResolvableType.forMethodParameter(
                        new MethodParameter(constructor, i), type
                );
                Annotation[] annotations = parameters[i].getDeclaredAnnotations();
                result.add(new ConstructorParameter(name, parameterType, annotations));
            }
            return Collections.unmodifiableList(result);
        }

        @Override
        List<ConstructorParameter> getConstructorParameters() {
            return this.constructorParameters;
        }

        @SuppressWarnings("unchecked")
        static <T> ValueObject<T> get(Constructor<?> bindConstructor, ResolvableType type) {
            return new DefaultValueObject<>((Constructor<T>) bindConstructor, type);
        }
    }

    /**
     * 正在绑定的构造函数参数。
     */
    private static class ConstructorParameter {
        private final String name;
        private final ResolvableType type;
        private final Annotation[] annotations;

        ConstructorParameter(String name, ResolvableType type, Annotation[] annotations) {
            this.name = DataObjectPropertyName.toDashedForm(name);
            this.type = type;
            this.annotations = annotations;
        }

        Object bind(DataObjectPropertyBinder propertyBinder) {
            return propertyBinder.bindProperty(this.name, Bindable.of(this.type).withAnnotations(this.annotations));
        }

        Annotation[] getAnnotations() {
            return this.annotations;
        }

        ResolvableType getType() {
            return this.type;
        }
    }
}
