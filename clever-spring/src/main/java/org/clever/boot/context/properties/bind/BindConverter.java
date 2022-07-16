package org.clever.boot.context.properties.bind;

import org.clever.beans.BeanUtils;
import org.clever.beans.PropertyEditorRegistry;
import org.clever.beans.SimpleTypeConverter;
import org.clever.beans.propertyeditors.CustomBooleanEditor;
import org.clever.beans.propertyeditors.CustomNumberEditor;
import org.clever.beans.propertyeditors.FileEditor;
import org.clever.boot.convert.ApplicationConversionService;
import org.clever.core.ResolvableType;
import org.clever.core.convert.*;
import org.clever.core.convert.converter.ConditionalGenericConverter;
import org.clever.core.convert.support.GenericConversionService;
import org.clever.util.CollectionUtils;

import java.beans.PropertyEditor;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Consumer;

/**
 * 用于处理绑定期间所需的任何转换的实用程序。
 * 这个类不是线程安全的，因此会为每个顶级绑定创建一个新实例。
 * <p>
 * 作者：lizw <br/>
 * 创建时间：2022/07/05 22:24 <br/>
 */
final class BindConverter {
    private static BindConverter sharedInstance;
    private final List<ConversionService> delegates;

    private BindConverter(List<ConversionService> conversionServices, Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
        List<ConversionService> delegates = new ArrayList<>();
        delegates.add(new TypeConverterConversionService(propertyEditorInitializer));
        boolean hasApplication = false;
        if (!CollectionUtils.isEmpty(conversionServices)) {
            for (ConversionService conversionService : conversionServices) {
                delegates.add(conversionService);
                hasApplication = hasApplication || conversionService instanceof ApplicationConversionService;
            }
        }
        if (!hasApplication) {
            delegates.add(ApplicationConversionService.getSharedInstance());
        }
        this.delegates = Collections.unmodifiableList(delegates);
    }

    boolean canConvert(Object source, ResolvableType targetType, Annotation... targetAnnotations) {
        return canConvert(
                TypeDescriptor.forObject(source),
                new ResolvableTypeDescriptor(targetType, targetAnnotations)
        );
    }

    private boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
        for (ConversionService service : this.delegates) {
            if (service.canConvert(sourceType, targetType)) {
                return true;
            }
        }
        return false;
    }

    <T> T convert(Object source, Bindable<T> target) {
        return convert(source, target.getType(), target.getAnnotations());
    }

    @SuppressWarnings("unchecked")
    <T> T convert(Object source, ResolvableType targetType, Annotation... targetAnnotations) {
        if (source == null) {
            return null;
        }
        return (T) convert(
                source,
                TypeDescriptor.forObject(source),
                new ResolvableTypeDescriptor(targetType, targetAnnotations)
        );
    }

    private Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        ConversionException failure = null;
        for (ConversionService delegate : this.delegates) {
            try {
                if (delegate.canConvert(sourceType, targetType)) {
                    return delegate.convert(source, sourceType, targetType);
                }
            } catch (ConversionException ex) {
                if (failure == null && ex instanceof ConversionFailedException) {
                    failure = ex;
                }
            }
        }
        throw (failure != null) ? failure : new ConverterNotFoundException(sourceType, targetType);
    }

    static BindConverter get(List<ConversionService> conversionServices, Consumer<PropertyEditorRegistry> propertyEditorInitializer) {
        boolean sharedApplicationConversionService = (conversionServices == null)
                || (conversionServices.size() == 1 && conversionServices.get(0) == ApplicationConversionService.getSharedInstance());
        if (propertyEditorInitializer == null && sharedApplicationConversionService) {
            return getSharedInstance();
        }
        return new BindConverter(conversionServices, propertyEditorInitializer);
    }

    private static BindConverter getSharedInstance() {
        if (sharedInstance == null) {
            sharedInstance = new BindConverter(null, null);
        }
        return sharedInstance;
    }

    /**
     * 由 {@link ResolvableType} 支持的 {@link TypeDescriptor}
     */
    private static class ResolvableTypeDescriptor extends TypeDescriptor {
        ResolvableTypeDescriptor(ResolvableType resolvableType, Annotation[] annotations) {
            super(resolvableType, null, annotations);
        }
    }

    /**
     * 委托给 {@link SimpleTypeConverter} 的 {@link ConversionService} 实现。
     * 允许对简单类型、数组和集合进行基于 {@link PropertyEditor} 的转换。
     */
    private static class TypeConverterConversionService extends GenericConversionService {
        TypeConverterConversionService(Consumer<PropertyEditorRegistry> initializer) {
            addConverter(new TypeConverterConverter(initializer));
            ApplicationConversionService.addDelimitedStringConverters(this);
        }

        @Override
        public boolean canConvert(TypeDescriptor sourceType, TypeDescriptor targetType) {
            // Prefer conversion service to handle things like String to char[].
            if (targetType.isArray() && targetType.getElementTypeDescriptor().isPrimitive()) {
                return false;
            }
            return super.canConvert(sourceType, targetType);
        }
    }

    /**
     * 委托给 {@link SimpleTypeConverter} 的 {@link ConditionalGenericConverter}
     */
    private static class TypeConverterConverter implements ConditionalGenericConverter {
        private static final Set<Class<?>> EXCLUDED_EDITORS;

        static {
            Set<Class<?>> excluded = new HashSet<>();
            excluded.add(CustomNumberEditor.class);
            excluded.add(CustomBooleanEditor.class);
            excluded.add(FileEditor.class);
            EXCLUDED_EDITORS = Collections.unmodifiableSet(excluded);
        }

        private final Consumer<PropertyEditorRegistry> initializer;

        // SimpleTypeConverter is not thread-safe to use for conversion but we can use it
        // in a thread-safe way to check if conversion is possible.
        private final SimpleTypeConverter matchesOnlyTypeConverter;

        TypeConverterConverter(Consumer<PropertyEditorRegistry> initializer) {
            this.initializer = initializer;
            this.matchesOnlyTypeConverter = createTypeConverter();
        }

        @Override
        public Set<ConvertiblePair> getConvertibleTypes() {
            return Collections.singleton(new ConvertiblePair(String.class, Object.class));
        }

        @Override
        public boolean matches(TypeDescriptor sourceType, TypeDescriptor targetType) {
            Class<?> type = targetType.getType();
            if (type == null || type == Object.class || Collection.class.isAssignableFrom(type) || Map.class.isAssignableFrom(type)) {
                return false;
            }
            PropertyEditor editor = this.matchesOnlyTypeConverter.getDefaultEditor(type);
            if (editor == null) {
                editor = this.matchesOnlyTypeConverter.findCustomEditor(type, null);
            }
            if (editor == null && String.class != type) {
                editor = BeanUtils.findEditorByConvention(type);
            }
            return (editor != null && !EXCLUDED_EDITORS.contains(editor.getClass()));
        }

        @Override
        public Object convert(Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
            return createTypeConverter().convertIfNecessary(source, targetType.getType());
        }

        private SimpleTypeConverter createTypeConverter() {
            SimpleTypeConverter typeConverter = new SimpleTypeConverter();
            if (this.initializer != null) {
                this.initializer.accept(typeConverter);
            }
            return typeConverter;
        }
    }
}
